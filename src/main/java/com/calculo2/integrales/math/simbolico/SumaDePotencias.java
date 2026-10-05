package com.calculo2.integrales.math.simbolico;

import com.calculo2.integrales.math.algebra.Racional;
import com.calculo2.integrales.math.parser.NodoExpresion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Una suma de potencias con exponentes fraccionarios: {@code 4sqrt(2) x^(3/2) + x^(1/2)}.
 *
 * <p>Amplia la regla de la potencia a las raices, que es lo que se hace en clase:
 * {@code sqrt(x) = x^(1/2)}, y entonces {@code x*sqrt(x) = x^(3/2)} se integra igual que
 * cualquier potencia. Es justo lo que aparece en el metodo de capas cuando la curva es
 * una raiz: el integrando {@code x * sqrt(8x)} no es un polinomio, pero si una potencia.</p>
 *
 * <p>Los coeficientes se guardan exactos, como una fraccion por una raiz cuadrada de un
 * entero: {@code sqrt(8x) = 2sqrt(2) x^(1/2)}. Asi la primitiva se escribe
 * {@code (8sqrt(2)/5) x^(5/2)} en lugar de {@code 2.262742 x^(5/2)}.</p>
 */
public final class SumaDePotencias {

    /** Exponente maximo entero al desarrollar potencias de sumas. */
    private static final int POTENCIA_MAXIMA = 12;

    /**
     * Identifica un termino: el exponente de la variable y el entero bajo la raiz del
     * coeficiente. Dos terminos con la misma clave se pueden sumar.
     */
    private record Clave(Racional exponente, long radical) {
    }

    /** Coeficiente racional de cada termino. */
    private final Map<Clave, Racional> terminos;

    private SumaDePotencias(Map<Clave, Racional> terminos) {
        Map<Clave, Racional> limpios = new LinkedHashMap<>();
        terminos.forEach((clave, coeficiente) -> {
            if (!coeficiente.esCero()) {
                limpios.put(clave, coeficiente);
            }
        });
        this.terminos = limpios;
    }

    private static SumaDePotencias termino(Racional coeficiente, long radical, Racional exponente) {
        Map<Clave, Racional> mapa = new LinkedHashMap<>();
        mapa.put(new Clave(exponente, radical), coeficiente);
        return new SumaDePotencias(mapa);
    }

    // ------------------------------------------------------------------
    // CONVERSION
    // ------------------------------------------------------------------

    /**
     * Convierte una expresion, si es una suma de potencias de la variable.
     *
     * @param nodo la expresion
     * @return la suma, o vacio si la expresion no tiene esa forma
     */
    public static Optional<SumaDePotencias> desde(NodoExpresion nodo) {
        return switch (nodo) {
            case NodoExpresion.Numero numero -> Racional.desdeDouble(numero.valor())
                    .map(q -> termino(q, 1, Racional.CERO));
            case NodoExpresion.Constante ignorada -> Optional.empty();
            case NodoExpresion.Variable ignorada ->
                    Optional.of(termino(Racional.UNO, 1, Racional.UNO));
            case NodoExpresion.Negacion negacion -> desde(negacion.operando()).map(s -> s.escalar(Racional.entero(-1)));
            case NodoExpresion.LlamadaFuncion llamada -> desdeFuncion(llamada);
            case NodoExpresion.OperacionBinaria operacion -> desdeOperacion(operacion);
        };
    }

    private static Optional<SumaDePotencias> desdeFuncion(NodoExpresion.LlamadaFuncion llamada) {
        Optional<SumaDePotencias> argumento = desde(llamada.argumento());
        if (argumento.isEmpty()) {
            return Optional.empty();
        }
        return switch (llamada.nombre()) {
            case "sqrt", "raiz" -> argumento.get().elevar(Racional.de(1, 2));
            case "cbrt" -> argumento.get().elevar(Racional.de(1, 3));
            default -> Optional.empty();
        };
    }

    private static Optional<SumaDePotencias> desdeOperacion(NodoExpresion.OperacionBinaria operacion) {
        Optional<SumaDePotencias> izquierda = desde(operacion.izquierda());
        Optional<SumaDePotencias> derecha = desde(operacion.derecha());

        // (c*sqrt(A))^2 con A de varios terminos no es una suma de potencias por partes,
        // pero su cuadrado es un polinomio. El conversor de polinomios ya sabe hacerlo.
        if (izquierda.isEmpty() || derecha.isEmpty()) {
            return ConversorPolinomio.convertir(operacion).flatMap(SumaDePotencias::desdePolinomio);
        }
        SumaDePotencias a = izquierda.get();
        SumaDePotencias b = derecha.get();

        return switch (operacion.simbolo()) {
            case "+" -> Optional.of(a.sumar(b));
            case "-" -> Optional.of(a.sumar(b.escalar(Racional.entero(-1))));
            case "*" -> Optional.of(a.multiplicar(b));
            case "/" -> b.inverso().map(a::multiplicar);
            case "^" -> b.comoNumero().flatMap(a::elevar);
            default -> Optional.empty();
        };
    }

    /** Convierte un polinomio ordinario. */
    private static Optional<SumaDePotencias> desdePolinomio(Polinomio polinomio) {
        Map<Clave, Racional> mapa = new LinkedHashMap<>();
        for (int grado = 0; grado <= polinomio.grado(); grado++) {
            Optional<Racional> coeficiente = Racional.desdeDouble(polinomio.coeficiente(grado));
            if (coeficiente.isEmpty()) {
                return Optional.empty();
            }
            mapa.put(new Clave(Racional.entero(grado), 1), coeficiente.get());
        }
        return Optional.of(new SumaDePotencias(mapa));
    }

    // ------------------------------------------------------------------
    // ALGEBRA
    // ------------------------------------------------------------------

    private SumaDePotencias sumar(SumaDePotencias otra) {
        Map<Clave, Racional> mapa = new LinkedHashMap<>(terminos);
        otra.terminos.forEach((clave, coeficiente) -> mapa.merge(clave, coeficiente, Racional::sumar));
        return new SumaDePotencias(mapa);
    }

    private SumaDePotencias escalar(Racional factor) {
        Map<Clave, Racional> mapa = new LinkedHashMap<>();
        terminos.forEach((clave, coeficiente) -> mapa.put(clave, coeficiente.multiplicar(factor)));
        return new SumaDePotencias(mapa);
    }

    private SumaDePotencias multiplicar(SumaDePotencias otra) {
        Map<Clave, Racional> mapa = new LinkedHashMap<>();
        for (Map.Entry<Clave, Racional> a : terminos.entrySet()) {
            for (Map.Entry<Clave, Racional> b : otra.terminos.entrySet()) {
                long[] raiz = simplificarRaiz(a.getKey().radical() * b.getKey().radical());
                Racional coeficiente = a.getValue().multiplicar(b.getValue())
                        .multiplicar(Racional.entero(raiz[0]));
                Clave clave = new Clave(a.getKey().exponente().sumar(b.getKey().exponente()), raiz[1]);
                mapa.merge(clave, coeficiente, Racional::sumar);
            }
        }
        return new SumaDePotencias(mapa);
    }

    /** El inverso, que solo existe si la suma tiene un unico termino. */
    private Optional<SumaDePotencias> inverso() {
        if (terminos.size() != 1) {
            return Optional.empty();
        }
        Map.Entry<Clave, Racional> unico = terminos.entrySet().iterator().next();
        long s = unico.getKey().radical();
        // 1 / (q sqrt(s) x^e) = (1/(q s)) sqrt(s) x^(-e)
        Racional coeficiente = unico.getValue().multiplicar(Racional.entero(s)).inverso();
        return Optional.of(termino(coeficiente, s, unico.getKey().exponente().negar()));
    }

    /** El valor, si la suma es un numero racional sin variable ni raiz. */
    private Optional<Racional> comoNumero() {
        if (terminos.isEmpty()) {
            return Optional.of(Racional.CERO);
        }
        if (terminos.size() != 1) {
            return Optional.empty();
        }
        Map.Entry<Clave, Racional> unico = terminos.entrySet().iterator().next();
        if (!unico.getKey().exponente().esCero() || unico.getKey().radical() != 1) {
            return Optional.empty();
        }
        return Optional.of(unico.getValue());
    }

    /** Eleva a un exponente racional. */
    private Optional<SumaDePotencias> elevar(Racional exponente) {
        if (terminos.size() == 1) {
            Map.Entry<Clave, Racional> unico = terminos.entrySet().iterator().next();
            return elevarTermino(unico.getValue(), unico.getKey().radical(),
                    unico.getKey().exponente(), exponente);
        }
        // Una suma solo se puede elevar a un entero positivo, desarrollando.
        if (!exponente.esEntero() || exponente.signo() < 0
                || exponente.numerador().compareTo(BigInteger.valueOf(POTENCIA_MAXIMA)) > 0) {
            return Optional.empty();
        }
        SumaDePotencias resultado = termino(Racional.UNO, 1, Racional.CERO);
        for (int i = 0; i < exponente.numerador().intValue(); i++) {
            resultado = resultado.multiplicar(this);
        }
        return Optional.of(resultado);
    }

    /** (q sqrt(s) x^e)^r. */
    private static Optional<SumaDePotencias> elevarTermino(Racional q, long s, Racional e, Racional r) {
        Racional nuevoExponente = e.multiplicar(r);
        if (r.esEntero()) {
            int n = r.numerador().intValue();
            Racional base = n >= 0 ? q.elevar(n) : q.inverso().elevar(-n);
            // sqrt(s)^n
            int m = Math.abs(n);
            Racional parteEntera = Racional.entero(s).elevar(m / 2);
            long radical = (m % 2 == 1) ? s : 1;
            Racional coeficiente = n >= 0
                    ? base.multiplicar(parteEntera)
                    : base.dividir(parteEntera).dividir(Racional.entero(radical));
            return Optional.of(termino(coeficiente, radical, nuevoExponente));
        }
        if (r.denominador().equals(BigInteger.TWO) && s == 1) {
            // q^(p/2) = sqrt(q^p)
            int p = r.numerador().intValue();
            Racional potencia = p >= 0 ? q.elevar(p) : q.inverso().elevar(-p);
            if (potencia.signo() < 0) {
                return Optional.empty();
            }
            // sqrt(n/d) = sqrt(n*d)/d
            BigInteger producto = potencia.numerador().multiply(potencia.denominador());
            if (producto.bitLength() > 62) {
                return Optional.empty();
            }
            long[] raiz = simplificarRaiz(producto.longValue());
            Racional coeficiente = new Racional(BigInteger.valueOf(raiz[0]), potencia.denominador());
            return Optional.of(termino(coeficiente, raiz[1], nuevoExponente));
        }
        if (r.denominador().equals(BigInteger.valueOf(3)) && s == 1) {
            int p = r.numerador().intValue();
            Racional potencia = p >= 0 ? q.elevar(p) : q.inverso().elevar(-p);
            Optional<Racional> cubica = raizCubicaExacta(potencia);
            return cubica.map(c -> termino(c, 1, nuevoExponente));
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // INTEGRACION
    // ------------------------------------------------------------------

    /**
     * La primitiva, termino a termino.
     *
     * @param primitiva la expresion evaluable
     * @param texto     la primitiva escrita
     */
    public record Primitiva(NodoExpresion primitiva, String texto) {
    }

    /**
     * Integra por la regla de la potencia.
     *
     * @param variable nombre de la variable
     * @return la primitiva
     */
    public Primitiva integrar(String variable) {
        NodoExpresion suma = null;
        List<String> textos = new ArrayList<>();

        for (Map.Entry<Clave, Racional> termino : ordenados()) {
            Racional e = termino.getKey().exponente();
            long s = termino.getKey().radical();
            Racional q = termino.getValue();
            NodoExpresion nodo;
            String texto;

            if (e.equals(Racional.entero(-1))) {
                // q sqrt(s) / x  ->  q sqrt(s) ln|x|
                NodoExpresion logaritmo = new NodoExpresion.LlamadaFuncion("ln",
                        new NodoExpresion.LlamadaFuncion("abs", new NodoExpresion.Variable(variable)));
                nodo = new NodoExpresion.OperacionBinaria("*", numero(q, s), logaritmo);
                texto = escribirCoeficiente(q, s, "ln|" + variable + "|");
            } else {
                Racional nuevo = e.sumar(Racional.UNO);
                Racional coeficiente = q.dividir(nuevo);
                nodo = new NodoExpresion.OperacionBinaria("*", numero(coeficiente, s),
                        new NodoExpresion.OperacionBinaria("^", new NodoExpresion.Variable(variable),
                                new NodoExpresion.Numero(nuevo.valor())));
                texto = escribirCoeficiente(coeficiente, s, potenciaTexto(variable, nuevo));
            }
            suma = suma == null ? nodo : new NodoExpresion.OperacionBinaria("+", suma, nodo);
            textos.add(texto);
        }
        if (suma == null) {
            return new Primitiva(new NodoExpresion.Numero(0), "0");
        }
        return new Primitiva(suma, unirTerminos(textos));
    }

    // ------------------------------------------------------------------
    // CONSULTA Y ESCRITURA
    // ------------------------------------------------------------------

    /** Indica si todos los exponentes son enteros no negativos y no hay raices. */
    public boolean esPolinomio() {
        for (Clave clave : terminos.keySet()) {
            if (!clave.exponente().esEntero() || clave.exponente().signo() < 0 || clave.radical() != 1) {
                return false;
            }
        }
        return true;
    }

    /** Indica si la suma no tiene terminos. */
    public boolean esVacia() {
        return terminos.isEmpty();
    }

    /** Indica si algun termino tiene un exponente que no es entero: viene de una raiz. */
    public boolean tieneExponentesFraccionarios() {
        for (Clave clave : terminos.keySet()) {
            if (!clave.exponente().esEntero()) {
                return true;
            }
        }
        return false;
    }

    /** Indica si algun termino es de la forma c/x, que se integra con un logaritmo. */
    public boolean tieneExponenteMenosUno() {
        for (Clave clave : terminos.keySet()) {
            if (clave.exponente().equals(Racional.entero(-1))) {
                return true;
            }
        }
        return false;
    }

    /** Indica si todos los terminos son de la forma c/x. */
    public boolean soloExponenteMenosUno() {
        for (Clave clave : terminos.keySet()) {
            if (!clave.exponente().equals(Racional.entero(-1))) {
                return false;
            }
        }
        return !terminos.isEmpty();
    }

    /** Escribe la suma, del exponente mayor al menor. */
    public String texto(String variable) {
        List<String> textos = new ArrayList<>();
        for (Map.Entry<Clave, Racional> termino : ordenados()) {
            textos.add(escribirCoeficiente(termino.getValue(), termino.getKey().radical(),
                    potenciaTexto(variable, termino.getKey().exponente())));
        }
        return textos.isEmpty() ? "0" : unirTerminos(textos);
    }

    private List<Map.Entry<Clave, Racional>> ordenados() {
        List<Map.Entry<Clave, Racional>> lista = new ArrayList<>(terminos.entrySet());
        lista.sort(Comparator.comparing((Map.Entry<Clave, Racional> t) -> t.getKey().exponente())
                .reversed());
        return lista;
    }

    private static String potenciaTexto(String variable, Racional exponente) {
        if (exponente.esCero()) {
            return "";
        }
        if (exponente.equals(Racional.UNO)) {
            return variable;
        }
        if (exponente.esEntero() && exponente.signo() > 0) {
            return variable + "^" + exponente.texto();
        }
        return variable + "^(" + exponente.texto() + ")";
    }

    /** Escribe q*sqrt(s)*parte, con el estilo de los polinomios: 8sqrt(2)x^(5/2)/5. */
    private static String escribirCoeficiente(Racional q, long s, String parte) {
        boolean negativo = q.signo() < 0;
        Racional magnitud = negativo ? q.negar() : q;
        String raiz = s == 1 ? "" : "sqrt(" + s + ")";
        String arriba = magnitud.numerador().equals(BigInteger.ONE) && (!raiz.isEmpty() || !parte.isEmpty())
                ? "" : magnitud.numerador().toString();
        String cuerpo = arriba + raiz + parte;
        if (cuerpo.isEmpty()) {
            cuerpo = "1";
        }
        String texto = magnitud.esEntero() ? cuerpo : cuerpo + "/" + magnitud.denominador();
        return (negativo ? "-" : "") + texto;
    }

    private static String unirTerminos(List<String> textos) {
        StringBuilder texto = new StringBuilder();
        for (String termino : textos) {
            if (texto.isEmpty()) {
                texto.append(termino);
            } else if (termino.startsWith("-")) {
                texto.append(" - ").append(termino.substring(1));
            } else {
                texto.append(" + ").append(termino);
            }
        }
        return texto.toString();
    }

    private static NodoExpresion numero(Racional q, long s) {
        return new NodoExpresion.Numero(q.valor() * Math.sqrt(s));
    }

    /** n = k^2 * s con s libre de cuadrados; devuelve {k, s}. */
    private static long[] simplificarRaiz(long n) {
        long fuera = 1;
        long dentro = 1;
        long resto = n;
        for (long p = 2; p * p <= resto; p++) {
            int veces = 0;
            while (resto % p == 0) {
                resto /= p;
                veces++;
            }
            for (int i = 0; i < veces / 2; i++) {
                fuera *= p;
            }
            if (veces % 2 == 1) {
                dentro *= p;
            }
        }
        return new long[]{fuera, dentro * resto};
    }

    private static Optional<Racional> raizCubicaExacta(Racional valor) {
        double aproximada = Math.cbrt(valor.valor());
        Optional<Racional> candidata = Racional.desdeDouble(aproximada);
        if (candidata.isPresent() && candidata.get().elevar(3).equals(valor)) {
            return candidata;
        }
        return Optional.empty();
    }
}
