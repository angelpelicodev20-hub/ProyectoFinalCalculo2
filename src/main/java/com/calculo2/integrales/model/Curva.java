package com.calculo2.integrales.model;

import com.calculo2.integrales.math.algebra.Despejador;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Una curva del enunciado, tal como se escribio y despejada en cada variable.
 *
 * <p>Una misma curva se puede necesitar de dos maneras. La parabola {@code y^2 = 8x} se
 * usa como {@code y = sqrt(8x)} si se integra respecto de x, y como {@code x = y^2/8} si
 * se integra respecto de y. Cual de las dos hace falta no se sabe hasta decidir como se
 * rebana la region, asi que se guardan las dos, con el procedimiento del despeje para
 * poder ensenarlo.</p>
 *
 * @param orden    posicion de la curva en el enunciado, empezando en cero
 * @param nombre   nombre de funcion si se declaro como f(x) =, o cadena vacia
 * @param ecuacion la ecuacion escrita, por ejemplo {@code y^2 = 8x}
 * @param forma    como venia: "Y_DE_X" para y = f(x), "X_DE_Y" para x = g(y), "IMPLICITA"
 * @param enX      la curva como funcion de x (ramas y = f(x)), o null si no se pudo despejar
 * @param enY      la curva como funcion de y (ramas x = g(y)), o null si no se pudo despejar
 * @param posicion donde aparece en el texto
 */
public record Curva(int orden, String nombre, String ecuacion, String forma,
                    Despejador.Despeje enX, Despejador.Despeje enY, int posicion) {

    /** Indica si la curva se puede escribir como funcion de esa variable. */
    public boolean disponibleEn(String variable) {
        return variable.equals("x") ? enX != null : enY != null;
    }

    /** El despeje en la variable pedida, o null. */
    public Despejador.Despeje despejeEn(String variable) {
        return variable.equals("x") ? enX : enY;
    }

    /** Indica si para usarla en esa variable hubo que despejar. */
    public boolean requiereDespejeEn(String variable) {
        Despejador.Despeje despeje = despejeEn(variable);
        return despeje != null && !despeje.yaEstabaDespejada();
    }

    /** Como se nombra en los textos: la ecuacion, o f(x) = ... si tenia nombre. */
    public String etiqueta() {
        return ecuacion;
    }

    /** Representacion para el navegador. */
    public Map<String, Object> aMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("ecuacion", ecuacion);
        mapa.put("nombre", nombre);
        mapa.put("forma", forma);
        mapa.put("enX", despejeAMapa(enX));
        mapa.put("enY", despejeAMapa(enY));
        return mapa;
    }

    private static Map<String, Object> despejeAMapa(Despejador.Despeje despeje) {
        if (despeje == null) {
            return null;
        }
        Map<String, Object> mapa = new LinkedHashMap<>();
        List<Map<String, Object>> ramas = new ArrayList<>();
        for (Despejador.Rama rama : despeje.ramas()) {
            Map<String, Object> entrada = new LinkedHashMap<>();
            entrada.put("expresion", rama.expresion());
            entrada.put("descripcion", rama.descripcion());
            ramas.add(entrada);
        }
        mapa.put("variable", despeje.variable());
        mapa.put("ramas", ramas);
        mapa.put("pasos", despeje.pasos());
        mapa.put("yaEstabaDespejada", despeje.yaEstabaDespejada());
        return mapa;
    }
}
