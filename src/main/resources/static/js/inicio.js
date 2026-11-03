/* ============================================================
   inicio.js — Movimiento propio de la página de inicio: buscador que
   se escribe solo, parallax, carrusel de beneficios, pestañas y guía.
   ============================================================ */
window.Inicio = (function () {
  const conPuntero = window.matchMedia('(hover: hover) and (pointer: fine)').matches;

  // El ejemplo del buscador se escribe solo, y se detiene si la persona lo está usando
  function maquinaDeEscribir() {
    const entrada = document.getElementById('busqueda-inicio');
    if (!entrada || Experiencia.sinMovimiento) return;
    const frases = ['Acetaminofén…', 'Ibuprofeno…', 'Perfumes…'];
    let frase = 0, letra = 0, borrando = false;
    (function paso() {
      if (document.activeElement === entrada || entrada.value) {
        setTimeout(paso, 600);
        return;
      }
      const texto = frases[frase];
      letra += borrando ? -1 : 1;
      entrada.placeholder = texto.slice(0, letra);
      let espera = borrando ? 45 : 95;
      if (!borrando && letra === texto.length) { borrando = true; espera = 1500; }
      if (borrando && letra === 0) { borrando = false; frase = (frase + 1) % frases.length; espera = 300; }
      setTimeout(paso, espera);
    })();
  }

  // Los elementos de la ilustración siguen levemente al mouse
  function parallax() {
    const zona = document.getElementById('portada-ilustracion');
    const portada = document.querySelector('.portada');
    if (!zona || !portada || !conPuntero || Experiencia.sinMovimiento) return;
    let pendiente = false, x = 0, y = 0;
    portada.addEventListener('pointermove', (e) => {
      x = e.clientX / window.innerWidth - .5;
      y = e.clientY / window.innerHeight - .5;
      if (pendiente) return;
      pendiente = true;
      requestAnimationFrame(() => {
        pendiente = false;
        zona.style.setProperty('--mx', x.toFixed(3));
        zona.style.setProperty('--my', y.toFixed(3));
      });
    }, { passive: true });
  }

  // En el celular los beneficios se vuelven un carrusel infinito: se duplican para no dejar un salto
  function carruselBeneficios() {
    const pista = document.getElementById('hechos-pista');
    if (!pista) return;
    [...pista.children].forEach(hijo => {
      const copia = hijo.cloneNode(true);
      copia.classList.add('hecho-copia');
      copia.setAttribute('aria-hidden', 'true');
      pista.appendChild(copia);
    });
    pista.addEventListener('touchstart', () => pista.classList.add('detenido'), { passive: true });
    pista.addEventListener('touchend', () => pista.classList.remove('detenido'), { passive: true });
    pista.addEventListener('touchcancel', () => pista.classList.remove('detenido'), { passive: true });
  }

  function pestanas() {
    const botones = [...document.querySelectorAll('[data-explorar]')];
    const indicador = document.getElementById('tabs-indicador');
    if (!botones.length) return;

    function moverIndicador(boton) {
      if (!indicador) return;
      indicador.style.setProperty('--x', boton.offsetLeft + 'px');
      indicador.style.setProperty('--w', boton.offsetWidth);
    }
    function activar(boton) {
      botones.forEach(b => {
        const on = b === boton;
        b.classList.toggle('activo', on);
        b.setAttribute('aria-selected', on);
        b.tabIndex = on ? 0 : -1;
        const panel = document.getElementById(b.dataset.explorar);
        panel.hidden = !on;
        if (on) {
          panel.classList.remove('panel-entra');
          void panel.offsetWidth;
          panel.classList.add('panel-entra');
        }
      });
      moverIndicador(boton);
    }
    botones.forEach((b, i) => {
      b.addEventListener('click', () => activar(b));
      b.addEventListener('keydown', (e) => {
        const d = { ArrowRight: 1, ArrowLeft: -1 }[e.key];
        if (!d) return;
        e.preventDefault();
        const siguiente = botones[(i + d + botones.length) % botones.length];
        activar(siguiente);
        siguiente.focus();
      });
    });
    moverIndicador(botones[0]);
    window.addEventListener('resize', () => moverIndicador(botones.find(b => b.classList.contains('activo'))));
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(() => moverIndicador(botones.find(b => b.classList.contains('activo'))));
  }

  // La línea de la guía se dibuja con el scroll y cada paso entra cuando ella llega
  function guia() {
    const ruta = document.getElementById('guia-ruta');
    if (!ruta) return;
    const pasos = [...ruta.querySelectorAll('.paso-guia')];
    const fijar = (avance) => {
      ruta.style.setProperty('--avance', avance.toFixed(3));
      pasos.forEach((p, i) => p.classList.toggle('llego', avance >= (i / (pasos.length - 1)) * .92));
    };
    if (Experiencia.sinMovimiento) { fijar(1); return; }

    let enPantalla = false, pendiente = false, maximo = 0;
    function calcular() {
      pendiente = false;
      const r = ruta.getBoundingClientRect();
      const alto = window.innerHeight;
      const recorrido = Math.max(alto * .5, r.height * .9);
      const avance = Math.min(1, Math.max(0, (alto * .88 - r.top) / recorrido));
      // Lo que ya entró se queda: la guía no se deshace al subir
      maximo = Math.max(maximo, avance);
      fijar(maximo);
    }
    window.addEventListener('scroll', () => {
      if (enPantalla && !pendiente && maximo < 1) { pendiente = true; requestAnimationFrame(calcular); }
    }, { passive: true });
    Experiencia.observar(ruta, (e) => { enPantalla = e.isIntersecting; if (enPantalla) calcular(); });
    fijar(0);
  }

  function iniciar() {
    carruselBeneficios();
    maquinaDeEscribir();
    parallax();
    pestanas();
    guia();
  }

  return { iniciar };
})();
