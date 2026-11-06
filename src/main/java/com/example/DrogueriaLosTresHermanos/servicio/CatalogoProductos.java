package com.example.DrogueriaLosTresHermanos.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Catálogo de medicamentos y variedades. Mantiene una copia en memoria para responder rápido
 * y guarda cada cambio en su almacén: MySQL si hay conexión configurada, o un archivo local.
 */
@Service
public class CatalogoProductos {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogoProductos.class);

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

    private final AlmacenCatalogo almacen;
    private final Map<String, Categoria> categorias = new LinkedHashMap<>();
    private final Map<Integer, Producto> productos = new LinkedHashMap<>();

    @Autowired
    public CatalogoProductos(AlmacenCatalogo almacen) {
        this.almacen = almacen;
        recargar();
        LOG.info("Catálogo cargado desde {}: {} productos en {} categorías", almacen.origen(), productos.size(), categorias.size());
    }

    /** Catálogo en un archivo local; lo usan las pruebas. */
    public CatalogoProductos(String archivo) {
        this(new AlmacenCatalogoArchivo(archivo));
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
        int provisional = id != null ? id : productos.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        List<Presentacion> presentaciones = new ArrayList<>();
        if (MEDICAMENTO.equals(d.tipo())) {
            int n = 1;
            for (Presentacion p : d.presentaciones()) {
                String presId = p.id() == null || p.id().isBlank() ? provisional + "-n" + n : p.id();
                presentaciones.add(new Presentacion(presId, p.tipo().strip(), p.conc().strip(), p.marca().strip(), p.precio()));
                n++;
            }
        }
        Producto nuevo = new Producto(provisional, d.tipo(), d.nombre().strip(), limpio(d.descripcion()), d.categoria(),
            limpio(d.principioActivo()), limpio(d.contenido()), d.formula(), d.disponibilidad(),
            previo == null || previo.activo(), foto != null ? foto : previo == null ? "" : previo.foto(),
            previo == null ? "" : previo.imagen(), VARIEDAD.equals(d.tipo()) ? d.precio() : 0, presentaciones);
        int definitivo = almacen.guardarProducto(nuevo, previo == null);
        productos.put(definitivo, conId(nuevo, definitivo));
        despuesDeCambiar();
        return Map.of();
    }

    /** Cambia solo la foto. Devuelve false si el producto ya no existe. */
    public synchronized boolean cambiarFoto(int id, String foto) {
        Producto p = productos.get(id);
        if (p == null) {
            return false;
        }
        almacen.cambiarFoto(id, foto);
        productos.put(id, new Producto(p.id(), p.tipo(), p.nombre(), p.descripcion(), p.categoria(), p.principioActivo(),
            p.contenido(), p.formula(), p.disponibilidad(), p.activo(), foto, p.imagen(), p.precio(), p.presentaciones()));
        despuesDeCambiar();
        return true;
    }

    public synchronized void cambiarVisibilidad(int id, boolean activo) {
        Producto p = productos.get(id);
        if (p != null) {
            almacen.cambiarVisibilidad(id, activo);
            productos.put(id, new Producto(p.id(), p.tipo(), p.nombre(), p.descripcion(), p.categoria(), p.principioActivo(),
                p.contenido(), p.formula(), p.disponibilidad(), activo, p.foto(), p.imagen(), p.precio(), p.presentaciones()));
            despuesDeCambiar();
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
            Categoria editada = new Categoria(id, limpioNombre, actual.tipo());
            almacen.guardarCategoria(editada, false);
            categorias.put(id, editada);
        } else {
            if (!MEDICAMENTO.equals(tipo) && !VARIEDAD.equals(tipo)) {
                return "Elige el tipo de la categoría.";
            }
            String nuevo = identificador(limpioNombre);
            if (nuevo.isEmpty() || categorias.containsKey(nuevo)) {
                return "Ya existe una categoría con ese nombre.";
            }
            Categoria creada = new Categoria(nuevo, limpioNombre, tipo);
            almacen.guardarCategoria(creada, true);
            categorias.put(nuevo, creada);
        }
        despuesDeCambiar();
        return null;
    }

    public synchronized String eliminarCategoria(String id) {
        if (!categorias.containsKey(id)) {
            return "La categoría ya no existe.";
        }
        if (usosCategoria(id) > 0) {
            return "No se puede eliminar: todavía tiene productos. Muévelos a otra categoría primero.";
        }
        almacen.eliminarCategoria(id);
        categorias.remove(id);
        despuesDeCambiar();
        return null;
    }

    // ── Almacén ───────────────────────────────────────────────

    private void recargar() {
        AlmacenCatalogo.Contenido c = almacen.cargar();
        categorias.clear();
        c.categorias().forEach(x -> categorias.put(x.id(), x));
        productos.clear();
        c.productos().forEach(x -> productos.put(x.id(), x));
    }

    private void despuesDeCambiar() {
        if (almacen.recargarTrasCambios()) {
            recargar();
        } else {
            almacen.sincronizar(new AlmacenCatalogo.Contenido(List.copyOf(categorias.values()), List.copyOf(productos.values())));
        }
    }

    private static Producto conId(Producto p, int id) {
        return p.id() == id ? p : new Producto(id, p.tipo(), p.nombre(), p.descripcion(), p.categoria(), p.principioActivo(), p.contenido(),
            p.formula(), p.disponibilidad(), p.activo(), p.foto(), p.imagen(), p.precio(), p.presentaciones());
    }

    private static String identificador(String nombre) {
        return Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private static String limpio(String v) {
        return v == null ? "" : v.strip();
    }
}
