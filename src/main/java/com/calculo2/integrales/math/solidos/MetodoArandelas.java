package com.calculo2.integrales.math.solidos;

import com.calculo2.integrales.math.integracion.MetodoIntegracion;
import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.model.EjeRotacion;

/**
 * Metodo de arandelas, tambien llamado de anillos.
 *
 * <p>Es el metodo de discos extendido a solidos huecos. Cuando la region que gira no toca
 * el eje, cada rebanada no es un disco lleno sino un anillo: hay un radio exterior
 * {@code R} y un radio interior {@code r} que delimita el agujero.</p>
 *
 * <pre>
 *   V = pi * integral de a a b de ( [R(x)]^2 - [r(x)]^2 ) dx
 * </pre>
 *
 * <p>Conviene subrayar un error frecuente: la formula resta los <em>cuadrados</em> de los
 * radios, no los radios. Restar primero y elevar despues, es decir {@code (R - r)^2}, da
 * un resultado distinto y equivocado.</p>
 *
 * <p>Otro detalle: cual de las dos curvas da el radio exterior puede cambiar a lo largo
 * del intervalo. Por eso el integrando compara las dos distancias en cada punto y toma la
 * mayor como {@code R}, en lugar de confiar en el orden en que el usuario escribio las
 * funciones.</p>
 */
public final class MetodoArandelas {

    private final MetodoIntegracion integrador;

    /**
     * @param integrador metodo numerico que resolvera la integral
     */
    public MetodoArandelas(MetodoIntegracion integrador) {
        this.integrador = integrador;
    }

    /**
     * Construye la funcion que hay que integrar: la diferencia de los radios al cuadrado.
     *
     * @param primeraCurva  una de las dos curvas que limitan la region
     * @param segundaCurva  la otra curva
     * @param eje           recta alrededor de la cual gira
     * @return la funcion {@code [R(v)]^2 - [r(v)]^2}
     */
    public static FuncionMatematica integrando(FuncionMatematica primeraCurva,
                                               FuncionMatematica segundaCurva,
                                               EjeRotacion eje) {
        return variable -> {
            double distanciaPrimera = eje.radioDesde(primeraCurva.evaluar(variable));
            double distanciaSegunda = eje.radioDesde(segundaCurva.evaluar(variable));

            double radioExterior = Math.max(distanciaPrimera, distanciaSegunda);
            double radioInterior = Math.min(distanciaPrimera, distanciaSegunda);

            return radioExterior * radioExterior - radioInterior * radioInterior;
        };
    }

    /**
     * Calcula el volumen del solido hueco.
     *
     * @param primeraCurva una de las dos curvas que limitan la region
     * @param segundaCurva la otra curva
     * @param eje          recta alrededor de la cual gira
     * @param a            limite inferior
     * @param b            limite superior
     * @param particiones  particiones para el metodo numerico
     * @return el volumen
     */
    public double calcularVolumen(FuncionMatematica primeraCurva, FuncionMatematica segundaCurva,
                                  EjeRotacion eje, double a, double b, int particiones) {
        double integral = integrador.integrar(
                integrando(primeraCurva, segundaCurva, eje), a, b, particiones);
        return Math.PI * Math.abs(integral);
    }

    /**
     * Devuelve el radio exterior en un punto, que es la mayor de las dos distancias al eje.
     *
     * @param primeraCurva una de las curvas
     * @param segundaCurva la otra curva
     * @param eje          recta alrededor de la cual gira
     * @param variable     punto donde se evalua
     * @return el radio exterior
     */
    public static double radioExteriorEn(FuncionMatematica primeraCurva,
                                         FuncionMatematica segundaCurva,
                                         EjeRotacion eje, double variable) {
        return Math.max(eje.radioDesde(primeraCurva.evaluar(variable)),
                        eje.radioDesde(segundaCurva.evaluar(variable)));
    }

    /**
     * Devuelve el radio del agujero en un punto, que es la menor de las dos distancias.
     *
     * @param primeraCurva una de las curvas
     * @param segundaCurva la otra curva
     * @param eje          recta alrededor de la cual gira
     * @param variable     punto donde se evalua
     * @return el radio interior
     */
    public static double radioInteriorEn(FuncionMatematica primeraCurva,
                                         FuncionMatematica segundaCurva,
                                         EjeRotacion eje, double variable) {
        return Math.min(eje.radioDesde(primeraCurva.evaluar(variable)),
                        eje.radioDesde(segundaCurva.evaluar(variable)));
    }
}
