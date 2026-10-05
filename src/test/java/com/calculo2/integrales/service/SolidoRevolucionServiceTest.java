package com.calculo2.integrales.service;

import com.calculo2.integrales.exception.LimitesInvalidosException;
import com.calculo2.integrales.model.EjeRotacion;
import com.calculo2.integrales.model.ResultadoSolido;
import com.calculo2.integrales.model.SolicitudSolido;
import com.calculo2.integrales.model.TipoSolido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del tema de solidos de revolucion.
 *
 * <p>Incluye los dos ejemplos que trae el material del curso y varios solidos cuyo volumen
 * se conoce por geometria elemental — cono, esfera, cilindro —, que son la mejor forma de
 * verificar que las formulas estan bien aplicadas: si al girar {@code y = x} sale algo
 * distinto del volumen de un cono, el error salta de inmediato.</p>
 */
@DisplayName("Servicio de solidos de revolucion")
class SolidoRevolucionServiceTest {

    private static final int PARTICIONES = 2000;

    private final SolidoRevolucionService servicio = new SolidoRevolucionService();

    private SolicitudSolido discos(String funcion, double a, double b, EjeRotacion eje) {
        return new SolicitudSolido(funcion, "", a, b, PARTICIONES,
                TipoSolido.DISCOS, eje, false, "");
    }

    private SolicitudSolido arandelas(String exterior, String interior,
                                      double a, double b, EjeRotacion eje) {
        return new SolicitudSolido(exterior, interior, a, b, PARTICIONES,
                TipoSolido.ARANDELAS, eje, false, "");
    }

    private SolicitudSolido capas(String exterior, String interior,
                                  double a, double b, EjeRotacion eje) {
        return new SolicitudSolido(exterior, interior, a, b, PARTICIONES,
                TipoSolido.CAPAS, eje, false, "");
    }

    @Nested
    @DisplayName("Ejemplos del material del curso")
    class EjemplosDelCurso {

        @Test
        @DisplayName("y = sqrt(x) girando sobre el eje X hasta x = 4 da 8*pi")
        void ejemploDeDiscos() {
            // V = pi * integral de 0 a 4 de x dx = pi * 8
            ResultadoSolido resultado = servicio.calcular(
                    discos("sqrt(x)", 0, 4, EjeRotacion.ejeX()));

            assertEquals(8.0 * Math.PI, resultado.volumen(), 1e-4);
        }

        @Test
        @DisplayName("la region entre y = x y y = x^2 girando sobre el eje X da 2*pi/15")
        void ejemploDeArandelas() {
            // V = pi * integral de 0 a 1 de (x^2 - x^4) dx = pi * (1/3 - 1/5) = 2*pi/15
            ResultadoSolido resultado = servicio.calcular(
                    arandelas("x", "x^2", 0, 1, EjeRotacion.ejeX()));

            assertEquals(2.0 * Math.PI / 15.0, resultado.volumen(), 1e-5);
        }
    }

    @Nested
    @DisplayName("Solidos de volumen conocido por geometria")
    class SolidosConocidos {

        @Test
        @DisplayName("y = x de 0 a 3 genera un cono de volumen 9*pi")
        void cono() {
            // Cono de radio 3 y altura 3: V = (1/3)*pi*r^2*h = 9*pi
            ResultadoSolido resultado = servicio.calcular(
                    discos("x", 0, 3, EjeRotacion.ejeX()));

            assertEquals(9.0 * Math.PI, resultado.volumen(), 1e-5);
        }

        @Test
        @DisplayName("y = sqrt(4 - x^2) de -2 a 2 genera una esfera de volumen 32*pi/3")
        void esfera() {
            // Esfera de radio 2: V = (4/3)*pi*r^3 = 32*pi/3
            ResultadoSolido resultado = servicio.calcular(
                    discos("sqrt(4 - x^2)", -2, 2, EjeRotacion.ejeX()));

            assertEquals(32.0 * Math.PI / 3.0, resultado.volumen(), 1e-2);
        }

        @Test
        @DisplayName("y = 2 de 0 a 5 genera un cilindro de volumen 20*pi")
        void cilindro() {
            // Cilindro de radio 2 y altura 5: V = pi*r^2*h = 20*pi
            ResultadoSolido resultado = servicio.calcular(
                    discos("2", 0, 5, EjeRotacion.ejeX()));

            assertEquals(20.0 * Math.PI, resultado.volumen(), 1e-6);
        }

        @Test
        @DisplayName("un tubo entre radios 1 y 2 tiene volumen 3*pi por unidad de largo")
        void tubo() {
            // V = pi * (2^2 - 1^2) * 1 = 3*pi
            ResultadoSolido resultado = servicio.calcular(
                    arandelas("2", "1", 0, 1, EjeRotacion.ejeX()));

            assertEquals(3.0 * Math.PI, resultado.volumen(), 1e-6);
        }
    }

    @Nested
    @DisplayName("Metodo de capas cilindricas")
    class Capas {

        @Test
        @DisplayName("y = x^2 de 0 a 1 girando sobre el eje Y da pi/2")
        void capasSobreElEjeY() {
            // V = 2*pi * integral de 0 a 1 de x * x^2 dx = 2*pi * (1/4) = pi/2
            ResultadoSolido resultado = servicio.calcular(
                    capas("x^2", "", 0, 1, EjeRotacion.ejeY()));

            assertEquals(Math.PI / 2.0, resultado.volumen(), 1e-5);
        }

        @Test
        @DisplayName("y = x de 0 a 1 girando sobre el eje Y da 2*pi/3")
        void conoInvertidoPorCapas() {
            // V = 2*pi * integral de 0 a 1 de x * x dx = 2*pi/3
            ResultadoSolido resultado = servicio.calcular(
                    capas("x", "", 0, 1, EjeRotacion.ejeY()));

            assertEquals(2.0 * Math.PI / 3.0, resultado.volumen(), 1e-5);
        }

        @Test
        @DisplayName("avisa cuando el eje de giro atraviesa la region")
        void ejeQueAtraviesaLaRegion() {
            ResultadoSolido resultado = servicio.calcular(
                    capas("x^2", "", -1, 1, EjeRotacion.ejeY()));

            String advertencia = String.valueOf(resultado.aMapa().get("advertencia"));
            assertFalse(advertencia.isBlank(),
                    "Debe advertir que las capas se superponen al girar");
        }
    }

    @Nested
    @DisplayName("Eje de giro corrido")
    class EjeCorrido {

        @Test
        @DisplayName("y = 3 de 0 a 2 girando sobre y = 1 da un cilindro de radio 2")
        void ejeHorizontalCorrido() {
            // Radio = |3 - 1| = 2, altura 2: V = pi * 4 * 2 = 8*pi
            ResultadoSolido resultado = servicio.calcular(
                    discos("3", 0, 2, EjeRotacion.horizontal(1)));

            assertEquals(8.0 * Math.PI, resultado.volumen(), 1e-6);
        }

        @Test
        @DisplayName("el radio se mide desde el eje corrido, no desde el eje X")
        void elRadioCambiaConElEje() {
            double sobreElEjeX = servicio.calcular(
                    discos("3", 0, 2, EjeRotacion.ejeX())).volumen();
            double sobreLaRecta = servicio.calcular(
                    discos("3", 0, 2, EjeRotacion.horizontal(1))).volumen();

            assertTrue(sobreLaRecta < sobreElEjeX,
                    "Acercar el eje a la curva reduce el radio y por tanto el volumen");
        }
    }

    @Nested
    @DisplayName("Contenido de la respuesta")
    class ContenidoDeLaRespuesta {

        @Test
        @DisplayName("expresa el volumen como multiplo de pi cuando sale limpio")
        void volumenEnTerminosDePi() {
            ResultadoSolido resultado = servicio.calcular(
                    discos("sqrt(x)", 0, 4, EjeRotacion.ejeX()));

            assertEquals("8*pi", String.valueOf(resultado.aMapa().get("volumenEnPi")));
        }

        @Test
        @DisplayName("incluye la malla del solido para dibujarlo")
        void incluyeLaMalla() {
            ResultadoSolido resultado = servicio.calcular(
                    discos("sqrt(x)", 0, 4, EjeRotacion.ejeX()));

            assertTrue(resultado.aMapa().get("grafica") != null,
                    "Debe traer la malla del solido");
        }

        @Test
        @DisplayName("incluye la explicacion paso a paso")
        void incluyePasos() {
            ResultadoSolido resultado = servicio.calcular(
                    discos("sqrt(x)", 0, 4, EjeRotacion.ejeX()));

            assertFalse(resultado.pasos().isEmpty(), "Debe traer los pasos de la solucion");
        }

        @Test
        @DisplayName("calcula tambien el area de la region que se hizo girar")
        void incluyeElAreaDeLaRegion() {
            // Area bajo sqrt(x) de 0 a 4 = 16/3
            ResultadoSolido resultado = servicio.calcular(
                    discos("sqrt(x)", 0, 4, EjeRotacion.ejeX()));

            assertEquals(16.0 / 3.0, resultado.areaRegion(), 1e-4);
        }
    }

    @Nested
    @DisplayName("Interpretacion del eje que llega del formulario")
    class EjeDesdeElFormulario {

        /**
         * El formulario manda el eje como texto, no como objeto, asi que este es el
         * camino que se recorre de verdad al usar la aplicacion. Los demas casos de
         * prueba construyen el eje a mano y no pasarian por aqui.
         */
        private SolicitudSolido desdeTexto(String funcion, String textoEje,
                                           double k, TipoSolido tipo) {
            return new SolicitudSolido(funcion, "", 0.0, 4.0, PARTICIONES,
                    tipo, EjeRotacion.desdeTexto(textoEje, k), false, "");
        }

        @Test
        @DisplayName("'x' sin signo igual nombra el eje X, que es horizontal")
        void laLetraSolaNombraUnEjeCoordenado() {
            ResultadoSolido resultado = servicio.calcular(
                    desdeTexto("sqrt(x)", "x", 0, TipoSolido.DISCOS));

            assertEquals(8.0 * Math.PI, resultado.volumen(), 1e-4);
        }

        @Test
        @DisplayName("'x = 2' con signo igual es una recta vertical")
        void laEcuacionConIgualNombraUnaRecta() {
            EjeRotacion eje = EjeRotacion.desdeTexto("x = 2", 0);

            assertFalse(eje.esHorizontal(), "x = 2 es una recta vertical");
            assertEquals(2.0, eje.desplazamiento(), 1e-9);
        }

        @Test
        @DisplayName("'y = 1' es una recta horizontal corrida")
        void rectaHorizontalCorrida() {
            EjeRotacion eje = EjeRotacion.desdeTexto("y = 1", 0);

            assertTrue(eje.esHorizontal(), "y = 1 es una recta horizontal");
            assertEquals(1.0, eje.desplazamiento(), 1e-9);
        }

        @Test
        @DisplayName("'y' sin signo igual nombra el eje Y, que es vertical")
        void ejeYDesdeTexto() {
            assertFalse(EjeRotacion.desdeTexto("y", 0).esHorizontal());
        }
    }

    @Nested
    @DisplayName("Cual curva hace de radio exterior")
    class OrdenDeLosRadios {

        /**
         * En [0, 1] la curva x^2 va por encima de x^3, asi que es ella la que queda mas
         * lejos del eje X y la que da el radio exterior. El estudiante puede escribirlas
         * en cualquier orden, y el orden de escritura no cambia la geometria.
         */
        private ResultadoSolido escritasAlReves() {
            return servicio.calcular(arandelas("x^3", "x^2", 0, 1, EjeRotacion.ejeX()));
        }

        @Test
        @DisplayName("pone como exterior la curva mas lejana al eje, no la escrita primero")
        void noSeFiaDelOrdenDeEscritura() {
            ResultadoSolido resultado = escritasAlReves();

            assertEquals("x^2", resultado.aMapa().get("funcionExterior"),
                    "x^2 va por encima de x^3 en [0, 1]");
            assertEquals("x^3", resultado.aMapa().get("funcionInterior"));
        }

        @Test
        @DisplayName("el volumen sale correcto, 2*pi/35")
        void volumenCorrecto() {
            // V = pi * integral de 0 a 1 de ((x^2)^2 - (x^3)^2) dx = pi (1/5 - 1/7)
            assertEquals(Math.PI * 2.0 / 35.0, escritasAlReves().volumen(), 1e-6);
        }

        @Test
        @DisplayName("la integral no queda negativa y rescatada por un valor absoluto")
        void laIntegralNoSaleAlReves() {
            // Con los radios cambiados, R^2 - r^2 seria negativo y el valor absoluto del
            // final devolveria igualmente el numero correcto. El numero no delataria
            // nada; lo que quedaria mal es la formula escrita, el dibujo y el
            // procedimiento, que es justo lo que el estudiante viene a ver.
            String evaluacion = escritasAlReves().pasos().stream()
                    .filter(paso -> paso.titulo().contains("Evaluar"))
                    .map(com.calculo2.integrales.model.PasoSolucion::expresion)
                    .findFirst()
                    .orElse("");

            assertFalse(evaluacion.contains("-2/35"),
                    "La resta de cuadrados debe salir positiva: " + evaluacion);
        }

        @Test
        @DisplayName("explica con un punto de prueba cual va por encima")
        void muestraLaComprobacion() {
            String comprobacion = escritasAlReves().pasos().stream()
                    .filter(paso -> paso.titulo().contains("mas lejos"))
                    .map(com.calculo2.integrales.model.PasoSolucion::expresion)
                    .findFirst()
                    .orElse("");

            assertFalse(comprobacion.isBlank(),
                    "Debe haber un paso que compare las dos curvas");
            assertTrue(comprobacion.contains("x^2") && comprobacion.contains("x^3"),
                    "Debe evaluarse cada curva en el punto de prueba: " + comprobacion);
        }

        @Test
        @DisplayName("escribe el cuadrado de una potencia con parentesis")
        void parentesisEnLaPotenciaDeUnaPotencia() {
            // Sin parentesis quedaria x^3^2, que por la asociatividad de la potencia se
            // lee x^(3^2) = x^9 y no el cuadrado de x^3.
            String integral = escritasAlReves().pasos().stream()
                    .filter(paso -> paso.titulo().contains("Construir"))
                    .map(com.calculo2.integrales.model.PasoSolucion::expresion)
                    .findFirst()
                    .orElse("");

            assertTrue(integral.contains("(x^2)^2"),
                    "El radio exterior elevado al cuadrado: " + integral);
            assertFalse(integral.contains("x^2^2"),
                    "Sin parentesis la expresion significaria otra cosa: " + integral);
        }

        @Test
        @DisplayName("no avisa de un cruce cuando las curvas solo se tocan en los extremos")
        void sinFalsoAvisoDeCruce() {
            // x^3 y x^2 coinciden en 0 y en 1 pero no se cruzan entre medias. Contar ese
            // contacto como un cruce llenaria de avisos ejercicios perfectamente normales.
            String metodo = escritasAlReves().pasos().stream()
                    .filter(paso -> paso.titulo().contains("metodo"))
                    .map(com.calculo2.integrales.model.PasoSolucion::expresion)
                    .findFirst()
                    .orElse("");

            assertFalse(metodo.contains("se cruzan dentro del intervalo"),
                    "Tocarse en los extremos no es cruzarse: " + metodo);
        }
    }

    @Nested
    @DisplayName("La region que se hace girar")
    class RegionPlana {

        @Test
        @DisplayName("se dibuja con los mismos datos con que se integro")
        void mismaFuenteQueElCalculo() {
            ResultadoSolido resultado = servicio.calcular(
                    arandelas("x^3", "x^2", 0, 1, EjeRotacion.ejeX()));

            @SuppressWarnings("unchecked")
            var region = (java.util.Map<String, Object>) resultado.aMapa().get("region");

            assertEquals(0.0, (double) region.get("limiteInferior"), 1e-9);
            assertEquals(1.0, (double) region.get("limiteSuperior"), 1e-9);
        }

        @Test
        @DisplayName("incluye el eje de giro, que es desde donde se mide el radio")
        void llevaElEjeDeGiro() {
            ResultadoSolido resultado = servicio.calcular(
                    discos("x", 0, 3, EjeRotacion.horizontal(-1)));

            @SuppressWarnings("unchecked")
            var region = (java.util.Map<String, Object>) resultado.aMapa().get("region");

            assertFalse(region.get("ejeRevolucion") == null,
                    "Sin el eje dibujado no se ve desde donde se mide el radio");
        }
    }

    @Nested
    @DisplayName("Errores que debe reportar")
    class Errores {

        @Test
        @DisplayName("las arandelas exigen la segunda curva")
        void arandelasSinSegundaCurva() {
            LimitesInvalidosException error = assertThrows(LimitesInvalidosException.class,
                    () -> servicio.calcular(arandelas("x", "", 0, 1, EjeRotacion.ejeX())));

            assertTrue(error.getMessage().toLowerCase().contains("arandelas"));
        }

        @Test
        @DisplayName("rechaza un intervalo de longitud cero")
        void limitesIguales() {
            assertThrows(LimitesInvalidosException.class,
                    () -> servicio.calcular(discos("x", 2, 2, EjeRotacion.ejeX())));
        }

        @Test
        @DisplayName("explica por que no puede aplicar capas con un eje horizontal")
        void capasQueExigiriaDespejarLaFuncion() {
            // Las capas con eje horizontal se apilan en vertical, asi que hay que integrar
            // respecto de y y describir la region como x = g(y). Con "3x - x^2", escrita
            // en terminos de x, eso obligaria a despejar una cuadratica.
            //
            // Antes esto moria en el parser diciendo "no reconozco x", que es cierto pero
            // no explica nada. El estudiante necesita saber que el metodo exige un paso
            // previo y cual es el que si se puede aplicar tal como escribio el ejercicio.
            LimitesInvalidosException error = assertThrows(LimitesInvalidosException.class,
                    () -> servicio.calcular(capas("3x - x^2", "", 0, 2, EjeRotacion.ejeX())));

            String mensaje = error.getMessage().toLowerCase();

            assertTrue(mensaje.contains("despejar"),
                    "Debe decir que habria que despejar la funcion: " + mensaje);
            assertTrue(mensaje.contains("discos"),
                    "Debe nombrar el metodo que si se puede aplicar: " + mensaje);
            assertFalse(mensaje.contains("no reconozco"),
                    "No debe caerse con un error del parser: " + mensaje);
        }

        @Test
        @DisplayName("no manda a revisar los limites cuando el problema es el metodo")
        void laSugerenciaApuntaAlMetodo() {
            LimitesInvalidosException error = assertThrows(LimitesInvalidosException.class,
                    () -> servicio.calcular(capas("3x - x^2", "", 0, 2, EjeRotacion.ejeX())));

            assertTrue(error.getSugerencia().toLowerCase().contains("metodo"),
                    "Los limites estan bien; lo que hay que cambiar es el metodo: "
                            + error.getSugerencia());
        }
    }
}
