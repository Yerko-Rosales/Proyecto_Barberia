# Proyecto Barbería Big-Boys

Sistema de gestión y agendamiento para **Barbería Big-Boys** (plataforma web + app móvil Android), que **complementa** la atención por orden de llegada y **reemplaza por completo** el registro manual en Excel en sus tres sucursales.

**Estudiantes:** Yerko Rosales (Jefe de Proyecto, Analista y UX/UI) · Jonathan Ojeda (Backend y Base de Datos)
**Asignatura / Docente:** PTY4614 Capstone / Fernando Herrera
**Período:** 14-08-2026 al 13-11-2026 · Defensa probable: 27-11-2026

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
- Arquitectura: web (React) + app Android (Kotlin/MVVM) → API REST → **PostgreSQL** (base de datos relacional).

## Documentación (raíz de la carpeta del semestre)

| Documento | Descripción |
| --- | --- |
| 00 Inventario y Resumen de Cambios | Qué se actualizó el 06-10-2026 y supuestos por validar |
| 02 a 15 | Planes de gestión (interesados, dirección, requerimientos, alcance, EDT, Gantt, costos, calidad, recursos, cambios, avance, cierre, riesgos) |
| 16 Diseño de Base de Datos | MER, modelo relacional, diccionario y DDL |
| 17 Especificación de Casos de Uso | 40 casos de uso |
| Anexo A | Cumplimiento normativo y gobierno de datos |
| Acta, Kick-Off, ERS rev. 04, DAS v3.0, RACI | Documentos base del proyecto |
| Diagramas/ | 34 diagramas (PNG, SVG y código PlantUML) |
| Base_de_Datos/ | Script `bigboys_ddl.sql` validado |

## Estructura del repositorio

```
├── Fase 1/ (Evidencias individuales y grupales)
├── Fase 2/
│   ├── Evidencias Grupales/      # Guías 2.4 y 2.6 (pendientes)
│   ├── Evidencias Individuales/  # Diarios de reflexión (pendientes)
│   ├── Evidencias Proyecto/
│   │   ├── Avance_Proyecto_Barberia.pptx
│   │   ├── Evidencias de documentación/  # Documentos del proyecto
│   │   └── Evidencias de sistema/
│   │       ├── Aplicación/               # Código (pendiente de subir sin secretos)
│   │       └── Base de datos/            # Script DDL y diagramas de BD
│   └── Seguimiento semanal/
└── Fase 3/ (Presentación final y diarios, pendientes)
```

## Estado del proyecto (06-10-2026, semana 8 de 13)

- Documentación de diseño completa (4+1, modelo relacional, cumplimiento Ley N° 21.719, KPIs y costos).
- Prototipos web y app funcionales; el código está en pausa y se retoma el 13-10-2026 con la migración a PostgreSQL.
- Temas abiertos: validar supuestos con el/la dueño/a, nombre del patrocinador y aprobación formal de los cambios SC-02, SC-03, SC-05 y SC-06.
