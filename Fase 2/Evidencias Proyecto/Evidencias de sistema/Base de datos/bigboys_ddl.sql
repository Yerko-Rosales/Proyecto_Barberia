-- =====================================================================
-- Proyecto Barbería Big-Boys — Script DDL de referencia (PostgreSQL 15+)
-- Modelo relacional v1.1 — 08-10-2026 (RAT actualizado el 20-10-2026: alojamiento en Supabase, EE. UU.)
-- Generado desde la definición única del modelo (diccionario de datos).
-- No almacena datos de tarjetas: el pago se delega a la pasarela (solo ID y estado).
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;  -- requerido para impedir reservas traslapadas
CREATE SCHEMA IF NOT EXISTS bigboys;
SET search_path TO bigboys;

-- Locales físicos de la barbería (3 en la versión inicial).
CREATE TABLE sucursal (
    id_sucursal SMALLSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    direccion VARCHAR(150) NOT NULL,
    comuna VARCHAR(60) NOT NULL,
    telefono VARCHAR(20),
    es_casa_matriz BOOLEAN NOT NULL DEFAULT false,
    activa BOOLEAN NOT NULL DEFAULT true,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Catálogo de roles del control de acceso (RBAC).
CREATE TABLE rol (
    id_rol SMALLINT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE CHECK (codigo IN ('CLIENTE','BARBERO','ADMIN_SUCURSAL','DUENO')),
    descripcion VARCHAR(120) NOT NULL
);

-- Cuenta de acceso (clientes registrados y personal). No almacena RUT ni datos de tarjetas.
CREATE TABLE usuario (
    id_usuario BIGSERIAL PRIMARY KEY,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre VARCHAR(80) NOT NULL,
    apellido VARCHAR(80),
    telefono VARCHAR(20),
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO','BLOQUEADO','SUPRIMIDO')),
    intentos_fallidos SMALLINT NOT NULL DEFAULT 0,
    debe_cambiar_clave BOOLEAN NOT NULL DEFAULT false,
    creado_por BIGINT REFERENCES usuario(id_usuario),
    ultimo_acceso TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ,
    suprimido_en TIMESTAMPTZ
);

-- Asignación de roles a usuarios, con alcance opcional por sucursal.
CREATE TABLE usuario_rol (
    id_usuario_rol BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES usuario(id_usuario),
    id_rol SMALLINT NOT NULL REFERENCES rol(id_rol),
    id_sucursal SMALLINT REFERENCES sucursal(id_sucursal),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (id_usuario, id_rol, id_sucursal)
);

-- Persona que reserva o es atendida. Puede ser registrado (con usuario) o invitado (solo nombre y teléfono).
CREATE TABLE cliente (
    id_cliente BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT UNIQUE REFERENCES usuario(id_usuario),
    es_invitado BOOLEAN NOT NULL DEFAULT false,
    nombre_contacto VARCHAR(80) NOT NULL,
    telefono_contacto VARCHAR(20),
    email_contacto VARCHAR(120),
    puntos INTEGER NOT NULL DEFAULT 0 CHECK (puntos >= 0),
    ultima_actividad TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (es_invitado OR id_usuario IS NOT NULL)
);

-- Colaborador que presta servicios en una sucursal.
CREATE TABLE barbero (
    id_barbero SERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL UNIQUE REFERENCES usuario(id_usuario),
    id_sucursal SMALLINT NOT NULL REFERENCES sucursal(id_sucursal),
    nombre_publico VARCHAR(80) NOT NULL,
    especialidad VARCHAR(80),
    activo BOOLEAN NOT NULL DEFAULT true,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Catálogo de servicios.
CREATE TABLE servicio (
    id_servicio SERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    categoria VARCHAR(30) NOT NULL CHECK (categoria IN ('CORTE','BARBA','COMBO','COMPLEMENTO','TRATAMIENTO')),
    reservable_individual BOOLEAN NOT NULL DEFAULT true,
    duracion_min SMALLINT NOT NULL CHECK (duracion_min BETWEEN 10 AND 60),
    precio_base NUMERIC(10,0) NOT NULL CHECK (precio_base > 0),
    activo BOOLEAN NOT NULL DEFAULT true
);

-- Servicios ofrecidos por sucursal y precio local (opcional).
CREATE TABLE servicio_sucursal (
    id_servicio INTEGER NOT NULL REFERENCES servicio(id_servicio),
    id_sucursal SMALLINT NOT NULL REFERENCES sucursal(id_sucursal),
    precio NUMERIC(10,0) CHECK (precio > 0),
    activo BOOLEAN NOT NULL DEFAULT true,
    PRIMARY KEY (id_servicio, id_sucursal)
);

-- Jornada semanal de cada barbero (disponibilidad base).
CREATE TABLE horario_barbero (
    id_horario SERIAL PRIMARY KEY,
    id_barbero INTEGER NOT NULL REFERENCES barbero(id_barbero),
    dia_semana SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL CHECK (hora_fin > hora_inicio),
    UNIQUE (id_barbero, dia_semana, hora_inicio)
);

-- Bloqueos puntuales de agenda (almuerzo, trámites, emergencias).
CREATE TABLE bloqueo_horario (
    id_bloqueo BIGSERIAL PRIMARY KEY,
    id_barbero INTEGER NOT NULL REFERENCES barbero(id_barbero),
    inicio TIMESTAMPTZ NOT NULL,
    fin TIMESTAMPTZ NOT NULL CHECK (fin > inicio),
    motivo VARCHAR(120),
    creado_por BIGINT NOT NULL REFERENCES usuario(id_usuario)
);

-- Parámetros del negocio versionados. Permite cambiar el abono (monto o porcentaje) por sucursal o servicio sin modificar el modelo.
CREATE TABLE configuracion (
    id_config SERIAL PRIMARY KEY,
    clave VARCHAR(60) NOT NULL,
    ambito VARCHAR(10) NOT NULL CHECK (ambito IN ('GLOBAL','SUCURSAL','SERVICIO')),
    id_sucursal SMALLINT REFERENCES sucursal(id_sucursal),
    id_servicio INTEGER REFERENCES servicio(id_servicio),
    tipo_valor VARCHAR(10) NOT NULL CHECK (tipo_valor IN ('MONTO','PORCENTAJE','ENTERO','TEXTO')),
    valor VARCHAR(60) NOT NULL,
    vigente_desde TIMESTAMPTZ NOT NULL DEFAULT now(),
    vigente_hasta TIMESTAMPTZ,
    modificado_por BIGINT REFERENCES usuario(id_usuario),
    CHECK ((ambito = 'GLOBAL' AND id_sucursal IS NULL AND id_servicio IS NULL) OR (ambito = 'SUCURSAL' AND id_sucursal IS NOT NULL) OR (ambito = 'SERVICIO' AND id_servicio IS NOT NULL))
);

-- Cita agendada. Solo queda tomada (CONFIRMADA) cuando el abono es autorizado por la pasarela.
CREATE TABLE reserva (
    id_reserva BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(12) NOT NULL UNIQUE,
    id_cliente BIGINT NOT NULL REFERENCES cliente(id_cliente),
    id_sucursal SMALLINT NOT NULL REFERENCES sucursal(id_sucursal),
    id_barbero INTEGER NOT NULL REFERENCES barbero(id_barbero),
    id_servicio INTEGER NOT NULL REFERENCES servicio(id_servicio),
    inicio TIMESTAMPTZ NOT NULL,
    fin TIMESTAMPTZ NOT NULL CHECK (fin > inicio),
    estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE_PAGO' CHECK (estado IN ('PENDIENTE_PAGO','CONFIRMADA','ATENDIDA','CANCELADA','NO_SHOW','EXPIRADA')),
    monto_abono NUMERIC(10,0) NOT NULL CHECK (monto_abono >= 0),
    cantidad_reagendamientos SMALLINT NOT NULL DEFAULT 0 CHECK (cantidad_reagendamientos BETWEEN 0 AND 2),
    canal VARCHAR(5) NOT NULL CHECK (canal IN ('WEB','APP','LOCAL')),
    expira_en TIMESTAMPTZ,
    cancelada_en TIMESTAMPTZ,
    motivo_cancelacion VARCHAR(120),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ,
    EXCLUDE USING gist (id_barbero WITH =, tstzrange(inicio, fin) WITH &&) WHERE (estado IN ('PENDIENTE_PAGO','CONFIRMADA','ATENDIDA'))
);

-- Historial de reagendamientos de una reserva (trazabilidad de la regla de 24 h y máximo 2).
CREATE TABLE reagendamiento (
    id_reagendamiento BIGSERIAL PRIMARY KEY,
    id_reserva BIGINT NOT NULL REFERENCES reserva(id_reserva),
    inicio_anterior TIMESTAMPTZ NOT NULL,
    inicio_nuevo TIMESTAMPTZ NOT NULL,
    horas_anticipacion NUMERIC(6,1) NOT NULL CHECK (horas_anticipacion >= 24),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Abono de reserva procesado por la pasarela. NO se almacenan datos de tarjetas: solo identificador y estado de la transacción.
CREATE TABLE pago (
    id_pago BIGSERIAL PRIMARY KEY,
    id_reserva BIGINT NOT NULL REFERENCES reserva(id_reserva),
    proveedor VARCHAR(15) NOT NULL CHECK (proveedor IN ('WEBPAY','MERCADOPAGO')),
    id_transaccion VARCHAR(100) NOT NULL UNIQUE,
    codigo_autorizacion VARCHAR(20),
    tipo_medio VARCHAR(10) CHECK (tipo_medio IN ('DEBITO','CREDITO','PREPAGO')),
    monto NUMERIC(10,0) NOT NULL CHECK (monto > 0),
    estado VARCHAR(12) NOT NULL CHECK (estado IN ('INICIADO','AUTORIZADO','RECHAZADO','APLICADO','RETENIDO','REEMBOLSADO','ANULADO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ
);

-- Turno de atención por orden de llegada (walk-in). Permite medir abandono real.
CREATE TABLE turno_fila (
    id_turno BIGSERIAL PRIMARY KEY,
    id_sucursal SMALLINT NOT NULL REFERENCES sucursal(id_sucursal),
    fecha DATE NOT NULL,
    numero SMALLINT NOT NULL CHECK (numero > 0),
    nombre_llamado VARCHAR(40),
    id_cliente BIGINT REFERENCES cliente(id_cliente),
    id_servicio INTEGER REFERENCES servicio(id_servicio),
    id_barbero_preferido INTEGER REFERENCES barbero(id_barbero),
    estado VARCHAR(12) NOT NULL DEFAULT 'EN_ESPERA' CHECK (estado IN ('EN_ESPERA','LLAMADO','EN_ATENCION','ATENDIDO','ABANDONO')),
    hora_llegada TIMESTAMPTZ NOT NULL DEFAULT now(),
    hora_llamado TIMESTAMPTZ,
    hora_termino TIMESTAMPTZ,
    UNIQUE (id_sucursal, fecha, numero)
);

-- Servicio efectivamente prestado. Reemplaza al registro manual en Excel.
CREATE TABLE atencion (
    id_atencion BIGSERIAL PRIMARY KEY,
    id_reserva BIGINT UNIQUE REFERENCES reserva(id_reserva),
    id_turno BIGINT UNIQUE REFERENCES turno_fila(id_turno),
    id_cliente BIGINT REFERENCES cliente(id_cliente),
    id_sucursal SMALLINT NOT NULL REFERENCES sucursal(id_sucursal),
    id_barbero INTEGER NOT NULL REFERENCES barbero(id_barbero),
    id_servicio INTEGER NOT NULL REFERENCES servicio(id_servicio),
    inicio TIMESTAMPTZ NOT NULL,
    fin TIMESTAMPTZ,
    monto_cobrado NUMERIC(10,0) NOT NULL CHECK (monto_cobrado >= 0),
    abono_aplicado NUMERIC(10,0) NOT NULL DEFAULT 0 CHECK (abono_aplicado >= 0),
    propina NUMERIC(10,0) NOT NULL DEFAULT 0 CHECK (propina >= 0),
    medio_pago_local VARCHAR(15) CHECK (medio_pago_local IN ('EFECTIVO','DEBITO','CREDITO','TRANSFERENCIA')),
    registrada_por BIGINT NOT NULL REFERENCES usuario(id_usuario),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (id_reserva IS NOT NULL OR id_turno IS NOT NULL)
);

-- Promociones (bienvenida, ruleta, códigos). Las comunicaciones promocionales requieren consentimiento.
CREATE TABLE promocion (
    id_promocion SERIAL PRIMARY KEY,
    codigo VARCHAR(20) UNIQUE,
    nombre VARCHAR(80) NOT NULL,
    tipo VARCHAR(12) NOT NULL CHECK (tipo IN ('BIENVENIDA','RULETA','CODIGO','PUNTOS')),
    tipo_descuento VARCHAR(10) NOT NULL CHECK (tipo_descuento IN ('MONTO','PORCENTAJE','SERVICIO')),
    valor NUMERIC(10,2) NOT NULL CHECK (valor >= 0),
    id_sucursal SMALLINT REFERENCES sucursal(id_sucursal),
    vigente_desde DATE NOT NULL,
    vigente_hasta DATE,
    cupos INTEGER CHECK (cupos > 0),
    activa BOOLEAN NOT NULL DEFAULT true
);

-- Beneficios asignados a clientes (bienvenida, ruleta, códigos).
CREATE TABLE cupon_cliente (
    id_cupon BIGSERIAL PRIMARY KEY,
    id_promocion INTEGER NOT NULL REFERENCES promocion(id_promocion),
    id_cliente BIGINT NOT NULL REFERENCES cliente(id_cliente),
    pin CHAR(4),
    estado VARCHAR(10) NOT NULL DEFAULT 'DISPONIBLE' CHECK (estado IN ('DISPONIBLE','CANJEADO','EXPIRADO')),
    id_atencion BIGINT REFERENCES atencion(id_atencion),
    asignado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    canjeado_en TIMESTAMPTZ
);

-- Acumulación y canje de puntos de fidelización (RF-20).
CREATE TABLE movimiento_puntos (
    id_movimiento BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES cliente(id_cliente),
    id_atencion BIGINT REFERENCES atencion(id_atencion),
    tipo VARCHAR(8) NOT NULL CHECK (tipo IN ('ACUMULA','CANJE','AJUSTE')),
    puntos INTEGER NOT NULL CHECK (puntos <> 0),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Mensajes enviados (confirmación, recordatorio, cancelación, promocional, derechos).
CREATE TABLE notificacion (
    id_notificacion BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT REFERENCES cliente(id_cliente),
    id_usuario BIGINT REFERENCES usuario(id_usuario),
    id_reserva BIGINT REFERENCES reserva(id_reserva),
    canal VARCHAR(6) NOT NULL CHECK (canal IN ('EMAIL','PUSH','INAPP','SMS')),
    tipo VARCHAR(15) NOT NULL CHECK (tipo IN ('CONFIRMACION','RECORDATORIO','CANCELACION','PROMOCIONAL','DERECHOS','SEGURIDAD')),
    asunto VARCHAR(150) NOT NULL,
    estado VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','ENVIADA','FALLIDA','LEIDA')),
    programada_para TIMESTAMPTZ,
    enviada_en TIMESTAMPTZ,
    CHECK (id_cliente IS NOT NULL OR id_usuario IS NOT NULL)
);

-- Versiones publicadas de la Política de Privacidad y de los Términos y Condiciones.
CREATE TABLE politica (
    id_politica SERIAL PRIMARY KEY,
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('PRIVACIDAD','TERMINOS')),
    version VARCHAR(10) NOT NULL,
    publicada_en DATE NOT NULL,
    url VARCHAR(200) NOT NULL,
    hash_sha256 CHAR(64) NOT NULL,
    vigente BOOLEAN NOT NULL DEFAULT false,
    UNIQUE (tipo, version)
);

-- Registro del consentimiento informado: versión de la política, fecha, finalidad y canal. El promocional es separado y opcional.
CREATE TABLE consentimiento (
    id_consentimiento BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES cliente(id_cliente),
    id_politica INTEGER NOT NULL REFERENCES politica(id_politica),
    finalidad VARCHAR(12) NOT NULL CHECK (finalidad IN ('SERVICIO','PROMOCIONAL')),
    otorgado BOOLEAN NOT NULL,
    canal VARCHAR(5) NOT NULL CHECK (canal IN ('WEB','APP','LOCAL')),
    registrado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    revocado_en TIMESTAMPTZ
);

-- Solicitudes de derechos del titular (acceso, rectificación, supresión, oposición, portabilidad y bloqueo) con plazos legales.
CREATE TABLE solicitud_derecho (
    id_solicitud BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(14) NOT NULL UNIQUE,
    id_cliente BIGINT NOT NULL REFERENCES cliente(id_cliente),
    tipo VARCHAR(14) NOT NULL CHECK (tipo IN ('ACCESO','RECTIFICACION','SUPRESION','OPOSICION','PORTABILIDAD','BLOQUEO')),
    estado VARCHAR(15) NOT NULL DEFAULT 'RECIBIDA' CHECK (estado IN ('RECIBIDA','EN_VERIFICACION','EN_PROCESO','PRORROGADA','RESPONDIDA','RECHAZADA')),
    detalle VARCHAR(500),
    fecha_ingreso TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_limite DATE NOT NULL,
    prorrogada_hasta DATE,
    fecha_respuesta TIMESTAMPTZ,
    respuesta VARCHAR(1000),
    atendida_por BIGINT REFERENCES usuario(id_usuario)
);

-- Log inmutable (solo inserción) de accesos y cambios. No guarda datos personales en claro en el detalle.
CREATE TABLE registro_auditoria (
    id_log BIGSERIAL PRIMARY KEY,
    ocurrido_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    id_usuario BIGINT REFERENCES usuario(id_usuario),
    rol VARCHAR(20),
    accion VARCHAR(25) NOT NULL CHECK (accion IN ('LOGIN','LOGIN_FALLIDO','LOGOUT','CREAR','ACTUALIZAR','ELIMINAR','CONSULTAR_DATO_PERSONAL','EXPORTAR','CAMBIO_CONFIG','DERECHO_TITULAR')),
    entidad VARCHAR(40) NOT NULL,
    id_entidad VARCHAR(40),
    detalle JSONB,
    ip_origen INET
);

-- Registro de incidentes y brechas de seguridad y de sus notificaciones (Agencia y titulares).
CREATE TABLE incidente_seguridad (
    id_incidente SERIAL PRIMARY KEY,
    detectado_en TIMESTAMPTZ NOT NULL,
    descripcion VARCHAR(1000) NOT NULL,
    categorias_datos VARCHAR(200),
    titulares_afectados INTEGER CHECK (titulares_afectados >= 0),
    nivel_riesgo VARCHAR(6) NOT NULL CHECK (nivel_riesgo IN ('BAJO','MEDIO','ALTO')),
    notificado_agencia_en TIMESTAMPTZ,
    notificado_titulares_en TIMESTAMPTZ,
    estado VARCHAR(10) NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO','CONTENIDO','CERRADO'))
);

-- Registro de Actividades de Tratamiento (RAT) mantenido dentro del sistema.
CREATE TABLE actividad_tratamiento (
    id_actividad SERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    finalidad VARCHAR(300) NOT NULL,
    base_licitud VARCHAR(30) NOT NULL CHECK (base_licitud IN ('CONSENTIMIENTO','CONTRATO','OBLIGACION_LEGAL','INTERES_LEGITIMO')),
    categorias_datos VARCHAR(300) NOT NULL,
    destinatarios VARCHAR(300),
    transferencia_internacional VARCHAR(200),
    plazo_conservacion VARCHAR(150) NOT NULL,
    medidas_seguridad VARCHAR(300) NOT NULL
);

-- Índices de apoyo a consultas frecuentes
CREATE INDEX ix_reserva_sucursal_inicio ON reserva (id_sucursal, inicio);
CREATE INDEX ix_reserva_cliente ON reserva (id_cliente);
CREATE INDEX ix_reserva_estado_expira ON reserva (estado, expira_en);
CREATE INDEX ix_atencion_sucursal_inicio ON atencion (id_sucursal, inicio);
CREATE INDEX ix_atencion_barbero_inicio ON atencion (id_barbero, inicio);
CREATE INDEX ix_turno_sucursal_fecha ON turno_fila (id_sucursal, fecha, estado);
CREATE INDEX ix_pago_reserva ON pago (id_reserva);
CREATE INDEX ix_consentimiento_cliente ON consentimiento (id_cliente, finalidad);
CREATE INDEX ix_solicitud_estado_limite ON solicitud_derecho (estado, fecha_limite);
CREATE INDEX ix_auditoria_fecha ON registro_auditoria (ocurrido_en);
CREATE INDEX ix_notificacion_programada ON notificacion (estado, programada_para);
CREATE UNIQUE INDEX ux_config_vigente ON configuracion (clave, ambito, COALESCE(id_sucursal, 0), COALESCE(id_servicio, 0)) WHERE vigente_hasta IS NULL;
CREATE UNIQUE INDEX ux_politica_vigente ON politica (tipo) WHERE vigente;

-- Registro de auditoría inmutable: se impide modificar o borrar filas
CREATE OR REPLACE FUNCTION fn_auditoria_inmutable() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'registro_auditoria es de solo inserción';
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER tg_auditoria_inmutable BEFORE UPDATE OR DELETE ON registro_auditoria
  FOR EACH ROW EXECUTE FUNCTION fn_auditoria_inmutable();

-- Solo el Dueño crea perfiles de colaboradores: un rol distinto de CLIENTE exige que el
-- usuario haya sido creado por un Dueño (excepción: el primer Dueño del sistema)
CREATE OR REPLACE FUNCTION fn_alta_colaborador() RETURNS trigger AS $$
DECLARE v_rol text; v_creador bigint;
BEGIN
  SELECT codigo INTO v_rol FROM rol WHERE id_rol = NEW.id_rol;
  IF v_rol = 'CLIENTE' THEN RETURN NEW; END IF;
  IF v_rol = 'DUENO' AND NOT EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r USING (id_rol) WHERE r.codigo = 'DUENO') THEN
    RETURN NEW;
  END IF;
  SELECT creado_por INTO v_creador FROM usuario WHERE id_usuario = NEW.id_usuario;
  IF v_creador IS NULL OR NOT EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r USING (id_rol)
                                      WHERE ur.id_usuario = v_creador AND r.codigo = 'DUENO') THEN
    RAISE EXCEPTION 'Solo el Dueño puede crear perfiles de colaboradores (rol %)', v_rol;
  END IF;
  RETURN NEW;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER tg_alta_colaborador BEFORE INSERT OR UPDATE ON usuario_rol
  FOR EACH ROW EXECUTE FUNCTION fn_alta_colaborador();

-- Plazo legal de respuesta: 30 días corridos desde el ingreso (Ley N° 21.719, art. 11)
CREATE OR REPLACE FUNCTION fn_solicitud_plazo() RETURNS trigger AS $$
BEGIN
  NEW.fecha_limite := (NEW.fecha_ingreso AT TIME ZONE 'America/Santiago')::date + 30;
  RETURN NEW;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER tg_solicitud_plazo BEFORE INSERT ON solicitud_derecho
  FOR EACH ROW EXECUTE FUNCTION fn_solicitud_plazo();

-- Vista: monto de abono vigente por sucursal y servicio (servicio > sucursal > global)
CREATE OR REPLACE VIEW v_abono_vigente AS
SELECT ss.id_sucursal, ss.id_servicio,
       COALESCE(cs.tipo_valor, csu.tipo_valor, cg.tipo_valor) AS tipo_valor,
       COALESCE(cs.valor, csu.valor, cg.valor)::numeric AS valor
FROM servicio_sucursal ss
LEFT JOIN configuracion cs  ON cs.clave = 'ABONO_RESERVA' AND cs.ambito = 'SERVICIO' AND cs.id_servicio = ss.id_servicio AND cs.vigente_hasta IS NULL
LEFT JOIN configuracion csu ON csu.clave = 'ABONO_RESERVA' AND csu.ambito = 'SUCURSAL' AND csu.id_sucursal = ss.id_sucursal AND csu.vigente_hasta IS NULL
LEFT JOIN configuracion cg  ON cg.clave = 'ABONO_RESERVA' AND cg.ambito = 'GLOBAL' AND cg.vigente_hasta IS NULL;

-- Vista para el barbero (solo lectura): atenciones del día, monto de cada una y total acumulado (RF-62)
CREATE OR REPLACE VIEW v_atenciones_barbero AS
SELECT a.id_barbero, (a.inicio AT TIME ZONE 'America/Santiago')::date AS fecha, a.inicio, s.nombre AS servicio, a.monto_cobrado, a.propina,
       COUNT(*) OVER w AS atenciones_acumuladas, SUM(a.monto_cobrado) OVER w AS total_acumulado
FROM atencion a JOIN servicio s ON s.id_servicio = a.id_servicio
WINDOW w AS (PARTITION BY a.id_barbero, (a.inicio AT TIME ZONE 'America/Santiago')::date ORDER BY a.inicio);

-- Vista KPI: abandono diario de la fila por orden de llegada, por sucursal
CREATE OR REPLACE VIEW v_kpi_abandono_diario AS
SELECT id_sucursal, fecha,
       COUNT(*) AS llegadas,
       COUNT(*) FILTER (WHERE estado = 'ATENDIDO') AS atendidos,
       COUNT(*) FILTER (WHERE estado = 'ABANDONO') AS abandonos,
       ROUND(100.0 * COUNT(*) FILTER (WHERE estado = 'ABANDONO') / NULLIF(COUNT(*), 0), 1) AS pct_abandono
FROM turno_fila GROUP BY id_sucursal, fecha;

-- =====================================================================
-- Datos iniciales
-- =====================================================================
INSERT INTO rol (id_rol, codigo, descripcion) VALUES
  (1, 'CLIENTE', 'Cliente registrado'),
  (2, 'BARBERO', 'Barbero / colaborador'),
  (3, 'ADMIN_SUCURSAL', 'Administrador de sucursal'),
  (4, 'DUENO', 'Dueño / administrador general');

INSERT INTO sucursal (codigo, nombre, direccion, comuna, es_casa_matriz) VALUES
  ('SUC-01', 'Peñaflor – Sucursal 21 de Mayo', '21 de Mayo 4566', 'Peñaflor', true),
  ('SUC-02', 'Peñaflor – Sucursal Alcalde Luis Araya Cereceda', 'Alcalde Luis Araya Cereceda 4387', 'Peñaflor', false),
  ('SUC-03', 'Talagante', 'Boulevard del Centro, local 3', 'Talagante', false);

-- Catálogo real de servicios (precios entregados por el negocio el 06-10-2026; S. Completo 60 min es el máximo confirmado; el resto son estimaciones menores (S-21))
INSERT INTO servicio (nombre, categoria, duracion_min, precio_base, reservable_individual) VALUES
  ('Corte', 'CORTE', 40, 11900, true),
  ('Corte + Cejas', 'COMBO', 45, 15470, true),
  ('Corte + Barba', 'COMBO', 50, 20230, true),
  ('S. Completo', 'COMBO', 60, 23800, true),
  ('Corte + Cejas + Lavado', 'COMBO', 55, 17850, true),
  ('Lavado', 'COMPLEMENTO', 15, 3570, false),
  ('Barba', 'BARBA', 30, 8330, true),
  ('Cejas', 'COMPLEMENTO', 15, 3570, false),
  ('Mascarillas Faciales', 'COMPLEMENTO', 15, 2380, false);

-- Todos los servicios se ofrecen en las 3 sucursales al precio base
INSERT INTO servicio_sucursal (id_servicio, id_sucursal) SELECT s.id_servicio, su.id_sucursal FROM servicio s CROSS JOIN sucursal su;

-- Parámetros del negocio (abono parametrizable: monto fijo hoy, porcentaje en el futuro sin cambiar el modelo)
INSERT INTO configuracion (clave, ambito, tipo_valor, valor) VALUES
  ('ABONO_RESERVA', 'GLOBAL', 'MONTO', '3000'),
  ('HORAS_MIN_REAGENDAR', 'GLOBAL', 'ENTERO', '24'),
  ('MAX_REAGENDAMIENTOS', 'GLOBAL', 'ENTERO', '2'),
  ('MINUTOS_EXPIRA_PAGO', 'GLOBAL', 'ENTERO', '15'),
  ('MESES_INACTIVIDAD_SUPRESION', 'GLOBAL', 'ENTERO', '24'),
  ('MESES_RETENCION_INVITADO', 'GLOBAL', 'ENTERO', '12'),
  ('DIAS_PLAZO_DERECHOS', 'GLOBAL', 'ENTERO', '30');

INSERT INTO politica (tipo, version, publicada_en, url, hash_sha256, vigente) VALUES
  ('PRIVACIDAD', '1.0', DATE '2026-11-13', 'https://[dominio-por-definir]/privacidad', repeat('0', 64), true),
  ('TERMINOS', '1.0', DATE '2026-11-13', 'https://[dominio-por-definir]/terminos', repeat('0', 64), true);

INSERT INTO actividad_tratamiento (nombre, finalidad, base_licitud, categorias_datos, destinatarios, transferencia_internacional, plazo_conservacion, medidas_seguridad) VALUES
  ('Gestión de cuentas y reservas', 'Crear la cuenta, agendar, confirmar y recordar citas', 'CONTRATO', 'Nombre, correo, teléfono, historial de reservas', 'Proveedor de hosting; proveedor de correo transaccional', 'Sí: EE. UU. (base de datos en Supabase, región us-west-2, Oregón; garantías contractuales del proveedor)', 'Mientras la cuenta esté activa; anonimización a los 24 meses de inactividad o al ejercer supresión', 'TLS, cifrado en reposo, RBAC, auditoría, respaldos'),
  ('Cobro de abono de reserva', 'Garantizar la asistencia mediante abono de $3.000', 'CONTRATO', 'ID de transacción, monto, estado (sin datos de tarjeta)', 'Transbank / Mercado Pago (encargados)', 'Sí: EE. UU. (base de datos en Supabase, región us-west-2, Oregón; garantías contractuales del proveedor)', '6 años (respaldo contable/tributario); el vínculo con el titular se anonimiza al suprimir la cuenta', 'Pago delegado a pasarela certificada PCI DSS'),
  ('Comunicaciones promocionales', 'Enviar ofertas y beneficios', 'CONSENTIMIENTO', 'Nombre, correo, teléfono', 'Proveedor de correo', 'Sí: EE. UU. (base de datos en Supabase, región us-west-2, Oregón; garantías contractuales del proveedor)', 'Hasta revocación del consentimiento', 'Consentimiento separado y revocable'),
  ('Atención por orden de llegada', 'Gestionar la fila y medir abandono', 'INTERES_LEGITIMO', 'Nombre de pila (opcional)', 'Ninguno', 'Sí: EE. UU. (base de datos en Supabase, región us-west-2, Oregón; garantías contractuales del proveedor)', '24 horas para el nombre; estadística anónima 6 años', 'Minimización'),
  ('Ejercicio de derechos del titular', 'Atender solicitudes ARCO-P y bloqueo', 'OBLIGACION_LEGAL', 'Identificación y detalle de la solicitud', 'Ninguno', 'Sí: EE. UU. (base de datos en Supabase, región us-west-2, Oregón; garantías contractuales del proveedor)', '5 años desde el cierre de la solicitud', 'Trazabilidad completa y plazos automáticos');
