package com.calculo2.integrales.math.simbolico;

import com.calculo2.integrales.math.util.Redondeo;

import java.util.Arrays;

/**
 * Un polinomio en una variable, guardado por sus coeficientes.
 *
 * <p>Existe para poder hacer algebra de verdad y no solo evaluar numeros. El paso de
 * "simplificar" de un procedimiento necesita desarrollar expresiones como
 * {@code (-x^2+4)^2 - (2x+1)^2} y llegar a {@code x^4 - 12x^2 - 4x + 15}; eso no se puede
 * mostrar si la funcion solo se sabe evaluar punto por punto.</p>
 *
 * <p>Los coeficientes se guardan indexados por grado: la posicion 0 es el termino
 * independiente, la 1 acompana a {@code x}, la 2 a {@code x^2}, y asi. Es la
 * representacion que vuelve triviales la suma, el producto y la integral.</p>
 *
 * <p>La clase es inmutable: cada operacion devuelve un polinomio nuevo.</p>
 */
public final class Polinomio {

    /** Por debajo de esto un coeficiente se considera cero. */
    private static final double EPSILON = 1e-10;

    private final double[] coeficientes;

    private Polinomio(double[] coeficientes) {
        this.coeficientes = recortar(coeficientes);
    }

    // ------------------------------------------------------------------
    // CONSTRUCCION
    // ------------------------------------------------------------------

    /**
     * Crea un polinomio a partir de sus coeficientes.
     *
     * @param coeficientesDesdeGradoCero coeficientes, empezando por el termino independiente
     * @return el polinomio
     */
    public static Polinomio de(double... coeficientesDesdeGradoCero) {
        return new Polinomio(coeficientesDesdeGradoCero.clone());
    }

    /** El polinomio constante con el valor indicado. */
    public static Polinomio constante(double valor) {
        return new Polinomio(new double[]{valor});
    }

    /** El polinomio {@code x}. */
    public static Polinomio variable() {
        return new Polinomio(new double[]{0, 1});
    }

    /** El polinomio cero. */
    public static Polinomio cero() {
        return new Polinomio(new double[]{0});
    }

    // ------------------------------------------------------------------
    // CONSULTA
    // ------------------------------------------------------------------

    /** Grado del polinomio; el polinomio cero tiene grado cero. */
    public int grado() {
        return coeficientes.length - 1;
    }

    /**
     * Coeficiente de un grado dado.
     *
     * @param grado grado consultado
     * @return el coeficiente, o cero si el grado supera al del polinomio
     */
    public double coeficiente(int grado) {
        if (grado < 0 || grado >= coeficientes.length) {
            return 0.0;
        }
        return coeficientes[grado];
    }

    /** Indica si el polinomio es identicamente cero. */
    public boolean esCero() {
        for (double coeficiente : coeficientes) {
            if (Math.abs(coeficiente) > EPSILON) {
                return false;
            }
        }
        return true;
    }

    /** Indica si el polinomio es una constante. */
    public boolean esConstante() {
        return grado() == 0;
    }

    // ------------------------------------------------------------------
    // ALGEBRA
    // ------------------------------------------------------------------

    /** Suma de dos polinomios. */
    public Polinomio mas(Polinomio otro) {
        int largo = Math.max(this.coeficientes.length, otro.coeficientes.length);
        double[] resultado = new double[largo];

        for (int i = 0; i < largo; i++) {
            resultado[i] = this.coeficiente(i) + otro.coeficiente(i);
        }
        return new Polinomio(resultado);
    }

    /** Resta de dos polinomios. */
    public Polinomio menos(Polinomio otro) {
        return this.mas(otro.por(-1.0));
    }

    /**
     * Producto de dos polinomios.
     *
     * <p>Cada termino de uno multiplica a cada termino del otro, y los grados se suman:
     * es el desarrollo que se hace a mano, solo que sin equivocarse al agrupar.</p>
     */
    public Polinomio por(Polinomio otro) {
        double[] resultado = new double[this.coeficientes.length + otro.coeficientes.length - 1];

        for (int i = 0; i < this.coeficientes.length; i++) {
            for (int j = 0; j < otro.coeficientes.length; j++) {
                resultado[i + j] += this.coeficientes[i] * otro.coeficientes[j];
            }
        }
        return new Polinomio(resultado);
    }

    /** Producto por un numero. */
    public Polinomio por(double factor) {
        double[] resultado = new double[coeficientes.length];
        for (int i = 0; i < coeficientes.length; i++) {
            resultado[i] = coeficientes[i] * factor;
        }
        return new Polinomio(resultado);
    }

    /**
     * Eleva el polinomio a una potencia entera no negativa.
     *
     * @param exponente exponente, cero o mayor
     * @return el polinomio elevado
     * @throws IllegalArgumentException si el exponente es negativo
     */
    public Polinomio elevado(int exponente) {
        if (exponente < 0) {
            throw new IllegalArgumentException(
                    "Un polinomio elevado a un exponente negativo ya no es un polinomio.");
        }

        Polinomio resultado = constante(1);
        for (int i = 0; i < exponente; i++) {
            resultado = resultado.por(this);
        }
        return resultado;
    }

    /** El cuadrado del polinomio; aparece en discos y arandelas. */
    public Polinomio alCuadrado() {
        return this.por(this);
    }

    // ------------------------------------------------------------------
    // CALCULO
    // ------------------------------------------------------------------

    /**
     * Evalua el polinomio en un punto.
     *
     * <p>Usa la regla de Horner, que reordena el calculo para hacer una multiplicacion
     * por grado en vez de calcular cada potencia por separado.</p>
     *
     * @param valor valor de la variable
     * @return el resultado
     */
    public double evaluar(double valor) {
        double resultado = 0.0;
        for (int i = coeficientes.length - 1; i >= 0; i--) {
            resultado = resultado * valor + coeficientes[i];
        }
        return resultado;
    }

    /**
     * Devuelve la antiderivada, con constante de integracion cero.
     *
     * <p>Es la regla de la potencia aplicada termino a termino: el exponente sube en uno
     * y el coeficiente se divide entre el nuevo exponente.</p>
     *
     * @return la primitiva del polinomio
     */
    public Polinomio integral() {
        double[] resultado = new double[coeficientes.length + 1];

        for (int grado = 0; grado < coeficientes.length; grado++) {
            resultado[grado + 1] = coeficientes[grado] / (grado + 1);
        }
        return new Polinomio(resultado);
    }

    /**
     * Calcula la integral definida por la regla de Barrow.
     *
     * @param a limite inferior
     * @param b limite superior
     * @return el valor de la integral
     */
    public double integralDefinida(double a, double b) {
        Polinomio primitiva = integral();
        return primitiva.evaluar(b) - primitiva.evaluar(a);
    }

    /** Derivada del polinomio. */
    public Polinomio derivada() {
        if (grado() == 0) {
            return cero();
        }

        double[] resultado = new double[coeficientes.length - 1];
        for (int grado = 1; grado < coeficientes.length; grado++) {
            resultado[grado - 1] = coeficientes[grado] * grado;
        }
        return new Polinomio(resultado);
    }

    // ------------------------------------------------------------------
    // ESCRITURA
    // ------------------------------------------------------------------

    /** El polinomio escrito con la variable {@code x}. */
    public String aTexto() {
        return aTexto("x");
    }

    /**
     * Escribe el polinomio como lo escribiria un alumno.
     *
     * <p>Del grado mayor al menor, sin terminos nulos, sin el coeficiente 1 delante de la
     * variable y con los signos integrados en la cadena: {@code x^4 - 12x^2 + 15} en
     * lugar de {@code 1x^4 + -12x^2 + 0x^1 + 15}.</p>
     *
     * @param variable nombre de la variable
     * @return el polinomio como texto
     */
    public String aTexto(String variable) {
        if (esCero()) {
            return "0";
        }

        StringBuilder texto = new StringBuilder();

        for (int grado = coeficientes.length - 1; grado >= 0; grado--) {
            double coeficiente = coeficientes[grado];
            if (Math.abs(coeficiente) <= EPSILON) {
                continue;
            }

            boolean esElPrimero = texto.isEmpty();
            if (esElPrimero) {
                if (coeficiente < 0) {
                    texto.append("-");
                }
            } else {
                texto.append(coeficiente < 0 ? " - " : " + ");
            }

            texto.append(escribirTermino(Math.abs(coeficiente), grado, variable));
        }

        return texto.toString();
    }

    /**
     * Escribe un termino ya sin signo, por ejemplo {@code 12x^2}.
     *
     * <p>Los coeficientes fraccionarios se escriben como fraccion y detras de la
     * variable: la primitiva de {@code x^2} se muestra como {@code x^3/3}, que es como se
     * escribe en clase, y no como {@code 0.333333x^3}, donde el origen del numero se
     * pierde.</p>
     */
    private static String escribirTermino(double magnitud, int grado, String variable) {
        // El termino independiente es solo el numero.
        if (grado == 0) {
            return Fraccion.texto(magnitud);
        }

        String parteVariable = (grado == 1) ? variable : variable + "^" + grado;

        // El coeficiente 1 no se escribe: es x^2, no 1x^2.
        if (Math.abs(magnitud - 1.0) <= EPSILON) {
            return parteVariable;
        }

        var fraccion = Fraccion.aproximar(magnitud);
        if (fraccion.isPresent() && !fraccion.get().esEntera()) {
            Fraccion.Valor valor = fraccion.get();
            String arriba = valor.numerador() == 1
                    ? parteVariable
                    : valor.numerador() + parteVariable;
            return arriba + "/" + valor.denominador();
        }

        return Redondeo.texto(magnitud) + parteVariable;
    }

    /**
     * Escribe el polinomio entre parentesis si tiene mas de un termino.
     * Sirve para insertarlo dentro de una expresion mayor sin cambiar su significado.
     *
     * @param variable nombre de la variable
     * @return el polinomio, con parentesis si hacen falta
     */
    public String aTextoAgrupado(String variable) {
        String texto = aTexto(variable);
        return cuentaTerminos() > 1 ? "(" + texto + ")" : texto;
    }

    /** Cuantos terminos no nulos tiene el polinomio. */
    public int cuentaTerminos() {
        int cuenta = 0;
        for (double coeficiente : coeficientes) {
            if (Math.abs(coeficiente) > EPSILON) {
                cuenta++;
            }
        }
        return cuenta;
    }

    @Override
    public String toString() {
        return aTexto();
    }

    // ------------------------------------------------------------------
    // APOYO
    // ------------------------------------------------------------------

    /**
     * Quita los ceros sobrantes del final.
     *
     * <p>Sin esto, restar {@code x^2 + 1} menos {@code x^2} daria un polinomio que dice
     * ser de grado dos con coeficiente cero, y el grado dejaria de ser fiable.</p>
     */
    private static double[] recortar(double[] valores) {
        int ultimo = valores.length - 1;
        while (ultimo > 0 && Math.abs(valores[ultimo]) <= EPSILON) {
            ultimo--;
        }
        return Arrays.copyOf(valores, ultimo + 1);
    }
}
