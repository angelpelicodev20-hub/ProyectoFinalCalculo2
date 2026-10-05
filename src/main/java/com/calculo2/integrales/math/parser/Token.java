package com.calculo2.integrales.math.parser;

/**
 * Pieza minima en que se parte la expresion escrita por el usuario.
 *
 * @param tipo    categoria de la pieza
 * @param texto   el fragmento tal como aparecio en la expresion
 * @param valor   el numero, cuando el tipo es {@link Tipo#NUMERO}; si no, cero
 * @param posicion indice del primer caracter dentro de la expresion original
 */
public record Token(Tipo tipo, String texto, double valor, int posicion) {

    /** Categorias posibles de un token. */
    public enum Tipo {
        /** Un literal numerico, por ejemplo {@code 3} o {@code 2.5}. */
        NUMERO,
        /** La variable independiente: {@code x} o {@code y}. */
        VARIABLE,
        /** Un nombre reconocido de funcion, por ejemplo {@code sin} o {@code ln}. */
        FUNCION,
        /** Una constante con nombre: {@code pi} o {@code e}. */
        CONSTANTE,
        /** Uno de los signos {@code + - * / ^}. */
        OPERADOR,
        /** Parentesis de apertura. */
        PARENTESIS_IZQUIERDO,
        /** Parentesis de cierre. */
        PARENTESIS_DERECHO,
        /** Separador de argumentos. */
        COMA,
        /** Marca el final de la expresion. */
        FIN
    }

    /** Crea un token que no lleva valor numerico asociado. */
    public static Token de(Tipo tipo, String texto, int posicion) {
        return new Token(tipo, texto, 0.0, posicion);
    }

    /** Crea un token de tipo numero. */
    public static Token numero(double valor, String texto, int posicion) {
        return new Token(Tipo.NUMERO, texto, valor, posicion);
    }

    /** Indica si este token es el operador indicado. */
    public boolean esOperador(String simbolo) {
        return tipo == Tipo.OPERADOR && texto.equals(simbolo);
    }
}
