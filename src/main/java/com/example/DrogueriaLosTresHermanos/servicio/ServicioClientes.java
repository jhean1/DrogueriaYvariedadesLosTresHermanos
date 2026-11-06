package com.example.DrogueriaLosTresHermanos.servicio;

import com.example.DrogueriaLosTresHermanos.servicio.BaseDatos.ErrorUsuario;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Cuentas de clientes de la tienda: registro, acceso, perfil, favoritos y eliminación. */
@Service
public class ServicioClientes {

    public record Cliente(long id, String nombre, String correo, String telefono, String direccion, String foto, String registro) {}

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern TELEFONO = Pattern.compile("^[0-9 +()-]{7,20}$");
    private static final int CLAVE_MINIMA = 8;
    private static final int CLAVE_MAXIMA = 72;
    private static final int MAX_FALLOS = 5;
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private record Fallos(int cantidad, Instant expira) {}

    private final BaseDatos bd;
    private final PasswordEncoder codificador;
    private final Map<String, Fallos> fallos = new ConcurrentHashMap<>();

    public ServicioClientes(BaseDatos bd, PasswordEncoder codificador) {
        this.bd = bd;
        this.codificador = codificador;
    }

    public boolean disponible() {
        return bd.disponible();
    }

    public long registrar(String nombre, String correo, String telefono, String direccion, String clave, boolean aceptaDatos, Long creadoPor) {
        if (!aceptaDatos) {
            throw new ErrorUsuario("Debes aceptar el tratamiento de tus datos personales para crear la cuenta.");
        }
        String n = texto(nombre, "Escribe tu nombre completo.", 120);
        String c = correo(correo);
        String t = telefono(telefono);
        String d = texto(direccion, "Escribe tu dirección de entrega.", 255);
        validarClave(clave);
        String version = bd.jdbc().queryForList("SELECT valor_configuracion FROM configuracion WHERE clave_configuracion = 'version_politica_datos'",
            String.class).stream().findFirst().orElse("1.0");
        try {
            bd.jdbc().update("INSERT INTO cliente (nombre_cliente, correo_cliente, telefono_cliente, direccion_cliente, clave_hash, creado_por_id, "
                    + "fecha_autorizacion_datos, version_politica) VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)",
                n, c, t, d, codificador.encode(clave), creadoPor, version);
        } catch (DuplicateKeyException e) {
            throw new ErrorUsuario("Ya existe una cuenta con ese correo. Inicia sesión o usa otro correo.");
        }
        return bd.jdbc().queryForObject("SELECT id FROM cliente WHERE correo_cliente = ?", Long.class, c);
    }

    /** Devuelve el id del cliente si el correo y la clave son correctos. Bloquea tras varios intentos fallidos. */
    public long ingresar(String correo, String clave, String ip) {
        Fallos f = fallos.get(ip);
        if (f != null && Instant.now().isBefore(f.expira()) && f.cantidad() >= MAX_FALLOS) {
            throw new ErrorUsuario("Demasiados intentos. Espera unos minutos y vuelve a intentarlo.");
        }
        String c = correo == null ? "" : correo.strip().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> filas = bd.jdbc().queryForList(
            "SELECT id, clave_hash FROM cliente WHERE correo_cliente = ? AND cuenta_activa = TRUE", c);
        boolean correcto = !filas.isEmpty() && clave != null && clave.length() <= CLAVE_MAXIMA
            && codificador.matches(clave, (String) filas.get(0).get("clave_hash"));
        if (!correcto) {
            Instant ahora = Instant.now();
            fallos.values().removeIf(x -> ahora.isAfter(x.expira()));
            fallos.merge(ip, new Fallos(1, ahora.plus(BLOQUEO)), (a, b) -> new Fallos(a.cantidad() + 1, b.expira()));
            throw new ErrorUsuario("El correo o la contraseña no son correctos.");
        }
        fallos.remove(ip);
        return ((Number) filas.get(0).get("id")).longValue();
    }

    public Optional<Cliente> obtener(long id) {
        return bd.jdbc().query("SELECT id, nombre_cliente, correo_cliente, telefono_cliente, direccion_cliente, foto_cliente, "
                + "DATE_FORMAT(fecha_registro, '%Y-%m-%d') FROM cliente WHERE id = ? AND cuenta_activa = TRUE",
            (r, n) -> new Cliente(r.getLong(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5), r.getString(6), r.getString(7)), id)
            .stream().findFirst();
    }

    public void actualizar(long id, String nombre, String telefono, String direccion) {
        bd.jdbc().update("UPDATE cliente SET nombre_cliente = ?, telefono_cliente = ?, direccion_cliente = ? WHERE id = ? AND cuenta_activa = TRUE",
            texto(nombre, "Escribe tu nombre completo.", 120), telefono(telefono), texto(direccion, "Escribe tu dirección de entrega.", 255), id);
    }

    public void cambiarFoto(long id, String foto) {
        bd.jdbc().update("UPDATE cliente SET foto_cliente = ? WHERE id = ? AND cuenta_activa = TRUE", foto, id);
    }

    public void cambiarClave(long id, String actual, String nueva) {
        String hash = bd.jdbc().queryForObject("SELECT clave_hash FROM cliente WHERE id = ?", String.class, id);
        if (actual == null || hash == null || !codificador.matches(actual, hash)) {
            throw new ErrorUsuario("La contraseña actual no es correcta.");
        }
        validarClave(nueva);
        bd.jdbc().update("UPDATE cliente SET clave_hash = ? WHERE id = ?", codificador.encode(nueva), id);
    }

    /** Cambia el correo de acceso. Pide la contraseña para que nadie con la sesión abierta se quede con la cuenta. */
    public void cambiarCorreo(long id, String nuevo, String clave) {
        String hash = bd.jdbc().queryForObject("SELECT clave_hash FROM cliente WHERE id = ?", String.class, id);
        if (clave == null || hash == null || !codificador.matches(clave, hash)) {
            throw new ErrorUsuario("La contraseña no es correcta.");
        }
        String c = correo(nuevo);
        try {
            bd.jdbc().update("UPDATE cliente SET correo_cliente = ? WHERE id = ? AND cuenta_activa = TRUE", c, id);
        } catch (DuplicateKeyException e) {
            throw new ErrorUsuario("Ese correo ya está en uso por otra cuenta.");
        }
    }

    /** Borra los datos personales y conserva las compras sin ellos (Ley 1581 de 2012). */
    public void eliminar(long id, String clave) {
        String hash = bd.jdbc().queryForObject("SELECT clave_hash FROM cliente WHERE id = ?", String.class, id);
        if (clave == null || hash == null || !codificador.matches(clave, hash)) {
            throw new ErrorUsuario("La contraseña no es correcta.");
        }
        try {
            bd.jdbc().update("CALL anonimizar_cliente(?)", id);
        } catch (DataAccessException e) {
            String regla = BaseDatos.mensajeDeRegla(e);
            if (regla != null) {
                throw new ErrorUsuario(regla);
            }
            throw e;
        }
    }

    // ── Favoritos ─────────────────────────────────────────────

    public List<Long> favoritos(long cliente) {
        return bd.jdbc().queryForList("SELECT producto_id FROM favorito WHERE cliente_id = ? ORDER BY fecha_favorito DESC", Long.class, cliente);
    }

    public void agregarFavorito(long cliente, long producto) {
        Integer existe = bd.jdbc().queryForObject("SELECT COUNT(*) FROM producto WHERE id = ? AND producto_activo = TRUE", Integer.class, producto);
        if (existe == null || existe == 0) {
            throw new ErrorUsuario("Ese producto no está disponible.");
        }
        bd.jdbc().update("INSERT IGNORE INTO favorito (cliente_id, producto_id) VALUES (?, ?)", cliente, producto);
    }

    public void quitarFavorito(long cliente, long producto) {
        bd.jdbc().update("DELETE FROM favorito WHERE cliente_id = ? AND producto_id = ?", cliente, producto);
    }

    // ── Para el panel ─────────────────────────────────────────

    public List<Map<String, Object>> listarParaPanel() {
        return bd.jdbc().queryForList("SELECT c.id, c.nombre_cliente AS nombre, c.correo_cliente AS correo, c.telefono_cliente AS telefono, "
            + "c.direccion_cliente AS direccion, DATE_FORMAT(c.fecha_registro, '%Y-%m-%d %H:%i') AS registro, c.creado_por_id IS NOT NULL AS creadoEnTienda, "
            + "(SELECT COUNT(*) FROM pedido p WHERE p.cliente_id = c.id) AS pedidos, "
            + "(SELECT pl.nombre_plan FROM suscripcion s JOIN plan_suscripcion pl ON pl.id = s.plan_id WHERE s.cliente_id = c.id "
            + " AND s.estado_suscripcion IN ('activa','cancelada') AND s.fecha_vencimiento > NOW() ORDER BY s.fecha_vencimiento DESC LIMIT 1) AS plan "
            + "FROM cliente c WHERE c.cuenta_activa = TRUE ORDER BY c.fecha_registro DESC");
    }

    // ── Validaciones ──────────────────────────────────────────

    private static String texto(String v, String mensaje, int maximo) {
        String t = v == null ? "" : v.strip();
        if (t.isEmpty()) {
            throw new ErrorUsuario(mensaje);
        }
        if (t.length() > maximo) {
            throw new ErrorUsuario("El texto es demasiado largo (máximo " + maximo + " caracteres).");
        }
        return t;
    }

    private static String correo(String v) {
        String c = v == null ? "" : v.strip().toLowerCase(Locale.ROOT);
        if (c.length() > 120 || !CORREO.matcher(c).matches()) {
            throw new ErrorUsuario("Escribe un correo válido.");
        }
        return c;
    }

    private static String telefono(String v) {
        String t = v == null ? "" : v.strip();
        if (!TELEFONO.matcher(t).matches()) {
            throw new ErrorUsuario("Escribe un teléfono válido.");
        }
        return t;
    }

    private static void validarClave(String clave) {
        if (clave == null || clave.length() < CLAVE_MINIMA) {
            throw new ErrorUsuario("La contraseña debe tener al menos " + CLAVE_MINIMA + " caracteres.");
        }
        if (clave.length() > CLAVE_MAXIMA) {
            throw new ErrorUsuario("La contraseña no puede tener más de " + CLAVE_MAXIMA + " caracteres.");
        }
    }
}
