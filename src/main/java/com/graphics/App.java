package com.graphics;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

/**
 * Flujo del bucle, frame por frame:
 *   1) calcular dt (segundos transcurridos),
 *   2) input.poll() — leer teclado,
 *   3) chequear ESC para cerrar,
 *   4) game.procesarInput(input) — convertir teclas en acciones,
 *   5) game.actualizar(dt) — fisica, spawn, colisiones, puntaje,
 *   6) actualizarTitulo() — reflejar puntaje/estado,
 *   7) game.render(renderer) — dibujar todo,
 *   8) glfwSwapBuffers + glfwPollEvents — presentar y procesar OS events.
 */
public class App {

    /** Tamano inicial de ventana en pixeles. */
    private static final int ANCHO = 900;
    private static final int ALTO = 700;

    /** Limite superior de dt para evitar saltos grandes si el frame se traba. */
    private static final float DT_MAX = 0.033f;

    private long window;
    private Renderer renderer;
    private InputManager input;
    private Game game;

    public void run() {
        initWindow();
        // El Renderer NECESITA contexto GL valido, por eso se inicia
        // despues de glfwMakeContextCurrent + GL.createCapabilities,
        // que ya pasaron dentro de initWindow().
        renderer = new Renderer();
        renderer.init();
        input = new InputManager(window);
        game = new Game();
        actualizarTitulo();

        loop();
        cleanup();
    }

    /**
     * Crea la ventana GLFW y el contexto OpenGL 3.3 core.
     * Notar el orden:
     *   1) glfwInit
     *   2) hints (config del contexto que queremos)
     *   3) glfwCreateWindow
     *   4) glfwMakeContextCurrent
     *   5) GL.createCapabilities ← carga las funciones GL para esta JVM
     * Si te salteas el paso 5, GL11/GL20/etc lanzan NullPointerException.
     */
    private void initWindow() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("No se pudo iniciar GLFW");
        }

        GLFW.glfwDefaultWindowHints();
        // Empezamos invisible para no ver un flash blanco mientras se
        // termina de configurar el contexto. Despues llamamos a glfwShowWindow.
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(ANCHO, ALTO, "Flappy Bird OpenGL", 0, 0);
        if (window == 0) {
            throw new RuntimeException("No se pudo crear la ventana");
        }

        GLFW.glfwMakeContextCurrent(window);
        // glfwSwapInterval(1) = VSync activado: limita el frame rate
        // a la tasa de refresco del monitor (tipicamente 60fps).
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();

       
        // Seteo el viewport al tamano REAL del framebuffer. En pantallas
        // HiDPI (escalado de Windows al 125% / 150%) el framebuffer puede
        // ser mas grande en pixeles que la ventana en "screen coords",
        // y sin esta llamada el render aparece pegado a una esquina.
        int[] fbW = new int[1], fbH = new int[1];
        GLFW.glfwGetFramebufferSize(window, fbW, fbH);
        GL11.glViewport(0, 0, fbW[0], fbH[0]);

        // Callback de resize: cada vez que el usuario cambia el tamano
        // de la ventana, OpenGL no actualiza el viewport solo. Lo
        // arreglamos con este callback. Sin esto, redimensionar partia
        // el render en mitades.
        GLFW.glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            GL11.glViewport(0, 0, w, h);
        });

       
        // Para los overlays semitransparentes de pantalla de inicio /
        // game over. SRC_ALPHA / ONE_MINUS_SRC_ALPHA es el "alpha
        // blending estandar": el pixel nuevo se mezcla con el de atras
        // segun su alpha. Si alpha=1.0 (caso normal) tapa lo de atras;
        // si alpha=0.5, queda 50/50.
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * El game loop clasico. Sigue corriendo mientras el usuario no haya
     * pedido cerrar la ventana (boton X o glfwSetWindowShouldClose).
     */
    private void loop() {
        float ultimoTiempo = (float) GLFW.glfwGetTime();

        while (!GLFW.glfwWindowShouldClose(window)) {
            // dt = tiempo desde el frame anterior, en segundos.
            float ahora = (float) GLFW.glfwGetTime();
            float dt = ahora - ultimoTiempo;
            ultimoTiempo = ahora;
            if (dt > DT_MAX) {
                dt = DT_MAX;
            }

            // 1) Leer teclado para este frame.
            input.poll();

            // 2) ESC cierra la ventana (lo maneja App, no Game).
            if (input.exitRequested()) {
                GLFW.glfwSetWindowShouldClose(window, true);
            }

            // 3) Pasar el input al Game para que decida acciones.
            game.procesarInput(input);

            // 4) Avanzar la simulacion.
            game.actualizar(dt);

            // 5) Reflejar el estado actual en la barra de titulo.
            actualizarTitulo();

            // 6) Dibujar el frame.
            game.render(renderer);

            // 7) Presentar el frame al sistema operativo y procesar
            //    eventos (teclado, mouse, redimensionado).
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    /**
     * Refresca el titulo segun el estado del juego. Lo hago todo desde
     * App (no desde Game) porque el titulo pertenece a la ventana, y
     * Game no debe conocer GLFW.
     */
    private void actualizarTitulo() {
        // Sub-fase 5.2/5.3: titulo segun la maquina de estados de Game.
        // En MENU no hay puntajes que mostrar; en JUGANDO/GAME_OVER
        // muestro 1 o 2 puntajes segun el modo de juego elegido.
        String base = "Flappy Bird OpenGL";
        Game.EstadoJuego est = game.getEstado();

        switch (est) {
            case MENU -> GLFW.glfwSetWindowTitle(window,
                base + " | MENU - Flechas para elegir, ENTER para empezar");

            case JUGANDO -> {
                String puntajes = switch (game.getModoJugadores()) {
                    case 1  -> " | Puntos: " + game.getPuntaje(0);
                    case 2  -> " | P1: " + game.getPuntaje(0) + " | P2: " + game.getPuntaje(1);
                    default -> " | P1: " + game.getPuntaje(0) + " | P2: " + game.getPuntaje(1)
                                + " | P3: " + game.getPuntaje(2);
                };
                GLFW.glfwSetWindowTitle(window,
                    base + puntajes + " | Nivel: " + game.getNivel());
            }

            case GAME_OVER -> {
                String puntajes = switch (game.getModoJugadores()) {
                    case 1  -> " | Puntos: " + game.getPuntaje(0);
                    case 2  -> " | P1: " + game.getPuntaje(0) + " | P2: " + game.getPuntaje(1);
                    default -> " | P1: " + game.getPuntaje(0) + " | P2: " + game.getPuntaje(1)
                                + " | P3: " + game.getPuntaje(2);
                };
                GLFW.glfwSetWindowTitle(window,
                    base + puntajes + " | GAME OVER - R para volver al menu");
            }
        }
    }

    /** Libera recursos GPU + ventana + GLFW. */
    private void cleanup() {
        renderer.cleanup();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) {
        new App().run();
    }
}
