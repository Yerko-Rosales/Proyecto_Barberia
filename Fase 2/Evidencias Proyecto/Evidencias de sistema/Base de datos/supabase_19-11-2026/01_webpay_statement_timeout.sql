-- 19-11-2026: el retorno de Webpay fallaba con "canceling statement due to statement timeout".
-- api_webpay_confirmar llama a Transbank (commit) dentro de la consulta, y Supabase corta las consultas
-- a los 3 s (rol anon) y 8 s (rol authenticated). El commit de Transbank tardó algo más de 3 s, se
-- canceló la consulta y el pago quedó aprobado en Transbank pero sin confirmar en la base.
-- Solución: límite de 25 s solo para las dos funciones que hablan con Transbank. PostgREST aplica el
-- statement_timeout declarado en la función (verificado con una función de prueba de 5 s llamada como anon).

ALTER FUNCTION public.api_webpay_iniciar(jsonb, text)     SET statement_timeout = '25s';
ALTER FUNCTION public.api_webpay_confirmar(text, boolean) SET statement_timeout = '25s';
NOTIFY pgrst, 'reload schema';

-- Segunda causa: la extensión http espera por defecto 5 s (http.timeout_msec = 5000). El commit de
-- Transbank tardó 5,35 s: la base dejó de esperar, consultó el estado antes de que Transbank terminara
-- y registró RECHAZADO un pago que Transbank dejó AUTHORIZED (id_pago 133, Wmwocw2ul).
-- (ALTER FUNCTION ... SET http.timeout_msec no está permitido en Supabase: "permission denied to set
-- parameter". Se usa la opción de curl de la propia extensión, verificada con un servidor que tarda 7 s:
-- sin la opción se corta a los 5.002 ms; con ella responde 200.)
CREATE OR REPLACE FUNCTION public._bb_webpay(p_metodo text, p_ruta text, p_cuerpo jsonb)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
DECLARE resp extensions.http_response;
        v_llave text := '579B532A7440BB0C9079DED94D31EA1615BACEB56610332264630D42D0A36B1C';
BEGIN
  -- La extensión http espera 5 s por defecto y el commit de Transbank puede tardar más:
  -- se amplía a 20 s (dentro del statement_timeout de 25 s de las funciones api_webpay_*).
  PERFORM extensions.http_set_curlopt('CURLOPT_TIMEOUT_MS', '20000');
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
END $function$;

-- Conciliación de los 3 pagos de prueba del 19-11-2026 (autorizados en Transbank, integración):
-- id_pago 130 (Wmwobkzyy) y 131 (Wmwobnrfh) habían quedado ANULADO; 132 (Wmwocjd0v) quedó INICIADO.
-- UPDATE pago SET estado = 'INICIADO' WHERE id_pago IN (130, 131) AND estado = 'ANULADO';
-- SELECT api_webpay_confirmar(id_transaccion, false) FROM pago WHERE id_pago IN (130, 131, 132);
-- Resultado: los 3 pagos AUTORIZADO y sus reservas CONFIRMADA.
-- id_pago 133 (Wmwocw2ul) había quedado RECHAZADO por la espera de 5 s; se concilió igual:
-- UPDATE pago SET estado = 'INICIADO' WHERE id_pago = 133 AND estado = 'RECHAZADO';
-- SELECT api_webpay_confirmar(id_transaccion, false) FROM pago WHERE id_pago = 133;  -> AUTORIZADO / CONFIRMADA
