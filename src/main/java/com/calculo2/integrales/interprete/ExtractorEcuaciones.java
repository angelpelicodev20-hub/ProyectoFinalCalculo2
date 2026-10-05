package com.calculo2.integrales.interprete;

import com.calculo2.integrales.math.parser.CatalogoFunciones;
import com.calculo2.integrales.math.parser.EvaluadorExpresion;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encuentra todas las ecuaciones escritas en el enunciado, esten o no despejadas.
 *
 * <p>Antes solo se buscaban encabezados del tipo {@code f(x) =} o {@code y =}, y eso
 * dejaba fuera la mitad de los ejercicios del curso: {@code y^2 = 8x},
 * {@code 4x^2 + 9y^2 = 36}, {@code (x - 1)^2 = 20 - 4y}, {@code 4y = 4 - x^2}. Aqui se
 * parte del signo igual y se lee hacia los dos lados mientras lo que aparece pueda ser
 * matematica, cortando en cuanto sale una palabra del enunciado.</p>
 *
 * <p>El caso delicado es la "y", que en espanol es una conjuncion y en matematicas una
 * variable. En {@code "y = 1 y x = 1"} la del medio une dos ecuaciones; en
 * {@code "x = y + 6"} es la variable. Se distinguen por lo que tienen alrededor: una
 * variable no aparece separada por un espacio de un numero que la precede, porque eso
 * seria escribir {@code 1 y} para decir "uno por y", cosa que nadie hace.</p>
 */
public final class ExtractorEcuaciones {

    private ExtractorEcuaciones() {
    }

    /** Las dos variables que pueden aparecer en las curvas. */
    private static final Set<String> VARIABLES = Set.of("x", "y");

    /** Digitos en superindice. */
    private static final String SUPERINDICES = "⁰¹²³⁴⁵⁶⁷⁸⁹";

    /** "f(x)", "g(x)", "h(t)" justo antes del signo igual. */
    private static final Pattern ENCABEZADO = Pattern.compile(
            "(?<![A-Za-z])([A-Za-z])\\s*\\(\\s*([A-Za-z])\\s*\\)\\s*$");

    /**
     * Una ecuacion encontrada en el texto.
     *
     * @param izquierda    el lado izquierdo, como texto ("y", "4x^2 + 9y^2")
     * @param derecha      el lado derecho
     * @param nombre       el nombre de funcion si venia como f(x) =, o cadena vacia
     * @param variableDeclarada la variable del encabezado f(x), o cadena vacia
     * @param inicio       posicion del primer caracter de la ecuacion
     * @param fin          posicion siguiente al ultimo
     */
    public record EcuacionDetectada(String izquierda, String derecha, String nombre,
                                    String variableDeclarada, int inicio, int fin) {

        /** La ecuacion completa, como se escribiria. */
        public String texto() {
            String lado = nombre.isEmpty() ? izquierda : nombre + "(" + variableDeclarada + ")";
            return lado + " = " + derecha;
        }
    }

    /**
     * Busca todas las ecuaciones.
     *
     * @param texto enunciado ya corregido, con su capitalizacion original
     * @return las ecuaciones, en el orden en que aparecen
     */
    public static List<EcuacionDetectada> buscar(String texto) {
        List<EcuacionDetectada> encontradas = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return encontradas;
        }
        String normalizado = ExtractorDatos.normalizar(texto);

        for (int igual = texto.indexOf('='); igual >= 0; igual = texto.indexOf('=', igual + 1)) {
            if (esParteDeUnaDesigualdad(texto, igual)) {
                continue;
            }

            // ---------- Lado izquierdo ----------
            String nombre = "";
            String variableDeclarada = "";
            String izquierda;
            int inicio;

            Matcher encabezado = ENCABEZADO.matcher(texto.substring(Math.max(0, igual - 12), igual));
            if (encabezado.find() && !VARIABLES.contains(encabezado.group(1).toLowerCase())) {
                nombre = encabezado.group(1).toLowerCase();
                variableDeclarada = encabezado.group(2).toLowerCase();
                izquierda = variableDeclarada.equals("y") ? "x" : "y";
                inicio = Math.max(0, igual - 12) + encabezado.start(1);
            } else {
                int[] limites = leerHaciaAtras(texto, normalizado, igual);
                inicio = limites[0];
                izquierda = texto.substring(limites[0], igual).trim();
            }

            // ---------- Lado derecho ----------
            int finDerecha = leerHaciaAdelante(texto, normalizado, igual + 1);
            String derecha = limpiarCola(texto.substring(igual + 1, finDerecha));

            izquierda = limpiarCabeza(izquierda);
            if (izquierda.isEmpty() || derecha.isEmpty()) {
                continue;
            }
            if (!seLee(izquierda) || !seLee(derecha)) {
                continue;
            }
            encontradas.add(new EcuacionDetectada(izquierda, derecha, nombre, variableDeclarada,
                    inicio, igual + 1 + texto.substring(igual + 1, finDerecha).indexOf(derecha)
                            + derecha.length()));
        }
        return encontradas;
    }

    // ------------------------------------------------------------------
    // LECTURA HACIA LA IZQUIERDA
    // ------------------------------------------------------------------

    /** Clase del ultimo elemento leido, para decidir que hace una "y" suelta. */
    private enum Pieza { NADA, OPERANDO, OPERADOR, FUNCION, ABRE }

    /**
     * Lee hacia atras desde el signo igual.
     *
     * @return {inicio} de la expresion de la izquierda
     */
    private static int[] leerHaciaAtras(String texto, String normalizado, int igual) {
        int i = igual - 1;
        int inicio = igual;
        int profundidad = 0;
        // Lo que hay a la derecha de la posicion actual, en el sentido normal de lectura.
        Pieza aLaDerecha = Pieza.OPERADOR;
        boolean huboEspacio = false;

        while (i >= 0) {
            char actual = texto.charAt(i);

            if (Character.isWhitespace(actual)) {
                huboEspacio = true;
                i--;
                continue;
            }

            if (Character.isLetter(actual)) {
                int comienzo = i;
                while (comienzo > 0 && Character.isLetter(texto.charAt(comienzo - 1))) {
                    comienzo--;
                }
                String palabra = normalizado.substring(comienzo, i + 1);

                // "1 y x = 1": la "y" que tiene a su derecha, separada por un espacio, el
                // principio de una expresion es la conjuncion que une dos ecuaciones.
                if (palabra.equals("y") && huboEspacio
                        && (aLaDerecha == Pieza.OPERANDO || aLaDerecha == Pieza.FUNCION
                            || aLaDerecha == Pieza.ABRE)) {
                    break;
                }
                if (!esVocabulario(palabra)) {
                    break;
                }
                inicio = comienzo;
                aLaDerecha = CatalogoFunciones.esFuncion(palabra) ? Pieza.FUNCION : Pieza.OPERANDO;
                huboEspacio = false;
                i = comienzo - 1;
                continue;
            }

            if (Character.isDigit(actual) || actual == '.' || SUPERINDICES.indexOf(actual) >= 0) {
                inicio = i;
                aLaDerecha = Pieza.OPERANDO;
                huboEspacio = false;
                i--;
                continue;
            }

            if ("+-*/^".indexOf(actual) >= 0) {
                inicio = i;
                aLaDerecha = Pieza.OPERADOR;
                huboEspacio = false;
                i--;
                continue;
            }

            if (actual == ')' || actual == ']') {
                profundidad++;
                inicio = i;
                aLaDerecha = Pieza.OPERANDO;
                huboEspacio = false;
                i--;
                continue;
            }

            if (actual == '(' || actual == '[') {
                if (profundidad == 0) {
                    break;
                }
                profundidad--;
                inicio = i;
                aLaDerecha = Pieza.ABRE;
                huboEspacio = false;
                i--;
                continue;
            }

            break;
        }

        // Un parentesis que no se cerro dentro no es de la expresion.
        while (profundidad > 0 && inicio < igual) {
            int cierre = texto.indexOf(')', inicio);
            if (cierre < 0 || cierre >= igual) {
                break;
            }
            inicio = cierre + 1;
            profundidad--;
        }
        return new int[]{inicio};
    }

    // ------------------------------------------------------------------
    // LECTURA HACIA LA DERECHA
    // ------------------------------------------------------------------

    /**
     * Lee hacia adelante desde el signo igual.
     *
     * @return la posicion donde termina la expresion de la derecha
     */
    private static int leerHaciaAdelante(String texto, String normalizado, int desde) {
        int i = desde;
        int fin = desde;
        int profundidad = 0;
        Pieza anterior = Pieza.OPERADOR;
        boolean huboEspacio = false;

        while (i < texto.length()) {
            char actual = texto.charAt(i);

            if (Character.isWhitespace(actual)) {
                huboEspacio = true;
                i++;
                continue;
            }

            if (Character.isLetter(actual)) {
                int final_ = i;
                while (final_ < texto.length() && Character.isLetter(texto.charAt(final_))) {
                    final_++;
                }
                String palabra = normalizado.substring(i, final_);

                // "y = 4x - x^2 y la ordenada 2": la "y" despues de un operando y un
                // espacio no multiplica, une frases.
                if (palabra.equals("y") && huboEspacio && anterior == Pieza.OPERANDO) {
                    break;
                }
                if (!esVocabulario(palabra)) {
                    break;
                }
                anterior = CatalogoFunciones.esFuncion(palabra) ? Pieza.FUNCION : Pieza.OPERANDO;
                huboEspacio = false;
                i = final_;
                fin = i;
                continue;
            }

            if (Character.isDigit(actual) || actual == '.' || SUPERINDICES.indexOf(actual) >= 0) {
                // Un numero que sigue a un operando tras un espacio empieza otra cosa:
                // "y = 1 2x" no es una expresion.
                if (huboEspacio && anterior == Pieza.OPERANDO && Character.isDigit(actual)
                        && !esFinDeNumero(texto, fin)) {
                    break;
                }
                anterior = Pieza.OPERANDO;
                huboEspacio = false;
                i++;
                fin = i;
                continue;
            }

            if ("+-*/^".indexOf(actual) >= 0) {
                anterior = Pieza.OPERADOR;
                huboEspacio = false;
                i++;
                fin = i;
                continue;
            }

            if (actual == '(' || actual == '[') {
                profundidad++;
                anterior = Pieza.ABRE;
                huboEspacio = false;
                i++;
                fin = i;
                continue;
            }

            if (actual == ')' || actual == ']') {
                if (profundidad == 0) {
                    break;
                }
                profundidad--;
                anterior = Pieza.OPERANDO;
                huboEspacio = false;
                i++;
                fin = i;
                continue;
            }

            break;
        }

        // Si quedo un parentesis abierto sin cerrar, la expresion termina antes de el.
        if (profundidad > 0) {
            int nivel = 0;
            for (int k = desde; k < fin; k++) {
                char c = texto.charAt(k);
                if (c == '(' || c == '[') {
                    if (nivel == 0) {
                        int cierre = buscarCierre(texto, k, fin);
                        if (cierre < 0) {
                            return k;
                        }
                        k = cierre;
                        continue;
                    }
                    nivel++;
                }
            }
        }
        return fin;
    }

    private static boolean esFinDeNumero(String texto, int fin) {
        return fin > 0 && texto.charAt(fin - 1) == '.';
    }

    private static int buscarCierre(String texto, int apertura, int limite) {
        int nivel = 0;
        for (int k = apertura; k < limite; k++) {
            char c = texto.charAt(k);
            if (c == '(' || c == '[') {
                nivel++;
            } else if (c == ')' || c == ']') {
                nivel--;
                if (nivel == 0) {
                    return k;
                }
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // APOYO
    // ------------------------------------------------------------------

    /** El signo igual de "&lt;=", "&gt;=" o "==" no separa una ecuacion. */
    private static boolean esParteDeUnaDesigualdad(String texto, int igual) {
        char antes = igual > 0 ? texto.charAt(igual - 1) : ' ';
        char despues = igual + 1 < texto.length() ? texto.charAt(igual + 1) : ' ';
        return antes == '<' || antes == '>' || antes == '!' || antes == '='
                || despues == '=' || despues == '<' || despues == '>';
    }

    /** Las palabras que pueden formar parte de una expresion. */
    private static boolean esVocabulario(String palabra) {
        return VARIABLES.contains(palabra)
                || CatalogoFunciones.esFuncion(palabra)
                || CatalogoFunciones.esConstante(palabra)
                || palabraCompuestaDeVariables(palabra);
    }

    /** "xy" o "yx", que el tokenizador lee como un producto. */
    private static boolean palabraCompuestaDeVariables(String palabra) {
        if (palabra.length() < 2 || palabra.length() > 3) {
            return false;
        }
        for (char c : palabra.toCharArray()) {
            if (c != 'x' && c != 'y') {
                return false;
            }
        }
        return true;
    }

    /** Quita operadores que hayan quedado colgando al final. */
    private static String limpiarCola(String expresion) {
        String texto = expresion.trim();
        while (!texto.isEmpty() && "+-*/^,.".indexOf(texto.charAt(texto.length() - 1)) >= 0) {
            texto = texto.substring(0, texto.length() - 1).trim();
        }
        return texto;
    }

    /** Quita operadores que hayan quedado colgando al principio, salvo el signo menos. */
    private static String limpiarCabeza(String expresion) {
        String texto = expresion.trim();
        while (!texto.isEmpty() && "+*/^,.".indexOf(texto.charAt(0)) >= 0) {
            texto = texto.substring(1).trim();
        }
        return texto;
    }

    /** Comprueba que el parser entienda el lado de la ecuacion. */
    private static boolean seLee(String lado) {
        try {
            EvaluadorExpresion.analizarConVariables(lado, VARIABLES);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
