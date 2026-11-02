-- =====================================================================
-- Proyecto Barbería Big-Boys · Migración del 22-10-2026
-- 1) Tabla 27: resena (calificación de barberos, RF-09).
-- 2) Coordenadas de las sucursales (mapa de la web y la app).
-- 3) Datos de prueba: dueño, 4 barberos y 1 cliente (marcados como demo).
-- 4) Capa de acceso (funciones api_*): la web y la app usan solo estas
--    funciones con la clave anon; las 27 tablas siguen con RLS cerrado.
-- Proyecto Supabase: «Big Boys Project» (dgrflhgotxljjiejckoj).
-- =====================================================================

SET search_path = public, extensions;

-- ---------------------------------------------------------------------
-- 1. Tabla resena
-- ---------------------------------------------------------------------
-- Calificación (1 a 5) que un cliente registrado deja a un barbero o a una sucursal.
CREATE TABLE IF NOT EXISTS resena (
    id_resena BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES cliente(id_cliente),
    id_sucursal SMALLINT NOT NULL REFERENCES sucursal(id_sucursal),
    id_barbero INTEGER REFERENCES barbero(id_barbero),
    id_atencion BIGINT UNIQUE REFERENCES atencion(id_atencion),
    calificacion SMALLINT NOT NULL CHECK (calificacion BETWEEN 1 AND 5),
    comentario VARCHAR(500),
    visible BOOLEAN NOT NULL DEFAULT true,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_resena_barbero ON resena (id_barbero) WHERE visible;
ALTER TABLE resena ENABLE ROW LEVEL SECURITY;

-- ---------------------------------------------------------------------
-- 2. Coordenadas de las sucursales (aproximadas a nivel de calle, OpenStreetMap)
-- ---------------------------------------------------------------------
ALTER TABLE sucursal ADD COLUMN IF NOT EXISTS latitud NUMERIC(9,6);
ALTER TABLE sucursal ADD COLUMN IF NOT EXISTS longitud NUMERIC(9,6);
UPDATE sucursal SET latitud = -33.605234, longitud = -70.890280 WHERE codigo = 'SUC-01';
UPDATE sucursal SET latitud = -33.607670, longitud = -70.900144 WHERE codigo = 'SUC-02';
UPDATE sucursal SET latitud = -33.663229, longitud = -70.928852 WHERE codigo = 'SUC-03';

-- ---------------------------------------------------------------------
-- 3. Datos de prueba (demo). Se reemplazan por la nómina real del dueño.
-- ---------------------------------------------------------------------
DO $$
DECLARE v_dueno BIGINT; v_u BIGINT; v_cli BIGINT; r RECORD;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM usuario WHERE email = 'admin@bigboys.cl') THEN
    INSERT INTO usuario (email, password_hash, nombre, apellido, debe_cambiar_clave)
    VALUES ('admin@bigboys.cl', crypt(gen_random_uuid()::text, gen_salt('bf', 10)), 'Administración', '(demo)', true)
    RETURNING id_usuario INTO v_dueno;
    INSERT INTO usuario_rol (id_usuario, id_rol) SELECT v_dueno, id_rol FROM rol WHERE codigo = 'DUENO';
  ELSE
    SELECT id_usuario INTO v_dueno FROM usuario WHERE email = 'admin@bigboys.cl';
  END IF;

  FOR r IN SELECT * FROM (VALUES
      (1, 'mateo@bigboys.cl',    'Mateo',    'Silva',   1::smallint, 'Mateo "Fade Master" Silva',    'Degradados de precisión, low y mid fade', ARRAY[9,10,11,12,14,15,16,17,18]),
      (2, 'esteban@bigboys.cl',  'Esteban',  'Rojas',   1::smallint, 'Esteban "El Maestro" Rojas',   'Corte clásico, barba spa y toalla caliente', ARRAY[10,11,12,13,15,16,17,18,19]),
      (3, 'cristian@bigboys.cl', 'Cristian', 'Castro',  2::smallint, 'Cristian "Black Gold" Castro', 'Freestyle, tintura y perfilado con cera', ARRAY[9,10,11,12,14,15,16,17,18]),
      (4, 'diego@bigboys.cl',    'Diego',    'Morales', 3::smallint, 'Diego "La Navaja" Morales',    'Afeitado tradicional y barboterapia', ARRAY[10,11,12,14,15,16,17,18])
    ) AS t(id, email, nombre, apellido, suc, publico, esp, horas)
  LOOP
    IF NOT EXISTS (SELECT 1 FROM barbero WHERE id_barbero = r.id) THEN
      INSERT INTO usuario (email, password_hash, nombre, apellido, creado_por, debe_cambiar_clave)
      VALUES (r.email, crypt(gen_random_uuid()::text, gen_salt('bf', 10)), r.nombre, r.apellido || ' (demo)', v_dueno, true)
      RETURNING id_usuario INTO v_u;
      INSERT INTO usuario_rol (id_usuario, id_rol, id_sucursal) SELECT v_u, id_rol, r.suc FROM rol WHERE codigo = 'BARBERO';
      INSERT INTO barbero (id_barbero, id_usuario, id_sucursal, nombre_publico, especialidad)
      VALUES (r.id, v_u, r.suc, r.publico, r.esp);
      -- Lunes a sábado: un bloque de una hora por cada hora del prototipo
      INSERT INTO horario_barbero (id_barbero, dia_semana, hora_inicio, hora_fin)
      SELECT r.id, d, make_time(h, 0, 0), make_time(h + 1, 0, 0)
      FROM generate_series(1, 6) d, unnest(r.horas) h;
    END IF;
  END LOOP;
  PERFORM setval(pg_get_serial_sequence('barbero', 'id_barbero'), GREATEST((SELECT max(id_barbero) FROM barbero), 1));

  -- Cliente de prueba (contraseña «123»; la app y la web envían su SHA-256)
  IF NOT EXISTS (SELECT 1 FROM usuario WHERE email = 'test@bigboys.cl') THEN
    INSERT INTO usuario (email, password_hash, nombre, apellido, telefono)
    VALUES ('test@bigboys.cl', crypt(encode(digest('123', 'sha256'), 'hex'), gen_salt('bf', 10)), 'Usuario de Prueba', 'Big Boys', '+56 9 8765 4321')
    RETURNING id_usuario INTO v_u;
    INSERT INTO usuario_rol (id_usuario, id_rol) SELECT v_u, id_rol FROM rol WHERE codigo = 'CLIENTE';
    INSERT INTO cliente (id_usuario, nombre_contacto, telefono_contacto, email_contacto)
    VALUES (v_u, 'Usuario de Prueba Big Boys', '+56 9 8765 4321', 'test@bigboys.cl') RETURNING id_cliente INTO v_cli;
    INSERT INTO consentimiento (id_cliente, id_politica, finalidad, otorgado, canal)
    SELECT v_cli, id_politica, 'SERVICIO', true, 'WEB' FROM politica WHERE tipo = 'PRIVACIDAD' AND vigente;
  END IF;
END $$;

-- ---------------------------------------------------------------------
-- 4. Capa de acceso
-- ---------------------------------------------------------------------
-- Funciones internas (no expuestas a la clave anon)
CREATE OR REPLACE FUNCTION _bb_cfg(p_clave text) RETURNS text
LANGUAGE sql STABLE SET search_path = public AS $$
  SELECT valor FROM configuracion
  WHERE clave = p_clave AND ambito = 'GLOBAL' AND vigente_desde <= now()
    AND (vigente_hasta IS NULL OR vigente_hasta > now())
  ORDER BY vigente_desde DESC LIMIT 1;
$$;

-- Valida credenciales; la contraseña llega como SHA-256 (hex) y se guarda con bcrypt.
CREATE OR REPLACE FUNCTION _bb_auth(p_email text, p_password_sha256 text) RETURNS usuario
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario;
BEGIN
  SELECT * INTO u FROM usuario WHERE email = lower(trim(p_email)) AND estado = 'ACTIVO';
  IF u.id_usuario IS NULL OR p_password_sha256 IS NULL
     OR u.password_hash <> crypt(lower(p_password_sha256), u.password_hash) THEN
    RETURN NULL;
  END IF;
  RETURN u;
END $$;

CREATE OR REPLACE FUNCTION _bb_rol(p_id_usuario bigint) RETURNS text
LANGUAGE sql STABLE SET search_path = public AS $$
  SELECT r.codigo FROM usuario_rol ur JOIN rol r USING (id_rol)
  WHERE ur.id_usuario = p_id_usuario
  ORDER BY CASE r.codigo WHEN 'DUENO' THEN 1 WHEN 'ADMIN_SUCURSAL' THEN 2 WHEN 'BARBERO' THEN 3 ELSE 4 END
  LIMIT 1;
$$;

-- «Juan Pérez» -> «Juan P.» (minimización al mostrar reservas de terceros)
CREATE OR REPLACE FUNCTION _bb_nombre_corto(p text) RETURNS text
LANGUAGE sql IMMUTABLE AS $$
  SELECT CASE WHEN split_part(trim(coalesce(p, '')), ' ', 2) = '' THEN split_part(trim(coalesce(p, 'Cliente')), ' ', 1)
              ELSE split_part(trim(p), ' ', 1) || ' ' || upper(left(split_part(trim(p), ' ', 2), 1)) || '.' END;
$$;

-- Fecha de la web («DD-MM-YYYY HH:MI») o de la app («YYYY-MM-DD HH:MI»), hora de Chile
CREATE OR REPLACE FUNCTION _bb_parse_fecha(p text) RETURNS timestamptz
LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE s text := substring(trim(coalesce(p, '')) from '^\d{1,4}-\d{1,2}-\d{1,4}\s+\d{1,2}:\d{2}');
BEGIN
  IF s IS NULL THEN RAISE EXCEPTION 'FECHA_INVALIDA: %', p; END IF;
  IF s ~ '^\d{4}-' THEN
    RETURN to_timestamp(s, 'YYYY-MM-DD HH24:MI')::timestamp AT TIME ZONE 'America/Santiago';
  END IF;
  RETURN to_timestamp(s, 'DD-MM-YYYY HH24:MI')::timestamp AT TIME ZONE 'America/Santiago';
END $$;

CREATE OR REPLACE FUNCTION _bb_estado(p text) RETURNS text
LANGUAGE sql IMMUTABLE AS $$
  SELECT CASE
    WHEN p ILIKE 'cancelad%' THEN 'CANCELADA'
    WHEN p ILIKE 'no-show%' OR p ILIKE 'no show%' THEN 'NO_SHOW'
    WHEN p ILIKE 'completad%' OR p ILIKE 'atendid%' THEN 'ATENDIDA'
    WHEN p ILIKE 'caducad%' OR p ILIKE 'expirad%' THEN 'EXPIRADA'
    WHEN p ILIKE 'pendiente%' THEN 'PENDIENTE_PAGO'
    ELSE 'CONFIRMADA' END;
$$;

-- Servicio por nombre exacto o por palabras clave (los catálogos antiguos usan otros nombres)
CREATE OR REPLACE FUNCTION _bb_servicio(p_id int, p_nombre text) RETURNS int
LANGUAGE plpgsql STABLE SET search_path = public AS $$
DECLARE n text := lower(coalesce(p_nombre, '')); v int;
BEGIN
  SELECT id_servicio INTO v FROM servicio WHERE id_servicio = p_id AND activo;
  IF v IS NOT NULL THEN RETURN v; END IF;
  SELECT id_servicio INTO v FROM servicio WHERE lower(nombre) = n AND activo;
  IF v IS NOT NULL THEN RETURN v; END IF;
  SELECT id_servicio INTO v FROM servicio WHERE activo AND nombre = CASE
    WHEN n ~ 'complet|vip' THEN 'S. Completo'
    WHEN n ~ 'ceja' AND n ~ 'lavado' THEN 'Corte + Cejas + Lavado'
    WHEN n ~ 'barba' AND n ~ 'corte|pelo|combo' THEN 'Corte + Barba'
    WHEN n ~ 'ceja' AND n ~ 'corte|pelo' THEN 'Corte + Cejas'
    WHEN n ~ 'barba' THEN 'Barba'
    ELSE 'Corte' END;
  RETURN v;
END $$;

CREATE OR REPLACE FUNCTION _bb_precio(p_servicio int, p_sucursal smallint) RETURNS numeric
LANGUAGE sql STABLE SET search_path = public AS $$
  SELECT coalesce((SELECT precio FROM servicio_sucursal WHERE id_servicio = p_servicio AND id_sucursal = p_sucursal AND activo),
                  (SELECT precio_base FROM servicio WHERE id_servicio = p_servicio));
$$;

-- Código de reserva: la web y la app generan su id con la hora en milisegundos;
-- se guarda en base 36 («W»/«A» + 8 caracteres) para respetar VARCHAR(12).
CREATE OR REPLACE FUNCTION _bb_b36(n bigint) RETURNS text
LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE d text := '0123456789abcdefghijklmnopqrstuvwxyz'; s text := '';
BEGIN
  IF n <= 0 THEN RETURN '0'; END IF;
  WHILE n > 0 LOOP s := substr(d, (n % 36)::int + 1, 1) || s; n := n / 36; END LOOP;
  RETURN s;
END $$;

CREATE OR REPLACE FUNCTION _bb_id_cliente_app(p_codigo text, p_id_reserva bigint) RETURNS bigint
LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE d text := '0123456789abcdefghijklmnopqrstuvwxyz'; n bigint := 0; c text;
BEGIN
  IF p_codigo !~ '^[WA][0-9a-z]+$' THEN RETURN p_id_reserva; END IF;
  FOREACH c IN ARRAY regexp_split_to_array(substr(p_codigo, 2), '') LOOP
    n := n * 36 + (strpos(d, c) - 1);
  END LOOP;
  RETURN n;
END $$;

-- Una reserva con la forma que ya usan la web y la app (fecha «YYYY-MM-DD HH:MI»;
-- el formato WEB/APP solo cambia el texto de algunos estados)
CREATE OR REPLACE FUNCTION _bb_reserva_json(r reserva, p_formato text, p_ver_datos boolean) RETURNS jsonb
LANGUAGE plpgsql STABLE SET search_path = public AS $$
DECLARE c cliente; s servicio; v_por text; v_motivo text; v_retenido boolean; v_estado text;
BEGIN
  SELECT * INTO c FROM cliente WHERE id_cliente = r.id_cliente;
  SELECT * INTO s FROM servicio WHERE id_servicio = r.id_servicio;
  v_por := substring(r.motivo_cancelacion from '^\[([^\]]+)\]');
  v_motivo := trim(regexp_replace(coalesce(r.motivo_cancelacion, ''), '^\[[^\]]+\]\s*', ''));
  -- Regla del abono: no-show o anulación del cliente con menos de 24 h pierde el abono
  v_retenido := r.estado = 'NO_SHOW'
             OR (r.estado = 'CANCELADA' AND coalesce(v_por, 'Cliente') ILIKE 'cliente%'
                 AND r.cancelada_en > r.inicio - interval '24 hours');
  v_estado := CASE r.estado
    WHEN 'CONFIRMADA' THEN 'Confirmada'
    WHEN 'PENDIENTE_PAGO' THEN 'Pendiente'
    WHEN 'ATENDIDA' THEN 'Completada'
    WHEN 'EXPIRADA' THEN 'Caducada'
    WHEN 'NO_SHOW' THEN CASE WHEN p_formato = 'WEB' THEN 'No-Show (Inasistencia)' ELSE 'No-Show' END
    ELSE CASE WHEN NOT v_retenido THEN 'Cancelada'
              WHEN p_formato = 'WEB' THEN 'Cancelada (Pérdida de Abono)'
              ELSE 'Cancelada (Abono Retenido)' END END;
  RETURN jsonb_build_object(
    'id', _bb_id_cliente_app(r.codigo, r.id_reserva),
    'codigo', r.codigo,
    'user_id', c.id_usuario,
    'barber_id', r.id_barbero,
    'location_id', r.id_sucursal,
    'service_id', r.id_servicio,
    'service_name', s.nombre,
    'service_price', _bb_precio(r.id_servicio, r.id_sucursal),
    'prepay_amount', r.monto_abono,
    'date_time', to_char(r.inicio AT TIME ZONE 'America/Santiago', 'YYYY-MM-DD HH24:MI'),
    'status', v_estado,
    'customer_name', CASE WHEN p_ver_datos THEN c.nombre_contacto ELSE _bb_nombre_corto(c.nombre_contacto) END,
    'customer_phone', CASE WHEN p_ver_datos THEN coalesce(c.telefono_contacto, '') ELSE '' END,
    'customer_email', CASE WHEN p_ver_datos THEN coalesce(c.email_contacto, '') ELSE '' END,
    'is_guest', c.es_invitado,
    'reschedule_count', r.cantidad_reagendamientos,
    'prepay_forfeited', v_retenido,
    'cancelled_by', v_por,
    'cancellation_reason', nullif(v_motivo, ''),
    'created_at', r.creado_en);
END $$;

CREATE OR REPLACE FUNCTION _bb_buscar_reserva(p jsonb) RETURNS reserva
LANGUAGE plpgsql STABLE SET search_path = public AS $$
DECLARE r reserva; v_id bigint; v_inicio timestamptz;
BEGIN
  -- 1) código exacto; 2) id en milisegundos generado por la web o la app;
  -- 3) barbero + fecha (copias locales); 4) id interno, solo si coincide el barbero
  IF p ? 'codigo' THEN SELECT * INTO r FROM reserva WHERE codigo = p->>'codigo'; END IF;
  IF (p->>'id') ~ '^\d+$' THEN v_id := (p->>'id')::bigint; END IF;
  IF r.id_reserva IS NULL AND v_id >= 1000000000 THEN
    SELECT * INTO r FROM reserva WHERE codigo IN ('W' || _bb_b36(v_id), 'A' || _bb_b36(v_id)) LIMIT 1;
  END IF;
  IF r.id_reserva IS NULL AND coalesce(p->>'date_time', '') <> '' AND (p->>'barber_id') ~ '^\d+$' THEN
    v_inicio := _bb_parse_fecha(p->>'date_time');
    SELECT * INTO r FROM reserva WHERE id_barbero = (p->>'barber_id')::int AND inicio = v_inicio
    ORDER BY (estado IN ('PENDIENTE_PAGO','CONFIRMADA')) DESC, creado_en DESC LIMIT 1;
  END IF;
  IF r.id_reserva IS NULL AND v_id IS NOT NULL AND (p->>'barber_id') ~ '^\d+$' THEN
    SELECT * INTO r FROM reserva WHERE id_reserva = v_id AND id_barbero = (p->>'barber_id')::int;
  END IF;
  RETURN r;
END $$;

-- ---- Funciones públicas (clave anon) --------------------------------

CREATE OR REPLACE FUNCTION api_sucursales() RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT coalesce(jsonb_agg(jsonb_build_object(
    'id', id_sucursal, 'code', codigo, 'name', nombre,
    'address', direccion || ', ' || comuna, 'phone', coalesce(telefono, ''),
    'lat', latitud, 'lng', longitud, 'is_primary', es_casa_matriz) ORDER BY id_sucursal), '[]')
  FROM sucursal WHERE activa;
$$;

CREATE OR REPLACE FUNCTION api_servicios(p_sucursal smallint DEFAULT NULL) RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT coalesce(jsonb_agg(jsonb_build_object(
    'id', s.id_servicio, 'name', s.nombre, 'category', s.categoria,
    'bookable', s.reservable_individual, 'duration_min', s.duracion_min,
    'price', CASE WHEN p_sucursal IS NULL THEN s.precio_base ELSE _bb_precio(s.id_servicio, p_sucursal) END)
    ORDER BY s.id_servicio), '[]')
  FROM servicio s WHERE s.activo;
$$;

CREATE OR REPLACE FUNCTION api_barberos() RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT coalesce(jsonb_agg(jsonb_build_object(
    'id', b.id_barbero, 'location_id', b.id_sucursal, 'name', b.nombre_publico,
    'specialty', coalesce(b.especialidad, ''), 'bio', '', 'avatar_url', '',
    'available_hours', coalesce((SELECT string_agg(DISTINCT to_char(h.hora_inicio, 'HH24:MI'), ',' ORDER BY to_char(h.hora_inicio, 'HH24:MI'))
                                 FROM horario_barbero h WHERE h.id_barbero = b.id_barbero), ''),
    'rating', coalesce((SELECT round(avg(calificacion), 1) FROM resena WHERE id_barbero = b.id_barbero AND visible), 5.0),
    'reviews', (SELECT count(*) FROM resena WHERE id_barbero = b.id_barbero AND visible),
    'active', b.activo) ORDER BY b.id_barbero), '[]')
  FROM barbero b;
$$;

CREATE OR REPLACE FUNCTION api_usuario_existe(p_email text) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (SELECT 1 FROM usuario WHERE email = lower(trim(p_email)) AND estado <> 'SUPRIMIDO');
$$;

CREATE OR REPLACE FUNCTION api_login(p_email text, p_password_sha256 text) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario; v_rol text; c cliente;
BEGIN
  u := _bb_auth(p_email, p_password_sha256);
  IF u.id_usuario IS NULL THEN
    UPDATE usuario SET intentos_fallidos = intentos_fallidos + 1,
           estado = CASE WHEN intentos_fallidos + 1 >= 5 THEN 'BLOQUEADO' ELSE estado END
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
    'must_change_password', u.debe_cambiar_clave);
END $$;

CREATE OR REPLACE FUNCTION api_registrar_cliente(p_email text, p_nombre_completo text, p_telefono text,
                                                 p_password_sha256 text, p_canal text DEFAULT 'WEB') RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE v_email text := lower(trim(p_email)); v_nombre text := trim(coalesce(p_nombre_completo, ''));
        v_u bigint; v_c bigint; v_canal text := CASE WHEN upper(p_canal) IN ('WEB','APP','LOCAL') THEN upper(p_canal) ELSE 'WEB' END;
BEGIN
  IF v_email !~ '^[^@\s]+@[^@\s]+\.[^@\s]+$' THEN RAISE EXCEPTION 'CORREO_INVALIDO'; END IF;
  IF v_nombre = '' THEN RAISE EXCEPTION 'NOMBRE_REQUERIDO'; END IF;
  IF coalesce(p_password_sha256, '') !~* '^[0-9a-f]{64}$' THEN RAISE EXCEPTION 'CLAVE_INVALIDA'; END IF;
  IF EXISTS (SELECT 1 FROM usuario WHERE email = v_email) THEN RAISE EXCEPTION 'CORREO_YA_REGISTRADO'; END IF;

  INSERT INTO usuario (email, password_hash, nombre, apellido, telefono)
  VALUES (v_email, crypt(lower(p_password_sha256), gen_salt('bf', 10)),
          split_part(v_nombre, ' ', 1), nullif(trim(substr(v_nombre, length(split_part(v_nombre, ' ', 1)) + 1)), ''),
          nullif(trim(p_telefono), ''))
  RETURNING id_usuario INTO v_u;
  INSERT INTO usuario_rol (id_usuario, id_rol) SELECT v_u, id_rol FROM rol WHERE codigo = 'CLIENTE';
  -- Si ya reservó como invitado con el mismo correo, se vincula ese registro
  SELECT id_cliente INTO v_c FROM cliente WHERE es_invitado AND id_usuario IS NULL AND lower(email_contacto) = v_email
  ORDER BY creado_en DESC LIMIT 1;
  IF v_c IS NOT NULL THEN
    UPDATE cliente SET id_usuario = v_u, es_invitado = false, nombre_contacto = v_nombre,
           telefono_contacto = coalesce(nullif(trim(p_telefono), ''), telefono_contacto), ultima_actividad = now()
     WHERE id_cliente = v_c;
  ELSE
    INSERT INTO cliente (id_usuario, nombre_contacto, telefono_contacto, email_contacto, ultima_actividad)
    VALUES (v_u, v_nombre, nullif(trim(p_telefono), ''), v_email, now()) RETURNING id_cliente INTO v_c;
  END IF;
  INSERT INTO consentimiento (id_cliente, id_politica, finalidad, otorgado, canal)
  SELECT v_c, id_politica, 'SERVICIO', true, v_canal FROM politica WHERE tipo = 'PRIVACIDAD' AND vigente;
  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad)
  VALUES (v_u, 'CLIENTE', 'CREAR', 'usuario', v_u::text);
  RETURN api_login(v_email, p_password_sha256);
END $$;

CREATE OR REPLACE FUNCTION api_actualizar_perfil(p_email text, p_password_sha256 text, p_nombre_completo text DEFAULT NULL,
                                                 p_telefono text DEFAULT NULL, p_nueva_password_sha256 text DEFAULT NULL) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario; v_nombre text := trim(coalesce(p_nombre_completo, ''));
BEGIN
  u := _bb_auth(p_email, p_password_sha256);
  IF u.id_usuario IS NULL THEN RAISE EXCEPTION 'CREDENCIALES_INVALIDAS'; END IF;
  IF p_nueva_password_sha256 IS NOT NULL AND p_nueva_password_sha256 !~* '^[0-9a-f]{64}$' THEN RAISE EXCEPTION 'CLAVE_INVALIDA'; END IF;
  UPDATE usuario SET
    nombre = CASE WHEN v_nombre <> '' THEN split_part(v_nombre, ' ', 1) ELSE nombre END,
    apellido = CASE WHEN v_nombre <> '' THEN nullif(trim(substr(v_nombre, length(split_part(v_nombre, ' ', 1)) + 1)), '') ELSE apellido END,
    telefono = coalesce(nullif(trim(p_telefono), ''), telefono),
    password_hash = CASE WHEN p_nueva_password_sha256 IS NOT NULL THEN crypt(lower(p_nueva_password_sha256), gen_salt('bf', 10)) ELSE password_hash END,
    debe_cambiar_clave = CASE WHEN p_nueva_password_sha256 IS NOT NULL THEN false ELSE debe_cambiar_clave END,
    actualizado_en = now()
  WHERE id_usuario = u.id_usuario;
  UPDATE cliente SET nombre_contacto = CASE WHEN v_nombre <> '' THEN v_nombre ELSE nombre_contacto END,
         telefono_contacto = coalesce(nullif(trim(p_telefono), ''), telefono_contacto)
   WHERE id_usuario = u.id_usuario;
  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad)
  VALUES (u.id_usuario, _bb_rol(u.id_usuario), 'ACTUALIZAR', 'usuario', u.id_usuario::text);
  RETURN api_login(p_email, coalesce(p_nueva_password_sha256, p_password_sha256));
END $$;

-- Derecho de supresión (Ley N° 21.719): se anonimiza la cuenta y se conservan las reservas sin datos personales
CREATE OR REPLACE FUNCTION api_suprimir_cuenta(p_email text, p_password_sha256 text) RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario; v_c bigint;
BEGIN
  u := _bb_auth(p_email, p_password_sha256);
  IF u.id_usuario IS NULL THEN RETURN false; END IF;
  SELECT id_cliente INTO v_c FROM cliente WHERE id_usuario = u.id_usuario;
  INSERT INTO solicitud_derecho (codigo, id_cliente, tipo, estado, detalle, fecha_respuesta, respuesta)
  SELECT 'SD' || to_char(now(), 'YYMMDDHH24MISS'), v_c, 'SUPRESION', 'RESPONDIDA', 'Solicitada desde la cuenta', now(), 'Cuenta suprimida y datos anonimizados'
  WHERE v_c IS NOT NULL;
  UPDATE usuario SET email = 'suprimido-' || id_usuario || '@bigboys.invalid', nombre = 'Titular', apellido = 'suprimido',
         telefono = NULL, password_hash = crypt(gen_random_uuid()::text, gen_salt('bf', 10)),
         estado = 'SUPRIMIDO', suprimido_en = now(), actualizado_en = now()
   WHERE id_usuario = u.id_usuario;
  UPDATE cliente SET nombre_contacto = 'Titular suprimido', telefono_contacto = NULL, email_contacto = NULL
   WHERE id_cliente = v_c;
  UPDATE consentimiento SET revocado_en = now() WHERE id_cliente = v_c AND revocado_en IS NULL;
  INSERT INTO registro_auditoria (id_usuario, accion, entidad, id_entidad, detalle)
  VALUES (u.id_usuario, 'DERECHO_TITULAR', 'usuario', u.id_usuario::text, '{"tipo":"SUPRESION"}');
  RETURN true;
END $$;

-- Reservas: todos ven los horarios ocupados; los datos de contacto solo los ve el titular o el personal.
CREATE OR REPLACE FUNCTION api_listar_reservas(p_formato text DEFAULT 'APP', p_email text DEFAULT NULL,
                                               p_password_sha256 text DEFAULT NULL) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario; v_staff boolean := false; v_cli bigint;
BEGIN
  IF p_email IS NOT NULL THEN
    u := _bb_auth(p_email, p_password_sha256);
    IF u.id_usuario IS NOT NULL THEN
      v_staff := _bb_rol(u.id_usuario) IN ('DUENO', 'ADMIN_SUCURSAL', 'BARBERO');
      SELECT id_cliente INTO v_cli FROM cliente WHERE id_usuario = u.id_usuario;
    END IF;
  END IF;
  RETURN coalesce((SELECT jsonb_agg(_bb_reserva_json(r, upper(p_formato), v_staff OR r.id_cliente = v_cli) ORDER BY r.inicio DESC)
                   FROM (SELECT * FROM reserva WHERE inicio > now() - interval '90 days' ORDER BY inicio DESC LIMIT 500) r), '[]');
END $$;

CREATE OR REPLACE FUNCTION api_crear_reserva(p jsonb) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE v_formato text := CASE WHEN upper(p->>'canal') = 'APP' THEN 'APP' ELSE 'WEB' END;
        v_email text := lower(trim(coalesce(p->>'customer_email', ''))); v_cli bigint; v_u bigint;
        v_suc smallint; v_bar int; v_srv int; v_inicio timestamptz; v_codigo text; r reserva;
        -- LOCAL = atención registrada en el local (sin abono)
        v_canal text := CASE WHEN upper(p->>'canal') IN ('WEB','APP','LOCAL') THEN upper(p->>'canal') ELSE 'WEB' END;
BEGIN
  -- Idempotente: si la reserva ya existe, se devuelve
  r := _bb_buscar_reserva(jsonb_build_object('id', p->'id'));
  IF r.id_reserva IS NOT NULL THEN RETURN _bb_reserva_json(r, v_formato, false); END IF;

  SELECT id_barbero, id_sucursal INTO v_bar, v_suc FROM barbero WHERE id_barbero = (p->>'barber_id')::int AND activo;
  IF v_bar IS NULL THEN RAISE EXCEPTION 'BARBERO_INVALIDO'; END IF;
  v_srv := _bb_servicio((p->>'service_id')::int, p->>'service_name');
  v_inicio := _bb_parse_fecha(p->>'date_time');

  -- Cliente registrado (por correo) o invitado con datos mínimos
  IF v_email <> '' THEN
    SELECT c.id_cliente, u.id_usuario INTO v_cli, v_u FROM usuario u JOIN cliente c ON c.id_usuario = u.id_usuario
    WHERE u.email = v_email AND u.estado = 'ACTIVO';
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
    INSERT INTO reserva (codigo, id_cliente, id_sucursal, id_barbero, id_servicio, inicio, fin, estado, monto_abono, canal)
    VALUES (coalesce(v_codigo, 'R' || to_char(now(), 'YYMMDDHH24MISS')), v_cli, v_suc, v_bar, v_srv, v_inicio,
            v_inicio + make_interval(mins => (SELECT duracion_min FROM servicio WHERE id_servicio = v_srv)),
            CASE WHEN _bb_estado(p->>'status') = 'PENDIENTE_PAGO' THEN 'PENDIENTE_PAGO'
                 WHEN v_canal = 'LOCAL' AND _bb_estado(p->>'status') = 'ATENDIDA' THEN 'ATENDIDA'
                 ELSE 'CONFIRMADA' END,
            CASE WHEN v_canal = 'LOCAL' THEN 0 ELSE coalesce(_bb_cfg('ABONO_RESERVA')::numeric, 3000) END, v_canal)
    RETURNING * INTO r;
  EXCEPTION WHEN exclusion_violation THEN
    RAISE EXCEPTION 'HORARIO_OCUPADO';
  END;
  IF v_email <> '' THEN
    INSERT INTO notificacion (id_cliente, id_reserva, canal, tipo, asunto)
    VALUES (v_cli, r.id_reserva, 'EMAIL', 'CONFIRMACION', 'Confirmación de reserva ' || r.codigo);
  END IF;
  RETURN _bb_reserva_json(r, v_formato, true);
END $$;

-- Cambia el estado o reagenda (regla: ≥ 24 h de anticipación y máximo 2 veces)
CREATE OR REPLACE FUNCTION api_actualizar_reserva(p jsonb) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE v_formato text := CASE WHEN upper(p->>'canal') = 'APP' THEN 'APP' ELSE 'WEB' END;
        r reserva; v_estado text; v_nuevo timestamptz; v_dur int;
BEGIN
  r := _bb_buscar_reserva(p);
  IF r.id_reserva IS NULL THEN RAISE EXCEPTION 'RESERVA_NO_ENCONTRADA'; END IF;

  IF coalesce(p->>'new_date_time', '') <> '' THEN
    v_nuevo := _bb_parse_fecha(p->>'new_date_time');
    IF v_nuevo <> r.inicio THEN
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
    END IF;
  END IF;

  IF coalesce(p->>'status', '') <> '' THEN
    v_estado := _bb_estado(p->>'status');
    IF v_estado <> r.estado THEN
      UPDATE reserva SET estado = v_estado, actualizado_en = now(),
             cancelada_en = CASE WHEN v_estado = 'CANCELADA' THEN now() ELSE cancelada_en END,
             motivo_cancelacion = CASE WHEN v_estado = 'CANCELADA' THEN left(
                 '[' || coalesce(nullif(trim(p->>'cancelled_by'), ''), 'Cliente') || '] ' || coalesce(p->>'cancellation_reason', ''), 120)
               ELSE motivo_cancelacion END
       WHERE id_reserva = r.id_reserva RETURNING * INTO r;
      IF v_estado = 'CANCELADA' THEN
        INSERT INTO notificacion (id_cliente, id_reserva, canal, tipo, asunto)
        SELECT r.id_cliente, r.id_reserva, 'EMAIL', 'CANCELACION', 'Cancelación de reserva ' || r.codigo
        FROM cliente WHERE id_cliente = r.id_cliente AND email_contacto IS NOT NULL;
      END IF;
    END IF;
  END IF;
  -- Sin credenciales no se devuelven datos de contacto
  RETURN _bb_reserva_json(r, v_formato, false);
END $$;

CREATE OR REPLACE FUNCTION api_listar_resenas(p_barbero int DEFAULT NULL, p_sucursal smallint DEFAULT NULL) RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT coalesce(jsonb_agg(jsonb_build_object(
    'id', z.id_resena, 'location_id', z.id_sucursal, 'barber_id', z.id_barbero,
    'rating', z.calificacion, 'comment', coalesce(z.comentario, ''),
    'author', _bb_nombre_corto(c.nombre_contacto), 'created_at', z.creado_en) ORDER BY z.creado_en DESC), '[]')
  FROM resena z JOIN cliente c USING (id_cliente)
  WHERE z.visible AND (p_barbero IS NULL OR z.id_barbero = p_barbero) AND (p_sucursal IS NULL OR z.id_sucursal = p_sucursal);
$$;

CREATE OR REPLACE FUNCTION api_crear_resena(p_email text, p_password_sha256 text, p_sucursal smallint, p_barbero int,
                                            p_calificacion int, p_comentario text) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario; v_c bigint; v_id bigint;
BEGIN
  u := _bb_auth(p_email, p_password_sha256);
  IF u.id_usuario IS NULL THEN RAISE EXCEPTION 'CREDENCIALES_INVALIDAS'; END IF;
  SELECT id_cliente INTO v_c FROM cliente WHERE id_usuario = u.id_usuario;
  IF v_c IS NULL THEN RAISE EXCEPTION 'SOLO_CLIENTES'; END IF;
  INSERT INTO resena (id_cliente, id_sucursal, id_barbero, calificacion, comentario)
  VALUES (v_c, coalesce(p_sucursal, (SELECT id_sucursal FROM barbero WHERE id_barbero = p_barbero)), p_barbero,
          p_calificacion, left(nullif(trim(p_comentario), ''), 500))
  RETURNING id_resena INTO v_id;
  RETURN jsonb_build_object('id', v_id);
END $$;

-- Permisos: la clave anon solo puede ejecutar las funciones api_*
REVOKE ALL ON FUNCTION _bb_cfg(text), _bb_auth(text, text), _bb_rol(bigint), _bb_nombre_corto(text), _bb_parse_fecha(text),
  _bb_estado(text), _bb_servicio(int, text), _bb_precio(int, smallint), _bb_b36(bigint), _bb_id_cliente_app(text, bigint),
  _bb_reserva_json(reserva, text, boolean), _bb_buscar_reserva(jsonb) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION api_sucursales(), api_servicios(smallint), api_barberos(), api_usuario_existe(text), api_login(text, text),
  api_registrar_cliente(text, text, text, text, text), api_actualizar_perfil(text, text, text, text, text),
  api_suprimir_cuenta(text, text), api_listar_reservas(text, text, text), api_crear_reserva(jsonb),
  api_actualizar_reserva(jsonb), api_listar_resenas(int, smallint), api_crear_resena(text, text, smallint, int, int, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api_sucursales(), api_servicios(smallint), api_barberos(), api_usuario_existe(text), api_login(text, text),
  api_registrar_cliente(text, text, text, text, text), api_actualizar_perfil(text, text, text, text, text),
  api_suprimir_cuenta(text, text), api_listar_reservas(text, text, text), api_crear_reserva(jsonb),
  api_actualizar_reserva(jsonb), api_listar_resenas(int, smallint), api_crear_resena(text, text, smallint, int, int, text)
  TO anon, authenticated;

-- Funciones auxiliares con search_path fijo (aviso del linter de Supabase)
ALTER FUNCTION _bb_nombre_corto(text) SET search_path = public;
ALTER FUNCTION _bb_parse_fecha(text) SET search_path = public;
ALTER FUNCTION _bb_estado(text) SET search_path = public;
ALTER FUNCTION _bb_b36(bigint) SET search_path = public;
ALTER FUNCTION _bb_id_cliente_app(text, bigint) SET search_path = public;
