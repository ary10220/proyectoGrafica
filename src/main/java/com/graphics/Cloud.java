package com.graphics;

public class Cloud {
    /**
     * Velocidad horizontal en NDC/seg. ~10 veces mas lento que las
     * tuberias (que van a 0.62 base) para que se note el parallax.
     */
    private static final float VELOCIDAD = 0.05f;

    private float x;
    private final float y;
    /** Multiplicador de tamano para que cada nube se vea distinta. */
    private final float escala;

    public Cloud(float x, float y, float escala) {
        this.x = x;
        this.y = y;
        this.escala = escala;
    }

    /**
     * Avanza la nube. Cuando se va por la izquierda, la "teletransporto"
     * al borde derecho (NDC=+1.3, un toque fuera de la pantalla) para
     * que entre suavemente y siga el ciclo.
     */
    public void actualizar(float dt) {
        x -= VELOCIDAD * dt;
        if (x <-1.3f) {
            x = 1.3f;
        }
    }

    /**
     * Dibuja la nube como tres circulos blancos con un poco de
     * desplazamiento horizontal y vertical. El "esponjado" sale solo
     * por superposicion — no hace falta blending ni transparencia.
     */
    public void dibujar(Renderer r) {
        float blanco = 1.0f;
        // Bola central (la mas grande).
        r.dibujarCirculo(x,                  y + 0.010f * escala,
                         0.14f * escala,     0.10f * escala,
                         0.0f, blanco, blanco, blanco);
        // Bola izquierda.
        r.dibujarCirculo(x -  0.055f * escala, y - 0.010f * escala,
                         0.10f * escala,     0.08f * escala,
                         0.0f, blanco, blanco, blanco);
        // Bola derecha.
        r.dibujarCirculo(x + 0.060f * escala, y - 0.005f * escala,
                         0.09f * escala,     0.07f * escala,
                         0.0f, blanco, blanco, blanco);
    }
}
