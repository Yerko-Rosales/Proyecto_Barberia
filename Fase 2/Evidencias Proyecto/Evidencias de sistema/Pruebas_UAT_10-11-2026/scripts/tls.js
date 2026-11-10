// CP-NF-03: ¿aceptan los servidores TLS 1.0/1.1? Se intenta el handshake forzando cada versión.
const tls = require('tls');
const hosts = ['dgrflhgotxljjiejckoj.supabase.co', 'yerko-rosales.github.io', 'webpay3gint.transbank.cl'];
const prueba = (host, v) => new Promise(res => {
  const s = tls.connect({ host, port: 443, servername: host, minVersion: v, maxVersion: v }, () => { res(`${v}: ACEPTA (${s.getProtocol()})`); s.end(); });
  s.on('error', e => res(`${v}: rechaza (${e.code || e.message.slice(0, 40)})`)); s.setTimeout(8000, () => { res(`${v}: sin respuesta`); s.destroy(); });
});
(async () => { for (const h of hosts) console.log(h, '→', (await Promise.all(['TLSv1', 'TLSv1.1', 'TLSv1.2', 'TLSv1.3'].map(v => prueba(h, v)))).join(' | ')); })();
