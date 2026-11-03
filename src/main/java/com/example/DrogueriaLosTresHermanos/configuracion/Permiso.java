package com.example.DrogueriaLosTresHermanos.configuracion;

/**
 * Permisos del panel, por módulo y acción. Spring Security los usa como
 * autoridades; la interfaz solo los refleja.
 *
 * {@code operativo} indica si hoy existe una función real detrás del permiso.
 * Los demás quedan definidos para cuando se construya el módulo.
 */
public enum Permiso {

    DASHBOARD_VER("dashboard.view", "Panel", "Ver el panel principal", true),

    CLIENTES_VER("customers.view", "Clientes", "Consultar clientes", true),
    CLIENTES_CREAR("customers.create", "Clientes", "Crear clientes", true),

    SUSCRIPCIONES_VER("subscriptions.view", "Suscripciones", "Consultar solicitudes de pago", true),
    SUSCRIPCIONES_APROBAR("subscriptions.approve", "Suscripciones", "Aprobar o rechazar pagos", true),
    SUSCRIPCIONES_ACTIVAR("subscriptions.activate", "Suscripciones", "Activar planes pagados en tienda", true),

    PRODUCTOS_VER("products.view", "Productos", "Consultar productos", false),
    PRODUCTOS_CREAR("products.create", "Productos", "Crear productos", false),
    PRODUCTOS_EDITAR("products.update", "Productos", "Modificar productos", false),
    PRODUCTOS_ELIMINAR("products.delete", "Productos", "Eliminar productos", false),

    INVENTARIO_VER("inventory.view", "Inventario", "Consultar existencias", false),
    INVENTARIO_AJUSTAR("inventory.adjust", "Inventario", "Ajustar existencias", false),
    INVENTARIO_MOVER("inventory.move", "Inventario", "Registrar movimientos y recepciones", false),
    INVENTARIO_CONTAR("inventory.count", "Inventario", "Registrar conteos físicos", false),
    LOTES_VER("batches.view", "Inventario", "Consultar lotes y vencimientos", false),
    LOTES_GESTIONAR("batches.manage", "Inventario", "Gestionar lotes y registros técnicos", false),

    VENTAS_VER("sales.view", "Ventas", "Consultar ventas", false),
    VENTAS_CREAR("sales.create", "Ventas", "Registrar ventas", false),
    VENTAS_ANULAR("sales.cancel", "Ventas", "Anular ventas", false),

    COMPRAS_VER("purchases.view", "Compras", "Consultar compras y proveedores", false),
    COMPRAS_GESTIONAR("purchases.manage", "Compras", "Gestionar compras y proveedores", false),

    CAJA_VER("cash.view", "Caja", "Consultar caja", false),
    CAJA_OPERAR("cash.operate", "Caja", "Abrir y cerrar caja", false),
    FINANZAS_VER("finance.view", "Caja", "Consultar información financiera", false),

    REPORTES_VER("reports.view", "Reportes", "Consultar reportes", false),
    AUDITORIA_VER("audit.view", "Auditoría", "Consultar el historial de acciones", true),
    NOTIFICACIONES_VER("notifications.view", "Notificaciones", "Consultar alertas administrativas", false),

    USUARIOS_GESTIONAR("users.manage", "Accesos", "Gestionar usuarios", true),
    ROLES_GESTIONAR("roles.manage", "Accesos", "Gestionar roles", true),
    PERMISOS_GESTIONAR("permissions.manage", "Accesos", "Gestionar permisos", true),
    AJUSTES_GESTIONAR("settings.manage", "Accesos", "Configurar el sistema", false);

    private final String codigo;
    private final String modulo;
    private final String descripcion;
    private final boolean operativo;

    Permiso(String codigo, String modulo, String descripcion, boolean operativo) {
        this.codigo = codigo;
        this.modulo = modulo;
        this.descripcion = descripcion;
        this.operativo = operativo;
    }

    public String getCodigo() { return codigo; }
    public String getModulo() { return modulo; }
    public String getDescripcion() { return descripcion; }
    public boolean isOperativo() { return operativo; }
}
