-- =====================================================================
-- Correcciones del Ciclo 1 (10-11-2026) — derechos del titular (Ley N° 21.719)
--  DEF-004 (CP-23): no se suprime una cuenta con reservas futuras confirmadas o pendientes de pago.
--  DEF-005 (CP-24): la oposición detiene de inmediato el tratamiento promocional.
--  DEF-006 (CP-26): el bloqueo detiene de inmediato los usos no esenciales (promocional).
--  DEF-007 (CP-68): una sola prórroga por solicitud; el rechazo informa el derecho a reclamar.
-- =====================================================================

CREATE OR REPLACE FUNCTION public.api_suprimir_cuenta(p_email text, p_password_sha256 text)
 RETURNS boolean
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
DECLARE u usuario; v_c bigint;
BEGIN
  u := _bb_auth(p_email, p_password_sha256);
  IF u.id_usuario IS NULL THEN RETURN false; END IF;
  SELECT id_cliente INTO v_c FROM cliente WHERE id_usuario = u.id_usuario;
  -- DEF-004: primero hay que cancelar las citas futuras (el titular decide qué pasa con su abono)
  IF EXISTS (SELECT 1 FROM reserva WHERE id_cliente = v_c AND estado IN ('CONFIRMADA', 'PENDIENTE_PAGO') AND inicio > now()) THEN
    RAISE EXCEPTION 'RESERVAS_FUTURAS';
  END IF;
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
END $function$;

CREATE OR REPLACE FUNCTION public.api_solicitud_derecho(p_email text, p_password_sha256 text, p_tipo text, p_detalle text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
DECLARE u usuario; v_c bigint; d solicitud_derecho; v_detenido boolean := false;
BEGIN
  u := _bb_auth(p_email, p_password_sha256);
  IF u.id_usuario IS NULL THEN RAISE EXCEPTION 'CREDENCIALES_INVALIDAS'; END IF;
  IF upper(p_tipo) NOT IN ('RECTIFICACION', 'OPOSICION', 'BLOQUEO') THEN RAISE EXCEPTION 'TIPO_INVALIDO'; END IF;
  SELECT id_cliente INTO v_c FROM cliente WHERE id_usuario = u.id_usuario;
  IF v_c IS NULL THEN RAISE EXCEPTION 'SOLO_CLIENTES'; END IF;
  -- DEF-005 y DEF-006: oposición y bloqueo detienen de inmediato el uso promocional (no esencial)
  IF upper(p_tipo) IN ('OPOSICION', 'BLOQUEO') THEN
    UPDATE consentimiento SET revocado_en = now()
     WHERE id_cliente = v_c AND finalidad = 'PROMOCIONAL' AND revocado_en IS NULL;
    v_detenido := true;
  END IF;
  INSERT INTO solicitud_derecho (codigo, id_cliente, tipo, estado, detalle)
  VALUES ('SD' || upper(_bb_b36((extract(epoch FROM clock_timestamp()) * 1000)::bigint)), v_c, upper(p_tipo),
          CASE WHEN v_detenido THEN 'EN_PROCESO' ELSE 'RECIBIDA' END, left(nullif(trim(p_detalle), ''), 500))
  RETURNING * INTO d;
  INSERT INTO notificacion (id_cliente, canal, tipo, asunto)
  VALUES (v_c, 'EMAIL', 'DERECHOS', 'Recibimos tu solicitud ' || d.codigo);
  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad, detalle)
  VALUES (u.id_usuario, _bb_rol(u.id_usuario), 'DERECHO_TITULAR', 'solicitud_derecho', d.codigo,
          jsonb_build_object('tipo', d.tipo, 'promocional_detenido', v_detenido));
  RETURN jsonb_build_object('codigo', d.codigo, 'tipo', d.tipo, 'estado', d.estado, 'fecha_limite', d.fecha_limite,
                            'promocional_detenido', v_detenido);
END $function$;

CREATE OR REPLACE FUNCTION public.api_solicitud_responder(p_email text, p_password_sha256 text, p_id bigint, p_estado text, p_respuesta text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
DECLARE u usuario; d solicitud_derecho; v_estado text := upper(p_estado); v_resp text := nullif(trim(p_respuesta), '');
BEGIN
  u := _bb_staff(p_email, p_password_sha256, ARRAY['DUENO']);
  IF v_estado NOT IN ('EN_PROCESO', 'RESPONDIDA', 'RECHAZADA', 'PRORROGADA') THEN RAISE EXCEPTION 'ESTADO_INVALIDO'; END IF;
  SELECT * INTO d FROM solicitud_derecho WHERE id_solicitud = p_id FOR UPDATE;
  IF d.id_solicitud IS NULL THEN RAISE EXCEPTION 'SOLICITUD_NO_ENCONTRADA'; END IF;
  -- DEF-007: la ley permite una sola prórroga por solicitud
  IF v_estado = 'PRORROGADA' AND d.prorrogada_hasta IS NOT NULL THEN RAISE EXCEPTION 'PRORROGA_YA_USADA'; END IF;
  IF v_estado = 'RECHAZADA' THEN
    v_resp := concat_ws(' ', v_resp, 'Puedes reclamar ante la Agencia de Protección de Datos Personales.');
  END IF;
  UPDATE solicitud_derecho SET estado = v_estado, respuesta = left(v_resp, 500),
         atendida_por = u.id_usuario,
         fecha_respuesta = CASE WHEN v_estado IN ('RESPONDIDA', 'RECHAZADA') THEN now() END,
         prorrogada_hasta = CASE WHEN v_estado = 'PRORROGADA' THEN fecha_limite + 30 ELSE prorrogada_hasta END
   WHERE id_solicitud = p_id RETURNING * INTO d;
  INSERT INTO notificacion (id_cliente, canal, tipo, asunto)
  VALUES (d.id_cliente, 'EMAIL', 'DERECHOS', 'Actualización de tu solicitud ' || d.codigo);
  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad, detalle)
  VALUES (u.id_usuario, 'DUENO', 'DERECHO_TITULAR', 'solicitud_derecho', d.codigo, jsonb_build_object('estado', d.estado));
  RETURN jsonb_build_object('codigo', d.codigo, 'estado', d.estado);
END $function$;
