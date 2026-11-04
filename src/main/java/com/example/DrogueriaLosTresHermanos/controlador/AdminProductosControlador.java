package com.example.DrogueriaLosTresHermanos.controlador;

import com.example.DrogueriaLosTresHermanos.catalogo.CatalogoProductos;
import com.example.DrogueriaLosTresHermanos.catalogo.CatalogoProductos.Datos;
import com.example.DrogueriaLosTresHermanos.catalogo.CatalogoProductos.Presentacion;
import com.example.DrogueriaLosTresHermanos.catalogo.CatalogoProductos.Producto;
import com.example.DrogueriaLosTresHermanos.catalogo.FotosProductos;
import com.example.DrogueriaLosTresHermanos.configuracion.DirectorioAccesos;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Productos y categorías. SeguridadConfig exige el permiso de cada ruta antes de llegar aquí. */
@Controller
public class AdminProductosControlador {

    private final CatalogoProductos catalogo;
    private final FotosProductos fotos;
    private final DirectorioAccesos directorio;

    public AdminProductosControlador(CatalogoProductos catalogo, FotosProductos fotos, DirectorioAccesos directorio) {
        this.catalogo = catalogo;
        this.fotos = fotos;
        this.directorio = directorio;
    }

    @GetMapping("/admin/productos")
    public String lista(Model m, Authentication a) {
        m.addAttribute("perfil", directorio.perfil(a));
        m.addAttribute("seccionActiva", "productos");
        m.addAttribute("productos", catalogo.todos());
        m.addAttribute("catalogo", catalogo);
        return "admin-productos";
    }

    @GetMapping("/admin/productos/nuevo")
    public String nuevo(Model m, Authentication a) {
        return formulario(m, a, null, new Datos(CatalogoProductos.MEDICAMENTO, "", "", "", "", "", false, CatalogoProductos.SIN_DATO, 0,
            List.of(new Presentacion("", "", "", "", 0))), "", Map.of());
    }

    @GetMapping("/admin/productos/{id:\\d+}")
    public String editar(@PathVariable int id, Model m, Authentication a) {
        Producto p = catalogo.buscar(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Presentacion> pres = p.presentaciones().isEmpty() ? List.of(new Presentacion("", "", "", "", 0)) : p.presentaciones();
        return formulario(m, a, id, new Datos(p.tipo(), p.nombre(), p.descripcion(), p.categoria(), p.principioActivo(), p.contenido(),
            p.formula(), p.disponibilidad(), p.precio(), pres), p.foto(), Map.of());
    }

    @PostMapping({"/admin/productos/nuevo", "/admin/productos/{id:\\d+}"})
    public String guardar(@PathVariable(required = false) Integer id,
                          @RequestParam(required = false) String tipo, @RequestParam String nombre,
                          @RequestParam(required = false) String descripcion, @RequestParam(required = false) String categoria,
                          @RequestParam(required = false) String principioActivo, @RequestParam(required = false) String contenido,
                          @RequestParam(defaultValue = "false") boolean formula, @RequestParam(required = false) String disponibilidad,
                          @RequestParam(required = false) String precio,
                          @RequestParam(name = "presId", required = false) List<String> presId,
                          @RequestParam(name = "presTipo", required = false) List<String> presTipo,
                          @RequestParam(name = "presConc", required = false) List<String> presConc,
                          @RequestParam(name = "presMarca", required = false) List<String> presMarca,
                          @RequestParam(name = "presPrecio", required = false) List<String> presPrecio,
                          @RequestParam(required = false) MultipartFile foto,
                          Model m, Authentication a, RedirectAttributes r) {
        List<Presentacion> pres = presentaciones(presId, presTipo, presConc, presMarca, presPrecio);
        Datos d = new Datos(tipo, nombre, descripcion, categoria, principioActivo, contenido, formula, disponibilidad, entero(precio), pres);

        Map<String, String> errores = new LinkedHashMap<>();
        String nombreFoto = null;
        if (foto != null && !foto.isEmpty()) {
            try {
                nombreFoto = fotos.guardar(foto);
            } catch (IllegalArgumentException e) {
                errores.put("foto", e.getMessage());
            } catch (IOException e) {
                errores.put("foto", "No se pudo guardar la foto. Inténtalo de nuevo.");
            }
        }
        if (errores.isEmpty()) {
            errores.putAll(catalogo.guardar(id, d, nombreFoto));
        }
        if (!errores.isEmpty()) {
            String actual = id == null ? "" : catalogo.buscar(id).map(Producto::foto).orElse("");
            return formulario(m, a, id, pres.isEmpty() ? conFilaVacia(d) : d, actual, errores);
        }
        directorio.auditar(a.getName(), id == null ? "Producto creado" : "Producto modificado", nombre.strip());
        r.addFlashAttribute("mensaje", id == null ? "Producto creado. Ya aparece en la tienda." : "Cambios guardados.");
        return "redirect:/admin/productos";
    }

    @PostMapping("/admin/productos/{id:\\d+}/visibilidad")
    public String visibilidad(@PathVariable int id, @RequestParam boolean activo, Authentication a, RedirectAttributes r) {
        Producto p = catalogo.buscar(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        catalogo.cambiarVisibilidad(id, activo);
        directorio.auditar(a.getName(), activo ? "Producto publicado" : "Producto retirado de la tienda", p.nombre());
        r.addFlashAttribute("mensaje", activo ? "El producto volvió a la tienda." : "El producto ya no aparece en la tienda.");
        return "redirect:/admin/productos";
    }

    /** Cambio rápido de imagen desde el catálogo de medicamentos. Responde la ruta de la foto, o el motivo si no se aceptó. */
    @PostMapping("/admin/productos/{id:\\d+}/foto")
    @ResponseBody
    public ResponseEntity<String> foto(@PathVariable int id, @RequestParam MultipartFile foto, Authentication a) {
        Producto p = catalogo.buscar(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            String nombre = fotos.guardar(foto);
            catalogo.cambiarFoto(id, nombre);
            directorio.auditar(a.getName(), "Imagen de producto cambiada", p.nombre());
            return ResponseEntity.ok("/productos/imagen/" + nombre);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No se pudo guardar la foto.");
        }
    }

    @GetMapping("/admin/categorias")
    public String categorias(Model m, Authentication a) {
        m.addAttribute("perfil", directorio.perfil(a));
        m.addAttribute("seccionActiva", "categorias");
        m.addAttribute("catalogo", catalogo);
        m.addAttribute("medicamentos", catalogo.categorias(CatalogoProductos.MEDICAMENTO));
        m.addAttribute("variedades", catalogo.categorias(CatalogoProductos.VARIEDAD));
        return "admin-categorias";
    }

    @PostMapping("/admin/categorias")
    public String guardarCategoria(@RequestParam(required = false) String id, @RequestParam String nombre,
                                   @RequestParam(required = false) String tipo, Authentication a, RedirectAttributes r) {
        return resultadoCategoria(catalogo.guardarCategoria(id, nombre, tipo), "Categoría guardada.", a, nombre, r);
    }

    @PostMapping("/admin/categorias/{id}/eliminar")
    public String eliminarCategoria(@PathVariable String id, Authentication a, RedirectAttributes r) {
        return resultadoCategoria(catalogo.eliminarCategoria(id), "Categoría eliminada.", a, id, r);
    }

    private String resultadoCategoria(String error, String exito, Authentication a, String detalle, RedirectAttributes r) {
        if (error != null) {
            r.addFlashAttribute("error", error);
        } else {
            directorio.auditar(a.getName(), exito, detalle);
            r.addFlashAttribute("mensaje", exito);
        }
        return "redirect:/admin/categorias";
    }

    private String formulario(Model m, Authentication a, Integer id, Datos d, String fotoActual, Map<String, String> errores) {
        m.addAttribute("perfil", directorio.perfil(a));
        m.addAttribute("seccionActiva", "productos");
        m.addAttribute("productoId", id);
        m.addAttribute("f", d);
        m.addAttribute("fotoActual", fotoActual);
        m.addAttribute("errores", errores);
        m.addAttribute("catMedicamentos", catalogo.categorias(CatalogoProductos.MEDICAMENTO));
        m.addAttribute("catVariedades", catalogo.categorias(CatalogoProductos.VARIEDAD));
        return "admin-producto-form";
    }

    private static Datos conFilaVacia(Datos d) {
        return new Datos(d.tipo(), d.nombre(), d.descripcion(), d.categoria(), d.principioActivo(), d.contenido(), d.formula(),
            d.disponibilidad(), d.precio(), List.of(new Presentacion("", "", "", "", 0)));
    }

    private static List<Presentacion> presentaciones(List<String> id, List<String> tipo, List<String> conc, List<String> marca, List<String> precio) {
        List<Presentacion> lista = new ArrayList<>();
        int n = tipo == null ? 0 : tipo.size();
        for (int i = 0; i < n; i++) {
            String t = valor(tipo, i), c = valor(conc, i), ma = valor(marca, i), pr = valor(precio, i);
            // Una fila totalmente vacía se ignora: es la que el formulario deja lista para escribir.
            if (t.isBlank() && c.isBlank() && ma.isBlank() && pr.isBlank()) {
                continue;
            }
            lista.add(new Presentacion(valor(id, i), t, c, ma, entero(pr)));
        }
        return lista;
    }

    private static String valor(List<String> lista, int i) {
        return lista != null && i < lista.size() && lista.get(i) != null ? lista.get(i) : "";
    }

    private static int entero(String v) {
        if (v == null) {
            return 0;
        }
        String digitos = v.replaceAll("[^0-9]", "");
        if (digitos.isEmpty() || digitos.length() > 9) {
            return 0;
        }
        return Integer.parseInt(digitos);
    }
}
