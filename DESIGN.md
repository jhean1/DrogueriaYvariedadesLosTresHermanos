# Sistema visual 3H

Referencia del diseño implementado en `static/css/design-system.css`. La tienda (`tienda.css`) y el panel (`admin.css`) se construyen sobre estos tokens.

## Idea

**Profesional y con vida.** La base es la composición de la marca: blanco y azul noche, con una curva de filo dorado. Sobre ella, un movimiento sutil que da personalidad sin recargar la página. El azul estructura, el rojo señala lo importante y el dorado solo aparece como línea fina.

## Color

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `--navy` | `#10224D` | `#17254B` | Superficies de marca |
| `--accion` | `#10224D` | `#3F63C9` | Acción principal |
| `--oro` | `#C2963A` | `#E6C26E` | Líneas finas y estados activos |
| `--rojo` | `#C8102E` | `#D11F3A` | CTA del inicio, contador del carrito y pasos de la guía |
| `--burgundy` | `#B00E28` | `#F4A7B5` | Precios, errores y categoría de variedades |
| `--royal` | `#17306E` | `#A4BBF0` | Enlaces |
| `--pearl` | `#F6F7FA` | `#0B1226` | Fondo de página |
| `--superficie` | `#FFFFFF` | `#131C36` | Tarjetas, barras y diálogos |
| `--tinta` / `-2` / `-3` | `#10224D` / `#38435C` / `#566178` | `#EDF0F7` / `#C8D0E0` / `#A9B5CB` | Texto |

Las superficies azul noche (`.footer-global`, `.hechos`, `.perfil-hero`, `.auth-marca`, `.lateral`) redefinen los tokens dentro de su bloque, así que cualquier componente funciona encima sin reglas especiales. Las imágenes de producto van sobre una placa clara en ambos temas.

## Tipografía

**Sora** para títulos (500 y 600) y **Manrope** para el resto. Precios y cantidades con numerales tabulares.

## Forma

Botones, filtros y búsqueda en forma de cápsula, como la del logo. Radios de 6, 10, 16 y 22 px. Las sombras quedan para lo que flota. Controles de al menos 44 px.

## Movimiento

Solo CSS y JavaScript propio, corto y discreto. `Experiencia.observar()` es el único IntersectionObserver: revela con `.visible`, inicia contadores y pausa con `.pausado` los bloques marcados con `data-loop` fuera de pantalla.

- **Inicio** (`inicio.js`): buscador que se escribe solo, parallax leve, carrusel de beneficios en el celular, pestañas y guía que se dibuja con el scroll.
- **Global** (`experiencia.js`): pantalla de carga, progreso de scroll, WhatsApp flotante, volver arriba, tilt leve y ondas al tocar.
- **Ficha** (`ficha.js`, `arte.js`): la imagen cambia con la presentación elegida, con tantas píldoras como indica.
- Con `prefers-reduced-motion` no hay pantalla de carga ni bucles, y los reveals son inmediatos. Tilt y parallax solo existen con mouse.

## Regla del buscador en el celular

En el inicio, el buscador de la barra flota sobre el contenido. **No debe ocupar espacio en la página**: si al aparecer cambia el alto del encabezado, mueve el buscador grande, se vuelve a ocultar y la página salta sin parar.

## Temas

`js/tema.js` se carga en `<head>`, antes de pintar. Guarda la elección en `lth_tema` y, si no hay elección, sigue al sistema.
