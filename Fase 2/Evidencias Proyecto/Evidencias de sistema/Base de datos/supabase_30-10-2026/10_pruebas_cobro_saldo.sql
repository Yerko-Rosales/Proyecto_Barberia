-- =====================================================================
-- Pruebas del cobro del saldo (migración 09_cobro_saldo_reserva.sql).
-- Se ejecutan dentro de una transacción que se deshace al final: no
-- dejan datos. Resultado: una fila por prueba con OK / FALLA.
-- El pago de Webpay se simula con una fila AUTORIZADO en pago (lo que
-- deja api_webpay_confirmar), para no depender de Transbank.
-- Las contraseñas demo del personal no se publican: antes de ejecutar, definir
--   SET pruebas.clave_dueno = '...';  SET pruebas.clave_barbero = '...';  SET pruebas.clave_caja = '...';
-- =====================================================================
BEGIN;
CREATE TEMP TABLE resultado (n serial, prueba text, ok boolean, detalle text) ON COMMIT DROP;

DO $$
DECLARE
  h_admin text := encode(digest(current_setting('pruebas.clave_dueno'), 'sha256'), 'hex');
  h_barb  text := encode(digest(current_setting('pruebas.clave_barbero'), 'sha256'), 'hex');
  h_caja  text := encode(digest(current_setting('pruebas.clave_caja'), 'sha256'), 'hex');
  v jsonb; v_web jsonb; v_sin jsonb; v_bar jsonb; v_otra jsonb; v_ns jsonb; v_cam jsonb;
  a atencion; v_ok boolean; v_antes numeric; v_despues numeric;
BEGIN
  -- Reservas de apoyo (sucursal 1 salvo v_otra)
  -- Corte ($11.900) con abono de $3.000 pagado por Webpay
  v_web := api_crear_reserva('{"id": 1790000200001, "canal": "WEB", "barber_id": 2, "service_name": "Corte", "date_time": "2026-12-10 10:00", "status": "Pendiente", "customer_name": "Prueba Saldo Webpay", "customer_email": "saldo.webpay@correo.cl"}');
  INSERT INTO pago (id_reserva, proveedor, id_transaccion, codigo_autorizacion, tipo_medio, monto, estado)
  SELECT id_reserva, 'WEBPAY', 'PRUEBA-SALDO-1', '1213', 'CREDITO', monto_abono, 'AUTORIZADO' FROM reserva WHERE codigo = v_web->>'codigo';
  UPDATE reserva SET estado = 'CONFIRMADA', expira_en = NULL WHERE codigo = v_web->>'codigo';
  -- Corte confirmado por el Dueño sin abono pagado
  v_sin := api_crear_reserva('{"id": 1790000200002, "canal": "WEB", "barber_id": 2, "service_name": "Corte", "date_time": "2026-12-10 11:00", "status": "Confirmada", "customer_name": "Prueba Saldo Sin Abono"}',
                             'admin@bigboys.cl', h_admin);
  -- Mascarillas Faciales ($2.380): el servicio cuesta menos que el abono
  v_bar := api_crear_reserva('{"id": 1790000200003, "canal": "WEB", "barber_id": 1, "service_name": "Mascarillas Faciales", "date_time": "2026-12-10 12:00", "status": "Pendiente", "customer_name": "Prueba Saldo Cero"}');
  INSERT INTO pago (id_reserva, proveedor, id_transaccion, codigo_autorizacion, tipo_medio, monto, estado)
  SELECT id_reserva, 'WEBPAY', 'PRUEBA-SALDO-3', '1214', 'DEBITO', monto_abono, 'AUTORIZADO' FROM reserva WHERE codigo = v_bar->>'codigo';
  UPDATE reserva SET estado = 'CONFIRMADA', expira_en = NULL WHERE codigo = v_bar->>'codigo';
  -- Sucursal 2 (barbero 3)
  v_otra := api_crear_reserva('{"id": 1790000200004, "canal": "WEB", "barber_id": 3, "service_name": "Corte", "date_time": "2026-12-10 13:00", "status": "Confirmada", "customer_name": "Prueba Saldo Sucursal Dos"}',
                              'admin@bigboys.cl', h_admin);
  -- No-show
  v_ns := api_crear_reserva('{"id": 1790000200005, "canal": "WEB", "barber_id": 1, "service_name": "Corte", "date_time": "2026-12-10 14:00", "status": "Confirmada", "customer_name": "Prueba Saldo No Show"}',
                            'admin@bigboys.cl', h_admin);
  PERFORM api_actualizar_reserva(jsonb_build_object('codigo', v_ns->>'codigo', 'status', 'No-Show'), 'admin@bigboys.cl', h_admin);
  -- Corte reservado; el cliente termina pidiendo Corte + Barba ($20.230)
  v_cam := api_crear_reserva('{"id": 1790000200006, "canal": "WEB", "barber_id": 1, "service_name": "Corte", "date_time": "2026-12-10 15:00", "status": "Pendiente", "customer_name": "Prueba Saldo Cambio"}');
  INSERT INTO pago (id_reserva, proveedor, id_transaccion, codigo_autorizacion, tipo_medio, monto, estado)
  SELECT id_reserva, 'WEBPAY', 'PRUEBA-SALDO-6', '1215', 'CREDITO', monto_abono, 'AUTORIZADO' FROM reserva WHERE codigo = v_cam->>'codigo';
  UPDATE reserva SET estado = 'CONFIRMADA', expira_en = NULL WHERE codigo = v_cam->>'codigo';

  -- C-01 El cajero/a ve el saldo: precio menos el abono pagado
  v := api_reserva_saldo('caja1@bigboys.cl', h_caja, jsonb_build_object('codigo', v_web->>'codigo'));
  INSERT INTO resultado (prueba, ok, detalle)
  VALUES ('C-01 Saldo = precio - abono pagado', (v->>'precio')::numeric = 11900 AND (v->>'abono_pagado')::numeric = 3000
          AND (v->>'saldo')::numeric = 8900 AND NOT (v->>'cobrada')::boolean, (v->>'precio') || ' - ' || (v->>'abono_aplicado') || ' = ' || (v->>'saldo'));

  -- C-02 Se exige el medio de pago si queda saldo por pagar en el local
  BEGIN
    PERFORM api_reserva_cobrar_saldo('caja1@bigboys.cl', h_caja, jsonb_build_object('codigo', v_web->>'codigo'));
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('C-02 Medio de pago obligatorio con saldo', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('C-02 Medio de pago obligatorio con saldo', SQLERRM LIKE '%MEDIO_PAGO_REQUERIDO%', SQLERRM);
  END;

  -- C-03 El cajero/a cobra el saldo: atención con abono aplicado, reserva ATENDIDA, abono APLICADO
  SELECT coalesce(sum(monto_cobrado), 0) INTO v_antes FROM v_atenciones_barbero
   WHERE id_barbero = 2 AND fecha = (now() AT TIME ZONE 'America/Santiago')::date;
  v := api_reserva_cobrar_saldo('caja1@bigboys.cl', h_caja,
         jsonb_build_object('codigo', v_web->>'codigo', 'propina', 1000, 'medio_pago', 'debito'));
  SELECT * INTO a FROM atencion WHERE id_atencion = (v->>'id_atencion')::bigint;
  SELECT a.monto_cobrado = 11900 AND a.abono_aplicado = 3000 AND a.propina = 1000 AND a.medio_pago_local = 'DEBITO'
         AND a.registrada_por = (SELECT id_usuario FROM usuario WHERE email = 'caja1@bigboys.cl')
         AND r.estado = 'ATENDIDA' AND pg.estado = 'APLICADO' AND (v->>'saldo')::numeric = 8900
    INTO v_ok
    FROM reserva r JOIN pago pg USING (id_reserva) WHERE r.codigo = v_web->>'codigo';
  INSERT INTO resultado (prueba, ok, detalle)
  VALUES ('C-03 Cobro del saldo registrado', v_ok, 'saldo ' || (v->>'saldo') || ' en ' || a.medio_pago_local || ', reserva ' || (v->>'estado'));

  -- C-04 El cobro queda en la auditoría con quién lo hizo
  INSERT INTO resultado (prueba, ok, detalle)
  SELECT 'C-04 Cobro auditado', count(*) = 1, count(*) || ' registro(s) ' || coalesce(max(rol), '')
    FROM registro_auditoria WHERE accion = 'CREAR' AND entidad = 'atencion' AND id_entidad = v->>'id_atencion'
     AND detalle->>'reserva' = v_web->>'codigo' AND (detalle->>'saldo')::numeric = 8900;

  -- C-05 Lo cobrado se suma a lo ganado por el barbero en el día (CU-41)
  SELECT coalesce(sum(monto_cobrado), 0) INTO v_despues FROM v_atenciones_barbero
   WHERE id_barbero = 2 AND fecha = (now() AT TIME ZONE 'America/Santiago')::date;
  v := api_mis_atenciones('esteban@bigboys.cl', h_barb, NULL);
  INSERT INTO resultado (prueba, ok, detalle)
  VALUES ('C-05 Atención en lo ganado por el barbero', v_despues - v_antes = 11900 AND (v->>'total')::numeric = v_despues,
          'total del día ' || (v->>'total'));

  -- C-06 Una reserva se cobra una sola vez
  BEGIN
    PERFORM api_reserva_cobrar_saldo('caja1@bigboys.cl', h_caja, jsonb_build_object('codigo', v_web->>'codigo', 'medio_pago', 'EFECTIVO'));
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('C-06 Sin doble cobro', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('C-06 Sin doble cobro', SQLERRM LIKE '%RESERVA_YA_COBRADA%', SQLERRM);
  END;

  -- C-07 Sin abono pagado se cobra el precio completo
  v := api_reserva_cobrar_saldo('admin@bigboys.cl', h_admin, jsonb_build_object('codigo', v_sin->>'codigo', 'medio_pago', 'EFECTIVO'));
  INSERT INTO resultado (prueba, ok, detalle)
  VALUES ('C-07 Sin abono: saldo = precio', (v->>'abono_aplicado')::numeric = 0 AND (v->>'saldo')::numeric = 11900,
          'saldo ' || (v->>'saldo'));

  -- C-08 Servicio más barato que el abono: saldo 0, sin medio de pago
  v := api_reserva_cobrar_saldo('caja1@bigboys.cl', h_caja, jsonb_build_object('codigo', v_bar->>'codigo'));
  SELECT * INTO a FROM atencion WHERE id_atencion = (v->>'id_atencion')::bigint;
  INSERT INTO resultado (prueba, ok, detalle)
  VALUES ('C-08 Abono cubre el servicio', (v->>'saldo')::numeric = 0 AND a.abono_aplicado = 2380 AND a.medio_pago_local IS NULL,
          'abono aplicado ' || a.abono_aplicado || ', saldo ' || (v->>'saldo'));

  -- C-09 Si el servicio final cambia, se cobra el nuevo monto menos el abono
  v := api_reserva_cobrar_saldo('caja1@bigboys.cl', h_caja, jsonb_build_object('codigo', v_cam->>'codigo', 'monto', 20230, 'medio_pago', 'TRANSFERENCIA'));
  INSERT INTO resultado (prueba, ok, detalle)
  VALUES ('C-09 Monto distinto al reservado', (v->>'precio')::numeric = 20230 AND (v->>'saldo')::numeric = 17230,
          (v->>'precio') || ' - ' || (v->>'abono_aplicado') || ' = ' || (v->>'saldo'));

  -- C-10 El barbero no cobra (solo lectura)
  BEGIN
    PERFORM api_reserva_saldo('esteban@bigboys.cl', h_barb, jsonb_build_object('codigo', v_otra->>'codigo'));
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('C-10 Barbero sin acceso al cobro', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('C-10 Barbero sin acceso al cobro', SQLERRM LIKE '%SIN_PERMISO%', SQLERRM);
  END;

  -- C-11 El cajero/a no cobra reservas de otra sucursal
  BEGIN
    PERFORM api_reserva_cobrar_saldo('caja1@bigboys.cl', h_caja, jsonb_build_object('codigo', v_otra->>'codigo', 'medio_pago', 'EFECTIVO'));
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('C-11 Cajero/a limitado a su sucursal', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('C-11 Cajero/a limitado a su sucursal', SQLERRM LIKE '%SIN_PERMISO%', SQLERRM);
  END;

  -- C-12 Un no-show no se cobra (el abono queda retenido)
  BEGIN
    PERFORM api_reserva_cobrar_saldo('admin@bigboys.cl', h_admin, jsonb_build_object('codigo', v_ns->>'codigo', 'medio_pago', 'EFECTIVO'));
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('C-12 No-show no se cobra', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('C-12 No-show no se cobra', SQLERRM LIKE '%RESERVA_NO_COBRABLE%', SQLERRM);
  END;

  -- C-13 Sin credenciales no se consulta ni se cobra
  BEGIN
    PERFORM api_reserva_cobrar_saldo(NULL, NULL, jsonb_build_object('codigo', v_otra->>'codigo', 'medio_pago', 'EFECTIVO'));
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('C-13 Cobro sin credenciales rechazado', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('C-13 Cobro sin credenciales rechazado', SQLERRM LIKE '%CREDENCIALES_INVALIDAS%', SQLERRM);
  END;

  -- C-14 El abono aplicado sigue figurando como pago aprobado
  v := api_webpay_estado(v_web->>'codigo');
  INSERT INTO resultado (prueba, ok, detalle)
  VALUES ('C-14 Webpay: abono aplicado sigue aprobado', (v->>'aprobado')::boolean AND v->>'estado' = 'APLICADO', v->>'estado');
END $$;

SELECT n, CASE WHEN ok THEN 'OK' ELSE 'FALLA' END AS resultado, prueba, detalle FROM resultado ORDER BY n;
ROLLBACK;
