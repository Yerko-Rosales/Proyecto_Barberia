# Pruebas de la semana S12 · 27-10-2026

Proyecto Barbería Big-Boys. Pruebas funcionales y de integración de lo construido para el hito H6 (sistema integrado, 30-10):
pago del abono con Webpay Plus, fila por orden de llegada, «Mis datos» y cuentas del personal.

- **Base de datos:** Supabase «Big Boys Project» (PostgreSQL 17), migración `Base de datos/supabase_2026-10-27/05_pagos_fila_derechos_personal.sql`.
- **Pago:** Webpay Plus, ambiente de **integración** de Transbank (tarjetas de prueba; no hay cargos reales).
- **Web:** `barberia_bigboys_web/index.html` servida en local (http://127.0.0.1:3000), Chrome automatizado con Puppeteer (`scripts/e2e_s12.js`).
- **App:** APK de depuración en el emulador Android 15 (API 35), `scripts/prueba_app_emulador.py`.

## 1. API (base de datos) · 20/20 OK

Script `Base de datos/supabase_2026-10-27/06_pruebas_api_s12.sql`; resultado en `resultado_pruebas_api_s12.txt`.
Cubre reglas (reserva pendiente que vence a los 15 min, horario bloqueado mientras se paga, transiciones de la fila)
y permisos (solo el Dueño crea personal, el cajero/a opera solo su sucursal, el barbero ve solo lo suyo, la cola de correos exige clave).

## 2. Web · 9/9 aprobadas

| ID | Caso | Resultado | Evidencia |
|---|---|---|---|
| CP-W1 | Reserva de invitado con abono pagado en Webpay (aprobado) | Aprobada: reserva confirmada, autorización 1213, tarjeta ···· 6623 | `webpay_aprobado_1_resumen.png`, `webpay_aprobado_2_resultado.png` |
| CP-W2 | Pago rechazado por el banco: la reserva no se confirma y el horario se libera | Aprobada (en el reintento; la 1.ª vez el formulario de Transbank no cargó a tiempo) | `webpay_rechazado_1_resumen.png`, `webpay_rechazado_2_resultado.png` |
| CP-P1 | Cajero/a: primer ingreso con clave temporal y cambio obligatorio | Aprobada | `personal_1_cambio_clave.png`, `personal_2_panel_cajero.png` |
| CP-F1 | Fila: registrar llegadas, llamar, atender, cobrar (Débito $11.900 + propina $1.000) y marcar abandono | Aprobada | `fila_1_cajero.png` |
| CP-F2 | Fila pública: el cliente ve la espera sin datos personales | Aprobada | `fila_2_publica.png` |
| CP-B1 | Barbero: ve solo sus atenciones y lo ganado, sin datos de contacto | Aprobada | `barbero_1_atenciones.png` |
| CP-D1 | Dueño: crea una cuenta de barbero con clave temporal y la bloquea | Aprobada | `personal_3_dueno_crea.png` |
| CP-M1 | «Mis datos»: el cliente ve sus datos y envía una solicitud de rectificación | Aprobada | `misdatos_1_cliente.png` |
| CP-M2 | Dueño responde la solicitud de derechos dentro del plazo | Aprobada | `misdatos_2_dueno.png` |

## 3. App Android (emulador) · 5 verificaciones

| Caso | Resultado | Evidencia |
|---|---|---|
| Barbero ingresa con su contraseña (sin PIN) y ve sus atenciones y lo ganado | Aprobada | `app_2_barbero.png` |
| Fila pública por sucursal | Aprobada | `app_3_fila.png` |
| Reserva → panel de Webpay → se crea la transacción y se abre el navegador | Aprobada | `app_4_webpay_panel.png`, `app_8_esperando.png` |
| «Ya pagué» sin completar el pago: informa el rechazo y libera el horario | Aprobada | `app_9_resultado.png` |
| «Mis datos» en el perfil del cliente | Aprobada | `app_10_misdatos.png` |

**Pendiente:** completar el pago dentro de la app. El formulario de Webpay no carga en el Chrome del emulador
(gráficos por software), así que el pago completo desde la app debe probarse en un teléfono Android real (riesgo R-16).

## 4. Errores encontrados por las pruebas y corregidos

1. **Web:** el asistente de reserva volvía al paso 1 cada 8 segundos (la consulta periódica de reservas recreaba la lista de sucursales).
2. **Base de datos:** el commit de Webpay fallaba porque la extensión `http` exige cuerpo en el PUT; además, si el commit falla, ahora se consulta el estado real en Transbank.
3. **Web y app:** las reservas caducadas (pago no completado) bloqueaban el horario en el calendario.
4. **App:** un barbero bloqueado seguía apareciendo como disponible para reservar.
5. **Web:** el panel del cajero/a mostraba la barra de navegación del cliente.

## 5. Cuentas de demostración

Dueño (admin@bigboys.cl), cajero/a (caja1@bigboys.cl), barberos (mateo@, esteban@, cristian@, diego@bigboys.cl) y cliente (test@bigboys.cl).
Las contraseñas del personal **no se publican** en este repositorio (es público); el equipo las comparte por un canal privado.
Para repetir las pruebas se cargan como variables de entorno: `BB_CLAVE_DUENO`, `BB_CLAVE_BARBERO`, `BB_CLAVE_CAJA`, `BB_CLAVE_CAJA_TEMPORAL`.

## 6. Corrección de riesgos de seguridad (28-10-2026)

Migración `Base de datos/supabase_2026-10-27/07_seguridad_reservas.sql`; pruebas `08_pruebas_seguridad.sql` → **16/16 OK**
(`resultado_pruebas_seguridad.txt`). La regresión de la API S12 sigue en 20/20.

| Riesgo encontrado | Corrección |
|---|---|
| Con la clave pública cualquiera podía cancelar o reagendar reservas ajenas, o dejar «Confirmada» una reserva sin pagar (`api_actualizar_reserva` sin credenciales) | El cliente solo cancela o reagenda **su** reserva (el invitado indica el correo con que reservó); los demás cambios de estado son del Dueño o del cajero/a de esa sucursal; todo cambio queda en `registro_auditoria` |
| `api_crear_reserva` permitía crear reservas confirmadas o atenciones presenciales sin pagar el abono | Sin cuenta del Dueño o del cajero/a solo se crea la reserva pendiente que confirma Webpay (`PAGO_REQUERIDO`) |
| Reservar con el correo de una cuenta registrada devolvía su nombre y teléfono | Solo se devuelven los datos que escribió el propio invitado |
| El barbero veía teléfono y correo de todos los clientes en el listado de reservas | Contacto solo para el titular, el Dueño y el cajero/a de la sucursal |
| 5 claves erradas dejaban la cuenta BLOQUEADA para siempre (cualquiera podía bloquear la cuenta del Dueño) | Bloqueo temporal de 15 min (parámetros `MAX_INTENTOS_LOGIN` y `MINUTOS_BLOQUEO_LOGIN`); durante el bloqueo ninguna función acepta la clave |

**Web** (`scripts/web_seguridad.js`, Chrome automatizado): 4/4 — reserva confirmada sin pago rechazada, cancelación con otro correo
rechazada, invitado cancela con su correo, el Dueño registra y cancela una atención. Sin errores de página.
**App**: envía las credenciales de la sesión y el correo del invitado al cambiar reservas; el Dueño inicia la sesión de datos al entrar
al panel. Compilación de depuración correcta.

Riesgos que quedan (aceptados por ahora): los intentos fallidos solo se cuentan en el inicio de sesión (no en las demás funciones);
`api_usuario_existe` permite saber si un correo está registrado; el correo del invitado no es un secreto fuerte.
