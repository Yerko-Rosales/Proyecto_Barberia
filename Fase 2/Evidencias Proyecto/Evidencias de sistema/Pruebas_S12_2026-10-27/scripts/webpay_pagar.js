// Completa el formulario de Webpay (integración) con una tarjeta de prueba y devuelve la URL de retorno.
// Uso como módulo: await pagarEnWebpay(page, { tarjeta, aprobar }) con la página ya en el formulario de Webpay.
const wait = ms => new Promise(r => setTimeout(r, ms));

async function visibles(page) {
  return page.evaluate(() => Array.from(document.querySelectorAll('input,button,select'))
    .filter(e => e.offsetParent !== null)
    .map(e => `${e.tagName}#${e.id}.${(e.className || '').toString().split(' ')[0]}[type=${e.type}] ${(e.innerText || e.placeholder || '').trim().slice(0, 30)}`));
}

async function clickText(page, text) {
  const ok = await page.evaluate((t) => {
    const el = Array.from(document.querySelectorAll('button,a')).find(b => b.offsetParent !== null && (b.innerText || '').trim().toLowerCase().includes(t.toLowerCase()));
    if (el) { el.click(); return true; }
    return false;
  }, text);
  if (!ok) throw new Error('No encontré el botón: ' + text + '\n' + (await visibles(page)).join('\n'));
}

async function pagarEnWebpay(page, { tarjeta = '4051885600446623', aprobar = true, log = () => {} } = {}) {
  await page.waitForSelector('#tarjetas', { timeout: 60000 });
  await page.click('#tarjetas');
  await page.waitForSelector('#card-number', { visible: true });
  await page.click('#card-number');
  await page.keyboard.type(tarjeta, { delay: 60 });
  await page.evaluate(() => {
    const el = document.querySelector('#card-number');
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
    el.dispatchEvent(new Event('blur', { bubbles: true }));
  });
  await wait(800);
  log('tras número: ' + (await visibles(page)).join(' | '));
  // Vencimiento y CVV aparecen tras el número
  const exp = await page.$('#card-exp');
  if (exp) await page.type('#card-exp', '1228', { delay: 30 });
  const cvv = await page.$('#card-cvv');
  if (cvv) await page.type('#card-cvv', '123', { delay: 30 });
  await wait(500);
  // Según el tipo de tarjeta, Webpay muestra «Continuar» (luego cuotas) o «Pagar» directamente
  try { await clickText(page, 'Continuar'); } catch (e) { await clickText(page, 'Pagar'); }
  await wait(2500);
  log('tras continuar: ' + (await visibles(page)).join(' | '));
  // Cuotas (crédito): se aceptan sin cuotas si aparece la opción
  const pagar = page.url().includes('webpayserver/dist') &&
    await page.evaluate(() => Array.from(document.querySelectorAll('button')).some(b => b.offsetParent !== null && /pagar/i.test(b.innerText)));
  if (pagar) { await clickText(page, 'Pagar'); await wait(3000); }
  log('tras pagar: ' + page.url() + ' | ' + (await visibles(page)).join(' | '));
  // Simulador del banco: RUT 11.111.111-1 y clave 123
  await page.waitForSelector('#rutClient, input[name=rutClient]', { timeout: 60000 });
  await page.type('#rutClient, input[name=rutClient]', '111111111', { delay: 20 });
  await page.type('#passwordClient, input[name=passwordClient]', '123', { delay: 20 });
  await page.click('input[type=submit], button[type=submit]');
  await wait(3000);
  log('banco: ' + page.url() + ' | ' + (await visibles(page)).join(' | '));
  // Aceptar o rechazar la autorización
  await page.waitForSelector('select', { timeout: 60000 });
  await page.select('select', aprobar ? 'TSY' : 'TSN');
  await Promise.all([
    page.waitForNavigation({ waitUntil: 'networkidle2', timeout: 90000 }).catch(() => {}),
    page.click('input[type=submit], button[type=submit]')
  ]);
  // Webpay muestra una página intermedia y luego vuelve al comercio
  for (let i = 0; i < 30 && !page.url().includes('127.0.0.1') && !page.url().includes('github.io'); i++) await wait(1000);
  return page.url();
}

module.exports = { pagarEnWebpay, visibles, clickText, wait };
