package com.calculo2.integrales.util;

import com.calculo2.integrales.math.parser.NodoExpresion;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convierte el arbol de una expresion en un objeto JSON que el navegador puede evaluar.
 *
 * <p>La grafica interactiva necesita la funcion, no solo unos cuantos puntos: al alejar o
 * desplazar la vista hay que dibujar la curva en zonas que no se habian calculado. En vez
 * de pedirle al servidor puntos nuevos en cada movimiento, se manda el arbol que ya
 * construyo el parser de Java, y el navegador solo lo recorre.</p>
 *
 * <p>No hay un segundo parser en JavaScript: la expresion se lee una sola vez, aqui, con
 * las mismas reglas que usa el calculo. El navegador recibe algo ya interpretado y no
 * puede entenderlo de otra manera.</p>
 *
 * <pre>
 *   {"t":"n","v":2}              numero
 *   {"t":"v"}                    la variable
 *   {"t":"-","a":...}            cambio de signo
 *   {"t":"b","o":"^","a":...,"b":...}   operacion
 *   {"t":"f","f":"sqrt","a":...} funcion del catalogo
 * </pre>
 */
public final class ArbolJson {

    private ArbolJson() {
    }

    /**
     * Convierte un nodo, con todos sus hijos.
     *
     * @param nodo la raiz de la expresion
     * @return el objeto listo para {@link JsonUtil#escribir(Object)}
     */
    public static Map<String, Object> aMapa(NodoExpresion nodo) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        switch (nodo) {
            case NodoExpresion.Numero numero -> {
                mapa.put("t", "n");
                mapa.put("v", numero.valor());
            }
            case NodoExpresion.Constante constante -> {
                mapa.put("t", "n");
                mapa.put("v", constante.valor());
            }
            case NodoExpresion.Variable ignorada -> mapa.put("t", "v");
            case NodoExpresion.Negacion negacion -> {
                mapa.put("t", "-");
                mapa.put("a", aMapa(negacion.operando()));
            }
            case NodoExpresion.OperacionBinaria operacion -> {
                mapa.put("t", "b");
                mapa.put("o", operacion.simbolo());
                mapa.put("a", aMapa(operacion.izquierda()));
                mapa.put("b", aMapa(operacion.derecha()));
            }
            case NodoExpresion.LlamadaFuncion llamada -> {
                mapa.put("t", "f");
                mapa.put("f", llamada.nombre());
                mapa.put("a", aMapa(llamada.argumento()));
            }
        }
        return mapa;
    }
}
