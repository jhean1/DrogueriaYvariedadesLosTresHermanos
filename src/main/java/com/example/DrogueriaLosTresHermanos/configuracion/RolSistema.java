package com.example.DrogueriaLosTresHermanos.configuracion;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.example.DrogueriaLosTresHermanos.configuracion.Permiso.*;

/**
 * Roles del equipo de la droguería y sus permisos efectivos.
 * Mientras no exista base de datos, esta tabla es la fuente de verdad.
 */
public enum RolSistema {

    PROPIETARIO("Propietario", "Acceso completo, usuarios, roles, finanzas y auditoría.",
        EnumSet.allOf(Permiso.class)),

    ADMINISTRADOR("Administrador", "Gestión operativa de clientes, pagos, productos, inventario y compras.",
        EnumSet.of(DASHBOARD_VER, CLIENTES_VER, CLIENTES_CREAR,
            SUSCRIPCIONES_VER, SUSCRIPCIONES_APROBAR, SUSCRIPCIONES_ACTIVAR,
            PRODUCTOS_VER, PRODUCTOS_CREAR, PRODUCTOS_EDITAR, PRODUCTOS_ELIMINAR,
            INVENTARIO_VER, INVENTARIO_AJUSTAR, INVENTARIO_MOVER, INVENTARIO_CONTAR, LOTES_VER,
            COMPRAS_VER, COMPRAS_GESTIONAR, VENTAS_VER, VENTAS_ANULAR,
            REPORTES_VER, NOTIFICACIONES_VER)),

    REGENTE("Regente / Responsable técnico", "Supervisión de productos farmacéuticos, lotes, vencimientos y trazabilidad.",
        EnumSet.of(DASHBOARD_VER, PRODUCTOS_VER, PRODUCTOS_EDITAR,
            INVENTARIO_VER, LOTES_VER, LOTES_GESTIONAR, NOTIFICACIONES_VER)),

    CAJERO("Cajero / Vendedor", "Punto de venta, consulta de productos, comprobantes y caja autorizada.",
        EnumSet.of(DASHBOARD_VER, PRODUCTOS_VER, INVENTARIO_VER,
            VENTAS_VER, VENTAS_CREAR, CAJA_VER, CAJA_OPERAR)),

    AUXILIAR("Auxiliar de inventario", "Existencias, recepción de productos, movimientos y conteos físicos.",
        EnumSet.of(DASHBOARD_VER, PRODUCTOS_VER, INVENTARIO_VER, INVENTARIO_MOVER,
            INVENTARIO_CONTAR, LOTES_VER, NOTIFICACIONES_VER));

    private final String nombre;
    private final String descripcion;
    private final Set<Permiso> permisos;

    RolSistema(String nombre, String descripcion, Set<Permiso> permisos) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.permisos = Collections.unmodifiableSet(permisos);
    }

    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Set<Permiso> getPermisos() { return permisos; }

    public boolean tiene(Permiso permiso) {
        return permisos.contains(permiso);
    }

    /** Autoridades de Spring Security: el rol y cada uno de sus permisos. */
    public List<GrantedAuthority> autoridades() {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + name()));
        permisos.forEach(p -> autoridades.add(new SimpleGrantedAuthority(p.getCodigo())));
        return autoridades;
    }
}
