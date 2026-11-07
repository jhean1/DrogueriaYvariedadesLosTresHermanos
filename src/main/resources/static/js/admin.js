// ══════════════════════════════════════════════════════
//  admin.js — Datos del panel desde la base de datos (API /api/admin).
//  El acceso a la página y a cada sección lo decide el servidor:
//  una sección sin permiso ni siquiera llega al HTML, y cada acción
//  se vuelve a validar en el servidor.
// ══════════════════════════════════════════════════════

const esc      = Seguridad.esc;
const ic       = Experiencia.icono;
const $id      = (id) => document.getElementById(id);
const permisos = new Set((document.body.dataset.permisos || '').split(' '));
const pesos    = (v) => '$' + (Number(v) || 0).toLocaleString('es-CO');

let solicitudActualId = null;
let clienteActivarId  = null;
let planActivarSel    = null;

const datos = { resumen: null, clientes: null, solicitudes: null, pedidos: null };
const MIGAS = { resumen:'Resumen', pedidos:'Pedidos', solicitudes:'Solicitudes de pago', clientes:'Clientes', 'crear-cliente':'Crear cliente', 'activar-plan':'Activar plan en tienda' };
const ESTADO_PEDIDO = {
  pendiente:  { texto: 'Pendiente', clase: 'insignia--aviso' },
  en_proceso: { texto: 'En preparación', clase: 'insignia--azul' },
  entregado:  { texto: 'Entregado', clase: 'insignia--exito' },
  cancelado:  { texto: 'Anulado', clase: 'insignia--peligro' }
};
const ESTADO_PAGO = {
  pendiente: { texto: 'Pendiente', clase: 'insignia--aviso' },
  aprobado:  { texto: 'Aprobado', clase: 'insignia--exito' },
  rechazado: { texto: 'Rechazado', clase: 'insignia--peligro' }
};

function fijar(id, valor) {
  const el = $id(id);
  if (el) el.textContent = valor;
}
function vacio(icono, titulo, texto) {
  return `<div class="vacio"><div class="vacio-icono">${ic(icono)}</div><h3>${esc(titulo)}</h3>${texto ? `<p>${esc(texto)}</p>` : ''}</div>`;
}
function insignia(mapa, estado) {
  const e = mapa[estado] || { texto: estado, clase: '' };
  return `<span class="insignia ${e.clase}">${esc(e.texto)}</span>`;
}
function fallo(contenedor, err, reintentar) {
  if (!contenedor) return;
  contenedor.innerHTML = `<div class="vacio"><div class="vacio-icono">${ic('alerta')}</div><h3>No se pudieron cargar los datos</h3><p>${esc(err.message)}</p><button type="button" class="btn btn--secundario btn--sm">Intentar de nuevo</button></div>`;
  contenedor.querySelector('button').addEventListener('click', reintentar);
}

// ── Carga de datos (cada lista solo si el rol puede verla) ─────
async function cargar(clave, forzar) {
  if (datos[clave] && !forzar) return datos[clave];
  const rutas = { resumen: '/api/admin/resumen', clientes: '/api/admin/clientes', solicitudes: '/api/admin/solicitudes', pedidos: '/api/admin/pedidos' };
  datos[clave] = await Api.pedir(rutas[clave]);
  return datos[clave];
}
const puedeVer = {
  clientes: permisos.has('customers.view'),
  solicitudes: permisos.has('subscriptions.view'),
  pedidos: permisos.has('sales.view')
};

// ── Inicializar ──────────────────────────────────────
document.addEventListener('DOMContentLoaded', function() {
  const hora = new Date().getHours();
  fijar('saludo', hora < 12 ? 'Buenos días' : hora < 19 ? 'Buenas tardes' : 'Buenas noches');
  actualizarResumen();
  abrirDesdeDireccion();
});
window.addEventListener('hashchange', abrirDesdeDireccion);

function abrirDesdeDireccion() {
  const id = window.location.hash.slice(1) || 'resumen';
  irSeccion($id('sec-' + id) ? id : 'resumen', true);
}

// Los botones de las tablas llevan sus datos en atributos data-*
document.addEventListener('click', function(e) {
  const boton = e.target.closest('[data-accion]');
  if (!boton) return;
  const { accion, id, estado } = boton.dataset;
  if (accion === 'ver-solicitud')       verSolicitud(Number(id));
  if (accion === 'estado-solicitud')    confirmarSolicitud(Number(id), estado);
  if (accion === 'activar-cliente')     irActivarPorCliente(Number(id));
  if (accion === 'preparar-activacion') prepararActivacion(Number(id));
  if (accion === 'estado-pedido')       cambiarEstadoPedido(boton, Number(id), estado);
  if (accion === 'elegir-plan')         selPlan(boton);
});
document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && solicitudActualId) cerrarModal(); });
$id('modal-solicitud')?.addEventListener('click', (e) => { if (e.target === e.currentTarget) cerrarModal(); });

// ── Navegación ───────────────────────────────────────
function irSeccion(id, desdeDireccion) {
  const seccion = $id('sec-' + id);
  if (!seccion) return;
  document.querySelectorAll('.seccion').forEach(s => s.classList.remove('activa'));
  seccion.classList.add('activa');
  document.querySelectorAll('.sidebar-item').forEach(s => {
    const activa = s.dataset.seccion === id;
    s.classList.toggle('activo', activa);
    if (activa) s.setAttribute('aria-current', 'page'); else s.removeAttribute('aria-current');
  });
  fijar('miga-actual', MIGAS[id] || 'Resumen');
  if (!desdeDireccion && window.location.hash !== '#' + id) history.replaceState(null, '', '#' + id);

  if (id === 'pedidos')      renderPedidos(true);
  if (id === 'solicitudes')  renderTablaSolicitudes(true);
  if (id === 'clientes')     renderTablaClientes(true);
  if (id === 'activar-plan') renderTablaBuscaActivar(true);
  Experiencia.revelar(seccion);
}

// ── Resumen ──────────────────────────────────────────
async function actualizarResumen() {
  const contar = (id, valor) => {
    const el = $id(id);
    if (!el) return;
    el.textContent = valor;
    el.dataset.contar = valor;
    delete el.dataset.contado;
  };
  if (typeof MEDICAMENTOS !== 'undefined') contar('stat-productos', MEDICAMENTOS.length + VARIEDADES.length);
  try {
    const r = await cargar('resumen', true);
    contar('stat-clientes', r.clientes);
    contar('stat-activos', r.planesActivos);
    contar('stat-pendientes', r.pagosPendientes);
    contar('stat-aprobados', r.pagosAprobados);
    contar('stat-pedidos', r.pedidosAbiertos);
    fijar('stat-ventas-hoy', pesos(r.ventasHoy));
    Experiencia.contadores();
    graficaEstados(r);
  } catch (err) {
    Panel.aviso(err.message, true);
  }
  const [sols, peds] = await Promise.all([
    puedeVer.solicitudes ? cargar('solicitudes', true).catch(() => null) : null,
    puedeVer.pedidos ? cargar('pedidos', true).catch(() => null) : null
  ]);
  if (sols) { graficaPlanes(sols); renderTablaRecientes(sols); }
  actividadReciente(sols || [], peds || []);
  Panel.pintarAvisos(sols);
}

// Dona con los estados reales de las solicitudes
function graficaEstados(r) {
  const caja = $id('grafica-estados');
  if (!caja) return;
  const series = [
    { nombre: 'Pendientes', valor: Number(r.pagosPendientes), color: 'var(--amber)' },
    { nombre: 'Aprobadas', valor: Number(r.pagosAprobados), color: 'var(--emerald)' },
    { nombre: 'Rechazadas', valor: Number(r.pagosRechazados), color: 'var(--burgundy)' }
  ];
  const total = series.reduce((s, x) => s + x.valor, 0);
  if (!total) { caja.innerHTML = vacio('grafica', 'Aún no hay solicitudes', 'La gráfica aparecerá con la primera solicitud de pago.'); return; }
  const largo = 2 * Math.PI * 42;
  let avance = 0;
  const arcos = series.filter(s => s.valor).map(s => {
    const tramo = (s.valor / total) * largo;
    const arco = `<circle class="dona-arco" cx="60" cy="60" r="42" stroke="${s.color}" stroke-dasharray="${tramo} ${largo - tramo}" stroke-dashoffset="${-avance}"/>`;
    avance += tramo;
    return arco;
  }).join('');
  caja.innerHTML = `<div class="dona">
    <svg viewBox="0 0 120 120" role="img" aria-label="${series.map(s => `${s.nombre}: ${s.valor}`).join(', ')}"><circle cx="60" cy="60" r="42" class="dona-fondo"/>${arcos}
      <text x="60" y="57" class="dona-total">${total}</text><text x="60" y="72" class="dona-texto">solicitudes</text></svg>
    <ul class="leyenda-lista">${series.map(s => `<li><span style="background:${s.color}"></span>${s.nombre}<strong>${s.valor}</strong></li>`).join('')}</ul>
  </div>`;
}

function graficaPlanes(sols) {
  const caja = $id('grafica-planes');
  if (!caja) return;
  if (!sols.length) { caja.innerHTML = vacio('estrella', 'Sin datos por plan', 'Se mostrarán los planes que soliciten tus clientes.'); return; }
  const planes = Object.values(Seguridad.PLANES).map(plan => ({
    nombre: plan.nombre, valor: sols.filter(s => Seguridad.planCanonico(s.plan)?.clave === plan.clave).length
  }));
  const max = Math.max(1, ...planes.map(p => p.valor));
  caja.innerHTML = `<ul class="barras">${planes.map(p => `<li><span>${esc(p.nombre)}</span><div class="barra-pista"><div class="barra-relleno" style="--ancho:${(p.valor / max) * 100}%"></div></div><strong>${p.valor}</strong></li>`).join('')}</ul>`;
}

// Fechas del servidor: 'AAAA-MM-DD HH:MM'
const aFecha = (t) => { const d = new Date(String(t || '').replace(' ', 'T')); return isNaN(d) ? null : d; };
const verFecha = (t) => { const d = aFecha(t); return d ? d.toLocaleString('es-CO', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' }) : '–'; };

function actividadReciente(sols, peds) {
  const caja = $id('actividad-reciente');
  if (!caja) return;
  const eventos = [
    ...sols.map(s => ({ fecha: aFecha(s.fecha), icono: 'tarjeta', texto: `${s.cliente} · pago del plan ${s.plan}`, estado: insignia(ESTADO_PAGO, s.estado) })),
    ...peds.map(p => ({ fecha: aFecha(p.fecha), icono: 'recibo', texto: `${p.cliente || 'Cliente'} · pedido ${p.numero}`, estado: insignia(ESTADO_PEDIDO, p.estado) }))
  ];
  if (!eventos.length) { caja.innerHTML = vacio('historial', 'Sin actividad todavía', 'Aquí verás las solicitudes y los pedidos más recientes.'); return; }
  const ordenados = eventos.sort((a, b) => (b.fecha?.getTime() || 0) - (a.fecha?.getTime() || 0)).slice(0, 6);
  caja.innerHTML = `<ol class="linea-tiempo">${ordenados.map(e => `<li>
    <span class="linea-icono">${ic(e.icono)}</span>
    <div><strong>${esc(e.texto)}</strong><small>${esc(e.fecha ? e.fecha.toLocaleString('es-CO') : '')}</small></div>
    ${e.estado}
  </li>`).join('')}</ol>`;
}

// ── Solicitudes de pago ───────────────────────────────
function renderTablaRecientes(sols) {
  const wrap = $id('tabla-recientes-wrap');
  if (!wrap) return;
  const pendientes = sols.filter(s => s.estado === 'pendiente').slice(0, 5);
  wrap.innerHTML = pendientes.length ? tablaHTML(pendientes) : vacio('check', 'Sin solicitudes pendientes', 'Las nuevas solicitudes de pago aparecerán aquí.');
}

async function renderTablaSolicitudes(recargar) {
  const wrap = $id('tabla-solicitudes-wrap');
  if (!wrap) return;
  try {
    const sols = await cargar('solicitudes', recargar);
    const q = ($id('busca-sol')?.value || '').trim().toLowerCase();
    const lista = sols.filter(s => !q || [s.cliente, s.correo, s.referencia, s.id].some(x => String(x || '').toLowerCase().includes(q)));
    wrap.innerHTML = tablaHTML(lista, sols.length);
    const t = wrap.querySelector('table');
    if (t) Panel.paginar(t, 10);
  } catch (err) { fallo(wrap, err, () => renderTablaSolicitudes(true)); }
}
function filtrarSolicitudes() { renderTablaSolicitudes(false); }

// Qué tanto respaldo trae el pago para verificarlo
function semaforo(s) {
  const extra = s.aprobacion || s.tieneComprobante || s.nota;
  if (s.origen === 'tienda') return { clase: 'insignia--exito', label: 'En tienda' };
  if (s.referencia && s.titular && extra) return { clase: 'insignia--exito', label: 'Completo' };
  if (s.referencia && s.titular) return { clase: 'insignia--aviso', label: 'Parcial' };
  return { clase: 'insignia--peligro', label: 'Mínimo' };
}

function tablaHTML(sols, total) {
  if (!sols.length) return vacio('tarjeta', total ? 'Ninguna solicitud coincide' : 'Aún no hay solicitudes de pago');
  const aprueba = permisos.has('subscriptions.approve');
  const rows = sols.map(s => {
    const sem = semaforo(s);
    return `<tr>
      <td data-etiqueta="Cliente"><div><strong>${esc(s.cliente)}</strong><small>${esc(s.correo || '')}</small></div></td>
      <td data-etiqueta="Plan">Plan ${esc(s.plan)} · <span class="num">${pesos(s.monto)}</span></td>
      <td data-etiqueta="Método">${esc(s.metodo)}</td>
      <td data-etiqueta="Respaldo"><span class="insignia ${sem.clase}">${sem.label}</span></td>
      <td data-etiqueta="Estado">${insignia(ESTADO_PAGO, s.estado)}</td>
      <td data-etiqueta="Fecha">${esc(verFecha(s.fecha))}</td>
      <td data-etiqueta="Acción"><div class="tabla-acciones">
        <button type="button" class="btn btn--secundario btn--sm" data-accion="ver-solicitud" data-id="${esc(s.id)}">Ver</button>
        ${s.estado === 'pendiente' && aprueba ? `
          <button type="button" class="btn btn--exito btn--sm" data-accion="estado-solicitud" data-estado="aprobado" data-id="${esc(s.id)}" aria-label="Aprobar el pago de ${esc(s.cliente)}">${ic('check')}</button>
          <button type="button" class="btn btn--peligro btn--sm" data-accion="estado-solicitud" data-estado="rechazado" data-id="${esc(s.id)}" aria-label="Rechazar el pago de ${esc(s.cliente)}">${ic('cerrar')}</button>` : ''}
      </div></td>
    </tr>`;
  }).join('');
  return `<div class="tabla-marco"><table class="tabla tabla--fichas">
    <thead><tr><th scope="col">Cliente</th><th scope="col">Plan</th><th scope="col">Método</th><th scope="col">Respaldo</th><th scope="col">Estado</th><th scope="col">Fecha</th><th scope="col">Acción</th></tr></thead>
    <tbody>${rows}</tbody>
  </table></div>`;
}

function verSolicitud(id) {
  const sol = (datos.solicitudes || []).find(s => s.id === id);
  if (!sol) return;
  solicitudActualId = id;
  const sem  = semaforo(sol);
  const fila = (clave, valor) => `<div class="detalle-fila"><dt>${clave}</dt><dd>${valor}</dd></div>`;
  $id('modal-body-sol').innerHTML = `<dl class="detalle-lista">
    ${fila('N.° solicitud', esc(sol.id))}
    ${fila('Cliente', `${esc(sol.cliente)} (${esc(sol.correo || 'sin correo')})`)}
    ${fila('Plan', `Plan ${esc(sol.plan)} · ${pesos(sol.monto)}`)}
    ${fila('Método de pago', esc(sol.metodo))}
    ${fila('Referencia', esc(sol.referencia || '–'))}
    ${fila('Fecha del pago', esc(sol.fechaPago || '–'))}
    ${fila('Titular', esc(sol.titular || '–'))}
    ${fila('N.° aprobación', esc(sol.aprobacion || 'No lo envió'))}
    ${fila('Nota', esc(sol.nota || '–'))}
    ${fila('Respaldo', `<span class="insignia ${sem.clase}">${sem.label}</span>`)}
    ${fila('Estado', insignia(ESTADO_PAGO, sol.estado) + (sol.motivo ? ` <small>${esc(sol.motivo)}</small>` : ''))}
    ${sol.tieneComprobante ? fila('Comprobante', `<a href="/api/admin/solicitudes/${esc(sol.id)}/comprobante" target="_blank" rel="noopener"><img class="foto-comprobante" src="/api/admin/solicitudes/${esc(sol.id)}/comprobante" alt="Comprobante de pago de ${esc(sol.cliente)}"/></a>`) : ''}
  </dl>`;
  const pie = document.querySelector('#modal-solicitud .modal-pie');
  if (pie) pie.hidden = sol.estado !== 'pendiente';
  const velo = $id('modal-solicitud');
  velo.classList.add('visible');
  Experiencia.atrapar(velo);
}
function cerrarModal() {
  const velo = $id('modal-solicitud');
  if (!velo) return;
  velo.classList.remove('visible');
  Experiencia.soltar(velo);
  solicitudActualId = null;
}

/** Diálogo que pide un texto obligatorio (motivo de rechazo o de anulación). */
function pedirMotivo({ titulo, texto, accion, etiqueta }) {
  return new Promise((resolver) => {
    const d = document.createElement('dialog');
    d.className = 'dialogo dialogo-confirmar';
    d.setAttribute('aria-labelledby', 'motivo-titulo');
    d.innerHTML = `<form method="dialog" class="dialogo-forma">
      <div class="modal-cabeza"><h2 id="motivo-titulo"></h2></div>
      <div class="modal-cuerpo"><p></p>
        <div class="campo"><label for="motivo-texto"></label><textarea id="motivo-texto" rows="3" maxlength="255" required></textarea></div>
      </div>
      <div class="modal-pie">
        <button class="btn btn--fantasma" value="no" formnovalidate>Cancelar</button>
        <button class="btn btn--peligro" value="si"></button>
      </div></form>`;
    d.querySelector('h2').textContent = titulo;
    d.querySelector('.modal-cuerpo p').textContent = texto;
    d.querySelector('label').textContent = etiqueta;
    d.querySelector('button[value=si]').textContent = accion;
    document.body.appendChild(d);
    d.addEventListener('close', () => {
      const valor = d.querySelector('textarea').value.trim();
      resolver(d.returnValue === 'si' && valor ? valor : null);
      d.remove();
    });
    d.showModal();
  });
}

async function confirmarSolicitud(id, estado) {
  const sol = (datos.solicitudes || []).find(s => s.id === id);
  if (!sol) return false;
  let ruta, cuerpo;
  if (estado === 'aprobado') {
    const si = await Experiencia.confirmar({
      titulo: 'Aprobar el pago',
      texto: `Confirma que recibiste ${pesos(sol.monto)} de ${sol.cliente}. Se activará su plan ${sol.plan}.`,
      accion: 'Aprobar y activar'
    });
    if (!si) return false;
    ruta = `/api/admin/solicitudes/${id}/aprobar`;
  } else {
    const motivo = await pedirMotivo({
      titulo: 'Rechazar el pago',
      texto: `${sol.cliente} verá el motivo en su perfil y podrá enviar el pago de nuevo.`,
      etiqueta: 'Motivo del rechazo', accion: 'Rechazar'
    });
    if (!motivo) return false;
    ruta = `/api/admin/solicitudes/${id}/rechazar`;
    cuerpo = { motivo };
  }
  try {
    await Api.pedir(ruta, { metodo: 'POST', datos: cuerpo || {} });
    Panel.aviso(estado === 'aprobado' ? 'Pago aprobado. El plan del cliente quedó activo.' : 'Solicitud rechazada.');
    datos.clientes = null;
    await actualizarResumen();
    if ($id('sec-solicitudes')?.classList.contains('activa')) renderTablaSolicitudes(false);
    return true;
  } catch (err) {
    Panel.aviso(err.message, true);
    return false;
  }
}
function accionSolicitudModal(estado) {
  if (!solicitudActualId) return;
  confirmarSolicitud(solicitudActualId, estado).then((si) => { if (si) cerrarModal(); });
}

// ── Pedidos ───────────────────────────────────────────
async function renderPedidos(recargar) {
  const wrap = $id('tabla-pedidos-wrap');
  if (!wrap) return;
  let todos;
  try { todos = await cargar('pedidos', recargar); } catch (err) { fallo(wrap, err, () => renderPedidos(true)); return; }
  const q = ($id('busca-ped').value || '').trim().toLowerCase();
  const entrega = $id('filtro-entrega').value;
  const estado = $id('filtro-estado')?.value || '';
  const lista = todos.filter(p =>
    (!q || String(p.cliente || '').toLowerCase().includes(q) || String(p.numero || '').toLowerCase().includes(q) || String(p.telefono || '').includes(q)) &&
    (!entrega || p.modalidad === entrega) && (!estado || p.estado === estado));
  fijar('total-pedidos', lista.length);
  if (!lista.length) {
    wrap.innerHTML = todos.length
      ? vacio('buscar', 'Ningún pedido coincide', 'Prueba con otro nombre o cambia el filtro.')
      : vacio('recibo', 'Aún no hay pedidos', 'Los pedidos que hagan los clientes en la tienda aparecerán aquí.');
    return;
  }
  const anula = permisos.has('sales.cancel');
  const rows = lista.map(p => {
    const abierto = p.estado === 'pendiente' || p.estado === 'en_proceso';
    const unidades = p.productos.reduce((s, i) => s + Number(i.cantidad), 0);
    return `<tr>
    <td data-etiqueta="Pedido"><div><strong>${esc(p.numero)}</strong><small>${esc(verFecha(p.fecha))}</small></div></td>
    <td data-etiqueta="Cliente"><div><strong>${esc(p.cliente || '–')}</strong><small>${esc(p.telefono || '')}</small></div></td>
    <td data-etiqueta="Productos"><details class="pedido-detalle"><summary>${unidades} unidad${unidades === 1 ? '' : 'es'}</summary>
      <ul>${p.productos.map(i => `<li>${esc(i.cantidad)} × ${esc(i.nombre)} <span class="num">${pesos(i.subtotal)}</span></li>`).join('')}</ul></details></td>
    <td data-etiqueta="Entrega">${p.modalidad === 'domicilio' ? `<span class="insignia insignia--azul">Domicilio</span><small class="pedido-direccion">${esc(p.direccion || '')}</small><small class="pedido-direccion">${
      Number(p.domicilio) > 0 ? `${pesos(p.domicilio)} · ${esc(String(p.distanciaKm).replace('.', ','))} km` : p.domicilioGratis ? 'Gratis (plan)' : 'Sin ubicación: acuerda el valor con el cliente'}</small>` : '<span class="insignia">Recoger en tienda</span>'}</td>
    <td data-etiqueta="Total" class="num"><strong>${pesos(p.total)}</strong>${Number(p.descuento) > 0 ? `<small>Plan -${esc(Number(p.descuentoPorcentaje))}%</small>` : ''}</td>
    <td data-etiqueta="Estado">${insignia(ESTADO_PEDIDO, p.estado)}</td>
    <td data-etiqueta="Acción"><div class="tabla-acciones">
      ${p.estado === 'pendiente' ? `<button type="button" class="btn btn--secundario btn--sm" data-accion="estado-pedido" data-estado="en_proceso" data-id="${esc(p.id)}">Preparar</button>` : ''}
      ${abierto ? `<button type="button" class="btn btn--exito btn--sm" data-accion="estado-pedido" data-estado="entregado" data-id="${esc(p.id)}">Entregado</button>` : ''}
      ${abierto && anula ? `<button type="button" class="btn btn--peligro btn--sm" data-accion="estado-pedido" data-estado="cancelado" data-id="${esc(p.id)}" aria-label="Anular el pedido ${esc(p.numero)}">Anular</button>` : ''}
      ${abierto ? '' : '–'}
    </div></td>
  </tr>`;
  }).join('');
  wrap.innerHTML = `<div class="tabla-marco"><table class="tabla tabla--fichas">
    <thead><tr><th scope="col">Pedido</th><th scope="col">Cliente</th><th scope="col">Productos</th><th scope="col">Entrega</th><th scope="col">Total</th><th scope="col">Estado</th><th scope="col">Acción</th></tr></thead>
    <tbody>${rows}</tbody></table></div>`;
  Panel.paginar(wrap.querySelector('table'), 10);
}

async function cambiarEstadoPedido(boton, id, estado) {
  const p = (datos.pedidos || []).find(x => x.id === id);
  if (!p) return;
  let motivo;
  if (estado === 'entregado') {
    const si = await Experiencia.confirmar({
      titulo: 'Marcar como entregado',
      texto: `Confirma que ${p.cliente || 'el cliente'} recibió el pedido ${p.numero} y pagó ${pesos(p.total)}. Se descontará del inventario.`,
      accion: 'Sí, se entregó'
    });
    if (!si) return;
  } else if (estado === 'cancelado') {
    motivo = await pedirMotivo({
      titulo: 'Anular el pedido ' + p.numero,
      texto: 'El pedido queda anulado y el cliente lo verá así en su cuenta.',
      etiqueta: 'Motivo de la anulación', accion: 'Anular pedido'
    });
    if (!motivo) return;
  }
  boton.disabled = true;
  try {
    await Api.pedir(`/api/admin/pedidos/${id}/estado`, { metodo: 'POST', datos: { estado, motivo } });
    Panel.aviso(estado === 'en_proceso' ? 'Pedido en preparación.' : estado === 'entregado' ? 'Pedido entregado.' : 'Pedido anulado.');
    await renderPedidos(true);
    actualizarResumen();
  } catch (err) {
    boton.disabled = false;
    Panel.aviso(err.message, true);
  }
}

// ── Clientes ──────────────────────────────────────────
function coincideCliente(c, q) {
  const qq = q.trim().toLowerCase();
  return !qq || [c.nombre, c.correo, c.telefono].some(x => String(x || '').toLowerCase().includes(qq));
}

async function renderTablaClientes(recargar) {
  const wrap = $id('tabla-clientes-wrap');
  if (!wrap) return;
  let todos;
  try { todos = await cargar('clientes', recargar); } catch (err) { fallo(wrap, err, () => renderTablaClientes(true)); return; }
  const lista = todos.filter(c => coincideCliente(c, $id('busca-cli')?.value || ''));
  if (!lista.length) { wrap.innerHTML = vacio('usuarios', todos.length ? 'Ningún cliente coincide' : 'Aún no hay clientes registrados'); return; }
  const activa = permisos.has('subscriptions.activate');
  const rows = lista.map(c => `<tr>
      <td data-etiqueta="Nombre"><div><strong>${esc(c.nombre)}</strong><small>Desde ${esc(verFecha(c.registro))}${c.creadoEnTienda ? ' · creado en tienda' : ''}</small></div></td>
      <td data-etiqueta="Correo">${esc(c.correo || '–')}</td>
      <td data-etiqueta="Teléfono">${esc(c.telefono || '–')}</td>
      <td data-etiqueta="Dirección">${esc(c.direccion || '–')}</td>
      <td data-etiqueta="Pedidos" class="num">${esc(c.pedidos)}</td>
      <td data-etiqueta="Plan">${c.plan ? `<span class="insignia insignia--exito">${esc(c.plan)}</span>` : '<span class="insignia">Sin plan</span>'}</td>
      <td data-etiqueta="Acción">${activa ? `<button type="button" class="btn btn--secundario btn--sm" data-accion="activar-cliente" data-id="${esc(c.id)}">Activar plan</button>` : '–'}</td>
    </tr>`).join('');
  wrap.innerHTML = `<div class="tabla-marco"><table class="tabla tabla--fichas">
    <thead><tr><th scope="col">Nombre</th><th scope="col">Correo</th><th scope="col">Teléfono</th><th scope="col">Dirección</th><th scope="col">Pedidos</th><th scope="col">Plan</th><th scope="col">Acción</th></tr></thead>
    <tbody>${rows}</tbody>
  </table></div>`;
  Panel.paginar(wrap.querySelector('table'), 10);
}
function filtrarClientes() { renderTablaClientes(false); }

// ── Crear cliente ─────────────────────────────────────
async function crearCliente(boton) {
  const valor = (id) => $id(id).value.trim();
  const msg = $id('msg-crear-cli');
  const error = (texto) => { msg.className = 'msg-form error'; msg.textContent = texto; };
  msg.className = 'msg-form';
  const cuerpo = {
    nombre: valor('nc-nombre'), correo: valor('nc-correo').toLowerCase(), telefono: valor('nc-telefono'),
    direccion: valor('nc-direccion'), clave: $id('nc-clave').value, aceptaDatos: $id('nc-datos').checked
  };
  if (!cuerpo.nombre || !cuerpo.correo || !cuerpo.telefono || !cuerpo.direccion || !cuerpo.clave) return error('Completa todos los campos.');
  if (cuerpo.clave.length < 8) return error('La contraseña temporal debe tener al menos 8 caracteres.');
  if (!cuerpo.aceptaDatos) return error('El cliente debe autorizar el tratamiento de sus datos personales.');
  if (boton) boton.disabled = true;
  try {
    await Api.pedir('/api/admin/clientes', { metodo: 'POST', datos: cuerpo });
    msg.className = 'msg-form ok';
    msg.textContent = `Cliente "${cuerpo.nombre}" creado. Entrégale su contraseña temporal y pídele cambiarla en "Mi cuenta".`;
    limpiarFormCliente();
    datos.clientes = null;
    actualizarResumen();
  } catch (err) {
    error(err.message);
  } finally {
    if (boton) boton.disabled = false;
  }
}

function limpiarFormCliente() {
  ['nc-nombre', 'nc-correo', 'nc-telefono', 'nc-direccion', 'nc-clave'].forEach(id => { $id(id).value = ''; });
  $id('nc-datos').checked = false;
}

// ── Activar plan (pago en efectivo en la tienda) ──────
async function renderTablaBuscaActivar(recargar) {
  const wrap = $id('tabla-busca-activar');
  if (!wrap) return;
  let todos;
  try { todos = await cargar('clientes', recargar); } catch (err) { fallo(wrap, err, () => renderTablaBuscaActivar(true)); return; }
  const q = $id('busca-act')?.value || '';
  const lista = todos.filter(c => coincideCliente(c, q));
  if (!lista.length) { wrap.innerHTML = vacio('usuarios', 'No hay clientes que mostrar', 'Si el cliente no tiene cuenta, créala primero en "Crear cliente".'); return; }
  const rows = lista.map(c => `<tr>
    <td data-etiqueta="Nombre"><strong>${esc(c.nombre)}</strong></td>
    <td data-etiqueta="Correo">${esc(c.correo || '–')}</td>
    <td data-etiqueta="Teléfono">${esc(c.telefono || '–')}</td>
    <td data-etiqueta="Plan">${c.plan ? `<span class="insignia insignia--exito">${esc(c.plan)}</span>` : '<span class="insignia">Sin plan</span>'}</td>
    <td data-etiqueta="Acción"><button type="button" class="btn btn--secundario btn--sm" data-accion="preparar-activacion" data-id="${esc(c.id)}">Seleccionar</button></td>
  </tr>`).join('');
  wrap.innerHTML = `<div class="tabla-marco"><table class="tabla tabla--fichas"><thead><tr><th scope="col">Nombre</th><th scope="col">Correo</th><th scope="col">Teléfono</th><th scope="col">Plan</th><th scope="col">Acción</th></tr></thead><tbody>${rows}</tbody></table></div>`;
  Panel.paginar(wrap.querySelector('table'), 10);
}
function buscarClienteActivar() { renderTablaBuscaActivar(false); }

function prepararActivacion(id) {
  const c = (datos.clientes || []).find(x => x.id === id);
  if (!c) return;
  clienteActivarId = id;
  planActivarSel   = null;
  fijar('activar-cliente-nombre', c.nombre);
  $id('panel-activacion').style.display = 'block';
  $id('msg-activar').className = 'msg-form';
  document.querySelectorAll('.plan-opt').forEach(e => { e.classList.remove('sel'); e.setAttribute('aria-pressed', 'false'); });
  $id('activar-nota').value = '';
  $id('panel-activacion').scrollIntoView({ behavior: Experiencia.sinMovimiento ? 'auto' : 'smooth', block: 'center' });
}

async function irActivarPorCliente(id) {
  irSeccion('activar-plan');
  await renderTablaBuscaActivar(false);
  prepararActivacion(id);
}

function selPlan(boton) {
  planActivarSel = boton.dataset.plan;
  document.querySelectorAll('.plan-opt').forEach(e => {
    e.classList.toggle('sel', e === boton);
    e.setAttribute('aria-pressed', String(e === boton));
  });
}

async function activarPlanAdmin(boton) {
  const msg = $id('msg-activar');
  msg.className = 'msg-form';
  if (!clienteActivarId) { msg.className = 'msg-form error'; msg.textContent = 'Selecciona un cliente primero.'; return; }
  if (!planActivarSel)   { msg.className = 'msg-form error'; msg.textContent = 'Selecciona un plan.'; return; }
  const cliente = (datos.clientes || []).find(c => c.id === clienteActivarId);
  const plan = Seguridad.planCanonico(planActivarSel);
  const si = await Experiencia.confirmar({
    titulo: 'Activar plan ' + plan.nombre,
    texto: `Confirma que ${cliente ? cliente.nombre : 'el cliente'} pagó el plan en efectivo. Se activa por 30 días.`,
    accion: 'Activar plan'
  });
  if (!si) return;
  if (boton) boton.disabled = true;
  try {
    await Api.pedir('/api/admin/suscripciones/tienda', { metodo: 'POST',
      datos: { clienteId: clienteActivarId, plan: plan.nombre, nota: $id('activar-nota').value.trim() || 'Pago en efectivo en la tienda' } });
    msg.className = 'msg-form ok';
    msg.textContent = `Plan ${plan.nombre} activado para ${cliente ? cliente.nombre : 'el cliente'}.`;
    planActivarSel = null;
    document.querySelectorAll('.plan-opt').forEach(e => { e.classList.remove('sel'); e.setAttribute('aria-pressed', 'false'); });
    datos.clientes = null;
    renderTablaBuscaActivar(true);
    actualizarResumen();
  } catch (err) {
    msg.className = 'msg-form error';
    msg.textContent = err.message;
  } finally {
    if (boton) boton.disabled = false;
  }
}

function cancelarActivacion() {
  $id('panel-activacion').style.display = 'none';
  clienteActivarId = null; planActivarSel = null;
}
