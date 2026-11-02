-- =====================================================================
-- Pruebas de la API S12 (reglas y permisos). Se ejecutan dentro de una
-- transacción que se deshace al final: no dejan datos en la base.
-- Resultado: una fila por prueba con OK / FALLA.
-- Las contraseñas demo del personal no se publican: antes de ejecutar, definir
--   SET pruebas.clave_dueno = '...';  SET pruebas.clave_barbero = '...';
-- =====================================================================
BEGIN;
CREATE TEMP TABLE resultado (n serial, prueba text, ok boolean, detalle text) ON COMMIT DROP;

DO $$
DECLARE
  h_admin text := encode(digest(current_setting('pruebas.clave_dueno'), 'sha256'), 'hex');
  h_barb  text := encode(digest(current_setting('pruebas.clave_barbero'), 'sha256'), 'hex');
  h_test  text := encode(digest('123', 'sha256'), 'hex');
  h_tmp   text := encode(digest('Temporal-Prueba1', 'sha256'), 'hex');
  v jsonb; v2 jsonb; v_err text; v_id bigint; v_turno bigint; v_caja2 bigint;
  PROCEDURE_OK boolean;
BEGIN
  -- P-01 Reserva PENDIENTE_PAGO bloquea el horario (exclusión) y vence a los 15 min
  v := api_crear_reserva('{"id": 1790000000001, "canal": "WEB", "barber_id": 2, "service_name": "Corte", "date_time": "2026-11-03 10:00", "status": "Pendiente", "customer_name": "Prueba A"}');
  SELECT expira_en BETWEEN now() + interval '14 min' AND now() + interval '16 min' INTO PROCEDURE_OK FROM reserva WHERE codigo = v->>'codigo';
  INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-01 Reserva pendiente con vencimiento de 15 min', PROCEDURE_OK, v->>'status');

  BEGIN
    PERFORM api_crear_reserva('{"id": 1790000000002, "canal": "WEB", "barber_id": 2, "service_name": "Corte", "date_time": "2026-11-03 10:00", "status": "Pendiente", "customer_name": "Prueba B"}');
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-02 Mismo horario rechazado mientras se paga', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-02 Mismo horario rechazado mientras se paga', SQLERRM LIKE '%HORARIO_OCUPADO%', SQLERRM);
  END;

  -- P-03 Al vencer el plazo, la reserva pasa a EXPIRADA y libera el horario
  UPDATE reserva SET expira_en = now() - interval '1 min' WHERE codigo = v->>'codigo';
  -- (desde 07_seguridad_reservas solo el Dueño o el cajero/a confirman sin pasar por Webpay)
  v2 := api_crear_reserva('{"id": 1790000000003, "canal": "WEB", "barber_id": 2, "service_name": "Corte", "date_time": "2026-11-03 10:00", "customer_name": "Prueba C"}',
                          'admin@bigboys.cl', h_admin);
  INSERT INTO resultado (prueba, ok, detalle)
  SELECT 'P-03 Pendiente vencida se libera', (SELECT estado FROM reserva WHERE codigo = v->>'codigo') = 'EXPIRADA' AND v2->>'status' = 'Confirmada',
         (SELECT estado FROM reserva WHERE codigo = v->>'codigo') || ' / nueva ' || (v2->>'status');

  -- P-04 Webpay: token inexistente
  BEGIN
    PERFORM api_webpay_confirmar('token-que-no-existe');
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-04 Confirmar token inexistente', false, 'sin error');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-04 Confirmar token inexistente', SQLERRM LIKE '%PAGO_NO_ENCONTRADO%', SQLERRM);
  END;

  -- P-05 Webpay: return_url inválida
  BEGIN
    PERFORM api_webpay_iniciar('{"barber_id": 2, "service_name": "Corte", "date_time": "2026-11-03 12:00"}', 'javascript:alert(1)');
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-05 return_url inválida', false, 'sin error');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-05 return_url inválida', SQLERRM LIKE '%RETURN_URL_INVALIDA%', SQLERRM);
  END;

  -- P-06 Personal: solo el Dueño crea cuentas (un barbero no puede)
  BEGIN
    PERFORM api_personal_crear('mateo@bigboys.cl', h_barb, jsonb_build_object('full_name', 'X', 'email', 'x@bigboys.cl', 'role', 'BARBERO', 'location_id', 1, 'temp_password_sha256', h_tmp));
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-06 Barbero no puede crear personal', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-06 Barbero no puede crear personal', SQLERRM LIKE '%SIN_PERMISO%', SQLERRM);
  END;

  -- P-07 El Dueño crea un cajero/a de la sucursal 2 con clave temporal (debe cambiarla)
  v := api_personal_crear('admin@bigboys.cl', h_admin, jsonb_build_object('full_name', 'Caja Prueba', 'email', 'caja.prueba@bigboys.cl', 'role', 'ADMIN_SUCURSAL', 'location_id', 2, 'temp_password_sha256', h_tmp));
  v_caja2 := (v->>'id')::bigint;
  v2 := api_login('caja.prueba@bigboys.cl', h_tmp);
  INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-07 Alta de cajero/a con cambio de clave obligatorio',
    (v2->>'must_change_password')::boolean AND (v2->>'location_id')::int = 2, v2->>'role');

  -- P-08 El cajero/a de la sucursal 2 no puede operar la fila de la sucursal 1
  BEGIN
    PERFORM api_fila_registrar('caja.prueba@bigboys.cl', h_tmp, '{"location_id": 1, "nombre": "Ana"}');
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-08 Cajero/a limitado a su sucursal', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-08 Cajero/a limitado a su sucursal', SQLERRM LIKE '%SIN_PERMISO%', SQLERRM);
  END;

  -- P-09 Fila: números correlativos por sucursal y día
  v := api_fila_registrar('caja.prueba@bigboys.cl', h_tmp, '{"location_id": 2, "nombre": "Ana Prueba"}');
  v2 := api_fila_registrar('caja.prueba@bigboys.cl', h_tmp, '{"location_id": 2, "nombre": "Beto Prueba"}');
  v_turno := (v->>'id')::bigint;
  INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-09 Números correlativos en la fila',
    (v2->>'numero')::int = (v->>'numero')::int + 1, (v->>'numero') || ' → ' || (v2->>'numero'));

  -- P-10 No se puede terminar una atención que no empezó
  BEGIN
    PERFORM api_fila_actualizar('caja.prueba@bigboys.cl', h_tmp, v_turno, 'TERMINAR', '{}');
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-10 Transición inválida de la fila', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-10 Transición inválida de la fila', SQLERRM LIKE '%ACCION_INVALIDA%', SQLERRM);
  END;

  -- P-11 Iniciar crea la atención y terminar registra monto y medio de pago
  PERFORM api_fila_actualizar('caja.prueba@bigboys.cl', h_tmp, v_turno, 'INICIAR', '{"barber_id": 3}');
  PERFORM api_fila_actualizar('caja.prueba@bigboys.cl', h_tmp, v_turno, 'TERMINAR', '{"monto": 15000, "propina": 500, "medio_pago": "CREDITO"}');
  INSERT INTO resultado (prueba, ok, detalle)
  SELECT 'P-11 Atención registrada con monto y medio de pago',
         a.monto_cobrado = 15000 AND a.medio_pago_local = 'CREDITO' AND a.registrada_por = v_caja2 AND t.estado = 'ATENDIDO',
         a.monto_cobrado || ' ' || a.medio_pago_local
  FROM atencion a JOIN turno_fila t USING (id_turno) WHERE a.id_turno = v_turno;

  -- P-12 La vista pública no muestra nombres completos
  v := api_fila_listar(2::smallint);
  INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-12 Fila pública con nombres abreviados',
    v::text NOT LIKE '%Ana Prueba%' AND v::text LIKE '%Ana P.%', (v->>'en_espera') || ' en espera');

  -- P-13 El barbero solo ve lo suyo (Cristian, sucursal 2)
  v := api_mis_atenciones('cristian@bigboys.cl', h_barb);
  INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-13 Barbero ve sus atenciones y el total',
    (v->>'total')::numeric >= 15000 AND v::text NOT LIKE '%Ana%', 'total ' || (v->>'total'));

  -- P-14 Un cliente no puede ver el panel del barbero
  BEGIN
    PERFORM api_mis_atenciones('test@bigboys.cl', h_test);
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-14 Cliente sin acceso a atenciones del barbero', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-14 Cliente sin acceso a atenciones del barbero', SQLERRM LIKE '%SIN_PERMISO%', SQLERRM);
  END;

  -- P-15 «Mis datos»: acceso registra la solicitud respondida y deja auditoría
  v := api_mis_datos('test@bigboys.cl', h_test, 'PORTABILIDAD');
  INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-15 Portabilidad de datos del titular',
    v->'cuenta'->>'email' = 'test@bigboys.cl' AND jsonb_typeof(v->'reservas') = 'array'
      AND EXISTS (SELECT 1 FROM registro_auditoria WHERE accion = 'EXPORTAR' AND ocurrido_en > now() - interval '1 min'),
    jsonb_array_length(v->'solicitudes') || ' solicitudes');

  -- P-16 Solicitud de oposición con plazo legal de 30 días y aviso por correo
  v := api_solicitud_derecho('test@bigboys.cl', h_test, 'OPOSICION', 'Prueba');
  INSERT INTO resultado (prueba, ok, detalle)
  SELECT 'P-16 Solicitud con plazo de 30 días y correo encolado',
         (v->>'fecha_limite')::date = (now() AT TIME ZONE 'America/Santiago')::date + 30
           AND EXISTS (SELECT 1 FROM notificacion WHERE tipo = 'DERECHOS' AND asunto LIKE '%' || (v->>'codigo')),
         v->>'codigo' || ' vence ' || (v->>'fecha_limite');

  -- P-17 Consentimiento promocional: otorgar y revocar
  PERFORM api_consentimiento('test@bigboys.cl', h_test, true);
  PERFORM api_consentimiento('test@bigboys.cl', h_test, false);
  INSERT INTO resultado (prueba, ok, detalle)
  SELECT 'P-17 Revocar consentimiento promocional',
         NOT EXISTS (SELECT 1 FROM consentimiento k JOIN cliente c USING (id_cliente) JOIN usuario u USING (id_usuario)
                     WHERE u.email = 'test@bigboys.cl' AND k.finalidad = 'PROMOCIONAL' AND k.revocado_en IS NULL), 'revocado';

  -- P-18 Bloquear al colaborador impide su ingreso
  PERFORM api_personal_actualizar('admin@bigboys.cl', h_admin, v_caja2, false);
  INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-18 Cuenta bloqueada no puede ingresar',
    api_login('caja.prueba@bigboys.cl', h_tmp) IS NULL, 'login NULL');

  -- P-19 Cola de correos: sin la clave del servidor no se entrega nada
  BEGIN
    PERFORM api_correo_pendientes('clave-falsa');
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-19 Cola de correos protegida', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-19 Cola de correos protegida', SQLERRM LIKE '%SIN_PERMISO%', SQLERRM);
  END;

  -- P-20 Con la clave correcta, la cola entrega los correos pendientes con su destinatario
  v := api_correo_pendientes(_bb_cfg('CLAVE_SERVICIO_CORREO'));
  INSERT INTO resultado (prueba, ok, detalle) VALUES ('P-20 Cola de correos con destinatario',
    jsonb_array_length(v) > 0 AND v->0->>'to' IS NOT NULL, jsonb_array_length(v) || ' pendientes');
END $$;

SELECT n, CASE WHEN ok THEN 'OK' ELSE 'FALLA' END AS resultado, prueba, detalle FROM resultado ORDER BY n;
ROLLBACK;
