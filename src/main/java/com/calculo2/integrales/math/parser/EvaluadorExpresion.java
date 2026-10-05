package com.calculo2.integrales.math.parser;

import com.calculo2.integrales.exception.ExpresionInvalidaException;

/**
 * Punto de entrada del parser: recibe el texto que escribio el usuario y devuelve algo
 * que se puede evaluar.
 *
 * <p>Encadena las tres etapas — {@link Tokenizador}, {@link AnalizadorSintactico} y el
 * arbol de {@link NodoExpresion} — para que el resto del proyecto no tenga que
 * conocerlas. El uso tipico es:</p>
 *
 * <pre>{@code
 * EvaluadorExpresion f = EvaluadorExpresion.compilar("x^2 + 1");
 * double valor = f.evaluar(3);      // 10
 * }</pre>
 */
public final class EvaluadorExpresion {

    private final String expresionOriginal;
    private final String variable;
    private final NodoExpresion raiz;

    private EvaluadorExpresion(String expresionOriginal, String variable, NodoExpresion raiz) {
        this.expresionOriginal = expresionOriginal;
        this.variable = variable;
        this.raiz = raiz;
    }

    /**
     * Compila una expresion cuya variable independiente es {@code x}.
     *
     * @param expresion texto escrito por el usuario
     * @return la expresion lista para evaluarse
     * @throws ExpresionInvalidaException si el texto no se puede interpretar
     */
    public static EvaluadorExpresion compilar(String expresion) {
        return compilar(expresion, "x");
    }

    /**
     * Compila una expresion indicando cual es la variable independiente.
     *
     * <p>Se usa {@code y} cuando la integracion es respecto al eje vertical, como en
     * algunos solidos de revolucion.</p>
     *
     * @param expresion texto escrito por el usuario
     * @param variable  nombre de la variable, normalmente "x" o "y"
     * @return la expresion lista para evaluarse
     * @throws ExpresionInvalidaException si el texto no se puede interpretar
     */
    /**
     * Lee una expresion en la que pueden aparecer varias variables a la vez.
     *
     * <p>No devuelve un evaluador, porque una expresion de dos variables no se puede
     * evaluar dando un solo valor: devuelve el arbol, para manipularlo algebraicamente.
     * Es lo que se necesita para despejar una ecuacion como {@code y^2 = 8x}.</p>
     *
     * @param expresion texto escrito por el usuario, sin signo igual
     * @param variables nombres de las variables admitidas
     * @return la raiz del arbol
     */
    public static NodoExpresion analizarConVariables(String expresion,
                                                     java.util.Set<String> variables) {
        String limpia = limpiar(expresion);
        return new AnalizadorSintactico(new Tokenizador(limpia, variables).tokenizar()).analizar();
    }

    public static EvaluadorExpresion compilar(String expresion, String variable) {
        String limpia = limpiar(expresion);
        String nombreVariable = (variable == null || variable.isBlank())
                ? "x"
                : variable.trim().toLowerCase();

        NodoExpresion raiz = new AnalizadorSintactico(
                new Tokenizador(limpia, nombreVariable).tokenizar()).analizar();

        return new EvaluadorExpresion(limpia, nombreVariable, raiz);
    }

    /**
     * Quita adornos que el usuario suele copiar del cuaderno y que no cambian el
     * significado: el "f(x) =" del inicio, las comas de miles y los espacios sobrantes.
     */
    private static String limpiar(String expresion) {
        if (expresion == null) {
            return "";
        }
        String texto = expresion.trim();

        // "f(x) = x^2" o "y = x^2" -> "x^2"
        int igual = texto.indexOf('=');
        if (igual >= 0 && igual < texto.length() - 1) {
            texto = texto.substring(igual + 1).trim();
        }

        // Simbolos que se escriben distinto segun el teclado.
        texto = texto.replace('×', '*')   // signo de multiplicacion
                     .replace('÷', '/')   // signo de division
                     .replace('−', '-')   // guion largo matematico
                     .replace('–', '-')
                     .replace('—', '-')
                     .replace("π", "pi");

        return traducirRaices(traducirSuperindices(texto));
    }

    /** Los digitos en superindice, en orden del cero al nueve. */
    private static final String SUPERINDICES = "⁰¹²³⁴⁵⁶⁷⁸⁹";

    /**
     * Traduce los exponentes escritos en superindice a la notacion con circunflejo.
     *
     * <p>Un enunciado copiado de un documento trae {@code x²} en lugar de {@code x^2}, y
     * es lo que el estudiante ve escrito en su hoja. Se convierte aqui para que el resto
     * del parser no tenga que saber que existen dos formas de escribir lo mismo.</p>
     *
     * <p>Los superindices seguidos forman un solo exponente: {@code x¹²} es {@code x^12},
     * no {@code x^1^2}.</p>
     */
    private static String traducirSuperindices(String texto) {
        StringBuilder resultado = new StringBuilder();

        for (int i = 0; i < texto.length(); i++) {
            int digito = SUPERINDICES.indexOf(texto.charAt(i));

            if (digito < 0) {
                resultado.append(texto.charAt(i));
                continue;
            }

            // Se juntan todos los superindices consecutivos en un unico exponente.
            resultado.append('^');
            while (i < texto.length()) {
                int siguiente = SUPERINDICES.indexOf(texto.charAt(i));
                if (siguiente < 0) {
                    break;
                }
                resultado.append((char) ('0' + siguiente));
                i++;
            }
            i--;
        }
        return resultado.toString();
    }

    /**
     * Traduce el simbolo de raiz a la funcion {@code sqrt}.
     *
     * <p>No basta con sustituir el caracter, porque el simbolo se escribe sin parentesis:
     * quien copia {@code √x} del cuaderno espera la raiz de {@code x}, y una sustitucion
     * directa produciria {@code sqrtx}, que no significa nada. Aqui se busca el operando
     * que sigue al simbolo y se envuelve entre parentesis, de modo que {@code √x} pase a
     * ser {@code sqrt(x)} y {@code √(x+1)} siga siendo {@code sqrt(x+1)}.</p>
     */
    private static String traducirRaices(String texto) {
        StringBuilder resultado = new StringBuilder();

        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c != '√') {
                resultado.append(c);
                continue;
            }

            int inicioOperando = i + 1;
            while (inicioOperando < texto.length()
                    && Character.isWhitespace(texto.charAt(inicioOperando))) {
                inicioOperando++;
            }

            int finOperando = finDelOperando(texto, inicioOperando);
            if (finOperando <= inicioOperando) {
                // No hay nada despues del simbolo; se deja el nombre para que el
                // analizador emita el error con su propio mensaje.
                resultado.append("sqrt");
                continue;
            }

            resultado.append("sqrt(")
                     .append(texto, inicioOperando, finOperando)
                     .append(')');
            i = finOperando - 1;
        }
        return resultado.toString();
    }

    /**
     * Localiza donde termina el operando que sigue al simbolo de raiz.
     *
     * @return el indice siguiente al ultimo caracter del operando
     */
    private static int finDelOperando(String texto, int inicio) {
        if (inicio >= texto.length()) {
            return inicio;
        }

        // Un grupo entre parentesis llega hasta su cierre correspondiente.
        if (texto.charAt(inicio) == '(') {
            int profundidad = 0;
            for (int i = inicio; i < texto.length(); i++) {
                char c = texto.charAt(i);
                if (c == '(') {
                    profundidad++;
                } else if (c == ')') {
                    profundidad--;
                    if (profundidad == 0) {
                        return i + 1;
                    }
                }
            }
            return texto.length();
        }

        // Si no, el operando es la secuencia de letras, digitos y puntos que sigue.
        int i = inicio;
        while (i < texto.length()
                && (Character.isLetterOrDigit(texto.charAt(i)) || texto.charAt(i) == '.')) {
            i++;
        }
        return i;
    }

    /**
     * Evalua la expresion en un punto.
     *
     * @param valor valor de la variable independiente
     * @return el resultado; puede ser NaN o infinito fuera del dominio
     */
    public double evaluar(double valor) {
        return raiz.evaluar(valor);
    }

    /** Devuelve la expresion como {@link FuncionMatematica}, lista para integrar. */
    public FuncionMatematica comoFuncion() {
        return raiz::evaluar;
    }

    /** El arbol resultante, por si se necesita recorrerlo. */
    public NodoExpresion raiz() {
        return raiz;
    }

    /** El texto ya limpio que realmente se analizo. */
    public String expresionOriginal() {
        return expresionOriginal;
    }

    /** Nombre de la variable independiente. */
    public String variable() {
        return variable;
    }

    /**
     * La expresion reconstruida desde el arbol, con todos los parentesis explicitos.
     * Sirve para mostrarle al usuario como se interpreto lo que escribio.
     */
    public String textoNormalizado() {
        return raiz.aTexto();
    }

    @Override
    public String toString() {
        return expresionOriginal;
    }
}
