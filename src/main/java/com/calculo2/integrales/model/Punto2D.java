package com.calculo2.integrales.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un punto del plano, usado para armar las curvas que se dibujan en el canvas.
 *
 * <p>Cuando la funcion no esta definida en ese punto, {@code y} vale NaN. El JSON lo
 * convierte en null y el frontend corta la linea ahi en lugar de unir dos tramos que no
 * se tocan.</p>
 *
 * @param x coordenada horizontal
 * @param y coordenada vertical, o NaN si la funcion no esta definida
 */
public record Punto2D(double x, double y) {

    /** Indica si el punto se puede dibujar. */
    public boolean esDibujable() {
        return !Double.isNaN(y) && !Double.isInfinite(y);
    }

    /** Representacion compacta para el JSON: un objeto con las dos coordenadas. */
    public Map<String, Object> aMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("x", x);
        mapa.put("y", y);
        return mapa;
    }
}
