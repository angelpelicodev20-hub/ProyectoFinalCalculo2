package com.calculo2.integrales.model;

import com.calculo2.integrales.math.util.Redondeo;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * La recta alrededor de la cual gira la region para formar el solido.
 *
 * <p>El material del curso trabaja con dos casos: eje de revolucion horizontal, que es
 * una recta {@code y = k}, y eje vertical, que es {@code x = k}. El caso mas comun,
 * girar alrededor del eje X, es simplemente {@code y = 0}.</p>
 *
 * <p>Guardar el desplazamiento {@code k} aparte permite resolver los ejercicios donde el
 * giro no es sobre un eje coordenado sino sobre una recta corrida, por ejemplo
 * {@code y = 2}. En esos casos el radio deja de ser {@code f(x)} y pasa a ser
 * {@code |f(x) - k|}.</p>
 *
 * @param orientacion si la recta es horizontal o vertical
 * @param desplazamiento el valor {@code k} de la recta
 */
public record EjeRotacion(Orientacion orientacion, double desplazamiento) {

    /** Las dos direcciones posibles del eje de giro. */
    public enum Orientacion {
        /** Recta de la forma {@code y = k}; se integra respecto a x. */
        HORIZONTAL,
        /** Recta de la forma {@code x = k}; se integra respecto a y. */
        VERTICAL
    }

    /** El eje X, que es el caso mas frecuente en los ejercicios. */
    public static EjeRotacion ejeX() {
        return new EjeRotacion(Orientacion.HORIZONTAL, 0.0);
    }

    /** El eje Y. */
    public static EjeRotacion ejeY() {
        return new EjeRotacion(Orientacion.VERTICAL, 0.0);
    }

    /** Una recta horizontal {@code y = k}. */
    public static EjeRotacion horizontal(double k) {
        return new EjeRotacion(Orientacion.HORIZONTAL, k);
    }

    /** Una recta vertical {@code x = k}. */
    public static EjeRotacion vertical(double k) {
        return new EjeRotacion(Orientacion.VERTICAL, k);
    }

    /**
     * Interpreta el eje que llego del formulario.
     *
     * <p>Acepta las formas que el usuario escribiria de manera natural: {@code "x"},
     * {@code "y"}, {@code "y=2"}, {@code "x = -1"}.</p>
     *
     * <p>Hay que distinguir dos lecturas de la misma letra, porque significan cosas
     * opuestas. Con signo igual, la letra nombra la variable de la ecuacion de una recta:
     * {@code x = 2} es una recta <em>vertical</em>. Sin signo igual, la letra nombra un
     * eje coordenado: girar alrededor de {@code "x"} es girar alrededor del eje X, que es
     * una recta <em>horizontal</em>.</p>
     *
     * @param texto           descripcion del eje
     * @param desplazamiento  valor de {@code k} enviado en un campo aparte
     * @return el eje correspondiente; si el texto no se entiende, devuelve el eje X
     */
    public static EjeRotacion desdeTexto(String texto, double desplazamiento) {
        if (texto == null || texto.isBlank()) {
            return horizontal(desplazamiento);
        }
        String limpio = texto.trim().toLowerCase(Locale.ROOT).replace(" ", "");

        // Ecuacion de una recta: "y=2" es horizontal, "x=-1" es vertical.
        // El numero escrito aqui gana sobre el campo aparte.
        int igual = limpio.indexOf('=');
        if (igual > 0) {
            String ladoIzquierdo = limpio.substring(0, igual);
            try {
                double valor = Double.parseDouble(limpio.substring(igual + 1));
                return ladoIzquierdo.startsWith("x") ? vertical(valor) : horizontal(valor);
            } catch (NumberFormatException e) {
                // Si el numero no se entiende, se usa el desplazamiento del otro campo.
            }
        }

        // Nombre de un eje coordenado: "x" es el eje X, que corre en horizontal.
        boolean esVertical = limpio.startsWith("y") || limpio.startsWith("vertical");
        return esVertical ? vertical(desplazamiento) : horizontal(desplazamiento);
    }

    /** Indica si el giro es alrededor de una recta horizontal. */
    public boolean esHorizontal() {
        return orientacion == Orientacion.HORIZONTAL;
    }

    /** Indica si el eje coincide con un eje coordenado, es decir, si {@code k} es cero. */
    public boolean pasaPorElOrigen() {
        return Math.abs(desplazamiento) < 1e-12;
    }

    /**
     * Calcula el radio de giro de un punto del perfil.
     *
     * @param valorDeLaFuncion altura de la curva en ese punto
     * @return la distancia de ese punto al eje de revolucion
     */
    public double radioDesde(double valorDeLaFuncion) {
        return Math.abs(valorDeLaFuncion - desplazamiento);
    }

    /** La ecuacion de la recta, para mostrarla en los pasos y en la grafica. */
    public String ecuacion() {
        String variable = esHorizontal() ? "y" : "x";
        return variable + " = " + Redondeo.texto(desplazamiento);
    }

    /** Nombre del eje para los textos explicativos. */
    public String nombreLargo() {
        if (pasaPorElOrigen()) {
            return esHorizontal() ? "el eje X" : "el eje Y";
        }
        return "la recta " + ecuacion();
    }

    /** Representacion para el JSON que consume el frontend. */
    public Map<String, Object> aMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("orientacion", orientacion.name());
        mapa.put("desplazamiento", desplazamiento);
        mapa.put("ecuacion", ecuacion());
        return mapa;
    }
}
