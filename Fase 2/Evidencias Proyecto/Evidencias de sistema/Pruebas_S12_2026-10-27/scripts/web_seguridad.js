// Verifica en el navegador que la web envía credenciales / correo al cambiar reservas
const puppeteer = require('puppeteer-core');
const URL_WEB = process.argv[2] || 'http://127.0.0.1:3000/';
const CLAVE_DUENO = process.env.BB_CLAVE_DUENO;

(async () => {
  const browser = await puppeteer.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  const page = await browser.newPage();
  const errores = [];
  page.on('pageerror', e => errores.push(e.message));
  await page.goto(URL_WEB, { waitUntil: 'networkidle2', timeout: 60000 });
  await page.waitForFunction(() => document.body.innerText.includes('Portal de Acceso'), { timeout: 60000 });

  const r = await page.evaluate(async (claveDueno) => {
    const out = {};
    const sb = store.getCloudClient();
    const sha = async (t) => Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(t))))
      .map(b => b.toString(16).padStart(2, '0')).join('');
    const estado = async (codigo) => (await sb.rpc('api_webpay_estado', { p_codigo: codigo })).data?.reserva_estado;
    const base = { canal: 'WEB', barber_id: 2, service_name: 'Corte', customer_name: 'Prueba Seguridad Web' };

    // 1. Sin cuenta del personal no se crea una reserva confirmada (bypass del pago)
    let x = await sb.rpc('api_crear_reserva', { p: { ...base, id: Date.now(), date_time: '2026-12-03 17:00', status: 'Confirmada' } });
    out['1 Anónimo no confirma sin pago'] = x.error && x.error.message.includes('PAGO_REQUERIDO') ? 'OK' : 'FALLA ' + JSON.stringify(x);

    // 2. Reserva pendiente de un invitado
    const id = Date.now();
    x = await sb.rpc('api_crear_reserva', { p: { ...base, id, date_time: '2026-12-03 17:00', status: 'Pendiente', customer_email: 'web.seguridad@correo.cl' } });
    const codigo = x.data && x.data.codigo;
    const appt = { id, barberId: 2, dateTime: '2026-12-03 17:00', customerEmail: 'otra.persona@correo.cl' };
    out['2 Cancelar con otro correo (store)'] = (await store.updateAppointmentStatusInCloud(appt, 'Cancelada')) === false
      && (await estado(codigo)) === 'PENDIENTE_PAGO' ? 'OK' : 'FALLA';
    appt.customerEmail = 'web.seguridad@correo.cl';
    out['3 Invitado cancela con su correo (store)'] = (await store.updateAppointmentStatusInCloud(appt, 'Cancelada')) === true
      && (await estado(codigo)) === 'CANCELADA' ? 'OK' : 'FALLA';

    // 4. Sesión del Dueño: registra una atención y la cancela
    StaffSession.set({ email: 'admin@bigboys.cl', hash: await sha(claveDueno), role: 'DUENO' });
    const id2 = Date.now() + 1;
    const appt2 = { id: id2, barberId: 2, locationId: 1, serviceName: 'Corte', dateTime: '2026-12-03 18:00', status: 'Confirmada', customerName: 'Prueba Seguridad Dueño' };
    const creada = await store.publishAppointmentToCloud(appt2);
    const cancelada = await store.updateAppointmentStatusInCloud({ ...appt2, cancelledBy: 'Administración' }, 'Cancelada');
    const c2 = 'W' + id2.toString(36).toUpperCase();
    out['4 Dueño confirma y cancela (store)'] = creada && cancelada ? 'OK (' + (await estado(c2) || c2) + ')' : 'FALLA ' + creada + ' ' + cancelada;
    StaffSession.clear();
    return out;
  }, CLAVE_DUENO);

  console.log(JSON.stringify(r, null, 2));
  console.log('Errores de página:', errores.length ? errores : 'ninguno');
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
