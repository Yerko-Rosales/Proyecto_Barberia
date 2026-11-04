// Recorre la web como invitado: pestañas, formulario de registro y flujo de agendamiento hasta elegir hora
const path = require('path');
const puppeteer = require(process.env.PP_PATH || 'puppeteer-core');
const URL_WEB = process.argv[2] || 'http://127.0.0.1:3000/';
const OUT = process.argv[3] || '.';
const wait = (ms) => new Promise(r => setTimeout(r, ms));

async function clic(page, t, selector = 'button') {
  const hs = await page.$$(selector);
  let mejor = null, largo = 1e9;
  for (const h of hs) {
    const n = await h.evaluate((el, x) => {
      const s = (el.innerText || '').trim();
      return el.offsetParent !== null && !el.disabled && s.toLowerCase().includes(x.toLowerCase()) ? s.length : -1;
    }, t);
    if (n >= 0 && n < largo) { mejor = h; largo = n; }
  }
  if (!mejor) throw new Error('No encontré: ' + t);
  await mejor.click();
  await wait(900);
}
async function esperar(page, t) {
  await page.waitForFunction((x) => document.body.innerText.toLowerCase().includes(x.toLowerCase()), { timeout: 30000 }, t);
}

(async () => {
  const browser = await puppeteer.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 900 });
  const errores = [];
  page.on('pageerror', e => errores.push(e.message));
  page.on('console', m => { if (m.type() === 'error') errores.push('console: ' + m.text()); });
  await page.goto(URL_WEB, { waitUntil: 'networkidle2', timeout: 60000 });
  await esperar(page, 'Portal de Acceso');
  // Formulario de registro (sin enviar)
  await clic(page, 'Regístrate aquí');
  await esperar(page, 'Nombre Completo');
  await page.screenshot({ path: path.join(OUT, 'web_registro.png') });
  await clic(page, 'Cancelar');
  await clic(page, 'Continuar como Invitado');
  const vistas = [];
  for (const tab of ['Barberos', 'Sucursales', 'Fila', 'Reseñas', 'Inicio']) {
    await clic(page, tab);
    await wait(800);
    vistas.push({ tab, texto: (await page.evaluate(() => document.body.innerText)).length });
    await page.screenshot({ path: path.join(OUT, `web_invitado_${tab}.png`) });
  }
  // Agendamiento hasta la elección de hora
  await clic(page, 'Agendar Cita');
  await esperar(page, 'Paso 1');
  const tarjetas = async () => page.$$('div[class*="cursor-pointer"][class*="rounded-2xl"]');
  await (await tarjetas())[0].click(); await wait(400);
  await clic(page, 'Continuar');
  await esperar(page, 'Paso 2');
  await clic(page, 'Mateo', 'div[class*="cursor-pointer"]');
  await clic(page, 'Continuar');
  await esperar(page, 'Paso 3');
  await (await tarjetas())[0].click(); await wait(400);
  await clic(page, 'Continuar');
  await esperar(page, 'Paso 4');
  const horas = await page.$$eval('button', bs => bs.filter(b => /^\d{2}:\d{2} hrs/.test(b.innerText.trim())).length);
  await page.screenshot({ path: path.join(OUT, 'web_invitado_agendar.png') });
  console.log(JSON.stringify({ vistas, horasMostradas: horas, errores }, null, 2));
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
