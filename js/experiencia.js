/* ============================================================
   experiencia.js — Movimiento y microinteracciones de la marca 3H.
   Solo JavaScript nativo. Si el usuario pide menos movimiento, el
   contenido aparece de inmediato y no se calcula ningún efecto.
   ============================================================ */
window.Experiencia = (function () {
  const sinMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const icono = (n) => `<svg class="ic" aria-hidden="true"><use href="/DrogueriaYvariedadesLosTresHermanos/img/iconos.svg#${n}"/></svg>`;

  // Revelado progresivo: --i escalona a los hermanos marcados con data-escalonar
  const observadorRevelar = 'IntersectionObserver' in window
    ? new IntersectionObserver((entradas, obs) => {
        entradas.forEach(e => {
          if (e.isIntersecting) { e.target.classList.add('visible'); obs.unobserve(e.target); }
        });
      }, { threshold: 0.1, rootMargin: '0px 0px -6% 0px' })
    : null;

  function revelar(raiz = document) {
    raiz.querySelectorAll('[data-escalonar]').forEach(grupo => {
      [...grupo.children].forEach((hijo, i) => {
        hijo.classList.add('revelar');
        hijo.style.setProperty('--i', Math.min(i, 8));
      });
    });
    raiz.querySelectorAll('.revelar:not(.visible)').forEach(el => {
      if (observadorRevelar && !sinMovimiento) observadorRevelar.observe(el);
      else el.classList.add('visible');
    });
  }

  // Contadores con desaceleración; el valor final ya está en el HTML para quien no tenga JS
  function animarContador(el) {
    const fin = Number(el.dataset.contar);
    if (!Number.isFinite(fin)) return;
    if (sinMovimiento) { el.textContent = fin.toLocaleString('es-CO'); return; }
    const inicio = performance.now();
    const duracion = 1300;
    (function paso(ahora) {
      const t = Math.min((ahora - inicio) / duracion, 1);
      el.textContent = Math.round(fin * (1 - Math.pow(1 - t, 3))).toLocaleString('es-CO');
      if (t < 1) requestAnimationFrame(paso);
    })(inicio);
  }

  function contadores(raiz = document) {
    const lista = raiz.querySelectorAll('[data-contar]:not([data-contado])');
    if (!('IntersectionObserver' in window)) { lista.forEach(animarContador); return; }
    const obs = new IntersectionObserver((entradas) => {
      entradas.forEach(e => {
        if (e.isIntersecting) {
          e.target.dataset.contado = '1';
          animarContador(e.target);
          obs.unobserve(e.target);
        }
      });
    }, { threshold: 0.5 });
    lista.forEach(el => obs.observe(el));
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
    revelar();
    contadores();
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

  return { revelar, contadores, atrapar, soltar, icono, sinMovimiento, confirmar };
})();
