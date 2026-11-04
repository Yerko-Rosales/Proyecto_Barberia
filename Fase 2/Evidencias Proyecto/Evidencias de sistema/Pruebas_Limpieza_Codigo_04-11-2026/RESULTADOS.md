# Limpieza de código sin uso y retiro del RUT (04-11-2026)

Objetivo: dejar la web y la app sin código sin uso ni advertencias de análisis estático antes del paso al ambiente UAT (10-11-2026), sin cambiar su funcionamiento.

## Qué se quitó

**RUT del cliente** (no se guarda en la base de datos y el ERS indica que no se solicita)
- Web: campo RUT de los 3 formularios de registro, perfil, correo de bienvenida y tabla de usuarios del dueño.
- App: campo RUT del registro, perfil y lista de clientes; validación Módulo 11 y columna `rut` de la base local.
- Se conservan: el RUT del titular en el correo de devolución bancaria del abono y el RUT ficticio del ambiente de pruebas de Transbank.

**App Android**
- Paquete antiguo `com.example.bigboys` (12 archivos) y pantalla `BarberDashboardScreen.kt`, que no se usaban.
- Funciones, consultas y clases sin referencias en el ViewModel, repositorio, DAO, modelos y utilitarios; imports sin uso.
- Recursos sin uso (íconos redondos, fondo del ícono, colores y reglas de respaldo de la plantilla).
- Dependencias comentadas en `build.gradle.kts`; las dependencias quedan en el catálogo `libs.versions.toml`.
- APIs obsoletas reemplazadas (`HorizontalDivider`, íconos `AutoMirrored`, `Locale.forLanguageTag`, `fallbackToDestructiveMigration(dropAllTables = true)`).
- Base local Room versión 9 (cambió el esquema).
- Se agregó `<queries>` para Google Maps (en Android 11+ la app no veía Maps y abría el navegador).

**Web**
- Variables, funciones, props y métodos del `store` sin uso (ingreso antiguo con PIN de barbero y administrador, reserva sin Webpay, exportación TSV duplicada, etc.).
- Librería Leaflet (se cargaba pero el mapa usa Google Maps) y reglas CSS sin uso.
- El comprobante de Webpay ahora tiene el id que usa la regla de impresión: antes «Imprimir» generaba una página en blanco.
- `mail_server.js`: `require('https')` y variable sin uso.

**Pruebas automáticas de la app**
- Se eliminaron las pruebas de plantilla `ExampleInstrumentedTest` (verificaba un paquete que no es el de la app) y `GreetingScreenshotTest` (captura de un texto de ejemplo).
- `ExampleRobolectricTest` pasa a `GuestNavigationTest`: recorre las pestañas reales del invitado (antes buscaba «Historial», que el invitado no tiene, y un ingreso con PIN que ya no existe).
- `ExampleUnitTest` pasa a `ViewModelPrepopulateTest`: espera a que se carguen las 3 sucursales (antes se quedaba esperando y fallaba por tiempo).

## Resultados

| Verificación | Antes | Después |
|---|---|---|
| Advertencias del compilador Kotlin | 42 | 0 |
| Código Kotlin sin uso (`kt_unused.py`): imports / declaraciones | 19 / 29 (+22 que quedaron sin uso al quitar las primeras) | 0 / 0 |
| Android Lint: recursos sin uso / UseKtx / dependencias fuera del catálogo | 7 / 6 / 7 | 0 / 0 / 0 |
| ESLint web (`no-unused-vars` y reglas de errores) | 29 (+2 en cascada) | 0 |
| Métodos del `store` sin uso (`metodos_sin_uso.js`) | 9 | 0 |
| Pruebas unitarias de la app | 9 de 12 | 10 de 10 |
| Líneas de código | — | 6.736 menos, 432 nuevas |

Lint sigue mostrando avisos que no son de código: versiones nuevas de dependencias (`NewerVersionAvailable`, `GradleDependency`, `AndroidGradlePluginVersion`) y del ícono (`MonochromeLauncherIcon`, `IconDipSize`). Actualizar dependencias antes del UAT se deja como decisión aparte.

**Recorridos de punta a punta (sin errores de página ni cierres de la app)**
- Web: invitado (registro, 5 pestañas y agendamiento hasta elegir hora), dueño (11 pestañas) y cajero (2 pestañas).
- App en emulador: invitado (5 pestañas), cliente (4 pestañas), barbero (panel de atenciones) y dueño (3 secciones).
- Registro sin RUT en web y app: la cuenta se crea en Supabase (`usuario` + `cliente`, rol CLIENTE). Ver `Pruebas_Sin_RUT_04-11-2026`.

## Scripts

- `scripts/kt_unused.py`: imports y declaraciones Kotlin sin referencias.
- `scripts/eslint.config.mjs`: configuración de ESLint 9 + eslint-plugin-react usada sobre el código de `index.html`.
- `scripts/metodos_sin_uso.js`: métodos de objetos y clases que nadie llama.
- `scripts/e2e_invitado.js`: recorrido de la web como invitado (Puppeteer).
