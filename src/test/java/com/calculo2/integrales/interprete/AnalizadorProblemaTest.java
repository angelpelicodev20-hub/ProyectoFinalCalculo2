package com.calculo2.integrales.interprete;

import com.calculo2.integrales.model.ProblemaEstructurado;
import com.calculo2.integrales.model.TemaProblema;
import com.calculo2.integrales.model.TipoSolido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del analizador de enunciados.
 *
 * <p>Lo que mas se comprueba es que el analisis no dependa de una redaccion concreta: el
 * mismo ejercicio escrito de varias maneras debe dar los mismos datos. Tambien se
 * comprueba lo contrario de lo habitual, que es que el sistema deje vacio lo que el
 * enunciado no dice en lugar de rellenarlo.</p>
 */
@DisplayName("Analizador de enunciados")
class AnalizadorProblemaTest {

    private final AnalizadorProblema analizador = new AnalizadorProblema();

    @Nested
    @DisplayName("El ejercicio de referencia del proyecto")
    class EjercicioDeReferencia {

        private ProblemaEstructurado analizado() {
            return analizador.analizar("""
                    Encuentre el volumen del solido que se genera al rotar
                    f(x) = -x^2+4 y g(x) = 2x +1 sobre el eje x en [0,1]
                    """);
        }

        @Test
        @DisplayName("identifica el tema como volumen de solidos de revolucion")
        void identificaElTema() {
            assertEquals(TemaProblema.VOLUMEN_SOLIDO, analizado().tema());
        }

        @Test
        @DisplayName("extrae las dos funciones sin arrastrar palabras del enunciado")
        void extraeLasFunciones() {
            ProblemaEstructurado problema = analizado();

            assertEquals("-x^2+4", problema.funcionF());
            assertEquals("2x +1", problema.funcionG());
        }

        @Test
        @DisplayName("extrae el intervalo")
        void extraeElIntervalo() {
            ProblemaEstructurado problema = analizado();

            assertEquals(0.0, problema.limiteInferior(), 1e-9);
            assertEquals(1.0, problema.limiteSuperior(), 1e-9);
        }

        @Test
        @DisplayName("extrae el eje de revolucion")
        void extraeElEje() {
            ProblemaEstructurado problema = analizado();

            assertNotNull(problema.eje());
            assertTrue(problema.eje().esHorizontal(), "El eje X es una recta horizontal");
            assertEquals(0.0, problema.eje().desplazamiento(), 1e-9);
        }

        @Test
        @DisplayName("elige arandelas porque la region no toca el eje")
        void eligeElMetodo() {
            assertEquals(TipoSolido.ARANDELAS, analizado().tipoSolido());
        }

        @Test
        @DisplayName("queda completo, sin datos faltantes")
        void quedaCompleto() {
            ProblemaEstructurado problema = analizado();

            assertTrue(problema.faltantes().isEmpty(),
                    "No deberia faltar nada: " + problema.faltantes());
            assertTrue(problema.estaCompleto());
        }

        @Test
        @DisplayName("explica en que se baso para cada conclusion")
        void explicaSusConclusiones() {
            assertFalse(analizado().evidencias().isEmpty(),
                    "La interpretacion debe poder revisarse");
        }
    }

    @Nested
    @DisplayName("No depende de la redaccion exacta")
    class VariacionesDeRedaccion {

        @Test
        @DisplayName("acepta 'girar', 'alrededor del eje X' y el intervalo en palabras")
        void otraRedaccion() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Calcule el volumen que resulta de girar la region entre "
                            + "f(x) = -x^2+4 y g(x) = 2x+1 alrededor del eje X, de 0 a 1.");

            assertEquals(TemaProblema.VOLUMEN_SOLIDO, problema.tema());
            assertEquals("-x^2+4", problema.funcionF());
            assertEquals("2x+1", problema.funcionG());
            assertEquals(0.0, problema.limiteInferior(), 1e-9);
            assertEquals(1.0, problema.limiteSuperior(), 1e-9);
            assertEquals(TipoSolido.ARANDELAS, problema.tipoSolido());
        }

        @Test
        @DisplayName("acepta los exponentes escritos en superindice")
        void conSuperindices() {
            // El superindice va escrito con su codigo unicode para que la prueba no
            // dependa de como guarde el editor este archivo.
            ProblemaEstructurado problema = analizador.analizar(
                    "Encuentre el volumen al rotar f(x) = -x² + 4 y g(x) = 2x + 1 "
                            + "sobre el eje x en [0,1]");

            assertEquals(TemaProblema.VOLUMEN_SOLIDO, problema.tema());
            assertEquals(0.0, problema.limiteInferior(), 1e-9);
            assertTrue(problema.tieneSegundaFuncion());

            // La funcion completa, no solo su primer caracter: el exponente en
            // superindice no debe cortar la lectura de la expresion.
            assertEquals("-x² + 4", problema.funcionF());
            assertEquals("2x + 1", problema.funcionG());
        }

        @Test
        @DisplayName("la funcion con superindice sigue siendo evaluable")
        void elSuperindiceSeParsea() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Area bajo la curva f(x) = x² en [0,3]");

            double valor = com.calculo2.integrales.math.parser.EvaluadorExpresion
                    .compilar(problema.funcionF()).evaluar(4);

            assertEquals(16.0, valor, 1e-9,
                    "Lo que se guarda en el campo tiene que poder volver al parser");
        }

        @Test
        @DisplayName("acepta 'desde ... hasta ...'")
        void intervaloDesdeHasta() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Area bajo la curva f(x) = x^2 desde 0 hasta 3");

            assertEquals(0.0, problema.limiteInferior(), 1e-9);
            assertEquals(3.0, problema.limiteSuperior(), 1e-9);
        }
    }

    @Nested
    @DisplayName("Identificacion de los temas de area")
    class TemasDeArea {

        @Test
        @DisplayName("una sola funcion es area bajo la curva")
        void unaFuncion() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Calcule el area bajo la curva f(x) = x^2 en [0,1]");

            assertEquals(TemaProblema.AREA_BAJO_CURVA, problema.tema());
            assertEquals("x^2", problema.funcionF());
        }

        @Test
        @DisplayName("dos funciones son area entre curvas, aunque no diga 'entre'")
        void dosFunciones() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Calcule el area de la region limitada por f(x) = x y g(x) = x^2");

            assertEquals(TemaProblema.AREA_ENTRE_CURVAS, problema.tema());
        }

        @Test
        @DisplayName("el numero de fronteras decide, no la palabra 'area'")
        void decideLaEstructura() {
            ProblemaEstructurado unaCurva = analizador.analizar("y = x^2 de 0 a 2");
            ProblemaEstructurado dosCurvas = analizador.analizar("y = x^2 y g(x) = x de 0 a 2");

            assertEquals(TemaProblema.AREA_BAJO_CURVA, unaCurva.tema());
            assertEquals(TemaProblema.AREA_ENTRE_CURVAS, dosCurvas.tema());
        }
    }

    @Nested
    @DisplayName("Datos que faltan")
    class DatosQueFaltan {

        @Test
        @DisplayName("detecta que falta el intervalo sin inventarlo")
        void faltaElIntervalo() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Calcule el volumen de y = x^2 + 1 al girarlo alrededor del eje X");

            assertEquals(TemaProblema.VOLUMEN_SOLIDO, problema.tema());
            assertEquals("x^2 + 1", problema.funcionF());
            assertNotNull(problema.eje());

            assertNull(problema.limiteInferior(), "No debe inventar el limite inferior");
            assertNull(problema.limiteSuperior(), "No debe inventar el limite superior");

            assertTrue(problema.faltantes().stream()
                            .anyMatch(falta -> falta.toLowerCase().contains("intervalo")),
                    "Debe avisar que falta el intervalo: " + problema.faltantes());
        }

        @Test
        @DisplayName("detecta que falta el eje de revolucion")
        void faltaElEje() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Calcule el volumen del solido generado por f(x) = x^2 en [0,2]");

            assertEquals(TemaProblema.VOLUMEN_SOLIDO, problema.tema());
            assertNull(problema.eje(), "No debe suponer un eje que el enunciado no dice");
            assertFalse(problema.faltantes().isEmpty());
        }

        @Test
        @DisplayName("sin funciones no arriesga un tema")
        void sinFunciones() {
            ProblemaEstructurado problema = analizador.analizar("Resuelva el ejercicio 4");

            assertEquals(TemaProblema.DESCONOCIDO, problema.tema());
            assertFalse(problema.estaCompleto());
        }

        @Test
        @DisplayName("con el enunciado vacio no inventa nada")
        void enunciadoVacio() {
            ProblemaEstructurado problema = analizador.analizar("   ");

            assertEquals(TemaProblema.DESCONOCIDO, problema.tema());
            assertEquals("", problema.funcionF());
            assertNull(problema.limiteInferior());
        }
    }

    @Nested
    @DisplayName("Eleccion del metodo por la geometria")
    class EleccionDelMetodo {

        @Test
        @DisplayName("una curva que toca el eje da discos")
        void discos() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Volumen al rotar f(x) = sqrt(x) sobre el eje x en [0,4]");

            assertEquals(TipoSolido.DISCOS, problema.tipoSolido());
        }

        @Test
        @DisplayName("dos curvas separadas del eje dan arandelas")
        void arandelas() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Volumen al rotar f(x) = 3 y g(x) = 1 sobre el eje x en [0,2]");

            assertEquals(TipoSolido.ARANDELAS, problema.tipoSolido());
        }

        @Test
        @DisplayName("un eje perpendicular a la variable da capas cilindricas")
        void capas() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Volumen al rotar f(x) = x^2 alrededor del eje Y en [0,1]");

            assertEquals(TipoSolido.CAPAS, problema.tipoSolido());
        }

        @Test
        @DisplayName("una recta corrida separa la region del eje y da arandelas")
        void ejeCorrido() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Volumen al rotar f(x) = x^2 alrededor de la recta y = -1 en [0,1]");

            assertNotNull(problema.eje());
            assertEquals(-1.0, problema.eje().desplazamiento(), 1e-9);
            assertEquals(TipoSolido.ARANDELAS, problema.tipoSolido());
        }
    }
}
