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
    toast.innerHTML = '<svg class="ic" aria-hidden="true"><use href="/img/iconos.svg#check"/></svg><span></span>';
    toast.lastChild.textContent = mensaje;
    container.appendChild(toast);

    setTimeout(() => {
      toast.classList.add('salir');
      toast.addEventListener('animationend', () => toast.remove());
    }, duracion);
  }

  return { mostrar };
})();

// ── Cuenta del cliente (la sesión vive en el servidor) ───────
// El servidor envía con cada página el estado inicial (CUENTA_INICIAL):
// si las cuentas están disponibles, quién inició sesión y su plan.
window.Cuenta = (function () {
  let estado = normalizar(window.CUENTA_INICIAL);

  function normalizar(e) {
    const r = e && typeof e === 'object' ? e : {};
    const cliente = r.cliente || null;
    if (cliente) cliente.foto = cliente.foto ? '/api/cuenta/foto' : null;
    return { disponible: !!r.disponible, cliente, suscripcion: r.suscripcion || null, equipo: !!r.equipo };
  }

  /** Vuelve a pedir el estado al servidor (después de cambiar datos o el plan). */
  async function recargar() {
    estado = normalizar(await Api.pedir('/api/cuenta'));
    document.documentElement.classList.toggle('con-sesion', !!estado.cliente);
    return estado;
  }

  async function salir(destino) {
    try { await Api.pedir('/api/cuenta/salida', { metodo: 'POST' }); } catch (e) { /* se redirige igual */ }
    window.location.href = destino || '/';
  }

  /** Enlace a iniciar sesión que regresa a la página actual. */
  function rutaLogin(pagina) {
    const actual = pagina || (window.location.pathname.replace(/^.*\//, '') + window.location.search);
    return '/login' + (actual ? '?retorno=' + encodeURIComponent(actual) : '');
  }

  return {
    disponible: () => estado.disponible,
    cliente: () => estado.cliente,
    equipo: () => estado.equipo,
    suscripcion: () => estado.suscripcion,
    recargar, salir, rutaLogin
  };
})();

// Mi cuenta: lleva al perfil si hay sesión de cliente, o al panel si es alguien del equipo
(function initMiCuenta() {
  const enlace = document.getElementById('btn-mi-cuenta');
  if (!enlace) return;
  if (Cuenta.cliente()) {
    enlace.href = '/perfil';
  } else if (Cuenta.equipo()) {
    enlace.href = '/admin';
    enlace.setAttribute('aria-label', 'Ir al panel');
    const etiqueta = enlace.querySelector('.etiqueta-cuenta');
    if (etiqueta) etiqueta.textContent = 'Panel';
  }
})();

// ── Carrito (en este navegador; precios del catálogo actual) ─
// Cada línea dice qué se compra: la presentación de un medicamento o el
// producto si es una variedad. El precio guardado es el de lista y solo
// sirve para mostrarlo: el servidor calcula el valor real al hacer el pedido.
window.Carrito = (function() {
  const KEY = 'drogueria_carrito';
  const MAX_CANTIDAD = 50;

  // Busca el producto en el catálogo publicado para usar su precio de hoy
  function enCatalogo(item) {
    if (typeof MEDICAMENTOS === 'undefined') return item;
    if (item.tipo === 'medicamento') {
      for (const m of MEDICAMENTOS) {
        const p = m.presentaciones.find(x => String(x.id) === String(item.presentacion));
        if (p) return { ...item, precio: p.precio, agotado: p.disponibilidad === 'agotado' || m.disponibilidad === 'agotado' };
      }
      return null;
    }
    const v = VARIEDADES.find(x => String(x.id) === String(item.producto));
    return v ? { ...item, precio: v.precio, agotado: v.disponibilidad === 'agotado' } : null;
  }

  function leer() {
    try {
      const items = JSON.parse(localStorage.getItem(KEY));
      if (!Array.isArray(items)) return [];
      // Descarta líneas de versiones anteriores o manipuladas
      return items.filter(i => i && (i.tipo === 'medicamento' || i.tipo === 'variedad')
        && /^[0-9]+$/.test(String(i.tipo === 'medicamento' ? i.presentacion : i.producto))
        && Number.isInteger(i.cantidad) && i.cantidad > 0 && i.cantidad <= MAX_CANTIDAD);
    } catch { return []; }
  }

  function obtener() {
    return leer().map(enCatalogo).filter(Boolean);
  }

  function guardar(items) {
    try {
      localStorage.setItem(KEY, JSON.stringify(items.map(({ clave, tipo, presentacion, producto, nombre, detalle, precio, cantidad }) =>
        ({ clave, tipo, presentacion, producto, nombre, detalle, precio, cantidad }))));
    } catch (e) { /* almacenamiento bloqueado: el carrito dura mientras la página esté abierta */ }
    actualizarBadge();
  }

  /** item: { tipo, presentacion | producto, nombre, detalle } */
  function agregar(item) {
    const clave = item.tipo === 'medicamento' ? 'p' + item.presentacion : 'v' + item.producto;
    const items = obtener();
    const idx = items.findIndex(i => i.clave === clave);
    if (idx !== -1) {
      if (items[idx].cantidad >= MAX_CANTIDAD) {
        Toast.mostrar(`Puedes pedir hasta ${MAX_CANTIDAD} unidades de cada producto`);
        return;
      }
      items[idx].cantidad += 1;
    } else {
      const nuevo = enCatalogo({ ...item, clave, cantidad: 1 });
      if (!nuevo) return;
      items.push(nuevo);
    }
    guardar(items);
    Toast.mostrar(`${item.nombre} se agregó al carrito`);
  }

  function remover(clave) {
    guardar(obtener().filter(i => i.clave !== clave));
  }

  function cambiarCantidad(clave, delta) {
    const items = obtener();
    const idx = items.findIndex(i => i.clave === clave);
    if (idx === -1) return;
    const nueva = Math.min(MAX_CANTIDAD, items[idx].cantidad + delta);
    if (nueva <= 0) items.splice(idx, 1);
    else items[idx].cantidad = nueva;
    guardar(items);
  }

  function vaciar() {
    guardar([]);
  }

  /** Precio que verá el cliente, con el descuento de su plan si lo tiene. */
  function precioCliente(item) {
    return Suscripcion.aplicar(item.precio);
  }

  function total() {
    return obtener().reduce((s, i) => s + precioCliente(i) * i.cantidad, 0);
  }

  function conteo() {
    return obtener().reduce((s, i) => s + i.cantidad, 0);
  }

  /** Lo que el servidor necesita para crear el pedido. */
  function lineasPedido() {
    return obtener().map(i => i.tipo === 'medicamento'
      ? { presentacion: Number(i.presentacion), cantidad: i.cantidad }
      : { producto: Number(i.producto), cantidad: i.cantidad });
  }

  function actualizarBadge() {
    const n = conteo();
    const badge = document.querySelector('.badge-carrito');
    if (badge) {
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
      fab.textContent = n;
      fab.toggleAttribute('data-cero', n === 0);
      fab.classList.remove('rebote');
      void fab.offsetWidth;
      fab.classList.add('rebote');
    });
  }

  // Inicializar badge al cargar
  document.addEventListener('DOMContentLoaded', actualizarBadge);

  return { obtener, agregar, remover, cambiarCantidad, vaciar, total, conteo, precioCliente, lineasPedido };
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

// ── Avatar navbar (cliente con sesión) ───────────────────────
// Si hay sesión: oculta "Regístrate" y cambia "Mi cuenta" por un menú
// con nombre, foto y opciones. Sin sesión, la barra queda igual.
window.initAvatar = function(opciones) {
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => _construirAvatar(opciones));
  } else {
    _construirAvatar(opciones);
  }
};

function _construirAvatar(opciones) {
  var opts      = opciones || {};
  var perfilUrl = opts.perfilUrl || '/perfil';
  var logoutUrl = opts.logoutUrl || '/';
  var cliente   = Cuenta.cliente();
  if (!cliente || document.getElementById('avatar-wrap')) return;

  var btnReg = document.getElementById('btn-registrate');
  if (btnReg) btnReg.style.display = 'none';
  var btnRegMenu = document.getElementById('btn-registrate-menu');
  if (btnRegMenu) btnRegMenu.style.display = 'none';

  var btnMiCuenta = document.getElementById('btn-mi-cuenta');
  if (!btnMiCuenta) return;
  btnMiCuenta.href = perfilUrl;

  var nombre  = cliente.nombre || 'Cliente';
  var correo  = cliente.correo || '';
  var esc     = Seguridad.esc;
  var inicial = String(nombre)[0].toUpperCase();
  var avatarHtml = cliente.foto
    ? '<img src="' + esc(cliente.foto) + '" alt="" style="width:100%;height:100%;border-radius:50%;object-fit:cover">'
    : esc(inicial);

  var wrap = document.createElement('div');
  wrap.className = 'avatar-navbar-wrap';
  wrap.id = 'avatar-wrap';
  wrap.innerHTML =
    '<button class="avatar-navbar-btn" id="avatar-btn" type="button" aria-label="Mi cuenta" aria-expanded="false" aria-controls="avatar-dropdown">' +
      '<div class="avatar-circulo">' + avatarHtml + '<span class="avatar-online-dot"></span></div>' +
      '<span class="avatar-nombre-corto">' + esc(String(nombre).split(' ')[0]) + '</span>' +
      '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true"><polyline points="6 9 12 15 18 9"/></svg>' +
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
        '<a class="avatar-drop-item" href="' + perfilUrl + '#perfil" role="menuitem">' +
          '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>' +
          'Mi perfil</a>' +
        '<a class="avatar-drop-item" href="' + perfilUrl + '#pedidos" role="menuitem">' +
          '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>' +
          'Mis pedidos</a>' +
        '<a class="avatar-drop-item" href="' + perfilUrl + '#favoritos" role="menuitem">' +
          '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>' +
          'Favoritos</a>' +
        '<div class="avatar-drop-divider"></div>' +
        '<button class="avatar-drop-item peligro" type="button" id="avatar-salir" role="menuitem">' +
          '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9"/></svg>' +
          'Cerrar sesión</button>' +
      '</div>' +
    '</div>';

  btnMiCuenta.parentNode.replaceChild(wrap, btnMiCuenta);
  document.getElementById('avatar-salir').addEventListener('click', function() { Cuenta.salir(logoutUrl); });

  document.addEventListener('click', function(e) {
    var w = document.getElementById('avatar-wrap');
    var d = document.getElementById('avatar-dropdown');
    var b = document.getElementById('avatar-btn');
    if (!w || !d) return;
    if (b && b.contains(e.target)) {
      var visible = d.classList.toggle('visible');
      b.setAttribute('aria-expanded', String(visible));
    } else if (!d.contains(e.target)) {
      d.classList.remove('visible');
      if (b) b.setAttribute('aria-expanded', 'false');
    }
  });

  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
      var d = document.getElementById('avatar-dropdown');
      var b = document.getElementById('avatar-btn');
      if (d) d.classList.remove('visible');
      if (b) b.setAttribute('aria-expanded', 'false');
    }
  });
}

// ── Cerrar sesión cliente ─────────────────────────────────────
window.cerrarSesionCliente = function(url) {
  Cuenta.salir(url);
};

// ── Protección de rutas: sin sesión, lleva a iniciar sesión y regresa ─
window.protegerRuta = function() {
  if (Cuenta.cliente()) return true;
  window.location.replace(Cuenta.rutaLogin());
  return false;
};

// ══════════════════════════════════════════════════════════════
//  DESCUENTO DEL PLAN
//  El plan vigente lo decide el servidor (aprobado por la droguería y
//  sin vencer). Uno cancelado conserva el descuento hasta su vencimiento.
//  Aquí solo se usa para mostrar precios: el cobro lo calcula el servidor.
// ══════════════════════════════════════════════════════════════
window.Suscripcion = (function() {
  /** Plan vigente del cliente con sesión, o null. */
  function obtener() {
    const s = Cuenta.suscripcion();
    if (!s) return null;
    const plan = Seguridad.planCanonico(s.plan);
    return {
      plan: plan ? plan.clave : String(s.plan || '').toLowerCase(),
      nombre: plan ? plan.nombre : s.plan,
      descuento: Number(s.descuento) || 0,
      inicio: s.inicio, vence: s.vence,
      estado: s.estado === 'cancelada' ? 'cancelada' : 'activa',
      domicilioGratis: !!s.domicilioGratis
    };
  }

  function descuento() {
    const sus = obtener();
    return sus ? sus.descuento : 0;
  }

  /** Precio con el descuento del plan, redondeado igual que en el servidor. */
  function aplicar(precio) {
    const pct = descuento();
    return pct ? Math.round(precio * (100 - pct) / 100) : precio;
  }

  /** Días que faltan para que venza (la fecha llega como AAAA-MM-DD). */
  function diasRestantes() {
    const sus = obtener();
    if (!sus || !sus.vence) return null;
    const [a, m, d] = sus.vence.split('-').map(Number);
    const hoy = new Date(); hoy.setHours(0, 0, 0, 0);
    return Math.round((new Date(a, m - 1, d) - hoy) / 86400000);
  }

  /** Aviso breve del descuento activo en cualquier página. */
  function mostrarBanner() {
    const sus = obtener();
    if (!sus || !sus.descuento || document.getElementById('sus-descuento-banner')) return;
    const banner  = document.createElement('aside');
    banner.setAttribute('aria-label', 'Tu plan de suscripción');
    banner.id     = 'sus-descuento-banner';
    banner.className = 'pildora-plan no-imprimir';
    banner.innerHTML =
      `<span class="punto punto--vivo"></span>
       <span>Plan ${Seguridad.esc(sus.nombre)}: <strong>${Seguridad.esc(sus.descuento)}% de descuento</strong></span>
       <a href="/perfil#suscripcion">Ver plan</a>`;
    document.body.appendChild(banner);
    // Es un aviso breve: no debe quedarse tapando contenido
    setTimeout(() => banner.classList.add('saliendo'), 7000);
    setTimeout(() => banner.remove(), 7600);
  }

  document.addEventListener('DOMContentLoaded', mostrarBanner);

  return { obtener, descuento, aplicar, diasRestantes, mostrarBanner };
})();

/** Fecha AAAA-MM-DD o AAAA-MM-DD HH:MM del servidor, escrita para leer. */
window.formatFecha = function(texto, conHora) {
  if (!texto) return '—';
  const [fecha, hora] = String(texto).split(' ');
  const [a, m, d] = fecha.split('-').map(Number);
  if (!a || !m || !d) return String(texto);
  const [h, min] = (hora || '0:0').split(':').map(Number);
  const f = new Date(a, m - 1, d, h || 0, min || 0);
  return f.toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' })
    + (conHora && hora ? ', ' + f.toLocaleTimeString('es-CO', { hour: 'numeric', minute: '2-digit' }) : '');
};
