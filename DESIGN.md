# Sistema visual 3H

Referencia del diseño implementado en `static/css/design-system.css`. La tienda (`tienda.css`) y el panel (`admin.css`) solo usan estos tokens.

## Idea

**El mostrador.** Buscar es preguntar en la droguería. La búsqueda manda, cada producto se abre en una ficha y la pieza propia de la marca es la **tira de pedido**: el recibo con borde troquelado que aparece en la portada y que se repite como línea superior en el resumen del pedido.

La interfaz es calmada y comercial. Sin degradados pesados, sin vidrio, sin bandas oscuras a pantalla completa, sin antetítulos, sin numeración decorativa.

## Color

Un color principal, uno secundario y neutros. El oro de la marca es puntual.

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `--accion` | `#1D3F91` | `#3563CC` | Fondo de la acción principal |
| `--royal` | `#1D3F91` | `#92B2F6` | Enlaces, estado activo, foco del icono |
| `--burgundy` | `#8E2338` | `#F0A5B4` | Errores, peligro y categoría de variedades |
| `--amber` | `#C2963A` | `#D9B060` | Avisos y datos pendientes en gráficas |
| `--emerald-tinta` | `#17694F` | `#71D9B8` | Éxito y precio con descuento |
| `--pearl` | `#F7F8FA` | `#0E1420` | Fondo de página |
| `--superficie` | `#FFFFFF` | `#161D2B` | Tarjetas, barras y diálogos |
| `--superficie-2` | `#F1F3F7` | `#1E2736` | Campos de búsqueda y encabezados de tabla |
| `--tinta` / `-2` / `-3` | `#141C2E` / `#3A4557` / `#596476` | `#E9EDF4` / `#C4CDDB` / `#A5B0C3` | Texto |
| `--borde` / `-fuerte` | `#E3E7EE` / `#C9D0DB` | `#2A3445` / `#3D495E` | Separadores y contornos de control |
| `--placa` / `-calida` | `#F3F5F9` / `#F8F4F1` | `#E6EAF1` / `#E9E3DE` | Fondo de la imagen del producto |
| `--navy` | `#14213D` | `#0A0F1A` | Solo el pie de página |

La imagen de un producto siempre va sobre una placa clara, atenuada en modo oscuro con `--placa-brillo`. Las ilustraciones y fotos no cambian de color.

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

Radios de 6, 10, 14 y 18 px. Los contornos hacen el trabajo y las sombras quedan para lo que flota: menús, diálogos y sugerencias. Controles de al menos 44 px de alto, 46 px en el teléfono.

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
