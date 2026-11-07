-- =====================================================================
-- Droguería y Variedades Los Tres Hermanos
-- Estructura de la base de datos (MySQL 9.7 LTS)
--
-- Se ejecuta sobre una base vacía:
--   mysql --default-character-set=utf8mb4 drogueria < 01-esquema.sql
--
-- Contiene tablas, restricciones, disparadores, procedimientos y vistas.
-- No contiene datos: esos están en 02-datos-iniciales.sql.
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- Accesos y auditoría
-- ---------------------------------------------------------------------

CREATE TABLE rol (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del rol',
  codigo_rol      VARCHAR(40)  NOT NULL COMMENT 'Código interno del rol, por ejemplo cajero',
  nombre_rol      VARCHAR(80)  NOT NULL COMMENT 'Nombre visible del rol',
  descripcion_rol VARCHAR(255) NULL COMMENT 'Responsabilidades del rol',
  rol_protegido   BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'Si el rol está protegido contra cambios',
  PRIMARY KEY (id),
  UNIQUE KEY uk_rol_codigo (codigo_rol)
) COMMENT='Roles del equipo de trabajo (RF015)';

CREATE TABLE permiso (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del permiso',
  codigo_permiso      VARCHAR(50)  NOT NULL COMMENT 'Código que usa la aplicación, por ejemplo products.view',
  modulo_permiso      VARCHAR(40)  NOT NULL COMMENT 'Módulo al que pertenece el permiso',
  descripcion_permiso VARCHAR(255) NOT NULL COMMENT 'Qué permite hacer',
  permiso_operativo   BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'Si ya existe en la aplicación una función real detrás del permiso',
  PRIMARY KEY (id),
  UNIQUE KEY uk_permiso_codigo (codigo_permiso)
) COMMENT='Acciones que se pueden autorizar en el panel';

CREATE TABLE rol_permiso (
  rol_id     INT UNSIGNED NOT NULL COMMENT 'Rol que recibe el permiso',
  permiso_id INT UNSIGNED NOT NULL COMMENT 'Permiso que se le otorga al rol',
  PRIMARY KEY (rol_id, permiso_id),
  CONSTRAINT fk_rol_permiso_rol FOREIGN KEY (rol_id) REFERENCES rol (id) ON DELETE CASCADE,
  CONSTRAINT fk_rol_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES permiso (id) ON DELETE CASCADE
) COMMENT='Permisos que tiene cada rol';

CREATE TABLE usuario (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del empleado',
  rol_id         INT UNSIGNED    NOT NULL COMMENT 'Rol que tiene asignado el empleado',
  nombre_usuario         VARCHAR(120)    NOT NULL COMMENT 'Nombre completo del empleado',
  correo_usuario         VARCHAR(120)    NOT NULL COMMENT 'Correo con el que inicia sesión',
  clave_hash     VARCHAR(255)    NOT NULL COMMENT 'Contraseña cifrada; nunca se guarda en texto plano',
  usuario_activo         BOOLEAN         NOT NULL DEFAULT TRUE COMMENT 'Si puede iniciar sesión',
  usuario_protegido      BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'Si es una cuenta protegida contra cambios, como la principal',
  fecha_creacion DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Cuándo se creó la cuenta',
  PRIMARY KEY (id),
  UNIQUE KEY uk_usuario_correo (correo_usuario),
  CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES rol (id)
) COMMENT='Cuentas de los empleados de la droguería';

CREATE TABLE usuario_permiso (
  usuario_id BIGINT UNSIGNED NOT NULL COMMENT 'Empleado al que aplica la excepción',
  permiso_id INT UNSIGNED    NOT NULL COMMENT 'Permiso que se agrega o se quita',
  tipo_excepcion       ENUM('concedido','revocado') NOT NULL COMMENT 'Si se agrega o se quita respecto a lo que da el rol',
  PRIMARY KEY (usuario_id, permiso_id),
  CONSTRAINT fk_usuario_permiso_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT fk_usuario_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES permiso (id) ON DELETE CASCADE
) COMMENT='Excepciones de permisos por empleado';

CREATE TABLE auditoria (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del evento',
  usuario_id BIGINT UNSIGNED NULL COMMENT 'Empleado que actuó, si se identificó',
  actor_auditoria      VARCHAR(120)    NOT NULL COMMENT 'Correo o nombre con el que se registró la acción',
  accion_auditoria     VARCHAR(120)    NOT NULL COMMENT 'Acción realizada, por ejemplo Producto modificado',
  detalle_auditoria    TEXT            NULL COMMENT 'Información adicional sobre la acción',
  fecha_auditoria      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora en que ocurrió',
  PRIMARY KEY (id),
  KEY idx_auditoria_fecha (fecha_auditoria),
  CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) COMMENT='Historial de acciones sensibles; no se puede modificar ni borrar';

CREATE TABLE configuracion (
  clave_configuracion               VARCHAR(60)  NOT NULL COMMENT 'Nombre del ajuste, por ejemplo costo_domicilio',
  valor_configuracion               VARCHAR(255) NOT NULL COMMENT 'Valor que tiene el ajuste',
  descripcion_configuracion         VARCHAR(255) NULL COMMENT 'Para qué sirve el ajuste',
  actualizado_por_id  BIGINT UNSIGNED NULL COMMENT 'Último empleado que lo cambió',
  fecha_actualizacion DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha y hora del último cambio',
  PRIMARY KEY (clave_configuracion),
  CONSTRAINT fk_configuracion_usuario FOREIGN KEY (actualizado_por_id) REFERENCES usuario (id)
) COMMENT='Ajustes generales del sistema';

-- ---------------------------------------------------------------------
-- Clientes
-- ---------------------------------------------------------------------

CREATE TABLE cliente (
  id                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del cliente',
  nombre_cliente                   VARCHAR(120) NOT NULL COMMENT 'Nombre completo del cliente',
  correo_cliente                   VARCHAR(120) NULL COMMENT 'Correo de acceso; se borra al eliminar la cuenta',
  telefono_cliente                 VARCHAR(20)  NULL COMMENT 'Teléfono de contacto; se borra al eliminar la cuenta',
  direccion_cliente                VARCHAR(255) NULL COMMENT 'Dirección de entrega; se borra al eliminar la cuenta',
  clave_hash               VARCHAR(255) NULL COMMENT 'Contraseña cifrada; se borra al eliminar la cuenta',
  foto_cliente                     VARCHAR(255) NULL COMMENT 'Ubicación de la foto de perfil',
  cuenta_activa                   BOOLEAN      NOT NULL DEFAULT TRUE COMMENT 'Falso cuando la cuenta fue eliminada',
  creado_por_id            BIGINT UNSIGNED NULL COMMENT 'Empleado que creó la cuenta, si no se registró el cliente',
  fecha_autorizacion_datos DATETIME     NOT NULL COMMENT 'Cuándo autorizó el tratamiento de sus datos (Ley 1581 de 2012)',
  version_politica         VARCHAR(20)  NOT NULL COMMENT 'Versión de la política que aceptó. POR REVISAR: la página todavía no numera su política',
  fecha_registro           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Cuándo se creó la cuenta',
  fecha_eliminacion        DATETIME     NULL COMMENT 'Cuándo se eliminó la cuenta',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cliente_correo (correo_cliente),
  CONSTRAINT fk_cliente_usuario FOREIGN KEY (creado_por_id) REFERENCES usuario (id),
  CONSTRAINT ck_cliente_activo CHECK (
    cuenta_activa = FALSE OR (correo_cliente IS NOT NULL AND telefono_cliente IS NOT NULL AND direccion_cliente IS NOT NULL AND clave_hash IS NOT NULL)
  ),
  CONSTRAINT ck_cliente_eliminado CHECK (cuenta_activa = TRUE OR fecha_eliminacion IS NOT NULL)
) COMMENT='Clientes con cuenta en la tienda';

CREATE TABLE recuperacion_clave (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la solicitud',
  cliente_id       BIGINT UNSIGNED NOT NULL COMMENT 'Cliente que pidió recuperar la contraseña',
  token_hash       VARCHAR(255)    NOT NULL COMMENT 'Código de recuperación cifrado',
  fecha_expiracion DATETIME        NOT NULL COMMENT 'Hasta cuándo sirve el código',
  fecha_uso        DATETIME        NULL COMMENT 'Cuándo se usó; un código solo sirve una vez',
  fecha_creacion   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Cuándo se solicitó',
  PRIMARY KEY (id),
  UNIQUE KEY uk_recuperacion_token (token_hash),
  CONSTRAINT fk_recuperacion_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id) ON DELETE CASCADE
) COMMENT='Códigos temporales para recuperar la contraseña';

-- ---------------------------------------------------------------------
-- Catálogo
-- ---------------------------------------------------------------------

CREATE TABLE categoria (
  id                 INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la categoría',
  codigo_categoria             VARCHAR(60)  NOT NULL COMMENT 'Código interno, por ejemplo analgesicos',
  nombre_categoria             VARCHAR(80)  NOT NULL COMMENT 'Nombre que se muestra en la página',
  tipo_categoria               ENUM('medicamento','variedad') NOT NULL COMMENT 'Tipo de productos que agrupa',
  categoria_padre_id INT UNSIGNED NULL COMMENT 'Categoría superior; vacía si es principal (RF019)',
  PRIMARY KEY (id),
  UNIQUE KEY uk_categoria_codigo (codigo_categoria),
  CONSTRAINT fk_categoria_padre FOREIGN KEY (categoria_padre_id) REFERENCES categoria (id)
) COMMENT='Categorías y subcategorías de productos';

CREATE TABLE producto (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del producto',
  categoria_id     INT UNSIGNED    NOT NULL COMMENT 'Categoría del producto; de ella se obtiene su tipo',
  nombre_producto           VARCHAR(150)    NOT NULL COMMENT 'Nombre comercial del producto',
  descripcion_producto      TEXT            NULL COMMENT 'Descripción para la tienda',
  principio_activo VARCHAR(150)    NULL COMMENT 'Principio activo, en medicamentos',
  contenido_producto        VARCHAR(100)    NULL COMMENT 'Contenido o tamaño, por ejemplo 100 ml',
  requiere_formula BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'Si se vende solo con fórmula médica',
  producto_activo           BOOLEAN         NOT NULL DEFAULT TRUE COMMENT 'Si aparece en la tienda',
  foto_producto             VARCHAR(255)    NULL COMMENT 'Ubicación de la foto subida desde el panel',
  imagen_catalogo           VARCHAR(255)    NULL COMMENT 'Imagen de referencia (parecida) que la tienda muestra con un aviso mientras no haya foto exacta',
  PRIMARY KEY (id),
  KEY idx_producto_nombre (nombre_producto),
  CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id)
) COMMENT='Medicamentos y variedades (RF001, RF003)';

CREATE TABLE presentacion (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de lo que se vende',
  producto_id        BIGINT UNSIGNED NOT NULL COMMENT 'Producto al que pertenece',
  tipo_presentacion               VARCHAR(60)     NOT NULL COMMENT 'Forma de venta, por ejemplo Tableta o Unidad',
  concentracion      VARCHAR(60)     NULL COMMENT 'Concentración, por ejemplo 500 mg',
  marca              VARCHAR(80)     NULL COMMENT 'Marca o laboratorio',
  codigo_barras      VARCHAR(40)     NULL COMMENT 'Código de barras para el lector',
  registro_sanitario VARCHAR(40)     NULL COMMENT 'Registro sanitario, cuando aplica',
  precio_venta             DECIMAL(12,2)   NOT NULL COMMENT 'Precio de venta vigente en pesos',
  stock_minimo       INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT 'Por debajo de este valor se genera una alerta (RF004)',
  stock_maximo       INT UNSIGNED    NULL COMMENT 'Existencia máxima recomendada',
  presentacion_activa             BOOLEAN         NOT NULL DEFAULT TRUE COMMENT 'Si se puede vender',
  PRIMARY KEY (id),
  UNIQUE KEY uk_presentacion_codigo_barras (codigo_barras),
  CONSTRAINT fk_presentacion_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
  CONSTRAINT ck_presentacion_precio CHECK (precio_venta >= 0),
  CONSTRAINT ck_presentacion_stock CHECK (stock_maximo IS NULL OR stock_maximo >= stock_minimo)
) COMMENT='Unidad que se vende y se almacena; las variedades tienen una sola';

CREATE TABLE favorito (
  cliente_id  BIGINT UNSIGNED NOT NULL COMMENT 'Cliente que lo guardó',
  producto_id BIGINT UNSIGNED NOT NULL COMMENT 'Producto guardado',
  fecha_favorito       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Cuándo se guardó',
  PRIMARY KEY (cliente_id, producto_id),
  CONSTRAINT fk_favorito_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id) ON DELETE CASCADE,
  CONSTRAINT fk_favorito_producto FOREIGN KEY (producto_id) REFERENCES producto (id) ON DELETE CASCADE
) COMMENT='Productos favoritos de cada cliente';

-- ---------------------------------------------------------------------
-- Suscripciones
-- ---------------------------------------------------------------------

CREATE TABLE metodo_pago (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del método',
  codigo_metodo_pago        VARCHAR(30)  NOT NULL COMMENT 'Código interno del método, por ejemplo nequi',
  nombre_metodo_pago        VARCHAR(60)  NOT NULL COMMENT 'Nombre que se muestra en la página',
  numero_cuenta VARCHAR(60)  NULL COMMENT 'Número de cuenta donde se recibe el pago',
  titular_cuenta       VARCHAR(120) NULL COMMENT 'Titular de la cuenta donde se recibe el pago',
  metodo_pago_activo        BOOLEAN      NOT NULL DEFAULT TRUE COMMENT 'Si está disponible para pagar',
  PRIMARY KEY (id),
  UNIQUE KEY uk_metodo_pago_codigo (codigo_metodo_pago)
) COMMENT='Formas de pago aceptadas';

CREATE TABLE plan_suscripcion (
  id                    INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del plan',
  nombre_plan                VARCHAR(40)  NOT NULL COMMENT 'Nombre del plan',
  descripcion_plan           VARCHAR(255) NULL COMMENT 'Qué ofrece el plan',
  precio_mensual        DECIMAL(12,2) NOT NULL COMMENT 'Precio vigente en pesos',
  descuento_porcentaje  TINYINT UNSIGNED NOT NULL COMMENT 'Descuento en la tienda durante la vigencia',
  duracion_dias         SMALLINT UNSIGNED NOT NULL DEFAULT 30 COMMENT 'Días de vigencia',
  domicilio_prioritario BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'Si incluye domicilio prioritario. POR REVISAR: la página no define en qué consiste la prioridad',
  domicilio_gratis      BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'Si el cliente con este plan no paga domicilio',
  ofertas_anticipadas   BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'Incluye ofertas anticipadas',
  soporte_whatsapp      BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'Incluye soporte por WhatsApp',
  acceso_exclusivo      BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'Si incluye acceso exclusivo. POR REVISAR: la página dice unas veces ofertas y otras lanzamientos',
  plan_activo                BOOLEAN      NOT NULL DEFAULT TRUE COMMENT 'Si se puede comprar',
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_nombre (nombre_plan),
  CONSTRAINT ck_plan_precio CHECK (precio_mensual >= 0),
  CONSTRAINT ck_plan_descuento CHECK (descuento_porcentaje <= 100),
  CONSTRAINT ck_plan_duracion CHECK (duracion_dias > 0)
) COMMENT='Planes de suscripción disponibles';

CREATE TABLE solicitud_suscripcion (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la solicitud',
  cliente_id        BIGINT UNSIGNED NOT NULL COMMENT 'Cliente que compra el plan',
  plan_id           INT UNSIGNED    NOT NULL COMMENT 'Plan que el cliente quiere comprar',
  metodo_pago_id    INT UNSIGNED    NOT NULL COMMENT 'Forma de pago usada',
  origen_solicitud            ENUM('web','tienda') NOT NULL COMMENT 'Si se compró en línea o en el local',
  monto_pagado             DECIMAL(12,2)   NOT NULL COMMENT 'Valor que se pagó',
  referencia_pago        VARCHAR(60)     NULL COMMENT 'Número de referencia de la transacción',
  numero_aprobacion VARCHAR(60)     NULL COMMENT 'Número de aprobación del banco',
  titular_pago           VARCHAR(120)    NULL COMMENT 'Titular de la cuenta que pagó',
  fecha_pago        DATE            NULL COMMENT 'Día en que se hizo el pago',
  comprobante_pago       VARCHAR(255)    NULL COMMENT 'Ubicación de la imagen del comprobante de pago',
  nota_solicitud              VARCHAR(255)    NULL COMMENT 'Observación del cliente o del empleado',
  estado_solicitud            ENUM('pendiente','aprobado','rechazado') NOT NULL DEFAULT 'pendiente' COMMENT 'Resultado de la revisión',
  motivo_rechazo    VARCHAR(255)    NULL COMMENT 'Por qué se rechazó',
  revisado_por_id   BIGINT UNSIGNED NULL COMMENT 'Empleado que revisó',
  fecha_revision    DATETIME        NULL COMMENT 'Cuándo se revisó',
  fecha_solicitud   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Cuándo se envió',
  PRIMARY KEY (id),
  UNIQUE KEY uk_solicitud_cliente_plan (id, cliente_id, plan_id),
  KEY idx_solicitud_estado (estado_solicitud),
  CONSTRAINT fk_solicitud_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
  CONSTRAINT fk_solicitud_plan FOREIGN KEY (plan_id) REFERENCES plan_suscripcion (id),
  CONSTRAINT fk_solicitud_metodo FOREIGN KEY (metodo_pago_id) REFERENCES metodo_pago (id),
  CONSTRAINT fk_solicitud_usuario FOREIGN KEY (revisado_por_id) REFERENCES usuario (id),
  CONSTRAINT ck_solicitud_monto CHECK (monto_pagado >= 0),
  CONSTRAINT ck_solicitud_revision CHECK (estado_solicitud = 'pendiente' OR (revisado_por_id IS NOT NULL AND fecha_revision IS NOT NULL))
) COMMENT='Compras de un plan, en línea o en el local, con su revisión';

CREATE TABLE suscripcion (
  id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la suscripción',
  cliente_id           BIGINT UNSIGNED NOT NULL COMMENT 'Cliente titular de la suscripción',
  plan_id              INT UNSIGNED    NOT NULL COMMENT 'Plan que compró el cliente',
  solicitud_id         BIGINT UNSIGNED NOT NULL COMMENT 'Solicitud aprobada que la originó',
  descuento_porcentaje TINYINT UNSIGNED NOT NULL COMMENT 'Descuento acordado al comprar, aunque el plan cambie después',
  fecha_inicio         DATETIME        NOT NULL COMMENT 'Inicio de la vigencia',
  fecha_vencimiento    DATETIME        NOT NULL COMMENT 'Fin de la vigencia',
  estado_suscripcion               ENUM('activa','vencida','cancelada','reemplazada') NOT NULL DEFAULT 'activa' COMMENT 'Situación del plan; reemplazada significa que se renovó',
  fecha_cancelacion    DATETIME        NULL COMMENT 'Cuándo se canceló',
  cliente_con_plan_activo       BIGINT UNSIGNED GENERATED ALWAYS AS (IF(estado_suscripcion = 'activa', cliente_id, NULL)) VIRTUAL COMMENT 'Se calcula solo: impide dos planes activos para un cliente',
  PRIMARY KEY (id),
  UNIQUE KEY uk_suscripcion_solicitud (solicitud_id),
  UNIQUE KEY uk_suscripcion_cliente_activo (cliente_con_plan_activo),
  UNIQUE KEY uk_suscripcion_id_cliente (id, cliente_id),
  CONSTRAINT fk_suscripcion_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
  CONSTRAINT fk_suscripcion_plan FOREIGN KEY (plan_id) REFERENCES plan_suscripcion (id),
  -- Garantiza que cliente y plan sean los mismos de la solicitud aprobada
  CONSTRAINT fk_suscripcion_solicitud FOREIGN KEY (solicitud_id, cliente_id, plan_id)
    REFERENCES solicitud_suscripcion (id, cliente_id, plan_id),
  CONSTRAINT ck_suscripcion_descuento CHECK (descuento_porcentaje <= 100),
  CONSTRAINT ck_suscripcion_fechas CHECK (fecha_vencimiento > fecha_inicio),
  CONSTRAINT ck_suscripcion_cancelada CHECK (estado_suscripcion <> 'cancelada' OR fecha_cancelacion IS NOT NULL)
) COMMENT='Planes contratados por los clientes y su vigencia';

-- ---------------------------------------------------------------------
-- Caja
-- ---------------------------------------------------------------------

CREATE TABLE sesion_caja (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del turno de caja',
  abierta_por_id BIGINT UNSIGNED NOT NULL COMMENT 'Empleado que abrió',
  cerrada_por_id BIGINT UNSIGNED NULL COMMENT 'Empleado que cerró',
  monto_inicial  DECIMAL(12,2)   NOT NULL COMMENT 'Efectivo con el que se abrió',
  monto_esperado DECIMAL(12,2)   NULL COMMENT 'Efectivo que debía haber al cerrar',
  monto_contado  DECIMAL(12,2)   NULL COMMENT 'Efectivo contado al cerrar',
  estado_sesion_caja         ENUM('abierta','cerrada') NOT NULL DEFAULT 'abierta' COMMENT 'Si el turno de caja sigue abierto o ya se cerró',
  fecha_apertura DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora en que se abrió la caja',
  fecha_cierre   DATETIME        NULL COMMENT 'Fecha y hora en que se cerró la caja',
  PRIMARY KEY (id),
  CONSTRAINT fk_sesion_abierta FOREIGN KEY (abierta_por_id) REFERENCES usuario (id),
  CONSTRAINT fk_sesion_cerrada FOREIGN KEY (cerrada_por_id) REFERENCES usuario (id),
  CONSTRAINT ck_sesion_monto CHECK (monto_inicial >= 0),
  CONSTRAINT ck_sesion_cierre CHECK (estado_sesion_caja = 'abierta' OR (cerrada_por_id IS NOT NULL AND monto_esperado IS NOT NULL AND monto_contado IS NOT NULL AND fecha_cierre IS NOT NULL))
) COMMENT='Turnos de caja: apertura y cierre';

CREATE TABLE arqueo_caja (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del arqueo',
  sesion_caja_id BIGINT UNSIGNED NOT NULL COMMENT 'Turno de caja que se contó',
  usuario_id     BIGINT UNSIGNED NOT NULL COMMENT 'Empleado que contó el efectivo',
  monto_esperado DECIMAL(12,2)   NOT NULL COMMENT 'Efectivo que debía haber en caja',
  monto_contado  DECIMAL(12,2)   NOT NULL COMMENT 'Efectivo que se encontró al contar',
  observacion_arqueo    VARCHAR(255)    NULL COMMENT 'Explicación de una diferencia',
  fecha_arqueo          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora del conteo',
  PRIMARY KEY (id),
  CONSTRAINT fk_arqueo_sesion FOREIGN KEY (sesion_caja_id) REFERENCES sesion_caja (id),
  CONSTRAINT fk_arqueo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) COMMENT='Conteos de efectivo durante un turno';

-- ---------------------------------------------------------------------
-- Pedidos y ventas
-- ---------------------------------------------------------------------

CREATE TABLE pedido (
  id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del pedido o venta',
  numero_pedido               VARCHAR(20)     NOT NULL COMMENT 'Número visible, por ejemplo LTH-K8F3A2',
  numero_comprobante          VARCHAR(30)     NULL COMMENT 'Número del comprobante interno (RF010); no es factura electrónica',
  canal_venta                       ENUM('web','pos') NOT NULL COMMENT 'Por dónde se vendió: web es la tienda en línea y pos es el mostrador',
  cliente_id                  BIGINT UNSIGNED NULL COMMENT 'Cliente que compró; vacío en ventas de mostrador sin cuenta',
  suscripcion_id              BIGINT UNSIGNED NULL COMMENT 'Suscripción que dio el descuento',
  vendedor_id                 BIGINT UNSIGNED NULL COMMENT 'Empleado que registró la venta (RF012)',
  sesion_caja_id              BIGINT UNSIGNED NULL COMMENT 'Turno de caja de la venta',
  modalidad_entrega                   ENUM('tienda','domicilio') NOT NULL COMMENT 'Si se recoge en la tienda o se lleva a domicilio',
  direccion_entrega           VARCHAR(255)    NULL COMMENT 'Dirección a la que se entrega este pedido',
  subtotal_pedido                    DECIMAL(12,2)   NOT NULL COMMENT 'Suma de cantidad por precio original',
  descuento_porcentaje        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Porcentaje aplicado (RF014)',
  descuento_aplicado          DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT 'Valor del descuento en pesos',
  descuento_autorizado_por_id BIGINT UNSIGNED NULL COMMENT 'Empleado que autorizó un descuento manual',
  costo_domicilio             DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT 'Valor cobrado por el domicilio',
  distancia_entrega_km        DECIMAL(5,2)    NULL COMMENT 'Distancia en línea recta de la droguería a la ubicación que compartió el cliente; vacía si no la compartió',
  total_pedido                       DECIMAL(12,2)   GENERATED ALWAYS AS (subtotal_pedido - descuento_aplicado + costo_domicilio) STORED NOT NULL COMMENT 'Se calcula solo: subtotal - descuento + domicilio',
  estado_pedido                      ENUM('pendiente','en_proceso','entregado','cancelado') NOT NULL DEFAULT 'pendiente' COMMENT 'En qué punto va el pedido',
  anulado_por_id              BIGINT UNSIGNED NULL COMMENT 'Empleado que anuló',
  motivo_anulacion            VARCHAR(255)    NULL COMMENT 'Por qué se anuló',
  fecha_anulacion             DATETIME        NULL COMMENT 'Cuándo se anuló',
  fecha_creacion              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora en que se registró',
  PRIMARY KEY (id),
  UNIQUE KEY uk_pedido_numero (numero_pedido),
  UNIQUE KEY uk_pedido_comprobante (numero_comprobante),
  KEY idx_pedido_fecha (fecha_creacion),
  KEY idx_pedido_estado (estado_pedido),
  CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
  -- Garantiza que la suscripción sea del mismo cliente del pedido
  CONSTRAINT fk_pedido_suscripcion FOREIGN KEY (suscripcion_id, cliente_id) REFERENCES suscripcion (id, cliente_id),
  CONSTRAINT fk_pedido_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuario (id),
  CONSTRAINT fk_pedido_sesion FOREIGN KEY (sesion_caja_id) REFERENCES sesion_caja (id),
  CONSTRAINT fk_pedido_autoriza FOREIGN KEY (descuento_autorizado_por_id) REFERENCES usuario (id),
  CONSTRAINT fk_pedido_anula FOREIGN KEY (anulado_por_id) REFERENCES usuario (id),
  CONSTRAINT ck_pedido_web CHECK (canal_venta <> 'web' OR cliente_id IS NOT NULL),
  CONSTRAINT ck_pedido_pos CHECK (canal_venta <> 'pos' OR vendedor_id IS NOT NULL),
  CONSTRAINT ck_pedido_domicilio CHECK (modalidad_entrega <> 'domicilio' OR direccion_entrega IS NOT NULL),
  CONSTRAINT ck_pedido_montos CHECK (subtotal_pedido >= 0 AND descuento_aplicado >= 0 AND descuento_aplicado <= subtotal_pedido AND costo_domicilio >= 0),
  CONSTRAINT ck_pedido_porcentaje CHECK (descuento_porcentaje <= 100),
  CONSTRAINT ck_pedido_anulacion CHECK (estado_pedido <> 'cancelado' OR fecha_anulacion IS NOT NULL)
) COMMENT='Pedidos en línea y ventas de mostrador (RF009)';

CREATE TABLE detalle_pedido (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la línea',
  pedido_id       BIGINT UNSIGNED NOT NULL COMMENT 'Pedido al que pertenece',
  presentacion_id BIGINT UNSIGNED NOT NULL COMMENT 'Presentación que se vendió',
  nombre_producto VARCHAR(255)    NOT NULL COMMENT 'Nombre al momento de la venta, para el comprobante',
  cantidad_vendida        INT UNSIGNED    NOT NULL COMMENT 'Unidades vendidas',
  precio_original DECIMAL(12,2)   NOT NULL COMMENT 'Precio de lista al momento de la venta',
  precio_unitario DECIMAL(12,2)   NOT NULL COMMENT 'Precio cobrado, ya con descuento',
  subtotal_linea        DECIMAL(12,2)   GENERATED ALWAYS AS (cantidad_vendida * precio_unitario) STORED NOT NULL COMMENT 'Se calcula solo: cantidad por precio cobrado',
  formula_soporte VARCHAR(255)    NULL COMMENT 'Ubicación del archivo de la fórmula médica, si se exige',
  PRIMARY KEY (id),
  CONSTRAINT fk_detalle_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
  CONSTRAINT fk_detalle_presentacion FOREIGN KEY (presentacion_id) REFERENCES presentacion (id),
  CONSTRAINT ck_detalle_cantidad CHECK (cantidad_vendida > 0),
  CONSTRAINT ck_detalle_precios CHECK (precio_original >= 0 AND precio_unitario >= 0 AND precio_unitario <= precio_original)
) COMMENT='Productos de cada pedido';

CREATE TABLE pago_pedido (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del pago',
  pedido_id      BIGINT UNSIGNED NOT NULL COMMENT 'Pedido al que corresponde el pago',
  metodo_pago_id INT UNSIGNED    NOT NULL COMMENT 'Forma de pago usada',
  monto_pago          DECIMAL(12,2)   NOT NULL COMMENT 'Valor que se pagó',
  referencia_pago     VARCHAR(60)     NULL COMMENT 'Referencia de la transacción',
  estado_pago         ENUM('pendiente','confirmado','rechazado','reembolsado') NOT NULL DEFAULT 'pendiente' COMMENT 'Si el pago está pendiente, confirmado, rechazado o reembolsado',
  fecha_pago          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora en que se registró',
  PRIMARY KEY (id),
  CONSTRAINT fk_pago_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
  CONSTRAINT fk_pago_metodo FOREIGN KEY (metodo_pago_id) REFERENCES metodo_pago (id),
  CONSTRAINT ck_pago_monto CHECK (monto_pago > 0)
) COMMENT='Pagos de cada pedido; un pedido puede tener varios';

CREATE TABLE movimiento_caja (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del movimiento',
  sesion_caja_id BIGINT UNSIGNED NOT NULL COMMENT 'Turno de caja en que ocurrió',
  usuario_id     BIGINT UNSIGNED NOT NULL COMMENT 'Empleado que lo registró',
  metodo_pago_id INT UNSIGNED    NOT NULL COMMENT 'Medio por el que entró o salió el dinero',
  tipo_movimiento_caja           ENUM('ingreso','egreso') NOT NULL COMMENT 'Si entra o sale dinero',
  categoria_movimiento_caja      ENUM('venta','suscripcion','devolucion','gasto','otro') NOT NULL COMMENT 'Motivo por el que entró o salió el dinero',
  monto_movimiento          DECIMAL(12,2)   NOT NULL COMMENT 'Valor del movimiento de dinero',
  descripcion_movimiento    VARCHAR(255)    NULL COMMENT 'Explicación del movimiento de dinero',
  pedido_id      BIGINT UNSIGNED NULL COMMENT 'Pedido que originó el movimiento, si aplica',
  suscripcion_id BIGINT UNSIGNED NULL COMMENT 'Suscripción que originó el movimiento, si aplica',
  fecha_movimiento          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora en que ocurrió',
  PRIMARY KEY (id),
  CONSTRAINT fk_movcaja_sesion FOREIGN KEY (sesion_caja_id) REFERENCES sesion_caja (id),
  CONSTRAINT fk_movcaja_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
  CONSTRAINT fk_movcaja_metodo FOREIGN KEY (metodo_pago_id) REFERENCES metodo_pago (id),
  CONSTRAINT fk_movcaja_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
  CONSTRAINT fk_movcaja_suscripcion FOREIGN KEY (suscripcion_id) REFERENCES suscripcion (id),
  CONSTRAINT ck_movcaja_monto CHECK (monto_movimiento > 0)
) COMMENT='Ingresos y egresos de dinero de cada turno';

-- ---------------------------------------------------------------------
-- Inventario y compras
-- ---------------------------------------------------------------------

CREATE TABLE lote (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del lote',
  presentacion_id   BIGINT UNSIGNED NOT NULL COMMENT 'Presentación que contiene el lote',
  numero_lote       VARCHAR(50)     NOT NULL COMMENT 'Número impreso por el fabricante (RF002)',
  fecha_vencimiento DATE            NULL COMMENT 'Vencimiento; vacío si el producto no vence (RF005)',
  cantidad_actual   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT 'Existencia; solo cambia mediante movimientos',
  costo_unitario    DECIMAL(12,2)   NOT NULL COMMENT 'Costo de compra por unidad',
  fecha_ingreso     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora en que se registró',
  PRIMARY KEY (id),
  UNIQUE KEY uk_lote_presentacion_numero (presentacion_id, numero_lote),
  KEY idx_lote_vencimiento (fecha_vencimiento),
  CONSTRAINT fk_lote_presentacion FOREIGN KEY (presentacion_id) REFERENCES presentacion (id),
  CONSTRAINT ck_lote_costo CHECK (costo_unitario >= 0)
) COMMENT='Lotes con su existencia y vencimiento';

CREATE TABLE devolucion (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la devolución',
  pedido_id  BIGINT UNSIGNED NOT NULL COMMENT 'Pedido al que corresponde la devolución',
  usuario_id BIGINT UNSIGNED NOT NULL COMMENT 'Empleado que la registró',
  motivo_devolucion     VARCHAR(255)    NOT NULL COMMENT 'Por qué el cliente devolvió el producto',
  fecha_devolucion      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora en que ocurrió',
  PRIMARY KEY (id),
  CONSTRAINT fk_devolucion_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
  CONSTRAINT fk_devolucion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) COMMENT='Devoluciones de clientes';

CREATE TABLE detalle_devolucion (
  id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la línea',
  devolucion_id        BIGINT UNSIGNED NOT NULL COMMENT 'Devolución a la que pertenece esta línea',
  detalle_pedido_id    BIGINT UNSIGNED NOT NULL COMMENT 'Producto del pedido que se devuelve',
  lote_id              BIGINT UNSIGNED NULL COMMENT 'Lote al que regresa, si reingresa',
  cantidad_devuelta             INT UNSIGNED    NOT NULL COMMENT 'Unidades devueltas',
  monto_devuelto                DECIMAL(12,2)   NOT NULL COMMENT 'Dinero devuelto al cliente',
  reingresa_inventario BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'Si el producto se puede volver a vender',
  PRIMARY KEY (id),
  CONSTRAINT fk_detdev_devolucion FOREIGN KEY (devolucion_id) REFERENCES devolucion (id),
  CONSTRAINT fk_detdev_detalle FOREIGN KEY (detalle_pedido_id) REFERENCES detalle_pedido (id),
  CONSTRAINT fk_detdev_lote FOREIGN KEY (lote_id) REFERENCES lote (id),
  CONSTRAINT ck_detdev_cantidad CHECK (cantidad_devuelta > 0),
  CONSTRAINT ck_detdev_monto CHECK (monto_devuelto >= 0),
  CONSTRAINT ck_detdev_reingreso CHECK (reingresa_inventario = FALSE OR lote_id IS NOT NULL)
) COMMENT='Productos de cada devolución de cliente';

CREATE TABLE proveedor (
  id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del proveedor',
  nit_proveedor       VARCHAR(20)     NOT NULL COMMENT 'NIT o documento del proveedor',
  nombre_proveedor    VARCHAR(150)    NOT NULL COMMENT 'Nombre o razón social del proveedor',
  contacto_proveedor  VARCHAR(120)    NULL COMMENT 'Persona de contacto en el proveedor',
  telefono_proveedor  VARCHAR(20)     NULL COMMENT 'Teléfono del proveedor',
  correo_proveedor    VARCHAR(120)    NULL COMMENT 'Correo del proveedor',
  direccion_proveedor VARCHAR(255)    NULL COMMENT 'Dirección del proveedor',
  proveedor_activo    BOOLEAN         NOT NULL DEFAULT TRUE COMMENT 'Si se le sigue comprando',
  PRIMARY KEY (id),
  UNIQUE KEY uk_proveedor_nit (nit_proveedor)
) COMMENT='Proveedores de la droguería (RF008)';

CREATE TABLE orden_compra (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la orden',
  numero_orden         VARCHAR(20)     NOT NULL COMMENT 'Número de la orden de compra',
  proveedor_id   BIGINT UNSIGNED NOT NULL COMMENT 'Proveedor al que se le compra',
  usuario_id     BIGINT UNSIGNED NOT NULL COMMENT 'Empleado que la creó',
  estado_orden         ENUM('borrador','enviada','parcial','recibida','cancelada') NOT NULL DEFAULT 'borrador' COMMENT 'En qué punto va la orden de compra',
  fecha_creacion DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Cuándo se creó',
  fecha_esperada DATE            NULL COMMENT 'Cuándo se espera recibir',
  PRIMARY KEY (id),
  UNIQUE KEY uk_orden_numero (numero_orden),
  CONSTRAINT fk_orden_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor (id),
  CONSTRAINT fk_orden_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) COMMENT='Órdenes de compra a proveedores';

CREATE TABLE detalle_orden_compra (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la línea',
  orden_compra_id BIGINT UNSIGNED NOT NULL COMMENT 'Orden de compra a la que pertenece esta línea',
  presentacion_id BIGINT UNSIGNED NOT NULL COMMENT 'Presentación que se pide',
  cantidad_pedida        INT UNSIGNED    NOT NULL COMMENT 'Unidades pedidas',
  costo_unitario  DECIMAL(12,2)   NOT NULL COMMENT 'Costo acordado por unidad',
  PRIMARY KEY (id),
  CONSTRAINT fk_detorden_orden FOREIGN KEY (orden_compra_id) REFERENCES orden_compra (id),
  CONSTRAINT fk_detorden_presentacion FOREIGN KEY (presentacion_id) REFERENCES presentacion (id),
  CONSTRAINT ck_detorden_cantidad CHECK (cantidad_pedida > 0),
  CONSTRAINT ck_detorden_costo CHECK (costo_unitario >= 0)
) COMMENT='Productos de cada orden de compra';

CREATE TABLE recepcion_mercancia (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la entrada',
  proveedor_id    BIGINT UNSIGNED NOT NULL COMMENT 'Quién entregó (RF002)',
  orden_compra_id BIGINT UNSIGNED NULL COMMENT 'Orden relacionada; una entrada puede no tenerla',
  usuario_id      BIGINT UNSIGNED NOT NULL COMMENT 'Empleado que recibió',
  numero_factura  VARCHAR(40)     NOT NULL COMMENT 'Factura del proveedor',
  total_factura   DECIMAL(12,2)   NOT NULL COMMENT 'Valor de esa factura',
  fecha_recepcion           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Cuándo se recibió (RF002)',
  PRIMARY KEY (id),
  KEY idx_recepcion_fecha (fecha_recepcion),
  CONSTRAINT fk_recepcion_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor (id),
  CONSTRAINT fk_recepcion_orden FOREIGN KEY (orden_compra_id) REFERENCES orden_compra (id),
  CONSTRAINT fk_recepcion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
  CONSTRAINT ck_recepcion_total CHECK (total_factura >= 0)
) COMMENT='Entradas de mercancía (RF002)';

CREATE TABLE detalle_recepcion (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la línea',
  recepcion_id     BIGINT UNSIGNED NOT NULL COMMENT 'Entrada de mercancía a la que pertenece esta línea',
  detalle_orden_id BIGINT UNSIGNED NULL COMMENT 'Línea de la orden de compra que se está recibiendo, si la hay',
  lote_id          BIGINT UNSIGNED NOT NULL COMMENT 'Lote en el que entra la mercancía',
  cantidad_recibida         INT UNSIGNED    NOT NULL COMMENT 'Unidades recibidas',
  costo_unitario   DECIMAL(12,2)   NOT NULL COMMENT 'Costo en la factura por unidad',
  PRIMARY KEY (id),
  CONSTRAINT fk_detrec_recepcion FOREIGN KEY (recepcion_id) REFERENCES recepcion_mercancia (id),
  CONSTRAINT fk_detrec_orden FOREIGN KEY (detalle_orden_id) REFERENCES detalle_orden_compra (id),
  CONSTRAINT fk_detrec_lote FOREIGN KEY (lote_id) REFERENCES lote (id),
  CONSTRAINT ck_detrec_cantidad CHECK (cantidad_recibida > 0),
  CONSTRAINT ck_detrec_costo CHECK (costo_unitario >= 0)
) COMMENT='Productos de cada entrada de mercancía';

CREATE TABLE devolucion_proveedor (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la devolución',
  proveedor_id BIGINT UNSIGNED NOT NULL COMMENT 'Proveedor al que se devuelve la mercancía',
  usuario_id   BIGINT UNSIGNED NOT NULL COMMENT 'Empleado que la registró',
  motivo_devolucion       VARCHAR(255)    NOT NULL COMMENT 'Por qué se devuelve, por ejemplo próximo a vencer',
  fecha_devolucion        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora en que ocurrió',
  PRIMARY KEY (id),
  CONSTRAINT fk_devprov_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor (id),
  CONSTRAINT fk_devprov_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) COMMENT='Devoluciones a proveedores';

CREATE TABLE detalle_devolucion_proveedor (
  id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la línea',
  devolucion_proveedor_id BIGINT UNSIGNED NOT NULL COMMENT 'Devolución a la que pertenece esta línea',
  lote_id                 BIGINT UNSIGNED NOT NULL COMMENT 'Lote de donde sale lo devuelto',
  cantidad_devuelta                INT UNSIGNED    NOT NULL COMMENT 'Unidades devueltas',
  costo_unitario          DECIMAL(12,2)   NOT NULL COMMENT 'Costo por unidad que se reconoce',
  PRIMARY KEY (id),
  CONSTRAINT fk_detdevprov_devolucion FOREIGN KEY (devolucion_proveedor_id) REFERENCES devolucion_proveedor (id),
  CONSTRAINT fk_detdevprov_lote FOREIGN KEY (lote_id) REFERENCES lote (id),
  CONSTRAINT ck_detdevprov_cantidad CHECK (cantidad_devuelta > 0),
  CONSTRAINT ck_detdevprov_costo CHECK (costo_unitario >= 0)
) COMMENT='Productos de cada devolución a proveedor';

CREATE TABLE conteo_fisico (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del conteo',
  usuario_id   BIGINT UNSIGNED NOT NULL COMMENT 'Empleado responsable',
  estado_conteo       ENUM('abierto','cerrado','anulado') NOT NULL DEFAULT 'abierto' COMMENT 'Si el conteo sigue abierto, ya se cerró o se anuló',
  observacion_conteo  VARCHAR(255)    NULL COMMENT 'Comentarios sobre el conteo',
  fecha_inicio DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora en que empezó el conteo',
  fecha_cierre DATETIME        NULL COMMENT 'Fecha y hora en que terminó el conteo',
  PRIMARY KEY (id),
  CONSTRAINT fk_conteo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
  CONSTRAINT ck_conteo_cierre CHECK (estado_conteo <> 'cerrado' OR fecha_cierre IS NOT NULL)
) COMMENT='Conteos físicos del inventario';

CREATE TABLE detalle_conteo (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la línea',
  conteo_id        BIGINT UNSIGNED NOT NULL COMMENT 'Conteo al que pertenece esta línea',
  lote_id          BIGINT UNSIGNED NOT NULL COMMENT 'Lote que se contó',
  cantidad_sistema INT UNSIGNED    NOT NULL COMMENT 'Cantidad que indicaba el sistema',
  cantidad_contada INT UNSIGNED    NOT NULL COMMENT 'Cantidad encontrada al contar',
  PRIMARY KEY (id),
  UNIQUE KEY uk_detconteo_conteo_lote (conteo_id, lote_id),
  CONSTRAINT fk_detconteo_conteo FOREIGN KEY (conteo_id) REFERENCES conteo_fisico (id),
  CONSTRAINT fk_detconteo_lote FOREIGN KEY (lote_id) REFERENCES lote (id)
) COMMENT='Resultado del conteo por lote';

CREATE TABLE movimiento_inventario (
  id                              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador del movimiento',
  lote_id                         BIGINT UNSIGNED NOT NULL COMMENT 'Lote cuya existencia cambia',
  usuario_id                      BIGINT UNSIGNED NULL COMMENT 'Empleado; vacío solo en ventas automáticas de la tienda en línea',
  tipo_movimiento                            ENUM('entrada','venta','anulacion','ajuste','devolucion_cliente','devolucion_proveedor','conteo') NOT NULL COMMENT 'Motivo por el que entró o salió el dinero',
  cantidad_movida                        INT             NOT NULL COMMENT 'Positiva si entra y negativa si sale',
  saldo_resultante                INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT 'Existencia del lote después del movimiento; la calcula un disparador',
  motivo_movimiento                          VARCHAR(255)    NULL COMMENT 'Explicación del movimiento; obligatoria en ajustes',
  detalle_pedido_id               BIGINT UNSIGNED NULL COMMENT 'Producto del pedido que se vendió o se anuló',
  detalle_recepcion_id            BIGINT UNSIGNED NULL COMMENT 'Línea de la entrada de mercancía que originó el movimiento',
  detalle_conteo_id               BIGINT UNSIGNED NULL COMMENT 'Línea del conteo que originó el movimiento',
  detalle_devolucion_id           BIGINT UNSIGNED NULL COMMENT 'Línea de la devolución del cliente que originó el movimiento',
  detalle_devolucion_proveedor_id BIGINT UNSIGNED NULL COMMENT 'Línea de la devolución al proveedor que originó el movimiento',
  fecha_movimiento                           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora en que ocurrió',
  PRIMARY KEY (id),
  KEY idx_movinv_fecha (fecha_movimiento),
  CONSTRAINT fk_movinv_lote FOREIGN KEY (lote_id) REFERENCES lote (id),
  CONSTRAINT fk_movinv_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
  CONSTRAINT fk_movinv_pedido FOREIGN KEY (detalle_pedido_id) REFERENCES detalle_pedido (id),
  CONSTRAINT fk_movinv_recepcion FOREIGN KEY (detalle_recepcion_id) REFERENCES detalle_recepcion (id),
  CONSTRAINT fk_movinv_conteo FOREIGN KEY (detalle_conteo_id) REFERENCES detalle_conteo (id),
  CONSTRAINT fk_movinv_devolucion FOREIGN KEY (detalle_devolucion_id) REFERENCES detalle_devolucion (id),
  CONSTRAINT fk_movinv_devprov FOREIGN KEY (detalle_devolucion_proveedor_id) REFERENCES detalle_devolucion_proveedor (id),
  CONSTRAINT ck_movinv_signo CHECK (
    (tipo_movimiento IN ('entrada','anulacion','devolucion_cliente') AND cantidad_movida > 0)
    OR (tipo_movimiento IN ('venta','devolucion_proveedor') AND cantidad_movida < 0)
    OR (tipo_movimiento IN ('ajuste','conteo') AND cantidad_movida <> 0)
  ),
  CONSTRAINT ck_movinv_usuario CHECK (usuario_id IS NOT NULL OR tipo_movimiento IN ('venta','anulacion')),
  CONSTRAINT ck_movinv_origen CHECK (
    (tipo_movimiento = 'entrada' AND detalle_recepcion_id IS NOT NULL)
    OR (tipo_movimiento IN ('venta','anulacion') AND detalle_pedido_id IS NOT NULL)
    OR (tipo_movimiento = 'ajuste' AND motivo_movimiento IS NOT NULL)
    OR (tipo_movimiento = 'devolucion_cliente' AND detalle_devolucion_id IS NOT NULL)
    OR (tipo_movimiento = 'devolucion_proveedor' AND detalle_devolucion_proveedor_id IS NOT NULL)
    OR (tipo_movimiento = 'conteo' AND detalle_conteo_id IS NOT NULL)
  )
) COMMENT='Historial de entradas y salidas de cada lote (RF006); no se puede modificar ni borrar';

CREATE TABLE notificacion (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la alerta',
  tipo_alerta            ENUM('agotado','proximo_vencer','inventario_bajo','administrativa') NOT NULL COMMENT 'Clase de alerta (RF004, RF005)',
  mensaje_alerta         VARCHAR(255)    NOT NULL COMMENT 'Mensaje que ve el empleado',
  presentacion_id BIGINT UNSIGNED NULL COMMENT 'Presentación a la que se refiere la alerta',
  lote_id         BIGINT UNSIGNED NULL COMMENT 'Lote al que se refiere la alerta',
  alerta_atendida        BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'Si la alerta ya se resolvió',
  atendida_por_id BIGINT UNSIGNED NULL COMMENT 'Empleado que la atendió; vacío si se resolvió sola',
  fecha_atencion  DATETIME        NULL COMMENT 'Fecha y hora en que se atendió la alerta',
  fecha_creacion  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora en que se generó la alerta',
  clave_abierta   VARCHAR(60)     GENERATED ALWAYS AS (
    IF(alerta_atendida = FALSE AND tipo_alerta <> 'administrativa', CONCAT(tipo_alerta, '-', IFNULL(presentacion_id, 0), '-', IFNULL(lote_id, 0)), NULL)
  ) VIRTUAL COMMENT 'Se calcula solo: evita repetir una alerta que sigue abierta',
  PRIMARY KEY (id),
  UNIQUE KEY uk_notificacion_abierta (clave_abierta),
  KEY idx_notificacion_atendida (alerta_atendida),
  CONSTRAINT fk_notificacion_presentacion FOREIGN KEY (presentacion_id) REFERENCES presentacion (id),
  CONSTRAINT fk_notificacion_lote FOREIGN KEY (lote_id) REFERENCES lote (id),
  CONSTRAINT fk_notificacion_usuario FOREIGN KEY (atendida_por_id) REFERENCES usuario (id)
) COMMENT='Alertas de stock, vencimiento y administración';

-- ---------------------------------------------------------------------
-- Disparadores: mantienen coherentes los datos aunque falle la aplicación
-- ---------------------------------------------------------------------

DELIMITER //

-- Una subcategoría debe ser del mismo tipo que su categoría superior
CREATE TRIGGER trg_categoria_insertar BEFORE INSERT ON categoria FOR EACH ROW
BEGIN
  DECLARE v_tipo VARCHAR(20);
  IF NEW.categoria_padre_id IS NOT NULL THEN
    SELECT tipo_categoria INTO v_tipo FROM categoria WHERE id = NEW.categoria_padre_id;
    IF v_tipo <> NEW.tipo_categoria THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La subcategoría debe ser del mismo tipo que su categoría superior';
    END IF;
  END IF;
END//

CREATE TRIGGER trg_categoria_actualizar BEFORE UPDATE ON categoria FOR EACH ROW
BEGIN
  DECLARE v_tipo VARCHAR(20);
  DECLARE v_actual INT UNSIGNED DEFAULT NEW.categoria_padre_id;
  DECLARE v_pasos INT DEFAULT 0;
  IF NEW.categoria_padre_id IS NOT NULL THEN
    SELECT tipo_categoria INTO v_tipo FROM categoria WHERE id = NEW.categoria_padre_id;
    IF v_tipo <> NEW.tipo_categoria THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La subcategoría debe ser del mismo tipo que su categoría superior';
    END IF;
    -- Recorre hacia arriba para impedir ciclos en la jerarquía
    WHILE v_actual IS NOT NULL AND v_pasos < 100 DO
      IF v_actual = NEW.id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una categoría no puede quedar dentro de sí misma';
      END IF;
      SET v_pasos = v_pasos + 1;
      SELECT categoria_padre_id INTO v_actual FROM categoria WHERE id = v_actual;
    END WHILE;
  END IF;
END//

-- La existencia de un lote nace en cero y solo cambia con movimientos
CREATE TRIGGER trg_lote_insertar BEFORE INSERT ON lote FOR EACH ROW
BEGIN
  SET NEW.cantidad_actual = 0;
END//

CREATE TRIGGER trg_lote_actualizar BEFORE UPDATE ON lote FOR EACH ROW
BEGIN
  IF NEW.cantidad_actual <> OLD.cantidad_actual AND @lth_movimiento IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La existencia solo cambia registrando un movimiento de inventario';
  END IF;
END//

-- Aplica cada movimiento: valida, actualiza el lote y guarda el saldo
CREATE TRIGGER trg_movinv_insertar BEFORE INSERT ON movimiento_inventario FOR EACH ROW
BEGIN
  DECLARE v_actual INT;
  DECLARE v_vence DATE;
  SELECT cantidad_actual, fecha_vencimiento INTO v_actual, v_vence FROM lote WHERE id = NEW.lote_id FOR UPDATE;
  IF NEW.tipo_movimiento = 'venta' AND v_vence IS NOT NULL AND v_vence < DATE(NEW.fecha_movimiento) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se puede vender un lote vencido';
  END IF;
  IF v_actual + NEW.cantidad_movida < 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La existencia del lote no alcanza';
  END IF;
  SET @lth_movimiento = 1;
  UPDATE lote SET cantidad_actual = v_actual + NEW.cantidad_movida WHERE id = NEW.lote_id;
  SET @lth_movimiento = NULL;
  SET NEW.saldo_resultante = v_actual + NEW.cantidad_movida;
END//

-- Abre o cierra las alertas de stock según la existencia resultante (RF004)
CREATE TRIGGER trg_movinv_alertas AFTER INSERT ON movimiento_inventario FOR EACH ROW
BEGIN
  DECLARE v_presentacion BIGINT UNSIGNED;
  DECLARE v_minimo INT UNSIGNED;
  DECLARE v_activa BOOLEAN;
  DECLARE v_nombre VARCHAR(255);
  DECLARE v_stock INT;
  SELECT pr.id, pr.stock_minimo, pr.presentacion_activa, CONCAT(p.nombre_producto, ' ', pr.tipo_presentacion, IFNULL(CONCAT(' ', pr.concentracion), ''))
    INTO v_presentacion, v_minimo, v_activa, v_nombre
    FROM lote l JOIN presentacion pr ON pr.id = l.presentacion_id JOIN producto p ON p.id = pr.producto_id
    WHERE l.id = NEW.lote_id;
  SELECT COALESCE(SUM(cantidad_actual), 0) INTO v_stock FROM lote WHERE presentacion_id = v_presentacion;
  IF v_activa THEN
    IF v_stock = 0 THEN
      INSERT IGNORE INTO notificacion (tipo_alerta, mensaje_alerta, presentacion_id, fecha_creacion)
        VALUES ('agotado', LEFT(CONCAT('Producto agotado: ', v_nombre), 255), v_presentacion, NEW.fecha_movimiento);
    ELSEIF v_stock < v_minimo THEN
      INSERT IGNORE INTO notificacion (tipo_alerta, mensaje_alerta, presentacion_id, fecha_creacion)
        VALUES ('inventario_bajo', LEFT(CONCAT('Quedan ', v_stock, ' unidades (mínimo ', v_minimo, '): ', v_nombre), 255), v_presentacion, NEW.fecha_movimiento);
    END IF;
  END IF;
  -- Resuelve lo que ya no aplica
  UPDATE notificacion SET alerta_atendida = TRUE, fecha_atencion = NEW.fecha_movimiento
    WHERE alerta_atendida = FALSE AND presentacion_id = v_presentacion AND (
      (tipo_alerta = 'agotado' AND v_stock > 0)
      OR (tipo_alerta = 'inventario_bajo' AND (v_stock >= v_minimo OR v_stock = 0))
    );
  UPDATE notificacion SET alerta_atendida = TRUE, fecha_atencion = NEW.fecha_movimiento
    WHERE alerta_atendida = FALSE AND tipo_alerta = 'proximo_vencer' AND lote_id = NEW.lote_id AND NEW.saldo_resultante = 0;
END//

-- El historial de inventario y la auditoría son permanentes
CREATE TRIGGER trg_movinv_no_actualizar BEFORE UPDATE ON movimiento_inventario FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El historial de inventario no se puede modificar; registra un ajuste';
END//

CREATE TRIGGER trg_movinv_no_borrar BEFORE DELETE ON movimiento_inventario FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El historial de inventario no se puede borrar';
END//

CREATE TRIGGER trg_auditoria_no_actualizar BEFORE UPDATE ON auditoria FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La auditoría no se puede modificar';
END//

CREATE TRIGGER trg_auditoria_no_borrar BEFORE DELETE ON auditoria FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La auditoría no se puede borrar';
END//

-- ---------------------------------------------------------------------
-- Procedimientos: operaciones de varios pasos.
-- Se llaman dentro de una transacción: si fallan, se revierte todo.
-- ---------------------------------------------------------------------

-- Descuenta una línea vendida empezando por el lote que vence antes (RF009)
CREATE PROCEDURE descontar_stock_fefo(IN p_detalle BIGINT UNSIGNED, IN p_usuario BIGINT UNSIGNED, IN p_fecha DATETIME)
BEGIN
  DECLARE v_presentacion BIGINT UNSIGNED;
  DECLARE v_pendiente INT;
  DECLARE v_disponible INT;
  DECLARE v_lote BIGINT UNSIGNED;
  DECLARE v_cantidad INT;
  DECLARE v_toma INT;
  DECLARE v_fin BOOLEAN DEFAULT FALSE;
  DECLARE c_lotes CURSOR FOR
    SELECT id, cantidad_actual FROM lote
    WHERE presentacion_id = v_presentacion AND cantidad_actual > 0
      AND (fecha_vencimiento IS NULL OR fecha_vencimiento >= DATE(p_fecha))
    ORDER BY fecha_vencimiento IS NULL, fecha_vencimiento, id
    FOR UPDATE;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_fin = TRUE;

  SELECT presentacion_id, cantidad_vendida INTO v_presentacion, v_pendiente FROM detalle_pedido WHERE id = p_detalle;
  IF v_presentacion IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La línea del pedido no existe';
  END IF;
  SET v_fin = FALSE;

  SELECT COALESCE(SUM(cantidad_actual), 0) INTO v_disponible FROM lote
    WHERE presentacion_id = v_presentacion
      AND (fecha_vencimiento IS NULL OR fecha_vencimiento >= DATE(p_fecha))
    FOR UPDATE;
  IF v_disponible < v_pendiente THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No hay existencia suficiente para esta venta';
  END IF;

  OPEN c_lotes;
  repartir: LOOP
    FETCH c_lotes INTO v_lote, v_cantidad;
    IF v_fin OR v_pendiente = 0 THEN
      LEAVE repartir;
    END IF;
    SET v_toma = LEAST(v_cantidad, v_pendiente);
    INSERT INTO movimiento_inventario (lote_id, usuario_id, tipo_movimiento, cantidad_movida, detalle_pedido_id, fecha_movimiento)
      VALUES (v_lote, p_usuario, 'venta', -v_toma, p_detalle, p_fecha);
    SET v_pendiente = v_pendiente - v_toma;
  END LOOP;
  CLOSE c_lotes;
END//

-- Anula un pedido: devuelve las unidades a sus lotes y reembolsa los pagos
CREATE PROCEDURE anular_pedido(IN p_pedido BIGINT UNSIGNED, IN p_usuario BIGINT UNSIGNED, IN p_motivo VARCHAR(255))
BEGIN
  DECLARE v_estado VARCHAR(20);
  SELECT estado_pedido INTO v_estado FROM pedido WHERE id = p_pedido FOR UPDATE;
  IF v_estado IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El pedido no existe';
  END IF;
  IF v_estado = 'cancelado' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El pedido ya está anulado';
  END IF;
  IF EXISTS (SELECT 1 FROM devolucion WHERE pedido_id = p_pedido) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El pedido tiene devoluciones; no se puede anular';
  END IF;
  INSERT INTO movimiento_inventario (lote_id, usuario_id, tipo_movimiento, cantidad_movida, motivo_movimiento, detalle_pedido_id)
    SELECT m.lote_id, p_usuario, 'anulacion', -m.cantidad_movida, p_motivo, m.detalle_pedido_id
    FROM movimiento_inventario m JOIN detalle_pedido d ON d.id = m.detalle_pedido_id
    WHERE d.pedido_id = p_pedido AND m.tipo_movimiento = 'venta';
  UPDATE pago_pedido SET estado_pago = 'reembolsado' WHERE pedido_id = p_pedido AND estado_pago = 'confirmado';
  UPDATE pedido SET estado_pedido = 'cancelado', anulado_por_id = p_usuario, motivo_anulacion = p_motivo, fecha_anulacion = NOW()
    WHERE id = p_pedido;
END//

-- Aprueba una solicitud y activa el plan. Si el cliente renueva, conserva sus días.
CREATE PROCEDURE aprobar_solicitud(IN p_solicitud BIGINT UNSIGNED, IN p_usuario BIGINT UNSIGNED, OUT p_suscripcion BIGINT UNSIGNED)
BEGIN
  DECLARE v_estado VARCHAR(20);
  DECLARE v_cliente BIGINT UNSIGNED;
  DECLARE v_plan INT UNSIGNED;
  DECLARE v_monto DECIMAL(12,2);
  DECLARE v_precio DECIMAL(12,2);
  DECLARE v_descuento TINYINT UNSIGNED;
  DECLARE v_dias SMALLINT UNSIGNED;
  DECLARE v_activa BIGINT UNSIGNED;
  DECLARE v_vence DATETIME;
  DECLARE v_base DATETIME DEFAULT NOW();

  SELECT estado_solicitud, cliente_id, plan_id, monto_pagado INTO v_estado, v_cliente, v_plan, v_monto
    FROM solicitud_suscripcion WHERE id = p_solicitud FOR UPDATE;
  IF v_estado IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La solicitud no existe';
  END IF;
  IF v_estado <> 'pendiente' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La solicitud ya fue revisada';
  END IF;
  SELECT precio_mensual, descuento_porcentaje, duracion_dias INTO v_precio, v_descuento, v_dias
    FROM plan_suscripcion WHERE id = v_plan;
  IF v_monto <> v_precio THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El valor pagado no coincide con el precio del plan';
  END IF;

  BEGIN
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_activa = NULL;
    SELECT id, fecha_vencimiento INTO v_activa, v_vence FROM suscripcion
      WHERE cliente_id = v_cliente AND estado_suscripcion = 'activa' FOR UPDATE;
  END;
  IF v_activa IS NOT NULL THEN
    UPDATE suscripcion SET estado_suscripcion = 'reemplazada' WHERE id = v_activa;
    IF v_vence > v_base THEN
      SET v_base = v_vence;
    END IF;
  END IF;

  UPDATE solicitud_suscripcion SET estado_solicitud = 'aprobado', revisado_por_id = p_usuario, fecha_revision = NOW()
    WHERE id = p_solicitud;
  INSERT INTO suscripcion (cliente_id, plan_id, solicitud_id, descuento_porcentaje, fecha_inicio, fecha_vencimiento)
    VALUES (v_cliente, v_plan, p_solicitud, v_descuento, NOW(), v_base + INTERVAL v_dias DAY);
  SET p_suscripcion = LAST_INSERT_ID();
END//

-- Elimina una cuenta: borra los datos personales y conserva las ventas (Ley 1581)
CREATE PROCEDURE anonimizar_cliente(IN p_cliente BIGINT UNSIGNED)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM cliente WHERE id = p_cliente) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El cliente no existe';
  END IF;
  IF EXISTS (SELECT 1 FROM pedido WHERE cliente_id = p_cliente AND estado_pedido IN ('pendiente','en_proceso')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El cliente tiene pedidos en curso; no se puede eliminar la cuenta todavía';
  END IF;
  IF EXISTS (SELECT 1 FROM solicitud_suscripcion WHERE cliente_id = p_cliente AND estado_solicitud = 'pendiente') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El cliente tiene un pago por revisar; no se puede eliminar la cuenta todavía';
  END IF;
  UPDATE suscripcion SET estado_suscripcion = 'cancelada', fecha_cancelacion = NOW() WHERE cliente_id = p_cliente AND estado_suscripcion = 'activa';
  UPDATE solicitud_suscripcion SET titular_pago = NULL, comprobante_pago = NULL, nota_solicitud = NULL, referencia_pago = NULL, numero_aprobacion = NULL
    WHERE cliente_id = p_cliente;
  UPDATE pedido SET direccion_entrega = 'Dato eliminado' WHERE cliente_id = p_cliente AND direccion_entrega IS NOT NULL;
  DELETE FROM favorito WHERE cliente_id = p_cliente;
  DELETE FROM recuperacion_clave WHERE cliente_id = p_cliente;
  UPDATE cliente SET nombre_cliente = 'Cuenta eliminada', correo_cliente = NULL, telefono_cliente = NULL, direccion_cliente = NULL, clave_hash = NULL,
      foto_cliente = NULL, cuenta_activa = FALSE, fecha_eliminacion = NOW()
    WHERE id = p_cliente;
END//

-- Tareas diarias: vence suscripciones y avisa de lotes próximos a vencer (RF005)
CREATE PROCEDURE tareas_diarias()
BEGIN
  DECLARE v_dias INT DEFAULT 90;
  SELECT CAST(valor_configuracion AS UNSIGNED) INTO v_dias FROM configuracion WHERE clave_configuracion = 'dias_alerta_vencimiento';

  UPDATE suscripcion SET estado_suscripcion = 'vencida' WHERE estado_suscripcion = 'activa' AND fecha_vencimiento < NOW();

  INSERT IGNORE INTO notificacion (tipo_alerta, mensaje_alerta, presentacion_id, lote_id)
    SELECT 'proximo_vencer',
           LEFT(CONCAT(p.nombre_producto, ' ', pr.tipo_presentacion, ', lote ', l.numero_lote, ': vence el ', DATE_FORMAT(l.fecha_vencimiento, '%d/%m/%Y')), 255),
           pr.id, l.id
    FROM lote l JOIN presentacion pr ON pr.id = l.presentacion_id JOIN producto p ON p.id = pr.producto_id
    WHERE l.cantidad_actual > 0 AND l.fecha_vencimiento IS NOT NULL
      AND l.fecha_vencimiento <= CURDATE() + INTERVAL v_dias DAY;
END//

CREATE EVENT evt_tareas_diarias
  ON SCHEDULE EVERY 1 DAY STARTS (CURRENT_DATE + INTERVAL 1 DAY + INTERVAL 1 HOUR)
  COMMENT 'Ejecuta las tareas diarias a la 1:00 a. m.'
  DO CALL tareas_diarias()//

DELIMITER ;

-- ---------------------------------------------------------------------
-- Vistas: consultas listas para los reportes
-- ---------------------------------------------------------------------

-- Existencias en tiempo real por producto y categoría (RF007, RF011)
CREATE VIEW vista_stock AS
SELECT pr.id AS presentacion_id, p.id AS producto_id, p.nombre_producto AS producto, c.nombre_categoria AS categoria, c.tipo_categoria AS tipo,
       pr.tipo_presentacion AS presentacion, pr.concentracion, pr.marca, pr.precio_venta AS precio, pr.stock_minimo,
       COALESCE(SUM(l.cantidad_actual), 0) AS stock_total,
       COALESCE(SUM(IF(l.fecha_vencimiento IS NULL OR l.fecha_vencimiento >= CURDATE(), l.cantidad_actual, 0)), 0) AS stock_vendible,
       COALESCE(SUM(IF(l.fecha_vencimiento IS NULL OR l.fecha_vencimiento >= CURDATE(), l.cantidad_actual, 0)), 0) < pr.stock_minimo AS bajo_minimo
FROM presentacion pr
JOIN producto p ON p.id = pr.producto_id
JOIN categoria c ON c.id = p.categoria_id
LEFT JOIN lote l ON l.presentacion_id = pr.id
GROUP BY pr.id, p.id, p.nombre_producto, c.nombre_categoria, c.tipo_categoria, pr.tipo_presentacion, pr.concentracion, pr.marca, pr.precio_venta, pr.stock_minimo;

-- Lotes con existencia ordenados por vencimiento (RF005)
CREATE VIEW vista_proximos_vencer AS
SELECT l.id AS lote_id, l.numero_lote, l.fecha_vencimiento, DATEDIFF(l.fecha_vencimiento, CURDATE()) AS dias_restantes,
       l.cantidad_actual, p.nombre_producto AS producto, pr.tipo_presentacion AS presentacion, pr.concentracion
FROM lote l
JOIN presentacion pr ON pr.id = l.presentacion_id
JOIN producto p ON p.id = pr.producto_id
WHERE l.cantidad_actual > 0 AND l.fecha_vencimiento IS NOT NULL;

-- Historial de entradas y salidas de cada producto (RF006)
CREATE VIEW vista_historial_inventario AS
SELECT m.id AS movimiento_id, m.fecha_movimiento AS fecha, m.tipo_movimiento AS tipo, m.cantidad_movida AS cantidad, m.saldo_resultante, m.motivo_movimiento AS motivo, l.numero_lote,
       p.nombre_producto AS producto, pr.tipo_presentacion AS presentacion, pr.concentracion, u.nombre_usuario AS usuario
FROM movimiento_inventario m
JOIN lote l ON l.id = m.lote_id
JOIN presentacion pr ON pr.id = l.presentacion_id
JOIN producto p ON p.id = pr.producto_id
LEFT JOIN usuario u ON u.id = m.usuario_id;

-- Ventas por fecha, producto y vendedor (RF012, RF013)
CREATE VIEW vista_ventas AS
SELECT pe.id AS pedido_id, pe.numero_pedido, pe.fecha_creacion AS fecha, pe.canal_venta AS canal, pe.estado_pedido AS estado, u.nombre_usuario AS vendedor,
       p.id AS producto_id, p.nombre_producto AS producto, c.nombre_categoria AS categoria, d.cantidad_vendida AS cantidad, d.precio_unitario, d.subtotal_linea AS subtotal
FROM pedido pe
JOIN detalle_pedido d ON d.pedido_id = pe.id
JOIN presentacion pr ON pr.id = d.presentacion_id
JOIN producto p ON p.id = pr.producto_id
JOIN categoria c ON c.id = p.categoria_id
LEFT JOIN usuario u ON u.id = pe.vendedor_id;
