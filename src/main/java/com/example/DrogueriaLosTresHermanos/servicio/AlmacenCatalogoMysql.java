package com.example.DrogueriaLosTresHermanos.servicio;

import com.example.DrogueriaLosTresHermanos.servicio.CatalogoProductos.Categoria;
import com.example.DrogueriaLosTresHermanos.servicio.CatalogoProductos.Presentacion;
import com.example.DrogueriaLosTresHermanos.servicio.CatalogoProductos.Producto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Catálogo en MySQL (tablas categoria, producto y presentacion).
 * La disponibilidad no se escribe a mano: sale de los lotes del inventario.
 */
public class AlmacenCatalogoMysql implements AlmacenCatalogo {

    /** Concentración que se muestra cuando la presentación no la tiene. */
    static final String SIN_CONCENTRACION = "No aplica";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaccion;

    public AlmacenCatalogoMysql(DataSource origen) {
        this.jdbc = new JdbcTemplate(origen);
        this.transaccion = new TransactionTemplate(new DataSourceTransactionManager(origen));
    }

    @Override
    public Contenido cargar() {
        List<Categoria> categorias = jdbc.query(
            "SELECT codigo_categoria, nombre_categoria, tipo_categoria FROM categoria ORDER BY id",
            (r, n) -> new Categoria(r.getString(1), r.getString(2), r.getString(3)));

        Map<Integer, List<Presentacion>> presentaciones = new HashMap<>();
        Map<Integer, String> contenidoVariedad = new HashMap<>();
        Map<Integer, Integer> precioVariedad = new HashMap<>();
        jdbc.query("SELECT id, producto_id, tipo_presentacion, concentracion, marca, precio_venta FROM presentacion "
                + "WHERE presentacion_activa = TRUE ORDER BY id", r -> {
            int producto = r.getInt(2);
            presentaciones.computeIfAbsent(producto, k -> new ArrayList<>()).add(new Presentacion(String.valueOf(r.getLong(1)),
                r.getString(3), Objects.requireNonNullElse(r.getString(4), SIN_CONCENTRACION), Objects.requireNonNullElse(r.getString(5), ""),
                r.getBigDecimal(6).intValue()));
            contenidoVariedad.putIfAbsent(producto, r.getString(3));
            precioVariedad.putIfAbsent(producto, r.getBigDecimal(6).intValue());
        });

        // Disponibilidad según el inventario: sin lotes todavía no hay dato
        Map<Integer, String> disponibilidad = new HashMap<>();
        jdbc.query("SELECT pr.producto_id, COUNT(l.id), COALESCE(SUM(IF(l.fecha_vencimiento IS NULL OR l.fecha_vencimiento >= CURDATE(), "
                + "l.cantidad_actual, 0)), 0) FROM presentacion pr LEFT JOIN lote l ON l.presentacion_id = pr.id "
                + "WHERE pr.presentacion_activa = TRUE GROUP BY pr.producto_id", r -> {
            long lotes = r.getLong(2);
            long vendible = r.getLong(3);
            disponibilidad.put(r.getInt(1), lotes == 0 ? CatalogoProductos.SIN_DATO : vendible > 0 ? "disponible" : "agotado");
        });

        List<Producto> productos = jdbc.query(
            "SELECT p.id, c.tipo_categoria, p.nombre_producto, p.descripcion_producto, c.codigo_categoria, p.principio_activo, "
                + "p.contenido_producto, p.requiere_formula, p.producto_activo, p.foto_producto, p.imagen_catalogo "
                + "FROM producto p JOIN categoria c ON c.id = p.categoria_id ORDER BY p.id",
            (r, n) -> {
                int id = r.getInt(1);
                boolean medicamento = CatalogoProductos.MEDICAMENTO.equals(r.getString(2));
                String contenido = r.getString(7);
                if ((contenido == null || contenido.isBlank()) && !medicamento) {
                    contenido = contenidoVariedad.getOrDefault(id, "");
                }
                return new Producto(id, r.getString(2), r.getString(3), Objects.requireNonNullElse(r.getString(4), ""), r.getString(5),
                    Objects.requireNonNullElse(r.getString(6), ""), Objects.requireNonNullElse(contenido, ""), r.getBoolean(8),
                    disponibilidad.getOrDefault(id, CatalogoProductos.SIN_DATO), r.getBoolean(9), Objects.requireNonNullElse(r.getString(10), ""),
                    Objects.requireNonNullElse(r.getString(11), ""), medicamento ? 0 : precioVariedad.getOrDefault(id, 0),
                    medicamento ? List.copyOf(presentaciones.getOrDefault(id, List.of())) : List.of());
            });
        return new Contenido(categorias, productos);
    }

    @Override
    public int guardarProducto(Producto p, boolean nuevo) {
        Integer resultado = transaccion.execute(estado -> {
            Integer categoria = jdbc.queryForObject("SELECT id FROM categoria WHERE codigo_categoria = ?", Integer.class, p.categoria());
            int id;
            if (nuevo) {
                KeyHolder llave = new GeneratedKeyHolder();
                jdbc.update(c -> {
                    PreparedStatement s = c.prepareStatement("INSERT INTO producto (categoria_id, nombre_producto, descripcion_producto, "
                        + "principio_activo, contenido_producto, requiere_formula, producto_activo, foto_producto, imagen_catalogo) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                    s.setInt(1, categoria);
                    s.setString(2, p.nombre());
                    s.setString(3, vacioANulo(p.descripcion()));
                    s.setString(4, vacioANulo(p.principioActivo()));
                    s.setString(5, vacioANulo(p.contenido()));
                    s.setBoolean(6, p.formula());
                    s.setBoolean(7, p.activo());
                    s.setString(8, vacioANulo(p.foto()));
                    s.setString(9, vacioANulo(p.imagen()));
                    return s;
                }, llave);
                id = Objects.requireNonNull(llave.getKey()).intValue();
            } else {
                id = p.id();
                jdbc.update("UPDATE producto SET categoria_id = ?, nombre_producto = ?, descripcion_producto = ?, principio_activo = ?, "
                        + "contenido_producto = ?, requiere_formula = ?, foto_producto = ? WHERE id = ?",
                    categoria, p.nombre(), vacioANulo(p.descripcion()), vacioANulo(p.principioActivo()), vacioANulo(p.contenido()),
                    p.formula(), vacioANulo(p.foto()), id);
            }
            if (p.esMedicamento()) {
                sincronizarPresentaciones(id, p.presentaciones());
            } else {
                guardarPrecioVariedad(id, p.precio());
            }
            return id;
        });
        return Objects.requireNonNull(resultado);
    }

    /** Actualiza, agrega o retira presentaciones. La que ya tiene ventas o lotes no se borra: se desactiva. */
    private void sincronizarPresentaciones(int producto, List<Presentacion> nuevas) {
        Set<Long> actuales = new HashSet<>(jdbc.queryForList(
            "SELECT id FROM presentacion WHERE producto_id = ? AND presentacion_activa = TRUE", Long.class, producto));
        Set<Long> conservadas = new HashSet<>();
        for (Presentacion q : nuevas) {
            String concentracion = SIN_CONCENTRACION.equalsIgnoreCase(q.conc()) ? null : vacioANulo(q.conc());
            Long id = numero(q.id());
            if (id != null && actuales.contains(id)) {
                jdbc.update("UPDATE presentacion SET tipo_presentacion = ?, concentracion = ?, marca = ?, precio_venta = ? WHERE id = ?",
                    q.tipo(), concentracion, vacioANulo(q.marca()), q.precio(), id);
                conservadas.add(id);
            } else {
                jdbc.update("INSERT INTO presentacion (producto_id, tipo_presentacion, concentracion, marca, precio_venta) VALUES (?, ?, ?, ?, ?)",
                    producto, q.tipo(), concentracion, vacioANulo(q.marca()), q.precio());
            }
        }
        for (Long retirada : actuales) {
            if (conservadas.contains(retirada)) {
                continue;
            }
            Integer usos = jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM detalle_pedido WHERE presentacion_id = ?) + "
                + "(SELECT COUNT(*) FROM lote WHERE presentacion_id = ?) + (SELECT COUNT(*) FROM detalle_orden_compra WHERE presentacion_id = ?) + "
                + "(SELECT COUNT(*) FROM notificacion WHERE presentacion_id = ?)", Integer.class, retirada, retirada, retirada, retirada);
            if (usos != null && usos > 0) {
                jdbc.update("UPDATE presentacion SET presentacion_activa = FALSE WHERE id = ?", retirada);
            } else {
                jdbc.update("DELETE FROM presentacion WHERE id = ?", retirada);
            }
        }
    }

    private void guardarPrecioVariedad(int producto, int precio) {
        List<Long> existentes = jdbc.queryForList(
            "SELECT id FROM presentacion WHERE producto_id = ? AND presentacion_activa = TRUE ORDER BY id LIMIT 1", Long.class, producto);
        if (existentes.isEmpty()) {
            jdbc.update("INSERT INTO presentacion (producto_id, tipo_presentacion, precio_venta) VALUES (?, 'Unidad', ?)", producto, precio);
        } else {
            jdbc.update("UPDATE presentacion SET precio_venta = ? WHERE id = ?", precio, existentes.get(0));
        }
    }

    @Override
    public void cambiarFoto(int id, String foto) {
        jdbc.update("UPDATE producto SET foto_producto = ? WHERE id = ?", vacioANulo(foto), id);
    }

    @Override
    public void cambiarVisibilidad(int id, boolean activo) {
        jdbc.update("UPDATE producto SET producto_activo = ? WHERE id = ?", activo, id);
    }

    @Override
    public void guardarCategoria(Categoria c, boolean nueva) {
        if (nueva) {
            jdbc.update("INSERT INTO categoria (codigo_categoria, nombre_categoria, tipo_categoria) VALUES (?, ?, ?)", c.id(), c.nombre(), c.tipo());
        } else {
            jdbc.update("UPDATE categoria SET nombre_categoria = ? WHERE codigo_categoria = ?", c.nombre(), c.id());
        }
    }

    @Override
    public void eliminarCategoria(String id) {
        jdbc.update("DELETE FROM categoria WHERE codigo_categoria = ?", id);
    }

    @Override
    public boolean recargarTrasCambios() {
        return true;
    }

    @Override
    public String origen() {
        return "MySQL";
    }

    private static String vacioANulo(String v) {
        return v == null || v.isBlank() ? null : v.strip();
    }

    private static Long numero(String v) {
        return v != null && v.matches("\\d{1,18}") ? Long.parseLong(v) : null;
    }
}
