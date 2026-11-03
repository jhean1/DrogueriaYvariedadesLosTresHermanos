// Modo claro y oscuro. Se carga en <head>, antes de pintar, para que nunca parpadee.
(function () {
  const CLAVE = 'lth_tema';
  const COLORES = { claro: '#F7F8FA', oscuro: '#0E1420' };
  const raiz = document.documentElement;

  function guardado() {
    try { return localStorage.getItem(CLAVE); } catch (e) { return null; }
  }

  function aplicar(tema) {
    raiz.dataset.tema = tema;
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.content = COLORES[tema];
    document.querySelectorAll('[data-tema-boton]').forEach(b => {
      b.setAttribute('aria-label', tema === 'oscuro' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro');
      b.setAttribute('aria-pressed', String(tema === 'oscuro'));
    });
  }

  const sistema = window.matchMedia('(prefers-color-scheme: dark)');
  const preferido = guardado();
  raiz.dataset.tema = preferido === 'oscuro' || preferido === 'claro' ? preferido : (sistema.matches ? 'oscuro' : 'claro');

  // Sin una elección guardada, se sigue al sistema también en vivo
  sistema.addEventListener('change', (e) => {
    if (!guardado()) aplicar(e.matches ? 'oscuro' : 'claro');
  });

  document.addEventListener('click', (e) => {
    const boton = e.target.closest && e.target.closest('[data-tema-boton]');
    if (!boton) return;
    const nuevo = raiz.dataset.tema === 'oscuro' ? 'claro' : 'oscuro';
    raiz.classList.add('tema-cambiando');
    boton.classList.add('girando');
    aplicar(nuevo);
    try { localStorage.setItem(CLAVE, nuevo); } catch (err) {}
    setTimeout(() => { raiz.classList.remove('tema-cambiando'); boton.classList.remove('girando'); }, 320);
  });

  document.addEventListener('DOMContentLoaded', () => aplicar(raiz.dataset.tema));
})();
