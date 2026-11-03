# Sistema visual 3H

Referencia del diseño implementado en `static/css/design-system.css`. La tienda (`tienda.css`) y el panel (`admin.css`) se construyen sobre estos tokens. Hay un solo tema.

## Idea

**La droguería de confianza del barrio.** Alegre, cercana y con vida: el azul domina el hero, los títulos, las tarjetas destacadas y el pie; el contenido respira sobre blanco y gris claro, y el rojo aparece donde importa.

## Color

| Token | Valor | Uso |
|---|---|---|
| `--azul` / `--azul-medio` / `--azul-claro` | `#1B2A6B` / `#2D3E9E` / `#4A5FD4` | Títulos, superficies y tarjetas destacadas |
| `--rojo` / `--rojo-vivo` | `#C0102A` / `#E8122F` | CTA principal, precios, insignias, navegación activa y carrito |
| `--blanco` / `--gris-claro` / `--gris-medio` | `#FFFFFF` / `#F4F5F9` / `#DDE0EC` | Fondos alternos y bordes |
| `--gris-texto` / `--negro-suave` | `#6B7280` / `#111827` | Texto |
| `--grad-hero`, `--grad-card`, `--grad-rojo` | ver archivo | Hero, superficies azules y CTA |

El texto secundario de la interfaz usa `#5F6675` porque `--gris-texto` no alcanza el contraste 4.5:1 sobre gris claro. Verde y naranja quedan solo para estados: éxito, avisos y WhatsApp. El logo conserva sus colores originales.

Las superficies azules (`.perfil-hero`, `.auth-marca`, `.lateral`, `.catalogo-cabecera`, `.sus-hero`, `.cabecera`, `.plan-card.popular`) redefinen los tokens de texto y borde dentro de su bloque, así que cualquier componente funciona encima sin reglas especiales.

## Tipografía

**Sora** para títulos (600 a 800) y **Manrope** para el resto (400 a 800). Precios y cantidades con numerales tabulares.

## Forma

Botones, filtros y búsqueda con forma de cápsula. Tarjetas con radios de 18 a 24 px y las sombras `--sombra-sm/md/lg`. Controles de al menos 44 px.

## Movimiento

Solo CSS y JavaScript propio. `Experiencia.observar()` es el único IntersectionObserver: revela con `.visible`, inicia contadores y pausa con `.pausado` los bloques marcados con `data-loop` cuando salen de pantalla.

- **Inicio** (`inicio.js`): buscador que se escribe solo, parallax, carrusel de beneficios, pestañas y guía que se dibuja con el scroll.
- **Global** (`experiencia.js`): progreso de scroll, botones flotantes, tilt y ondas.
- El gradiente del hero se mueve por `transform` para no repintar. Las excepciones a transform y opacity son pedidas por el diseño: el cambio de forma de los blobs (solo en escritorio) y el trazo de los SVG.
- Con `prefers-reduced-motion` no hay pantalla de entrada ni bucles, y los reveals son inmediatos. Tilt y parallax solo existen con mouse.
