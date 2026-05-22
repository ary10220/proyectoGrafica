package com.graphics;

/*coordenadas: x es el centro horizontal de la tuberia,
 * y todo el dibujo es en NDC (sistema -1..+1).
 */
public class Pipe {

    // === Constantes propias de las tuberias ===
    public static final float ANCHO = 0.18f;
    // Tamano vertical del hueco por donde pasa el pajaro.
    public static final float GAP_ALTO = 0.48f;
    // Velocidad BASE (nivel 1) con la que las tuberias se mueven hacia
    // la izquierda. Game la usa como punto de partida y la va escalando
    // a medida que sube el nivel (Fase 3). En runtime, la velocidad real
    // se la pasa Game a actualizar(dt, velocidad), no se lee directo.
    public static final float VELOCIDAD = 0.62f;
    // Rango vertical donde puede caer el centro del hueco. Limita la
    // dificultad: si el centro pudiera estar en +0.9 el hueco se iria
    // arriba de la pantalla.
    public static final float GAP_MIN_CENTRO = -0.45f;
    public static final float GAP_MAX_CENTRO = 0.45f;

    // === Estado mutable ===
    // Centro horizontal en NDC. Cambia cada frame (la tuberia avanza).
    private float x;
    // Centro vertical del hueco. Se decide al spawnear y nunca cambia.
    private final float gapCentroY;
    // Bandera POR JUGADOR (Sub-fase 5.3 / R2): la misma tuberia da 1
    // punto a CADA jugador la primera vez que la cruza. Como P1, P2 y
    // P3 estan en X distintas, suelen cruzarla en momentos distintos.
    // usados quedan en false (sin efecto).
    // Java inicializa boolean[] en false por defecto.
    private final boolean[] puntuadaPorJugador = new boolean[3];

    public Pipe(float x, float gapCentroY) {
        this.x = x;
        this.gapCentroY = gapCentroY;
    }

    /**
     * Avance horizontal por frame. dt = segundos transcurridos desde el
     * frame anterior, asi la velocidad esta en "unidades NDC por segundo"
     * y se ve igual en cualquier frame rate.
     *
     * La velocidad la pasa el Game (no se lee de Pipe.VELOCIDAD), porque
     * a partir de la Fase 3 esa velocidad cambia segun el nivel del
     * juego. Asi todas las tuberias en pantalla aceleran juntas cuando
     * sube de nivel — no solo las que se spawnean despues.
     */
    public void actualizar(float dt, float velocidad) {
        x -= velocidad * dt;
    }

    /**
     * True cuando la tuberia salio de la vista por la izquierda.
     * Game usa esto para removerla de la lista y no acumular memoria.
     * Uso -1.3 (no -1.0) para dar un margen y evitar que se note el
     * borrado en el borde.
     */
    public boolean estaFueraDePantalla() {
        return x + ANCHO * 0.5f < -1.3f;
    }

    /**
     * True cuando el borde derecho de la tuberia ya quedo a la izquierda
     * del pajaro. Es el momento exacto para sumar el punto.
     */
    public boolean yaPaso(float birdX) {
        return x + ANCHO * 0.5f < birdX;
    }

    // === Estilo de las tuberias (Fase intermedia 4.5 / Parte B) ===
    // Cada tuberia ahora son varios rectangulos en vez de uno plano:
    //   - cuerpo verde brillante,
    //   - dos bordecitos oscuros laterales (relieve),
    //   - franja clara a la izquierda (highlight de luz),
    //   - "boquilla" mas ancha en el extremo cercano al gap.
    private static final float[] VERDE_BASE  = {0.36f, 0.78f, 0.30f};
    private static final float[] VERDE_OSC   = {0.20f, 0.50f, 0.18f};
    private static final float[] VERDE_CLARO = {0.55f, 0.90f, 0.45f};
    private static final float BORDE_ANCHO     = 0.012f;
    private static final float HIGHLIGHT_ANCHO = 0.018f;
    // Tamano de la boquilla (el "labio" en el extremo cerca del gap).
    private static final float BOQUILLA_ANCHO = ANCHO * 1.15f;
    private static final float BOQUILLA_ALTO  = 0.06f;

    /**
     * Dibuja los dos tramos verdes (superior e inferior) salvo que el
     * tramo no exista (caso borde donde el hueco toca un borde de la
     * pantalla y un tramo queda con altura <= 0). Cada tramo se compone
     * de un cuerpo + boquilla; cada uno se delega al helper
     * dibujarCuerpoTuberia para no repetir la logica de bordes.
     */
    public void dibujar(Renderer r) {
        float gapTop    = gapCentroY + GAP_ALTO * 0.5f;
        float gapBottom = gapCentroY - GAP_ALTO * 0.5f;

        // ----- Tramo SUPERIOR -----
        // Cuerpo: desde gapTop hasta el techo (+1.0).
        float altoSup = 1.0f - gapTop;
        if (altoSup > 0.0f) {
            float yCentroSup = gapTop + altoSup * 0.5f;
            dibujarCuerpoTuberia(r, x, yCentroSup, ANCHO, altoSup);
            // Boquilla en el EXTREMO INFERIOR (la que "mira" al gap).
            float yBoquilla = gapTop + BOQUILLA_ALTO * 0.5f;
            dibujarCuerpoTuberia(r, x, yBoquilla, BOQUILLA_ANCHO, BOQUILLA_ALTO);
        }

        // ----- Tramo INFERIOR -----
        // Cuerpo: desde el piso (-1.0) hasta gapBottom.
        float altoInf = gapBottom + 1.0f;
        if (altoInf > 0.0f) {
            float yCentroInf = -1.0f + altoInf * 0.5f;
            dibujarCuerpoTuberia(r, x, yCentroInf, ANCHO, altoInf);
            // Boquilla en el EXTREMO SUPERIOR (mirando al gap).
            float yBoquilla = gapBottom - BOQUILLA_ALTO * 0.5f;
            dibujarCuerpoTuberia(r, x, yBoquilla, BOQUILLA_ANCHO, BOQUILLA_ALTO);
        }
    }

    /**
     * Helper: dibuja un "tramo" de tuberia (cuerpo + 2 bordes + 1
     * highlight) centrado en (xCentro, yCentro) con el ancho y alto
     * pedidos. La logica es la misma para el cuerpo principal y para
     * la boquilla — solo cambian las dimensiones.
     *
     * Capas (orden de dibujo, ultima encima):
     *   1) cuerpo verde brillante,
     *   2) borde oscuro DERECHO (queda tapado por el highlight si esta del
     *      mismo lado, pero como el highlight va a la izquierda, este se ve),
     *   3) borde oscuro IZQUIERDO,
     *   4) highlight verde claro un poquito a la derecha del borde izq.
     */
    private void dibujarCuerpoTuberia(Renderer r,
                                      float xCentro, float yCentro,
                                      float ancho, float alto) {
        float xIzq = xCentro - ancho * 0.5f;
        float xDer = xCentro + ancho * 0.5f;

        // 1) Cuerpo verde brillante.
        r.dibujarRect(xCentro, yCentro, ancho, alto,
                      VERDE_BASE[0], VERDE_BASE[1], VERDE_BASE[2]);

        // 2) Borde oscuro DERECHO (centrado justo al borde, hacia adentro).
        r.dibujarRect(xDer - BORDE_ANCHO * 0.5f, yCentro, BORDE_ANCHO, alto,
                      VERDE_OSC[0], VERDE_OSC[1], VERDE_OSC[2]);

        // 3) Borde oscuro IZQUIERDO.
        r.dibujarRect(xIzq + BORDE_ANCHO * 0.5f, yCentro, BORDE_ANCHO, alto,
                      VERDE_OSC[0], VERDE_OSC[1], VERDE_OSC[2]);

        // 4) Highlight: franja vertical clara entre el borde izq y el cuerpo.
        float xHL = xIzq + BORDE_ANCHO + HIGHLIGHT_ANCHO * 0.5f;
        r.dibujarRect(xHL, yCentro, HIGHLIGHT_ANCHO, alto,
                      VERDE_CLARO[0], VERDE_CLARO[1], VERDE_CLARO[2]);
    }

    // Getters/setters minimos que necesita Game.
    public float getX() { return x; }
    public float getGapCentroY() { return gapCentroY; }
    /** True si el jugador 'idx' (0=P1, 1=P2, 2=P3) ya cruzo esta tuberia. */
    public boolean estaPuntuada(int idxJugador) {
        return puntuadaPorJugador[idxJugador];
    }

    /** Marca que el jugador 'idx' (0=P1, 1=P2, 2=P3) cruzo esta tuberia. */
    public void marcarPuntuada(int idxJugador) {
        puntuadaPorJugador[idxJugador] = true;
    }
}
