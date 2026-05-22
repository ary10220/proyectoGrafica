package com.graphics;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Renderer:
 * cuatro primitivas + un fondo:
 *   - dibujarRect(...)            → rectangulos.
 *   - dibujarTriangulo(...)       → triangulo apuntando a +X.
 *   - dibujarCirculo(...)         → circulo (TRIANGLE_FAN).
 *   - dibujarFondoDegradado(...)  → quad full-screen con color
 *                                   interpolado top→bottom.
 *
 * Usamos DOS shader programs distintos:
 *   1) "principal":  un color uniforme aplicado a toda la primitiva.
 *      Lo usan todas las formas chicas (pajaro, tuberias, nubes, suelo,
 *      overlay).
 *   2) "fondo":      cada vertice tiene SU PROPIO color, y el fragment
 *      shader recibe el color INTERPOLADO entre los vertices via un
 *      "varying" (out -> in). Esto produce el degradado "gratis" — el
 *      hardware interpola color por pixel sin que nosotros tengamos que
 *      pintar muchas franjas a mano.
 *
 * Cada primitiva tiene su propio VAO en GPU. El draw call:
 *   1) elige programa,
 *   2) ata el VAO,
 *   3) setea uniforms (en el caso del programa principal),
 *   4) glDrawArrays.
 *
 * IMPORTANTE: init() debe llamarse DESPUES de GL.createCapabilities().
 */
public class Renderer {

    // === Programs ===
    private int programaPrincipal;   // uColor uniforme (rect/tri/circ)
    private int programaFondo;       // color por vertice (degradado)

    // === Recursos del programa principal ===
    private int vaoQuad,  vboQuad;
    private int vaoTri,   vboTri;
    private int vaoCirc,  vboCirc;
    private static final int CIRCULO_SEGMENTOS = 32;
    private static final int CIRCULO_VERTICES  = CIRCULO_SEGMENTOS + 2;

    private int uOffsetLoc, uScaleLoc, uRotationLoc, uColorLoc, uAlphaLoc;

    // === Recursos del programa fondo ===
    private int vaoFondo, vboFondo;
    // FloatBuffer reutilizable para subir colores del degradado al
    // VBO sin allocar memoria nativa cada frame. 4 vertices x 5 floats
    // por vertice (x, y, r, g, b) = 20 floats.
    private final FloatBuffer fondoBuf = BufferUtils.createFloatBuffer(20);

    public void init() {
        crearShadersPrincipal();
        crearQuadVao();
        crearTrianguloVao();
        crearCirculoVao();

        crearShaderFondo();
        crearFondoVao();
    }

    /**
     * Limpia el frame. NO ata ningun program ni VAO — cada metodo de
     * dibujo ata el suyo. Si nadie dibuja un fondo despues, queda el
     * color de clear (negro). Como Game llama dibujarFondoDegradado()
     * justo despues, en la practica nunca se ve el negro.
     */
    public void beginFrame() {
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    public void cleanup() {
        GL30.glDeleteVertexArrays(vaoQuad);
        GL30.glDeleteVertexArrays(vaoTri);
        GL30.glDeleteVertexArrays(vaoCirc);
        GL30.glDeleteVertexArrays(vaoFondo);
        GL15.glDeleteBuffers(vboQuad);
        GL15.glDeleteBuffers(vboTri);
        GL15.glDeleteBuffers(vboCirc);
        GL15.glDeleteBuffers(vboFondo);
        GL20.glDeleteProgram(programaPrincipal);
        GL20.glDeleteProgram(programaFondo);
    }


    public void dibujarRect(float x, float y, float ancho, float alto,
                            float r, float g, float b) {
        dibujarRect(x, y, ancho, alto, 0.0f, r, g, b);
    }

    public void dibujarRect(float x, float y, float ancho, float alto,
                            float rotacion, float r, float g, float b) {
        emitir(vaoQuad, 6, GL11.GL_TRIANGLES,
               x, y, ancho, alto, rotacion, r, g, b, 1.0f);
    }

    /**
     * Rectangulo con ALPHA (transparencia). Usar para overlays
     * semitransparentes (pantalla de inicio/game over que dejan ver lo
     * que hay detras). Requiere blending habilitado en App.
     *
     * Nombre distinto a dibujarRect para evitar choque de firma con la
     * version de rotacion: en Java los overloads se distinguen por
     * tipos, no por nombres de parametros, y (x,y,w,h,rotacion,r,g,b)
     * tiene los mismos 8 floats que (x,y,w,h,r,g,b,a).
     */
    public void dibujarRectAlpha(float x, float y, float ancho, float alto,
                                 float r, float g, float b, float a) {
        emitir(vaoQuad, 6, GL11.GL_TRIANGLES,
               x, y, ancho, alto, 0.0f, r, g, b, a);
    }

    public void dibujarTriangulo(float x, float y, float ancho, float alto,
                                 float rotacion, float r, float g, float b) {
        emitir(vaoTri, 3, GL11.GL_TRIANGLES,
               x, y, ancho, alto, rotacion, r, g, b, 1.0f);
    }

    public void dibujarCirculo(float x, float y, float ancho, float alto,
                               float rotacion, float r, float g, float b) {
        emitir(vaoCirc, CIRCULO_VERTICES, GL11.GL_TRIANGLE_FAN,
               x, y, ancho, alto, rotacion, r, g, b, 1.0f);
    }

    /**
     * Dibuja un degradado vertical full-screen con (rT, gT, bT) en el
     * borde superior y (rB, gB, bB) en el inferior. Los pixeles del
     * medio salen interpolados por el hardware automaticamente (es lo
     * que hace el rasterizador con los varying entre vertex y fragment).
     *
     * Internamente: rearma los 4 vertices del quad full-screen con sus
     * colores, los sube al VBO (glBufferSubData, no realloca el VBO
     * porque ya tiene el tamano correcto desde init), y dibuja un
     * TRIANGLE_STRIP de 4 vertices.
     */
    public void dibujarFondoDegradado(float rT, float gT, float bT,
                                      float rB, float gB, float bB) {
        // Layout por vertice: x, y, r, g, b.
        // Orden TRIANGLE_STRIP: TL, BL, TR, BR (forma 2 triangulos).
        fondoBuf.clear();
        fondoBuf.put(-1).put( 1).put(rT).put(gT).put(bT); // TL
        fondoBuf.put(-1).put(-1).put(rB).put(gB).put(bB); // BL
        fondoBuf.put( 1).put( 1).put(rT).put(gT).put(bT); // TR
        fondoBuf.put( 1).put(-1).put(rB).put(gB).put(bB); // BR
        fondoBuf.flip();

        GL20.glUseProgram(programaFondo);
        GL30.glBindVertexArray(vaoFondo);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboFondo);
        // Reemplazo los datos del VBO sin reallocar memoria GPU.
        // El offset 0 quiere decir "desde el principio".
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, fondoBuf);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    // -------------------------------------------------------------
    // Helpers internos
    // -------------------------------------------------------------

    /**
     * Disparo del draw call. Setea TODOS los uniforms del programa
     * principal (incluyendo uAlpha) cada vez, asi nunca queda un valor
     * de un draw anterior contaminando este. El cost de glUniform es
     * irrelevante a la escala del juego (~50 draws/frame).
     */
    private void emitir(int vao, int count, int mode,
                        float x, float y, float ancho, float alto, float rotacion,
                        float r, float g, float b, float a) {
        GL20.glUseProgram(programaPrincipal);
        GL30.glBindVertexArray(vao);
        GL20.glUniform2f(uOffsetLoc, x, y);
        GL20.glUniform2f(uScaleLoc, ancho, alto);
        GL20.glUniform1f(uRotationLoc, rotacion);
        GL20.glUniform3f(uColorLoc, r, g, b);
        GL20.glUniform1f(uAlphaLoc, a);
        GL11.glDrawArrays(mode, 0, count);
    }

    private int ultimoVbo;
    private int crearVao(float[] vertices) {
        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer buf = BufferUtils.createFloatBuffer(vertices.length);
        buf.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);

        // Atributo 0 = posicion, 3 floats por vertice, sin offset.
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        ultimoVbo = vbo;
        return vao;
    }

    // -------------------------------------------------------------
    // VAOs del programa principal
    // -------------------------------------------------------------

    private void crearQuadVao() {
        float[] v = {
            -0.5f, -0.5f, 0.0f,
             0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f,  0.5f, 0.0f
        };
        vaoQuad = crearVao(v);
        vboQuad = ultimoVbo;
    }

    private void crearTrianguloVao() {
        float[] v = {
             0.5f,  0.0f, 0.0f,
            -0.5f,  0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f
        };
        vaoTri = crearVao(v);
        vboTri = ultimoVbo;
    }

    private void crearCirculoVao() {
        float[] v = new float[CIRCULO_VERTICES * 3];
        v[0] = 0.0f; v[1] = 0.0f; v[2] = 0.0f;
        for (int i = 0; i <= CIRCULO_SEGMENTOS; i++) {
            float angulo = (float) (2.0 * Math.PI * i / CIRCULO_SEGMENTOS);
            int base = (i + 1) * 3;
            v[base + 0] = (float) (0.5 * Math.cos(angulo));
            v[base + 1] = (float) (0.5 * Math.sin(angulo));
            v[base + 2] = 0.0f;
        }
        vaoCirc = crearVao(v);
        vboCirc = ultimoVbo;
    }

    // -------------------------------------------------------------
    // VAO del programa fondo (2 atributos: pos + color)
    // -------------------------------------------------------------

    /**
     * Crea el VAO del fondo con un VBO vacio del tamano justo (20 floats
     * = 4 vertices * 5 atributos). Los datos los carga cada vez que se
     * llama dibujarFondoDegradado(). Usamos GL_DYNAMIC_DRAW como hint
     * porque vamos a actualizarlo seguido (1 vez por frame).
     *
     * Layout interleaved (atributos intercalados en el mismo VBO):
     *   v0: x, y, r, g, b
     *   v1: x, y, r, g, b
     *   v2: x, y, r, g, b
     *   v3: x, y, r, g, b
     *
     * Atributo 0 = posicion (2 floats, offset 0).
     * Atributo 1 = color    (3 floats, offset 2*4 = 8 bytes).
     * Stride = 5 floats * 4 bytes = 20 bytes (lo que ocupa UN vertice).
     */
    private void crearFondoVao() {
        vaoFondo = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoFondo);

        vboFondo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboFondo);
        // Reservo 20 floats vacios (los datos los pone dibujarFondoDegradado).
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER,
                (long) 20 * Float.BYTES, GL15.GL_DYNAMIC_DRAW);

        int stride = 5 * Float.BYTES;
        // aPos: 2 floats, offset 0.
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, 0L);
        GL20.glEnableVertexAttribArray(0);
        // aColor: 3 floats, offset 2*4 bytes.
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, stride, 2L * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    // -------------------------------------------------------------
    // Shaders
    // -------------------------------------------------------------

    /**
     * Programa principal: aplica escala -> rotacion -> traslacion al
     * vertice base y pinta del color uniforme uColor.
     */
    private void crearShadersPrincipal() {
        String vertexSrc = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            uniform vec2  uOffset;
            uniform vec2  uScale;
            uniform float uRotation;
            void main() {
                vec2 escalado = aPos.xy * uScale;
                float c = cos(uRotation);
                float s = sin(uRotation);
                vec2 rotado = vec2(
                    escalado.x * c - escalado.y * s,
                    escalado.x * s + escalado.y * c
                );
                vec2 finalPos = rotado + uOffset;
                gl_Position = vec4(finalPos, aPos.z, 1.0);
            }
            """;

        String fragmentSrc = """
            #version 330 core
            uniform vec3  uColor;
            uniform float uAlpha;
            out vec4 fragColor;
            void main() {
                // uAlpha controla transparencia: 1.0 = opaco (caso por
                // defecto), valores menores → translucido (overlays).
                // Para que la translucidez funcione hay que tener
                // GL_BLEND habilitado con SRC_ALPHA / ONE_MINUS_SRC_ALPHA
                // (eso lo prende App.initWindow una vez).
                fragColor = vec4(uColor, uAlpha);
            }
            """;

        programaPrincipal = compilarPrograma(vertexSrc, fragmentSrc);

        uOffsetLoc   = GL20.glGetUniformLocation(programaPrincipal, "uOffset");
        uScaleLoc    = GL20.glGetUniformLocation(programaPrincipal, "uScale");
        uRotationLoc = GL20.glGetUniformLocation(programaPrincipal, "uRotation");
        uColorLoc    = GL20.glGetUniformLocation(programaPrincipal, "uColor");
        uAlphaLoc    = GL20.glGetUniformLocation(programaPrincipal, "uAlpha");
        if (uOffsetLoc == -1 || uScaleLoc == -1 || uRotationLoc == -1
                || uColorLoc == -1 || uAlphaLoc == -1) {
            throw new RuntimeException("Uniforms del programa principal no encontrados");
        }
    }

    /**
     * Programa fondo: cada vertice trae su propio color. El vertex
     * shader lo entrega al fragment shader via "vColor". El rasterizador
     * INTERPOLA vColor entre los 3 vertices de cada triangulo, asi cada
     * pixel recibe un color mezclado proporcional a su distancia a los
     * vertices. Con un quad de 4 vertices coloreados (top y bot), eso
     * da un degradado vertical perfecto.
     *
     * No hay uniforms — todo el "estado" de este shader viene de los
     * atributos por vertice.
     */
    private void crearShaderFondo() {
        String vertexSrc = """
            #version 330 core
            layout (location = 0) in vec2 aPos;
            layout (location = 1) in vec3 aColor;
            out vec3 vColor;
            void main() {
                vColor = aColor;
                gl_Position = vec4(aPos, 0.0, 1.0);
            }
            """;

        String fragmentSrc = """
            #version 330 core
            in vec3 vColor;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(vColor, 1.0);
            }
            """;

        programaFondo = compilarPrograma(vertexSrc, fragmentSrc);
    }

    /** Compila vertex+fragment, los linkea y devuelve el program id. */
    private int compilarPrograma(String vertexSrc, String fragmentSrc) {
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSrc);
        GL20.glCompileShader(vertexShader);
        comprobarShader(vertexShader, "Vertex");

        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSrc);
        GL20.glCompileShader(fragmentShader);
        comprobarShader(fragmentShader, "Fragment");

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Error al enlazar programa: "
                    + GL20.glGetProgramInfoLog(program));
        }

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        return program;
    }

    private void comprobarShader(int shader, String tipo) {
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(tipo + " shader: "
                    + GL20.glGetShaderInfoLog(shader));
        }
    }
}
