/**
 * =======================================================================
 * SERVICIO EN LA NUBE GOOGLE APPS SCRIPT - BIG BOYS BARBERÍAS CHILE 💈
 * =======================================================================
 * 
 * Este script se aloja 100% GRATIS en los servidores de Google, sin tarjetas
 * de crédito ni costos, y permite enviar correos automáticos 24/7 incluso
 * cuando tu computador está apagado.
 * 
 * -----------------------------------------------------------------------
 * INSTRUCCIONES DE INSTALACIÓN (Toma menos de 2 minutos):
 * -----------------------------------------------------------------------
 * 1. Abre tu navegador e inicia sesión con el correo de la barbería:
 *    👉 bigboysbarberia.chile@gmail.com
 * 
 * 2. Entra a la página oficial de Google Apps Script:
 *    👉 https://script.google.com
 * 
 * 3. Haz clic en el botón superior izquierdo: "Nuevo proyecto" (o "New project").
 * 
 * 4. Borra el código que aparece por defecto y PEGA TODO ESTE ARCHIVO.
 * 
 * 5. Haz clic en el botón azul superior: "Implementar" -> "Nueva implementación"
 *    (o "Deploy" -> "New deployment").
 * 
 * 6. En el engranaje ⚙️ de la izquierda, selecciona: "Aplicación web" (Web app).
 * 
 * 7. Configura estos 3 campos:
 *    - Descripción: Big Boys Mailer
 *    - Ejecutar como: Yo (bigboysbarberia.chile@gmail.com)
 *    - Quién tiene acceso: Cualquier persona (Anyone)  <--- ¡MUY IMPORTANTE!
 * 
 * 8. Haz clic en "Implementar" (Deploy).
 *    Google te pedirá autorizar permisos una sola vez:
 *    - Haz clic en "Revisar permisos"
 *    - Selecciona tu cuenta bigboysbarberia.chile@gmail.com
 *    - Si te aparece "Google no ha verificado esta app", haz clic en "Avanzado" (Advanced)
 *      y luego en "Ir a Big Boys Mailer (no seguro)".
 *    - Haz clic en "Permitir" (Allow).
 * 
 * 9. ¡Listo! Google te dará una "URL de la aplicación web" (termina en /exec).
 *    Copia esa URL y pégala en la web de Big Boys (Panel Admin -> Base de Correos -> Nube).
 * =======================================================================
 */

function doPost(e) {
  try {
    var raw = e.postData ? e.postData.contents : '';
    var data = {};
    try {
      data = JSON.parse(raw);
    } catch (parseErr) {
      data = e.parameter || {};
    }

    var to = data.to;
    var subject = data.subject;
    var body = data.body;

    if (!to || !subject || !body) {
      return ContentService.createTextOutput(JSON.stringify({
        status: 'error',
        message: 'Faltan parámetros obligatorios: to, subject, body'
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // Envío oficial de correo desde la cuenta de Gmail de Big Boys
    GmailApp.sendEmail(to.trim(), subject, body, {
      name: 'Big Boys Barbershop Chile 💈',
      replyTo: 'bigboysbarberia.chile@gmail.com'
    });

    return ContentService.createTextOutput(JSON.stringify({
      status: 'success',
      deliveredTo: to.trim(),
      timestamp: new Date().toISOString()
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      status: 'error',
      message: error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function doGet(e) {
  try {
    // Soporte para envío rápido o verificación de estado (Health Check)
    if (e && e.parameter && e.parameter.to && e.parameter.subject && e.parameter.body) {
      GmailApp.sendEmail(e.parameter.to.trim(), e.parameter.subject, e.parameter.body, {
        name: 'Big Boys Barbershop Chile 💈',
        replyTo: 'bigboysbarberia.chile@gmail.com'
      });
      return ContentService.createTextOutput(JSON.stringify({
        status: 'success',
        deliveredTo: e.parameter.to.trim()
      })).setMimeType(ContentService.MimeType.JSON);
    }

    return ContentService.createTextOutput(JSON.stringify({
      status: 'online',
      service: 'Big Boys Cloud Mailer 24/7',
      sender: 'bigboysbarberia.chile@gmail.com',
      message: 'El servidor de correos en la nube de Google está activo y listo para despachar.'
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      status: 'error',
      message: error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
