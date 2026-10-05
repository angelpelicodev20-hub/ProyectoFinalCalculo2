package com.calculo2.integrales.model;

/**
 * De donde salio cada dato del ejercicio.
 *
 * <p>No es lo mismo un intervalo que el enunciado escribe entre corchetes que uno que
 * salio de resolver {@code f(x) = g(x)}, ni esos dos que la lectura de "la ordenada 2"
 * como la recta {@code x = 2}. Los tres son utilizables, pero la confianza que merecen
 * es distinta y el estudiante tiene derecho a saber cual esta mirando.</p>
 *
 * <p>Distinguirlos es ademas lo que permite no pedir lo que ya se dedujo. Un dato
 * {@link #DERIVADO} esta tan determinado como uno {@link #EXPLICITO}: preguntar por el
 * seria pedirle al estudiante que haga la cuenta que la aplicacion acaba de hacer.</p>
 */
public enum OrigenDato {

    /** El enunciado lo dice con todas las letras. */
    EXPLICITO("dato explicito del enunciado"),

    /** Se obtuvo resolviendo: intersecciones, cortes con el eje, raices. */
    DERIVADO("deducido matematicamente"),

    /**
     * Se obtuvo leyendo una expresion del lenguaje matematico que admite lectura.
     *
     * <p>Es el unico de los cuatro que puede equivocarse sin que haya un error de
     * calculo detras, y por eso se muestra siempre acompanado de la lectura que se
     * hizo.</p>
     */
    INTERPRETADO("interpretado del lenguaje del enunciado"),

    /** Ni esta escrito ni se pudo deducir. */
    FALTANTE("no se pudo determinar");

    private final String etiqueta;

    OrigenDato(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Como se nombra en pantalla. */
    public String etiqueta() {
        return etiqueta;
    }

    /** Indica si el dato quedo determinado, sin importar por que camino. */
    public boolean estaDeterminado() {
        return this != FALTANTE;
    }
}
