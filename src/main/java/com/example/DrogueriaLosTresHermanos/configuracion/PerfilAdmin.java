package com.example.DrogueriaLosTresHermanos.configuracion;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Permisos efectivos del usuario autenticado, para que la vista refleje lo que el servidor autoriza. */
public record PerfilAdmin(String correo, String rolId, String rolNombre, Set<String> permisos, List<ModuloGestion> modulos) {

    public boolean puede(String permiso) {
        return permisos.contains(permiso);
    }

    public boolean operaClientes() {
        return puede(Permiso.CLIENTES_VER.getCodigo());
    }

    public boolean operaPagos() {
        return puede(Permiso.SUSCRIPCIONES_VER.getCodigo());
    }

    public boolean gestionaAccesos() {
        return puede(Permiso.ROLES_GESTIONAR.getCodigo()) || puede(Permiso.USUARIOS_GESTIONAR.getCodigo());
    }

    /** Permisos para el atributo data-permisos de la vista. */
    public String permisosComoTexto() {
        return permisos.stream().sorted().collect(Collectors.joining(" "));
    }
}
