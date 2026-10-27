const http = require('http');
const tls = require('tls');
const https = require('https');

const GMAIL_USER = process.env.GMAIL_USER || 'bigboysbarberia.chile@gmail.com';
const GMAIL_PASS = process.env.GMAIL_PASS;
if (!GMAIL_PASS) {
  console.error('Falta GMAIL_PASS. Crea el archivo .env (ver .env.example) e inicia con: npm run mail');
  process.exit(1);
}
const PORT = 3001;

// Envío nativo SMTP TLS 465 hacia Gmail
function sendGmailSMTP({ to, subject, body }) {
  return new Promise((resolve, reject) => {
    const socket = tls.connect(465, 'smtp.gmail.com', () => {
      // Conectado
    });

    let step = 0;
    let timer = setTimeout(() => {
      try { socket.destroy(); } catch (e) {}
      reject(new Error('SMTP timeout'));
    }, 15000);

    socket.setEncoding('utf8');
    socket.on('data', (data) => {
      if (data.startsWith('220') && step === 0) {
        step = 1;
        socket.write('EHLO localhost\r\n');
      } else if (step === 1 && data.includes('250')) {
        step = 2;
        socket.write('AUTH LOGIN\r\n');
      } else if (step === 2 && data.startsWith('334')) {
        step = 3;
        socket.write(Buffer.from(GMAIL_USER).toString('base64') + '\r\n');
      } else if (step === 3 && data.startsWith('334')) {
        step = 4;
        socket.write(Buffer.from(GMAIL_PASS).toString('base64') + '\r\n');
      } else if (step === 4 && data.startsWith('235')) {
        step = 5;
        socket.write(`MAIL FROM:<${GMAIL_USER}>\r\n`);
      } else if (step === 5 && data.startsWith('250')) {
        step = 6;
        socket.write(`RCPT TO:<${to}>\r\n`);
      } else if (step === 6 && data.startsWith('250')) {
        step = 7;
        socket.write('DATA\r\n');
      } else if (step === 7 && data.startsWith('354')) {
        step = 8;
        const msg = [
          `From: Big Boys Barbershop Chile <${GMAIL_USER}>`,
          `To: <${to}>`,
          `Subject: =?UTF-8?B?${Buffer.from(subject).toString('base64')}?=`,
          'MIME-Version: 1.0',
          'Content-Type: text/plain; charset=UTF-8',
          'Content-Transfer-Encoding: 8bit',
          '',
          body,
          '.\r\n'
        ].join('\r\n');
        socket.write(msg);
      } else if (step === 8 && data.startsWith('250')) {
        clearTimeout(timer);
        socket.write('QUIT\r\n');
        resolve({ success: true, to });
      } else if (data.startsWith('535')) {
        clearTimeout(timer);
        reject(new Error('Credenciales inválidas en Gmail SMTP'));
      }
    });

    socket.on('error', (err) => {
      clearTimeout(timer);
      reject(err);
    });
  });
}

// Servidor HTTP local con CORS
const server = http.createServer(async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  if (req.url === '/api/status' && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'online', sender: GMAIL_USER }));
    return;
  }

  if (req.url === '/api/send-email' && req.method === 'POST') {
    let bodyData = '';
    req.on('data', chunk => bodyData += chunk);
    req.on('end', async () => {
      try {
        const payload = JSON.parse(bodyData || '{}');
        const { to, subject, body } = payload;
        if (!to || !subject || !body) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ success: false, error: 'Faltan parámetros: to, subject, body' }));
          return;
        }

        console.log(`[SMTP] Enviando correo a: ${to} | Asunto: ${subject}`);
        const result = await sendGmailSMTP({ to, subject, body });
        console.log(`[SMTP] ✓ Despacho exitoso hacia: ${to}`);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, to }));
      } catch (err) {
        console.error('[SMTP] Error enviando correo:', err.message);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: err.message }));
      }
    });
    return;
  }

  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: 'Not Found' }));
});

// Cola de correos de Supabase (tabla notificacion): confirmación, recordatorio 24 h antes,
// cancelación y avisos de derechos del titular. La base solo entrega la cola a quien tiene
// la CLAVE_SERVICIO_CORREO (archivo .env, fuera del repositorio).
const SUPABASE_URL = process.env.SUPABASE_URL || 'https://dgrflhgotxljjiejckoj.supabase.co';
const SUPABASE_ANON_KEY = process.env.SUPABASE_ANON_KEY;
const CLAVE_SERVICIO_CORREO = process.env.CLAVE_SERVICIO_CORREO;

async function rpc(fn, params) {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/rpc/${fn}`, {
    method: 'POST',
    headers: { apikey: SUPABASE_ANON_KEY, Authorization: `Bearer ${SUPABASE_ANON_KEY}`, 'Content-Type': 'application/json' },
    body: JSON.stringify(params)
  });
  if (!res.ok) throw new Error(`${fn}: HTTP ${res.status} ${await res.text()}`);
  return res.json();
}

function cuerpoCorreo(n) {
  const saludo = `Hola ${n.nombre || ''},\n\n`;
  const detalle = n.codigo
    ? `Código de reserva: ${n.codigo}\nFecha y hora: ${n.fecha}\nServicio: ${n.servicio}\nBarbero: ${n.barbero}\nSucursal: ${n.sucursal} (${n.direccion})\n`
    : '';
  const pie = '\nBarbería Big Boys · Talagante y Peñaflor\nEste correo se envió automáticamente; no respondas a esta dirección.';
  switch (n.tipo) {
    case 'CONFIRMACION':
      return saludo + 'Tu reserva quedó confirmada.\n\n' + detalle +
        `Abono pagado: $${Number(n.abono || 0).toLocaleString('es-CL')} (se descuenta del total en el local).\n` +
        'Puedes reagendar hasta 24 horas antes (máximo 2 veces). Si anulas con menos de 24 horas o no asistes, el abono se pierde.\n' + pie;
    case 'RECORDATORIO':
      return saludo + 'Te recordamos tu reserva de mañana.\n\n' + detalle + pie;
    case 'CANCELACION':
      return saludo + 'Tu reserva fue cancelada.\n\n' + detalle + pie;
    case 'DERECHOS':
      return saludo + `${n.asunto}.\nPuedes revisar el estado en «Mis datos» de tu perfil. Respondemos dentro de 30 días (Ley N° 21.719).\n` + pie;
    default:
      return saludo + n.asunto + '\n' + pie;
  }
}

async function procesarColaSupabase() {
  if (!SUPABASE_ANON_KEY || !CLAVE_SERVICIO_CORREO) return;
  try {
    const pendientes = await rpc('api_correo_pendientes', { p_clave: CLAVE_SERVICIO_CORREO });
    for (const n of pendientes) {
      try {
        await sendGmailSMTP({ to: n.to, subject: n.asunto, body: cuerpoCorreo(n) });
        await rpc('api_correo_marcar', { p_clave: CLAVE_SERVICIO_CORREO, p_id: n.id, p_enviado: true });
        console.log(`[Cola Supabase] ✓ ${n.tipo} enviado a ${n.to}`);
      } catch (err) {
        // Queda PENDIENTE y se reintenta en el siguiente ciclo (p. ej., si Gmail exige iniciar sesión)
        console.error(`[Cola Supabase] ✗ ${n.tipo} a ${n.to}: ${err.message} (se reintentará)`);
        break;
      }
    }
  } catch (err) {
    console.error('[Cola Supabase] No se pudo leer la cola:', err.message);
  }
}

// Iniciar servidor
server.listen(PORT, () => {
  console.log(`====================================================`);
  console.log(`SERVIDOR OFICIAL DE CORREOS BIG BOYS BARBERÍAS CHILE`);
  console.log(`====================================================`);
  console.log(`✓ Remitente Oficial: ${GMAIL_USER}`);
  console.log(`✓ Servidor HTTP activo en: http://localhost:${PORT}`);
  console.log(CLAVE_SERVICIO_CORREO ? '✓ Cola de correos de Supabase activa (cada 60 s).' : '⚠ Falta CLAVE_SERVICIO_CORREO en .env: la cola de Supabase no se procesa.');
  console.log(`====================================================\n`);

  // Revisar la cola de correos de Supabase al iniciar y cada 60 segundos
  procesarColaSupabase();
  setInterval(procesarColaSupabase, 60000);
});
