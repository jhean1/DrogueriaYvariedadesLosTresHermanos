/* ============================================================
   seguridad.js — Utilidades de seguridad para el navegador.
   Debe cargarse antes que global.js y que cualquier script de página.
   ============================================================ */
window.Seguridad = (function () {
  const ITERACIONES = 310000;

  // Fuente única de precios y descuentos. Nunca se confía en valores guardados
  // en localStorage/sessionStorage, porque el usuario puede editarlos.
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

  /** Devuelve la imagen solo si es un data URL de un formato de mapa de bits permitido. */
  function imagenDataSegura(valor) {
    return typeof valor === 'string'
      && /^data:image\/(png|jpeg|webp);base64,[a-z0-9+/=]+$/i.test(valor) ? valor : '';
  }

  /** Solo permite volver a páginas internas conocidas (evita redirección abierta). */
  function rutaRetornoSegura(retorno, porDefecto) {
    const permitidas = ['suscripciones', 'pago-suscripcion', 'perfil', 'checkout', 'recibo'];
    return permitidas.includes(retorno) ? '/' + retorno : porDefecto;
  }

  function planCanonico(nombre) {
    const clave = String(nombre || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
    return Object.prototype.hasOwnProperty.call(PLANES, clave) ? PLANES[clave] : null;
  }

  function aBase64(bytes) {
    return btoa(String.fromCharCode(...bytes));
  }

  function deBase64(texto) {
    return Uint8Array.from(atob(texto), c => c.charCodeAt(0));
  }

  // crypto.subtle solo existe en contextos seguros: HTTPS o localhost.
  function criptoDisponible() {
    return !!(window.crypto && window.crypto.subtle);
  }

  async function derivar(clave, sal, iteraciones) {
    const llave = await crypto.subtle.importKey(
      'raw', new TextEncoder().encode(clave), 'PBKDF2', false, ['deriveBits']);
    const bits = await crypto.subtle.deriveBits(
      { name: 'PBKDF2', hash: 'SHA-256', salt: sal, iterations: iteraciones }, llave, 256);
    return new Uint8Array(bits);
  }

  async function crearCredencial(clave) {
    const sal = crypto.getRandomValues(new Uint8Array(16));
    const hash = await derivar(clave, sal, ITERACIONES);
    return { alg: 'PBKDF2-SHA256', iter: ITERACIONES, sal: aBase64(sal), hash: aBase64(hash) };
  }

  async function verificarCredencial(clave, credencial) {
    if (!credencial || !credencial.sal || !credencial.hash || !credencial.iter) return false;
    const calculado = await derivar(clave, deBase64(credencial.sal), credencial.iter);
    const esperado = deBase64(credencial.hash);
    if (calculado.length !== esperado.length) return false;
    let diferencia = 0;
    calculado.forEach((byte, i) => { diferencia |= byte ^ esperado[i]; });
    return diferencia === 0;
  }

  // Borra secretos de versiones anteriores y convierte a hash las claves que
  // estaban guardadas en texto plano.
  async function migrarDatosAntiguos() {
    try {
      localStorage.removeItem('lth_admins');
      localStorage.removeItem('lth_admin_codigo');
      sessionStorage.removeItem('lth_admin');

      if (!criptoDisponible()) return;
      const clientes = JSON.parse(localStorage.getItem('lth_clientes') || '[]');
      if (!Array.isArray(clientes)) return;

      let huboCambios = false;
      for (const cliente of clientes) {
        const antigua = cliente.password || cliente.clave;
        if (typeof antigua === 'string' && antigua) {
          cliente.credencial = await crearCredencial(antigua);
          delete cliente.password;
          delete cliente.clave;
          huboCambios = true;
        }
      }
      if (huboCambios) localStorage.setItem('lth_clientes', JSON.stringify(clientes));
    } catch (e) {
      console.error('No se pudieron migrar los datos antiguos', e);
    }
  }

  return {
    esc,
    imagenDataSegura,
    rutaRetornoSegura,
    planCanonico,
    criptoDisponible,
    crearCredencial,
    verificarCredencial,
    listo: migrarDatosAntiguos()
  };
})();
