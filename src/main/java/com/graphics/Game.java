package com.graphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


public class Game {

    /** Estados de alto nivel del juego. */
    public enum EstadoJuego { MENU, JUGANDO, GAME_OVER }

    /** Tiempo BASE entre tuberias (nivel 1). */
    private static final float TIEMPO_ENTRE_TUBERIAS = 1.5f;
    /** Posicion x donde "nacen" las tuberias. */
    private static final float X_SPAWN_PIPE = 1.2f;

    // === Progresion de dificultad (Fase 3 / R3) ===
    private static final int   PUNTOS_POR_NIVEL        = 3;
    private static final int   NIVEL_MAX               = 6;
    private static final float INCREMENTO_VELOCIDAD    = 0.10f;
    private static final float REDUCCION_TIEMPO_ENTRE  = 0.12f;

    // === Mejoras visuales (Fase 4 / R4) ===
    private static final float[] CIELO_TOP = {0.40f, 0.68f, 0.95f};
    private static final float[] CIELO_BOT = {0.95f, 0.78f, 0.55f};
    private static final float[] TIERRA  = {0.42f, 0.32f, 0.18f};
    private static final float[] PASTO   = {0.30f, 0.55f, 0.22f};

    // === Configuracion del jugador 1 (amarillo) ===
    private static final float P1_X = -0.45f;
    private static final float[] P1_CUERPO = {0.98f, 0.85f, 0.20f};
    private static final float[] P1_COLA   = {0.85f, 0.65f, 0.10f};
    private static final float[] P1_ALA    = {0.80f, 0.60f, 0.10f};
    private static final float[] P1_PICO   = {0.95f, 0.55f, 0.10f};

    // === Configuracion del jugador 2 (celeste / azul) ===
    private static final float P2_X = -0.20f;
    private static final float[] P2_CUERPO = {0.30f, 0.65f, 0.95f};
    private static final float[] P2_COLA   = {0.15f, 0.45f, 0.80f};
    private static final float[] P2_ALA    = {0.10f, 0.35f, 0.65f};
    private static final float[] P2_PICO   = {0.95f, 0.65f, 0.10f}; // naranja

    
    private static final float P3_X = -0.15f;
    private static final float[] P3_CUERPO = {0.30f, 0.85f, 0.40f};
    private static final float[] P3_COLA   = {0.20f, 0.65f, 0.25f};
    private static final float[] P3_ALA    = {0.15f, 0.50f, 0.20f};
    private static final float[] P3_PICO   = {0.95f, 0.55f, 0.10f}; // naranja

    /** Cantidad MAXIMA de jugadores soportados (centralizada). */
    private static final int MAX_JUGADORES = 3;

    /** Puntos para ganar: el primer jugador que llegue corta la partida. */
    private static final int PUNTOS_PARA_GANAR = 5;

    // === Estado del mundo ===
    private final List<Bird> birds = new ArrayList<>();
    private final List<Pipe> pipes;
    private final List<Cloud> clouds;
    private final Random random;

    // === Puntajes (uno por jugador, indices alineados con birds) ===
    private final int[] puntajes = new int[MAX_JUGADORES];

    private float timerSpawn;
    /** Tiempo acumulado desde el arranque de la app. Avanza siempre. */
    private float tiempoTotal;
    /** Cronometro de la partida. Solo corre en estado JUGANDO. */
    private float tiempoPartida;

    // === Estado de la maquina de estados ===
    private EstadoJuego estado;
    /** Cuantos jugadores tiene la partida actual: 1, 2*/
    private int modoJugadores;
    /** Opcion del menu actualmente resaltada: 0 = "1 JUGADOR", 1 = "2 JUGADORES", 2 = "3 JUGADORES". */
    private int opcionMenu;

    public Game() {
        this.pipes  = new ArrayList<>();
        this.clouds = new ArrayList<>();
        this.random = new Random();

        clouds.add(new Cloud(-0.6f, 0.65f, 1.0f));
        clouds.add(new Cloud( 0.0f, 0.80f, 0.8f));
        clouds.add(new Cloud( 0.7f, 0.55f, 1.2f));
        clouds.add(new Cloud( 1.1f, 0.78f, 0.9f));

        // Estado inicial: estamos en el menu, todavia sin birds creados.
        // Las nubes y el cielo ya se ven; el jugador elige el modo.
        estado = EstadoJuego.MENU;
        opcionMenu = 0;
        modoJugadores = 1;
    }

    /**
     * Transiciona a JUGANDO: crea los birds segun modoJugadores y
     * resetea todo lo del mundo (tuberias, puntajes, cronometro).
     * El tiempoTotal NO se toca — sigue avanzando para que las
     * animaciones (nubes, parpadeo) tengan continuidad.
     */
    private void empezarPartida() {
        birds.clear();
        birds.add(new Bird(P1_X, P1_CUERPO, P1_COLA, P1_ALA, P1_PICO));
        if (modoJugadores >= 2) {
            birds.add(new Bird(P2_X, P2_CUERPO, P2_COLA, P2_ALA, P2_PICO));
        }
        if (modoJugadores >= 3) {
            birds.add(new Bird(P3_X, P3_CUERPO, P3_COLA, P3_ALA, P3_PICO));
        }
        for (int i = 0; i < puntajes.length; i++) {
            puntajes[i] = 0;
        }
        pipes.clear();
        timerSpawn = 0.0f;
        tiempoPartida = 0.0f;
        estado = EstadoJuego.JUGANDO;
    }

    /**
     * Transiciona a MENU: limpia birds y tuberias para que el menu se
     * vea con un fondo limpio (solo cielo + nubes + suelo). El
     * cronometro y los puntajes los preserva el flujo natural — se
     * resetean en empezarPartida() cuando el jugador arranca de nuevo.
     */
    private void irAMenu() {
        birds.clear();
        pipes.clear();
        estado = EstadoJuego.MENU;
    }

    /**
     * Despacho de input segun el estado. El switch garantiza que cada
     * estado tiene su handler dedicado y que no se ejecuta logica del
     * estado equivocado (ej: que SPACE haga saltar mientras estoy en
     * el menu).
     */
    public void procesarInput(InputManager input) {
        switch (estado) {
            case MENU      -> procesarInputMenu(input);
            case JUGANDO   -> procesarInputJugando(input);
            case GAME_OVER -> procesarInputGameOver(input);
        }
    }

    private void procesarInputMenu(InputManager input) {
        // 3 opciones (0=1P, 1=2P, 2=3P). Flechas navegan ciclicamente.
        if (input.isFlechaArriba()) {
            opcionMenu = (opcionMenu + 2) % 3;
        }
        if (input.isFlechaAbajo()) {
            opcionMenu = (opcionMenu + 1) % 3;
        }
        if (input.isEnter()) {
            modoJugadores = opcionMenu + 1;
            empezarPartida();
        }
    }

    private void procesarInputJugando(InputManager input) {
        // Bird.saltar() ya se autoprotege: si el bird esta muerto,
        // hace early return. Asi puedo llamar siempre sin chequear vivo.
        if (input.isSaltoP1() && !birds.isEmpty()) {
            birds.get(0).saltar();
        }
        if (input.isSaltoP2() && birds.size() >= 2) {
            birds.get(1).saltar();
        }
        if (input.isSaltoP3() && birds.size() >= 3) {
            birds.get(2).saltar();
        }
    }

    private void procesarInputGameOver(InputManager input) {
        if (input.isR()) {
            irAMenu();
        }
    }

    /**
     * Avanza la simulacion un dt:
     *   - tiempoTotal y nubes corren SIEMPRE.
     *   - El resto (fisica, tuberias, cronometro) corre SOLO en JUGANDO.
     *   - Si en JUGANDO mueren todos los birds, pasa a GAME_OVER.
     *   - Si algun jugador alcanza PUNTOS_PARA_GANAR, pasa a GAME_OVER
     *     (el primero que llega gana).
     */
    public void actualizar(float dt) {
        tiempoTotal += dt;
        for (Cloud c : clouds) {
            c.actualizar(dt);
        }

        if (estado != EstadoJuego.JUGANDO) {
            return;
        }

        tiempoPartida += dt;

        // Cada Bird se actualiza solo (si esta muerto, hace nada). Si
        // toca un limite del mundo se autoflagea como muerto.
        for (Bird b : birds) {
            b.actualizar(dt);
        }

        if (todosMuertos()) {
            estado = EstadoJuego.GAME_OVER;
            return;
        }

        // Spawn y avance de tuberias.
        timerSpawn += dt;
        if (timerSpawn >= tiempoEntreActual()) {
            timerSpawn = 0.0f;
            spawnPipe();
        }

        Iterator<Pipe> it = pipes.iterator();
        while (it.hasNext()) {
            Pipe p = it.next();
            p.actualizar(dt, velocidadActual());

            // Por cada bird vivo: scoring + colision INDEPENDIENTES.
            // Los birds muertos se saltean — ya no suman ni mueren mas.
            for (int i = 0; i < birds.size(); i++) {
                Bird b = birds.get(i);
                if (!b.isVivo()) continue;

                if (!p.estaPuntuada(i) && p.yaPaso(b.getX())) {
                    p.marcarPuntuada(i);
                    puntajes[i]++;
                }

                if (colisiona(b, p)) {
                    b.matar();
                }
            }

            if (p.estaFueraDePantalla()) {
                it.remove();
            }
        }

        // Re-chequeo: si por la colision con tuberias murieron todos,
        // o si alguien alcanzo el puntaje de victoria en este frame.
        if (todosMuertos() || alguienGano()) {
            estado = EstadoJuego.GAME_OVER;
        }
    }

    /** True si NO queda ningun bird vivo (game over). */
    private boolean todosMuertos() {
        for (Bird b : birds) {
            if (b.isVivo()) return false;
        }
        return true;
    }

    /** True si algun jugador alcanzo el puntaje para ganar. */
    private boolean alguienGano() {
        for (int i = 0; i < modoJugadores; i++) {
            if (puntajes[i] >= PUNTOS_PARA_GANAR) return true;
        }
        return false;
    }

    /**
     * Render del frame. El ORDEN importa porque no hay z-buffer:
     *   1) cielo, 2) nubes, 3) suelo, 4) tuberias, 5) birds,
     *   6) pantalla del estado actual (HUD / menu / game over).
     */
    public void render(Renderer r) {
        r.beginFrame();

        r.dibujarFondoDegradado(
            CIELO_TOP[0], CIELO_TOP[1], CIELO_TOP[2],
            CIELO_BOT[0], CIELO_BOT[1], CIELO_BOT[2]);
        
        for (Cloud c : clouds) c.dibujar(r);

        r.dibujarRect(0.0f, -0.94f, 2.0f, 0.12f,
                      TIERRA[0], TIERRA[1], TIERRA[2]);
        r.dibujarRect(0.0f, -0.87f, 2.0f, 0.02f,
                      PASTO[0],  PASTO[1],  PASTO[2]);

        for (Pipe p : pipes) p.dibujar(r);
        for (Bird b : birds) b.dibujar(r, tiempoTotal);

        switch (estado) {
            case JUGANDO   -> renderHUD(r);
            case MENU      -> renderMenu(r);
            case GAME_OVER -> renderGameOver(r);
        }
    }

    private void spawnPipe() {
        float gapCentro = Pipe.GAP_MIN_CENTRO
                + random.nextFloat() * (Pipe.GAP_MAX_CENTRO - Pipe.GAP_MIN_CENTRO);
        pipes.add(new Pipe(X_SPAWN_PIPE, gapCentro));
    }

    private boolean colisiona(Bird b, Pipe p) {
        float bX = b.getX();
        float birdLeft   = bX        - Bird.ANCHO * 0.5f;
        float birdRight  = bX        + Bird.ANCHO * 0.5f;
        float birdBottom = b.getY()  - Bird.ALTO  * 0.5f;
        float birdTop    = b.getY()  + Bird.ALTO  * 0.5f;

        float pipeLeft   = p.getX() - Pipe.ANCHO * 0.5f;
        float pipeRight  = p.getX() + Pipe.ANCHO * 0.5f;

        boolean overlapX = birdRight > pipeLeft && birdLeft < pipeRight;
        if (!overlapX) return false;

        float gapTop    = p.getGapCentroY() + Pipe.GAP_ALTO * 0.5f;
        float gapBottom = p.getGapCentroY() - Pipe.GAP_ALTO * 0.5f;
        return birdTop > gapTop || birdBottom < gapBottom;
    }

    // -------------------------------------------------------------
    // HUD durante la partida (modo 1P o 2P)
    // -------------------------------------------------------------

    private void renderHUD(Renderer r) {
        switch (modoJugadores) {
            case 1  -> renderHUD1P(r);
            case 2  -> renderHUD2P(r);
            default -> renderHUD3P(r);
        }
    }

    private void renderHUD1P(Renderer r) {
        float escala = 0.04f;
        float yTop   = 0.97f;
        float[] blanco = {1.0f, 1.0f, 1.0f};

        String txtPuntos = "PUNTOS: " + puntajes[0];
        String txtTiempo = "TIEMPO: " + getTiempoFormateado();
        String txtNivel  = "NIVEL: "  + getNivel();

        TextRenderer.dibujarTexto(r, txtPuntos, -0.97f, yTop, escala, blanco);

        float anchoT = TextRenderer.anchoTexto(txtTiempo, escala);
        TextRenderer.dibujarTexto(r, txtTiempo,
                                  -anchoT * 0.5f, yTop, escala, blanco);

        float anchoN = TextRenderer.anchoTexto(txtNivel, escala);
        TextRenderer.dibujarTexto(r, txtNivel,
                                  0.97f - anchoN, yTop, escala, blanco);
    }

    /**
     * HUD modo 2P en DOS LINEAS:
     *   linea 1: P1 (izq, amarillo) - TIEMPO (centro, blanco) - P2 (der, celeste)
     *   linea 2:                       NIVEL  (centro, blanco)
     * Los colores hacen evidente cual columna mira cada jugador.
     */
    private void renderHUD2P(Renderer r) {
        float escala = 0.04f;
        float yTopL1 = 0.97f;
        float yTopL2 = 0.90f;
        float[] blanco = {1.0f, 1.0f, 1.0f};

        String txtP1     = "P1: " + puntajes[0];
        String txtTiempo = "TIEMPO: " + getTiempoFormateado();
        String txtP2     = "P2: " + puntajes[1];
        String txtNivel  = "NIVEL: " + getNivel();

        // Linea 1.
        TextRenderer.dibujarTexto(r, txtP1, -0.97f, yTopL1, escala, P1_CUERPO);

        float anchoT = TextRenderer.anchoTexto(txtTiempo, escala);
        TextRenderer.dibujarTexto(r, txtTiempo,
                                  -anchoT * 0.5f, yTopL1, escala, blanco);

        float anchoP2 = TextRenderer.anchoTexto(txtP2, escala);
        TextRenderer.dibujarTexto(r, txtP2,
                                  0.97f - anchoP2, yTopL1, escala, P2_CUERPO);

        // Linea 2 (NIVEL centrado debajo del TIEMPO).
        float anchoN = TextRenderer.anchoTexto(txtNivel, escala);
        TextRenderer.dibujarTexto(r, txtNivel,
                                  -anchoN * 0.5f, yTopL2, escala, blanco);
    }

    
    private void renderHUD3P(Renderer r) {
        float escala = 0.04f;
        float yTopL1 = 0.97f;
        float yTopL2 = 0.90f;
        float[] blanco = {1.0f, 1.0f, 1.0f};

        String txtP1     = "P1: " + puntajes[0];
        String txtTiempo = "TIEMPO: " + getTiempoFormateado();
        String txtP2     = "P2: " + puntajes[1];
        String txtP3     = "P3: " + puntajes[2];
        String txtNivel  = "NIVEL: " + getNivel();

        // Linea 1.
        TextRenderer.dibujarTexto(r, txtP1, -0.97f, yTopL1, escala, P1_CUERPO);

        float anchoT = TextRenderer.anchoTexto(txtTiempo, escala);
        TextRenderer.dibujarTexto(r, txtTiempo,
                                  -anchoT * 0.5f, yTopL1, escala, blanco);

        float anchoP2 = TextRenderer.anchoTexto(txtP2, escala);
        TextRenderer.dibujarTexto(r, txtP2,
                                  0.97f - anchoP2, yTopL1, escala, P2_CUERPO);

        // Linea 2: P3 a la izquierda, NIVEL a la derecha.
        TextRenderer.dibujarTexto(r, txtP3, -0.97f, yTopL2, escala, P3_CUERPO);

        float anchoN = TextRenderer.anchoTexto(txtNivel, escala);
        TextRenderer.dibujarTexto(r, txtNivel,
                                  0.97f - anchoN, yTopL2, escala, blanco);
    }

    // -------------------------------------------------------------
    // Pantalla de menu (Sub-fase 5.2)
    // -------------------------------------------------------------

    private void renderMenu(Renderer r) {
        // Overlay translucido (las nubes detras siguen visibles).
        r.dibujarRectAlpha(0.0f, 0.0f, 2.0f, 2.0f,
                           0.0f, 0.0f, 0.0f, 0.50f);

        // Tarjeta con marco doble (mismo estilo que game over).
        float anchoTarj = 1.20f, altoTarj = 1.30f;
        float[] marco = {0.20f, 0.15f, 0.10f};
        float[] crema = {0.96f, 0.92f, 0.80f};
        r.dibujarRect(0.0f, 0.0f, anchoTarj,         altoTarj,         marco[0], marco[1], marco[2]);
        r.dibujarRect(0.0f, 0.0f, anchoTarj - 0.04f, altoTarj - 0.04f, crema[0], crema[1], crema[2]);
        r.dibujarRect(0.0f, 0.0f, anchoTarj - 0.10f, altoTarj - 0.10f, marco[0], marco[1], marco[2]);
        r.dibujarRect(0.0f, 0.0f, anchoTarj - 0.12f, altoTarj - 0.12f, crema[0], crema[1], crema[2]);

        // Titulo grande "FLAPPY BIRD" amarillo.
        float[] amarillo = {0.95f, 0.75f, 0.05f};
        String titulo = "FLAPPY BIRD";
        float escTit = 0.10f;
        float anchoTit = TextRenderer.anchoTexto(titulo, escTit);
        TextRenderer.dibujarTexto(r, titulo,
                                  -anchoTit * 0.5f, 0.50f, escTit, amarillo);

        float[] separador = {0.40f, 0.30f, 0.20f};
        r.dibujarRect(0.0f, 0.36f, 1.00f, 0.005f,
                      separador[0], separador[1], separador[2]);

        // "MODO DE JUEGO:" subtitulo.
        float[] negro = {0.10f, 0.10f, 0.10f};
        String txtModo = "MODO DE JUEGO:";
        float escModo  = 0.04f;
        float anchoModo = TextRenderer.anchoTexto(txtModo, escModo);
        TextRenderer.dibujarTexto(r, txtModo,
                                  -anchoModo * 0.5f, 0.24f, escModo, negro);

        // Las tres opciones, una resaltada.
        renderOpcionMenu(r, "1 JUGADOR",    0.14f, opcionMenu == 0);
        renderOpcionMenu(r, "2 JUGADORES",  0.02f, opcionMenu == 1);
        renderOpcionMenu(r, "3 JUGADORES", -0.10f, opcionMenu == 2);

        // Separador inferior.
        r.dibujarRect(0.0f, -0.22f, 1.00f, 0.005f,
                      separador[0], separador[1], separador[2]);

        // Ayuda de controles.
        float escHelp = 0.035f;
        renderTextoCentrado(r, "FLECHAS PARA ELEGIR", -0.32f, escHelp, negro);
        renderTextoCentrado(r, "ENTER PARA EMPEZAR",  -0.44f, escHelp, negro);
    }

    /**
     * Dibuja una opcion del menu. Si esta seleccionada, va sobre un
     * rectangulo amarillo con texto negro. Si no, texto gris-oscuro
     * (NOTA: el spec original pedia texto blanco; lo cambio a gris
     * oscuro porque blanco sobre crema es ilegible. La separacion
     * visual ya la da el rectangulo amarillo del seleccionado.)
     */
    private void renderOpcionMenu(Renderer r, String texto, float yTop,
                                  boolean seleccionada) {
        float esc = 0.06f;
        float anchoTxt = TextRenderer.anchoTexto(texto, esc);
        float xLeft = -anchoTxt * 0.5f;
        float[] negro = {0.10f, 0.10f, 0.10f};

        if (seleccionada) {
            // Fondo amarillo: ancho un toque mayor que el texto, alto un
            // poco mayor que la altura de la letra (le da aire arriba/abajo).
            float[] amarillo = {0.95f, 0.75f, 0.05f};
            float centroY  = yTop - esc * 0.5f;
            float anchoBg  = anchoTxt + 0.10f;
            float altoBg   = esc * 1.4f;
            r.dibujarRect(0.0f, centroY, anchoBg, altoBg,
                          amarillo[0], amarillo[1], amarillo[2]);
            TextRenderer.dibujarTexto(r, texto, xLeft, yTop, esc, negro);
        } else {
            float[] grisOscuro = {0.45f, 0.40f, 0.30f};
            TextRenderer.dibujarTexto(r, texto, xLeft, yTop, esc, grisOscuro);
        }
    }

    // -------------------------------------------------------------
    // Pantalla de game over (modo 1P o 2P)
    // -------------------------------------------------------------

    private void renderGameOver(Renderer r) {
        r.dibujarRectAlpha(0.0f, 0.0f, 2.0f, 2.0f,
                           0.0f, 0.0f, 0.0f, 0.55f);

        float anchoTarj = 1.20f, altoTarj = 1.40f;
        float[] marco = {0.20f, 0.15f, 0.10f};
        float[] crema = {0.96f, 0.92f, 0.80f};
        r.dibujarRect(0.0f, 0.0f, anchoTarj,         altoTarj,         marco[0], marco[1], marco[2]);
        r.dibujarRect(0.0f, 0.0f, anchoTarj - 0.04f, altoTarj - 0.04f, crema[0], crema[1], crema[2]);
        r.dibujarRect(0.0f, 0.0f, anchoTarj - 0.10f, altoTarj - 0.10f, marco[0], marco[1], marco[2]);
        r.dibujarRect(0.0f, 0.0f, anchoTarj - 0.12f, altoTarj - 0.12f, crema[0], crema[1], crema[2]);

        // Titulo "GAME OVER" rojo grande.
        float[] rojo = {0.85f, 0.10f, 0.10f};
        String titulo = "GAME OVER";
        float escTit = 0.09f;
        float anchoTit = TextRenderer.anchoTexto(titulo, escTit);
        TextRenderer.dibujarTexto(r, titulo,
                                  -anchoTit * 0.5f, 0.55f, escTit, rojo);

        float[] separador = {0.40f, 0.30f, 0.20f};
        r.dibujarRect(0.0f, 0.40f, 1.00f, 0.005f,
                      separador[0], separador[1], separador[2]);

        switch (modoJugadores) {
            case 1  -> renderGameOverStats1P(r);
            case 2  -> renderGameOverStats2P(r);
            default -> renderGameOverStats3P(r);
        }

        r.dibujarRect(0.0f, -0.50f, 1.00f, 0.005f,
                      separador[0], separador[1], separador[2]);

        float[] negro = {0.10f, 0.10f, 0.10f};
        renderTextoCentrado(r, "PRESIONA R PARA VOLVER AL MENU",
                            -0.60f, 0.035f, negro);
    }

    private void renderGameOverStats1P(Renderer r) {
        float[] negro = {0.10f, 0.10f, 0.10f};
        float esc = 0.05f;
        float xLeft = -0.40f;
        float ySpacing = 0.13f;
        float yStart = 0.28f;

        TextRenderer.dibujarTexto(r, "PUNTOS:    " + puntajes[0],
                                  xLeft, yStart, esc, negro);
        TextRenderer.dibujarTexto(r, "TIEMPO: " + getTiempoFormateado(),
                                  xLeft, yStart - ySpacing, esc, negro);
        TextRenderer.dibujarTexto(r, "NIVEL:     " + getNivel(),
                                  xLeft, yStart - 2 * ySpacing, esc, negro);
    }

    /**
     * Stats 2P: muestra puntajes con colores de cada jugador, una linea
     * con el GANADOR (o EMPATE) y los datos compartidos (tiempo, nivel).
     */
    private void renderGameOverStats2P(Renderer r) {
        float[] negro = {0.10f, 0.10f, 0.10f};
        float esc = 0.05f;
        float xLeft = -0.50f;
        float ySpacing = 0.10f;
        float yStart = 0.30f;

        // Puntajes de cada jugador con su color.
        TextRenderer.dibujarTexto(r, "JUGADOR 1: " + puntajes[0],
                                  xLeft, yStart, esc, P1_CUERPO);
        TextRenderer.dibujarTexto(r, "JUGADOR 2: " + puntajes[1],
                                  xLeft, yStart - ySpacing, esc, P2_CUERPO);

        // Ganador o empate, centrado, en su color.
        String ganadorTxt;
        float[] colorGanador;
        if (puntajes[0] > puntajes[1]) {
            ganadorTxt = "GANADOR: JUGADOR 1";
            colorGanador = P1_CUERPO;
        } else if (puntajes[1] > puntajes[0]) {
            ganadorTxt = "GANADOR: JUGADOR 2";
            colorGanador = P2_CUERPO;
        } else {
            ganadorTxt = "EMPATE";
            colorGanador = negro;
        }
        renderTextoCentrado(r, ganadorTxt, yStart - 2.5f * ySpacing,
                            esc, colorGanador);

        // Tiempo y nivel (compartidos).
        TextRenderer.dibujarTexto(r, "TIEMPO: " + getTiempoFormateado(),
                                  xLeft, yStart - 4 * ySpacing, esc, negro);
        TextRenderer.dibujarTexto(r, "NIVEL:  " + getNivel(),
                                  xLeft, yStart - 5 * ySpacing, esc, negro);
    }

    /**
     * Stats 3P: muestra puntajes con colores de cada jugador, una linea
     * con el GANADOR (unico maximo) o EMPATE (si hay maxima compartido),
     * y los datos compartidos (tiempo, nivel).
     */
    private void renderGameOverStats3P(Renderer r) {
        float[] negro = {0.10f, 0.10f, 0.10f};
        float esc = 0.045f;
        float xLeft = -0.50f;
        float ySpacing = 0.09f;
        float yStart = 0.32f;

        // Puntajes de cada jugador con su color.
        TextRenderer.dibujarTexto(r, "JUGADOR 1: " + puntajes[0],
                                  xLeft, yStart, esc, P1_CUERPO);
        TextRenderer.dibujarTexto(r, "JUGADOR 2: " + puntajes[1],
                                  xLeft, yStart - ySpacing, esc, P2_CUERPO);
        TextRenderer.dibujarTexto(r, "JUGADOR 3: " + puntajes[2],
                                  xLeft, yStart - 2 * ySpacing, esc, P3_CUERPO);

        // Ganador: idx del unico maximo. Si hay empate al tope, EMPATE.
        int max = puntajes[0];
        for (int i = 1; i < 3; i++) {
            if (puntajes[i] > max) max = puntajes[i];
        }
        int countMax = 0, idxGanador = -1;
        for (int i = 0; i < 3; i++) {
            if (puntajes[i] == max) { countMax++; idxGanador = i; }
        }
        String ganadorTxt;
        float[] colorGanador;
        if (countMax == 1) {
            ganadorTxt = "GANADOR: JUGADOR " + (idxGanador + 1);
            colorGanador = switch (idxGanador) {
                case 0 -> P1_CUERPO;
                case 1 -> P2_CUERPO;
                default -> P3_CUERPO;
            };
        } else {
            ganadorTxt = "EMPATE";
            colorGanador = negro;
        }
        renderTextoCentrado(r, ganadorTxt, yStart - 3.5f * ySpacing,
                            esc, colorGanador);

        // Tiempo y nivel (compartidos).
        TextRenderer.dibujarTexto(r, "TIEMPO: " + getTiempoFormateado(),
                                  xLeft, yStart - 5 * ySpacing, esc, negro);
        TextRenderer.dibujarTexto(r, "NIVEL:  " + getNivel(),
                                  xLeft, yStart - 6 * ySpacing, esc, negro);
    }

    /** Helper: dibuja un texto CENTRADO horizontalmente en x=0. */
    private void renderTextoCentrado(Renderer r, String txt,
                                     float yTop, float esc, float[] color) {
        float ancho = TextRenderer.anchoTexto(txt, esc);
        TextRenderer.dibujarTexto(r, txt, -ancho * 0.5f, yTop, esc, color);
    }

    // -------------------------------------------------------------
    // Progresion de dificultad y getters
    // -------------------------------------------------------------

    /**
     * Nivel actual: lo dispara MAX(puntajes). En 1P es directo el
     * puntaje del unico bird; en 2P es el max para que el ritmo suba
     * en cuanto cualquiera destaque (presion competitiva).
     */
    public int getNivel() {
        int max = 0;
        for (int p : puntajes) {
            if (p > max) max = p;
        }
        int nivel = 1 + (max / PUNTOS_POR_NIVEL);
        return Math.min(nivel, NIVEL_MAX);
    }

    private float velocidadActual() {
        return Pipe.VELOCIDAD + (getNivel() - 1) * INCREMENTO_VELOCIDAD;
    }

    private float tiempoEntreActual() {
        return TIEMPO_ENTRE_TUBERIAS - (getNivel() - 1) * REDUCCION_TIEMPO_ENTRE;
    }

    public String getTiempoFormateado() {
        int totalSeg = (int) tiempoPartida;
        int min = totalSeg / 60;
        int seg = totalSeg % 60;
        return String.format("%02d:%02d", min, seg);
    }

    // === Getters publicos ===
    public EstadoJuego getEstado()        { return estado; }
    public int getModoJugadores()         { return modoJugadores; }
    public int getPuntaje(int idxJugador) { return puntajes[idxJugador]; }
}
