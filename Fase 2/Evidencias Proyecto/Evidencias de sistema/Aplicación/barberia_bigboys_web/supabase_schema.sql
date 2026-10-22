-- =====================================================================
-- DUOC UC • ESCUELA DE INFORMÁTICA Y TELECOMUNICACIONES
-- Ingeniería en Informática • PTY4614 — Capstone Project
-- Proyecto Barbería Big-Boys — Sistema de Agendamiento y Gestión (Web + App Móvil)
-- Script DDL Maestro Oficial para Supabase PostgreSQL 15+
-- Autores: Yerko Rosales (Jefe de Proyecto) • Jonathan Ojeda (Backend y BD)
-- Docente: Fernando Herrera | Versión: 1.0 Aprobado (Reemplazo SC-04 Firestore)
-- =====================================================================

-- 1. EXTENSIONES
-- Extensión btree_gist requerida para la restricción de exclusión de traslapes en reservas
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- 2. TABLAS DEL MODELO RELACIONAL NORMALIZADO (3FN - 26 TABLAS)

-- -------------------------------------------------------------
-- 1. SUCURSAL (Locales físicos operativos)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.sucursal (
    id_sucursal SMALLSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    direccion VARCHAR(150) NOT NULL,
    comuna VARCHAR(60) NOT NULL,
    telefono VARCHAR(20),
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    es_casa_matriz BOOLEAN NOT NULL DEFAULT false,
    activa BOOLEAN NOT NULL DEFAULT true,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -------------------------------------------------------------
-- 2. ROL (Catálogo de roles RBAC)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.rol (
    id_rol SMALLINT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE CHECK (codigo IN ('CLIENTE', 'BARBERO', 'ADMIN_SUCURSAL', 'DUENO')),
    descripcion VARCHAR(120) NOT NULL
);

-- -------------------------------------------------------------
-- 3. USUARIO (Cuentas de acceso centralizadas - Sin RUT por Ley 21.719)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.usuario (
    id_usuario BIGSERIAL PRIMARY KEY,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre VARCHAR(80) NOT NULL,
    apellido VARCHAR(80),
    telefono VARCHAR(20),
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'BLOQUEADO', 'SUPRIMIDO')),
    intentos_fallidos SMALLINT NOT NULL DEFAULT 0,
    ultimo_acceso TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ,
    suprimido_en TIMESTAMPTZ
);

-- -------------------------------------------------------------
-- 4. USUARIO_ROL (Asignación RBAC con alcance opcional por sucursal)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.usuario_rol (
    id_usuario_rol BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES public.usuario(id_usuario) ON DELETE CASCADE,
    id_rol SMALLINT NOT NULL REFERENCES public.rol(id_rol) ON DELETE CASCADE,
    id_sucursal SMALLINT REFERENCES public.sucursal(id_sucursal) ON DELETE CASCADE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (id_usuario, id_rol, id_sucursal)
);

-- -------------------------------------------------------------
-- 5. CLIENTE (Titular del agendamiento: Registrado o Invitado)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.cliente (
    id_cliente BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT UNIQUE REFERENCES public.usuario(id_usuario) ON DELETE SET NULL,
    es_invitado BOOLEAN NOT NULL DEFAULT false,
    nombre_contacto VARCHAR(80) NOT NULL,
    telefono_contacto VARCHAR(20),
    email_contacto VARCHAR(120),
    puntos INTEGER NOT NULL DEFAULT 0 CHECK (puntos >= 0),
    ultima_actividad TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (es_invitado OR id_usuario IS NOT NULL)
);

-- -------------------------------------------------------------
-- 6. BARBERO (Profesionales técnicos asignados a sucursales)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.barbero (
    id_barbero SERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL UNIQUE REFERENCES public.usuario(id_usuario) ON DELETE RESTRICT,
    id_sucursal SMALLINT NOT NULL REFERENCES public.sucursal(id_sucursal) ON DELETE RESTRICT,
    nombre_publico VARCHAR(80) NOT NULL,
    especialidad VARCHAR(80),
    bio TEXT,
    avatar_url TEXT,
    activo BOOLEAN NOT NULL DEFAULT true,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -------------------------------------------------------------
-- 7. SERVICIO (Catálogo general de prestaciones)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.servicio (
    id_servicio SERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    categoria VARCHAR(30) NOT NULL CHECK (categoria IN ('CORTE', 'BARBA', 'COMBO', 'COMPLEMENTO', 'TRATAMIENTO')),
    reservable_individual BOOLEAN NOT NULL DEFAULT true,
    duracion_min SMALLINT NOT NULL CHECK (duracion_min BETWEEN 10 AND 240),
    precio_base NUMERIC(10,0) NOT NULL CHECK (precio_base > 0),
    activo BOOLEAN NOT NULL DEFAULT true
);

-- -------------------------------------------------------------
-- 8. SERVICIO_SUCURSAL (Precios y disponibilidad diferenciada local)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.servicio_sucursal (
    id_servicio INTEGER NOT NULL REFERENCES public.servicio(id_servicio) ON DELETE CASCADE,
    id_sucursal SMALLINT NOT NULL REFERENCES public.sucursal(id_sucursal) ON DELETE CASCADE,
    precio NUMERIC(10,0) CHECK (precio > 0),
    activo BOOLEAN NOT NULL DEFAULT true,
    PRIMARY KEY (id_servicio, id_sucursal)
);

-- -------------------------------------------------------------
-- 9. HORARIO_BARBERO (Jornada base semanal por día ISO 1-7)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.horario_barbero (
    id_horario SERIAL PRIMARY KEY,
    id_barbero INTEGER NOT NULL REFERENCES public.barbero(id_barbero) ON DELETE CASCADE,
    dia_semana SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL CHECK (hora_fin > hora_inicio),
    UNIQUE (id_barbero, dia_semana, hora_inicio)
);

-- -------------------------------------------------------------
-- 10. BLOQUEO_HORARIO (Colaciones, permisos e incidencias)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.bloqueo_horario (
    id_bloqueo BIGSERIAL PRIMARY KEY,
    id_barbero INTEGER NOT NULL REFERENCES public.barbero(id_barbero) ON DELETE CASCADE,
    inicio TIMESTAMPTZ NOT NULL,
    fin TIMESTAMPTZ NOT NULL CHECK (fin > inicio),
    motivo VARCHAR(120),
    creado_por BIGINT NOT NULL REFERENCES public.usuario(id_usuario)
);

-- -------------------------------------------------------------
-- 11. CONFIGURACION (Motor de parámetros versionados por ámbito)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.configuracion (
    id_config SERIAL PRIMARY KEY,
    clave VARCHAR(60) NOT NULL,
    ambito VARCHAR(10) NOT NULL CHECK (ambito IN ('GLOBAL', 'SUCURSAL', 'SERVICIO')),
    id_sucursal SMALLINT REFERENCES public.sucursal(id_sucursal) ON DELETE CASCADE,
    id_servicio INTEGER REFERENCES public.servicio(id_servicio) ON DELETE CASCADE,
    tipo_valor VARCHAR(10) NOT NULL CHECK (tipo_valor IN ('MONTO', 'PORCENTAJE', 'ENTERO', 'TEXTO')),
    valor VARCHAR(60) NOT NULL,
    vigente_desde TIMESTAMPTZ NOT NULL DEFAULT now(),
    vigente_hasta TIMESTAMPTZ,
    modificado_por BIGINT REFERENCES public.usuario(id_usuario),
    CHECK (
        (ambito = 'GLOBAL' AND id_sucursal IS NULL AND id_servicio IS NULL) OR
        (ambito = 'SUCURSAL' AND id_sucursal IS NOT NULL) OR
        (ambito = 'SERVICIO' AND id_servicio IS NOT NULL)
    )
);

-- -------------------------------------------------------------
-- 12. RESERVA (Citas agendadas con exclusión anti-traslape)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.reserva (
    id_reserva BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(12) NOT NULL UNIQUE,
    id_cliente BIGINT NOT NULL REFERENCES public.cliente(id_cliente) ON DELETE RESTRICT,
    id_sucursal SMALLINT NOT NULL REFERENCES public.sucursal(id_sucursal) ON DELETE RESTRICT,
    id_barbero INTEGER NOT NULL REFERENCES public.barbero(id_barbero) ON DELETE RESTRICT,
    id_servicio INTEGER NOT NULL REFERENCES public.servicio(id_servicio) ON DELETE RESTRICT,
    inicio TIMESTAMPTZ NOT NULL,
    fin TIMESTAMPTZ NOT NULL CHECK (fin > inicio),
    estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE_PAGO' CHECK (estado IN ('PENDIENTE_PAGO', 'CONFIRMADA', 'ATENDIDA', 'CANCELADA', 'NO_SHOW', 'EXPIRADA')),
    monto_abono NUMERIC(10,0) NOT NULL CHECK (monto_abono >= 0),
    cantidad_reagendamientos SMALLINT NOT NULL DEFAULT 0 CHECK (cantidad_reagendamientos BETWEEN 0 AND 2),
    canal VARCHAR(5) NOT NULL CHECK (canal IN ('WEB', 'APP', 'LOCAL')),
    expira_en TIMESTAMPTZ,
    cancelada_en TIMESTAMPTZ,
    motivo_cancelacion VARCHAR(120),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ,
    -- Restricción de exclusión física: impide reservas dobles o traslapadas en el mismo barbero
    EXCLUDE USING gist (
        id_barbero WITH =,
        tstzrange(inicio, fin) WITH &&
    ) WHERE (estado IN ('PENDIENTE_PAGO', 'CONFIRMADA', 'ATENDIDA'))
);

-- -------------------------------------------------------------
-- 13. REAGENDAMIENTO (Trazabilidad y validación de regla 24h)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.reagendamiento (
    id_reagendamiento BIGSERIAL PRIMARY KEY,
    id_reserva BIGINT NOT NULL REFERENCES public.reserva(id_reserva) ON DELETE CASCADE,
    inicio_anterior TIMESTAMPTZ NOT NULL,
    inicio_nuevo TIMESTAMPTZ NOT NULL,
    horas_anticipacion NUMERIC(6,1) NOT NULL CHECK (horas_anticipacion >= 24),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -------------------------------------------------------------
-- 14. PAGO (Transacciones de recaudación sin almacenar datos de tarjeta)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.pago (
    id_pago BIGSERIAL PRIMARY KEY,
    id_reserva BIGINT NOT NULL REFERENCES public.reserva(id_reserva) ON DELETE CASCADE,
    proveedor VARCHAR(15) NOT NULL CHECK (proveedor IN ('WEBPAY', 'MERCADOPAGO')),
    id_transaccion VARCHAR(100) NOT NULL UNIQUE,
    codigo_autorizacion VARCHAR(20),
    tipo_medio VARCHAR(10) CHECK (tipo_medio IN ('DEBITO', 'CREDITO', 'PREPAGO')),
    monto NUMERIC(10,0) NOT NULL CHECK (monto > 0),
    estado VARCHAR(12) NOT NULL CHECK (estado IN ('INICIADO', 'AUTORIZADO', 'RECHAZADO', 'APLICADO', 'RETENIDO', 'REEMBOLSADO', 'ANULADO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ
);

-- -------------------------------------------------------------
-- 15. TURNO_FILA (Fila presencial espontánea walk-in)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.turno_fila (
    id_turno BIGSERIAL PRIMARY KEY,
    id_sucursal SMALLINT NOT NULL REFERENCES public.sucursal(id_sucursal) ON DELETE CASCADE,
    fecha DATE NOT NULL,
    numero SMALLINT NOT NULL CHECK (numero > 0),
    nombre_llamado VARCHAR(40),
    id_cliente BIGINT REFERENCES public.cliente(id_cliente) ON DELETE SET NULL,
    id_servicio INTEGER REFERENCES public.servicio(id_servicio) ON DELETE RESTRICT,
    id_barbero_pref INTEGER REFERENCES public.barbero(id_barbero) ON DELETE SET NULL,
    estado VARCHAR(12) NOT NULL DEFAULT 'EN_ESPERA' CHECK (estado IN ('EN_ESPERA', 'LLAMADO', 'EN_ATENCION', 'ATENDIDO', 'ABANDONO')),
    hora_llegada TIMESTAMPTZ NOT NULL DEFAULT now(),
    hora_llamado TIMESTAMPTZ,
    hora_termino TIMESTAMPTZ,
    UNIQUE (id_sucursal, fecha, numero)
);

-- -------------------------------------------------------------
-- 16. ATENCION (Consolidación real en sillón y balance de caja)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.atencion (
    id_atencion BIGSERIAL PRIMARY KEY,
    id_reserva BIGINT UNIQUE REFERENCES public.reserva(id_reserva) ON DELETE SET NULL,
    id_turno BIGINT UNIQUE REFERENCES turno_fila(id_turno) ON DELETE SET NULL,
    id_cliente BIGINT REFERENCES cliente(id_cliente) ON DELETE RESTRICT,
    id_sucursal SMALLINT NOT NULL REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT,
    id_barbero INTEGER NOT NULL REFERENCES barbero(id_barbero) ON DELETE RESTRICT,
    id_servicio INTEGER NOT NULL REFERENCES servicio(id_servicio) ON DELETE RESTRICT,
    inicio TIMESTAMPTZ NOT NULL,
    fin TIMESTAMPTZ,
    monto_cobrado NUMERIC(10,0) NOT NULL CHECK (monto_cobrado >= 0),
    abono_aplicado NUMERIC(10,0) NOT NULL DEFAULT 0 CHECK (abono_aplicado >= 0),
    propina NUMERIC(10,0) NOT NULL DEFAULT 0 CHECK (propina >= 0),
    medio_pago_local VARCHAR(15) CHECK (medio_pago_local IN ('EFECTIVO', 'DEBITO', 'CREDITO', 'TRANSFERENCIA')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (id_reserva IS NOT NULL OR id_turno IS NOT NULL)
);

-- -------------------------------------------------------------
-- 17. PROMOCION (Módulo de marketing y campañas)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.promocion (
    id_promocion SERIAL PRIMARY KEY,
    codigo VARCHAR(20) UNIQUE,
    nombre VARCHAR(80) NOT NULL,
    tipo VARCHAR(12) NOT NULL CHECK (tipo IN ('BIENVENIDA', 'RULETA', 'CODIGO', 'PUNTOS')),
    tipo_descuento VARCHAR(10) NOT NULL CHECK (tipo_descuento IN ('MONTO', 'PORCENTAJE', 'SERVICIO')),
    valor NUMERIC(10,2) NOT NULL CHECK (valor >= 0),
    id_sucursal SMALLINT REFERENCES sucursal(id_sucursal) ON DELETE CASCADE,
    vigente_desde DATE NOT NULL,
    vigente_hasta DATE,
    cupos INTEGER CHECK (cupos > 0),
    activa BOOLEAN NOT NULL DEFAULT true
);

-- -------------------------------------------------------------
-- 18. CUPON_CLIENTE (Cupones asignados y canjeados)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.cupon_cliente (
    id_cupon BIGSERIAL PRIMARY KEY,
    id_promocion INTEGER NOT NULL REFERENCES promocion(id_promocion) ON DELETE CASCADE,
    id_cliente BIGINT NOT NULL REFERENCES cliente(id_cliente) ON DELETE CASCADE,
    pin CHAR(4),
    estado VARCHAR(10) NOT NULL DEFAULT 'DISPONIBLE' CHECK (estado IN ('DISPONIBLE', 'CANJEADO', 'EXPIRADO')),
    id_atencion BIGINT REFERENCES atencion(id_atencion) ON DELETE SET NULL,
    asignado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    canjeado_en TIMESTAMPTZ
);

-- -------------------------------------------------------------
-- 19. MOVIMIENTO_PUNTOS (Programa de lealtad RF-20)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.movimiento_puntos (
    id_movimiento BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES cliente(id_cliente) ON DELETE CASCADE,
    id_atencion BIGINT REFERENCES atencion(id_atencion) ON DELETE SET NULL,
    tipo VARCHAR(8) NOT NULL CHECK (tipo IN ('ACUMULA', 'CANJE', 'AJUSTE')),
    puntos INTEGER NOT NULL CHECK (puntos <> 0),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -------------------------------------------------------------
-- 20. NOTIFICACION (Cola multicanal: Email, Push, SMS, In-App)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.notificacion (
    id_notificacion BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT REFERENCES cliente(id_cliente) ON DELETE CASCADE,
    id_usuario BIGINT REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    id_reserva BIGINT REFERENCES reserva(id_reserva) ON DELETE SET NULL,
    canal VARCHAR(6) NOT NULL CHECK (canal IN ('EMAIL', 'PUSH', 'INAPP', 'SMS')),
    tipo VARCHAR(15) NOT NULL CHECK (tipo IN ('CONFIRMACION', 'RECORDATORIO', 'CANCELACION', 'PROMOCIONAL', 'DERECHOS', 'SEGURIDAD')),
    asunto VARCHAR(150) NOT NULL,
    estado VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'ENVIADA', 'FALLIDA', 'LEIDA')),
    programada_para TIMESTAMPTZ,
    enviada_en TIMESTAMPTZ,
    CHECK (id_cliente IS NOT NULL OR id_usuario IS NOT NULL)
);

-- -------------------------------------------------------------
-- 21. POLITICA (Términos legales y privacidad con hash SHA-256)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.politica (
    id_politica SERIAL PRIMARY KEY,
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('PRIVACIDAD', 'TERMINOS')),
    version VARCHAR(10) NOT NULL,
    publicada_en DATE NOT NULL,
    url VARCHAR(200) NOT NULL,
    hash_sha256 CHAR(64) NOT NULL,
    vigente BOOLEAN NOT NULL DEFAULT false,
    UNIQUE (tipo, version)
);

-- -------------------------------------------------------------
-- 22. CONSENTIMIENTO (Aceptación expresa y revocación)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.consentimiento (
    id_consentimiento BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES cliente(id_cliente) ON DELETE CASCADE,
    id_politica INTEGER NOT NULL REFERENCES politica(id_politica) ON DELETE CASCADE,
    finalidad VARCHAR(12) NOT NULL CHECK (finalidad IN ('SERVICIO', 'PROMOCIONAL')),
    otorgado BOOLEAN NOT NULL,
    canal VARCHAR(5) NOT NULL CHECK (canal IN ('WEB', 'APP', 'LOCAL')),
    registrado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    revocado_en TIMESTAMPTZ
);

-- -------------------------------------------------------------
-- 23. SOLICITUD_DERECHO (Canal formal derechos ARCO-P Ley 21.719)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.solicitud_derecho (
    id_solicitud BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(14) NOT NULL UNIQUE,
    id_cliente BIGINT NOT NULL REFERENCES cliente(id_cliente) ON DELETE CASCADE,
    tipo VARCHAR(14) NOT NULL CHECK (tipo IN ('ACCESO', 'RECTIFICACION', 'SUPRESION', 'OPOSICION', 'PORTABILIDAD', 'BLOQUEO')),
    estado VARCHAR(15) NOT NULL DEFAULT 'RECIBIDA' CHECK (estado IN ('RECIBIDA', 'EN_VERIFICACION', 'EN_PROCESO', 'PRORROGADA', 'RESPONDIDA', 'RECHAZADA')),
    detalle VARCHAR(500),
    fecha_ingreso TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_limite DATE NOT NULL,
    prorrogada_hasta DATE,
    fecha_respuesta TIMESTAMPTZ,
    respuesta VARCHAR(1000),
    atendida_por BIGINT REFERENCES usuario(id_usuario)
);

-- -------------------------------------------------------------
-- 24. REGISTRO_AUDITORIA (Log inmutable de eventos forenses)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.registro_auditoria (
    id_log BIGSERIAL PRIMARY KEY,
    ocurrido_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    id_usuario BIGINT REFERENCES usuario(id_usuario) ON DELETE SET NULL,
    rol VARCHAR(20),
    accion VARCHAR(25) NOT NULL CHECK (accion IN ('LOGIN', 'LOGIN_FALLIDO', 'LOGOUT', 'CREAR', 'ACTUALIZAR', 'ELIMINAR', 'CONSULTAR_DATO_PERSONAL', 'EXPORTAR', 'CAMBIO_CONFIG', 'DERECHO_TITULAR')),
    entidad VARCHAR(40) NOT NULL,
    id_entidad VARCHAR(40),
    detalle JSONB,
    ip_origen INET
);

-- -------------------------------------------------------------
-- 25. INCIDENTE_SEGURIDAD (Gobernanza de brechas normativas)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.incidente_seguridad (
    id_incidente BIGSERIAL PRIMARY KEY,
    fecha_deteccion TIMESTAMPTZ NOT NULL DEFAULT now(),
    tipo VARCHAR(40) NOT NULL,
    nivel_riesgo VARCHAR(10) NOT NULL CHECK (nivel_riesgo IN ('BAJO', 'MEDIO', 'ALTO')),
    descripcion TEXT NOT NULL,
    medidas_adoptadas TEXT,
    notificado_agencia BOOLEAN DEFAULT false,
    creado_en TIMESTAMPTZ DEFAULT now()
);

-- -------------------------------------------------------------
-- 26. ACTIVIDAD_TRATAMIENTO (Registro RAT legal exigido por ley)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.actividad_tratamiento (
    id_actividad SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    finalidad TEXT NOT NULL,
    categoria_datos TEXT NOT NULL,
    base_legal VARCHAR(50) NOT NULL,
    plazo_retencion VARCHAR(50) NOT NULL,
    medidas_seguridad TEXT NOT NULL,
    creado_en TIMESTAMPTZ DEFAULT now()
);


-- =====================================================================
-- 3. TRIGGERS Y FUNCIONES PL/pgSQL
-- =====================================================================

-- Trigger 1: Inmutabilidad estricta de auditoría (aborta cualquier UPDATE o DELETE)
CREATE OR REPLACE FUNCTION public.fn_auditoria_inmutable()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'registro_auditoria es estrictamente de solo insercion (inmutable por Ley 21.719)';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tg_auditoria_inmutable ON public.registro_auditoria;
CREATE TRIGGER tg_auditoria_inmutable
BEFORE UPDATE OR DELETE ON public.registro_auditoria
FOR EACH ROW EXECUTE FUNCTION public.fn_auditoria_inmutable();

-- Trigger 2: Cálculo automático del plazo legal de 30 días corridos (Ley N° 21.719)
CREATE OR REPLACE FUNCTION public.fn_solicitud_plazo()
RETURNS trigger AS $$
BEGIN
    NEW.fecha_limite := (NEW.fecha_ingreso AT TIME ZONE 'America/Santiago')::date + 30;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tg_solicitud_plazo ON public.solicitud_derecho;
CREATE TRIGGER tg_solicitud_plazo
BEFORE INSERT ON public.solicitud_derecho
FOR EACH ROW EXECUTE FUNCTION public.fn_solicitud_plazo();


-- =====================================================================
-- 4. VISTAS Y RESOLUCIÓN JERÁRQUICA DE ABONO
-- =====================================================================
-- Jerarquía: Servicio > Sucursal > Global
CREATE OR REPLACE VIEW public.v_abono_vigente AS
SELECT 
    c.id_config,
    c.clave,
    c.ambito,
    c.id_sucursal,
    c.id_servicio,
    c.tipo_valor,
    c.valor,
    c.vigente_desde
FROM public.configuracion c
WHERE c.clave = 'ABONO_RESERVA'
  AND (c.vigente_hasta IS NULL OR c.vigente_hasta > now());


-- =====================================================================
-- 5. POLÍTICAS DE ACCESO Y SEGURIDAD (Row Level Security - RLS)
-- =====================================================================
ALTER TABLE public.sucursal ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.rol ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.usuario_rol ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cliente ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.barbero ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.servicio ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.servicio_sucursal ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.horario_barbero ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bloqueo_horario ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.configuracion ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reserva ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reagendamiento ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pago ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.turno_fila ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.atencion ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.promocion ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cupon_cliente ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.movimiento_puntos ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notificacion ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.politica ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.consentimiento ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.solicitud_derecho ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.registro_auditoria ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.incidente_seguridad ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.actividad_tratamiento ENABLE ROW LEVEL SECURITY;

-- Políticas permisivas de desarrollo y acceso completo para cliente y admin
CREATE POLICY "Acceso público sucursales" ON public.sucursal FOR ALL USING (true);
CREATE POLICY "Acceso público roles" ON public.rol FOR ALL USING (true);
CREATE POLICY "Acceso público usuarios" ON public.usuario FOR ALL USING (true);
CREATE POLICY "Acceso público usuario_rol" ON public.usuario_rol FOR ALL USING (true);
CREATE POLICY "Acceso público clientes" ON public.cliente FOR ALL USING (true);
CREATE POLICY "Acceso público barberos" ON public.barbero FOR ALL USING (true);
CREATE POLICY "Acceso público servicios" ON public.servicio FOR ALL USING (true);
CREATE POLICY "Acceso público servicio_sucursal" ON public.servicio_sucursal FOR ALL USING (true);
CREATE POLICY "Acceso público horario_barbero" ON public.horario_barbero FOR ALL USING (true);
CREATE POLICY "Acceso público bloqueo_horario" ON public.bloqueo_horario FOR ALL USING (true);
CREATE POLICY "Acceso público configuracion" ON public.configuracion FOR ALL USING (true);
CREATE POLICY "Acceso público reservas" ON public.reserva FOR ALL USING (true);
CREATE POLICY "Acceso público reagendamientos" ON public.reagendamiento FOR ALL USING (true);
CREATE POLICY "Acceso público pagos" ON public.pago FOR ALL USING (true);
CREATE POLICY "Acceso público turno_fila" ON public.turno_fila FOR ALL USING (true);
CREATE POLICY "Acceso público atenciones" ON public.atencion FOR ALL USING (true);
CREATE POLICY "Acceso público promociones" ON public.promocion FOR ALL USING (true);
CREATE POLICY "Acceso público cupon_cliente" ON public.cupon_cliente FOR ALL USING (true);
CREATE POLICY "Acceso público puntos" ON public.movimiento_puntos FOR ALL USING (true);
CREATE POLICY "Acceso público notificaciones" ON public.notificacion FOR ALL USING (true);
CREATE POLICY "Acceso público politicas" ON public.politica FOR ALL USING (true);
CREATE POLICY "Acceso público consentimientos" ON public.consentimiento FOR ALL USING (true);
CREATE POLICY "Acceso público solicitudes" ON public.solicitud_derecho FOR ALL USING (true);
CREATE POLICY "Acceso público auditoria" ON public.registro_auditoria FOR ALL USING (true);
CREATE POLICY "Acceso público incidentes" ON public.incidente_seguridad FOR ALL USING (true);
CREATE POLICY "Acceso público actividades" ON public.actividad_tratamiento FOR ALL USING (true);


-- =====================================================================
-- 6. PUBLICACIÓN DE TIEMPO REAL (Supabase Realtime)
-- =====================================================================
ALTER PUBLICATION supabase_realtime ADD TABLE public.reserva;
ALTER PUBLICATION supabase_realtime ADD TABLE public.turno_fila;
ALTER PUBLICATION supabase_realtime ADD TABLE public.barbero;
ALTER PUBLICATION supabase_realtime ADD TABLE public.pago;
ALTER PUBLICATION supabase_realtime ADD TABLE public.bloqueo_horario;


-- =====================================================================
-- 7. DATOS INICIALES (SEED DATA OFICIAL BIG BOYS CHILE)
-- =====================================================================

-- 1. Sucursales
INSERT INTO public.sucursal (id_sucursal, codigo, nombre, direccion, comuna, telefono, lat, lng, es_casa_matriz, activa) VALUES
(1, 'TAL-01', 'Big Boys - Talagante Centro', 'Boulevard del centro, local 3', 'Talagante', '+56 9 8765 4321', -33.6631, -70.9286, true, true),
(2, 'PEN-01', 'Big Boys - Peñaflor Principal', '21 de mayo 4566', 'Peñaflor', '+56 9 1234 5678', -33.6080, -70.8993, false, true),
(3, 'PEN-02', 'Big Boys - Peñaflor Araya Cereceda', 'Alcalde Luis Araya Cereceda 4387', 'Peñaflor', '+56 9 5555 4444', -33.6077, -70.9001, false, true)
ON CONFLICT (id_sucursal) DO UPDATE SET
nombre = EXCLUDED.nombre, direccion = EXCLUDED.direccion, comuna = EXCLUDED.comuna, telefono = EXCLUDED.telefono, lat = EXCLUDED.lat, lng = EXCLUDED.lng;

-- 2. Roles RBAC
INSERT INTO public.rol (id_rol, codigo, descripcion) VALUES
(1, 'CLIENTE', 'Cliente regular o invitado registrado'),
(2, 'BARBERO', 'Colaborador técnico barbero profesional'),
(3, 'ADMIN_SUCURSAL', 'Administrador encargado de local físico'),
(4, 'DUENO', 'Dueño y Gerencia General')
ON CONFLICT (id_rol) DO UPDATE SET descripcion = EXCLUDED.descripcion;

-- 3. Usuarios base
INSERT INTO public.usuario (id_usuario, email, password_hash, nombre, apellido, telefono, estado) VALUES
(1, 'admin@bigboys.cl', '$2a$10$X8O.U2Qh10r/b9.D9q3Uo.g6eP8o51.Zc9rP4m6O1nB2e3F4g5h6i', 'Yerko', 'Rosales', '+56 9 8888 7777', 'ACTIVO'),
(2, 'jonathan@bigboys.cl', '$2a$10$X8O.U2Qh10r/b9.D9q3Uo.g6eP8o51.Zc9rP4m6O1nB2e3F4g5h6i', 'Jonathan', 'Ojeda', '+56 9 9999 6666', 'ACTIVO'),
(3, 'mateo@bigboys.cl', '$2a$10$X8O.U2Qh10r/b9.D9q3Uo.g6eP8o51.Zc9rP4m6O1nB2e3F4g5h6i', 'Mateo', 'Silva', '+56 9 1111 2222', 'ACTIVO'),
(4, 'esteban@bigboys.cl', '$2a$10$X8O.U2Qh10r/b9.D9q3Uo.g6eP8o51.Zc9rP4m6O1nB2e3F4g5h6i', 'Esteban', 'Rojas', '+56 9 2222 3333', 'ACTIVO'),
(5, 'cristian@bigboys.cl', '$2a$10$X8O.U2Qh10r/b9.D9q3Uo.g6eP8o51.Zc9rP4m6O1nB2e3F4g5h6i', 'Cristian', 'Castro', '+56 9 3333 4444', 'ACTIVO'),
(6, 'diego@bigboys.cl', '$2a$10$X8O.U2Qh10r/b9.D9q3Uo.g6eP8o51.Zc9rP4m6O1nB2e3F4g5h6i', 'Diego', 'Morales', '+56 9 4444 5555', 'ACTIVO'),
(7, 'cliente.test@bigboys.cl', '$2a$10$X8O.U2Qh10r/b9.D9q3Uo.g6eP8o51.Zc9rP4m6O1nB2e3F4g5h6i', 'Juan', 'Pérez', '+56 9 1122 3344', 'ACTIVO')
ON CONFLICT (id_usuario) DO NOTHING;

-- 4. Asignación de Roles
INSERT INTO public.usuario_rol (id_usuario, id_rol, id_sucursal) VALUES
(1, 4, NULL),
(2, 4, NULL),
(3, 2, 1),
(4, 2, 1),
(5, 2, 2),
(6, 2, 3),
(7, 1, NULL)
ON CONFLICT (id_usuario, id_rol, id_sucursal) DO NOTHING;

-- 5. Clientes
INSERT INTO public.cliente (id_cliente, id_usuario, es_invitado, nombre_contacto, telefono_contacto, email_contacto, puntos) VALUES
(1, 7, false, 'Juan Pérez', '+56 9 1122 3344', 'cliente.test@bigboys.cl', 100)
ON CONFLICT (id_cliente) DO NOTHING;

-- 6. Barberos
INSERT INTO public.barbero (id_barbero, id_usuario, id_sucursal, nombre_publico, especialidad, bio, avatar_url, activo) VALUES
(1, 3, 1, 'Mateo "Fade Master" Silva', 'Degradados de Precisión, Low & Mid Fade', 'Master Barber con más de 8 años de experiencia en cortes urbanos de élite y diseño con navaja.', 'https://images.unsplash.com/photo-1622286342621-4bd786c2447c?auto=format&fit=crop&q=80&w=300', true),
(2, 4, 1, 'Esteban "El Maestro" Rojas', 'Corte Clásico, Barba Spa y Toalla Caliente', 'Especialista en estilismo tradicional de caballeros, perfilado y rituales de toalla caliente.', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=300', true),
(3, 5, 2, 'Cristian "Black Gold" Castro', 'Freestyle, Tintura y Perfilado con Cera', 'Creatividad sin límites en diseños geométricos, colorimetría capilar y asesoría de imagen moderna.', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=300', true),
(4, 6, 3, 'Diego "La Navaja" Morales', 'Afeitado Tradicional y Barboterapia', 'Tratamientos de toalla caliente, vapor de ozono y afeitados al ras con técnicas italianas.', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&q=80&w=300', true)
ON CONFLICT (id_barbero) DO UPDATE SET
nombre_publico = EXCLUDED.nombre_publico, especialidad = EXCLUDED.especialidad, bio = EXCLUDED.bio;

-- 7. Horarios Semanales Base de Barberos (Lunes a Sábado, 10:00 a 19:00)
INSERT INTO public.horario_barbero (id_barbero, dia_semana, hora_inicio, hora_fin) VALUES
(1, 1, '10:00', '19:00'),
(1, 2, '10:00', '19:00'),
(1, 3, '10:00', '19:00'),
(1, 4, '10:00', '19:00'),
(1, 5, '10:00', '19:00'),
(1, 6, '10:00', '19:00'),
(2, 1, '10:00', '19:00'),
(2, 2, '10:00', '19:00'),
(2, 3, '10:00', '19:00'),
(2, 4, '10:00', '19:00'),
(2, 5, '10:00', '19:00'),
(2, 6, '10:00', '19:00')
ON CONFLICT (id_barbero, dia_semana, hora_inicio) DO NOTHING;

-- 8. Catálogo Oficial de Servicios
INSERT INTO public.servicio (id_servicio, nombre, categoria, reservable_individual, duracion_min, precio_base, activo) VALUES
(1, 'Corte de Pelo Degrado', 'CORTE', true, 45, 15000, true),
(2, 'Arreglo de Barba Royale', 'BARBA', true, 30, 10000, true),
(3, 'Combo Big Boys (Pelo + Barba)', 'COMBO', true, 60, 22000, true),
(4, 'Servicio Completo VIP', 'COMBO', true, 80, 28000, true),
(5, 'Corte Infantil (Hasta 10 años)', 'CORTE', true, 30, 12000, true),
(6, 'Corte Padre e Hijo', 'COMBO', true, 75, 25000, true),
(7, 'Perfilado de Cejas con Navaja', 'COMPLEMENTO', true, 15, 5000, true),
(8, 'Lavado & Peinado Estilizado', 'COMPLEMENTO', true, 20, 7000, true),
(9, 'Tratamiento Capilar Anticaída', 'TRATAMIENTO', true, 30, 14000, true)
ON CONFLICT (id_servicio) DO UPDATE SET
nombre = EXCLUDED.nombre, precio_base = EXCLUDED.precio_base, duracion_min = EXCLUDED.duracion_min;

-- 9. Parametrización Oficial del Abono y Reglas Comerciales (Página 5 del Informe)
INSERT INTO public.configuracion (clave, ambito, id_sucursal, id_servicio, tipo_valor, valor) VALUES
('ABONO_RESERVA', 'GLOBAL', NULL, NULL, 'MONTO', '3000'),
('HORAS_MIN_REAGENDAR', 'GLOBAL', NULL, NULL, 'ENTERO', '24'),
('MAX_REAGENDAMIENTOS', 'GLOBAL', NULL, NULL, 'ENTERO', '2'),
('MINUTOS_EXPIRA_PAGO', 'GLOBAL', NULL, NULL, 'ENTERO', '15')
ON CONFLICT DO NOTHING;

-- 10. Políticas Legales Vigentes (Ley N° 21.719)
INSERT INTO public.politica (id_politica, tipo, version, publicada_en, url, hash_sha256, vigente) VALUES
(1, 'PRIVACIDAD', '1.0', '2026-10-01', 'https://bigboys.cl/privacidad', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', true),
(2, 'TERMINOS', '1.0', '2026-10-01', 'https://bigboys.cl/terminos', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', true)
ON CONFLICT (id_politica) DO UPDATE SET vigente = EXCLUDED.vigente;

-- 11. Resetear secuencias numéricas (PostgreSQL sequences)
SELECT setval('public.sucursal_id_sucursal_seq', (SELECT COALESCE(MAX(id_sucursal), 1) FROM public.sucursal));
SELECT setval('public.usuario_id_usuario_seq', (SELECT COALESCE(MAX(id_usuario), 1) FROM public.usuario));
SELECT setval('public.cliente_id_cliente_seq', (SELECT COALESCE(MAX(id_cliente), 1) FROM public.cliente));
SELECT setval('public.barbero_id_barbero_seq', (SELECT COALESCE(MAX(id_barbero), 1) FROM public.barbero));
SELECT setval('public.servicio_id_servicio_seq', (SELECT COALESCE(MAX(id_servicio), 1) FROM public.servicio));
SELECT setval('public.configuracion_id_config_seq', (SELECT COALESCE(MAX(id_config), 1) FROM public.configuracion));
SELECT setval('public.politica_id_politica_seq', (SELECT COALESCE(MAX(id_politica), 1) FROM public.politica));
