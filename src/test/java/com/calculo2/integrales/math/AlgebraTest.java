package com.calculo2.integrales.math;

import com.calculo2.integrales.math.algebra.Despejador;
import com.calculo2.integrales.math.algebra.Factorizador;
import com.calculo2.integrales.math.algebra.ResolutorEcuaciones;
import com.calculo2.integrales.math.parser.EvaluadorExpresion;
import com.calculo2.integrales.math.simbolico.ConversorPolinomio;
import com.calculo2.integrales.math.simbolico.IntegradorSimbolico;
import com.calculo2.integrales.math.simbolico.Polinomio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El algebra que necesitan los ejercicios: factorizar, resolver ecuaciones con raices,
 * despejar curvas y simplificar los integrandos con raices.
 */
@DisplayName("Algebra de los procedimientos")
class AlgebraTest {

    private static ResolutorEcuaciones.Solucion resolver(String izquierda, String derecha) {
        return ResolutorEcuaciones.resolver(
                EvaluadorExpresion.compilar(izquierda).raiz(),
                EvaluadorExpresion.compilar(derecha).raiz(),
                izquierda, derecha, "x", -100, 100);
    }

    @Nested
    @DisplayName("Factorizacion")
    class Factorizacion {

        @Test
        @DisplayName("saca el factor comun y la diferencia de cuadrados")
        void factorComunYDiferenciaDeCuadrados() {
            Factorizador.Resultado resultado = Factorizador.resolverIgualACero(
                    Polinomio.de(0, -1, 0, 1), "x");

            assertEquals("x(x - 1)(x + 1)", resultado.formaFactorizada());
            assertEquals(List.of(-1.0, 0.0, 1.0), resultado.valores());
            assertTrue(resultado.exacto());
        }

        @Test
        @DisplayName("encuentra raices racionales con el teorema de la raiz racional")
        void raicesRacionales() {
            // x^4 - 8x = x(x - 2)(x^2 + 2x + 4)
            Factorizador.Resultado resultado = Factorizador.resolverIgualACero(
                    Polinomio.de(0, -8, 0, 0, 1), "x");

            assertEquals("x(x - 2)(x^2 + 2x + 4)", resultado.formaFactorizada());
            assertEquals(List.of(0.0, 2.0), resultado.valores());
        }

        @Test
        @DisplayName("da las raices irracionales exactas con la formula general")
        void raicesIrracionales() {
            Factorizador.Resultado resultado = Factorizador.resolverIgualACero(
                    Polinomio.de(-1, -2, 1), "x");

            assertEquals("1 - sqrt(2)", resultado.raices().get(0).texto());
            assertEquals("1 + sqrt(2)", resultado.raices().get(1).texto());
        }

        @Test
        @DisplayName("escribe los factores con coeficientes enteros")
        void factoresEnteros() {
            Factorizador.Resultado resultado = Factorizador.resolverIgualACero(
                    Polinomio.de(1, -5, 6), "x");

            assertEquals("(2x - 1)(3x - 1)", resultado.formaFactorizada());
        }

        @Test
        @DisplayName("dice cuando un trinomio no tiene raices reales")
        void sinRaicesReales() {
            Factorizador.Resultado resultado = Factorizador.resolverIgualACero(
                    Polinomio.de(1, 0, 1), "x");

            assertTrue(resultado.raices().isEmpty());
            assertTrue(String.join(" ", resultado.pasos()).contains("negativo"));
        }
    }

    @Nested
    @DisplayName("Ecuaciones con raices")
    class EcuacionesConRaices {

        @Test
        @DisplayName("eleva al cuadrado y comprueba las soluciones")
        void elevaAlCuadrado() {
            ResolutorEcuaciones.Solucion solucion = resolver("x^2", "sqrt(8x)");

            assertEquals(List.of(0.0, 2.0), solucion.valores());
            assertTrue(String.join(" ", solucion.pasos()).contains("al cuadrado"));
        }

        @Test
        @DisplayName("descarta las soluciones falsas que aparecen al elevar")
        void descartaSolucionesFalsas() {
            // sqrt(x) = x - 6 se cumple en x = 9; x = 4 aparece al elevar y no vale.
            ResolutorEcuaciones.Solucion solucion = resolver("sqrt(x)", "x - 6");

            assertEquals(List.of(9.0), solucion.valores());
        }

        @Test
        @DisplayName("encuentra el corte en el borde del dominio")
        void bordeDelDominio() {
            ResolutorEcuaciones.Solucion solucion = resolver("sqrt(8x)", "-sqrt(8x)");

            assertEquals(List.of(0.0), solucion.valores());
        }
    }

    @Nested
    @DisplayName("Despeje")
    class Despeje {

        @Test
        @DisplayName("y^2 = 8x da dos ramas en y y una en x")
        void parabola() {
            Despejador.Despeje enY = Despejador.despejar("y^2", "8x", "y").orElseThrow();
            Despejador.Despeje enX = Despejador.despejar("y^2", "8x", "x").orElseThrow();

            assertEquals("sqrt(8x)", enY.ramas().get(0).expresion());
            assertEquals("-sqrt(8x)", enY.ramas().get(1).expresion());
            assertEquals("y^2/8", enX.ramas().get(0).expresion());
        }

        @Test
        @DisplayName("la elipse saca el factor de la raiz")
        void elipse() {
            Despejador.Despeje enY = Despejador.despejar("4x^2 + 9y^2", "36", "y").orElseThrow();

            assertEquals("(2/3)sqrt(9 - x^2)", enY.ramas().get(0).expresion());
        }

        @Test
        @DisplayName("(x - 1)^2 = 20 - 4y se despeja sacando la raiz")
        void potenciaDeUnaExpresionLineal() {
            Despejador.Despeje enX = Despejador.despejar("(x - 1)^2", "20 - 4y", "x").orElseThrow();

            assertEquals("1 + sqrt(20 - 4y)", enX.ramas().get(0).expresion());
            assertEquals("1 - sqrt(20 - 4y)", enX.ramas().get(1).expresion());
        }

        @Test
        @DisplayName("4y = 4 - x^2 queda como y = 1 - x^2/4")
        void lineal() {
            Despejador.Despeje enY = Despejador.despejar("4y", "4 - x^2", "y").orElseThrow();

            assertEquals("1 - x^2/4", enY.ramas().get(0).expresion());
        }

        @Test
        @DisplayName("una cubica general no se despeja, y no se inventa nada")
        void cubicaGeneral() {
            assertFalse(Despejador.despejar("y", "x^3 + x + 1", "x").isPresent());
        }
    }

    @Nested
    @DisplayName("Integrandos con raices")
    class IntegrandosConRaices {

        @Test
        @DisplayName("el cuadrado de una raiz es un polinomio")
        void cuadradoDeUnaRaiz() {
            Polinomio polinomio = ConversorPolinomio.convertir(
                    EvaluadorExpresion.compilar("((2/3)sqrt(9 - x^2))^2").raiz()).orElseThrow();

            assertEquals(4.0, polinomio.coeficiente(0), 1e-12);
            assertEquals(-4.0 / 9, polinomio.coeficiente(2), 1e-12);
        }

        @Test
        @DisplayName("x*sqrt(8x) se integra con la regla de la potencia")
        void potenciaFraccionaria() {
            IntegradorSimbolico.Antiderivada primitiva = IntegradorSimbolico.integrar(
                    EvaluadorExpresion.compilar("x*(sqrt(8x) - (-sqrt(8x)))").raiz(), "x").orElseThrow();

            assertEquals(64.0 / 5, primitiva.integralDefinida(0, 2), 1e-9);
            assertEquals("8sqrt(2)x^(5/2)/5", primitiva.texto());
        }
    }
}
