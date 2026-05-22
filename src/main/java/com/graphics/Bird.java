package com.graphics;

public class Bird {

    // === Constantes COMPARTIDAS por todos los pajaros ===
    public static final float ANCHO = 0.10f;
    public static final float ALTO  = 0.10f;
    public static final float GRAVEDAD = -1.9f;
    public static final float IMPULSO_SALTO = 0.85f;
    public static final float VELOCIDAD_MAX_CAIDA = -1.8f;

    // Limites del mundo. Coinciden con la cima del suelo dibujado.
    public static final float LIMITE_INFERIOR = -0.86f;
    public static final float LIMITE_SUPERIOR =  1.0f;

    // Inclinacion del cuerpo segun velocidad vertical.
    private static final float INCLINACION_FACTOR = 0.7f;
    private static final float INCLINACION_LIMITE = 0.6f;

    // Animacion del ala (frecuencia y amplitud).
    private static final float ALA_FREC = 8.0f;
    private static final float ALA_AMP  = 0.6f;

    // === Datos POR INSTANCIA ===

    /** Posicion horizontal fija de ESTE pajaro en NDC. */
    private final float x;

    // Paleta del pajaro (4 partes coloreables). El ojo (blanco) y la
    // pupila (negra) son universales y se quedan hardcoded en dibujar().
    private final float[] colorCuerpo;
    private final float[] colorCola;
    private final float[] colorAla;
    private final float[] colorPico;

    // === Estado mutable ===
    private float y;
    private float velY;

    /** True mientras este pajaro este "en juego". False = murio y se
     *  congela en su ultima posicion (no se mueve mas, parpadea). */
    private boolean vivo;

    // Tiempo total acumulado (en segundos). Lo usa la animacion del ala
    // y se congela junto con la fisica cuando muere — apropiado: un
    // pajaro muerto no aletea.
    private float tiempo;
    // Marca para resetear la fase del aleteo en cada salto.
    private float tiempoUltimoSalto;

    // Buffer reutilizable para evitar alocar arrays en proyectarLocal.
    private final float[] mundoTmp = new float[2];

    public Bird(float x,
                float[] colorCuerpo, float[] colorCola,
                float[] colorAla, float[] colorPico) {
        this.x = x;
        this.colorCuerpo = colorCuerpo;
        this.colorCola   = colorCola;
        this.colorAla    = colorAla;
        this.colorPico   = colorPico;
        reset();
    }

    /**
     * Reinicia este pajaro a "centro de pantalla, quieto, vivo". Lo
     * llama el constructor y tambien Game.reset() cuando el jugador
     * pide reiniciar la partida.
     */
    public void reset() {
        this.y = 0.0f;
        this.velY = 0.0f;
        this.tiempo = 0.0f;
        this.tiempoUltimoSalto = 0.0f;
        this.vivo = true;
    }

    /**
     * Aplica el impulso del salto. Si el pajaro esta muerto, no hace
     * nada — un pajaro muerto NO salta (defensa contra que el jugador
     * mantenga apretada la tecla en game over).
     */
    public void saltar() {
        if (!vivo) {
            return;
        }
        this.velY = IMPULSO_SALTO;
        this.tiempoUltimoSalto = tiempo;
    }

    /**
     * Avance fisico por frame. Si el pajaro ya esta muerto, no hace
     * NADA — la simulacion se congela. Si esta vivo, integra gravedad,
     * actualiza posicion y al final chequea si toco un limite del
     * mundo (techo o suelo). Si lo toco, se autoflagea como muerto.
     *
     * El check de "tope del mundo" es interno al pajaro: nadie de
     * afuera necesita preguntar tocoLimite(). Lo unico que hace falta
     * desde fuera es preguntar isVivo() despues de actualizar.
     */
    public void actualizar(float dt) {
        if (!vivo) {
            return;
        }

        velY += GRAVEDAD * dt;
        if (velY < VELOCIDAD_MAX_CAIDA) {
            velY = VELOCIDAD_MAX_CAIDA;
        }
        y += velY * dt;
        tiempo += dt;

        if (tocoLimite()) {
            vivo = false;
        }
    }

    /**
     * Marca al pajaro como muerto desde AFUERA. La usa Game cuando el
     * pajaro choca contra una tuberia (Game conoce las tuberias, Bird
     * no). Es idempotente: llamar matar() varias veces no rompe nada.
     */
    public void matar() {
        this.vivo = false;
    }

    public boolean isVivo() {
        return vivo;
    }

    /**
     * Detecta techo/suelo. Privado: solo lo usa Bird internamente
     * dentro de actualizar(). Game NO lo llama mas — pregunta isVivo().
     */
    private boolean tocoLimite() {
        float top = y + ALTO * 0.5f;
        float bottom = y - ALTO * 0.5f;
        return top >= LIMITE_SUPERIOR || bottom <= LIMITE_INFERIOR;
    }

    /**
     * Dibuja al pajaro como composicion de primitivas. El parpadeo de
     * "estoy muerto" lo decide el pajaro mismo en base a su campo vivo
     * — antes de la Sub-fase 5.1, Game tenia que pasarle un boolean
     * "parpadeando" como argumento. Ahora la API es mas simple: Game
     * solo le pasa el tiempo total para la animacion.
     */
    public void dibujar(Renderer r, float tiempoTotal) {
        // Si esta muerto, alterna visible/invisible 5 veces por segundo.
        // Uso tiempoTotal (de Game) que avanza siempre, y NO el campo
        // "tiempo" interno — porque ese se congela cuando vivo=false.
        if (!vivo && ((int) (tiempoTotal * 10.0f) % 2 == 0)) {
            return;
        }
        float rot = clamp(velY * INCLINACION_FACTOR,
                          -INCLINACION_LIMITE, +INCLINACION_LIMITE);
        float fase = (tiempo - tiempoUltimoSalto) * ALA_FREC;
        float anguloAla = (float) Math.sin(fase) * ALA_AMP;

        // Cola.
        proyectarLocal(-ANCHO * 0.55f, 0.0f, rot);
        r.dibujarTriangulo(mundoTmp[0], mundoTmp[1],
                0.06f, 0.05f,
                rot + (float) Math.PI,
                colorCola[0], colorCola[1], colorCola[2]);


        // Cuerpo.
        proyectarLocal(0.0f, 0.0f, rot);
        r.dibujarCirculo(mundoTmp[0], mundoTmp[1],
                ANCHO, ALTO * 0.85f,
                rot,
                colorCuerpo[0], colorCuerpo[1], colorCuerpo[2]);
        //Ala
        proyectarLocal(-0.005f, -0.015f, rot);
        r.dibujarTriangulo(mundoTmp[0], mundoTmp[1],
            0.05f, 0.045f,
            rot + anguloAla,
            colorAla[0], colorAla[1], colorAla[2]);        
        // Pico.
        proyectarLocal(ANCHO * 0.50f + 0.005f, 0.005f, rot);
        r.dibujarTriangulo(mundoTmp[0], mundoTmp[1],
                0.05f, 0.035f,
                rot,
                colorPico[0], colorPico[1], colorPico[2]);

        // Ojo (blanco, universal).
        proyectarLocal(0.020f, 0.020f, rot);
        r.dibujarCirculo(mundoTmp[0], mundoTmp[1],
                0.030f, 0.030f, rot,
                1.0f, 1.0f, 1.0f);

        // Pupila (negra, universal).
        proyectarLocal(0.025f, 0.020f, rot);
        r.dibujarCirculo(mundoTmp[0], mundoTmp[1],
                0.014f, 0.014f, rot,
                0.0f, 0.0f, 0.0f);
        
         
    }

    private void proyectarLocal(float lx, float ly, float rot) {
        float c = (float) Math.cos(rot);
        float s = (float) Math.sin(rot);
        mundoTmp[0] = x + lx * c - ly * s;
        mundoTmp[1] = y + lx * s + ly * c;
    }

    private static float clamp(float v, float min, float max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    public float getX()    { return x; }
    public float getY()    { return y; }
    public float getVelY() { return velY; }
}
