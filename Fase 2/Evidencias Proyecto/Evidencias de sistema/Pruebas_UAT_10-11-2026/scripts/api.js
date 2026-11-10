// Cliente mínimo de la API publicada (mismas funciones api_* que usan la web y la app). Las claves se leen de uat.env.
const fs = require('fs'), crypto = require('crypto');
const leer = f => Object.fromEntries(fs.readFileSync(f, 'utf8').split(/\r?\n/).filter(l => /^[A-Z_]+=/.test(l)).map(l => [l.slice(0, l.indexOf('=')), l.slice(l.indexOf('=') + 1).trim()]));
const APP = 'C:/Users/User/Desktop/informatica/8°_Semestre/Proyecto_Barberia/Fase 2/Evidencias Proyecto/Evidencias de sistema/Aplicación/';
const web = leer(APP + 'barberia_bigboys_web/.env'), uat = leer(APP + 'uat.env');
const sha = s => crypto.createHash('sha256').update(String(s).trim()).digest('hex');
async function rpc(fn, body) {
  const r = await fetch(web.SUPABASE_URL + '/rest/v1/rpc/' + fn, { method: 'POST', headers: { apikey: web.SUPABASE_ANON_KEY, Authorization: 'Bearer ' + web.SUPABASE_ANON_KEY, 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
  const t = await r.text(); let j; try { j = JSON.parse(t); } catch (e) { j = t; }
  if (!r.ok) { const e = new Error((j && j.message) || t); e.status = r.status; throw e; }
  return j;
}
const CUENTAS = { dueno: ['admin@bigboys.cl', sha(uat.BB_CLAVE_DUENO)], caja: ['caja1@bigboys.cl', sha(uat.BB_CLAVE_CAJA)], barbero: ['mateo@bigboys.cl', sha(uat.BB_CLAVE_BARBERO)] };
const cred = k => ({ p_email: CUENTAS[k][0], p_password_sha256: CUENTAS[k][1] });
module.exports = { rpc, sha, cred, CUENTAS, uat };
if (require.main === module) (async () => {
  for (const k of Object.keys(CUENTAS)) {
    const s = await rpc('api_login', cred(k)).catch(e => ({ error: e.message }));
    console.log(k, '→', s && s.role ? `OK (${s.role}${s.location_id ? ', sucursal ' + s.location_id : ''})` : 'NO INGRESA ' + JSON.stringify(s).slice(0, 80));
  }
})();
