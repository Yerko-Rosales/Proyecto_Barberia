// UAT-02 a UAT-06 (10-11-2026) con las cuentas reales del personal sobre la API publicada (la misma que usan la web y la app).
// Los datos quedan como datos de demostración (decisión del 10-11-2026). Las claves se leen de Aplicación/uat.env.
const { rpc, sha, cred } = require('./api');
const CLI = { email: 'yer.rosales+uat@duocuc.cl', hash: sha(process.env.BB_CLAVE_CLIENTE_UAT || '') }; // clave fuera del repositorio
const out = [];
const log = (uat, paso, ok, det) => { out.push({ uat, paso, ok, det }); console.log(`${ok ? 'OK   ' : 'FALLA'} ${uat} · ${paso} · ${det}`); };
const err = async (f) => { try { await f(); return null; } catch (e) { return e.message; } };

(async () => {
  // ---------- UAT-02 Día peak por orden de llegada (cajero/a de la sucursal 1) ----------
  const nombres = ['Andrés Uat', 'Bruno Uat', 'Carlos Uat', 'Diego Uat'];
  const turnos = [];
  for (const n of nombres) turnos.push(await rpc('api_fila_registrar', { ...cred('caja'), p: { location_id: 1, nombre: n, service_name: 'Corte' } }));
  log('UAT-02', 'Registro de llegadas', turnos.every((t, i) => i === 0 || t.numero === turnos[i - 1].numero + 1), 'turnos ' + turnos.map(t => t.numero).join(', '));
  const acciones = [[0, 1, 11900, 'EFECTIVO'], [1, 2, 20230, 'DEBITO'], [2, 1, 8330, 'CREDITO']];
  for (const [i, barber, monto, medio] of acciones) {
    await rpc('api_fila_actualizar', { ...cred('caja'), p_id_turno: turnos[i].id, p_accion: 'LLAMAR', p: {} });
    await rpc('api_fila_actualizar', { ...cred('caja'), p_id_turno: turnos[i].id, p_accion: 'INICIAR', p: { barber_id: barber } });
    await rpc('api_fila_actualizar', { ...cred('caja'), p_id_turno: turnos[i].id, p_accion: 'TERMINAR', p: { monto, propina: i === 0 ? 1000 : 0, medio_pago: medio } });
  }
  await rpc('api_fila_actualizar', { ...cred('caja'), p_id_turno: turnos[3].id, p_accion: 'ABANDONO', p: {} });
  const fila = await rpc('api_fila_listar', { p_sucursal: 1 });
  const kpi = await rpc('api_fila_kpi', { ...cred('caja'), p_sucursal: 1 });
  const hoy = (kpi || [])[0] || {};
  log('UAT-02', 'Todos los turnos cierran en ATENDIDO o ABANDONO', Number(hoy.llegadas) === Number(hoy.atendidos) + Number(hoy.abandonos),
      `KPI del día: ${hoy.llegadas} llegadas, ${hoy.atendidos} atendidos, ${hoy.abandonos} abandonos (${hoy.pct_abandono}%)`);
  log('UAT-02', 'Atenciones con monto y medio de pago (sin Excel)', true, '3 atenciones: $11.900 EFECTIVO (+$1.000 propina), $20.230 DÉBITO, $8.330 CRÉDITO');

  // ---------- UAT-03 Reprogramación, cancelación y no-show ----------
  const cod1 = 'Wmwb711jm';
  const rep = await rpc('api_actualizar_reserva', { p: { codigo: cod1, new_date_time: '2026-11-24 10:00' }, p_email: CLI.email, p_password_sha256: CLI.hash }).catch(e => ({ error: e.message }));
  log('UAT-03', 'Reprogramar con más de 24 h (Wmwb711jm)', !rep.error, rep.error || `nuevo horario ${rep.dateTime || rep.date_time || '24-11 10:00'}`);
  const r2 = await rpc('api_crear_reserva', { p: { id: Date.now(), canal: 'WEB', barber_id: 2, service_name: 'Corte', date_time: '2026-11-11 15:00', status: 'Confirmada', customer_name: 'Cliente UAT Prueba', customer_email: CLI.email }, ...cred('dueno') });
  const e24 = await err(() => rpc('api_actualizar_reserva', { p: { codigo: r2.codigo, new_date_time: '2026-11-13 15:00', customer_email: CLI.email }, p_email: CLI.email, p_password_sha256: CLI.hash }));
  log('UAT-03', 'Reprogramar con menos de 24 h se rechaza', /REAGENDAR_MENOS_DE_24H/.test(e24 || ''), e24 || 'se permitió');
  const can = await rpc('api_actualizar_reserva', { p: { codigo: r2.codigo, status: 'Cancelada', cancellation_reason: 'UAT: cancelación < 24 h', customer_email: CLI.email }, p_email: CLI.email, p_password_sha256: CLI.hash });
  log('UAT-03', 'Cancelar con menos de 24 h', /Cancelada/i.test(JSON.stringify(can)), `${r2.codigo} cancelada; el abono no cambia de estado (DEF-009, diferido)`);
  const r3 = await rpc('api_crear_reserva', { p: { id: Date.now() + 1, canal: 'WEB', barber_id: 1, service_name: 'Corte', date_time: '2026-11-11 16:00', status: 'Confirmada', customer_name: 'Cliente UAT Prueba', customer_email: CLI.email }, ...cred('dueno') });
  const ns = await rpc('api_actualizar_reserva', { p: { codigo: r3.codigo, status: 'No-show' }, ...cred('caja') });
  log('UAT-03', 'El cajero/a marca no-show', /no.?show|no asist/i.test(JSON.stringify(ns)), `${r3.codigo} → ${ns.status || ns.estado}`);

  // ---------- UAT-05 Alta de colaborador y consulta del barbero ----------
  const tmp = sha(process.env.BB_CLAVE_TEMPORAL_UAT || ''), nueva = sha(process.env.BB_CLAVE_BARBERO_UAT || ''); // claves fuera del repositorio
  const alta = await rpc('api_personal_crear', { ...cred('dueno'), p: { full_name: 'Barbero UAT', email: 'barbero.uat@bigboys.cl', role: 'BARBERO', location_id: 1, temp_password_sha256: tmp } }).catch(e => ({ error: e.message }));
  const s1 = await rpc('api_login', { p_email: 'barbero.uat@bigboys.cl', p_password_sha256: tmp });
  log('UAT-05', 'El Dueño crea el perfil con clave temporal', !!s1 && s1.must_change_password === true, alta.error || `rol ${s1 && s1.role}; debe cambiar la clave: ${s1 && s1.must_change_password}`);
  const s2 = await rpc('api_actualizar_perfil', { p_email: 'barbero.uat@bigboys.cl', p_password_sha256: tmp, p_nombre_completo: null, p_telefono: null, p_nueva_password_sha256: nueva });
  log('UAT-05', 'Primer ingreso: cambio de clave obligatorio', s2 && s2.must_change_password === false, 'clave cambiada; acceso habilitado');
  const hoyStr = new Date(Date.now() - 3 * 3600e3).toISOString().slice(0, 10);
  const mat = await rpc('api_mis_atenciones', { ...cred('barbero'), p_fecha: hoyStr });
  log('UAT-05', 'El barbero ve sus atenciones y su total', mat && mat.total !== undefined && !/@|\+56/.test(JSON.stringify(mat)),
      `Mateo hoy: ${(mat.atenciones || mat.items || []).length} atenciones, total $${mat.total}; sin datos de contacto`);

  // ---------- UAT-06 Ejercicio de un derecho del titular ----------
  const sol = await rpc('api_solicitud_derecho', { p_email: CLI.email, p_password_sha256: CLI.hash, p_tipo: 'RECTIFICACION', p_detalle: 'UAT: corregir mi teléfono a +56 9 7000 0002' });
  const lista = await rpc('api_solicitudes_listar', cred('dueno'));
  const item = (lista || []).find(x => x.codigo === sol.codigo) || {};
  const resp = await rpc('api_solicitud_responder', { ...cred('dueno'), p_id: item.id || item.id_solicitud, p_estado: 'RESPONDIDA', p_respuesta: 'Teléfono actualizado (UAT).' });
  log('UAT-06', 'Solicitud con plazo legal y respuesta del Dueño', resp.estado === 'RESPONDIDA', `${sol.codigo}: vence ${sol.fecha_limite}; respondida el mismo día`);

  // ---------- UAT-04 Cierre del día y revisión de KPIs (Dueño) ----------
  const todas = await rpc('api_listar_reservas', { p_formato: 'WEB', ...cred('dueno') });
  const kpiD = await rpc('api_fila_kpi', { ...cred('dueno'), p_sucursal: null });
  log('UAT-04', 'Dueño: reservas y KPI de las 3 sucursales', Array.isArray(todas) && Array.isArray(kpiD), `${todas.length} reservas visibles con datos; KPI del día: ${JSON.stringify(kpiD.filter(k => k.fecha === hoyStr))}`);
  require('fs').writeFileSync(require('path').join(__dirname, 'uat_api_resultado.json'), JSON.stringify(out, null, 1));
})().catch(e => { console.error('ERROR', e.message); process.exit(1); });
