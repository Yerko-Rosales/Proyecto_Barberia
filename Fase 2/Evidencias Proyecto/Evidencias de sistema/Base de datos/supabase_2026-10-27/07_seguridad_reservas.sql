-- =====================================================================
-- Big Boys · S12 (27-10-2026) · Corrección de riesgos de seguridad
--  1. api_actualizar_reserva exige credenciales: el cliente solo cancela o
--     reagenda su propia reserva (el invitado indica el correo con que
--     reservó); los demás cambios de estado son del Dueño o del cajero/a
--     de la sucursal. Antes cualquiera con la clave pública podía cancelar
--     reservas ajenas o dejar una reserva «Confirmada» sin pagar.
--  2. api_crear_reserva: sin cuenta de gestión solo se crea la reserva
--     PENDIENTE_PAGO (la confirma Webpay); confirmar sin abono o registrar
--     una atención LOCAL exige al Dueño o al cajero/a. Ya no devuelve los
--     datos de contacto de una cuenta registrada a quien no la acredita.
--  3. api_listar_reservas: el barbero ya no ve teléfono ni correo de los
--     clientes; el cajero/a solo los de su sucursal.
--  4. Bloqueo por intentos fallidos: 5 intentos bloquean la cuenta 15 min
--     (antes la dejaban BLOQUEADA para siempre: cualquiera podía bloquear
--     la cuenta del Dueño escribiendo mal su clave 5 veces). Durante el
--     bloqueo ninguna función api_* acepta la contraseña.
-- Migración aplicada: s12_seguridad_reservas
-- =====================================================================

-- ---------------------------------------------------------------------
-- Helper: ¿el usuario gestiona la sucursal? (Dueño o cajero/a de ella)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION _bb_es_gestor(p_id_usuario bigint, p_sucursal smallint)
RETURNS boolean LANGUAGE sql STABLE SET search_path = public AS $$
  SELECT EXISTS (
    SELECT 1 FROM usuario_rol ur JOIN rol r USING (id_rol)
    WHERE ur.id_usuario = p_id_usuario
      AND (r.codigo = 'DUENO'
           OR (r.codigo = 'ADMIN_SUCURSAL' AND (ur.id_sucursal IS NULL OR ur.id_sucursal = p_sucursal))));
$$;

-- ---------------------------------------------------------------------
-- 4. Autenticación con bloqueo temporal
-- ---------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS ix_auditoria_usuario_accion
  ON registro_auditoria (id_usuario, accion, ocurrido_en DESC);

CREATE OR REPLACE FUNCTION _bb_auth(p_email text, p_password_sha256 text)
RETURNS usuario LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario;
BEGIN
  SELECT * INTO u FROM usuario WHERE email = lower(trim(p_email)) AND estado = 'ACTIVO';
  IF u.id_usuario IS NULL OR p_password_sha256 IS NULL THEN RETURN NULL; END IF;
  -- Bloqueo temporal: con 5 intentos fallidos se rechaza todo durante 15 min desde el último
  IF u.intentos_fallidos >= coalesce(_bb_cfg('MAX_INTENTOS_LOGIN')::int, 5) AND EXISTS (
       SELECT 1 FROM registro_auditoria
       WHERE id_usuario = u.id_usuario AND accion = 'LOGIN_FALLIDO'
         AND ocurrido_en > now() - make_interval(mins => coalesce(_bb_cfg('MINUTOS_BLOQUEO_LOGIN')::int, 15))) THEN
    RETURN NULL;
  END IF;
  IF u.password_hash <> crypt(lower(p_password_sha256), u.password_hash) THEN RETURN NULL; END IF;
  RETURN u;
END $$;

CREATE OR REPLACE FUNCTION api_login(p_email text, p_password_sha256 text)
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario; v_rol text; c cliente;
BEGIN
  u := _bb_auth(p_email, p_password_sha256);
  IF u.id_usuario IS NULL THEN
    -- Se cuenta el intento; el bloqueo es temporal (ver _bb_auth), la cuenta sigue ACTIVA
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
    -- Sucursal del personal (cajero/a o barbero); NULL para el Dueño y los clientes
    'location_id', coalesce((SELECT ur.id_sucursal FROM usuario_rol ur WHERE ur.id_usuario = u.id_usuario AND ur.id_sucursal IS NOT NULL LIMIT 1),
                            (SELECT b.id_sucursal FROM barbero b WHERE b.id_usuario = u.id_usuario)),
    'barber_id', (SELECT b.id_barbero FROM barbero b WHERE b.id_usuario = u.id_usuario));
END $$;

-- ---------------------------------------------------------------------
-- 2. Crear reserva
-- ---------------------------------------------------------------------
DROP FUNCTION IF EXISTS api_crear_reserva(jsonb);
CREATE FUNCTION api_crear_reserva(p jsonb, p_email text DEFAULT NULL, p_password_sha256 text DEFAULT NULL)
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE v_formato text := CASE WHEN upper(p->>'canal') = 'APP' THEN 'APP' ELSE 'WEB' END;
        v_email text := lower(trim(coalesce(p->>'customer_email', ''))); v_cli bigint; v_u bigint;
        v_suc smallint; v_bar int; v_srv int; v_inicio timestamptz; v_codigo text; r reserva;
        v_canal text := CASE WHEN upper(p->>'canal') IN ('WEB','APP','LOCAL') THEN upper(p->>'canal') ELSE 'WEB' END;
        v_estado text := _bb_estado(p->>'status'); u usuario; v_gestor boolean := false;
BEGIN
  r := _bb_buscar_reserva(jsonb_build_object('id', p->'id'));
  IF r.id_reserva IS NOT NULL THEN RETURN _bb_reserva_json(r, v_formato, false); END IF;
  PERFORM _bb_expirar_pendientes();

  SELECT id_barbero, id_sucursal INTO v_bar, v_suc FROM barbero WHERE id_barbero = (p->>'barber_id')::int AND activo;
  IF v_bar IS NULL THEN RAISE EXCEPTION 'BARBERO_INVALIDO'; END IF;

  IF p_email IS NOT NULL THEN
    u := _bb_auth(p_email, p_password_sha256);
    v_gestor := u.id_usuario IS NOT NULL AND _bb_es_gestor(u.id_usuario, v_suc);
  END IF;
  -- Sin cuenta de gestión solo se crea la reserva pendiente: la confirma el pago en Webpay
  IF NOT v_gestor AND (v_estado <> 'PENDIENTE_PAGO' OR v_canal = 'LOCAL') THEN
    RAISE EXCEPTION 'PAGO_REQUERIDO';
  END IF;

  v_srv := _bb_servicio((p->>'service_id')::int, p->>'service_name');
  v_inicio := _bb_parse_fecha(p->>'date_time');

  IF v_email <> '' THEN
    SELECT c.id_cliente, u2.id_usuario INTO v_cli, v_u FROM usuario u2 JOIN cliente c ON c.id_usuario = u2.id_usuario
    WHERE u2.email = v_email AND u2.estado = 'ACTIVO';
  END IF;
  IF v_cli IS NULL THEN
    INSERT INTO cliente (es_invitado, nombre_contacto, telefono_contacto, email_contacto, ultima_actividad)
    VALUES (true, left(coalesce(nullif(trim(p->>'customer_name'), ''), 'Invitado'), 80),
            left(nullif(trim(p->>'customer_phone'), ''), 20), nullif(v_email, ''), now())
    RETURNING id_cliente INTO v_cli;
  ELSE
    UPDATE cliente SET ultima_actividad = now() WHERE id_cliente = v_cli;
  END IF;

  v_codigo := CASE WHEN (p->>'id') ~ '^\d{10,15}$' THEN CASE WHEN v_formato = 'APP' THEN 'A' ELSE 'W' END || _bb_b36((p->>'id')::bigint) END;
  BEGIN
    INSERT INTO reserva (codigo, id_cliente, id_sucursal, id_barbero, id_servicio, inicio, fin, estado, monto_abono, canal, expira_en)
    VALUES (coalesce(v_codigo, 'R' || to_char(now(), 'YYMMDDHH24MISS')), v_cli, v_suc, v_bar, v_srv, v_inicio,
            v_inicio + make_interval(mins => (SELECT duracion_min FROM servicio WHERE id_servicio = v_srv)),
            CASE WHEN v_estado = 'PENDIENTE_PAGO' THEN 'PENDIENTE_PAGO'
                 WHEN v_canal = 'LOCAL' AND v_estado = 'ATENDIDA' THEN 'ATENDIDA'
                 ELSE 'CONFIRMADA' END,
            CASE WHEN v_canal = 'LOCAL' THEN 0 ELSE coalesce(_bb_cfg('ABONO_RESERVA')::numeric, 3000) END, v_canal,
            CASE WHEN v_estado = 'PENDIENTE_PAGO'
                 THEN now() + make_interval(mins => coalesce(_bb_cfg('MINUTOS_EXPIRA_PAGO')::int, 15)) END)
    RETURNING * INTO r;
  EXCEPTION WHEN exclusion_violation THEN
    RAISE EXCEPTION 'HORARIO_OCUPADO';
  END;
  IF v_email <> '' AND r.estado = 'CONFIRMADA' THEN
    INSERT INTO notificacion (id_cliente, id_reserva, canal, tipo, asunto)
    VALUES (v_cli, r.id_reserva, 'EMAIL', 'CONFIRMACION', 'Confirmación de reserva ' || r.codigo);
  END IF;
  IF v_gestor THEN
    INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad)
    VALUES (u.id_usuario, _bb_rol(u.id_usuario), 'CREAR', 'reserva', r.codigo);
  END IF;
  -- Los datos de contacto se devuelven al personal o al invitado que los acaba de escribir,
  -- nunca los de una cuenta registrada a quien no se identificó
  RETURN _bb_reserva_json(r, v_formato, v_gestor OR v_u IS NULL);
END $$;

-- ---------------------------------------------------------------------
-- 1. Actualizar reserva (cancelar, reagendar, cambiar estado)
-- ---------------------------------------------------------------------
DROP FUNCTION IF EXISTS api_actualizar_reserva(jsonb);
CREATE FUNCTION api_actualizar_reserva(p jsonb, p_email text DEFAULT NULL, p_password_sha256 text DEFAULT NULL)
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE v_formato text := CASE WHEN upper(p->>'canal') = 'APP' THEN 'APP' ELSE 'WEB' END;
        r reserva; c cliente; u usuario; v_estado text; v_nuevo timestamptz; v_dur int; v_por text;
        v_gestor boolean := false; v_titular boolean := false; v_antes text; v_reag boolean := false;
BEGIN
  r := _bb_buscar_reserva(p);
  IF r.id_reserva IS NULL THEN RAISE EXCEPTION 'RESERVA_NO_ENCONTRADA'; END IF;
  SELECT * INTO c FROM cliente WHERE id_cliente = r.id_cliente;
  v_antes := r.estado;

  IF p_email IS NOT NULL THEN
    u := _bb_auth(p_email, p_password_sha256);
    IF u.id_usuario IS NOT NULL THEN
      v_gestor := _bb_es_gestor(u.id_usuario, r.id_sucursal);
      v_titular := coalesce(c.id_usuario = u.id_usuario, false);
    END IF;
  END IF;
  -- El invitado se identifica con el correo con que reservó
  IF NOT v_gestor AND NOT v_titular AND c.id_usuario IS NULL AND coalesce(c.email_contacto, '') <> ''
     AND lower(c.email_contacto) = lower(trim(coalesce(p->>'customer_email', ''))) THEN
    v_titular := true;
  END IF;
  IF NOT v_gestor AND NOT v_titular THEN RAISE EXCEPTION 'SIN_PERMISO'; END IF;

  IF coalesce(p->>'new_date_time', '') <> '' THEN
    v_nuevo := _bb_parse_fecha(p->>'new_date_time');
    IF v_nuevo <> r.inicio THEN
      IF NOT v_gestor AND r.estado <> 'CONFIRMADA' THEN RAISE EXCEPTION 'SIN_PERMISO'; END IF;
      IF r.inicio - now() < interval '24 hours' THEN RAISE EXCEPTION 'REAGENDAR_MENOS_DE_24H'; END IF;
      IF r.cantidad_reagendamientos >= coalesce(_bb_cfg('MAX_REAGENDAMIENTOS')::int, 2) THEN RAISE EXCEPTION 'MAXIMO_REAGENDAMIENTOS'; END IF;
      v_dur := extract(epoch FROM r.fin - r.inicio) / 60;
      INSERT INTO reagendamiento (id_reserva, inicio_anterior, inicio_nuevo, horas_anticipacion)
      VALUES (r.id_reserva, r.inicio, v_nuevo, round(extract(epoch FROM r.inicio - now()) / 3600.0, 1));
      BEGIN
        UPDATE reserva SET inicio = v_nuevo, fin = v_nuevo + make_interval(mins => v_dur),
               cantidad_reagendamientos = cantidad_reagendamientos + 1, actualizado_en = now()
         WHERE id_reserva = r.id_reserva RETURNING * INTO r;
      EXCEPTION WHEN exclusion_violation THEN RAISE EXCEPTION 'HORARIO_OCUPADO';
      END;
      v_reag := true;
    END IF;
  END IF;

  IF coalesce(p->>'status', '') <> '' THEN
    v_estado := _bb_estado(p->>'status');
    IF v_estado <> r.estado THEN
      -- El cliente solo puede cancelar (o dar por caducada su reserva sin pagar)
      IF NOT v_gestor AND NOT (v_estado = 'CANCELADA' AND r.estado IN ('CONFIRMADA', 'PENDIENTE_PAGO'))
                      AND NOT (v_estado = 'EXPIRADA' AND r.estado = 'PENDIENTE_PAGO') THEN
        RAISE EXCEPTION 'SIN_PERMISO';
      END IF;
      v_por := nullif(trim(p->>'cancelled_by'), '');
      v_por := CASE WHEN v_gestor THEN coalesce(v_por, 'Administración')
                    WHEN v_por ILIKE 'cliente%' THEN v_por ELSE 'Cliente' END;
      UPDATE reserva SET estado = v_estado, actualizado_en = now(),
             cancelada_en = CASE WHEN v_estado = 'CANCELADA' THEN now() ELSE cancelada_en END,
             motivo_cancelacion = CASE WHEN v_estado = 'CANCELADA' THEN left(
                 '[' || v_por || '] ' || coalesce(p->>'cancellation_reason', ''), 120)
               ELSE motivo_cancelacion END
       WHERE id_reserva = r.id_reserva RETURNING * INTO r;
      IF v_estado = 'CANCELADA' THEN
        INSERT INTO notificacion (id_cliente, id_reserva, canal, tipo, asunto)
        SELECT r.id_cliente, r.id_reserva, 'EMAIL', 'CANCELACION', 'Cancelación de reserva ' || r.codigo
        FROM cliente WHERE id_cliente = r.id_cliente AND email_contacto IS NOT NULL;
      END IF;
    END IF;
  END IF;

  IF r.estado <> v_antes OR v_reag THEN
    INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad, detalle)
    VALUES (u.id_usuario, CASE WHEN u.id_usuario IS NOT NULL THEN _bb_rol(u.id_usuario) END, 'ACTUALIZAR', 'reserva', r.codigo,
            jsonb_build_object('estado_anterior', v_antes, 'estado', r.estado, 'reagendada', v_reag,
                               'por', CASE WHEN v_gestor THEN 'GESTOR' ELSE 'TITULAR' END));
  END IF;
  RETURN _bb_reserva_json(r, v_formato, true);
END $$;

-- ---------------------------------------------------------------------
-- 3. Listar reservas: datos de contacto solo al titular y a quien gestiona la sucursal
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION api_listar_reservas(p_formato text DEFAULT 'APP', p_email text DEFAULT NULL, p_password_sha256 text DEFAULT NULL)
RETURNS jsonb LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario; v_cli bigint; v_dueno boolean := false; v_sucs smallint[] := '{}';
BEGIN
  IF p_email IS NOT NULL THEN
    u := _bb_auth(p_email, p_password_sha256);
    IF u.id_usuario IS NOT NULL THEN
      SELECT bool_or(r.codigo = 'DUENO'),
             coalesce(array_agg(ur.id_sucursal) FILTER (WHERE r.codigo = 'ADMIN_SUCURSAL' AND ur.id_sucursal IS NOT NULL), '{}')
        INTO v_dueno, v_sucs
        FROM usuario_rol ur JOIN rol r USING (id_rol) WHERE ur.id_usuario = u.id_usuario;
      SELECT id_cliente INTO v_cli FROM cliente WHERE id_usuario = u.id_usuario;
    END IF;
  END IF;
  RETURN coalesce((SELECT jsonb_agg(_bb_reserva_json(r, upper(p_formato),
                                    coalesce(v_dueno, false) OR r.id_sucursal = ANY (v_sucs) OR r.id_cliente = v_cli)
                                    ORDER BY r.inicio DESC)
                   FROM (SELECT * FROM reserva WHERE inicio > now() - interval '90 days' ORDER BY inicio DESC LIMIT 500) r), '[]');
END $$;

-- ---------------------------------------------------------------------
-- Permisos: los helpers no se exponen; las api_* sí
-- ---------------------------------------------------------------------
REVOKE ALL ON FUNCTION _bb_es_gestor(bigint, smallint) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION _bb_auth(text, text) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION api_crear_reserva(jsonb, text, text) FROM PUBLIC;
REVOKE ALL ON FUNCTION api_actualizar_reserva(jsonb, text, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api_crear_reserva(jsonb, text, text) TO anon, authenticated;
GRANT EXECUTE ON FUNCTION api_actualizar_reserva(jsonb, text, text) TO anon, authenticated;

-- Parámetros del bloqueo (migración s12_config_bloqueo_login)
INSERT INTO configuracion (clave, ambito, tipo_valor, valor)
SELECT x.clave, 'GLOBAL', 'ENTERO', x.valor
FROM (VALUES ('MAX_INTENTOS_LOGIN', '5'), ('MINUTOS_BLOQUEO_LOGIN', '15')) x(clave, valor)
WHERE NOT EXISTS (SELECT 1 FROM configuracion c WHERE c.clave = x.clave AND c.ambito = 'GLOBAL');
