package com.calculo2.integrales.math;

import com.calculo2.integrales.math.integracion.MetodoIntegracion;
import com.calculo2.integrales.math.integracion.ReglaSimpson;
import com.calculo2.integrales.math.integracion.SumaRiemann;
import com.calculo2.integrales.math.parser.EvaluadorExpresion;
import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.math.util.BuscadorRaices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de los dos metodos numericos que quedan en el proyecto.
 *
 * <p>La aplicacion ya no ofrece un menu de metodos numericos: los ejercicios se resuelven
 * encontrando la primitiva. De todo aquello solo sobreviven dos piezas, y cada una por una
 * razon concreta:</p>
 *
 * <ul>
 *   <li>{@link SumaRiemann} porque es la definicion de la que nace la integral, y verla
 *       dibujada explica el concepto. Sigue siendo una opcion visible del tema de area
 *       bajo la curva.</li>
 *   <li>{@link ReglaSimpson} porque hace de respaldo interno: cuando un integrando no tiene
 *       primitiva elemental hay que dar algun numero, y entonces la aplicacion avisa de que
 *       el valor es aproximado.</li>
 * </ul>
 *
 * <p>Trapecio, Gauss y Simpson adaptativo se eliminaron del proyecto junto con el selector
 * que los ofrecia.</p>
 */
@DisplayName("Metodos numericos que se conservan")
class MetodosIntegracionTest {

    private static final int PARTICIONES = 1000;

    private static FuncionMatematica f(String expresion) {
        return EvaluadorExpresion.compilar(expresion).comoFuncion();
    }

    @Nested
    @DisplayName("Simpson como respaldo interno")
    class SimpsonDeRespaldo {

        @Test
        @DisplayName("integral de x^2 de 0 a 1 vale 1/3")
        void polinomioSencillo() {
            assertEquals(1.0 / 3.0, new ReglaSimpson().integrar(f("x^2"), 0, 1, PARTICIONES), 1e-9);
        }

        @Test
        @DisplayName("integral de sin(x) de 0 a pi vale 2")
        void seno() {
            assertEquals(2.0,
                    new ReglaSimpson().integrar(f("sin(x)"), 0, Math.PI, PARTICIONES), 1e-9);
        }

        @Test
        @DisplayName("es exacto con polinomios de grado tres")
        void exactoEnCubicas() {
            double resultado = new ReglaSimpson().integrar(f("x^3 + 2x^2 - 5x + 1"), 0, 3, 100);
            // Valor exacto: 81/4 + 18 - 45/2 + 3
            assertEquals(81.0 / 4.0 + 18.0 - 22.5 + 3.0, resultado, 1e-9);
        }

        @Test
        @DisplayName("resuelve lo que el integrador simbolico no sabe")
        void integrandoSinPrimitivaElemental() {
            // integral de 0 a pi de x*sin(x) = pi, y necesita integracion por partes:
            // es justo el caso en que la aplicacion recurre a la aproximacion.
            assertEquals(Math.PI,
                    new ReglaSimpson().integrar(f("x*sin(x)"), 0, Math.PI, PARTICIONES), 1e-6);
        }

        @Test
        @DisplayName("ajusta a par un numero impar de particiones")
        void ajustaParticionesImpares() {
            assertEquals(1.0 / 3.0, new ReglaSimpson().integrar(f("x^2"), 0, 1, 101), 1e-9);
        }
    }

    @Nested
    @DisplayName("Suma de Riemann")
    class Riemann {

        @Test
        @DisplayName("se acerca al valor exacto al subir el numero de rectangulos")
        void convergeConMasRectangulos() {
            MetodoIntegracion medio = SumaRiemann.puntoMedio();
            double exacto = 1.0 / 3.0;

            double errorPocos = Math.abs(medio.integrar(f("x^2"), 0, 1, 10) - exacto);
            double errorMuchos = Math.abs(medio.integrar(f("x^2"), 0, 1, 1000) - exacto);

            assertTrue(errorMuchos < errorPocos,
                    "Mas rectangulos deben acercar la suma al area real");
        }

        @Test
        @DisplayName("por la izquierda subestima una funcion creciente")
        void izquierdaSubestima() {
            double izquierda = new SumaRiemann(SumaRiemann.Posicion.IZQUIERDA)
                    .integrar(f("x^2"), 0, 1, 100);

            assertTrue(izquierda < 1.0 / 3.0,
                    "Cada rectangulo se queda por debajo de la curva");
        }

        @Test
        @DisplayName("por la derecha sobrestima una funcion creciente")
        void derechaSobrestima() {
            double derecha = new SumaRiemann(SumaRiemann.Posicion.DERECHA)
                    .integrar(f("x^2"), 0, 1, 100);

            assertTrue(derecha > 1.0 / 3.0,
                    "Cada rectangulo sobresale por encima de la curva");
        }

        @Test
        @DisplayName("el punto medio queda entre las otras dos y mas cerca")
        void puntoMedioCompensa() {
            double exacto = 1.0 / 3.0;
            int n = 20;

            double errorIzquierda = Math.abs(
                    new SumaRiemann(SumaRiemann.Posicion.IZQUIERDA).integrar(f("x^2"), 0, 1, n) - exacto);
            double errorMedio = Math.abs(
                    SumaRiemann.puntoMedio().integrar(f("x^2"), 0, 1, n) - exacto);

            assertTrue(errorMedio < errorIzquierda,
                    "Lo que sobra de un lado compensa lo que falta del otro");
        }

        @Test
        @DisplayName("genera los rectangulos que se dibujan sobre la curva")
        void generaLosRectangulos() {
            List<SumaRiemann.Rectangulo> rectangulos =
                    SumaRiemann.puntoMedio().generarRectangulos(f("x^2"), 0, 1, 10);

            assertEquals(10, rectangulos.size());
            assertEquals(0.1, rectangulos.get(0).base(), 1e-9);
            assertEquals(0.05, rectangulos.get(0).xMuestra(), 1e-9);
        }
    }

    @Nested
    @DisplayName("Busqueda de raices")
    class Raices {

        @Test
        @DisplayName("encuentra donde x^2 - 4 vale cero")
        void raicesDeUnaParabola() {
            List<Double> raices = BuscadorRaices.buscarEnIntervalo(f("x^2 - 4"), -10, 10);

            assertEquals(2, raices.size(), "x^2 - 4 tiene dos raices: -2 y 2");
            assertEquals(-2.0, raices.get(0), 1e-6);
            assertEquals(2.0, raices.get(1), 1e-6);
        }

        @Test
        @DisplayName("encuentra donde se cortan x^2 y x")
        void interseccionDeDosCurvas() {
            List<Double> raices = BuscadorRaices.buscarEnIntervalo(f("x^2 - x"), -5, 5);

            assertEquals(2, raices.size(), "x^2 y x se cortan en 0 y en 1");
            assertEquals(0.0, raices.get(0), 1e-6);
            assertEquals(1.0, raices.get(1), 1e-6);
        }

        @Test
        @DisplayName("los cortes de integracion incluyen los dos extremos")
        void cortesConLosExtremos() {
            List<Double> cortes = BuscadorRaices.cortesDeIntegracion(f("x^2 - x"), -1, 2);

            assertEquals(-1.0, cortes.get(0), 1e-9);
            assertEquals(2.0, cortes.get(cortes.size() - 1), 1e-9);
            assertEquals(4, cortes.size(),
                    "Deben salir los dos extremos y las dos raices interiores");
        }
    }
}
