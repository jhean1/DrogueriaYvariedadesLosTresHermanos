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
    const conPlan = Suscripcion.aplicar(precio);
    if (conPlan === precio) return `<span class="${clase} num">${formatPrecio(precio)}</span>`;
    return `<span class="${clase}-original num">${formatPrecio(precio)}</span>
      <span class="${clase} con-descuento num">${formatPrecio(conPlan)}</span>`;
  }

  // ── Favoritos del cliente (se guardan en su cuenta) ──────
  let favoritos = null;
  function cargarFavoritos() {
    if (favoritos || !Cuenta.cliente()) return Promise.resolve(favoritos);
    return Api.pedir('/api/cuenta/favoritos').then(lista => (favoritos = new Set(lista.map(Number)))).catch(() => null);
  }
  function botonFavorito(id) {
    if (!Cuenta.cliente()) return '';
    const guardado = favoritos && favoritos.has(id);
    return `<button type="button" class="ficha-pregunta ficha-favorito" data-favorito="${id}" aria-pressed="${guardado}">${ic('corazon')} ${guardado ? 'Guardado en favoritos' : 'Guardar en favoritos'}</button>`;
  }
  function alternarFavorito(boton) {
    const id = +boton.dataset.favorito;
    const guardado = favoritos && favoritos.has(id);
    boton.disabled = true;
    Api.pedir('/api/cuenta/favoritos/' + id, { metodo: guardado ? 'DELETE' : 'POST' })
      .then(() => {
        if (!favoritos) favoritos = new Set();
        guardado ? favoritos.delete(id) : favoritos.add(id);
        boton.outerHTML = botonFavorito(id);
        Toast.mostrar(guardado ? 'Quitado de tus favoritos' : 'Guardado en tus favoritos');
      })
      .catch(err => { boton.disabled = false; Toast.mostrar(err.message); });
  }

  function etiquetas(p) {
    return [
      p.formula ? `<span class="insignia insignia--aviso">${ic('documento')} Requiere fórmula médica</span>` : '',
      p.disponibilidad === 'disponible' ? `<span class="insignia insignia--exito"><span class="punto"></span>Disponible</span>` : '',
      p.disponibilidad === 'agotado' ? `<span class="insignia insignia--peligro">Agotado</span>` : ''
    ].join('');
  }

  function editorImagenes() {
    return document.getElementById('editor-imagenes');
  }
  function botonImagen(m) {
    if (!editorImagenes()) return '';
    const texto = m.foto ? 'Cambiar imagen' : 'Agregar imagen';
    return `<button type="button" class="btn-foto" data-foto="${m.id}" aria-label="${texto} de ${esc(m.nombre)}">${ic('subir')} ${texto}</button>`;
  }

  function subirImagen(boton, archivo) {
    const m = buscarMed(+boton.dataset.foto);
    const editor = editorImagenes();
    if (!m || !editor) return;
    if (!/^image\/(jpeg|png|webp)$/.test(archivo.type)) { Toast.mostrar('La foto debe ser JPG, PNG o WEBP.'); return; }
    if (archivo.size > 3 * 1024 * 1024) { Toast.mostrar('La foto no puede superar 3 MB.'); return; }
    const datos = new FormData();
    datos.append('foto', archivo);
    datos.append('_csrf', editor.dataset.csrf);
    boton.setAttribute('aria-busy', 'true');
    fetch('/admin/productos/' + m.id + '/foto', { method: 'POST', body: datos, redirect: 'manual' })
      .then(r => r.text().then(texto => ({ ok: r.ok, texto, status: r.status })))
      .then(({ ok, texto, status }) => {
        if (!ok) { Toast.mostrar(status === 400 ? texto : 'No se pudo guardar la imagen. Revisa tu sesión e inténtalo de nuevo.'); return; }
        m.foto = texto;
        const tarjeta = boton.closest('.med-card');
        tarjeta.querySelector('.med-arte').innerHTML = Arte.medicamento(m, m.presentaciones[0]);
        tarjeta.querySelector('.btn-foto').outerHTML = botonImagen(m);
        Toast.mostrar('Imagen guardada. Ya aparece en la tienda.');
      })
      .catch(() => Toast.mostrar('No se pudo guardar la imagen. Revisa tu conexión.'))
      .finally(() => boton.removeAttribute('aria-busy'));
  }

  // ── Tarjetas ──────────────────────────────────────────────
  function tarjetaMedicamento(m, i) {
    const n = m.presentaciones.length;
    const conc = [...new Set(m.presentaciones.map(p => p.conc))];
    return `<article class="med-card" data-med="${m.id}" style="--i:${Math.min(i, 8)}">
      <button type="button" class="med-arte" data-ficha="medicamento" data-id="${m.id}" aria-label="Ver detalles de ${esc(m.nombre)}" tabindex="-1">${Arte.medicamento(m, m.presentaciones[0])}</button>
      ${botonImagen(m)}
      <div class="med-body">
        <span class="med-categoria">${esc(LABELS_CAT[m.cat] || m.cat)}</span>
        <h2 class="med-nombre"><button type="button" data-ficha="medicamento" data-id="${m.id}">${esc(m.nombre)}</button></h2>
        <p class="med-desc">${esc(m.desc)}</p>
        <p class="med-formas">${n} ${n === 1 ? 'presentación' : 'presentaciones'} · ${esc(conc.slice(0, 2).join(', '))}${conc.length > 2 ? '…' : ''}</p>
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
    return `<div class="ficha-arte">${Arte.medicamento(med, pres)}<span class="ficha-arte-etiqueta">${esc(pres.tipo)}</span></div>
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
        <div class="ficha-enlaces"><a class="ficha-pregunta" href="${WHATSAPP}?text=${pregunta}" target="_blank" rel="noopener">${ic('whatsapp')} Preguntar por WhatsApp</a>${botonFavorito(med.id)}</div>
        ${!med.foto && med.imagen ? '<p class="ficha-nota">La imagen es de referencia: el empaque que recibes puede ser de otra marca o laboratorio, con el mismo medicamento y concentración.</p>' : ''}
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
        <div class="ficha-enlaces"><a class="ficha-pregunta" href="${WHATSAPP}?text=${pregunta}" target="_blank" rel="noopener">${ic('whatsapp')} Preguntar por WhatsApp</a>${botonFavorito(v.id)}</div>
        ${!v.foto && v.imagen ? '<p class="ficha-nota">La imagen es de referencia: el producto puede variar en presentación o diseño.</p>' : ''}
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
      const orden = fila.dataset.grupo;
      const antes = actual(med).tipo;
      elegir(med, fila.dataset.grupo, btn.dataset[fila.dataset.grupo]);
      dialogo.querySelector('#ficha-contenido').innerHTML = pintarMedicamento(med);
      // La imagen cambia con la presentación elegida; el cambio se anima solo cuando de verdad cambia
      if (actual(med).tipo !== antes) dialogo.querySelector('.ficha-arte').classList.add('arte-cambia');
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
    const pintar = () => { d.querySelector('#ficha-contenido').innerHTML = tipo === 'medicamento' ? pintarMedicamento(p) : pintarVariedad(p); };
    pintar();
    if (!d.open) d.showModal();
    // Los favoritos llegan del servidor: al recibirlos se marca el botón
    if (Cuenta.cliente() && !favoritos) cargarFavoritos().then(() => { if (d.open && d.dataset.id === String(id)) {
      const b = d.querySelector('[data-favorito]');
      if (b) b.outerHTML = botonFavorito(id);
    } });
    return true;
  }

  // ── Agregar al carrito ────────────────────────────────────
  // Un círculo rojo vuela desde el botón hasta el carrito
  function volar(origen) {
    const destino = document.getElementById('btn-abrir-carrito');
    if (!origen || !destino || Experiencia.sinMovimiento || !origen.animate) return;
    const a = origen.getBoundingClientRect(), b = destino.getBoundingClientRect();
    const circulo = document.createElement('div');
    circulo.className = 'vuelo';
    const izq = a.left + a.width / 2 - 9, arriba = a.top + a.height / 2 - 9;
    Object.assign(circulo.style, { left: izq + 'px', top: arriba + 'px' });
    (dialogo && dialogo.open ? dialogo : document.body).appendChild(circulo);
    const x = b.left + b.width / 2 - (izq + 9), y = b.top + b.height / 2 - (arriba + 9);
    circulo.animate(
      [{ transform: 'none', opacity: 1 }, { transform: `translate(${x * .5}px, ${y * .5 - 60}px) scale(1.15)`, opacity: 1, offset: .5 }, { transform: `translate(${x}px, ${y}px) scale(.5)`, opacity: .6 }],
      { duration: 560, easing: 'cubic-bezier(.4,0,.2,1)' }
    ).onfinish = () => circulo.remove();
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
    const foto = e.target.closest('.btn-foto');
    if (foto) {
      const entrada = document.createElement('input');
      entrada.type = 'file';
      entrada.accept = 'image/jpeg,image/png,image/webp';
      entrada.addEventListener('change', () => { if (entrada.files[0]) subirImagen(foto, entrada.files[0]); });
      entrada.click();
      return;
    }
    const fav = e.target.closest('[data-favorito]');
    if (fav) { alternarFavorito(fav); return; }
    const abre = e.target.closest('[data-ficha]');
    if (abre) { abrir(abre.dataset.ficha, +abre.dataset.id); return; }

    const med = e.target.closest('.btn-agregar');
    if (med) {
      const m = buscarMed(+med.dataset.id);
      if (!m) return;
      const pres = actual(m);
      volar(med);
      Carrito.agregar({
        tipo: 'medicamento', presentacion: pres.id, nombre: m.nombre, detalle: `${pres.conc} · ${pres.tipo} · ${pres.marca}`
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
      volar(vr);
      Carrito.agregar({ tipo: 'variedad', producto: v.id, nombre: v.nombre, detalle: v.contenido || '' });
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
