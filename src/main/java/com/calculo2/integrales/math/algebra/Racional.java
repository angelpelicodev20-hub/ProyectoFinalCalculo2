package com.calculo2.integrales.math.algebra;

import java.math.BigInteger;
import java.util.Optional;

/**
 * Un numero racional exacto, p/q, sin errores de redondeo.
 *
 * <p>Factorizar exige aritmetica exacta. Con {@code double}, comprobar si {@code x = 1/3}
 * anula un polinomio da un residuo de {@code 1e-17} en vez de cero, y decidir si eso es
 * una raiz o no seria adivinar. Con fracciones la respuesta es un si o un no.</p>
 *
 * <p>Se apoya en {@link BigInteger} para no desbordarse al multiplicar coeficientes: un
 * polinomio de grado cuatro con coeficientes de dos cifras ya produce productos que no
 * caben en un {@code long} al evaluarlo en una fraccion.</p>
 *
 * @param numerador   el numerador, con el signo del numero
 * @param denominador el denominador, siempre positivo
 */
public record Racional(BigInteger numerador, BigInteger denominador) implements Comparable<Racional> {

    /** El cero. */
    public static final Racional CERO = new Racional(BigInteger.ZERO, BigInteger.ONE);

    /** El uno. */
    public static final Racional UNO = new Racional(BigInteger.ONE, BigInteger.ONE);

    /** Denominador maximo al reconocer una fraccion escrita como decimal. */
    private static final long DENOMINADOR_MAXIMO = 100_000L;

    /**
     * Construye la fraccion ya simplificada y con el signo en el numerador.
     */
    public Racional {
        if (denominador.signum() == 0) {
            throw new ArithmeticException("Division entre cero.");
        }
        if (denominador.signum() < 0) {
            numerador = numerador.negate();
            denominador = denominador.negate();
        }
        BigInteger divisor = numerador.gcd(denominador);
        if (!divisor.equals(BigInteger.ONE) && divisor.signum() != 0) {
            numerador = numerador.divide(divisor);
            denominador = denominador.divide(divisor);
        }
        if (numerador.signum() == 0) {
            denominador = BigInteger.ONE;
        }
    }

    /** La fraccion n/d. */
    public static Racional de(long numerador, long denominador) {
        return new Racional(BigInteger.valueOf(numerador), BigInteger.valueOf(denominador));
    }

    /** El entero n. */
    public static Racional entero(long valor) {
        return de(valor, 1);
    }

    /**
     * Reconoce la fraccion que hay detras de un decimal.
     *
     * <p>Los coeficientes llegan del parser como {@code double}: {@code 1/3} llega como
     * {@code 0.333...}. Las fracciones continuas encuentran la fraccion de denominador
     * mas pequeno que reproduce el numero, que es la que escribio el estudiante.</p>
     *
     * @param valor el decimal
     * @return la fraccion, o vacio si el numero no es racional con un denominador razonable
     */
    public static Optional<Racional> desdeDouble(double valor) {
        if (!Double.isFinite(valor)) {
            return Optional.empty();
        }
        if (valor == Math.rint(valor) && Math.abs(valor) < 1e15) {
            return Optional.of(entero((long) valor));
        }

        double tolerancia = 1e-11 * Math.max(1.0, Math.abs(valor));
        long h0 = 0, h1 = 1;
        long k0 = 1, k1 = 0;
        double resto = valor;

        for (int i = 0; i < 40; i++) {
            long a = (long) Math.floor(resto);
            long h2 = a * h1 + h0;
            long k2 = a * k1 + k0;
            if (k2 > DENOMINADOR_MAXIMO || k2 <= 0) {
                break;
            }
            if (Math.abs((double) h2 / k2 - valor) < tolerancia) {
                return Optional.of(de(h2, k2));
            }
            h0 = h1;
            h1 = h2;
            k0 = k1;
            k1 = k2;
            double fraccion = resto - a;
            if (Math.abs(fraccion) < 1e-15) {
                break;
            }
            resto = 1.0 / fraccion;
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // ARITMETICA
    // ------------------------------------------------------------------

    public Racional sumar(Racional otro) {
        return new Racional(
                numerador.multiply(otro.denominador).add(otro.numerador.multiply(denominador)),
                denominador.multiply(otro.denominador));
    }

    public Racional restar(Racional otro) {
        return sumar(otro.negar());
    }

    public Racional multiplicar(Racional otro) {
        return new Racional(numerador.multiply(otro.numerador),
                denominador.multiply(otro.denominador));
    }

    public Racional dividir(Racional otro) {
        return new Racional(numerador.multiply(otro.denominador),
                denominador.multiply(otro.numerador));
    }

    public Racional negar() {
        return new Racional(numerador.negate(), denominador);
    }

    public Racional inverso() {
        return new Racional(denominador, numerador);
    }

    /** Eleva a un exponente entero no negativo. */
    public Racional elevar(int exponente) {
        return new Racional(numerador.pow(exponente), denominador.pow(exponente));
    }

    // ------------------------------------------------------------------
    // CONSULTA
    // ------------------------------------------------------------------

    public boolean esCero() {
        return numerador.signum() == 0;
    }

    public boolean esEntero() {
        return denominador.equals(BigInteger.ONE);
    }

    public int signo() {
        return numerador.signum();
    }

    /** El valor como decimal. */
    public double valor() {
        return numerador.doubleValue() / denominador.doubleValue();
    }

    /**
     * La raiz cuadrada exacta, si la tiene.
     *
     * @return la raiz, o vacio si el numero es negativo o no es un cuadrado perfecto
     */
    public Optional<Racional> raizCuadrada() {
        if (signo() < 0) {
            return Optional.empty();
        }
        BigInteger arriba = numerador.sqrt();
        BigInteger abajo = denominador.sqrt();
        if (arriba.multiply(arriba).equals(numerador) && abajo.multiply(abajo).equals(denominador)) {
            return Optional.of(new Racional(arriba, abajo));
        }
        return Optional.empty();
    }

    @Override
    public int compareTo(Racional otro) {
        return numerador.multiply(otro.denominador)
                .compareTo(otro.numerador.multiply(denominador));
    }

    /** Escrito como en el cuaderno: {@code 3}, {@code -1/2}. */
    public String texto() {
        return esEntero() ? numerador.toString() : numerador + "/" + denominador;
    }

    @Override
    public String toString() {
        return texto();
    }
}
