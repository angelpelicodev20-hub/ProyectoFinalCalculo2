package com.calculo2.integrales.math.util;

import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.model.EjeRotacion;

/**
 * Decide cual de dos curvas va arriba y cual queda mas lejos del eje de giro.
 *
 * <p>Es la pregunta que en clase se responde tomando un punto de prueba dentro del
 * intervalo, y no se puede saltar: el orden en que el estudiante escribio las funciones no
 * dice nada sobre cual esta encima. En {@code y = x^3} y {@code y = x^2} sobre
 * {@code [0, 1]} la segunda va por arriba aunque se haya escrito despues, y tomarlas en el
 * orden de escritura pondria el radio exterior donde va el interior.</p>
 *
 * <p>El efecto de equivocarse no siempre es visible. En arandelas, cambiar {@code R} por
 * {@code r} le da la vuelta al signo de la integral, y un valor absoluto al final devuelve
 * el numero correcto con el procedimiento equivocado: la formula escrita dice una cosa y
 * la cuenta hace otra. Por eso el orden se decide aqui, antes de construir el
 * integrando.</p>
 *
 * <p>La comparacion se hace muestreando el intervalo entero y no un solo punto. Un punto
 * basta cuando las curvas no se cruzan, pero no avisa de si se cruzan, y eso es
 * justamente lo que hay que saber antes de dar por buena una respuesta.</p>
 */
public final class PosicionRelativa {

    private PosicionRelativa() {
    }

    /** Cuantos puntos interiores se examinan. */
    private static final int MUESTRAS = 200;

    /** Diferencias menores que esto se consideran un empate, no una posicion. */
    private static final double TOLERANCIA = 1e-9;

    /**
     * El resultado de comparar dos curvas en un intervalo.
     *
     * @param primeraVaArriba true si la primera curva queda por encima en la mayor parte
     * @param seCruzan        true si intercambian posiciones dentro del intervalo
     * @param puntoDePrueba   el punto interior con el que se puede comprobar a mano
     * @param valorPrimera    lo que vale la primera curva en ese punto
     * @param valorSegunda    lo que vale la segunda curva en ese punto
     */
    public record Comparacion(boolean primeraVaArriba, boolean seCruzan, double puntoDePrueba,
                              double valorPrimera, double valorSegunda) {

        /** Escribe la comprobacion tal como se hace en el cuaderno. */
        public String comprobacion(String nombrePrimera, String nombreSegunda, String variable) {
            String arriba = primeraVaArriba ? nombrePrimera : nombreSegunda;
            String abajo = primeraVaArriba ? nombreSegunda : nombrePrimera;

            double mayor = primeraVaArriba ? valorPrimera : valorSegunda;
            double menor = primeraVaArriba ? valorSegunda : valorPrimera;

            return "Se toma un punto dentro del intervalo, " + variable + " = "
                    + Redondeo.texto(puntoDePrueba) + ":\n"
                    + "   " + nombrePrimera + " = " + Redondeo.texto(valorPrimera) + "\n"
                    + "   " + nombreSegunda + " = " + Redondeo.texto(valorSegunda) + "\n"
                    + "Como " + Redondeo.texto(mayor) + " > " + Redondeo.texto(menor)
                    + ", en este intervalo " + arriba + " va por encima de " + abajo + ".";
        }
    }

    /**
     * Compara la altura de las dos curvas.
     *
     * @param primera primera curva
     * @param segunda segunda curva
     * @param a       limite inferior
     * @param b       limite superior
     * @return quien va arriba, y si se cruzan
     */
    public static Comparacion comparar(FuncionMatematica primera, FuncionMatematica segunda,
                                       double a, double b) {
        return medir(primera, segunda, a, b, null);
    }

    /**
     * Compara la distancia de las dos curvas al eje de giro.
     *
     * <p>No es la misma pregunta que cual va arriba. Girando alrededor de {@code y = 5},
     * la curva mas alta es la que queda <b>mas cerca</b> del eje, y por tanto la que
     * define el agujero y no el borde exterior.</p>
     *
     * @param primera primera curva
     * @param segunda segunda curva
     * @param eje     recta alrededor de la cual gira la region
     * @param a       limite inferior
     * @param b       limite superior
     * @return si la primera curva es la que queda mas lejos del eje
     */
    public static Comparacion compararDistanciaAlEje(FuncionMatematica primera,
                                                     FuncionMatematica segunda, EjeRotacion eje,
                                                     double a, double b) {
        return medir(primera, segunda, a, b, eje);
    }

    /**
     * Recorre el intervalo contando en cuantos puntos gana cada curva.
     *
     * <p>Se cuenta en vez de mirar un punto solo porque asi se detecta el cruce. Los
     * extremos se dejan fuera del recuento: es justo donde las curvas suelen tocarse, y
     * un empate en el borde no dice nada sobre quien va arriba dentro.</p>
     *
     * @param eje si no es null, se comparan distancias al eje en lugar de alturas
     */
    private static Comparacion medir(FuncionMatematica primera, FuncionMatematica segunda,
                                     double a, double b, EjeRotacion eje) {
        double inicio = Math.min(a, b);
        double fin = Math.max(a, b);
        double paso = (fin - inicio) / (MUESTRAS + 1);

        int ganaPrimera = 0;
        int ganaSegunda = 0;

        double puntoDePrueba = (inicio + fin) / 2.0;
        double valorPrimeraEnPrueba = Double.NaN;
        double valorSegundaEnPrueba = Double.NaN;
        double mayorSeparacion = -1;

        for (int i = 1; i <= MUESTRAS; i++) {
            double punto = inicio + i * paso;

            double valorPrimera = ValidadorFuncion.evaluarSeguro(primera, punto);
            double valorSegunda = ValidadorFuncion.evaluarSeguro(segunda, punto);

            if (!ValidadorFuncion.esUtilizable(valorPrimera)
                    || !ValidadorFuncion.esUtilizable(valorSegunda)) {
                continue;
            }

            double medidaPrimera = eje == null ? valorPrimera : eje.radioDesde(valorPrimera);
            double medidaSegunda = eje == null ? valorSegunda : eje.radioDesde(valorSegunda);

            double separacion = Math.abs(medidaPrimera - medidaSegunda);
            if (separacion < TOLERANCIA) {
                continue;
            }

            if (medidaPrimera > medidaSegunda) {
                ganaPrimera++;
            } else {
                ganaSegunda++;
            }

            // El punto de prueba que se le muestra al estudiante es donde mas se separan:
            // ahi la diferencia se ve sin tener que fiarse de los decimales.
            if (separacion > mayorSeparacion) {
                mayorSeparacion = separacion;
                puntoDePrueba = punto;
                valorPrimeraEnPrueba = valorPrimera;
                valorSegundaEnPrueba = valorSegunda;
            }
        }

        boolean primeraVaArriba = ganaPrimera >= ganaSegunda;
        boolean seCruzan = ganaPrimera > 0 && ganaSegunda > 0;

        if (Double.isNaN(valorPrimeraEnPrueba)) {
            valorPrimeraEnPrueba = ValidadorFuncion.evaluarSeguro(primera, puntoDePrueba);
            valorSegundaEnPrueba = ValidadorFuncion.evaluarSeguro(segunda, puntoDePrueba);
        }

        return new Comparacion(primeraVaArriba, seCruzan, puntoDePrueba,
                valorPrimeraEnPrueba, valorSegundaEnPrueba);
    }
}
