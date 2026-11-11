-- 18_fotos_personal.sql · 11-11-2026 (S14)
-- Foto de perfil del personal (Dueño, cajero/a y barberos) para identificarlo en la web y la app.
-- * usuario.foto_url guarda la imagen como data URL (PNG, JPEG o WebP en base64, máx. ~300 KB) o una URL https.
-- * api_login, api_barberos y api_personal_listar devuelven la foto.
-- * api_personal_foto: el Dueño cambia la foto de cualquier funcionario; cada funcionario puede cambiar la suya.
--   Los clientes no tienen foto de perfil (minimización de datos, Ley N° 21.719).
-- Las fotos iniciales (avatares ilustrados con iniciales y rol, sin imagen de personas reales) están en 19_avatares_personal.sql.

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS foto_url text;
ALTER TABLE usuario DROP CONSTRAINT IF EXISTS usuario_foto_url_check;
ALTER TABLE usuario ADD CONSTRAINT usuario_foto_url_check CHECK (
  foto_url IS NULL OR (length(foto_url) <= 400000 AND
    (foto_url ~ '^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+$' OR foto_url ~ '^https://[^\s"<>]+$')));
COMMENT ON COLUMN usuario.foto_url IS 'Foto de perfil del personal (data URL base64 o https). NULL en clientes.';

CREATE OR REPLACE FUNCTION public.api_login(p_email text, p_password_sha256 text)
 RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public', 'extensions'
AS $function$
DECLARE u usuario; v_rol text; c cliente;
BEGIN
  u := _bb_auth(p_email, p_password_sha256);
  IF u.id_usuario IS NULL THEN
    UPDATE usuario SET intentos_fallidos = least(intentos_fallidos + 1, 999)
     WHERE email = lower(trim(p_email)) AND estado = 'ACTIVO'
     RETURNING * INTO u;
    INSERT INTO registro_auditoria (id_usuario, accion, entidad, id_entidad)
    VALUES (u.id_usuario, 'LOGIN_FALLIDO', 'usuario', u.id_usuario::text);
    RETURN NULL;
  END IF;
  UPDATE usuario SET intentos_fallidos = 0, ultimo_acceso = now() WHERE id_usuario = u.id_usuario;
  v_rol := _bb_rol(u.id_usuario);
  SELECT * INTO c FROM cliente WHERE id_usuario = u.id_usuario;
  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad)
  VALUES (u.id_usuario, v_rol, 'LOGIN', 'usuario', u.id_usuario::text);
  RETURN jsonb_build_object(
    'id', u.id_usuario, 'full_name', trim(u.nombre || ' ' || coalesce(u.apellido, '')),
    'email', u.email, 'phone', coalesce(u.telefono, ''), 'rut', '',
    'role', v_rol, 'client_id', c.id_cliente, 'points', coalesce(c.puntos, 0),
    'must_change_password', u.debe_cambiar_clave,
    'photo_url', CASE WHEN v_rol <> 'CLIENTE' THEN coalesce(u.foto_url, '') ELSE '' END,
    'location_id', coalesce((SELECT ur.id_sucursal FROM usuario_rol ur WHERE ur.id_usuario = u.id_usuario AND ur.id_sucursal IS NOT NULL LIMIT 1),
                            (SELECT b.id_sucursal FROM barbero b WHERE b.id_usuario = u.id_usuario)),
    'barber_id', (SELECT b.id_barbero FROM barbero b WHERE b.id_usuario = u.id_usuario));
END $function$;

CREATE OR REPLACE FUNCTION public.api_barberos()
 RETURNS jsonb LANGUAGE sql STABLE SECURITY DEFINER SET search_path TO 'public'
AS $function$
  SELECT coalesce(jsonb_agg(jsonb_build_object(
    'id', b.id_barbero, 'location_id', b.id_sucursal, 'name', b.nombre_publico,
    'specialty', coalesce(b.especialidad, ''), 'bio', '',
    'avatar_url', coalesce((SELECT u.foto_url FROM usuario u WHERE u.id_usuario = b.id_usuario), ''),
    'available_hours', coalesce((SELECT string_agg(DISTINCT to_char(h.hora_inicio, 'HH24:MI'), ',' ORDER BY to_char(h.hora_inicio, 'HH24:MI'))
                                 FROM horario_barbero h WHERE h.id_barbero = b.id_barbero), ''),
    'rating', coalesce((SELECT round(avg(calificacion), 1) FROM resena WHERE id_barbero = b.id_barbero AND visible), 5.0),
    'reviews', (SELECT count(*) FROM resena WHERE id_barbero = b.id_barbero AND visible),
    'active', b.activo) ORDER BY b.id_barbero), '[]')
  FROM barbero b;
$function$;

CREATE OR REPLACE FUNCTION public.api_personal_listar(p_email text, p_password_sha256 text)
 RETURNS jsonb LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path TO 'public', 'extensions'
AS $function$
BEGIN
  PERFORM _bb_staff(p_email, p_password_sha256, ARRAY['DUENO']);
  RETURN coalesce((SELECT jsonb_agg(jsonb_build_object(
      'id', u.id_usuario, 'email', u.email, 'full_name', trim(u.nombre || ' ' || coalesce(u.apellido, '')),
      'phone', coalesce(u.telefono, ''), 'role', r.codigo, 'location_id', coalesce(ur.id_sucursal, b.id_sucursal),
      'estado', u.estado, 'must_change_password', u.debe_cambiar_clave,
      'ultimo_acceso', to_char(u.ultimo_acceso AT TIME ZONE 'America/Santiago', 'YYYY-MM-DD HH24:MI'),
      'barber_id', b.id_barbero, 'public_name', b.nombre_publico, 'specialty', b.especialidad,
      'photo_url', coalesce(u.foto_url, ''))
      ORDER BY r.codigo, u.nombre)
    FROM usuario u JOIN usuario_rol ur USING (id_usuario) JOIN rol r USING (id_rol)
    LEFT JOIN barbero b ON b.id_usuario = u.id_usuario
    WHERE r.codigo IN ('BARBERO', 'ADMIN_SUCURSAL') AND u.estado <> 'SUPRIMIDO'), '[]');
END $function$;

-- Cambia la foto de perfil de un funcionario. p_foto: data URL (png/jpeg/webp, base64) o https; '' la quita.
CREATE OR REPLACE FUNCTION public.api_personal_foto(p_email text, p_password_sha256 text, p_id_usuario bigint, p_foto text)
 RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public', 'extensions'
AS $function$
DECLARE u usuario; v_rol text; v_destino text; v_foto text := nullif(trim(coalesce(p_foto, '')), '');
BEGIN
  u := _bb_auth(p_email, p_password_sha256);
  IF u.id_usuario IS NULL THEN RAISE EXCEPTION 'CREDENCIALES_INVALIDAS'; END IF;
  v_rol := _bb_rol(u.id_usuario);
  v_destino := _bb_rol(p_id_usuario);
  IF v_destino IS NULL OR v_destino = 'CLIENTE' THEN RAISE EXCEPTION 'USUARIO_INVALIDO'; END IF;
  IF NOT (v_rol = 'DUENO' OR (v_rol IN ('ADMIN_SUCURSAL', 'BARBERO') AND p_id_usuario = u.id_usuario)) THEN
    RAISE EXCEPTION 'SIN_PERMISO';
  END IF;
  IF v_foto IS NOT NULL AND (length(v_foto) > 400000 OR NOT (
       v_foto ~ '^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+$' OR v_foto ~ '^https://[^\s"<>]+$')) THEN
    RAISE EXCEPTION 'FOTO_INVALIDA';
  END IF;
  UPDATE usuario SET foto_url = v_foto, actualizado_en = now() WHERE id_usuario = p_id_usuario;
  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad)
  VALUES (u.id_usuario, v_rol, 'ACTUALIZAR', 'usuario_foto', p_id_usuario::text);
  RETURN jsonb_build_object('id', p_id_usuario, 'photo_url', coalesce(v_foto, ''));
END $function$;

REVOKE ALL ON FUNCTION public.api_personal_foto(text, text, bigint, text) FROM public;
GRANT EXECUTE ON FUNCTION public.api_personal_foto(text, text, bigint, text) TO anon, authenticated;
