-- 22 · Demo (12-11-2026): la restricción reserva_id_barbero_tstzrange_excl rechaza dos reservas
-- cruzadas del mismo barbero. Termina con RAISE EXCEPTION, por lo que no deja nada guardado.
-- Video: demo_reserva_cruzada_12-11-2026.mp4

DO $$
DECLARE c bigint; b int; sv int; su smallint;
  t timestamptz := (date_trunc('day', now() AT TIME ZONE 'America/Santiago') + interval '2 days 10 hours') AT TIME ZONE 'America/Santiago';
BEGIN
  SELECT id_cliente INTO c FROM cliente LIMIT 1;
  SELECT id_barbero, id_sucursal INTO b, su FROM barbero WHERE activo LIMIT 1;
  SELECT id_servicio INTO sv FROM servicio LIMIT 1;
  INSERT INTO reserva (codigo, id_cliente, id_sucursal, id_barbero, id_servicio, inicio, fin, estado, monto_abono, canal)
  VALUES ('DEMO-1', c, su, b, sv, t, t + interval '40 min', 'CONFIRMADA', 3000, 'WEB');
  BEGIN
    INSERT INTO reserva (codigo, id_cliente, id_sucursal, id_barbero, id_servicio, inicio, fin, estado, monto_abono, canal)
    VALUES ('DEMO-2', c, su, b, sv, t + interval '20 min', t + interval '60 min', 'CONFIRMADA', 3000, 'WEB');
  EXCEPTION WHEN exclusion_violation THEN
    RAISE EXCEPTION 'DEMO OK -> la BD rechazó la reserva cruzada (DEMO-1 % a %, DEMO-2 % a %): %',
      to_char(t AT TIME ZONE 'America/Santiago','DD-MM-YYYY HH24:MI'), to_char((t + interval '40 min') AT TIME ZONE 'America/Santiago','HH24:MI'),
      to_char((t + interval '20 min') AT TIME ZONE 'America/Santiago','HH24:MI'), to_char((t + interval '60 min') AT TIME ZONE 'America/Santiago','HH24:MI'), SQLERRM;
  END;
END $$;