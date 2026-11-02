-- =====================================================================
-- Proyecto Barbería Big-Boys · Migración del 27-10-2026 (S12)
-- 1) Pago del abono con Webpay Plus (ambiente de integración de Transbank).
-- 2) Fila por orden de llegada (cajero/a) y pantalla pública de la fila.
-- 3) «Mis datos»: derechos del titular (Ley N° 21.719) y consentimientos.
-- 4) Cuentas del personal: el Dueño crea, bloquea y restablece perfiles.
-- 5) Cola de correos (confirmación, recordatorio, cancelación) que envía
--    el servidor de correo, no la app ni el navegador.
-- Se mantiene el criterio del 22-10: las tablas siguen con RLS cerrado y
-- la web y la app usan solo funciones api_* con la clave anon.
-- Proyecto Supabase: «Big Boys Project» (dgrflhgotxljjiejckoj).
-- =====================================================================

SET search_path = public, extensions;

CREATE EXTENSION IF NOT EXISTS http WITH SCHEMA extensions;

-- ---------------------------------------------------------------------
-- 0. Parámetros
-- ---------------------------------------------------------------------
-- Ambiente de integración de Transbank (Webpay Plus). En producción se cambian el host y el
-- código de comercio, y la llave secreta del negocio se configura en _bb_webpay (64 caracteres,
-- no cabe en configuracion.valor VARCHAR(60)).
INSERT INTO configuracion (clave, ambito, tipo_valor, valor)
SELECT v.clave, 'GLOBAL', 'TEXTO', v.valor
FROM (VALUES
  ('WEBPAY_HOST', 'https://webpay3gint.transbank.cl'),
  ('WEBPAY_CODIGO_COMERCIO', '597055555532'),
  -- Clave que comparte solo el servidor de correo (archivo .env, fuera del repositorio)
  ('CLAVE_SERVICIO_CORREO', encode(gen_random_bytes(24), 'hex'))
) AS v(clave, valor)
WHERE NOT EXISTS (SELECT 1 FROM configuracion c WHERE c.clave = v.clave AND c.ambito = 'GLOBAL');

CREATE INDEX IF NOT EXISTS ix_pago_reserva ON pago (id_reserva);
CREATE INDEX IF NOT EXISTS ix_turno_fila_dia ON turno_fila (id_sucursal, fecha);

-- ---------------------------------------------------------------------
-- 1. Funciones internas
-- ---------------------------------------------------------------------
-- Valida credenciales de personal; el cajero/a (ADMIN_SUCURSAL) solo opera su sucursal.
CREATE OR REPLACE FUNCTION _bb_staff(p_email text, p_password_sha256 text, p_roles text[], p_sucursal smallint DEFAULT NULL) RETURNS usuario
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario; v_ok boolean;
BEGIN
  u := _bb_auth(p_email, p_password_sha256);
  IF u.id_usuario IS NULL THEN RAISE EXCEPTION 'CREDENCIALES_INVALIDAS'; END IF;
  SELECT EXISTS (
    SELECT 1 FROM usuario_rol ur JOIN rol r USING (id_rol)
    WHERE ur.id_usuario = u.id_usuario AND r.codigo = ANY (p_roles)
      AND (r.codigo = 'DUENO' OR p_sucursal IS NULL OR ur.id_sucursal IS NULL OR ur.id_sucursal = p_sucursal)) INTO v_ok;
  IF NOT v_ok THEN RAISE EXCEPTION 'SIN_PERMISO'; END IF;
  RETURN u;
END $$;

-- Reservas con el pago pendiente que vencieron (15 min): liberan el horario
CREATE OR REPLACE FUNCTION _bb_expirar_pendientes() RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  WITH v AS (
    UPDATE reserva SET estado = 'EXPIRADA', actualizado_en = now()
     WHERE estado = 'PENDIENTE_PAGO' AND expira_en IS NOT NULL AND expira_en < now()
    RETURNING id_reserva)
  UPDATE pago SET estado = 'ANULADO', actualizado_en = now()
   WHERE estado = 'INICIADO' AND id_reserva IN (SELECT id_reserva FROM v);
END $$;

-- Llamada a la API REST de Webpay Plus (v1.2). La llave es la PÚBLICA del ambiente de integración.
CREATE OR REPLACE FUNCTION _bb_webpay(p_metodo text, p_ruta text, p_cuerpo jsonb) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE resp extensions.http_response;
        v_llave text := '579B532A7440BB0C9079DED94D31EA1615BACEB56610332264630D42D0A36B1C';
BEGIN
  resp := extensions.http((
    p_metodo,
    _bb_cfg('WEBPAY_HOST') || '/rswebpaytransaction/api/webpay/v1.2/transactions' || coalesce(p_ruta, ''),
    ARRAY[extensions.http_header('Tbk-Api-Key-Id', _bb_cfg('WEBPAY_CODIGO_COMERCIO')),
          extensions.http_header('Tbk-Api-Key-Secret', v_llave)],
    'application/json',
    -- La extensión http exige cuerpo en PUT/POST: el commit va con un JSON vacío
    CASE WHEN p_metodo = 'GET' THEN NULL ELSE coalesce(p_cuerpo, '{}'::jsonb)::text END)::extensions.http_request);
  IF resp.status NOT BETWEEN 200 AND 299 THEN
    RAISE EXCEPTION 'WEBPAY_ERROR_%: %', resp.status, left(coalesce(resp.content, ''), 200);
  END IF;
  RETURN resp.content::jsonb;
END $$;

-- ---------------------------------------------------------------------
-- 2. Reserva: la confirmación por correo se encola solo cuando la reserva queda confirmada
-- ---------------------------------------------------------------------
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
  PERFORM _bb_expirar_pendientes();

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
    INSERT INTO reserva (codigo, id_cliente, id_sucursal, id_barbero, id_servicio, inicio, fin, estado, monto_abono, canal, expira_en)
    VALUES (coalesce(v_codigo, 'R' || to_char(now(), 'YYMMDDHH24MISS')), v_cli, v_suc, v_bar, v_srv, v_inicio,
            v_inicio + make_interval(mins => (SELECT duracion_min FROM servicio WHERE id_servicio = v_srv)),
            CASE WHEN _bb_estado(p->>'status') = 'PENDIENTE_PAGO' THEN 'PENDIENTE_PAGO'
                 WHEN v_canal = 'LOCAL' AND _bb_estado(p->>'status') = 'ATENDIDA' THEN 'ATENDIDA'
                 ELSE 'CONFIRMADA' END,
            CASE WHEN v_canal = 'LOCAL' THEN 0 ELSE coalesce(_bb_cfg('ABONO_RESERVA')::numeric, 3000) END, v_canal,
            CASE WHEN _bb_estado(p->>'status') = 'PENDIENTE_PAGO'
                 THEN now() + make_interval(mins => coalesce(_bb_cfg('MINUTOS_EXPIRA_PAGO')::int, 15)) END)
    RETURNING * INTO r;
  EXCEPTION WHEN exclusion_violation THEN
    RAISE EXCEPTION 'HORARIO_OCUPADO';
  END;
  IF v_email <> '' AND r.estado = 'CONFIRMADA' THEN
    INSERT INTO notificacion (id_cliente, id_reserva, canal, tipo, asunto)
    VALUES (v_cli, r.id_reserva, 'EMAIL', 'CONFIRMACION', 'Confirmación de reserva ' || r.codigo);
  END IF;
  RETURN _bb_reserva_json(r, v_formato, true);
END $$;

-- ---------------------------------------------------------------------
-- 3. Webpay Plus
-- ---------------------------------------------------------------------
-- Crea la reserva en PENDIENTE_PAGO (bloquea el horario 15 min) y la transacción en Transbank.
-- La web/app redirige al cliente a url + token_ws; Transbank vuelve a p_return_url.
CREATE OR REPLACE FUNCTION api_webpay_iniciar(p jsonb, p_return_url text) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE v_res jsonb; r reserva; v_tx jsonb; v_id bigint := CASE WHEN (p->>'id') ~ '^\d{10,15}$' THEN (p->>'id')::bigint
                                                                ELSE (extract(epoch FROM clock_timestamp()) * 1000)::bigint END;
BEGIN
  IF coalesce(p_return_url, '') !~ '^https?://' THEN RAISE EXCEPTION 'RETURN_URL_INVALIDA'; END IF;
  IF upper(coalesce(p->>'canal', 'WEB')) = 'LOCAL' THEN RAISE EXCEPTION 'CANAL_SIN_ABONO'; END IF;
  v_res := api_crear_reserva(p || jsonb_build_object('id', v_id, 'status', 'Pendiente'));
  SELECT * INTO r FROM reserva WHERE codigo = v_res->>'codigo';
  IF r.estado <> 'PENDIENTE_PAGO' THEN RAISE EXCEPTION 'RESERVA_NO_PENDIENTE: %', r.estado; END IF;

  v_tx := _bb_webpay('POST', NULL, jsonb_build_object(
    'buy_order', r.codigo, 'session_id', 'S' || r.id_reserva,
    'amount', r.monto_abono::int, 'return_url', p_return_url));
  INSERT INTO pago (id_reserva, proveedor, id_transaccion, monto, estado)
  VALUES (r.id_reserva, 'WEBPAY', v_tx->>'token', r.monto_abono, 'INICIADO');
  RETURN jsonb_build_object('token', v_tx->>'token', 'url', v_tx->>'url', 'codigo', r.codigo,
                            'monto', r.monto_abono, 'expira_en', r.expira_en, 'reserva', v_res);
END $$;

-- Confirma (commit) al volver de Webpay. p_abortado = el cliente anuló el pago (TBK_TOKEN).
-- Idempotente: si el pago ya se resolvió, devuelve el resultado guardado.
CREATE OR REPLACE FUNCTION api_webpay_confirmar(p_token text, p_abortado boolean DEFAULT false) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE pg pago; r reserva; v_tx jsonb; v_ok boolean; v_cupo boolean := true; v_tarjeta text;
BEGIN
  SELECT * INTO pg FROM pago WHERE id_transaccion = p_token AND proveedor = 'WEBPAY' FOR UPDATE;
  IF pg.id_pago IS NULL THEN RAISE EXCEPTION 'PAGO_NO_ENCONTRADO'; END IF;

  IF pg.estado = 'INICIADO' THEN
    IF p_abortado THEN
      UPDATE pago SET estado = 'ANULADO', actualizado_en = now() WHERE id_pago = pg.id_pago RETURNING * INTO pg;
      UPDATE reserva SET estado = 'EXPIRADA', actualizado_en = now()
       WHERE id_reserva = pg.id_reserva AND estado = 'PENDIENTE_PAGO';
    ELSE
      BEGIN
        v_tx := _bb_webpay('PUT', '/' || p_token, NULL);
      EXCEPTION WHEN OTHERS THEN
        -- Si el commit falla (red, token ya confirmado), se consulta el estado real en Transbank
        BEGIN
          v_tx := _bb_webpay('GET', '/' || p_token, NULL);
        EXCEPTION WHEN OTHERS THEN
          v_tx := jsonb_build_object('status', 'FAILED', 'error', SQLERRM);
        END;
      END;
      v_ok := v_tx->>'status' = 'AUTHORIZED' AND (v_tx->>'response_code')::int = 0;
      v_tarjeta := v_tx #>> '{card_detail,card_number}';
      UPDATE pago SET
        estado = CASE WHEN v_ok THEN 'AUTORIZADO' ELSE 'RECHAZADO' END,
        codigo_autorizacion = nullif(v_tx->>'authorization_code', ''),
        tipo_medio = CASE v_tx->>'payment_type_code' WHEN 'VD' THEN 'DEBITO' WHEN 'VP' THEN 'PREPAGO'
                          WHEN 'VN' THEN 'CREDITO' WHEN 'VC' THEN 'CREDITO' WHEN 'SI' THEN 'CREDITO'
                          WHEN 'S2' THEN 'CREDITO' WHEN 'NC' THEN 'CREDITO' END,
        actualizado_en = now()
      WHERE id_pago = pg.id_pago RETURNING * INTO pg;
      IF v_ok THEN
        BEGIN
          UPDATE reserva SET estado = 'CONFIRMADA', expira_en = NULL, actualizado_en = now()
           WHERE id_reserva = pg.id_reserva AND estado IN ('PENDIENTE_PAGO', 'EXPIRADA');
        EXCEPTION WHEN exclusion_violation THEN
          v_cupo := false;  -- el horario venció y lo tomó otra persona: el abono queda para devolución
        END;
        INSERT INTO notificacion (id_cliente, id_reserva, canal, tipo, asunto)
        SELECT r2.id_cliente, r2.id_reserva, 'EMAIL', 'CONFIRMACION', 'Confirmación de reserva ' || r2.codigo
        FROM reserva r2 JOIN cliente c USING (id_cliente)
        WHERE r2.id_reserva = pg.id_reserva AND r2.estado = 'CONFIRMADA' AND c.email_contacto IS NOT NULL;
      ELSE
        UPDATE reserva SET estado = 'EXPIRADA', actualizado_en = now()
         WHERE id_reserva = pg.id_reserva AND estado = 'PENDIENTE_PAGO';
      END IF;
    END IF;
  END IF;

  SELECT * INTO r FROM reserva WHERE id_reserva = pg.id_reserva;
  RETURN jsonb_build_object(
    'estado', pg.estado, 'aprobado', pg.estado = 'AUTORIZADO', 'codigo', r.codigo,
    'reserva_estado', r.estado, 'sin_cupo', pg.estado = 'AUTORIZADO' AND r.estado <> 'CONFIRMADA',
    'autorizacion', pg.codigo_autorizacion, 'tipo_medio', pg.tipo_medio, 'tarjeta', v_tarjeta,
    'monto', pg.monto, 'orden_compra', r.codigo,
    'fecha', to_char(pg.actualizado_en AT TIME ZONE 'America/Santiago', 'YYYY-MM-DD HH24:MI'),
    'reserva', _bb_reserva_json(r, 'WEB', false));
END $$;

-- Estado del pago de una reserva (la app lo consulta al volver del navegador)
CREATE OR REPLACE FUNCTION api_webpay_estado(p_codigo text) RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT jsonb_build_object('codigo', r.codigo, 'reserva_estado', r.estado,
                            'estado', pg.estado, 'aprobado', pg.estado = 'AUTORIZADO',
                            'autorizacion', pg.codigo_autorizacion, 'monto', pg.monto)
  FROM reserva r LEFT JOIN LATERAL (SELECT * FROM pago WHERE id_reserva = r.id_reserva ORDER BY creado_en DESC LIMIT 1) pg ON true
  WHERE r.codigo = p_codigo;
$$;

-- ---------------------------------------------------------------------
-- 4. Fila por orden de llegada
-- ---------------------------------------------------------------------
-- Pantalla pública de la fila de hoy: solo número, nombre abreviado y estado.
CREATE OR REPLACE FUNCTION api_fila_listar(p_sucursal smallint) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public AS $$
DECLARE v_hoy date := (now() AT TIME ZONE 'America/Santiago')::date; v_espera int; v_barberos int; v_dur numeric;
BEGIN
  SELECT count(*) INTO v_espera FROM turno_fila WHERE id_sucursal = p_sucursal AND fecha = v_hoy AND estado IN ('EN_ESPERA', 'LLAMADO');
  SELECT greatest(count(*), 1) INTO v_barberos FROM barbero WHERE id_sucursal = p_sucursal AND activo;
  SELECT coalesce(avg(duracion_min), 30) INTO v_dur FROM servicio WHERE activo AND reservable_individual;
  RETURN jsonb_build_object(
    'location_id', p_sucursal, 'fecha', v_hoy, 'en_espera', v_espera,
    'espera_estimada_min', ceil(v_espera * v_dur / v_barberos),
    'turnos', coalesce((SELECT jsonb_agg(jsonb_build_object(
        'id', t.id_turno, 'numero', t.numero, 'nombre', _bb_nombre_corto(t.nombre_llamado),
        'estado', t.estado, 'servicio', s.nombre, 'barber_id', t.id_barbero_preferido,
        'barbero', b.nombre_publico,
        'llegada', to_char(t.hora_llegada AT TIME ZONE 'America/Santiago', 'HH24:MI'),
        'llamado', to_char(t.hora_llamado AT TIME ZONE 'America/Santiago', 'HH24:MI')) ORDER BY t.numero)
      FROM turno_fila t LEFT JOIN servicio s USING (id_servicio) LEFT JOIN barbero b ON b.id_barbero = t.id_barbero_preferido
      WHERE t.id_sucursal = p_sucursal AND t.fecha = v_hoy), '[]'));
END $$;

-- El cajero/a registra la llegada del cliente (CU-26)
CREATE OR REPLACE FUNCTION api_fila_registrar(p_email text, p_password_sha256 text, p jsonb) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario; v_suc smallint := (p->>'location_id')::smallint; v_hoy date := (now() AT TIME ZONE 'America/Santiago')::date;
        v_num int; v_cli bigint; t turno_fila; v_nombre text := left(trim(coalesce(p->>'nombre', '')), 40);
BEGIN
  u := _bb_staff(p_email, p_password_sha256, ARRAY['DUENO', 'ADMIN_SUCURSAL'], v_suc);
  IF v_nombre = '' THEN RAISE EXCEPTION 'NOMBRE_REQUERIDO'; END IF;
  IF NOT EXISTS (SELECT 1 FROM sucursal WHERE id_sucursal = v_suc AND activa) THEN RAISE EXCEPTION 'SUCURSAL_INVALIDA'; END IF;
  PERFORM pg_advisory_xact_lock(91000 + v_suc);
  SELECT coalesce(max(numero), 0) + 1 INTO v_num FROM turno_fila WHERE id_sucursal = v_suc AND fecha = v_hoy;
  -- Si es un cliente registrado, el turno se asocia a su cuenta (para su historial)
  IF coalesce(p->>'email', '') <> '' THEN
    SELECT c.id_cliente INTO v_cli FROM usuario x JOIN cliente c ON c.id_usuario = x.id_usuario
    WHERE x.email = lower(trim(p->>'email')) AND x.estado = 'ACTIVO';
  END IF;
  INSERT INTO turno_fila (id_sucursal, fecha, numero, nombre_llamado, id_cliente, id_servicio, id_barbero_preferido)
  VALUES (v_suc, v_hoy, v_num, v_nombre, v_cli,
          _bb_servicio((p->>'service_id')::int, p->>'service_name'),
          (SELECT id_barbero FROM barbero WHERE id_barbero = (p->>'barber_id')::int AND id_sucursal = v_suc AND activo))
  RETURNING * INTO t;
  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad)
  VALUES (u.id_usuario, _bb_rol(u.id_usuario), 'CREAR', 'turno_fila', t.id_turno::text);
  RETURN jsonb_build_object('id', t.id_turno, 'numero', t.numero, 'estado', t.estado);
END $$;

-- Avanza el turno: LLAMAR → INICIAR (crea la atención) → TERMINAR (monto y medio de pago) · ABANDONO (CU-27..29)
CREATE OR REPLACE FUNCTION api_fila_actualizar(p_email text, p_password_sha256 text, p_id_turno bigint,
                                               p_accion text, p jsonb DEFAULT '{}') RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario; t turno_fila; v_acc text := upper(coalesce(p_accion, '')); v_bar int; v_srv int; v_monto numeric;
BEGIN
  SELECT * INTO t FROM turno_fila WHERE id_turno = p_id_turno FOR UPDATE;
  IF t.id_turno IS NULL THEN RAISE EXCEPTION 'TURNO_NO_ENCONTRADO'; END IF;
  u := _bb_staff(p_email, p_password_sha256, ARRAY['DUENO', 'ADMIN_SUCURSAL'], t.id_sucursal);

  IF v_acc = 'LLAMAR' AND t.estado IN ('EN_ESPERA', 'LLAMADO') THEN
    UPDATE turno_fila SET estado = 'LLAMADO', hora_llamado = now() WHERE id_turno = t.id_turno RETURNING * INTO t;
  ELSIF v_acc = 'INICIAR' AND t.estado IN ('EN_ESPERA', 'LLAMADO') THEN
    v_bar := coalesce((p->>'barber_id')::int, t.id_barbero_preferido);
    IF NOT EXISTS (SELECT 1 FROM barbero WHERE id_barbero = v_bar AND id_sucursal = t.id_sucursal AND activo) THEN
      RAISE EXCEPTION 'BARBERO_REQUERIDO';
    END IF;
    v_srv := coalesce(_bb_servicio((p->>'service_id')::int, p->>'service_name'), t.id_servicio, _bb_servicio(NULL, 'Corte'));
    UPDATE turno_fila SET estado = 'EN_ATENCION', id_barbero_preferido = v_bar, id_servicio = v_srv,
           hora_llamado = coalesce(hora_llamado, now())
     WHERE id_turno = t.id_turno RETURNING * INTO t;
    INSERT INTO atencion (id_turno, id_cliente, id_sucursal, id_barbero, id_servicio, inicio, monto_cobrado, registrada_por)
    VALUES (t.id_turno, t.id_cliente, t.id_sucursal, v_bar, v_srv, now(), 0, u.id_usuario);
  ELSIF v_acc = 'TERMINAR' AND t.estado = 'EN_ATENCION' THEN
    v_monto := coalesce((p->>'monto')::numeric, _bb_precio(t.id_servicio, t.id_sucursal));
    UPDATE atencion SET fin = now(), monto_cobrado = v_monto,
           propina = coalesce((p->>'propina')::numeric, 0),
           medio_pago_local = CASE WHEN upper(p->>'medio_pago') IN ('EFECTIVO','DEBITO','CREDITO','TRANSFERENCIA')
                                   THEN upper(p->>'medio_pago') ELSE 'EFECTIVO' END
     WHERE id_turno = t.id_turno;
    UPDATE turno_fila SET estado = 'ATENDIDO', hora_termino = now() WHERE id_turno = t.id_turno RETURNING * INTO t;
  ELSIF v_acc = 'ABANDONO' AND t.estado IN ('EN_ESPERA', 'LLAMADO') THEN
    UPDATE turno_fila SET estado = 'ABANDONO', hora_termino = now() WHERE id_turno = t.id_turno RETURNING * INTO t;
  ELSE
    RAISE EXCEPTION 'ACCION_INVALIDA: % desde %', v_acc, t.estado;
  END IF;
  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad, detalle)
  VALUES (u.id_usuario, _bb_rol(u.id_usuario), 'ACTUALIZAR', 'turno_fila', t.id_turno::text, jsonb_build_object('accion', v_acc));
  RETURN jsonb_build_object('id', t.id_turno, 'numero', t.numero, 'estado', t.estado);
END $$;

-- KPI del día por sucursal (llegadas, atendidos, abandonos) para el panel de administración
CREATE OR REPLACE FUNCTION api_fila_kpi(p_email text, p_password_sha256 text, p_sucursal smallint DEFAULT NULL) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public, extensions AS $$
BEGIN
  PERFORM _bb_staff(p_email, p_password_sha256, ARRAY['DUENO', 'ADMIN_SUCURSAL'], p_sucursal);
  RETURN coalesce((SELECT jsonb_agg(to_jsonb(k) ORDER BY k.fecha DESC, k.id_sucursal) FROM v_kpi_abandono_diario k
                   WHERE (p_sucursal IS NULL OR k.id_sucursal = p_sucursal) AND k.fecha > current_date - 30), '[]');
END $$;

-- El barbero solo ve sus atenciones y lo ganado (CU-41)
CREATE OR REPLACE FUNCTION api_mis_atenciones(p_email text, p_password_sha256 text, p_fecha date DEFAULT NULL) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario; v_bar int; v_dia date := coalesce(p_fecha, (now() AT TIME ZONE 'America/Santiago')::date);
BEGIN
  u := _bb_staff(p_email, p_password_sha256, ARRAY['BARBERO']);
  SELECT id_barbero INTO v_bar FROM barbero WHERE id_usuario = u.id_usuario;
  RETURN jsonb_build_object('barber_id', v_bar, 'fecha', v_dia,
    'atenciones', coalesce((SELECT jsonb_agg(jsonb_build_object(
        'hora', to_char(v.inicio AT TIME ZONE 'America/Santiago', 'HH24:MI'), 'servicio', v.servicio,
        'monto', v.monto_cobrado, 'propina', v.propina) ORDER BY v.inicio)
      FROM v_atenciones_barbero v WHERE v.id_barbero = v_bar AND v.fecha = v_dia), '[]'),
    'total', coalesce((SELECT sum(monto_cobrado) FROM v_atenciones_barbero WHERE id_barbero = v_bar AND fecha = v_dia), 0),
    'propinas', coalesce((SELECT sum(propina) FROM v_atenciones_barbero WHERE id_barbero = v_bar AND fecha = v_dia), 0));
END $$;

-- ---------------------------------------------------------------------
-- 5. «Mis datos» (derechos del titular)
-- ---------------------------------------------------------------------
-- Acceso y portabilidad: entrega todos los datos del titular en JSON y deja la solicitud respondida.
CREATE OR REPLACE FUNCTION api_mis_datos(p_email text, p_password_sha256 text, p_tipo text DEFAULT 'ACCESO') RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario; c cliente; v_tipo text := CASE WHEN upper(p_tipo) = 'PORTABILIDAD' THEN 'PORTABILIDAD' ELSE 'ACCESO' END;
BEGIN
  u := _bb_auth(p_email, p_password_sha256);
  IF u.id_usuario IS NULL THEN RAISE EXCEPTION 'CREDENCIALES_INVALIDAS'; END IF;
  SELECT * INTO c FROM cliente WHERE id_usuario = u.id_usuario;
  IF c.id_cliente IS NOT NULL THEN
    INSERT INTO solicitud_derecho (codigo, id_cliente, tipo, estado, detalle, fecha_respuesta, respuesta)
    VALUES ('SD' || upper(_bb_b36((extract(epoch FROM clock_timestamp()) * 1000)::bigint)), c.id_cliente, v_tipo, 'RESPONDIDA',
            'Autoservicio «Mis datos»', now(), 'Datos entregados en línea');
  END IF;
  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad, detalle)
  VALUES (u.id_usuario, _bb_rol(u.id_usuario), CASE WHEN v_tipo = 'PORTABILIDAD' THEN 'EXPORTAR' ELSE 'DERECHO_TITULAR' END,
          'usuario', u.id_usuario::text, jsonb_build_object('tipo', v_tipo));
  RETURN jsonb_build_object(
    'generado_en', to_char(now() AT TIME ZONE 'America/Santiago', 'YYYY-MM-DD HH24:MI'),
    'responsable', 'Barbería Big Boys',
    'cuenta', jsonb_build_object('nombre', u.nombre, 'apellido', u.apellido, 'email', u.email,
                                 'telefono', u.telefono, 'creada_en', u.creado_en, 'ultimo_acceso', u.ultimo_acceso),
    'cliente', CASE WHEN c.id_cliente IS NULL THEN NULL ELSE jsonb_build_object(
                 'nombre_contacto', c.nombre_contacto, 'telefono_contacto', c.telefono_contacto,
                 'email_contacto', c.email_contacto, 'puntos', c.puntos, 'ultima_actividad', c.ultima_actividad) END,
    'reservas', coalesce((SELECT jsonb_agg(jsonb_build_object('codigo', r.codigo, 'sucursal', s.nombre,
                   'servicio', sv.nombre, 'inicio', to_char(r.inicio AT TIME ZONE 'America/Santiago', 'YYYY-MM-DD HH24:MI'),
                   'estado', r.estado, 'abono', r.monto_abono, 'canal', r.canal) ORDER BY r.inicio DESC)
                 FROM reserva r JOIN sucursal s USING (id_sucursal) JOIN servicio sv USING (id_servicio)
                 WHERE r.id_cliente = c.id_cliente), '[]'),
    'pagos', coalesce((SELECT jsonb_agg(jsonb_build_object('reserva', r.codigo, 'proveedor', p.proveedor,
                   'monto', p.monto, 'estado', p.estado, 'medio', p.tipo_medio, 'fecha', p.creado_en))
                 FROM pago p JOIN reserva r USING (id_reserva) WHERE r.id_cliente = c.id_cliente), '[]'),
    'atenciones', coalesce((SELECT jsonb_agg(jsonb_build_object('inicio', a.inicio, 'servicio', sv.nombre,
                   'monto', a.monto_cobrado)) FROM atencion a JOIN servicio sv USING (id_servicio)
                 WHERE a.id_cliente = c.id_cliente), '[]'),
    'resenas', coalesce((SELECT jsonb_agg(jsonb_build_object('calificacion', z.calificacion, 'comentario', z.comentario,
                   'fecha', z.creado_en)) FROM resena z WHERE z.id_cliente = c.id_cliente), '[]'),
    'consentimientos', coalesce((SELECT jsonb_agg(jsonb_build_object('finalidad', k.finalidad, 'otorgado', k.otorgado,
                   'canal', k.canal, 'politica', po.tipo || ' v' || po.version, 'registrado_en', k.registrado_en,
                   'revocado_en', k.revocado_en) ORDER BY k.registrado_en)
                 FROM consentimiento k JOIN politica po USING (id_politica) WHERE k.id_cliente = c.id_cliente), '[]'),
    'solicitudes', coalesce((SELECT jsonb_agg(jsonb_build_object('codigo', d.codigo, 'tipo', d.tipo, 'estado', d.estado,
                   'ingreso', d.fecha_ingreso, 'fecha_limite', d.fecha_limite, 'respuesta', d.respuesta) ORDER BY d.fecha_ingreso DESC)
                 FROM solicitud_derecho d WHERE d.id_cliente = c.id_cliente), '[]'));
END $$;

-- Rectificación, oposición o bloqueo: queda RECIBIDA con plazo de 30 días (trigger tg_solicitud_plazo)
CREATE OR REPLACE FUNCTION api_solicitud_derecho(p_email text, p_password_sha256 text, p_tipo text, p_detalle text) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario; v_c bigint; d solicitud_derecho;
BEGIN
  u := _bb_auth(p_email, p_password_sha256);
  IF u.id_usuario IS NULL THEN RAISE EXCEPTION 'CREDENCIALES_INVALIDAS'; END IF;
  IF upper(p_tipo) NOT IN ('RECTIFICACION', 'OPOSICION', 'BLOQUEO') THEN RAISE EXCEPTION 'TIPO_INVALIDO'; END IF;
  SELECT id_cliente INTO v_c FROM cliente WHERE id_usuario = u.id_usuario;
  IF v_c IS NULL THEN RAISE EXCEPTION 'SOLO_CLIENTES'; END IF;
  INSERT INTO solicitud_derecho (codigo, id_cliente, tipo, detalle)
  VALUES ('SD' || upper(_bb_b36((extract(epoch FROM clock_timestamp()) * 1000)::bigint)), v_c, upper(p_tipo), left(nullif(trim(p_detalle), ''), 500))
  RETURNING * INTO d;
  INSERT INTO notificacion (id_cliente, canal, tipo, asunto)
  VALUES (v_c, 'EMAIL', 'DERECHOS', 'Recibimos tu solicitud ' || d.codigo);
  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad, detalle)
  VALUES (u.id_usuario, _bb_rol(u.id_usuario), 'DERECHO_TITULAR', 'solicitud_derecho', d.codigo, jsonb_build_object('tipo', d.tipo));
  RETURN jsonb_build_object('codigo', d.codigo, 'tipo', d.tipo, 'estado', d.estado, 'fecha_limite', d.fecha_limite);
END $$;

-- Otorga o revoca el consentimiento para comunicaciones promocionales
CREATE OR REPLACE FUNCTION api_consentimiento(p_email text, p_password_sha256 text, p_otorgado boolean, p_canal text DEFAULT 'WEB') RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario; v_c bigint;
BEGIN
  u := _bb_auth(p_email, p_password_sha256);
  IF u.id_usuario IS NULL THEN RAISE EXCEPTION 'CREDENCIALES_INVALIDAS'; END IF;
  SELECT id_cliente INTO v_c FROM cliente WHERE id_usuario = u.id_usuario;
  IF v_c IS NULL THEN RAISE EXCEPTION 'SOLO_CLIENTES'; END IF;
  UPDATE consentimiento SET revocado_en = now() WHERE id_cliente = v_c AND finalidad = 'PROMOCIONAL' AND revocado_en IS NULL;
  IF p_otorgado THEN
    INSERT INTO consentimiento (id_cliente, id_politica, finalidad, otorgado, canal)
    SELECT v_c, id_politica, 'PROMOCIONAL', true, CASE WHEN upper(p_canal) IN ('WEB','APP','LOCAL') THEN upper(p_canal) ELSE 'WEB' END
    FROM politica WHERE tipo = 'PRIVACIDAD' AND vigente;
  END IF;
  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad, detalle)
  VALUES (u.id_usuario, _bb_rol(u.id_usuario), 'DERECHO_TITULAR', 'consentimiento', v_c::text,
          jsonb_build_object('finalidad', 'PROMOCIONAL', 'otorgado', p_otorgado));
  RETURN jsonb_build_object('promocional', p_otorgado);
END $$;

-- El Dueño revisa y responde las solicitudes dentro del plazo
CREATE OR REPLACE FUNCTION api_solicitudes_listar(p_email text, p_password_sha256 text) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public, extensions AS $$
BEGIN
  PERFORM _bb_staff(p_email, p_password_sha256, ARRAY['DUENO']);
  RETURN coalesce((SELECT jsonb_agg(jsonb_build_object('id', d.id_solicitud, 'codigo', d.codigo, 'tipo', d.tipo,
            'estado', d.estado, 'detalle', d.detalle, 'titular', c.nombre_contacto, 'email', c.email_contacto,
            'ingreso', to_char(d.fecha_ingreso AT TIME ZONE 'America/Santiago', 'YYYY-MM-DD'),
            'fecha_limite', coalesce(d.prorrogada_hasta, d.fecha_limite), 'respuesta', d.respuesta)
          ORDER BY (d.estado IN ('RESPONDIDA', 'RECHAZADA')), d.fecha_limite)
    FROM solicitud_derecho d JOIN cliente c USING (id_cliente)), '[]');
END $$;

CREATE OR REPLACE FUNCTION api_solicitud_responder(p_email text, p_password_sha256 text, p_id bigint,
                                                   p_estado text, p_respuesta text) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE u usuario; d solicitud_derecho;
BEGIN
  u := _bb_staff(p_email, p_password_sha256, ARRAY['DUENO']);
  IF upper(p_estado) NOT IN ('EN_PROCESO', 'RESPONDIDA', 'RECHAZADA', 'PRORROGADA') THEN RAISE EXCEPTION 'ESTADO_INVALIDO'; END IF;
  UPDATE solicitud_derecho SET estado = upper(p_estado), respuesta = left(nullif(trim(p_respuesta), ''), 500),
         atendida_por = u.id_usuario,
         fecha_respuesta = CASE WHEN upper(p_estado) IN ('RESPONDIDA', 'RECHAZADA') THEN now() END,
         prorrogada_hasta = CASE WHEN upper(p_estado) = 'PRORROGADA' THEN fecha_limite + 30 ELSE prorrogada_hasta END
   WHERE id_solicitud = p_id RETURNING * INTO d;
  IF d.id_solicitud IS NULL THEN RAISE EXCEPTION 'SOLICITUD_NO_ENCONTRADA'; END IF;
  INSERT INTO notificacion (id_cliente, canal, tipo, asunto)
  VALUES (d.id_cliente, 'EMAIL', 'DERECHOS', 'Actualización de tu solicitud ' || d.codigo);
  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad, detalle)
  VALUES (u.id_usuario, 'DUENO', 'DERECHO_TITULAR', 'solicitud_derecho', d.codigo, jsonb_build_object('estado', d.estado));
  RETURN jsonb_build_object('codigo', d.codigo, 'estado', d.estado);
END $$;

-- ---------------------------------------------------------------------
-- 6. Cuentas del personal (SC-16, RF-63, CU-31)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION api_personal_listar(p_email text, p_password_sha256 text) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public, extensions AS $$
BEGIN
  PERFORM _bb_staff(p_email, p_password_sha256, ARRAY['DUENO']);
  RETURN coalesce((SELECT jsonb_agg(jsonb_build_object(
      'id', u.id_usuario, 'email', u.email, 'full_name', trim(u.nombre || ' ' || coalesce(u.apellido, '')),
      'phone', coalesce(u.telefono, ''), 'role', r.codigo, 'location_id', coalesce(ur.id_sucursal, b.id_sucursal),
      'estado', u.estado, 'must_change_password', u.debe_cambiar_clave,
      'ultimo_acceso', to_char(u.ultimo_acceso AT TIME ZONE 'America/Santiago', 'YYYY-MM-DD HH24:MI'),
      'barber_id', b.id_barbero, 'public_name', b.nombre_publico, 'specialty', b.especialidad)
      ORDER BY r.codigo, u.nombre)
    FROM usuario u JOIN usuario_rol ur USING (id_usuario) JOIN rol r USING (id_rol)
    LEFT JOIN barbero b ON b.id_usuario = u.id_usuario
    WHERE r.codigo IN ('BARBERO', 'ADMIN_SUCURSAL') AND u.estado <> 'SUPRIMIDO'), '[]');
END $$;

-- La clave temporal la genera el panel del Dueño y llega como SHA-256; el colaborador la cambia al ingresar.
CREATE OR REPLACE FUNCTION api_personal_crear(p_email text, p_password_sha256 text, p jsonb) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE d usuario; v_email text := lower(trim(coalesce(p->>'email', ''))); v_rol text := upper(coalesce(p->>'role', ''));
        v_suc smallint := (p->>'location_id')::smallint; v_nombre text := trim(coalesce(p->>'full_name', ''));
        v_u bigint; v_bar int;
BEGIN
  d := _bb_staff(p_email, p_password_sha256, ARRAY['DUENO']);
  IF v_rol NOT IN ('BARBERO', 'ADMIN_SUCURSAL') THEN RAISE EXCEPTION 'ROL_INVALIDO'; END IF;
  IF v_email !~ '^[^@\s]+@[^@\s]+\.[^@\s]+$' THEN RAISE EXCEPTION 'CORREO_INVALIDO'; END IF;
  IF v_nombre = '' THEN RAISE EXCEPTION 'NOMBRE_REQUERIDO'; END IF;
  IF NOT EXISTS (SELECT 1 FROM sucursal WHERE id_sucursal = v_suc AND activa) THEN RAISE EXCEPTION 'SUCURSAL_INVALIDA'; END IF;
  IF coalesce(p->>'temp_password_sha256', '') !~* '^[0-9a-f]{64}$' THEN RAISE EXCEPTION 'CLAVE_INVALIDA'; END IF;
  IF EXISTS (SELECT 1 FROM usuario WHERE email = v_email) THEN RAISE EXCEPTION 'CORREO_YA_REGISTRADO'; END IF;

  INSERT INTO usuario (email, password_hash, nombre, apellido, telefono, creado_por, debe_cambiar_clave)
  VALUES (v_email, crypt(lower(p->>'temp_password_sha256'), gen_salt('bf', 10)), split_part(v_nombre, ' ', 1),
          nullif(trim(substr(v_nombre, length(split_part(v_nombre, ' ', 1)) + 1)), ''),
          nullif(trim(p->>'phone'), ''), d.id_usuario, true)
  RETURNING id_usuario INTO v_u;
  INSERT INTO usuario_rol (id_usuario, id_rol, id_sucursal) SELECT v_u, id_rol, v_suc FROM rol WHERE codigo = v_rol;
  IF v_rol = 'BARBERO' THEN
    INSERT INTO barbero (id_usuario, id_sucursal, nombre_publico, especialidad)
    VALUES (v_u, v_suc, left(coalesce(nullif(trim(p->>'public_name'), ''), v_nombre), 60), nullif(trim(p->>'specialty'), ''))
    RETURNING id_barbero INTO v_bar;
    -- Horario inicial: lunes a sábado de 10:00 a 19:00 en bloques de una hora (el Dueño lo ajusta después)
    INSERT INTO horario_barbero (id_barbero, dia_semana, hora_inicio, hora_fin)
    SELECT v_bar, dd, make_time(h, 0, 0), make_time(h + 1, 0, 0) FROM generate_series(1, 6) dd, generate_series(10, 18) h;
  END IF;
  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad, detalle)
  VALUES (d.id_usuario, 'DUENO', 'CREAR', 'usuario', v_u::text, jsonb_build_object('rol', v_rol, 'sucursal', v_suc));
  RETURN jsonb_build_object('id', v_u, 'email', v_email, 'role', v_rol, 'barber_id', v_bar);
END $$;

-- Bloquea o reactiva una cuenta del personal; al reactivar se puede asignar una nueva clave temporal
CREATE OR REPLACE FUNCTION api_personal_actualizar(p_email text, p_password_sha256 text, p_id_usuario bigint,
                                                   p_activo boolean, p_temp_password_sha256 text DEFAULT NULL) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE d usuario; x usuario;
BEGIN
  d := _bb_staff(p_email, p_password_sha256, ARRAY['DUENO']);
  IF NOT EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r USING (id_rol)
                 WHERE ur.id_usuario = p_id_usuario AND r.codigo IN ('BARBERO', 'ADMIN_SUCURSAL')) THEN
    RAISE EXCEPTION 'NO_ES_PERSONAL';
  END IF;
  IF p_temp_password_sha256 IS NOT NULL AND p_temp_password_sha256 !~* '^[0-9a-f]{64}$' THEN RAISE EXCEPTION 'CLAVE_INVALIDA'; END IF;
  UPDATE usuario SET estado = CASE WHEN p_activo THEN 'ACTIVO' ELSE 'BLOQUEADO' END, intentos_fallidos = 0,
         password_hash = CASE WHEN p_temp_password_sha256 IS NOT NULL
                              THEN crypt(lower(p_temp_password_sha256), gen_salt('bf', 10)) ELSE password_hash END,
         debe_cambiar_clave = debe_cambiar_clave OR p_temp_password_sha256 IS NOT NULL, actualizado_en = now()
   WHERE id_usuario = p_id_usuario AND estado <> 'SUPRIMIDO' RETURNING * INTO x;
  UPDATE barbero SET activo = p_activo WHERE id_usuario = p_id_usuario;
  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad, detalle)
  VALUES (d.id_usuario, 'DUENO', 'ACTUALIZAR', 'usuario', p_id_usuario::text,
          jsonb_build_object('activo', p_activo, 'nueva_clave_temporal', p_temp_password_sha256 IS NOT NULL));
  RETURN jsonb_build_object('id', x.id_usuario, 'estado', x.estado, 'must_change_password', x.debe_cambiar_clave);
END $$;

-- ---------------------------------------------------------------------
-- 7. Cola de correos (la procesa mail_server.js con la CLAVE_SERVICIO_CORREO)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION api_correo_pendientes(p_clave text) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF p_clave IS NULL OR p_clave <> _bb_cfg('CLAVE_SERVICIO_CORREO') THEN RAISE EXCEPTION 'SIN_PERMISO'; END IF;
  PERFORM _bb_expirar_pendientes();
  -- Recordatorio 24 h antes de cada reserva confirmada (una sola vez)
  INSERT INTO notificacion (id_cliente, id_reserva, canal, tipo, asunto, programada_para)
  SELECT r.id_cliente, r.id_reserva, 'EMAIL', 'RECORDATORIO', 'Recordatorio de tu reserva ' || r.codigo, r.inicio - interval '24 hours'
  FROM reserva r JOIN cliente c USING (id_cliente)
  WHERE r.estado = 'CONFIRMADA' AND r.inicio BETWEEN now() AND now() + interval '24 hours' AND c.email_contacto IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM notificacion n WHERE n.id_reserva = r.id_reserva AND n.tipo = 'RECORDATORIO');
  RETURN coalesce((SELECT jsonb_agg(x ORDER BY (x->>'id')::bigint) FROM (
    SELECT jsonb_build_object(
      'id', n.id_notificacion, 'tipo', n.tipo, 'asunto', n.asunto,
      'to', coalesce(c.email_contacto, u.email), 'nombre', coalesce(c.nombre_contacto, u.nombre),
      'codigo', r.codigo, 'estado_reserva', r.estado,
      'fecha', to_char(r.inicio AT TIME ZONE 'America/Santiago', 'DD-MM-YYYY HH24:MI'),
      'servicio', sv.nombre, 'sucursal', s.nombre, 'direccion', s.direccion || ', ' || s.comuna,
      'barbero', b.nombre_publico, 'abono', r.monto_abono) AS x
    FROM notificacion n
    LEFT JOIN cliente c ON c.id_cliente = n.id_cliente LEFT JOIN usuario u ON u.id_usuario = n.id_usuario
    LEFT JOIN reserva r ON r.id_reserva = n.id_reserva LEFT JOIN servicio sv ON sv.id_servicio = r.id_servicio
    LEFT JOIN sucursal s ON s.id_sucursal = r.id_sucursal LEFT JOIN barbero b ON b.id_barbero = r.id_barbero
    WHERE n.canal = 'EMAIL' AND n.estado = 'PENDIENTE' AND coalesce(n.programada_para, now()) <= now()
      AND coalesce(c.email_contacto, u.email) IS NOT NULL
    ORDER BY n.id_notificacion LIMIT 20) q), '[]');
END $$;

CREATE OR REPLACE FUNCTION api_correo_marcar(p_clave text, p_id bigint, p_enviado boolean) RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF p_clave IS NULL OR p_clave <> _bb_cfg('CLAVE_SERVICIO_CORREO') THEN RAISE EXCEPTION 'SIN_PERMISO'; END IF;
  UPDATE notificacion SET estado = CASE WHEN p_enviado THEN 'ENVIADA' ELSE 'FALLIDA' END,
         enviada_en = CASE WHEN p_enviado THEN now() END
   WHERE id_notificacion = p_id AND estado = 'PENDIENTE';
  RETURN FOUND;
END $$;

-- ---------------------------------------------------------------------
-- 8. Permisos
-- ---------------------------------------------------------------------
REVOKE ALL ON FUNCTION _bb_staff(text, text, text[], smallint), _bb_expirar_pendientes(), _bb_webpay(text, text, jsonb)
  FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION api_crear_reserva(jsonb), api_webpay_iniciar(jsonb, text), api_webpay_confirmar(text, boolean),
  api_webpay_estado(text), api_fila_listar(smallint), api_fila_registrar(text, text, jsonb),
  api_fila_actualizar(text, text, bigint, text, jsonb), api_fila_kpi(text, text, smallint), api_mis_atenciones(text, text, date),
  api_mis_datos(text, text, text), api_solicitud_derecho(text, text, text, text), api_consentimiento(text, text, boolean, text),
  api_solicitudes_listar(text, text), api_solicitud_responder(text, text, bigint, text, text),
  api_personal_listar(text, text), api_personal_crear(text, text, jsonb), api_personal_actualizar(text, text, bigint, boolean, text),
  api_correo_pendientes(text), api_correo_marcar(text, bigint, boolean) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api_crear_reserva(jsonb), api_webpay_iniciar(jsonb, text), api_webpay_confirmar(text, boolean),
  api_webpay_estado(text), api_fila_listar(smallint), api_fila_registrar(text, text, jsonb),
  api_fila_actualizar(text, text, bigint, text, jsonb), api_fila_kpi(text, text, smallint), api_mis_atenciones(text, text, date),
  api_mis_datos(text, text, text), api_solicitud_derecho(text, text, text, text), api_consentimiento(text, text, boolean, text),
  api_solicitudes_listar(text, text), api_solicitud_responder(text, text, bigint, text, text),
  api_personal_listar(text, text), api_personal_crear(text, text, jsonb), api_personal_actualizar(text, text, bigint, boolean, text),
  api_correo_pendientes(text), api_correo_marcar(text, bigint, boolean) TO anon, authenticated;

-- ---------------------------------------------------------------------
-- 9. api_login devuelve la sucursal y el barbero del personal (panel del cajero/a y del barbero)
-- ---------------------------------------------------------------------
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
    'must_change_password', u.debe_cambiar_clave,
    -- Sucursal del personal (cajero/a o barbero); NULL para el Dueño y los clientes
    'location_id', coalesce((SELECT ur.id_sucursal FROM usuario_rol ur WHERE ur.id_usuario = u.id_usuario AND ur.id_sucursal IS NOT NULL LIMIT 1),
                            (SELECT b.id_sucursal FROM barbero b WHERE b.id_usuario = u.id_usuario)),
    'barber_id', (SELECT b.id_barbero FROM barbero b WHERE b.id_usuario = u.id_usuario));
END $$;

-- ---------------------------------------------------------------------
-- 10. Ingreso del barbero en la app: elige su nombre y escribe su contraseña
--     (reemplaza el PIN 1234; usa api_login y su bloqueo tras 5 intentos fallidos)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION api_login_barbero(p_id_barbero int, p_password_sha256 text) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE v_email text; v jsonb;
BEGIN
  SELECT u.email INTO v_email FROM barbero b JOIN usuario u USING (id_usuario) WHERE b.id_barbero = p_id_barbero AND b.activo;
  IF v_email IS NULL THEN RETURN NULL; END IF;
  v := api_login(v_email, p_password_sha256);
  IF v IS NULL OR v->>'role' <> 'BARBERO' THEN RETURN NULL; END IF;
  RETURN v;
END $$;
REVOKE ALL ON FUNCTION api_login_barbero(int, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api_login_barbero(int, text) TO anon, authenticated;
