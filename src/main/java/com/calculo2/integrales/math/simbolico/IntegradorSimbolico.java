package com.calculo2.integrales.math.simbolico;

import com.calculo2.integrales.math.parser.NodoExpresion;
import com.calculo2.integrales.math.util.Redondeo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Calcula la antiderivada de una expresion, cuando se puede.
 *
 * <p>Es la pieza que permite resolver como en clase: encontrar {@code F(x)} y aplicar la
 * regla de Barrow, {@code F(b) - F(a)}. Un metodo numerico devuelve el mismo numero, pero
 * no sirve para estudiar, porque no hay nada que copiar al cuaderno.</p>
 *
 * <p>No es un integrador general — eso seria un proyecto entero —, sino uno que cubre lo
 * que aparece en el curso:</p>
 *
 * <ul>
 *   <li>Polinomios completos, desarrollando productos y potencias.</li>
 *   <li>La regla de la potencia con cualquier exponente constante, incluidas raices como
 *       {@code sqrt(x) = x^(1/2)} y potencias negativas.</li>
 *   <li>El caso {@code 1/x}, cuya primitiva es {@code ln|x|} y no sale de la regla de la
 *       potencia.</li>
 *   <li>Las funciones del catalogo cuyo argumento es lineal, ajustando por la constante
 *       que aparece al deshacer la regla de la cadena.</li>
 * </ul>
 *
 * <p>Cuando la expresion se sale de ahi devuelve un {@link Optional} vacio. Esa respuesta
 * es deliberada: es preferible decir que no se supo integrar y pasar al calculo
 * aproximado, a inventar una primitiva equivocada.</p>
 */
public final class IntegradorSimbolico {

    private IntegradorSimbolico() {
    }

    /** Margen para decidir si un numero es entero o si dos coinciden. */
    private static final double EPSILON = 1e-9;

    /**
     * Una antiderivada ya calculada.
     *
     * @param expresion la primitiva como arbol, lista para evaluarse
     * @param texto     la primitiva escrita para mostrarla
     * @param reglas    las reglas de integracion que se usaron, para explicarlas
     */
    public record Antiderivada(NodoExpresion expresion, String texto, List<String> reglas) {

        /**
         * Evalua la primitiva en un punto.
         *
         * @param valor valor de la variable
         * @return el valor de {@code F} en ese punto
         */
        public double evaluar(double valor) {
            return expresion.evaluar(valor);
        }

        /**
         * Aplica la regla de Barrow.
         *
         * @param a limite inferior
         * @param b limite superior
         * @return {@code F(b) - F(a)}
         */
        public double integralDefinida(double a, double b) {
            return evaluar(b) - evaluar(a);
        }
    }

    /**
     * Calcula la antiderivada de la expresion.
     *
     * @param expresion expresion a integrar
     * @param variable  nombre de la variable de integracion
     * @return la primitiva, o vacio si no se supo integrar
     */
    public static Optional<Antiderivada> integrar(NodoExpresion expresion, String variable) {
        if (expresion == null) {
            return Optional.empty();
        }

        // Primero se prueba el camino polinomico: cuando aplica, da el texto mas limpio
        // porque el polinomio ya viene desarrollado y ordenado por grados.
        Optional<Polinomio> polinomio = ConversorPolinomio.convertir(expresion);
        if (polinomio.isPresent()) {
            Polinomio primitiva = polinomio.get().integral();
            return Optional.of(new Antiderivada(
                    aNodo(primitiva, variable),
                    primitiva.aTexto(variable),
                    List.of(reglaDeLaPotencia(variable))));
        }

        // Despues, las potencias con exponente fraccionario: x*sqrt(x) = x^(3/2) se integra
        // con la misma regla de la potencia en cuanto la raiz se escribe como exponente.
        Optional<SumaDePotencias> potencias = SumaDePotencias.desde(expresion);
        if (potencias.isPresent() && !potencias.get().esVacia()) {
            SumaDePotencias suma = potencias.get();
            SumaDePotencias.Primitiva primitiva = suma.integrar(variable);
            List<String> reglasDePotencias = new ArrayList<>();
            if (suma.tieneExponentesFraccionarios()) {
                reglasDePotencias.add("Cada raiz se escribe como una potencia de exponente "
                        + "fraccionario, por ejemplo sqrt(" + variable + ") = " + variable
                        + "^(1/2), y el integrando queda como " + suma.texto(variable) + ".");
            }
            if (!suma.soloExponenteMenosUno()) {
                reglasDePotencias.add(reglaDeLaPotencia(variable));
            }
            if (suma.tieneExponenteMenosUno()) {
                reglasDePotencias.add("La integral de 1/" + variable + " es ln|" + variable
                        + "|, porque la regla de la potencia no sirve cuando el exponente es -1: "
                        + "sumarle uno daria cero en el denominador.");
            }
            return Optional.of(new Antiderivada(primitiva.primitiva(), primitiva.texto(),
                    reglasDePotencias));
        }

        Set<String> reglas = new LinkedHashSet<>();
        Optional<NodoExpresion> primitiva = integrarNodo(expresion, variable, reglas);

        return primitiva.map(nodo -> new Antiderivada(
                nodo,
                FormateadorMatematico.escribir(nodo),
                new ArrayList<>(reglas)));
    }

    // ------------------------------------------------------------------
    // REGLAS DE INTEGRACION
    // ------------------------------------------------------------------

    /**
     * Integra un nodo aplicando la regla que corresponda a su forma.
     *
     * @param expresion nodo a integrar
     * @param variable  nombre de la variable
     * @param reglas    se van anotando aqui las reglas usadas, para explicarlas despues
     * @return la primitiva, o vacio si ninguna regla aplica
     */
    private static Optional<NodoExpresion> integrarNodo(NodoExpresion expresion, String variable,
                                                        Set<String> reglas) {
        // Una expresion sin la variable es una constante respecto a ella, aunque por
        // dentro tenga operaciones: la integral de 3*pi es 3*pi*x.
        if (esConstante(expresion)) {
            reglas.add("La integral de una constante es la constante por " + variable + ".");
            return Optional.of(producto(expresion, new NodoExpresion.Variable(variable)));
        }

        return switch (expresion) {
            case NodoExpresion.Variable ignorada -> {
                reglas.add(reglaDeLaPotencia(variable));
                yield Optional.of(division(
                        potencia(new NodoExpresion.Variable(variable), numero(2)),
                        numero(2)));
            }

            case NodoExpresion.Negacion negacion ->
                    integrarNodo(negacion.operando(), variable, reglas)
                            .map(NodoExpresion.Negacion::new);

            case NodoExpresion.OperacionBinaria operacion ->
                    integrarOperacion(operacion, variable, reglas);

            case NodoExpresion.LlamadaFuncion llamada ->
                    integrarFuncion(llamada, variable, reglas);

            default -> Optional.empty();
        };
    }

    /** Integra una suma, una resta, un producto por constante, un cociente o una potencia. */
    private static Optional<NodoExpresion> integrarOperacion(NodoExpresion.OperacionBinaria operacion,
                                                             String variable, Set<String> reglas) {
        NodoExpresion izquierda = operacion.izquierda();
        NodoExpresion derecha = operacion.derecha();

        switch (operacion.simbolo()) {
            case "+", "-" -> {
                // La integral de una suma es la suma de las integrales.
                reglas.add("La integral de una suma es la suma de las integrales de cada termino.");

                Optional<NodoExpresion> primeraParte = integrarNodo(izquierda, variable, reglas);
                Optional<NodoExpresion> segundaParte = integrarNodo(derecha, variable, reglas);

                if (primeraParte.isEmpty() || segundaParte.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(new NodoExpresion.OperacionBinaria(
                        operacion.simbolo(), primeraParte.get(), segundaParte.get()));
            }

            case "*" -> {
                // Solo se sabe resolver cuando uno de los factores es constante; un
                // producto de dos funciones de x necesitaria integracion por partes.
                if (esConstante(izquierda)) {
                    reglas.add("Las constantes que multiplican salen fuera de la integral.");
                    return integrarNodo(derecha, variable, reglas)
                            .map(primitiva -> producto(izquierda, primitiva));
                }
                if (esConstante(derecha)) {
                    reglas.add("Las constantes que multiplican salen fuera de la integral.");
                    return integrarNodo(izquierda, variable, reglas)
                            .map(primitiva -> producto(primitiva, derecha));
                }
                return Optional.empty();
            }

            case "/" -> {
                // Dividir entre una constante es multiplicar por su inverso.
                if (esConstante(derecha)) {
                    reglas.add("Las constantes que dividen salen fuera de la integral.");
                    return integrarNodo(izquierda, variable, reglas)
                            .map(primitiva -> division(primitiva, derecha));
                }
                // El caso c/(ax+b) es un logaritmo; se reescribe como potencia -1.
                if (esConstante(izquierda)) {
                    NodoExpresion comoPotencia = potencia(derecha, numero(-1));
                    return integrarNodo(comoPotencia, variable, reglas)
                            .map(primitiva -> producto(izquierda, primitiva));
                }
                return Optional.empty();
            }

            case "^" -> {
                return integrarPotencia(izquierda, derecha, variable, reglas);
            }

            default -> {
                return Optional.empty();
            }
        }
    }

    /**
     * Integra una potencia cuya base es lineal y cuyo exponente es constante.
     *
     * <p>Para {@code (ax+b)^n} la primitiva es {@code (ax+b)^(n+1) / (a(n+1))}. El
     * divisor {@code a} aparece al deshacer la regla de la cadena: al derivar el
     * resultado, la derivada interna vuelve a sacar ese {@code a}.</p>
     *
     * <p>El exponente {@code -1} se trata aparte porque sumarle uno daria cero en el
     * denominador; ese caso es el logaritmo.</p>
     */
    private static Optional<NodoExpresion> integrarPotencia(NodoExpresion base, NodoExpresion exponente,
                                                            String variable, Set<String> reglas) {
        if (!esConstante(exponente)) {
            return Optional.empty();
        }

        Optional<double[]> lineal = comoLineal(base);
        if (lineal.isEmpty()) {
            return Optional.empty();
        }

        double pendiente = lineal.get()[0];
        if (Math.abs(pendiente) < EPSILON) {
            return Optional.empty();
        }

        double valorExponente = exponente.evaluar(0);

        if (Math.abs(valorExponente + 1.0) < EPSILON) {
            reglas.add("La integral de 1/u es ln|u|, porque la regla de la potencia no sirve "
                    + "cuando el exponente es -1: sumarle uno daria cero en el denominador.");

            NodoExpresion logaritmo = new NodoExpresion.LlamadaFuncion(
                    "ln", new NodoExpresion.LlamadaFuncion("abs", base));
            return Optional.of(dividirEntre(logaritmo, pendiente, reglas, base, variable));
        }

        reglas.add(reglaDeLaPotencia(variable));

        NodoExpresion elevada = potencia(base, numero(valorExponente + 1.0));
        NodoExpresion resultado = division(elevada, numero(valorExponente + 1.0));

        return Optional.of(dividirEntre(resultado, pendiente, reglas, base, variable));
    }

    /**
     * Integra una funcion del catalogo cuyo argumento sea lineal.
     *
     * <p>Se usan las primitivas conocidas de memoria, divididas por la pendiente del
     * argumento cuando ese argumento no es simplemente la variable.</p>
     */
    private static Optional<NodoExpresion> integrarFuncion(NodoExpresion.LlamadaFuncion llamada,
                                                           String variable, Set<String> reglas) {
        NodoExpresion argumento = llamada.argumento();

        Optional<double[]> lineal = comoLineal(argumento);
        if (lineal.isEmpty()) {
            return Optional.empty();
        }

        double pendiente = lineal.get()[0];
        if (Math.abs(pendiente) < EPSILON) {
            return Optional.empty();
        }

        Optional<NodoExpresion> primitiva = primitivaConocida(llamada.nombre(), argumento, reglas);
        return primitiva.map(nodo -> dividirEntre(nodo, pendiente, reglas, argumento, variable));
    }

    /**
     * Devuelve la primitiva de memoria de una funcion del catalogo.
     *
     * @param nombre    nombre de la funcion
     * @param argumento su argumento, ya comprobado como lineal
     * @param reglas    donde anotar la regla usada
     * @return la primitiva, o vacio si esa funcion no esta en la tabla
     */
    private static Optional<NodoExpresion> primitivaConocida(String nombre, NodoExpresion argumento,
                                                             Set<String> reglas) {
        switch (nombre) {
            case "sin", "sen" -> {
                reglas.add("La integral de sin(u) es -cos(u).");
                return Optional.of(new NodoExpresion.Negacion(
                        new NodoExpresion.LlamadaFuncion("cos", argumento)));
            }
            case "cos" -> {
                reglas.add("La integral de cos(u) es sin(u).");
                return Optional.of(new NodoExpresion.LlamadaFuncion("sin", argumento));
            }
            case "exp" -> {
                reglas.add("La integral de e^u es e^u: la exponencial es su propia primitiva.");
                return Optional.of(new NodoExpresion.LlamadaFuncion("exp", argumento));
            }
            case "sqrt", "raiz" -> {
                reglas.add("Una raiz cuadrada es una potencia de exponente 1/2, asi que se "
                        + "aplica la regla de la potencia: el exponente pasa a 3/2.");
                NodoExpresion elevada = potencia(argumento, numero(1.5));
                return Optional.of(producto(numero(2.0 / 3.0), elevada));
            }
            case "tan" -> {
                reglas.add("La integral de tan(u) es -ln|cos(u)|.");
                return Optional.of(new NodoExpresion.Negacion(
                        new NodoExpresion.LlamadaFuncion("ln",
                                new NodoExpresion.LlamadaFuncion("abs",
                                        new NodoExpresion.LlamadaFuncion("cos", argumento)))));
            }
            case "ln" -> {
                reglas.add("La integral de ln(u) es u*ln(u) - u, que sale de integrar por partes.");
                NodoExpresion logaritmo = new NodoExpresion.LlamadaFuncion("ln", argumento);
                return Optional.of(new NodoExpresion.OperacionBinaria(
                        "-", producto(argumento, logaritmo), argumento));
            }
            case "sinh" -> {
                reglas.add("La integral de sinh(u) es cosh(u).");
                return Optional.of(new NodoExpresion.LlamadaFuncion("cosh", argumento));
            }
            case "cosh" -> {
                reglas.add("La integral de cosh(u) es sinh(u).");
                return Optional.of(new NodoExpresion.LlamadaFuncion("sinh", argumento));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    // ------------------------------------------------------------------
    // APOYO
    // ------------------------------------------------------------------

    /**
     * Divide la primitiva entre la pendiente del argumento, si hace falta.
     *
     * <p>Cuando el argumento es la variable sola, la pendiente vale uno y no hay nada que
     * ajustar. En cualquier otro caso hay que dividir, y conviene decir por que.</p>
     */
    private static NodoExpresion dividirEntre(NodoExpresion primitiva, double pendiente,
                                              Set<String> reglas, NodoExpresion argumento,
                                              String variable) {
        if (Math.abs(pendiente - 1.0) < EPSILON) {
            return primitiva;
        }

        reglas.add("Como dentro de la funcion no esta " + variable + " sola sino "
                + FormateadorMatematico.escribir(argumento) + ", hay que dividir entre "
                + Redondeo.texto(pendiente) + ": es lo que aparece al deshacer la regla de la cadena.");

        return division(primitiva, numero(pendiente));
    }

    /**
     * Reconoce una expresion de la forma {@code a*variable + b}.
     *
     * @param expresion expresion a examinar
     * @return un arreglo con la pendiente y el termino independiente, o vacio si no es lineal
     */
    private static Optional<double[]> comoLineal(NodoExpresion expresion) {
        Optional<Polinomio> polinomio = ConversorPolinomio.convertir(expresion);

        if (polinomio.isEmpty() || polinomio.get().grado() > 1) {
            return Optional.empty();
        }

        Polinomio lineal = polinomio.get();
        return Optional.of(new double[]{lineal.coeficiente(1), lineal.coeficiente(0)});
    }

    /**
     * Indica si la expresion no depende de la variable.
     *
     * <p>Se comprueba sobre la estructura y no evaluando en dos puntos: una funcion puede
     * coincidir por casualidad en los puntos que se prueben sin ser constante.</p>
     */
    private static boolean esConstante(NodoExpresion expresion) {
        return switch (expresion) {
            case NodoExpresion.Numero ignorada -> true;
            case NodoExpresion.Constante ignorada -> true;
            case NodoExpresion.Variable ignorada -> false;
            case NodoExpresion.Negacion negacion -> esConstante(negacion.operando());
            case NodoExpresion.LlamadaFuncion llamada -> esConstante(llamada.argumento());
            case NodoExpresion.OperacionBinaria operacion ->
                    esConstante(operacion.izquierda()) && esConstante(operacion.derecha());
        };
    }

    /** Convierte un polinomio en arbol, para poder evaluarlo con el resto del sistema. */
    private static NodoExpresion aNodo(Polinomio polinomio, String variable) {
        NodoExpresion resultado = numero(polinomio.coeficiente(0));

        for (int grado = 1; grado <= polinomio.grado(); grado++) {
            double coeficiente = polinomio.coeficiente(grado);
            if (Math.abs(coeficiente) < 1e-14) {
                continue;
            }

            NodoExpresion termino = producto(
                    numero(coeficiente),
                    potencia(new NodoExpresion.Variable(variable), numero(grado)));

            resultado = new NodoExpresion.OperacionBinaria("+", resultado, termino);
        }
        return resultado;
    }

    private static String reglaDeLaPotencia(String variable) {
        return "Regla de la potencia: la integral de " + variable + "^n es "
                + variable + "^(n+1)/(n+1), valida para todo n distinto de -1.";
    }

    private static NodoExpresion numero(double valor) {
        return new NodoExpresion.Numero(valor);
    }

    private static NodoExpresion producto(NodoExpresion izquierda, NodoExpresion derecha) {
        return new NodoExpresion.OperacionBinaria("*", izquierda, derecha);
    }

    private static NodoExpresion division(NodoExpresion izquierda, NodoExpresion derecha) {
        return new NodoExpresion.OperacionBinaria("/", izquierda, derecha);
    }

    private static NodoExpresion potencia(NodoExpresion base, NodoExpresion exponente) {
        return new NodoExpresion.OperacionBinaria("^", base, exponente);
    }
}
