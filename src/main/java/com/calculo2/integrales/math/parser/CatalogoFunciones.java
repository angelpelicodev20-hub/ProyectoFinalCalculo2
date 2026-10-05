package com.calculo2.integrales.math.parser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

/**
 * Lista de nombres que el usuario puede escribir dentro de una funcion.
 *
 * <p>Guarda dos cosas: las funciones de un argumento ({@code sin}, {@code ln}, ...) y
 * las constantes con nombre ({@code pi}, {@code e}). El analizador consulta este
 * catalogo para decidir si un nombre es valido, y la pagina de ayuda lo usa para
 * mostrarle al usuario que puede escribir.</p>
 */
public final class CatalogoFunciones {

    private CatalogoFunciones() {
    }

    /** Nombre de la funcion asociado a como se calcula. */
    private static final Map<String, DoubleUnaryOperator> FUNCIONES = crearFunciones();

    /** Descripcion en espanol de cada funcion, para la pagina de ayuda. */
    private static final Map<String, String> DESCRIPCIONES = crearDescripciones();

    /** Constantes con nombre. */
    private static final Map<String, Double> CONSTANTES = crearConstantes();

    private static Map<String, DoubleUnaryOperator> crearFunciones() {
        Map<String, DoubleUnaryOperator> mapa = new LinkedHashMap<>();

        // Trigonometricas directas.
        mapa.put("sin", Math::sin);
        mapa.put("sen", Math::sin);
        mapa.put("cos", Math::cos);
        mapa.put("tan", Math::tan);
        mapa.put("cot", valor -> 1.0 / Math.tan(valor));
        mapa.put("sec", valor -> 1.0 / Math.cos(valor));
        mapa.put("csc", valor -> 1.0 / Math.sin(valor));

        // Trigonometricas inversas.
        mapa.put("asin", Math::asin);
        mapa.put("acos", Math::acos);
        mapa.put("atan", Math::atan);

        // Hiperbolicas.
        mapa.put("sinh", Math::sinh);
        mapa.put("cosh", Math::cosh);
        mapa.put("tanh", Math::tanh);

        // Exponenciales y logaritmos.
        mapa.put("exp", Math::exp);
        mapa.put("ln", Math::log);
        mapa.put("log", Math::log10);
        mapa.put("log10", Math::log10);

        // Raices y valor absoluto.
        mapa.put("sqrt", Math::sqrt);
        mapa.put("raiz", Math::sqrt);
        mapa.put("cbrt", Math::cbrt);
        mapa.put("abs", Math::abs);

        // Redondeo.
        mapa.put("floor", Math::floor);
        mapa.put("ceil", Math::ceil);
        mapa.put("round", valor -> (double) Math.round(valor));

        return Collections.unmodifiableMap(mapa);
    }

    private static Map<String, String> crearDescripciones() {
        Map<String, String> mapa = new LinkedHashMap<>();
        mapa.put("sin", "Seno (el angulo va en radianes)");
        mapa.put("sen", "Seno, escrito en espanol");
        mapa.put("cos", "Coseno");
        mapa.put("tan", "Tangente");
        mapa.put("cot", "Cotangente");
        mapa.put("sec", "Secante");
        mapa.put("csc", "Cosecante");
        mapa.put("asin", "Arcoseno");
        mapa.put("acos", "Arcocoseno");
        mapa.put("atan", "Arcotangente");
        mapa.put("sinh", "Seno hiperbolico");
        mapa.put("cosh", "Coseno hiperbolico");
        mapa.put("tanh", "Tangente hiperbolica");
        mapa.put("exp", "Exponencial, equivale a e^x");
        mapa.put("ln", "Logaritmo natural, base e");
        mapa.put("log", "Logaritmo base 10");
        mapa.put("log10", "Logaritmo base 10");
        mapa.put("sqrt", "Raiz cuadrada");
        mapa.put("raiz", "Raiz cuadrada, escrito en espanol");
        mapa.put("cbrt", "Raiz cubica");
        mapa.put("abs", "Valor absoluto");
        mapa.put("floor", "Redondeo hacia abajo");
        mapa.put("ceil", "Redondeo hacia arriba");
        mapa.put("round", "Redondeo al entero mas cercano");
        return Collections.unmodifiableMap(mapa);
    }

    private static Map<String, Double> crearConstantes() {
        Map<String, Double> mapa = new LinkedHashMap<>();
        mapa.put("pi", Math.PI);
        mapa.put("e", Math.E);
        return Collections.unmodifiableMap(mapa);
    }

    /** Indica si el nombre corresponde a una funcion conocida. */
    public static boolean esFuncion(String nombre) {
        return FUNCIONES.containsKey(nombre);
    }

    /** Indica si el nombre corresponde a una constante conocida. */
    public static boolean esConstante(String nombre) {
        return CONSTANTES.containsKey(nombre);
    }

    /**
     * Devuelve como se calcula la funcion indicada.
     *
     * @param nombre nombre en minusculas
     * @return la operacion, o null si el nombre no esta en el catalogo
     */
    public static DoubleUnaryOperator obtenerFuncion(String nombre) {
        return FUNCIONES.get(nombre);
    }

    /**
     * Devuelve el valor de la constante indicada.
     *
     * @param nombre nombre en minusculas
     * @return el valor, o NaN si el nombre no esta en el catalogo
     */
    public static double obtenerConstante(String nombre) {
        return CONSTANTES.getOrDefault(nombre, Double.NaN);
    }

    /** Todas las funciones con su descripcion, para la pagina de ayuda. */
    public static Map<String, String> descripciones() {
        return DESCRIPCIONES;
    }

    /** Todas las constantes con su valor, para la pagina de ayuda. */
    public static Map<String, Double> constantes() {
        return CONSTANTES;
    }
}
