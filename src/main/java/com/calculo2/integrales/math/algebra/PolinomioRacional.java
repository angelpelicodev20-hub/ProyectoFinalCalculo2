package com.calculo2.integrales.math.algebra;

import com.calculo2.integrales.math.simbolico.Polinomio;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

/**
 * Un polinomio de una variable con coeficientes racionales exactos.
 *
 * <p>Es la version exacta de {@link Polinomio}. Se usa para factorizar: probar raices,
 * dividir entre {@code (x - r)} y comprobar que el resto es cero, cosas que con decimales
 * solo se podrian hacer "mas o menos".</p>
 */
public final class PolinomioRacional {

    /** Coeficientes desde el grado cero; el ultimo no es nulo salvo en el polinomio cero. */
    private final Racional[] coeficientes;

    private PolinomioRacional(Racional[] coeficientes) {
        this.coeficientes = recortar(coeficientes);
    }

    /** El polinomio con estos coeficientes, del grado cero hacia arriba. */
    public static PolinomioRacional de(Racional... coeficientesDesdeGradoCero) {
        return new PolinomioRacional(coeficientesDesdeGradoCero.clone());
    }

    /**
     * Convierte un polinomio de coeficientes decimales.
     *
     * @return el polinomio exacto, o vacio si algun coeficiente no es una fraccion
     */
    public static Optional<PolinomioRacional> desde(Polinomio polinomio) {
        Racional[] exactos = new Racional[polinomio.grado() + 1];
        for (int i = 0; i <= polinomio.grado(); i++) {
            Optional<Racional> exacto = Racional.desdeDouble(polinomio.coeficiente(i));
            if (exacto.isEmpty()) {
                return Optional.empty();
            }
            exactos[i] = exacto.get();
        }
        return Optional.of(new PolinomioRacional(exactos));
    }

    // ------------------------------------------------------------------
    // CONSULTA
    // ------------------------------------------------------------------

    public int grado() {
        return coeficientes.length - 1;
    }

    public Racional coeficiente(int grado) {
        if (grado < 0 || grado >= coeficientes.length) {
            return Racional.CERO;
        }
        return coeficientes[grado];
    }

    /** El coeficiente del termino de mayor grado. */
    public Racional principal() {
        return coeficientes[coeficientes.length - 1];
    }

    public boolean esCero() {
        return coeficientes.length == 1 && coeficientes[0].esCero();
    }

    /** El menor grado con coeficiente no nulo: el exponente del factor comun x^k. */
    public int menorGrado() {
        for (int i = 0; i < coeficientes.length; i++) {
            if (!coeficientes[i].esCero()) {
                return i;
            }
        }
        return 0;
    }

    /** Evalua el polinomio en un racional, sin redondeo. */
    public Racional evaluar(Racional valor) {
        Racional resultado = Racional.CERO;
        for (int i = coeficientes.length - 1; i >= 0; i--) {
            resultado = resultado.multiplicar(valor).sumar(coeficientes[i]);
        }
        return resultado;
    }

    /** Indica si todos los coeficientes son enteros. */
    public boolean tieneCoeficientesEnteros() {
        for (Racional coeficiente : coeficientes) {
            if (!coeficiente.esEntero()) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // ALGEBRA
    // ------------------------------------------------------------------

    /** Multiplica todos los coeficientes por un numero. */
    public PolinomioRacional escalar(Racional factor) {
        Racional[] resultado = new Racional[coeficientes.length];
        for (int i = 0; i < coeficientes.length; i++) {
            resultado[i] = coeficientes[i].multiplicar(factor);
        }
        return new PolinomioRacional(resultado);
    }

    /** Divide entre x^k, quitando los k primeros coeficientes, que deben ser nulos. */
    public PolinomioRacional dividirEntrePotenciaDeX(int k) {
        if (k <= 0) {
            return this;
        }
        return new PolinomioRacional(Arrays.copyOfRange(coeficientes, k, coeficientes.length));
    }

    /**
     * Divide entre {@code (x - r)} por division sintetica.
     *
     * <p>Se llama solo con raices comprobadas, asi que el resto es cero y se descarta.</p>
     *
     * @param raiz el valor r
     * @return el cociente, de un grado menos
     */
    public PolinomioRacional dividirEntreRaiz(Racional raiz) {
        int n = grado();
        Racional[] cociente = new Racional[Math.max(1, n)];
        Racional arrastre = Racional.CERO;
        for (int i = n; i >= 1; i--) {
            arrastre = arrastre.multiplicar(raiz).sumar(coeficientes[i]);
            cociente[i - 1] = arrastre;
        }
        return new PolinomioRacional(cociente);
    }

    /**
     * Separa el factor numerico para dejar un polinomio de coeficientes enteros primitivo
     * y con el coeficiente principal positivo.
     *
     * <p>Es el primer paso de cualquier factorizacion a mano: {@code x^2/4 - 1 = 0} se
     * multiplica por 4 y {@code -x^2 + x + 2 = 0} por {@code -1}, y la ecuacion que queda
     * tiene las mismas raices pero es mucho mas comoda.</p>
     *
     * @return el factor k tal que este polinomio es k por el polinomio primitivo
     */
    public Racional contenido() {
        BigInteger mcmDenominadores = BigInteger.ONE;
        for (Racional coeficiente : coeficientes) {
            BigInteger d = coeficiente.denominador();
            mcmDenominadores = mcmDenominadores.divide(mcmDenominadores.gcd(d)).multiply(d);
        }
        BigInteger mcdNumeradores = BigInteger.ZERO;
        for (Racional coeficiente : coeficientes) {
            BigInteger entero = coeficiente.numerador()
                    .multiply(mcmDenominadores.divide(coeficiente.denominador()));
            mcdNumeradores = mcdNumeradores.gcd(entero);
        }
        if (mcdNumeradores.signum() == 0) {
            return Racional.UNO;
        }
        Racional contenido = new Racional(mcdNumeradores, mcmDenominadores);
        return principal().signo() < 0 ? contenido.negar() : contenido;
    }

    /** Convierte a la version de coeficientes decimales. */
    public Polinomio aPolinomio() {
        double[] valores = new double[coeficientes.length];
        for (int i = 0; i < coeficientes.length; i++) {
            valores[i] = coeficientes[i].valor();
        }
        return Polinomio.de(valores);
    }

    // ------------------------------------------------------------------
    // ESCRITURA
    // ------------------------------------------------------------------

    /**
     * Escribe el polinomio de mayor a menor grado, como en el cuaderno.
     *
     * @param variable nombre de la variable
     * @return por ejemplo {@code x^3 - 2x + 1/2}
     */
    public String texto(String variable) {
        if (esCero()) {
            return "0";
        }
        StringBuilder texto = new StringBuilder();
        for (int grado = coeficientes.length - 1; grado >= 0; grado--) {
            Racional coeficiente = coeficientes[grado];
            if (coeficiente.esCero()) {
                continue;
            }
            if (texto.isEmpty()) {
                if (coeficiente.signo() < 0) {
                    texto.append("-");
                }
            } else {
                texto.append(coeficiente.signo() < 0 ? " - " : " + ");
            }
            texto.append(termino(coeficiente.signo() < 0 ? coeficiente.negar() : coeficiente,
                    grado, variable));
        }
        return texto.toString();
    }

    /**
     * Escribe el polinomio como se leeria en un libro.
     *
     * <p>Igual que {@link #texto(String)}, salvo cuando el termino de mayor grado es
     * negativo: entonces se empieza por el de menor grado, porque {@code 4 - x^2} se lee
     * mejor que {@code -x^2 + 4} y es como aparece en los enunciados.</p>
     *
     * @param variable nombre de la variable
     * @return el texto
     */
    public String textoParaLeer(String variable) {
        if (principal().signo() >= 0 || esCero()) {
            return texto(variable);
        }
        StringBuilder texto = new StringBuilder();
        for (int grado = 0; grado < coeficientes.length; grado++) {
            Racional coeficiente = coeficientes[grado];
            if (coeficiente.esCero()) {
                continue;
            }
            if (texto.isEmpty()) {
                if (coeficiente.signo() < 0) {
                    texto.append("-");
                }
            } else {
                texto.append(coeficiente.signo() < 0 ? " - " : " + ");
            }
            texto.append(termino(coeficiente.signo() < 0 ? coeficiente.negar() : coeficiente,
                    grado, variable));
        }
        return texto.toString();
    }

    /** Cuantos terminos no nulos tiene. */
    public int cuentaTerminos() {
        int terminos = 0;
        for (Racional coeficiente : coeficientes) {
            if (!coeficiente.esCero()) {
                terminos++;
            }
        }
        return terminos;
    }

    /**
     * Escribe el polinomio entre parentesis si tiene mas de un termino.
     *
     * @param variable nombre de la variable
     * @return el texto listo para ir como factor
     */
    public String textoComoFactor(String variable) {
        int terminos = 0;
        for (Racional coeficiente : coeficientes) {
            if (!coeficiente.esCero()) {
                terminos++;
            }
        }
        String texto = texto(variable);
        return terminos > 1 ? "(" + texto + ")" : texto;
    }

    /** Escribe un termino con coeficiente positivo. */
    static String termino(Racional magnitud, int grado, String variable) {
        if (grado == 0) {
            return magnitud.texto();
        }
        String parteVariable = grado == 1 ? variable : variable + "^" + grado;
        if (magnitud.equals(Racional.UNO)) {
            return parteVariable;
        }
        if (magnitud.esEntero()) {
            return magnitud.numerador() + parteVariable;
        }
        String arriba = magnitud.numerador().equals(BigInteger.ONE)
                ? parteVariable
                : magnitud.numerador() + parteVariable;
        return arriba + "/" + magnitud.denominador();
    }

    @Override
    public String toString() {
        return texto("x");
    }

    // ------------------------------------------------------------------
    // APOYO
    // ------------------------------------------------------------------

    private static Racional[] recortar(Racional[] valores) {
        if (valores.length == 0) {
            return new Racional[]{Racional.CERO};
        }
        int ultimo = valores.length - 1;
        while (ultimo > 0 && valores[ultimo].esCero()) {
            ultimo--;
        }
        return Arrays.copyOf(valores, ultimo + 1);
    }
}
