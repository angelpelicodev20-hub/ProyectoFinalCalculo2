package com.calculo2.integrales.math.simbolico;

import com.calculo2.integrales.math.parser.NodoExpresion;

import java.util.Optional;

/**
 * Una expresion de la forma {@code P(x) + c * sqrt(A(x))}, con P y A polinomios.
 *
 * <p>Es la forma que tienen casi todas las curvas despejadas del curso:
 * {@code sqrt(8x)}, {@code (2/3)sqrt(9 - x^2)}, {@code 1 + 2sqrt(5 - y)}. Reconocerla
 * sirve para dos cosas:</p>
 *
 * <ul>
 *   <li>Al resolver ecuaciones, para aislar la raiz y elevar al cuadrado.</li>
 *   <li>Al integrar, porque el cuadrado de {@code c*sqrt(A)} es el polinomio
 *       {@code c^2 A}: es lo que pasa en el metodo de discos, donde el radio es una raiz y
 *       se eleva al cuadrado. Sin esto, {@code (sqrt(8x))^2} no se reconoceria como
 *       {@code 8x} y la integral caeria en la aproximacion numerica.</li>
 * </ul>
 *
 * @param parteSinRaiz el polinomio P
 * @param coeficiente  el numero c que multiplica a la raiz, cero si no hay raiz
 * @param radicando    el polinomio A, o null si no hay raiz
 * @param indice       2 para raiz cuadrada, 3 para cubica, 0 si no hay raiz
 */
public record FormaRadical(Polinomio parteSinRaiz, double coeficiente, Polinomio radicando,
                           int indice) {

    /** Indica si la expresion tiene de verdad una raiz. */
    public boolean tieneRaiz() {
        return radicando != null && Math.abs(coeficiente) > 1e-15;
    }

    /** Indica si las dos expresiones tienen la misma raiz, con el mismo radicando. */
    public boolean mismaRaiz(FormaRadical otra) {
        if (!tieneRaiz() || !otra.tieneRaiz() || indice != otra.indice) {
            return false;
        }
        return iguales(radicando, otra.radicando);
    }

    /**
     * Indica si la expresion es solo la raiz por un numero, sin parte polinomica.
     *
     * <p>Es el caso en que elevar a la potencia del indice da un polinomio:
     * {@code ((2/3)sqrt(9 - x^2))^2 = (4/9)(9 - x^2)}.</p>
     */
    public boolean esRaizPura() {
        return tieneRaiz() && parteSinRaiz.esCero();
    }

    /**
     * Reconoce la forma en una expresion.
     *
     * @param nodo la expresion
     * @return la forma, o vacio si la expresion no la tiene
     */
    public static Optional<FormaRadical> de(NodoExpresion nodo) {
        Optional<Polinomio> polinomio = ConversorPolinomio.convertir(nodo);
        if (polinomio.isPresent()) {
            return Optional.of(new FormaRadical(polinomio.get(), 0.0, null, 0));
        }
        return switch (nodo) {
            case NodoExpresion.LlamadaFuncion llamada -> {
                int indice = switch (llamada.nombre()) {
                    case "sqrt", "raiz" -> 2;
                    case "cbrt" -> 3;
                    default -> 0;
                };
                if (indice == 0) {
                    yield Optional.empty();
                }
                yield ConversorPolinomio.convertir(llamada.argumento())
                        .map(a -> new FormaRadical(Polinomio.cero(), 1.0, a, indice));
            }
            case NodoExpresion.Negacion negacion -> de(negacion.operando()).map(f -> f.escalar(-1.0));
            case NodoExpresion.OperacionBinaria operacion -> deOperacion(operacion);
            default -> Optional.empty();
        };
    }

    private static Optional<FormaRadical> deOperacion(NodoExpresion.OperacionBinaria operacion) {
        // Una potencia no tiene esta forma: si es el cuadrado de una raiz, ya la reconocio
        // el conversor de polinomios al principio de de().
        if (operacion.simbolo().equals("^")) {
            return Optional.empty();
        }
        Optional<FormaRadical> izquierda = de(operacion.izquierda());
        Optional<FormaRadical> derecha = de(operacion.derecha());
        if (izquierda.isEmpty() || derecha.isEmpty()) {
            return Optional.empty();
        }
        FormaRadical a = izquierda.get();
        FormaRadical b = derecha.get();

        return switch (operacion.simbolo()) {
            case "+" -> a.sumar(b, 1.0);
            case "-" -> a.sumar(b, -1.0);
            case "*" -> {
                if (!a.tieneRaiz() && a.parteSinRaiz.esConstante()) {
                    yield Optional.of(b.escalar(a.parteSinRaiz.coeficiente(0)));
                }
                if (!b.tieneRaiz() && b.parteSinRaiz.esConstante()) {
                    yield Optional.of(a.escalar(b.parteSinRaiz.coeficiente(0)));
                }
                yield Optional.empty();
            }
            case "/" -> {
                if (!b.tieneRaiz() && b.parteSinRaiz.esConstante()
                        && Math.abs(b.parteSinRaiz.coeficiente(0)) > 1e-15) {
                    yield Optional.of(a.escalar(1.0 / b.parteSinRaiz.coeficiente(0)));
                }
                yield Optional.empty();
            }
            default -> Optional.empty();
        };
    }

    /** Multiplica toda la expresion por un numero. */
    public FormaRadical escalar(double factor) {
        return new FormaRadical(parteSinRaiz.por(factor), coeficiente * factor, radicando, indice);
    }

    private Optional<FormaRadical> sumar(FormaRadical otra, double signo) {
        Polinomio suma = parteSinRaiz.mas(otra.parteSinRaiz.por(signo));
        if (!otra.tieneRaiz()) {
            return Optional.of(new FormaRadical(suma, coeficiente, radicando, indice));
        }
        if (!tieneRaiz()) {
            return Optional.of(new FormaRadical(suma, otra.coeficiente * signo,
                    otra.radicando, otra.indice));
        }
        if (!mismaRaiz(otra)) {
            return Optional.empty();
        }
        return Optional.of(new FormaRadical(suma, coeficiente + signo * otra.coeficiente,
                radicando, indice));
    }

    private static boolean iguales(Polinomio a, Polinomio b) {
        int grado = Math.max(a.grado(), b.grado());
        for (int i = 0; i <= grado; i++) {
            if (Math.abs(a.coeficiente(i) - b.coeficiente(i)) > 1e-9) {
                return false;
            }
        }
        return true;
    }
}
