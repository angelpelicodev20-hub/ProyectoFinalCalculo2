package com.calculo2.integrales.math.algebra;

import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.math.parser.NodoExpresion;
import com.calculo2.integrales.math.simbolico.ConversorPolinomio;
import com.calculo2.integrales.math.simbolico.FormaRadical;
import com.calculo2.integrales.math.simbolico.Polinomio;
import com.calculo2.integrales.math.util.BuscadorRaices;
import com.calculo2.integrales.math.util.Redondeo;
import com.calculo2.integrales.math.util.ValidadorFuncion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Resuelve una ecuacion de una variable, {@code f(x) = g(x)}, y escribe como lo hizo.
 *
 * <p>Es lo que se hace para encontrar los puntos de interseccion de dos curvas o los
 * cortes de una curva con el eje. Se intenta, por orden:</p>
 *
 * <ol>
 *   <li><b>Polinomica.</b> Se pasa todo a un lado y se factoriza con
 *       {@link Factorizador}.</li>
 *   <li><b>Con una raiz.</b> Si aparece una raiz cuadrada o cubica, como en
 *       {@code x^2 = sqrt(8x)}, se aisla, se elevan los dos lados para quitarla y se
 *       resuelve el polinomio que queda. Despues se comprueba cada solucion en la ecuacion
 *       original, porque elevar al cuadrado puede introducir soluciones falsas.</li>
 *   <li><b>Numerica.</b> Si nada de lo anterior aplica (por ejemplo {@code sin(x) = cos(x)}),
 *       las soluciones se buscan numericamente, y se dice.</li>
 * </ol>
 */
public final class ResolutorEcuaciones {

    private ResolutorEcuaciones() {
    }

    /**
     * La solucion de una ecuacion.
     *
     * @param raices    las soluciones reales encontradas, ordenadas
     * @param pasos     el procedimiento, linea a linea
     * @param exacta    true si las soluciones se obtuvieron de forma exacta
     * @param identicas true si los dos lados son la misma expresion
     */
    public record Solucion(List<Factorizador.Raiz> raices, List<String> pasos, boolean exacta,
                           boolean identicas) {

        /** Los valores de las soluciones. */
        public List<Double> valores() {
            List<Double> valores = new ArrayList<>();
            for (Factorizador.Raiz raiz : raices) {
                valores.add(raiz.valor());
            }
            return valores;
        }

        /** Las soluciones dentro de un intervalo. */
        public Solucion entre(double desde, double hasta) {
            List<Factorizador.Raiz> dentro = new ArrayList<>();
            for (Factorizador.Raiz raiz : raices) {
                if (raiz.valor() >= desde - 1e-9 && raiz.valor() <= hasta + 1e-9) {
                    dentro.add(raiz);
                }
            }
            return new Solucion(dentro, pasos, exacta, identicas);
        }

        /** El texto exacto de la solucion mas cercana a un valor, o el valor redondeado. */
        public String textoDe(double valor) {
            for (Factorizador.Raiz raiz : raices) {
                if (Math.abs(raiz.valor() - valor) < 1e-7) {
                    return raiz.exacta() ? raiz.texto() : Redondeo.texto(valor);
                }
            }
            return Redondeo.texto(valor);
        }
    }

    /**
     * Resuelve {@code izquierda = derecha}.
     *
     * @param izquierda      lado izquierdo, como arbol en la variable
     * @param derecha        lado derecho
     * @param textoIzquierda como se escribe el lado izquierdo
     * @param textoDerecha   como se escribe el lado derecho
     * @param variable       nombre de la variable
     * @param desde          desde donde buscar si hay que hacerlo numericamente
     * @param hasta          hasta donde
     * @return las soluciones y el procedimiento
     */
    public static Solucion resolver(NodoExpresion izquierda, NodoExpresion derecha,
                                    String textoIzquierda, String textoDerecha,
                                    String variable, double desde, double hasta) {
        List<String> pasos = new ArrayList<>();
        pasos.add(textoIzquierda + " = " + textoDerecha);

        // ---------- 1. Polinomica ----------
        Optional<Polinomio> izquierdaPolinomio = ConversorPolinomio.convertir(izquierda);
        Optional<Polinomio> derechaPolinomio = ConversorPolinomio.convertir(derecha);
        if (izquierdaPolinomio.isPresent() && derechaPolinomio.isPresent()) {
            return resolverPolinomio(izquierdaPolinomio.get(), derechaPolinomio.get(),
                    variable, pasos, derechaPolinomio.get().esCero());
        }

        // ---------- 2. Con una raiz ----------
        Optional<FormaRadical> formaIzquierda = FormaRadical.de(izquierda);
        Optional<FormaRadical> formaDerecha = FormaRadical.de(derecha);
        if (formaIzquierda.isPresent() && formaDerecha.isPresent()) {
            Optional<Solucion> conRaiz = resolverConRaiz(formaIzquierda.get(), formaDerecha.get(),
                    izquierda, derecha, variable, pasos);
            if (conRaiz.isPresent()) {
                return conRaiz.get();
            }
        }

        // ---------- 3. Numerica ----------
        FuncionMatematica diferencia = valor -> izquierda.evaluar(valor) - derecha.evaluar(valor);
        List<Double> valores = raicesNumericas(diferencia, desde, hasta);
        List<Factorizador.Raiz> raices = new ArrayList<>();
        for (double valor : valores) {
            raices.add(new Factorizador.Raiz(valor, "≈ " + Redondeo.texto(valor), 1, false));
        }
        pasos.add("La ecuacion no es polinomica, asi que no se puede resolver factorizando. "
                + "Sus soluciones se buscan numericamente (biseccion), entre "
                + Redondeo.texto(desde) + " y " + Redondeo.texto(hasta) + ":");
        pasos.add(raices.isEmpty()
                ? "   No hay soluciones en ese rango."
                : "   " + Factorizador.listar(variable, raices));
        return new Solucion(raices, pasos, false, false);
    }

    // ------------------------------------------------------------------
    // POLINOMICA
    // ------------------------------------------------------------------

    private static Solucion resolverPolinomio(Polinomio izquierda, Polinomio derecha, String variable,
                                              List<String> pasos, boolean yaIgualadaACero) {
        Polinomio todo = izquierda.menos(derecha);
        if (todo.esCero()) {
            pasos.add("Los dos lados son la misma expresion: las curvas coinciden en todos los "
                    + "puntos.");
            return new Solucion(List.of(), pasos, true, true);
        }
        if (!yaIgualadaACero) {
            pasos.add("Se pasa todo al lado izquierdo y se simplifica:");
            pasos.add("   " + Despejador.textoPolinomio(todo, variable) + " = 0");
        }
        Factorizador.Resultado resultado = Factorizador.resolverIgualACero(todo, variable);
        pasos.addAll(resultado.pasos());
        return new Solucion(resultado.raices(), pasos, resultado.exacto(), false);
    }

    // ------------------------------------------------------------------
    // CON UNA RAIZ
    // ------------------------------------------------------------------

    private static Optional<Solucion> resolverConRaiz(FormaRadical izquierda, FormaRadical derecha,
                                                      NodoExpresion nodoIzquierdo,
                                                      NodoExpresion nodoDerecho,
                                                      String variable, List<String> pasos) {
        boolean izquierdaConRaiz = izquierda.tieneRaiz();
        boolean derechaConRaiz = derecha.tieneRaiz();
        if (!izquierdaConRaiz && !derechaConRaiz) {
            return Optional.empty();
        }
        if (izquierdaConRaiz && derechaConRaiz && !izquierda.mismaRaiz(derecha)) {
            return Optional.empty();
        }

        FormaRadical conRaiz = izquierdaConRaiz ? izquierda : derecha;
        int indice = conRaiz.indice();
        Polinomio radicando = conRaiz.radicando();

        // P1 + c1*R = P2 + c2*R  ->  (c1 - c2) R = P2 - P1
        double coeficiente = izquierda.coeficiente() - derecha.coeficiente();
        Polinomio otroLado = derecha.parteSinRaiz().menos(izquierda.parteSinRaiz());

        // La raiz se deja con signo positivo, como se haria a mano: sqrt(8x) = x^2 y no
        // -sqrt(8x) = -x^2.
        if (coeficiente < 0) {
            coeficiente = -coeficiente;
            otroLado = otroLado.por(-1.0);
        }

        if (Math.abs(coeficiente) < 1e-12) {
            // Las raices se cancelan: queda una ecuacion polinomica.
            return Optional.of(resolverPolinomio(izquierda.parteSinRaiz(), derecha.parteSinRaiz(),
                    variable, pasos, false));
        }

        String raizTexto = (indice == 2 ? "sqrt(" : "cbrt(")
                + Despejador.textoPolinomio(radicando, variable) + ")";
        String ladoRaiz = textoCoeficiente(coeficiente) + raizTexto;
        String ladoPolinomio = Despejador.textoPolinomio(otroLado, variable);

        pasos.add("Se deja la raiz sola en un lado:");
        pasos.add("   " + ladoRaiz + " = " + ladoPolinomio);

        // Se elevan los dos lados al indice de la raiz.
        Polinomio izquierdaElevada = radicando.por(Math.pow(coeficiente, indice));
        Polinomio derechaElevada = otroLado.elevado(indice);
        pasos.add(indice == 2
                ? "Se elevan al cuadrado los dos lados para quitar la raiz:"
                : "Se elevan al cubo los dos lados para quitar la raiz:");
        String potencia = indice == 2 ? "^2" : "^3";
        boolean sinCoeficiente = Math.abs(Math.pow(coeficiente, indice) - 1.0) < 1e-12;
        String radicandoTexto = Despejador.textoPolinomio(radicando, variable);
        pasos.add("   " + (sinCoeficiente
                ? radicandoTexto
                : "(" + textoNumero(Math.pow(coeficiente, indice)) + ")(" + radicandoTexto + ")")
                + " = (" + ladoPolinomio + ")" + potencia);

        Polinomio todo = izquierdaElevada.menos(derechaElevada);
        if (todo.esCero()) {
            return Optional.empty();
        }
        pasos.add("Se pasa todo a un lado:");
        pasos.add("   " + Despejador.textoPolinomio(todo, variable) + " = 0");

        Factorizador.Resultado resultado = Factorizador.resolverIgualACero(todo, variable);
        pasos.addAll(resultado.pasos());

        // Al elevar al cuadrado pueden aparecer soluciones que no lo son de la original.
        List<Factorizador.Raiz> validas = new ArrayList<>();
        List<String> descartadas = new ArrayList<>();
        for (Factorizador.Raiz raiz : resultado.raices()) {
            double a = nodoIzquierdo.evaluar(raiz.valor());
            double b = nodoDerecho.evaluar(raiz.valor());
            double escala = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Double.isFinite(a) && Double.isFinite(b) && Math.abs(a - b) < 1e-7 * escala) {
                validas.add(raiz);
            } else {
                descartadas.add(variable + " = " + raiz.texto());
            }
        }

        if (indice == 2) {
            if (descartadas.isEmpty()) {
                pasos.add("Se comprueba cada solucion en la ecuacion original (al elevar al "
                        + "cuadrado podrian aparecer soluciones falsas): todas la cumplen.");
            } else {
                pasos.add("Se comprueba cada solucion en la ecuacion original, porque al elevar "
                        + "al cuadrado pueden aparecer soluciones falsas. No la cumplen, y se "
                        + "descartan: " + String.join(", ", descartadas) + ".");
            }
        }
        if (!validas.isEmpty()) {
            pasos.add("Soluciones validas: " + Factorizador.listar(variable, validas));
        }
        validas.sort(Comparator.comparingDouble(Factorizador.Raiz::valor));
        return Optional.of(new Solucion(validas, pasos, resultado.exacto(), false));
    }

    // ------------------------------------------------------------------
    // NUMERICA
    // ------------------------------------------------------------------

    /**
     * Busca las raices de una funcion, incluidas las que caen justo en el borde de su
     * dominio.
     *
     * <p>{@code sqrt(8x) = -sqrt(8x)} solo se cumple en {@code x = 0}, que es donde
     * empieza el dominio de la raiz: a la izquierda la diferencia no existe y a la derecha
     * es positiva, asi que no hay cambio de signo que detectar. Por eso, ademas de los
     * cambios de signo, se examinan los bordes del dominio.</p>
     *
     * @param funcion la funcion
     * @param desde   inicio de la busqueda
     * @param hasta   fin de la busqueda
     * @return las raices, ordenadas
     */
    public static List<Double> raicesNumericas(FuncionMatematica funcion, double desde, double hasta) {
        List<Double> raices = new ArrayList<>(BuscadorRaices.buscarEnIntervalo(funcion, desde, hasta));

        int muestras = 4000;
        double paso = (hasta - desde) / muestras;
        double anterior = ValidadorFuncion.evaluarSeguro(funcion, desde);
        for (int i = 1; i <= muestras; i++) {
            double x = desde + i * paso;
            double actual = ValidadorFuncion.evaluarSeguro(funcion, x);
            boolean antesDefinida = Double.isFinite(anterior);
            boolean ahoraDefinida = Double.isFinite(actual);
            if (antesDefinida != ahoraDefinida) {
                double borde = bordeDelDominio(funcion, x - paso, x, antesDefinida);
                double valor = ValidadorFuncion.evaluarSeguro(funcion, borde);
                if (Double.isFinite(valor) && Math.abs(valor) < 1e-5) {
                    agregarSiEsNueva(raices, redondearSiCasiEntero(borde), paso);
                }
            }
            anterior = actual;
        }
        raices.sort(Double::compare);
        return raices;
    }

    /** Localiza por biseccion donde la funcion pasa de estar definida a no estarlo. */
    private static double bordeDelDominio(FuncionMatematica funcion, double a, double b,
                                          boolean definidaEnA) {
        double izquierda = a;
        double derecha = b;
        for (int i = 0; i < 80; i++) {
            double medio = (izquierda + derecha) / 2.0;
            boolean definida = Double.isFinite(ValidadorFuncion.evaluarSeguro(funcion, medio));
            if (definida == definidaEnA) {
                izquierda = medio;
            } else {
                derecha = medio;
            }
        }
        return definidaEnA ? izquierda : derecha;
    }

    private static double redondearSiCasiEntero(double valor) {
        double entero = Math.rint(valor);
        return Math.abs(valor - entero) < 1e-9 ? entero : valor;
    }

    private static void agregarSiEsNueva(List<Double> raices, double raiz, double paso) {
        for (double existente : raices) {
            if (Math.abs(existente - raiz) < Math.max(paso, 1e-9)) {
                return;
            }
        }
        raices.add(raiz);
    }

    // ------------------------------------------------------------------
    // ESCRITURA
    // ------------------------------------------------------------------

    private static String textoCoeficiente(double valor) {
        if (Math.abs(valor - 1.0) < 1e-12) {
            return "";
        }
        if (Math.abs(valor + 1.0) < 1e-12) {
            return "-";
        }
        String texto = textoNumero(valor);
        return texto.contains("/") ? "(" + texto + ")" : texto;
    }

    private static String textoNumero(double valor) {
        return Racional.desdeDouble(valor).map(Racional::texto).orElseGet(() -> Redondeo.texto(valor));
    }

}
