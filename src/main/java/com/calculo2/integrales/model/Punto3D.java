package com.calculo2.integrales.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un punto del espacio, usado en la malla del solido de revolucion.
 *
 * <p>El eje X corre a lo largo del eje de giro, y los ejes Y y Z forman el circulo que
 * describe cada punto del perfil al girar.</p>
 *
 * @param x coordenada a lo largo del eje de revolucion
 * @param y coordenada vertical
 * @param z coordenada de profundidad
 */
public record Punto3D(double x, double y, double z) {

    /** Indica si el punto se puede dibujar. */
    public boolean esDibujable() {
        return !Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(z)
                && !Double.isInfinite(x) && !Double.isInfinite(y) && !Double.isInfinite(z);
    }

    /** Distancia del punto al eje de revolucion. */
    public double radio() {
        return Math.sqrt(y * y + z * z);
    }

    /** Representacion compacta para el JSON. */
    public Map<String, Object> aMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("x", x);
        mapa.put("y", y);
        mapa.put("z", z);
        return mapa;
    }
}
