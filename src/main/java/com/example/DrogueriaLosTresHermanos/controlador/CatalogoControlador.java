package com.example.DrogueriaLosTresHermanos.controlador;

import com.example.DrogueriaLosTresHermanos.catalogo.CatalogoProductos;
import com.example.DrogueriaLosTresHermanos.catalogo.CatalogoProductos.Categoria;
import com.example.DrogueriaLosTresHermanos.catalogo.CatalogoProductos.Presentacion;
import com.example.DrogueriaLosTresHermanos.catalogo.CatalogoProductos.Producto;
import com.example.DrogueriaLosTresHermanos.catalogo.FotosProductos;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/** Entrega el catálogo público a la tienda como un script, y las fotos de los productos. */
@Controller
public class CatalogoControlador {

    private final CatalogoProductos catalogo;
    private final FotosProductos fotos;

    public CatalogoControlador(CatalogoProductos catalogo, FotosProductos fotos) {
        this.catalogo = catalogo;
        this.fotos = fotos;
    }

    @GetMapping(value = "/js/catalogo.js", produces = "text/javascript;charset=UTF-8")
    public ResponseEntity<String> script() {
        String js = "const CATEGORIAS = [" + catalogo.categorias().stream().map(this::categoria).collect(Collectors.joining(",")) + "];\n"
            + "const LABELS_CAT = Object.fromEntries(CATEGORIAS.map(c => [c.id, c.nombre]));\n"
            + "const CAT_LABELS = LABELS_CAT;\n"
            + "const MEDICAMENTOS = [" + lista(catalogo.visibles(CatalogoProductos.MEDICAMENTO)) + "];\n"
            + "const VARIEDADES = [" + lista(catalogo.visibles(CatalogoProductos.VARIEDAD)) + "];\n";
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(js);
    }

    @GetMapping("/productos/imagen/{nombre}")
    public ResponseEntity<Resource> foto(@PathVariable String nombre) {
        Path ruta = fotos.ruta(nombre).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(fotos.tipoDe(nombre))).body(new FileSystemResource(ruta));
    }

    private String lista(List<Producto> productos) {
        return productos.stream().map(this::producto).collect(Collectors.joining(",\n"));
    }

    private String categoria(Categoria c) {
        return "{id:" + txt(c.id()) + ",nombre:" + txt(c.nombre()) + ",tipo:" + txt(c.tipo()) + "}";
    }

    private String producto(Producto p) {
        StringBuilder s = new StringBuilder("{id:").append(p.id())
            .append(",nombre:").append(txt(p.nombre()))
            .append(",cat:").append(txt(p.categoria()))
            .append(",desc:").append(txt(p.descripcion()))
            .append(",principio:").append(txt(p.principioActivo()))
            .append(",contenido:").append(txt(p.contenido()))
            .append(",formula:").append(p.formula())
            .append(",disponibilidad:").append(txt(p.disponibilidad()))
            .append(",imagen:").append(txt(p.imagen()))
            .append(",foto:").append(txt(p.foto().isEmpty() ? "" : "/productos/imagen/" + p.foto()));
        if (p.esMedicamento()) {
            s.append(",presentaciones:[").append(p.presentaciones().stream().map(this::presentacion).collect(Collectors.joining(","))).append("]");
        } else {
            s.append(",precio:").append(p.precio());
        }
        return s.append("}").toString();
    }

    private String presentacion(Presentacion q) {
        return "{id:" + txt(q.id()) + ",tipo:" + txt(q.tipo()) + ",conc:" + txt(q.conc()) + ",marca:" + txt(q.marca()) + ",precio:" + q.precio() + "}";
    }

    /** Literal de texto de JavaScript; escapa todo lo que no sea alfanumérico habitual para no romper el script. */
    private static String txt(String v) {
        StringBuilder s = new StringBuilder("\"");
        for (char c : (v == null ? "" : v).toCharArray()) {
            if (c == '"' || c == '\\' || c < 0x20 || c == '<' || c == '>' || c == '&' || c == ' ' || c == ' ') {
                s.append(String.format("\\u%04x", (int) c));
            } else {
                s.append(c);
            }
        }
        return s.append('"').toString();
    }
}
