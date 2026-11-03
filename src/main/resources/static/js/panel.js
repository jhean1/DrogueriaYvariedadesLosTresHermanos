/* ============================================================
   panel.js — Marco del panel: menú lateral, menús flotantes y
   búsqueda global. Solo muestra datos que ya existen en el navegador.
   ============================================================ */
window.Panel = (function () {
  const $ = (id) => document.getElementById(id);
  const esc = Seguridad.esc;
  const leer = (clave) => {
    try {
      const datos = JSON.parse(localStorage.getItem(clave));
      return Array.isArray(datos) ? datos : [];
    } catch (e) { return []; }
  };

  function menuLateral() {
    const lateral = $('lateral'), velo = $('lateral-velo'), abrir = $('btn-abrir-lateral');
    if (!lateral || !abrir) return;
    const fijar = (abierto) => {
      lateral.classList.toggle('abierto', abierto);
      velo.classList.toggle('visible', abierto);
      abrir.setAttribute('aria-expanded', String(abierto));
      document.body.classList.toggle('sin-scroll', abierto);
    };
    abrir.addEventListener('click', () => fijar(true));
    velo.addEventListener('click', () => fijar(false));
    $('btn-cerrar-lateral')?.addEventListener('click', () => fijar(false));
    lateral.addEventListener('click', (e) => { if (e.target.closest('a')) fijar(false); });
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') fijar(false); });
  }

  function menuFlotante(botonId, menuId) {
    const boton = $(botonId), menu = $(menuId);
    if (!boton || !menu) return;
    const cerrar = () => { menu.hidden = true; boton.setAttribute('aria-expanded', 'false'); };
    boton.addEventListener('click', (e) => {
      e.stopPropagation();
      const abrir = menu.hidden;
      document.querySelectorAll('.menu-flotante').forEach(m => { m.hidden = true; });
      menu.hidden = !abrir;
      boton.setAttribute('aria-expanded', String(abrir));
    });
    document.addEventListener('click', (e) => { if (!menu.contains(e.target)) cerrar(); });
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') cerrar(); });
  }

  // Los avisos reflejan únicamente solicitudes de pago pendientes reales
  function pintarAvisos() {
    const menu = $('menu-avisos');
    if (!menu) return;
    const todas = leer('lth_solicitudes').filter(s => s.estado === 'pendiente');
    const badge = $('badge-pendientes');
    if (badge) { badge.textContent = todas.length; badge.hidden = !todas.length; }
    const pendientes = todas.slice(-5).reverse();
    $('avisos-punto').hidden = !pendientes.length;
    menu.innerHTML = `<div class="menu-titulo">Avisos</div>` + (pendientes.length
      ? pendientes.map(s => `<a class="menu-opcion" href="/admin#solicitudes">
          ${Experiencia.icono('tarjeta')}<span><strong>${esc(s.clienteNombre || 'Cliente')}</strong><small>Solicitud pendiente, plan ${esc(Seguridad.planCanonico(s.plan)?.nombre || s.plan)}</small></span></a>`).join('')
      : `<div class="vacio vacio--compacto"><div class="vacio-icono">${Experiencia.icono('campana')}</div><p>No tienes avisos pendientes.</p></div>`);
  }

  function busqueda() {
    const entrada = $('busqueda-global'), caja = $('resultados-busqueda');
    if (!entrada || !caja) return;
    const permisos = (document.body.dataset.permisos || '').split(' ');
    entrada.addEventListener('input', () => {
      const q = entrada.value.trim().toLowerCase();
      if (!q) { caja.hidden = true; return; }
      const coincide = (...v) => v.some(x => String(x || '').toLowerCase().includes(q));
      const clientes = !document.body.dataset.permisos || permisos.includes('customers.view')
        ? leer('lth_clientes').filter(c => coincide(c.nombre, c.correo, c.telefono)).slice(0, 5) : [];
      const solicitudes = !document.body.dataset.permisos || permisos.includes('subscriptions.view')
        ? leer('lth_solicitudes').filter(s => coincide(s.clienteNombre, s.clienteCorreo, s.referencia, s.id)).slice(0, 5) : [];
      const filas = [
        ...clientes.map(c => `<a class="menu-opcion" href="/admin#clientes">${Experiencia.icono('usuario')}<span><strong>${esc(c.nombre)}</strong><small>${esc(c.correo)}</small></span></a>`),
        ...solicitudes.map(s => `<a class="menu-opcion" href="/admin#solicitudes">${Experiencia.icono('tarjeta')}<span><strong>${esc(s.clienteNombre)}</strong><small>${esc(s.estado)}, plan ${esc(Seguridad.planCanonico(s.plan)?.nombre || s.plan)}</small></span></a>`)
      ];
      caja.innerHTML = filas.join('') || `<div class="menu-titulo">Sin resultados para "${esc(q)}"</div>`;
      caja.hidden = false;
    });
    document.addEventListener('click', (e) => {
      if (!$('buscador-panel').contains(e.target)) caja.hidden = true;
      if (e.target.closest('#resultados-busqueda a')) { caja.hidden = true; entrada.value = ''; }
    });
  }

  // Paginación de cualquier tabla ya pintada. filtrar() recibe una condición por fila.
  function paginar(tablaOId, tamano = 10) {
    const tabla = typeof tablaOId === 'string' ? $(tablaOId) : tablaOId;
    if (!tabla) return null;
    const filas = [...tabla.tBodies[0].rows];
    const contenedor = tabla.closest('.tarjeta') || tabla.parentElement;
    let pie = (tabla.id && contenedor.querySelector(`[data-paginador="${tabla.id}"]`)) || tabla.closest('[id$="-wrap"]')?.querySelector('.tabla-pie');
    if (!pie) {
      pie = document.createElement('div');
      pie.className = 'tabla-pie';
      tabla.closest('.tabla-marco').insertAdjacentElement('afterend', pie);
    }
    let visibles = filas, pagina = 0;

    function pintar() {
      const total = Math.max(1, Math.ceil(visibles.length / tamano));
      pagina = Math.min(pagina, total - 1);
      filas.forEach(f => { f.hidden = true; });
      const desde = pagina * tamano;
      visibles.slice(desde, desde + tamano).forEach(f => { f.hidden = false; });
      tabla.closest('.tabla-marco').hidden = !visibles.length;
      pie.hidden = visibles.length <= tamano;
      pie.innerHTML = `<span class="num">${desde + 1}–${Math.min(desde + tamano, visibles.length)} de ${visibles.length}</span>
        <div class="paginas">
          <button type="button" class="btn btn--secundario btn--sm" data-p="-1" ${pagina === 0 ? 'disabled' : ''}>Anterior</button>
          <button type="button" class="btn btn--secundario btn--sm" data-p="1" ${pagina >= total - 1 ? 'disabled' : ''}>Siguiente</button>
        </div>`;
    }
    pie.onclick = (e) => {
      const b = e.target.closest('[data-p]');
      if (!b) return;
      pagina += +b.dataset.p;
      pintar();
    };
    pintar();
    return {
      filtrar(condicion) { visibles = filas.filter(condicion); pagina = 0; pintar(); return visibles.length; }
    };
  }

  // Mensaje breve de resultado de una acción
  function aviso(texto, error) {
    let caja = document.querySelector('.toast-wrap');
    if (!caja) {
      caja = document.createElement('div');
      caja.className = 'toast-wrap';
      caja.setAttribute('role', 'status');
      document.body.appendChild(caja);
    }
    const t = document.createElement('div');
    t.className = 'toast' + (error ? ' err' : '');
    t.innerHTML = Experiencia.icono(error ? 'alerta' : 'check') + '<span></span>';
    t.lastChild.textContent = texto;
    caja.appendChild(t);
    setTimeout(() => { t.classList.add('salir'); t.addEventListener('animationend', () => t.remove()); }, 3200);
  }

  document.addEventListener('DOMContentLoaded', () => {
    menuLateral();
    menuFlotante('btn-avisos', 'menu-avisos');
    menuFlotante('btn-usuario', 'menu-usuario');
    pintarAvisos();
    busqueda();
  });

  return { pintarAvisos, leer, paginar, aviso };
})();
