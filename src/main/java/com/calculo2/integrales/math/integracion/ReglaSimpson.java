package com.calculo2.integrales.math.integracion;

import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.math.util.ValidadorFuncion;

/**
 * Regla de Simpson (un tercio).
 *
 * <p>Toma los puntos de tres en tres y hace pasar una parabola por ellos, en vez de una
 * recta. Como la mayoria de las funciones del curso se parecen localmente a una
 * parabola, el resultado es mucho mas preciso que el trapecio con el mismo numero de
 * particiones.</p>
 *
 * <pre>
 *   A = (h / 3) * [ f(x_0) + 4*f(x_1) + 2*f(x_2) + 4*f(x_3) + ... + 4*f(x_{n-1}) + f(x_n) ]
 * </pre>
 *
 * <p>Los coeficientes alternan 4 y 2 en los puntos interiores. El numero de particiones
 * debe ser par, porque cada parabola consume dos subintervalos.</p>
 *
 * <p>Es exacta hasta polinomios de grado tres y su error decrece como {@code h^4}: por eso
 * es el metodo por defecto de la aplicacion.</p>
 */
public final class ReglaSimpson implements MetodoIntegracion {

    @Override
    public double integrar(FuncionMatematica funcion, double a, double b, int particiones) {
        int n = (particiones % 2 == 0) ? particiones : particiones + 1;
        double h = (b - a) / n;

        // Los extremos llevan coeficiente 1.
        double suma = valorEn(funcion, a) + valorEn(funcion, b);

        for (int k = 1; k < n; k++) {
            double coeficiente = (k % 2 == 0) ? 2.0 : 4.0;
            suma += coeficiente * valorEn(funcion, a + k * h);
        }

        return suma * h / 3.0;
    }

    private static double valorEn(FuncionMatematica funcion, double x) {
        double valor = ValidadorFuncion.evaluarSeguro(funcion, x);
        return Double.isNaN(valor) ? 0.0 : valor;
    }

    @Override
    public boolean exigeParticionesPares() {
        return true;
    }

    @Override
    public String nombre() {
        return "Regla de Simpson";
    }

    @Override
    public String formula() {
        return "A = (h/3) * [ f(x0) + 4*f(x1) + 2*f(x2) + ... + 4*f(x_{n-1}) + f(xn) ]";
    }

    @Override
    public String descripcion() {
        return "Aproxima la curva con parabolas que pasan por cada tres puntos consecutivos. "
                + "Es exacta para polinomios hasta grado tres, asi que en la mayoria de ejercicios "
                + "del curso devuelve el resultado exacto. Necesita un numero par de particiones.";
    }

    @Override
    public int ordenDelError() {
        return 4;
    }
}
