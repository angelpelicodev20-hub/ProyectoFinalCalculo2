package com.calculo2.integrales.service;

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
 * Pruebas del tema de area entre dos curvas.
 *
 * <p>El caso que mas importa comprobar es el de curvas que se cruzan dentro del intervalo:
 * ahi la formula {@code integral de (f - g)} deja de dar el area, porque los tramos se
 * restan entre si, y hay que partir la integral en los puntos de corte.</p>
 */
@DisplayName("Servicio de area entre dos curvas")
class AreaEntreCurvasServiceTest {

    private final AreaEntreCurvasService servicio = new AreaEntreCurvasService();

    private SolicitudIntegral solicitud(String f, String g, double a, double b) {
        return new SolicitudIntegral(f, g, a, b, 20,
                MetodoArea.ANALITICO, SumaRiemann.Posicion.MEDIO, false, "");
    }

    private SolicitudIntegral conLimitesAutomaticos(String f, String g) {
        return new SolicitudIntegral(f, g, null, null, 20,
                MetodoArea.ANALITICO, SumaRiemann.Posicion.MEDIO, true, "");
    }

    @Nested
    @DisplayName("Areas con valor conocido")
    class AreasConocidas {

        @Test
        @DisplayName("entre x y x^2, de 0 a 1, el area vale 1/6")
        void rectaYParabola() {
            // integral de 0 a 1 de (x - x^2) = 1/2 - 1/3 = 1/6
            ResultadoIntegral resultado = servicio.calcular(solicitud("x", "x^2", 0, 1));
            assertEquals(1.0 / 6.0, resultado.area(), 1e-6);
        }

        @Test
        @DisplayName("el orden en que se escriban las curvas no cambia el area")
        void elOrdenNoImporta() {
            double areaUnOrden = servicio.calcular(solicitud("x", "x^2", 0, 1)).area();
            double areaOtroOrden = servicio.calcular(solicitud("x^2", "x", 0, 1)).area();

            assertEquals(areaUnOrden, areaOtroOrden, 1e-9,
                    "El area es la misma; solo cambiaria el signo de la integral");
        }

        @Test
        @DisplayName("entre dos rectas paralelas queda una franja rectangular")
        void franjaEntreParalelas() {
            // Separacion constante de 3 unidades a lo largo de un intervalo de ancho 2.
            ResultadoIntegral resultado = servicio.calcular(solicitud("x + 3", "x", 0, 2));
            assertEquals(6.0, resultado.area(), 1e-6);
        }

        @Test
        @DisplayName("entre 4 - x^2 y el eje X el area vale 32/3")
        void parabolaYEje() {
            ResultadoIntegral resultado = servicio.calcular(solicitud("4 - x^2", "0", -2, 2));
            assertEquals(32.0 / 3.0, resultado.area(), 1e-5);
        }
    }

    @Nested
    @DisplayName("Curvas que se cruzan dentro del intervalo")
    class CurvasQueSeCruzan {

        @Test
        @DisplayName("parte la integral y no deja que los tramos se cancelen")
        void tramosQueNoSeCancelan() {
            // De -1 a 1, x^3 y x se cruzan en -1, 0 y 1. Integrar de corrido da cero por
            // simetria; el area real es 1/2.
            ResultadoIntegral resultado = servicio.calcular(solicitud("x^3", "x", -1, 1));

            assertEquals(0.0, resultado.integral(), 1e-6,
                    "Integrar de corrido da cero porque los tramos se cancelan");
            assertEquals(0.5, resultado.area(), 1e-5,
                    "El area real no es cero");
        }

        @Test
        @DisplayName("detecta los puntos de corte")
        void detectaLosCortes() {
            ResultadoIntegral resultado = servicio.calcular(solicitud("x^3", "x", -1, 1));
            assertTrue(resultado.tieneCruces(), "Debe registrar los puntos donde se cruzan");
        }

        @Test
        @DisplayName("avisa al usuario cuando hubo cruces")
        void avisaDeLosCruces() {
            ResultadoIntegral resultado = servicio.calcular(solicitud("x^3", "x", -1, 1));
            String advertencia = String.valueOf(resultado.aMapa().get("advertencia"));

            assertFalse(advertencia.isBlank(), "Debe explicar por que la integral difiere del area");
        }

        @Test
        @DisplayName("area entre sin(x) y cos(x) de 0 a pi vale 2*raiz(2)")
        void senoYCoseno() {
            // Se cruzan en pi/4, asi que la integral se parte en dos tramos:
            //   de 0 a pi/4    manda cos:  integral de (cos - sin) = raiz(2) - 1
            //   de pi/4 a pi   manda sin:  integral de (sin - cos) = 1 + raiz(2)
            // Sumando los dos: 2*raiz(2).
            ResultadoIntegral resultado = servicio.calcular(solicitud("sin(x)", "cos(x)", 0, Math.PI));
            assertEquals(2.0 * Math.sqrt(2.0), resultado.area(), 1e-4);
        }
    }

    @Nested
    @DisplayName("Limites deducidos de las intersecciones")
    class LimitesAutomaticos {

        @Test
        @DisplayName("encuentra solo la region encerrada entre x y x^2")
        void deduceLosLimites() {
            ResultadoIntegral resultado = servicio.calcular(conLimitesAutomaticos("x", "x^2"));

            assertEquals(1.0 / 6.0, resultado.area(), 1e-5);
            assertEquals(0.0, (double) resultado.aMapa().get("limiteInferior"), 1e-6);
            assertEquals(1.0, (double) resultado.aMapa().get("limiteSuperior"), 1e-6);
        }

        @Test
        @DisplayName("encuentra la region encerrada entre 4 - x^2 y el eje")
        void deduceLimitesDeUnaParabola() {
            ResultadoIntegral resultado = servicio.calcular(conLimitesAutomaticos("4 - x^2", "0"));
            assertEquals(32.0 / 3.0, resultado.area(), 1e-4);
        }

        @Test
        @DisplayName("avisa cuando las curvas no encierran ninguna region")
        void curvasQueNoSeCortan() {
            assertThrows(LimitesInvalidosException.class,
                    () -> servicio.calcular(conLimitesAutomaticos("x + 5", "x")));
        }
    }

    @Nested
    @DisplayName("Errores que debe reportar")
    class Errores {

        @Test
        @DisplayName("exige la segunda curva")
        void faltaLaSegundaCurva() {
            LimitesInvalidosException error = assertThrows(LimitesInvalidosException.class,
                    () -> servicio.calcular(solicitud("x^2", "", 0, 1)));

            assertTrue(error.getMessage().toLowerCase().contains("dos"),
                    "El mensaje debe explicar que hacen falta las dos funciones");
        }

        @Test
        @DisplayName("rechaza un intervalo de longitud cero")
        void limitesIguales() {
            assertThrows(LimitesInvalidosException.class,
                    () -> servicio.calcular(solicitud("x", "x^2", 1, 1)));
        }
    }

    @Nested
    @DisplayName("Contenido de la respuesta")
    class ContenidoDeLaRespuesta {

        @Test
        @DisplayName("incluye la explicacion paso a paso")
        void incluyePasos() {
            ResultadoIntegral resultado = servicio.calcular(solicitud("x^3", "x", -1, 1));

            assertFalse(resultado.pasos().isEmpty(), "Debe traer la explicacion paso a paso");
        }

        @Test
        @DisplayName("ya no trae el detalle por tramos ni la comparacion entre metodos")
        void sinSeccionesRetiradas() {
            ResultadoIntegral resultado = servicio.calcular(solicitud("x^3", "x", -1, 1));

            assertFalse(resultado.aMapa().containsKey("tramos"),
                    "El detalle por tramos se retiro de la aplicacion");
            assertFalse(resultado.aMapa().containsKey("comparacion"),
                    "La comparacion entre metodos se retiro de la aplicacion");
        }

        @Test
        @DisplayName("explica de donde salieron los limites cuando los busco el sistema")
        void explicaLosLimitesBuscados() {
            ResultadoIntegral resultado = servicio.calcular(conLimitesAutomaticos("x", "x^2"));
            String explicacion = String.valueOf(resultado.aMapa().get("explicacionLimites"));

            assertFalse(explicacion.isBlank(),
                    "Los limites calculados deben venir explicados, no usarse en silencio");
            assertTrue(explicacion.contains("="),
                    "La explicacion debe mostrar la ecuacion que se resolvio");
        }
    }
}
