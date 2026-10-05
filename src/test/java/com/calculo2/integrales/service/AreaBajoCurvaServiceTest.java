package com.calculo2.integrales.service;

import com.calculo2.integrales.exception.ExpresionInvalidaException;
import com.calculo2.integrales.exception.LimitesInvalidosException;
import com.calculo2.integrales.math.integracion.SumaRiemann;
import com.calculo2.integrales.model.MetodoArea;
import com.calculo2.integrales.model.ResultadoIntegral;
import com.calculo2.integrales.model.SolicitudIntegral;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del tema de area bajo la curva.
 *
 * <p>Lo que mas se comprueba aqui es la distincion entre la integral y el area, que es el
 * punto donde el tema se presta a confusion: cuando la curva cruza el eje X, los dos
 * valores dejan de coincidir.</p>
 */
@DisplayName("Servicio de area bajo la curva")
class AreaBajoCurvaServiceTest {

    private final AreaBajoCurvaService servicio = new AreaBajoCurvaService();

    private SolicitudIntegral solicitud(String funcion, double a, double b) {
        return new SolicitudIntegral(funcion, "", a, b, 20,
                MetodoArea.ANALITICO, SumaRiemann.Posicion.MEDIO, false, "");
    }

    @Nested
    @DisplayName("Areas con valor conocido")
    class AreasConocidas {

        @Test
        @DisplayName("el area bajo x^2 entre 0 y 1 vale 1/3")
        void parabola() {
            ResultadoIntegral resultado = servicio.calcular(solicitud("x^2", 0, 1));

            assertEquals(1.0 / 3.0, resultado.integral(), 1e-6);
            assertEquals(1.0 / 3.0, resultado.area(), 1e-6);
        }

        @Test
        @DisplayName("el area bajo una constante es la de un rectangulo")
        void constante() {
            // Base 4, altura 5.
            ResultadoIntegral resultado = servicio.calcular(solicitud("5", 0, 4));
            assertEquals(20.0, resultado.area(), 1e-6);
        }

        @Test
        @DisplayName("el area bajo x entre 0 y 3 es la de un triangulo")
        void triangulo() {
            // Base 3, altura 3, area = 9/2.
            ResultadoIntegral resultado = servicio.calcular(solicitud("x", 0, 3));
            assertEquals(4.5, resultado.area(), 1e-6);
        }

        @Test
        @DisplayName("el area bajo sqrt(x) entre 0 y 4 vale 16/3")
        void raizCuadrada() {
            ResultadoIntegral resultado = servicio.calcular(solicitud("sqrt(x)", 0, 4));
            assertEquals(16.0 / 3.0, resultado.area(), 1e-4);
        }
    }

    @Nested
    @DisplayName("Diferencia entre la integral y el area")
    class IntegralContraArea {

        @Test
        @DisplayName("bajo el eje, la integral es negativa pero el area no")
        void curvaCompletamenteBajoElEje() {
            ResultadoIntegral resultado = servicio.calcular(solicitud("-x^2", 0, 1));

            assertEquals(-1.0 / 3.0, resultado.integral(), 1e-6);
            assertEquals(1.0 / 3.0, resultado.area(), 1e-6);
        }

        @Test
        @DisplayName("sin(x) de 0 a 2pi: la integral vale 0 y el area 4")
        void areasQueSeCancelan() {
            ResultadoIntegral resultado = servicio.calcular(solicitud("sin(x)", 0, 2 * Math.PI));

            assertEquals(0.0, resultado.integral(), 1e-6);
            assertEquals(4.0, resultado.area(), 1e-4);
            assertTrue(resultado.tieneCruces(),
                    "Debe detectar que la curva cruza el eje en x = pi");
        }

        @Test
        @DisplayName("x^2 no cuenta como cruce porque solo toca el eje")
        void tocarElEjeNoEsCruzarlo() {
            ResultadoIntegral resultado = servicio.calcular(solicitud("x^2", -1, 1));

            assertFalse(resultado.tieneCruces(),
                    "x^2 vale cero en el origen pero no cambia de signo");
            assertEquals(2.0 / 3.0, resultado.area(), 1e-6);
        }
    }

    @Nested
    @DisplayName("Contenido de la respuesta")
    class ContenidoDeLaRespuesta {

        @Test
        @DisplayName("incluye la explicacion paso a paso")
        void incluyePasos() {
            ResultadoIntegral resultado = servicio.calcular(solicitud("x^2", 0, 1));
            assertFalse(resultado.pasos().isEmpty(), "El resultado debe traer pasos");
        }

        @Test
        @DisplayName("incluye los puntos de la grafica")
        void incluyeGrafica() {
            ResultadoIntegral resultado = servicio.calcular(solicitud("x^2", 0, 1));
            Object grafica = resultado.aMapa().get("grafica");
            assertTrue(grafica != null, "El resultado debe traer los datos de la grafica");
        }

        @Test
        @DisplayName("resuelve por la regla de Barrow y muestra la primitiva")
        void incluyeLaAntiderivada() {
            ResultadoIntegral resultado = servicio.calcular(solicitud("x^2", 0, 1));

            assertTrue(resultado.fueAnalitica(),
                    "Un polinomio debe resolverse encontrando la primitiva, no aproximando");
            assertEquals("x^3/3", resultado.aMapa().get("antiderivada"));
        }

        @Test
        @DisplayName("ya no trae la comparacion entre metodos ni el detalle por tramos")
        void sinSeccionesRetiradas() {
            ResultadoIntegral resultado = servicio.calcular(solicitud("sin(x)", 0, 2 * Math.PI));

            assertFalse(resultado.aMapa().containsKey("comparacion"),
                    "La comparacion entre metodos se retiro de la aplicacion");
            assertFalse(resultado.aMapa().containsKey("tramos"),
                    "El detalle por tramos se retiro de la aplicacion");
        }
    }

    @Nested
    @DisplayName("Errores que debe reportar")
    class Errores {

        @Test
        @DisplayName("rechaza una funcion mal escrita")
        void funcionInvalida() {
            assertThrows(ExpresionInvalidaException.class,
                    () -> servicio.calcular(solicitud("x^", 0, 1)));
        }

        @Test
        @DisplayName("rechaza un intervalo de longitud cero")
        void limitesIguales() {
            assertThrows(LimitesInvalidosException.class,
                    () -> servicio.calcular(solicitud("x^2", 2, 2)));
        }

        @Test
        @DisplayName("avisa cuando la funcion tiene una asintota en el intervalo")
        void asintotaDentroDelIntervalo() {
            assertThrows(LimitesInvalidosException.class,
                    () -> servicio.calcular(solicitud("1/x", -1, 1)));
        }

        @Test
        @DisplayName("avisa cuando la funcion no esta definida en el intervalo")
        void fueraDelDominio() {
            assertThrows(LimitesInvalidosException.class,
                    () -> servicio.calcular(solicitud("ln(x)", -5, -1)));
        }

        @Test
        @DisplayName("ajusta un numero de rectangulos fuera de rango en vez de fallar")
        void rectangulosFueraDeRango() {
            // Los rectangulos ya no intervienen en el resultado, que se obtiene por la
            // regla de Barrow; solo sirven para dibujar. Un valor absurdo se acota al
            // minimo util en lugar de impedir el calculo.
            SolicitudIntegral peticion = new SolicitudIntegral(
                    "x^2", "", 0.0, 1.0, 0, MetodoArea.RIEMANN,
                    SumaRiemann.Posicion.MEDIO, false, "");

            ResultadoIntegral resultado = servicio.calcular(peticion);
            assertEquals(1.0 / 3.0, resultado.area(), 1e-6);
        }

        @Test
        @DisplayName("pide los limites en lugar de suponerlos")
        void faltanLosLimites() {
            SolicitudIntegral peticion = new SolicitudIntegral(
                    "x^2", "", null, null, 20, MetodoArea.ANALITICO,
                    SumaRiemann.Posicion.MEDIO, false, "");

            LimitesInvalidosException error = assertThrows(
                    LimitesInvalidosException.class, () -> servicio.calcular(peticion));

            assertTrue(error.getMessage().toLowerCase().contains("limites"),
                    "Debe decir que faltan los limites, no inventarlos");
        }
    }

    @Nested
    @DisplayName("Limites al reves")
    class LimitesInvertidos {

        @Test
        @DisplayName("los ordena y calcula el area igual")
        void ordenaLosLimites() {
            ResultadoIntegral resultado = servicio.calcular(solicitud("x^2", 1, 0));
            assertEquals(1.0 / 3.0, resultado.area(), 1e-6);
        }
    }
}
