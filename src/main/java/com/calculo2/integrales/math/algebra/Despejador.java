package com.calculo2.integrales.math.algebra;

import com.calculo2.integrales.math.parser.CatalogoFunciones;
import com.calculo2.integrales.math.parser.EvaluadorExpresion;
import com.calculo2.integrales.math.parser.NodoExpresion;
import com.calculo2.integrales.math.simbolico.Polinomio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

/**
 * Despeja una variable en una ecuacion de dos variables.
 *
 * <p>Muchos enunciados dan las curvas sin despejar: {@code y^2 = 8x},
 * {@code 4x^2 + 9y^2 = 36}, {@code (x - 1)^2 = 20 - 4y}, {@code 4y = 4 - x^2}. Para
 * integrar hace falta escribirlas como funcion de la variable de integracion, y a veces
 * de la otra: el metodo de discos alrededor de un eje vertical necesita {@code x = g(y)}.</p>
 *
 * <p>Se resuelven los casos que aparecen en el curso, con el mismo procedimiento que se
 * haria a mano:</p>
 *
 * <ul>
 *   <li>Lineal en la variable: se agrupa y se divide ({@code 4y = 4 - x^2}).</li>
 *   <li>Cuadratica sin termino lineal: raiz cuadrada con las dos ramas
 *       ({@code y^2 = 8x -> y = +-sqrt(8x)}).</li>
 *   <li>Potencia de una expresion lineal: se saca la raiz y se despeja
 *       ({@code (x - 1)^2 = 20 - 4y -> x = 1 +- sqrt(20 - 4y)}).</li>
 *   <li>Cuadratica completa: se completa el cuadrado.</li>
 *   <li>Potencia pura: {@code y = x^3 -> x = cbrt(y)}.</li>
 * </ul>
 *
 * <p>Cada rama que sale se comprueba sustituyendola en la ecuacion original. Si la
 * comprobacion falla, la rama se descarta: un despeje equivocado produciria una region
 * distinta de la del ejercicio.</p>
 */
public final class Despejador {

    private Despejador() {
    }

    /** Las dos variables que pueden aparecer en las curvas del curso. */
    private static final Set<String> VARIABLES = Set.of("x", "y");

    /**
     * Una de las funciones que salen al despejar.
     *
     * @param expresion   la funcion, escrita en la otra variable y lista para el parser
     * @param descripcion que rama es: "rama de arriba", "rama derecha", o vacio si es unica
     * @param signo       +1 o -1 en las ramas de una raiz par, 0 si la rama es unica
     */
    public record Rama(String expresion, String descripcion, int signo) {
    }

    /**
     * El resultado de despejar.
     *
     * @param variable          la variable que se despejo
     * @param otraVariable      aquella en que quedan escritas las ramas
     * @param ramas             las funciones obtenidas
     * @param pasos             el procedimiento, linea a linea
     * @param yaEstabaDespejada true si la ecuacion ya venia de la forma {@code y = f(x)}
     */
    public record Despeje(String variable, String otraVariable, List<Rama> ramas,
                          List<String> pasos, boolean yaEstabaDespejada) {
    }

    /**
     * Despeja una variable.
     *
     * @param izquierda lado izquierdo de la ecuacion, como texto
     * @param derecha   lado derecho
     * @param objetivo  la variable que se quiere despejar, "x" o "y"
     * @return el despeje, o vacio si no se sabe hacer o la variable no aparece
     */
    public static Optional<Despeje> despejar(String izquierda, String derecha, String objetivo) {
        String otra = objetivo.equals("x") ? "y" : "x";
        NodoExpresion ladoIzquierdo;
        NodoExpresion ladoDerecho;
        try {
            ladoIzquierdo = EvaluadorExpresion.analizarConVariables(izquierda, VARIABLES);
            ladoDerecho = EvaluadorExpresion.analizarConVariables(derecha, VARIABLES);
        } catch (RuntimeException e) {
            return Optional.empty();
        }

        boolean izquierdaLaTiene = contiene(ladoIzquierdo, objetivo);
        boolean derechaLaTiene = contiene(ladoDerecho, objetivo);

        if (!izquierdaLaTiene && !derechaLaTiene) {
            return Optional.empty();
        }

        String ecuacion = izquierda.trim() + " = " + derecha.trim();

        // ---------- Ya despejada ----------
        if (esLaVariable(ladoIzquierdo, objetivo) && !derechaLaTiene) {
            return Optional.of(new Despeje(objetivo, otra,
                    List.of(new Rama(derecha.trim(), "", 0)),
                    List.of(ecuacion), true));
        }
        if (esLaVariable(ladoDerecho, objetivo) && !izquierdaLaTiene) {
            return Optional.of(new Despeje(objetivo, otra,
                    List.of(new Rama(izquierda.trim(), "", 0)),
                    List.of(derecha.trim() + " = " + izquierda.trim()), true));
        }

        // ---------- Potencia de una expresion lineal ----------
        Optional<Despeje> porPotencia = izquierdaLaTiene && !derechaLaTiene
                ? despejarPotenciaDeLineal(ladoIzquierdo, derecha.trim(), ladoDerecho, objetivo, ecuacion)
                : (!izquierdaLaTiene
                    ? despejarPotenciaDeLineal(ladoDerecho, izquierda.trim(), ladoIzquierdo, objetivo, ecuacion)
                    : Optional.empty());
        if (porPotencia.isPresent() && verificar(porPotencia.get(), ladoIzquierdo, ladoDerecho)) {
            return porPotencia;
        }

        // ---------- Por grados ----------
        Optional<PolinomioDosVariables> izquierdaPolinomio = PolinomioDosVariables.desde(ladoIzquierdo);
        Optional<PolinomioDosVariables> derechaPolinomio = PolinomioDosVariables.desde(ladoDerecho);
        if (izquierdaPolinomio.isEmpty() || derechaPolinomio.isEmpty()) {
            return Optional.empty();
        }

        PolinomioDosVariables todo = izquierdaPolinomio.get().restar(derechaPolinomio.get());
        Optional<Despeje> porGrados = despejarPorGrados(todo, objetivo, ecuacion);
        if (porGrados.isPresent() && verificar(porGrados.get(), ladoIzquierdo, ladoDerecho)) {
            return porGrados;
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // POTENCIA DE UNA EXPRESION LINEAL: (ax + b)^n = R
    // ------------------------------------------------------------------

    private static Optional<Despeje> despejarPotenciaDeLineal(NodoExpresion conVariable, String textoOtroLado,
                                                              NodoExpresion otroLado, String objetivo,
                                                              String ecuacion) {
        if (!(conVariable instanceof NodoExpresion.OperacionBinaria potencia)
                || !potencia.simbolo().equals("^")
                || !(potencia.derecha() instanceof NodoExpresion.Numero exponente)) {
            return Optional.empty();
        }
        double valorExponente = exponente.valor();
        if (valorExponente < 2 || valorExponente > 9
                || Math.abs(valorExponente - Math.rint(valorExponente)) > 1e-9) {
            return Optional.empty();
        }
        int n = (int) Math.rint(valorExponente);

        Optional<PolinomioDosVariables> base = PolinomioDosVariables.desde(potencia.izquierda());
        String otra = objetivo.equals("x") ? "y" : "x";
        if (base.isEmpty() || base.get().gradoEn(objetivo) != 1 || base.get().gradoEn(otra) != 0) {
            return Optional.empty();
        }
        List<Polinomio> coeficientes = base.get().coeficientesEn(objetivo);
        double a = coeficientes.get(1).coeficiente(0);
        double b = coeficientes.get(0).coeficiente(0);
        if (Math.abs(a) < 1e-12) {
            return Optional.empty();
        }

        String radicando = otroLado instanceof NodoExpresion.Numero
                || otroLado instanceof NodoExpresion.Variable
                ? textoOtroLado : "(" + textoOtroLado + ")";
        String raiz = switch (n) {
            case 2 -> "sqrt(" + textoOtroLado + ")";
            case 3 -> "cbrt(" + textoOtroLado + ")";
            default -> radicando + "^(1/" + n + ")";
        };

        String baseTexto = escribirLineal(a, b, objetivo);
        List<String> pasos = new ArrayList<>();
        pasos.add(ecuacion);
        List<Rama> ramas = new ArrayList<>();

        // Si la base es la variable sola, despejar es sacar la raiz y nada mas.
        boolean baseEsLaVariable = Math.abs(a - 1.0) < 1e-12 && Math.abs(b) < 1e-12;

        if (n % 2 == 1) {
            pasos.add("Se saca la raiz " + (n == 3 ? "cubica" : "de indice " + n)
                    + " a los dos lados:");
            String rama = despejarLineal(a, b, raiz, objetivo);
            if (!baseEsLaVariable) {
                pasos.add("   " + baseTexto + " = " + raiz);
            }
            pasos.add("   " + objetivo + " = " + rama);
            ramas.add(new Rama(rama, "", 0));
        } else {
            pasos.add("Se saca la raiz " + (n == 2 ? "cuadrada" : "de indice " + n)
                    + " a los dos lados; como el exponente es par, hay dos signos:");
            String mas = despejarLineal(a, b, raiz, objetivo);
            String menos = despejarLineal(a, b, "-" + raiz, objetivo);
            if (baseEsLaVariable) {
                pasos.add("   " + objetivo + " = +-" + raiz);
            } else {
                pasos.add("   " + baseTexto + " = +-" + raiz);
                pasos.add("   " + objetivo + " = " + mas + "   o   " + objetivo + " = " + menos);
            }
            boolean mayorConMas = a > 0;
            ramas.add(new Rama(mas, nombreDeRama(objetivo, mayorConMas), +1));
            ramas.add(new Rama(menos, nombreDeRama(objetivo, !mayorConMas), -1));
        }
        return Optional.of(new Despeje(objetivo, otra, ramas, pasos, false));
    }

    /** Escribe a*t + b. */
    private static String escribirLineal(double a, double b, String t) {
        String parteVariable = coeficienteTexto(a) + t;
        if (Math.abs(b) < 1e-12) {
            return parteVariable;
        }
        return parteVariable + (b < 0 ? " - " : " + ") + numeroTexto(Math.abs(b));
    }

    /** Despeja t de a*t + b = valor. */
    private static String despejarLineal(double a, double b, String valor, String t) {
        String sinB;
        if (Math.abs(b) < 1e-12) {
            sinB = valor;
        } else if (valor.startsWith("-")) {
            sinB = numeroTexto(-b) + " - " + valor.substring(1);
        } else {
            sinB = numeroTexto(-b) + " + " + valor;
        }
        if (Math.abs(a - 1.0) < 1e-12) {
            return sinB;
        }
        if (Math.abs(a + 1.0) < 1e-12) {
            return "-(" + sinB + ")";
        }
        return "(" + sinB + ")/" + numeroTexto(a);
    }

    // ------------------------------------------------------------------
    // POR GRADOS
    // ------------------------------------------------------------------

    private static Optional<Despeje> despejarPorGrados(PolinomioDosVariables todo, String t,
                                                       String ecuacion) {
        String u = t.equals("x") ? "y" : "x";
        int grado = todo.gradoEn(t);
        List<Polinomio> c = todo.coeficientesEn(t);
        List<String> pasos = new ArrayList<>();
        pasos.add(ecuacion);

        // ---------- Grado 1: a(u)*t + b(u) = 0 ----------
        if (grado == 1) {
            Polinomio a = c.get(1);
            Polinomio b = c.get(0);
            if (a.esConstante()) {
                double divisor = a.coeficiente(0);
                Polinomio resultado = b.por(-1.0 / divisor);
                String despejada = textoPolinomio(resultado, u);
                // a t = -b; si a es negativo se cambia el signo de los dos lados, que es
                // como se escribe a mano: 8x = y^2 y no -8x = -y^2.
                double signo = divisor < 0 ? -1.0 : 1.0;
                String agrupado = coeficienteTexto(divisor * signo) + t + " = "
                        + textoPolinomio(b.por(-signo), u);
                if (Math.abs(Math.abs(divisor) - 1.0) > 1e-12) {
                    pasos.add("Se deja " + t + " sola en un lado y se divide entre "
                            + numeroTexto(Math.abs(divisor)) + ":");
                    pasos.add("   " + agrupado);
                } else {
                    pasos.add("Se deja " + t + " sola en un lado:");
                }
                pasos.add("   " + t + " = " + despejada);
                return Optional.of(new Despeje(t, u, List.of(new Rama(despejada, "", 0)), pasos, false));
            }
            String despejada = "(" + textoPolinomio(b.por(-1.0), u) + ")/(" + textoPolinomio(a, u) + ")";
            pasos.add("Se saca " + t + " como factor comun y se divide:");
            pasos.add("   " + t + " = " + despejada);
            return Optional.of(new Despeje(t, u, List.of(new Rama(despejada, "", 0)), pasos, false));
        }

        // ---------- Grado 2 ----------
        if (grado == 2) {
            Polinomio a = c.get(2);
            Polinomio b = c.get(1);
            Polinomio cero = c.get(0);
            if (!a.esConstante()) {
                return Optional.empty();
            }
            double coeficiente = a.coeficiente(0);

            if (b.esCero()) {
                // a t^2 + c(u) = 0  ->  t^2 = -c(u)/a
                Polinomio radicando = cero.por(-1.0 / coeficiente);
                if (Math.abs(coeficiente - 1.0) > 1e-12) {
                    pasos.add("Se deja " + t + "^2 sola en un lado:");
                    pasos.add("   " + coeficienteTexto(coeficiente) + t + "^2 = "
                            + textoPolinomio(cero.por(-1.0), u));
                }
                pasos.add("   " + t + "^2 = " + textoPolinomio(radicando, u));
                RaizTexto raiz = RaizTexto.de(radicando, u);
                pasos.add("Se saca la raiz cuadrada; como el exponente es par, hay dos signos:");
                pasos.add("   " + t + " = +-" + raiz.texto());
                return Optional.of(new Despeje(t, u, List.of(
                        new Rama(raiz.texto(), nombreDeRama(t, true), +1),
                        new Rama("-" + raiz.texto(), nombreDeRama(t, false), -1)), pasos, false));
            }

            // a t^2 + b(u) t + c(u) = 0: se completa el cuadrado.
            Polinomio centro = b.por(-1.0 / (2.0 * coeficiente));
            Polinomio radicando = b.alCuadrado().menos(cero.por(4.0 * coeficiente))
                    .por(1.0 / (4.0 * coeficiente * coeficiente));
            RaizTexto raiz = RaizTexto.de(radicando, u);
            String textoCentro = textoPolinomio(centro, u);
            boolean centroCero = centro.esCero();

            String mas = centroCero ? raiz.texto() : textoCentro + " + " + raiz.texto();
            String menos = centroCero ? "-" + raiz.texto() : textoCentro + " - " + raiz.texto();

            pasos.add("Es de segundo grado en " + t + ". Completando el cuadrado (o con la formula "
                    + "general):");
            pasos.add("   " + t + " = " + (centroCero ? "" : textoCentro + " ") + "+- " + raiz.texto());
            return Optional.of(new Despeje(t, u, List.of(
                    new Rama(mas, nombreDeRama(t, true), +1),
                    new Rama(menos, nombreDeRama(t, false), -1)), pasos, false));
        }

        // ---------- Potencia pura: a t^n + c(u) = 0 ----------
        if (grado >= 3) {
            for (int k = 1; k < grado; k++) {
                if (!c.get(k).esCero()) {
                    return Optional.empty();
                }
            }
            Polinomio a = c.get(grado);
            if (!a.esConstante()) {
                return Optional.empty();
            }
            Polinomio radicando = c.get(0).por(-1.0 / a.coeficiente(0));
            String textoRadicando = textoPolinomio(radicando, u);
            pasos.add("   " + t + "^" + grado + " = " + textoRadicando);
            if (grado == 3) {
                String rama = "cbrt(" + textoRadicando + ")";
                pasos.add("Se saca la raiz cubica:");
                pasos.add("   " + t + " = " + rama);
                return Optional.of(new Despeje(t, u, List.of(new Rama(rama, "", 0)), pasos, false));
            }
            String rama = "(" + textoRadicando + ")^(1/" + grado + ")";
            if (grado % 2 == 1) {
                pasos.add("   " + t + " = " + rama);
                return Optional.of(new Despeje(t, u, List.of(new Rama(rama, "", 0)), pasos, false));
            }
            pasos.add("   " + t + " = +-" + rama);
            return Optional.of(new Despeje(t, u, List.of(
                    new Rama(rama, nombreDeRama(t, true), +1),
                    new Rama("-" + rama, nombreDeRama(t, false), -1)), pasos, false));
        }

        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // COMPROBACION
    // ------------------------------------------------------------------

    /**
     * Sustituye cada rama en la ecuacion original y comprueba que se cumple.
     *
     * <p>Basta con que se cumpla en los puntos donde la rama esta definida. Un punto en
     * que no se cumpla delataria un despeje mal hecho, y entonces se prefiere no despejar
     * a entregar una curva que no es la del enunciado.</p>
     */
    private static boolean verificar(Despeje despeje, NodoExpresion izquierda, NodoExpresion derecha) {
        String t = despeje.variable();
        for (Rama rama : despeje.ramas()) {
            EvaluadorExpresion funcion;
            try {
                funcion = EvaluadorExpresion.compilar(rama.expresion(), despeje.otraVariable());
            } catch (RuntimeException e) {
                return false;
            }
            int comprobados = 0;
            for (int i = -40; i <= 40; i++) {
                double u = i * 0.37 + 0.011;
                double valor = funcion.evaluar(u);
                if (!Double.isFinite(valor)) {
                    continue;
                }
                double x = t.equals("x") ? valor : u;
                double y = t.equals("y") ? valor : u;
                double ladoA = evaluar(izquierda, x, y);
                double ladoB = evaluar(derecha, x, y);
                if (!Double.isFinite(ladoA) || !Double.isFinite(ladoB)) {
                    continue;
                }
                double escala = Math.max(1.0, Math.max(Math.abs(ladoA), Math.abs(ladoB)));
                if (Math.abs(ladoA - ladoB) > 1e-6 * escala) {
                    return false;
                }
                comprobados++;
            }
            if (comprobados == 0) {
                return false;
            }
        }
        return true;
    }

    /** Evalua una expresion de dos variables. */
    static double evaluar(NodoExpresion nodo, double x, double y) {
        return switch (nodo) {
            case NodoExpresion.Numero numero -> numero.valor();
            case NodoExpresion.Constante constante -> constante.valor();
            case NodoExpresion.Variable variable -> variable.nombre().equals("y") ? y : x;
            case NodoExpresion.Negacion negacion -> -evaluar(negacion.operando(), x, y);
            case NodoExpresion.LlamadaFuncion llamada -> {
                DoubleUnaryOperator funcion = CatalogoFunciones.obtenerFuncion(llamada.nombre());
                yield funcion == null ? Double.NaN
                        : funcion.applyAsDouble(evaluar(llamada.argumento(), x, y));
            }
            case NodoExpresion.OperacionBinaria operacion -> new NodoExpresion.OperacionBinaria(
                    operacion.simbolo(),
                    new NodoExpresion.Numero(evaluar(operacion.izquierda(), x, y)),
                    new NodoExpresion.Numero(evaluar(operacion.derecha(), x, y))).evaluar(0);
        };
    }

    /** Indica si la variable aparece en la expresion. */
    public static boolean contiene(NodoExpresion nodo, String variable) {
        return switch (nodo) {
            case NodoExpresion.Numero ignorado -> false;
            case NodoExpresion.Constante ignorado -> false;
            case NodoExpresion.Variable v -> v.nombre().equals(variable);
            case NodoExpresion.Negacion negacion -> contiene(negacion.operando(), variable);
            case NodoExpresion.LlamadaFuncion llamada -> contiene(llamada.argumento(), variable);
            case NodoExpresion.OperacionBinaria operacion ->
                    contiene(operacion.izquierda(), variable) || contiene(operacion.derecha(), variable);
        };
    }

    private static boolean esLaVariable(NodoExpresion nodo, String variable) {
        return nodo instanceof NodoExpresion.Variable v && v.nombre().equals(variable);
    }

    // ------------------------------------------------------------------
    // ESCRITURA
    // ------------------------------------------------------------------

    /** Nombre de la rama segun la variable despejada y el signo. */
    private static String nombreDeRama(String variable, boolean mayor) {
        if (variable.equals("y")) {
            return mayor ? "rama de arriba" : "rama de abajo";
        }
        return mayor ? "rama derecha" : "rama izquierda";
    }

    /**
     * Escribe un polinomio como se leeria en un libro, con fracciones exactas.
     *
     * @param polinomio el polinomio
     * @param variable  su variable
     * @return el texto, listo tambien para el parser
     */
    public static String textoPolinomio(Polinomio polinomio, String variable) {
        return PolinomioRacional.desde(polinomio)
                .map(exacto -> exacto.textoParaLeer(variable))
                .orElseGet(() -> polinomio.aTexto(variable));
    }

    /** Un numero escrito como fraccion si lo es. */
    private static String numeroTexto(double valor) {
        return Racional.desdeDouble(valor).map(Racional::texto)
                .orElseGet(() -> com.calculo2.integrales.math.util.Redondeo.texto(valor));
    }

    /** Coeficiente delante de una variable: se omite el 1 y el -1 queda como signo. */
    private static String coeficienteTexto(double valor) {
        if (Math.abs(valor - 1.0) < 1e-12) {
            return "";
        }
        if (Math.abs(valor + 1.0) < 1e-12) {
            return "-";
        }
        Optional<Racional> exacto = Racional.desdeDouble(valor);
        if (exacto.isPresent() && !exacto.get().esEntero()) {
            return "(" + exacto.get().texto() + ")";
        }
        return numeroTexto(valor);
    }

    /**
     * La raiz cuadrada de un polinomio, sacando lo que se pueda fuera.
     *
     * <p>{@code sqrt((36 - 4x^2)/9)} se escribe {@code (2/3)sqrt(9 - x^2)}, que es como lo
     * escribiria el profesor. Solo se saca el factor si su raiz es exacta: {@code sqrt(8x)}
     * se deja asi, porque partirlo en {@code 2sqrt(2)sqrt(x)} no ayuda a reconocer la
     * parabola del enunciado.</p>
     */
    private record RaizTexto(String texto) {

        static RaizTexto de(Polinomio radicando, String variable) {
            Optional<PolinomioRacional> exacto = PolinomioRacional.desde(radicando);
            if (exacto.isPresent() && !exacto.get().esCero()) {
                Racional contenido = exacto.get().contenido();
                Racional magnitud = contenido.signo() < 0 ? contenido.negar() : contenido;
                Optional<Racional> fuera = magnitud.raizCuadrada();
                if (fuera.isPresent() && !fuera.get().equals(Racional.UNO)) {
                    PolinomioRacional dentro = exacto.get().escalar(magnitud.inverso());
                    if (dentro.grado() == 0) {
                        return new RaizTexto(fuera.get().texto());
                    }
                    String coeficiente = fuera.get().esEntero()
                            ? fuera.get().texto()
                            : "(" + fuera.get().texto() + ")";
                    return new RaizTexto(coeficiente + "sqrt(" + dentro.textoParaLeer(variable) + ")");
                }
                if (exacto.get().grado() == 0) {
                    Racional valor = exacto.get().coeficiente(0);
                    Optional<Racional> raiz = valor.raizCuadrada();
                    if (raiz.isPresent()) {
                        return new RaizTexto(raiz.get().texto());
                    }
                }
                return new RaizTexto("sqrt(" + exacto.get().textoParaLeer(variable) + ")");
            }
            return new RaizTexto("sqrt(" + radicando.aTexto(variable) + ")");
        }
    }
}
