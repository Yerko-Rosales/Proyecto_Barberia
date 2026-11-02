-- =====================================================================
-- Pruebas de la anticipación mínima (migración 11_anticipacion_minima.sql).
-- Se ejecutan dentro de una transacción que se deshace al final: no
-- dejan datos. Las horas se calculan desde «ahora» en Chile, así la
-- prueba sirve cualquier día. Resultado: una fila por prueba con OK / FALLA.
-- Las contraseñas demo del personal no se publican: antes de ejecutar, definir
--   SET pruebas.clave_dueno = '...';  SET pruebas.clave_caja = '...';
-- =====================================================================
BEGIN;
CREATE TEMP TABLE resultado (n serial, prueba text, ok boolean, detalle text) ON COMMIT DROP;

DO $$
DECLARE
  h_admin text := encode(digest(current_setting('pruebas.clave_dueno'), 'sha256'), 'hex');
  h_caja  text := encode(digest(current_setting('pruebas.clave_caja'), 'sha256'), 'hex');
  -- Hora en Chile, redondeada al minuto, más N minutos, como la envían la web y la app
  t_pasada text := to_char(date_trunc('minute', now() AT TIME ZONE 'America/Santiago') - interval '60 min', 'YYYY-MM-DD HH24:MI');
  t_60     text := to_char(date_trunc('minute', now() AT TIME ZONE 'America/Santiago') + interval '60 min', 'YYYY-MM-DD HH24:MI');
  t_100    text := to_char(date_trunc('minute', now() AT TIME ZONE 'America/Santiago') + interval '100 min', 'YYYY-MM-DD HH24:MI');
  t_lejos  text := to_char(date_trunc('minute', now() AT TIME ZONE 'America/Santiago') + interval '3 days', 'YYYY-MM-DD HH24:MI');
  v jsonb; v_lejos jsonb;
BEGIN
  -- A-01 No se reserva una hora que ya pasó
  BEGIN
    PERFORM api_crear_reserva(jsonb_build_object('id', 1790000400001, 'canal', 'WEB', 'barber_id', 2, 'service_name', 'Corte',
                              'date_time', t_pasada, 'status', 'Pendiente', 'customer_name', 'Prueba Anticipación'));
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('A-01 Hora pasada rechazada', false, 'se permitió ' || t_pasada);
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('A-01 Hora pasada rechazada', SQLERRM LIKE '%ANTICIPACION_MINIMA%', SQLERRM);
  END;

  -- A-02 No se reserva con menos de 90 minutos (app)
  BEGIN
    PERFORM api_crear_reserva(jsonb_build_object('id', 1790000400002, 'canal', 'APP', 'barber_id', 2, 'service_name', 'Corte',
                              'date_time', t_60, 'status', 'Pendiente', 'customer_name', 'Prueba Anticipación'));
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('A-02 Menos de 90 min rechazada', false, 'se permitió ' || t_60);
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('A-02 Menos de 90 min rechazada', SQLERRM LIKE '%ANTICIPACION_MINIMA: 90%', SQLERRM);
  END;

  -- A-03 La regla también aplica al Dueño (reserva web confirmada sin pago)
  BEGIN
    PERFORM api_crear_reserva(jsonb_build_object('id', 1790000400003, 'canal', 'WEB', 'barber_id', 2, 'service_name', 'Corte',
                              'date_time', t_60, 'status', 'Confirmada', 'customer_name', 'Prueba Anticipación'), 'admin@bigboys.cl', h_admin);
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('A-03 Regla aplica al Dueño', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('A-03 Regla aplica al Dueño', SQLERRM LIKE '%ANTICIPACION_MINIMA%', SQLERRM);
  END;

  -- A-04 Con 90 minutos o más sí se reserva
  v := api_crear_reserva(jsonb_build_object('id', 1790000400004, 'canal', 'WEB', 'barber_id', 2, 'service_name', 'Corte',
                         'date_time', t_100, 'status', 'Pendiente', 'customer_name', 'Prueba Anticipación'));
  INSERT INTO resultado (prueba, ok, detalle) VALUES ('A-04 Con 100 min se permite', v->>'status' = 'Pendiente', t_100 || ' ' || (v->>'status'));

  -- A-05 La atención presencial (LOCAL) del cajero/a no se valida
  v := api_crear_reserva(jsonb_build_object('id', 1790000400005, 'canal', 'LOCAL', 'barber_id', 1, 'service_name', 'Corte',
                         'date_time', t_pasada, 'status', 'Completada', 'customer_name', 'Prueba Anticipación'), 'caja1@bigboys.cl', h_caja);
  INSERT INTO resultado (prueba, ok, detalle) VALUES ('A-05 Atención LOCAL sin restricción', v->>'status' = 'Completada', v->>'status');

  -- A-06 Reagendar (Dueño) a una hora con menos de 90 minutos se rechaza
  v_lejos := api_crear_reserva(jsonb_build_object('id', 1790000400006, 'canal', 'WEB', 'barber_id', 1, 'service_name', 'Corte',
                               'date_time', t_lejos, 'status', 'Confirmada', 'customer_name', 'Prueba Anticipación'), 'admin@bigboys.cl', h_admin);
  BEGIN
    PERFORM api_actualizar_reserva(jsonb_build_object('codigo', v_lejos->>'codigo', 'new_date_time', t_60), 'admin@bigboys.cl', h_admin);
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('A-06 Reagendar a menos de 90 min rechazado', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN
    INSERT INTO resultado (prueba, ok, detalle) VALUES ('A-06 Reagendar a menos de 90 min rechazado', SQLERRM LIKE '%ANTICIPACION_MINIMA%', SQLERRM);
  END;

  -- A-07 Cambiar el estado de una reserva ya tomada no se ve afectado
  v := api_actualizar_reserva(jsonb_build_object('codigo', v_lejos->>'codigo', 'status', 'Cancelada'), 'admin@bigboys.cl', h_admin);
  INSERT INTO resultado (prueba, ok, detalle) VALUES ('A-07 Cambio de estado sin restricción', v->>'status' LIKE 'Cancelada%', v->>'status');
END $$;

SELECT n, CASE WHEN ok THEN 'OK' ELSE 'FALLA' END AS resultado, prueba, detalle FROM resultado ORDER BY n;
ROLLBACK;
