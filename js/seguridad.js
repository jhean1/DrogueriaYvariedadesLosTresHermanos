/* ============================================================
   seguridad.js — Utilidades de seguridad y conexión con el servidor.
   Debe cargarse antes que global.js y que cualquier script de página.
   Las cuentas, pedidos y planes viven en el servidor: el navegador
   no guarda contraseñas ni datos de clientes.
   ============================================================ */
window.Seguridad = (function () {
  // Nombres y orden de los planes para mostrarlos. El precio y el descuento que
  // se cobran los decide siempre el servidor con la base de datos.
  const PLANES = {
    basico:   { clave: 'basico',   nombre: 'Básico',   descuento: 3,  precio: 4900  },
    estandar: { clave: 'estandar', nombre: 'Estándar', descuento: 5,  precio: 9900  },
    regular:  { clave: 'regular',  nombre: 'Regular',  descuento: 10, precio: 14900 },
    premium:  { clave: 'premium',  nombre: 'Premium',  descuento: 15, precio: 19900 }
  };

  /** Escapa un valor para insertarlo en HTML o en un atributo entre comillas. */
  function esc(valor) {
    if (valor === null || valor === undefined) return '';
    return String(valor).replace(/[&<>"'`]/g, c => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;', '`': '&#96;'
    }[c]));
  }

  /** Solo permite volver a páginas internas conocidas (evita redirección abierta). */
  function rutaRetornoSegura(retorno, porDefecto) {
    const permitidas = ['suscripciones', 'pago-suscripcion', 'perfil', 'checkout'];
    const [pagina, consulta] = String(retorno || '').split('?');
    if (!permitidas.includes(pagina)) return porDefecto;
    // Solo se conserva el plan elegido, con un valor conocido
    const plan = new URLSearchParams(consulta || '').get('plan');
    return '/DrogueriaYvariedadesLosTresHermanos/' + pagina + (plan && planCanonico(plan) ? '?plan=' + planCanonico(plan).clave : '');
  }

  function planCanonico(nombre) {
    const clave = String(nombre || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
    return Object.prototype.hasOwnProperty.call(PLANES, clave) ? PLANES[clave] : null;
  }

  // Versiones anteriores guardaban cuentas, pedidos y pagos en este navegador.
  // Ya no se usan y contienen datos personales: se borran.
  function limpiarDatosAntiguos() {
    try {
      ['lth_admins', 'lth_admin_codigo', 'lth_clientes', 'lth_ventas', 'lth_solicitudes', 'lth_suscripcion']
        .forEach(k => localStorage.removeItem(k));
      for (let i = localStorage.length - 1; i >= 0; i--) {
        const k = localStorage.key(i);
        if (k && (k.startsWith('lth_sus_') || k.startsWith('lth_foto_'))) localStorage.removeItem(k);
      }
      ['lth_admin', 'lth_cliente', 'lth_recien_registro', 'lth_plan_pago', 'lth_plan_pendiente']
        .forEach(k => sessionStorage.removeItem(k));
    } catch (e) { /* almacenamiento bloqueado: no hay nada que limpiar */ }
  }
  limpiarDatosAntiguos();

  return { esc, rutaRetornoSegura, planCanonico, PLANES };
})();

/* Peticiones a la API del servidor con la sesión y el token CSRF de la página.
   Devuelve el JSON de respuesta o lanza un Error con un mensaje para el usuario
   (error.estado trae el código HTTP; 0 si no hubo conexión). */
window.Api = (function () {
  function meta(nombre) {
    const m = document.querySelector(`meta[name="${nombre}"]`);
    return m ? m.content : '';
  }

  const MENSAJES = {
    401: 'Tu sesión terminó. Inicia sesión de nuevo.',
    403: 'No tienes permiso para hacer esto, o la página estuvo abierta mucho tiempo. Recárgala e inténtalo otra vez.',
    404: 'No encontramos lo que buscas.',
    413: 'El archivo es demasiado grande.',
    503: 'Las cuentas en línea no están disponibles en este momento. Escríbenos por WhatsApp.'
  };

  function error(mensaje, estado) {
    const e = new Error(mensaje);
    e.estado = estado;
    return e;
  }

  async function pedir(ruta, { metodo = 'GET', datos, formulario } = {}) {
    const opciones = { method: metodo, credentials: 'same-origin', headers: { Accept: 'application/json' } };
    if (metodo !== 'GET') {
      const token = meta('_csrf');
      if (token) opciones.headers[meta('_csrf_header') || 'X-CSRF-TOKEN'] = token;
    }
    if (formulario) {
      opciones.body = formulario;
    } else if (datos !== undefined) {
      opciones.headers['Content-Type'] = 'application/json';
      opciones.body = JSON.stringify(datos);
    }
    let r;
    try {
      r = await fetch(ruta, opciones);
    } catch (e) {
      throw error('No hay conexión con la droguería. Revisa tu internet e inténtalo de nuevo.', 0);
    }
    const tipo = r.headers.get('content-type') || '';
    const cuerpo = tipo.includes('application/json') ? await r.json().catch(() => null) : null;
    if (!r.ok) {
      // Los mensajes de la API de la droguería traen solo "error"; la respuesta genérica de
      // Spring (por ejemplo, token CSRF vencido) trae además "status" y un texto en inglés.
      const propio = cuerpo && typeof cuerpo.error === 'string' && !('status' in cuerpo) ? cuerpo.error : null;
      throw error(propio || MENSAJES[r.status] || 'Ocurrió un error. Inténtalo de nuevo en unos minutos.', r.status);
    }
    return cuerpo;
  }

  return { pedir };
})();
