package com.graphics;

import org.lwjgl.glfw.GLFW;

/**
 * InputManager:
 * Lee el teclado de la ventana GLFW con DETECCION DE FLANCO.
 *
 * Por que deteccion de flanco:
 * Si solo preguntas "esta presionada SPACE" frame a frame, mientras el
 * jugador mantenga la tecla apretada vas a recibir true en TODOS los
 * frames (sesenta veces por segundo). Para que el pajaro salte una sola
 * vez por pulsacion necesitas detectar el momento exacto en que la
 * tecla pasa de "soltada" a "presionada" — eso es el flanco de subida.
 *
 * Sub-fase 5.2/5.3: el InputManager amplia su API para soportar:
 *   - Salto P1 (SPACE) y Salto P2 (W) — modo 2 jugadores.
 *   - Flecha ARRIBA / ABAJO — navegacion del menu.
 *   - ENTER — confirmar opcion del menu.
 *   - R — volver al menu desde game over.
 *
 * Renombro las funciones a un esquema mas explicito:
 *   spaceJustPressed → isSaltoP1
 *   wJustPressed     → isSaltoP2
 *   rJustPressed     → isR
 * Asi el codigo de Game lee cosas como "if (input.isSaltoP1())..."
 * en vez de tener que recordar que SPACE es para P1.
 *
 * Contrato de uso (igual que antes):
 *   1) Llamar poll() exactamente UNA vez al inicio de cada frame.
 *   2) Despues, leer los metodos isXxx() cuantas veces quieras.
 */
public class InputManager {

    private final long window;

    // Para cada tecla con deteccion de flanco, guardo dos booleans:
    // su estado AHORA y su estado del frame ANTERIOR. El "just pressed"
    // es ahora && !antes.
    private boolean spaceAhora,   spaceAntes;     // SPACE → P1 salto
    private boolean wAhora,       wAntes;         // W     → P2 salto
    private boolean rAhora,       rAntes;         // R     → reiniciar
    private boolean arribaAhora,  arribaAntes;    // ↑     → menu sube
    private boolean abajoAhora,   abajoAntes;     // ↓     → menu baja
    private boolean enterAhora,   enterAntes;     // ENTER → confirmar

    public InputManager(long window) {
        this.window = window;
    }

    /**
     * Refresca el estado del teclado para este frame. Antes de leer
     * cada tecla, "rota" su valor: lo que era ahora pasa a antes, y se
     * vuelve a consultar GLFW para llenar ahora.
     *
     * Llamar UNA vez al inicio del frame.
     */
    public void poll() {
        spaceAntes  = spaceAhora;
        spaceAhora  = pressed(GLFW.GLFW_KEY_SPACE);

        wAntes      = wAhora;
        wAhora      = pressed(GLFW.GLFW_KEY_W);

        rAntes      = rAhora;
        rAhora      = pressed(GLFW.GLFW_KEY_R);

        arribaAntes = arribaAhora;
        arribaAhora = pressed(GLFW.GLFW_KEY_UP);

        abajoAntes  = abajoAhora;
        abajoAhora  = pressed(GLFW.GLFW_KEY_DOWN);

        enterAntes  = enterAhora;
        enterAhora  = pressed(GLFW.GLFW_KEY_ENTER);
    }

    /** Salto del jugador 1 (SPACE). True solo en el frame del flanco. */
    public boolean isSaltoP1()      { return spaceAhora  && !spaceAntes;  }
    /** Salto del jugador 2 (W). True solo en el frame del flanco. */
    public boolean isSaltoP2()      { return wAhora      && !wAntes;      }
    /** Salto del jugador 3 (flecha ARRIBA). True solo en el frame del flanco.
     *  Comparte tecla con isFlechaArriba(): no hay conflicto porque cada
     *  estado del juego solo escucha las teclas que le competen (P3 en
     *  JUGANDO, flechas de menu en MENU). */
    public boolean isSaltoP3()      { return arribaAhora && !arribaAntes; }
    /** Tecla R: vuelve al menu (en game over). */
    public boolean isR()            { return rAhora      && !rAntes;      }
    /** Flecha arriba (menu): cambia opcion seleccionada. */
    public boolean isFlechaArriba() { return arribaAhora && !arribaAntes; }
    /** Flecha abajo (menu): cambia opcion seleccionada. */
    public boolean isFlechaAbajo()  { return abajoAhora  && !abajoAntes;  }
    /** ENTER (menu): confirma opcion y arranca el juego. */
    public boolean isEnter()        { return enterAhora  && !enterAntes;  }

    /**
     * ESC NO necesita deteccion de flanco: lo unico que hacemos es
     * pedirle a la ventana que se cierre. Que se "dispare" varios
     * frames seguidos no causa ningun problema.
     */
    public boolean exitRequested() {
        return pressed(GLFW.GLFW_KEY_ESCAPE);
    }

    /** Helper minimo para no escribir 6 veces glfwGetKey == GLFW_PRESS. */
    private boolean pressed(int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }
}
