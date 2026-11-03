/* ============================================================
   ficha.js — Tarjetas de producto, ficha de detalle y agregar al carrito.
   Solo muestra lo que está registrado en el catálogo: si un dato no
   existe (principio activo, disponibilidad), no se inventa ni se rellena.
   ============================================================ */
window.Ficha = (function () {
  const WHATSAPP = 'https://wa.me/573225065554';
  const esc = (v) => Seguridad.esc(v);
  const ic = (n) => Experiencia.icono(n);
  const seleccion = {};
  let dialogo = null;

  const buscarMed = (id) => MEDICAMENTOS.find(m => m.id === id);
  const buscarVar = (id) => VARIEDADES.find(v => v.id === id);
  const desde = (m) => Math.min(...m.presentaciones.map(p => p.precio));

  function precioHTML(precio, clase) {
    const pct = window.Suscripcion ? Suscripcion.descuento() : 0;
    if (!pct) return `<span class="${clase} num">${formatPrecio(precio)}</span>`;
    return `<span class="${clase}-original num">${formatPrecio(precio)}</span>
      <span class="${clase} con-descuento num">${formatPrecio(Math.round(precio * (1 - pct / 100)))}</span>`;
  }

  function etiquetas(p) {
    return [
      p.formula ? `<span class="insignia insignia--aviso">${ic('documento')} Requiere fórmula médica</span>` : '',
      p.disponibilidad === 'disponible' ? `<span class="insignia insignia--exito"><span class="punto"></span>Disponible</span>` : '',
      p.disponibilidad === 'agotado' ? `<span class="insignia insignia--peligro">Agotado</span>` : ''
    ].join('');
  }

  // ── Tarjetas ──────────────────────────────────────────────
  function tarjetaMedicamento(m, i) {
    const n = m.presentaciones.length;
    const conc = [...new Set(m.presentaciones.map(p => p.conc))];
    return `<article class="med-card" data-med="${m.id}" style="--i:${Math.min(i, 8)}">
      <button type="button" class="med-arte" data-ficha="medicamento" data-id="${m.id}" aria-label="Ver detalles de ${esc(m.nombre)}" tabindex="-1">${Arte.medicamento(m, m.presentaciones[0])}</button>
      <div class="med-body">
        <span class="med-categoria">${esc(LABELS_CAT[m.cat] || m.cat)}</span>
        <h2 class="med-nombre"><button type="button" data-ficha="medicamento" data-id="${m.id}">${esc(m.nombre)}</button></h2>
        <p class="med-desc">${esc(m.desc)}</p>
        <p class="med-formas">${n} presentacion${n === 1 ? '' : 'es'} · ${esc(conc.slice(0, 2).join(', '))}${conc.length > 2 ? '…' : ''}</p>
        ${m.formula || m.disponibilidad !== 'sin-dato' ? `<div class="med-etiquetas">${etiquetas(m)}</div>` : ''}
      </div>
      <div class="med-footer">
        <div class="med-precio-wrap"><span class="med-precio-unit">Desde</span>${precioHTML(desde(m), 'med-precio')}</div>
        <button type="button" class="btn btn--primario btn--sm btn-elegir" data-ficha="medicamento" data-id="${m.id}" aria-label="Elegir presentación de ${esc(m.nombre)}">Elegir</button>
      </div>
    </article>`;
  }

  function tarjetaVariedad(v, i) {
    const agotado = v.disponibilidad === 'agotado';
    return `<article class="var-card var-card--${esc(v.cat)}" style="--i:${Math.min(i, 8)}">
      <button type="button" class="var-arte" data-ficha="variedad" data-id="${v.id}" aria-label="Ver detalles de ${esc(v.nombre)}" tabindex="-1">${Arte.variedad(v)}</button>
      <div class="var-body">
        <span class="var-cat">${esc(LABELS_CAT[v.cat] || v.cat)}</span>
        <h2 class="var-nombre"><button type="button" data-ficha="variedad" data-id="${v.id}">${esc(v.nombre)}</button></h2>
        ${v.contenido ? `<p class="med-formas">${esc(v.contenido)}</p>` : ''}
        ${v.disponibilidad !== 'sin-dato' ? `<div class="med-etiquetas">${etiquetas(v)}</div>` : ''}
      </div>
      <div class="var-footer">
        <div class="var-precios">${precioHTML(v.precio, 'var-precio')}</div>
        <button type="button" class="btn btn--secundario btn--sm btn-agregar-var" data-id="${v.id}" ${agotado ? 'disabled' : ''} aria-label="Agregar ${esc(v.nombre)} al carrito">${ic('mas')} Agregar</button>
      </div>
    </article>`;
  }

  // ── Presentación elegida ──────────────────────────────────
  function actual(med) {
    const p0 = med.presentaciones[0];
    const e = seleccion[med.id] || (seleccion[med.id] = { tipo: p0.tipo, conc: p0.conc, marca: p0.marca });
    return med.presentaciones.find(p => p.tipo === e.tipo && p.conc === e.conc && p.marca === e.marca) || p0;
  }

  function elegir(med, grupo, valor) {
    const e = seleccion[med.id];
    e[grupo] = valor;
    // Si la combinación ya no existe, se conserva lo elegido hasta donde sea posible
    const concs = [...new Set(med.presentaciones.filter(p => p.tipo === e.tipo).map(p => p.conc))];
    if (!concs.includes(e.conc)) e.conc = concs[0];
    const marcas = med.presentaciones.filter(p => p.tipo === e.tipo && p.conc === e.conc).map(p => p.marca);
    if (!marcas.includes(e.marca)) e.marca = marcas[0];
  }

  const fila = (titulo, grupo, valores, activo) => `<fieldset class="selector">
    <legend class="selector-label">${titulo}</legend>
    <div class="selector-row" data-grupo="${grupo}">
      ${valores.map(v => `<button type="button" class="sel-btn ${v === activo ? 'activo' : ''}" aria-pressed="${v === activo}" data-${grupo}="${esc(v)}">${esc(v)}</button>`).join('')}
    </div>
  </fieldset>`;

  function pintarMedicamento(med) {
    const pres = actual(med), e = seleccion[med.id];
    const agotado = med.disponibilidad === 'agotado';
    const tipos = [...new Set(med.presentaciones.map(p => p.tipo))];
    const concs = [...new Set(med.presentaciones.filter(p => p.tipo === e.tipo).map(p => p.conc))];
    const marcas = med.presentaciones.filter(p => p.tipo === e.tipo && p.conc === e.conc).map(p => p.marca);
    const pregunta = encodeURIComponent(`Hola, quiero preguntar por ${med.nombre}: ${pres.tipo}, ${pres.conc}, ${pres.marca}.`);
    return `<div class="ficha-arte">${Arte.medicamento(med, pres)}</div>
      <div class="ficha-info">
        <span class="med-categoria">${esc(LABELS_CAT[med.cat] || med.cat)}</span>
        <h2 id="ficha-titulo">${esc(med.nombre)}</h2>
        ${med.desc ? `<p class="ficha-desc">${esc(med.desc)}</p>` : ''}
        ${med.principio ? `<p class="ficha-dato"><span>Principio activo</span>${esc(med.principio)}</p>` : ''}
        <div class="med-etiquetas">${etiquetas(med)}</div>
        ${fila('Presentación', 'tipo', tipos, e.tipo)}
        ${fila('Concentración', 'conc', concs, e.conc)}
        ${fila('Marca', 'marca', marcas, e.marca)}
        <div class="ficha-compra">
          <div class="ficha-precio med-precio-wrap">${precioHTML(pres.precio, 'med-precio')}<span class="med-precio-unit">${esc(pres.tipo)} · ${esc(pres.conc)} · ${esc(pres.marca)}</span></div>
          <button type="button" class="btn btn--primario btn-agregar" data-id="${med.id}" ${agotado ? 'disabled' : ''}>${ic('carrito')} ${agotado ? 'Agotado' : 'Agregar al carrito'}</button>
        </div>
        <a class="ficha-pregunta" href="${WHATSAPP}?text=${pregunta}" target="_blank" rel="noopener">${ic('whatsapp')} Preguntar por WhatsApp</a>
        <p class="ficha-nota">Consulta a un profesional de la salud antes de usar cualquier medicamento.</p>
      </div>`;
  }

  function pintarVariedad(v) {
    const agotado = v.disponibilidad === 'agotado';
    const pregunta = encodeURIComponent(`Hola, quiero preguntar por ${v.nombre}.`);
    return `<div class="ficha-arte ficha-arte--variedad">${Arte.variedad(v)}</div>
      <div class="ficha-info">
        <span class="var-cat">${esc(LABELS_CAT[v.cat] || v.cat)}</span>
        <h2 id="ficha-titulo">${esc(v.nombre)}</h2>
        ${v.desc ? `<p class="ficha-desc">${esc(v.desc)}</p>` : ''}
        ${v.contenido ? `<p class="ficha-dato"><span>Contenido</span>${esc(v.contenido)}</p>` : ''}
        <div class="med-etiquetas">${etiquetas(v)}</div>
        <div class="ficha-compra">
          <div class="ficha-precio med-precio-wrap">${precioHTML(v.precio, 'med-precio')}</div>
          <button type="button" class="btn btn--primario btn-agregar-var" data-id="${v.id}" ${agotado ? 'disabled' : ''}>${ic('carrito')} ${agotado ? 'Agotado' : 'Agregar al carrito'}</button>
        </div>
        <a class="ficha-pregunta" href="${WHATSAPP}?text=${pregunta}" target="_blank" rel="noopener">${ic('whatsapp')} Preguntar por WhatsApp</a>
      </div>`;
  }

  function crearDialogo() {
    if (dialogo) return dialogo;
    dialogo = document.createElement('dialog');
    dialogo.id = 'ficha-producto';
    dialogo.className = 'dialogo dialogo--ficha';
    dialogo.setAttribute('aria-labelledby', 'ficha-titulo');
    dialogo.innerHTML = `<button type="button" class="btn-icono ficha-cerrar" aria-label="Cerrar">${ic('cerrar')}</button><div class="ficha" id="ficha-contenido"></div>`;
    document.body.appendChild(dialogo);
    dialogo.querySelector('.ficha-cerrar').addEventListener('click', () => dialogo.close());
    dialogo.addEventListener('click', (e) => {
      if (e.target === dialogo) { dialogo.close(); return; }
      const btn = e.target.closest('.sel-btn');
      if (!btn) return;
      const fila = btn.closest('[data-grupo]');
      const med = buscarMed(+dialogo.dataset.id);
      if (!med) return;
      elegir(med, fila.dataset.grupo, btn.dataset[fila.dataset.grupo]);
      const orden = fila.dataset.grupo;
      dialogo.querySelector('#ficha-contenido').innerHTML = pintarMedicamento(med);
      dialogo.querySelector(`[data-grupo="${orden}"] .sel-btn.activo`)?.focus();
    });
    return dialogo;
  }

  function abrir(tipo, id) {
    const d = crearDialogo();
    const p = tipo === 'medicamento' ? buscarMed(id) : buscarVar(id);
    if (!p) return false;
    d.dataset.tipo = tipo;
    d.dataset.id = id;
    d.querySelector('#ficha-contenido').innerHTML = tipo === 'medicamento' ? pintarMedicamento(p) : pintarVariedad(p);
    if (!d.open) d.showModal();
    return true;
  }

  // ── Agregar al carrito ────────────────────────────────────
  // La miniatura viaja hasta el carrito: confirma dónde quedó el producto
  function volar(origen) {
    const destino = document.getElementById('btn-abrir-carrito');
    if (!origen || !destino || Experiencia.sinMovimiento || !origen.animate) return;
    const a = origen.getBoundingClientRect(), b = destino.getBoundingClientRect();
    const lado = 72;
    const copia = document.createElement('div');
    copia.className = 'vuelo';
    copia.innerHTML = origen.querySelector('svg, img')?.outerHTML || '';
    const izq = a.left + a.width / 2 - lado / 2, arriba = a.top + a.height / 2 - lado / 2;
    Object.assign(copia.style, { left: izq + 'px', top: arriba + 'px', width: lado + 'px', height: lado + 'px' });
    (dialogo && dialogo.open ? dialogo : document.body).appendChild(copia);
    const x = b.left + b.width / 2 - (izq + lado / 2), y = b.top + b.height / 2 - (arriba + lado / 2);
    copia.animate(
      [{ transform: 'none', opacity: 1 }, { transform: `translate(${x}px, ${y}px) scale(.12)`, opacity: .35 }],
      { duration: 480, easing: 'cubic-bezier(.4,.1,.3,1)' }
    ).onfinish = () => copia.remove();
  }

  function confirmar(boton) {
    const original = boton.innerHTML;
    boton.classList.add('hecho');
    boton.innerHTML = `${ic('check')} Agregado`;
    setTimeout(() => { boton.classList.remove('hecho'); boton.innerHTML = original; }, 1200);
  }

  // La ficha se cierra sola al terminar, para que se vea el carrito actualizado
  function cerrarTrasAgregar() {
    if (dialogo && dialogo.open) setTimeout(() => { if (dialogo.open) dialogo.close(); }, Experiencia.sinMovimiento ? 300 : 620);
  }

  document.addEventListener('click', (e) => {
    const abre = e.target.closest('[data-ficha]');
    if (abre) { abrir(abre.dataset.ficha, +abre.dataset.id); return; }

    const med = e.target.closest('.btn-agregar');
    if (med) {
      const m = buscarMed(+med.dataset.id);
      if (!m) return;
      const pres = actual(m);
      volar(dialogo?.querySelector('.ficha-arte'));
      Carrito.agregar({
        id: pres.id, nombre: m.nombre, detalle: `${pres.conc} · ${pres.tipo} · ${pres.marca}`,
        precio: window.Suscripcion ? Suscripcion.aplicar(pres.precio) : pres.precio, imagen: m.imagen
      });
      renderDrawer();
      confirmar(med);
      cerrarTrasAgregar();
      return;
    }

    const vr = e.target.closest('.btn-agregar-var');
    if (vr) {
      const v = buscarVar(+vr.dataset.id);
      if (!v) return;
      volar(vr.closest('.var-card')?.querySelector('.var-arte') || dialogo?.querySelector('.ficha-arte'));
      Carrito.agregar({
        id: v.id, nombre: v.nombre, precio: window.Suscripcion ? Suscripcion.aplicar(v.precio) : v.precio, imagen: v.imagen
      });
      renderDrawer();
      confirmar(vr);
      cerrarTrasAgregar();
    }
  });

  // Enlace directo a un producto desde el buscador: /medicamentos?producto=3
  document.addEventListener('DOMContentLoaded', () => {
    const id = +new URLSearchParams(location.search).get('producto');
    if (!id) return;
    if (location.pathname.includes('medicamentos')) abrir('medicamento', id);
    if (location.pathname.includes('variedades')) abrir('variedad', id);
  });

  return { tarjetaMedicamento, tarjetaVariedad, abrir, desde };
})();
