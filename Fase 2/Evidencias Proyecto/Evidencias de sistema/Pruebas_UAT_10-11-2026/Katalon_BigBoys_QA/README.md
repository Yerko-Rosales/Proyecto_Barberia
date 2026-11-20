# BigBoys_QA: regresión del ambiente QA/UAT en Katalon Studio

Este proyecto de Katalon Studio 10.0.0 respalda el ambiente QA/UAT de Barbería Big-Boys: la web publicada en GitHub Pages y la API `api_*` de Supabase. Se creó el 10-11-2026 y se describe en el documento 18 v1.6, sección 13 y Tabla 22.

## Contenido

| Carpeta | Qué contiene |
|---|---|
| `Test Cases/01_Web` | 8 casos (KAT-WEB-01 a 08). Prueban la web en Chrome: el portal, el registro con aceptación de la política, el ingreso como invitado sin PIN, las credenciales inválidas, el Dueño, el cajero/a, el barbero y que no se publique una cuenta de prueba (DEF-018) |
| `Test Cases/02_API` | 10 casos (KAT-API-01 a 10). Prueban la API: login por rol, catálogo, reserva sin abono, horario ocupado, permisos, caja del día, privacidad y tablas cerradas |
| `Test Suites` | Tres suites (detalle abajo) |
| `Object Repository/Web` | 23 elementos de la web, localizados por XPath |
| `Keywords/bigboys/QA.groovy` | Palabras clave propias: llamadas a la API, ingreso del personal y lectura de `uat.env` |
| `Profiles/default` | URL de la web y de la API, la clave pública, la ruta de `uat.env` y el tiempo de espera |

Las suites son:

- `TS_00_Regresion_QA_Completa`: los 18 casos.
- `TS_01_Web_UAT`: los 8 casos web.
- `TS_02_API_Supabase`: los 10 casos de API.

## Seguridad y datos

- **Contraseñas:** no están en el proyecto. Se leen de `Aplicación/uat.env` (que está fuera de git), con la ruta del perfil `RUTA_UAT_ENV`.
- **Registro de Katalon:** las contraseñas no se escriben en el log. La API las recibe como SHA-256 y en el navegador se escriben sin pasar por `setText`.
- **Datos:** los casos no crean datos. Las escrituras se prueban con rechazos (`PAGO_REQUERIDO`, `HORARIO_OCUPADO`, `SIN_PERMISO`) y la base de datos revierte la transacción.
- **Auditoría:** `api_caja_resumen` sí deja un registro de auditoría en cada consulta, como corresponde.

## Cómo ejecutarlo

1. Abre Katalon Studio y elige **File → Open Project**. Selecciona esta carpeta (`Katalon_BigBoys_QA`).
2. Confirma que `Aplicación/uat.env` tenga `BB_CLAVE_DUENO`, `BB_CLAVE_CAJA` y `BB_CLAVE_BARBERO`.
3. Abre `Test Suites/TS_00_Regresion_QA_Completa` y presiona **Run → Chrome**. Los casos de API no abren el navegador.
4. Al terminar, el reporte queda en `Reports/<fecha>/TS_00_Regresion_QA_Completa/<fecha>/`. Incluye el HTML, el CSV, `JUnit_Report.xml` y las capturas. Desde la pestaña del reporte puedes exportarlo a PDF.

Ten en cuenta:

- **ChromeDriver:** debe coincidir con la versión de Chrome. El 10-11-2026 se actualizó a la 156, en `~/.katalon/packages/KS-10.0.0/configuration/resources/drivers/chromedriver_win32/`. El driver anterior quedó como `chromedriver_130_respaldo.exe`. Si Chrome se actualiza, usa **Tools → Update WebDrivers → Chrome**.
- **Ejecución por consola:** `katalonc` necesita una licencia de Katalon Runtime Engine. Con la edición gratuita, ejecuta las suites desde la interfaz de Katalon Studio.

## Validación previa (10-11-2026)

Las pruebas se validaron antes de ejecutarlas en Katalon:

- **Casos de API:** los 10 se ejecutaron contra la API publicada con las librerías de Katalon 10.0.0.
- **Casos web:** los 7 se verificaron en Chrome 156 con los mismos localizadores; se encontraron los 40.
- **Resultado:** 17 de 17 conformes.

## Ejecuciones en Katalon Studio (11-11-2026)

| Hora | Casos | Resultado | Observación |
|---|---|---|---|
| 09:47 | 17 | 14 aprobados, 3 con error | WEB-05 a 07: en `ingresarPersonal` una variable se llamaba igual que el método `correo()`. Error del script, no de la aplicación; se renombró a `campoCorreo` |
| 10:30 | 17 | 14 aprobados, 3 con error | Mismo error: Katalon usó la palabra clave compilada antes de la corrección |
| 10:45 | 17 | 17 aprobados | Primera ejecución con la corrección |
| 10:51 | 17 | 17 aprobados | Grabada en video (`../capturas/Prueba_UAT_Katalon_11-11-2026.mp4`). El video mostró la «Cuenta de Prueba Rápida» publicada en la web: DEF-018, corregido el mismo día y cubierto por KAT-WEB-08 |
| 11:40 | 18 | 18 aprobados | Primera ejecución con KAT-WEB-08 |
| 11:45 | 18 | **18 aprobados** | Ejecución final, en 3 min 36 s (`Reports/20261111_114552`). Grabada en video (`../capturas/Prueba_UAT_Katalon_18_casos_11-11-2026.mp4`) |

Si Katalon no toma un cambio de `Keywords/`, abre el archivo en Katalon y guárdalo (Ctrl+S) para que lo compile.
