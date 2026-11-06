"""Genera el diccionario de datos y los diagramas a partir de la base de datos real.

Como todo sale de MySQL, la documentación no puede contradecir al script.

Uso: python generar_documentacion.py [archivo de credenciales de MySQL]
Requiere la base drogueria cargada (solo lee su estructura) y, para las imágenes, PlantUML (plantuml.jar) y Java.
"""
import collections
import os
import pathlib
import re
import subprocess
import sys
import textwrap

MYSQL = os.environ.get('MYSQL_BIN', r'C:\Program Files\MySQL\MySQL Server 9.7\bin\mysql.exe')
JAVA = os.environ.get('JAVA_BIN', r'C:\Program Files\Java\jdk-27\bin\java.exe')
PLANTUML = os.environ.get('PLANTUML_JAR', r'C:\Users\ACER1\plantuml\plantuml.jar')
CRED = sys.argv[1] if len(sys.argv) > 1 else r'C:\Users\ACER1\mysql-drogueria\root97.cnf'
BASE = pathlib.Path(__file__).resolve().parents[1]
DIAGRAMAS = BASE / 'diagramas'
ESQUEMA = 'drogueria'

# Orden, nombre, color y tablas de cada módulo
MODULOS = [
    ('accesos', 'Accesos y auditoría', '#E4E9F7', 'Quién entra al panel, qué puede hacer y qué hizo.',
     ['rol', 'permiso', 'rol_permiso', 'usuario', 'usuario_permiso', 'auditoria', 'configuracion']),
    ('clientes', 'Clientes y suscripciones', '#FBEFD5', 'Cuentas de la tienda, compra de planes y su vigencia.',
     ['cliente', 'favorito', 'recuperacion_clave', 'plan_suscripcion', 'solicitud_suscripcion', 'suscripcion', 'metodo_pago']),
    ('catalogo', 'Catálogo', '#DFF3E4', 'Lo que se vende: categorías, productos y presentaciones.',
     ['categoria', 'producto', 'presentacion']),
    ('ventas', 'Pedidos y ventas', '#FBE3E4', 'Pedidos en línea y ventas de mostrador, con sus pagos y devoluciones.',
     ['pedido', 'detalle_pedido', 'pago_pedido', 'devolucion', 'detalle_devolucion']),
    ('inventario', 'Inventario', '#DDF1F6', 'Lotes, vencimientos, historial de entradas y salidas, conteos y alertas.',
     ['lote', 'movimiento_inventario', 'conteo_fisico', 'detalle_conteo', 'notificacion']),
    ('compras', 'Compras y proveedores', '#EDE4F5', 'Proveedores, órdenes de compra, entradas de mercancía y devoluciones.',
     ['proveedor', 'orden_compra', 'detalle_orden_compra', 'recepcion_mercancia', 'detalle_recepcion', 'devolucion_proveedor', 'detalle_devolucion_proveedor']),
    ('caja', 'Caja', '#F4F1DE', 'Turnos de caja, arqueos e ingresos y egresos de dinero.',
     ['sesion_caja', 'arqueo_caja', 'movimiento_caja']),
]
MODULO_DE = {t: m for m in MODULOS for t in m[4]}
ESTILO = """hide methods
hide stereotype
hide circle
skinparam shadowing false
skinparam roundCorner 6
skinparam defaultFontName Arial
skinparam dpi 110
skinparam ArrowColor #3A4A6B
skinparam ArrowThickness 1.1
skinparam nodesep 45
skinparam ranksep 55
skinparam class {
  BackgroundColor White
  BorderColor #10224D
  FontSize 13
  AttributeFontSize 12
}
skinparam package {
  BorderColor #8A94A8
  FontSize 16
  FontStyle bold
  BackgroundColor transparent
}"""


def consulta(sql):
    r = subprocess.run([MYSQL, f'--defaults-extra-file={CRED}', '--batch', '--skip-column-names', '--raw', '-D', 'information_schema'],
                       input=sql.encode('utf-8'), capture_output=True, timeout=120)
    if r.returncode:
        raise SystemExit(r.stderr.decode('utf-8', 'replace'))
    return [l.split('\t') for l in r.stdout.decode('utf-8').splitlines() if l]


def leer():
    t = {}
    for nombre, comentario in consulta(f"SELECT table_name, table_comment FROM tables WHERE table_schema='{ESQUEMA}' AND table_type='BASE TABLE'"):
        t[nombre] = dict(comentario=comentario, cols=[], unicos=[], fks=[], checks=[])
    assert set(t) == set(MODULO_DE), set(t) ^ set(MODULO_DE)
    for tb, col, tipo, nulo, defecto, extra, coment in consulta(
            f"SELECT table_name, column_name, column_type, is_nullable, IFNULL(column_default,'-'), extra, column_comment FROM columns "
            f"WHERE table_schema='{ESQUEMA}' ORDER BY table_name, ordinal_position"):
        if tb in t:
            t[tb]['cols'].append(dict(n=col, tipo=tipo, nulo=nulo == 'YES', defecto=defecto, extra=extra, c=coment))
    pk = collections.defaultdict(list)
    for tb, col in consulta(f"SELECT table_name, column_name FROM key_column_usage WHERE table_schema='{ESQUEMA}' AND constraint_name='PRIMARY' ORDER BY table_name, ordinal_position"):
        pk[tb].append(col)
    uq = collections.defaultdict(list)
    for tb, idx, col in consulta(f"SELECT table_name, index_name, column_name FROM statistics WHERE table_schema='{ESQUEMA}' AND non_unique=0 AND index_name<>'PRIMARY' ORDER BY table_name, index_name, seq_in_index"):
        uq[(tb, idx)].append(col)
    for (tb, idx), cols in uq.items():
        if tb in t:
            t[tb]['unicos'].append(cols)
    fk = collections.OrderedDict()
    for tb, nom, col, ref, refcol in consulta(f"SELECT table_name, constraint_name, column_name, referenced_table_name, referenced_column_name FROM key_column_usage "
                                              f"WHERE table_schema='{ESQUEMA}' AND referenced_table_name IS NOT NULL ORDER BY table_name, constraint_name, ordinal_position"):
        fk.setdefault((tb, nom), dict(ref=ref, cols=[], refcols=[]))
        fk[(tb, nom)]['cols'].append(col)
        fk[(tb, nom)]['refcols'].append(refcol)
    for (tb, _), v in fk.items():
        t[tb]['fks'].append(v)
    for tb, nom, cl in consulta(f"SELECT tc.table_name, cc.constraint_name, REPLACE(REPLACE(cc.check_clause,'\\n',' '),'\\\\','') FROM check_constraints cc "
                                f"JOIN table_constraints tc ON tc.constraint_schema=cc.constraint_schema AND tc.constraint_name=cc.constraint_name "
                                f"WHERE cc.constraint_schema='{ESQUEMA}' ORDER BY tc.table_name, cc.constraint_name"):
        t[tb]['checks'].append((nom, cl))
    for nombre, v in t.items():
        v['pk'] = pk[nombre]
        en_fk = {c for f in v['fks'] for c in f['cols']}
        en_uq = {c for u in v['unicos'] if len(u) == 1 for c in u}
        for c in v['cols']:
            c['pk'] = c['n'] in v['pk']
            c['fk'] = c['n'] in en_fk
            c['uq'] = c['n'] in en_uq
            c['gen'] = 'VIRTUAL GENERATED' in c['extra'] or 'STORED GENERATED' in c['extra']
    return t


def limpiar(texto):
    """Quita las referencias a requisitos, que no le dicen nada a quien llega de nuevo."""
    return re.sub(r'\s*\((?:RF\d+(?:, )?)+\)', '', texto).strip()


def tipo_sencillo(c):
    """Explica el tipo de dato sin términos técnicos."""
    tipo = c['tipo']
    if tipo.startswith('tinyint(1)'):
        return 'sí o no'
    if tipo.startswith('enum'):
        return 'una de estas opciones: ' + tipo[5:-1].replace("','", ', ').strip("'")
    m = re.match(r'varchar\((\d+)\)', tipo)
    if m:
        return f'texto, hasta {m[1]} caracteres'
    if tipo == 'text':
        return 'texto largo'
    if tipo.startswith('decimal'):
        return 'valor en pesos'
    if tipo.startswith('datetime'):
        return 'fecha y hora'
    if tipo.startswith('date'):
        return 'fecha'
    return 'número entero'


def tipo_largo(c):
    tipo = c['tipo']
    if tipo.startswith('tinyint(1)'):
        return 'BOOLEAN'
    if tipo.startswith('enum'):
        return 'ENUM: ' + tipo[5:-1].replace("','", ', ').strip("'")
    return tipo.upper()


def titulo_tabla(nombre, v):
    """Nombre de la tabla y, debajo, qué guarda en palabras sencillas."""
    return f'**{nombre}**\\n' + '\\n'.join(textwrap.wrap(limpiar(v['comentario']), 42))


def resaltar(texto):
    return texto.replace('POR REVISAR', '<color:#B00020>**POR REVISAR**</color>')


def entidad(nombre, v, color):
    filas = []
    for k, c in enumerate(v['cols']):
        marcas = []
        if c['pk']:
            marcas.append('clave primaria')
        for f in v['fks']:
            # En una clave compuesta, el enlace se marca en su primer campo
            if c['n'] == f['cols'][0]:
                marcas.append(f"clave foránea → {f['ref']}")
        if c['uq']:
            marcas.append('no se repite')
        if c['gen']:
            marcas.append('la calcula MySQL')
        fin = ' <color:#3A4A6B>[' + '; '.join(dict.fromkeys(marcas)) + ']</color>' if marcas else ''
        requisito = 'opcional' if c['nulo'] else 'obligatorio'
        if k == len(v['pk']):
            filas.append('  --')
        filas.append(f"  {{field}} **{c['n']}**{fin}")
        desc = resaltar(limpiar(c['c']))
        if c['tipo'].startswith('enum'):
            filas.append(f"  {{field}} {desc} ({requisito})")
            filas.append(f"  {{field}} <color:#555555>{tipo_sencillo(c)}</color>")
        else:
            filas.append(f"  {{field}} {desc} <color:#555555>({tipo_sencillo(c)}; {requisito})</color>")
    return f'entity "{titulo_tabla(nombre, v)}" as {nombre} {color} {{\n' + '\n'.join(filas) + '\n}'


def conector(f, v):
    nulable = any(next(c for c in v['cols'] if c['n'] == n)['nulo'] for n in f['cols'])
    unico = any(sorted(f['cols']) == sorted(u) or (f['cols'][0] == u[0] and len(u) == 1) for u in v['unicos'])
    izq = '|o' if nulable else '||'
    der = 'o|' if unico else 'o{'
    if unico:
        frase = 'uno a uno'
    elif nulable:
        frase = 'opcional: uno a muchos'
    else:
        frase = 'uno a muchos'
    return izq, der, frase


def relacion(tabla, f, v, etiqueta=True):
    izq, der, frase = conector(f, v)
    linea = f"{f['ref']} {izq}--{der} {tabla}"
    if not etiqueta:
        return linea
    col = next(c for c in v['cols'] if c['n'] == f['cols'][0])
    texto = re.split(r'[;.:] ', limpiar(col['c']))[0]
    partes = textwrap.wrap(texto, 26) + [f'({frase})']
    return linea + ' : "' + '\\n'.join(partes) + '"'


LEYENDA = """legend left
  **Cómo leer este diagrama**
  **Clave primaria:** identifica cada registro de la tabla, sin repetirse.
  **Clave foránea:** enlace con un registro de otra tabla.
  **Línea entre tablas:** ||  exactamente uno;  o|  cero o uno;  o{  cero o muchos.
  **POR REVISAR:** su significado debe confirmarlo el negocio o el equipo.
endlegend"""
NOTA_USUARIO = 'note as N\nLos campos **usuario_id**, **vendedor_id** y los terminados\nen **_por_id** indican qué empleado hizo la operación\n(tabla **usuario**). Esas líneas se omiten para que\nel diagrama se pueda leer.\nend note'


def puml(nombre, titulo, cuerpo):
    return f'@startuml {nombre}\ntitle {titulo}\n{ESTILO}\n{cuerpo}\n@enduml\n'


def diagrama_resumen():
    colores = {m[0]: m[2] for m in MODULOS}
    cajas = [('catalogo', 'Catálogo', 'qué se vende'),
             ('compras', 'Compras y proveedores', 'qué se compra y a quién'),
             ('inventario', 'Inventario', 'lotes, vencimientos y alertas'),
             ('ventas', 'Pedidos y ventas', 'en línea y en mostrador'),
             ('clientes', 'Clientes y suscripciones', 'cuentas y planes'),
             ('caja', 'Caja', 'dinero de cada turno'),
             ('accesos', 'Accesos y auditoría', 'empleados, permisos e historial')]
    flechas = [('catalogo', '-->', 'inventario', 'cada presentación\\ntiene sus lotes'),
               ('compras', '-->', 'inventario', 'cada entrada\\ncrea existencias'),
               ('catalogo', '-->', 'ventas', 'se vende\\nuna presentación'),
               ('ventas', '-->', 'inventario', 'cada venta descuenta\\nel lote que vence antes'),
               ('clientes', '-->', 'ventas', 'el plan activo\\nda el descuento'),
               ('ventas', '-->', 'caja', 'las ventas de mostrador\\nentran a la caja'),
               ('accesos', '..>', 'caja', 'registra qué empleado\\nhizo cada operación')]
    lineas = ['@startuml resumen', 'title Cómo se conectan las partes de la base de datos', 'skinparam shadowing false',
              'skinparam roundCorner 10', 'skinparam defaultFontName Arial', 'skinparam dpi 110', 'skinparam ArrowColor #3A4A6B',
              'skinparam nodesep 70', 'skinparam ranksep 80', 'skinparam rectangle {', '  BorderColor #10224D', '  FontSize 14', '}']
    lineas += [f'rectangle "**{n}**\\n{d}" as {c} {colores[c]}' for c, n, d in cajas]
    lineas += [f'{a} {f} {b} : {e}' for a, f, b, e in flechas]
    lineas.append('@enduml\n')
    return '\n'.join(lineas)


def diagrama_general(t):
    partes = []
    for clave, nom, color, _, tablas in MODULOS:
        partes.append(f'package "{nom}" {color} {{')
        partes += [f'  entity "{titulo_tabla(x, t[x])}" as {x}' for x in tablas]
        partes.append('}')
    lineas = {relacion(tb, f, v, etiqueta=False) for tb, v in t.items() for f in v['fks'] if f['ref'] != 'usuario'}
    partes += sorted(lineas)
    partes.append('hide members')
    partes.append(LEYENDA)
    partes.append('note as N\nCasi todas las tablas guardan también qué empleado\nhizo cada operación. Esas líneas hacia **usuario**\nse omiten para que el mapa se pueda leer.\nend note')
    return puml('mapa-general', 'Mapa general: todas las tablas y qué guarda cada una', '\n'.join(partes))


def diagrama_modulo(t, modulo):
    clave, nom, color, desc, tablas = modulo
    partes = [entidad(x, t[x], color) for x in tablas]
    externas = {}
    rel = []
    omitidas = False
    for x in tablas:
        for f in t[x]['fks']:
            if f['ref'] == 'usuario' and clave != 'accesos':
                omitidas = True
                continue
            rel.append(relacion(x, f, t[x]))
            if f['ref'] not in tablas:
                externas[f['ref']] = MODULO_DE[f['ref']]
    # Tablas de otros módulos: solo su nombre y qué guardan, para ver con qué se conecta
    for x, m in sorted(externas.items()):
        partes.append(f'entity "{titulo_tabla(x, t[x])}" as {x} {m[2]}')
        partes.append(f'hide {x} members')
    partes += rel
    if omitidas:
        partes.append(NOTA_USUARIO)
    partes.append(LEYENDA)
    return puml(f'modulo-{clave}', f'{nom}: {desc}', '\n'.join(partes))


def diagrama_completo(t):
    partes = []
    for _, nom, color, _, tablas in MODULOS:
        partes.append(f'package "{nom}" {{')
        partes += ['  ' + entidad(x, t[x], color).replace('\n', '\n  ') for x in tablas]
        partes.append('}')
    partes += [relacion(tb, f, v, etiqueta=False) for tb, v in t.items() for f in v['fks']]
    partes.append(LEYENDA)
    return puml('completo', 'Modelo completo: todas las tablas, campos y relaciones', '\n'.join(partes))


def dibujar(archivos):
    if not pathlib.Path(PLANTUML).exists():
        print('PlantUML no está disponible: se generaron los .puml, pero no las imágenes')
        return
    for a in archivos:
        r = subprocess.run([JAVA, '-Djava.awt.headless=true', '-DPLANTUML_LIMIT_SIZE=16384', '-jar', PLANTUML, '-Playout=smetana', '-charset', 'UTF-8', '-tpng', str(a)],
                           capture_output=True, timeout=240)
        if r.returncode:
            raise SystemExit(f'{a.name}: {r.stdout.decode()} {r.stderr.decode()}')


def diccionario(t):
    conteos = dict(tablas=len(t), columnas=sum(len(v['cols']) for v in t.values()), fks=sum(len(v['fks']) for v in t.values()))
    s = ['# Diccionario de datos', '',
         'Describe cada tabla y columna de la base de datos de la Droguería y Variedades Los Tres Hermanos.',
         'Este archivo se genera desde MySQL con `herramientas/generar_documentacion.py`; si cambia la base, se vuelve a generar.', '',
         f"**{conteos['tablas']} tablas, {conteos['columnas']} columnas y {conteos['fks']} claves foráneas.** Motor InnoDB y juego de caracteres utf8mb4.", '',
         '**Cómo leer las tablas:** PK es clave primaria, FK es clave foránea y Único significa que el valor no se puede repetir. Una columna calculada la obtiene MySQL sola.', '',
         '## Contenido', '']
    for _, nom, _, desc, tablas in MODULOS:
        s.append(f'- **{nom}:** ' + ', '.join(f'[`{x}`](#{x})' for x in tablas))
    for clave, nom, _, desc, tablas in MODULOS:
        s += ['', f'## {nom}', '', desc]
        for x in tablas:
            v = t[x]
            s += ['', f'### {x}', '', v['comentario'] + '.', '', '| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |', '|---|---|---|---|---|---|']
            for c in v['cols']:
                claves = [m for m, si in (('PK', c['pk']), ('FK', c['fk']), ('Único', c['uq']), ('Calculada', c['gen'])) if si]
                defecto = 'autonumérico' if 'auto_increment' in c['extra'] else {'-': '', 'CURRENT_TIMESTAMP': 'fecha actual', '1': 'verdadero' if c['tipo'].startswith('tinyint(1)') else '1', '0': 'falso' if c['tipo'].startswith('tinyint(1)') else '0'}.get(c['defecto'], c['defecto'])
                if c['gen']:
                    defecto = ''
                s.append(f"| `{c['n']}` | {tipo_largo(c)} | {'No' if c['nulo'] else 'Sí'} | {', '.join(claves)} | {defecto} | {c['c']} |")
            if v['fks']:
                s += ['', '**Se relaciona con:**', '']
                s += [f"- `{', '.join(f['cols'])}` → [`{f['ref']}`](#{f['ref']}) (`{', '.join(f['refcols'])}`)" for f in v['fks']]
            compuestos = [u for u in v['unicos'] if len(u) > 1]
            if compuestos:
                s += ['', '**Combinaciones que no se pueden repetir:** ' + '; '.join('(`' + '`, `'.join(u) + '`)' for u in compuestos)]
            if v['checks']:
                s += ['', '**Reglas (CHECK):**', '']
                s += [f"- `{n}`: `{cl.replace('|', '¦')}`" for n, cl in v['checks']]
    revisar = [(x, c) for _, _, _, _, tablas in MODULOS for x in tablas for c in t[x]['cols'] if 'POR REVISAR' in c['c']]
    s += ['', '## Campos por revisar', '', 'Su significado no se pudo confirmar con el código del proyecto. Debe aclararlo el negocio o el equipo.', '',
          '| Tabla | Campo | Qué falta aclarar |', '|---|---|---|']
    s += [f"| `{x}` | `{c['n']}` | {c['c'].split('POR REVISAR: ')[1]} |" for x, c in revisar]
    s += ['', '## Lógica dentro de la base de datos', '', '### Disparadores', '', '| Disparador | Tabla | Cuándo | Qué hace |', '|---|---|---|---|']
    QUE = {
        'trg_categoria_insertar': 'Exige que la subcategoría sea del mismo tipo que su categoría superior.',
        'trg_categoria_actualizar': 'Hace la misma validación e impide ciclos en la jerarquía.',
        'trg_lote_insertar': 'Hace que todo lote nazca con existencia cero.',
        'trg_lote_actualizar': 'Impide cambiar la existencia sin registrar un movimiento.',
        'trg_movinv_insertar': 'Rechaza vender lotes vencidos o sin existencia, actualiza el lote y guarda el saldo.',
        'trg_movinv_alertas': 'Abre o cierra las alertas de agotado y de inventario bajo.',
        'trg_movinv_no_actualizar': 'Impide modificar el historial de inventario.',
        'trg_movinv_no_borrar': 'Impide borrar el historial de inventario.',
        'trg_auditoria_no_actualizar': 'Impide modificar la auditoría.',
        'trg_auditoria_no_borrar': 'Impide borrar la auditoría.'}
    for n, tb, cuando, ev in consulta(f"SELECT trigger_name, event_object_table, action_timing, event_manipulation FROM triggers WHERE trigger_schema='{ESQUEMA}' ORDER BY event_object_table, trigger_name"):
        s.append(f"| `{n}` | `{tb}` | {'Antes' if cuando == 'BEFORE' else 'Después'} de {dict(INSERT='insertar', UPDATE='actualizar', DELETE='borrar')[ev]} | {QUE[n]} |")
    s += ['', '### Procedimientos', '', '| Procedimiento | Qué hace |', '|---|---|',
          '| `descontar_stock_fefo` | Descuenta una línea vendida, empezando por el lote que vence antes. Si no alcanza, rechaza la venta completa. |',
          '| `anular_pedido` | Devuelve las unidades a sus lotes, reembolsa los pagos y marca el pedido como cancelado. |',
          '| `aprobar_solicitud` | Verifica el precio contra el plan, aprueba y activa la suscripción. Al renovar conserva los días que faltaban. |',
          '| `anonimizar_cliente` | Borra los datos personales de una cuenta y conserva sus ventas sin ellos. |',
          '| `tareas_diarias` | Marca las suscripciones vencidas y genera alertas de lotes próximos a vencer. La ejecuta el evento `evt_tareas_diarias`. |',
          '', '### Vistas', '', '| Vista | Para qué sirve |', '|---|---|',
          '| `vista_stock` | Existencias en tiempo real por producto y categoría (RF007, RF011). |',
          '| `vista_proximos_vencer` | Lotes con existencia y días que les faltan para vencer (RF005). |',
          '| `vista_historial_inventario` | Entradas y salidas de cada producto (RF006). |',
          '| `vista_ventas` | Ventas por fecha, producto y vendedor (RF012, RF013). |', '']
    return '\n'.join(s)


t = leer()
DIAGRAMAS.mkdir(exist_ok=True)
archivos = {'resumen': diagrama_resumen(), 'mapa-general': diagrama_general(t), 'completo': diagrama_completo(t)}
for m in MODULOS:
    archivos[f'modulo-{m[0]}'] = diagrama_modulo(t, m)
rutas = []
for n, c in archivos.items():
    p = DIAGRAMAS / f'{n}.puml'
    p.write_text(c, 'utf-8', newline='\n')
    rutas.append(p)
(BASE / 'diccionario-de-datos.md').write_text(diccionario(t), 'utf-8', newline='\n')
dibujar(rutas)
print(f'{len(t)} tablas; {len(rutas)} diagramas; diccionario listo')
