package com.calculo2.integrales.math.util;

import com.calculo2.integrales.exception.LimitesInvalidosException;
import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.util.Constantes;

/**
 * Revisa que los datos de una peticion tengan sentido antes de calcular.
 *
 * <p>Se ocupa de dos cosas distintas: que los numeros del formulario sean coherentes
 * (limites, particiones) y que la funcion realmente se pueda integrar en el intervalo
 * pedido. Lo segundo se comprueba muestreando el intervalo, porque no hay forma de
 * saberlo mirando la expresion.</p>
 */
public final class ValidadorFuncion {

    private ValidadorFuncion() {
    }

    /** Cuantos puntos se prueban al revisar si la funcion es integrable. */
    private static final int MUESTRAS = 200;

    /**
     * Comprueba que los limites de integracion sean utilizables.
     *
     * @param a limite inferior
     * @param b limite superior
     * @throws LimitesInvalidosException si algun limite no es un numero finito o si son iguales
     */
    public static void validarLimites(double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            throw new LimitesInvalidosException("Los limites de integracion deben ser numeros.");
        }
        if (Double.isInfinite(a) || Double.isInfinite(b)) {
            throw new LimitesInvalidosException(
                    "Esta aplicacion resuelve integrales definidas; los limites deben ser finitos.");
        }
        if (Math.abs(b - a) < 1e-12) {
            throw new LimitesInvalidosException(
                    "El limite inferior y el superior son iguales, asi que la region no tiene area.");
        }
    }

    /**
     * Comprueba que el numero de particiones este dentro del rango permitido.
     *
     * @param particiones valor recibido del formulario
     * @param exigirPar   true cuando el metodo elegido necesita un numero par, como Simpson
     * @return el numero de particiones ya ajustado
     * @throws LimitesInvalidosException si el valor esta fuera de rango
     */
    public static int validarParticiones(int particiones, boolean exigirPar) {
        if (particiones < Constantes.PARTICIONES_MINIMAS) {
            throw new LimitesInvalidosException(
                    "El numero de particiones debe ser al menos " + Constantes.PARTICIONES_MINIMAS + ".");
        }
        if (particiones > Constantes.PARTICIONES_MAXIMAS) {
            throw new LimitesInvalidosException(
                    "El numero de particiones no puede pasar de " + Constantes.PARTICIONES_MAXIMAS
                            + "; con ese valor el calculo tardaria demasiado.");
        }
        if (exigirPar && particiones % 2 != 0) {
            // La regla de Simpson agrupa los subintervalos de dos en dos.
            return particiones + 1;
        }
        return particiones;
    }

    /**
     * Recorre el intervalo evaluando la funcion para detectar problemas antes de integrar.
     *
     * @param funcion  funcion a revisar
     * @param a        limite inferior
     * @param b        limite superior
     * @param nombre   como llamar a la funcion en el mensaje de error, por ejemplo "f(x)"
     * @throws LimitesInvalidosException si la funcion no esta definida o se dispara en el intervalo
     */
    public static void validarIntegrable(FuncionMatematica funcion, double a, double b, String nombre) {
        double inicio = Math.min(a, b);
        double fin = Math.max(a, b);
        double paso = (fin - inicio) / MUESTRAS;

        int indefinidos = 0;
        int divergentes = 0;

        for (int i = 0; i <= MUESTRAS; i++) {
            double x = inicio + i * paso;
            double valor = evaluarSeguro(funcion, x);

            if (Double.isNaN(valor)) {
                indefinidos++;
            } else if (Math.abs(valor) > Constantes.LIMITE_DIVERGENCIA) {
                divergentes++;
            }
        }

        if (indefinidos > MUESTRAS / 2) {
            throw new LimitesInvalidosException(
                    nombre + " no esta definida en la mayor parte del intervalo ["
                            + Redondeo.texto(inicio) + ", " + Redondeo.texto(fin)
                            + "]. Revise el dominio: por ejemplo, ln(x) necesita x mayor que cero"
                            + " y sqrt(x) necesita x mayor o igual a cero.");
        }
        if (divergentes > 0) {
            throw new LimitesInvalidosException(
                    nombre + " se dispara al infinito dentro de ["
                            + Redondeo.texto(inicio) + ", " + Redondeo.texto(fin)
                            + "]. Hay una asintota en ese intervalo, asi que la integral definida"
                            + " no se puede calcular con estos limites.");
        }
    }

    /**
     * Evalua la funcion sin dejar que una excepcion interrumpa el recorrido.
     *
     * @param funcion funcion a evaluar
     * @param x       punto
     * @return el valor, o NaN si la evaluacion fallo
     */
    public static double evaluarSeguro(FuncionMatematica funcion, double x) {
        try {
            return funcion.evaluar(x);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    /**
     * Indica si el valor sirve para dibujar o integrar.
     *
     * @param valor resultado de evaluar la funcion
     * @return true si es un numero finito y de magnitud razonable
     */
    public static boolean esUtilizable(double valor) {
        return !Double.isNaN(valor)
                && !Double.isInfinite(valor)
                && Math.abs(valor) < Constantes.LIMITE_DIVERGENCIA;
    }
}
