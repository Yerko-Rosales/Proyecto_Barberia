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
| 00 Inventario y Resumen de Cambios | Qué se actualizó desde el 06-10-2026 (sección 10: seguimiento del 06-11-2026) y supuestos por validar |
| 02 a 15 | Planes de gestión (interesados, dirección, requerimientos, alcance, EDT, Gantt, costos, calidad, recursos, cambios, avance, cierre, riesgos) |
| 16 Diseño de Base de Datos | MER, modelo relacional, diccionario y DDL |
| 17 Especificación de Casos de Uso | 41 casos de uso (CU-01 a CU-41) |
| 18 Plan y Casos de Prueba | 77 casos funcionales, 11 no funcionales, 6 escenarios de aceptación y el registro de pruebas automatizadas (BD, API, web y app) |
| Anexo A | Cumplimiento normativo y gobierno de datos |
| Acta, Kick-Off, ERS rev. 04.2 (63 RF / 11 RNF), DAS v3.3, RACI | Documentos base del proyecto |
| KPIs y Costos (xlsx), Política de Privacidad y T&C | Indicadores, costos y textos legales |
| Diagramas/ | 35 diagramas (PNG y StarUML `.mdj`) |
| `Evidencias de sistema/Base de datos/` | Script `bigboys_ddl.sql` (27 tablas) validado: 24/24 pruebas; migraciones de Supabase y pruebas de la API por fecha en `supabase_DD-MM-AAAA/`; script de respaldo en `respaldo/` |

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

## Estado del proyecto (06-11-2026, semana 13 de 16)

- **Hitos cumplidos:** H4 cierre de la Fase 2 (18-10), H5 presentación de avance (23-10) y H6 sistema integrado (30-10). Próximos: H7 paso al ambiente UAT (10-11), H8 entrega final (13-11) y H9 defensa (27-11).
- **Base de datos:** PostgreSQL 17 en Supabase (región us-west-2, EE. UU.; transferencia internacional declarada, SC-18), 27 tablas con RLS cerrado. La web y la app usan solo la API: 34 funciones `api_*` (SC-20).
- **Funciones:** reserva con pago del abono en Webpay Plus (ambiente de integración de Transbank), reagendamiento y anulación con sus reglas, anticipación mínima de 90 minutos, fila por orden de llegada, «Mis datos», cuentas del personal, cobro del saldo en caja y reseñas. Sin RUT en los formularios (04-11).
- **Pruebas:** BD 24/24, API 20/20, seguridad 16/16, cobro del saldo 14/14, anticipación 7/7, web 13/13 y app 10/10; app probada en un teléfono Android real (06-11). Ciclos 1 y 2 de casos funcionales y UAT, pendientes.
- **Ambiente UAT:** la web se publica en GitHub Pages (rama `gh-pages`). Pendiente antes del H7: correos (R-17), respaldo diario con pg_dump (R-08) y limpieza de los datos de prueba (R-18).
- **Documentación:** al 06-11-2026: 29 solicitudes de cambio, 18 riesgos (16 activos), DAS 3.3, ERS rev. 04.2 y documentos 00, 04, 07, 11, 12, 15, 16, 17 y 18 actualizados (detalle en la sección 10 del documento 00). El código está sin secretos: las claves se leen desde archivos `.env` locales excluidos de git.
