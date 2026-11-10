// CP-NF-01: 50 usuarios concurrentes consultan la disponibilidad (api_listar_reservas + api_barberos) en la API publicada.
const fs = require('fs');
const env = Object.fromEntries(fs.readFileSync(process.argv[2], 'utf8').split(/\r?\n/).filter(l => l.includes('=')).map(l => [l.slice(0, l.indexOf('=')).trim(), l.slice(l.indexOf('=') + 1).trim()]));
const URL = env.SUPABASE_URL + '/rest/v1/rpc/', KEY = env.SUPABASE_ANON_KEY;
const call = async (fn, body) => { const t = performance.now(); const r = await fetch(URL + fn, { method: 'POST', headers: { apikey: KEY, Authorization: 'Bearer ' + KEY, 'Content-Type': 'application/json' }, body: JSON.stringify(body) }); await r.text(); return { ms: performance.now() - t, ok: r.ok }; };
const pct = (a, p) => { const s = [...a].sort((x, y) => x - y); return s[Math.min(s.length - 1, Math.ceil(p / 100 * s.length) - 1)]; };
(async () => {
  await call('api_barberos', {}); // calentamiento
  for (const [fn, body] of [['api_listar_reservas', { p_formato: 'WEB' }], ['api_barberos', {}]]) {
    const res = [];
    for (let ronda = 0; ronda < 4; ronda++) res.push(...await Promise.all(Array.from({ length: 50 }, () => call(fn, body))));
    const ms = res.map(r => r.ms);
    console.log(`${fn}: ${res.length} llamadas (4 rondas de 50 concurrentes), errores ${res.filter(r => !r.ok).length}, p50 ${pct(ms, 50).toFixed(0)} ms, p95 ${pct(ms, 95).toFixed(0)} ms, máx ${Math.max(...ms).toFixed(0)} ms`);
  }
  const seq = []; for (let i = 0; i < 20; i++) seq.push((await call('api_listar_reservas', { p_formato: 'WEB' })).ms);
  console.log(`api_listar_reservas secuencial (1 usuario): p50 ${pct(seq, 50).toFixed(0)} ms, p95 ${pct(seq, 95).toFixed(0)} ms`);
})();
