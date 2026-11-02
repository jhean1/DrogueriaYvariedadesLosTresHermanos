package com.example.DrogueriaLosTresHermanos.controlador;

import com.example.DrogueriaLosTresHermanos.configuracion.EquipoProperties;
import com.example.DrogueriaLosTresHermanos.configuracion.ModuloGestion;
import com.example.DrogueriaLosTresHermanos.configuracion.PerfilAdmin;
import com.example.DrogueriaLosTresHermanos.configuracion.Permiso;
import com.example.DrogueriaLosTresHermanos.configuracion.RolSistema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Vistas de gestión. Spring Security autoriza cada ruta en SeguridadConfig antes de llegar aquí. */
@Controller
public class AdminControlador {

    public record UsuarioVista(String correo, RolSistema rol) {}

    private final String correoPropietario;
    private final EquipoProperties equipo;

    public AdminControlador(@Value("${app.admin.correo}") String correoPropietario, EquipoProperties equipo) {
        this.correoPropietario = correoPropietario;
        this.equipo = equipo;
    }

    @GetMapping("/admin/modulos/{slug}")
    public String modulo(@PathVariable String slug, Model modelo, Authentication autenticacion) {
        ModuloGestion modulo = ModuloGestion.porSlug(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        modelo.addAttribute("perfil", PerfilAdmin.de(autenticacion));
        modelo.addAttribute("modulo", modulo);
        modelo.addAttribute("seccionActiva", slug);
        return "admin-modulo";
    }

    @GetMapping("/admin/accesos")
    public String accesos(Model modelo, Authentication autenticacion) {
        Map<String, List<Permiso>> permisosPorModulo = new LinkedHashMap<>();
        for (Permiso permiso : Permiso.values()) {
            permisosPorModulo.computeIfAbsent(permiso.getModulo(), m -> new ArrayList<>()).add(permiso);
        }

        List<UsuarioVista> usuarios = new ArrayList<>();
        usuarios.add(new UsuarioVista(correoPropietario, RolSistema.PROPIETARIO));
        equipo.equipo().forEach(m -> usuarios.add(new UsuarioVista(m.correo(), m.rol())));

        modelo.addAttribute("perfil", PerfilAdmin.de(autenticacion));
        modelo.addAttribute("seccionActiva", "accesos");
        modelo.addAttribute("roles", RolSistema.values());
        modelo.addAttribute("permisosPorModulo", permisosPorModulo);
        modelo.addAttribute("usuarios", usuarios);
        return "admin-accesos";
    }
}
