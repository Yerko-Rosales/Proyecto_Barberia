-- =====================================================================
-- Pruebas de seguridad (migración 07_seguridad_reservas.sql). Se ejecutan
-- dentro de una transacción que se deshace al final: no dejan datos.
-- Resultado: una fila por prueba con OK / FALLA.
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
  h_test  text := encode(digest('123', 'sha256'), 'hex');
  h_mala  text := encode(digest('clave-incorrecta', 'sha256'), 'hex');
  v jsonb; v_pend jsonb; v_otra jsonb; v_ok boolean; i int;
  -- Reserva pendiente de un invitado (sucursal 1, barbero 2)
  p_pend jsonb := '{"id": 1790000100001, "canal": "WEB", "barber_id": 2, "service_name": "Corte", "date_time": "2026-12-01 11:00", "status": "Pendiente", "customer_name": "Invitado Seguridad", "customer_phone": "+56911112222", "customer_email": "invitado.seguridad@correo.cl"}';
  -- Reserva confirmada en la sucursal 2 (barbero 3), creada por el Dueño
  p_otra jsonb := '{"id": 1790000100002, "canal": "WEB", "barber_id": 3, "service_name": "Corte", "date_time": "2026-12-02 11:00", "status": "Confirmada", "customer_name": "Cliente Sucursal Dos", "customer_phone": "+56933334444", "customer_email": "sucursal.dos@correo.cl"}';
BEGIN
  -- S-01 Sin cuenta de gestión no se crea una reserva confirmada sin pagar
  BEGIN
    PERFORM api_crear_reserva('{"id": 1790000100003, "canal": "WEB", "barber_id": 2, "service_name": "Corte", "date_time": "2026-12-01 13:00", "status": "Confirmada", "customer_name": "Sin Pago"}');
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-01 Reserva confirmada sin pago rechazada', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-01 Reserva confirmada sin pago rechazada', SQLERRM LIKE '%PAGO_REQUERIDO%', SQLERRM);
  END;

  -- S-02 Sin cuenta de gestión no se registra una atención presencial (LOCAL)
  BEGIN
    PERFORM api_crear_reserva('{"id": 1790000100004, "canal": "LOCAL", "barber_id": 2, "service_name": "Corte", "date_time": "2026-12-01 14:00", "status": "Completada", "customer_name": "Sin Cuenta"}');
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-02 Atención LOCAL anónima rechazada', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-02 Atención LOCAL anónima rechazada', SQLERRM LIKE '%PAGO_REQUERIDO%', SQLERRM);
  END;

  -- S-03 El cajero/a registra una atención LOCAL en su sucursal
  v := api_crear_reserva('{"id": 1790000100005, "canal": "LOCAL", "barber_id": 1, "service_name": "Corte", "date_time": "2026-12-01 15:00", "status": "Completada", "customer_name": "Presencial"}',
                         'caja1@bigboys.cl', h_caja);
  INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-03 Cajero/a registra atención LOCAL', v->>'status' = 'Completada', v->>'status');

  -- Reservas de apoyo
  v_pend := api_crear_reserva(p_pend);
  v_otra := api_crear_reserva(p_otra, 'admin@bigboys.cl', h_admin);

  -- S-04 Sin credenciales ni correo no se cancela una reserva ajena
  BEGIN
    PERFORM api_actualizar_reserva(jsonb_build_object('codigo', v_pend->>'codigo', 'status', 'Cancelada'));
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-04 Cancelar reserva ajena sin credenciales', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-04 Cancelar reserva ajena sin credenciales', SQLERRM LIKE '%SIN_PERMISO%', SQLERRM);
  END;

  -- S-05 El invitado no puede confirmar su reserva sin pagar
  BEGIN
    PERFORM api_actualizar_reserva(jsonb_build_object('codigo', v_pend->>'codigo', 'status', 'Confirmada', 'customer_email', 'invitado.seguridad@correo.cl'));
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-05 Invitado no confirma sin pagar', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-05 Invitado no confirma sin pagar', SQLERRM LIKE '%SIN_PERMISO%', SQLERRM);
  END;

  -- S-06 Otro cliente registrado no puede cancelar la reserva
  BEGIN
    PERFORM api_actualizar_reserva(jsonb_build_object('codigo', v_pend->>'codigo', 'status', 'Cancelada'), 'test@bigboys.cl', h_test);
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-06 Otro cliente no cancela reserva ajena', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-06 Otro cliente no cancela reserva ajena', SQLERRM LIKE '%SIN_PERMISO%', SQLERRM);
  END;

  -- S-07 El barbero no cambia estados de reservas (solo lectura)
  BEGIN
    PERFORM api_actualizar_reserva(jsonb_build_object('codigo', v_otra->>'codigo', 'status', 'No-Show'), 'mateo@bigboys.cl', h_barb);
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-07 Barbero no cambia estados', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-07 Barbero no cambia estados', SQLERRM LIKE '%SIN_PERMISO%', SQLERRM);
  END;

  -- S-08 El cajero/a no gestiona reservas de otra sucursal
  BEGIN
    PERFORM api_actualizar_reserva(jsonb_build_object('codigo', v_otra->>'codigo', 'status', 'Cancelada'), 'caja1@bigboys.cl', h_caja);
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-08 Cajero/a limitado a su sucursal', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-08 Cajero/a limitado a su sucursal', SQLERRM LIKE '%SIN_PERMISO%', SQLERRM);
  END;

  -- S-09 El invitado cancela su propia reserva indicando su correo
  v := api_actualizar_reserva(jsonb_build_object('codigo', v_pend->>'codigo', 'status', 'Cancelada', 'customer_email', 'Invitado.Seguridad@correo.cl'));
  INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-09 Invitado cancela su reserva con su correo', v->>'status' LIKE 'Cancelada%', v->>'status');

  -- S-10 El Dueño marca la inasistencia y queda en la auditoría
  v := api_actualizar_reserva(jsonb_build_object('codigo', v_otra->>'codigo', 'status', 'No-Show'), 'admin@bigboys.cl', h_admin);
  INSERT INTO resultado (prueba, ok, detalle)
  SELECT 'S-10 Dueño cambia estado con auditoría', v->>'status' LIKE 'No-Show%' AND EXISTS (
           SELECT 1 FROM registro_auditoria WHERE entidad = 'reserva' AND id_entidad = v_otra->>'codigo' AND detalle->>'por' = 'GESTOR'),
         v->>'status';

  -- S-11 El barbero no ve teléfono ni correo de los clientes
  SELECT bool_and(x->>'customer_phone' = '' AND x->>'customer_email' = '') INTO v_ok
  FROM jsonb_array_elements(api_listar_reservas('WEB', 'mateo@bigboys.cl', h_barb)) x;
  INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-11 Barbero sin datos de contacto', v_ok, 'contacto oculto');

  -- S-12 El cajero/a no ve el contacto de otra sucursal; el Dueño sí
  INSERT INTO resultado (prueba, ok, detalle)
  SELECT 'S-12 Contacto según sucursal',
         (SELECT x->>'customer_phone' = '' FROM jsonb_array_elements(api_listar_reservas('WEB', 'caja1@bigboys.cl', h_caja)) x
           WHERE x->>'codigo' = v_otra->>'codigo')
         AND (SELECT x->>'customer_phone' = '+56933334444' FROM jsonb_array_elements(api_listar_reservas('WEB', 'admin@bigboys.cl', h_admin)) x
           WHERE x->>'codigo' = v_otra->>'codigo'), 'cajero/a oculto, Dueño visible';

  -- S-13 Reservar con el correo de una cuenta registrada no devuelve sus datos
  v := api_crear_reserva('{"id": 1790000100006, "canal": "WEB", "barber_id": 2, "service_name": "Corte", "date_time": "2026-12-01 16:00", "status": "Pendiente", "customer_name": "X", "customer_email": "test@bigboys.cl"}');
  INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-13 Sin datos de una cuenta ajena', v->>'customer_email' = '' AND v->>'customer_phone' = '', v->>'customer_name');

  -- S-14 Cinco intentos fallidos bloquean la cuenta 15 min, sin dejarla BLOQUEADA para siempre
  FOR i IN 1..5 LOOP PERFORM api_login('test@bigboys.cl', h_mala); END LOOP;
  INSERT INTO resultado (prueba, ok, detalle)
  SELECT 'S-14 Bloqueo temporal tras 5 intentos', api_login('test@bigboys.cl', h_test) IS NULL
         AND (SELECT estado FROM usuario WHERE email = 'test@bigboys.cl') = 'ACTIVO', 'clave correcta rechazada; cuenta ACTIVA';

  -- S-15 Durante el bloqueo ninguna otra función acepta la clave
  BEGIN
    PERFORM api_mis_datos('test@bigboys.cl', h_test, 'ACCESO');
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-15 Bloqueo aplica a toda la API', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('S-15 Bloqueo aplica a toda la API', true, SQLERRM);
  END;

  -- S-16 Pasado el plazo se vuelve a ingresar y se reinicia el contador
  --      (la auditoría es de solo inserción: se simula el paso del tiempo con un plazo de 0 min)
  UPDATE configuracion SET valor = '0' WHERE clave = 'MINUTOS_BLOQUEO_LOGIN' AND ambito = 'GLOBAL';
  v_ok := api_login('test@bigboys.cl', h_test) IS NOT NULL;
  INSERT INTO resultado (prueba, ok, detalle)
  SELECT 'S-16 Ingreso tras el bloqueo', v_ok AND intentos_fallidos = 0, 'contador en ' || intentos_fallidos
  FROM usuario WHERE email = 'test@bigboys.cl';
END $$;

SELECT n, CASE WHEN ok THEN 'OK' ELSE 'FALLA' END AS resultado, prueba, detalle FROM resultado ORDER BY n;
ROLLBACK;
