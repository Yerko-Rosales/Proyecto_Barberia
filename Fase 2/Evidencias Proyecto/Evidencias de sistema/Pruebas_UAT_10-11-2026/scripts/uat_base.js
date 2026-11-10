// Pruebas funcionales de punta a punta (S12) sobre la web real y la base de datos de Supabase.
// Uso: node e2e_s12.js <url_web> <carpeta_evidencias> [pruebas separadas por coma]
const path = require('path');
const fs = require('fs');
const puppeteer = require('puppeteer-core');
const { pagarEnWebpay, wait } = require('C:/Users/User/Desktop/informatica/8°_Semestre/Proyecto_Barberia/Fase 2/Evidencias Proyecto/Evidencias de sistema/Pruebas_S12_27-10-2026/scripts/webpay_pagar');

const URL_WEB = process.argv[2] || 'http://127.0.0.1:3000/';
const OUT = process.argv[3] || 'evidencias';
const SOLO = (process.argv[4] || '').split(',').filter(Boolean);
fs.mkdirSync(OUT, { recursive: true });
const resultados = [];
// Contraseñas de las cuentas demo del personal: se leen del entorno (no se publican en el repositorio)
const CLAVE = {
  dueno: process.env.BB_CLAVE_DUENO, barbero: process.env.BB_CLAVE_BARBERO,
  caja: process.env.BB_CLAVE_CAJA, cajaTemporal: process.env.BB_CLAVE_CAJA_TEMPORAL
};

async function nuevaPagina(browser) {
  const ctx = await browser.createBrowserContext();
  const page = await ctx.newPage();
  await page.setViewport({ width: 1280, height: 900 });
  page.errores = [];
  page.on('pageerror', e => page.errores.push(e.message));
  page.on('dialog', d => d.accept());
  await page.goto(URL_WEB, { waitUntil: 'networkidle2', timeout: 60000 });
  await page.waitForFunction(() => document.body.innerText.includes('Portal de Acceso'), { timeout: 60000 });
  return page;
}
const texto = (page) => page.evaluate(() => document.body.innerText);
async function esperarTexto(page, t, ms = 30000) {
  // Sondeo tolerante a navegaciones (al volver de Webpay cambia el contexto de la página)
  const fin = Date.now() + ms;
  while (Date.now() < fin) {
    try {
      const ok = await page.evaluate((x) => document.body && document.body.innerText.toLowerCase().includes(x.toLowerCase()), t);
      if (ok) return;
    } catch (e) {}
    await wait(500);
  }
  await page.screenshot({ path: path.join(OUT, 'debug_' + t.replace(/[^a-z0-9]/gi, '_').slice(0, 20) + '.png') }).catch(() => {});
  throw new Error(`No apareció «${t}»`);
}
// Clic en el elemento visible más pequeño cuyo texto contiene t (botones, tarjetas)
async function clic(page, t, selector = 'button,a,div[class*="cursor-pointer"],label') {
  const hs = await page.$$(selector);
  let mejor = null, largo = 1e9;
  for (const h of hs) {
    const info = await h.evaluate((el, x) => {
      const s = (el.innerText || '').trim();
      return el.offsetParent !== null && !el.disabled && s.toLowerCase().includes(x.toLowerCase()) ? s.length : -1;
    }, t);
    if (info >= 0 && info < largo) { mejor = h; largo = info; }
  }
  if (!mejor) {
    await page.screenshot({ path: path.join(OUT, 'debug_clic_' + t.replace(/[^a-z0-9]/gi, '_').slice(0, 20) + '.png') });
    throw new Error('No encontré: ' + t);
  }
  await mejor.click();
  await wait(700);
}
async function escribir(page, placeholder, valor) {
  const sel = `input[placeholder="${placeholder}"],textarea[placeholder="${placeholder}"]`;
  await page.waitForSelector(sel, { visible: true });
  await page.click(sel, { clickCount: 3 });
  await page.type(sel, valor, { delay: 15 });
}
async function captura(page, nombre) { await page.screenshot({ path: path.join(OUT, nombre + '.png') }); }

async function prueba(id, nombre, fn) {
  if (SOLO.length && !SOLO.includes(id)) return;
  const t0 = Date.now();
  try {
    const detalle = await fn();
    resultados.push({ id, nombre, estado: 'APROBADA', detalle: detalle || '', seg: Math.round((Date.now() - t0) / 1000) });
    console.log('✓', id, nombre, detalle || '');
  } catch (e) {
    resultados.push({ id, nombre, estado: 'FALLIDA', detalle: e.message.split('\n')[0], seg: Math.round((Date.now() - t0) / 1000) });
    console.log('✗', id, nombre, e.message);
  }
}

async function entrarPersonal(page, pestana, correo, clave) {
  await clic(page, pestana, 'button');
  await escribir(page, 'correo@bigboys.cl', correo);
  await escribir(page, 'Contraseña', clave);
  await clic(page, 'Ingresar →', 'button');
}

async function reservarYPagar(page, { dia, aprobar, nombre }) {
  await clic(page, 'Continuar como Invitado', 'button');
  await clic(page, 'Agendar Cita', 'button');
  await esperarTexto(page, 'Paso 1');
  const tarjetas = async () => page.$$('div[class*="cursor-pointer"][class*="rounded-2xl"]');
  await (await tarjetas())[0].click(); await wait(400);
  await clic(page, 'Continuar', 'button');
  await esperarTexto(page, 'Paso 2');
  await clic(page, 'Mateo', 'div[class*="cursor-pointer"]');
  await clic(page, 'Continuar', 'button');
  await esperarTexto(page, 'Paso 3');
  await (await tarjetas())[0].click(); await wait(400);
  await clic(page, 'Continuar', 'button');
  await esperarTexto(page, 'Paso 4');
  // Día del mes actual y primera hora libre
  const dias = await page.$$('div.grid-cols-7 button:not([disabled])');
  let ok = false;
  for (const d of dias) {
    if ((await d.evaluate(e => e.innerText.trim())) === String(dia)) { await d.click(); ok = true; break; }
  }
  if (!ok) throw new Error('Día no disponible: ' + dia);
  await wait(600);
  const horas = await page.$$('button:not([disabled])');
  let hora = null;
  for (const h of horas) {
    const s = await h.evaluate(e => e.innerText.trim());
    if (/^\d{2}:\d{2} hrs$/.test(s)) { await h.click(); hora = s; break; }
  }
  if (!hora) { await captura(page, 'debug_horas'); throw new Error('Sin horas libres'); }
  await clic(page, 'Continuar', 'button');
  await esperarTexto(page, 'Resumen de tu Agendamiento');
  await escribir(page, 'Nombre completo *', nombre);
  await escribir(page, 'Teléfono de contacto *', '+56 9 1111 2222');
  if (process.env.CORREO) await escribir(page, 'Correo electrónico (opcional)', process.env.CORREO);
  await clic(page, 'Pagar Abono con Webpay', 'button');
  await esperarTexto(page, 'con Webpay →');
  await captura(page, `webpay_${aprobar ? 'aprobado' : 'rechazado'}_1_resumen`);
  await Promise.all([page.waitForNavigation({ timeout: 60000 }).catch(() => {}), clic(page, 'con Webpay →', 'button')]);
  try {
    await pagarEnWebpay(page, { aprobar, log: (m) => fs.appendFileSync(path.join(OUT, 'webpay_log.txt'), m + '\n') });
  } catch (e) { await captura(page, 'debug_webpay'); throw e; }
  await esperarTexto(page, aprobar ? 'TRANSACCIÓN APROBADA' : 'No se realizó ningún cargo', 120000);
  await captura(page, `webpay_${aprobar ? 'aprobado' : 'rechazado'}_2_resultado`);
  return { hora, texto: await texto(page) };
}

module.exports = { nuevaPagina, esperarTexto, clic, escribir, captura, texto, pagarEnWebpay, wait, setOut: (o) => {} };
