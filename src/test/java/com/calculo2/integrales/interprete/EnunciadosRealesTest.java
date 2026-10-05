package com.calculo2.integrales.interprete;

import com.calculo2.integrales.model.EjeRotacion;
import com.calculo2.integrales.model.Frontera;
import com.calculo2.integrales.model.OrigenDato;
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
 * Enunciados copiados tal como aparecen en las tareas del curso.
 *
 * <p>Los cuatro ejercicios de aqui son los que destaparon el problema que estas pruebas
 * protegen: la aplicacion declaraba faltantes datos que el enunciado si daba, solo que sin
 * escribirlos como campos. Cada uno cubre una forma distinta de decir lo mismo:</p>
 *
 * <ul>
 *   <li>una frontera nombrada en palabras ("la ordenada 2");</li>
 *   <li>unos limites que hay que sacar resolviendo una ecuacion;</li>
 *   <li>una region que el propio eje de giro cierra;</li>
 *   <li>un metodo que el enunciado exige aplicar.</li>
 * </ul>
 *
 * <p>Lo que se comprueba no es solo que salga el numero correcto, sino que salga por el
 * camino correcto: que una frontera no se confunda con un eje, que un dato interpretado se
 * marque como tal, y que lo unico que se pida sea lo que de verdad no se puede saber.</p>
 */
@DisplayName("Enunciados reales de tareas")
class EnunciadosRealesTest {

    private final AnalizadorProblema analizador = new AnalizadorProblema();

    // ==================================================================
    // CASO 1
    // ==================================================================

    @Nested
    @DisplayName("Caso 1: \"y la ordenada 2\"")
    class LaOrdenadaDos {

        private ProblemaEstructurado analizado() {
            return analizador.analizar(
                    "Hallar el volumen generado por la rotacion limitada por "
                            + "y = 4x - x^2 y la ordenada 2.");
        }

        @Test
        @DisplayName("identifica el tema y la funcion")
        void temaYFuncion() {
            ProblemaEstructurado problema = analizado();

            assertEquals(TemaProblema.VOLUMEN_SOLIDO, problema.tema());
            assertEquals("4x - x^2", problema.funcionF());
        }

        @Test
        @DisplayName("lee \"la ordenada 2\" como la frontera x = 2")
        void laOrdenadaEsUnaFrontera() {
            ProblemaEstructurado problema = analizado();

            assertEquals(1, problema.fronteras().size(),
                    "El enunciado nombra una sola recta");

            Frontera frontera = problema.fronteras().get(0);
            assertTrue(frontera.esVertical());
            assertEquals(2.0, frontera.valor(), 1e-9);
        }

        @Test
        @DisplayName("marca esa lectura como interpretada, no como dato escrito")
        void laLecturaSeMarcaComoInterpretada() {
            // No esta escrita como ecuacion: sale de entender que significa "ordenada".
            // Presentarla con la misma firmeza que un dato escrito esconderia justo la
            // parte que el estudiante deberia revisar.
            assertEquals(OrigenDato.INTERPRETADO, analizado().fronteras().get(0).origen());
        }

        @Test
        @DisplayName("no confunde esa frontera con el eje de revolucion")
        void laFronteraNoEsElEje() {
            // Es el error que esta prueba existe para impedir. La recta x = 2 cierra la
            // region; de alrededor de que gira, el enunciado no dice nada.
            assertNull(analizado().eje(),
                    "x = 2 limita la region, no dice alrededor de que rota");
        }

        @Test
        @DisplayName("deduce el intervalo [0, 2] con la recta y el corte de la curva")
        void deduceElIntervalo() {
            ProblemaEstructurado problema = analizado();

            // La parabola corta el eje X en 0 y en 4; con la recta en 2, la region que
            // queda cerrada va de 0 a 2.
            assertEquals(0.0, problema.limiteInferior(), 1e-6);
            assertEquals(2.0, problema.limiteSuperior(), 1e-6);
        }

        @Test
        @DisplayName("pide unicamente el eje, no el intervalo")
        void pideSoloLoQueFalta() {
            ProblemaEstructurado problema = analizado();

            assertEquals(1, problema.faltantes().size(),
                    "Solo el eje quedo sin determinar: " + problema.faltantes());
            assertTrue(problema.faltantes().get(0).toLowerCase().contains("eje"));
        }

        @Test
        @DisplayName("avisa de que la region podia ser la otra")
        void avisaDeLaOtraLectura() {
            // La curva corta el suelo a los dos lados de x = 2, asi que [2, 4] tambien
            // encerraria region. Se elige una, pero se dice que habia dos.
            assertFalse(analizado().ambiguedades().isEmpty());
        }
    }

    // ==================================================================
    // CASO 2
    // ==================================================================

    @Nested
    @DisplayName("Caso 2: anillos con y = x^3, y = x^2")
    class MetodoDeAnillos {

        private ProblemaEstructurado analizado() {
            return analizador.analizar(
                    "Hallar el volumen generado en la rotacion del area formada "
                            + "alrededor del eje X aplicando metodo de anillos: "
                            + "y = x^3, y = x^2");
        }

        @Test
        @DisplayName("extrae las dos funciones y el eje X")
        void funcionesYEje() {
            ProblemaEstructurado problema = analizado();

            assertEquals("x^3", problema.funcionF());
            assertEquals("x^2", problema.funcionG());
            assertEquals(EjeRotacion.ejeX(), problema.eje());
        }

        @Test
        @DisplayName("calcula el intervalo [0, 1] igualando las curvas")
        void deduceLosLimites() {
            ProblemaEstructurado problema = analizado();

            // x^3 = x^2  ->  x^2(x - 1) = 0  ->  x = 0, x = 1
            assertEquals(0.0, problema.limiteInferior(), 1e-6);
            assertEquals(1.0, problema.limiteSuperior(), 1e-6);
            assertEquals(OrigenDato.DERIVADO, problema.origenDe("limites"));
        }

        @Test
        @DisplayName("no pide los limites, porque puede calcularlos")
        void noPideLoQuePuedeDeducir() {
            assertTrue(analizado().faltantes().isEmpty(),
                    "No falta nada: " + analizado().faltantes());
            assertTrue(analizado().estaCompleto());
        }

        @Test
        @DisplayName("respeta el metodo que pide el enunciado")
        void respetaElMetodoPedido() {
            ProblemaEstructurado problema = analizado();

            assertEquals(TipoSolido.ARANDELAS, problema.metodoSolicitado(),
                    "\"anillos\" y \"arandelas\" nombran el mismo metodo");
            assertEquals(TipoSolido.ARANDELAS, problema.tipoSolido());
        }

        @Test
        @DisplayName("muestra la ecuacion de la que salieron los limites")
        void explicaComoLosObtuvo() {
            // Un intervalo deducido y presentado sin su cuenta no se distingue de uno
            // inventado, que es lo que nunca debe pasar.
            String explicacion = analizado().explicacionLimites();

            assertFalse(explicacion.isBlank());
            assertTrue(explicacion.contains("f(x) = g(x)"),
                    "Debe verse la ecuacion que se planteo: " + explicacion);
        }
    }

    // ==================================================================
    // CASO 3
    // ==================================================================

    @Nested
    @DisplayName("Caso 3: eje Y y la vertical x = 3")
    class GiroSobreElEjeY {

        private ProblemaEstructurado analizado() {
            return analizador.analizar(
                    "Hallar el volumen del solido de revolucion que se genera al hacer "
                            + "girar sobre el eje Y la region comprendida entre "
                            + "y = -x^3 + 4x^2 - 3x + 1 y la vertical x = 3.");
        }

        @Test
        @DisplayName("reconoce el eje Y como eje de giro")
        void reconoceElEje() {
            assertEquals(EjeRotacion.ejeY(), analizado().eje());
        }

        @Test
        @DisplayName("reconoce x = 3 como frontera y no como eje")
        void reconoceLaFrontera() {
            ProblemaEstructurado problema = analizado();

            assertEquals(1, problema.fronteras().size());
            assertEquals(3.0, problema.fronteras().get(0).valor(), 1e-9);
            assertEquals(OrigenDato.EXPLICITO, problema.fronteras().get(0).origen());
        }

        @Test
        @DisplayName("cierra la region entre el eje de giro y esa recta")
        void deduceElIntervalo() {
            ProblemaEstructurado problema = analizado();

            // El eje Y es la recta x = 0; con la vertical x = 3 la region va de 0 a 3.
            assertEquals(0.0, problema.limiteInferior(), 1e-9);
            assertEquals(3.0, problema.limiteSuperior(), 1e-9);
        }

        @Test
        @DisplayName("no pide nada que pueda deducirse")
        void noPideNada() {
            assertTrue(analizado().faltantes().isEmpty(),
                    "No deberia faltar nada: " + analizado().faltantes());
        }

        @Test
        @DisplayName("elige capas, porque el eje es perpendicular a la variable")
        void eligeCapas() {
            // Las funciones estan en x y el eje es vertical: rebanar perpendicular al eje
            // obligaria a despejar x en funcion de y.
            assertEquals(TipoSolido.CAPAS, analizado().tipoSolido());
        }
    }

    // ==================================================================
    // CASO 4
    // ==================================================================

    @Nested
    @DisplayName("Caso 4: capas cilindricas pedidas por el enunciado")
    class CapasPedidas {

        private ProblemaEstructurado analizado() {
            return analizador.analizar(
                    "Hallar el volumen generado en la rotacion del area formada alrededor "
                            + "del eje X aplicando el metodo de capas cilindricas. Defina R "
                            + "como la region acotada por la grafica de f(x) = 3x - x^2 y el "
                            + "eje X sobre el intervalo [0,2].");
        }

        @Test
        @DisplayName("distingue el eje X de giro del eje X que cierra la region")
        void dosPapelesParaLaMismaRecta() {
            ProblemaEstructurado problema = analizado();

            // El primero va detras de "alrededor del": es el eje de giro. El segundo va
            // en la lista de lo que acota la region: es el suelo.
            assertEquals(EjeRotacion.ejeX(), problema.eje());
            assertEquals(1, problema.fronteras().size());
            assertFalse(problema.fronteras().get(0).esVertical(),
                    "El eje X como frontera es la recta horizontal y = 0");
        }

        @Test
        @DisplayName("toma el intervalo escrito entre corchetes")
        void intervaloExplicito() {
            ProblemaEstructurado problema = analizado();

            assertEquals(0.0, problema.limiteInferior(), 1e-9);
            assertEquals(2.0, problema.limiteSuperior(), 1e-9);
            assertEquals(OrigenDato.EXPLICITO, problema.origenDe("limites"));
        }

        @Test
        @DisplayName("aplica las capas que pide el enunciado, no las que sugiere la geometria")
        void respetaElMetodoAunqueLaGeometriaSugieraOtro() {
            ProblemaEstructurado problema = analizado();

            // Girando sobre el eje X una region que lo toca, la geometria pediria discos.
            // Pero el ejercicio esta practicando capas, y cambiarlo en silencio dejaria al
            // estudiante sin lo que fue a buscar.
            assertEquals(TipoSolido.CAPAS, problema.metodoSolicitado());
            assertEquals(TipoSolido.CAPAS, problema.tipoSolido());
            assertEquals(TipoSolido.DISCOS, problema.metodoSugerido());
        }

        @Test
        @DisplayName("avisa de que la geometria sugeria otro metodo")
        void loDiceEnVozAlta() {
            // Respetar el metodo no es lo mismo que callar la diferencia: si los dos no
            // dan lo mismo, hay un error en alguna parte y hay que poder verlo.
            assertTrue(analizado().avisos().stream()
                            .anyMatch(aviso -> aviso.contains("geometria")),
                    "Debe decir que por geometria correspondia otro metodo");
        }
    }

    // ==================================================================
    // VARIACIONES DE ESCRITURA
    // ==================================================================

    @Nested
    @DisplayName("La misma region escrita de varias maneras")
    class VariacionesDeEscritura {

        @Test
        @DisplayName("f(x) = ... y y = ... nombran lo mismo")
        void dosFormasDeDeclararUnaFuncion() {
            ProblemaEstructurado conEfe = analizador.analizar(
                    "Area bajo f(x) = x^2 en [0, 3]");
            ProblemaEstructurado conYe = analizador.analizar(
                    "Area bajo y = x^2 en [0, 3]");

            assertEquals(conEfe.funcionF(), conYe.funcionF());
            assertEquals(conEfe.tema(), conYe.tema());
        }

        @Test
        @DisplayName("los superindices se leen igual que el acento circunflejo")
        void superindices() {
            ProblemaEstructurado problema = analizador.analizar(
                    "Hallar el volumen al girar y = x² alrededor del eje X en [0, 1]");

            assertNotNull(problema.eje());
            assertTrue(problema.funcionF().contains("x"),
                    "Debe leerse la funcion: " + problema.funcionF());
        }

        @Test
        @DisplayName("\"anillos\" y \"arandelas\" seleccionan el mismo metodo")
        void sinonimosDelMetodo() {
            String base = "Volumen al girar y = x^2, y = x alrededor del eje X "
                    + "por el metodo de ";

            assertEquals(
                    analizador.analizar(base + "anillos").metodoSolicitado(),
                    analizador.analizar(base + "arandelas").metodoSolicitado());
        }
    }
}
