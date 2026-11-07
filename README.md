<div align="center">

<img src="src/main/resources/static/img/marca/logo-vertical.svg" alt="Los Tres Hermanos" width="240">

# Droguería y Variedades Los Tres Hermanos

**Tienda web para una droguería de Arjona, Bolívar (Colombia): catálogo de medicamentos y variedades, pedidos en línea, planes de suscripción con descuento y panel de administración, sobre MySQL.**

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-activo-6DB33F?logo=springsecurity&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-plantillas-005F0F?logo=thymeleaf&logoColor=white)
![Estado](https://img.shields.io/badge/estado-proyecto%20acad%C3%A9mico-1B2A6B)

**[Ver el sitio publicado](https://jhean1.github.io/DrogueriaYvariedadesLosTresHermanos/)**

[Capturas](#-capturas) · [Funcionalidades](#-funcionalidades) · [Equipo y roles](#-equipo-y-roles) · [Arquitectura](#-arquitectura) · [Instalación](#-instalación-y-ejecución) · [Seguridad](#-seguridad) · [Limitaciones](#%EF%B8%8F-limitaciones-actuales)

<img src="docs/imagenes/inicio.png" alt="Página de inicio de la droguería" width="850">

</div>

## 📌 Sobre el proyecto

Este proyecto de aula lleva a la web la atención de una droguería de barrio. Un cliente puede explorar los productos, armar su carrito, elegir entre domicilio o recoger en tienda y hacer su pedido desde su cuenta. La droguería lo ve en el panel, lo prepara y lo entrega; el pago se hace en efectivo al recibirlo.

Los clientes también pueden suscribirse a un plan mensual para obtener descuentos. El administrador revisa los comprobantes de pago y activa los planes desde su panel.

> [!IMPORTANT]
> Las cuentas de clientes, los pedidos, los pagos de planes y el catálogo se guardan en MySQL (base `drogueria`), así que **todo eso solo funciona con el servidor Spring Boot en marcha**. El [sitio de GitHub Pages](https://jhean1.github.io/DrogueriaYvariedadesLosTresHermanos/) es una vista previa estática del catálogo: allí no se pueden crear cuentas ni hacer pedidos en línea (se pide por WhatsApp). Consulta las [limitaciones actuales](#%EF%B8%8F-limitaciones-actuales).

## 📸 Capturas

> [!NOTE]
> Las capturas son de una versión anterior con **datos ficticios**: todavía no muestran las fotos de los productos, los precios nuevos de los planes ni el cálculo del domicilio.

### Catálogo

| Medicamentos | Variedades |
|:---:|:---:|
| <img src="docs/imagenes/medicamentos.png" alt="Catálogo de medicamentos con filtros y selector de presentación" width="420"> | <img src="docs/imagenes/variedades.png" alt="Catálogo de variedades por categoría" width="420"> |
| Búsqueda, filtros y selección de presentación, concentración y marca | Perfumes, útiles escolares y cuidado personal |

| Ficha de producto | Modo oscuro |
|:---:|:---:|
| <img src="docs/imagenes/ficha.png" alt="Ficha de un medicamento con sus presentaciones" width="420"> | <img src="docs/imagenes/oscuro-inicio.png" alt="Inicio en modo oscuro" width="420"> |

### Suscripciones y cuenta del cliente

| Planes de suscripción | Registro | Mi cuenta |
|:---:|:---:|:---:|
| <img src="docs/imagenes/suscripciones.png" alt="Los cuatro planes de suscripción" width="280"> | <img src="docs/imagenes/registro.png" alt="Formulario de registro de clientes" width="280"> | <img src="docs/imagenes/perfil.png" alt="Perfil del cliente con su historial de pedidos" width="280"> |

### Panel de administración

| Resumen | Revisión de un pago |
|:---:|:---:|
| <img src="docs/imagenes/admin-panel.png" alt="Resumen del panel con indicadores y gráficas" width="420"> | <img src="docs/imagenes/admin-solicitud.png" alt="Detalle de una solicitud de pago" width="420"> |

| Productos | Formulario de producto |
|:---:|:---:|
| <img src="docs/imagenes/admin-productos.png" alt="Lista de productos con búsqueda y filtros" width="420"> | <img src="docs/imagenes/admin-producto.png" alt="Formulario de edición de un medicamento" width="420"> |

| Roles y permisos | Módulo en desarrollo | Acceso del equipo |
|:---:|:---:|:---:|
| <img src="docs/imagenes/admin-accesos.png" alt="Roles y permisos del equipo" width="280"> | <img src="docs/imagenes/admin-modulo.png" alt="Módulo de inventario en desarrollo" width="280"> | <img src="docs/imagenes/admin-login.png" alt="Inicio de sesión del equipo" width="280"> |

### En el celular

| Inicio | Medicamentos |
|:---:|:---:|
| <img src="docs/imagenes/movil-inicio.png" alt="Inicio en un celular" width="240"> | <img src="docs/imagenes/movil-medicamentos.png" alt="Catálogo en un celular" width="240"> |

## ✨ Funcionalidades

### Para clientes

- 🔎 **Búsqueda central:** visible en toda la tienda, con sugerencias al escribir por nombre, marca, principio activo o categoría.
- 💊 **Catálogo:** tarjetas compactas y ficha de producto con presentación, concentración, marca y precio.
- 🌗 **Modo claro y oscuro:** interruptor visible que recuerda la preferencia, con movimiento sutil que respeta la opción de menos animación.
- 🖼️ **Imágenes de productos:** foto exacta del producto o, si no hay, una imagen parecida marcada como «Imagen de referencia». El personal autorizado sube las fotos propias desde el catálogo.
- 🧭 **Guía de compra:** cuatro pasos en el inicio que se dibujan al bajar por la página.
- 🛒 **Carrito y pedido:** carrito persistente, entrega a domicilio o recogida en tienda. El servidor calcula precios, descuento y domicilio con la base de datos.
- 🛵 **Domicilio según la distancia:** el cliente comparte su ubicación y el valor se calcula desde la droguería ($3.000 hasta 1 km y $1.000 por cada km adicional, hasta 8 km). Con el plan Premium es gratis. Si no comparte la ubicación, la droguería le confirma el valor.
- 🧾 **Recibo e historial:** recibo de cada pedido con su estado y consulta de compras anteriores desde **Mi cuenta**.
- ❤️ **Favoritos:** el cliente guarda productos desde su ficha y los ve en **Mi cuenta**.
- ⭐ **Suscripciones:** descuento automático en toda la tienda cuando el plan está aprobado y vigente.
- 💳 **Pago de planes:** efectivo en la tienda, o en línea con envío de comprobante por los medios que la droguería tenga configurados (Nequi, Daviplata, PSE o Bancolombia).
- 👤 **Cuenta personal:** foto, edición de datos, cambio de correo y contraseña, y eliminación de la cuenta (se borran los datos personales y se conservan las ventas sin ellos).

### Para administradores

- 📊 **Resumen:** indicadores y accesos rápidos según el rol.
- 🧴 **Productos y categorías:** crear, editar, retirar y volver a publicar medicamentos y variedades, con foto, validación y auditoría.
- 📦 **Pedidos:** búsqueda y filtros; preparar, marcar como entregado (descuenta el inventario por vencimiento) o anular con motivo.
- ✅ **Pagos:** revisión de comprobantes y aprobación, o rechazo con motivo que el cliente ve en su cuenta.
- 👥 **Clientes:** búsqueda y creación de cuentas.
- 🏪 **Pagos en tienda:** activación manual del plan cuando el cliente paga en efectivo.
- 🔑 **Usuarios y permisos:** crea roles y cuentas, asigna o revoca permisos por rol o por persona y activa o desactiva cuentas; Spring Security lo valida en cada petición.
- 🕵️ **Auditoría:** historial de inicios de sesión y cambios de accesos.
- 🧭 **Módulos planificados:** inventario, ventas, compras, caja, reportes, auditoría y notificaciones, marcados como *en desarrollo* y sin acciones simuladas.

### Planes de suscripción

| Plan | Precio mensual | Descuento | Domicilio prioritario | Domicilio gratis | Ofertas anticipadas y acceso exclusivo |
|---|---:|:---:|:---:|:---:|:---:|
| 🌱 Básico | $9.900 COP | 3 % | — | — | — |
| 💊 Estándar | $19.900 COP | 5 % | — | — | — |
| 🏥 Regular | $29.900 COP | 10 % | ✅ | — | — |
| 👑 Premium | $39.900 COP | 15 % | ✅ | ✅ | ✅ |

Los precios, los descuentos y la tarifa del domicilio están en la base de datos (tablas `plan_suscripcion` y `configuracion`), así que se pueden ajustar sin cambiar el código.

## 👥 Equipo y roles

### Cómo entra el equipo al panel

1. Abre `/admin/login` en el servidor (por ejemplo, <http://localhost:8081/admin/login>). El panel **no existe en GitHub Pages**.
2. **Propietario:** entra con `ADMIN_CORREO` (por defecto `admin@lostreshermanos.com`) y la clave de `ADMIN_CLAVE`. Si no se define, la aplicación inventa una clave temporal en cada arranque y la escribe en la consola.
3. **Resto del equipo:** el propietario crea cada cuenta en **Usuarios y permisos** (`/admin/accesos`), con su correo, una clave temporal y su rol. También se pueden crear al arrancar con `APP_EQUIPO_0_CORREO`, `APP_EQUIPO_0_ROL` y `APP_EQUIPO_0_CLAVE` (1, 2… para más personas).
4. Cada persona ve solo las secciones de su rol. El servidor vuelve a comprobar el permiso en cada acción.

Las cuentas de **clientes** son otra cosa: se crean en `/registro` (o desde el panel, en *Crear cliente*) y entran por `/login`. Un cliente nunca entra al panel.

### Roles

| Rol | Para quién | Qué puede hacer |
|---|---|---|
| Propietario | Dueño de la droguería | Todo: usuarios, roles, permisos, auditoría y configuración. No se puede desactivar. |
| Administrador | Quien maneja la operación | Clientes, pagos de planes, planes en tienda, productos, pedidos (incluido anularlos) e inventario. |
| Regente | Responsable técnico | Productos farmacéuticos, lotes y vencimientos. |
| Cajero / Vendedor | Mostrador | Consulta de productos y pedidos, ventas y caja. |
| Auxiliar de inventario | Bodega | Existencias, recepción de mercancía y conteos. |

Desde **Usuarios y permisos** se pueden crear roles nuevos y quitar o dar permisos a una persona en particular. Inventario, ventas de mostrador, compras, caja y reportes ya tienen sus permisos y su lugar en el panel, pero todavía están en desarrollo.

## 🛠️ Tecnologías

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1 (Spring MVC) |
| Seguridad | Spring Security, sesiones en el servidor y BCrypt |
| Vistas | Thymeleaf con fragmentos, HTML5, CSS3 y JavaScript sin frameworks |
| Diseño | Sistema de diseño propio con variables CSS; tipografías Sora y Manrope |
| Base de datos | MySQL 9.7 LTS con JdbcTemplate y HikariCP ([documentación](docs/base-de-datos/README.md)) |
| Construcción | Maven (incluye Maven Wrapper) |
| Pruebas | JUnit 5, Spring MockMvc y Spring Security Test |

## 🏗️ Arquitectura

El servidor entrega las páginas, guarda cuentas, pedidos y pagos en MySQL y protege el panel. El navegador solo guarda el carrito y el tema de color.

```mermaid
flowchart LR
    C["🧑 Cliente"] --> N
    A["🛡️ Administrador"] --> N

    subgraph N["Navegador"]
        V["Páginas HTML y JavaScript"]
        L[("Carrito y tema")]
        V <--> L
    end

    subgraph S["Servidor Spring Boot"]
        F["Spring Security"]
        P["Controladores y API"]
        T["Plantillas Thymeleaf"]
        F --> P --> T
    end

    P <--> D[("MySQL: drogueria")]

    V -- "Solicita páginas" --> F
    T -- "Devuelve HTML" --> V
    V -- "Pedidos, cuentas y pagos (API)" --> F
```

### Flujo de una suscripción

```mermaid
sequenceDiagram
    actor Cliente
    participant Tienda as Tienda web
    actor Admin as Administrador

    Cliente->>Tienda: Elige un plan y envía su comprobante
    Tienda-->>Cliente: Solicitud pendiente, todavía sin descuento
    Admin->>Tienda: Revisa el comprobante
    alt Pago válido
        Admin->>Tienda: Aprueba la solicitud
        Tienda-->>Cliente: Plan activo por 30 días con descuento
    else Pago no válido
        Admin->>Tienda: Rechaza la solicitud
        Tienda-->>Cliente: Solicitud rechazada, sin descuento
    end
```

### Estructura del proyecto

```text
.
├── datos/                          # Solo en tu equipo (no se sube): conexión, accesos y fotos subidas
├── docs/base-de-datos/             # Diagramas, diccionario, scripts y herramientas de MySQL
├── docs/imagenes/                  # Capturas de este README
├── fotos-productos/                # Imágenes de los productos (exactas y de referencia)
├── scripts/                        # Generador del sitio de GitHub Pages
├── pom.xml                         # Dependencias y construcción
└── src
    ├── main
    │   ├── java/com/example/DrogueriaLosTresHermanos
    │   │   ├── configuracion/      # Spring Security, filtros y propiedades del equipo
    │   │   ├── controlador/        # Controlador (MVC): rutas de la tienda y del panel
    │   │   ├── modelo/             # Modelo (MVC): datos, roles, permisos y módulos
    │   │   └── servicio/           # Catálogo, clientes, pedidos, planes, fotos y permisos
    │   └── resources
    │       ├── application.properties
    │       ├── catalogo-inicial.properties  # Catálogo de respaldo cuando no hay base de datos
    │       ├── static
    │       │   ├── css/            # design-system.css, tienda.css y admin.css
    │       │   ├── img/marca/      # Sistema de identidad 3H (SVG y PNG)
    │       │   └── js/             # Lógica, búsqueda, ficha de producto y tema
    │       └── templates/          # Páginas Thymeleaf y fragmentos compartidos
    └── test/java/...               # Pruebas automatizadas
```

### Rutas

| Ruta | Página | Acceso |
|---|---|---|
| `/` | Inicio | Público |
| `/medicamentos`, `/variedades` | Catálogos | Público |
| `/servicios`, `/nosotros`, `/contacto` | Información de la droguería | Público |
| `/suscripciones` | Planes | Público |
| `/registro`, `/login` | Cuenta de cliente | Público |
| `/perfil`, `/checkout`, `/recibo`, `/pago-suscripcion` | Área del cliente | Con sesión de cliente en el servidor |
| `/api/cuenta/**`, `/api/pedidos`, `/api/suscripciones/**` | API de la tienda | Con sesión de cliente (salvo registro e ingreso) |
| `/api/planes`, `/api/pagos/medios`, `/api/domicilio` | Planes, medios de pago y valor del domicilio | Público |
| `/api/admin/**` | API del panel | **Según el permiso de cada acción** |
| `/admin/login` | Acceso de administrador | Público |
| `/admin` | Panel de administración | **Todo el equipo; cada sección según permisos** |
| `/admin/productos` | Lista de productos | **Con `products.view`** |
| `/admin/productos/nuevo` | Crear producto | **Con `products.create`** |
| `/admin/productos/{id}` | Editar producto | **Con `products.update`** |
| `/admin/productos/{id}/visibilidad` | Retirar o publicar | **Con `products.delete`** |
| `/admin/productos/{id}/foto` | Cambiar la imagen desde el catálogo | **Con `products.update`** |
| `/admin/categorias` | Categorías | **Con `products.update`** |
| `/js/catalogo.js`, `/productos/imagen/{foto}` | Catálogo publicado y fotos | Público |
| `/admin/modulos/{modulo}` | Módulos en desarrollo | **Según el permiso de cada módulo** |
| `/admin/accesos` | Usuarios y permisos | **Con `users.manage` o `roles.manage`** |
| `/admin/modulos/auditoria` | Historial de acciones | **Solo con `audit.view`** |

## 🚀 Instalación y ejecución

### Requisitos

- [JDK 21 o superior](https://adoptium.net/). No hace falta instalar Maven, porque el proyecto incluye Maven Wrapper.
- [MySQL 9.7 LTS](https://dev.mysql.com/downloads/mysql/) para cuentas, pedidos y planes. Sin MySQL la tienda arranca igual, pero solo muestra el catálogo.

### Pasos

**1. Clona el repositorio**

```bash
git clone https://github.com/jhean1/DrogueriaYvariedadesLosTresHermanos.git
cd DrogueriaYvariedadesLosTresHermanos
```

**2. Crea la base de datos** siguiendo [docs/base-de-datos](docs/base-de-datos/README.md) (scripts `00`, `01` y `02`) y escribe la conexión en `datos/conexion.properties`:

```properties
app.db.url=jdbc:mysql://127.0.0.1:3306/drogueria?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Bogota
app.db.usuario=drogueria_app
app.db.clave=la-clave-del-usuario-de-mysql
```

**3. Define la clave del administrador** (mínimo 12 caracteres)

```powershell
# Windows (PowerShell)
$env:ADMIN_CLAVE = "escribe-aqui-una-clave-larga-y-unica"
```

```bash
# Linux o macOS
export ADMIN_CLAVE="escribe-aqui-una-clave-larga-y-unica"
```

Si no la defines, la aplicación genera una clave temporal en cada arranque y la muestra en la consola.

**4. Inicia la aplicación**

```powershell
# Windows
.\mvnw.cmd spring-boot:run
```

```bash
# Linux o macOS
./mvnw spring-boot:run
```

**5. Abre el sitio**

- Tienda: <http://localhost:8081>
- Panel de administración: <http://localhost:8081/admin/login>

> [!NOTE]
> En un hosting, en lugar de `datos/conexion.properties` usa las variables `APP_DB_URL`, `APP_DB_USUARIO` y `APP_DB_CLAVE`. Compartir la ubicación para calcular el domicilio solo funciona con HTTPS (o en `localhost`).

### Variables de entorno

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `ADMIN_CORREO` | Correo para entrar al panel | `admin@lostreshermanos.com` |
| `ADMIN_CLAVE` | Clave del administrador (12 caracteres o más) | Se genera una temporal en cada arranque |
| `COOKIE_SEGURA` | Usa `true` si el sitio funciona con HTTPS | `false` |
| `APP_DATOS` | Archivo donde se guardan roles, cuentas del equipo y auditoría | `datos/accesos.properties` |
| `APP_CATALOGO` | Archivo del catálogo administrado desde el panel | `datos/catalogo.properties` |
| `APP_FOTOS` | Carpeta de las fotos que se suben desde el panel | `datos/fotos` |
| `APP_FOTOS_REPOSITORIO` | Imágenes del catálogo incluidas en el repositorio; se usan si la foto no está en `APP_FOTOS` | `fotos-productos` |
| `APP_DB_URL`, `APP_DB_USUARIO`, `APP_DB_CLAVE` | Conexión con MySQL | Lo de `datos/conexion.properties` |
| `APP_FOTOS_CLIENTES` | Carpeta de las fotos de perfil de los clientes | `datos/fotos-clientes` |
| `APP_COMPROBANTES` | Carpeta de los comprobantes de pago de planes | `datos/comprobantes` |
| `APP_EQUIPO_0_CORREO`, `APP_EQUIPO_0_ROL`, `APP_EQUIPO_0_CLAVE` | Miembro adicional del equipo. Usa 1, 2… para agregar más | Sin miembros adicionales |

Roles disponibles para `APP_EQUIPO_N_ROL`: `ADMINISTRADOR`, `REGENTE`, `CAJERO` y `AUXILIAR`. La cuenta de `ADMIN_CORREO` es la del propietario.

### Pruebas

```powershell
.\mvnw.cmd test    # En Linux o macOS: ./mvnw test
```

## 🔒 Seguridad

| Medida | Qué protege |
|---|---|
| Credenciales en variables de entorno y BCrypt | Evita que la clave del administrador aparezca en el código |
| Sesión en el servidor para `/admin` | Impide entrar al panel sin autenticarse |
| Roles y permisos en Spring Security | Bloquea con 403 cada ruta sin permiso, omite del HTML las secciones no autorizadas y aplica los cambios de inmediato, incluso en sesiones abiertas |
| Reglas contra la escalada de privilegios | Nadie concede permisos que no tiene ni modifica su propia cuenta o rol; el propietario es intocable |
| Protección CSRF | Rechaza formularios y peticiones a la API enviados desde otros sitios |
| Bloqueo tras 5 intentos fallidos durante 15 minutos | Frena los ataques de fuerza bruta contra el panel |
| BCrypt para las contraseñas de clientes y bloqueo tras 5 intentos | Las contraseñas no se guardan en texto plano ni en el navegador |
| Precios, descuentos y totales calculados en el servidor | Un carrito alterado en el navegador no cambia lo que se cobra |
| Escape de datos en HTML | Reduce el riesgo de XSS con nombres o pedidos manipulados |
| Lista de rutas de retorno permitidas | Evita redirecciones a sitios externos después del login |
| Precio del plan tomado de la base de datos | Impide aprobar un plan con precio o descuento alterados |
| CSP y otras cabeceras de seguridad | Limitan scripts externos y el envío de datos a otros dominios, y evitan que el sitio se inserte en otras páginas |

## ⚠️ Limitaciones actuales

- **Cuentas de pago sin configurar:** mientras la droguería no escriba sus cuentas reales en la configuración de la base, los planes solo se pagan en efectivo en la tienda.
- **Domicilio en línea recta:** la distancia se mide en línea recta desde la droguería, no por calles; el valor real del recorrido puede ser algo mayor. El punto de la droguería (`tienda_latitud` y `tienda_longitud` en `configuracion`) es aproximado: conviene reemplazarlo por el exacto.
- **Sitio de GitHub Pages:** es una copia estática; no hay cuentas, pedidos en línea ni panel.
- **La CSP permite scripts en línea:** las plantillas actuales los necesitan.
- **Accesos en un archivo local:** roles, cuentas del equipo y auditoría se guardan en `datos/accesos.properties`. Funciona con un solo servidor y un disco persistente; para crecer conviene pasarlo a una base de datos.
- **Los módulos de gestión están en desarrollo:** muestran su alcance, pero aún no tienen datos ni acciones.
- **Fotos subidas desde el panel:** se guardan en `datos/fotos` del servidor. Para que también aparezcan en GitHub Pages hay que copiarlas a `fotos-productos/` y anotarlas en `docs/base-de-datos/datos/imagenes-productos.csv`.
- **Imágenes del catálogo:** 99 productos tienen su foto exacta y 272 una imagen de referencia, marcada así en la tienda; están en `fotos-productos/` y la relación con cada producto en `docs/base-de-datos/datos/imagenes-productos.csv`. Las imágenes provienen de catálogos públicos de tiendas colombianas; lo ideal es reemplazarlas por fotos propias desde el panel. Los productos restantes usan ilustraciones.

## 🗺️ Próximos pasos

- [ ] Reemplazar las imágenes de referencia por fotos propias de la droguería.
- [x] Incorporar una base de datos y una API en el servidor.
- [x] Validar cuentas, precios, pedidos y suscripciones en el servidor.
- [ ] Escribir las cuentas reales para recibir pagos de planes.
- [ ] Construir los módulos de inventario, ventas, compras, caja, reportes y auditoría.
- [ ] Publicar el sitio con HTTPS.

## 🌐 Publicación en GitHub Pages

Cada cambio en `master` ejecuta las pruebas, genera una copia estática de la tienda con `scripts/generar_sitio.py` y la publica en la rama `gh-pages`.

GitHub Pages solo sirve archivos estáticos, así que el sitio publicado funciona de esta forma:

| Función | En GitHub Pages | Con el servidor Spring Boot |
|---|---|---|
| Catálogo y carrito | ✅ | ✅ |
| Pedidos | Por WhatsApp | En línea, con recibo y seguimiento |
| Cuentas de cliente | ❌ No hay servidor | ✅ Guardadas en MySQL |
| Suscripciones | Se solicitan por WhatsApp | Formulario de pago con revisión en el panel |
| Panel de administración y roles | ❌ No se publica | ✅ |

Para generar el sitio en tu equipo:

```bash
./mvnw package -DskipTests
python scripts/generar_sitio.py --salida _sitio
```

## 🌿 Ramas

| Rama | Propósito |
|---|---|
| `master` | Versión principal del proyecto |
| `frontend` | Rama de trabajo; se integra en `master` cuando está probada |
| `gh-pages` | Sitio estático generado automáticamente; no se edita a mano |

## 👤 Autor

Desarrollado por [**@jhean1**](https://github.com/jhean1) como proyecto de aula.

---

<div align="center">

Droguería y Variedades Los Tres Hermanos · Arjona, Bolívar, Colombia

</div>
