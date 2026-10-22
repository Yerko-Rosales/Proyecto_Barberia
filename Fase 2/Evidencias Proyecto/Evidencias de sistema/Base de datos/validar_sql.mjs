import { PGlite } from '@electric-sql/pglite';
import { btree_gist } from '@electric-sql/pglite/contrib/btree_gist';
import fs from 'fs';

const db = new PGlite({ extensions: { btree_gist } });
const ddl = fs.readFileSync(process.argv[2], 'utf8');
const results = [];
const ok = (n) => results.push(['OK', n]);
const fail = (n, e) => results.push(['FALLA', n, e]);

try { await db.exec(ddl); ok('Script DDL completo ejecutado sin errores'); }
catch (e) { fail('Script DDL', e.message); console.log(results); process.exit(1); }

await db.exec(`SET search_path TO bigboys;
INSERT INTO usuario (email, password_hash, nombre) VALUES ('barbero1@bb.cl','x','Barbero Uno'),('cliente1@bb.cl','x','Cliente Uno'),('dueno@bb.cl','x','Dueño');
INSERT INTO barbero (id_usuario, id_sucursal, nombre_publico) VALUES (1,1,'Barbero Uno');
INSERT INTO cliente (id_usuario, nombre_contacto) VALUES (2,'Cliente Uno');
`);

const tests = [
  ['Reserva válida', `INSERT INTO reserva (codigo,id_cliente,id_sucursal,id_barbero,id_servicio,inicio,fin,monto_abono,canal) VALUES ('BB-0001',1,1,1,1,'2026-11-20 10:00-03','2026-11-20 10:40-03',3000,'WEB')`, true],
  ['Doble reserva traslapada (mismo barbero) es rechazada', `INSERT INTO reserva (codigo,id_cliente,id_sucursal,id_barbero,id_servicio,inicio,fin,monto_abono,canal) VALUES ('BB-0002',1,1,1,1,'2026-11-20 10:20-03','2026-11-20 11:00-03',3000,'APP')`, false],
  ['Reserva en horario cancelado previamente se permite', `UPDATE reserva SET estado='CANCELADA' WHERE codigo='BB-0001'; INSERT INTO reserva (codigo,id_cliente,id_sucursal,id_barbero,id_servicio,inicio,fin,monto_abono,canal) VALUES ('BB-0003',1,1,1,1,'2026-11-20 10:00-03','2026-11-20 10:40-03',3000,'APP')`, true],
  ['Más de 2 reagendamientos es rechazado', `UPDATE reserva SET cantidad_reagendamientos=3 WHERE codigo='BB-0003'`, false],
  ['Reagendamiento con menos de 24 h es rechazado', `INSERT INTO reagendamiento (id_reserva,inicio_anterior,inicio_nuevo,horas_anticipacion) SELECT id_reserva,'2026-11-20 10:00-03','2026-11-21 10:00-03',5 FROM reserva WHERE codigo='BB-0003'`, false],
  ['Invitado sin usuario permitido', `INSERT INTO cliente (es_invitado, nombre_contacto, telefono_contacto) VALUES (true,'Invitado','+56900000000')`, true],
  ['Cliente no invitado sin usuario es rechazado', `INSERT INTO cliente (es_invitado, nombre_contacto) VALUES (false,'X')`, false],
  ['Pago sin datos de tarjeta (solo ID y estado)', `INSERT INTO pago (id_reserva,proveedor,id_transaccion,monto,estado,tipo_medio) SELECT id_reserva,'WEBPAY','tok_123',3000,'AUTORIZADO','DEBITO' FROM reserva WHERE codigo='BB-0003'`, true],
  ['Atención sin reserva ni turno es rechazada', `INSERT INTO atencion (id_sucursal,id_barbero,id_servicio,inicio,monto_cobrado,registrada_por) VALUES (1,1,1,now(),11900,3)`, false],
  ['Turno por orden de llegada y atención asociada', `INSERT INTO turno_fila (id_sucursal,fecha,numero) VALUES (1,CURRENT_DATE,1); INSERT INTO atencion (id_turno,id_sucursal,id_barbero,id_servicio,inicio,monto_cobrado,registrada_por) VALUES (1,1,1,1,now(),11900,3)`, true],
  ['Atención sin cajero que la registre es rechazada', `INSERT INTO turno_fila (id_sucursal,fecha,numero) VALUES (1,CURRENT_DATE,2); INSERT INTO atencion (id_turno,id_sucursal,id_barbero,id_servicio,inicio,monto_cobrado) VALUES (2,1,1,1,now(),11900)`, false],
  ['Solicitud de derecho calcula plazo de 30 días', `INSERT INTO solicitud_derecho (codigo,id_cliente,tipo,fecha_limite) VALUES ('SD-0001',1,'ACCESO',CURRENT_DATE)`, true],
  ['Auditoría es de solo inserción (UPDATE rechazado)', `INSERT INTO registro_auditoria (accion,entidad) VALUES ('LOGIN','usuario'); UPDATE registro_auditoria SET entidad='x'`, false],
  ['Configuración de abono por porcentaje en una sucursal (sin cambiar modelo)', `INSERT INTO configuracion (clave,ambito,id_sucursal,tipo_valor,valor) VALUES ('ABONO_RESERVA','SUCURSAL',3,'PORCENTAJE','20')`, true],
  ['Primer Dueño del sistema se asigna (arranque)', `INSERT INTO usuario_rol (id_usuario,id_rol) VALUES (3,4)`, true],
  ['Dueño crea perfil de cajero/a con contraseña temporal', `INSERT INTO usuario (email,password_hash,nombre,creado_por,debe_cambiar_clave) VALUES ('cajero1@bb.cl','x','Cajera Uno',3,true); INSERT INTO usuario_rol (id_usuario,id_rol,id_sucursal) SELECT id_usuario,3,1 FROM usuario WHERE email='cajero1@bb.cl'`, true],
  ['Colaborador autorregistrado (sin Dueño creador) es rechazado', `INSERT INTO usuario (email,password_hash,nombre) VALUES ('falso@bb.cl','x','Falso'); INSERT INTO usuario_rol (id_usuario,id_rol,id_sucursal) SELECT id_usuario,2,1 FROM usuario WHERE email='falso@bb.cl'`, false],
  ['Perfil creado por un no-Dueño es rechazado', `INSERT INTO usuario (email,password_hash,nombre,creado_por) VALUES ('falso2@bb.cl','x','Falso 2',2); INSERT INTO usuario_rol (id_usuario,id_rol,id_sucursal) SELECT id_usuario,2,1 FROM usuario WHERE email='falso2@bb.cl'`, false],
  ['Cliente se autorregistra con rol CLIENTE', `INSERT INTO usuario_rol (id_usuario,id_rol) VALUES (2,1)`, true],
  ['Servicio de más de 60 minutos es rechazado (máximo confirmado)', `INSERT INTO servicio (nombre,categoria,duracion_min,precio_base) VALUES ('Prueba larga','COMBO',75,30000)`, false],
  ['Configuración GLOBAL con sucursal es rechazada', `INSERT INTO configuracion (clave,ambito,id_sucursal,tipo_valor,valor) VALUES ('X','GLOBAL',1,'ENTERO','1')`, false],
  ['Reseña válida de un cliente a un barbero', `INSERT INTO resena (id_cliente,id_sucursal,id_barbero,calificacion,comentario) VALUES (1,1,1,5,'Excelente')`, true],
  ['Reseña con calificación fuera de 1 a 5 es rechazada', `INSERT INTO resena (id_cliente,id_sucursal,id_barbero,calificacion) VALUES (1,1,1,6)`, false],
];
for (const [name, sql, expectOk] of tests) {
  try { await db.exec(sql); expectOk ? ok(name) : fail(name, 'se esperaba rechazo y fue aceptado'); }
  catch (e) { expectOk ? fail(name, e.message) : ok(name + ' → ' + e.message.split('\n')[0].slice(0, 90)); }
}
const r = await db.query(`SELECT fecha_limite - (fecha_ingreso AT TIME ZONE 'America/Santiago')::date AS dias FROM bigboys.solicitud_derecho`);
results.push(['INFO', 'Días de plazo calculados', String(r.rows[0]?.dias)]);
const v = await db.query(`SELECT DISTINCT id_sucursal, tipo_valor, valor FROM bigboys.v_abono_vigente ORDER BY 1`);
results.push(['INFO', 'Abono vigente por sucursal', JSON.stringify(v.rows)]);
const n = await db.query(`SELECT count(*) FROM information_schema.tables WHERE table_schema='bigboys' AND table_type='BASE TABLE'`);
results.push(['INFO', 'Tablas creadas', String(n.rows[0].count)]);
const vb = await db.query(`SELECT atenciones_acumuladas, total_acumulado FROM bigboys.v_atenciones_barbero ORDER BY inicio DESC LIMIT 1`);
results.push(['INFO', 'Vista del barbero (atenciones, total acumulado)', JSON.stringify(vb.rows[0])]);
const sv = await db.query(`SELECT count(*) AS n, min(precio_base) AS minimo, max(precio_base) AS maximo FROM bigboys.servicio`);
results.push(['INFO', 'Servicios cargados (n, mínimo, máximo)', JSON.stringify(sv.rows[0])]);
for (const x of results) console.log(x.join(' | '));
fs.writeFileSync(process.argv[3], results.map(x => x.join(' | ')).join('\n'));
