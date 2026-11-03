# Sistema visual 3H

Referencia del diseño implementado en `static/css/design-system.css`. La tienda (`tienda.css`) y el panel (`admin.css`) solo usan estos tokens.

## Idea

**La curva dorada.** La identidad sale del logo y de la pieza de marca: azul noche, un arco dorado y una cápsula roja y blanca. El azul entra en curva con filo dorado en la portada, la franja de hechos, el pie y el acceso. Los botones y filtros tienen forma de cápsula, y un trazo dorado subraya los títulos principales.

**El mostrador.** La búsqueda manda, cada producto se abre en una ficha y el recibo de ejemplo con borde troquelado aparece en la portada.

## Color

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `--navy` | `#10224D` | `#17254B` | Superficies de marca |
| `--accion` | `#10224D` | `#E6C26E` | Fondo de la acción principal |
| `--oro` | `#C2963A` | `#E6C26E` | Filos, curvas y estados activos |
| `--rojo` | `#C8102E` | `#D11F3A` | Contador del carrito |
| `--royal` | `#17306E` | `#A4BBF0` | Enlaces |
| `--burgundy` | `#B00E28` | `#F4A7B5` | Errores, peligro y categoría de variedades |
| `--emerald-tinta` | `#17694F` | `#71D9B8` | Éxito y precio con descuento |
| `--pearl` | `#F6F7FA` | `#0B1226` | Fondo de página |
| `--superficie` | `#FFFFFF` | `#131C36` | Tarjetas, barras y diálogos |
| `--tinta` / `-2` / `-3` | `#10224D` / `#38435C` / `#566178` | `#EDF0F7` / `#C8D0E0` / `#A9B5CB` | Texto |

Las superficies azul noche (`.footer-global`, `.hechos`, `.perfil-hero`, `.auth-marca`, `.lateral`) redefinen estos tokens dentro de su propio bloque: el mismo componente funciona encima sin reglas especiales. Las ilustraciones usan los colores del logo y siempre van sobre una placa clara.

## Tipografía

**Sora** para títulos (500 y 600), **Manrope** para todo lo demás (400 a 700). Los precios y las cantidades usan numerales tabulares (`.num`).

| Token | Tamaño | Uso |
|---|---|---|
| `--t-4xl` | 32–44 px | Título de la portada |
| `--t-3xl` | 26–34 px | Título de página y de sección |
| `--t-2xl` | 22–26 px | Título del panel y de la ficha |
| `--t-xl` | 20 px | Subtítulos |
| `--t-md` | 16 px | Cuerpo |
| `--t-sm`, `--t-xs` | 14 y 12 px | Tablas, ayudas y etiquetas |

## Forma y profundidad

Radios de 6, 10, 16 y 22 px, y forma de cápsula en botones, filtros y búsqueda. Los contornos hacen el trabajo y las sombras quedan para lo que flota: menús, diálogos y sugerencias. Controles de al menos 44 px de alto, 46 px en el teléfono.

## Componentes propios

- **Buscador global** (`buscador.js`): combobox accesible, sin distinguir tildes. Busca en nombre, principio activo, marca, categoría, presentación y descripción. En el inicio el buscador grande de la portada ocupa ese lugar, y el de la barra aparece cuando aquel sale de la pantalla.
- **Tarjeta de producto** y **ficha** (`ficha.js`): la tarjeta resume, la ficha decide. Solo se muestran etiquetas de datos registrados: fórmula médica y disponibilidad no aparecen si no hay dato.
- **Tira de pedido**: `.tira` en la portada, siempre rotulada como ejemplo y con precios reales del catálogo.
- **Confirmación**: `Experiencia.confirmar()` o el atributo `data-confirmar` en cualquier formulario.
- **Tablas**: `Panel.paginar()` agrega paginación y filtrado. Por debajo de 900 px, `.tabla--fichas` convierte cada fila en una ficha.

## Movimiento

Corto y con función: revelado de 12 px una sola vez, viaje de la miniatura hasta el carrito, líneas de la tira al cargar y fundido al cambiar de tema. Con `prefers-reduced-motion` todo queda inmediato. Nada rebota ni se anima en bucle, salvo los cargadores.

## Temas

`js/tema.js` se carga en `<head>`, antes de pintar. Guarda la elección en `lth_tema` y, si no hay elección, sigue al sistema. Cada color pasa por un token, así que un componente nuevo que use tokens ya funciona en ambos temas.
