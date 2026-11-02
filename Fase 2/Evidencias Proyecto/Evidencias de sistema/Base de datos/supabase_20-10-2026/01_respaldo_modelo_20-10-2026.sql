-- Respaldo de las 26 tablas del modelo antes de la migración del 20-10-2026
CREATE SCHEMA IF NOT EXISTS respaldo_20261020;
REVOKE ALL ON SCHEMA respaldo_20261020 FROM PUBLIC;
CREATE TABLE respaldo_20261020.sucursal AS TABLE public.sucursal;
CREATE TABLE respaldo_20261020.rol AS TABLE public.rol;
CREATE TABLE respaldo_20261020.usuario AS TABLE public.usuario;
CREATE TABLE respaldo_20261020.usuario_rol AS TABLE public.usuario_rol;
CREATE TABLE respaldo_20261020.cliente AS TABLE public.cliente;
CREATE TABLE respaldo_20261020.barbero AS TABLE public.barbero;
CREATE TABLE respaldo_20261020.servicio AS TABLE public.servicio;
CREATE TABLE respaldo_20261020.servicio_sucursal AS TABLE public.servicio_sucursal;
CREATE TABLE respaldo_20261020.horario_barbero AS TABLE public.horario_barbero;
CREATE TABLE respaldo_20261020.bloqueo_horario AS TABLE public.bloqueo_horario;
CREATE TABLE respaldo_20261020.configuracion AS TABLE public.configuracion;
CREATE TABLE respaldo_20261020.reserva AS TABLE public.reserva;
CREATE TABLE respaldo_20261020.reagendamiento AS TABLE public.reagendamiento;
CREATE TABLE respaldo_20261020.pago AS TABLE public.pago;
CREATE TABLE respaldo_20261020.turno_fila AS TABLE public.turno_fila;
CREATE TABLE respaldo_20261020.atencion AS TABLE public.atencion;
CREATE TABLE respaldo_20261020.promocion AS TABLE public.promocion;
CREATE TABLE respaldo_20261020.cupon_cliente AS TABLE public.cupon_cliente;
CREATE TABLE respaldo_20261020.movimiento_puntos AS TABLE public.movimiento_puntos;
CREATE TABLE respaldo_20261020.notificacion AS TABLE public.notificacion;
CREATE TABLE respaldo_20261020.politica AS TABLE public.politica;
CREATE TABLE respaldo_20261020.consentimiento AS TABLE public.consentimiento;
CREATE TABLE respaldo_20261020.solicitud_derecho AS TABLE public.solicitud_derecho;
CREATE TABLE respaldo_20261020.registro_auditoria AS TABLE public.registro_auditoria;
CREATE TABLE respaldo_20261020.incidente_seguridad AS TABLE public.incidente_seguridad;
CREATE TABLE respaldo_20261020.actividad_tratamiento AS TABLE public.actividad_tratamiento;

-- btree_gist fuera del esquema público (aviso de seguridad de Supabase)
ALTER EXTENSION btree_gist SET SCHEMA extensions;
