package com.calculo2.integrales.math.simbolico;

import java.util.Optional;

/**
 * Reconoce fracciones sencillas escondidas dentro de un numero decimal.
 *
 * <p>Integrar produce fracciones todo el tiempo: la primitiva de {@code x^2} lleva un
 * tercio delante. Mostrar eso como {@code 0.333333x^3} obliga al alumno a adivinar de
 * donde salio ese numero, mientras que {@code x^3/3} se reconoce de inmediato como la
 * regla de la potencia.</p>
 *
 * <p>La busqueda es deliberadamente modesta: se prueban denominadores pequenos y, si
 * ninguno encaja, se devuelve vacio y el numero se escribe en decimal. Forzar una
 * fraccion para cualquier decimal daria cosas como {@code 7071/10000} que no ayudan a
 * nadie.</p>
 */
public final class Fraccion {

    private Fraccion() {
    }

    /** Denominador mas grande que se considera "sencillo". */
    private static final int DENOMINADOR_MAXIMO = 1000;

    /** Margen para aceptar que el numerador es entero. */
    private static final double EPSILON = 1e-9;

    /**
     * Una fraccion con su numerador y su denominador.
     *
     * @param numerador   parte de arriba, lleva el signo
     * @param denominador parte de abajo, siempre positiva y mayor que cero
     */
    public record Valor(long numerador, long denominador) {

        /** Indica si la fraccion es en realidad un entero. */
        public boolean esEntera() {
            return denominador == 1;
        }

        /** La fraccion escrita, por ejemplo {@code 2/3} o {@code 5}. */
        public String aTexto() {
            return esEntera() ? String.valueOf(numerador) : numerador + "/" + denominador;
        }
    }

    /**
     * Intenta escribir el numero como una fraccion de denominador pequeno.
     *
     * @param valor numero a examinar
     * @return la fraccion equivalente, o vacio si no hay ninguna sencilla
     */
    public static Optional<Valor> aproximar(double valor) {
        if (!Double.isFinite(valor)) {
            return Optional.empty();
        }

        for (int denominador = 1; denominador <= DENOMINADOR_MAXIMO; denominador++) {
            double numerador = valor * denominador;
            double redondeado = Math.rint(numerador);

            if (Math.abs(numerador - redondeado) < EPSILON) {
                return Optional.of(simplificar((long) redondeado, denominador));
            }
        }
        return Optional.empty();
    }

    /**
     * Escribe el numero como fraccion si se puede, y en decimal si no.
     *
     * @param valor numero a escribir
     * @return el numero como texto
     */
    public static String texto(double valor) {
        return aproximar(valor)
                .map(Valor::aTexto)
                .orElseGet(() -> com.calculo2.integrales.math.util.Redondeo.texto(valor));
    }

    /**
     * Reduce la fraccion dividiendo entre el maximo comun divisor.
     *
     * <p>Sin esto, un valor como 0.5 encontrado con denominador 2 estaria bien, pero uno
     * como 0.25 hallado con denominador 8 daria 2/8 en vez de 1/4.</p>
     */
    private static Valor simplificar(long numerador, long denominador) {
        long divisor = maximoComunDivisor(Math.abs(numerador), denominador);
        if (divisor == 0) {
            return new Valor(0, 1);
        }
        return new Valor(numerador / divisor, denominador / divisor);
    }

    private static long maximoComunDivisor(long a, long b) {
        while (b != 0) {
            long resto = a % b;
            a = b;
            b = resto;
        }
        return a;
    }
}
