package com.example.DrogueriaLosTresHermanos.modelo;


import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.example.DrogueriaLosTresHermanos.modelo.Permiso.*;

/** Roles iniciales del equipo. Desde el panel se pueden ajustar y crear otros (ver DirectorioAccesos). */
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
}
