package com.calculo2.integrales.math.simbolico;

import com.calculo2.integrales.math.parser.NodoExpresion;
import com.calculo2.integrales.math.util.Redondeo;

/**
 * Escribe un arbol de expresion como lo escribiria una persona.
 *
 * <p>{@code NodoExpresion.aTexto()} pone todos los parentesis posibles, porque su trabajo
 * es mostrarle al usuario como se interpreto lo que escribio y ahi la ambiguedad seria un
 * problema. En un procedimiento paso a paso eso estorba: nadie escribe
 * {@code ((x ^ 2) + (3 * x))} en el cuaderno.</p>
 *
 * <p>Este formateador pone parentesis solo donde hacen falta. La regla es comparar la
 * precedencia del nodo con la del lugar donde se va a insertar: si lo que viene tiene
 * menos prioridad que el contexto, hay que agruparlo para no cambiar el significado.</p>
 */
public final class FormateadorMatematico {

    private FormateadorMatematico() {
    }

    /** Niveles de prioridad; un numero mayor se aprieta mas fuerte. */
    private static final int PRIORIDAD_SUMA = 1;
    private static final int PRIORIDAD_PRODUCTO = 2;
    private static final int PRIORIDAD_UNARIO = 3;
    private static final int PRIORIDAD_POTENCIA = 4;
    private static final int PRIORIDAD_ATOMO = 5;

    /**
     * Escribe la expresion en texto limpio.
     *
     * @param expresion arbol a escribir
     * @return la expresion como texto
     */
    public static String escribir(NodoExpresion expresion) {
        if (expresion == null) {
            return "";
        }
        return escribir(expresion, 0);
    }

    /**
     * Escribe la expresion sabiendo con que fuerza la aprieta el contexto.
     *
     * @param expresion           arbol a escribir
     * @param prioridadDelContexto prioridad del operador que la contiene
     * @return la expresion, con parentesis solo si hacen falta
     */
    private static String escribir(NodoExpresion expresion, int prioridadDelContexto) {
        String texto = escribirSinAgrupar(expresion);
        int prioridadPropia = prioridadDe(expresion);

        boolean necesitaParentesis = prioridadPropia < prioridadDelContexto;
        return necesitaParentesis ? "(" + texto + ")" : texto;
    }

    private static String escribirSinAgrupar(NodoExpresion expresion) {
        return switch (expresion) {
            case NodoExpresion.Numero numero -> Redondeo.texto(numero.valor());

            case NodoExpresion.Variable variable -> variable.nombre();

            case NodoExpresion.Constante constante -> constante.nombre();

            case NodoExpresion.Negacion negacion ->
                    "-" + escribir(negacion.operando(), PRIORIDAD_UNARIO);

            case NodoExpresion.LlamadaFuncion llamada ->
                    llamada.nombre() + "(" + escribir(llamada.argumento(), 0) + ")";

            case NodoExpresion.OperacionBinaria operacion -> escribirOperacion(operacion);
        };
    }

    private static String escribirOperacion(NodoExpresion.OperacionBinaria operacion) {
        String simbolo = operacion.simbolo();
        int prioridad = prioridadDelSimbolo(simbolo);

        // La potencia se asocia a la derecha, y eso decide los dos lados a la vez. El
        // derecho no necesita parentesis aunque sea otra potencia, porque 2^3^2 ya
        // significa 2^(3^2); el izquierdo si los necesita por el mismo motivo, ya que
        // escribir x^3^2 para el cuadrado de x^3 diria x^(3^2), que es otra cosa. Es el
        // caso que aparece en arandelas: el radio exterior suele ser una potencia y hay
        // que elevarla al cuadrado.
        boolean esPotencia = simbolo.equals("^");
        int prioridadIzquierda = esPotencia ? prioridad + 1 : prioridad;
        int prioridadDerecha = esPotencia ? prioridad : prioridad + 1;

        String izquierda = escribir(operacion.izquierda(), prioridadIzquierda);
        String derecha = escribir(operacion.derecha(), prioridadDerecha);

        // La potencia se ve mejor pegada; los demas operadores, separados.
        if (simbolo.equals("^")) {
            return izquierda + "^" + derecha;
        }
        // El producto se escribe como en el cuaderno cuando no hay ambiguedad: 8x,
        // 2sqrt(x), x(1 - x), (x - 2)(x + 1). Sigue siendo texto que el parser entiende.
        if (simbolo.equals("*") && sePuedeYuxtaponer(izquierda, derecha)) {
            return izquierda + derecha;
        }
        return izquierda + " " + simbolo + " " + derecha;
    }

    /**
     * Decide si un producto se puede escribir sin el signo de multiplicar.
     *
     * <p>Solo en los casos en que un lector no puede dudar: un numero delante de una
     * letra o de un parentesis, una variable delante de un parentesis, y dos parentesis
     * seguidos. {@code 2 * 3} sigue con su signo, porque {@code 23} seria otro numero.</p>
     */
    private static boolean sePuedeYuxtaponer(String izquierda, String derecha) {
        if (izquierda.isEmpty() || derecha.isEmpty()) {
            return false;
        }
        char ultimo = izquierda.charAt(izquierda.length() - 1);
        char primero = derecha.charAt(0);

        boolean izquierdaEsNumero = izquierda.matches("-?\\d+(\\.\\d+)?");
        if (izquierdaEsNumero && (Character.isLetter(primero) || primero == '(')) {
            return true;
        }
        if (ultimo == ')' && primero == '(') {
            return true;
        }
        return izquierda.matches("[a-z]") && primero == '(';
    }

    private static int prioridadDe(NodoExpresion expresion) {
        return switch (expresion) {
            case NodoExpresion.Numero numero ->
                    // Un numero negativo se comporta como un signo delante del valor.
                    numero.valor() < 0 ? PRIORIDAD_UNARIO : PRIORIDAD_ATOMO;
            case NodoExpresion.Variable ignorada -> PRIORIDAD_ATOMO;
            case NodoExpresion.Constante ignorada -> PRIORIDAD_ATOMO;
            case NodoExpresion.LlamadaFuncion ignorada -> PRIORIDAD_ATOMO;
            case NodoExpresion.Negacion ignorada -> PRIORIDAD_UNARIO;
            case NodoExpresion.OperacionBinaria operacion ->
                    prioridadDelSimbolo(operacion.simbolo());
        };
    }

    private static int prioridadDelSimbolo(String simbolo) {
        return switch (simbolo) {
            case "+", "-" -> PRIORIDAD_SUMA;
            case "*", "/", "%" -> PRIORIDAD_PRODUCTO;
            case "^" -> PRIORIDAD_POTENCIA;
            default -> PRIORIDAD_SUMA;
        };
    }

    /**
     * Escribe la expresion agrupada entre parentesis si tiene mas de un termino.
     * Sirve para insertarla dentro de una formula mayor.
     *
     * @param expresion arbol a escribir
     * @return la expresion, agrupada si hace falta
     */
    public static String escribirAgrupado(NodoExpresion expresion) {
        return escribir(expresion, PRIORIDAD_PRODUCTO);
    }
}
