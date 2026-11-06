"""Genera los scripts de datos de la base de datos.

  sql/02-datos-iniciales.sql  Datos reales tomados del proyecto: permisos, roles, planes y catálogo.

datos_prueba() arma, solo en memoria, un año de operación simulada. Lo usa probar_bd.py en una
base temporal (drogueria_verificacion) que borra al terminar; nunca se guarda ni llega a la base real.

Uso: python generar_datos.py
El resultado es siempre el mismo porque el azar usa una semilla fija.
"""
import datetime as dt
import pathlib
import random
import re

RAIZ = pathlib.Path(__file__).resolve().parents[3]
SQL = RAIZ / 'docs/base-de-datos/sql'
JAVA = RAIZ / 'src/main/java/com/example/DrogueriaLosTresHermanos'
HOY = dt.datetime(2026, 11, 4, 8, 0)
DIAS = 365


def q(v):
    """Convierte un valor de Python en un literal SQL."""
    if v is None:
        return 'NULL'
    if isinstance(v, bool):
        return 'TRUE' if v else 'FALSE'
    if isinstance(v, int):
        return str(v)
    if isinstance(v, float):
        return f'{v:.2f}'
    if isinstance(v, dt.datetime):
        return f"'{v:%Y-%m-%d %H:%M:%S}'"
    if isinstance(v, dt.date):
        return f"'{v:%Y-%m-%d}'"
    return "'" + str(v).replace('\\', '\\\\').replace("'", "''") + "'"


def ins(tabla, **c):
    return f"INSERT INTO {tabla} ({', '.join(c)}) VALUES ({', '.join(q(v) for v in c.values())});"


# ---------------------------------------------------------------------
# Lectura de los datos reales del proyecto
# ---------------------------------------------------------------------

def leer_permisos():
    t = (JAVA / 'modelo/Permiso.java').read_text('utf-8')
    return re.findall(r'^\s+([A-Z_]+)\("([^"]+)", "([^"]+)", "([^"]+)", (true|false)\)', t, re.M)


def leer_roles(permisos):
    t = (JAVA / 'modelo/RolSistema.java').read_text('utf-8')
    roles = []
    for m in re.finditer(r'^\s+([A-Z_]+)\("([^"]+)", "([^"]+)",\s*EnumSet\.(allOf\(Permiso\.class\)|of\(([^)]*)\))\)', t, re.M):
        lista = [p[0] for p in permisos] if m[4].startswith('allOf') else re.findall(r'[A-Z_]+', m[5])
        roles.append((m[1].lower(), m[2], m[3], lista))
    return roles


def leer_planes():
    t = (JAVA / 'controlador/PaginaControlador.java').read_text('utf-8')
    patron = r'new PlanSuscripcion\((\d+)L, "([^"]+)",\s*"([^"]+)",\s*(\d+), (\d+), (true|false), (true|false), (true|false), (true|false)\)'
    return re.findall(patron, t)


def leer_catalogo():
    """Catálogo verificado contra el INVIMA y las tiendas (datos/catalogo-verificado.csv)."""
    import csv
    filas = list(csv.DictReader(open(RAIZ / 'docs/base-de-datos/datos/catalogo-verificado.csv', encoding='utf-8')))
    cats, por_codigo = [], {}
    for f in filas:
        if f['categoria_codigo'] not in por_codigo:
            por_codigo[f['categoria_codigo']] = len(cats) + 1
            cats.append((len(cats) + 1, f['categoria_codigo'], f['categoria'], f['tipo']))
    prods, vistos, pres = [], set(), []
    for f in filas:
        pid = int(f['producto_id'])
        if pid not in vistos:
            vistos.add(pid)
            prods.append(dict(id=pid, categoria_id=por_codigo[f['categoria_codigo']], nombre_producto=f['nombre'],
                              principio_activo=f['principio_activo'] or None, requiere_formula=f['requiere_formula'] == '1',
                              producto_activo=f['activo'] == '1'))
        conc = f['concentracion'] if f['concentracion'] not in ('', 'No aplica') else None
        pres.append(dict(id=int(f['presentacion_id']), producto_id=pid, tipo=f['tipo_presentacion'], concentracion=conc,
                         marca=f['marca'] or None, registro=f['registro_sanitario'] or None, precio=float(f['precio']),
                         minimo=int(f['stock_minimo']), med=f['tipo'] == 'medicamento'))
    return cats, prods, pres


PERMISOS = leer_permisos()
ROLES = leer_roles(PERMISOS)
PLANES = leer_planes()
CATS, PRODS, PRES = leer_catalogo()
assert len(PERMISOS) == 31 and len(ROLES) == 5 and len(PLANES) == 4 and len(PRES) >= 400, (len(PERMISOS), len(ROLES), len(PLANES), len(PRES))
METODOS = [('nequi', 'Nequi'), ('daviplata', 'Daviplata'), ('pse', 'PSE'), ('bancolombia', 'Bancolombia'), ('tienda', 'En tienda')]
ID_METODO = {m[0]: i for i, m in enumerate(METODOS, 1)}


def datos_iniciales():
    s = ['-- =====================================================================',
         '-- Datos iniciales: lo que necesita la página para funcionar en línea.',
         '-- Archivo generado por herramientas/generar_datos.py a partir del proyecto.',
         '-- No incluye usuarios: la cuenta principal la crea la aplicación con su clave.',
         '-- Las cuentas de pago quedan vacías hasta que el negocio confirme las reales.',
         '-- =====================================================================',
         'SET NAMES utf8mb4;', 'START TRANSACTION;', '']
    s.append('-- Permisos')
    for i, (_, cod, mod, des, op) in enumerate(PERMISOS, 1):
        s.append(ins('permiso', id=i, codigo_permiso=cod, modulo_permiso=mod, descripcion_permiso=des, permiso_operativo=op == 'true'))
    id_permiso = {p[0]: i for i, p in enumerate(PERMISOS, 1)}
    s.append('\n-- Roles y sus permisos')
    for i, (cod, nom, des, lista) in enumerate(ROLES, 1):
        s.append(ins('rol', id=i, codigo_rol=cod, nombre_rol=nom, descripcion_rol=des, rol_protegido=cod == 'propietario'))
        for x in lista:
            s.append(ins('rol_permiso', rol_id=i, permiso_id=id_permiso[x]))
    s.append('\n-- Métodos de pago')
    for i, (cod, nom) in enumerate(METODOS, 1):
        s.append(ins('metodo_pago', id=i, codigo_metodo_pago=cod, nombre_metodo_pago=nom))
    s.append('\n-- Planes de suscripción')
    for pid, nom, des, pre, dto, a, b, c, d in PLANES:
        s.append(ins('plan_suscripcion', id=int(pid), nombre_plan=nom, descripcion_plan=des, precio_mensual=float(pre),
                     descuento_porcentaje=int(dto), duracion_dias=30, domicilio_prioritario=a == 'true',
                     ofertas_anticipadas=b == 'true', soporte_whatsapp=c == 'true', acceso_exclusivo=d == 'true'))
    s.append('\n-- Configuración')
    s.append(ins('configuracion', clave_configuracion='dias_alerta_vencimiento', valor_configuracion='90', descripcion_configuracion='Días de anticipación de la alerta de vencimiento'))
    s.append(ins('configuracion', clave_configuracion='costo_domicilio', valor_configuracion='0', descripcion_configuracion='Valor del domicilio en pesos; pendiente de definir por el negocio'))
    s.append(ins('configuracion', clave_configuracion='version_politica_datos', valor_configuracion='1.0', descripcion_configuracion='Versión vigente de la política de datos personales'))
    # Cuentas para recibir pagos de planes. Vacías hasta que la droguería escriba las reales:
    # la página solo ofrece los medios que tengan valor.
    for clave, desc in [('pago_nequi', 'Número Nequi que recibe los pagos de planes'),
                        ('pago_daviplata', 'Número Daviplata que recibe los pagos de planes'),
                        ('pago_bancolombia', 'Tipo y número de cuenta Bancolombia que recibe los pagos'),
                        ('pago_pse', 'Enlace o convenio PSE de la droguería'),
                        ('pago_titular', 'Nombre del titular de las cuentas de pago')]:
        s.append(ins('configuracion', clave_configuracion=clave, valor_configuracion='', descripcion_configuracion=desc))
    s.append('\n-- Categorías')
    for i, cod, nom, tipo in CATS:
        s.append(ins('categoria', id=i, codigo_categoria=cod, nombre_categoria=nom, tipo_categoria=tipo))
    s.append('\n-- Productos')
    for p in PRODS:
        s.append(ins('producto', **p))
    s.append('\n-- Presentaciones')
    for x in PRES:
        s.append(ins('presentacion', id=x['id'], producto_id=x['producto_id'], tipo_presentacion=x['tipo'], concentracion=x['concentracion'],
                     marca=x['marca'], registro_sanitario=x['registro'], precio_venta=x['precio'], stock_minimo=x['minimo']))
    s += ['', 'COMMIT;', '']
    return '\n'.join(s)


# ---------------------------------------------------------------------
# Datos de prueba: simulan un año de operación en orden cronológico
# ---------------------------------------------------------------------

def datos_prueba(base='drogueria_verificacion'):
    r = random.Random(2026)
    s = ['-- =====================================================================',
         '-- DATOS DE PRUEBA INVENTADOS. NO deben llegar a la página en línea.',
         '-- Generado en memoria por herramientas/generar_datos.py para probar_bd.py.',
         f'-- Se detiene si la base activa no se llama {base}.',
         '-- =====================================================================',
         'SET NAMES utf8mb4;', '',
         'DROP PROCEDURE IF EXISTS verificar_base_de_pruebas;',
         'DELIMITER //',
         'CREATE PROCEDURE verificar_base_de_pruebas()',
         'BEGIN',
         f"  IF DATABASE() <> '{base}' THEN",
         f"    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Estos datos de prueba solo se cargan en {base}';",
         '  END IF;',
         'END//',
         'DELIMITER ;',
         'CALL verificar_base_de_pruebas();',
         'DROP PROCEDURE verificar_base_de_pruebas;',
         '', 'START TRANSACTION;', '']
    e = s.append

    # Equipo: las claves no son hashes válidos, así que nadie puede entrar con estas cuentas
    roles_equipo = [('propietario', 'Marta Hernández'), ('administrador', 'Carlos Pérez'), ('regente', 'Lucía Gómez'),
                    ('cajero', 'Andrés Díaz'), ('cajero', 'Paola Ruiz'), ('cajero', 'Jorge Mena'),
                    ('auxiliar', 'Sandra Vega'), ('auxiliar', 'Iván Torres')]
    id_rol = {x[0]: i for i, x in enumerate(ROLES, 1)}
    inicio = HOY - dt.timedelta(days=DIAS)
    for i, (rol, nom) in enumerate(roles_equipo, 1):
        e(ins('usuario', id=i, rol_id=id_rol[rol], nombre_usuario=nom, correo_usuario=f'empleado{i}@ejemplo.invalid',
              clave_hash='!cuenta-de-prueba-sin-acceso', fecha_creacion=inicio))
    CAJEROS, AUX, ADMIN = [4, 5, 6], [7, 8], 2

    for i in range(1, 26):
        e(ins('proveedor', id=i, nit_proveedor=f'900{100000 + i}-{i % 10}', nombre_proveedor=f'Distribuidora de prueba {i} S.A.S.',
              contacto_proveedor=f'Contacto {i}', telefono_proveedor=f'30000{i:05d}', correo_proveedor=f'proveedor{i}@ejemplo.invalid',
              direccion_proveedor=f'Calle {i} # {i + 10}-20, Cartagena'))

    nombres = 'Ana Luis María José Carmen Pedro Laura Diego Sofía Juan Valentina Mateo Camila Daniel Isabella Samuel'.split()
    apellidos = 'García Rodríguez Martínez López Hernández Pérez Gómez Díaz Torres Ramírez Flores Rivera Morales Ortiz Castro Rojas'.split()
    NCLI = 320
    alta = {}
    for i in range(1, NCLI + 1):
        f = inicio + dt.timedelta(days=r.randint(0, DIAS - 40), hours=r.randint(8, 19))
        alta[i] = f
        e(ins('cliente', id=i, nombre_cliente=f'{r.choice(nombres)} {r.choice(apellidos)} {r.choice(apellidos)}', correo_cliente=f'cliente{i}@ejemplo.invalid',
              telefono_cliente=f'31{i:08d}', direccion_cliente=f'Carrera {r.randint(1, 60)} # {r.randint(1, 80)}-{r.randint(1, 99)}, Arjona',
              clave_hash='!cuenta-de-prueba-sin-acceso', creado_por_id=ADMIN if i % 15 == 0 else None,
              fecha_autorizacion_datos=f, version_politica='1.0', fecha_registro=f))
    pares = set()
    while len(pares) < 400:
        pares.add((r.randint(1, NCLI), r.choice(PRODS)['id']))
    for c, p in sorted(pares):
        e(ins('favorito', cliente_id=c, producto_id=p, fecha_favorito=alta[c] + dt.timedelta(days=1)))

    for x in PRES:
        x['min'] = x['minimo']
        e(f"UPDATE presentacion SET stock_maximo = {x['min'] * 20}, codigo_barras = '77000{x['id']:08d}' WHERE id = {x['id']};")

    nombre_prod = {p['id']: p['nombre_producto'] for p in PRODS}
    tipos = {}
    for x in PRES:
        tipos[x['id']] = (nombre_prod[x['producto_id']] + ' ' + x['tipo'] + (' ' + x['concentracion'] if x['concentracion'] else ''))

    st = dict(lote=0, rec=0, drec=0, orden=0, dorden=0, ped=0, det=0, pago=0, ses=0, mcaja=0, sol=0, sus=0,
              dev=0, ddev=0, dprov=0, ddprov=0, conteo=0, dconteo=0, arq=0, aud=0)
    lotes = {x['id']: [] for x in PRES}   # presentacion -> [dict(id, venc, qty)]

    def nuevo(k):
        st[k] += 1
        return st[k]

    def mov(lote, usuario, tipo, cant, fecha, **ref):
        e(ins('movimiento_inventario', lote_id=lote, usuario_id=usuario, tipo_movimiento=tipo, cantidad_movida=cant, fecha_movimiento=fecha, **ref))

    def recibir(cuando, presentaciones, grande=False):
        prov = r.randint(1, 25)
        usuario = r.choice(AUX)
        lineas = []
        orden = None
        if r.random() < .4 and not grande:
            orden = nuevo('orden')
            e(ins('orden_compra', id=orden, numero_orden=f'OC-{orden:05d}', proveedor_id=prov, usuario_id=usuario, estado_orden='recibida',
                  fecha_creacion=cuando - dt.timedelta(days=3), fecha_esperada=cuando.date()))
        for pid in presentaciones:
            x = PRES[pid - 1]
            cant = r.randint(150, 300) if grande else r.randint(60, 180)
            costo = round(x['precio'] * r.uniform(.55, .75))
            lote = nuevo('lote')
            # La mayoría vence lejos; una parte vence pronto para ejercitar las alertas
            dias = r.choice([r.randint(45, 150), r.randint(200, 720), r.randint(200, 720), r.randint(200, 720)])
            venc = (cuando + dt.timedelta(days=dias)).date() if x['med'] else None
            e(ins('lote', id=lote, presentacion_id=pid, numero_lote=f'L{cuando:%y%m}-{lote:05d}', fecha_vencimiento=venc,
                  costo_unitario=float(costo), fecha_ingreso=cuando))
            lotes[pid].append(dict(id=lote, venc=venc, qty=0))
            lineas.append((pid, lote, cant, costo))
        rec = nuevo('rec')
        e(ins('recepcion_mercancia', id=rec, proveedor_id=prov, orden_compra_id=orden, usuario_id=usuario, numero_factura=f'FAC-{rec:06d}',
              total_factura=float(sum(c * k for _, _, c, k in lineas)), fecha_recepcion=cuando))
        for pid, lote, cant, costo in lineas:
            dord = None
            if orden:
                dord = nuevo('dorden')
                e(ins('detalle_orden_compra', id=dord, orden_compra_id=orden, presentacion_id=pid, cantidad_pedida=cant, costo_unitario=float(costo)))
            d = nuevo('drec')
            e(ins('detalle_recepcion', id=d, recepcion_id=rec, detalle_orden_id=dord, lote_id=lote, cantidad_recibida=cant, costo_unitario=float(costo)))
            mov(lote, usuario, 'entrada', cant, cuando, detalle_recepcion_id=d)
            next(l for l in lotes[pid] if l['id'] == lote)['qty'] = cant

    def vendible(pid, dia):
        v = [l for l in lotes[pid] if l['qty'] > 0 and (l['venc'] is None or l['venc'] >= dia)]
        return sorted(v, key=lambda l: (l['venc'] is None, l['venc'] or dt.date.max, l['id']))

    def caja(ses, usuario, metodo, tipo, categoria, monto, fecha, **ref):
        e(ins('movimiento_caja', id=nuevo('mcaja'), sesion_caja_id=ses, usuario_id=usuario, metodo_pago_id=metodo, tipo_movimiento_caja=tipo,
              categoria_movimiento_caja=categoria, monto_movimiento=float(monto), fecha_movimiento=fecha, **ref))

    # Primer abastecimiento: todo el catálogo tiene al menos un lote
    recibir(inicio, [x['id'] for x in PRES], grande=True)

    activa = {}      # cliente -> dict(id, vence, pct)
    terminar = HOY

    def vencer(ahora):
        for c, a in list(activa.items()):
            if a['vence'] < ahora:
                e(f"UPDATE suscripcion SET estado_suscripcion = 'vencida' WHERE id = {a['id']};")
                del activa[c]

    for n in range(DIAS):
        dia = inicio + dt.timedelta(days=n)
        abre = dia.replace(hour=8, minute=0)
        cajero = r.choice(CAJEROS)
        ses = nuevo('ses')
        base = 150000.0
        efectivo = base
        e(ins('sesion_caja', id=ses, abierta_por_id=cajero, monto_inicial=base, fecha_apertura=abre))
        vencer(abre)

        if n % 3 == 2:
            recibir(dia.replace(hour=9, minute=r.randint(0, 59)), r.sample([x['id'] for x in PRES], r.randint(3, 7)))

        # Solicitudes de suscripción
        if r.random() < .45:
            cuando = dia.replace(hour=r.randint(9, 18), minute=r.randint(0, 59))
            cli = r.randint(1, NCLI)
            if alta[cli] < cuando:
                pl = r.choice(PLANES)
                origen = r.choice(['web', 'web', 'tienda'])
                metodo = 'tienda' if origen == 'tienda' else r.choice(['nequi', 'daviplata', 'pse', 'bancolombia'])
                sol = nuevo('sol')
                reciente = n > DIAS - 4
                estado = 'aprobado' if origen == 'tienda' else ('pendiente' if reciente else r.choices(['aprobado', 'rechazado'], [85, 15])[0])
                monto = float(pl[3])
                e(ins('solicitud_suscripcion', id=sol, cliente_id=cli, plan_id=int(pl[0]), metodo_pago_id=ID_METODO[metodo], origen_solicitud=origen,
                      monto_pagado=monto, referencia_pago=None if origen == 'tienda' else f'REF{sol:08d}', fecha_pago=cuando.date(),
                      estado_solicitud=estado, motivo_rechazo='El pago no se pudo verificar' if estado == 'rechazado' else None,
                      revisado_por_id=None if estado == 'pendiente' else ADMIN, fecha_revision=None if estado == 'pendiente' else cuando,
                      fecha_solicitud=cuando))
                if estado == 'aprobado':
                    fin = cuando
                    if cli in activa:
                        e(f"UPDATE suscripcion SET estado_suscripcion = 'reemplazada' WHERE id = {activa[cli]['id']};")
                        fin = activa[cli]['vence']
                    sus = nuevo('sus')
                    vence = fin + dt.timedelta(days=30)
                    e(ins('suscripcion', id=sus, cliente_id=cli, plan_id=int(pl[0]), solicitud_id=sol, descuento_porcentaje=int(pl[4]),
                          fecha_inicio=cuando, fecha_vencimiento=vence))
                    activa[cli] = dict(id=sus, vence=vence, pct=int(pl[4]))
                    if origen == 'tienda':
                        caja(ses, cajero, ID_METODO['tienda'], 'ingreso', 'suscripcion', monto, cuando, suscripcion_id=sus)
                        efectivo += monto
                    if r.random() < .06:
                        e(f"UPDATE suscripcion SET estado_suscripcion = 'cancelada', fecha_cancelacion = {q(cuando + dt.timedelta(days=5))} WHERE id = {sus};")
                        del activa[cli]

        # Pedidos y ventas del día
        for hora in sorted(r.sample(range(8 * 60 + 5, 20 * 60), r.randint(3, 6))):
            cuando = dia.replace(hour=hora // 60, minute=hora % 60)
            canal = r.choices(['pos', 'web'], [65, 35])[0]
            cli = None
            if canal == 'web' or r.random() < .3:
                cli = r.randint(1, NCLI)
                if alta[cli] > cuando:
                    if canal == 'web':
                        continue
                    cli = None
            a = activa.get(cli)
            pct = a['pct'] if a and a['vence'] >= cuando else 0
            lineas = []
            for pid in r.sample([x['id'] for x in PRES], r.randint(1, 4)):
                cant = r.randint(1, 3)
                if sum(l['qty'] for l in vendible(pid, cuando.date())) >= cant:
                    lineas.append((pid, cant))
            if not lineas:
                continue
            ped = nuevo('ped')
            subtotal = descuento = 0
            filas = []
            for pid, cant in lineas:
                orig = PRES[pid - 1]['precio']
                unit = float(round(orig * (100 - pct) / 100))
                subtotal += cant * orig
                descuento += cant * (orig - unit)
                filas.append((pid, cant, orig, unit))
            modalidad = 'tienda' if canal == 'pos' else r.choice(['tienda', 'domicilio'])
            vieja = n < DIAS - 3
            estado = 'entregado' if canal == 'pos' or vieja else r.choice(['pendiente', 'en_proceso'])
            total = subtotal - descuento
            e(ins('pedido', id=ped, numero_pedido=f'LTH-{ped:06d}', numero_comprobante=f'CP-{ped:06d}' if estado == 'entregado' else None,
                  canal_venta=canal, cliente_id=cli, suscripcion_id=a['id'] if pct else None, vendedor_id=cajero if canal == 'pos' else None,
                  sesion_caja_id=ses if canal == 'pos' else None, modalidad_entrega=modalidad,
                  direccion_entrega=f'Carrera {r.randint(1, 60)} # {r.randint(1, 80)}-{r.randint(1, 99)}, Arjona' if modalidad == 'domicilio' else None,
                  subtotal_pedido=float(subtotal), descuento_porcentaje=pct, descuento_aplicado=float(descuento), estado_pedido=estado, fecha_creacion=cuando))
            salidas = []
            for pid, cant, orig, unit in filas:
                det = nuevo('det')
                e(ins('detalle_pedido', id=det, pedido_id=ped, presentacion_id=pid, nombre_producto=tipos[pid], cantidad_vendida=cant,
                      precio_original=orig, precio_unitario=unit))
                falta = cant
                for l in vendible(pid, cuando.date()):
                    toma = min(l['qty'], falta)
                    if toma == 0:
                        break
                    l['qty'] -= toma
                    falta -= toma
                    mov(l['id'], cajero if canal == 'pos' else None, 'venta', -toma, cuando, detalle_pedido_id=det)
                    salidas.append((l, toma, det, unit))
            metodo = r.choices(['tienda', 'nequi', 'daviplata'], [70, 20, 10])[0] if canal == 'pos' else r.choice(['nequi', 'daviplata', 'pse', 'bancolombia'])
            pago = nuevo('pago')
            e(ins('pago_pedido', id=pago, pedido_id=ped, metodo_pago_id=ID_METODO[metodo], monto_pago=float(total),
                  referencia_pago=None if metodo == 'tienda' else f'PAG{pago:08d}', estado_pago='confirmado' if estado == 'entregado' else 'pendiente', fecha_pago=cuando))
            if canal == 'pos':
                caja(ses, cajero, ID_METODO[metodo], 'ingreso', 'venta', total, cuando, pedido_id=ped)
                if metodo == 'tienda':
                    efectivo += total

            suerte = r.random()
            if suerte < .03 and vieja:
                # Anulación: cada unidad vuelve a su lote
                for l, toma, det, _ in salidas:
                    l['qty'] += toma
                    mov(l['id'], ADMIN, 'anulacion', toma, cuando + dt.timedelta(minutes=20), detalle_pedido_id=det, motivo_movimiento='Anulada a solicitud del cliente')
                e(f"UPDATE pedido SET estado_pedido = 'cancelado', anulado_por_id = {ADMIN}, motivo_anulacion = 'Anulada a solicitud del cliente', "
                  f"fecha_anulacion = {q(cuando + dt.timedelta(minutes=20))} WHERE id = {ped};")
                e(f"UPDATE pago_pedido SET estado_pago = 'reembolsado' WHERE id = {pago};")
                if canal == 'pos':
                    caja(ses, ADMIN, ID_METODO[metodo], 'egreso', 'devolucion', total, cuando + dt.timedelta(minutes=20), pedido_id=ped)
                    if metodo == 'tienda':
                        efectivo -= total
            elif suerte < .06 and vieja and salidas and estado == 'entregado':
                # Devolución parcial de una unidad
                l, toma, det, unit = salidas[0]
                dev = nuevo('dev')
                fecha = cuando + dt.timedelta(hours=1)
                e(ins('devolucion', id=dev, pedido_id=ped, usuario_id=ADMIN, motivo_devolucion='Producto no requerido', fecha_devolucion=fecha))
                reingresa = r.random() < .6
                dd = nuevo('ddev')
                e(ins('detalle_devolucion', id=dd, devolucion_id=dev, detalle_pedido_id=det, lote_id=l['id'] if reingresa else None,
                      cantidad_devuelta=1, monto_devuelto=unit, reingresa_inventario=reingresa))
                if reingresa:
                    l['qty'] += 1
                    mov(l['id'], ADMIN, 'devolucion_cliente', 1, fecha, detalle_devolucion_id=dd)
                caja(ses, ADMIN, ID_METODO['tienda'], 'egreso', 'devolucion', unit, fecha, pedido_id=ped)
                efectivo -= unit

        # Gastos
        if n % 7 == 3:
            valor = float(r.randint(10, 60) * 1000)
            if efectivo > valor:
                caja(ses, cajero, ID_METODO['tienda'], 'egreso', 'gasto', valor, dia.replace(hour=15), descripcion_movimiento='Gasto menor de la tienda')
                efectivo -= valor

        # Arqueo semanal
        if n % 7 == 0:
            e(ins('arqueo_caja', id=nuevo('arq'), sesion_caja_id=ses, usuario_id=ADMIN, monto_esperado=float(efectivo), monto_contado=float(efectivo),
                  fecha_arqueo=dia.replace(hour=19)))

        # Conteo físico mensual
        if n % 30 == 15:
            cont = nuevo('conteo')
            fin = dia.replace(hour=20, minute=30)
            e(ins('conteo_fisico', id=cont, usuario_id=r.choice(AUX), estado_conteo='cerrado', observacion_conteo='Conteo mensual',
                  fecha_inicio=dia.replace(hour=20), fecha_cierre=fin))
            con_stock = [l for ls in lotes.values() for l in ls if l['qty'] > 0]
            for l in r.sample(con_stock, min(15, len(con_stock))):
                dif = r.choice([0, 0, 0, 0, -1, 1])
                contada = max(0, l['qty'] + dif)
                dc = nuevo('dconteo')
                e(ins('detalle_conteo', id=dc, conteo_id=cont, lote_id=l['id'], cantidad_sistema=l['qty'], cantidad_contada=contada))
                if contada != l['qty']:
                    mov(l['id'], r.choice(AUX), 'conteo', contada - l['qty'], fin, detalle_conteo_id=dc, motivo_movimiento='Diferencia del conteo mensual')
                    l['qty'] = contada

        # Devolución al proveedor de lotes por vencer
        if n % 20 == 10:
            pronto = [l for ls in lotes.values() for l in ls if l['qty'] > 0 and l['venc'] and l['venc'] <= (dia + dt.timedelta(days=30)).date()]
            if pronto:
                l = r.choice(pronto)
                dp = nuevo('dprov')
                fecha = dia.replace(hour=20, minute=45)
                u = r.choice(AUX)
                e(ins('devolucion_proveedor', id=dp, proveedor_id=r.randint(1, 25), usuario_id=u, motivo_devolucion='Próximo a vencer', fecha_devolucion=fecha))
                dd = nuevo('ddprov')
                e(ins('detalle_devolucion_proveedor', id=dd, devolucion_proveedor_id=dp, lote_id=l['id'], cantidad_devuelta=l['qty'], costo_unitario=1000.0))
                mov(l['id'], u, 'devolucion_proveedor', -l['qty'], fecha, detalle_devolucion_proveedor_id=dd)
                l['qty'] = 0

        # Cierre del turno; el último día queda abierto
        if n < DIAS - 1:
            e(f"UPDATE sesion_caja SET estado_sesion_caja = 'cerrada', cerrada_por_id = {cajero}, monto_esperado = {efectivo:.2f}, monto_contado = {efectivo:.2f}, "
              f"fecha_cierre = {q(dia.replace(hour=21))} WHERE id = {ses};")

    # Excepciones de permisos y solicitudes de recuperación de contraseña
    id_perm = {p[1]: i for i, p in enumerate(PERMISOS, 1)}
    e(ins('usuario_permiso', usuario_id=4, permiso_id=id_perm['sales.cancel'], tipo_excepcion='concedido'))
    e(ins('usuario_permiso', usuario_id=5, permiso_id=id_perm['cash.operate'], tipo_excepcion='revocado'))
    e(ins('usuario_permiso', usuario_id=7, permiso_id=id_perm['inventory.adjust'], tipo_excepcion='concedido'))
    for i in range(1, 21):
        f = HOY - dt.timedelta(days=r.randint(1, 200))
        usado = f + dt.timedelta(minutes=10) if i % 3 else None
        e(ins('recuperacion_clave', cliente_id=r.randint(1, NCLI), token_hash=f'!token-de-prueba-{i}', fecha_expiracion=f + dt.timedelta(hours=1),
              fecha_uso=usado, fecha_creacion=f))

    # Auditoría
    acciones = ['Inicio de sesión', 'Producto modificado', 'Pago aprobado', 'Venta anulada', 'Permisos modificados', 'Ajuste de inventario']
    for _ in range(400):
        u = r.randint(1, 8)
        e(ins('auditoria', id=nuevo('aud'), usuario_id=u, actor_auditoria=f'empleado{u}@ejemplo.invalid', accion_auditoria=r.choice(acciones), detalle_auditoria='Evento de prueba',
              fecha_auditoria=inicio + dt.timedelta(days=r.randint(0, DIAS - 1), hours=r.randint(8, 20))))

    e('\nCALL tareas_diarias();')
    e('COMMIT;\n')
    return '\n'.join(s)


def catalogo_inicial():
    """El mismo catálogo en el archivo que usa la aplicación cuando no hay base de datos (pruebas y copia estática)."""
    def v(x):
        return str(x if x is not None else '').replace('\\', '\\\\').replace('\n', ' ')
    s = ['# Catálogo verificado. Archivo generado por docs/base-de-datos/herramientas/generar_datos.py; no se edita a mano.']
    for n, (_, cod, nom, tipo) in enumerate(CATS):
        s += [f'cat.{n}.id={v(cod)}', f'cat.{n}.nombre={v(nom)}', f'cat.{n}.tipo={v(tipo)}']
    codigo = {c[0]: c[1] for c in CATS}
    tipo_cat = {c[0]: c[3] for c in CATS}
    for n, p in enumerate(PRODS):
        b = f'p.{n}.'
        tipo = tipo_cat[p['categoria_id']]
        propias = [x for x in PRES if x['producto_id'] == p['id']]
        s += [f'{b}id={p["id"]}', f'{b}tipo={tipo}', f'{b}nombre={v(p["nombre_producto"])}', f'{b}descripcion=',
              f'{b}categoria={codigo[p["categoria_id"]]}', f'{b}principio={v(p["principio_activo"])}',
              f'{b}contenido={"" if tipo == "medicamento" else v(propias[0]["tipo"])}',
              f'{b}formula={str(p["requiere_formula"]).lower()}', f'{b}disponibilidad=sin-dato',
              f'{b}activo={str(p["producto_activo"]).lower()}', f'{b}foto=', f'{b}imagen=',
              f'{b}precio={0 if tipo == "medicamento" else int(propias[0]["precio"])}']
        if tipo == 'medicamento':
            for j, x in enumerate(propias):
                c = f'{b}pres.{j}.'
                s += [f'{c}id={x["id"]}', f'{c}tipo={v(x["tipo"])}', f'{c}conc={v(x["concentracion"] or "No aplica")}',
                      f'{c}marca={v(x["marca"] or "Por definir")}', f'{c}precio={int(x["precio"])}']
    return '\n'.join(s) + '\n'


if __name__ == '__main__':
    (SQL / '02-datos-iniciales.sql').write_text(datos_iniciales(), 'utf-8', newline='\n')
    (RAIZ / 'src/main/resources/catalogo-inicial.properties').write_text(catalogo_inicial(), 'utf-8', newline='\n')
    print(f'{len(PERMISOS)} permisos, {len(ROLES)} roles, {len(PLANES)} planes, {len(CATS)} categorías, {len(PRODS)} productos, {len(PRES)} presentaciones')
