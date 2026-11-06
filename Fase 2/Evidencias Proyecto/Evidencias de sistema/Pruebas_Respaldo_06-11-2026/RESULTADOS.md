# Respaldo diario y prueba de restauración (06-11-2026)

Riesgo R-08 y caso CP-NF-09 (RNF-09). El plan gratuito de Supabase no incluye respaldos, así que el proyecto mantiene uno propio.

## Configuración
- Herramienta: `pg_dump` 17.11, la misma versión de PostgreSQL que usa Supabase.
- Script: `Base de datos/respaldo/respaldar_supabase.ps1`.
  - Formato *custom*, esquema `public`, sin dueños ni privilegios.
  - Conserva los últimos 30 respaldos.
- Usuario de base de datos `respaldo_bigboys`.
  - Solo lectura: rol `pg_read_all_data`.
  - Tiene `BYPASSRLS`, porque las tablas tienen RLS cerrado.
  - Máximo 3 conexiones.
  - Su contraseña solo está en una variable de entorno del equipo; nunca en el repositorio.
- Conexión: pooler de Supabase en modo sesión (`aws-1-us-west-2`, TLS).
- Tarea programada de Windows «Respaldo BigBoys Supabase»: todos los días a las 23:30.
- Destino: `Documentos\Respaldos_BigBoys`, fuera del repositorio porque contiene datos personales.

## Resultados
| Verificación | Resultado |
|---|---|
| Primer respaldo, ejecutado por la tarea programada | `bigboys_06-11-2026_1419.dump`, 205 KB, código de salida 0 |
| Contenido según `pg_restore -l` | 27 tablas con datos, 34 funciones `api_*` y 4 triggers |
| Restauración en un PostgreSQL 17 local vacío | Sin errores (código 0) |
| Tiempo de restauración | 1,8 s; la meta (RTO) es ≤ 4 h |
| Conteos restaurados frente a producción | Iguales: reservas 23, clientes 25, usuarios 13, auditoría 118, configuración 13 |
| Pérdida máxima de datos (RPO) | ≤ 24 h, con un respaldo diario; la meta es ≤ 24 h |

Antes de restaurar, la base de prueba recibió lo que Supabase trae por defecto:
- Los roles `anon`, `authenticated` y `service_role`.
- El esquema `extensions`, con `pgcrypto`, `btree_gist` y `uuid-ossp`.

Al terminar, la copia restaurada se eliminó.

## Cómo repetir la prueba
1. `pg_restore -l <archivo.dump>`: lista el contenido del respaldo.
2. Crear una base vacía con los roles y extensiones indicados arriba.
3. `pg_restore --no-owner --no-privileges -d <base_de_prueba> <archivo.dump>`.
