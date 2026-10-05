package com.calculo2.integrales.interprete;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Corrige las formas de escribir matematicas que el parser no entenderia, antes de leer
 * nada.
 *
 * <p>Los enunciados se copian de documentos, de capturas o del teclado del telefono, y
 * llegan con escrituras que significan una cosa evidente para quien las lee pero otra
 * para el parser:</p>
 *
 * <pre>
 *   y = x3 – x        el guion largo es una resta y "x3" es x al cubo
 *   y = x ^2          el espacio no separa la potencia de su base
 *   4x² + 9y ² = 36   el superindice va con la y aunque haya un espacio
 * </pre>
 *
 * <p>Sin esta correccion, "x3 – x" se leeria como "x3", porque el guion largo no es un
 * simbolo de operacion y corta la expresion, y aun leyendo el guion, "x3" seria x por 3.</p>
 *
 * <p>Se cambia solo lo imprescindible. Lo que ya esta bien escrito se deja tal cual,
 * incluidos los espacios y los superindices, porque es lo que se le muestra al estudiante
 * como "la funcion que usted escribio".</p>
 *
 * <p>Las sustituciones conservan la posicion de cada caracter siempre que es posible, y
 * cuando no (al insertar el {@code ^}), lo hacen antes de cualquier otra lectura: todas
 * las posiciones que usa el analizador se refieren al texto ya corregido.</p>
 */
public final class PreprocesadorEnunciado {

    private PreprocesadorEnunciado() {
    }

    /** Una variable pegada a un exponente sin acento circunflejo: x2, x3, y2. */
    private static final Pattern VARIABLE_CON_EXPONENTE = Pattern.compile(
            "(?<![A-Za-z\\u00C0-\\u00FF_^])([xyXY])(\\d{1,2})(?![\\d.]\\d)");

    /** Espacios alrededor del acento circunflejo. */
    private static final Pattern ESPACIOS_EN_POTENCIA = Pattern.compile("\\s*\\^\\s*");

    /** Espacios antes de un superindice: "y ²". */
    private static final Pattern ESPACIO_ANTES_DE_SUPERINDICE = Pattern.compile(
            "(?<=[\\w)])\\s+(?=[\\u00B2\\u00B3\\u2070\\u00B9\\u2074-\\u2079])");

    /**
     * Deja el enunciado listo para extraer de el las expresiones.
     *
     * @param texto el enunciado tal como lo escribio el estudiante
     * @return el enunciado corregido
     */
    public static String corregir(String texto) {
        if (texto == null) {
            return "";
        }
        String corregido = texto
                // Los guiones largos de los procesadores de texto son restas.
                .replace('–', '-')
                .replace('—', '-')
                .replace('−', '-')
                .replace('‒', '-')
                // Signos de multiplicar y dividir de otros teclados.
                .replace('×', '*')
                .replace('÷', '/')
                .replace('·', '*')
                // Comillas tipograficas y espacios raros que llegan al copiar y pegar.
                .replace(' ', ' ')
                .replace('“', '"')
                .replace('”', '"');

        corregido = ESPACIOS_EN_POTENCIA.matcher(corregido).replaceAll("^");
        corregido = ESPACIO_ANTES_DE_SUPERINDICE.matcher(corregido).replaceAll("");

        Matcher variable = VARIABLE_CON_EXPONENTE.matcher(corregido);
        StringBuilder resultado = new StringBuilder();
        while (variable.find()) {
            variable.appendReplacement(resultado,
                    Matcher.quoteReplacement(variable.group(1) + "^" + variable.group(2)));
        }
        variable.appendTail(resultado);
        return resultado.toString();
    }
}
