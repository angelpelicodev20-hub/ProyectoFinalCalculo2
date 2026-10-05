package com.calculo2.integrales.math.parser;

import com.calculo2.integrales.exception.ExpresionInvalidaException;
import com.calculo2.integrales.util.Constantes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parte la expresion escrita por el usuario en tokens.
 *
 * <p>Ademas de separar numeros, nombres y signos, resuelve aqui la multiplicacion
 * implicita: donde el usuario escribe {@code 2x}, {@code 3sin(x)} o {@code (x+1)(x-1)}
 * se inserta un token {@code *}. Hacerlo en esta etapa deja al analizador sintactico
 * con una gramatica limpia.</p>
 */
public final class Tokenizador {

    private final String expresion;
    private final Set<String> variables;
    private final List<Token> tokens = new ArrayList<>();
    private int posicion;

    /**
     * @param expresion texto escrito por el usuario
     * @param variable  nombre de la variable independiente, normalmente "x" o "y"
     */
    public Tokenizador(String expresion, String variable) {
        this(expresion, Set.of(variable == null ? "x" : variable.toLowerCase()));
    }

    /**
     * Variante con varias variables a la vez.
     *
     * <p>La usan las ecuaciones que relacionan x e y sin estar despejadas, como
     * {@code 4x^2 + 9y^2 = 36}: para poder despejar una de las dos hay que leerlas
     * primero, y en ellas las dos letras son variables legitimas.</p>
     *
     * @param expresion texto escrito por el usuario
     * @param variables nombres de las variables admitidas
     */
    public Tokenizador(String expresion, Set<String> variables) {
        this.expresion = expresion == null ? "" : expresion;
        this.variables = Set.copyOf(variables);
    }

    /**
     * Recorre la expresion completa y devuelve la lista de tokens, terminada en
     * un token {@link Token.Tipo#FIN}.
     *
     * @throws ExpresionInvalidaException si aparece un caracter o un nombre desconocido
     */
    public List<Token> tokenizar() {
        validarLongitud();

        while (posicion < expresion.length()) {
            char c = expresion.charAt(posicion);

            if (Character.isWhitespace(c)) {
                posicion++;
                continue;
            }
            if (Character.isDigit(c) || c == '.') {
                leerNumero();
                continue;
            }
            if (Character.isLetter(c)) {
                leerNombre();
                continue;
            }
            leerSimbolo(c);
        }

        tokens.add(Token.de(Token.Tipo.FIN, "", posicion));
        return tokens;
    }

    private void validarLongitud() {
        if (expresion.isBlank()) {
            throw new ExpresionInvalidaException("Escriba una funcion antes de calcular.");
        }
        if (expresion.length() > Constantes.LONGITUD_MAXIMA_EXPRESION) {
            throw new ExpresionInvalidaException(
                    "La funcion es demasiado larga; el maximo es "
                            + Constantes.LONGITUD_MAXIMA_EXPRESION + " caracteres.");
        }
    }

    // ------------------------------------------------------------------
    // LECTURA DE CADA CLASE DE TOKEN
    // ------------------------------------------------------------------

    private void leerNumero() {
        insertarMultiplicacionImplicitaSiHaceFalta();

        int inicio = posicion;
        boolean puntoUsado = false;

        while (posicion < expresion.length()) {
            char c = expresion.charAt(posicion);
            if (Character.isDigit(c)) {
                posicion++;
            } else if (c == '.' && !puntoUsado) {
                puntoUsado = true;
                posicion++;
            } else {
                break;
            }
        }

        // Notacion cientifica: 1e-3, 2.5E+8.
        if (posicion < expresion.length() && esInicioDeExponente()) {
            posicion++;
            if (posicion < expresion.length()
                    && (expresion.charAt(posicion) == '+' || expresion.charAt(posicion) == '-')) {
                posicion++;
            }
            while (posicion < expresion.length() && Character.isDigit(expresion.charAt(posicion))) {
                posicion++;
            }
        }

        String texto = expresion.substring(inicio, posicion);
        try {
            tokens.add(Token.numero(Double.parseDouble(texto), texto, inicio));
        } catch (NumberFormatException e) {
            throw new ExpresionInvalidaException("El numero '" + texto + "' no es valido.", inicio);
        }
    }

    /**
     * Una 'e' solo abre un exponente si despues viene un digito o un signo seguido de
     * digito. Asi {@code 2e} se lee como {@code 2 * e} (el numero de Euler) y
     * {@code 2e5} como el numero doscientos mil.
     */
    private boolean esInicioDeExponente() {
        char c = expresion.charAt(posicion);
        if (c != 'e' && c != 'E') {
            return false;
        }
        int siguiente = posicion + 1;
        if (siguiente >= expresion.length()) {
            return false;
        }
        char s = expresion.charAt(siguiente);
        if (Character.isDigit(s)) {
            return true;
        }
        return (s == '+' || s == '-')
                && siguiente + 1 < expresion.length()
                && Character.isDigit(expresion.charAt(siguiente + 1));
    }

    /**
     * Lee una secuencia de letras y la reparte en tokens.
     *
     * <p>Si toda la secuencia es un nombre conocido se emite un solo token. Si no, se
     * va tomando el prefijo conocido mas largo, que es lo que permite escribir
     * {@code xsin(x)} o {@code 2xy}. Si algun trozo no se reconoce, se avisa al usuario.</p>
     */
    private void leerNombre() {
        int inicio = posicion;
        while (posicion < expresion.length() && Character.isLetter(expresion.charAt(posicion))) {
            posicion++;
        }
        String bloque = expresion.substring(inicio, posicion).toLowerCase();

        int desplazamiento = 0;
        while (desplazamiento < bloque.length()) {
            int largo = mayorPrefijoConocido(bloque, desplazamiento);
            if (largo == 0) {
                String desconocido = bloque.substring(desplazamiento);
                throw new ExpresionInvalidaException(
                        "No reconozco '" + desconocido + "'. Revise el nombre de la funcion o la variable.",
                        inicio + desplazamiento);
            }
            String nombre = bloque.substring(desplazamiento, desplazamiento + largo);
            agregarNombre(nombre, inicio + desplazamiento);
            desplazamiento += largo;
        }
    }

    /**
     * Devuelve la longitud del prefijo conocido mas largo que empieza en el indice dado,
     * o cero si ninguno lo es.
     */
    private int mayorPrefijoConocido(String bloque, int desde) {
        for (int largo = bloque.length() - desde; largo >= 1; largo--) {
            String candidato = bloque.substring(desde, desde + largo);
            if (esNombreConocido(candidato)) {
                return largo;
            }
        }
        return 0;
    }

    private boolean esNombreConocido(String nombre) {
        return variables.contains(nombre)
                || CatalogoFunciones.esFuncion(nombre)
                || CatalogoFunciones.esConstante(nombre);
    }

    private void agregarNombre(String nombre, int posicionNombre) {
        insertarMultiplicacionImplicitaSiHaceFalta();

        if (CatalogoFunciones.esFuncion(nombre)) {
            tokens.add(Token.de(Token.Tipo.FUNCION, nombre, posicionNombre));
        } else if (variables.contains(nombre)) {
            tokens.add(Token.de(Token.Tipo.VARIABLE, nombre, posicionNombre));
        } else {
            tokens.add(Token.de(Token.Tipo.CONSTANTE, nombre, posicionNombre));
        }
    }

    private void leerSimbolo(char c) {
        switch (c) {
            case '+', '-', '/', '%' -> {
                tokens.add(Token.de(Token.Tipo.OPERADOR, String.valueOf(c), posicion));
                posicion++;
            }
            case '*' -> {
                // "**" es otra forma de escribir la potencia.
                boolean doble = posicion + 1 < expresion.length() && expresion.charAt(posicion + 1) == '*';
                tokens.add(Token.de(Token.Tipo.OPERADOR, doble ? "^" : "*", posicion));
                posicion += doble ? 2 : 1;
            }
            case '^' -> {
                tokens.add(Token.de(Token.Tipo.OPERADOR, "^", posicion));
                posicion++;
            }
            case '(', '[', '{' -> {
                insertarMultiplicacionImplicitaSiHaceFalta();
                tokens.add(Token.de(Token.Tipo.PARENTESIS_IZQUIERDO, "(", posicion));
                posicion++;
            }
            case ')', ']', '}' -> {
                tokens.add(Token.de(Token.Tipo.PARENTESIS_DERECHO, ")", posicion));
                posicion++;
            }
            case ',', ';' -> {
                tokens.add(Token.de(Token.Tipo.COMA, ",", posicion));
                posicion++;
            }
            default -> throw new ExpresionInvalidaException(
                    "El caracter '" + c + "' no se puede usar en una funcion.", posicion);
        }
    }

    // ------------------------------------------------------------------
    // MULTIPLICACION IMPLICITA
    // ------------------------------------------------------------------

    /**
     * Inserta un {@code *} cuando el token anterior cierra un valor y el que viene
     * abre otro: {@code 2x}, {@code 2(x+1)}, {@code (x+1)(x-1)}, {@code x sin(x)}.
     */
    private void insertarMultiplicacionImplicitaSiHaceFalta() {
        if (tokens.isEmpty()) {
            return;
        }
        Token anterior = tokens.get(tokens.size() - 1);
        boolean cierraUnValor = switch (anterior.tipo()) {
            case NUMERO, VARIABLE, CONSTANTE, PARENTESIS_DERECHO -> true;
            default -> false;
        };
        if (cierraUnValor) {
            tokens.add(Token.de(Token.Tipo.OPERADOR, "*", posicion));
        }
    }
}
