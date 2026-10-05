package com.calculo2.integrales.service;

import com.calculo2.integrales.math.parser.EvaluadorExpresion;
import com.calculo2.integrales.math.simbolico.ConversorPolinomio;
import com.calculo2.integrales.math.simbolico.FormateadorMatematico;
import com.calculo2.integrales.math.simbolico.Fraccion;
import com.calculo2.integrales.math.util.Redondeo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Evalua expresiones sueltas, sin resolver ningun ejercicio.
 *
 * <p>Es la modalidad de calculadora. Sirve para las cuentas que aparecen en medio de un
 * problema — cuanto vale {@code pi/3}, cuanto da {@code f(2)} — y se mantiene aparte del
 * solucionador a proposito: ahi la respuesta es un numero y aqui es un procedimiento, y
 * mezclarlas haria que ninguna de las dos quedara clara.</p>
 */
public final class CalculadoraService {

    /**
     * Evalua una expresion.
     *
     * @param expresion   lo que escribio el usuario
     * @param variable    nombre de la variable, normalmente "x"
     * @param valor       valor que toma la variable
     * @param tieneValor  false si la expresion no lleva variables
     * @return el resultado con su interpretacion
     */
    public Map<String, Object> evaluar(String expresion, String variable,
                                       double valor, boolean tieneValor) {
        EvaluadorExpresion compilada = EvaluadorExpresion.compilar(expresion, variable);
        double resultado = compilada.evaluar(tieneValor ? valor : 0);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("exito", true);
        respuesta.put("expresion", compilada.expresionOriginal());
        respuesta.put("interpretacion", FormateadorMatematico.escribir(compilada.raiz()));
        respuesta.put("valor", Redondeo.paraMostrar(resultado));
        respuesta.put("valorTexto", Redondeo.texto(resultado));
        respuesta.put("valorExacto", Fraccion.texto(resultado));
        respuesta.put("variable", variable);
        respuesta.put("evaluadaEn", tieneValor ? valor : null);

        // Cuando la expresion es un polinomio se muestra desarrollado: es util para
        // comprobar a mano un producto de binomios sin tener que resolver nada mas.
        ConversorPolinomio.convertir(compilada.raiz()).ifPresent(polinomio ->
                respuesta.put("desarrollado", polinomio.aTexto(variable)));

        return respuesta;
    }
}
