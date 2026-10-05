package com.calculo2.integrales.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un renglon de la explicacion paso a paso que acompana al resultado.
 *
 * <p>La idea es que el alumno pueda seguir el procedimiento como lo escribiria en el
 * cuaderno: primero el planteamiento, luego la formula, luego la sustitucion y al final
 * el resultado. Cada paso lleva un titulo corto, una explicacion en palabras y, cuando
 * corresponde, la expresion matematica de ese momento.</p>
 *
 * @param numero      posicion del paso, empezando en 1
 * @param titulo      encabezado corto, por ejemplo "Plantear la integral"
 * @param explicacion que se hace en este paso y por que
 * @param expresion   la formula o el calculo de este paso; puede ir vacia
 * @param resultado   el valor obtenido, si el paso produce uno; puede ir vacio
 */
public record PasoSolucion(int numero, String titulo, String explicacion,
                           String expresion, String resultado) {

    /** Crea un paso que solo explica, sin formula. */
    public static PasoSolucion soloTexto(int numero, String titulo, String explicacion) {
        return new PasoSolucion(numero, titulo, explicacion, "", "");
    }

    /** Crea un paso con explicacion y formula, sin valor final. */
    public static PasoSolucion conExpresion(int numero, String titulo, String explicacion,
                                            String expresion) {
        return new PasoSolucion(numero, titulo, explicacion, expresion, "");
    }

    /** Representacion para el JSON que consume el panel de pasos. */
    public Map<String, Object> aMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("numero", numero);
        mapa.put("titulo", titulo);
        mapa.put("explicacion", explicacion);
        mapa.put("expresion", expresion);
        mapa.put("resultado", resultado);
        return mapa;
    }
}
