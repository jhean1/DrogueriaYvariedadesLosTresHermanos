package com.example.DrogueriaLosTresHermanos.configuracion;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Permisos efectivos del usuario autenticado, para que la vista refleje lo que el servidor autoriza. */
public record PerfilAdmin(String correo, RolSistema rol, Set<String> permisos, List<ModuloGestion> modulos) {

    public static PerfilAdmin de(Authentication autenticacion) {
        Set<String> autoridades = autenticacion.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toUnmodifiableSet());

        RolSistema rol = Stream.of(RolSistema.values())
            .filter(r -> autoridades.contains("ROLE_" + r.name()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("El usuario no tiene un rol del sistema"));

        List<ModuloGestion> modulos = Stream.of(ModuloGestion.values())
            .filter(m -> autoridades.contains(m.getPermiso().getCodigo()))
            .toList();

        return new PerfilAdmin(autenticacion.getName(), rol, autoridades, modulos);
    }

    public boolean puede(String permiso) {
        return permisos.contains(permiso);
    }

    public boolean operaClientes() {
        return puede(Permiso.CLIENTES_VER.getCodigo());
    }

    public boolean operaPagos() {
        return puede(Permiso.SUSCRIPCIONES_VER.getCodigo());
    }

    /** Permisos para el atributo data-permisos de la vista, sin incluir el rol. */
    public String permisosComoTexto() {
        return permisos.stream().filter(p -> !p.startsWith("ROLE_")).sorted().collect(Collectors.joining(" "));
    }
}
