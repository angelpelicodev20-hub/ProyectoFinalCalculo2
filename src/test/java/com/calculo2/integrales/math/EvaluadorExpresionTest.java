package com.calculo2.integrales.math;

import com.calculo2.integrales.exception.ExpresionInvalidaException;
import com.calculo2.integrales.math.parser.EvaluadorExpresion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del parser de funciones.
 *
 * <p>El parser es la puerta de entrada de todo el proyecto: si interpreta mal lo que el
 * usuario escribe, el resultado sera incorrecto por mas que la integracion este bien.
 * Estas pruebas cubren sobre todo los casos donde una lectura descuidada daria un numero
 * distinto: precedencia de operadores, signos y potencias.</p>
 */
@DisplayName("Parser de expresiones matematicas")
class EvaluadorExpresionTest {

    private static final double TOLERANCIA = 1e-9;

    private double evaluar(String expresion, double x) {
        return EvaluadorExpresion.compilar(expresion).evaluar(x);
    }

    @Nested
    @DisplayName("Operaciones basicas")
    class OperacionesBasicas {

        @Test
        @DisplayName("suma, resta, producto y division")
        void operacionesAritmeticas() {
            assertEquals(7.0, evaluar("3 + 4", 0), TOLERANCIA);
            assertEquals(-1.0, evaluar("3 - 4", 0), TOLERANCIA);
            assertEquals(12.0, evaluar("3 * 4", 0), TOLERANCIA);
            assertEquals(0.75, evaluar("3 / 4", 0), TOLERANCIA);
        }

        @Test
        @DisplayName("la multiplicacion va antes que la suma")
        void precedenciaDeProducto() {
            assertEquals(14.0, evaluar("2 + 3 * 4", 0), TOLERANCIA);
            assertEquals(20.0, evaluar("(2 + 3) * 4", 0), TOLERANCIA);
        }

        @Test
        @DisplayName("la variable toma el valor que se le pasa")
        void variable() {
            assertEquals(9.0, evaluar("x^2", 3), TOLERANCIA);
            assertEquals(0.0, evaluar("x - 5", 5), TOLERANCIA);
        }
    }

    @Nested
    @DisplayName("Potencias y signos")
    class PotenciasYSignos {

        @Test
        @DisplayName("-x^2 se lee como -(x^2), no como (-x)^2")
        void elSignoNoEntraEnLaPotencia() {
            assertEquals(-9.0, evaluar("-x^2", 3), TOLERANCIA);
            assertEquals(9.0, evaluar("(-x)^2", 3), TOLERANCIA);
        }

        @Test
        @DisplayName("la potencia se asocia a la derecha: 2^3^2 = 2^9")
        void potenciaAsociativaALaDerecha() {
            assertEquals(512.0, evaluar("2^3^2", 0), TOLERANCIA);
        }

        @Test
        @DisplayName("admite exponentes negativos")
        void exponenteNegativo() {
            assertEquals(0.5, evaluar("2^-1", 0), TOLERANCIA);
        }

        @Test
        @DisplayName("la raiz cubica de un numero negativo si tiene resultado")
        void raizImparDeNegativo() {
            assertEquals(-2.0, evaluar("(-8)^(1/3)", 0), 1e-6);
        }
    }

    @Nested
    @DisplayName("Multiplicacion implicita")
    class MultiplicacionImplicita {

        @Test
        @DisplayName("2x equivale a 2 * x")
        void numeroPegadoALaVariable() {
            assertEquals(10.0, evaluar("2x", 5), TOLERANCIA);
        }

        @Test
        @DisplayName("2(x+1) equivale a 2 * (x+1)")
        void numeroPegadoAlParentesis() {
            assertEquals(12.0, evaluar("2(x+1)", 5), TOLERANCIA);
        }

        @Test
        @DisplayName("(x+1)(x-1) equivale a su producto")
        void parentesisPegados() {
            assertEquals(24.0, evaluar("(x+1)(x-1)", 5), TOLERANCIA);
        }

        @Test
        @DisplayName("3sin(0) equivale a 3 * sin(0)")
        void numeroPegadoAFuncion() {
            assertEquals(0.0, evaluar("3sin(x)", 0), TOLERANCIA);
        }
    }

    @Nested
    @DisplayName("Funciones y constantes del catalogo")
    class FuncionesDelCatalogo {

        @Test
        @DisplayName("trigonometricas en radianes")
        void trigonometricas() {
            assertEquals(0.0, evaluar("sin(x)", 0), TOLERANCIA);
            assertEquals(1.0, evaluar("cos(x)", 0), TOLERANCIA);
            assertEquals(1.0, evaluar("sin(x)", Math.PI / 2), TOLERANCIA);
        }

        @Test
        @DisplayName("raiz, logaritmo y exponencial")
        void raicesYLogaritmos() {
            assertEquals(3.0, evaluar("sqrt(x)", 9), TOLERANCIA);
            assertEquals(1.0, evaluar("ln(x)", Math.E), TOLERANCIA);
            assertEquals(2.0, evaluar("log(x)", 100), TOLERANCIA);
            assertEquals(1.0, evaluar("exp(x)", 0), TOLERANCIA);
        }

        @Test
        @DisplayName("pi y e valen lo que deben")
        void constantes() {
            assertEquals(Math.PI, evaluar("pi", 0), TOLERANCIA);
            assertEquals(Math.E, evaluar("e", 0), TOLERANCIA);
        }

        @Test
        @DisplayName("acepta los nombres en espanol")
        void nombresEnEspanol() {
            assertEquals(0.0, evaluar("sen(x)", 0), TOLERANCIA);
            assertEquals(3.0, evaluar("raiz(x)", 9), TOLERANCIA);
        }
    }

    @Nested
    @DisplayName("Limpieza de la entrada")
    class LimpiezaDeEntrada {

        @Test
        @DisplayName("descarta el 'f(x) =' que se copia del cuaderno")
        void quitaElEncabezado() {
            assertEquals(9.0, evaluar("f(x) = x^2", 3), TOLERANCIA);
            assertEquals(9.0, evaluar("y = x^2", 3), TOLERANCIA);
        }

        @Test
        @DisplayName("entiende los simbolos que vienen de copiar y pegar")
        void simbolosAlternativos() {
            assertEquals(3.0, evaluar("√x", 9), TOLERANCIA);
            assertEquals(Math.PI, evaluar("π", 0), TOLERANCIA);
        }

        @Test
        @DisplayName("los espacios no cambian el resultado")
        void espaciosIrrelevantes() {
            assertEquals(evaluar("x^2+3*x", 2), evaluar("  x ^ 2  +  3 * x ", 2), TOLERANCIA);
        }
    }

    @Nested
    @DisplayName("Errores que debe reportar")
    class Errores {

        @Test
        @DisplayName("avisa cuando falta cerrar un parentesis")
        void parentesisSinCerrar() {
            ExpresionInvalidaException error = assertThrows(
                    ExpresionInvalidaException.class, () -> evaluar("sin(x", 0));
            assertTrue(error.getMessage().toLowerCase().contains("parentesis"));
        }

        @Test
        @DisplayName("avisa cuando el nombre de la funcion no existe")
        void funcionDesconocida() {
            assertThrows(ExpresionInvalidaException.class, () -> evaluar("zorp(x)", 0));
        }

        @Test
        @DisplayName("avisa cuando la expresion viene vacia")
        void expresionVacia() {
            assertThrows(ExpresionInvalidaException.class, () -> evaluar("   ", 0));
        }

        @Test
        @DisplayName("avisa cuando falta el operando de un operador")
        void operadorSinOperando() {
            assertThrows(ExpresionInvalidaException.class, () -> evaluar("x +", 0));
        }

        @Test
        @DisplayName("exige parentesis despues del nombre de una funcion")
        void funcionSinParentesis() {
            assertThrows(ExpresionInvalidaException.class, () -> evaluar("sin x + 1", 0));
        }
    }

    @Nested
    @DisplayName("Variable de integracion")
    class VariableDeIntegracion {

        @Test
        @DisplayName("puede compilarse respecto a y en lugar de x")
        void variableY() {
            EvaluadorExpresion expresion = EvaluadorExpresion.compilar("y^2", "y");
            assertEquals(16.0, expresion.evaluar(4), TOLERANCIA);
        }
    }
}
