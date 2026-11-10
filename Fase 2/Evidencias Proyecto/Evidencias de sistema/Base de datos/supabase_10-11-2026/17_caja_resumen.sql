-- =====================================================================
-- DEF-016 (UAT-04, 10-11-2026): el cierre del día no incluía las atenciones por orden de llegada.
-- api_caja_resumen entrega, para un rango de fechas (hora de Chile), las atenciones registradas
-- (fila y reservas) por sucursal y por medio de pago, el subtotal Peñaflor y el consolidado.
--  * total_servicios = suma de lo cobrado por los servicios; abonos_webpay = parte pagada antes con Webpay;
--    en_caja = total_servicios - abonos_webpay (lo que se recibió en el local); propinas aparte.
-- Permisos: el Dueño ve todas las sucursales; el cajero/a, solo la suya.
-- =====================================================================
CREATE OR REPLACE FUNCTION public.api_caja_resumen(p_email text, p_password_sha256 text, p_desde date DEFAULT NULL,
                                                   p_hasta date DEFAULT NULL, p_sucursal smallint DEFAULT NULL)
 RETURNS jsonb
 LANGUAGE plpgsql
 VOLATILE SECURITY DEFINER  -- registra la consulta en la auditoría
 SET search_path TO 'public', 'extensions'
AS $function$
DECLARE u usuario; v_dueno boolean; v_suc smallint := p_sucursal;
        v_desde date := coalesce(p_desde, (now() AT TIME ZONE 'America/Santiago')::date);
        v_hasta date := coalesce(p_hasta, p_desde, (now() AT TIME ZONE 'America/Santiago')::date);
BEGIN
  u := _bb_staff(p_email, p_password_sha256, ARRAY['DUENO', 'ADMIN_SUCURSAL'], p_sucursal);
  v_dueno := EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r USING (id_rol) WHERE ur.id_usuario = u.id_usuario AND r.codigo = 'DUENO');
  IF NOT v_dueno THEN
    SELECT ur.id_sucursal INTO v_suc FROM usuario_rol ur JOIN rol r USING (id_rol)
     WHERE ur.id_usuario = u.id_usuario AND r.codigo = 'ADMIN_SUCURSAL' LIMIT 1;
  END IF;
  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad, detalle)
  VALUES (u.id_usuario, _bb_rol(u.id_usuario), 'CONSULTAR_DATO_PERSONAL', 'atencion', 'caja',
          jsonb_build_object('desde', v_desde, 'hasta', v_hasta, 'sucursal', v_suc));
  RETURN (
    WITH a AS (
      SELECT a.*, s.nombre AS sucursal, s.comuna,
             CASE WHEN a.id_turno IS NOT NULL THEN 'FILA' ELSE 'RESERVA' END AS origen
        FROM atencion a JOIN sucursal s USING (id_sucursal)
       WHERE (coalesce(a.fin, a.inicio) AT TIME ZONE 'America/Santiago')::date BETWEEN v_desde AND v_hasta
         AND a.fin IS NOT NULL
         AND (v_suc IS NULL OR a.id_sucursal = v_suc)
    ), tot AS (
      SELECT id_sucursal, sucursal, comuna, count(*) AS atenciones,
             count(*) FILTER (WHERE origen = 'FILA') AS por_llegada, count(*) FILTER (WHERE origen = 'RESERVA') AS con_reserva,
             sum(monto_cobrado) AS total_servicios, sum(abono_aplicado) AS abonos_webpay,
             sum(monto_cobrado - abono_aplicado) AS en_caja, sum(propina) AS propinas,
             jsonb_object_agg_strict(medio, monto) AS por_medio
        FROM (SELECT *, medio_pago_local AS medio,
                     sum(monto_cobrado - abono_aplicado) OVER (PARTITION BY id_sucursal, medio_pago_local) AS monto
                FROM a) x
       GROUP BY id_sucursal, sucursal, comuna
    )
    SELECT jsonb_build_object(
      'desde', v_desde, 'hasta', v_hasta,
      'sucursales', coalesce((SELECT jsonb_agg(to_jsonb(t) ORDER BY t.id_sucursal) FROM tot t), '[]'),
      'subtotal_penaflor', (SELECT jsonb_build_object('atenciones', coalesce(sum(atenciones), 0), 'total_servicios', coalesce(sum(total_servicios), 0),
                                                       'abonos_webpay', coalesce(sum(abonos_webpay), 0), 'en_caja', coalesce(sum(en_caja), 0), 'propinas', coalesce(sum(propinas), 0))
                              FROM tot WHERE comuna ILIKE 'Pe%aflor'),
      'consolidado', (SELECT jsonb_build_object('atenciones', coalesce(sum(atenciones), 0), 'total_servicios', coalesce(sum(total_servicios), 0),
                                                 'abonos_webpay', coalesce(sum(abonos_webpay), 0), 'en_caja', coalesce(sum(en_caja), 0), 'propinas', coalesce(sum(propinas), 0))
                        FROM tot)));
END $function$;

REVOKE ALL ON FUNCTION public.api_caja_resumen(text, text, date, date, smallint) FROM public;
GRANT EXECUTE ON FUNCTION public.api_caja_resumen(text, text, date, date, smallint) TO anon, authenticated;
