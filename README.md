# Flappy Bird OpenGL — Programación Gráfica (Primer Parcial)

Mini-juego estilo Flappy Bird implementado en Java con **LWJGL 3.3.6** y **OpenGL 3.3 core profile**. Sin texturas: todo se dibuja con primitivas (rectángulos, triángulos, círculos vía `TRIANGLE_FAN`). Soporta menú de selección **1 jugador** o **2 jugadores en pantalla compartida**.

---

## Integrantes

- _[Tu nombre completo aquí]_
- _[Legajo / email si corresponde]_

---

## Controles

### En el menú (al iniciar la app)

| Tecla | Acción |
|---|---|
| ↑ / ↓ | Cambiar opción seleccionada |
| ENTER | Confirmar y empezar la partida |

### Durante la partida

| Tecla | Acción |
|---|---|
| SPACE | Saltar — Jugador 1 (pájaro **amarillo**) |
| W | Saltar — Jugador 2 (pájaro **celeste**, solo en modo 2P) |

### En game over

| Tecla | Acción |
|---|---|
| R | Volver al menú principal |

### Siempre

| Tecla | Acción |
|---|---|
| ESC | Cerrar el juego |

---

## Cómo compilar y correr

### Requisitos

- **JDK 17 o superior** (probado con JDK 21).
- **Maven 3.x** (o el bundled de NetBeans en `<NetBeans>/java/maven/bin/`).
- **Sistema operativo: Windows** (las natives de LWJGL están configuradas para Windows). Para Linux/macOS, ver "Notas de portabilidad" más abajo.

### Compilar

Desde la carpeta raíz del proyecto:

```bash
mvn clean compile
```

### Correr

La clase principal (`com.graphics.App`) está preconfigurada en el `pom.xml`, así que basta con:

```bash
mvn exec:java
```

### Desde NetBeans

1. **File → Open Project**, elegir la carpeta `ProyectoFloppy`.
2. Esperar a que NetBeans descargue las dependencias de LWJGL.
3. Click derecho en el proyecto → **Run**.

---

## Estructura del código

```
src/main/java/com/graphics/
├── App.java           — main(), ciclo de vida de ventana GLFW + contexto OpenGL,
│                        bucle principal del juego, título de ventana.
├── Game.java          — estado del juego, máquina de estados (MENU / JUGANDO /
│                        GAME_OVER), lógica de scoring, colisiones, render
│                        orquestado de fondo + entidades + UI.
├── Bird.java          — pájaro: posición, velocidad, física, dibujo (cuerpo,
│                        pico, ala animada, cola, ojo + pupila). Se autoflagea
│                        muerto al tocar techo/suelo. Soporta múltiples
│                        instancias (P1 y P2) con paletas de color distintas.
├── Pipe.java          — tubería estilo Flappy clásico (cuerpo + bordes oscuros
│                        + highlight + boquilla). Recuerda quién la cruzó por
│                        jugador para scoring independiente.
├── Cloud.java         — nube decorativa (3 círculos blancos solapados),
│                        movimiento lento con wrap para parallax.
├── Renderer.java      — encapsula todo OpenGL: 2 shader programs (uno con
│                        color uniforme, otro con color por vértice para el
│                        cielo degradado), 4 VAOs (quad/triángulo/círculo/
│                        fondo), helpers dibujarRect/Triangulo/Circulo/
│                        FondoDegradado/RectAlpha.
├── InputManager.java  — teclado con detección de flanco (SPACE, W, R, ↑, ↓,
│                        ENTER) + ESC sin flanco. Un poll() por frame, los
│                        métodos isXxxJustPressed() son lecturas puras.
└── TextRenderer.java  — fuente bitmap 5×7 estática, sin librerías de texto.
                         Soporta A-Z, 0-9, espacio, ":", "-". Cada letra son
                         hasta 35 rectangulitos.
```

---

## Requerimientos del parcial

| ID  | Requerimiento | Implementación |
|-----|---|---|
| **R1** | Pájaro con figuras geométricas (≥5 partes), inclinación según `velY` | `Bird.dibujar()` compone el pájaro con **6 partes** (cola + ala + cuerpo + pico + ojo + pupila). Cuerpo es un círculo (`TRIANGLE_FAN`); pico y cola son triángulos; ala es un rectángulo. Inclinación: `rot = clamp(velY · 0.7, ±0.6 rad)`. Aleteo: `sin((tiempo - tiempoUltimoSalto) · 8) · 0.6`. La fase del aleteo se reinicia en cada salto → "sincronizado con el salto". |
| **R2** | Dos jugadores en la misma ventana, puntajes independientes, tuberías compartidas, partida termina solo cuando ambos chocan | Modo 2P opcional desde el **menú principal**. P1 amarillo en `x=-0.45` (SPACE), P2 celeste en `x=-0.30` (W). Estado por jugador en arrays (`puntajes[2]`, `Pipe.puntuadaPorJugador[2]`). El campo `vivo` está dentro de `Bird` — uno puede morir y el otro sigue jugando con su pájaro parpadeando atrás. Game over solo cuando `todosMuertos()` es true. Distinción visual por color de cuerpo + colores en el HUD + colores en la pantalla de game over. |
| **R3** | Velocidad progresiva con tope, nivel reflejado en UI | `Game.getNivel() = 1 + max(puntajes) / 3`, **capeado en NIVEL_MAX = 6**. Cada nivel suma `+0.10` a `Pipe.VELOCIDAD` y resta `0.12s` a `TIEMPO_ENTRE_TUBERIAS`. El nivel actual se muestra en el HUD (parte superior) y en el título de la ventana. |
| **R4** | Mejoras visuales: degradado, nubes/montañas, suelo, parpadeo del pájaro al morir | Cielo: **degradado por color por vértice** (segundo shader + interpolación de varying entre vertex y fragment shader, no son franjas pintadas a mano). Nubes: 4 instancias de `Cloud`, cada una son 3 círculos blancos solapados, parallax con velocidad 10× más lenta que las tuberías y wrap al borde derecho. Suelo: tierra marrón + franja delgada de pasto verde encima; el pájaro muere visualmente al tocar el pasto (`Bird.LIMITE_INFERIOR = -0.86`). Parpadeo: `Bird.dibujar()` salta el render en frames alternos a 5 Hz cuando `!vivo`, usando `tiempoTotal` (que avanza siempre) — así sigue parpadeando incluso con la simulación congelada. |

### Extras incluidos (5% de "calidad de código" + agregados)

- **Refactor a múltiples clases** (8 archivos, una responsabilidad cada uno).
- **Pantalla de menú** con selección de modo navegable por flechas + ENTER.
- **HUD durante el juego**: PUNTOS / TIEMPO / NIVEL en 1P; en 2P se reorganiza a `P1 / TIEMPO / P2` arriba y `NIVEL` debajo, con cada puntaje en el color del jugador correspondiente.
- **Cronómetro `mm:ss`** que se congela al perder.
- **Pantalla de game over** con tarjeta de doble marco y, en 2P, **GANADOR (en su color) o EMPATE**.
- **Tuberías estilizadas** (cuerpo + bordes oscuros + highlight claro + boquilla más ancha en el extremo del gap).
- **TextRenderer propio** con fuente bitmap 5×7 (sin dependencias externas, sin texturas).
- **Blending alpha** habilitado para los overlays semitransparentes (`SRC_ALPHA / ONE_MINUS_SRC_ALPHA`).
- **Callback de framebuffer resize** para que `glViewport` siga sincronizado al redimensionar la ventana.
- **Animación del ala sincronizada con el salto** (la fase senoidal se reinicia en cada `Bird.saltar()`).

---

## Cambios respecto al enunciado base

A continuación los cambios visibles para alguien que conozca el enunciado original. Cada uno está justificado por una razón concreta.

| Cambio | Justificación |
|---|---|
| Se reemplazaron los booleans `started` y `gameOver` por `enum EstadoJuego { MENU, JUGANDO, GAME_OVER }` | Una sola fuente de verdad sobre la pantalla a mostrar; el `switch` exhaustivo del compilador previene combinaciones inválidas y olvidos al agregar estados. |
| `Bird.X` (constante global) pasó a `x` (campo de instancia) | Para que P1 y P2 puedan estar en columnas distintas sin que se pisen visualmente. |
| `Bird` tiene campo `vivo` privado en vez de un `boolean gameOver` en `Game` | "La muerte es información del pájaro", no del orquestador. Evita doble fuente de verdad. |
| `Game.isGameOver()` se DERIVA de los `birds` (no es un campo) | Mismo motivo: una sola fuente de verdad. Imposible que `gameOver=true` y `bird.isVivo()=true` simultáneamente. |
| Tecla R en game over **vuelve al menú** (no reinicia directo) | Permite cambiar de modo de juego sin tener que cerrar y reabrir. |
| Nivel = `max(puntajes)`, no la suma | En 2P la suma escalaría la dificultad demasiado rápido; el `max` mantiene presión competitiva — el ritmo sube en cuanto cualquiera destaque. |
| Texto del menú "no seleccionado" en gris oscuro en vez de blanco | Blanco sobre la tarjeta crema queda invisible. Justificado en comentario en `Game.java`. |

---

## Notas de portabilidad

Las dependencias LWJGL declaran sus *natives* para **Windows** (`<lwjgl.natives>natives-windows</lwjgl.natives>` en el `pom.xml`). Para correrlo en otros sistemas, cambiar esa línea a:

- Linux: `natives-linux`
- macOS Intel: `natives-macos`
- macOS Apple Silicon: `natives-macos-arm64`

Y limpiar caché de Maven con `mvn clean` antes de recompilar.

---

## Tecnologías

- **Java 17** (compilado con `<maven.compiler.release>17</maven.compiler.release>`, probado con JDK 21).
- **LWJGL 3.3.6** (Lightweight Java Game Library): bindings de OpenGL y GLFW.
- **GLFW**: ventana, contexto OpenGL, input de teclado.
- **OpenGL 3.3 Core Profile**: shaders, VAOs, VBOs.
- **GLSL 330**: 2 shader programs (uno con `uColor` uniforme, otro con varying interpolado para el degradado del cielo).
- **Maven 3.x**: build y gestión de dependencias.
- **`exec-maven-plugin` 3.6.3**: para correr la app con `mvn exec:java` sin clasificar el `mainClass` cada vez.
