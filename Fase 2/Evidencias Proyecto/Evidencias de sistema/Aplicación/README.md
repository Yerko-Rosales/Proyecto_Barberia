# Big Boys — App Android y sitio web

Código del Proyecto Barbería Big-Boys (PTY4614 Capstone): la app Android y el sitio web, que comparten la misma
API y base de datos en Supabase (PostgreSQL 17, 36 funciones `api_*`).

| Carpeta / archivo | Contenido |
|---|---|
| `app/` | App Android (Kotlin, Jetpack Compose, MVVM). Paquete `com.bigboys.barberia`, versión 1.1. |
| `barberia_bigboys_web/` | Sitio web (React 18 en `index.html`, Tailwind), política `privacidad.html` y servicio de correos `mail_server.js`. |
| `BigBoys_app-debug_*.apk` | APK de prueba (firma debug). La más reciente incluye el Asesor de estilo con IA. |
| `BigBoys_v1.*_release_*.apk` | APK release firmadas con la llave de subida. |

## Claves y secretos

- La app y la web **no necesitan ninguna clave privada**. Solo usan la URL de Supabase y su clave pública (`anon`),
  que está en `SupabaseClientProvider.kt` y en `index.html`. Los permisos los controla la base de datos (RLS y funciones).
- **La clave de Google Gemini no va en la app ni en la web.** Vive solo como secreto de la Edge Function `asesor-ia`
  en Supabase (`GEMINI_API_KEY` y, opcional, `GEMINI_MODEL`). El código de la función está en
  `Base de datos/supabase_19-11-2026/edge_functions/asesor-ia/index.ts`.
- `.env.example` existe porque lo pide el plugin de secretos de Gradle; hoy no define ninguna clave.
- El servicio de correos usa `barberia_bigboys_web/.env` (copiar desde `.env.example`). Ese archivo no se sube al repositorio.

## Compilar la app

1. Abrir esta carpeta en Android Studio (**File > Open**) y esperar la sincronización de Gradle.
2. Ejecutar en un emulador o teléfono (variante **debug**, firmada con `debug.keystore`).
3. Para la variante **release**, definir las variables de entorno `KEYSTORE_PATH`, `STORE_PASSWORD` y `KEY_PASSWORD`
   (alias `upload`). La llave de subida no está en el repositorio.

Con Gradle desde la terminal: `gradle assembleDebug` (APK en `app/build/outputs/apk/debug/`) y
`gradle testDebugUnitTest` (el repositorio no incluye el wrapper `gradlew`; Android Studio usa su propio Gradle).

## Probar la web

- Publicada en GitHub Pages: https://yerko-rosales.github.io/Proyecto_Barberia/ (rama `gh-pages`).
- Local: en `barberia_bigboys_web/` ejecutar `npx serve . -l 3000` y abrir http://localhost:3000.
- La cámara del Asesor IA necesita **https** o **localhost**.

## Correos de confirmación

Las confirmaciones, recordatorios y cancelaciones quedan en la tabla `notificacion` de Supabase. Los envía el servicio
`mail_server.js`, que revisa esa cola cada minuto **solo mientras está corriendo**:

```bash
cd barberia_bigboys_web
npm run mail   # usa el archivo .env
```

En producción, este servicio debe correr en el servidor de alojamiento, no en un PC.

## Asesor de estilo con IA (SC-34)

- El cliente marca la casilla de consentimiento y toma o sube una foto. La web y la app la envían a la Edge Function
  `asesor-ia`, que llama a Google Gemini (`gemini-3.8-flash`) y devuelve la forma del rostro, el cabello, la barba y el
  corte recomendado. El servicio y el precio salen siempre del catálogo.
- Big Boys no guarda la foto. El asesor usa el **plan gratuito** de la Gemini API: en ese plan Google puede usar el
  contenido para mejorar sus productos, y así lo informan la casilla y la Política de Privacidad v1.2. El límite diario
  gratuito no es público; se revisa en Google AI Studio.
- Sin foto: el modo «Ajuste Manual» recomienda por reglas, sin IA.

## Webpay (ambiente de integración)

El pago del abono se inicia y confirma desde la base de datos (`api_webpay_iniciar` y `api_webpay_confirmar`, que
llaman a Transbank). Esas funciones tienen un límite de 25 s y la llamada a Transbank espera hasta 20 s (DEF-022, ver
`Base de datos/supabase_19-11-2026/01_webpay_statement_timeout.sql`).
