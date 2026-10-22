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

// Listener periódico para la cola de correos de Cloud Firestore (colección 'mail')
async function checkFirestoreMailQueue() {
  const url = 'https://firestore.googleapis.com/v1/projects/bigboys-adbe6/databases/(default)/documents/mail';
  https.get(url, (res) => {
    let raw = '';
    res.on('data', c => raw += c);
    res.on('end', async () => {
      try {
        const json = JSON.parse(raw);
        const docs = json.documents || [];
        for (const doc of docs) {
          const f = doc.fields || {};
          const status = f.status?.stringValue;
          if (status === 'pending') {
            const to = f.to?.stringValue;
            const subject = f.subject?.stringValue;
            const body = f.body?.stringValue;
            if (to && subject && body) {
              console.log(`[Firestore Queue] Despachando correo pendiente para: ${to}`);
              try {
                await sendGmailSMTP({ to, subject, body });
                console.log(`[Firestore Queue] ✓ Correo enviado a: ${to}`);

                // Actualizar estado en Firestore a 'delivered'
                const updateUrl = 'https://firestore.googleapis.com/v1/' + doc.name + '?updateMask.fieldPaths=status&updateMask.fieldPaths=deliveredAt';
                const patchData = JSON.stringify({
                  fields: {
                    status: { stringValue: 'delivered' },
                    deliveredAt: { integerValue: Date.now() }
                  }
                });
                const patchReq = https.request(updateUrl, {
                  method: 'PATCH',
                  headers: {
                    'Content-Type': 'application/json',
                    'Content-Length': Buffer.byteLength(patchData)
                  }
                });
                patchReq.write(patchData);
                patchReq.end();
              } catch (sendErr) {
                console.error(`[Firestore Queue] Error enviando a ${to}:`, sendErr.message);
              }
            }
          }
        }
      } catch (e) {}
    });
  }).on('error', () => {});
}

// Iniciar servidor
server.listen(PORT, () => {
  console.log(`====================================================`);
  console.log(`SERVIDOR OFICIAL DE CORREOS BIG BOYS BARBERÍAS CHILE`);
  console.log(`====================================================`);
  console.log(`✓ Remitente Oficial: ${GMAIL_USER}`);
  console.log(`✓ Servidor HTTP activo en: http://localhost:${PORT}`);
  console.log(`✓ Cola de Cloud Firestore 'mail' escuchando activamente.`);
  console.log(`====================================================\n`);

  // Revisar cola de Firestore cada 6 segundos
  setInterval(checkFirestoreMailQueue, 6000);
});
