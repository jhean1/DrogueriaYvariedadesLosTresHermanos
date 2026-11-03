package com.example.DrogueriaLosTresHermanos.controlador;

import com.example.DrogueriaLosTresHermanos.configuracion.DirectorioAccesos;
import com.example.DrogueriaLosTresHermanos.configuracion.DirectorioAccesos.OperacionNoPermitida;
import com.example.DrogueriaLosTresHermanos.configuracion.ModuloGestion;
import com.example.DrogueriaLosTresHermanos.configuracion.Permiso;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Vistas de gestión. Spring Security autoriza cada ruta en SeguridadConfig antes de llegar aquí. */
@Controller
public class AdminControlador {

    private static final String ACCESOS = "redirect:/admin/accesos";

    private final DirectorioAccesos directorio;

    public AdminControlador(DirectorioAccesos directorio) {
        this.directorio = directorio;
    }

    @GetMapping("/admin/modulos/{slug}")
    public String modulo(@PathVariable String slug, Model modelo, Authentication autenticacion) {
        ModuloGestion modulo = ModuloGestion.porSlug(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        modelo.addAttribute("perfil", directorio.perfil(autenticacion));
        modelo.addAttribute("modulo", modulo);
        modelo.addAttribute("seccionActiva", slug);
        if (modulo == ModuloGestion.AUDITORIA) {
            modelo.addAttribute("eventos", directorio.eventos());
            return "admin-auditoria";
        }
        return "admin-modulo";
    }

    @GetMapping("/admin/accesos")
    public String accesos(Model modelo, Authentication autenticacion) {
        Map<String, List<Permiso>> permisosPorModulo = new LinkedHashMap<>();
        for (Permiso permiso : Permiso.values()) {
            permisosPorModulo.computeIfAbsent(permiso.getModulo(), m -> new ArrayList<>()).add(permiso);
        }
        modelo.addAttribute("perfil", directorio.perfil(autenticacion));
        modelo.addAttribute("seccionActiva", "accesos");
        modelo.addAttribute("roles", directorio.roles());
        modelo.addAttribute("permisosPorModulo", permisosPorModulo);
        modelo.addAttribute("usuarios", directorio.cuentas());
        modelo.addAttribute("directorio", directorio);
        return "admin-accesos";
    }

    @PostMapping("/admin/accesos/roles")
    public String crearRol(@RequestParam String nombre, @RequestParam(required = false) String descripcion,
                           @RequestParam(required = false) String base, Authentication a, RedirectAttributes r) {
        return ejecutar(r, "Rol creado.", () -> directorio.crearRol(directorio.actor(a), nombre, descripcion, base));
    }

    @PostMapping("/admin/accesos/roles/{id}/permisos")
    public String permisosRol(@PathVariable String id, @RequestParam(required = false) List<String> permisos,
                              Authentication a, RedirectAttributes r) {
        return ejecutar(r, "Permisos del rol actualizados.",
            () -> directorio.cambiarPermisosRol(directorio.actor(a), id, permisos == null ? List.of() : permisos));
    }

    @PostMapping("/admin/accesos/usuarios")
    public String crearCuenta(@RequestParam String correo, @RequestParam String clave, @RequestParam String rol,
                              Authentication a, RedirectAttributes r) {
        return ejecutar(r, "Cuenta creada.", () -> directorio.crearCuenta(directorio.actor(a), correo, clave, rol));
    }

    @PostMapping("/admin/accesos/usuarios/{id}/rol")
    public String rolCuenta(@PathVariable String id, @RequestParam String rol, Authentication a, RedirectAttributes r) {
        return ejecutar(r, "Rol de la cuenta actualizado.", () -> directorio.cambiarRolCuenta(directorio.actor(a), id, rol));
    }

    @PostMapping("/admin/accesos/usuarios/{id}/estado")
    public String estadoCuenta(@PathVariable String id, @RequestParam boolean activa, Authentication a, RedirectAttributes r) {
        return ejecutar(r, activa ? "Cuenta activada." : "Cuenta desactivada.",
            () -> directorio.cambiarEstadoCuenta(directorio.actor(a), id, activa));
    }

    @PostMapping("/admin/accesos/usuarios/{id}/permisos")
    public String permisosCuenta(@PathVariable String id, @RequestParam(required = false) List<String> concedidos,
                                 @RequestParam(required = false) List<String> revocados, Authentication a, RedirectAttributes r) {
        return ejecutar(r, "Permisos individuales actualizados.", () -> directorio.cambiarPermisosCuenta(directorio.actor(a), id,
            concedidos == null ? List.of() : concedidos, revocados == null ? List.of() : revocados));
    }

    private String ejecutar(RedirectAttributes redireccion, String exito, Runnable accion) {
        try {
            accion.run();
            redireccion.addFlashAttribute("mensaje", exito);
        } catch (OperacionNoPermitida e) {
            redireccion.addFlashAttribute("error", e.getMessage());
        }
        return ACCESOS;
    }
}
