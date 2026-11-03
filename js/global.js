/* ============================================================
   global.js — Lógica compartida de todas las páginas
   Heurística Nielsen #1: Visibilidad del estado del sistema
   Heurística Nielsen #3: Control y libertad del usuario
   ============================================================ */

// ── Navbar sticky + hamburguesa ──────────────────────────────
(function initNavbar() {
  const navbar   = document.querySelector('.navbar');
  const btnMenu  = document.querySelector('.btn-menu');
  const navLinks = document.querySelector('.navbar-nav');
  const velo     = document.getElementById('barra-velo');
  if (!navbar) return;

  function alDesplazar() {
    navbar.classList.toggle('scrolled', window.scrollY > 16);
  }
  window.addEventListener('scroll', alDesplazar, { passive: true });
  alDesplazar();

  if (!btnMenu || !navLinks) return;

  function fijarMenu(abierto) {
    navLinks.classList.toggle('abierto', abierto);
    velo && velo.classList.toggle('visible', abierto);
    btnMenu.setAttribute('aria-expanded', String(abierto));
    document.body.classList.toggle('sin-scroll', abierto);
  }

  btnMenu.addEventListener('click', () => fijarMenu(!navLinks.classList.contains('abierto')));
  velo && velo.addEventListener('click', () => fijarMenu(false));
  document.getElementById('btn-cerrar-menu')?.addEventListener('click', () => {
    fijarMenu(false);
    btnMenu.focus();
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && navLinks.classList.contains('abierto')) {
      fijarMenu(false);
      btnMenu.focus();
    }
  });
  window.addEventListener('resize', () => {
    if (window.innerWidth > 860) fijarMenu(false);
  });
})();

// Mi cuenta: lleva al perfil si hay sesión de cliente
(function initMiCuenta() {
  const enlace = document.getElementById('btn-mi-cuenta');
  if (!enlace) return;
  try {
    const sesion = JSON.parse(sessionStorage.getItem('lth_cliente') || 'null');
    enlace.href = (sesion && sesion.rol === 'cliente') ? '/DrogueriaYvariedadesLosTresHermanos/perfil' : '/DrogueriaYvariedadesLosTresHermanos/login';
  } catch (e) {}
})();

// ── Sistema de Toast (Nielsen #1 — feedback al usuario) ──────
window.Toast = (function() {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  function mostrar(mensaje, duracion = 3000) {
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerHTML = '<svg class="ic" aria-hidden="true"><use href="/DrogueriaYvariedadesLosTresHermanos/img/iconos.svg#check"/></svg><span></span>';
    toast.lastChild.textContent = mensaje;
    container.appendChild(toast);

    setTimeout(() => {
      toast.classList.add('salir');
      toast.addEventListener('animationend', () => toast.remove());
    }, duracion);
  }

  return { mostrar };
})();

// ── Carrito global compartido (localStorage) ─────────────────
window.Carrito = (function() {
  const KEY = 'drogueria_carrito';

  function obtener() {
    try {
      const items = JSON.parse(localStorage.getItem(KEY));
      if (!Array.isArray(items)) return [];
      // Descarta cantidades o precios manipulados (negativos, texto, etc.)
      return items.filter(i => i
        && Number.isInteger(i.cantidad) && i.cantidad > 0
        && Number.isFinite(i.precio) && i.precio >= 0);
    } catch { return []; }
  }

  function guardar(items) {
    localStorage.setItem(KEY, JSON.stringify(items));
    actualizarBadge();
  }

  function agregar(producto) {
    const items = obtener();
    const idx = items.findIndex(i => i.id === producto.id);
    if (idx !== -1) {
      items[idx].cantidad += 1;
    } else {
      items.push({ ...producto, cantidad: 1 });
    }
    guardar(items);
    Toast.mostrar(`${producto.nombre} se agregó al carrito`);
  }

  function remover(id) {
    // Comparar como string para IDs compuestos
    const items = obtener().filter(i => String(i.id) !== String(id));
    guardar(items);
  }

  function cambiarCantidad(id, delta) {
    const items = obtener();
    // Comparar como string para IDs compuestos como "1-a1u"
    const idx = items.findIndex(i => String(i.id) === String(id));
    if (idx === -1) return;
    const nuevaCantidad = items[idx].cantidad + delta;
    if (nuevaCantidad <= 0) {
      // Eliminar si llega a 0
      items.splice(idx, 1);
    } else {
      items[idx].cantidad = nuevaCantidad;
    }
    guardar(items);
  }

  function total() {
    return obtener().reduce((s, i) => s + i.precio * i.cantidad, 0);
  }

  function conteo() {
    return obtener().reduce((s, i) => s + i.cantidad, 0);
  }

  function actualizarBadge() {
    const badge = document.querySelector('.badge-carrito');
    if (badge) {
      const n = conteo();
      const cambio = badge.textContent !== String(n);
      badge.textContent = n;
      badge.style.display = n > 0 ? 'grid' : 'none';
      if (cambio) {
        badge.classList.remove('rebote');
        void badge.offsetWidth;
        badge.classList.add('rebote');
      }
    }
    document.querySelectorAll('.fab-badge').forEach(fab => {
      fab.textContent = conteo();
      fab.toggleAttribute('data-cero', conteo() === 0);
      fab.classList.remove('rebote');
      void fab.offsetWidth;
      fab.classList.add('rebote');
    });
  }

  // Inicializar badge al cargar
  document.addEventListener('DOMContentLoaded', actualizarBadge);

  return { obtener, agregar, remover, cambiarCantidad, total, conteo };
})();

// ── Estrellas de calificación ─────────────────────────────────
window.renderEstrellas = function(rating) {
  let html = '<div class="estrellas" aria-label="Calificación: ' + rating + ' de 5">';
  for (let i = 1; i <= 5; i++) {
    const clase = i <= Math.round(rating) ? 'estrella-llena' : 'estrella-vacia';
    html += `<svg class="${clase}" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26"/>
    </svg>`;
  }
  html += '</div>';
  return html;
};

// ── Formatear precio colombiano ───────────────────────────────
window.formatPrecio = function(valor) {
  return '$' + (Number(valor) || 0).toLocaleString('es-CO');
};

// ── Avatar navbar (cliente logueado) ────────────────────────────────────────
// Detecta si hay sesión activa y transforma el navbar:
//   - Oculta el botón "Regístrate"
//   - Reemplaza "Mi cuenta" por un dropdown con nombre, avatar y opciones
// Si no hay sesión, el navbar queda intacto (links a /registro y /login).
// ────────────────────────────────────────────────────────────────────────────
window.initAvatar = function(opciones) {
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => _construirAvatar(opciones));
  } else {
    _construirAvatar(opciones);
  }
};

function _construirAvatar(opciones) {
  var opts      = opciones || {};
  var perfilUrl = opts.perfilUrl || '/DrogueriaYvariedadesLosTresHermanos/perfil';
  var logoutUrl = opts.logoutUrl || '/DrogueriaYvariedadesLosTresHermanos/';

  // 1. Leer sesión
  var sesion;
  try { sesion = JSON.parse(sessionStorage.getItem('lth_cliente') || 'null'); } catch(e) { sesion = null; }

  if (!sesion || sesion.rol !== 'cliente') return; // sin sesión → navbar queda intacto

  // 2. Ocultar "Regístrate" (tiene id="btn-registrate" en index.html)
  var btnReg = document.getElementById('btn-registrate');
  if (btnReg) btnReg.style.display = 'none';

  // 3. Localizar "Mi cuenta"
  var btnMiCuenta = document.getElementById('btn-mi-cuenta');
  if (!btnMiCuenta) return;

  // 4. Apuntar a /perfil AHORA (garantía mínima aunque falle el dropdown)
  btnMiCuenta.href = perfilUrl;

  // 5. Datos del usuario para el dropdown
  var nombre  = sesion.nombre  || 'Usuario';
  var correo  = sesion.correo  || '';
  var esc     = Seguridad.esc;
  var inicial = String(nombre)[0].toUpperCase();
  var fotoData = '';
  try { fotoData = Seguridad.imagenDataSegura(localStorage.getItem('lth_foto_' + sesion.id)); } catch(e) {}

  var avatarHtml = fotoData
    ? '<img src="' + esc(fotoData) + '" alt="' + esc(nombre) + '" style="width:100%;height:100%;border-radius:50%;object-fit:cover">'
    : esc(inicial);

  // 6. Crear el contenedor del dropdown e insertarlo en lugar del botón
  var wrap = document.createElement('div');
  wrap.className = 'avatar-navbar-wrap';
  wrap.id = 'avatar-wrap';
  wrap.innerHTML =
    '<button class="avatar-navbar-btn" id="avatar-btn" aria-label="Mi cuenta" aria-expanded="false">' +
      '<div class="avatar-circulo">' + avatarHtml + '<span class="avatar-online-dot"></span></div>' +
      '<span class="avatar-nombre-corto">' + esc(String(nombre).split(' ')[0]) + '</span>' +
      '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>' +
    '</button>' +
    '<div class="avatar-dropdown" id="avatar-dropdown" role="menu">' +
      '<div class="avatar-dropdown-header">' +
        '<div class="avatar-grande-drop">' + avatarHtml + '</div>' +
        '<div class="avatar-drop-info">' +
          '<div class="avatar-drop-nombre">' + esc(nombre) + '</div>' +
          '<div class="avatar-drop-correo">' + esc(correo) + '</div>' +
        '</div>' +
      '</div>' +
      '<div class="avatar-dropdown-items">' +
        '<a class="avatar-drop-item" href="' + perfilUrl + '" role="menuitem">' +
          '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>' +
          'Mi Perfil</a>' +
        '<a class="avatar-drop-item" href="' + perfilUrl + '#pedidos" role="menuitem">' +
          '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>' +
          'Mis Pedidos</a>' +
        '<a class="avatar-drop-item" href="' + perfilUrl + '#favoritos" role="menuitem">' +
          '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>' +
          'Favoritos</a>' +
        '<div class="avatar-drop-divider"></div>' +
        '<button class="avatar-drop-item peligro" onclick="cerrarSesionCliente(\'' + logoutUrl + '\')" role="menuitem">' +
          '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9"/></svg>' +
          'Cerrar sesión</button>' +
      '</div>' +
    '</div>';

  // Reemplazar el botón original con el wrapper del dropdown
  btnMiCuenta.parentNode.replaceChild(wrap, btnMiCuenta);

  // 7. Toggle dropdown al hacer clic
  document.addEventListener('click', function(e) {
    var w = document.getElementById('avatar-wrap');
    var d = document.getElementById('avatar-dropdown');
    var b = document.getElementById('avatar-btn');
    if (!w || !d) return;
    if (w.contains(e.target)) {
      var visible = d.classList.toggle('visible');
      if (b) b.setAttribute('aria-expanded', String(visible));
    } else {
      d.classList.remove('visible');
      if (b) b.setAttribute('aria-expanded', 'false');
    }
  });

  // 8. Cerrar con Escape
  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
      var d = document.getElementById('avatar-dropdown');
      var b = document.getElementById('avatar-btn');
      if (d) d.classList.remove('visible');
      if (b) b.setAttribute('aria-expanded', 'false');
    }
  });
} // fin _construirAvatar

// ── Cerrar sesión cliente ─────────────────────────────────────

window.cerrarSesionCliente = function(url) {
  sessionStorage.removeItem('lth_cliente');
  window.location.href = url || '/DrogueriaYvariedadesLosTresHermanos/';
};

// ── Protección de rutas (redirige a login si no hay sesión) ──────────────────
window.protegerRuta = function() {
  let sesion = null;
  try { sesion = JSON.parse(sessionStorage.getItem('lth_cliente') || 'null'); } catch(e) {}
  if (!sesion || !sesion.correo) {
    const retorno = window.location.pathname.replace(/^.*\//, '').replace(/\.html$/, '');
    window.location.href = '/DrogueriaYvariedadesLosTresHermanos/login' + (retorno ? '?retorno=' + encodeURIComponent(retorno) : '');
  }
};

// ── Verificar y cancelar suscripciones vencidas ──────────────────────────────
window.verificarVencimientoSuscripcion = function() {
  try {
    const KEY = 'lth_suscripcion';
    const sus = JSON.parse(localStorage.getItem(KEY));
    if (!sus || !sus.fechaVencimiento) return;

    const ahora      = new Date();
    const vencimiento = new Date(sus.fechaVencimiento);
    const diasRestantes = Math.ceil((vencimiento - ahora) / (1000 * 60 * 60 * 24));

    // Cancelar si ya venció
    if (diasRestantes <= 0) {
      localStorage.removeItem(KEY);
      // Mostrar aviso si hay banner
      const banner = document.getElementById('banner-suscripcion-global');
      if (banner) {
        banner.style.background = 'var(--burgundy)';
        (banner.querySelector('.banner-texto') || banner).textContent =
          'Tu suscripción venció y fue cancelada automáticamente.';
        setTimeout(() => banner.remove(), 5000);
      }
      return;
    }

    // Alerta si quedan 5 días o menos
    if (diasRestantes <= 5) {
      const alerta = document.getElementById('alerta-vencimiento-prox');
      if (alerta) {
        alerta.style.display = 'flex';
        const txt = alerta.querySelector('.alerta-venc-txt');
        if (txt) txt.textContent =
          `Tu plan vence en ${diasRestantes} día${diasRestantes === 1 ? '' : 's'}. Renuévalo para mantener tu descuento.`;
      }
    }
  } catch(e) {}
};

// ══════════════════════════════════════════════════════════════
//  SISTEMA DE SUSCRIPCIONES Y DESCUENTOS AUTOMÁTICOS
//  Solo cuenta el plan que el administrador aprobó para el cliente
//  con sesión (lth_sus_<id>). Una solicitud pendiente o rechazada
//  no da descuento. El porcentaje sale siempre de la tabla oficial.
// ══════════════════════════════════════════════════════════════

window.Suscripcion = (function() {
  function claveCliente() {
    try {
      const sesion = JSON.parse(sessionStorage.getItem('lth_cliente') || 'null');
      return (sesion && sesion.rol === 'cliente' && sesion.id) ? 'lth_sus_' + sesion.id : null;
    } catch(e) { return null; }
  }

  /**
   * Devuelve la suscripción vigente del cliente o null. Una suscripción cancelada
   * conserva el descuento hasta su vencimiento, como se le informa al cliente.
   */
  function obtener() {
    try {
      const clave = claveCliente();
      const sus   = clave ? JSON.parse(localStorage.getItem(clave) || 'null') : null;
      if (!sus || (sus.estado !== 'activo' && sus.estado !== 'cancelado')) return null;

      const plan        = Seguridad.planCanonico(sus.plan);
      const vencimiento = new Date(sus.fechaVencimiento);
      if (!plan || !(vencimiento > new Date())) return null;

      return {
        plan: plan.clave, descuento: plan.descuento, precio: plan.precio,
        estado: sus.estado, fecha: sus.fecha, fechaVencimiento: sus.fechaVencimiento
      };
    } catch(e) { return null; }
  }

  /** Marca como cancelada la suscripción del cliente con sesión */
  function cancelar() {
    const clave = claveCliente();
    if (!clave) return;
    try {
      const sus = JSON.parse(localStorage.getItem(clave) || 'null');
      if (sus) {
        sus.estado = 'cancelado';
        localStorage.setItem(clave, JSON.stringify(sus));
      }
    } catch(e) {}
  }

  /** Devuelve el porcentaje de descuento (0 si no hay suscripción) */
  function descuento() {
    const sus = obtener();
    return sus ? sus.descuento : 0;
  }

  /** Calcula el precio con descuento */
  function aplicar(precio) {
    const pct = descuento();
    if (!pct) return precio;
    return Math.round(precio * (1 - pct / 100));
  }

  /**
   * Inyecta un banner de descuento activo debajo del navbar en cualquier página.
   * Sólo aparece si hay suscripción con descuento.
   */
  function mostrarBanner() {
    const sus = obtener();
    if (!sus) return;

    // Evitar duplicados
    if (document.getElementById('sus-descuento-banner')) return;

    const nombrePlan = Seguridad.planCanonico(sus.plan).nombre;
    const banner  = document.createElement('aside');
    banner.setAttribute('aria-label', 'Tu plan de suscripción');
    banner.id     = 'sus-descuento-banner';
    banner.className = 'pildora-plan no-imprimir';
    banner.innerHTML =
      `<span class="punto punto--vivo"></span>
       <span>Plan ${Seguridad.esc(nombrePlan)}: <strong>${Seguridad.esc(sus.descuento)}% de descuento</strong></span>
       <a href="/DrogueriaYvariedadesLosTresHermanos/perfil#suscripcion">Ver plan</a>`;
    document.body.appendChild(banner);
    // Es un aviso breve: no debe quedarse tapando contenido
    setTimeout(() => banner.classList.add('saliendo'), 7000);
    setTimeout(() => banner.remove(), 7600);
  }

  // Inicializar banner en DOMContentLoaded
  document.addEventListener('DOMContentLoaded', mostrarBanner);

  return { obtener, cancelar, descuento, aplicar, mostrarBanner };
})();
