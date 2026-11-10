-- =====================================================================
-- Ciclo 1 de pruebas funcionales (documento 18, CP-01 a CP-77): casos que se
-- verifican en la base de datos y la API. Ejecutado el 10-11-2026 sobre Supabase.
-- Corre dentro de una transacción que se deshace al final: no deja datos.
-- Las claves de las cuentas demo se reemplazan SOLO dentro de la transacción
-- por una clave de prueba conocida, para no publicar las reales.
-- Resultado: una fila por verificación con OK / FALLA y el detalle obtenido.
-- =====================================================================
BEGIN;
CREATE TEMP TABLE resultado (n serial, cp text, ok boolean, detalle text) ON COMMIT DROP;

DO $$
DECLARE
  h text := encode(digest('Prueba123', 'sha256'), 'hex');      -- clave de prueba (solo en esta transacción)
  h_mala text := encode(digest('Incorrecta9', 'sha256'), 'hex');
  h_nueva text := encode(digest('Nueva123', 'sha256'), 'hex');
  v jsonb; v2 jsonb; n int; n2 int; x text; ok boolean; v_id bigint; v_cli bigint; v_turno bigint; v_cod text;
BEGIN
  UPDATE usuario SET password_hash = crypt(h, gen_salt('bf', 4)), intentos_fallidos = 0
   WHERE email IN ('admin@bigboys.cl', 'caja1@bigboys.cl', 'mateo@bigboys.cl', 'test@bigboys.cl');

  -- CP-01 Registro exitoso: rol CLIENTE, consentimiento de SERVICIO con canal y sin promocional
  v := api_registrar_cliente('cp01.uat@prueba.invalid', 'Cliente Uno Prueba', '+56 9 1111 0001', h, 'WEB');
  SELECT id_cliente INTO v_cli FROM cliente WHERE email_contacto = 'cp01.uat@prueba.invalid';
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-01', v->>'role' = 'CLIENTE'
     AND EXISTS (SELECT 1 FROM consentimiento WHERE id_cliente = v_cli AND finalidad = 'SERVICIO' AND canal = 'WEB')
     AND NOT EXISTS (SELECT 1 FROM consentimiento WHERE id_cliente = v_cli AND finalidad = 'PROMOCIONAL'),
     'rol ' || (v->>'role') || '; consentimiento SERVICIO/WEB; sin PROMOCIONAL';

  -- CP-03 Correo ya registrado
  BEGIN
    PERFORM api_registrar_cliente('cp01.uat@prueba.invalid', 'Otra', '+56 9 1', h, 'WEB');
    INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-03', false, 'se creó un duplicado');
  EXCEPTION WHEN OTHERS THEN INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-03', SQLERRM LIKE '%CORREO_YA_REGISTRADO%', SQLERRM);
  END;

  -- CP-06 Inicio de sesión de cliente y cajero/a con alcance a la sucursal 1, auditado
  SELECT count(*) INTO n FROM registro_auditoria WHERE accion = 'LOGIN';
  v := api_login('test@bigboys.cl', h); v2 := api_login('caja1@bigboys.cl', h);
  SELECT count(*) INTO n2 FROM registro_auditoria WHERE accion = 'LOGIN';
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-06', v->>'role' = 'CLIENTE' AND v2->>'role' = 'ADMIN_SUCURSAL'
     AND (v2->>'location_id')::int = 1 AND n2 = n + 2, 'cliente + cajero/a suc. ' || (v2->>'location_id') || '; ' || (n2 - n) || ' LOGIN auditados');

  -- CP-07 Credenciales inválidas: misma respuesta con correo existente o inexistente; suma intentos
  SELECT intentos_fallidos INTO n FROM usuario WHERE email = 'test@bigboys.cl';
  ok := api_login('test@bigboys.cl', h_mala) IS NULL AND api_login('no.existe@prueba.invalid', h_mala) IS NULL;
  SELECT intentos_fallidos INTO n2 FROM usuario WHERE email = 'test@bigboys.cl';
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-07', ok AND n2 = n + 1, 'ambos NULL; intentos ' || n || ' → ' || n2);

  -- CP-08 Bloqueo tras 5 intentos fallidos: con la clave correcta tampoco entra
  FOR i IN 1..5 LOOP PERFORM api_login('test@bigboys.cl', h_mala); END LOOP;
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-08', api_login('test@bigboys.cl', h) IS NULL,
    'bloqueado por ' || _bb_cfg('MINUTOS_BLOQUEO_LOGIN') || ' min; aviso por correo: ' ||
    CASE WHEN EXISTS (SELECT 1 FROM notificacion WHERE tipo = 'SEGURIDAD') THEN 'sí' ELSE 'no se envía' END);
  UPDATE usuario SET intentos_fallidos = 0 WHERE email = 'test@bigboys.cl';

  -- CP-09 Colaborador con clave temporal: debe cambiarla y al cambiarla se libera
  v := api_personal_crear('admin@bigboys.cl', h, jsonb_build_object('full_name', 'Barbero Uat', 'email', 'barbero.uat@prueba.invalid',
         'role', 'BARBERO', 'location_id', 1, 'temp_password_sha256', h));
  v2 := api_login('barbero.uat@prueba.invalid', h);
  v := api_actualizar_perfil('barbero.uat@prueba.invalid', h, NULL, NULL, h_nueva);
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-09', (v2->>'must_change_password')::boolean AND NOT (v->>'must_change_password')::boolean,
    'temporal: debe cambiar = ' || (v2->>'must_change_password') || '; después = ' || (v->>'must_change_password'));

  -- CP-12 Edición del perfil auditada
  SELECT count(*) INTO n FROM registro_auditoria WHERE accion = 'ACTUALIZAR' AND entidad = 'usuario';
  v := api_actualizar_perfil('cp01.uat@prueba.invalid', h, NULL, '+56 9 2222 0002', NULL);
  SELECT count(*) INTO n2 FROM registro_auditoria WHERE accion = 'ACTUALIZAR' AND entidad = 'usuario';
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-12', v->>'phone' = '+56 9 2222 0002' AND n2 = n + 1, 'teléfono ' || (v->>'phone') || '; auditado');

  -- CP-15 Control de acceso por rol y sucursal
  ok := true; x := '';
  BEGIN PERFORM api_personal_crear('mateo@bigboys.cl', h, '{"full_name":"X","email":"x@prueba.invalid","role":"BARBERO","location_id":1}'); ok := false;
  EXCEPTION WHEN OTHERS THEN x := x || 'barbero:' || SQLERRM || ' '; END;
  BEGIN PERFORM api_mis_atenciones('test@bigboys.cl', h, current_date); ok := false;
  EXCEPTION WHEN OTHERS THEN x := x || 'cliente:' || SQLERRM || ' '; END;
  BEGIN PERFORM api_fila_kpi('caja1@bigboys.cl', h, 2::smallint); ok := false;
  EXCEPTION WHEN OTHERS THEN x := x || 'caja suc.2:' || SQLERRM; END;
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-15', ok, x);

  -- CP-18 Otorgar y revocar consentimiento promocional, con fecha
  PERFORM api_consentimiento('cp01.uat@prueba.invalid', h, true, 'APP');
  PERFORM api_consentimiento('cp01.uat@prueba.invalid', h, false, 'APP');
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-18', count(*) = 1 AND bool_and(revocado_en IS NOT NULL), count(*) || ' registro promocional; revocado con fecha'
    FROM consentimiento WHERE id_cliente = v_cli AND finalidad = 'PROMOCIONAL';

  -- CP-19 Solicitud de derecho con folio, plazo de 30 días y correo de recepción
  v := api_solicitud_derecho('cp01.uat@prueba.invalid', h, 'RECTIFICACION', 'Prueba UAT');
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-19', (v->>'fecha_limite')::date = (now() AT TIME ZONE 'America/Santiago')::date + 30
     AND EXISTS (SELECT 1 FROM notificacion WHERE tipo = 'DERECHOS' AND asunto LIKE '%' || (v->>'codigo')),
     (v->>'codigo') || ' vence ' || (v->>'fecha_limite') || '; correo encolado';

  -- CP-21 Derecho de acceso: datos de la cuenta, reservas, pagos (sin tarjeta) y consentimientos
  v := api_mis_datos('test@bigboys.cl', h, 'ACCESO');
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-21', v ? 'cuenta' AND v ? 'reservas' AND v ? 'pagos' AND v ? 'consentimientos'
     AND v::text !~* '(numero_tarjeta|card_number|cvv)', 'secciones: ' || (SELECT string_agg(k, ', ') FROM jsonb_object_keys(v) k)
     || '; destinatarios: ' || CASE WHEN v ? 'destinatarios' THEN 'sí' ELSE 'no se informan' END);

  -- CP-23 Supresión con una cita futura confirmada: debe impedirse
  v := api_crear_reserva('{"id": 1790001000001, "canal": "WEB", "barber_id": 1, "service_name": "Corte", "date_time": "2026-11-25 10:00", "customer_name": "Cliente Uno Prueba"}',
                         'admin@bigboys.cl', h);
  UPDATE reserva SET id_cliente = v_cli WHERE codigo = v->>'codigo';
  BEGIN
    ok := api_suprimir_cuenta('cp01.uat@prueba.invalid', h);
    RAISE EXCEPTION 'deshacer_supresion';   -- se deshace la supresión para seguir usando la cuenta en los casos siguientes
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM = 'deshacer_supresion' THEN
      INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-23', NOT ok, CASE WHEN ok THEN 'la cuenta se suprimió con una reserva CONFIRMADA futura' ELSE 'rechazada' END);
    ELSE INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-23', true, SQLERRM); END IF;
  END;

  -- CP-22 Supresión sin citas futuras: anonimiza y revoca consentimientos
  v := api_registrar_cliente('cp22.uat@prueba.invalid', 'Cliente Supresion', '+56 9 3333 0003', h, 'WEB');
  ok := api_suprimir_cuenta('cp22.uat@prueba.invalid', h);
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-22', ok AND u.estado = 'SUPRIMIDO' AND u.email LIKE 'suprimido-%' AND c.email_contacto IS NULL
     AND NOT EXISTS (SELECT 1 FROM consentimiento k WHERE k.id_cliente = c.id_cliente AND k.revocado_en IS NULL),
     u.estado || '; ' || u.email || '; consentimientos revocados'
    FROM usuario u JOIN cliente c USING (id_usuario) WHERE u.id_usuario = (v->>'id')::bigint;

  -- CP-24 Oposición: el tratamiento promocional debe detenerse de inmediato
  v := api_registrar_cliente('cp24.uat@prueba.invalid', 'Cliente Oposicion', '+56 9 4444 0004', h, 'WEB');
  PERFORM api_consentimiento('cp24.uat@prueba.invalid', h, true, 'WEB');
  PERFORM api_solicitud_derecho('cp24.uat@prueba.invalid', h, 'OPOSICION', 'No quiero promociones');
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-24', NOT EXISTS (SELECT 1 FROM consentimiento k WHERE k.id_cliente = c.id_cliente AND k.finalidad = 'PROMOCIONAL' AND k.revocado_en IS NULL),
     CASE WHEN EXISTS (SELECT 1 FROM consentimiento k WHERE k.id_cliente = c.id_cliente AND k.finalidad = 'PROMOCIONAL' AND k.revocado_en IS NULL)
          THEN 'solicitud registrada, pero el consentimiento promocional sigue vigente' ELSE 'detenido' END
    FROM cliente c WHERE c.email_contacto = 'cp24.uat@prueba.invalid';

  -- CP-25 Portabilidad: archivo estructurado (JSON) y exportación auditada
  v := api_mis_datos('test@bigboys.cl', h, 'PORTABILIDAD');
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-25', jsonb_typeof(v) = 'object' AND EXISTS (SELECT 1 FROM registro_auditoria WHERE accion = 'EXPORTAR' AND ocurrido_en > now() - interval '1 min'),
    'JSON generado y EXPORTAR auditado; CSV y enlace temporal de 24 h: no implementados');

  -- CP-26 Bloqueo: la cuenta debe quedar BLOQUEADA
  PERFORM api_solicitud_derecho('cp24.uat@prueba.invalid', h, 'BLOQUEO', 'Bloquear mis datos');
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-26', estado = 'BLOQUEADO', 'estado de la cuenta tras solicitar bloqueo: ' || estado
    FROM usuario WHERE email = 'cp24.uat@prueba.invalid';

  -- CP-27 Directorio: 3 sucursales activas con dirección
  v := api_sucursales();
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-27', jsonb_array_length(v) = 3, jsonb_array_length(v) || ' sucursales: ' ||
    (SELECT string_agg(coalesce(e->>'name', e->>'nombre'), ' · ') FROM jsonb_array_elements(v) e));

  -- CP-28 Catálogo con precios y barberos activos (los desactivados no aparecen)
  v := api_barberos(); v2 := api_servicios(1::smallint);
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-28', v::text NOT LIKE '%Barbero Prueba%' AND jsonb_array_length(v2) >= 6,
    jsonb_array_length(v) || ' barberos activos (sin los 2 desactivados); ' || jsonb_array_length(v2) || ' servicios con precio');

  -- CP-29 Disponibilidad: un bloqueo de horario debe impedir reservar ese bloque
  INSERT INTO bloqueo_horario (id_barbero, inicio, fin, motivo, creado_por) VALUES (1, '2026-11-26 14:00-03', '2026-11-26 15:00-03', 'Prueba UAT', 1);
  BEGIN
    v := api_crear_reserva('{"id": 1790001000002, "canal": "WEB", "barber_id": 1, "service_name": "Corte", "date_time": "2026-11-26 14:00", "status": "Pendiente", "customer_name": "Prueba Bloqueo"}');
    INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-29', false, 'se reservó dentro de un bloqueo de horario (' || (v->>'codigo') || ')');
  EXCEPTION WHEN OTHERS THEN INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-29', true, SQLERRM);
  END;

  -- CP-33 Dos reservas al mismo bloque: la base de datos rechaza la segunda
  v := api_crear_reserva('{"id": 1790001000003, "canal": "WEB", "barber_id": 2, "service_name": "Corte", "date_time": "2026-11-26 11:00", "status": "Pendiente", "customer_name": "Prueba A"}');
  BEGIN
    PERFORM api_crear_reserva('{"id": 1790001000004, "canal": "APP", "barber_id": 2, "service_name": "Corte", "date_time": "2026-11-26 11:00", "status": "Pendiente", "customer_name": "Prueba B"}');
    INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-33', false, 'se aceptaron dos reservas');
  EXCEPTION WHEN OTHERS THEN INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-33', SQLERRM LIKE '%HORARIO_OCUPADO%', SQLERRM);
  END;

  -- CP-35 Reserva impaga vencida: pasa a EXPIRADA y libera el bloque
  UPDATE reserva SET expira_en = now() - interval '1 min' WHERE codigo = v->>'codigo';
  v2 := api_crear_reserva('{"id": 1790001000005, "canal": "WEB", "barber_id": 2, "service_name": "Corte", "date_time": "2026-11-26 11:00", "status": "Pendiente", "customer_name": "Prueba C"}');
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-35', estado = 'EXPIRADA' AND v2->>'codigo' IS NOT NULL, estado || '; bloque reservado de nuevo'
    FROM reserva WHERE codigo = v->>'codigo';

  -- CP-36 Abono por sucursal: 20% en la sucursal 3 debe dar $2.380 para un corte
  INSERT INTO configuracion (clave, ambito, id_sucursal, tipo_valor, valor) VALUES ('ABONO_RESERVA', 'SUCURSAL', 3, 'PORCENTAJE', '20');
  v := api_crear_reserva('{"id": 1790001000006, "canal": "WEB", "barber_id": 4, "service_name": "Corte", "date_time": "2026-11-26 16:00", "status": "Pendiente", "customer_name": "Prueba Abono"}');
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-36', monto_abono = 2380, 'abono calculado: $' || monto_abono || ' (esperado $2.380)'
    FROM reserva WHERE codigo = v->>'codigo';

  -- CP-37 Sin datos de tarjeta en la tabla pago
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-37', NOT bool_or(column_name ~* '(tarjeta|card|cvv|venc|pan)'), string_agg(column_name, ', ')
    FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'pago';

  -- CP-38 Reprogramación con más de 24 h: contador en 1 e historial
  v := api_crear_reserva('{"id": 1790001000007, "canal": "WEB", "barber_id": 1, "service_name": "Corte", "date_time": "2026-11-27 10:00", "customer_name": "Cliente Uno Prueba"}', 'admin@bigboys.cl', h);
  v_cod := v->>'codigo';
  UPDATE reserva SET id_cliente = v_cli WHERE codigo = v_cod;
  v := api_actualizar_reserva(jsonb_build_object('codigo', v_cod, 'new_date_time', '2026-11-27 11:00'), 'cp01.uat@prueba.invalid', h);
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-38', r.cantidad_reagendamientos = 1 AND EXISTS (SELECT 1 FROM reagendamiento g WHERE g.id_reserva = r.id_reserva),
     'reagendamientos = ' || r.cantidad_reagendamientos || '; historial registrado' FROM reserva r WHERE codigo = v_cod;

  -- CP-39 Tercer reagendamiento rechazado
  PERFORM api_actualizar_reserva(jsonb_build_object('codigo', v_cod, 'new_date_time', '2026-11-27 12:00'), 'cp01.uat@prueba.invalid', h);
  BEGIN
    PERFORM api_actualizar_reserva(jsonb_build_object('codigo', v_cod, 'new_date_time', '2026-11-27 15:00'), 'cp01.uat@prueba.invalid', h);
    INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-39', false, 'se permitió un tercer cambio');
  EXCEPTION WHEN OTHERS THEN INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-39', SQLERRM LIKE '%MAXIMO_REAGENDAMIENTOS%', SQLERRM);
  END;

  -- CP-40 Reprogramación con menos de 24 h rechazada
  v := api_crear_reserva('{"id": 1790001000008, "canal": "WEB", "barber_id": 2, "service_name": "Corte", "date_time": "2026-11-11 10:00", "customer_name": "Cliente Uno Prueba"}', 'admin@bigboys.cl', h);
  UPDATE reserva SET id_cliente = v_cli WHERE codigo = v->>'codigo';
  BEGIN
    PERFORM api_actualizar_reserva(jsonb_build_object('codigo', v->>'codigo', 'new_date_time', '2026-11-12 10:00'), 'cp01.uat@prueba.invalid', h);
    INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-40', false, 'se permitió');
  EXCEPTION WHEN OTHERS THEN INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-40', SQLERRM LIKE '%REAGENDAR_MENOS_DE_24H%', SQLERRM);
  END;

  -- CP-42 Cancelación con menos de 24 h: el pago debe quedar RETENIDO
  INSERT INTO pago (id_reserva, proveedor, id_transaccion, codigo_autorizacion, tipo_medio, monto, estado)
  SELECT id_reserva, 'WEBPAY', 'uat-cp42', '1213', 'CREDITO', 3000, 'AUTORIZADO' FROM reserva WHERE codigo = v->>'codigo';
  PERFORM api_actualizar_reserva(jsonb_build_object('codigo', v->>'codigo', 'status', 'Cancelada'), 'cp01.uat@prueba.invalid', h);
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-42', p.estado = 'RETENIDO', 'reserva ' || r.estado || '; pago ' || p.estado || ' (esperado RETENIDO)'
    FROM reserva r JOIN pago p USING (id_reserva) WHERE r.codigo = v->>'codigo';

  -- CP-41 Cancelación con 24 h o más: cupo liberado, correo de cancelación, auditoría y saldo a favor
  PERFORM api_actualizar_reserva(jsonb_build_object('codigo', v_cod, 'status', 'Cancelada'), 'cp01.uat@prueba.invalid', h);
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-41', r.estado = 'CANCELADA'
     AND EXISTS (SELECT 1 FROM notificacion WHERE id_reserva = r.id_reserva AND tipo = 'CANCELACION')
     AND EXISTS (SELECT 1 FROM registro_auditoria WHERE entidad = 'reserva' AND id_entidad = r.codigo),
     'CANCELADA, correo de cancelación y auditoría; saldo a favor del abono: no se registra' FROM reserva r WHERE codigo = v_cod;

  -- CP-44 Recordatorio: una reserva confirmada dentro de 24 h genera su recordatorio
  v := api_crear_reserva('{"id": 1790001000009, "canal": "WEB", "barber_id": 1, "service_name": "Corte", "date_time": "2026-11-11 12:00", "customer_name": "Cliente Uno Prueba"}', 'admin@bigboys.cl', h);
  UPDATE reserva SET id_cliente = v_cli WHERE codigo = v->>'codigo';
  PERFORM api_correo_pendientes(_bb_cfg('CLAVE_SERVICIO_CORREO'));
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-44', count(*) = 1, count(*) || ' recordatorio encolado para ' || (v->>'codigo')
    FROM notificacion n JOIN reserva r USING (id_reserva) WHERE r.codigo = v->>'codigo' AND n.tipo = 'RECORDATORIO';

  -- CP-47 Fila: número correlativo y estados EN_ESPERA → LLAMADO → EN_ATENCION → ATENDIDO
  v := api_fila_registrar('caja1@bigboys.cl', h, '{"location_id": 1, "nombre": "Pedro Uat"}');
  v2 := api_fila_registrar('caja1@bigboys.cl', h, '{"location_id": 1, "nombre": "Luis Uat"}');
  v_turno := (v->>'id')::bigint; x := v->>'estado';
  x := x || ' → ' || ((api_fila_actualizar('caja1@bigboys.cl', h, v_turno, 'LLAMAR'))->>'estado');
  x := x || ' → ' || ((api_fila_actualizar('caja1@bigboys.cl', h, v_turno, 'INICIAR', '{"barber_id": 1}'))->>'estado');
  x := x || ' → ' || ((api_fila_actualizar('caja1@bigboys.cl', h, v_turno, 'TERMINAR', '{"medio_pago": "EFECTIVO"}'))->>'estado');
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-47', (v2->>'numero')::int = (v->>'numero')::int + 1 AND x LIKE '%ATENDIDO',
    'turnos ' || (v->>'numero') || ' y ' || (v2->>'numero') || '; ' || x || '; tiempo estimado: ' ||
    CASE WHEN (api_fila_listar(1::smallint))::text ~* 'estimad|espera_min' THEN 'sí' ELSE 'no se informa' END);

  -- CP-48 Abandono de la fila reflejado en el KPI
  PERFORM api_fila_actualizar('caja1@bigboys.cl', h, (v2->>'id')::bigint, 'ABANDONO');
  v := api_fila_kpi('caja1@bigboys.cl', h, 1::smallint);
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-48', v::text LIKE '%abandon%', left((v->0)::text, 160));

  -- CP-53 El barbero y el cliente no cambian estados de atención
  ok := true; x := '';
  BEGIN PERFORM api_reserva_cobrar_saldo('mateo@bigboys.cl', h, jsonb_build_object('codigo', v_cod)); ok := false;
  EXCEPTION WHEN OTHERS THEN x := 'barbero: ' || SQLERRM; END;
  v := api_crear_reserva('{"id": 1790001000010, "canal": "WEB", "barber_id": 1, "service_name": "Corte", "date_time": "2026-11-28 10:00", "customer_name": "Cliente Uno Prueba"}', 'admin@bigboys.cl', h);
  UPDATE reserva SET id_cliente = v_cli WHERE codigo = v->>'codigo';
  BEGIN PERFORM api_actualizar_reserva(jsonb_build_object('codigo', v->>'codigo', 'status', 'Completada'), 'cp01.uat@prueba.invalid', h); ok := false;
  EXCEPTION WHEN OTHERS THEN x := x || '; cliente: ' || SQLERRM; END;
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-53', ok, x);

  -- CP-52 No-show: el cajero/a lo marca y el pago debe quedar RETENIDO
  INSERT INTO pago (id_reserva, proveedor, id_transaccion, codigo_autorizacion, tipo_medio, monto, estado)
  SELECT id_reserva, 'WEBPAY', 'uat-cp52', '1213', 'DEBITO', 3000, 'AUTORIZADO' FROM reserva WHERE codigo = v->>'codigo';
  v2 := api_actualizar_reserva(jsonb_build_object('codigo', v->>'codigo', 'status', 'No-show'), 'caja1@bigboys.cl', h);
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-52', r.estado = 'NO_SHOW' AND p.estado = 'RETENIDO', 'reserva ' || r.estado || '; pago ' || p.estado || ' (esperado RETENIDO)'
    FROM reserva r JOIN pago p USING (id_reserva) WHERE r.codigo = v->>'codigo';

  -- CP-54 Término de atención con abono: saldo $8.900, abono APLICADO y puntos al cliente
  v := api_crear_reserva('{"id": 1790001000011, "canal": "WEB", "barber_id": 1, "service_name": "Corte", "date_time": "2026-11-28 11:00", "customer_name": "Cliente Uno Prueba"}', 'admin@bigboys.cl', h);
  UPDATE reserva SET id_cliente = v_cli WHERE codigo = v->>'codigo';
  INSERT INTO pago (id_reserva, proveedor, id_transaccion, codigo_autorizacion, tipo_medio, monto, estado)
  SELECT id_reserva, 'WEBPAY', 'uat-cp54', '1213', 'CREDITO', 3000, 'AUTORIZADO' FROM reserva WHERE codigo = v->>'codigo';
  v2 := api_reserva_saldo('caja1@bigboys.cl', h, jsonb_build_object('codigo', v->>'codigo'));
  PERFORM api_reserva_cobrar_saldo('caja1@bigboys.cl', h, jsonb_build_object('codigo', v->>'codigo', 'medio_pago', 'EFECTIVO'));
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-54', p.estado = 'APLICADO' AND a.registrada_por IS NOT NULL AND c.puntos > 0,
     'saldo $' || (v2->>'saldo') || '; pago ' || p.estado || '; registrada por usuario ' || a.registrada_por || '; puntos del cliente: ' || c.puntos
    FROM reserva r JOIN pago p USING (id_reserva) JOIN atencion a USING (id_reserva) JOIN cliente c ON c.id_cliente = r.id_cliente WHERE r.codigo = v->>'codigo';

  -- CP-56 El barbero ve sus atenciones del día sin datos de contacto
  v := api_mis_atenciones('mateo@bigboys.cl', h, current_date);
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-56', v ? 'total' AND v::text !~* '(@|\+56)', 'total $' || (v->>'total') || '; sin correos ni teléfonos');

  -- CP-58 / CP-59 Solo el Dueño crea colaboradores, con clave temporal y auditoría
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-58', EXISTS (SELECT 1 FROM registro_auditoria a JOIN usuario u ON u.id_usuario::text = a.id_entidad
     WHERE u.email = 'barbero.uat@prueba.invalid' AND a.accion = 'CREAR' AND a.rol = 'DUENO'), 'alta auditada con rol DUENO (CP-09: clave temporal)';
  ok := true; x := '';
  BEGIN PERFORM api_personal_crear('caja1@bigboys.cl', h, '{"full_name":"X","email":"x2@prueba.invalid","role":"BARBERO","location_id":1}'); ok := false;
  EXCEPTION WHEN OTHERS THEN x := 'cajero/a: ' || SQLERRM; END;
  BEGIN INSERT INTO usuario_rol (id_usuario, id_rol) SELECT id_usuario, (SELECT id_rol FROM rol WHERE codigo = 'DUENO') FROM usuario WHERE email = 'cp01.uat@prueba.invalid'; ok := false;
  EXCEPTION WHEN OTHERS THEN x := x || '; asignación directa: ' || SQLERRM; END;
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-59', ok, x);

  -- CP-60 Colaborador desactivado no puede iniciar sesión
  SELECT id_usuario INTO v_id FROM usuario WHERE email = 'barbero.uat@prueba.invalid';
  PERFORM api_personal_actualizar('admin@bigboys.cl', h, v_id, false);
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-60', api_login('barbero.uat@prueba.invalid', h_nueva) IS NULL, 'inicio de sesión rechazado tras desactivar');

  -- CP-67 / CP-68 Respuesta con responsable y fecha; prórroga de 30 días y segunda prórroga rechazada
  SELECT id_solicitud INTO v_id FROM solicitud_derecho WHERE id_cliente = v_cli AND tipo = 'RECTIFICACION' ORDER BY id_solicitud DESC LIMIT 1;
  v := api_solicitud_responder('admin@bigboys.cl', h, v_id, 'PRORROGADA', 'Necesitamos más plazo');
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-68a', prorrogada_hasta = fecha_limite + 30, 'prórroga hasta ' || prorrogada_hasta FROM solicitud_derecho WHERE id_solicitud = v_id;
  BEGIN
    v := api_solicitud_responder('admin@bigboys.cl', h, v_id, 'PRORROGADA', 'Otra prórroga');
    INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-68b', false, 'se permitió una segunda prórroga');
  EXCEPTION WHEN OTHERS THEN INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-68b', true, SQLERRM);
  END;
  v := api_solicitud_responder('admin@bigboys.cl', h, v_id, 'RESPONDIDA', 'Dato corregido');
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-67', estado = 'RESPONDIDA' AND fecha_respuesta IS NOT NULL AND atendida_por IS NOT NULL,
     estado || ', respondida por usuario ' || atendida_por FROM solicitud_derecho WHERE id_solicitud = v_id;

  -- CP-69 La auditoría no se puede modificar ni borrar
  ok := true; x := '';
  BEGIN UPDATE registro_auditoria SET detalle = '{}' WHERE id_log = (SELECT min(id_log) FROM registro_auditoria); ok := false;
  EXCEPTION WHEN OTHERS THEN x := 'UPDATE: ' || SQLERRM; END;
  BEGIN DELETE FROM registro_auditoria WHERE id_log = (SELECT min(id_log) FROM registro_auditoria); ok := false;
  EXCEPTION WHEN OTHERS THEN x := x || '; DELETE: ' || SQLERRM; END;
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-69', ok, x);

  -- CP-73 Políticas versionadas con huella SHA-256
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-73', bool_and(version IS NOT NULL AND hash_sha256 ~ '^[0-9a-f]{64}$'),
     string_agg(tipo || ' v' || version || ' (' || left(coalesce(hash_sha256, 'sin huella'), 8) || ')', ', ') FROM politica WHERE vigente;

  -- CP-74 KPI: el cajero/a solo ve su sucursal y el Dueño ve todas
  v := api_fila_kpi('admin@bigboys.cl', h, NULL);
  INSERT INTO resultado (cp, ok, detalle) VALUES ('CP-74', (SELECT count(DISTINCT e->>'id_sucursal') FROM jsonb_array_elements(v) e) >= 1,
    'Dueño: ' || (SELECT count(DISTINCT e->>'id_sucursal') FROM jsonb_array_elements(v) e) || ' sucursal(es) con datos en 30 días; cajero/a de otra sucursal rechazado (CP-15)');

  -- CP-70 / CP-71 / CP-72 Retención, incidentes y RAT: funciones disponibles en la API
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-70', bool_or(proname ~* 'retenc|anonimiz|inactiv'), 'funciones de retención: ' || coalesce(string_agg(proname, ',') FILTER (WHERE proname ~* 'retenc|anonimiz|inactiv'), 'ninguna')
    FROM pg_proc WHERE pronamespace = 'public'::regnamespace;
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-71', bool_or(proname ~* 'incidente'), 'funciones de incidentes: ' || coalesce(string_agg(proname, ',') FILTER (WHERE proname ~* 'incidente'), 'ninguna')
    FROM pg_proc WHERE pronamespace = 'public'::regnamespace;
  INSERT INTO resultado (cp, ok, detalle) SELECT 'CP-72', (SELECT count(*) FROM actividad_tratamiento) > 0 AND bool_or(proname ~* 'tratamiento|rat'),
     (SELECT count(*) FROM actividad_tratamiento) || ' actividades en el RAT; exportación por API: ' ||
     CASE WHEN bool_or(proname ~* 'tratamiento|rat') THEN 'sí' ELSE 'no' END FROM pg_proc WHERE pronamespace = 'public'::regnamespace;
END $$;
SELECT n, CASE WHEN ok THEN 'OK' ELSE 'FALLA' END AS resultado, cp, detalle FROM resultado ORDER BY n;
ROLLBACK;
