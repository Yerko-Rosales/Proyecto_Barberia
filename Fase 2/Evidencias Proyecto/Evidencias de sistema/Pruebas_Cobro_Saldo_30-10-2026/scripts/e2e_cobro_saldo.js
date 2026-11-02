// Prueba de punta a punta del cobro del saldo en la web (panel del cajero/a)
// Uso: node e2e_cobro_saldo.js <url_web> <carpeta_evidencias> <nombre_cliente>
const path = require('path');
const fs = require('fs');
const puppeteer = require(process.env.PP_PATH || 'puppeteer-core');

const URL_WEB = process.argv[2] || 'http://127.0.0.1:3000/';
const OUT = process.argv[3] || 'evidencias';
const CLIENTE = process.argv[4] || 'Prueba Cobro Saldo';
const CLAVE_CAJA = process.env.BB_CLAVE_CAJA;
fs.mkdirSync(OUT, { recursive: true });
const wait = (ms) => new Promise(r => setTimeout(r, ms));

async function esperarTexto(page, t, ms = 30000) {
  const fin = Date.now() + ms;
  while (Date.now() < fin) {
    const ok = await page.evaluate((x) => document.body.innerText.toLowerCase().includes(x.toLowerCase()), t).catch(() => false);
    if (ok) return;
    await wait(500);
  }
  await page.screenshot({ path: path.join(OUT, 'debug_' + t.replace(/[^a-z0-9]/gi, '_').slice(0, 20) + '.png') });
  throw new Error(`No apareció «${t}»`);
}
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

(async () => {
  const browser = await puppeteer.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 900 });
  const errores = [];
  page.on('pageerror', e => errores.push(e.message));
  await page.goto(URL_WEB, { waitUntil: 'networkidle2', timeout: 60000 });
  await esperarTexto(page, 'Portal de Acceso', 60000);

  // Ingreso del cajero/a de la sucursal 1
  await clic(page, 'Administración');
  await page.type('input[placeholder="correo@bigboys.cl"]', 'caja1@bigboys.cl');
  await page.type('input[placeholder="Contraseña"]', CLAVE_CAJA);
  await clic(page, 'Ingresar →');
  await esperarTexto(page, 'Registrar llegada');
  await clic(page, 'Reservas de hoy');
  await esperarTexto(page, CLIENTE);

  // Botón «Cobrar saldo» de la fila del cliente de prueba
  const fila = await page.evaluateHandle((c) => [...document.querySelectorAll('div')].reverse()
    .find(d => d.innerText.toLowerCase().includes(c.toLowerCase()) && d.querySelector('button') && d.innerText.length < 200), CLIENTE);
  const boton = await fila.$('button');
  await boton.click();
  await esperarTexto(page, 'Saldo a pagar en el local');
  await wait(800);
  const modal = await page.$eval('div.fixed form', f => f.innerText);
  await page.screenshot({ path: path.join(OUT, 'web_cobro_1_saldo.png') });
  if (!modal.includes('8.900')) throw new Error('El saldo mostrado no es $8.900: ' + modal);

  await page.select('div.fixed form select', 'DEBITO');
  const tip = (await page.$$('div.fixed form input[type=number]'))[1];
  await tip.click({ clickCount: 3 });
  await tip.type('1000');
  await clic(page, 'Registrar cobro');
  await esperarTexto(page, 'saldo de');
  await wait(1000);
  await page.screenshot({ path: path.join(OUT, 'web_cobro_2_registrado.png') });
  const aviso = await page.evaluate(() => [...document.querySelectorAll('div')].map(d => d.innerText).find(t => t.startsWith('✓') && t.includes('saldo de')));

  console.log(JSON.stringify({ modal: modal.replace(/\n+/g, ' | '), aviso, errores }, null, 2));
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
