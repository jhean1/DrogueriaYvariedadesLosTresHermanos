package com.example.DrogueriaLosTresHermanos.servicio;

import com.example.DrogueriaLosTresHermanos.servicio.CatalogoProductos.Categoria;
import com.example.DrogueriaLosTresHermanos.servicio.CatalogoProductos.Presentacion;
import com.example.DrogueriaLosTresHermanos.servicio.CatalogoProductos.Producto;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 * Catálogo en un archivo local. El primer arranque parte de catalogo-inicial.properties.
 * Se usa cuando no hay base de datos configurada: pruebas automáticas y copia estática del sitio.
 */
public class AlmacenCatalogoArchivo implements AlmacenCatalogo {

    private final Path archivo;

    public AlmacenCatalogoArchivo(String archivo) {
        this.archivo = Path.of(archivo);
    }

    @Override
    public Contenido cargar() {
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
        List<Categoria> categorias = new ArrayList<>();
        for (int i = 0; p.containsKey("cat." + i + ".id"); i++) {
            categorias.add(new Categoria(p.getProperty("cat." + i + ".id"), p.getProperty("cat." + i + ".nombre"), p.getProperty("cat." + i + ".tipo")));
        }
        List<Producto> productos = new ArrayList<>();
        for (int i = 0; p.containsKey("p." + i + ".id"); i++) {
            String b = "p." + i + ".";
            List<Presentacion> pres = new ArrayList<>();
            for (int j = 0; p.containsKey(b + "pres." + j + ".id"); j++) {
                String q = b + "pres." + j + ".";
                pres.add(new Presentacion(p.getProperty(q + "id"), p.getProperty(q + "tipo"), p.getProperty(q + "conc"),
                    p.getProperty(q + "marca"), Integer.parseInt(p.getProperty(q + "precio"))));
            }
            productos.add(new Producto(Integer.parseInt(p.getProperty(b + "id")), p.getProperty(b + "tipo"), p.getProperty(b + "nombre"),
                p.getProperty(b + "descripcion", ""), p.getProperty(b + "categoria"), p.getProperty(b + "principio", ""),
                p.getProperty(b + "contenido", ""), Boolean.parseBoolean(p.getProperty(b + "formula")),
                p.getProperty(b + "disponibilidad", CatalogoProductos.SIN_DATO), Boolean.parseBoolean(p.getProperty(b + "activo", "true")),
                p.getProperty(b + "foto", ""), p.getProperty(b + "imagen", ""), Integer.parseInt(p.getProperty(b + "precio", "0")),
                List.copyOf(pres)));
        }
        productos.sort(Comparator.comparingInt(Producto::id));
        return new Contenido(categorias, productos);
    }

    // En el archivo, cada cambio se guarda escribiendo el catálogo completo (ver sincronizar).
    @Override
    public int guardarProducto(Producto producto, boolean nuevo) {
        return producto.id();
    }

    @Override
    public void cambiarFoto(int id, String foto) {
    }

    @Override
    public void cambiarVisibilidad(int id, boolean activo) {
    }

    @Override
    public void guardarCategoria(Categoria categoria, boolean nueva) {
    }

    @Override
    public void eliminarCategoria(String id) {
    }

    @Override
    public void sincronizar(Contenido todo) {
        Properties p = new Properties();
        int i = 0;
        for (Categoria c : todo.categorias()) {
            p.setProperty("cat." + i + ".id", c.id());
            p.setProperty("cat." + i + ".nombre", c.nombre());
            p.setProperty("cat." + i + ".tipo", c.tipo());
            i++;
        }
        i = 0;
        for (Producto x : todo.productos()) {
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

    @Override
    public String origen() {
        return "archivo " + archivo;
    }
}
