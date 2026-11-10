// Verifica si el acceso antiguo «PIN Administrador» (1234) da acceso a datos desde la web publicada
const puppeteer = require('puppeteer-core');
const wait = ms => new Promise(r => setTimeout(r, ms));
(async () => {
  const b = await puppeteer.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  const p = await b.newPage(); await p.setViewport({ width: 1366, height: 900 });
  const llamadas = [];
  p.on('request', r => { if (r.url().includes('/rest/v1/rpc/')) llamadas.push(r.url().split('/rpc/')[1]); });
  await p.goto('https://yerko-rosales.github.io/Proyecto_Barberia/?v=' + Date.now(), { waitUntil: 'networkidle2' });
  const clic = async (t, sel = 'button') => p.evaluate((t, sel) => { const e = [...document.querySelectorAll(sel)].find(x => x.offsetParent && x.innerText.trim().includes(t)); if (e) { e.click(); return true; } return false; }, t, sel);
  console.log('invitado', await clic('Continuar como Invitado')); await wait(1500);
  await clic('Cerrar'); await clic('Más tarde'); await wait(800);
  const btns = await p.evaluate(() => [...document.querySelectorAll('button')].filter(x => x.offsetParent).map(x => x.innerText.trim().slice(0, 25)).filter(Boolean));
  console.log('botones:', btns.slice(0, 25).join(' | '));
  console.log('ingresar', await clic('Ingresar / Registro')); await wait(1000);
  console.log('tab admin', await clic('🛡️ Admin')); await p.screenshot({ path: 'uat/pin_modal.png' }); await wait(500);
  await p.type('input[placeholder="1234"]', '1234').catch(e => console.log('sin campo PIN'));
  await p.keyboard.press('Enter'); console.log('acceder enviado'); await wait(4000);
  await p.screenshot({ path: 'uat/pin_admin.png' });
  const txt = await p.evaluate(() => document.body.innerText);
  console.log('admin activo:', await p.evaluate(() => localStorage.getItem('bigboys_is_admin')));
  const i = txt.indexOf('Wmwb1z0ze'); console.log('reserva real:', i < 0 ? 'no aparece' : txt.slice(Math.max(0,i - 80), i + 260).replace(/\s+/g, ' '));
  console.log('correo del titular visible:', txt.includes('yer.rosales'), '| Juan Pérez (dato local):', txt.includes('Juan Pérez'));
  console.log('rpc llamadas:', [...new Set(llamadas)].join(', '));
  await b.close();
})();
