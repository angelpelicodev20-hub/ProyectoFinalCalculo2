package com.calculo2.integrales.math.parser;

/**
 * Una funcion matematica de una sola variable, ya lista para evaluarse.
 *
 * <p>Es la forma compilada de lo que el usuario escribio. La variable puede ser
 * {@code x} o {@code y} segun el eje de integracion; a esta interfaz solo le
 * interesa recibir un numero y devolver otro.</p>
 */
@FunctionalInterface
public interface FuncionMatematica {

    /**
     * Evalua la funcion en un punto.
     *
     * @param variable valor de la variable independiente
     * @return el resultado; puede ser NaN o infinito si el punto no pertenece al dominio
     */
    double evaluar(double variable);

    /** Devuelve la funcion constante cero, util como valor por defecto. */
    static FuncionMatematica cero() {
        return variable -> 0.0;
    }

    /** Devuelve una funcion constante. */
    static FuncionMatematica constante(double valor) {
        return variable -> valor;
    }

    /** Devuelve la diferencia {@code this - otra}, usada en area entre curvas. */
    default FuncionMatematica menos(FuncionMatematica otra) {
        return variable -> this.evaluar(variable) - otra.evaluar(variable);
    }

    /** Devuelve el valor absoluto de esta funcion. */
    default FuncionMatematica valorAbsoluto() {
        return variable -> Math.abs(this.evaluar(variable));
    }

    /** Devuelve el cuadrado de esta funcion, usado en discos y arandelas. */
    default FuncionMatematica alCuadrado() {
        return variable -> {
            double valor = this.evaluar(variable);
            return valor * valor;
        };
    }
}
