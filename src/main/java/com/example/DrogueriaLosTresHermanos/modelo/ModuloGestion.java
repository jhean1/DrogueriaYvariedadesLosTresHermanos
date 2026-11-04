package com.example.DrogueriaLosTresHermanos.modelo;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Módulos de gestión planificados. Todos están en desarrollo: no tienen datos ni acciones todavía. */
public enum ModuloGestion {

    INVENTARIO("inventario", "Inventario", "caja",
        "Control de existencias, lotes y vencimientos para evitar faltantes y pérdidas.",
        Permiso.INVENTARIO_VER,
        List.of("Existencias y stock mínimo o máximo", "Lotes y fechas de vencimiento",
            "Movimientos y ajustes", "Conteos físicos", "Alertas de inventario")),

    VENTAS("ventas", "Ventas y POS", "recibo",
        "Punto de venta para registrar ventas de mostrador y emitir comprobantes.",
        Permiso.VENTAS_VER,
        List.of("Registro de ventas", "Búsqueda de productos y carrito", "Descuentos autorizados",
            "Métodos de pago y comprobantes", "Devoluciones", "Anulaciones autorizadas")),

    COMPRAS("compras", "Compras y proveedores", "camion",
        "Abastecimiento: proveedores, órdenes de compra y recepción de mercancía.",
        Permiso.COMPRAS_VER,
        List.of("Proveedores", "Órdenes de compra", "Recepción de mercancía",
            "Facturas y costos", "Devoluciones a proveedores")),

    CAJA("caja", "Caja y finanzas", "billete",
        "Apertura y cierre de caja, arqueos y control de ingresos y gastos.",
        Permiso.CAJA_VER,
        List.of("Apertura y cierre", "Arqueos y diferencias", "Ingresos y egresos", "Gastos", "Reportes de caja")),

    REPORTES("reportes", "Reportes", "grafica",
        "Indicadores de ventas, inventario, compras y actividad del equipo.",
        Permiso.REPORTES_VER,
        List.of("Ventas", "Inventario y compras", "Productos más vendidos", "Productos con baja rotación",
            "Rentabilidad", "Actividad de empleados")),

    AUDITORIA("auditoria", "Auditoría", "historial",
        "Historial de acciones sensibles para saber quién hizo qué y cuándo.",
        Permiso.AUDITORIA_VER,
        List.of("Inicios de sesión", "Modificaciones", "Operaciones sensibles", "Cambios de permisos",
            "Movimientos de inventario", "Anulaciones")),

    NOTIFICACIONES("notificaciones", "Notificaciones", "campana",
        "Alertas operativas centralizadas para actuar a tiempo.",
        Permiso.NOTIFICACIONES_VER,
        List.of("Productos agotados", "Productos próximos a vencer", "Inventario bajo",
            "Alertas administrativas"));

    private final String slug;
    private final String nombre;
    private final String icono;
    private final String descripcion;
    private final Permiso permiso;
    private final List<String> capacidades;

    ModuloGestion(String slug, String nombre, String icono, String descripcion, Permiso permiso, List<String> capacidades) {
        this.slug = slug;
        this.nombre = nombre;
        this.icono = icono;
        this.descripcion = descripcion;
        this.permiso = permiso;
        this.capacidades = capacidades;
    }

    public static Optional<ModuloGestion> porSlug(String slug) {
        return Stream.of(values()).filter(m -> m.slug.equals(slug)).findFirst();
    }

    public String getSlug() { return slug; }
    public String getNombre() { return nombre; }
    public String getIcono() { return icono; }
    public String getDescripcion() { return descripcion; }
    public Permiso getPermiso() { return permiso; }
    public List<String> getCapacidades() { return capacidades; }
    public String getRuta() { return "/admin/modulos/" + slug; }
}
