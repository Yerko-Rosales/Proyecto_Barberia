// Verifica DEF-002 y DEF-003 en la web: sin acceso por PIN y registro con aceptación obligatoria de la política.
const puppeteer = require('puppeteer-core');
const BASE = process.argv[2];
const wait = ms => new Promise(r => setTimeout(r, ms));
(async () => {
  const b = await puppeteer.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  const p = await b.newPage(); await p.setViewport({ width: 1366, height: 900 });
  const errs = []; p.on('pageerror', e => errs.push(e.message));
  const clic = async (t, sel = 'button') => p.evaluate((t, sel) => { const e = [...document.querySelectorAll(sel)].find(x => x.offsetParent && x.innerText.trim().toLowerCase().includes(t.toLowerCase())); if (e) { e.click(); return true; } return false; }, t, sel);
  const txt = () => p.evaluate(() => document.body.innerText);
  await p.goto(BASE + '?v=' + Date.now(), { waitUntil: 'networkidle2' });
  // 1) Portal: registro sin marcar la casilla
  console.log('abrir registro:', await clic('Regístrate'));
  await wait(800);
  const casillas = await p.$$('input[type=checkbox]');
  console.log('casillas visibles en el registro:', casillas.length, '| enlace a la política:', await p.evaluate(() => !!document.querySelector('a[href="./privacidad.html"]')));
  await p.screenshot({ path: 'uat/registro_casilla.png' });
  const f = await p.$('input[placeholder="Ej: Juan Pérez"]');
  if (f) {
    const form = await p.evaluateHandle(() => [...document.querySelectorAll('form')].find(x => x.querySelector('input[placeholder="Ej: Juan Pérez"]')));
    const vals = ['Prueba Portal', '+56 9 1111 3333', 'portal.uat@prueba.invalid', 'Prueba123'];
    const ins = await form.$$('input:not([type=checkbox])');
    for (let k = 0; k < ins.length && k < 4; k++) await ins[k].type(vals[k]);
    await form.evaluate(x => x.requestSubmit()); await wait(1200);
    console.log('portal sin aceptar:', (await txt()).includes('debes aceptar la Política') ? 'RECHAZADO con mensaje' : 'NO se rechazó');
    await p.screenshot({ path: 'uat/portal_sin_aceptar.png' }); }
  // 2) Invitado → «Ingresar / Registro» → sin pestaña Admin
  await p.goto(BASE + '?v=' + Date.now(), { waitUntil: 'networkidle2' });
  await clic('Continuar como Invitado'); await wait(1200); await clic('Más tarde'); await wait(600);
  await clic('Ingresar / Registro'); await wait(800);
  const t = await txt();
  console.log('pestaña Admin con PIN:', t.includes('PIN Administrador') || /🛡️ Admin/.test(t) ? 'PRESENTE' : 'eliminada');
  await clic('Registrarme'); await wait(500);
  await p.type('input[placeholder="Nombre completo *"]', 'Prueba Casilla');
  await p.type('input[placeholder="Correo electrónico *"]', 'casilla.uat@prueba.invalid');
  await p.type('input[placeholder="Teléfono celular *"]', '+56 9 1111 2222');
  await p.type('input[placeholder="Contraseña (mínimo 6 car.) *"]', 'Prueba123');
  await p.evaluate(() => [...document.querySelectorAll('form')].find(f => f.querySelector('input[placeholder="Nombre completo *"]') && f.offsetParent).requestSubmit());
  await wait(1200);
  console.log('registro sin aceptar:', (await txt()).includes('debes aceptar la Política') ? 'RECHAZADO con mensaje' : 'NO se rechazó');
  await p.screenshot({ path: 'uat/registro_sin_aceptar.png' });
  console.log('localStorage admin_config:', await p.evaluate(() => localStorage.getItem('bigboys_admin_config')));
  const r = await p.goto(BASE + 'privacidad.html', { waitUntil: 'networkidle2' });
  console.log('privacidad.html:', r.status(), (await txt()).slice(0, 80).replace(/\s+/g, ' '));
  console.log('errores JS:', errs.length ? errs.slice(0, 3) : 'ninguno');
  await b.close();
})();
