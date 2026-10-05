package com.calculo2.integrales.math.simbolico;

import com.calculo2.integrales.math.parser.NodoExpresion;

import java.util.Optional;

/**
 * Intenta reconocer un polinomio dentro del arbol que produjo el parser.
 *
 * <p>El parser guarda {@code x^2 + 3x} como un arbol de operaciones, que sirve para
 * evaluar pero no para hacer algebra. Esta clase recorre ese arbol y, cuando todo lo que
 * encuentra son sumas, restas, productos y potencias enteras, arma el {@link Polinomio}
 * equivalente.</p>
 *
 * <p>Devuelve un {@link Optional} vacio cuando la expresion no es polinomica — por
 * ejemplo si aparece {@code sin(x)} o {@code 1/x} —, y esa respuesta es informacion util:
 * le dice al resto del programa que ese camino de solucion no aplica, en lugar de
 * entregar un resultado inventado.</p>
 */
public final class ConversorPolinomio {

    private ConversorPolinomio() {
    }

    /**
     * Convierte la expresion a polinomio, si es posible.
     *
     * @param expresion arbol producido por el parser
     * @return el polinomio equivalente, o vacio si la expresion no es polinomica
     */
    public static Optional<Polinomio> convertir(NodoExpresion expresion) {
        if (expresion == null) {
            return Optional.empty();
        }

        return switch (expresion) {
            case NodoExpresion.Numero numero ->
                    Optional.of(Polinomio.constante(numero.valor()));

            case NodoExpresion.Constante constante ->
                    Optional.of(Polinomio.constante(constante.valor()));

            case NodoExpresion.Variable ignorada ->
                    Optional.of(Polinomio.variable());

            case NodoExpresion.Negacion negacion ->
                    convertir(negacion.operando()).map(polinomio -> polinomio.por(-1.0));

            case NodoExpresion.OperacionBinaria operacion ->
                    convertirOperacion(operacion);

            // Ninguna funcion del catalogo devuelve un polinomio, ni siquiera cuando su
            // argumento lo sea: sin(x) no es polinomica por mucho que x lo sea.
            case NodoExpresion.LlamadaFuncion ignorada ->
                    Optional.empty();
        };
    }

    /**
     * Convierte una operacion de dos operandos.
     *
     * @param operacion nodo de la operacion
     * @return el polinomio resultante, o vacio si la operacion saca del mundo polinomico
     */
    private static Optional<Polinomio> convertirOperacion(NodoExpresion.OperacionBinaria operacion) {
        Optional<Polinomio> izquierda = convertir(operacion.izquierda());
        Optional<Polinomio> derecha = convertir(operacion.derecha());

        // El cuadrado de una raiz si es un polinomio: (sqrt(8x))^2 = 8x. Es el caso del
        // metodo de discos con un radio que es una raiz, y reconocerlo deja la integral
        // en la regla de la potencia en lugar de mandarla a la aproximacion numerica.
        if (izquierda.isEmpty() && derecha.isPresent() && derecha.get().esConstante()
                && operacion.simbolo().equals("^")) {
            return potenciaDeUnaRaiz(operacion.izquierda(), derecha.get().coeficiente(0));
        }

        if (izquierda.isEmpty() || derecha.isEmpty()) {
            return Optional.empty();
        }

        Polinomio a = izquierda.get();
        Polinomio b = derecha.get();

        return switch (operacion.simbolo()) {
            case "+" -> Optional.of(a.mas(b));
            case "-" -> Optional.of(a.menos(b));
            case "*" -> Optional.of(a.por(b));
            case "/" -> dividir(a, b);
            case "^" -> elevar(a, b);
            default -> Optional.empty();
        };
    }

    /**
     * Divide, pero solo entre una constante.
     *
     * <p>Dividir entre algo que contiene la variable saca del mundo de los polinomios:
     * {@code 1/x} no lo es. En cambio {@code (x^2+1)/3} sigue siendolo.</p>
     */
    private static Optional<Polinomio> dividir(Polinomio numerador, Polinomio divisor) {
        if (!divisor.esConstante() || divisor.esCero()) {
            return Optional.empty();
        }
        return Optional.of(numerador.por(1.0 / divisor.coeficiente(0)));
    }

    /**
     * Eleva a una potencia, pero solo si el exponente es un entero no negativo.
     *
     * <p>Un exponente fraccionario da una raiz y uno negativo da un cociente; ninguno de
     * los dos es un polinomio.</p>
     */
    private static Optional<Polinomio> elevar(Polinomio base, Polinomio exponente) {
        if (!exponente.esConstante()) {
            return Optional.empty();
        }

        double valor = exponente.coeficiente(0);
        boolean esEnteroNoNegativo = valor >= 0
                && Math.abs(valor - Math.rint(valor)) < 1e-9;

        if (!esEnteroNoNegativo) {
            return Optional.empty();
        }

        // Un exponente enorme haria crecer el arreglo de coeficientes sin control.
        int entero = (int) Math.rint(valor);
        if (entero > 40) {
            return Optional.empty();
        }

        return Optional.of(base.elevado(entero));
    }

    /**
     * Eleva una raiz a una potencia que la elimina.
     *
     * <p>{@code (c*sqrt(A))^2 = c^2 A} y {@code (c*cbrt(A))^3 = c^3 A}, y lo mismo con
     * cualquier multiplo del indice. Solo vale cuando la base es la raiz sola por un
     * numero: {@code (1 + sqrt(x))^2} deja un termino con raiz y no es un polinomio.</p>
     */
    private static Optional<Polinomio> potenciaDeUnaRaiz(NodoExpresion base, double exponente) {
        Optional<FormaRadical> forma = FormaRadical.de(base);
        if (forma.isEmpty() || !forma.get().esRaizPura()) {
            return Optional.empty();
        }
        int indice = forma.get().indice();
        double veces = exponente / indice;
        if (veces < 1 || Math.abs(veces - Math.rint(veces)) > 1e-9 || veces > 20) {
            return Optional.empty();
        }
        Polinomio sinRaiz = forma.get().radicando()
                .por(Math.pow(forma.get().coeficiente(), indice));
        return Optional.of(sinRaiz.elevado((int) Math.rint(veces)));
    }

    /**
     * Indica si la expresion es polinomica.
     *
     * @param expresion arbol a revisar
     * @return true si se puede convertir a polinomio
     */
    public static boolean esPolinomio(NodoExpresion expresion) {
        return convertir(expresion).isPresent();
    }
}
