package com.calculo2.integrales.math.algebra;

import com.calculo2.integrales.math.parser.NodoExpresion;
import com.calculo2.integrales.math.simbolico.Polinomio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Un polinomio en x e y, como {@code 4x^2 + 9y^2 - 36}.
 *
 * <p>Las curvas que el enunciado no da despejadas son ecuaciones en las dos variables:
 * la elipse {@code 4x^2 + 9y^2 = 36}, la parabola {@code y^2 = 8x}. Para despejar una
 * de ellas hay que ver la ecuacion como un polinomio en esa variable, con coeficientes
 * que dependen de la otra: {@code 9 y^2 + (4x^2 - 36) = 0} es de segundo grado en y.
 * Esta clase guarda los terminos y hace esa agrupacion.</p>
 */
public final class PolinomioDosVariables {

    /** Grado maximo que se acepta en cada variable. */
    private static final int GRADO_MAXIMO = 12;

    /** Clave de x^i y^j. */
    private static int clave(int i, int j) {
        return i * 64 + j;
    }

    /** Coeficiente de cada termino x^i y^j, indexado por {@link #clave(int, int)}. */
    private final Map<Integer, Double> terminos;

    private PolinomioDosVariables(Map<Integer, Double> terminos) {
        Map<Integer, Double> limpios = new TreeMap<>();
        for (Map.Entry<Integer, Double> termino : terminos.entrySet()) {
            if (Math.abs(termino.getValue()) > 1e-12) {
                limpios.put(termino.getKey(), termino.getValue());
            }
        }
        this.terminos = limpios;
    }

    /** Una constante. */
    public static PolinomioDosVariables constante(double valor) {
        return new PolinomioDosVariables(Map.of(clave(0, 0), valor));
    }

    /** La variable x o la variable y. */
    public static PolinomioDosVariables variable(String nombre) {
        return new PolinomioDosVariables(Map.of(nombre.equals("y") ? clave(0, 1) : clave(1, 0), 1.0));
    }

    // ------------------------------------------------------------------
    // CONVERSION DESDE EL ARBOL
    // ------------------------------------------------------------------

    /**
     * Convierte una expresion en x e y.
     *
     * @param nodo la expresion
     * @return el polinomio, o vacio si la expresion no es polinomica
     */
    public static Optional<PolinomioDosVariables> desde(NodoExpresion nodo) {
        return switch (nodo) {
            case NodoExpresion.Numero numero -> Optional.of(constante(numero.valor()));
            case NodoExpresion.Constante constante -> Optional.of(constante(constante.valor()));
            case NodoExpresion.Variable variable -> {
                String nombre = variable.nombre();
                yield (nombre.equals("x") || nombre.equals("y"))
                        ? Optional.of(variable(nombre))
                        : Optional.empty();
            }
            case NodoExpresion.Negacion negacion ->
                    desde(negacion.operando()).map(p -> p.escalar(-1.0));
            case NodoExpresion.LlamadaFuncion ignorada -> Optional.empty();
            case NodoExpresion.OperacionBinaria operacion -> desdeOperacion(operacion);
        };
    }

    private static Optional<PolinomioDosVariables> desdeOperacion(NodoExpresion.OperacionBinaria operacion) {
        Optional<PolinomioDosVariables> izquierda = desde(operacion.izquierda());
        Optional<PolinomioDosVariables> derecha = desde(operacion.derecha());
        if (izquierda.isEmpty() || derecha.isEmpty()) {
            return Optional.empty();
        }
        PolinomioDosVariables a = izquierda.get();
        PolinomioDosVariables b = derecha.get();

        return switch (operacion.simbolo()) {
            case "+" -> Optional.of(a.sumar(b));
            case "-" -> Optional.of(a.restar(b));
            case "*" -> Optional.of(a.multiplicar(b));
            case "/" -> b.esConstante() && Math.abs(b.valorConstante()) > 1e-15
                    ? Optional.of(a.escalar(1.0 / b.valorConstante()))
                    : Optional.empty();
            case "^" -> {
                if (!b.esConstante()) {
                    yield Optional.empty();
                }
                double exponente = b.valorConstante();
                if (exponente < 0 || Math.abs(exponente - Math.rint(exponente)) > 1e-9
                        || exponente > GRADO_MAXIMO) {
                    yield Optional.empty();
                }
                yield Optional.of(a.elevar((int) Math.rint(exponente)));
            }
            default -> Optional.empty();
        };
    }

    // ------------------------------------------------------------------
    // ALGEBRA
    // ------------------------------------------------------------------

    public PolinomioDosVariables sumar(PolinomioDosVariables otro) {
        Map<Integer, Double> resultado = new TreeMap<>(terminos);
        otro.terminos.forEach((k, v) -> resultado.merge(k, v, Double::sum));
        return new PolinomioDosVariables(resultado);
    }

    public PolinomioDosVariables restar(PolinomioDosVariables otro) {
        return sumar(otro.escalar(-1.0));
    }

    public PolinomioDosVariables escalar(double factor) {
        Map<Integer, Double> resultado = new TreeMap<>();
        terminos.forEach((k, v) -> resultado.put(k, v * factor));
        return new PolinomioDosVariables(resultado);
    }

    public PolinomioDosVariables multiplicar(PolinomioDosVariables otro) {
        Map<Integer, Double> resultado = new TreeMap<>();
        for (Map.Entry<Integer, Double> a : terminos.entrySet()) {
            for (Map.Entry<Integer, Double> b : otro.terminos.entrySet()) {
                int i = a.getKey() / 64 + b.getKey() / 64;
                int j = a.getKey() % 64 + b.getKey() % 64;
                resultado.merge(clave(i, j), a.getValue() * b.getValue(), Double::sum);
            }
        }
        return new PolinomioDosVariables(resultado);
    }

    public PolinomioDosVariables elevar(int exponente) {
        PolinomioDosVariables resultado = constante(1.0);
        for (int i = 0; i < exponente; i++) {
            resultado = resultado.multiplicar(this);
        }
        return resultado;
    }

    // ------------------------------------------------------------------
    // CONSULTA
    // ------------------------------------------------------------------

    public boolean esConstante() {
        return terminos.isEmpty() || (terminos.size() == 1 && terminos.containsKey(clave(0, 0)));
    }

    public double valorConstante() {
        return terminos.getOrDefault(clave(0, 0), 0.0);
    }

    public boolean esCero() {
        return terminos.isEmpty();
    }

    /** El grado en una de las dos variables. */
    public int gradoEn(String variable) {
        int grado = 0;
        for (int k : terminos.keySet()) {
            grado = Math.max(grado, variable.equals("y") ? k % 64 : k / 64);
        }
        return grado;
    }

    /**
     * Agrupa por potencias de una variable.
     *
     * <p>Para {@code 9y^2 + 4x^2 - 36} agrupado en y devuelve
     * {@code [4x^2 - 36, 0, 9]}: el coeficiente de y^0, el de y^1 y el de y^2, cada uno
     * como polinomio en la otra variable.</p>
     *
     * @param variable la variable por la que se agrupa
     * @return los coeficientes, del grado cero hacia arriba
     */
    public List<Polinomio> coeficientesEn(String variable) {
        int grado = gradoEn(variable);
        int gradoOtra = gradoEn(variable.equals("y") ? "x" : "y");
        List<double[]> coeficientes = new ArrayList<>();
        for (int k = 0; k <= grado; k++) {
            coeficientes.add(new double[gradoOtra + 1]);
        }
        for (Map.Entry<Integer, Double> termino : terminos.entrySet()) {
            int i = termino.getKey() / 64;
            int j = termino.getKey() % 64;
            int potencia = variable.equals("y") ? j : i;
            int potenciaOtra = variable.equals("y") ? i : j;
            coeficientes.get(potencia)[potenciaOtra] += termino.getValue();
        }
        List<Polinomio> resultado = new ArrayList<>();
        for (double[] coeficiente : coeficientes) {
            resultado.add(Polinomio.de(coeficiente));
        }
        return resultado;
    }

    /** Evalua el polinomio en un punto. */
    public double evaluar(double x, double y) {
        double suma = 0.0;
        for (Map.Entry<Integer, Double> termino : terminos.entrySet()) {
            suma += termino.getValue()
                    * Math.pow(x, termino.getKey() / 64)
                    * Math.pow(y, termino.getKey() % 64);
        }
        return suma;
    }
}
