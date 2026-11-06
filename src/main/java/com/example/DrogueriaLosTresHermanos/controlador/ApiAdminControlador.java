package com.example.DrogueriaLosTresHermanos.controlador;

import com.example.DrogueriaLosTresHermanos.servicio.ArchivosClientes;
import com.example.DrogueriaLosTresHermanos.servicio.DirectorioAccesos;
import com.example.DrogueriaLosTresHermanos.servicio.EquipoEnBaseDatos;
import com.example.DrogueriaLosTresHermanos.servicio.ServicioClientes;
import com.example.DrogueriaLosTresHermanos.servicio.ServicioPedidos;
import com.example.DrogueriaLosTresHermanos.servicio.ServicioSuscripciones;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** Datos del panel: clientes, pagos de suscripciones y pedidos. SeguridadConfig exige el permiso de cada ruta. */
@RestController
@RequestMapping("/api/admin")
public class ApiAdminControlador {

    private final ServicioClientes clientes;
    private final ServicioPedidos pedidos;
    private final ServicioSuscripciones suscripciones;
    private final EquipoEnBaseDatos equipo;
    private final DirectorioAccesos directorio;
    private final ArchivosClientes archivos;

    public ApiAdminControlador(ServicioClientes clientes, ServicioPedidos pedidos, ServicioSuscripciones suscripciones,
                               EquipoEnBaseDatos equipo, DirectorioAccesos directorio, ArchivosClientes archivos) {
        this.clientes = clientes;
        this.pedidos = pedidos;
        this.suscripciones = suscripciones;
        this.equipo = equipo;
        this.directorio = directorio;
        this.archivos = archivos;
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumen() {
        return pedidos.resumenPanel();
    }

    // ── Clientes ──────────────────────────────────────────────

    @GetMapping("/clientes")
    public List<Map<String, Object>> clientes() {
        return clientes.listarParaPanel();
    }

    @PostMapping("/clientes")
    public Map<String, Object> crearCliente(@RequestBody Map<String, Object> d, Authentication a) {
        long id = clientes.registrar(texto(d, "nombre"), texto(d, "correo"), texto(d, "telefono"), texto(d, "direccion"), texto(d, "clave"),
            Boolean.TRUE.equals(d.get("aceptaDatos")), equipo.idDe(a.getName()));
        directorio.auditar(a.getName(), "Cliente creado", "Cliente " + id);
        return Map.of("id", id);
    }

    // ── Pagos de suscripciones ────────────────────────────────

    @GetMapping("/solicitudes")
    public List<Map<String, Object>> solicitudes() {
        return suscripciones.solicitudesParaPanel();
    }

    @GetMapping("/solicitudes/{id}/comprobante")
    public ResponseEntity<Resource> comprobante(@PathVariable long id) {
        String nombre = suscripciones.comprobante(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var ruta = archivos.comprobantes().ruta(nombre).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(archivos.comprobantes().tipoDe(nombre)))
            .body(new FileSystemResource(ruta));
    }

    @PostMapping("/solicitudes/{id}/aprobar")
    public Map<String, Object> aprobar(@PathVariable long id, Authentication a) {
        long suscripcion = suscripciones.aprobar(id, equipo.idDe(a.getName()));
        directorio.auditar(a.getName(), "Pago de suscripción aprobado", "Solicitud " + id);
        return Map.of("suscripcion", suscripcion);
    }

    @PostMapping("/solicitudes/{id}/rechazar")
    public Map<String, Object> rechazar(@PathVariable long id, @RequestBody Map<String, Object> d, Authentication a) {
        suscripciones.rechazar(id, equipo.idDe(a.getName()), texto(d, "motivo"));
        directorio.auditar(a.getName(), "Pago de suscripción rechazado", "Solicitud " + id);
        return Map.of("ok", true);
    }

    @PostMapping("/suscripciones/tienda")
    public Map<String, Object> activarEnTienda(@RequestBody Map<String, Object> d, Authentication a) {
        Object cliente = d.get("clienteId");
        long id;
        try {
            id = Long.parseLong(String.valueOf(cliente));
        } catch (NumberFormatException e) {
            throw new com.example.DrogueriaLosTresHermanos.servicio.BaseDatos.ErrorUsuario("Elige un cliente.");
        }
        long suscripcion = suscripciones.activarEnTienda(id, texto(d, "plan"), equipo.idDe(a.getName()), texto(d, "nota"));
        directorio.auditar(a.getName(), "Plan activado en tienda", "Cliente " + id + ", plan " + texto(d, "plan"));
        return Map.of("suscripcion", suscripcion);
    }

    // ── Pedidos ───────────────────────────────────────────────

    @GetMapping("/pedidos")
    public List<Map<String, Object>> pedidos() {
        return pedidos.paraPanel();
    }

    @PostMapping("/pedidos/{id}/estado")
    public Map<String, Object> cambiarEstado(@PathVariable long id, @RequestBody Map<String, Object> d, Authentication a) {
        String estado = texto(d, "estado");
        if ("cancelado".equals(estado) && a.getAuthorities().stream().noneMatch(x -> x.getAuthority().equals(com.example.DrogueriaLosTresHermanos.modelo.Permiso.VENTAS_ANULAR.getCodigo()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para anular pedidos.");
        }
        pedidos.cambiarEstado(id, estado == null ? "" : estado, equipo.idDe(a.getName()), texto(d, "motivo"));
        directorio.auditar(a.getName(), "cancelado".equals(estado) ? "Pedido anulado" : "Pedido actualizado", "Pedido " + id + " → " + estado);
        return Map.of("ok", true);
    }

    private static String texto(Map<String, Object> d, String clave) {
        Object v = d.get(clave);
        return v == null ? null : String.valueOf(v);
    }
}
