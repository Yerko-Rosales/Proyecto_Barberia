# La contraseña de Gmail ya no se compila en la APK (06-11-2026)

Riesgo R-03 (exposición de credenciales).

## Problema
- El Secrets Gradle Plugin copiaba `GMAIL_APP_PASSWORD`, desde el archivo local `Aplicación/.env`, a `BuildConfig`.
- `DirectGmailSender.kt` usaba ese valor como contraseña por defecto.
- Por eso, toda APK compilada en un equipo que tuviera ese `.env` llevaba la contraseña de aplicación de Gmail dentro.

## Cambio
- `DirectGmailSender.kt`: la contraseña por defecto queda vacía y ya no se lee `BuildConfig`.
  - El Dueño puede seguir configurando una cuenta de Gmail desde Ajustes; esa clave se guarda solo en su dispositivo.
  - Los correos del sistema los envía el servidor de correo, que lee la cola de Supabase.
- Se quitó `GMAIL_APP_PASSWORD` de `.env.example` y del `.env` local, que no se versiona.

## Verificación
| Verificación | Resultado |
|---|---|
| Compilación `assembleDebug` | Correcta |
| Pruebas unitarias de la app | 10 de 10 |
| Campos de `BuildConfig` generado | `APPLICATION_ID`, `BUILD_TYPE`, `VERSION_NAME`, `GEMINI_API_KEY` (valor de ejemplo); ya no está `GMAIL_APP_PASSWORD` |
| Búsqueda de la contraseña anterior en `classes*.dex` de la APK | 0 coincidencias |
