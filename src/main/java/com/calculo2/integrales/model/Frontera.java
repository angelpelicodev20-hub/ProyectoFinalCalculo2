package com.calculo2.integrales.model;

import com.calculo2.integrales.math.util.Redondeo;


/**
 * Una recta que limita la region, que no es lo mismo que el eje de revolucion.
 *
 * <p>Esta distincion es la que evita el error mas grave que puede cometer el analizador.
 * En</p>
 *
 * <pre>Hallar el volumen generado por la rotacion limitada por y = 4x - x^2 y la ordenada 2</pre>
 *
 * <p>la recta {@code x = 2} cierra la region por un costado. No dice alrededor de que
 * gira esa region: son dos papeles distintos, y confundirlos produce un solido que no es
 * el del ejercicio. Una misma ecuacion, {@code x = 2}, puede desempenar cualquiera de los
 * dos papeles segun como la introduzca el enunciado, y por eso el papel se guarda aparte
 * del valor.</p>
 *
 * @param orientacion si la recta es vertical ({@code x = k}) u horizontal ({@code y = k})
 * @param valor       el valor de k
 * @param origen      si estaba escrita o si hubo que interpretarla
 * @param lectura     como se leyo, para poder mostrarselo al estudiante
 */
public record Frontera(Orientacion orientacion, double valor, OrigenDato origen, String lectura) {

    /** Las dos direcciones que puede tener una recta que limita la region. */
    public enum Orientacion {
        /** {@code x = k}: limita la region por la izquierda o por la derecha. */
        VERTICAL,
        /** {@code y = k}: limita la region por arriba o por abajo. */
        HORIZONTAL
    }

    /** Una recta vertical {@code x = k}. */
    public static Frontera vertical(double k, OrigenDato origen, String lectura) {
        return new Frontera(Orientacion.VERTICAL, k, origen, lectura);
    }

    /** Una recta horizontal {@code y = k}. */
    public static Frontera horizontal(double k, OrigenDato origen, String lectura) {
        return new Frontera(Orientacion.HORIZONTAL, k, origen, lectura);
    }

    /** Indica si la recta es vertical. */
    public boolean esVertical() {
        return orientacion == Orientacion.VERTICAL;
    }

    /** La ecuacion de la recta, tal como se escribe. */
    public String ecuacion() {
        return (esVertical() ? "x = " : "y = ") + Redondeo.texto(valor);
    }
}
