/* ============================================================
   global.js — Lógica compartida de todas las páginas
   Heurística Nielsen #1: Visibilidad del estado del sistema
   Heurística Nielsen #3: Control y libertad del usuario
   ============================================================ */

// ── Navbar sticky + hamburguesa ──────────────────────────────
(function initNavbar() {
  const navbar  = document.querySelector('.navbar');
  const btnMenu = document.querySelector('.btn-menu');
  const navLinks = document.querySelector('.navbar-nav');

  if (!navbar) return;

  // Nielsen #1: el navbar muestra visualmente en qué sección estás
  const currentPage = location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.nav-link').forEach(link => {
    const href = link.getAttribute('href') || '';
    if (href.includes(currentPage) || (currentPage === 'index.html' && href === '../index.html')) {
      link.classList.add('active');
    }
  });

  // Sombra al hacer scroll
  window.addEventListener('scroll', () => {
    navbar.classList.toggle('scrolled', window.scrollY > 10);
  }, { passive: true });

  // Menú hamburguesa
  if (btnMenu && navLinks) {
    btnMenu.addEventListener('click', () => {
      const abierto = navLinks.classList.toggle('abierto');
      btnMenu.setAttribute('aria-expanded', abierto);
    });

    // Cerrar al hacer clic fuera
    document.addEventListener('click', (e) => {
      if (!navbar.contains(e.target)) {
        navLinks.classList.remove('abierto');
        btnMenu.setAttribute('aria-expanded', 'false');
      }
    });
  }
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
    toast.textContent = mensaje;
    container.appendChild(toast);

    setTimeout(() => {
      toast.classList.add('salir');
      toast.addEventListener('animationend', () => toast.remove());
    }, duracion);
  }

  return { mostrar };
})();

// ── Animaciones de entrada al hacer scroll ───────────────────
(function initScrollAnimations() {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.style.animationPlayState = 'running';
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.12 });

  document.querySelectorAll('.animar').forEach(el => {
    el.style.animationPlayState = 'paused';
    observer.observe(el);
  });
})();

// ── Carrito global compartido (localStorage) ─────────────────
window.Carrito = (function() {
  const KEY = 'drogueria_carrito';

  function obtener() {
    try {
      return JSON.parse(localStorage.getItem(KEY)) || [];
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
    Toast.mostrar(`✓ ${producto.nombre} agregado al carrito`);
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
      badge.textContent = n;
      badge.style.display = n > 0 ? 'grid' : 'none';
    }
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
  return '$' + valor.toLocaleString('es-CO');
};

// ── Avatar navbar (cliente logueado) ────────────────────────
window.initAvatar = function(opciones) {
  // Esperar a que el DOM esté listo
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => _runAvatar(opciones));
  } else {
    _runAvatar(opciones);
  }
};

function _runAvatar(opciones) {
  // opciones: { perfilUrl, logoutUrl, homeUrl }
  const opts = opciones || {};
  const perfilUrl  = opts.perfilUrl  || 'perfil-cliente.html';
  const logoutUrl  = opts.logoutUrl  || '../index.html';

  const sesion = JSON.parse(sessionStorage.getItem('lth_cliente') || 'null');
  const btnMiCuenta = document.getElementById('btn-mi-cuenta');

  if (!btnMiCuenta) return;

  if (sesion && sesion.rol === 'cliente') {
    const inicial  = (sesion.nombre || 'U')[0].toUpperCase();
    const nombre   = sesion.nombre  || 'Usuario';
    const correo   = sesion.correo  || '';
    const fotoKey  = 'lth_foto_' + sesion.id;
    const fotoData = localStorage.getItem(fotoKey) || '';

    const avatarImg = fotoData
      ? `<img src="${fotoData}" alt="${nombre}">`
      : inicial;

    btnMiCuenta.outerHTML = `
      <div class="avatar-navbar-wrap" id="avatar-wrap">
        <button class="avatar-navbar-btn" id="avatar-btn" aria-label="Mi cuenta" aria-expanded="false">
          <div class="avatar-circulo" style="position:relative">
            ${avatarImg}
            <span class="avatar-online-dot"></span>
          </div>
          <span class="avatar-nombre-corto">${nombre.split(' ')[0]}</span>
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--gris-texto)" stroke-width="2.5">
            <polyline points="6 9 12 15 18 9"/>
          </svg>
        </button>

        <div class="avatar-dropdown" id="avatar-dropdown" role="menu">
          <div class="avatar-dropdown-header">
            <div class="avatar-grande-drop">
              ${avatarImg}
            </div>
            <div class="avatar-drop-info">
              <div class="avatar-drop-nombre">${nombre}</div>
              <div class="avatar-drop-correo">${correo}</div>
            </div>
          </div>
          <div class="avatar-dropdown-items">
            <a class="avatar-drop-item" href="${perfilUrl}" role="menuitem">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>
              </svg>
              Mi Perfil
            </a>
            <a class="avatar-drop-item" href="${perfilUrl}#pedidos" role="menuitem">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/>
              </svg>
              Mis Pedidos
            </a>
            <a class="avatar-drop-item" href="${perfilUrl}#favoritos" role="menuitem">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/>
              </svg>
              Favoritos
            </a>
            <div class="avatar-drop-divider"></div>
            <button class="avatar-drop-item peligro" onclick="cerrarSesionCliente('${logoutUrl}')" role="menuitem">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9"/>
              </svg>
              Cerrar sesión
            </button>
          </div>
        </div>
      </div>`;

    // Toggle dropdown
    document.addEventListener('click', (e) => {
      const wrap = document.getElementById('avatar-wrap');
      const drop = document.getElementById('avatar-dropdown');
      const btn  = document.getElementById('avatar-btn');
      if (!wrap || !drop) return;
      if (wrap.contains(e.target)) {
        const visible = drop.classList.toggle('visible');
        btn.setAttribute('aria-expanded', visible);
      } else {
        drop.classList.remove('visible');
        btn && btn.setAttribute('aria-expanded', 'false');
      }
    });

    // Teclado
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        const drop = document.getElementById('avatar-dropdown');
        const btn  = document.getElementById('avatar-btn');
        drop && drop.classList.remove('visible');
        btn  && btn.setAttribute('aria-expanded', 'false');
      }
    });

  } else {
    // No logueado — mostrar botón Mi cuenta normal
    // El botón ya está en el HTML con id="btn-mi-cuenta"
  }
} // fin _runAvatar

// ── Cerrar sesión cliente ─────────────────────────────────────

window.cerrarSesionCliente = function(url) {
  sessionStorage.removeItem('lth_cliente');
  window.location.href = url || '../index.html';
};
