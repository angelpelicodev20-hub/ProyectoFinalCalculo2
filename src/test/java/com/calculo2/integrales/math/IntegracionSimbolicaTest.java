package com.calculo2.integrales.math;

import com.calculo2.integrales.math.parser.EvaluadorExpresion;
import com.calculo2.integrales.math.simbolico.ConversorPolinomio;
import com.calculo2.integrales.math.simbolico.IntegradorSimbolico;
import com.calculo2.integrales.math.simbolico.Polinomio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del algebra de polinomios y del integrador simbolico.
 *
 * <p>Son la base del procedimiento paso a paso: si la antiderivada sale mal, el
 * resultado y toda la explicacion salen mal con ella. Por eso se comprueban dos cosas
 * distintas en cada caso: que el valor numerico coincida con el conocido, y que el texto
 * de la primitiva sea el que un alumno escribiria.</p>
 */
@DisplayName("Integracion simbolica")
class IntegracionSimbolicaTest {

    private static final double TOLERANCIA = 1e-9;

    private static Polinomio polinomio(String expresion) {
        return ConversorPolinomio.convertir(
                EvaluadorExpresion.compilar(expresion).raiz()).orElseThrow();
    }

    private static IntegradorSimbolico.Antiderivada primitiva(String expresion) {
        return IntegradorSimbolico.integrar(
                EvaluadorExpresion.compilar(expresion).raiz(), "x").orElseThrow();
    }

    @Nested
    @DisplayName("Algebra de polinomios")
    class AlgebraDePolinomios {

        @Test
        @DisplayName("reconoce un polinomio escrito por el usuario")
        void reconoceUnPolinomio() {
            assertEquals("x^2 + 3x + 2", polinomio("x^2 + 3x + 2").aTexto());
        }

        @Test
        @DisplayName("desarrolla un producto de binomios")
        void desarrollaUnProducto() {
            assertEquals("x^2 - 1", polinomio("(x+1)(x-1)").aTexto());
        }

        @Test
        @DisplayName("desarrolla una potencia")
        void desarrollaUnaPotencia() {
            assertEquals("x^2 + 4x + 4", polinomio("(x+2)^2").aTexto());
        }

        @Test
        @DisplayName("desarrolla la diferencia de cuadrados del metodo de arandelas")
        void desarrollaLaDiferenciaDeCuadrados() {
            // Es exactamente el integrando del ejercicio de prueba del proyecto:
            // (-x^2+4)^2 - (2x+1)^2 = x^4 - 8x^2 + 16 - 4x^2 - 4x - 1
            Polinomio resultado = polinomio("(-x^2+4)^2 - (2x+1)^2");
            assertEquals("x^4 - 12x^2 - 4x + 15", resultado.aTexto());
        }

        @Test
        @DisplayName("no escribe el coeficiente 1 ni los terminos nulos")
        void escrituraLimpia() {
            assertEquals("x^3 - x", polinomio("x^3 + 0x^2 - x").aTexto());
        }

        @Test
        @DisplayName("rechaza lo que no es polinomio")
        void rechazaLoQueNoEsPolinomio() {
            assertFalse(ConversorPolinomio.esPolinomio(
                    EvaluadorExpresion.compilar("sin(x)").raiz()));
            assertFalse(ConversorPolinomio.esPolinomio(
                    EvaluadorExpresion.compilar("1/x").raiz()));
            assertFalse(ConversorPolinomio.esPolinomio(
                    EvaluadorExpresion.compilar("sqrt(x)").raiz()));
        }

        @Test
        @DisplayName("el grado baja cuando los terminos se cancelan")
        void elGradoSeAjusta() {
            assertEquals(0, polinomio("x^2 - x^2 + 5").grado());
        }
    }

    @Nested
    @DisplayName("Antiderivadas de polinomios")
    class AntiderivadasDePolinomios {

        @Test
        @DisplayName("la integral de x^2 es x^3/3")
        void potenciaSencilla() {
            assertEquals("x^3/3", primitiva("x^2").texto());
            assertEquals(1.0 / 3.0, primitiva("x^2").integralDefinida(0, 1), TOLERANCIA);
        }

        @Test
        @DisplayName("la integral de una constante es la constante por x")
        void constante() {
            assertEquals(20.0, primitiva("5").integralDefinida(0, 4), TOLERANCIA);
        }

        @Test
        @DisplayName("integra termino a termino")
        void terminoATermino() {
            // integral de 3x^2 + 2x + 1 = x^3 + x^2 + x
            assertEquals("x^3 + x^2 + x", primitiva("3x^2 + 2x + 1").texto());
            assertEquals(3.0, primitiva("3x^2 + 2x + 1").integralDefinida(0, 1), TOLERANCIA);
        }

        @Test
        @DisplayName("resuelve el integrando del ejercicio de arandelas")
        void integrandoDeArandelas() {
            // integral de 0 a 1 de (x^4 - 12x^2 - 4x + 15) = 1/5 - 4 - 2 + 15 = 9.2
            assertEquals(9.2, primitiva("(-x^2+4)^2 - (2x+1)^2").integralDefinida(0, 1), 1e-7);
        }
    }

    @Nested
    @DisplayName("Antiderivadas que no son polinomios")
    class AntiderivadasNoPolinomicas {

        @Test
        @DisplayName("la integral de sqrt(x) de 0 a 4 vale 16/3")
        void raizCuadrada() {
            assertEquals(16.0 / 3.0, primitiva("sqrt(x)").integralDefinida(0, 4), 1e-7);
        }

        @Test
        @DisplayName("la integral de 1/x de 1 a e vale 1")
        void logaritmo() {
            assertEquals(1.0, primitiva("1/x").integralDefinida(1, Math.E), 1e-7);
        }

        @Test
        @DisplayName("la integral de sin(x) de 0 a pi vale 2")
        void seno() {
            assertEquals(2.0, primitiva("sin(x)").integralDefinida(0, Math.PI), 1e-9);
        }

        @Test
        @DisplayName("la integral de cos(x) de 0 a pi/2 vale 1")
        void coseno() {
            assertEquals(1.0, primitiva("cos(x)").integralDefinida(0, Math.PI / 2), 1e-9);
        }

        @Test
        @DisplayName("la integral de e^x de 0 a 1 vale e - 1")
        void exponencial() {
            assertEquals(Math.E - 1.0, primitiva("exp(x)").integralDefinida(0, 1), 1e-9);
        }

        @Test
        @DisplayName("ajusta por la constante de la regla de la cadena")
        void argumentoLineal() {
            // integral de 0 a pi/2 de sin(2x) = [-cos(2x)/2] = (-(-1) + 1)/2 = 1
            assertEquals(1.0, primitiva("sin(2x)").integralDefinida(0, Math.PI / 2), 1e-9);
        }

        @Test
        @DisplayName("integra una potencia de un binomio lineal")
        void potenciaDeBinomio() {
            // integral de 0 a 1 de (2x+1)^3 = [(2x+1)^4/8] = (81 - 1)/8 = 10
            assertEquals(10.0, primitiva("(2x+1)^3").integralDefinida(0, 1), 1e-7);
        }

        @Test
        @DisplayName("integra una potencia negativa")
        void potenciaNegativa() {
            // integral de 1 a 2 de x^-2 = [-1/x] = -1/2 + 1 = 0.5
            assertEquals(0.5, primitiva("x^-2").integralDefinida(1, 2), 1e-9);
        }
    }

    @Nested
    @DisplayName("Lo que no sabe integrar")
    class LimitesDelIntegrador {

        @Test
        @DisplayName("avisa en lugar de inventar una primitiva")
        void devuelveVacioSiNoSabe() {
            // Un producto de dos funciones de x necesitaria integracion por partes.
            Optional<IntegradorSimbolico.Antiderivada> resultado = IntegradorSimbolico.integrar(
                    EvaluadorExpresion.compilar("x*sin(x)").raiz(), "x");

            assertTrue(resultado.isEmpty(),
                    "Si no sabe integrarlo debe decirlo, no devolver algo equivocado");
        }

        @Test
        @DisplayName("tampoco inventa con un argumento que no es lineal")
        void argumentoNoLineal() {
            Optional<IntegradorSimbolico.Antiderivada> resultado = IntegradorSimbolico.integrar(
                    EvaluadorExpresion.compilar("sin(x^2)").raiz(), "x");

            assertTrue(resultado.isEmpty());
        }
    }

    @Nested
    @DisplayName("Explicacion de las reglas")
    class ExplicacionDeReglas {

        @Test
        @DisplayName("cada primitiva viene con las reglas que se usaron")
        void traeLasReglas() {
            assertFalse(primitiva("x^2").reglas().isEmpty(),
                    "El procedimiento necesita poder citar la regla aplicada");
        }

        @Test
        @DisplayName("explica por que 1/x es un caso aparte")
        void explicaElLogaritmo() {
            String reglas = String.join(" ", primitiva("1/x").reglas());
            assertTrue(reglas.contains("ln"),
                    "Debe explicar que la regla de la potencia no sirve con exponente -1");
        }
    }
}
