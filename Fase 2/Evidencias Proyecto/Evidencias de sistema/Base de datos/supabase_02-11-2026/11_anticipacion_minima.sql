-- =====================================================================
-- Big Boys · S13 (02-11-2026) · Anticipación mínima para reservar
--  Antes se podía reservar (o reagendar a) una hora de hoy que ya había
--  pasado. Ahora una reserva web o app debe empezar al menos
--  MINUTOS_ANTICIPACION_RESERVA minutos después de este momento (90 por
--  defecto, parámetro del negocio en configuracion).
--  Se valida con un trigger sobre reserva, así aplica a todas las vías:
--  api_crear_reserva, api_webpay_iniciar y el reagendamiento de
--  api_actualizar_reserva. Las atenciones presenciales (canal LOCAL) no
--  se validan: se registran cuando el cliente ya está en el local.
-- Migración aplicada: s13_anticipacion_minima
-- Pruebas: 12_pruebas_anticipacion.sql
-- =====================================================================

INSERT INTO configuracion (clave, ambito, tipo_valor, valor)
SELECT 'MINUTOS_ANTICIPACION_RESERVA', 'GLOBAL', 'ENTERO', '90'
WHERE NOT EXISTS (SELECT 1 FROM configuracion WHERE clave = 'MINUTOS_ANTICIPACION_RESERVA' AND ambito = 'GLOBAL');

CREATE OR REPLACE FUNCTION _bb_validar_anticipacion() RETURNS trigger
LANGUAGE plpgsql SET search_path = public AS $$
DECLARE v_min int := coalesce(_bb_cfg('MINUTOS_ANTICIPACION_RESERVA')::int, 90);
BEGIN
  IF NEW.canal <> 'LOCAL'
     AND (TG_OP = 'INSERT' OR NEW.inicio IS DISTINCT FROM OLD.inicio)
     AND NEW.inicio < now() + make_interval(mins => v_min) THEN
    RAISE EXCEPTION 'ANTICIPACION_MINIMA: %', v_min;
  END IF;
  RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_reserva_anticipacion ON reserva;
CREATE TRIGGER trg_reserva_anticipacion
  BEFORE INSERT OR UPDATE OF inicio ON reserva
  FOR EACH ROW EXECUTE FUNCTION _bb_validar_anticipacion();

REVOKE ALL ON FUNCTION _bb_validar_anticipacion() FROM PUBLIC, anon, authenticated;
