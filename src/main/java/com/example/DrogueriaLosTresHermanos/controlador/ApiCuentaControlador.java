package com.example.DrogueriaLosTresHermanos.controlador;

import com.example.DrogueriaLosTresHermanos.servicio.ArchivosClientes;
import com.example.DrogueriaLosTresHermanos.servicio.BaseDatos.ErrorUsuario;
import com.example.DrogueriaLosTresHermanos.servicio.ServicioClientes;
import com.example.DrogueriaLosTresHermanos.servicio.ServicioPedidos;
import com.example.DrogueriaLosTresHermanos.servicio.ServicioSuscripciones;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cuenta del cliente, pedidos y suscripciones de la tienda. La sesión del cliente vive en el servidor
 * (atributo de la sesión HTTP), separada de la del equipo del panel.
 */
@RestController
@RequestMapping("/api")
public class ApiCuentaControlador {

    static final String SESION = "clienteId";

    private final ServicioClientes clientes;
    private final ServicioPedidos pedidos;
    private final ServicioSuscripciones suscripciones;
    private final ArchivosClientes archivos;

    public ApiCuentaControlador(ServicioClientes clientes, ServicioPedidos pedidos, ServicioSuscripciones suscripciones, ArchivosClientes archivos) {
        this.clientes = clientes;
        this.pedidos = pedidos;
        this.suscripciones = suscripciones;
        this.archivos = archivos;
    }

    // ── Sesión ────────────────────────────────────────────────

    @GetMapping("/cuenta")
    public ResponseEntity<Map<String, Object>> estado(HttpSession sesion) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("disponible", clientes.disponible());
        Long id = (Long) sesion.getAttribute(SESION);
        if (clientes.disponible() && id != null) {
            var c = clientes.obtener(id);
            if (c.isEmpty()) {
                sesion.removeAttribute(SESION);
            } else {
                r.put("cliente", vista(c.get()));
                r.put("suscripcion", suscripciones.vigente(id).orElse(null));
            }
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(r);
    }

    @PostMapping("/cuenta/registro")
    public Map<String, Object> registrar(@RequestBody Map<String, Object> d, HttpServletRequest peticion) {
        long id = clientes.registrar(texto(d, "nombre"), texto(d, "correo"), texto(d, "telefono"), texto(d, "direccion"), texto(d, "clave"),
            Boolean.TRUE.equals(d.get("aceptaDatos")), null);
        iniciar(peticion, id);
        return Map.of("cliente", vista(clientes.obtener(id).orElseThrow()));
    }

    @PostMapping("/cuenta/ingreso")
    public Map<String, Object> ingresar(@RequestBody Map<String, Object> d, HttpServletRequest peticion) {
        long id = clientes.ingresar(texto(d, "correo"), texto(d, "clave"), peticion.getRemoteAddr());
        iniciar(peticion, id);
        return Map.of("cliente", vista(clientes.obtener(id).orElseThrow()));
    }

    @PostMapping("/cuenta/salida")
    public Map<String, Object> salir(HttpSession sesion) {
        sesion.removeAttribute(SESION);
        return Map.of("ok", true);
    }

    @PutMapping("/cuenta")
    public Map<String, Object> actualizar(@RequestBody Map<String, Object> d, HttpSession sesion) {
        long id = cliente(sesion);
        clientes.actualizar(id, texto(d, "nombre"), texto(d, "telefono"), texto(d, "direccion"));
        return Map.of("cliente", vista(clientes.obtener(id).orElseThrow()));
    }

    @PostMapping("/cuenta/clave")
    public Map<String, Object> cambiarClave(@RequestBody Map<String, Object> d, HttpSession sesion) {
        clientes.cambiarClave(cliente(sesion), texto(d, "actual"), texto(d, "nueva"));
        return Map.of("ok", true);
    }

    @PostMapping("/cuenta/correo")
    public Map<String, Object> cambiarCorreo(@RequestBody Map<String, Object> d, HttpSession sesion) {
        long id = cliente(sesion);
        clientes.cambiarCorreo(id, texto(d, "correo"), texto(d, "clave"));
        return Map.of("cliente", vista(clientes.obtener(id).orElseThrow()));
    }

    @PostMapping("/cuenta/eliminar")
    public Map<String, Object> eliminar(@RequestBody Map<String, Object> d, HttpSession sesion) {
        long id = cliente(sesion);
        String foto = clientes.obtener(id).map(ServicioClientes.Cliente::foto).orElse(null);
        List<String> comprobantes = suscripciones.comprobantesDe(id);
        clientes.eliminar(id, texto(d, "clave"));
        // La base ya no los referencia: también se borran los archivos
        archivos.fotos().borrar(foto);
        comprobantes.forEach(archivos.comprobantes()::borrar);
        sesion.removeAttribute(SESION);
        return Map.of("ok", true);
    }

    @PostMapping("/cuenta/foto")
    public Map<String, Object> foto(@RequestParam MultipartFile foto, HttpSession sesion) throws IOException {
        long id = cliente(sesion);
        try {
            String anterior = clientes.obtener(id).map(ServicioClientes.Cliente::foto).orElse(null);
            String nombre = archivos.fotos().guardar(foto);
            clientes.cambiarFoto(id, nombre);
            archivos.fotos().borrar(anterior);
            return Map.of("foto", "/api/cuenta/foto");
        } catch (IllegalArgumentException e) {
            throw new ErrorUsuario(e.getMessage());
        }
    }

    /** La foto solo la ve su dueño. */
    @GetMapping("/cuenta/foto")
    public ResponseEntity<Resource> verFoto(HttpSession sesion) {
        var c = clientes.obtener(cliente(sesion)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var ruta = archivos.fotos().ruta(c.foto()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(archivos.fotos().tipoDe(c.foto())))
            .body(new FileSystemResource(ruta));
    }

    // ── Favoritos ─────────────────────────────────────────────

    @GetMapping("/cuenta/favoritos")
    public List<Long> favoritos(HttpSession sesion) {
        return clientes.favoritos(cliente(sesion));
    }

    @PostMapping("/cuenta/favoritos/{producto}")
    public Map<String, Object> agregarFavorito(@PathVariable long producto, HttpSession sesion) {
        clientes.agregarFavorito(cliente(sesion), producto);
        return Map.of("ok", true);
    }

    @DeleteMapping("/cuenta/favoritos/{producto}")
    public Map<String, Object> quitarFavorito(@PathVariable long producto, HttpSession sesion) {
        clientes.quitarFavorito(cliente(sesion), producto);
        return Map.of("ok", true);
    }

    // ── Pedidos ───────────────────────────────────────────────

    @PostMapping("/pedidos")
    public Map<String, Object> crearPedido(@RequestBody Map<String, Object> d, HttpSession sesion) {
        long id = cliente(sesion);
        List<ServicioPedidos.Linea> lineas = new java.util.ArrayList<>();
        if (d.get("lineas") instanceof List<?> lista) {
            for (Object o : lista) {
                if (o instanceof Map<?, ?> m) {
                    lineas.add(new ServicioPedidos.Linea(numero(m.get("presentacion")), numero(m.get("producto")), entero(m.get("cantidad"))));
                }
            }
        }
        String numero = pedidos.crear(id, lineas, texto(d, "modalidad"), texto(d, "direccion"));
        return Map.of("pedido", pedidos.deClientePorNumero(id, numero).orElseThrow());
    }

    @GetMapping("/cuenta/pedidos")
    public List<Map<String, Object>> misPedidos(HttpSession sesion) {
        return pedidos.deCliente(cliente(sesion));
    }

    @GetMapping("/cuenta/pedidos/{numero}")
    public Map<String, Object> miPedido(@PathVariable String numero, HttpSession sesion) {
        return pedidos.deClientePorNumero(cliente(sesion), numero).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // ── Suscripciones ─────────────────────────────────────────

    @GetMapping("/planes")
    public List<ServicioSuscripciones.Plan> planes() {
        return suscripciones.planes();
    }

    @GetMapping("/pagos/medios")
    public List<ServicioSuscripciones.MedioPago> mediosDePago() {
        return suscripciones.mediosDePago();
    }

    @PostMapping("/suscripciones/solicitud")
    public Map<String, Object> solicitar(@RequestParam String plan, @RequestParam String metodo, @RequestParam(required = false) String referencia,
                                         @RequestParam(required = false) String aprobacion, @RequestParam(required = false) String titular,
                                         @RequestParam(required = false) String fechaPago, @RequestParam(required = false) String nota,
                                         @RequestParam(required = false) MultipartFile comprobante, HttpSession sesion) throws IOException {
        long id = cliente(sesion);
        String archivo = null;
        if (comprobante != null && !comprobante.isEmpty()) {
            try {
                archivo = archivos.comprobantes().guardar(comprobante);
            } catch (IllegalArgumentException e) {
                throw new ErrorUsuario(e.getMessage().replace("La foto", "La imagen del comprobante"));
            }
        }
        return Map.of("solicitud", suscripciones.solicitar(id, plan, metodo, referencia, aprobacion, titular, fechaPago, nota, archivo));
    }

    @GetMapping("/cuenta/solicitudes")
    public List<Map<String, Object>> misSolicitudes(HttpSession sesion) {
        return suscripciones.solicitudesDe(cliente(sesion));
    }

    @PostMapping("/cuenta/suscripcion/cancelar")
    public Map<String, Object> cancelarPlan(HttpSession sesion) {
        long id = cliente(sesion);
        suscripciones.cancelar(id);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("suscripcion", suscripciones.vigente(id).orElse(null));
        return r;
    }

    // ── Apoyo ─────────────────────────────────────────────────

    private void iniciar(HttpServletRequest peticion, long id) {
        HttpSession s = peticion.getSession(true);
        peticion.changeSessionId();          // evita la fijación de sesión
        peticion.getSession().setAttribute(SESION, id);
        s.setMaxInactiveInterval(60 * 60 * 4);
    }

    static long cliente(HttpSession sesion) {
        Object id = sesion.getAttribute(SESION);
        if (!(id instanceof Long l)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar.");
        }
        return l;
    }

    private static Map<String, Object> vista(ServicioClientes.Cliente c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.id());
        m.put("nombre", c.nombre());
        m.put("correo", c.correo());
        m.put("telefono", c.telefono());
        m.put("direccion", c.direccion());
        m.put("foto", c.foto() == null ? null : "/api/cuenta/foto");
        m.put("registro", c.registro());
        return m;
    }

    private static String texto(Map<String, Object> d, String clave) {
        Object v = d.get(clave);
        return v == null ? null : String.valueOf(v);
    }

    private static Long numero(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(v).strip());
        } catch (NumberFormatException e) {
            throw new ErrorUsuario("Uno de los productos del carrito no es válido.");
        }
    }

    private static int entero(Object v) {
        Long n = numero(v);
        return n == null || n > Integer.MAX_VALUE ? 0 : n.intValue();
    }
}
