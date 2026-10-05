package com.calculo2.integrales.model;

import java.util.Locale;

/**
 * Las formas de resolver un area que el estudiante puede elegir.
 *
 * <p>Antes la aplicacion ofrecia media docena de metodos numericos — trapecio, Simpson,
 * Gauss, adaptativo — y todos respondian lo mismo: un numero aproximado. Eso sirve para
 * programar, pero no para estudiar, porque ninguno deja un procedimiento que se pueda
 * copiar al cuaderno.</p>
 *
 * <p>Quedan solo dos opciones, y cada una responde a una pregunta distinta del curso:</p>
 *
 * <ul>
 *   <li>{@link #ANALITICO} resuelve como en clase, encontrando la primitiva y aplicando
 *       la regla de Barrow. Es el que da el resultado exacto y el procedimiento completo.</li>
 *   <li>{@link #RIEMANN} calcula la suma de rectangulos, que es la definicion de la que
 *       nace la integral. No se conserva por precision, sino porque ver los rectangulos
 *       sobre la curva explica de donde sale la integral mejor que cualquier texto.</li>
 * </ul>
 */
public enum MetodoArea {

    /** Integracion analitica: se busca la primitiva y se aplica la regla de Barrow. */
    ANALITICO("Integracion directa (regla de Barrow)"),

    /** Suma de rectangulos de Riemann, la definicion de la integral definida. */
    RIEMANN("Rectangulos de Riemann");

    private final String etiqueta;

    MetodoArea(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Nombre del metodo tal como se muestra en pantalla. */
    public String etiqueta() {
        return etiqueta;
    }

    /** Indica si el metodo dibuja rectangulos sobre la curva. */
    public boolean dibujaRectangulos() {
        return this == RIEMANN;
    }

    /**
     * Traduce el texto que llego del navegador al metodo correspondiente.
     *
     * @param texto nombre del metodo
     * @return el metodo indicado, o {@link #ANALITICO} si no coincide con ninguno
     */
    public static MetodoArea desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return ANALITICO;
        }
        String normalizado = texto.trim().toUpperCase(Locale.ROOT).replace(' ', '_');

        for (MetodoArea metodo : values()) {
            if (metodo.name().equals(normalizado)) {
                return metodo;
            }
        }
        // Las claves antiguas del selector numerico siguen llegando de enlaces guardados;
        // todas las de Riemann se reconducen a la unica que queda.
        return normalizado.startsWith("RIEMANN") ? RIEMANN : ANALITICO;
    }
}
