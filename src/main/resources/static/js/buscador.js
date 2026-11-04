/* ============================================================
   buscador.js — Búsqueda de la tienda, con sugerencias al escribir.
   Busca en nombre, marca, principio activo, presentación, categoría
   y descripción, sin distinguir tildes ni mayúsculas.
   ============================================================ */
window.Buscador = (function () {
  const WHATSAPP = 'https://wa.me/573225065554';
  const esc = (v) => Seguridad.esc(v);
  const normalizar = (t) => String(t || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

  function indice() {
    const meds = MEDICAMENTOS.map(m => ({
      tipo: 'medicamento', p: m, nombre: normalizar(m.nombre), desc: normalizar(m.desc),
      principio: normalizar(m.principio), cat: normalizar(LABELS_CAT[m.cat]),
      marcas: [...new Set(m.presentaciones.map(q => q.marca))].map(x => [x, normalizar(x)]),
      formas: normalizar(m.presentaciones.map(q => `${q.tipo} ${q.conc}`).join(' ')),
      desde: Math.min(...m.presentaciones.map(q => q.precio))
    }));
    const vars = VARIEDADES.map(v => ({
      tipo: 'variedad', p: v, nombre: normalizar(v.nombre), desc: normalizar(v.desc),
      principio: '', cat: normalizar(LABELS_CAT[v.cat]), marcas: [], formas: normalizar(v.contenido), desde: v.precio
    }));
    return [...meds, ...vars];
  }

  // Devuelve cada coincidencia con su peso y con la razón, para explicarla en la sugerencia
  function buscar(texto, lista) {
    const q = normalizar(texto);
    if (!q) return [];
    const salida = [];
    (lista || indice()).forEach(e => {
      let peso = 0, razon = '';
      const marca = e.marcas.find(([, n]) => n.includes(q));
      if (e.nombre.startsWith(q)) peso = 100;
      else if (e.nombre.includes(q)) peso = 80;
      else if (e.principio.includes(q)) { peso = 70; razon = 'Principio activo: ' + e.p.principio; }
      else if (marca) { peso = 60; razon = 'Marca ' + marca[0]; }
      else if (e.cat.includes(q)) { peso = 40; razon = LABELS_CAT[e.p.cat]; }
      else if (e.formas.includes(q)) peso = 30;
      else if (e.desc.includes(q)) peso = 20;
      if (peso) salida.push({ ...e, peso, razon });
    });
    return salida.sort((a, b) => b.peso - a.peso || a.nombre.localeCompare(b.nombre));
  }

  const url = (e) => (e.tipo === 'medicamento' ? '/medicamentos' : '/variedades') + '?producto=' + e.p.id;
  const arte = (e) => e.tipo === 'medicamento' ? Arte.medicamento(e.p, e.p.presentaciones[0]) : Arte.variedad(e.p);

  function sinResultados(texto) {
    const mensaje = encodeURIComponent(`Hola, estoy buscando "${texto}". ¿Lo tienen disponible?`);
    return `<div class="sug-vacio">
      <strong>No encontramos "${esc(texto)}" en el catálogo</strong>
      <p>Revisa cómo está escrito o pregúntanos: te respondemos por WhatsApp.</p>
      <a class="btn btn--secundario btn--sm" href="${WHATSAPP}?text=${mensaje}" target="_blank" rel="noopener">${Experiencia.icono('whatsapp')} Preguntar por este producto</a>
    </div>`;
  }

  function iniciar(form) {
    const input = form.querySelector('input[type=search]');
    const lista = form.querySelector('[role=listbox]');
    const limpiar = form.querySelector('.buscador-limpiar');
    let opciones = [], activa = -1;

    function cerrar() {
      lista.hidden = true;
      input.setAttribute('aria-expanded', 'false');
      input.removeAttribute('aria-activedescendant');
      activa = -1;
    }

    function marcar(i) {
      opciones.forEach(o => o.setAttribute('aria-selected', 'false'));
      activa = i;
      if (i < 0) { input.removeAttribute('aria-activedescendant'); return; }
      opciones[i].setAttribute('aria-selected', 'true');
      input.setAttribute('aria-activedescendant', opciones[i].id);
      opciones[i].scrollIntoView({ block: 'nearest' });
    }

    function pintar() {
      const texto = input.value.trim();
      limpiar.hidden = !texto;
      if (!texto) { cerrar(); return; }
      const hallados = buscar(texto);
      const grupo = (titulo, items) => !items.length ? '' : `<div class="sug-grupo" role="group" aria-label="${titulo}">
        <div class="sug-titulo" aria-hidden="true">${titulo}</div>
        ${items.map(e => `<a class="sug-opcion" role="option" aria-selected="false" id="${input.id}-${e.tipo}-${e.p.id}" href="${url(e)}">
          <span class="sug-arte ${e.tipo === 'variedad' ? 'sug-arte--variedad' : ''}">${arte(e)}</span>
          <span class="sug-texto"><strong>${esc(e.p.nombre)}</strong><small>${esc(e.razon || LABELS_CAT[e.p.cat] || '')}</small></span>
          <span class="sug-precio num">${e.tipo === 'medicamento' ? 'Desde ' : ''}${formatPrecio(e.desde)}</span>
        </a>`).join('')}
      </div>`;
      if (!hallados.length) {
        lista.innerHTML = sinResultados(texto);
      } else {
        const meds = grupo('Medicamentos', hallados.filter(e => e.tipo === 'medicamento').slice(0, 5));
        const vars = grupo('Variedades', hallados.filter(e => e.tipo === 'variedad').slice(0, 5));
        // Primero el grupo que tiene la mejor coincidencia
        lista.innerHTML =
          (hallados[0].tipo === 'variedad' ? vars + meds : meds + vars) +
          `<a class="sug-todos" role="option" aria-selected="false" id="${input.id}-todos" href="${hallados[0].tipo === 'variedad' ? '/variedades' : '/medicamentos'}?q=${encodeURIComponent(texto)}">Ver todos los resultados de "${esc(texto)}" ${Experiencia.icono('flecha')}</a>`;
      }
      opciones = [...lista.querySelectorAll('[role=option]')];
      lista.hidden = false;
      input.setAttribute('aria-expanded', 'true');
      marcar(-1);
    }

    input.addEventListener('input', pintar);
    input.addEventListener('focus', () => { if (input.value.trim()) pintar(); });
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') { if (!lista.hidden) { e.stopPropagation(); cerrar(); } return; }
      if (lista.hidden || !opciones.length) return;
      if (e.key === 'ArrowDown') { e.preventDefault(); marcar((activa + 1) % opciones.length); }
      if (e.key === 'ArrowUp') { e.preventDefault(); marcar(activa <= 0 ? opciones.length - 1 : activa - 1); }
      if (e.key === 'Enter' && activa >= 0) { e.preventDefault(); opciones[activa].click(); }
    });
    limpiar.addEventListener('click', () => { input.value = ''; pintar(); input.focus(); });
    document.addEventListener('click', (e) => { if (!form.contains(e.target)) cerrar(); });
    form.addEventListener('focusout', (e) => { if (e.relatedTarget && !form.contains(e.relatedTarget)) cerrar(); });
    form.addEventListener('submit', (e) => {
      if (!input.value.trim()) { e.preventDefault(); input.focus(); }
    });
  }

  document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-buscador]').forEach(iniciar);

    // En el inicio el buscador grande manda. El de la barra aparece cuando aquel ya no se ve.
    const grande = document.getElementById('buscador-inicio');
    const barra = document.querySelector('.navbar');
    const pequeno = barra && barra.querySelector('.buscador-global');
    if (grande && pequeno) {
      // El de la barra toma el relevo justo cuando el grande llega hasta él
      let pendiente = false;
      const medir = () => {
        pendiente = false;
        const tope = Math.max(barra.getBoundingClientRect().bottom, pequeno.getBoundingClientRect().bottom);
        document.body.classList.toggle('buscador-grande-visible', grande.getBoundingClientRect().bottom > tope);
      };
      const pedir = () => { if (!pendiente) { pendiente = true; requestAnimationFrame(medir); } };
      window.addEventListener('scroll', pedir, { passive: true });
      window.addEventListener('resize', pedir, { passive: true });
      medir();
    }
  });

  return { buscar, normalizar, indice };
})();
