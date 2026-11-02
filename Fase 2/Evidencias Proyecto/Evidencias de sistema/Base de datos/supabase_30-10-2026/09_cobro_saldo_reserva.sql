-- =====================================================================
-- Big Boys · S12 (30-10-2026) · Cobro del saldo de una reserva atendida
--  Antes, marcar una reserva «Atendida» solo cambiaba su estado: no se
--  creaba la atención, no se descontaba el abono pagado por Webpay y no
--  quedaba registrado el saldo ni con qué se pagó en el local.
--  1. api_reserva_saldo: precio del servicio, abono pagado y saldo por
--     cobrar (para mostrarlo al cajero/a antes de cobrar).
--  2. api_reserva_cobrar_saldo: el Dueño o el cajero/a de la sucursal
--     registra la atención (monto total, abono aplicado, propina y medio
--     de pago del saldo), deja el abono como APLICADO, la reserva como
--     ATENDIDA y el registro en auditoría (CREAR atencion). Una reserva
--     se cobra una sola vez.
--  3. api_webpay_estado considera aprobado también el abono ya aplicado.
-- Migraciones aplicadas: s12_cobro_saldo_reserva y s12_cobro_saldo_reserva_auditoria
-- Pruebas: 10_pruebas_cobro_saldo.sql (14/14 OK)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Helper: precio, abono pagado y saldo de una reserva
--   saldo = monto total - abono aplicado (el abono no supera el monto)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION _bb_saldo_reserva(r reserva, p_monto numeric DEFAULT NULL) RETURNS jsonb
LANGUAGE plpgsql STABLE SET search_path = public AS $$
DECLARE v_precio numeric; v_pagado numeric; v_aplicado numeric;
BEGIN
  v_precio := coalesce(p_monto, _bb_precio(r.id_servicio, r.id_sucursal), 0);
  SELECT coalesce(sum(monto), 0) INTO v_pagado
    FROM pago WHERE id_reserva = r.id_reserva AND estado IN ('AUTORIZADO', 'APLICADO');
  v_aplicado := least(v_pagado, v_precio);
  RETURN jsonb_build_object(
    'codigo', r.codigo,
    'estado', r.estado,
    'servicio', (SELECT nombre FROM servicio WHERE id_servicio = r.id_servicio),
    'precio', v_precio,
    'abono_pagado', v_pagado,
    'abono_aplicado', v_aplicado,
    'saldo', v_precio - v_aplicado,
    'cobrada', EXISTS (SELECT 1 FROM atencion WHERE id_reserva = r.id_reserva));
END $$;

-- ---------------------------------------------------------------------
-- 1. Consultar el saldo (Dueño o cajero/a de la sucursal)
--    p: identificación de la reserva (codigo | id | barber_id + date_time)
--       y opcionalmente monto (si el servicio final cambió de precio)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION api_reserva_saldo(p_email text, p_password_sha256 text, p jsonb) RETURNS jsonb
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE r reserva;
BEGIN
  r := _bb_buscar_reserva(p);
  IF r.id_reserva IS NULL THEN RAISE EXCEPTION 'RESERVA_NO_ENCONTRADA'; END IF;
  PERFORM _bb_staff(p_email, p_password_sha256, ARRAY['DUENO', 'ADMIN_SUCURSAL'], r.id_sucursal);
  RETURN _bb_saldo_reserva(r, nullif(p->>'monto', '')::numeric);
END $$;

-- ---------------------------------------------------------------------
-- 2. Cobrar el saldo y cerrar la reserva
--    p: identificación de la reserva + monto (vacío = precio del servicio),
--       propina y medio_pago (EFECTIVO, DEBITO, CREDITO o TRANSFERENCIA;
--       obligatorio si queda algo por pagar en el local)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION api_reserva_cobrar_saldo(p_email text, p_password_sha256 text, p jsonb) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, extensions AS $$
DECLARE r reserva; u usuario; v_saldo jsonb; v_monto numeric; v_propina numeric; v_medio text; v_id bigint;
BEGIN
  r := _bb_buscar_reserva(p);
  IF r.id_reserva IS NULL THEN RAISE EXCEPTION 'RESERVA_NO_ENCONTRADA'; END IF;
  SELECT * INTO r FROM reserva WHERE id_reserva = r.id_reserva FOR UPDATE;
  u := _bb_staff(p_email, p_password_sha256, ARRAY['DUENO', 'ADMIN_SUCURSAL'], r.id_sucursal);

  IF EXISTS (SELECT 1 FROM atencion WHERE id_reserva = r.id_reserva) THEN RAISE EXCEPTION 'RESERVA_YA_COBRADA'; END IF;
  -- Solo una reserva tomada (o atendida sin cobro registrado); no-show y anulaciones no se cobran
  IF r.estado NOT IN ('CONFIRMADA', 'ATENDIDA') THEN RAISE EXCEPTION 'RESERVA_NO_COBRABLE: %', r.estado; END IF;

  v_monto := nullif(p->>'monto', '')::numeric;
  v_propina := coalesce(nullif(p->>'propina', '')::numeric, 0);
  IF coalesce(v_monto, 0) < 0 OR v_propina < 0 THEN RAISE EXCEPTION 'MONTO_INVALIDO'; END IF;
  v_saldo := _bb_saldo_reserva(r, v_monto);
  v_monto := (v_saldo->>'precio')::numeric;

  v_medio := upper(coalesce(p->>'medio_pago', ''));
  IF v_medio NOT IN ('EFECTIVO', 'DEBITO', 'CREDITO', 'TRANSFERENCIA') THEN
    IF (v_saldo->>'saldo')::numeric + v_propina > 0 THEN RAISE EXCEPTION 'MEDIO_PAGO_REQUERIDO'; END IF;
    v_medio := NULL;
  END IF;

  INSERT INTO atencion (id_reserva, id_cliente, id_sucursal, id_barbero, id_servicio, inicio, fin,
                        monto_cobrado, abono_aplicado, propina, medio_pago_local, registrada_por)
  VALUES (r.id_reserva, r.id_cliente, r.id_sucursal, r.id_barbero, r.id_servicio, least(r.inicio, now()), now(),
          v_monto, (v_saldo->>'abono_aplicado')::numeric, v_propina, v_medio, u.id_usuario)
  RETURNING id_atencion INTO v_id;

  UPDATE pago SET estado = 'APLICADO', actualizado_en = now()
   WHERE id_reserva = r.id_reserva AND estado = 'AUTORIZADO';
  UPDATE reserva SET estado = 'ATENDIDA', actualizado_en = now()
   WHERE id_reserva = r.id_reserva RETURNING * INTO r;

  INSERT INTO registro_auditoria (id_usuario, rol, accion, entidad, id_entidad, detalle)
  VALUES (u.id_usuario, _bb_rol(u.id_usuario), 'CREAR', 'atencion', v_id::text,
          jsonb_build_object('cobro', 'SALDO_RESERVA', 'reserva', r.codigo, 'monto', v_monto, 'abono_aplicado', v_saldo->'abono_aplicado',
                             'saldo', v_saldo->'saldo', 'propina', v_propina, 'medio_pago', v_medio));

  RETURN _bb_saldo_reserva(r, v_monto) || jsonb_build_object('id_atencion', v_id, 'propina', v_propina,
                                                             'medio_pago', v_medio, 'estado', r.estado);
END $$;

-- ---------------------------------------------------------------------
-- 3. El abono aplicado al servicio sigue contando como pago aprobado
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION api_webpay_estado(p_codigo text) RETURNS jsonb
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT jsonb_build_object('codigo', r.codigo, 'reserva_estado', r.estado,
                            'estado', pg.estado, 'aprobado', pg.estado IN ('AUTORIZADO', 'APLICADO'),
                            'autorizacion', pg.codigo_autorizacion, 'monto', pg.monto)
  FROM reserva r LEFT JOIN LATERAL (SELECT * FROM pago WHERE id_reserva = r.id_reserva ORDER BY creado_en DESC LIMIT 1) pg ON true
  WHERE r.codigo = p_codigo;
$$;

-- ---------------------------------------------------------------------
-- Permisos: el helper no se expone; las api_* sí
-- ---------------------------------------------------------------------
REVOKE ALL ON FUNCTION _bb_saldo_reserva(reserva, numeric) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION api_reserva_saldo(text, text, jsonb), api_reserva_cobrar_saldo(text, text, jsonb) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION api_reserva_saldo(text, text, jsonb), api_reserva_cobrar_saldo(text, text, jsonb) TO anon, authenticated;
