// UAT-01 (10-11-2026): un cliente nuevo se registra (aceptando la política), reserva en 5 pasos y paga el abono con Webpay.
// Uso: node uat01.js <url_web> <carpeta_evidencias>
const puppeteer = require('puppeteer-core');
const { nuevaPagina, esperarTexto, clic, captura, texto, pagarEnWebpay, wait } = require('./uat_base');
const CORREO = 'yer.rosales+uat@duocuc.cl', CLAVE = process.env.BB_CLAVE_CLIENTE_UAT; // clave del cliente de prueba, fuera del repositorio

(async () => {
  const browser = await puppeteer.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  const page = await nuevaPagina(browser);
  // 1) Registro con aceptación de la política
  await clic(page, 'Regístrate', 'button'); await wait(800);
  const form = await page.evaluateHandle(() => [...document.querySelectorAll('form')].find(x => x.querySelector('input[placeholder="Ej: Juan Pérez"]')));
  const ins = await form.$$('input:not([type=checkbox])');
  const datos = ['Cliente UAT Prueba', '+56 9 7000 0001', CORREO, CLAVE];
  for (let k = 0; k < datos.length; k++) await ins[k].type(datos[k], { delay: 10 });
  await (await form.$('input[type=checkbox]')).click();
  await captura(page, 'uat01_1_registro');
  await form.evaluate(x => x.requestSubmit());
  await esperarTexto(page, 'Agendar', 30000); await wait(2500);
  await captura(page, 'uat01_2_sesion');
  // 2) Reserva en 5 pasos (cronometrada) y pago del abono
  const t0 = Date.now();
  await clic(page, 'Agendar Cita', 'button');
  await esperarTexto(page, 'Paso 1');
  const tarjetas = async () => page.$$('div[class*="cursor-pointer"][class*="rounded-2xl"]');
  await (await tarjetas())[0].click(); await wait(400); await clic(page, 'Continuar', 'button');
  await esperarTexto(page, 'Paso 2'); await clic(page, 'Mateo', 'div[class*="cursor-pointer"]'); await clic(page, 'Continuar', 'button');
  await esperarTexto(page, 'Paso 3'); await (await tarjetas())[0].click(); await wait(400); await clic(page, 'Continuar', 'button');
  await esperarTexto(page, 'Paso 4');
  for (const d of await page.$$('div.grid-cols-7 button:not([disabled])')) {
    if ((await d.evaluate(e => e.innerText.trim())) === '24') { await d.click(); break; }
  }
  await wait(600); let hora = null;
  for (const h of await page.$$('button:not([disabled])')) {
    const s = await h.evaluate(e => e.innerText.trim());
    if (/^\d{2}:\d{2} hrs$/.test(s)) { await h.click(); hora = s; break; }
  }
  await clic(page, 'Continuar', 'button'); await esperarTexto(page, 'Resumen de tu Agendamiento');
  await clic(page, 'Pagar Abono con Webpay', 'button'); await esperarTexto(page, 'con Webpay →');
  const tReserva = (Date.now() - t0) / 1000;
  await captura(page, 'uat01_3_resumen');
  await Promise.all([page.waitForNavigation({ timeout: 60000 }).catch(() => {}), clic(page, 'con Webpay →', 'button')]);
  await pagarEnWebpay(page, { aprobar: true });
  await esperarTexto(page, 'TRANSACCIÓN APROBADA', 120000);
  const tTotal = (Date.now() - t0) / 1000;
  await captura(page, 'uat01_4_aprobado');
  const t = await texto(page);
  const cod = (t.match(/\b[WA]m[a-z0-9]{6,8}\b/) || [''])[0];
  console.log(JSON.stringify({ hora, codigo: cod, seg_reserva_hasta_pago: tReserva, seg_total_con_webpay: tTotal }));
  await browser.close();
})().catch(e => { console.error('ERROR', e.message); process.exit(1); });
