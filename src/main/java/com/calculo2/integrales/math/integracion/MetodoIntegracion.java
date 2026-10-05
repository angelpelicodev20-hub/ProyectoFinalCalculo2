package com.calculo2.integrales.math.integracion;

import com.calculo2.integrales.math.parser.FuncionMatematica;

/**
 * Contrato comun de los metodos que aproximan una integral definida.
 *
 * <p>Todos resuelven el mismo problema — el area bajo {@code f} entre {@code a} y
 * {@code b} — pero con distinta precision y distinto costo. Tenerlos detras de una
 * interfaz permite que el usuario cambie de metodo en el formulario sin que el resto
 * del proyecto se entere.</p>
 */
public interface MetodoIntegracion {

    /**
     * Aproxima la integral definida.
     *
     * @param funcion     funcion a integrar
     * @param a           limite inferior
     * @param b           limite superior
     * @param particiones en cuantos subintervalos se divide {@code [a, b]}
     * @return el valor aproximado de la integral
     */
    double integrar(FuncionMatematica funcion, double a, double b, int particiones);

    /** Nombre del metodo tal como se muestra en pantalla. */
    String nombre();

    /** La formula del metodo, escrita para que el alumno la reconozca del cuaderno. */
    String formula();

    /** Explicacion breve de en que consiste el metodo. */
    String descripcion();

    /**
     * Indica si el metodo exige un numero par de particiones.
     * Simpson es el unico que lo necesita, porque agrupa los subintervalos de dos en dos.
     */
    default boolean exigeParticionesPares() {
        return false;
    }

    /**
     * Orden del error del metodo, usado en la explicacion paso a paso.
     * Por ejemplo, el trapecio devuelve 2 porque su error decrece como {@code h^2}.
     */
    default int ordenDelError() {
        return 1;
    }
}
