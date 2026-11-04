// Verifica en la web que el registro de clientes ya no pide RUT y que la cuenta se crea igual
const path = require('path');
const puppeteer = require(process.env.PP_PATH || 'puppeteer-core');
const URL_WEB = process.argv[2] || 'http://127.0.0.1:3000/';
const OUT = process.argv[3] || '.';
const EMAIL = process.argv[4];
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
  page.on('dialog', d => { errores.push('alert: ' + d.message()); d.dismiss(); });
  await page.goto(URL_WEB, { waitUntil: 'networkidle2', timeout: 60000 });
  await esperar(page, 'Portal de Acceso');
  await clic(page, 'Regístrate aquí');
  await esperar(page, 'Correo Electrónico');
  const form = await page.$eval('form', f => ({
    texto: f.innerText,
    campos: [...f.querySelectorAll('input')].filter(i => i.offsetParent !== null).map(i => i.type + ':' + (i.placeholder || ''))
  })).catch(() => null);
  const formulario = await page.evaluate(() => {
    const f = [...document.querySelectorAll('form')].find(x => x.offsetParent !== null && /Nombre Completo/i.test(x.innerText));
    return f ? { pideRut: /\bRUT\b/i.test(f.innerText), campos: [...f.querySelectorAll('input')].filter(i => i.offsetParent !== null).map(i => i.type + ':' + (i.placeholder || '')) } : null;
  });
  await page.screenshot({ path: path.join(OUT, 'web_registro_sin_rut.png') });
  let registro = null;
  if (EMAIL && formulario) {
    const ins = await page.evaluateHandle(() => [...document.querySelectorAll('form')].find(x => x.offsetParent !== null && /Nombre Completo/i.test(x.innerText))).then(h => h.$$('input'));
    const vis = [];
    for (const i of ins) if (await i.evaluate(el => el.offsetParent !== null)) vis.push(i);
    for (const i of vis) {
      const t = await i.evaluate(el => el.type + '|' + el.placeholder);
      if (t.startsWith('checkbox')) { if (/privacidad|política/i.test(await i.evaluate(el => el.closest('label')?.innerText || el.parentElement.innerText))) await i.click(); continue; }
      if (t.startsWith('email')) await i.type(EMAIL);
      else if (t.startsWith('tel')) await i.type('+56 9 1111 2222');
      else if (t.startsWith('password')) await i.type('Prueba2026');
      else if (t.startsWith('text')) await i.type('Prueba Sin Rut');
    }
    await page.screenshot({ path: path.join(OUT, 'web_registro_sin_rut_lleno.png') });
    const btn = await page.evaluateHandle(() => [...document.querySelectorAll('form button')].find(b => b.offsetParent !== null && /Crear cuenta/i.test(b.innerText)));
    await btn.click();
    await wait(6000);
    await page.screenshot({ path: path.join(OUT, 'web_registro_sin_rut_ok.png') });
    registro = await page.evaluate(() => { try { return JSON.parse(localStorage.getItem('bigboys_logged_user')); } catch (e) { return null; } });
  }
  console.log(JSON.stringify({ formulario, registro, errores }, null, 2));
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
