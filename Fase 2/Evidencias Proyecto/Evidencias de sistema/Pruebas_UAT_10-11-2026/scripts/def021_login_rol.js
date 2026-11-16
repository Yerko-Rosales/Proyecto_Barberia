// DEF-021: el login de cliente rechaza las cuentas del personal (api_login simulado: no usa credenciales reales).
// Uso: node def021_login_rol.js [url_web]
const puppeteer = require('puppeteer-core');
const URL_WEB = process.argv[2] || 'https://yerko-rosales.github.io/Proyecto_Barberia/';
const wait = ms => new Promise(r => setTimeout(r, ms));
async function caso(browser, rol) {
  const ctx = await browser.createBrowserContext(); const p = await ctx.newPage();
  await p.setViewport({ width: 1280, height: 900 });
  const errs = []; p.on('pageerror', e => errs.push(e.message));
  await p.setRequestInterception(true);
  p.on('request', r => {
    const H = { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': '*', 'Access-Control-Allow-Methods': 'POST, OPTIONS' };
    if (r.url().includes('/rpc/api_login') && r.method() === 'OPTIONS') return r.respond({ status: 204, headers: H });
    if (r.url().includes('/rpc/api_login')) {
      return r.respond({ status: 200, contentType: 'application/json', headers: H,
        body: JSON.stringify({ id: 999, full_name: 'Prueba ' + rol, email: 'prueba@ejemplo.cl', phone: '', role: rol, client_id: rol === 'CLIENTE' ? 999 : null, points: 0 }) });
    }
    r.continue();
  });
  await p.goto(URL_WEB, { waitUntil: 'networkidle2', timeout: 60000 });
  await wait(1500);
  await p.type('input[type=email], input[placeholder="tu.correo@ejemplo.cl"]', 'prueba@ejemplo.cl');
  await p.type('input[type=password]', 'Prueba123');
  await p.evaluate(() => { const b = [...document.querySelectorAll('button')].find(x => x.innerText.trim().toUpperCase().includes('INICIAR SESIÓN') && x.type === 'submit') || [...document.querySelectorAll('button')].find(x => x.innerText.trim().toUpperCase() === '🔑 INICIAR SESIÓN' || x.innerText.toUpperCase().includes('INICIAR SESIÓN')); b.click(); });
  await wait(3000); 
  const t = await p.evaluate(() => document.body.innerText);
  const msg = (t.match(/Esta cuenta es de[^\n]*/) || [''])[0];
  const logged = await p.evaluate(() => localStorage.getItem('bigboys_logged_user'));
  console.log(rol.padEnd(15), '| mensaje:', msg || '(ninguno)', '| sesión cliente:', logged ? 'SÍ' : 'no', '| portal visible:', t.includes('Portal de Acceso'), '| errores:', errs.length);
  await ctx.close();
}
(async () => {
  const b = await puppeteer.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  for (const r of ['ADMIN_SUCURSAL', 'BARBERO', 'DUENO', 'CLIENTE']) await caso(b, r);
  // sesión antigua de personal guardada como cliente
  const ctx = await b.createBrowserContext(); const p = await ctx.newPage();
  await p.goto(URL_WEB, { waitUntil: 'domcontentloaded' });
  await p.evaluate(() => localStorage.setItem('bigboys_logged_user', JSON.stringify({ id: 8, fullName: 'Camila Caja', email: 'x@y.cl', role: 'ADMIN_SUCURSAL' })));
  await p.reload({ waitUntil: 'networkidle2' }); await wait(1500);
  console.log('sesión antigua de cajero tras recargar:', await p.evaluate(() => localStorage.getItem('bigboys_logged_user')) ? 'SIGUE' : 'descartada');
  await b.close();
})().catch(e => { console.error(e); process.exit(1); });
