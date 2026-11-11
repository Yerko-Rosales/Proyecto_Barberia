# Pruebas de cierre · Ciclo 1, Ciclo 2 y UAT · 10-11-2026 (S14)

Proyecto Barbería Big-Boys. Ambiente: web publicada en GitHub Pages, app Android 1.0 (APK release) y base de datos Supabase (PostgreSQL 17).
El equipo del proyecto (YR y JO) actuó como ambiente y usuarios de prueba (UAT), con las cuentas demo del Dueño, del cajero/a de la sucursal 1 y del barbero.
Los datos generados se conservan como datos de demostración (R-18, «Aceptar»). Las claves de las cuentas se leen de `Aplicación/uat.env`, que no se versiona.

## 1. Ciclo 1 · casos funcionales CP-01 a CP-77

Resultado: **51 de 77 aprobados** (24 aprobados, 19 con observación y 8 corregidos y aprobados en la re-prueba) y **26 diferidos a la V2** por la solicitud de cambio SC-30.

| Prioridad | Casos | Aprobados | Diferidos |
|---|---|---|---|
| Muy alta | 26 | 21 | 5 |
| Alta | 38 | 27 | 11 |
| Media | 12 | 3 | 9 |
| Baja | 1 | 0 | 1 |

Scripts: `Base de datos/supabase_10-11-2026/14_ciclo1_funcional.sql` (47 verificaciones en una transacción que se deshace) y `15_correcciones_uat.sql` (re-prueba). Las verificaciones de interfaz están en `scripts/`.

| Caso | Prioridad | Estado | Evidencia | Defecto |
|---|---|---|---|---|
| CP-01 Registro exitoso de cliente | Alta | Corregido y aprobado | SQL: rol CLIENTE, consentimiento SERVICIO con canal y sin promocional. La casilla de aceptación faltaba (DEF-003); corregida en web y app. | DEF-003 |
| CP-02 Registro sin aceptar la política | Alta | Corregido y aprobado | Sin aceptar la política no se crea la cuenta y se explica por qué: web (3 formularios) y app (EMU). | DEF-003 |
| CP-03 Registro con correo ya existente | Alta | Aprobado con observación | SQL: CORREO_YA_REGISTRADO, sin duplicado. No se ofrece recuperar la contraseña (función diferida, CP-10). |  |
| CP-04 Registro con contraseña débil | Alta | Aprobado con observación | COD: «abc12» y «abcdefgh» se rechazan indicando el requisito. La regla vigente es 6 caracteres con letra y número (no 8). |  |
| CP-05 Cupón de bienvenida limitado a 100 registros | Media | Diferido a la V2 (SC-30) | COD: el regalo de bienvenida se entrega sin límite de 100 registros ni PIN en la base de datos. | SC-30 |
| CP-06 Inicio de sesión exitoso | Alta | Aprobado | SQL: sesión de cliente y de cajero/a con alcance a la sucursal 1; 2 LOGIN auditados. |  |
| CP-07 Credenciales inválidas sin revelar el correo | Alta | Aprobado | SQL: misma respuesta con correo existente e inexistente; suma el contador de intentos. |  |
| CP-08 Bloqueo temporal tras 5 intentos fallidos | Alta | Aprobado con observación | SQL: tras 5 intentos la cuenta queda bloqueada 15 min. No se envía aviso por correo. |  |
| CP-09 Primer ingreso de colaborador con contraseña temporal | Muy alta | Aprobado | SQL: clave temporal con cambio obligatorio; al cambiarla se libera el acceso. |  |
| CP-10 Recuperación de contraseña | Alta | Diferido a la V2 (SC-30) | COD: no existe recuperación de contraseña por enlace (el personal recibe una clave temporal del Dueño). | SC-30 |
| CP-11 Recuperación con enlace vencido o correo no registrado | Alta | Diferido a la V2 (SC-30) | Depende de CP-10. | SC-30 |
| CP-12 Edición del perfil (rectificación directa) | Media | Aprobado con observación | SQL: teléfono actualizado y auditado. El correo no es editable desde el perfil. |  |
| CP-13 Reserva express como invitado | Alta | Aprobado con observación | E2E: invitado reserva con nombre y teléfono (Wmwb1z0ze); pop-up de bienvenida con «Más tarde». No tiene casilla promocional. |  |
| CP-14 Invitado decide registrarse desde el pop-up | Alta | Aprobado con observación | E2E: desde el pop-up se abre el registro y la cuenta se crea; el pop-up aparece antes de elegir servicio, por lo que no hay selección que conservar. |  |
| CP-15 Control de acceso por rol y sucursal | Alta | Aprobado con observación | SQL: barbero, cliente y cajero/a de otra sucursal reciben SIN_PERMISO. Los intentos rechazados no quedan en la auditoría. | DEF-002 |
| CP-16 Cierre de sesión seguro | Media | Aprobado con observación | COD: no hay tokens; cada llamada exige credenciales y al cerrar sesión se limpia el estado local. |  |
| CP-17 Nueva versión sustancial de la política | Muy alta | Diferido a la V2 (SC-30) | COD: no hay re-aceptación por nueva versión de la política. | SC-30 |
| CP-18 Otorgar y revocar consentimiento promocional | Muy alta | Aprobado | SQL: otorgar y revocar el consentimiento promocional queda con fecha. |  |
| CP-19 Crear solicitud de derecho del titular | Muy alta | Aprobado | SQL: folio, fecha límite +30 días y correo de recepción encolado. |  |
| CP-20 Solicitud de invitado sin identidad verificable | Muy alta | Aprobado con observación | COD: el invitado no puede pedir datos sin crear cuenta; no se entregan datos sin identidad verificada. |  |
| CP-21 Derecho de acceso | Muy alta | Aprobado con observación | SQL: cuenta, reservas, pagos sin datos de tarjeta, consentimientos y solicitudes; la solicitud queda RESPONDIDA. No lista destinatarios (están en la política). |  |
| CP-22 Derecho de supresión | Muy alta | Aprobado | SQL: cuenta SUPRIMIDA y anonimizada; consentimientos revocados. |  |
| CP-23 Supresión con citas futuras | Muy alta | Corregido y aprobado | SQL: la supresión con una reserva futura se permitía; corregido: RESERVAS_FUTURAS y mensaje en la web. | DEF-004 |
| CP-24 Derecho de oposición | Muy alta | Corregido y aprobado | SQL: la oposición no detenía el promocional; corregido: se revoca de inmediato. | DEF-005 |
| CP-25 Derecho de portabilidad | Muy alta | Aprobado con observación | SQL: archivo JSON estructurado y EXPORTAR auditado. Sin CSV ni enlace temporal de 24 h. |  |
| CP-26 Derecho de bloqueo | Alta | Corregido y aprobado | SQL: el bloqueo no detenía los usos no esenciales; corregido. La cuenta sigue activa para seguir la solicitud. | DEF-006 |
| CP-27 Directorio de sucursales y navegación | Alta | Aprobado | SQL: las 3 sucursales con dirección; E2E: enlace a Google Maps. |  |
| CP-28 Catálogo de servicios y barberos por sucursal | Alta | Aprobado | SQL + EMU: servicios con precio; los barberos desactivados vienen con active=false y la web y la app no los muestran. |  |
| CP-29 Cálculo de disponibilidad | Muy alta | Diferido a la V2 (SC-30) | SQL: se pudo reservar dentro de un bloqueo de horario (la jornada sí se respeta). | DEF-010 |
| CP-30 Fecha sin disponibilidad | Muy alta | Aprobado con observación | E2E: las horas ocupadas aparecen bloqueadas; no se sugiere otra fecha o barbero. |  |
| CP-31 Asignación de barbero «Cualquiera» | Media | Diferido a la V2 (SC-30) | COD: la opción «Cualquiera» existe en la fila, no en la reserva en línea. | SC-30 |
| CP-32 Agendamiento completo con abono (camino feliz) | Muy alta | Aprobado | E2E 10-11: reserva Wmwb1z0ze PENDIENTE_PAGO → CONFIRMADA, pago AUTORIZADO 1213 y correo de confirmación recibido. |  |
| CP-33 Dos clientes reservan el mismo bloque | Muy alta | Aprobado | SQL: HORARIO_OCUPADO; la base de datos rechaza el traslape. |  |
| CP-34 Pago rechazado | Muy alta | Diferido a la V2 (SC-30) | E2E: el rechazo deja la reserva PENDIENTE_PAGO, pero no hay botón para reintentar el pago. | DEF-014 |
| CP-35 Expiración de reserva impaga | Alta | Aprobado | SQL: la reserva impaga pasa a EXPIRADA y el bloque se vuelve a reservar. |  |
| CP-36 Abono leído desde Configuración | Muy alta | Diferido a la V2 (SC-30) | SQL: el abono es global ($3.000); no se aplica un 20% por sucursal. | DEF-011 |
| CP-37 El sistema no guarda datos de tarjeta | Muy alta | Aprobado | SQL: la tabla pago no tiene columnas de tarjeta. |  |
| CP-38 Reprogramación permitida | Alta | Aprobado | SQL: reagendamiento con contador 1 e historial. |  |
| CP-39 Tercer reagendamiento | Alta | Aprobado | SQL: MAXIMO_REAGENDAMIENTOS. |  |
| CP-40 Reprogramación con menos de 24 h | Alta | Aprobado | SQL: REAGENDAR_MENOS_DE_24H. |  |
| CP-41 Cancelación con 24 h o más | Alta | Diferido a la V2 (SC-30) | SQL: CANCELADA, cupo liberado, correo y auditoría; el abono no queda como saldo a favor. | DEF-009 |
| CP-42 Cancelación con menos de 24 h | Alta | Diferido a la V2 (SC-30) | SQL: cancelación < 24 h; el pago queda AUTORIZADO en vez de RETENIDO. | DEF-009 |
| CP-43 Cancelación por el local | Alta | Diferido a la V2 (SC-30) | COD: no se solicita la devolución a la pasarela. | DEF-009 |
| CP-44 Confirmación y recordatorio | Alta | Aprobado | E2E: confirmación enviada (10-11); SQL: recordatorio encolado 24 h antes. |  |
| CP-45 Fallo en el envío de notificaciones | Media | Diferido a la V2 (SC-30) | COD: el servidor de correos reintenta sin límite; no marca FALLIDA tras 3 intentos. | DEF-015 |
| CP-46 Mis citas e historial | Media | Aprobado con observación | E2E: «Mis citas» con estados y abonos (web y app); sin puntos (fidelización diferida). |  |
| CP-47 Registro y llamado de turno | Alta | Aprobado | SQL: números correlativos y EN_ESPERA → LLAMADO → EN_ATENCION → ATENDIDO, con tiempo estimado. |  |
| CP-48 Abandono de la fila | Alta | Aprobado con observación | SQL: ABANDONO reflejado en el KPI (50% en la prueba). El nombre se abrevia en la vista pública, pero no se borra a las 24 h. |  |
| CP-49 Agenda del día y bloqueo de horario | Alta | Diferido a la V2 (SC-30) | Agenda del día operativa; no hay bloqueo de horario por barbero. | DEF-010 |
| CP-50 Bloqueo sobre una reserva confirmada | Media | Diferido a la V2 (SC-30) | Depende de CP-49. | DEF-010 |
| CP-51 Confirmar servicio e iniciar atención | Muy alta | Aprobado | SQL (C-09): el cobro usa el servicio real y el barbero asignado. |  |
| CP-52 Marcar no-show | Alta | Diferido a la V2 (SC-30) | SQL: la reserva queda NO_SHOW, pero el pago no pasa a RETENIDO. | DEF-009 |
| CP-53 Solo el cajero/a cambia estados | Muy alta | Aprobado | SQL: barbero y cliente reciben SIN_PERMISO. |  |
| CP-54 Registrar término de atención | Muy alta | Diferido a la V2 (SC-30) | SQL: saldo $8.900, abono APLICADO y atención registrada por el cajero/a; no se acreditan puntos. | SC-30 |
| CP-55 Caja y resumen de ingresos | Alta | Corregido y aprobado | UAT-04: Ventas y Reportes no incluían la fila (DEF-016); corregido con «Caja del día»: 3 atenciones, $40.460 y $1.000 de propinas, por medio de pago, iguales a la caja. | DEF-016 |
| CP-56 Barbero consulta atenciones y ganancias | Alta | Aprobado | SQL: el barbero ve sus atenciones y el total, sin datos de contacto. |  |
| CP-57 Gestión de servicios | Alta | Diferido a la V2 (SC-30) | COD: no hay mantención del catálogo de servicios desde la aplicación. | SC-30 |
| CP-58 Dueño crea perfil de colaborador | Muy alta | Aprobado | SQL: alta por el Dueño, auditada, con clave temporal. |  |
| CP-59 Colaboradores no pueden crearse por otra vía | Muy alta | Aprobado | SQL: el cajero/a no crea personal y la base de datos impide asignar roles por otra vía. |  |
| CP-60 Desactivación de colaborador | Muy alta | Aprobado | SQL: colaborador desactivado no inicia sesión. |  |
| CP-61 Gestión de sucursales | Media | Diferido a la V2 (SC-30) | COD: no hay mantención de sucursales desde la aplicación. | SC-30 |
| CP-62 Configuración de parámetros | Alta | Diferido a la V2 (SC-30) | COD: no hay pantalla para versionar parámetros (se cambian en la base de datos). | SC-30 |
| CP-63 Promociones solo con consentimiento | Media | Diferido a la V2 (SC-30) | COD: no hay envío de promociones. | SC-30 |
| CP-64 Canje de código promocional | Media | Diferido a la V2 (SC-30) | COD: los cupones del panel son locales; no hay códigos en la base de datos. | SC-30 |
| CP-65 Ruleta de beneficios | Baja | Diferido a la V2 (SC-30) | COD: no existe la ruleta. | SC-30 |
| CP-66 Canje de puntos | Media | Diferido a la V2 (SC-30) | COD: no existe el canje de puntos. | SC-30 |
| CP-67 Atención de solicitudes de derechos | Muy alta | Aprobado con observación | SQL: respuesta con fecha y responsable. Sin alerta 5 días antes del vencimiento. |  |
| CP-68 Prórroga y rechazo fundado | Muy alta | Corregido y aprobado | SQL: se permitía una segunda prórroga; corregido (PRORROGA_YA_USADA) y el rechazo informa el reclamo ante la Agencia. | DEF-007 |
| CP-69 Consulta y protección de la auditoría | Alta | Aprobado con observación | SQL: la auditoría rechaza UPDATE y DELETE. El Dueño no tiene pantalla para consultarla ni exportarla. |  |
| CP-70 Política de retención | Alta | Diferido a la V2 (SC-30) | SQL: no hay proceso automático de retención (parámetros definidos: 24 y 12 meses). | SC-30 |
| CP-71 Gestión de incidente de seguridad | Alta | Diferido a la V2 (SC-30) | SQL: tabla de incidentes sin flujo en la aplicación. | SC-30 |
| CP-72 Registro de actividades de tratamiento | Media | Diferido a la V2 (SC-30) | SQL: el RAT tiene 5 actividades, pero no se exporta desde la aplicación. | SC-30 |
| CP-73 Políticas versionadas | Alta | Corregido y aprobado | SQL: las políticas tenían huella en ceros y URL por definir; corregido: publicada en privacidad.html con SHA-256 registrado. | DEF-008 |
| CP-74 Dashboard de KPIs | Alta | Aprobado con observación | SQL: KPI de abandono por sucursal; el cajero/a solo ve la suya. El subtotal Peñaflor no se muestra por separado. |  |
| CP-75 Reportes exportables | Alta | Aprobado con observación | E2E (sesión del Dueño, 06-11): reporte en Excel por período y sucursal con los totales del panel; no hay PDF. |  |
| CP-76 Uso de la app sin conexión | Alta | Aprobado con observación | EMU: en modo avión la app abre y muestra los datos guardados; la sincronización de cambios encolados no se verificó. |  |
| CP-77 Coherencia entre web y app | Muy alta | Aprobado | E2E: las reservas de la web y de la app comparten la API y el código (Amvr2q370, Wmvr463up). |  |

## 2. Ciclo 2 · casos no funcionales

| Caso | Tema | Estado | Evidencia |
|---|---|---|---|
| CP-NF-01 | Rendimiento | No aprobado (DEF-013) | Disponibilidad con 50 usuarios concurrentes (4 rondas, 200 llamadas, 0 errores): p95 1.300 ms extremo a extremo; en la base de datos la función tarda 13 ms. La diferencia es la red hasta el servidor en EE. UU. (Oregón). |
| CP-NF-02 | Disponibilidad y sin conexión | Aprobado con observación | La app abre en modo avión con los datos guardados (CP-76). La disponibilidad ≥ 99,5% se mide durante la marcha blanca. |
| CP-NF-03 | Cifrado y credenciales | Aprobado | Supabase, GitHub Pages y Transbank rechazan TLS 1.0 y 1.1 y aceptan TLS 1.2 y 1.3; las 15 claves con bcrypt ($2a$); sin secretos en el repositorio. |
| CP-NF-04 | OWASP Top 10 básico | Corregido y aprobado (DEF-002) | Inyección SQL tratada como dato; cambiar la reserva de otro cliente: SIN_PERMISO; listado público sin datos personales; RLS activo en 27 de 27 tablas y 0 políticas abiertas a anon. Se eliminó el acceso por PIN del prototipo. |
| CP-NF-05 | Usabilidad | Aprobado con observación | Reserva de 5 pasos hasta el pago en 7,4 s (UAT-01, recorrido automatizado). La medición con 5 usuarios nuevos se hace en la marcha blanca. |
| CP-NF-06 | Compatibilidad | Aprobado con observación | Flujo CP-32 completo en Chrome (Wmwb711jm) y en Edge (Wmwb7dgec); app en Android 15 (emulador) y en un teléfono real (06-11). Sin pruebas en Firefox, Safari ni Android 8. |
| CP-NF-07 | Mantenibilidad y arquitectura | Aprobado | Clientes → API (funciones api_*) → PostgreSQL, sin acceso directo a tablas; pruebas automatizadas en verde (BD, API, web y app 10/10). |
| CP-NF-08 | Privacidad por defecto | Corregido y aprobado (DEF-003, DEF-012) | Casilla de aceptación obligatoria y promocional desmarcado; auditoría con identificadores, no nombres; los 6 derechos operativos tras DEF-004 a DEF-007; sin credenciales de prueba en la APK release. |
| CP-NF-09 | Respaldo y recuperación | Aprobado | Restauración del respaldo diario en 1,8 s (06-11-2026), con conteos iguales a producción. |
| CP-NF-10 | Trazabilidad | Aprobado con observación | Ingresos, consultas de «Mis datos», exportaciones, derechos y la consulta de caja quedan en la auditoría; los cambios de configuración no tienen pantalla (se hacen en la base de datos). |
| CP-NF-11 | Residencia de datos | Aprobado | La política publicada declara la transferencia internacional a EE. UU. (Supabase), coherente con el RAT. |

## 3. Pruebas de aceptación (UAT)

| Escenario | Descripción | Estado | Resultado |
|---|---|---|---|
| UAT-01 | Cliente nuevo agenda y paga | Aprobado | Registro con aceptación de la política, reserva Wmwb711jm (24-11-2026 09:00) en 7,4 s y pago Webpay aprobado; correo de confirmación recibido en yer.rosales+uat@duocuc.cl. Ejecutado en la web publicada (la app usa la misma API, CP-77). |
| UAT-02 | Día peak por orden de llegada | Aprobado | Cajero/a de la sucursal 1: 4 llegadas con números correlativos; 3 atendidos con monto y medio de pago y 1 abandono; KPI del día 25% de abandono; ningún registro en Excel. |
| UAT-03 | Reprogramación, cancelación y no-show | Aprobado con observación | Reprogramación con más de 24 h (Wmwb711jm → 10:00); con menos de 24 h se rechaza; cancelación Wmwb73u4f; no-show Wmwb73vjz. El abono no cambia de estado al cancelar o en no-show (DEF-009, diferido). |
| UAT-04 | Cierre del día y KPIs | Corregido y aprobado (DEF-016, DEF-017) | La primera ejecución falló: Ventas y Reportes no incluían la fila y mezclaban datos de ejemplo. Tras la corrección, «Caja del día» suma 3 atenciones, $40.460 y $1.000 de propinas (EFECTIVO $11.900, DÉBITO $20.230, CRÉDITO $8.330), igual que la caja del día; agenda con las 28 reservas reales. |
| UAT-05 | Alta de colaborador y consulta del barbero | Aprobado | El Dueño crea «Barbero UAT» con clave temporal; el primer ingreso exige cambiarla; Mateo ve 2 atenciones y $20.230, sin datos de contacto. |
| UAT-06 | Ejercicio de un derecho del titular | Aprobado | Solicitud de rectificación SDMWB73YRN con plazo al 10-12-2026, respondida por el Dueño el mismo día y trazada en la auditoría. |

Satisfacción de los usuarios de prueba (criterio ≥ 4 de 5): la registran YR y JO en el Acta de Cierre (documento 13).

## 4. Registro de defectos

18 defectos: 12 cerrados y 6 diferidos a la V2 (SC-30). DEF-018 se agregó el 11-11-2026.

| ID | Casos | Descripción | Severidad | Estado | Detección | Cierre |
|---|---|---|---|---|---|---|
| DEF-001 | CP-32 | La app saltaba el paso 3 y mostraba un servicio de $15.000. | Media | Cerrado | 06-11-2026 | 10-11-2026 |
| DEF-002 | CP-15, CP-NF-04 | La web conservaba la pestaña «Admin» del prototipo: con el PIN 1234 cualquier visitante abría un panel de administración con datos locales de ejemplo (la API seguía protegida). | Alta | Cerrado | 10-11-2026 | 10-11-2026 |
| DEF-003 | CP-01, CP-02 | El registro en la web y en la app no pedía aceptar la Política de Privacidad y los T&C; el consentimiento se registraba sin acción del titular. | Alta | Cerrado | 10-11-2026 | 10-11-2026 |
| DEF-004 | CP-23 | Se podía suprimir la cuenta con reservas futuras confirmadas. | Alta | Cerrado | 10-11-2026 | 10-11-2026 |
| DEF-005 | CP-24 | La oposición quedaba registrada, pero no detenía el tratamiento promocional. | Alta | Cerrado | 10-11-2026 | 10-11-2026 |
| DEF-006 | CP-26 | El bloqueo quedaba registrado, pero no detenía los usos no esenciales. | Media | Cerrado | 10-11-2026 | 10-11-2026 |
| DEF-007 | CP-68 | Se permitía una segunda prórroga y el rechazo no informaba el derecho a reclamar. | Media | Cerrado | 10-11-2026 | 10-11-2026 |
| DEF-008 | CP-73 | Las políticas no estaban publicadas: URL por definir y huella SHA-256 en ceros. | Media | Cerrado | 10-11-2026 | 10-11-2026 |
| DEF-012 | CP-NF-08 | La APK release mostraba una tarjeta «usuario de prueba» con correo y clave. | Media | Cerrado | 10-11-2026 | 10-11-2026 |
| DEF-009 | CP-41, CP-42, CP-43, CP-52 | El estado del abono no cambia al cancelar o en no-show (saldo a favor, RETENIDO, REEMBOLSADO); las reglas de 24 h y de reagendamiento sí se aplican. | Alta | Diferido (SC-30) | 10-11-2026 | — |
| DEF-010 | CP-29, CP-49, CP-50 | Los bloqueos de horario de un barbero no impiden reservar. | Media | Diferido (SC-30) | 10-11-2026 | — |
| DEF-011 | CP-36 | El abono no se puede definir por sucursal, servicio o porcentaje (es global y parametrizable). | Media | Diferido (SC-30) | 10-11-2026 | — |
| DEF-013 | CP-NF-01 | Con 50 usuarios concurrentes la disponibilidad responde en 1,3 s (p95) extremo a extremo; en la base de datos tarda 13 ms. | Media | Diferido (SC-30) | 10-11-2026 | — |
| DEF-014 | CP-34 | Tras un pago rechazado no hay botón para reintentar; el cliente debe reservar de nuevo. | Baja | Diferido (SC-30) | 10-11-2026 | — |
| DEF-015 | CP-45 | El servidor de correos reintenta sin límite y no marca FALLIDA tras 3 intentos. | Baja | Diferido (SC-30) | 10-11-2026 | — |
| DEF-016 | UAT-04, CP-55 | Ventas y Reportes del Dueño no incluían las atenciones de la fila: el cierre de caja no cuadraba. | Alta | Cerrado | 10-11-2026 | 10-11-2026 |
| DEF-017 | UAT-04, CP-74 | La web cargaba citas, reseñas y correos de ejemplo del prototipo y los mezclaba con los datos reales. | Media | Cerrado | 10-11-2026 | 10-11-2026 |
| DEF-018 | CP-NF-04, CP-NF-08 | La web publicada mostraba una «Cuenta de Prueba Rápida» con correo y clave que inicia sesión en una cuenta de cliente real, y aceptaba esa cuenta en el navegador sin consultar la base de datos. Se detectó en el video de la regresión en Katalon (11-11-2026). Evidencia: `capturas/def018_*.png`. | Media | Cerrado | 11-11-2026 | 11-11-2026 |

## 5. Archivos

- `scripts/`: verificaciones de interfaz y UAT (Node + Puppeteer y API publicada). `api.js` lee las claves de `uat.env` (no versionado).
- `capturas/`: pantallas de la web y la app usadas como evidencia.
