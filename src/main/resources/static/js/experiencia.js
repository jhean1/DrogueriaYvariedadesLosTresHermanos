/* ============================================================
   experiencia.js — Movimiento y microinteracciones de la marca 3H.
   Solo JavaScript nativo. Si el usuario pide menos movimiento, el
   contenido aparece de inmediato y no se calcula ningún efecto.
   ============================================================ */
window.Experiencia = (function () {
  const sinMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const conPuntero = window.matchMedia('(hover: hover) and (pointer: fine)').matches;
  const icono = (n) => `<svg class="ic" aria-hidden="true"><use href="/img/iconos.svg#${n}"/></svg>`;

  // Un solo observador para toda la página: cada elemento registra qué hacer al entrar o salir
  const seguidos = new Map();
  const observador = 'IntersectionObserver' in window
    ? new IntersectionObserver((entradas) => {
        entradas.forEach(e => { const fn = seguidos.get(e.target); if (fn) fn(e); });
      }, { threshold: [0, 0.1] })
    : null;
  function observar(el, fn) {
    if (!observador) { fn({ isIntersecting: true, intersectionRatio: 1, target: el, boundingClientRect: el.getBoundingClientRect() }); return; }
    seguidos.set(el, fn);
    observador.observe(el);
  }
  function dejar(el) {
    seguidos.delete(el);
    if (observador) observador.unobserve(el);
  }

  // Revelado progresivo: --i escalona a los hermanos marcados con data-escalonar
  function revelar(raiz = document) {
    raiz.querySelectorAll('[data-escalonar]').forEach(grupo => {
      [...grupo.children].forEach((hijo, i) => {
        hijo.classList.add('revelar');
        hijo.style.setProperty('--i', Math.min(i, 8));
      });
    });
    raiz.querySelectorAll('.revelar:not(.visible)').forEach(el => {
      if (sinMovimiento || !observador) { el.classList.add('visible'); return; }
      observar(el, (e) => {
        const alto = e.boundingClientRect.height > window.innerHeight * .8;
        if (e.intersectionRatio >= 0.1 || (alto && e.isIntersecting)) {
          e.target.classList.add('visible');
          dejar(e.target);
        }
      });
    });
  }

  // Contadores que suben desde 0; el valor final ya está en el HTML para quien no tenga JS
  function animarContador(el) {
    const fin = Number(el.dataset.contar);
    if (!Number.isFinite(fin)) return;
    if (sinMovimiento) { el.textContent = fin.toLocaleString('es-CO'); return; }
    const inicio = performance.now();
    const duracion = 900;
    (function paso(ahora) {
      const t = Math.min((ahora - inicio) / duracion, 1);
      el.textContent = Math.round(fin * (1 - Math.pow(1 - t, 3))).toLocaleString('es-CO');
      if (t < 1) requestAnimationFrame(paso);
    })(inicio);
  }

  function contadores(raiz = document) {
    raiz.querySelectorAll('[data-contar]:not([data-contado])').forEach(el => {
      observar(el, (e) => {
        if (!e.isIntersecting) return;
        e.target.dataset.contado = '1';
        animarContador(e.target);
        dejar(e.target);
      });
    });
  }

  // Las animaciones en bucle se detienen cuando su sección no está en pantalla
  function bucles() {
    document.querySelectorAll('[data-loop]').forEach(el => {
      observar(el, (e) => e.target.classList.toggle('pausado', !e.isIntersecting));
    });
  }

  // Barra de progreso y botón de volver arriba: comparten un solo cálculo por cuadro
  function progresoScroll() {
    const barra = document.querySelector('.progreso-scroll');
    const subir = document.getElementById('flota-subir');
    if (!barra && !subir) return;
    const anillo = subir && subir.querySelector('circle');
    const largo = 2 * Math.PI * 22;
    let pendiente = false;
    function pintar() {
      pendiente = false;
      const total = document.documentElement.scrollHeight - window.innerHeight;
      const avance = total > 0 ? Math.min(1, window.scrollY / total) : 0;
      if (barra) barra.style.setProperty('--avance', avance.toFixed(4));
      if (subir) {
        subir.classList.toggle('visible', window.scrollY > 500);
        anillo.style.strokeDashoffset = (largo * (1 - avance)).toFixed(1);
      }
    }
    window.addEventListener('scroll', () => {
      if (!pendiente) { pendiente = true; requestAnimationFrame(pintar); }
    }, { passive: true });
    pintar();
  }

  // Botones flotantes de la tienda. El panel y el acceso del equipo no los llevan.
  function flotantes() {
    if (!document.querySelector('.navbar')) return;
    const caja = document.createElement('aside');
    caja.className = 'flotantes no-imprimir';
    caja.setAttribute('aria-label', 'Accesos rápidos');
    caja.innerHTML = `
      <button class="flota-subir" id="flota-subir" type="button" aria-label="Volver arriba">
        <svg viewBox="0 0 48 48" aria-hidden="true"><circle cx="24" cy="24" r="22" stroke-dasharray="${(2 * Math.PI * 22).toFixed(1)}"/></svg>
        ${icono('flecha')}
      </button>
      <a class="flota-wa" href="https://wa.me/573225065554" target="_blank" rel="noopener" aria-label="Escribir por WhatsApp">
        <span class="flota-aviso" aria-hidden="true">¿Te ayudamos?</span>
        <span class="flota-pulso" aria-hidden="true"></span><span class="flota-pulso flota-pulso--2" aria-hidden="true"></span>
        ${icono('whatsapp')}
      </a>`;
    document.body.appendChild(caja);
    caja.querySelector('#flota-subir').addEventListener('click', () => window.scrollTo({ top: 0, behavior: sinMovimiento ? 'auto' : 'smooth' }));
    const aviso = caja.querySelector('.flota-aviso');
    setTimeout(() => aviso.classList.add('visible'), 5000);
    setTimeout(() => aviso.classList.remove('visible'), 10000);
  }

  // Tilt leve de las tarjetas, solo con mouse
  function tilt() {
    if (!conPuntero || sinMovimiento) return;
    let pendiente = false, ultimo = null;
    document.addEventListener('pointermove', (e) => {
      const tarjeta = e.target.closest && e.target.closest('.med-card, .var-card');
      if (ultimo && ultimo !== tarjeta) { ultimo.style.removeProperty('--rx'); ultimo.style.removeProperty('--ry'); }
      ultimo = tarjeta;
      if (!tarjeta || pendiente) return;
      pendiente = true;
      requestAnimationFrame(() => {
        pendiente = false;
        const r = tarjeta.getBoundingClientRect();
        tarjeta.style.setProperty('--ry', (((e.clientX - r.left) / r.width - .5) * 3).toFixed(2) + 'deg');
        tarjeta.style.setProperty('--rx', ((.5 - (e.clientY - r.top) / r.height) * 3).toFixed(2) + 'deg');
      });
    }, { passive: true });
  }

  // Onda al tocar los botones de compra
  function ondas() {
    if (sinMovimiento) return;
    document.addEventListener('pointerdown', (e) => {
      const boton = e.target.closest && e.target.closest('.btn-elegir, .btn-agregar, .btn-agregar-var');
      if (!boton || boton.disabled) return;
      const r = boton.getBoundingClientRect();
      const onda = document.createElement('span');
      onda.className = 'onda';
      onda.style.left = (e.clientX - r.left - 6) + 'px';
      onda.style.top = (e.clientY - r.top - 6) + 'px';
      onda.addEventListener('animationend', () => onda.remove());
      boton.appendChild(onda);
    }, { passive: true });
  }

  // Los elementos que actúan como botón también responden al teclado
  function teclado() {
    document.addEventListener('keydown', (e) => {
      if (e.key !== 'Enter' && e.key !== ' ') return;
      const el = e.target;
      if (el.matches && el.matches('[role="button"]') && el.tagName !== 'BUTTON' && el.tagName !== 'A') {
        e.preventDefault();
        el.click();
      }
    });
  }

  // Mantiene el foco dentro de un diálogo abierto y lo devuelve al cerrarlo
  let ultimoFoco = null;
  function atrapar(contenedor) {
    ultimoFoco = document.activeElement;
    const foco = contenedor.querySelector('button, a[href], input, select, textarea');
    foco && foco.focus();
    contenedor._trampa = (e) => {
      if (e.key !== 'Tab') return;
      const items = [...contenedor.querySelectorAll('button:not([disabled]), a[href], input:not([disabled]), select, textarea')];
      if (!items.length) return;
      const [a, z] = [items[0], items[items.length - 1]];
      if (e.shiftKey && document.activeElement === a) { e.preventDefault(); z.focus(); }
      else if (!e.shiftKey && document.activeElement === z) { e.preventDefault(); a.focus(); }
    };
    contenedor.addEventListener('keydown', contenedor._trampa);
  }
  function soltar(contenedor) {
    contenedor._trampa && contenedor.removeEventListener('keydown', contenedor._trampa);
    ultimoFoco && ultimoFoco.focus && ultimoFoco.focus();
    ultimoFoco = null;
  }

  // Abre y cierra diálogos de contenido legal
  function legales() {
    const velo = document.getElementById('modal-terminos');
    if (!velo || !window.TERMINOS_LEGALES) return;
    window.abrirTerminos = (tipo) => {
      const data = window.TERMINOS_LEGALES[tipo];
      if (!data) return;
      document.getElementById('modal-terminos-titulo').textContent = data.titulo;
      document.getElementById('modal-terminos-body').innerHTML = data.html;
      velo.classList.add('visible');
      atrapar(velo);
    };
    window.cerrarTerminos = () => { velo.classList.remove('visible'); soltar(velo); };
    velo.addEventListener('click', (e) => { if (e.target === velo) window.cerrarTerminos(); });
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && velo.classList.contains('visible')) window.cerrarTerminos();
    });
  }

  document.addEventListener('DOMContentLoaded', () => {
    flotantes();
    revelar();
    contadores();
    bucles();
    progresoScroll();
    tilt();
    ondas();
    teclado();
    legales();
  });

  // Confirmación antes de una acción importante. Devuelve una promesa con la respuesta.
  function confirmar({ titulo, texto, accion = 'Confirmar', peligro = false }) {
    return new Promise((resolver) => {
      const d = document.createElement('dialog');
      d.className = 'dialogo dialogo-confirmar';
      d.setAttribute('aria-labelledby', 'confirmar-titulo');
      d.innerHTML = `<form method="dialog" class="dialogo-forma">
        <div class="modal-cabeza"><h2 id="confirmar-titulo"></h2></div>
        <div class="modal-cuerpo"><p></p></div>
        <div class="modal-pie">
          <button class="btn btn--fantasma" value="no">Cancelar</button>
          <button class="btn ${peligro ? 'btn--peligro' : 'btn--primario'}" value="si"></button>
        </div></form>`;
      d.querySelector('h2').textContent = titulo;
      d.querySelector('p').textContent = texto;
      d.querySelector('button[value=si]').textContent = accion;
      document.body.appendChild(d);
      d.addEventListener('close', () => { resolver(d.returnValue === 'si'); d.remove(); });
      d.showModal();
    });
  }

  // Cualquier formulario o botón con data-confirmar pide confirmación antes de continuar
  function confirmaciones() {
    document.addEventListener('submit', (e) => {
      const form = e.target;
      const boton = e.submitter && e.submitter.dataset.confirmar ? e.submitter : form;
      if (!boton.dataset || !boton.dataset.confirmar || form.dataset.confirmado) return;
      e.preventDefault();
      confirmar({
        titulo: boton.dataset.confirmarTitulo || '¿Confirmas esta acción?',
        texto: boton.dataset.confirmar,
        accion: boton.dataset.confirmarAccion || 'Confirmar',
        peligro: boton.dataset.confirmarPeligro !== undefined
      }).then((si) => {
        if (!si) return;
        form.dataset.confirmado = '1';
        if (e.submitter) form.requestSubmit(e.submitter); else form.requestSubmit();
      });
    });
  }

  document.addEventListener('DOMContentLoaded', confirmaciones);

  return { revelar, contadores, observar, dejar, atrapar, soltar, icono, sinMovimiento, confirmar };
})();
