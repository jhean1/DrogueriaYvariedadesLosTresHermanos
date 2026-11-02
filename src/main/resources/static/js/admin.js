// ══════════════════════════════════════════════════════
//  admin.js — Datos del panel (localStorage de este navegador).
//  El acceso a la página y a cada sección lo decide el servidor:
//  una sección sin permiso ni siquiera llega al HTML.
// ══════════════════════════════════════════════════════

const CLIENTES_KEY = 'lth_clientes';
const SOL_KEY      = 'lth_solicitudes';
const esc          = Seguridad.esc;
const ic           = Experiencia.icono;
const $id          = (id) => document.getElementById(id);
const permisos     = new Set((document.body.dataset.permisos || '').split(' '));

let solicitudActualId = null;
let clienteActivarId  = null;
let planActivarSel    = null;

const NOMBRES_METODO = { nequi:'Nequi', daviplata:'Daviplata', pse:'PSE', bancolombia:'Bancolombia', tienda:'En tienda' };
const nombrePlan = (p) => Seguridad.planCanonico(p)?.nombre || p;
const MIGAS = { resumen:'Resumen', solicitudes:'Solicitudes de pago', clientes:'Clientes', 'crear-cliente':'Crear cliente', 'activar-plan':'Activar plan en tienda' };

// ── Helpers de localStorage ──────────────────────────
function getClientes() {
  try { return JSON.parse(localStorage.getItem(CLIENTES_KEY)) || []; } catch { return []; }
}
function setClientes(arr) { localStorage.setItem(CLIENTES_KEY, JSON.stringify(arr)); }
function getSolicitudes() {
  try { return JSON.parse(localStorage.getItem(SOL_KEY)) || []; } catch { return []; }
}
function setSolicitudes(arr) { localStorage.setItem(SOL_KEY, JSON.stringify(arr)); }
function getVentas() {
  try { return JSON.parse(localStorage.getItem('lth_ventas')) || []; } catch { return []; }
}
function getSuscripcion(idCliente) {
  try { return JSON.parse(localStorage.getItem('lth_sus_' + idCliente) || 'null'); } catch { return null; }
}
function fijar(id, valor) {
  const el = $id(id);
  if (el) el.textContent = valor;
}
function vacio(icono, titulo, texto) {
  return `<div class="vacio"><div class="vacio-icono">${ic(icono)}</div><h3>${esc(titulo)}</h3>${texto ? `<p>${esc(texto)}</p>` : ''}</div>`;
}

// ── Semáforo de completitud de solicitud ─────────────
function semaforo(sol) {
  const tiene = (x) => x && String(x).trim();
  const obligatorios = tiene(sol.referencia) && tiene(sol.fecha) && tiene(sol.titular);
  const opcionales   = tiene(sol.aprobacion) || tiene(sol.foto) || tiene(sol.nota);
  if (obligatorios && opcionales) return { clase: 'insignia--exito', label: 'Completo' };
  if (obligatorios)               return { clase: 'insignia--aviso', label: 'Parcial' };
  return                                 { clase: 'insignia--peligro', label: 'Mínimo' };
}
function insigniaEstado(estado) {
  const clase = { pendiente:'insignia--aviso', aprobado:'insignia--exito', rechazado:'insignia--peligro', activo:'insignia--exito', cancelado:'' };
  return `<span class="insignia ${Object.prototype.hasOwnProperty.call(clase, estado) ? clase[estado] : ''}">${esc(estado)}</span>`;
}

// ── Inicializar ──────────────────────────────────────
document.addEventListener('DOMContentLoaded', function() {
  const hora = new Date().getHours();
  fijar('saludo', hora < 12 ? 'Buenos días' : hora < 19 ? 'Buenas tardes' : 'Buenas noches');
  actualizarResumen();
  renderTablaRecientes();
  actualizarBadgePendientes();
  abrirDesdeDireccion();
});
window.addEventListener('hashchange', abrirDesdeDireccion);

function abrirDesdeDireccion() {
  const id = window.location.hash.slice(1) || 'resumen';
  irSeccion($id('sec-' + id) ? id : 'resumen', true);
}

// Los botones de las tablas llevan sus datos en atributos data-*. Así, un nombre
// o id manipulado no puede convertirse en código, como sí ocurriría con onclick.
document.addEventListener('click', function(e) {
  const boton = e.target.closest('[data-accion]');
  if (!boton) return;
  const { accion, id, estado } = boton.dataset;
  if (accion === 'ver-solicitud')       verSolicitud(id);
  if (accion === 'estado-solicitud')    accionSolicitud(id, estado);
  if (accion === 'activar-cliente')     irActivarPorCliente(id);
  if (accion === 'preparar-activacion') {
    const cliente = getClientes().find(c => c.id === id);
    if (cliente) prepararActivacion(cliente.id, cliente.nombre);
  }
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

  if (id === 'solicitudes')  renderTablaSolicitudes(getSolicitudes());
  if (id === 'clientes')     renderTablaClientes(getClientes());
  if (id === 'activar-plan') renderTablaBuscaActivar(getClientes());
  Experiencia.revelar(seccion);
}

// ── Resumen ──────────────────────────────────────────
function actualizarResumen() {
  const clientes = getClientes();
  const sols     = getSolicitudes();
  const ventas   = getVentas();
  const activos  = clientes.filter(c => getSuscripcion(c.id)?.estado === 'activo');
  const contar   = (id, valor) => {
    const el = $id(id);
    if (!el) return;
    el.textContent = valor;
    el.dataset.contar = valor;
    delete el.dataset.contado;
  };
  contar('stat-clientes', clientes.length);
  contar('stat-activos', activos.length);
  contar('stat-pendientes', sols.filter(s => s.estado === 'pendiente').length);
  contar('stat-aprobados', sols.filter(s => s.estado === 'aprobado').length);
  contar('stat-pedidos', ventas.length);
  Experiencia.contadores();
  graficaEstados(sols);
  graficaPlanes(sols);
  actividadReciente(sols, ventas);
}

// Dona con los estados reales de las solicitudes
function graficaEstados(sols) {
  const caja = $id('grafica-estados');
  if (!caja) return;
  if (!sols.length) { caja.innerHTML = vacio('grafica', 'Aún no hay solicitudes', 'La gráfica aparecerá con la primera solicitud de pago.'); return; }
  const series = [
    { nombre: 'Pendientes', valor: sols.filter(s => s.estado === 'pendiente').length, color: 'var(--amber)' },
    { nombre: 'Aprobadas', valor: sols.filter(s => s.estado === 'aprobado').length, color: 'var(--emerald)' },
    { nombre: 'Rechazadas', valor: sols.filter(s => s.estado === 'rechazado').length, color: 'var(--burgundy)' }
  ];
  const total = sols.length, largo = 2 * Math.PI * 42;
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
  const planes = ['basico', 'estandar', 'regular', 'premium'].map(c => {
    const plan = Seguridad.planCanonico(c);
    return { nombre: plan.nombre, valor: sols.filter(s => Seguridad.planCanonico(s.plan)?.clave === c).length };
  });
  const max = Math.max(1, ...planes.map(p => p.valor));
  caja.innerHTML = `<ul class="barras">${planes.map(p => `<li><span>${p.nombre}</span><div class="barra-pista"><div class="barra-relleno" style="--ancho:${(p.valor / max) * 100}%"></div></div><strong>${p.valor}</strong></li>`).join('')}</ul>`;
}

function actividadReciente(sols, ventas) {
  const caja = $id('actividad-reciente');
  if (!caja) return;
  const eventos = [];
  if (permisos.has('subscriptions.view')) {
    sols.forEach(s => eventos.push({ fecha: new Date(s.fechaSolicitud), icono: 'tarjeta', texto: `${s.clienteNombre || 'Cliente'} · solicitud del plan ${nombrePlan(s.plan)}`, estado: s.estado }));
  }
  if (permisos.has('sales.view')) {
    ventas.forEach(v => eventos.push({ fecha: null, orden: -1, icono: 'recibo', texto: `${v.clienteNombre || 'Cliente'} · pedido ${v.id}`, detalle: v.fecha, estado: v.estado }));
  }
  if (!eventos.length) { caja.innerHTML = vacio('historial', 'Sin actividad todavía', 'Aquí verás las solicitudes y los pedidos más recientes.'); return; }
  const ordenados = eventos.sort((a, b) => (b.fecha?.getTime() || 0) - (a.fecha?.getTime() || 0)).slice(0, 6);
  caja.innerHTML = `<ol class="linea-tiempo">${ordenados.map(e => `<li>
    <span class="linea-icono">${ic(e.icono)}</span>
    <div><strong>${esc(e.texto)}</strong><small>${esc(e.detalle || (e.fecha && !isNaN(e.fecha) ? e.fecha.toLocaleString('es-CO') : ''))}</small></div>
    ${insigniaEstado(e.estado)}
  </li>`).join('')}</ol>`;
}

function actualizarBadgePendientes() {
  Panel.pintarAvisos();
}

// ── Tablas de solicitudes ─────────────────────────────
function renderTablaRecientes() {
  const wrap = $id('tabla-recientes-wrap');
  if (!wrap) return;
  const sols = getSolicitudes().filter(s => s.estado === 'pendiente').slice(-5).reverse();
  wrap.innerHTML = sols.length ? tablaHTML(sols) : vacio('check', 'Sin solicitudes pendientes', 'Las nuevas solicitudes de pago aparecerán aquí.');
}

function renderTablaSolicitudes(sols) {
  const wrap = $id('tabla-solicitudes-wrap');
  if (wrap) wrap.innerHTML = tablaHTML(sols);
}

function filtrarSolicitudes(q) {
  const qq = q.toLowerCase();
  renderTablaSolicitudes(getSolicitudes().filter(s =>
    String(s.clienteNombre||'').toLowerCase().includes(qq) ||
    String(s.clienteCorreo||'').toLowerCase().includes(qq) ||
    String(s.referencia||'').toLowerCase().includes(qq) ||
    String(s.id||'').toLowerCase().includes(qq)
  ));
}

function tablaHTML(sols) {
  if (!sols.length) return vacio('tarjeta', 'No hay solicitudes que mostrar');
  const aprueba = permisos.has('subscriptions.approve');
  const rows = sols.map(s => {
    const sem = semaforo(s);
    return `<tr>
      <td data-etiqueta="Cliente"><div><strong>${esc(s.clienteNombre||'–')}</strong><small>${esc(s.clienteCorreo)}</small></div></td>
      <td data-etiqueta="Plan">Plan ${esc(nombrePlan(s.plan))}</td>
      <td data-etiqueta="Método">${esc(NOMBRES_METODO[s.metodo]||s.metodo)}</td>
      <td data-etiqueta="Completitud"><span class="insignia ${sem.clase}">${sem.label}</span></td>
      <td data-etiqueta="Estado">${insigniaEstado(s.estado)}</td>
      <td data-etiqueta="Fecha">${esc(String(s.fechaSolicitud||'').split('T')[0]||'–')}</td>
      <td data-etiqueta="Acción"><div class="tabla-acciones">
        <button type="button" class="btn btn--secundario btn--sm" data-accion="ver-solicitud" data-id="${esc(s.id)}">Ver</button>
        ${s.estado === 'pendiente' && aprueba ? `
          <button type="button" class="btn btn--exito btn--sm" data-accion="estado-solicitud" data-estado="aprobado" data-id="${esc(s.id)}" aria-label="Aprobar">${ic('check')}</button>
          <button type="button" class="btn btn--peligro btn--sm" data-accion="estado-solicitud" data-estado="rechazado" data-id="${esc(s.id)}" aria-label="Rechazar">${ic('cerrar')}</button>` : ''}
      </div></td>
    </tr>`;
  }).join('');
  return `<div class="tabla-marco"><table class="tabla tabla--fichas">
    <thead><tr><th scope="col">Cliente</th><th scope="col">Plan</th><th scope="col">Método</th><th scope="col">Completitud</th><th scope="col">Estado</th><th scope="col">Fecha</th><th scope="col">Acción</th></tr></thead>
    <tbody>${rows}</tbody>
  </table></div>`;
}

// ── Ver detalle solicitud ─────────────────────────────
function verSolicitud(id) {
  const sol = getSolicitudes().find(s => s.id === id);
  if (!sol) return;
  solicitudActualId = id;
  const sem  = semaforo(sol);
  const plan = Seguridad.planCanonico(sol.plan);
  const foto = Seguridad.imagenDataSegura(sol.foto);
  const fila = (clave, valor) => `<div class="detalle-fila"><dt>${clave}</dt><dd>${valor}</dd></div>`;
  $id('modal-body-sol').innerHTML = `<dl class="detalle-lista">
    ${fila('N.° solicitud', esc(sol.id))}
    ${fila('Cliente', `${esc(sol.clienteNombre)} (${esc(sol.clienteCorreo)})`)}
    ${fila('Plan', plan ? `Plan ${esc(plan.nombre)} · $${plan.precio.toLocaleString('es-CO')}` : '<span class="insignia insignia--peligro">Plan no válido</span>')}
    ${fila('Método de pago', esc(NOMBRES_METODO[sol.metodo]||sol.metodo))}
    ${fila('Referencia', esc(sol.referencia||'–'))}
    ${fila('Fecha del pago', esc(sol.fecha||'–'))}
    ${fila('Titular', esc(sol.titular||'–'))}
    ${fila('N.° aprobación', esc(sol.aprobacion||'No proporcionado'))}
    ${fila('Nota', esc(sol.nota||'–'))}
    ${fila('Completitud', `<span class="insignia ${sem.clase}">${sem.label}</span>`)}
    ${fila('Estado', insigniaEstado(sol.estado))}
    ${foto ? fila('Comprobante', `<img class="foto-comprobante" src="${esc(foto)}" alt="Comprobante de pago"/>`) : ''}
  </dl>`;
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

// ── Aprobar / rechazar ────────────────────────────────
function accionSolicitud(id, nuevoEstado) {
  const sols = getSolicitudes();
  const idx  = sols.findIndex(s => s.id === id);
  if (idx === -1) return;

  // El precio y el descuento salen de la tabla oficial, no de la solicitud,
  // porque el cliente puede alterarla antes de enviarla.
  const plan = Seguridad.planCanonico(sols[idx].plan);
  if (nuevoEstado === 'aprobado' && !plan) {
    alert('La solicitud tiene un plan no válido y no se puede aprobar.');
    return;
  }

  sols[idx].estado = nuevoEstado;
  setSolicitudes(sols);

  if (nuevoEstado === 'aprobado') {
    const sol = sols[idx];
    const cli = getClientes().find(c => c.id === sol.clienteId || c.correo === sol.clienteCorreo);
    if (cli) {
      const ahora = new Date(); const venc = new Date(ahora); venc.setDate(venc.getDate()+30);
      localStorage.setItem('lth_sus_' + cli.id, JSON.stringify({
        plan: plan.clave, descuento: plan.descuento, precio: plan.precio,
        fecha: ahora.toISOString(), fechaVencimiento: venc.toISOString(), estado: 'activo'
      }));
    }
  }
  actualizarResumen();
  actualizarBadgePendientes();
  renderTablaSolicitudes(getSolicitudes());
  renderTablaRecientes();
}
function accionSolicitudModal(estado) {
  if (solicitudActualId) { accionSolicitud(solicitudActualId, estado); cerrarModal(); }
}

// ── Tabla clientes ────────────────────────────────────
function renderTablaClientes(lista) {
  const wrap = $id('tabla-clientes-wrap');
  if (!wrap) return;
  if (!lista.length) { wrap.innerHTML = vacio('usuarios', 'No hay clientes registrados aún', 'Los clientes que se registren en este navegador aparecerán aquí.'); return; }
  const activa = permisos.has('subscriptions.activate');
  const rows = lista.map(c => {
    const sus = getSuscripcion(c.id);
    const plan = sus && sus.estado === 'activo' && new Date(sus.fechaVencimiento) > new Date() ? Seguridad.planCanonico(sus.plan) : null;
    return `<tr>
      <td data-etiqueta="Nombre"><strong>${esc(c.nombre||'–')}</strong></td>
      <td data-etiqueta="Correo">${esc(c.correo||'–')}</td>
      <td data-etiqueta="Teléfono">${esc(c.telefono||'–')}</td>
      <td data-etiqueta="Dirección">${esc(c.direccion||'–')}</td>
      <td data-etiqueta="Plan">${plan ? `<span class="insignia insignia--exito">${esc(plan.nombre)}</span>` : '<span class="insignia">Sin plan</span>'}</td>
      <td data-etiqueta="Acción">${activa ? `<button type="button" class="btn btn--secundario btn--sm" data-accion="activar-cliente" data-id="${esc(c.id)}">Activar plan</button>` : '–'}</td>
    </tr>`;
  }).join('');
  wrap.innerHTML = `<div class="tabla-marco"><table class="tabla tabla--fichas">
    <thead><tr><th scope="col">Nombre</th><th scope="col">Correo</th><th scope="col">Teléfono</th><th scope="col">Dirección</th><th scope="col">Plan</th><th scope="col">Acción</th></tr></thead>
    <tbody>${rows}</tbody>
  </table></div>`;
}

function filtrarClientes(q) {
  const qq = q.toLowerCase();
  renderTablaClientes(getClientes().filter(c =>
    String(c.nombre||'').toLowerCase().includes(qq) ||
    String(c.correo||'').toLowerCase().includes(qq) ||
    String(c.telefono||'').includes(q)
  ));
}

// ── Crear cliente ─────────────────────────────────────
async function crearCliente() {
  const nombre    = $id('nc-nombre').value.trim();
  const correo    = $id('nc-correo').value.trim().toLowerCase();
  const telefono  = $id('nc-telefono').value.trim();
  const direccion = $id('nc-direccion').value.trim();
  const clave     = $id('nc-clave').value;
  const msg       = $id('msg-crear-cli');
  const error = (texto) => { msg.className = 'msg-form error'; msg.textContent = texto; };

  msg.className = 'msg-form';
  if (!nombre || !correo || !clave) return error('Nombre, correo y contraseña son obligatorios.');
  if (clave.length < 8) return error('La contraseña debe tener al menos 8 caracteres.');
  if (!Seguridad.criptoDisponible()) return error('Crear clientes requiere una conexión segura (HTTPS).');

  await Seguridad.listo;
  const credencial = await Seguridad.crearCredencial(clave);
  const clientes = getClientes();
  if (clientes.find(c => String(c.correo).toLowerCase() === correo)) return error('Ya existe un cliente con ese correo.');

  clientes.push({
    id: 'cli-' + Date.now(), nombre, correo, telefono, direccion, credencial, rol: 'cliente',
    creadoPorAdmin: true, fechaCreacion: new Date().toISOString()
  });
  setClientes(clientes);
  msg.className = 'msg-form ok';
  msg.textContent = `Cliente "${nombre}" creado correctamente.`;
  limpiarFormCliente();
  actualizarResumen();
}

function limpiarFormCliente() {
  ['nc-nombre','nc-correo','nc-telefono','nc-direccion','nc-clave'].forEach(id => { $id(id).value = ''; });
}

// ── Activar plan (tienda) ─────────────────────────────
function renderTablaBuscaActivar(lista) {
  const wrap = $id('tabla-busca-activar');
  if (!wrap) return;
  if (!lista.length) { wrap.innerHTML = vacio('usuarios', 'No hay clientes que mostrar'); return; }
  const rows = lista.map(c => `<tr>
    <td data-etiqueta="Nombre"><strong>${esc(c.nombre)}</strong></td>
    <td data-etiqueta="Correo">${esc(c.correo)}</td>
    <td data-etiqueta="Teléfono">${esc(c.telefono||'–')}</td>
    <td data-etiqueta="Acción"><button type="button" class="btn btn--secundario btn--sm" data-accion="preparar-activacion" data-id="${esc(c.id)}">Seleccionar</button></td>
  </tr>`).join('');
  wrap.innerHTML = `<div class="tabla-marco"><table class="tabla tabla--fichas"><thead><tr><th scope="col">Nombre</th><th scope="col">Correo</th><th scope="col">Teléfono</th><th scope="col">Acción</th></tr></thead><tbody>${rows}</tbody></table></div>`;
}

function buscarClienteActivar(q) {
  const qq = q.toLowerCase();
  renderTablaBuscaActivar(getClientes().filter(c =>
    String(c.nombre||'').toLowerCase().includes(qq) ||
    String(c.correo||'').toLowerCase().includes(qq) ||
    String(c.telefono||'').includes(q)
  ));
}

function prepararActivacion(id, nombre) {
  clienteActivarId = id;
  planActivarSel   = null;
  fijar('activar-cliente-nombre', nombre);
  $id('panel-activacion').style.display = 'block';
  $id('msg-activar').className = 'msg-form';
  document.querySelectorAll('.plan-opt').forEach(e => e.classList.remove('sel'));
  $id('activar-nota').value = '';
  $id('panel-activacion').scrollIntoView({ behavior: Experiencia.sinMovimiento ? 'auto' : 'smooth', block: 'center' });
}

function irActivarPorCliente(id) {
  const cli = getClientes().find(c => c.id === id);
  if (!cli) return;
  irSeccion('activar-plan');
  setTimeout(() => prepararActivacion(cli.id, cli.nombre), 100);
}

function selPlan(nombre, precio, descuento) {
  planActivarSel = { nombre, precio, descuento };
  document.querySelectorAll('.plan-opt').forEach(e => e.classList.remove('sel'));
  event.currentTarget.classList.add('sel');
}

function activarPlanAdmin() {
  const msg = $id('msg-activar');
  msg.className = 'msg-form';
  if (!clienteActivarId) { msg.className='msg-form error'; msg.textContent='Selecciona un cliente primero.'; return; }
  if (!planActivarSel)   { msg.className='msg-form error'; msg.textContent='Selecciona un plan.'; return; }

  const ahora = new Date(); const venc = new Date(ahora); venc.setDate(venc.getDate()+30);
  const sus = {
    plan: planActivarSel.nombre, descuento: planActivarSel.descuento,
    precio: planActivarSel.precio,
    fecha: ahora.toISOString(), fechaVencimiento: venc.toISOString(),
    estado: 'activo', activadoEnTienda: true,
    nota: $id('activar-nota').value || 'Pago en efectivo – tienda'
  };
  localStorage.setItem('lth_sus_' + clienteActivarId, JSON.stringify(sus));

  const cli = getClientes().find(c => c.id === clienteActivarId);
  const sols = getSolicitudes();
  sols.push({
    id: 'SOL-' + Date.now(), clienteId: clienteActivarId,
    clienteNombre: cli?.nombre||'', clienteCorreo: cli?.correo||'',
    plan: planActivarSel.nombre, descuento: planActivarSel.descuento,
    precio: planActivarSel.precio, metodo: 'tienda',
    referencia: 'Pago efectivo tienda', fecha: ahora.toISOString().split('T')[0],
    titular: cli?.nombre||'', nota: sus.nota,
    estado: 'aprobado', fechaSolicitud: ahora.toISOString()
  });
  setSolicitudes(sols);
  actualizarResumen();
  actualizarBadgePendientes();

  msg.className = 'msg-form ok';
  msg.textContent = `Plan "${planActivarSel.nombre}" activado para ${cli?.nombre||clienteActivarId}.`;
  planActivarSel = null;
  document.querySelectorAll('.plan-opt').forEach(e => e.classList.remove('sel'));
}

function cancelarActivacion() {
  $id('panel-activacion').style.display = 'none';
  clienteActivarId = null; planActivarSel = null;
}
