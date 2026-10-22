# 💈 Big Boys Barbershop Chile · Aplicación Web Oficial

Versión web moderna, responsiva y completa de **Big Boys Barberías (Chile)**, replicando al 100% las funciones, paleta de colores y reglas de negocio de la aplicación Android.

---

## 🎨 Paleta de Colores Oficial

- **Dorado de Lujo (Gold Primary):** `#FFD700`
- **Acento Dorado (Gold Accent):** `#D4AF37` / `#B8860B`
- **Negro Mate (Dark Background):** `#121212`
- **Pizarra Oscura (Card Background):** `#1E1E1E` / `#18181B`
- **Blanco Marfil (Text Ivory):** `#F4F4F5`
- **Verde Confirmación (Green Confirm):** `#2ECC71` / `#10B981`
- **Rojo Alerta (Red Cancel):** `#E74C3C` / `#EF4444`

Soporta alternar instantáneamente entre **Modo Oscuro** y **Modo Claro** desde la barra superior.

---

## 🚀 Cómo Abrir y Probar la Aplicación

### Opción 1: Apertura Directa (Sin Terminal)
Simplemente haz **doble clic en `index.html`** y se abrirá en Google Chrome, Microsoft Edge, Firefox o cualquier navegador web.

### Opción 2: Con Servidor Local
Si tienes Node.js instalado, puedes ejecutar en la carpeta del proyecto:
```bash
npx serve . -l 3000
```
Y abrir `http://localhost:3000` en tu navegador.

---

## 🌟 Funcionalidades Incluidas

### 1. Autenticación & Registro con RUT Chileno
- Validación oficial de **RUT chileno con algoritmo Módulo 11**.
- Cifrado hash SHA-256 de contraseñas.
- Generación automática de **Cupón de Bienvenida (PIN de 4 dígitos)** para un *Perfilado de Cejas Profesional Gratis* (limitado a 100 cupos).
- Acceso en **Modo Invitado** para reservar sin registrarse.
- Acceso de **Administrador** mediante PIN de seguridad (Predeterminado: `1234`).

### 2. Catálogo Oficial de Barberos
- Los **12 maestros barberos** del proyecto original (Mateo Silva, Carlos Mendoza, Lucas Parra, Diego Alarcón, Sebastián Reyes, Felipe Soto, Matías Rojas, Misael Castro, Joaquín Guzmán, Andrés Muñoz, Mauricio Ortiz y Tomás Lagos).
- Filtros por sucursal (Talagante, Peñaflor 1, Peñaflor 2).
- Biografía completa de cada barbero, especialidad y horarios de atención.
- Calificación directa por estrellas (1 a 5).
- Botón directo de reserva que preselecciona al barbero en el asistente.

### 3. Red de Sucursales & Mapa Interactivo Leaflet
- Mapa interactivo centrado en Talagante y Peñaflor con pines dorados personalizados.
- Tarjetas con horarios, teléfonos, cantidad de sillones y botón para abrir la ruta en **Google Maps**.
- Botón para agendar directamente en la sucursal elegida.

### 4. Asistente de Reserva en 5 Pasos
- **Paso 1:** Selección de sucursal.
- **Paso 2:** Selección de barbero filtrado por sucursal.
- **Paso 3:** Selección de servicio con precios en CLP:
  - *Corte de Pelo Degrado:* $15.000 CLP
  - *Arreglo de Barba Royale:* $10.000 CLP
  - *Combo Big Boys (Pelo + Barba):* $22.000 CLP
  - *Servicio Completo VIP:* $28.000 CLP
- **Paso 4:** Calendario en tiempo real que **bloquea fechas pasadas** y detecta horas ya ocupadas para el barbero.
- **Paso 5:** Resumen formal de agendamiento, datos del cliente y aviso de abono anticipado de $5.000 CLP.

### 5. Simulador Transbank Webpay Plus (Chile)
- Selección de bancos chilenos: *BancoEstado, Banco de Chile, Santander, BCI, Itaú, Falabella, Scotiabank, BICE, Coopeuch*.
- Redirección con candado SSL HTTPS.
- Portal seguro con la identidad gráfica del banco seleccionado, RUT y generación de token dinámico (BE Pass / Mi Pass / SMS).
- Procesamiento en tiempo real con Token ID único.
- Comprobante oficial de pago electrónico con RUT de comercio (`76.438.291-K`), Orden de Compra y Código de Autorización.
- Opción de imprimir o guardar el comprobante en PDF.

### 6. Sistema de Reseñas y Calificaciones
- Promedio general en tiempo real (ej: 4.9⭐).
- Filtros por sucursal, barbero y tipo de servicio.
- Etiquetas rápidas (*Puntualidad 10/10, Corte Impecable, Ambiente VIP, etc.*).
- Formulario modal interactivo para publicar nuevas reseñas.

### 7. Panel de Administración Central (PIN: `1234`)
- **📊 Reportes & Ventas:** Gráfico de torta/donut circular SVG con desglose de ventas por barbero, porcentajes y recaudación en CLP.
- **📅 Agenda de Citas:** Tabla de control con filtros por estado, cambio de estado en vivo y opción de cancelación con plantilla de devolución de abono.
- **✉️ Correo Gmail Real:** Configuración de credenciales de remitente y clave de aplicación para despachos.
- **✂️ Corte Manual (Walk-in):** Registro rápido de clientes sin reserva previa ($0 abono).
- **💈 Gestión de Barberos:** Agregar, editar, eliminar y activar/pausar disponibilidad (Activo / No Disponible hoy).
- **🎁 Canjear Regalos:** Validador en caja del PIN de 4 dígitos para canjear el perfilado de cejas.
- **📁 Planillas Excel:** Descarga de movimientos de citas en formato TSV/CSV para abrir directamente en Microsoft Excel.
- **⭐ Moderación de Reseñas:** Revisión y eliminación de comentarios.
- **👤 Mi Perfil & PIN:** Modificación del nombre de administrador, cambio de PIN y avatar.

---

🇨🇱 **Big Boys Barberías** · Donde el estilo se junta con la distinción.
