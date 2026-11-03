package com.example.DrogueriaLosTresHermanos.configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Roles, cuentas del equipo y auditoría del panel. Es la fuente de verdad de los permisos.
 *
 * Mientras no exista base de datos, los cambios se guardan en un archivo local
 * (app.datos). Reemplazar esta clase por repositorios no afecta a las vistas ni a las reglas.
 *
 * Reglas contra la escalada de privilegios, aplicadas aquí y no en la interfaz:
 * nadie concede ni retira un permiso que no tiene, nadie se modifica a sí mismo ni a su propio
 * rol, y solo el propietario puede tocar lo relacionado con el rol de propietario.
 */
@Service
public class DirectorioAccesos implements UserDetailsService {

    public record Rol(String id, String nombre, String descripcion, Set<String> permisos, boolean protegido) {
        public boolean tiene(String permiso) {
            return permisos.contains(permiso);
        }
    }

    public record Cuenta(String id, String correo, String claveHash, String rolId, boolean activa,
                         Set<String> concedidos, Set<String> revocados, boolean protegida) {}

    public record Evento(Instant fecha, String actor, String accion, String detalle) {}

    /** Quien ejecuta un cambio, con sus permisos efectivos en este momento. */
    public record Actor(String correo, Set<String> permisos, String rolId) {
        boolean esPropietario() {
            return RolSistema.PROPIETARIO.name().equals(rolId);
        }
    }

    /** Operación rechazada; su mensaje es seguro para mostrarlo al usuario. */
    public static class OperacionNoPermitida extends RuntimeException {
        public OperacionNoPermitida(String mensaje) {
            super(mensaje);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(DirectorioAccesos.class);
    private static final int LONGITUD_MINIMA_CLAVE = 12;
    private static final int LONGITUD_MAXIMA_BCRYPT = 72;
    private static final int MAX_EVENTOS = 500;
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final PasswordEncoder codificador;
    private final Path archivo;
    private final Map<String, Rol> roles = new LinkedHashMap<>();
    private final Map<String, Cuenta> cuentas = new LinkedHashMap<>();
    private final Deque<Evento> eventos = new ArrayDeque<>();

    public DirectorioAccesos(@Value("${app.admin.correo}") String correoPropietario,
                             @Value("${app.admin.clave:}") String clavePropietario,
                             @Value("${app.datos}") String archivo,
                             EquipoProperties equipo,
                             PasswordEncoder codificador) {
        this.codificador = codificador;
        this.archivo = Path.of(archivo);

        for (RolSistema r : RolSistema.values()) {
            Set<String> codigos = r.getPermisos().stream().map(Permiso::getCodigo).collect(Collectors.toCollection(TreeSet::new));
            roles.put(r.name(), new Rol(r.name(), r.getNombre(), r.getDescripcion(), codigos, r == RolSistema.PROPIETARIO));
        }
        cargar();

        // La cuenta del propietario siempre sale del entorno y no se guarda en el archivo.
        String dueno = normalizar(correoPropietario);
        cuentas.put(dueno, new Cuenta("propietario", dueno, codificador.encode(resolverClave(clavePropietario)),
            RolSistema.PROPIETARIO.name(), true, Set.of(), Set.of(), true));

        for (EquipoProperties.Miembro m : equipo.equipo()) {
            if (m.correo() == null || m.correo().isBlank() || m.rol() == null) {
                throw new IllegalStateException("Cada miembro del equipo necesita correo y rol.");
            }
            validarClaveInicial(m.clave(), "La clave de " + m.correo());
            String correo = normalizar(m.correo());
            String hash = codificador.encode(m.clave());
            Cuenta previa = cuentas.get(correo);
            // El rol y el estado definidos desde el panel prevalecen sobre el entorno.
            cuentas.put(correo, previa != null
                ? new Cuenta(previa.id(), correo, hash, previa.rolId(), previa.activa(), previa.concedidos(), previa.revocados(), false)
                : new Cuenta(nuevoId(), correo, hash, m.rol().name(), true, Set.of(), Set.of(), false));
        }
        guardar();
    }

    // ── Spring Security ───────────────────────────────────────

    @Override
    public synchronized UserDetails loadUserByUsername(String correo) {
        Cuenta c = cuentas.get(normalizar(correo));
        if (c == null) {
            throw new UsernameNotFoundException("Cuenta no encontrada");
        }
        return User.withUsername(c.correo()).password(c.claveHash())
            .disabled(!c.activa()).authorities(autoridades(c)).build();
    }

    /** Autoridades vigentes de una cuenta activa; vacío si ya no existe o fue desactivada. */
    public synchronized Optional<List<GrantedAuthority>> autoridadesVigentes(String correo) {
        Cuenta c = cuentas.get(normalizar(correo));
        return c != null && c.activa() ? Optional.of(autoridades(c)) : Optional.empty();
    }

    public synchronized PerfilAdmin perfil(Authentication autenticacion) {
        Set<String> permisos = autenticacion.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority).filter(a -> !a.startsWith("ROLE_"))
            .collect(Collectors.toUnmodifiableSet());
        Cuenta c = cuentas.get(normalizar(autenticacion.getName()));
        Rol rol = c == null ? null : roles.get(c.rolId());
        List<ModuloGestion> modulos = Stream.of(ModuloGestion.values())
            .filter(m -> permisos.contains(m.getPermiso().getCodigo())).toList();
        return new PerfilAdmin(autenticacion.getName(), c == null ? "" : c.rolId(),
            rol == null ? "Sin rol" : rol.nombre(), permisos, modulos);
    }

    public Actor actor(Authentication autenticacion) {
        PerfilAdmin p = perfil(autenticacion);
        return new Actor(p.correo(), p.permisos(), p.rolId());
    }

    @EventListener
    public void alEntrar(AuthenticationSuccessEvent evento) {
        registrar(evento.getAuthentication().getName(), "Inicio de sesión", "Acceso correcto");
    }

    @EventListener
    public void alFallar(AbstractAuthenticationFailureEvent evento) {
        // Solo se anota el correo si existe: en ese campo a veces se escribe una contraseña por error.
        String nombre = evento.getAuthentication().getName();
        boolean conocida;
        synchronized (this) {
            conocida = nombre != null && cuentas.containsKey(normalizar(nombre));
        }
        registrar(conocida ? nombre : "Cuenta desconocida", "Inicio de sesión fallido", "Acceso rechazado");
    }

    /** Registra una acción de otro módulo del panel en la auditoría. */
    public void auditar(String actor, String accion, String detalle) {
        registrar(actor, accion, detalle);
    }

    // ── Consultas ─────────────────────────────────────────────

    public synchronized List<Rol> roles() {
        return List.copyOf(roles.values());
    }

    public synchronized List<Cuenta> cuentas() {
        return List.copyOf(cuentas.values());
    }

    public synchronized List<Evento> eventos() {
        return List.copyOf(eventos);
    }

    public synchronized String nombreRol(String id) {
        Rol r = roles.get(id);
        return r == null ? id : r.nombre();
    }

    // ── Roles ─────────────────────────────────────────────────

    public synchronized void crearRol(Actor actor, String nombre, String descripcion, String basadoEn) {
        String limpio = texto(nombre, 60, "El nombre del rol");
        String detalle = descripcion == null ? "" : descripcion.strip();
        if (detalle.length() > 200) {
            throw new OperacionNoPermitida("La descripción no puede superar 200 caracteres.");
        }
        if (roles.values().stream().anyMatch(r -> r.nombre().equalsIgnoreCase(limpio))) {
            throw new OperacionNoPermitida("Ya existe un rol con ese nombre.");
        }
        // Un rol nuevo solo hereda los permisos de la base que quien lo crea también tiene.
        Rol base = basadoEn == null || basadoEn.isBlank() ? null : roles.get(basadoEn);
        Set<String> permisos = new TreeSet<>(base == null ? Set.of() : base.permisos());
        permisos.retainAll(actor.permisos());
        permisos.add(Permiso.DASHBOARD_VER.getCodigo());
        String id = "ROL_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        roles.put(id, new Rol(id, limpio, detalle, permisos, false));
        anotar(actor.correo(), "Rol creado", limpio);
        guardar();
    }

    public synchronized void cambiarPermisosRol(Actor actor, String rolId, Collection<String> pedidos) {
        Rol rol = roles.get(rolId);
        if (rol == null) {
            throw new OperacionNoPermitida("El rol no existe.");
        }
        if (rol.protegido()) {
            throw new OperacionNoPermitida("El rol de propietario no se puede modificar.");
        }
        if (rolId.equals(actor.rolId())) {
            throw new OperacionNoPermitida("No puedes modificar los permisos de tu propio rol.");
        }
        Set<String> nuevos = aplicarEnAlcance(actor, rol.permisos(), pedidos);
        roles.put(rolId, new Rol(rol.id(), rol.nombre(), rol.descripcion(), nuevos, false));
        anotar(actor.correo(), "Permisos de rol cambiados", rol.nombre() + ": " + diferencia(rol.permisos(), nuevos));
        guardar();
    }

    // ── Cuentas ───────────────────────────────────────────────

    public synchronized void crearCuenta(Actor actor, String correo, String clave, String rolId) {
        String limpio = normalizar(correo == null ? "" : correo);
        if (!CORREO.matcher(limpio).matches() || limpio.length() > 120) {
            throw new OperacionNoPermitida("Escribe un correo válido.");
        }
        if (cuentas.containsKey(limpio)) {
            throw new OperacionNoPermitida("Ya existe una cuenta con ese correo.");
        }
        if (clave == null || clave.length() < LONGITUD_MINIMA_CLAVE) {
            throw new OperacionNoPermitida("La contraseña debe tener al menos " + LONGITUD_MINIMA_CLAVE + " caracteres.");
        }
        if (clave.getBytes(StandardCharsets.UTF_8).length > LONGITUD_MAXIMA_BCRYPT) {
            throw new OperacionNoPermitida("La contraseña es demasiado larga.");
        }
        comprobarRolAsignable(actor, rolId);
        cuentas.put(limpio, new Cuenta(nuevoId(), limpio, codificador.encode(clave), rolId, true, Set.of(), Set.of(), false));
        anotar(actor.correo(), "Cuenta creada", limpio + " con rol " + nombreRol(rolId));
        guardar();
    }

    public synchronized void cambiarRolCuenta(Actor actor, String cuentaId, String rolId) {
        Cuenta c = cuentaGestionable(actor, cuentaId);
        comprobarRolAsignable(actor, rolId);
        cuentas.put(c.correo(), new Cuenta(c.id(), c.correo(), c.claveHash(), rolId, c.activa(), c.concedidos(), c.revocados(), false));
        anotar(actor.correo(), "Rol de cuenta cambiado", c.correo() + ": " + nombreRol(c.rolId()) + " a " + nombreRol(rolId));
        guardar();
    }

    public synchronized void cambiarEstadoCuenta(Actor actor, String cuentaId, boolean activa) {
        Cuenta c = cuentaGestionable(actor, cuentaId);
        cuentas.put(c.correo(), new Cuenta(c.id(), c.correo(), c.claveHash(), c.rolId(), activa, c.concedidos(), c.revocados(), false));
        anotar(actor.correo(), activa ? "Cuenta activada" : "Cuenta desactivada", c.correo());
        guardar();
    }

    /** Excepciones individuales sobre los permisos del rol: se conceden o se revocan por cuenta. */
    public synchronized void cambiarPermisosCuenta(Actor actor, String cuentaId, Collection<String> concedidos, Collection<String> revocados) {
        Cuenta c = cuentaGestionable(actor, cuentaId);
        Set<String> nuevosConcedidos = aplicarEnAlcance(actor, c.concedidos(), concedidos);
        Set<String> nuevosRevocados = aplicarEnAlcance(actor, c.revocados(), revocados);
        nuevosRevocados.removeAll(nuevosConcedidos);
        cuentas.put(c.correo(), new Cuenta(c.id(), c.correo(), c.claveHash(), c.rolId(), c.activa(), nuevosConcedidos, nuevosRevocados, false));
        anotar(actor.correo(), "Permisos individuales cambiados",
            c.correo() + ": concedidos " + diferencia(c.concedidos(), nuevosConcedidos) + "; revocados " + diferencia(c.revocados(), nuevosRevocados));
        guardar();
    }

    // ── Reglas internas ───────────────────────────────────────

    private Cuenta cuentaGestionable(Actor actor, String cuentaId) {
        Cuenta c = cuentas.values().stream().filter(x -> x.id().equals(cuentaId)).findFirst()
            .orElseThrow(() -> new OperacionNoPermitida("La cuenta no existe."));
        if (c.protegida()) {
            throw new OperacionNoPermitida("La cuenta del propietario se administra desde la configuración del servidor.");
        }
        if (c.correo().equals(normalizar(actor.correo()))) {
            throw new OperacionNoPermitida("No puedes modificar tu propia cuenta.");
        }
        if (RolSistema.PROPIETARIO.name().equals(c.rolId()) && !actor.esPropietario()) {
            throw new OperacionNoPermitida("Solo el propietario puede modificar esta cuenta.");
        }
        return c;
    }

    private void comprobarRolAsignable(Actor actor, String rolId) {
        Rol rol = roles.get(rolId);
        if (rol == null) {
            throw new OperacionNoPermitida("El rol no existe.");
        }
        if (!actor.permisos().containsAll(rol.permisos())) {
            throw new OperacionNoPermitida("No puedes asignar un rol con permisos que tú no tienes.");
        }
    }

    /** Solo cambia los permisos que el actor posee; los demás conservan su valor actual. */
    private static Set<String> aplicarEnAlcance(Actor actor, Set<String> actuales, Collection<String> pedidos) {
        Set<String> validos = Stream.of(Permiso.values()).map(Permiso::getCodigo).collect(Collectors.toSet());
        Set<String> resultado = new TreeSet<>(actuales);
        resultado.removeAll(actor.permisos());
        pedidos.stream().filter(validos::contains).filter(actor.permisos()::contains).forEach(resultado::add);
        return resultado;
    }

    private List<GrantedAuthority> autoridades(Cuenta c) {
        Rol rol = roles.get(c.rolId());
        Set<String> efectivos = new TreeSet<>(rol == null ? Set.of() : rol.permisos());
        efectivos.addAll(c.concedidos());
        efectivos.removeAll(c.revocados());
        List<GrantedAuthority> lista = new ArrayList<>();
        lista.add(new SimpleGrantedAuthority("ROLE_" + c.rolId()));
        efectivos.forEach(p -> lista.add(new SimpleGrantedAuthority(p)));
        return lista;
    }

    private static String diferencia(Set<String> antes, Set<String> despues) {
        List<String> cambios = new ArrayList<>();
        despues.stream().filter(p -> !antes.contains(p)).forEach(p -> cambios.add("+" + p));
        antes.stream().filter(p -> !despues.contains(p)).forEach(p -> cambios.add("-" + p));
        return cambios.isEmpty() ? "sin cambios" : String.join(" ", cambios);
    }

    private static String texto(String valor, int maximo, String campo) {
        String limpio = valor == null ? "" : valor.strip();
        if (limpio.isEmpty() || limpio.length() > maximo) {
            throw new OperacionNoPermitida(campo + " debe tener entre 1 y " + maximo + " caracteres.");
        }
        return limpio;
    }

    private static String normalizar(String correo) {
        return correo.strip().toLowerCase(Locale.ROOT);
    }

    private static String nuevoId() {
        return UUID.randomUUID().toString().substring(0, 12);
    }

    private static String resolverClave(String clave) {
        if (clave == null || clave.isBlank()) {
            byte[] aleatorio = new byte[18];
            new SecureRandom().nextBytes(aleatorio);
            String generada = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
            log.warn("ADMIN_CLAVE no está definida. Clave temporal del administrador (cambia en cada arranque): {}", generada);
            return generada;
        }
        validarClaveInicial(clave, "ADMIN_CLAVE");
        return clave;
    }

    private static void validarClaveInicial(String clave, String nombre) {
        if (clave == null || clave.length() < LONGITUD_MINIMA_CLAVE) {
            throw new IllegalStateException(nombre + " debe tener al menos " + LONGITUD_MINIMA_CLAVE + " caracteres.");
        }
        if (clave.getBytes(StandardCharsets.UTF_8).length > LONGITUD_MAXIMA_BCRYPT) {
            throw new IllegalStateException(nombre + " no puede superar " + LONGITUD_MAXIMA_BCRYPT + " bytes.");
        }
    }

    // ── Auditoría y persistencia ──────────────────────────────

    private synchronized void registrar(String actor, String accion, String detalle) {
        anotar(actor, accion, detalle);
        guardar();
    }

    private void anotar(String actor, String accion, String detalle) {
        eventos.addFirst(new Evento(Instant.now(), actor, accion, detalle));
        while (eventos.size() > MAX_EVENTOS) {
            eventos.removeLast();
        }
    }

    private void cargar() {
        if (!Files.exists(archivo)) {
            return;
        }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + archivo, e);
        }
        for (int i = 0; p.containsKey("rol." + i + ".id"); i++) {
            String id = p.getProperty("rol." + i + ".id");
            if (!RolSistema.PROPIETARIO.name().equals(id)) {
                roles.put(id, new Rol(id, p.getProperty("rol." + i + ".nombre"), p.getProperty("rol." + i + ".descripcion", ""),
                    lista(p.getProperty("rol." + i + ".permisos")), false));
            }
        }
        for (int i = 0; p.containsKey("cuenta." + i + ".id"); i++) {
            String correo = p.getProperty("cuenta." + i + ".correo");
            cuentas.put(correo, new Cuenta(p.getProperty("cuenta." + i + ".id"), correo, p.getProperty("cuenta." + i + ".clave"),
                p.getProperty("cuenta." + i + ".rol"), Boolean.parseBoolean(p.getProperty("cuenta." + i + ".activa")),
                lista(p.getProperty("cuenta." + i + ".concedidos")), lista(p.getProperty("cuenta." + i + ".revocados")), false));
        }
        for (int i = 0; p.containsKey("evento." + i + ".fecha"); i++) {
            eventos.addLast(new Evento(Instant.parse(p.getProperty("evento." + i + ".fecha")), p.getProperty("evento." + i + ".actor"),
                p.getProperty("evento." + i + ".accion"), p.getProperty("evento." + i + ".detalle")));
        }
    }

    private void guardar() {
        Properties p = new Properties();
        int i = 0;
        for (Rol r : roles.values()) {
            p.setProperty("rol." + i + ".id", r.id());
            p.setProperty("rol." + i + ".nombre", r.nombre());
            p.setProperty("rol." + i + ".descripcion", r.descripcion());
            p.setProperty("rol." + i + ".permisos", String.join(",", r.permisos()));
            i++;
        }
        i = 0;
        for (Cuenta c : cuentas.values()) {
            if (c.protegida()) {
                continue;
            }
            p.setProperty("cuenta." + i + ".id", c.id());
            p.setProperty("cuenta." + i + ".correo", c.correo());
            p.setProperty("cuenta." + i + ".clave", c.claveHash());
            p.setProperty("cuenta." + i + ".rol", c.rolId());
            p.setProperty("cuenta." + i + ".activa", String.valueOf(c.activa()));
            p.setProperty("cuenta." + i + ".concedidos", String.join(",", c.concedidos()));
            p.setProperty("cuenta." + i + ".revocados", String.join(",", c.revocados()));
            i++;
        }
        i = 0;
        for (Evento e : eventos) {
            p.setProperty("evento." + i + ".fecha", e.fecha().toString());
            p.setProperty("evento." + i + ".actor", e.actor());
            p.setProperty("evento." + i + ".accion", e.accion());
            p.setProperty("evento." + i + ".detalle", e.detalle());
            i++;
        }
        try {
            Path carpeta = archivo.toAbsolutePath().getParent();
            Files.createDirectories(carpeta);
            Path temporal = Files.createTempFile(carpeta, "accesos", ".tmp");
            try (Writer w = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                p.store(w, "Accesos del panel. No editar a mano ni subir al repositorio.");
            }
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar " + archivo, e);
        }
    }

    private static Set<String> lista(String valor) {
        return valor == null || valor.isBlank()
            ? new TreeSet<>()
            : new TreeSet<>(Arrays.asList(valor.split(",")));
    }
}
