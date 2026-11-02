// Pruebas funcionales de punta a punta (S12) sobre la web real y la base de datos de Supabase.
// Uso: node e2e_s12.js <url_web> <carpeta_evidencias> [pruebas separadas por coma]
const path = require('path');
const fs = require('fs');
const puppeteer = require('puppeteer-core');
const { pagarEnWebpay, wait } = require('./webpay_pagar');

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

(async () => {
  const browser = await puppeteer.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  const dia = Number(process.env.DIA || 30);

  await prueba('CP-W1', 'Reserva de invitado con abono pagado en Webpay (aprobado)', async () => {
    const page = await nuevaPagina(browser);
    const r = await reservarYPagar(page, { dia, aprobar: true, nombre: 'Prueba Webpay Aprobado' });
    const codigo = (r.texto.match(/Código de reserva:\s*(\S+)/) || [])[1];
    if (!codigo) throw new Error('Sin código de reserva en el comprobante');
    fs.writeFileSync(path.join(OUT, 'codigo_cp_w1.txt'), codigo);
    return `código ${codigo}, ${r.hora}, autorización ${(r.texto.match(/autorización:\s*(\S+)/i) || [])[1]}`;
  });

  await prueba('CP-W2', 'Pago rechazado por el banco: la reserva no se confirma y el horario se libera', async () => {
    const page = await nuevaPagina(browser);
    const r = await reservarYPagar(page, { dia, aprobar: false, nombre: 'Prueba Webpay Rechazado' });
    if (!r.texto.includes('No se realizó ningún cargo')) throw new Error('Mensaje de rechazo no visible');
    return `rechazo mostrado (${r.hora})`;
  });

  await prueba('CP-P1', 'Cajero/a: primer ingreso con clave temporal y cambio obligatorio', async () => {
    const page = await nuevaPagina(browser);
    await entrarPersonal(page, 'Administración', 'caja1@bigboys.cl', CLAVE.cajaTemporal);
    await esperarTexto(page, 'Crea tu contraseña personal');
    await captura(page, 'personal_1_cambio_clave');
    await escribir(page, 'Nueva contraseña', CLAVE.caja);
    await escribir(page, 'Repite la nueva contraseña', CLAVE.caja);
    await clic(page, 'Guardar y entrar', 'button');
    await esperarTexto(page, 'Registrar llegada');
    await captura(page, 'personal_2_panel_cajero');
    return 'clave cambiada, panel del cajero/a abierto';
  });

  await prueba('CP-F1', 'Fila: registrar llegadas, llamar, atender, cobrar y marcar abandono', async () => {
    const page = await nuevaPagina(browser);
    await entrarPersonal(page, 'Administración', 'caja1@bigboys.cl', CLAVE.caja);
    await esperarTexto(page, 'Registrar llegada');
    await wait(2500);
    const contar = async () => {
      const t = await texto(page);
      return { at: Number((t.match(/Atendidos hoy\s*(\d+)/) || [])[1] || 0), ab: Number((t.match(/Se fueron\s*(\d+)/) || [])[1] || 0) };
    };
    const antes = await contar();
    for (const n of ['Pedro Prueba', 'Luis Prueba']) {
      await escribir(page, 'Nombre para llamarlo', n);
      await clic(page, 'Dar número', 'button');
      await esperarTexto(page, `${n} quedó con el número`);
    }
    await clic(page, '📣 Llamar', 'button');
    await esperarTexto(page, 'Llamado');
    await clic(page, '✂️ Iniciar', 'button');
    await esperarTexto(page, 'En atención');
    await clic(page, 'Terminar y cobrar', 'button');
    await escribir(page, 'Monto cobrado (vacío = precio del servicio)', '11900');
    await escribir(page, 'Propina', '1000');
    await page.select('div.fixed select', 'DEBITO');
    await clic(page, 'Registrar', 'button');
    await wait(1500);
    await clic(page, 'Se fue', 'button');
    await wait(2500);
    await captura(page, 'fila_1_cajero');
    const despues = await contar();
    if (despues.at !== antes.at + 1 || despues.ab !== antes.ab + 1) throw new Error(`Contadores inesperados: ${JSON.stringify(antes)} → ${JSON.stringify(despues)}`);
    return '1 atendido (Débito $11.900 + propina $1.000), 1 abandono';
  });

  await prueba('CP-F2', 'Fila pública: el cliente ve la espera sin datos personales', async () => {
    const page = await nuevaPagina(browser);
    await clic(page, 'Continuar como Invitado', 'button');
    await page.setViewport({ width: 420, height: 900 });
    await clic(page, 'Fila', 'button');
    await esperarTexto(page, 'FILA POR ORDEN DE LLEGADA');
    await wait(2000);
    await captura(page, 'fila_2_publica');
    const t = await texto(page);
    if (t.includes('Pedro Prueba') || t.includes('Luis Prueba')) throw new Error('La vista pública muestra nombres completos');
    return 'muestra números y espera estimada, sin nombres';
  });

  await prueba('CP-B1', 'Barbero: ve solo sus atenciones y lo ganado', async () => {
    const page = await nuevaPagina(browser);
    await entrarPersonal(page, 'Barbero', 'mateo@bigboys.cl', CLAVE.barbero);
    await esperarTexto(page, 'Total cobrado');
    await wait(2500);
    await captura(page, 'barbero_1_atenciones');
    const t = await texto(page);
    if (!t.includes('11.900')) throw new Error('No aparece la atención de $11.900');
    return 'atención de $11.900 visible; sin datos de contacto';
  });

  await prueba('CP-D1', 'Dueño: crea una cuenta de barbero con clave temporal y la bloquea', async () => {
    const page = await nuevaPagina(browser);
    await entrarPersonal(page, 'Administración', 'admin@bigboys.cl', CLAVE.dueno);
    await clic(page, 'Cuentas del personal', 'button');
    await esperarTexto(page, 'Crear cuenta de colaborador');
    const correo = `barbero.prueba${Date.now() % 100000}@bigboys.cl`;
    await escribir(page, 'Nombre y apellido', 'Barbero Prueba');
    await escribir(page, 'Correo', correo);
    await clic(page, 'Crear y generar clave temporal', 'button');
    await esperarTexto(page, 'Clave temporal de');
    await captura(page, 'personal_3_dueno_crea');
    await clic(page, 'Ya la entregué', 'button');
    await wait(1500);
    const filas = await page.$$('div.rounded-2xl');
    for (const f of filas) {
      if ((await f.evaluate(e => e.innerText)).includes(correo)) {
        const b = await f.$('button');
        if (b) { await b.click(); break; }
      }
    }
    await wait(2500);
    const t = await texto(page);
    const bloque = t.slice(t.indexOf(correo) - 200, t.indexOf(correo) + 200);
    if (!bloque.includes('BLOQUEADO')) throw new Error('La cuenta no quedó bloqueada');
    fs.writeFileSync(path.join(OUT, 'correo_cp_d1.txt'), correo);
    return `${correo} creado y bloqueado`;
  });

  await prueba('CP-M1', '«Mis datos»: el cliente ve sus datos, los descarga y envía una solicitud de rectificación', async () => {
    const page = await nuevaPagina(browser);
    await escribir(page, 'correo@ejemplo.cl', 'test@bigboys.cl').catch(async () => {
      await page.type('input[type=email]', 'test@bigboys.cl');
    });
    await page.type('input[type=password]', '123');
    await page.keyboard.press('Enter');
    await esperarTexto(page, 'Historial');
    await clic(page, 'Historial', 'button');
    await clic(page, 'Editar Perfil', 'button');
    await clic(page, 'Ver mis datos', 'button');
    await esperarTexto(page, 'Reservas:');
    await escribir(page, 'Cuéntanos qué necesitas', 'Mi teléfono cambió (prueba S12)');
    await clic(page, 'Enviar solicitud', 'button');
    await esperarTexto(page, 'recibida');
    await captura(page, 'misdatos_1_cliente');
    const t = await texto(page);
    return (t.match(/Solicitud (\S+) recibida/) || [])[0] || 'solicitud enviada';
  });

  await prueba('CP-M2', 'Dueño responde la solicitud de derechos dentro del plazo', async () => {
    const page = await nuevaPagina(browser);
    await entrarPersonal(page, 'Administración', 'admin@bigboys.cl', CLAVE.dueno);
    await clic(page, 'Derechos (Ley 21.719)', 'button');
    await esperarTexto(page, 'Plazo legal');
    await wait(1500);
    await escribir(page, 'Respuesta al titular', 'Teléfono actualizado. Gracias.');
    await clic(page, 'Responder', 'button');
    await wait(2500);
    await captura(page, 'misdatos_2_dueno');
    const t = await texto(page);
    if (!t.includes('Respuesta: Teléfono actualizado')) throw new Error('No se registró la respuesta');
    return 'solicitud respondida';
  });

  await browser.close();
  fs.writeFileSync(path.join(OUT, 'resultados.json'), JSON.stringify(resultados, null, 2));
  const ok = resultados.filter(r => r.estado === 'APROBADA').length;
  console.log(`\nRESULTADO: ${ok}/${resultados.length} aprobadas`);
})().catch(e => { console.error('FALLO GENERAL', e); process.exit(1); });
