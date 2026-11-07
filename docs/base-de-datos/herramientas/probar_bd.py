"""Crea la base de datos desde cero y comprueba que funcione y rechace lo inválido.

Uso: python probar_bd.py [archivo de credenciales de MySQL]
Trabaja solo en una base temporal, drogueria_verificacion, que se borra al terminar.
Primero la llena con los scripts reales (01 y 02) y después con un año de operación
simulada que arma generar_datos.py en memoria. La base real «drogueria» no se toca.
"""
import os
import pathlib
import subprocess
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from generar_datos import datos_prueba  # noqa: E402

MYSQL = os.environ.get('MYSQL_BIN', r'C:\Program Files\MySQL\MySQL Server 9.7\bin\mysql.exe')
CRED = sys.argv[1] if len(sys.argv) > 1 else r'C:\Users\ACER1\mysql-drogueria\root97.cnf'
SQL = pathlib.Path(__file__).resolve().parents[1] / 'sql'
BASE = 'drogueria_verificacion'
fallos = 0


def correr(codigo, base=BASE):
    r = subprocess.run([MYSQL, f'--defaults-extra-file={CRED}', '--batch', '--skip-column-names', base],
                       input=codigo.encode('utf-8'), capture_output=True, timeout=600)
    return r.returncode, r.stdout.decode('utf-8', 'replace').strip(), r.stderr.decode('utf-8', 'replace').strip()


def ok(nombre, condicion, extra=''):
    global fallos
    print(('  OK     ' if condicion else '  FALLA  ') + nombre + (f'  -> {extra}' if extra and not condicion else ''))
    fallos += 0 if condicion else 1


def valor(codigo, base=BASE):
    c, o, e = correr(codigo, base)
    if c:
        raise SystemExit(f'Error en consulta: {e}\n{codigo}')
    return o


def cargar(base, *archivos):
    for a in archivos:
        c, _, e = correr((SQL / a).read_text('utf-8'), base)
        if c:
            raise SystemExit(f'{a} falló en {base}: {e}')


def rechaza(nombre, codigo, texto):
    """Lo inválido debe fallar. Al cerrar la conexión, la transacción se revierte."""
    c, _, e = correr('START TRANSACTION;\n' + codigo)
    ok(nombre, c != 0 and texto.lower() in e.lower(), e or 'no fue rechazado')


def resultado(nombre, codigo, esperado):
    """Corre un escenario válido y lo revierte, sin alterar los datos."""
    c, o, e = correr(f'START TRANSACTION;\n{codigo}\nROLLBACK;')
    ultima = o.splitlines()[-1] if o else ''
    ok(nombre, c == 0 and ultima == esperado, f'{e or ultima!r}, se esperaba {esperado!r}')


def contar_registros():
    tablas = valor(f"SELECT table_name FROM information_schema.tables WHERE table_schema='{BASE}' AND table_type='BASE TABLE' ORDER BY 1;").split()
    union = ' UNION ALL '.join(f"SELECT '{t}', COUNT(*) FROM {t}" for t in tablas)
    return [(t, int(n)) for t, n in (f.split('\t') for f in valor(union + ';').splitlines())]


print('1. Instalación desde cero (en la base temporal)')
inicio = time.time()
c, _, e = correr((SQL / '00-crear-bases.sql').read_text('utf-8'), 'mysql')
ok('00-crear-bases.sql', c == 0, e)
correr(f'DROP DATABASE IF EXISTS {BASE}; CREATE DATABASE {BASE} CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;', 'mysql')
cargar(BASE, '01-esquema.sql', '02-datos-iniciales.sql')
print(f'  OK     01 y 02 cargados en {time.time() - inicio:.0f} s')

print('\n2. Lo que se instala en la base real')
ok('sin usuarios, clientes ni pedidos inventados',
   valor('SELECT (SELECT COUNT(*) FROM usuario)+(SELECT COUNT(*) FROM cliente)+(SELECT COUNT(*) FROM pedido)+(SELECT COUNT(*) FROM lote);') == '0')
import csv as _csv
_cat = list(_csv.DictReader(open(SQL.parent / 'datos/catalogo-verificado.csv', encoding='utf-8')))
_esperado = f"{len({f['producto_id'] for f in _cat})}-{len(_cat)}-{len({f['categoria_codigo'] for f in _cat})}"
ok(f'catálogo verificado cargado completo ({_esperado} productos-presentaciones-categorías)',
   valor('SELECT CONCAT((SELECT COUNT(*) FROM producto),"-",(SELECT COUNT(*) FROM presentacion),"-",(SELECT COUNT(*) FROM categoria));') == _esperado)
reales = sum(n for _, n in contar_registros())
ok(f'la instalación real trae al menos mil registros ({reales})', reales >= 1000, str(reales))
c, _, e = correr(datos_prueba('otra_base'))
ok('los datos simulados se niegan a cargarse fuera de la base temporal', c != 0 and 'solo se cargan' in e, e)

print('\nCargando un año de operación simulada en la base temporal...')
c, _, e = correr(datos_prueba(BASE))
if c:
    raise SystemExit(f'Los datos simulados fallaron: {e}')

print('\n3. Coherencia de los datos cargados (cada conteo debe dar 0)')
coherencia = {
    'cada lote coincide con la suma de sus movimientos':
        'SELECT COUNT(*) FROM lote l LEFT JOIN (SELECT lote_id, SUM(cantidad_movida) s FROM movimiento_inventario GROUP BY lote_id) m ON m.lote_id=l.id WHERE l.cantidad_actual<>COALESCE(m.s,0);',
    'subtotal del pedido coincide con sus líneas':
        'SELECT COUNT(*) FROM pedido p JOIN (SELECT pedido_id, SUM(cantidad_vendida*precio_original) s FROM detalle_pedido GROUP BY pedido_id) d ON d.pedido_id=p.id WHERE p.subtotal_pedido<>d.s;',
    'descuento del pedido coincide con sus líneas':
        'SELECT COUNT(*) FROM pedido p JOIN (SELECT pedido_id, SUM(cantidad_vendida*(precio_original-precio_unitario)) s FROM detalle_pedido GROUP BY pedido_id) d ON d.pedido_id=p.id WHERE p.descuento_aplicado<>d.s;',
    'total calculado correcto':
        'SELECT COUNT(*) FROM pedido WHERE total_pedido<>subtotal_pedido-descuento_aplicado+costo_domicilio;',
    'todo pedido tiene líneas': 'SELECT COUNT(*) FROM pedido p LEFT JOIN detalle_pedido d ON d.pedido_id=p.id WHERE d.id IS NULL;',
    'cada línea activa descontó exactamente lo vendido':
        "SELECT COUNT(*) FROM detalle_pedido d JOIN (SELECT detalle_pedido_id i, -SUM(cantidad_movida) s FROM movimiento_inventario WHERE tipo_movimiento='venta' GROUP BY detalle_pedido_id) m ON m.i=d.id WHERE m.s<>d.cantidad_vendida;",
    'toda línea tiene su salida de inventario':
        "SELECT COUNT(*) FROM detalle_pedido d LEFT JOIN movimiento_inventario m ON m.detalle_pedido_id=d.id AND m.tipo_movimiento='venta' WHERE m.id IS NULL;",
    'pedidos anulados devolvieron todo a inventario':
        "SELECT COUNT(*) FROM pedido p JOIN detalle_pedido d ON d.pedido_id=p.id JOIN movimiento_inventario m ON m.detalle_pedido_id=d.id WHERE p.estado_pedido='cancelado' GROUP BY p.id HAVING SUM(m.cantidad_movida)<>0;",
    'ningún lote se vendió vencido':
        "SELECT COUNT(*) FROM movimiento_inventario m JOIN lote l ON l.id=m.lote_id WHERE m.tipo_movimiento='venta' AND l.fecha_vencimiento<DATE(m.fecha_movimiento);",
    'un cliente no tiene dos planes activos':
        "SELECT COUNT(*) FROM (SELECT cliente_id FROM suscripcion WHERE estado_suscripcion='activa' GROUP BY cliente_id HAVING COUNT(*)>1) x;",
    'la suscripción coincide con su solicitud':
        'SELECT COUNT(*) FROM suscripcion s JOIN solicitud_suscripcion o ON o.id=s.solicitud_id WHERE o.cliente_id<>s.cliente_id OR o.plan_id<>s.plan_id OR o.estado_solicitud<>"aprobado";',
    'pagos no superan el total': 'SELECT COUNT(*) FROM pago_pedido g JOIN pedido p ON p.id=g.pedido_id WHERE g.monto_pago>p.total_pedido;',
    'el saldo final de cada movimiento no es negativo': 'SELECT COUNT(*) FROM movimiento_inventario WHERE saldo_resultante<0;',
    'hay una sola alerta abierta por situación':
        "SELECT COUNT(*) FROM (SELECT tipo_alerta,presentacion_id,lote_id FROM notificacion WHERE alerta_atendida=FALSE AND tipo_alerta<>'administrativa' GROUP BY 1,2,3 HAVING COUNT(*)>1) x;",
    'no hay correos de prueba fuera del dominio reservado':
        "SELECT COUNT(*) FROM cliente WHERE correo_cliente NOT LIKE '%@ejemplo.invalid';",
}
for nombre, consulta in coherencia.items():
    o = valor(consulta)
    ok(nombre, o in ('0', ''), o)

print('\n4. La base rechaza lo inválido')
L = '(SELECT id FROM lote WHERE cantidad_actual>0 AND (fecha_vencimiento IS NULL OR fecha_vencimiento>CURDATE()) ORDER BY id LIMIT 1)'
DET = '(SELECT id FROM detalle_pedido ORDER BY id LIMIT 1)'
rechaza('vender más de lo que hay', f"SELECT id, cantidad_actual INTO @l, @q FROM lote WHERE cantidad_actual>0 AND (fecha_vencimiento IS NULL OR fecha_vencimiento>CURDATE()) LIMIT 1; INSERT INTO movimiento_inventario(lote_id,usuario_id,tipo_movimiento,cantidad_movida,detalle_pedido_id) VALUES (@l,4,'venta',-(@q+1),{DET});", 'no alcanza')
rechaza('cambiar la existencia sin un movimiento', f'SET @l={L}; UPDATE lote SET cantidad_actual=cantidad_actual+5 WHERE id=@l;', 'solo cambia')
rechaza('borrar el historial de inventario', 'DELETE FROM movimiento_inventario LIMIT 1;', 'no se puede borrar')
rechaza('modificar el historial de inventario', 'UPDATE movimiento_inventario SET cantidad_movida=1 LIMIT 1;', 'no se puede modificar')
rechaza('borrar la auditoría', 'DELETE FROM auditoria LIMIT 1;', 'no se puede borrar')
rechaza('vender un lote vencido', """
INSERT INTO lote(presentacion_id,numero_lote,fecha_vencimiento,costo_unitario) VALUES (1,'VENCIDO','2020-01-01',100);
SET @l=LAST_INSERT_ID();
INSERT INTO recepcion_mercancia(proveedor_id,usuario_id,numero_factura,total_factura) VALUES (1,7,'X',1000);
INSERT INTO detalle_recepcion(recepcion_id,lote_id,cantidad_recibida,costo_unitario) VALUES (LAST_INSERT_ID(),@l,10,100);
INSERT INTO movimiento_inventario(lote_id,usuario_id,tipo_movimiento,cantidad_movida,detalle_recepcion_id) VALUES (@l,7,'entrada',10,LAST_INSERT_ID());
INSERT INTO movimiento_inventario(lote_id,usuario_id,tipo_movimiento,cantidad_movida,detalle_pedido_id) VALUES (@l,4,'venta',-1,(SELECT id FROM detalle_pedido LIMIT 1));""", 'vencido')
rechaza('salida con signo positivo', f"SET @l={L}; INSERT INTO movimiento_inventario(lote_id,usuario_id,tipo_movimiento,cantidad_movida,detalle_pedido_id) VALUES (@l,4,'venta',5,{DET});", 'ck_movinv_signo')
rechaza('ajuste de inventario sin motivo', f"SET @l={L}; INSERT INTO movimiento_inventario(lote_id,usuario_id,tipo_movimiento,cantidad_movida) VALUES (@l,7,'ajuste',1);", 'ck_movinv_origen')
rechaza('movimiento manual sin responsable', f"SET @l={L}; INSERT INTO movimiento_inventario(lote_id,tipo_movimiento,cantidad_movida,motivo_movimiento) VALUES (@l,'ajuste',1,'x');", 'ck_movinv_usuario')
rechaza('dos planes activos para un cliente', """
SET @c=(SELECT cliente_id FROM suscripcion WHERE estado_suscripcion='activa' LIMIT 1);
INSERT INTO solicitud_suscripcion(cliente_id,plan_id,metodo_pago_id,origen_solicitud,monto_pagado,estado_solicitud,revisado_por_id,fecha_revision) VALUES (@c,1,1,'web',4900,'aprobado',2,NOW());
INSERT INTO suscripcion(cliente_id,plan_id,solicitud_id,descuento_porcentaje,fecha_inicio,fecha_vencimiento) VALUES (@c,1,LAST_INSERT_ID(),3,NOW(),NOW()+INTERVAL 30 DAY);""", 'uk_suscripcion_cliente_activo')
rechaza('suscripción con cliente distinto al de la solicitud', """
INSERT INTO solicitud_suscripcion(cliente_id,plan_id,metodo_pago_id,origen_solicitud,monto_pagado,estado_solicitud,revisado_por_id,fecha_revision) VALUES (1,1,1,'web',4900,'aprobado',2,NOW());
INSERT INTO suscripcion(cliente_id,plan_id,solicitud_id,descuento_porcentaje,fecha_inicio,fecha_vencimiento,estado_suscripcion) VALUES (2,1,LAST_INSERT_ID(),3,NOW(),NOW()+INTERVAL 30 DAY,'vencida');""", 'fk_suscripcion_solicitud')
rechaza('pedido con la suscripción de otro cliente', """
SELECT id, cliente_id INTO @s, @c FROM suscripcion LIMIT 1;
INSERT INTO pedido(numero_pedido,canal_venta,cliente_id,suscripcion_id,modalidad_entrega,subtotal_pedido) VALUES ('T-1','web',IF(@c=1,2,1),@s,'tienda',100);""", 'fk_pedido_suscripcion')
rechaza('pedido web sin cliente', "INSERT INTO pedido(numero_pedido,canal_venta,modalidad_entrega,subtotal_pedido) VALUES ('T-2','web','tienda',100);", 'ck_pedido_web')
rechaza('venta de mostrador sin vendedor', "INSERT INTO pedido(numero_pedido,canal_venta,modalidad_entrega,subtotal_pedido) VALUES ('T-3','pos','tienda',100);", 'ck_pedido_pos')
rechaza('domicilio sin dirección', "INSERT INTO pedido(numero_pedido,canal_venta,cliente_id,modalidad_entrega,subtotal_pedido) VALUES ('T-4','web',1,'domicilio',100);", 'ck_pedido_domicilio')
rechaza('descuento mayor que el subtotal', "INSERT INTO pedido(numero_pedido,canal_venta,cliente_id,modalidad_entrega,subtotal_pedido,descuento_aplicado) VALUES ('T-5','web',1,'tienda',100,200);", 'ck_pedido_montos')
rechaza('precio cobrado mayor que el de lista', "INSERT INTO detalle_pedido(pedido_id,presentacion_id,nombre_producto,cantidad_vendida,precio_original,precio_unitario) VALUES (1,1,'x',1,100,200);", 'ck_detalle_precios')
rechaza('línea con cantidad cero', "INSERT INTO detalle_pedido(pedido_id,presentacion_id,nombre_producto,cantidad_vendida,precio_original,precio_unitario) VALUES (1,1,'x',0,100,100);", 'ck_detalle_cantidad')
rechaza('cliente activo sin correo', "INSERT INTO cliente(nombre_cliente,telefono_cliente,direccion_cliente,clave_hash,fecha_autorizacion_datos,version_politica) VALUES ('X','1','d','h',NOW(),'1');", 'ck_cliente_activo')
rechaza('cliente sin autorización de datos', "INSERT INTO cliente(nombre_cliente,correo_cliente,telefono_cliente,direccion_cliente,clave_hash,version_politica) VALUES ('X','x@ejemplo.invalid','1','d','h','1');", 'fecha_autorizacion_datos')
rechaza('correo de cliente repetido', "INSERT INTO cliente(nombre_cliente,correo_cliente,telefono_cliente,direccion_cliente,clave_hash,fecha_autorizacion_datos,version_politica) VALUES ('X','cliente1@ejemplo.invalid','1','d','h',NOW(),'1');", 'uk_cliente_correo')
rechaza('borrar un producto con ventas', 'DELETE FROM presentacion WHERE id=(SELECT presentacion_id FROM detalle_pedido LIMIT 1);', 'foreign key')
rechaza('borrar un cliente con pedidos', 'DELETE FROM cliente WHERE id=(SELECT cliente_id FROM pedido WHERE cliente_id IS NOT NULL LIMIT 1);', 'foreign key')
rechaza('subcategoría de otro tipo', "INSERT INTO categoria(codigo_categoria,nombre_categoria,tipo_categoria,categoria_padre_id) VALUES ('x','X','variedad',1);", 'mismo tipo')
rechaza('categoría dentro de sí misma', """
INSERT INTO categoria(codigo_categoria,nombre_categoria,tipo_categoria,categoria_padre_id) VALUES ('hija','Hija','medicamento',1);
UPDATE categoria SET categoria_padre_id=LAST_INSERT_ID() WHERE id=1;""", 'dentro de sí misma')
rechaza('aprobar un plan con el precio alterado', """
INSERT INTO solicitud_suscripcion(cliente_id,plan_id,metodo_pago_id,origen_solicitud,monto_pagado) VALUES (1,4,1,'web',100);
CALL aprobar_solicitud(LAST_INSERT_ID(),2,@s);""", 'no coincide')
rechaza('aprobar dos veces la misma solicitud', "CALL aprobar_solicitud((SELECT id FROM solicitud_suscripcion WHERE estado_solicitud='aprobado' LIMIT 1),2,@s);", 'ya fue revisada')
rechaza('anular dos veces un pedido', "CALL anular_pedido((SELECT id FROM pedido WHERE estado_pedido='cancelado' LIMIT 1),2,'x');", 'ya está anulado')
rechaza('anular un pedido con devoluciones', "CALL anular_pedido((SELECT pedido_id FROM devolucion LIMIT 1),2,'x');", 'tiene devoluciones')
rechaza('eliminar una cuenta con pedidos en curso', "CALL anonimizar_cliente((SELECT cliente_id FROM pedido WHERE estado_pedido IN ('pendiente','en_proceso') LIMIT 1));", 'en curso')
rechaza('eliminar una cuenta con un pago por revisar', """
INSERT INTO cliente(nombre_cliente,correo_cliente,telefono_cliente,direccion_cliente,clave_hash,fecha_autorizacion_datos,version_politica) VALUES ('P','p@ejemplo.invalid','1','d','h',NOW(),'1'); SET @c=LAST_INSERT_ID();
INSERT INTO solicitud_suscripcion(cliente_id,plan_id,metodo_pago_id,origen_solicitud,monto_pagado) VALUES (@c,1,1,'web',4900);
CALL anonimizar_cliente(@c);""", 'por revisar')

print('\n5. Las operaciones hacen lo correcto (se revierten al terminar)')
PREP = """
INSERT INTO producto(categoria_id,nombre_producto) VALUES (1,'Producto T'); SET @pr=LAST_INSERT_ID();
INSERT INTO presentacion(producto_id,tipo_presentacion,precio_venta,stock_minimo) VALUES (@pr,'Unidad',1000,6); SET @p=LAST_INSERT_ID();
INSERT INTO lote(presentacion_id,numero_lote,fecha_vencimiento,costo_unitario) VALUES (@p,'TARDE',CURDATE()+INTERVAL 300 DAY,500); SET @tarde=LAST_INSERT_ID();
INSERT INTO lote(presentacion_id,numero_lote,fecha_vencimiento,costo_unitario) VALUES (@p,'PRONTO',CURDATE()+INTERVAL 10 DAY,500); SET @pronto=LAST_INSERT_ID();
INSERT INTO recepcion_mercancia(proveedor_id,usuario_id,numero_factura,total_factura) VALUES (1,7,'T',1000); SET @rec=LAST_INSERT_ID();
INSERT INTO detalle_recepcion(recepcion_id,lote_id,cantidad_recibida,costo_unitario) VALUES (@rec,@tarde,5,500);
INSERT INTO movimiento_inventario(lote_id,usuario_id,tipo_movimiento,cantidad_movida,detalle_recepcion_id) VALUES (@tarde,7,'entrada',5,LAST_INSERT_ID());
INSERT INTO detalle_recepcion(recepcion_id,lote_id,cantidad_recibida,costo_unitario) VALUES (@rec,@pronto,3,500);
INSERT INTO movimiento_inventario(lote_id,usuario_id,tipo_movimiento,cantidad_movida,detalle_recepcion_id) VALUES (@pronto,7,'entrada',3,LAST_INSERT_ID());
INSERT INTO pedido(numero_pedido,canal_venta,vendedor_id,modalidad_entrega,subtotal_pedido,costo_domicilio,descuento_aplicado) VALUES ('T-V','pos',4,'tienda',4000,500,400); SET @ped=LAST_INSERT_ID();
INSERT INTO detalle_pedido(pedido_id,presentacion_id,nombre_producto,cantidad_vendida,precio_original,precio_unitario) VALUES (@ped,@p,'Producto T',4,1000,900); SET @det=LAST_INSERT_ID();
"""
resultado('el total y el subtotal se calculan solos', PREP + 'SELECT CONCAT(total_pedido,"/",(SELECT subtotal_linea FROM detalle_pedido WHERE id=@det)) FROM pedido WHERE id=@ped;', '4100.00/3600.00')
resultado('la venta descuenta primero el lote que vence antes', PREP + "CALL descontar_stock_fefo(@det,4,NOW()); SELECT CONCAT((SELECT cantidad_actual FROM lote WHERE id=@pronto),'-',(SELECT cantidad_actual FROM lote WHERE id=@tarde));", '0-4')
rechaza('una venta sin existencia suficiente no descuenta nada', PREP + "UPDATE detalle_pedido SET cantidad_vendida=99 WHERE id=@det; CALL descontar_stock_fefo(@det,4,NOW());", 'existencia suficiente')
resultado('la existencia baja del mínimo y aparece la alerta', PREP + "CALL descontar_stock_fefo(@det,4,NOW()); SELECT COUNT(*) FROM notificacion WHERE presentacion_id=@p AND tipo_alerta='inventario_bajo' AND alerta_atendida=FALSE;", '1')
resultado('al reponer, la alerta se cierra sola', PREP + """CALL descontar_stock_fefo(@det,4,NOW());
INSERT INTO detalle_recepcion(recepcion_id,lote_id,cantidad_recibida,costo_unitario) VALUES (@rec,@tarde,50,500);
INSERT INTO movimiento_inventario(lote_id,usuario_id,tipo_movimiento,cantidad_movida,detalle_recepcion_id) VALUES (@tarde,7,'entrada',50,LAST_INSERT_ID());
SELECT COUNT(*) FROM notificacion WHERE presentacion_id=@p AND tipo_alerta='inventario_bajo' AND alerta_atendida=FALSE;""", '0')
resultado('anular devuelve las unidades y reembolsa', PREP + """CALL descontar_stock_fefo(@det,4,NOW());
INSERT INTO pago_pedido(pedido_id,metodo_pago_id,monto_pago,estado_pago) VALUES (@ped,5,4100,'confirmado');
CALL anular_pedido(@ped,2,'Prueba');
SELECT CONCAT((SELECT SUM(cantidad_actual) FROM lote WHERE presentacion_id=@p),'-',(SELECT estado_pago FROM pago_pedido WHERE pedido_id=@ped),'-',(SELECT estado_pedido FROM pedido WHERE id=@ped));""", '8-reembolsado-cancelado')
resultado('renovar conserva los días que faltaban', """
INSERT INTO cliente(nombre_cliente,correo_cliente,telefono_cliente,direccion_cliente,clave_hash,fecha_autorizacion_datos,version_politica) VALUES ('R','r@ejemplo.invalid','1','d','h',NOW(),'1'); SET @c=LAST_INSERT_ID();
INSERT INTO solicitud_suscripcion(cliente_id,plan_id,metodo_pago_id,origen_solicitud,monto_pagado) VALUES (@c,1,1,'web',(SELECT precio_mensual FROM plan_suscripcion WHERE id=1)); CALL aprobar_solicitud(LAST_INSERT_ID(),2,@a);
UPDATE suscripcion SET fecha_vencimiento=NOW()+INTERVAL 10 DAY WHERE id=@a;
INSERT INTO solicitud_suscripcion(cliente_id,plan_id,metodo_pago_id,origen_solicitud,monto_pagado) VALUES (@c,2,1,'web',(SELECT precio_mensual FROM plan_suscripcion WHERE id=2)); CALL aprobar_solicitud(LAST_INSERT_ID(),2,@b);
SELECT CONCAT((SELECT estado_suscripcion FROM suscripcion WHERE id=@a),'-',(SELECT estado_suscripcion FROM suscripcion WHERE id=@b),'-',(SELECT DATEDIFF(fecha_vencimiento,CURDATE()) FROM suscripcion WHERE id=@b),'-',(SELECT descuento_porcentaje FROM suscripcion WHERE id=@b));""", 'reemplazada-activa-40-5')
resultado('eliminar una cuenta borra sus datos y conserva sus ventas', """
SET @c=(SELECT p.cliente_id FROM pedido p WHERE p.cliente_id IS NOT NULL AND p.modalidad_entrega='domicilio' AND NOT EXISTS (SELECT 1 FROM pedido x WHERE x.cliente_id=p.cliente_id AND x.estado_pedido IN ('pendiente','en_proceso')) AND NOT EXISTS (SELECT 1 FROM solicitud_suscripcion s WHERE s.cliente_id=p.cliente_id AND s.estado_solicitud='pendiente') LIMIT 1);
SET @n=(SELECT COUNT(*) FROM pedido WHERE cliente_id=@c);
CALL anonimizar_cliente(@c);
SELECT CONCAT((SELECT CONCAT(nombre_cliente,'|',IFNULL(correo_cliente,'sin-correo'),'|',IFNULL(telefono_cliente,'sin-tel'),'|',IFNULL(clave_hash,'sin-clave')) FROM cliente WHERE id=@c),'|',(SELECT COUNT(*) FROM pedido WHERE cliente_id=@c)=@n,'|',(SELECT COUNT(*) FROM favorito WHERE cliente_id=@c),'|',(SELECT COUNT(*) FROM pedido WHERE cliente_id=@c AND direccion_entrega LIKE 'Carrera%'));""", 'Cuenta eliminada|sin-correo|sin-tel|sin-clave|1|0|0')
resultado('las tareas diarias avisan una sola vez y vencen planes', PREP + """CALL tareas_diarias(); CALL tareas_diarias();
SET @v=(SELECT id FROM suscripcion WHERE estado_suscripcion='activa' LIMIT 1);
UPDATE suscripcion SET fecha_vencimiento=NOW()-INTERVAL 1 DAY, fecha_inicio=NOW()-INTERVAL 40 DAY WHERE id=@v;
CALL tareas_diarias();
SELECT CONCAT((SELECT COUNT(*) FROM notificacion WHERE lote_id=@pronto AND tipo_alerta='proximo_vencer'),'-',(SELECT estado_suscripcion FROM suscripcion WHERE id=@v));""", '1-vencida')
resultado('el stock vendible no cuenta lotes vencidos', PREP + "INSERT INTO lote(presentacion_id,numero_lote,fecha_vencimiento,costo_unitario) VALUES (@p,'V',CURDATE()-INTERVAL 1 DAY,1); INSERT INTO recepcion_mercancia(proveedor_id,usuario_id,numero_factura,total_factura) VALUES (1,7,'U',1); SET @l=(SELECT id FROM lote WHERE numero_lote='V' AND presentacion_id=@p); INSERT INTO detalle_recepcion(recepcion_id,lote_id,cantidad_recibida,costo_unitario) VALUES (LAST_INSERT_ID(),@l,4,1); INSERT INTO movimiento_inventario(lote_id,usuario_id,tipo_movimiento,cantidad_movida,detalle_recepcion_id) VALUES (@l,7,'entrada',4,LAST_INSERT_ID()); SELECT CONCAT(stock_total,'-',stock_vendible) FROM vista_stock WHERE presentacion_id=@p;", '12-8')
resultado('las vistas de reportes responden', "SELECT CONCAT((SELECT COUNT(*)>0 FROM vista_stock),(SELECT COUNT(*)>0 FROM vista_ventas),(SELECT COUNT(*)>0 FROM vista_historial_inventario),(SELECT COUNT(*)>0 FROM vista_proximos_vencer));", '1111')

print('\n6. Registros por tabla (con la operación simulada)')
total = 0
for tabla, n in contar_registros():
    total += n
    print(f'  {tabla:32}{n:>7}')
print(f'  {"TOTAL":32}{total:>7}')

correr(f'DROP DATABASE IF EXISTS {BASE};', 'mysql')
print('  OK     base temporal borrada')
print(f'\n{"TODO CORRECTO" if not fallos else str(fallos) + " COMPROBACIONES FALLARON"}')
sys.exit(1 if fallos else 0)
