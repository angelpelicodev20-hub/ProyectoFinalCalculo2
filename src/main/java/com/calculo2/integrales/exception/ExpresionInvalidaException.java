package com.calculo2.integrales.exception;

/**
 * Se lanza cuando la funcion escrita por el usuario no se puede leer:
 * un parentesis sin cerrar, un nombre de funcion desconocido, un simbolo raro.
 *
 * <p>El mensaje esta redactado para mostrarse tal cual en pantalla, asi que debe
 * explicar el problema en terminos que el usuario entienda.</p>
 */
public class ExpresionInvalidaException extends RuntimeException {

    private final int posicion;

    /**
     * @param mensaje explicacion para el usuario
     */
    public ExpresionInvalidaException(String mensaje) {
        this(mensaje, -1);
    }

    /**
     * @param mensaje  explicacion para el usuario
     * @param posicion indice del caracter que causo el problema, o -1 si no aplica
     */
    public ExpresionInvalidaException(String mensaje, int posicion) {
        super(mensaje);
        this.posicion = posicion;
    }

    /** Indice del caracter problematico dentro de la expresion, o -1 si no se conoce. */
    public int getPosicion() {
        return posicion;
    }
}
