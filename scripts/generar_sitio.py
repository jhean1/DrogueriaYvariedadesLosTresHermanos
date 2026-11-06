#!/usr/bin/env python3
"""Genera la versión estática de la tienda para GitHub Pages.

Arranca la aplicación ya compilada (target/*.jar), descarga cada página pública,
ajusta las rutas a la dirección de Pages y deja el resultado en la carpeta de salida.
El panel /admin no se publica: necesita el servidor para validar sus permisos.

Uso: python scripts/generar_sitio.py --salida _sitio [--base /Repositorio]
"""
import argparse
import pathlib
import re
import secrets
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request

RAIZ = pathlib.Path(__file__).resolve().parent.parent
ESTATICOS = RAIZ / 'src/main/resources/static'
PUERTO = 8095
ORIGEN = f'http://127.0.0.1:{PUERTO}'

# Página pública -> archivo. /pago-suscripcion no se publica: sin panel, nadie revisaría los pagos.
PAGINAS = {
    '/': 'index.html', '/medicamentos': 'medicamentos.html', '/variedades': 'variedades.html',
    '/servicios': 'servicios.html', '/suscripciones': 'suscripciones.html', '/nosotros': 'nosotros.html',
    '/contacto': 'contacto.html', '/login': 'login.html', '/registro': 'registro.html',
    '/perfil': 'perfil.html', '/checkout': 'checkout.html', '/recibo': 'recibo.html',
    '/esta-pagina-no-existe': '404.html',
}
# Recursos que solo usa el panel
EXCLUIDOS = {'css/admin.css', 'js/admin.js', 'js/panel.js'}

RUTAS = 'perfil|login|registro|checkout|recibo|medicamentos|variedades|servicios|suscripciones|nosotros|contacto'
ABSOLUTA = re.compile(rf'''(?<=['"`(])/(?=(?:{RUTAS})(?:['"`?#)\s]|$)|img/|js/|css/|productos/|\#)''')
RAIZ_SOLA = re.compile(r'''(['"`])/\1''')

# Equivale a la política del servidor; frame-ancestors no se admite en una etiqueta meta.
CSP = ("default-src 'self'; script-src 'self' 'unsafe-inline'; "
       "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; "
       "img-src 'self' data:; frame-src https://www.google.com; connect-src 'self'; object-src 'none'; "
       "base-uri 'self'; form-action 'self'")


def ajustar_rutas(texto, base):
    texto = ABSOLUTA.sub(base + '/', texto)
    return RAIZ_SOLA.sub(lambda m: f'{m.group(1)}{base}/{m.group(1)}', texto)


def descargar(ruta):
    try:
        pedido = urllib.request.Request(ORIGEN + ruta, headers={'Accept': 'text/html'})
        with urllib.request.urlopen(pedido, timeout=20) as r:
            return r.read().decode('utf-8')
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return e.read().decode('utf-8')
        raise


def esperar_aplicacion():
    for _ in range(120):
        try:
            urllib.request.urlopen(ORIGEN + '/', timeout=2)
            return
        except (urllib.error.URLError, OSError):
            time.sleep(.5)
    raise SystemExit('La aplicación no arrancó')


def generar(salida, base):
    if salida.exists():
        shutil.rmtree(salida)
    salida.mkdir(parents=True)

    for archivo in ESTATICOS.rglob('*'):
        relativo = archivo.relative_to(ESTATICOS).as_posix()
        if archivo.is_dir() or relativo in EXCLUIDOS:
            continue
        destino = salida / relativo
        destino.parent.mkdir(parents=True, exist_ok=True)
        if archivo.suffix in {'.js', '.css'}:
            destino.write_text(ajustar_rutas(archivo.read_text(encoding='utf-8'), base), encoding='utf-8')
        else:
            shutil.copyfile(archivo, destino)

    shutil.copyfile(RAIZ / 'scripts/sitio/modo-estatico.js', salida / 'js/modo-estatico.js')
    with urllib.request.urlopen(f'{ORIGEN}/js/catalogo.js', timeout=20) as r:
        catalogo = r.read().decode('utf-8')
    (salida / 'js/catalogo.js').write_text(ajustar_rutas(catalogo, base), encoding='utf-8')
    for foto in set(re.findall(r'/productos/imagen/([\w.-]+)', catalogo)):
        destino = salida / 'productos/imagen' / foto
        destino.parent.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(f'{ORIGEN}/productos/imagen/{foto}', timeout=20) as r:
            destino.write_bytes(r.read())
    meta = f'<meta http-equiv="Content-Security-Policy" content="{CSP}">'
    modo = f'<script src="{base}/js/modo-estatico.js"></script>'
    for ruta, nombre in PAGINAS.items():
        html = ajustar_rutas(descargar(ruta), base)
        html = html.replace('<meta charset="UTF-8">', '<meta charset="UTF-8">\n  ' + meta, 1)
        assert meta in html, f'{ruta}: no se pudo agregar la política de contenido'
        html = html.replace('</body>', f'  {modo}\n</body>', 1)
        (salida / nombre).write_text(html, encoding='utf-8')
    (salida / '.nojekyll').write_text('', encoding='utf-8')


def validar(salida, base):
    errores = []
    enlace = re.compile(r'''(?:href|src)=["'](/[^"'#?]*)''')
    for archivo in salida.rglob('*.html'):
        texto = archivo.read_text(encoding='utf-8')
        for ruta in enlace.findall(texto):
            if not ruta.startswith(base + '/'):
                errores.append(f'{archivo.name}: ruta sin ajustar {ruta}')
                continue
            relativa = ruta[len(base) + 1:]
            if relativa and not ((salida / relativa).exists() or (salida / (relativa + '.html')).exists()):
                errores.append(f'{archivo.name}: enlace roto {ruta}')
    for archivo in list(salida.rglob('*.html')) + list(salida.rglob('*.js')):
        if re.search(rb'ghp_|BEGIN [A-Z ]*PRIVATE', archivo.read_bytes()):
            errores.append(f'{archivo.name}: posible secreto')
    if errores:
        raise SystemExit('Validación fallida:\n' + '\n'.join(sorted(set(errores))))


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--salida', default='_sitio', type=pathlib.Path)
    p.add_argument('--base', default='/DrogueriaYvariedadesLosTresHermanos')
    p.add_argument('--catalogo', default='datos/catalogo.properties')
    p.add_argument('--fotos', default='datos/fotos')
    a = p.parse_args()

    jar = next((j for j in sorted((RAIZ / 'target').glob('*.jar')) if 'original' not in j.name), None)
    if not jar:
        raise SystemExit('Compila primero con: ./mvnw package -DskipTests')

    proceso = subprocess.Popen(
        ['java', '-jar', str(jar), f'--server.port={PUERTO}', f'--app.admin.clave={secrets.token_urlsafe(24)}',
         f'--app.datos={tempfile.gettempdir()}/sitio-{secrets.token_hex(6)}.properties',
         '--app.catalogo=' + a.catalogo, '--app.fotos=' + a.fotos,
         # Sin base de datos: la versión estática no tiene cuentas ni pedidos en línea
         '--app.db.url='],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    try:
        esperar_aplicacion()
        generar(a.salida, a.base.rstrip('/'))
        validar(a.salida, a.base.rstrip('/'))
    finally:
        proceso.terminate()
        proceso.wait(timeout=30)
    print(f'Sitio generado en {a.salida}: {sum(1 for f in a.salida.rglob("*") if f.is_file())} archivos')


if __name__ == '__main__':
    sys.exit(main())
