# Diccionario de datos

Describe cada tabla y columna de la base de datos de la Droguería y Variedades Los Tres Hermanos.
Este archivo se genera desde MySQL con `herramientas/generar_documentacion.py`; si cambia la base, se vuelve a generar.

**37 tablas, 286 columnas y 73 claves foráneas.** Motor InnoDB y juego de caracteres utf8mb4.

**Cómo leer las tablas:** PK es clave primaria, FK es clave foránea y Único significa que el valor no se puede repetir. Una columna calculada la obtiene MySQL sola.

## Contenido

- **Accesos y auditoría:** [`rol`](#rol), [`permiso`](#permiso), [`rol_permiso`](#rol_permiso), [`usuario`](#usuario), [`usuario_permiso`](#usuario_permiso), [`auditoria`](#auditoria), [`configuracion`](#configuracion)
- **Clientes y suscripciones:** [`cliente`](#cliente), [`favorito`](#favorito), [`recuperacion_clave`](#recuperacion_clave), [`plan_suscripcion`](#plan_suscripcion), [`solicitud_suscripcion`](#solicitud_suscripcion), [`suscripcion`](#suscripcion), [`metodo_pago`](#metodo_pago)
- **Catálogo:** [`categoria`](#categoria), [`producto`](#producto), [`presentacion`](#presentacion)
- **Pedidos y ventas:** [`pedido`](#pedido), [`detalle_pedido`](#detalle_pedido), [`pago_pedido`](#pago_pedido), [`devolucion`](#devolucion), [`detalle_devolucion`](#detalle_devolucion)
- **Inventario:** [`lote`](#lote), [`movimiento_inventario`](#movimiento_inventario), [`conteo_fisico`](#conteo_fisico), [`detalle_conteo`](#detalle_conteo), [`notificacion`](#notificacion)
- **Compras y proveedores:** [`proveedor`](#proveedor), [`orden_compra`](#orden_compra), [`detalle_orden_compra`](#detalle_orden_compra), [`recepcion_mercancia`](#recepcion_mercancia), [`detalle_recepcion`](#detalle_recepcion), [`devolucion_proveedor`](#devolucion_proveedor), [`detalle_devolucion_proveedor`](#detalle_devolucion_proveedor)
- **Caja:** [`sesion_caja`](#sesion_caja), [`arqueo_caja`](#arqueo_caja), [`movimiento_caja`](#movimiento_caja)

## Accesos y auditoría

Quién entra al panel, qué puede hacer y qué hizo.

### rol

Roles del equipo de trabajo (RF015).

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | INT UNSIGNED | Sí | PK | autonumérico | Identificador del rol |
| `codigo_rol` | VARCHAR(40) | Sí | Único |  | Código interno del rol, por ejemplo cajero |
| `nombre_rol` | VARCHAR(80) | Sí |  |  | Nombre visible del rol |
| `descripcion_rol` | VARCHAR(255) | No |  |  | Responsabilidades del rol |
| `rol_protegido` | BOOLEAN | Sí |  | falso | Si el rol está protegido contra cambios |

### permiso

Acciones que se pueden autorizar en el panel.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | INT UNSIGNED | Sí | PK | autonumérico | Identificador del permiso |
| `codigo_permiso` | VARCHAR(50) | Sí | Único |  | Código que usa la aplicación, por ejemplo products.view |
| `modulo_permiso` | VARCHAR(40) | Sí |  |  | Módulo al que pertenece el permiso |
| `descripcion_permiso` | VARCHAR(255) | Sí |  |  | Qué permite hacer |
| `permiso_operativo` | BOOLEAN | Sí |  | falso | Si ya existe en la aplicación una función real detrás del permiso |

### rol_permiso

Permisos que tiene cada rol.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `rol_id` | INT UNSIGNED | Sí | PK, FK |  | Rol que recibe el permiso |
| `permiso_id` | INT UNSIGNED | Sí | PK, FK |  | Permiso que se le otorga al rol |

**Se relaciona con:**

- `permiso_id` → [`permiso`](#permiso) (`id`)
- `rol_id` → [`rol`](#rol) (`id`)

### usuario

Cuentas de los empleados de la droguería.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador del empleado |
| `rol_id` | INT UNSIGNED | Sí | FK |  | Rol que tiene asignado el empleado |
| `nombre_usuario` | VARCHAR(120) | Sí |  |  | Nombre completo del empleado |
| `correo_usuario` | VARCHAR(120) | Sí | Único |  | Correo con el que inicia sesión |
| `clave_hash` | VARCHAR(255) | Sí |  |  | Contraseña cifrada; nunca se guarda en texto plano |
| `usuario_activo` | BOOLEAN | Sí |  | verdadero | Si puede iniciar sesión |
| `usuario_protegido` | BOOLEAN | Sí |  | falso | Si es una cuenta protegida contra cambios, como la principal |
| `fecha_creacion` | DATETIME | Sí |  | fecha actual | Cuándo se creó la cuenta |

**Se relaciona con:**

- `rol_id` → [`rol`](#rol) (`id`)

### usuario_permiso

Excepciones de permisos por empleado.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `usuario_id` | BIGINT UNSIGNED | Sí | PK, FK |  | Empleado al que aplica la excepción |
| `permiso_id` | INT UNSIGNED | Sí | PK, FK |  | Permiso que se agrega o se quita |
| `tipo_excepcion` | ENUM: concedido, revocado | Sí |  |  | Si se agrega o se quita respecto a lo que da el rol |

**Se relaciona con:**

- `permiso_id` → [`permiso`](#permiso) (`id`)
- `usuario_id` → [`usuario`](#usuario) (`id`)

### auditoria

Historial de acciones sensibles; no se puede modificar ni borrar.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador del evento |
| `usuario_id` | BIGINT UNSIGNED | No | FK |  | Empleado que actuó, si se identificó |
| `actor_auditoria` | VARCHAR(120) | Sí |  |  | Correo o nombre con el que se registró la acción |
| `accion_auditoria` | VARCHAR(120) | Sí |  |  | Acción realizada, por ejemplo Producto modificado |
| `detalle_auditoria` | TEXT | No |  |  | Información adicional sobre la acción |
| `fecha_auditoria` | DATETIME | Sí |  | fecha actual | Fecha y hora en que ocurrió |

**Se relaciona con:**

- `usuario_id` → [`usuario`](#usuario) (`id`)

### configuracion

Ajustes generales del sistema.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `clave_configuracion` | VARCHAR(60) | Sí | PK |  | Nombre del ajuste, por ejemplo costo_domicilio |
| `valor_configuracion` | VARCHAR(255) | Sí |  |  | Valor que tiene el ajuste |
| `descripcion_configuracion` | VARCHAR(255) | No |  |  | Para qué sirve el ajuste |
| `actualizado_por_id` | BIGINT UNSIGNED | No | FK |  | Último empleado que lo cambió |
| `fecha_actualizacion` | DATETIME | Sí |  | fecha actual | Fecha y hora del último cambio |

**Se relaciona con:**

- `actualizado_por_id` → [`usuario`](#usuario) (`id`)

## Clientes y suscripciones

Cuentas de la tienda, compra de planes y su vigencia.

### cliente

Clientes con cuenta en la tienda.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador del cliente |
| `nombre_cliente` | VARCHAR(120) | Sí |  |  | Nombre completo del cliente |
| `correo_cliente` | VARCHAR(120) | No | Único |  | Correo de acceso; se borra al eliminar la cuenta |
| `telefono_cliente` | VARCHAR(20) | No |  |  | Teléfono de contacto; se borra al eliminar la cuenta |
| `direccion_cliente` | VARCHAR(255) | No |  |  | Dirección de entrega; se borra al eliminar la cuenta |
| `clave_hash` | VARCHAR(255) | No |  |  | Contraseña cifrada; se borra al eliminar la cuenta |
| `foto_cliente` | VARCHAR(255) | No |  |  | Ubicación de la foto de perfil |
| `cuenta_activa` | BOOLEAN | Sí |  | verdadero | Falso cuando la cuenta fue eliminada |
| `creado_por_id` | BIGINT UNSIGNED | No | FK |  | Empleado que creó la cuenta, si no se registró el cliente |
| `fecha_autorizacion_datos` | DATETIME | Sí |  |  | Cuándo autorizó el tratamiento de sus datos (Ley 1581 de 2012) |
| `version_politica` | VARCHAR(20) | Sí |  |  | Versión de la política que aceptó. POR REVISAR: la página todavía no numera su política |
| `fecha_registro` | DATETIME | Sí |  | fecha actual | Cuándo se creó la cuenta |
| `fecha_eliminacion` | DATETIME | No |  |  | Cuándo se eliminó la cuenta |

**Se relaciona con:**

- `creado_por_id` → [`usuario`](#usuario) (`id`)

**Reglas (CHECK):**

- `ck_cliente_activo`: `((`cuenta_activa` = false) or ((`correo_cliente` is not null) and (`telefono_cliente` is not null) and (`direccion_cliente` is not null) and (`clave_hash` is not null)))`
- `ck_cliente_eliminado`: `((`cuenta_activa` = true) or (`fecha_eliminacion` is not null))`

### favorito

Productos favoritos de cada cliente.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `cliente_id` | BIGINT UNSIGNED | Sí | PK, FK |  | Cliente que lo guardó |
| `producto_id` | BIGINT UNSIGNED | Sí | PK, FK |  | Producto guardado |
| `fecha_favorito` | DATETIME | Sí |  | fecha actual | Cuándo se guardó |

**Se relaciona con:**

- `cliente_id` → [`cliente`](#cliente) (`id`)
- `producto_id` → [`producto`](#producto) (`id`)

### recuperacion_clave

Códigos temporales para recuperar la contraseña.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador de la solicitud |
| `cliente_id` | BIGINT UNSIGNED | Sí | FK |  | Cliente que pidió recuperar la contraseña |
| `token_hash` | VARCHAR(255) | Sí | Único |  | Código de recuperación cifrado |
| `fecha_expiracion` | DATETIME | Sí |  |  | Hasta cuándo sirve el código |
| `fecha_uso` | DATETIME | No |  |  | Cuándo se usó; un código solo sirve una vez |
| `fecha_creacion` | DATETIME | Sí |  | fecha actual | Cuándo se solicitó |

**Se relaciona con:**

- `cliente_id` → [`cliente`](#cliente) (`id`)

### plan_suscripcion

Planes de suscripción disponibles.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | INT UNSIGNED | Sí | PK | autonumérico | Identificador del plan |
| `nombre_plan` | VARCHAR(40) | Sí | Único |  | Nombre del plan |
| `descripcion_plan` | VARCHAR(255) | No |  |  | Qué ofrece el plan |
| `precio_mensual` | DECIMAL(12,2) | Sí |  |  | Precio vigente en pesos |
| `descuento_porcentaje` | TINYINT UNSIGNED | Sí |  |  | Descuento en la tienda durante la vigencia |
| `duracion_dias` | SMALLINT UNSIGNED | Sí |  | 30 | Días de vigencia |
| `domicilio_prioritario` | BOOLEAN | Sí |  | falso | Si incluye domicilio prioritario. POR REVISAR: la página no define en qué consiste la prioridad |
| `ofertas_anticipadas` | BOOLEAN | Sí |  | falso | Incluye ofertas anticipadas |
| `soporte_whatsapp` | BOOLEAN | Sí |  | falso | Incluye soporte por WhatsApp |
| `acceso_exclusivo` | BOOLEAN | Sí |  | falso | Si incluye acceso exclusivo. POR REVISAR: la página dice unas veces ofertas y otras lanzamientos |
| `plan_activo` | BOOLEAN | Sí |  | verdadero | Si se puede comprar |

**Reglas (CHECK):**

- `ck_plan_descuento`: `(`descuento_porcentaje` <= 100)`
- `ck_plan_duracion`: `(`duracion_dias` > 0)`
- `ck_plan_precio`: `(`precio_mensual` >= 0)`

### solicitud_suscripcion

Compras de un plan, en línea o en el local, con su revisión.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador de la solicitud |
| `cliente_id` | BIGINT UNSIGNED | Sí | FK |  | Cliente que compra el plan |
| `plan_id` | INT UNSIGNED | Sí | FK |  | Plan que el cliente quiere comprar |
| `metodo_pago_id` | INT UNSIGNED | Sí | FK |  | Forma de pago usada |
| `origen_solicitud` | ENUM: web, tienda | Sí |  |  | Si se compró en línea o en el local |
| `monto_pagado` | DECIMAL(12,2) | Sí |  |  | Valor que se pagó |
| `referencia_pago` | VARCHAR(60) | No |  |  | Número de referencia de la transacción |
| `numero_aprobacion` | VARCHAR(60) | No |  |  | Número de aprobación del banco |
| `titular_pago` | VARCHAR(120) | No |  |  | Titular de la cuenta que pagó |
| `fecha_pago` | DATE | No |  |  | Día en que se hizo el pago |
| `comprobante_pago` | VARCHAR(255) | No |  |  | Ubicación de la imagen del comprobante de pago |
| `nota_solicitud` | VARCHAR(255) | No |  |  | Observación del cliente o del empleado |
| `estado_solicitud` | ENUM: pendiente, aprobado, rechazado | Sí |  | pendiente | Resultado de la revisión |
| `motivo_rechazo` | VARCHAR(255) | No |  |  | Por qué se rechazó |
| `revisado_por_id` | BIGINT UNSIGNED | No | FK |  | Empleado que revisó |
| `fecha_revision` | DATETIME | No |  |  | Cuándo se revisó |
| `fecha_solicitud` | DATETIME | Sí |  | fecha actual | Cuándo se envió |

**Se relaciona con:**

- `cliente_id` → [`cliente`](#cliente) (`id`)
- `metodo_pago_id` → [`metodo_pago`](#metodo_pago) (`id`)
- `plan_id` → [`plan_suscripcion`](#plan_suscripcion) (`id`)
- `revisado_por_id` → [`usuario`](#usuario) (`id`)

**Combinaciones que no se pueden repetir:** (`id`, `cliente_id`, `plan_id`)

**Reglas (CHECK):**

- `ck_solicitud_monto`: `(`monto_pagado` >= 0)`
- `ck_solicitud_revision`: `((`estado_solicitud` = _utf8mb4'pendiente') or ((`revisado_por_id` is not null) and (`fecha_revision` is not null)))`

### suscripcion

Planes contratados por los clientes y su vigencia.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador de la suscripción |
| `cliente_id` | BIGINT UNSIGNED | Sí | FK |  | Cliente titular de la suscripción |
| `plan_id` | INT UNSIGNED | Sí | FK |  | Plan que compró el cliente |
| `solicitud_id` | BIGINT UNSIGNED | Sí | FK, Único |  | Solicitud aprobada que la originó |
| `descuento_porcentaje` | TINYINT UNSIGNED | Sí |  |  | Descuento acordado al comprar, aunque el plan cambie después |
| `fecha_inicio` | DATETIME | Sí |  |  | Inicio de la vigencia |
| `fecha_vencimiento` | DATETIME | Sí |  |  | Fin de la vigencia |
| `estado_suscripcion` | ENUM: activa, vencida, cancelada, reemplazada | Sí |  | activa | Situación del plan; reemplazada significa que se renovó |
| `fecha_cancelacion` | DATETIME | No |  |  | Cuándo se canceló |
| `cliente_con_plan_activo` | BIGINT UNSIGNED | No | Único, Calculada |  | Se calcula solo: impide dos planes activos para un cliente |

**Se relaciona con:**

- `cliente_id` → [`cliente`](#cliente) (`id`)
- `plan_id` → [`plan_suscripcion`](#plan_suscripcion) (`id`)
- `solicitud_id, cliente_id, plan_id` → [`solicitud_suscripcion`](#solicitud_suscripcion) (`id, cliente_id, plan_id`)

**Combinaciones que no se pueden repetir:** (`id`, `cliente_id`)

**Reglas (CHECK):**

- `ck_suscripcion_cancelada`: `((`estado_suscripcion` <> _utf8mb4'cancelada') or (`fecha_cancelacion` is not null))`
- `ck_suscripcion_descuento`: `(`descuento_porcentaje` <= 100)`
- `ck_suscripcion_fechas`: `(`fecha_vencimiento` > `fecha_inicio`)`

### metodo_pago

Formas de pago aceptadas.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | INT UNSIGNED | Sí | PK | autonumérico | Identificador del método |
| `codigo_metodo_pago` | VARCHAR(30) | Sí | Único |  | Código interno del método, por ejemplo nequi |
| `nombre_metodo_pago` | VARCHAR(60) | Sí |  |  | Nombre que se muestra en la página |
| `numero_cuenta` | VARCHAR(60) | No |  |  | Número de cuenta donde se recibe el pago |
| `titular_cuenta` | VARCHAR(120) | No |  |  | Titular de la cuenta donde se recibe el pago |
| `metodo_pago_activo` | BOOLEAN | Sí |  | verdadero | Si está disponible para pagar |

## Catálogo

Lo que se vende: categorías, productos y presentaciones.

### categoria

Categorías y subcategorías de productos.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | INT UNSIGNED | Sí | PK | autonumérico | Identificador de la categoría |
| `codigo_categoria` | VARCHAR(60) | Sí | Único |  | Código interno, por ejemplo analgesicos |
| `nombre_categoria` | VARCHAR(80) | Sí |  |  | Nombre que se muestra en la página |
| `tipo_categoria` | ENUM: medicamento, variedad | Sí |  |  | Tipo de productos que agrupa |
| `categoria_padre_id` | INT UNSIGNED | No | FK |  | Categoría superior; vacía si es principal (RF019) |

**Se relaciona con:**

- `categoria_padre_id` → [`categoria`](#categoria) (`id`)

### producto

Medicamentos y variedades (RF001, RF003).

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador del producto |
| `categoria_id` | INT UNSIGNED | Sí | FK |  | Categoría del producto; de ella se obtiene su tipo |
| `nombre_producto` | VARCHAR(150) | Sí |  |  | Nombre comercial del producto |
| `descripcion_producto` | TEXT | No |  |  | Descripción para la tienda |
| `principio_activo` | VARCHAR(150) | No |  |  | Principio activo, en medicamentos |
| `contenido_producto` | VARCHAR(100) | No |  |  | Contenido o tamaño, por ejemplo 100 ml |
| `requiere_formula` | BOOLEAN | Sí |  | falso | Si se vende solo con fórmula médica |
| `producto_activo` | BOOLEAN | Sí |  | verdadero | Si aparece en la tienda |
| `foto_producto` | VARCHAR(255) | No |  |  | Ubicación de la foto subida desde el panel |
| `imagen_catalogo` | VARCHAR(255) | No |  |  | Imagen asignada en el catálogo inicial. POR REVISAR: confirmar cómo la usa la tienda frente a la foto |

**Se relaciona con:**

- `categoria_id` → [`categoria`](#categoria) (`id`)

### presentacion

Unidad que se vende y se almacena; las variedades tienen una sola.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador de lo que se vende |
| `producto_id` | BIGINT UNSIGNED | Sí | FK |  | Producto al que pertenece |
| `tipo_presentacion` | VARCHAR(60) | Sí |  |  | Forma de venta, por ejemplo Tableta o Unidad |
| `concentracion` | VARCHAR(60) | No |  |  | Concentración, por ejemplo 500 mg |
| `marca` | VARCHAR(80) | No |  |  | Marca o laboratorio |
| `codigo_barras` | VARCHAR(40) | No | Único |  | Código de barras para el lector |
| `registro_sanitario` | VARCHAR(40) | No |  |  | Registro sanitario, cuando aplica |
| `precio_venta` | DECIMAL(12,2) | Sí |  |  | Precio de venta vigente en pesos |
| `stock_minimo` | INT UNSIGNED | Sí |  | 0 | Por debajo de este valor se genera una alerta (RF004) |
| `stock_maximo` | INT UNSIGNED | No |  |  | Existencia máxima recomendada |
| `presentacion_activa` | BOOLEAN | Sí |  | verdadero | Si se puede vender |

**Se relaciona con:**

- `producto_id` → [`producto`](#producto) (`id`)

**Reglas (CHECK):**

- `ck_presentacion_precio`: `(`precio_venta` >= 0)`
- `ck_presentacion_stock`: `((`stock_maximo` is null) or (`stock_maximo` >= `stock_minimo`))`

## Pedidos y ventas

Pedidos en línea y ventas de mostrador, con sus pagos y devoluciones.

### pedido

Pedidos en línea y ventas de mostrador (RF009).

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador del pedido o venta |
| `numero_pedido` | VARCHAR(20) | Sí | Único |  | Número visible, por ejemplo LTH-K8F3A2 |
| `numero_comprobante` | VARCHAR(30) | No | Único |  | Número del comprobante interno (RF010); no es factura electrónica |
| `canal_venta` | ENUM: web, pos | Sí |  |  | Por dónde se vendió: web es la tienda en línea y pos es el mostrador |
| `cliente_id` | BIGINT UNSIGNED | No | FK |  | Cliente que compró; vacío en ventas de mostrador sin cuenta |
| `suscripcion_id` | BIGINT UNSIGNED | No | FK |  | Suscripción que dio el descuento |
| `vendedor_id` | BIGINT UNSIGNED | No | FK |  | Empleado que registró la venta (RF012) |
| `sesion_caja_id` | BIGINT UNSIGNED | No | FK |  | Turno de caja de la venta |
| `modalidad_entrega` | ENUM: tienda, domicilio | Sí |  |  | Si se recoge en la tienda o se lleva a domicilio |
| `direccion_entrega` | VARCHAR(255) | No |  |  | Dirección a la que se entrega este pedido |
| `subtotal_pedido` | DECIMAL(12,2) | Sí |  |  | Suma de cantidad por precio original |
| `descuento_porcentaje` | TINYINT UNSIGNED | Sí |  | 0 | Porcentaje aplicado (RF014) |
| `descuento_aplicado` | DECIMAL(12,2) | Sí |  | 0.00 | Valor del descuento en pesos |
| `descuento_autorizado_por_id` | BIGINT UNSIGNED | No | FK |  | Empleado que autorizó un descuento manual |
| `costo_domicilio` | DECIMAL(12,2) | Sí |  | 0.00 | Valor cobrado por el domicilio |
| `total_pedido` | DECIMAL(12,2) | Sí | Calculada |  | Se calcula solo: subtotal - descuento + domicilio |
| `estado_pedido` | ENUM: pendiente, en_proceso, entregado, cancelado | Sí |  | pendiente | En qué punto va el pedido |
| `anulado_por_id` | BIGINT UNSIGNED | No | FK |  | Empleado que anuló |
| `motivo_anulacion` | VARCHAR(255) | No |  |  | Por qué se anuló |
| `fecha_anulacion` | DATETIME | No |  |  | Cuándo se anuló |
| `fecha_creacion` | DATETIME | Sí |  | fecha actual | Fecha y hora en que se registró |

**Se relaciona con:**

- `anulado_por_id` → [`usuario`](#usuario) (`id`)
- `descuento_autorizado_por_id` → [`usuario`](#usuario) (`id`)
- `cliente_id` → [`cliente`](#cliente) (`id`)
- `sesion_caja_id` → [`sesion_caja`](#sesion_caja) (`id`)
- `suscripcion_id, cliente_id` → [`suscripcion`](#suscripcion) (`id, cliente_id`)
- `vendedor_id` → [`usuario`](#usuario) (`id`)

**Reglas (CHECK):**

- `ck_pedido_anulacion`: `((`estado_pedido` <> _utf8mb4'cancelado') or (`fecha_anulacion` is not null))`
- `ck_pedido_domicilio`: `((`modalidad_entrega` <> _utf8mb4'domicilio') or (`direccion_entrega` is not null))`
- `ck_pedido_montos`: `((`subtotal_pedido` >= 0) and (`descuento_aplicado` >= 0) and (`descuento_aplicado` <= `subtotal_pedido`) and (`costo_domicilio` >= 0))`
- `ck_pedido_porcentaje`: `(`descuento_porcentaje` <= 100)`
- `ck_pedido_pos`: `((`canal_venta` <> _utf8mb4'pos') or (`vendedor_id` is not null))`
- `ck_pedido_web`: `((`canal_venta` <> _utf8mb4'web') or (`cliente_id` is not null))`

### detalle_pedido

Productos de cada pedido.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador de la línea |
| `pedido_id` | BIGINT UNSIGNED | Sí | FK |  | Pedido al que pertenece |
| `presentacion_id` | BIGINT UNSIGNED | Sí | FK |  | Presentación que se vendió |
| `nombre_producto` | VARCHAR(255) | Sí |  |  | Nombre al momento de la venta, para el comprobante |
| `cantidad_vendida` | INT UNSIGNED | Sí |  |  | Unidades vendidas |
| `precio_original` | DECIMAL(12,2) | Sí |  |  | Precio de lista al momento de la venta |
| `precio_unitario` | DECIMAL(12,2) | Sí |  |  | Precio cobrado, ya con descuento |
| `subtotal_linea` | DECIMAL(12,2) | Sí | Calculada |  | Se calcula solo: cantidad por precio cobrado |
| `formula_soporte` | VARCHAR(255) | No |  |  | Ubicación del archivo de la fórmula médica, si se exige |

**Se relaciona con:**

- `pedido_id` → [`pedido`](#pedido) (`id`)
- `presentacion_id` → [`presentacion`](#presentacion) (`id`)

**Reglas (CHECK):**

- `ck_detalle_cantidad`: `(`cantidad_vendida` > 0)`
- `ck_detalle_precios`: `((`precio_original` >= 0) and (`precio_unitario` >= 0) and (`precio_unitario` <= `precio_original`))`

### pago_pedido

Pagos de cada pedido; un pedido puede tener varios.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador del pago |
| `pedido_id` | BIGINT UNSIGNED | Sí | FK |  | Pedido al que corresponde el pago |
| `metodo_pago_id` | INT UNSIGNED | Sí | FK |  | Forma de pago usada |
| `monto_pago` | DECIMAL(12,2) | Sí |  |  | Valor que se pagó |
| `referencia_pago` | VARCHAR(60) | No |  |  | Referencia de la transacción |
| `estado_pago` | ENUM: pendiente, confirmado, rechazado, reembolsado | Sí |  | pendiente | Si el pago está pendiente, confirmado, rechazado o reembolsado |
| `fecha_pago` | DATETIME | Sí |  | fecha actual | Fecha y hora en que se registró |

**Se relaciona con:**

- `metodo_pago_id` → [`metodo_pago`](#metodo_pago) (`id`)
- `pedido_id` → [`pedido`](#pedido) (`id`)

**Reglas (CHECK):**

- `ck_pago_monto`: `(`monto_pago` > 0)`

### devolucion

Devoluciones de clientes.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador de la devolución |
| `pedido_id` | BIGINT UNSIGNED | Sí | FK |  | Pedido al que corresponde la devolución |
| `usuario_id` | BIGINT UNSIGNED | Sí | FK |  | Empleado que la registró |
| `motivo_devolucion` | VARCHAR(255) | Sí |  |  | Por qué el cliente devolvió el producto |
| `fecha_devolucion` | DATETIME | Sí |  | fecha actual | Fecha y hora en que ocurrió |

**Se relaciona con:**

- `pedido_id` → [`pedido`](#pedido) (`id`)
- `usuario_id` → [`usuario`](#usuario) (`id`)

### detalle_devolucion

Productos de cada devolución de cliente.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador de la línea |
| `devolucion_id` | BIGINT UNSIGNED | Sí | FK |  | Devolución a la que pertenece esta línea |
| `detalle_pedido_id` | BIGINT UNSIGNED | Sí | FK |  | Producto del pedido que se devuelve |
| `lote_id` | BIGINT UNSIGNED | No | FK |  | Lote al que regresa, si reingresa |
| `cantidad_devuelta` | INT UNSIGNED | Sí |  |  | Unidades devueltas |
| `monto_devuelto` | DECIMAL(12,2) | Sí |  |  | Dinero devuelto al cliente |
| `reingresa_inventario` | BOOLEAN | Sí |  | falso | Si el producto se puede volver a vender |

**Se relaciona con:**

- `detalle_pedido_id` → [`detalle_pedido`](#detalle_pedido) (`id`)
- `devolucion_id` → [`devolucion`](#devolucion) (`id`)
- `lote_id` → [`lote`](#lote) (`id`)

**Reglas (CHECK):**

- `ck_detdev_cantidad`: `(`cantidad_devuelta` > 0)`
- `ck_detdev_monto`: `(`monto_devuelto` >= 0)`
- `ck_detdev_reingreso`: `((`reingresa_inventario` = false) or (`lote_id` is not null))`

## Inventario

Lotes, vencimientos, historial de entradas y salidas, conteos y alertas.

### lote

Lotes con su existencia y vencimiento.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador del lote |
| `presentacion_id` | BIGINT UNSIGNED | Sí | FK |  | Presentación que contiene el lote |
| `numero_lote` | VARCHAR(50) | Sí |  |  | Número impreso por el fabricante (RF002) |
| `fecha_vencimiento` | DATE | No |  |  | Vencimiento; vacío si el producto no vence (RF005) |
| `cantidad_actual` | INT UNSIGNED | Sí |  | 0 | Existencia; solo cambia mediante movimientos |
| `costo_unitario` | DECIMAL(12,2) | Sí |  |  | Costo de compra por unidad |
| `fecha_ingreso` | DATETIME | Sí |  | fecha actual | Fecha y hora en que se registró |

**Se relaciona con:**

- `presentacion_id` → [`presentacion`](#presentacion) (`id`)

**Combinaciones que no se pueden repetir:** (`presentacion_id`, `numero_lote`)

**Reglas (CHECK):**

- `ck_lote_costo`: `(`costo_unitario` >= 0)`

### movimiento_inventario

Historial de entradas y salidas de cada lote (RF006); no se puede modificar ni borrar.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador del movimiento |
| `lote_id` | BIGINT UNSIGNED | Sí | FK |  | Lote cuya existencia cambia |
| `usuario_id` | BIGINT UNSIGNED | No | FK |  | Empleado; vacío solo en ventas automáticas de la tienda en línea |
| `tipo_movimiento` | ENUM: entrada, venta, anulacion, ajuste, devolucion_cliente, devolucion_proveedor, conteo | Sí |  |  | Motivo por el que entró o salió el dinero |
| `cantidad_movida` | INT | Sí |  |  | Positiva si entra y negativa si sale |
| `saldo_resultante` | INT UNSIGNED | Sí |  | 0 | Existencia del lote después del movimiento; la calcula un disparador |
| `motivo_movimiento` | VARCHAR(255) | No |  |  | Explicación del movimiento; obligatoria en ajustes |
| `detalle_pedido_id` | BIGINT UNSIGNED | No | FK |  | Producto del pedido que se vendió o se anuló |
| `detalle_recepcion_id` | BIGINT UNSIGNED | No | FK |  | Línea de la entrada de mercancía que originó el movimiento |
| `detalle_conteo_id` | BIGINT UNSIGNED | No | FK |  | Línea del conteo que originó el movimiento |
| `detalle_devolucion_id` | BIGINT UNSIGNED | No | FK |  | Línea de la devolución del cliente que originó el movimiento |
| `detalle_devolucion_proveedor_id` | BIGINT UNSIGNED | No | FK |  | Línea de la devolución al proveedor que originó el movimiento |
| `fecha_movimiento` | DATETIME | Sí |  | fecha actual | Fecha y hora en que ocurrió |

**Se relaciona con:**

- `detalle_conteo_id` → [`detalle_conteo`](#detalle_conteo) (`id`)
- `detalle_devolucion_id` → [`detalle_devolucion`](#detalle_devolucion) (`id`)
- `detalle_devolucion_proveedor_id` → [`detalle_devolucion_proveedor`](#detalle_devolucion_proveedor) (`id`)
- `lote_id` → [`lote`](#lote) (`id`)
- `detalle_pedido_id` → [`detalle_pedido`](#detalle_pedido) (`id`)
- `detalle_recepcion_id` → [`detalle_recepcion`](#detalle_recepcion) (`id`)
- `usuario_id` → [`usuario`](#usuario) (`id`)

**Reglas (CHECK):**

- `ck_movinv_origen`: `(((`tipo_movimiento` = _utf8mb4'entrada') and (`detalle_recepcion_id` is not null)) or ((`tipo_movimiento` in (_utf8mb4'venta',_utf8mb4'anulacion')) and (`detalle_pedido_id` is not null)) or ((`tipo_movimiento` = _utf8mb4'ajuste') and (`motivo_movimiento` is not null)) or ((`tipo_movimiento` = _utf8mb4'devolucion_cliente') and (`detalle_devolucion_id` is not null)) or ((`tipo_movimiento` = _utf8mb4'devolucion_proveedor') and (`detalle_devolucion_proveedor_id` is not null)) or ((`tipo_movimiento` = _utf8mb4'conteo') and (`detalle_conteo_id` is not null)))`
- `ck_movinv_signo`: `(((`tipo_movimiento` in (_utf8mb4'entrada',_utf8mb4'anulacion',_utf8mb4'devolucion_cliente')) and (`cantidad_movida` > 0)) or ((`tipo_movimiento` in (_utf8mb4'venta',_utf8mb4'devolucion_proveedor')) and (`cantidad_movida` < 0)) or ((`tipo_movimiento` in (_utf8mb4'ajuste',_utf8mb4'conteo')) and (`cantidad_movida` <> 0)))`
- `ck_movinv_usuario`: `((`usuario_id` is not null) or (`tipo_movimiento` in (_utf8mb4'venta',_utf8mb4'anulacion')))`

### conteo_fisico

Conteos físicos del inventario.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador del conteo |
| `usuario_id` | BIGINT UNSIGNED | Sí | FK |  | Empleado responsable |
| `estado_conteo` | ENUM: abierto, cerrado, anulado | Sí |  | abierto | Si el conteo sigue abierto, ya se cerró o se anuló |
| `observacion_conteo` | VARCHAR(255) | No |  |  | Comentarios sobre el conteo |
| `fecha_inicio` | DATETIME | Sí |  | fecha actual | Fecha y hora en que empezó el conteo |
| `fecha_cierre` | DATETIME | No |  |  | Fecha y hora en que terminó el conteo |

**Se relaciona con:**

- `usuario_id` → [`usuario`](#usuario) (`id`)

**Reglas (CHECK):**

- `ck_conteo_cierre`: `((`estado_conteo` <> _utf8mb4'cerrado') or (`fecha_cierre` is not null))`

### detalle_conteo

Resultado del conteo por lote.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador de la línea |
| `conteo_id` | BIGINT UNSIGNED | Sí | FK |  | Conteo al que pertenece esta línea |
| `lote_id` | BIGINT UNSIGNED | Sí | FK |  | Lote que se contó |
| `cantidad_sistema` | INT UNSIGNED | Sí |  |  | Cantidad que indicaba el sistema |
| `cantidad_contada` | INT UNSIGNED | Sí |  |  | Cantidad encontrada al contar |

**Se relaciona con:**

- `conteo_id` → [`conteo_fisico`](#conteo_fisico) (`id`)
- `lote_id` → [`lote`](#lote) (`id`)

**Combinaciones que no se pueden repetir:** (`conteo_id`, `lote_id`)

### notificacion

Alertas de stock, vencimiento y administración.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador de la alerta |
| `tipo_alerta` | ENUM: agotado, proximo_vencer, inventario_bajo, administrativa | Sí |  |  | Clase de alerta (RF004, RF005) |
| `mensaje_alerta` | VARCHAR(255) | Sí |  |  | Mensaje que ve el empleado |
| `presentacion_id` | BIGINT UNSIGNED | No | FK |  | Presentación a la que se refiere la alerta |
| `lote_id` | BIGINT UNSIGNED | No | FK |  | Lote al que se refiere la alerta |
| `alerta_atendida` | BOOLEAN | Sí |  | falso | Si la alerta ya se resolvió |
| `atendida_por_id` | BIGINT UNSIGNED | No | FK |  | Empleado que la atendió; vacío si se resolvió sola |
| `fecha_atencion` | DATETIME | No |  |  | Fecha y hora en que se atendió la alerta |
| `fecha_creacion` | DATETIME | Sí |  | fecha actual | Fecha y hora en que se generó la alerta |
| `clave_abierta` | VARCHAR(60) | No | Único, Calculada |  | Se calcula solo: evita repetir una alerta que sigue abierta |

**Se relaciona con:**

- `lote_id` → [`lote`](#lote) (`id`)
- `presentacion_id` → [`presentacion`](#presentacion) (`id`)
- `atendida_por_id` → [`usuario`](#usuario) (`id`)

## Compras y proveedores

Proveedores, órdenes de compra, entradas de mercancía y devoluciones.

### proveedor

Proveedores de la droguería (RF008).

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador del proveedor |
| `nit_proveedor` | VARCHAR(20) | Sí | Único |  | NIT o documento del proveedor |
| `nombre_proveedor` | VARCHAR(150) | Sí |  |  | Nombre o razón social del proveedor |
| `contacto_proveedor` | VARCHAR(120) | No |  |  | Persona de contacto en el proveedor |
| `telefono_proveedor` | VARCHAR(20) | No |  |  | Teléfono del proveedor |
| `correo_proveedor` | VARCHAR(120) | No |  |  | Correo del proveedor |
| `direccion_proveedor` | VARCHAR(255) | No |  |  | Dirección del proveedor |
| `proveedor_activo` | BOOLEAN | Sí |  | verdadero | Si se le sigue comprando |

### orden_compra

Órdenes de compra a proveedores.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador de la orden |
| `numero_orden` | VARCHAR(20) | Sí | Único |  | Número de la orden de compra |
| `proveedor_id` | BIGINT UNSIGNED | Sí | FK |  | Proveedor al que se le compra |
| `usuario_id` | BIGINT UNSIGNED | Sí | FK |  | Empleado que la creó |
| `estado_orden` | ENUM: borrador, enviada, parcial, recibida, cancelada | Sí |  | borrador | En qué punto va la orden de compra |
| `fecha_creacion` | DATETIME | Sí |  | fecha actual | Cuándo se creó |
| `fecha_esperada` | DATE | No |  |  | Cuándo se espera recibir |

**Se relaciona con:**

- `proveedor_id` → [`proveedor`](#proveedor) (`id`)
- `usuario_id` → [`usuario`](#usuario) (`id`)

### detalle_orden_compra

Productos de cada orden de compra.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador de la línea |
| `orden_compra_id` | BIGINT UNSIGNED | Sí | FK |  | Orden de compra a la que pertenece esta línea |
| `presentacion_id` | BIGINT UNSIGNED | Sí | FK |  | Presentación que se pide |
| `cantidad_pedida` | INT UNSIGNED | Sí |  |  | Unidades pedidas |
| `costo_unitario` | DECIMAL(12,2) | Sí |  |  | Costo acordado por unidad |

**Se relaciona con:**

- `orden_compra_id` → [`orden_compra`](#orden_compra) (`id`)
- `presentacion_id` → [`presentacion`](#presentacion) (`id`)

**Reglas (CHECK):**

- `ck_detorden_cantidad`: `(`cantidad_pedida` > 0)`
- `ck_detorden_costo`: `(`costo_unitario` >= 0)`

### recepcion_mercancia

Entradas de mercancía (RF002).

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador de la entrada |
| `proveedor_id` | BIGINT UNSIGNED | Sí | FK |  | Quién entregó (RF002) |
| `orden_compra_id` | BIGINT UNSIGNED | No | FK |  | Orden relacionada; una entrada puede no tenerla |
| `usuario_id` | BIGINT UNSIGNED | Sí | FK |  | Empleado que recibió |
| `numero_factura` | VARCHAR(40) | Sí |  |  | Factura del proveedor |
| `total_factura` | DECIMAL(12,2) | Sí |  |  | Valor de esa factura |
| `fecha_recepcion` | DATETIME | Sí |  | fecha actual | Cuándo se recibió (RF002) |

**Se relaciona con:**

- `orden_compra_id` → [`orden_compra`](#orden_compra) (`id`)
- `proveedor_id` → [`proveedor`](#proveedor) (`id`)
- `usuario_id` → [`usuario`](#usuario) (`id`)

**Reglas (CHECK):**

- `ck_recepcion_total`: `(`total_factura` >= 0)`

### detalle_recepcion

Productos de cada entrada de mercancía.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador de la línea |
| `recepcion_id` | BIGINT UNSIGNED | Sí | FK |  | Entrada de mercancía a la que pertenece esta línea |
| `detalle_orden_id` | BIGINT UNSIGNED | No | FK |  | Línea de la orden de compra que se está recibiendo, si la hay |
| `lote_id` | BIGINT UNSIGNED | Sí | FK |  | Lote en el que entra la mercancía |
| `cantidad_recibida` | INT UNSIGNED | Sí |  |  | Unidades recibidas |
| `costo_unitario` | DECIMAL(12,2) | Sí |  |  | Costo en la factura por unidad |

**Se relaciona con:**

- `lote_id` → [`lote`](#lote) (`id`)
- `detalle_orden_id` → [`detalle_orden_compra`](#detalle_orden_compra) (`id`)
- `recepcion_id` → [`recepcion_mercancia`](#recepcion_mercancia) (`id`)

**Reglas (CHECK):**

- `ck_detrec_cantidad`: `(`cantidad_recibida` > 0)`
- `ck_detrec_costo`: `(`costo_unitario` >= 0)`

### devolucion_proveedor

Devoluciones a proveedores.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador de la devolución |
| `proveedor_id` | BIGINT UNSIGNED | Sí | FK |  | Proveedor al que se devuelve la mercancía |
| `usuario_id` | BIGINT UNSIGNED | Sí | FK |  | Empleado que la registró |
| `motivo_devolucion` | VARCHAR(255) | Sí |  |  | Por qué se devuelve, por ejemplo próximo a vencer |
| `fecha_devolucion` | DATETIME | Sí |  | fecha actual | Fecha y hora en que ocurrió |

**Se relaciona con:**

- `proveedor_id` → [`proveedor`](#proveedor) (`id`)
- `usuario_id` → [`usuario`](#usuario) (`id`)

### detalle_devolucion_proveedor

Productos de cada devolución a proveedor.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador de la línea |
| `devolucion_proveedor_id` | BIGINT UNSIGNED | Sí | FK |  | Devolución a la que pertenece esta línea |
| `lote_id` | BIGINT UNSIGNED | Sí | FK |  | Lote de donde sale lo devuelto |
| `cantidad_devuelta` | INT UNSIGNED | Sí |  |  | Unidades devueltas |
| `costo_unitario` | DECIMAL(12,2) | Sí |  |  | Costo por unidad que se reconoce |

**Se relaciona con:**

- `devolucion_proveedor_id` → [`devolucion_proveedor`](#devolucion_proveedor) (`id`)
- `lote_id` → [`lote`](#lote) (`id`)

**Reglas (CHECK):**

- `ck_detdevprov_cantidad`: `(`cantidad_devuelta` > 0)`
- `ck_detdevprov_costo`: `(`costo_unitario` >= 0)`

## Caja

Turnos de caja, arqueos e ingresos y egresos de dinero.

### sesion_caja

Turnos de caja: apertura y cierre.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador del turno de caja |
| `abierta_por_id` | BIGINT UNSIGNED | Sí | FK |  | Empleado que abrió |
| `cerrada_por_id` | BIGINT UNSIGNED | No | FK |  | Empleado que cerró |
| `monto_inicial` | DECIMAL(12,2) | Sí |  |  | Efectivo con el que se abrió |
| `monto_esperado` | DECIMAL(12,2) | No |  |  | Efectivo que debía haber al cerrar |
| `monto_contado` | DECIMAL(12,2) | No |  |  | Efectivo contado al cerrar |
| `estado_sesion_caja` | ENUM: abierta, cerrada | Sí |  | abierta | Si el turno de caja sigue abierto o ya se cerró |
| `fecha_apertura` | DATETIME | Sí |  | fecha actual | Fecha y hora en que se abrió la caja |
| `fecha_cierre` | DATETIME | No |  |  | Fecha y hora en que se cerró la caja |

**Se relaciona con:**

- `abierta_por_id` → [`usuario`](#usuario) (`id`)
- `cerrada_por_id` → [`usuario`](#usuario) (`id`)

**Reglas (CHECK):**

- `ck_sesion_cierre`: `((`estado_sesion_caja` = _utf8mb4'abierta') or ((`cerrada_por_id` is not null) and (`monto_esperado` is not null) and (`monto_contado` is not null) and (`fecha_cierre` is not null)))`
- `ck_sesion_monto`: `(`monto_inicial` >= 0)`

### arqueo_caja

Conteos de efectivo durante un turno.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador del arqueo |
| `sesion_caja_id` | BIGINT UNSIGNED | Sí | FK |  | Turno de caja que se contó |
| `usuario_id` | BIGINT UNSIGNED | Sí | FK |  | Empleado que contó el efectivo |
| `monto_esperado` | DECIMAL(12,2) | Sí |  |  | Efectivo que debía haber en caja |
| `monto_contado` | DECIMAL(12,2) | Sí |  |  | Efectivo que se encontró al contar |
| `observacion_arqueo` | VARCHAR(255) | No |  |  | Explicación de una diferencia |
| `fecha_arqueo` | DATETIME | Sí |  | fecha actual | Fecha y hora del conteo |

**Se relaciona con:**

- `sesion_caja_id` → [`sesion_caja`](#sesion_caja) (`id`)
- `usuario_id` → [`usuario`](#usuario) (`id`)

### movimiento_caja

Ingresos y egresos de dinero de cada turno.

| Columna | Tipo | Obligatoria | Clave | Por defecto | Descripción |
|---|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | Sí | PK | autonumérico | Identificador del movimiento |
| `sesion_caja_id` | BIGINT UNSIGNED | Sí | FK |  | Turno de caja en que ocurrió |
| `usuario_id` | BIGINT UNSIGNED | Sí | FK |  | Empleado que lo registró |
| `metodo_pago_id` | INT UNSIGNED | Sí | FK |  | Medio por el que entró o salió el dinero |
| `tipo_movimiento_caja` | ENUM: ingreso, egreso | Sí |  |  | Si entra o sale dinero |
| `categoria_movimiento_caja` | ENUM: venta, suscripcion, devolucion, gasto, otro | Sí |  |  | Motivo por el que entró o salió el dinero |
| `monto_movimiento` | DECIMAL(12,2) | Sí |  |  | Valor del movimiento de dinero |
| `descripcion_movimiento` | VARCHAR(255) | No |  |  | Explicación del movimiento de dinero |
| `pedido_id` | BIGINT UNSIGNED | No | FK |  | Pedido que originó el movimiento, si aplica |
| `suscripcion_id` | BIGINT UNSIGNED | No | FK |  | Suscripción que originó el movimiento, si aplica |
| `fecha_movimiento` | DATETIME | Sí |  | fecha actual | Fecha y hora en que ocurrió |

**Se relaciona con:**

- `metodo_pago_id` → [`metodo_pago`](#metodo_pago) (`id`)
- `pedido_id` → [`pedido`](#pedido) (`id`)
- `sesion_caja_id` → [`sesion_caja`](#sesion_caja) (`id`)
- `suscripcion_id` → [`suscripcion`](#suscripcion) (`id`)
- `usuario_id` → [`usuario`](#usuario) (`id`)

**Reglas (CHECK):**

- `ck_movcaja_monto`: `(`monto_movimiento` > 0)`

## Campos por revisar

Su significado no se pudo confirmar con el código del proyecto. Debe aclararlo el negocio o el equipo.

| Tabla | Campo | Qué falta aclarar |
|---|---|---|
| `cliente` | `version_politica` | la página todavía no numera su política |
| `plan_suscripcion` | `domicilio_prioritario` | la página no define en qué consiste la prioridad |
| `plan_suscripcion` | `acceso_exclusivo` | la página dice unas veces ofertas y otras lanzamientos |
| `producto` | `imagen_catalogo` | confirmar cómo la usa la tienda frente a la foto |

## Lógica dentro de la base de datos

### Disparadores

| Disparador | Tabla | Cuándo | Qué hace |
|---|---|---|---|
| `trg_auditoria_no_actualizar` | `auditoria` | Antes de actualizar | Impide modificar la auditoría. |
| `trg_auditoria_no_borrar` | `auditoria` | Antes de borrar | Impide borrar la auditoría. |
| `trg_categoria_actualizar` | `categoria` | Antes de actualizar | Hace la misma validación e impide ciclos en la jerarquía. |
| `trg_categoria_insertar` | `categoria` | Antes de insertar | Exige que la subcategoría sea del mismo tipo que su categoría superior. |
| `trg_lote_actualizar` | `lote` | Antes de actualizar | Impide cambiar la existencia sin registrar un movimiento. |
| `trg_lote_insertar` | `lote` | Antes de insertar | Hace que todo lote nazca con existencia cero. |
| `trg_movinv_alertas` | `movimiento_inventario` | Después de insertar | Abre o cierra las alertas de agotado y de inventario bajo. |
| `trg_movinv_insertar` | `movimiento_inventario` | Antes de insertar | Rechaza vender lotes vencidos o sin existencia, actualiza el lote y guarda el saldo. |
| `trg_movinv_no_actualizar` | `movimiento_inventario` | Antes de actualizar | Impide modificar el historial de inventario. |
| `trg_movinv_no_borrar` | `movimiento_inventario` | Antes de borrar | Impide borrar el historial de inventario. |

### Procedimientos

| Procedimiento | Qué hace |
|---|---|
| `descontar_stock_fefo` | Descuenta una línea vendida, empezando por el lote que vence antes. Si no alcanza, rechaza la venta completa. |
| `anular_pedido` | Devuelve las unidades a sus lotes, reembolsa los pagos y marca el pedido como cancelado. |
| `aprobar_solicitud` | Verifica el precio contra el plan, aprueba y activa la suscripción. Al renovar conserva los días que faltaban. |
| `anonimizar_cliente` | Borra los datos personales de una cuenta y conserva sus ventas sin ellos. |
| `tareas_diarias` | Marca las suscripciones vencidas y genera alertas de lotes próximos a vencer. La ejecuta el evento `evt_tareas_diarias`. |

### Vistas

| Vista | Para qué sirve |
|---|---|
| `vista_stock` | Existencias en tiempo real por producto y categoría (RF007, RF011). |
| `vista_proximos_vencer` | Lotes con existencia y días que les faltan para vencer (RF005). |
| `vista_historial_inventario` | Entradas y salidas de cada producto (RF006). |
| `vista_ventas` | Ventas por fecha, producto y vendedor (RF012, RF013). |
