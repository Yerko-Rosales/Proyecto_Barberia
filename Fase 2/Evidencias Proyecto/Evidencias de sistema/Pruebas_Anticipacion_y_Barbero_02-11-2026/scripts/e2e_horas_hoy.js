// Verifica en la web que hoy no se puedan elegir horas pasadas ni con menos de 90 minutos
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
  await wait(700);
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
  await page.goto(URL_WEB, { waitUntil: 'networkidle2', timeout: 60000 });
  await esperar(page, 'Portal de Acceso');
  await clic(page, 'Continuar como Invitado');
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
  // Día de hoy en el calendario
  const hoy = String(Number(new Date().toLocaleDateString('sv-SE', { timeZone: 'America/Santiago' }).slice(8, 10)));
  const dias = await page.$$('div.grid-cols-7 button:not([disabled])');
  for (const d of dias) { if ((await d.evaluate(el => el.innerText.trim())) === hoy) { await d.click(); break; } }
  await wait(800);
  const horas = await page.$$eval('button', bs => bs.filter(b => /^\d{2}:\d{2} hrs/.test(b.innerText.trim()))
    .map(b => ({ hora: b.innerText.trim().replace(/\s+/g, ' '), bloqueada: b.disabled })));
  await page.screenshot({ path: path.join(OUT, 'web_horas_hoy.png') });
  const ahora = new Date().toLocaleTimeString('es-CL', { timeZone: 'America/Santiago', hour: '2-digit', minute: '2-digit' });
  console.log(JSON.stringify({ ahora, horas, errores }, null, 2));
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
