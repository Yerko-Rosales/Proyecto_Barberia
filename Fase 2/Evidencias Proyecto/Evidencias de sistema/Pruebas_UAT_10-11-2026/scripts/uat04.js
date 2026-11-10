// UAT-04 (10-11-2026): el Dueño entra a la web publicada y revisa la fila, las ventas y los reportes del día.
const puppeteer = require('puppeteer-core');
const fs = require('fs');
const { nuevaPagina, esperarTexto, clic, escribir, captura, texto, wait } = require('./uat_base');
const env = Object.fromEntries(fs.readFileSync('C:/Users/User/Desktop/informatica/8°_Semestre/Proyecto_Barberia/Fase 2/Evidencias Proyecto/Evidencias de sistema/Aplicación/uat.env', 'utf8')
  .split(/\r?\n/).filter(l => /^[A-Z_]+=/.test(l)).map(l => [l.slice(0, l.indexOf('=')), l.slice(l.indexOf('=') + 1).trim()]));

(async () => {
  const browser = await puppeteer.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  const page = await nuevaPagina(browser);
  await page.setViewport({ width: 1600, height: 1000 });
  await clic(page, 'Administración', 'button');
  await escribir(page, 'correo@bigboys.cl', 'admin@bigboys.cl');
  await escribir(page, 'Contraseña', env.BB_CLAVE_DUENO);
  await clic(page, 'Ingresar →', 'button');
  await esperarTexto(page, 'Agenda', 40000); await wait(3000);
  await captura(page, 'uat04_1_panel');
  await clic(page, 'Fila por llegada', 'button'); await wait(3000);
  const tf = await texto(page);
  const m = tf.match(/En espera\s*(\d+)[\s\S]*?Atendidos hoy\s*(\d+)[\s\S]*?Se fueron\s*(\d+)/);
  console.log('Fila sucursal 1 (panel del Dueño):', m ? `en espera ${m[1]}, atendidos ${m[2]}, se fueron ${m[3]}` : 'no se leyó');
  await captura(page, 'uat04_2_fila');
  await clic(page, 'Ventas', 'button'); await wait(2000);
  const tv = (await texto(page)).replace(/\s+/g, ' ');
  console.log('Ventas:', (tv.match(/RECAUDACI[ÓO]N TOTAL.{0,60}/i) || [''])[0], '|', (tv.match(/ABONOS GARANTIZADOS.{0,40}/i) || [''])[0]);
  await captura(page, 'uat04_3_ventas');
  await clic(page, 'Reportes Excel', 'button'); await wait(2000);
  await clic(page, 'Diario', 'button').catch(() => {}); await wait(1500);
  const tr = (await texto(page)).replace(/\s+/g, ' ');
  console.log('Reporte diario:', (tr.match(/CITAS EN PERIODO.{0,40}/i) || [''])[0], '|', (tr.match(/RECAUDACI[ÓO]N TOTAL.{0,40}/i) || [''])[0]);
  await captura(page, 'uat04_4_reportes');
  await browser.close();
})().catch(e => { console.error('ERROR', e.message); process.exit(1); });
