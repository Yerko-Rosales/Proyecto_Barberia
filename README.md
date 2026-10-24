# Proyecto Barbería Big-Boys

Sistema de gestión y agendamiento para **Barbería Big-Boys** (plataforma web + app móvil Android), que **complementa** la atención por orden de llegada y **reemplaza por completo** el registro manual en Excel en sus tres sucursales.

**Estudiantes:** Yerko Rosales (Jefe de Proyecto, Analista y UX/UI) · Jonathan Ojeda (Backend y Base de Datos)
**Asignatura / Docente:** PTY4614 Capstone / Fernando Herrera
**Período:** 14-08-2026 al 13-11-2026 (semana 1 = 10 al 16-08-2026; entrega final en S14) · Defensa probable: 27-11-2026 (S16)
**Fases:** F1 14-08 al 04-09 · F2 07-09 al 18-10 · F3 19-10 al 13-11

---

## Sucursales

| Código | Sucursal | Dirección |
| --- | --- | --- |
| SUC-01 | Peñaflor – Sucursal 21 de Mayo | 21 de Mayo 4566, Peñaflor |
| SUC-02 | Peñaflor – Sucursal Alcalde Luis Araya Cereceda | Alcalde Luis Araya Cereceda 4387, Peñaflor |
| SUC-03 | Talagante | Boulevard del Centro, local 3, Talagante |

## Problema y oportunidad (día peak)

En días de mayor demanda se atienden ~130 clientes y **26 se van sin atención (16,7%)**: ~4.326 clientes y **$51.484.160** perdidos al año, valorizados solo con el precio del corte ($11.900), ya que no se sabe qué servicio habría tomado cada cliente. Rango de sensibilidad: $36,0 M (Barba, $8.330) a $103,0 M (S. Completo, $23.800). Supuestos por validar en el Plan de Costos.

## Servicios y precios

| Servicio | Precio |
| --- | --- |
| Corte | $11.900 |
| Corte + Cejas | $15.470 |
| Corte + Barba | $20.230 |
| S. Completo | $23.800 |
| Corte + Cejas + Lavado | $17.850 |
| Barba | $8.330 |
| Lavado (complemento) | $3.570 |
| Cejas (complemento) | $3.570 |
| Mascarillas Faciales (complemento) | $2.380 |

## Producto

- Perfil de cliente, agendamiento del servicio habitual o nuevo e ingreso como invitado (pop-up promocional).
- **Abono de reserva de $3.000** fijo y parametrizable (por sucursal o servicio); se descuenta del servicio. Reagendar con ≥ 24 h (máx. 2 veces); anulación < 24 h o no-show pierde el abono.
- Fila digital por orden de llegada y registro digital de atenciones (reemplazo del Excel).
- KPIs por sucursal, subtotal Peñaflor y consolidado.
- **Cumplimiento anticipado de la Ley N° 21.719**: consentimientos, módulo «Mis datos» (acceso, rectificación, supresión, oposición, portabilidad, bloqueo), auditoría, retención y políticas.
- Arquitectura: web (React) + app Android (Kotlin/MVVM) → API REST → **PostgreSQL** (base de datos relacional). La API la publica Supabase mediante funciones `api_*` (SC-20).

## Documentación (`Fase 2/Evidencias Proyecto/Evidencias de documentación/`)

| Documento | Descripción |
| --- | --- |
| 00 Inventario y Resumen de Cambios | Qué se actualizó el 06-10-2026 y supuestos por validar |
| 02 a 15 | Planes de gestión (interesados, dirección, requerimientos, alcance, EDT, Gantt, costos, calidad, recursos, cambios, avance, cierre, riesgos) |
| 16 Diseño de Base de Datos | MER, modelo relacional, diccionario y DDL |
| 17 Especificación de Casos de Uso | 41 casos de uso (CU-01 a CU-41) |
| 18 Plan y Casos de Prueba | 77 casos funcionales, 24 pruebas automatizadas de BD, 11 no funcionales y 6 escenarios de aceptación |
| Anexo A | Cumplimiento normativo y gobierno de datos |
| Acta, Kick-Off, ERS rev. 04.1 (63 RF / 11 RNF), DAS v3.2, RACI | Documentos base del proyecto |
| KPIs y Costos (xlsx), Política de Privacidad y T&C | Indicadores, costos y textos legales |
| Diagramas/ | 35 diagramas (PNG, SVG,  y StarUML `.mdj`) |
| `Evidencias de sistema/Base de datos/` | Script `bigboys_ddl.sql` (27 tablas) validado: 24/24 pruebas; migraciones de Supabase y pruebas de la API en `supabase_2026-10-22/` |

## Estructura del repositorio

```
├── Fase 1/ (Evidencias individuales y grupales)
├── Fase 2/
│   ├── Evidencias Grupales/      # Guías 2.4 y 2.6
│   ├── Evidencias Individuales/  # Diarios de reflexión 2.1
│   ├── Evidencias Proyecto/
│   │   ├── Avance_Proyecto_Barberia*.pptx (original, v2 y v3) y Seguimiento/Roadmap 09-10
│   │   ├── Evidencias de documentación/  # Documentos del proyecto
│   │   └── Evidencias de sistema/
│   │       ├── Aplicación/               # Código de la web y la app (sin secretos; ver .env.example)
│   │       └── Base de datos/            # Script DDL y diagramas de BD
│   └── Seguimiento semanal/
└── Fase 3/ (Presentación final y diarios 3.1)
```

## Estado del proyecto (22-10-2026, semana 11 de 16)

- **Fase 2 cerrada (H4, 18-10-2026):** documentación de diseño completa (4+1, modelo relacional, cumplimiento Ley N° 21.719, KPIs y costos, plan y casos de prueba).
- **Fase 3 en curso:** próximo hito H5, presentación de avance (viernes 23-10-2026); luego H6 sistema integrado, H7 UAT, H8 entrega final (13-11) y H9 defensa (27-11).
- **Base de datos:** el 20-10-2026 se implementó el modelo relacional en Supabase (PostgreSQL 17, región us-west-2, EE. UU.), con RLS activo. La transferencia internacional quedó declarada (SC-18). El 22-10-2026 se agregó la tabla `resena` (modelo v1.2, SC-21): 27 tablas y 24/24 pruebas.
- **Desarrollo:** desde el 22-10-2026 la web y la app usan el modelo relacional a través de 13 funciones `api_*` que Supabase publica como API REST (SC-20). Las tablas siguen cerradas a la clave pública y las tablas del prototipo se eliminaron. Se probaron la API (17/17), la web de punta a punta y la compilación de la app. El código está en el repositorio sin secretos: la clave de correo se lee desde archivos `.env` locales excluidos de git (ver `.env.example`). Cuenta de prueba: `test@bigboys.cl` / `123`.
- **Documentación:** el 22-10-2026 se alinearon todos los documentos con este estado (detalle en la sección 8 del documento 00): DAS 3.2, ERS rev. 04.1, DIAG-13, DIAG-27, DIAG-28 y DIAG-31 actualizados, 17 riesgos (R-14 cerrado; R-15 a R-17 agregados el 23-10-2026) y 21 solicitudes de cambio.
