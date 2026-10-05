package com.calculo2.integrales.math.parser;

import com.calculo2.integrales.exception.ExpresionInvalidaException;

import java.util.function.DoubleUnaryOperator;

/**
 * Nodo del arbol en que se convierte la expresion del usuario.
 *
 * <p>Por ejemplo {@code x^2 + 3} se guarda como una suma cuyo lado izquierdo es una
 * potencia y cuyo lado derecho es el numero tres. Evaluar la funcion es recorrer el
 * arbol desde la raiz.</p>
 *
 * <p>La interfaz es sellada: solo existen los cinco tipos de nodo declarados abajo,
 * asi que el compilador puede verificar que ningun caso quede sin tratar.</p>
 */
public sealed interface NodoExpresion {

    /**
     * Calcula el valor de este nodo.
     *
     * @param variable valor de la variable independiente
     * @return el resultado; puede ser NaN o infinito fuera del dominio
     */
    double evaluar(double variable);

    /**
     * Reconstruye la expresion en texto, ya normalizada.
     * Sirve para mostrarle al usuario como se interpreto lo que escribio.
     */
    String aTexto();

    // ------------------------------------------------------------------
    // TIPOS DE NODO
    // ------------------------------------------------------------------

    /** Un numero fijo dentro de la expresion. */
    record Numero(double valor) implements NodoExpresion {

        @Override
        public double evaluar(double variable) {
            return valor;
        }

        @Override
        public String aTexto() {
            if (valor == Math.rint(valor) && Math.abs(valor) < 1e15) {
                return String.valueOf((long) valor);
            }
            return String.valueOf(valor);
        }
    }

    /** La variable independiente. */
    record Variable(String nombre) implements NodoExpresion {

        @Override
        public double evaluar(double variable) {
            return variable;
        }

        @Override
        public String aTexto() {
            return nombre;
        }
    }

    /** Una constante con nombre, como pi o e. */
    record Constante(String nombre, double valor) implements NodoExpresion {

        @Override
        public double evaluar(double variable) {
            return valor;
        }

        @Override
        public String aTexto() {
            return nombre;
        }
    }

    /** El signo menos delante de una expresion. */
    record Negacion(NodoExpresion operando) implements NodoExpresion {

        @Override
        public double evaluar(double variable) {
            return -operando.evaluar(variable);
        }

        @Override
        public String aTexto() {
            return "-" + operando.aTexto();
        }
    }

    /** Una operacion con dos operandos. */
    record OperacionBinaria(String simbolo, NodoExpresion izquierda, NodoExpresion derecha)
            implements NodoExpresion {

        @Override
        public double evaluar(double variable) {
            double a = izquierda.evaluar(variable);
            double b = derecha.evaluar(variable);
            return switch (simbolo) {
                case "+" -> a + b;
                case "-" -> a - b;
                case "*" -> a * b;
                case "/" -> a / b;
                case "%" -> a % b;
                case "^" -> potencia(a, b);
                default -> throw new ExpresionInvalidaException(
                        "El operador '" + simbolo + "' no esta soportado.");
            };
        }

        /**
         * Java devuelve NaN para casos como {@code (-8)^(1/3)}, que en clase si tiene
         * respuesta. Cuando la base es negativa y el exponente es un entero impar
         * disfrazado de fraccion, se calcula la raiz sobre el valor absoluto y se le
         * devuelve el signo.
         */
        private static double potencia(double base, double exponente) {
            double resultado = Math.pow(base, exponente);
            if (!Double.isNaN(resultado) || base >= 0) {
                return resultado;
            }
            double inverso = 1.0 / exponente;
            double inversoRedondeado = Math.rint(inverso);
            boolean esRaizImpar = Math.abs(inverso - inversoRedondeado) < 1e-9
                    && Math.abs(inversoRedondeado) % 2 == 1;
            if (esRaizImpar) {
                return -Math.pow(-base, exponente);
            }
            return Double.NaN;
        }

        @Override
        public String aTexto() {
            return "(" + izquierda.aTexto() + " " + simbolo + " " + derecha.aTexto() + ")";
        }
    }

    /** La llamada a una funcion del catalogo, como {@code sin(x)}. */
    record LlamadaFuncion(String nombre, NodoExpresion argumento) implements NodoExpresion {

        @Override
        public double evaluar(double variable) {
            DoubleUnaryOperator funcion = CatalogoFunciones.obtenerFuncion(nombre);
            if (funcion == null) {
                throw new ExpresionInvalidaException("La funcion '" + nombre + "' no existe.");
            }
            return funcion.applyAsDouble(argumento.evaluar(variable));
        }

        @Override
        public String aTexto() {
            return nombre + "(" + argumento.aTexto() + ")";
        }
    }
}
