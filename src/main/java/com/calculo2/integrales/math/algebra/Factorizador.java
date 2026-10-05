package com.calculo2.integrales.math.algebra;

import com.calculo2.integrales.math.simbolico.Polinomio;
import com.calculo2.integrales.math.util.BuscadorRaices;
import com.calculo2.integrales.math.util.Redondeo;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Resuelve {@code P(x) = 0} factorizando, y deja escrito cada paso como en el cuaderno.
 *
 * <p>El orden es el que se ensena en clase:</p>
 *
 * <ol>
 *   <li>Dejar los coeficientes enteros y el primero positivo.</li>
 *   <li>Sacar el factor comun {@code x^k}.</li>
 *   <li>Si queda un trinomio, factorizarlo (diferencia de cuadrados, dos numeros que
 *       multiplicados den c y sumados den b, o formula general).</li>
 *   <li>Si queda un polinomio de grado tres o mas, buscar raices racionales probando los
 *       divisores del termino independiente y dividir por {@code (x - r)}.</li>
 *   <li>Igualar cada factor a cero.</li>
 * </ol>
 *
 * <p>Las raices salen exactas siempre que se pueda: {@code 1/2}, {@code 1 + sqrt(2)},
 * {@code cbrt(3)}. Solo cuando un factor no tiene raices racionales ni es una potencia
 * pura se recurre al calculo numerico, y se dice.</p>
 */
public final class Factorizador {

    private Factorizador() {
    }

    /** Por encima de este valor no se enumeran divisores: serian demasiados. */
    private static final long LIMITE_DIVISORES = 10_000_000L;

    /**
     * Una solucion de la ecuacion.
     *
     * @param valor         el valor numerico
     * @param texto         el valor escrito de forma exacta, o aproximado si no se pudo
     * @param multiplicidad cuantas veces aparece como raiz
     * @param exacta        true si el texto es exacto
     */
    public record Raiz(double valor, String texto, int multiplicidad, boolean exacta) {
    }

    /**
     * Lo que se obtuvo al resolver.
     *
     * @param raices           las soluciones reales, ordenadas
     * @param formaFactorizada el polinomio factorizado, o cadena vacia si no se factorizo
     * @param pasos            el procedimiento, linea a linea
     * @param exacto           true si todas las raices se obtuvieron de forma exacta
     */
    public record Resultado(List<Raiz> raices, String formaFactorizada, List<String> pasos,
                            boolean exacto) {

        /** Los valores de las raices, sin repetir. */
        public List<Double> valores() {
            List<Double> valores = new ArrayList<>();
            for (Raiz raiz : raices) {
                valores.add(raiz.valor());
            }
            return valores;
        }
    }

    /** Un factor del polinomio, con su exponente. */
    private record Factor(String texto, int exponente) {
    }

    /** El estado que se va llenando mientras se factoriza. */
    private static final class Trabajo {
        final String variable;
        final List<String> pasos = new ArrayList<>();
        final List<Factor> factores = new ArrayList<>();
        final List<String> resoluciones = new ArrayList<>();
        final List<Raiz> raices = new ArrayList<>();
        boolean exacto = true;

        Trabajo(String variable) {
            this.variable = variable;
        }
    }

    /**
     * Resuelve {@code P(v) = 0}.
     *
     * @param polinomio el polinomio, ya con todo pasado a un lado
     * @param variable  nombre de la variable
     * @return las raices reales y el procedimiento
     */
    public static Resultado resolverIgualACero(Polinomio polinomio, String variable) {
        Optional<PolinomioRacional> exacto = PolinomioRacional.desde(polinomio);
        if (exacto.isEmpty()) {
            return resolverNumericamente(polinomio, variable);
        }

        Trabajo trabajo = new Trabajo(variable);
        PolinomioRacional p = exacto.get();

        if (p.grado() == 0) {
            if (p.esCero()) {
                trabajo.pasos.add("La ecuacion se cumple para cualquier valor de " + variable
                        + ": las dos expresiones son la misma.");
            } else {
                trabajo.pasos.add("Queda " + p.texto(variable) + " = 0, que es imposible: "
                        + "la ecuacion no tiene solucion.");
            }
            return new Resultado(List.of(), "", trabajo.pasos, true);
        }

        // ---------- 1. Coeficientes enteros y primero positivo ----------
        Racional contenido = p.contenido();
        PolinomioRacional q = p.escalar(contenido.inverso());

        if (!contenido.equals(Racional.UNO)) {
            trabajo.pasos.add(comoSeNormaliza(contenido));
            trabajo.pasos.add("   " + q.texto(variable) + " = 0");
        }

        // ---------- 2. Factor comun x^k ----------
        int k = q.menorGrado();
        PolinomioRacional resto = q;
        if (k > 0 && q.grado() > k) {
            resto = q.dividirEntrePotenciaDeX(k);
            String factor = potenciaDeVariable(variable, k);
            trabajo.pasos.add("Se saca factor comun " + factor + ":");
            trabajo.pasos.add("   " + factor + resto.textoComoFactor(variable) + " = 0");
            agregarFactorX(trabajo, k);
        } else if (k > 0) {
            // El polinomio entero es a*x^k: la unica raiz es cero.
            resto = q.dividirEntrePotenciaDeX(k);
            agregarFactorX(trabajo, k);
        }

        // ---------- 3. El resto ----------
        factorizarResto(resto, trabajo);

        // ---------- 4. Forma factorizada y cada factor a cero ----------
        String forma = escribirForma(trabajo.factores);
        boolean seFactorizo = trabajo.factores.size() > 1
                || (!trabajo.factores.isEmpty() && trabajo.factores.get(0).exponente() > 1);

        if (seFactorizo) {
            trabajo.pasos.add("Factorizado:");
            trabajo.pasos.add("   " + forma + " = 0");
        }
        if (!trabajo.resoluciones.isEmpty()) {
            trabajo.pasos.add(seFactorizo
                    ? "Cada factor se iguala a cero:"
                    : "Se despeja:");
            for (String resolucion : trabajo.resoluciones) {
                trabajo.pasos.add("   " + resolucion);
            }
        }

        List<Raiz> ordenadas = unir(trabajo.raices);
        if (ordenadas.isEmpty()) {
            trabajo.pasos.add("La ecuacion no tiene soluciones reales.");
        } else {
            trabajo.pasos.add("Soluciones: " + listar(variable, ordenadas));
        }

        return new Resultado(ordenadas, seFactorizo ? forma : "", trabajo.pasos, trabajo.exacto);
    }

    // ------------------------------------------------------------------
    // FACTORIZAR LO QUE QUEDA
    // ------------------------------------------------------------------

    /** Factoriza un polinomio sin factor x comun, segun su grado. */
    private static void factorizarResto(PolinomioRacional p, Trabajo trabajo) {
        if (p.grado() <= 0) {
            return;
        }
        // Se trabaja siempre con un polinomio primitivo de primer coeficiente positivo:
        // asi los factores lineales salen con coeficientes enteros y su producto
        // reproduce exactamente el polinomio.
        Racional contenido = p.contenido();
        if (!contenido.equals(Racional.UNO)) {
            p = p.escalar(contenido.inverso());
            trabajo.pasos.add(comoSeNormaliza(contenido));
            trabajo.pasos.add("   " + p.texto(trabajo.variable) + " = 0");
        }
        if (p.grado() == 1) {
            agregarLineal(p, 1, trabajo);
            return;
        }
        if (p.grado() == 2) {
            factorizarCuadratico(p, trabajo);
            return;
        }
        factorizarPorRaicesRacionales(p, trabajo);
    }

    /** Un factor lineal ax + b, con la raiz que aporta. */
    private static void agregarLineal(PolinomioRacional p, int exponente, Trabajo trabajo) {
        String v = trabajo.variable;
        Racional raiz = p.coeficiente(0).negar().dividir(p.coeficiente(1));
        trabajo.factores.add(new Factor(p.textoComoFactor(v), exponente));
        trabajo.raices.add(new Raiz(raiz.valor(), raiz.texto(), exponente, true));

        String despejado = v + " = " + raiz.texto();
        trabajo.resoluciones.add(p.texto(v).equals(v)
                ? despejado
                : p.texto(v) + " = 0  ->  " + despejado);
    }

    /**
     * Factoriza un trinomio de segundo grado.
     *
     * <p>Se elige la tecnica que se usaria a mano: diferencia de cuadrados si no hay
     * termino lineal, dos numeros si el trinomio es monico y sus raices son enteras, y la
     * formula general en cualquier otro caso. Si el discriminante es negativo el trinomio
     * no tiene raices reales y se dice por que.</p>
     */
    private static void factorizarCuadratico(PolinomioRacional p, Trabajo trabajo) {
        String v = trabajo.variable;
        Racional a = p.coeficiente(2);
        Racional b = p.coeficiente(1);
        Racional c = p.coeficiente(0);
        Racional discriminante = b.multiplicar(b).restar(Racional.entero(4).multiplicar(a).multiplicar(c));

        if (discriminante.signo() < 0) {
            trabajo.pasos.add("El factor " + p.texto(v) + " no se puede descomponer: su "
                    + "discriminante b^2 - 4ac = " + escribirDiscriminante(a, b, c)
                    + " = " + discriminante.texto() + " es negativo, asi que no tiene raices "
                    + "reales.");
            trabajo.factores.add(new Factor(p.textoComoFactor(v), 1));
            trabajo.resoluciones.add(p.texto(v) + " = 0  no tiene soluciones reales");
            return;
        }

        Optional<Racional> raizDelDiscriminante = discriminante.raizCuadrada();

        if (raizDelDiscriminante.isPresent()) {
            Racional dosA = Racional.entero(2).multiplicar(a);
            Racional r1 = b.negar().sumar(raizDelDiscriminante.get()).dividir(dosA);
            Racional r2 = b.negar().restar(raizDelDiscriminante.get()).dividir(dosA);

            if (discriminante.esCero()) {
                trabajo.pasos.add("Es un trinomio cuadrado perfecto:");
                PolinomioRacional lineal = factorLineal(r1, Racional.UNO);
                trabajo.pasos.add("   " + p.texto(v) + " = " + lineal.textoComoFactor(v) + "^2");
                agregarLineal(lineal, 2, trabajo);
                return;
            }

            if (b.esCero()) {
                trabajo.pasos.add("No hay termino en " + v + ": es una diferencia de cuadrados, "
                        + "a^2 - b^2 = (a - b)(a + b).");
            } else if (a.equals(Racional.UNO) && r1.esEntero() && r2.esEntero()) {
                Racional p1 = r1.negar();
                Racional p2 = r2.negar();
                trabajo.pasos.add("Se buscan dos numeros que multiplicados den " + c.texto()
                        + " y sumados den " + b.texto() + ": son " + p1.texto() + " y "
                        + p2.texto() + ".");
            } else {
                trabajo.pasos.add("Con la formula general, " + v + " = (-b +- sqrt(b^2 - 4ac)) / (2a):");
                trabajo.pasos.add("   b^2 - 4ac = " + escribirDiscriminante(a, b, c) + " = "
                        + discriminante.texto());
                trabajo.pasos.add("   " + v + " = (" + b.negar().texto() + " +- "
                        + raizDelDiscriminante.get().texto() + ") / " + dosA.texto());
            }

            // El polinomio es primitivo, asi que su coeficiente principal es justo el
            // producto de los denominadores de las raices: (qx - p)(sx - r) lo reproduce.
            PolinomioRacional primero = factorLineal(r1, Racional.UNO);
            PolinomioRacional segundo = factorLineal(r2, Racional.UNO);
            trabajo.pasos.add("   " + p.texto(v) + " = " + primero.textoComoFactor(v)
                    + segundo.textoComoFactor(v));
            agregarLineal(primero, 1, trabajo);
            agregarLineal(segundo, 1, trabajo);
            return;
        }

        // Raices irracionales: la formula general las da exactas con una raiz cuadrada.
        Racional dosA = Racional.entero(2).multiplicar(a);
        Racional centro = b.negar().dividir(dosA);
        RaizSimplificada raiz = RaizSimplificada.de(discriminante);
        Racional radio = raiz.coeficiente().dividir(dosA);

        trabajo.pasos.add("El factor " + p.texto(v) + " no tiene raices racionales. Con la "
                + "formula general, " + v + " = (-b +- sqrt(b^2 - 4ac)) / (2a):");
        trabajo.pasos.add("   b^2 - 4ac = " + escribirDiscriminante(a, b, c) + " = "
                + discriminante.texto());
        trabajo.pasos.add("   " + v + " = (" + b.negar().texto() + " +- sqrt("
                + discriminante.texto() + ")) / " + dosA.texto());

        String mas = escribirCentroMasRadio(centro, radio, raiz.radicando(), true);
        String menos = escribirCentroMasRadio(centro, radio, raiz.radicando(), false);

        double valorRadio = Math.abs(radio.valor()) * Math.sqrt(raiz.radicando().doubleValue());
        double valorMas = centro.valor() + valorRadio;
        double valorMenos = centro.valor() - valorRadio;

        trabajo.factores.add(new Factor(p.textoComoFactor(v), 1));
        trabajo.raices.add(new Raiz(valorMas, mas, 1, true));
        trabajo.raices.add(new Raiz(valorMenos, menos, 1, true));
        trabajo.resoluciones.add(p.texto(v) + " = 0  ->  " + v + " = " + mas + ",  "
                + v + " = " + menos);
    }

    /**
     * Busca raices racionales en un polinomio de grado tres o mas.
     *
     * <p>Por el teorema de la raiz racional, toda raiz racional {@code p/q} cumple que
     * {@code p} divide al termino independiente y {@code q} al coeficiente principal. Se
     * prueban esos candidatos y, en cuanto uno anula el polinomio, se divide entre
     * {@code (x - r)} y se sigue con el cociente.</p>
     */
    private static void factorizarPorRaicesRacionales(PolinomioRacional p, Trabajo trabajo) {
        String v = trabajo.variable;
        Optional<Racional> encontrada = buscarRaizRacional(p);

        if (encontrada.isPresent()) {
            Racional r = encontrada.get();
            PolinomioRacional factor = factorLineal(r, Racional.UNO);
            PolinomioRacional cociente = p.dividirEntreRaiz(r);
            // Dividir entre (x - p/q) y escribir el factor como (qx - p) obliga a pasar
            // ese q al cociente, o el producto no daria el polinomio de partida.
            if (!r.esEntero()) {
                cociente = cociente.escalar(new Racional(BigInteger.ONE, r.denominador()));
            }

            trabajo.pasos.add("Se prueban los divisores del termino independiente ("
                    + p.coeficiente(0).texto() + ")"
                    + (p.principal().equals(Racional.UNO) ? ""
                       : " entre los del coeficiente principal (" + p.principal().texto() + ")")
                    + ". Con " + v + " = " + r.texto() + " el polinomio vale cero, asi que "
                    + factor.textoComoFactor(v) + " es un factor. Dividiendo (division sintetica):");
            trabajo.pasos.add("   " + p.texto(v) + " = " + factor.textoComoFactor(v)
                    + cociente.textoComoFactor(v));

            // La misma raiz puede repetirse en el cociente.
            int multiplicidad = 1;
            while (cociente.grado() >= 1 && cociente.evaluar(r).esCero()) {
                cociente = cociente.dividirEntreRaiz(r);
                multiplicidad++;
            }
            if (multiplicidad > 1) {
                trabajo.pasos.add("   " + v + " = " + r.texto() + " vuelve a anular el cociente: "
                        + "es una raiz de multiplicidad " + multiplicidad + ".");
            }
            agregarLineal(factor, multiplicidad, trabajo);
            factorizarResto(cociente, trabajo);
            return;
        }

        // Sin raices racionales. Si solo hay dos terminos, es una potencia pura.
        if (esPotenciaPura(p)) {
            resolverPotenciaPura(p, trabajo);
            return;
        }

        // Ultimo recurso: raices numericas.
        trabajo.exacto = false;
        Polinomio decimal = p.aPolinomio();
        double cota = cotaDeCauchy(p);
        List<Double> raices = BuscadorRaices.buscarEnIntervalo(decimal::evaluar, -cota, cota);

        trabajo.pasos.add("El factor " + p.texto(v) + " no tiene raices racionales (ninguno de "
                + "los divisores del termino independiente lo anula), asi que sus raices reales "
                + "se calculan numericamente.");
        trabajo.factores.add(new Factor(p.textoComoFactor(v), 1));

        if (raices.isEmpty()) {
            trabajo.resoluciones.add(p.texto(v) + " = 0  no tiene soluciones reales");
            return;
        }
        List<String> textos = new ArrayList<>();
        for (double raiz : raices) {
            String texto = "≈ " + Redondeo.texto(raiz);
            trabajo.raices.add(new Raiz(raiz, texto, 1, false));
            textos.add(v + " " + texto);
        }
        trabajo.resoluciones.add(p.texto(v) + " = 0  ->  " + String.join(",  ", textos));
    }

    /** Resuelve {@code a*x^n + c = 0} despejando la potencia. */
    private static void resolverPotenciaPura(PolinomioRacional p, Trabajo trabajo) {
        String v = trabajo.variable;
        int n = p.grado();
        Racional valor = p.coeficiente(0).negar().dividir(p.principal());

        trabajo.factores.add(new Factor(p.textoComoFactor(v), 1));
        String potencia = potenciaDeVariable(v, n);

        if (n % 2 == 1) {
            String texto = raizEnesimaTexto(valor, n);
            double numero = Math.signum(valor.valor()) * Math.pow(Math.abs(valor.valor()), 1.0 / n);
            trabajo.raices.add(new Raiz(numero, texto, 1, true));
            trabajo.resoluciones.add(p.texto(v) + " = 0  ->  " + potencia + " = " + valor.texto()
                    + "  ->  " + v + " = " + texto);
            return;
        }

        if (valor.signo() < 0) {
            trabajo.resoluciones.add(p.texto(v) + " = 0  ->  " + potencia + " = " + valor.texto()
                    + ", que no tiene soluciones reales porque una potencia par no es negativa");
            return;
        }

        String texto = raizEnesimaTexto(valor, n);
        double numero = Math.pow(valor.valor(), 1.0 / n);
        trabajo.raices.add(new Raiz(numero, texto, 1, true));
        trabajo.raices.add(new Raiz(-numero, "-" + texto, 1, true));
        trabajo.resoluciones.add(p.texto(v) + " = 0  ->  " + potencia + " = " + valor.texto()
                + "  ->  " + v + " = +-" + texto);
    }

    // ------------------------------------------------------------------
    // APOYO ALGEBRAICO
    // ------------------------------------------------------------------

    /** Prueba los candidatos del teorema de la raiz racional. */
    private static Optional<Racional> buscarRaizRacional(PolinomioRacional p) {
        Racional contenido = p.contenido();
        PolinomioRacional entero = p.escalar(contenido.inverso());

        BigInteger independiente = entero.coeficiente(0).numerador().abs();
        BigInteger principal = entero.principal().numerador().abs();

        if (independiente.signum() == 0) {
            return Optional.of(Racional.CERO);
        }
        if (independiente.compareTo(BigInteger.valueOf(LIMITE_DIVISORES)) > 0
                || principal.compareTo(BigInteger.valueOf(LIMITE_DIVISORES)) > 0) {
            return Optional.empty();
        }

        List<Racional> candidatos = new ArrayList<>();
        for (long arriba : divisores(independiente.longValue())) {
            for (long abajo : divisores(principal.longValue())) {
                candidatos.add(Racional.de(arriba, abajo));
                candidatos.add(Racional.de(-arriba, abajo));
            }
        }
        // Primero los mas sencillos: es el orden en que se prueban a mano.
        candidatos.sort(Comparator.comparingDouble((Racional r) -> Math.abs(r.valor()))
                .thenComparing(r -> -r.signo()));

        for (Racional candidato : candidatos) {
            if (p.evaluar(candidato).esCero()) {
                return Optional.of(candidato);
            }
        }
        return Optional.empty();
    }

    /** Los divisores positivos de un entero positivo. */
    private static List<Long> divisores(long n) {
        List<Long> divisores = new ArrayList<>();
        for (long d = 1; d * d <= n; d++) {
            if (n % d == 0) {
                divisores.add(d);
                if (d != n / d) {
                    divisores.add(n / d);
                }
            }
        }
        return divisores;
    }

    /** El factor lineal que anula r, con el coeficiente indicado delante de x. */
    private static PolinomioRacional factorLineal(Racional raiz, Racional coeficiente) {
        // Si la raiz es una fraccion p/q, el factor se escribe qx - p, sin fracciones.
        if (!raiz.esEntero() && coeficiente.equals(Racional.UNO)) {
            Racional q = new Racional(raiz.denominador(), BigInteger.ONE);
            Racional p = new Racional(raiz.numerador(), BigInteger.ONE);
            return PolinomioRacional.de(p.negar(), q);
        }
        if (!coeficiente.equals(Racional.UNO)) {
            return PolinomioRacional.de(raiz.multiplicar(coeficiente).negar(), coeficiente);
        }
        return PolinomioRacional.de(raiz.negar(), Racional.UNO);
    }

    /** Indica si el polinomio solo tiene el termino principal y el independiente. */
    private static boolean esPotenciaPura(PolinomioRacional p) {
        if (p.coeficiente(0).esCero()) {
            return false;
        }
        for (int i = 1; i < p.grado(); i++) {
            if (!p.coeficiente(i).esCero()) {
                return false;
            }
        }
        return true;
    }

    /** Cota de Cauchy: todas las raices reales caen en [-cota, cota]. */
    private static double cotaDeCauchy(PolinomioRacional p) {
        double principal = Math.abs(p.principal().valor());
        double maximo = 0.0;
        for (int i = 0; i < p.grado(); i++) {
            maximo = Math.max(maximo, Math.abs(p.coeficiente(i).valor()) / principal);
        }
        return 1.0 + maximo;
    }

    // ------------------------------------------------------------------
    // ESCRITURA
    // ------------------------------------------------------------------

    /** Explica por que numero se multiplico la ecuacion. */
    private static String comoSeNormaliza(Racional contenido) {
        Racional inverso = contenido.inverso();
        if (contenido.equals(Racional.entero(-1))) {
            return "Se multiplican los dos lados por -1, para que el primer termino sea positivo:";
        }
        if (contenido.esEntero()) {
            return "Se dividen los dos lados entre " + contenido.texto() + ":";
        }
        return "Se multiplican los dos lados por " + inverso.texto()
                + ", para trabajar con coeficientes enteros:";
    }

    /** Agrega el factor x^k y su raiz cero. */
    private static void agregarFactorX(Trabajo trabajo, int k) {
        String v = trabajo.variable;
        String factor = potenciaDeVariable(v, k);
        trabajo.factores.add(new Factor(factor, 1));
        trabajo.raices.add(new Raiz(0.0, "0", k, true));
        trabajo.resoluciones.add(k == 1 ? v + " = 0" : factor + " = 0  ->  " + v + " = 0");
    }

    private static String potenciaDeVariable(String v, int k) {
        return k == 1 ? v : v + "^" + k;
    }

    private static String escribirForma(List<Factor> factores) {
        StringBuilder forma = new StringBuilder();
        for (Factor factor : factores) {
            if (factor.exponente() > 1) {
                String base = factor.texto().startsWith("(") ? factor.texto() : "(" + factor.texto() + ")";
                forma.append(base).append('^').append(factor.exponente());
            } else {
                forma.append(factor.texto());
            }
        }
        return forma.toString();
    }

    private static String escribirDiscriminante(Racional a, Racional b, Racional c) {
        return "(" + b.texto() + ")^2 - 4(" + a.texto() + ")(" + c.texto() + ")";
    }

    /** Escribe centro +- radio*sqrt(t) como texto exacto. */
    private static String escribirCentroMasRadio(Racional centro, Racional radio,
                                                 BigInteger radicando, boolean mas) {
        Racional magnitud = radio.signo() < 0 ? radio.negar() : radio;
        String raiz = "sqrt(" + radicando + ")";
        String termino;
        if (magnitud.equals(Racional.UNO)) {
            termino = raiz;
        } else if (magnitud.esEntero()) {
            termino = magnitud.texto() + raiz;
        } else if (magnitud.numerador().equals(BigInteger.ONE)) {
            termino = raiz + "/" + magnitud.denominador();
        } else {
            termino = magnitud.numerador() + raiz + "/" + magnitud.denominador();
        }
        if (centro.esCero()) {
            return mas ? termino : "-" + termino;
        }
        return centro.texto() + (mas ? " + " : " - ") + termino;
    }

    /** Escribe la raiz n-esima de un racional, exacta si lo es. */
    private static String raizEnesimaTexto(Racional valor, int n) {
        double numero = Math.signum(valor.valor()) * Math.pow(Math.abs(valor.valor()), 1.0 / n);
        Optional<Racional> exacta = Racional.desdeDouble(numero);
        if (exacta.isPresent() && exacta.get().elevar(n).equals(valor)) {
            return exacta.get().texto();
        }
        if (n == 2) {
            return "sqrt(" + valor.texto() + ")";
        }
        if (n == 3) {
            return "cbrt(" + valor.texto() + ")";
        }
        return "(" + valor.texto() + ")^(1/" + n + ")";
    }

    /** Une raices repetidas y las ordena. */
    private static List<Raiz> unir(List<Raiz> raices) {
        List<Raiz> ordenadas = new ArrayList<>(raices);
        ordenadas.sort(Comparator.comparingDouble(Raiz::valor));
        List<Raiz> unidas = new ArrayList<>();
        for (Raiz raiz : ordenadas) {
            if (!unidas.isEmpty()
                    && Math.abs(unidas.get(unidas.size() - 1).valor() - raiz.valor()) < 1e-9) {
                Raiz anterior = unidas.remove(unidas.size() - 1);
                unidas.add(new Raiz(anterior.valor(), anterior.texto(),
                        anterior.multiplicidad() + raiz.multiplicidad(),
                        anterior.exacta() && raiz.exacta()));
            } else {
                unidas.add(raiz);
            }
        }
        return unidas;
    }

    /** Escribe la lista final de soluciones. */
    static String listar(String variable, List<Raiz> raices) {
        List<String> textos = new ArrayList<>();
        for (Raiz raiz : raices) {
            String texto = raiz.texto();
            boolean esDecimalExacto = raiz.exacta() && !texto.contains("sqrt") && !texto.contains("cbrt")
                    && !texto.contains("^") && !texto.contains("/");
            if (raiz.exacta() && !esDecimalExacto) {
                texto += " (≈ " + Redondeo.texto(raiz.valor()) + ")";
            }
            textos.add(variable + (texto.startsWith("≈") ? " " : " = ") + texto);
        }
        return String.join(",  ", textos);
    }

    /** Respaldo cuando los coeficientes no son fracciones exactas. */
    private static Resultado resolverNumericamente(Polinomio polinomio, String variable) {
        double principal = Math.abs(polinomio.coeficiente(polinomio.grado()));
        double maximo = 0.0;
        for (int i = 0; i < polinomio.grado(); i++) {
            maximo = Math.max(maximo, Math.abs(polinomio.coeficiente(i)) / Math.max(principal, 1e-300));
        }
        double cota = 1.0 + maximo;
        List<Double> valores = BuscadorRaices.buscarEnIntervalo(polinomio::evaluar, -cota, cota);

        List<Raiz> raices = new ArrayList<>();
        for (double valor : valores) {
            raices.add(new Raiz(valor, "≈ " + Redondeo.texto(valor), 1, false));
        }
        List<String> pasos = new ArrayList<>();
        pasos.add("Los coeficientes no son fracciones sencillas, asi que las raices se calculan "
                + "numericamente.");
        pasos.add(raices.isEmpty()
                ? "La ecuacion no tiene soluciones reales."
                : "Soluciones: " + listar(variable, raices));
        return new Resultado(raices, "", pasos, false);
    }

    /**
     * Una raiz cuadrada simplificada: sqrt(n/d) = coeficiente * sqrt(radicando).
     *
     * @param coeficiente la parte que sale de la raiz
     * @param radicando   lo que queda dentro, sin factores cuadrados
     */
    record RaizSimplificada(Racional coeficiente, BigInteger radicando) {

        /** Simplifica la raiz de un racional positivo. */
        static RaizSimplificada de(Racional valor) {
            // sqrt(n/d) = sqrt(n*d)/d
            BigInteger producto = valor.numerador().multiply(valor.denominador());
            BigInteger fuera = BigInteger.ONE;
            BigInteger dentro = BigInteger.ONE;
            BigInteger resto = producto;
            for (long primo = 2; BigInteger.valueOf(primo * primo).compareTo(resto) <= 0; primo++) {
                BigInteger p = BigInteger.valueOf(primo);
                int veces = 0;
                while (resto.mod(p).signum() == 0) {
                    resto = resto.divide(p);
                    veces++;
                }
                fuera = fuera.multiply(p.pow(veces / 2));
                if (veces % 2 == 1) {
                    dentro = dentro.multiply(p);
                }
                if (primo > 1_000_000) {
                    break;
                }
            }
            dentro = dentro.multiply(resto);
            return new RaizSimplificada(new Racional(fuera, valor.denominador()), dentro);
        }
    }
}
