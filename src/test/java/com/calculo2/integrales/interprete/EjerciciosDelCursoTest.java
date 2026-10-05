package com.calculo2.integrales.interprete;

import com.calculo2.integrales.math.integracion.SumaRiemann;
import com.calculo2.integrales.model.EjeRotacion;
import com.calculo2.integrales.model.MetodoArea;
import com.calculo2.integrales.model.OrigenDato;
import com.calculo2.integrales.model.ProblemaEstructurado;
import com.calculo2.integrales.model.ResultadoIntegral;
import com.calculo2.integrales.model.ResultadoSolido;
import com.calculo2.integrales.model.SolicitudIntegral;
import com.calculo2.integrales.model.SolicitudSolido;
import com.calculo2.integrales.model.TemaProblema;
import com.calculo2.integrales.model.TipoSolido;
import com.calculo2.integrales.service.AreaBajoCurvaService;
import com.calculo2.integrales.service.AreaEntreCurvasService;
import com.calculo2.integrales.service.SolidoRevolucionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los ejercicios de la lista del curso, copiados tal como se reparten.
 *
 * <p>Son los que la aplicacion no lograba resolver: llevan guiones largos, exponentes
 * escritos como "x2", curvas sin despejar ({@code y^2 = 8x}, la elipse), funciones de y
 * ({@code x = y^2}), intervalos en palabras ("x menor e igual q 1") y ejes nombrados de
 * todas las formas posibles ("al rotar y = 0", "haciendo rotar el eje y", "con respecto
 * a esa recta").</p>
 *
 * <p>Cada uno se analiza y se resuelve con los datos que deja el analizador, que es el
 * mismo camino que sigue el boton Resolver de la pagina. El volumen esperado es el del
 * libro.</p>
 */
@DisplayName("Ejercicios de la lista del curso")
class EjerciciosDelCursoTest {

    private final AnalizadorProblema analizador = new AnalizadorProblema();

    static Stream<Arguments> volumenes() {
        double pi = Math.PI;
        return Stream.of(
                Arguments.of("Encontrar el volumen generado por la gráfica y = x3 – x , el eje x al rotar y = 0",
                        16 * pi / 105),
                Arguments.of("Calcular el volumen del sólido generado al girar, en torno de la recta x = 2, la región "
                        + "Limitada por las gráficas de y = x^3 + x + 1,   y = 1  y  x = 1", 29 * pi / 15),
                Arguments.of("Calcular el volumen de un sólido de revolución engendrado por la región limitada "
                        + "y = 1/ (x2 + 1)^2 y el eje x ( x menor e igual a 1 y x mayor e igual a 0 )",
                        pi * (11.0 / 48 + 5 * pi / 64)),
                Arguments.of("Calcular el volumen del sólido de revolución que se genera al girar la región "
                        + "limitada por Y = x – x^3 y el eje x ( x menor e igual q 1 y x mayor e igual q 0)",
                        8 * pi / 105),
                Arguments.of("Encontrar el volumen del sólido de revolución generado al hacer girar sobre el eje x "
                        + "la Región encerrada en el primer cuadrante por la elipse 4x² + 9y ²=36 y los ejes "
                        + "coordenados", 8 * pi),
                Arguments.of("Encontrar el volumen del sólido generado al girar sobre el eje y la región limitada "
                        + "por la curva y = x^3,  el eje y y la recta y = 3", 9 * pi / 5 * Math.cbrt(9)),
                Arguments.of("Encontrar el volumen generado al girar sobre el eje x la región encerrada por las "
                        + "parábolas y = x ^2 , y^2 = 8x", 48 * pi / 5),
                Arguments.of("Encontrar el volumen generado por las gráficas x = y^2 , x = y + 6 haciendo rotar "
                        + "el eje y", 500 * pi / 3),
                Arguments.of("Calcular el volumen del sólido generado al girar, alrededor de la recta x = 1, la "
                        + "región Limitada por la curva (x – 1)^2 = 20 – 4y y las rectas x = 1, y = 1, y = 3",
                        24 * pi),
                Arguments.of("Hallar el volumen al girar el área limitada por la parábola y^2 = 8x y la ordenada "
                        + "correspondiente a x = 2 con respecto al eje y", 128 * pi / 5),
                Arguments.of("Hallar el volumen generado el la rotación del área comprendida entre la parábola "
                        + "y = 4x – x^2 y el eje x con respecto a la recta y = 6", 1408 * pi / 15),
                Arguments.of("Hallar el volumen generado el la rotación del área comprendida entre la parábola "
                        + "y^2 = 8x y la ordenada correspondiente a x = 2 con respecto a esa recta (método de anillo)",
                        256 * pi / 15),
                Arguments.of("Encontrar el Volumen engendrado al girar sobre el eje y, la región del primer cuadrante "
                        + "Situada por encima de la parábola y = x2 y por debajo de la parábola y = 2 – x2", pi),
                Arguments.of("Encontrar el volumen de un sólido de revolución engendrado al girar sobre el eje y "
                        + "la región limitada por la curva y = (x – 1) ^3, el eje x, y la recta x = 2",
                        9 * pi / 10),
                Arguments.of("Encontrar el volumen del sólido generado por las gráficas y = 4 – x2 , "
                        + "4y = 4 – x2 al hacer rotar el eje x", 32 * pi),
                Arguments.of("Encontrar el volumen del sólido generado por las gráficas y = x^2 , y^2 = 8x al "
                        + "hacer rotar el eje x.", 48 * pi / 5),
                Arguments.of("Encontrar el volumen generado en la rotación del área del primer cuadrante "
                        + "limitada Por la parábola y^2 = 8x y la ordenada correspondiente a x = 2 con respecto "
                        + "al eje x", 16 * pi));
    }

    @ParameterizedTest(name = "{index}: V = {1}")
    @MethodSource("volumenes")
    @DisplayName("se interpreta completo y el volumen es el del libro")
    void volumenDelLibro(String enunciado, double esperado) {
        ProblemaEstructurado problema = analizador.analizar(enunciado);

        assertEquals(TemaProblema.VOLUMEN_SOLIDO, problema.tema());
        assertTrue(problema.estaCompleto(), "Deberia poder resolverse: " + problema.faltantes());

        ResultadoSolido resultado = new SolidoRevolucionService().calcular(solicitud(problema));
        assertEquals(esperado, resultado.volumen(), 1e-6 * Math.max(1.0, esperado));
    }

    // ==================================================================
    // DETALLES DE LA LECTURA
    // ==================================================================

    @Nested
    @DisplayName("Lectura de cada enunciado")
    class Lectura {

        @Test
        @DisplayName("\"x3 - x\" con guion largo es x al cubo menos x, y \"al rotar y = 0\" es el eje")
        void guionLargoYExponentePegado() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Encontrar el volumen generado por la gráfica y = x3 – x , el eje x al rotar y = 0");

            assertEquals("x^3 - x", problema.funcionF());
            assertEquals(EjeRotacion.ejeX(), problema.eje());
            assertEquals(-1.0, problema.limiteInferior(), 1e-9);
            assertEquals(1.0, problema.limiteSuperior(), 1e-9);
        }

        @Test
        @DisplayName("la recta del eje y la frontera x = 1 no se confunden")
        void ejeYFronteraDistintos() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Calcular el volumen del sólido generado al girar, en torno de la recta x = 2, la región "
                            + "Limitada por las gráficas de y = x^3 + x + 1,   y = 1  y  x = 1");

            assertEquals(EjeRotacion.vertical(2), problema.eje());
            assertEquals(0.0, problema.limiteInferior(), 1e-9);
            assertEquals(1.0, problema.limiteSuperior(), 1e-9);
            assertEquals(TipoSolido.CAPAS, problema.tipoSolido());
        }

        @Test
        @DisplayName("\"x menor e igual q 1 y x mayor e igual q 0\" es el intervalo [0, 1]")
        void desigualdadesEnPalabras() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Calcular el volumen del sólido de revolución que se genera al girar la región "
                            + "limitada por Y = x – x^3 y el eje x ( x menor e igual q 1 y x mayor e igual q 0)");

            assertEquals(0.0, problema.limiteInferior(), 1e-9);
            assertEquals(1.0, problema.limiteSuperior(), 1e-9);
            assertEquals(OrigenDato.EXPLICITO, problema.origenDe("limites"));
        }

        @Test
        @DisplayName("sin eje escrito, toma el eje X que nombra y lo marca como interpretado")
        void ejeNoEscrito() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Calcular el volumen del sólido de revolución que se genera al girar la región "
                            + "limitada por Y = x – x^3 y el eje x ( x menor e igual q 1 y x mayor e igual q 0)");

            assertEquals(EjeRotacion.ejeX(), problema.eje());
            assertEquals(OrigenDato.INTERPRETADO, problema.origenDe("eje"));
            assertFalse(problema.ambiguedades().isEmpty(), "Debe avisar que el eje se interpreto");
        }

        @Test
        @DisplayName("despeja la elipse y usa la rama del primer cuadrante")
        void elipse() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Encontrar el volumen del sólido de revolución generado al hacer girar sobre el eje x "
                            + "la Región encerrada en el primer cuadrante por la elipse 4x² + 9y ²=36 y los "
                            + "ejes coordenados");

            assertEquals("(2/3)sqrt(9 - x^2)", problema.funcionF());
            assertEquals(0.0, problema.limiteInferior(), 1e-9);
            assertEquals(3.0, problema.limiteSuperior(), 1e-9);
            assertTrue(problema.explicacionRegion().contains("Despejando y"));
        }

        @Test
        @DisplayName("x = y^2 y x = y + 6 se integran respecto de y")
        void funcionesDeY() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Encontrar el volumen generado por las gráficas x = y^2 , x = y + 6 haciendo rotar el eje y");

            assertEquals("y", problema.variable());
            assertEquals(EjeRotacion.ejeY(), problema.eje());
            assertEquals(-2.0, problema.limiteInferior(), 1e-9);
            assertEquals(3.0, problema.limiteSuperior(), 1e-9);
            assertEquals(TipoSolido.ARANDELAS, problema.tipoSolido());
        }

        @Test
        @DisplayName("\"con respecto a esa recta\" toma como eje la ordenada nombrada antes")
        void referenciaAEsaRecta() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Hallar el volumen generado el la rotación del área comprendida entre la parábola "
                            + "y^2 = 8x y la ordenada correspondiente a x = 2 con respecto a esa recta (método de "
                            + "anillo)");

            assertEquals(EjeRotacion.vertical(2), problema.eje());
            assertEquals(1, problema.fronteras().size(), "x = 2 sigue siendo frontera");
            assertEquals(TipoSolido.ARANDELAS, problema.tipoSolido(), "Se respeta el metodo pedido");
            assertEquals("y", problema.variable(), "Arandelas con eje vertical se integran en y");
        }

        @Test
        @DisplayName("el primer cuadrante recorta la region entre las dos parabolas")
        void primerCuadrante() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Encontrar el Volumen engendrado al girar sobre el eje y, la región del primer cuadrante "
                            + "Situada por encima de la parábola y = x2 y por debajo de la parábola y = 2 – x2");

            assertEquals(0.0, problema.limiteInferior(), 1e-9);
            assertEquals(1.0, problema.limiteSuperior(), 1e-9);
        }

        @Test
        @DisplayName("con una sola curva y el eje de giro no hay region cerrada, y lo dice")
        void regionSinCerrar() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Encuentre el volumen de la región limitada por f(x) = x2 + 1, alrededor de la recta x = 3");

            assertEquals("x^2 + 1", problema.funcionF());
            assertEquals(EjeRotacion.vertical(3), problema.eje());
            assertNull(problema.limiteInferior(), "No debe inventar limites");
            assertTrue(problema.faltantes().stream().anyMatch(f -> f.contains("intervalo")),
                    "Debe decir que falta cerrar la region: " + problema.faltantes());
            assertTrue(problema.faltantes().get(0).contains("eje de giro"),
                    "Debe aclarar que x = 3 es el eje y no una frontera");
        }
    }

    // ==================================================================
    // AREAS
    // ==================================================================

    static Stream<Arguments> areas() {
        return Stream.of(
                Arguments.of("Calcule el área bajo la curva f(x) = x^2 en [0,1]", 1.0 / 3),
                Arguments.of("Hallar el área de la región limitada por la curva y = x^2 - 4 y el eje x", 32.0 / 3),
                Arguments.of("Hallar el área limitada por y = x^3 - 6x^2 + 8x y el eje x", 8.0),
                Arguments.of("Calcule el área entre las curvas y = x^2 y y = x + 2", 4.5),
                Arguments.of("Calcular el área de la región comprendida entre y = x^3 y y = x", 0.5),
                Arguments.of("Encontrar el área encerrada por las parábolas y = x^2 , y^2 = 8x", 8.0 / 3),
                Arguments.of("Hallar el área entre x = y^2 y x = y + 6", 125.0 / 6),
                Arguments.of("Hallar el área de la región limitada por y = sin(x) y el eje x entre 0 y pi", 2.0),
                Arguments.of("Área bajo la curva y = 4 – x2", 32.0 / 3),
                Arguments.of("Calcular el área limitada por la curva y = x^2 - 2x y la recta y = x", 4.5),
                Arguments.of("Calcule el área de la región limitada por y = 1/x, el eje x, x = 1 y x = e", 1.0),
                Arguments.of("Determine el área de la región acotada por y = x2 y y = 2x – x2", 1.0 / 3),
                Arguments.of("Calcule el área bajo la curva f(x) = x3 – x entre x = -1 y x = 1", 0.5));
    }

    @ParameterizedTest(name = "{index}: A = {1}")
    @MethodSource("areas")
    @DisplayName("las areas se interpretan y se calculan bien")
    void areaEsperada(String enunciado, double esperada) {
        ProblemaEstructurado problema = analizador.analizar(enunciado);
        assertTrue(problema.estaCompleto(), "Deberia poder resolverse: " + problema.faltantes());

        SolicitudIntegral solicitud = new SolicitudIntegral(problema.funcionF(), problema.funcionG(),
                problema.limiteInferior(), problema.limiteSuperior(), 20, MetodoArea.desdeTexto("ANALITICO"),
                SumaRiemann.Posicion.MEDIO, false, problema.explicacionLimites(), problema.variable(),
                problema.explicacionRegion(), problema.curvas().stream().map(c -> c.ecuacion()).toList());

        ResultadoIntegral resultado = problema.tema() == TemaProblema.AREA_BAJO_CURVA
                ? new AreaBajoCurvaService().calcular(solicitud)
                : new AreaEntreCurvasService().calcular(solicitud);

        assertEquals(esperada, resultado.area(), 1e-6);
    }

    @Test
    @DisplayName("el area entre curvas muestra la factorizacion y la funcion superior de cada tramo")
    void procedimientoDelAreaEntreCurvas() {
        SolicitudIntegral solicitud = new SolicitudIntegral("x^3", "x", null, null, 20,
                MetodoArea.desdeTexto("ANALITICO"), SumaRiemann.Posicion.MEDIO, true, "");
        ResultadoIntegral resultado = new AreaEntreCurvasService().calcular(solicitud);

        String pasos = resultado.pasos().toString();
        assertTrue(pasos.contains("Hallar los puntos de interseccion"));
        assertTrue(pasos.contains("x(x - 1)(x + 1) = 0"), "Debe factorizar: " + pasos);
        assertTrue(pasos.contains("Identificar la funcion superior"));
        assertEquals(0.5, resultado.area(), 1e-9);
    }

    // ==================================================================
    // APOYO
    // ==================================================================

    /** La misma solicitud que arma la pagina Resolver con los campos que lleno el analizador. */
    private static SolicitudSolido solicitud(ProblemaEstructurado problema) {
        return new SolicitudSolido(problema.funcionF(), problema.funcionG(),
                problema.limiteInferior(), problema.limiteSuperior(), 2000,
                problema.tipoSolido(), problema.eje(), false, problema.explicacionLimites(),
                problema.explicacionRegion(),
                problema.curvas().stream().map(c -> c.ecuacion()).toList());
    }
}
