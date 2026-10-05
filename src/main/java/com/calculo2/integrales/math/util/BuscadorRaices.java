package com.calculo2.integrales.math.util;

import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.util.Constantes;

import java.util.ArrayList;
import java.util.List;

/**
 * Encuentra los puntos donde una funcion vale cero dentro de un intervalo.
 *
 * <p>Es la pieza que resuelve las intersecciones del tema de area entre curvas: los
 * puntos donde {@code f(x) = g(x)} son las raices de {@code f(x) - g(x)}.</p>
 *
 * <p>El procedimiento tiene dos etapas. Primero se recorre el intervalo en pasos
 * pequenos buscando cambios de signo, porque un cambio de signo garantiza que hay una
 * raiz en medio. Despues, cada tramo sospechoso se afina por biseccion hasta la
 * tolerancia del proyecto.</p>
 */
public final class BuscadorRaices {

    private BuscadorRaices() {
    }

    /** En cuantos tramos se divide el intervalo durante el barrido inicial. */
    private static final int TRAMOS_DE_BARRIDO = 2000;

    /** Tope de iteraciones de la biseccion; con 200 se alcanza la precision de un double. */
    private static final int ITERACIONES_BISECCION = 200;

    /**
     * Busca todas las raices de la funcion en el intervalo dado.
     *
     * @param funcion funcion cuyas raices se buscan
     * @param a       extremo izquierdo del intervalo
     * @param b       extremo derecho del intervalo
     * @return las raices encontradas, ordenadas de menor a mayor y sin repeticiones
     */
    public static List<Double> buscarEnIntervalo(FuncionMatematica funcion, double a, double b) {
        double inicio = Math.min(a, b);
        double fin = Math.max(a, b);
        List<Double> raices = new ArrayList<>();

        double paso = (fin - inicio) / TRAMOS_DE_BARRIDO;
        double xAnterior = inicio;
        double valorAnterior = ValidadorFuncion.evaluarSeguro(funcion, xAnterior);

        agregarSiEsRaiz(raices, xAnterior, valorAnterior, paso);

        for (int i = 1; i <= TRAMOS_DE_BARRIDO; i++) {
            double x = (i == TRAMOS_DE_BARRIDO) ? fin : inicio + i * paso;
            double valor = ValidadorFuncion.evaluarSeguro(funcion, x);

            if (esRaizExacta(valor)) {
                agregarSiEsNueva(raices, x, paso);
            } else if (hayCambioDeSigno(valorAnterior, valor)) {
                double raiz = bisecar(funcion, xAnterior, x);
                if (!Double.isNaN(raiz)) {
                    agregarSiEsNueva(raices, raiz, paso);
                }
            }

            xAnterior = x;
            valorAnterior = valor;
        }

        raices.sort(Double::compare);
        return raices;
    }

    /**
     * Afina una raiz que se sabe encerrada entre dos puntos con signos opuestos.
     *
     * <p>En cada vuelta se parte el intervalo por la mitad y se conserva la mitad donde
     * el signo sigue cambiando. El intervalo se reduce a la mitad cada vez, asi que la
     * convergencia esta garantizada.</p>
     *
     * @param funcion funcion cuya raiz se busca
     * @param a       extremo donde la funcion tiene un signo
     * @param b       extremo donde tiene el signo contrario
     * @return la raiz, o NaN si el metodo no puede aplicarse
     */
    public static double bisecar(FuncionMatematica funcion, double a, double b) {
        double izquierda = a;
        double derecha = b;
        double valorIzquierda = ValidadorFuncion.evaluarSeguro(funcion, izquierda);
        double valorDerecha = ValidadorFuncion.evaluarSeguro(funcion, derecha);

        if (!hayCambioDeSigno(valorIzquierda, valorDerecha)) {
            return Double.NaN;
        }

        for (int i = 0; i < ITERACIONES_BISECCION; i++) {
            double medio = (izquierda + derecha) / 2.0;
            double valorMedio = ValidadorFuncion.evaluarSeguro(funcion, medio);

            if (esRaizExacta(valorMedio) || (derecha - izquierda) / 2.0 < Constantes.TOLERANCIA) {
                return medio;
            }
            if (hayCambioDeSigno(valorIzquierda, valorMedio)) {
                derecha = medio;
            } else {
                izquierda = medio;
                valorIzquierda = valorMedio;
            }
        }
        return (izquierda + derecha) / 2.0;
    }

    /**
     * Devuelve los puntos que delimitan los tramos de integracion: los dos extremos mas
     * las raices interiores, en orden.
     *
     * <p>El area entre dos curvas necesita este corte porque en cada tramo cambia cual
     * de las dos funciones va arriba, y el signo de la diferencia se invierte.</p>
     *
     * @param funcion diferencia de las dos curvas
     * @param a       limite inferior
     * @param b       limite superior
     * @return lista ordenada que empieza en {@code a} y termina en {@code b}
     */
    public static List<Double> cortesDeIntegracion(FuncionMatematica funcion, double a, double b) {
        double inicio = Math.min(a, b);
        double fin = Math.max(a, b);

        List<Double> cortes = new ArrayList<>();
        cortes.add(inicio);

        for (double raiz : buscarEnIntervalo(funcion, inicio, fin)) {
            boolean esInterior = raiz > inicio + Constantes.EPSILON_INTERSECCION
                    && raiz < fin - Constantes.EPSILON_INTERSECCION;
            if (esInterior) {
                cortes.add(raiz);
            }
        }

        cortes.add(fin);
        return cortes;
    }

    // ------------------------------------------------------------------
    // APOYO
    // ------------------------------------------------------------------

    private static boolean hayCambioDeSigno(double valorA, double valorB) {
        if (Double.isNaN(valorA) || Double.isNaN(valorB)) {
            return false;
        }
        return (valorA < 0 && valorB > 0) || (valorA > 0 && valorB < 0);
    }

    private static boolean esRaizExacta(double valor) {
        return !Double.isNaN(valor) && Math.abs(valor) < Constantes.EPSILON_INTERSECCION;
    }

    private static void agregarSiEsRaiz(List<Double> raices, double x, double valor, double paso) {
        if (esRaizExacta(valor)) {
            agregarSiEsNueva(raices, x, paso);
        }
    }

    /**
     * Evita registrar dos veces la misma raiz. Una funcion que solo roza el eje, como
     * {@code x^2}, puede dar varios puntos casi iguales durante el barrido.
     */
    private static void agregarSiEsNueva(List<Double> raices, double raiz, double paso) {
        double separacionMinima = Math.max(Math.abs(paso), Constantes.EPSILON_INTERSECCION);
        for (double existente : raices) {
            if (Math.abs(existente - raiz) < separacionMinima) {
                return;
            }
        }
        raices.add(raiz);
    }
}
