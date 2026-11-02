-- =====================================================================
-- Proyecto Barbería Big-Boys · 22-10-2026
-- Eliminación de las 7 tablas del prototipo (users, appointments, locations,
-- barbers, coupons, reviews, mail). La web y la app ya usan el modelo
-- relacional a través de las funciones api_* (03_resena_y_api.sql).
-- Las tablas solo tenían datos de prueba: 2 usuarios, 2 citas, 3 sucursales
-- ficticias y 4 barberos (cargados como demo en barbero). Cierra el riesgo R-14.
-- =====================================================================
DROP TABLE IF EXISTS public.mail;
DROP TABLE IF EXISTS public.coupons;
DROP TABLE IF EXISTS public.reviews;
DROP TABLE IF EXISTS public.appointments;
DROP TABLE IF EXISTS public.barbers;
DROP TABLE IF EXISTS public.locations;
DROP TABLE IF EXISTS public.users;
