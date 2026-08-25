# Plataforma Web y App Móvil para Barbería

Sistema digital de agendamiento y gestión para un servicio de barbería, que reemplaza la atención por orden de llegada y el registro manual en Excel. Incluye agenda de citas, perfil de clientes, abono de reserva y fidelización digital.

**Estudiantes:** Jonathan Ojeda · Yerko Rosales
**Curso / Profesor:** Capstone / Fernando Herrera

---

## Contexto

Actualmente la barbería opera de forma manual:

- No existe un sistema de reservas; se atiende por orden de llegada.
- Con alta demanda, clientes se van sin ser atendidos.
- Las atenciones (colaborador, monto) se registran a mano en Excel.
- No hay perfiles ni historial digital de clientes, colaboradores ni del negocio.

## Objetivo general

Diseñar e implementar una plataforma web y app móvil que reemplace la atención por orden de llegada y el registro manual en Excel, con perfiles diferenciados para clientes, colaboradores y administradores.

## Objetivos específicos

- Permitir a cada cliente crear su perfil y agendar su servicio habitual o solicitar uno nuevo.
- Implementar el abono de un porcentaje del servicio para asegurar la asistencia y el cupo reservado.
- Habilitar el ingreso como invitado, con un pop-up promocional que incentive el registro.
- Incorporar perfiles para colaboradores y administradores, reemplazando el registro manual en Excel.

## Perfiles de usuario

| Perfil | Funciones principales |
| --- | --- |
| **Cliente** | Crea su perfil, agenda servicios habituales o nuevos, abona su reserva y revisa su historial. |
| **Colaborador** | Registra digitalmente los servicios que realiza (detalle y monto), reemplazando el Excel manual. |
| **Administrador** | Supervisa la agenda general, el desempeño de los colaboradores y los reportes del negocio. |

## Arquitectura (módulos del sistema)

- **Mantenedor:** administración de perfiles (clientes, colaboradores, administradores), servicios y horarios.
- **Negocio (Transacciones):** agendamiento, cálculo y registro de abonos, confirmación de citas.
- **Gestión:** toma de decisiones, control operativo y reportes de desempeño.

`Sitio web + App móvil → Base de datos → Datos maestros del cliente`

## Estructura del repositorio

```
├── Fase 1/
│   ├── Evidencias Individuales/   # Autoevaluaciones y diario de reflexión por alumno
│   └── Evidencias Grupales/       # Presentación, formativa y guía de definición del proyecto
├── Fase 2/
│   ├── Evidencias Individuales/   # Diario de reflexión por alumno
│   ├── Evidencias Grupales/       # Guías de desarrollo e informe final
│   └── Evidencias Proyecto/       # Presentación, documentación y evidencias del sistema (app + BD)
└── Fase 3/
    ├── Evidencias Individuales/   # Diario de reflexión por alumno
    └── Evidencias Grupales/       # Presentación final del proyecto
```

> Las planillas de evaluación marcadas como "Enviada por correo" no se incluyen en el repositorio.

## Estado del proyecto

Avance general: **30%** — Meta próxima etapa: **70%**

## Temas abiertos

- Definir la pasarela de pago a integrar para el abono.
- Establecer la política de reembolso ante cancelaciones.
- Fijar el porcentaje estándar de abono por servicio.
