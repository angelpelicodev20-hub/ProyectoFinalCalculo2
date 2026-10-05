package com.calculo2.integrales.math.integracion;

import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.math.util.ValidadorFuncion;

import java.util.ArrayList;
import java.util.List;

/**
 * Suma de Riemann: la definicion misma de la integral definida.
 *
 * <p>Es el metodo del que parte el tema de area bajo la curva en el material del curso:
 * el intervalo se corta en {@code n} rectangulos de base {@code h} y el area se aproxima
 * sumando sus areas. La altura de cada rectangulo depende de donde se mida la funcion,
 * y de ahi salen las tres variantes de {@link Posicion}.</p>
 *
 * <pre>
 *   Integral de a a b de f(x) dx  =  limite, cuando n tiende a infinito, de
 *   la suma desde k=1 hasta n de f(x_k) * h,     con h = (b - a) / n
 * </pre>
 *
 * <p>Ademas de la suma, esta clase sabe devolver los rectangulos uno por uno, que es lo
 * que el frontend dibuja encima de la curva.</p>
 */
public final class SumaRiemann implements MetodoIntegracion {

    /** Punto de cada subintervalo donde se mide la altura del rectangulo. */
    public enum Posicion {
        /** Extremo izquierdo: subestima cuando la funcion crece. */
        IZQUIERDA("izquierda", "extremo izquierdo"),
        /** Extremo derecho: sobrestima cuando la funcion crece. */
        DERECHA("derecha", "extremo derecho"),
        /** Punto medio: mucho mas preciso, porque los errores se compensan. */
        MEDIO("punto medio", "punto medio");

        private final String etiqueta;
        private final String descripcion;

        Posicion(String etiqueta, String descripcion) {
            this.etiqueta = etiqueta;
            this.descripcion = descripcion;
        }

        /** Como se nombra la variante en pantalla. */
        public String etiqueta() {
            return etiqueta;
        }

        /** Donde se mide la altura, en palabras. */
        public String descripcion() {
            return descripcion;
        }
    }

    /**
     * Un rectangulo de la suma, con todo lo necesario para dibujarlo y para llenar la
     * fila correspondiente de la tabla.
     *
     * @param indice     numero de rectangulo, empezando en 1
     * @param xIzquierda borde izquierdo de la base
     * @param xDerecha   borde derecho de la base
     * @param xMuestra   punto donde se midio la altura
     * @param altura     valor de la funcion en {@code xMuestra}
     * @param area       area con signo del rectangulo
     */
    public record Rectangulo(int indice, double xIzquierda, double xDerecha,
                             double xMuestra, double altura, double area) {

        /** Ancho de la base del rectangulo. */
        public double base() {
            return xDerecha - xIzquierda;
        }
    }

    private final Posicion posicion;

    /**
     * @param posicion donde se mide la altura de cada rectangulo
     */
    public SumaRiemann(Posicion posicion) {
        this.posicion = posicion;
    }

    /** Crea la variante por punto medio, que es la mas precisa de las tres. */
    public static SumaRiemann puntoMedio() {
        return new SumaRiemann(Posicion.MEDIO);
    }

    @Override
    public double integrar(FuncionMatematica funcion, double a, double b, int particiones) {
        double h = (b - a) / particiones;
        double suma = 0.0;

        for (int k = 0; k < particiones; k++) {
            double xIzquierda = a + k * h;
            double altura = ValidadorFuncion.evaluarSeguro(funcion, puntoDeMuestra(xIzquierda, h));
            if (!Double.isNaN(altura)) {
                suma += altura;
            }
        }
        return suma * h;
    }

    /**
     * Genera los rectangulos de la suma para poder dibujarlos.
     *
     * <p>Se limita la cantidad porque dibujar mil rectangulos en pantalla no aporta nada
     * y hace lenta la grafica; para el calculo si se usan todas las particiones.</p>
     *
     * @param funcion     funcion a integrar
     * @param a           limite inferior
     * @param b           limite superior
     * @param particiones cuantos rectangulos generar
     * @return la lista de rectangulos, en orden de izquierda a derecha
     */
    public List<Rectangulo> generarRectangulos(FuncionMatematica funcion, double a, double b,
                                               int particiones) {
        double h = (b - a) / particiones;
        List<Rectangulo> rectangulos = new ArrayList<>(particiones);

        for (int k = 0; k < particiones; k++) {
            double xIzquierda = a + k * h;
            double xDerecha = xIzquierda + h;
            double xMuestra = puntoDeMuestra(xIzquierda, h);
            double altura = ValidadorFuncion.evaluarSeguro(funcion, xMuestra);

            rectangulos.add(new Rectangulo(
                    k + 1, xIzquierda, xDerecha, xMuestra, altura, altura * h));
        }
        return rectangulos;
    }

    /** Devuelve el punto del subintervalo donde toca medir la altura. */
    private double puntoDeMuestra(double xIzquierda, double h) {
        return switch (posicion) {
            case IZQUIERDA -> xIzquierda;
            case DERECHA -> xIzquierda + h;
            case MEDIO -> xIzquierda + h / 2.0;
        };
    }

    /** La variante que esta instancia representa. */
    public Posicion posicion() {
        return posicion;
    }

    @Override
    public String nombre() {
        return "Suma de Riemann por " + posicion.etiqueta();
    }

    @Override
    public String formula() {
        return "A = suma desde k=1 hasta n de f(x_k) * h,   con h = (b - a) / n";
    }

    @Override
    public String descripcion() {
        return "Divide el intervalo en n rectangulos de base h y mide la altura de cada uno en el "
                + posicion.descripcion() + " de su subintervalo. Es la definicion de la integral "
                + "definida: mientras mas rectangulos, mas se acerca al area real.";
    }

    @Override
    public int ordenDelError() {
        return posicion == Posicion.MEDIO ? 2 : 1;
    }
}
