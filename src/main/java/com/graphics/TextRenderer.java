package com.graphics;

import java.util.HashMap;
import java.util.Map;

/**

 *   - El alfabeto es CONSTANTE — 39 entradas que nunca cambian — asi que
 *     no tiene sentido tener una instancia con campos mutables.
 *   - Los metodos publicos son funciones puras: input → dibuja → vuelve.
 *     No guardan estado entre llamadas.
 *   - Game lo usa directo: TextRenderer.dibujarTexto(...).
 *
 * Sistema de coordenadas:
 *   - Todo en NDC (-1..+1).
 *   - (x, y) que recibe dibujarTexto es la ESQUINA SUPERIOR IZQUIERDA
 *     del texto.
 *   - escalaLetra es la ALTURA de cada letra (=7 "pixeles del font").
 *   - El ancho de letra sale en proporcion: 5/7 * escalaLetra.
 *   - Entre letras dejo 1 "pixel" de separacion (mismo tamano que un
 *     pixel del bitmap).
 */
public class TextRenderer {

    /** Filas y columnas del bitmap por letra. */
    private static final int FILAS    = 7;
    private static final int COLUMNAS = 5;

    /**
     * Alfabeto: cada letra es un array de 7 strings, cada uno de 5
     * caracteres. '#' = pixel encendido, cualquier otro caracter
     * (espacio o punto) = apagado. Asi se ve la forma de cada letra
     * de un vistazo cuando lees el codigo, lo cual es util para
     * agregar caracteres nuevos despues.
     */
    private static final Map<Character, String[]> ALFABETO = new HashMap<>();
    static {
        // -------- Letras A-Z --------
        ALFABETO.put('A', new String[]{
            " ### ",
            "#   #",
            "#   #",
            "#####",
            "#   #",
            "#   #",
            "#   #"
        });
        ALFABETO.put('B', new String[]{
            "#### ",
            "#   #",
            "#   #",
            "#### ",
            "#   #",
            "#   #",
            "#### "
        });
        ALFABETO.put('C', new String[]{
            " ####",
            "#    ",
            "#    ",
            "#    ",
            "#    ",
            "#    ",
            " ####"
        });
        ALFABETO.put('D', new String[]{
            "#### ",
            "#   #",
            "#   #",
            "#   #",
            "#   #",
            "#   #",
            "#### "
        });
        ALFABETO.put('E', new String[]{
            "#####",
            "#    ",
            "#    ",
            "#####",
            "#    ",
            "#    ",
            "#####"
        });
        ALFABETO.put('F', new String[]{
            "#####",
            "#    ",
            "#    ",
            "#####",
            "#    ",
            "#    ",
            "#    "
        });
        ALFABETO.put('G', new String[]{
            " ####",
            "#    ",
            "#    ",
            "# ###",
            "#   #",
            "#   #",
            " ####"
        });
        ALFABETO.put('H', new String[]{
            "#   #",
            "#   #",
            "#   #",
            "#####",
            "#   #",
            "#   #",
            "#   #"
        });
        ALFABETO.put('I', new String[]{
            "#####",
            "  #  ",
            "  #  ",
            "  #  ",
            "  #  ",
            "  #  ",
            "#####"
        });
        ALFABETO.put('J', new String[]{
            "    #",
            "    #",
            "    #",
            "    #",
            "    #",
            "#   #",
            " ### "
        });
        ALFABETO.put('K', new String[]{
            "#   #",
            "#  # ",
            "# #  ",
            "##   ",
            "# #  ",
            "#  # ",
            "#   #"
        });
        ALFABETO.put('L', new String[]{
            "#    ",
            "#    ",
            "#    ",
            "#    ",
            "#    ",
            "#    ",
            "#####"
        });
        ALFABETO.put('M', new String[]{
            "#   #",
            "## ##",
            "# # #",
            "#   #",
            "#   #",
            "#   #",
            "#   #"
        });
        ALFABETO.put('N', new String[]{
            "#   #",
            "##  #",
            "# # #",
            "#  ##",
            "#   #",
            "#   #",
            "#   #"
        });
        ALFABETO.put('O', new String[]{
            " ### ",
            "#   #",
            "#   #",
            "#   #",
            "#   #",
            "#   #",
            " ### "
        });
        ALFABETO.put('P', new String[]{
            "#### ",
            "#   #",
            "#   #",
            "#### ",
            "#    ",
            "#    ",
            "#    "
        });
        ALFABETO.put('Q', new String[]{
            " ### ",
            "#   #",
            "#   #",
            "#   #",
            "# # #",
            "#  # ",
            " ## #"
        });
        ALFABETO.put('R', new String[]{
            "#### ",
            "#   #",
            "#   #",
            "#### ",
            "# #  ",
            "#  # ",
            "#   #"
        });
        ALFABETO.put('S', new String[]{
            " ####",
            "#    ",
            "#    ",
            " ### ",
            "    #",
            "    #",
            "#### "
        });
        ALFABETO.put('T', new String[]{
            "#####",
            "  #  ",
            "  #  ",
            "  #  ",
            "  #  ",
            "  #  ",
            "  #  "
        });
        ALFABETO.put('U', new String[]{
            "#   #",
            "#   #",
            "#   #",
            "#   #",
            "#   #",
            "#   #",
            " ### "
        });
        ALFABETO.put('V', new String[]{
            "#   #",
            "#   #",
            "#   #",
            "#   #",
            "#   #",
            " # # ",
            "  #  "
        });
        ALFABETO.put('W', new String[]{
            "#   #",
            "#   #",
            "#   #",
            "#   #",
            "# # #",
            "## ##",
            "#   #"
        });
        ALFABETO.put('X', new String[]{
            "#   #",
            "#   #",
            " # # ",
            "  #  ",
            " # # ",
            "#   #",
            "#   #"
        });
        ALFABETO.put('Y', new String[]{
            "#   #",
            "#   #",
            "#   #",
            " # # ",
            "  #  ",
            "  #  ",
            "  #  "
        });
        ALFABETO.put('Z', new String[]{
            "#####",
            "    #",
            "   # ",
            "  #  ",
            " #   ",
            "#    ",
            "#####"
        });

        // -------- Digitos 0-9 --------
        ALFABETO.put('0', new String[]{
            " ### ",
            "#   #",
            "#  ##",
            "# # #",
            "##  #",
            "#   #",
            " ### "
        });
        ALFABETO.put('1', new String[]{
            "  #  ",
            " ##  ",
            "  #  ",
            "  #  ",
            "  #  ",
            "  #  ",
            "#####"
        });
        ALFABETO.put('2', new String[]{
            " ### ",
            "#   #",
            "    #",
            "   # ",
            "  #  ",
            " #   ",
            "#####"
        });
        ALFABETO.put('3', new String[]{
            " ### ",
            "#   #",
            "    #",
            "  ## ",
            "    #",
            "#   #",
            " ### "
        });
        ALFABETO.put('4', new String[]{
            "   # ",
            "  ## ",
            " # # ",
            "#  # ",
            "#####",
            "   # ",
            "   # "
        });
        ALFABETO.put('5', new String[]{
            "#####",
            "#    ",
            "#### ",
            "    #",
            "    #",
            "#   #",
            " ### "
        });
        ALFABETO.put('6', new String[]{
            " ### ",
            "#   #",
            "#    ",
            "#### ",
            "#   #",
            "#   #",
            " ### "
        });
        ALFABETO.put('7', new String[]{
            "#####",
            "    #",
            "   # ",
            "  #  ",
            " #   ",
            " #   ",
            " #   "
        });
        ALFABETO.put('8', new String[]{
            " ### ",
            "#   #",
            "#   #",
            " ### ",
            "#   #",
            "#   #",
            " ### "
        });
        ALFABETO.put('9', new String[]{
            " ### ",
            "#   #",
            "#   #",
            " ####",
            "    #",
            "#   #",
            " ### "
        });

        // -------- Simbolos --------
        ALFABETO.put(' ', new String[]{
            "     ",
            "     ",
            "     ",
            "     ",
            "     ",
            "     ",
            "     "
        });
        ALFABETO.put(':', new String[]{
            "     ",
            "     ",
            "  #  ",
            "     ",
            "  #  ",
            "     ",
            "     "
        });
        ALFABETO.put('-', new String[]{
            "     ",
            "     ",
            "     ",
            "#####",
            "     ",
            "     ",
            "     "
        });
    }

    // Constructor privado: nadie debe instanciar esta clase.
    private TextRenderer() {}

    /**
     * Dibuja un texto. (xLeft, yTop) es la esquina superior izquierda
     * del primer caracter. La altura de cada letra es escalaLetra. El
     * texto crece hacia la derecha desde xLeft.
     *
     * El parametro 'color' es un float[3] con (r, g, b) en [0, 1].
     */
    public static void dibujarTexto(Renderer r, String texto,
                                    float xLeft, float yTop,
                                    float escalaLetra, float[] color) {
        // Tamano de un "pixel del font" en NDC.
        float pixel = escalaLetra / FILAS;
        // Distancia entre origenes de letras consecutivas (5 pixeles
        // de letra + 1 pixel de separacion).
        float pasoEntreLetras = (COLUMNAS + 1) * pixel;

        for (int i = 0; i < texto.length(); i++) {
            char c = Character.toUpperCase(texto.charAt(i));
            float xLetra = xLeft + i * pasoEntreLetras;
            dibujarLetra(r, c, xLetra, yTop, pixel, color);
        }
    }

    /**
     * Devuelve el ancho que ocupara un texto al dibujarlo con la escala
     * dada. Util para CENTRAR ("xLeft = -anchoTexto/2") o alinear a la
     * derecha ("xLeft = limiteDerecho - anchoTexto").
     */
    public static float anchoTexto(String texto, float escalaLetra) {
        if (texto.isEmpty()) return 0.0f;
        float pixel = escalaLetra / FILAS;
        // n letras de ancho COLUMNAS pixeles + (n-1) separaciones de 1 pixel.
        return texto.length() * COLUMNAS * pixel
                + (texto.length() - 1) * pixel;
    }

    /**
     * Dibuja UNA letra. Recorre la grilla 5x7 y por cada celda con '#'
     * dispara un rectangulo en el lugar correspondiente. (xLeft, yTop)
     * es la esquina superior-izquierda de la letra; la letra crece hacia
     * abajo y hacia la derecha desde ahi.
     *
     * Si la letra no esta en el alfabeto, dibuja un cuadrado relleno
     * del tamano de una letra como placeholder — no crashea.
     */
    private static void dibujarLetra(Renderer r, char c,
                                     float xLeft, float yTop,
                                     float pixel, float[] color) {
        String[] bitmap = ALFABETO.get(c);
        if (bitmap == null) {
            // Placeholder: cuadrado lleno del tamano de la letra.
            float cx = xLeft + COLUMNAS * 0.5f * pixel;
            float cy = yTop  - FILAS    * 0.5f * pixel;
            r.dibujarRect(cx, cy, COLUMNAS * pixel, FILAS * pixel,
                          color[0], color[1], color[2]);
            return;
        }

        for (int row = 0; row < FILAS; row++) {
            String filaStr = bitmap[row];
            for (int col = 0; col < COLUMNAS; col++) {
                if (filaStr.charAt(col) == '#') {
                    // Centro del rect: medio pixel adentro de la celda.
                    // y disminuye al bajar (NDC: arriba es +1).
                    float cx = xLeft + (col + 0.5f) * pixel;
                    float cy = yTop  - (row + 0.5f) * pixel;
                    r.dibujarRect(cx, cy, pixel, pixel,
                                  color[0], color[1], color[2]);
                }
            }
        }
    }
}
