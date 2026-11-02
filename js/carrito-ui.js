// Panel lateral del carrito, compartido por toda la tienda.
(function () {
  const obtener = (id) => document.getElementById(id);

  window.abrirDrawer = function () {
    const panel = obtener('carrito-drawer');
    if (!panel) return;
    panel.classList.add('abierto');
    obtener('drawer-overlay').classList.add('visible');
    document.body.classList.add('sin-scroll');
    window.renderDrawer();
    window.Experiencia && Experiencia.atrapar(panel);
  };

  window.cerrarDrawer = function () {
    const panel = obtener('carrito-drawer');
    if (!panel) return;
    panel.classList.remove('abierto');
    obtener('drawer-overlay').classList.remove('visible');
    document.body.classList.remove('sin-scroll');
    window.Experiencia && Experiencia.soltar(panel);
  };

  window.renderDrawer = function () {
    const body = obtener('drawer-body');
    const footer = obtener('drawer-footer');
    if (!body) return;
    const items = Carrito.obtener();
    const esc = Seguridad.esc;
    const ic = Experiencia.icono;

    if (!items.length) {
      body.innerHTML = `<div class="vacio">
        <div class="vacio-icono">${ic('carrito')}</div>
        <h3>Tu carrito está vacío</h3>
        <p>Explora el catálogo y agrega lo que necesites.</p>
        <a class="btn btn--secundario btn--sm" href="/DrogueriaYvariedadesLosTresHermanos/medicamentos">Ver medicamentos</a>
      </div>`;
      footer.style.display = 'none';
      return;
    }

    body.innerHTML = items.map(item => `
      <div class="carrito-item">
        <div class="carrito-item-miniatura">${ic(String(item.id).includes('-') ? 'capsula' : 'bolsa')}</div>
        <div class="carrito-item-info">
          <div class="carrito-item-nombre">${esc(item.nombre)}</div>
          ${item.detalle ? `<div class="carrito-item-detalle">${esc(item.detalle)}</div>` : ''}
          <div class="carrito-item-precio">${formatPrecio(item.precio)}</div>
        </div>
        <div class="carrito-item-controles">
          <div class="selector-cantidad">
            <button class="btn-qty" data-id="${esc(item.id)}" data-delta="-1" aria-label="Quitar una unidad">${ic('menos')}</button>
            <span class="qty-num">${esc(item.cantidad)}</span>
            <button class="btn-qty" data-id="${esc(item.id)}" data-delta="1" aria-label="Agregar una unidad">${ic('mas')}</button>
          </div>
          <button class="btn-eliminar-item" data-eliminar="${esc(item.id)}" aria-label="Eliminar producto">${ic('papelera')}</button>
        </div>
      </div>`).join('');

    obtener('total-valor').textContent = formatPrecio(Carrito.total());
    footer.style.display = '';

    body.querySelectorAll('.btn-qty').forEach(btn => {
      btn.addEventListener('click', () => {
        Carrito.cambiarCantidad(btn.dataset.id, +btn.dataset.delta);
        window.renderDrawer();
      });
    });
    body.querySelectorAll('.btn-eliminar-item').forEach(btn => {
      btn.addEventListener('click', () => {
        Carrito.remover(btn.dataset.eliminar);
        window.renderDrawer();
        Toast.mostrar('Producto eliminado del carrito');
      });
    });
  };

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && obtener('carrito-drawer')?.classList.contains('abierto')) window.cerrarDrawer();
  });
})();
