package com.calculo2.integrales.interprete;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encuentra el intervalo de integracion escrito en el enunciado, en cualquiera de las
 * formas en que se escribe.
 *
 * <pre>
 *   en [0, 1]                                   corchetes
 *   de 0 a 4,  desde 0 hasta 4,  entre 0 y 4    en palabras
 *   entre x = 0 y x = 4                         con la variable
 *   0 &lt;= x &lt;= 1,  0 ≤ x ≤ 1                       desigualdad doble
 *   x menor o igual a 1 y x mayor o igual a 0  desigualdades en palabras
 *   ( x menor e igual q 1 y x mayor e igual q 0 )
 * </pre>
 *
 * <p>La ultima forma, con "e" en lugar de "o" y "q" en lugar de "que", es como lo
 * escriben muchos estudiantes, y no reconocerla obligaba a escribir otra vez un dato que
 * ya estaba en el enunciado.</p>
 */
public final class ExtractorIntervalo {

    private ExtractorIntervalo() {
    }

    /** Un numero, con signo y decimales, o una constante como pi o sqrt(2). */
    private static final String NUMERO = "(-?\\s*(?:\\d+(?:[.,]\\d+)?|pi|sqrt\\(\\d+\\)|e)(?:\\s*/\\s*\\d+)?)";

    /** [0, 1] y (0; 1). */
    private static final Pattern CORCHETES = Pattern.compile(
            "[\\[(]\\s*" + NUMERO + "\\s*[,;]\\s*" + NUMERO + "\\s*[\\])]");

    /** "entre x = 0 y x = 4", "de x = 0 a x = 2", "desde y = 1 hasta y = 3". */
    private static final Pattern CON_VARIABLE = Pattern.compile(
            "\\b(?:de|desde|entre)\\s+([xy])\\s*=\\s*" + NUMERO
                    + "\\s+(?:a|hasta|y)\\s+(?:\\1\\s*=\\s*)?" + NUMERO);

    /** "de 0 a 4", "desde 0 hasta 4", "entre 0 y 4". */
    private static final Pattern EN_PALABRAS = Pattern.compile(
            "\\b(?:de|desde|entre)\\s+" + NUMERO + "\\s+(?:a|hasta|y)\\s+" + NUMERO + "(?![\\w^(])");

    /** "0 <= x <= 1", "0 ≤ x ≤ 1", "0 < x < 1". */
    private static final Pattern DESIGUALDAD_DOBLE = Pattern.compile(
            NUMERO + "\\s*(?:<=|\u2264|<|=<)\\s*([xy])\\s*(?:<=|\u2264|<|=<)\\s*" + NUMERO);

    /** "x <= 1", "x ≤ 1". */
    private static final Pattern MENOR_SIMBOLO = Pattern.compile(
            "\\b([xy])\\s*(?:<=|\u2264|<|=<)\\s*" + NUMERO);

    /** "x >= 0", "x ≥ 0". */
    private static final Pattern MAYOR_SIMBOLO = Pattern.compile(
            "\\b([xy])\\s*(?:>=|\u2265|>|=>)\\s*" + NUMERO);

    /** "x menor o igual a 1", "x menor e igual q 1", "x es menor que 1". */
    private static final Pattern MENOR_PALABRAS = Pattern.compile(
            "\\b([xy])\\s+(?:es\\s+)?menor\\s+(?:(?:o|e|y)\\s+igual\\s+)?(?:a|que|q|de)?\\s*" + NUMERO);

    /** "x mayor o igual a 0", "x mayor e igual q 0". */
    private static final Pattern MAYOR_PALABRAS = Pattern.compile(
            "\\b([xy])\\s+(?:es\\s+)?mayor\\s+(?:(?:o|e|y)\\s+igual\\s+)?(?:a|que|q|de)?\\s*" + NUMERO);

    /**
     * Un intervalo encontrado.
     *
     * @param inferior el limite inferior
     * @param superior el limite superior
     * @param variable la variable a la que se refiere, "x" o "y"
     * @param origen   como estaba escrito, para explicarlo
     * @param inicio   donde empieza en el texto
     * @param fin      donde termina
     */
    public record Intervalo(double inferior, double superior, String variable, String origen,
                            int inicio, int fin) {
    }

    /**
     * Busca el intervalo.
     *
     * @param normalizado enunciado en minusculas y sin tildes, ya corregido
     * @return el intervalo, o vacio si el enunciado no lo escribe
     */
    public static Optional<Intervalo> buscar(String normalizado) {
        if (normalizado == null || normalizado.isBlank()) {
            return Optional.empty();
        }

        Matcher corchetes = CORCHETES.matcher(normalizado);
        while (corchetes.find()) {
            Optional<Intervalo> encontrado = construir(corchetes.group(1), corchetes.group(2), "x",
                    "el enunciado da el intervalo entre corchetes: [" + corchetes.group(1).trim()
                            + ", " + corchetes.group(2).trim() + "]",
                    corchetes.start(), corchetes.end());
            if (encontrado.isPresent()) {
                return encontrado;
            }
        }

        Matcher doble = DESIGUALDAD_DOBLE.matcher(normalizado);
        if (doble.find()) {
            Optional<Intervalo> encontrado = construir(doble.group(1), doble.group(3), doble.group(2),
                    "el enunciado acota la variable con la desigualdad \"" + doble.group().trim() + "\"",
                    doble.start(), doble.end());
            if (encontrado.isPresent()) {
                return encontrado;
            }
        }

        Optional<Intervalo> porPalabras = desigualdadesSueltas(normalizado,
                MENOR_PALABRAS, MAYOR_PALABRAS);
        if (porPalabras.isPresent()) {
            return porPalabras;
        }
        Optional<Intervalo> porSimbolos = desigualdadesSueltas(normalizado,
                MENOR_SIMBOLO, MAYOR_SIMBOLO);
        if (porSimbolos.isPresent()) {
            return porSimbolos;
        }

        Matcher conVariable = CON_VARIABLE.matcher(normalizado);
        if (conVariable.find()) {
            Optional<Intervalo> encontrado = construir(conVariable.group(2), conVariable.group(3),
                    conVariable.group(1),
                    "el enunciado describe el intervalo en palabras: \"" + conVariable.group().trim() + "\"",
                    conVariable.start(), conVariable.end());
            if (encontrado.isPresent()) {
                return encontrado;
            }
        }

        Matcher palabras = EN_PALABRAS.matcher(normalizado);
        while (palabras.find()) {
            Optional<Intervalo> encontrado = construir(palabras.group(1), palabras.group(2), "x",
                    "el enunciado describe el intervalo en palabras: \"" + palabras.group().trim() + "\"",
                    palabras.start(), palabras.end());
            if (encontrado.isPresent()) {
                return encontrado;
            }
        }
        return Optional.empty();
    }

    /**
     * Junta una cota superior y una inferior escritas por separado.
     *
     * <p>"x menor o igual a 1 y x mayor o igual a 0" son dos frases, y solo juntas dicen
     * que el intervalo es {@code [0, 1]}. Si falta una de las dos, no hay intervalo: una
     * sola cota no acota la region por los dos lados.</p>
     */
    private static Optional<Intervalo> desigualdadesSueltas(String texto, Pattern menor, Pattern mayor) {
        Matcher cotaSuperior = menor.matcher(texto);
        Matcher cotaInferior = mayor.matcher(texto);
        if (!cotaSuperior.find() || !cotaInferior.find()) {
            return Optional.empty();
        }
        if (!cotaSuperior.group(1).equals(cotaInferior.group(1))) {
            return Optional.empty();
        }
        int inicio = Math.min(cotaSuperior.start(), cotaInferior.start());
        int fin = Math.max(cotaSuperior.end(), cotaInferior.end());
        return construir(cotaInferior.group(2), cotaSuperior.group(2), cotaSuperior.group(1),
                "el enunciado acota la variable: \"" + cotaInferior.group().trim() + "\" y \""
                        + cotaSuperior.group().trim() + "\"",
                inicio, fin);
    }

    private static Optional<Intervalo> construir(String textoInferior, String textoSuperior,
                                                 String variable, String origen, int inicio, int fin) {
        Optional<Double> inferior = ExtractorDatos.comoNumero(textoInferior.replace(" ", "").replace(',', '.'));
        Optional<Double> superior = ExtractorDatos.comoNumero(textoSuperior.replace(" ", "").replace(',', '.'));
        if (inferior.isEmpty() || superior.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Intervalo(inferior.get(), superior.get(), variable, origen, inicio, fin));
    }
}
