package com.example.DrogueriaLosTresHermanos.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Catálogo de medicamentos y variedades. El primer arranque parte de
 * catalogo-inicial.properties; los cambios del panel se guardan en un archivo local.
 */
@Service
public class CatalogoProductos {

    public static final String MEDICAMENTO = "medicamento";
    public static final String VARIEDAD = "variedad";
    public static final String SIN_DATO = "sin-dato";
    private static final Set<String> DISPONIBILIDADES = Set.of(SIN_DATO, "disponible", "agotado");
    private static final int PRECIO_MAXIMO = 10_000_000;

    public record Categoria(String id, String nombre, String tipo) {}

    public record Presentacion(String id, String tipo, String conc, String marca, int precio) {}

    public record Producto(int id, String tipo, String nombre, String descripcion, String categoria,
                           String principioActivo, String contenido, boolean formula, String disponibilidad,
                           boolean activo, String foto, String imagen, int precio, List<Presentacion> presentaciones) {
        public boolean esMedicamento() {
            return MEDICAMENTO.equals(tipo);
        }

        public int precioDesde() {
            return esMedicamento()
                ? presentaciones.stream().mapToInt(Presentacion::precio).min().orElse(0)
                : precio;
        }
    }

    /** Datos enviados desde el formulario, antes de validar. */
    public record Datos(String tipo, String nombre, String descripcion, String categoria, String principioActivo,
                        String contenido, boolean formula, String disponibilidad, int precio,
                        List<Presentacion> presentaciones) {}

    private final Path archivo;
    private final Map<String, Categoria> categorias = new LinkedHashMap<>();
    private final Map<Integer, Producto> productos = new LinkedHashMap<>();

    public CatalogoProductos(@Value("${app.catalogo}") String archivo) {
        this.archivo = Path.of(archivo);
        cargar();
    }

    // ── Consultas ─────────────────────────────────────────────

    public synchronized List<Producto> todos() {
        return List.copyOf(productos.values());
    }

    public synchronized List<Producto> visibles(String tipo) {
        return productos.values().stream().filter(p -> p.activo() && tipo.equals(p.tipo())).toList();
    }

    public synchronized Optional<Producto> buscar(int id) {
        return Optional.ofNullable(productos.get(id));
    }

    public synchronized List<Categoria> categorias() {
        return List.copyOf(categorias.values());
    }

    public synchronized List<Categoria> categorias(String tipo) {
        return categorias.values().stream().filter(c -> tipo.equals(c.tipo())).toList();
    }

    public synchronized String nombreCategoria(String id) {
        Categoria c = categorias.get(id);
        return c == null ? id : c.nombre();
    }

    public synchronized long usosCategoria(String id) {
        return productos.values().stream().filter(p -> id.equals(p.categoria())).count();
    }

    // ── Productos ─────────────────────────────────────────────

    /** Devuelve los errores por campo; si está vacío, el producto quedó guardado. */
    public synchronized Map<String, String> guardar(Integer id, Datos d, String foto) {
        Map<String, String> errores = validar(d);
        Producto previo = id == null ? null : productos.get(id);
        if (id != null && previo == null) {
            errores.put("general", "El producto ya no existe.");
        }
        if (!errores.isEmpty()) {
            return errores;
        }
        int nuevoId = id != null ? id : productos.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        List<Presentacion> presentaciones = new ArrayList<>();
        if (MEDICAMENTO.equals(d.tipo())) {
            int n = 1;
            for (Presentacion p : d.presentaciones()) {
                String presId = p.id() == null || p.id().isBlank() ? nuevoId + "-n" + n : p.id();
                presentaciones.add(new Presentacion(presId, p.tipo().strip(), p.conc().strip(), p.marca().strip(), p.precio()));
                n++;
            }
        }
        productos.put(nuevoId, new Producto(nuevoId, d.tipo(), d.nombre().strip(), limpio(d.descripcion()), d.categoria(),
            limpio(d.principioActivo()), limpio(d.contenido()), d.formula(), d.disponibilidad(),
            previo == null || previo.activo(), foto != null ? foto : previo == null ? "" : previo.foto(),
            previo == null ? "" : previo.imagen(), VARIEDAD.equals(d.tipo()) ? d.precio() : 0, presentaciones));
        persistir();
        return Map.of();
    }

    /** Cambia solo la foto. Devuelve false si el producto ya no existe. */
    public synchronized boolean cambiarFoto(int id, String foto) {
        Producto p = productos.get(id);
        if (p == null) {
            return false;
        }
        productos.put(id, new Producto(p.id(), p.tipo(), p.nombre(), p.descripcion(), p.categoria(), p.principioActivo(),
            p.contenido(), p.formula(), p.disponibilidad(), p.activo(), foto, p.imagen(), p.precio(), p.presentaciones()));
        persistir();
        return true;
    }

    public synchronized void cambiarVisibilidad(int id, boolean activo) {
        Producto p = productos.get(id);
        if (p != null) {
            productos.put(id, new Producto(p.id(), p.tipo(), p.nombre(), p.descripcion(), p.categoria(), p.principioActivo(),
                p.contenido(), p.formula(), p.disponibilidad(), activo, p.foto(), p.imagen(), p.precio(), p.presentaciones()));
            persistir();
        }
    }

    private Map<String, String> validar(Datos d) {
        Map<String, String> e = new LinkedHashMap<>();
        if (!MEDICAMENTO.equals(d.tipo()) && !VARIEDAD.equals(d.tipo())) {
            e.put("tipo", "Elige si es un medicamento o una variedad.");
        }
        if (d.nombre() == null || d.nombre().isBlank()) {
            e.put("nombre", "Escribe el nombre con el que el cliente lo reconoce.");
        } else if (d.nombre().strip().length() > 80) {
            e.put("nombre", "El nombre no puede superar 80 caracteres.");
        }
        if (limpio(d.descripcion()).length() > 300) {
            e.put("descripcion", "La descripción no puede superar 300 caracteres.");
        }
        Categoria c = d.categoria() == null ? null : categorias.get(d.categoria());
        if (c == null || !c.tipo().equals(d.tipo())) {
            e.put("categoria", "Elige una categoría de este tipo de producto.");
        }
        if (!DISPONIBILIDADES.contains(String.valueOf(d.disponibilidad()))) {
            e.put("disponibilidad", "Elige una opción de disponibilidad.");
        }
        if (MEDICAMENTO.equals(d.tipo())) {
            if (d.presentaciones() == null || d.presentaciones().isEmpty()) {
                e.put("presentaciones", "Agrega al menos una presentación con su precio.");
            } else {
                for (Presentacion p : d.presentaciones()) {
                    if (p.tipo().isBlank() || p.conc().isBlank() || p.marca().isBlank()) {
                        e.put("presentaciones", "Cada presentación necesita tipo, concentración y marca.");
                    } else if (p.precio() <= 0 || p.precio() > PRECIO_MAXIMO) {
                        e.put("presentaciones", "Cada presentación necesita un precio válido en pesos.");
                    }
                }
            }
        } else if (VARIEDAD.equals(d.tipo()) && (d.precio() <= 0 || d.precio() > PRECIO_MAXIMO)) {
            e.put("precio", "Escribe un precio válido en pesos.");
        }
        return e;
    }

    // ── Categorías ────────────────────────────────────────────

    /** Devuelve un mensaje de error, o null si la categoría quedó guardada. */
    public synchronized String guardarCategoria(String id, String nombre, String tipo) {
        String limpioNombre = nombre == null ? "" : nombre.strip();
        if (limpioNombre.isEmpty() || limpioNombre.length() > 40) {
            return "El nombre de la categoría debe tener entre 1 y 40 caracteres.";
        }
        if (id != null && !id.isBlank()) {
            Categoria actual = categorias.get(id);
            if (actual == null) {
                return "La categoría ya no existe.";
            }
            categorias.put(id, new Categoria(id, limpioNombre, actual.tipo()));
        } else {
            if (!MEDICAMENTO.equals(tipo) && !VARIEDAD.equals(tipo)) {
                return "Elige el tipo de la categoría.";
            }
            String nuevo = identificador(limpioNombre);
            if (nuevo.isEmpty() || categorias.containsKey(nuevo)) {
                return "Ya existe una categoría con ese nombre.";
            }
            categorias.put(nuevo, new Categoria(nuevo, limpioNombre, tipo));
        }
        persistir();
        return null;
    }

    public synchronized String eliminarCategoria(String id) {
        if (!categorias.containsKey(id)) {
            return "La categoría ya no existe.";
        }
        if (usosCategoria(id) > 0) {
            return "No se puede eliminar: todavía tiene productos. Muévelos a otra categoría primero.";
        }
        categorias.remove(id);
        persistir();
        return null;
    }

    private static String identificador(String nombre) {
        return Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private static String limpio(String v) {
        return v == null ? "" : v.strip();
    }

    // ── Persistencia ──────────────────────────────────────────

    private void cargar() {
        Properties p = new Properties();
        try {
            if (Files.exists(archivo)) {
                try (Reader r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                    p.load(r);
                }
            } else {
                try (Reader r = new InputStreamReader(new ClassPathResource("catalogo-inicial.properties").getInputStream(), StandardCharsets.UTF_8)) {
                    p.load(r);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el catálogo", e);
        }
        for (int i = 0; p.containsKey("cat." + i + ".id"); i++) {
            String id = p.getProperty("cat." + i + ".id");
            categorias.put(id, new Categoria(id, p.getProperty("cat." + i + ".nombre"), p.getProperty("cat." + i + ".tipo")));
        }
        List<Producto> leidos = new ArrayList<>();
        for (int i = 0; p.containsKey("p." + i + ".id"); i++) {
            String b = "p." + i + ".";
            List<Presentacion> pres = new ArrayList<>();
            for (int j = 0; p.containsKey(b + "pres." + j + ".id"); j++) {
                String q = b + "pres." + j + ".";
                pres.add(new Presentacion(p.getProperty(q + "id"), p.getProperty(q + "tipo"), p.getProperty(q + "conc"),
                    p.getProperty(q + "marca"), Integer.parseInt(p.getProperty(q + "precio"))));
            }
            leidos.add(new Producto(Integer.parseInt(p.getProperty(b + "id")), p.getProperty(b + "tipo"), p.getProperty(b + "nombre"),
                p.getProperty(b + "descripcion", ""), p.getProperty(b + "categoria"), p.getProperty(b + "principio", ""),
                p.getProperty(b + "contenido", ""), Boolean.parseBoolean(p.getProperty(b + "formula")),
                p.getProperty(b + "disponibilidad", SIN_DATO), Boolean.parseBoolean(p.getProperty(b + "activo", "true")),
                p.getProperty(b + "foto", ""), p.getProperty(b + "imagen", ""), Integer.parseInt(p.getProperty(b + "precio", "0")),
                List.copyOf(pres)));
        }
        leidos.stream().sorted(Comparator.comparingInt(Producto::id)).forEach(x -> productos.put(x.id(), x));
    }

    private void persistir() {
        Properties p = new Properties();
        int i = 0;
        for (Categoria c : categorias.values()) {
            p.setProperty("cat." + i + ".id", c.id());
            p.setProperty("cat." + i + ".nombre", c.nombre());
            p.setProperty("cat." + i + ".tipo", c.tipo());
            i++;
        }
        i = 0;
        for (Producto x : productos.values()) {
            String b = "p." + i + ".";
            p.setProperty(b + "id", String.valueOf(x.id()));
            p.setProperty(b + "tipo", x.tipo());
            p.setProperty(b + "nombre", x.nombre());
            p.setProperty(b + "descripcion", x.descripcion());
            p.setProperty(b + "categoria", x.categoria());
            p.setProperty(b + "principio", x.principioActivo());
            p.setProperty(b + "contenido", x.contenido());
            p.setProperty(b + "formula", String.valueOf(x.formula()));
            p.setProperty(b + "disponibilidad", x.disponibilidad());
            p.setProperty(b + "activo", String.valueOf(x.activo()));
            p.setProperty(b + "foto", x.foto());
            p.setProperty(b + "imagen", x.imagen());
            p.setProperty(b + "precio", String.valueOf(x.precio()));
            int j = 0;
            for (Presentacion q : x.presentaciones()) {
                String k = b + "pres." + j + ".";
                p.setProperty(k + "id", q.id());
                p.setProperty(k + "tipo", q.tipo());
                p.setProperty(k + "conc", q.conc());
                p.setProperty(k + "marca", q.marca());
                p.setProperty(k + "precio", String.valueOf(q.precio()));
                j++;
            }
            i++;
        }
        try {
            Path carpeta = archivo.toAbsolutePath().getParent();
            Files.createDirectories(carpeta);
            Path temporal = Files.createTempFile(carpeta, "catalogo", ".tmp");
            try (Writer w = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                p.store(w, "Catalogo de la drogueria. Se administra desde el panel.");
            }
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el catálogo", e);
        }
    }
}
