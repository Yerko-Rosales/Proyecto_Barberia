-- =====================================================================
-- 06-11-2026 (S13) - Limpieza previa al UAT: cuentas de prueba del 04-11
-- prueba.sinrut.0411@bigboys.test (id_usuario 13, id_cliente 53)
-- prueba.sinrut.app0411@bigboys.test (id_usuario 14, id_cliente 54)
--
-- No se borran: registro_auditoria es de solo inserción (tg_auditoria_inmutable)
-- y conserva 4 filas de estas cuentas. Se anonimizan igual que api_suprimir_cuenta:
-- correo inválido, nombre genérico, sin teléfono, clave aleatoria, estado SUPRIMIDO,
-- consentimientos revocados. No se crea solicitud_derecho: no la pidió un titular,
-- es limpieza de datos de prueba. Queda una fila de auditoría por cuenta.
-- =====================================================================
BEGIN;

UPDATE usuario
   SET email = 'suprimido-' || id_usuario || '@bigboys.invalid', nombre = 'Titular', apellido = 'suprimido',
       telefono = NULL, password_hash = extensions.crypt(gen_random_uuid()::text, extensions.gen_salt('bf', 10)),
       estado = 'SUPRIMIDO', suprimido_en = now(), actualizado_en = now()
 WHERE id_usuario IN (13, 14) AND email LIKE 'prueba.sinrut%@bigboys.test';

UPDATE cliente SET nombre_contacto = 'Titular suprimido', telefono_contacto = NULL, email_contacto = NULL
 WHERE id_usuario IN (13, 14);

UPDATE consentimiento SET revocado_en = now()
 WHERE id_cliente IN (SELECT id_cliente FROM cliente WHERE id_usuario IN (13, 14)) AND revocado_en IS NULL;

INSERT INTO registro_auditoria (id_usuario, accion, entidad, id_entidad, detalle)
SELECT id_usuario, 'ACTUALIZAR', 'usuario', id_usuario::text, '{"motivo":"limpieza de datos de prueba previa al UAT (06-11-2026)","estado":"SUPRIMIDO"}'
  FROM usuario WHERE id_usuario IN (13, 14) AND estado = 'SUPRIMIDO';

COMMIT;

-- Verificación
SELECT id_usuario, email, nombre, apellido, telefono, estado, suprimido_en FROM usuario WHERE id_usuario IN (13, 14);
