package com.calculo2.integrales.math.solidos;

import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.math.util.ValidadorFuncion;
import com.calculo2.integrales.model.DatosGrafica3D;
import com.calculo2.integrales.model.EjeRotacion;
import com.calculo2.integrales.model.Punto3D;
import com.calculo2.integrales.util.Constantes;

import java.util.ArrayList;
import java.util.List;

/**
 * Construye la malla de puntos que el navegador dibuja como solido de revolucion.
 *
 * <p>La idea es sencilla: se recorre el eje de giro tomando cortes, y en cada corte se
 * genera el circulo que describe el borde de la region al dar la vuelta. Uniendo un
 * circulo con el siguiente se forma la superficie.</p>
 *
 * <p>Para un punto que esta a distancia {@code R} del eje, el circulo se parametriza con
 * el angulo {@code t} entre 0 y 2*pi:</p>
 *
 * <pre>
 *   Eje horizontal y = k:   (p, k + R*cos(t), R*sen(t))
 *   Eje vertical   x = k:   (k + R*cos(t), p, R*sen(t))
 * </pre>
 *
 * <p>donde {@code p} es la posicion del corte a lo largo del eje. La tercera coordenada
 * es la profundidad, que el JavaScript proyecta sobre el canvas.</p>
 */
public final class GeneradorMallaRevolucion {

    private GeneradorMallaRevolucion() {
    }

    /**
     * Cuantos puntos se examinan para encontrar los radios de cada corte en capas.
     *
     * <p>En capas, el radio de cada anillo no se evalua: se busca recorriendo el
     * intervalo hasta dar con el punto mas lejano y el mas cercano de la region a esa
     * altura. Con pocos puntos el radio queda redondeado al paso del recorrido, y
     * anillos vecinos repiten radio y despues saltan, de modo que el contorno del solido
     * sale escalonado. Es un calculo barato, asi que se hace fino.</p>
     */
    private static final int MUESTRAS_RADIO = 800;

    /**
     * Malla de un solido macizo, generado por el metodo de discos.
     *
     * @param curva curva que se hace girar
     * @param eje   recta alrededor de la cual gira
     * @param a     limite inferior
     * @param b     limite superior
     * @return los datos listos para enviarse al navegador
     */
    public static DatosGrafica3D paraDiscos(FuncionMatematica curva, EjeRotacion eje,
                                            double a, double b) {
        DatosGrafica3D datos = new DatosGrafica3D();
        datos.fijarEje(eje);
        datos.fijarLimites(a, b);

        double paso = (b - a) / Constantes.CORTES_MALLA;
        double radioMaximo = 0.0;

        for (int i = 0; i <= Constantes.CORTES_MALLA; i++) {
            double posicion = a + i * paso;
            double valor = ValidadorFuncion.evaluarSeguro(curva, posicion);
            if (Double.isNaN(valor)) {
                continue;
            }

            double radio = eje.radioDesde(valor);
            radioMaximo = Math.max(radioMaximo, radio);

            datos.agregarAnilloExterior(crearAnillo(posicion, radio, eje));
            datos.agregarPuntoPerfilExterior(posicion, valor);
        }

        datos.fijarRadioMaximo(radioMaximo);
        return datos;
    }

    /**
     * Malla de un solido hueco, generado por el metodo de arandelas.
     *
     * <p>Se generan dos superficies: la exterior y la del agujero. El frontend dibuja la
     * interior mas oscura para que se note el hueco.</p>
     *
     * @param curvaExterior una de las curvas que limitan la region
     * @param curvaInterior la otra curva
     * @param eje           recta alrededor de la cual gira
     * @param a             limite inferior
     * @param b             limite superior
     * @return los datos listos para enviarse al navegador
     */
    public static DatosGrafica3D paraArandelas(FuncionMatematica curvaExterior,
                                               FuncionMatematica curvaInterior,
                                               EjeRotacion eje, double a, double b) {
        DatosGrafica3D datos = new DatosGrafica3D();
        datos.fijarEje(eje);
        datos.fijarLimites(a, b);

        double paso = (b - a) / Constantes.CORTES_MALLA;
        double radioMaximo = 0.0;

        for (int i = 0; i <= Constantes.CORTES_MALLA; i++) {
            double posicion = a + i * paso;
            double valorExterior = ValidadorFuncion.evaluarSeguro(curvaExterior, posicion);
            double valorInterior = ValidadorFuncion.evaluarSeguro(curvaInterior, posicion);
            if (Double.isNaN(valorExterior) || Double.isNaN(valorInterior)) {
                continue;
            }

            // Cual curva queda mas lejos del eje puede cambiar a lo largo del intervalo.
            double distanciaUna = eje.radioDesde(valorExterior);
            double distanciaOtra = eje.radioDesde(valorInterior);
            double radioMayor = Math.max(distanciaUna, distanciaOtra);
            double radioMenor = Math.min(distanciaUna, distanciaOtra);

            radioMaximo = Math.max(radioMaximo, radioMayor);

            datos.agregarAnilloExterior(crearAnillo(posicion, radioMayor, eje));
            datos.agregarAnilloInterior(crearAnillo(posicion, radioMenor, eje));
            datos.agregarPuntoPerfilExterior(posicion, valorExterior);
            datos.agregarPuntoPerfilInterior(posicion, valorInterior);
        }

        datos.fijarRadioMaximo(radioMaximo);
        return datos;
    }

    /**
     * Malla de un solido generado por capas cilindricas.
     *
     * <p>Aqui la integracion corre perpendicular al eje de giro, asi que los cortes no
     * coinciden con los puntos donde se evalua la funcion. Para armar la malla hay que
     * recorrer el eje y preguntarse, en cada altura, que parte de la region alcanza ese
     * nivel: el radio exterior es la porcion de region mas lejana al eje y el interior la
     * mas cercana. Eso se resuelve muestreando el intervalo de integracion en cada
     * corte.</p>
     *
     * @param curvaSuperior curva que limita la region por arriba
     * @param curvaInferior curva que la limita por abajo
     * @param eje           recta alrededor de la cual gira
     * @param a             limite inferior de integracion
     * @param b             limite superior de integracion
     * @return los datos listos para enviarse al navegador
     */
    public static DatosGrafica3D paraCapas(FuncionMatematica curvaSuperior,
                                           FuncionMatematica curvaInferior,
                                           EjeRotacion eje, double a, double b) {
        DatosGrafica3D datos = new DatosGrafica3D();
        datos.fijarEje(eje);
        datos.fijarLimites(a, b);

        double[] extremos = rangoDeAlturas(curvaSuperior, curvaInferior, a, b);
        double alturaMinima = extremos[0];
        double alturaMaxima = extremos[1];

        if (alturaMinima > alturaMaxima) {
            datos.fijarRadioMaximo(1.0);
            return datos;
        }

        double pasoAltura = (alturaMaxima - alturaMinima) / Constantes.CORTES_MALLA;
        double pasoMuestreo = (b - a) / Constantes.CORTES_MALLA;
        double pasoRadio = (b - a) / MUESTRAS_RADIO;
        double radioMaximo = 0.0;

        for (int i = 0; i <= Constantes.CORTES_MALLA; i++) {
            double altura = alturaMinima + i * pasoAltura;

            double radioMayor = 0.0;
            double radioMenor = Double.POSITIVE_INFINITY;
            boolean hayRegion = false;

            for (int j = 0; j <= MUESTRAS_RADIO; j++) {
                double x = a + j * pasoRadio;
                double techo = ValidadorFuncion.evaluarSeguro(curvaSuperior, x);
                double piso = ValidadorFuncion.evaluarSeguro(curvaInferior, x);
                if (Double.isNaN(techo) || Double.isNaN(piso)) {
                    continue;
                }

                double limiteBajo = Math.min(techo, piso);
                double limiteAlto = Math.max(techo, piso);

                if (altura >= limiteBajo && altura <= limiteAlto) {
                    double radio = MetodoCapas.radioEn(eje, x);
                    radioMayor = Math.max(radioMayor, radio);
                    radioMenor = Math.min(radioMenor, radio);
                    hayRegion = true;
                }
            }

            if (!hayRegion) {
                continue;
            }

            radioMaximo = Math.max(radioMaximo, radioMayor);
            datos.agregarAnilloExterior(crearAnillo(altura, radioMayor, eje));

            // Solo hay agujero si la region no llega hasta el eje.
            if (radioMenor > 1e-9) {
                datos.agregarAnilloInterior(crearAnillo(altura, radioMenor, eje));
            }
        }

        for (int j = 0; j <= Constantes.CORTES_MALLA; j++) {
            double x = a + j * pasoMuestreo;
            double techo = ValidadorFuncion.evaluarSeguro(curvaSuperior, x);
            double piso = ValidadorFuncion.evaluarSeguro(curvaInferior, x);
            if (!Double.isNaN(techo)) {
                datos.agregarPuntoPerfilExterior(x, techo);
            }
            if (!Double.isNaN(piso)) {
                datos.agregarPuntoPerfilInterior(x, piso);
            }
        }

        datos.fijarRadioMaximo(radioMaximo);
        return datos;
    }

    // ------------------------------------------------------------------
    // APOYO
    // ------------------------------------------------------------------

    /**
     * Genera el circulo completo que describe un punto del perfil al girar.
     *
     * @param posicion posicion del corte a lo largo del eje de giro
     * @param radio    distancia del punto al eje
     * @param eje      recta alrededor de la cual gira
     * @return los puntos del circulo, en orden
     */
    private static List<Punto3D> crearAnillo(double posicion, double radio, EjeRotacion eje) {
        int segmentos = Constantes.SEGMENTOS_ANILLO;
        List<Punto3D> anillo = new ArrayList<>(segmentos + 1);
        double k = eje.desplazamiento();

        // Se cierra el circulo repitiendo el primer punto al final.
        for (int i = 0; i <= segmentos; i++) {
            double angulo = 2.0 * Math.PI * i / segmentos;
            double desviacion = radio * Math.cos(angulo);
            double profundidad = radio * Math.sin(angulo);

            if (eje.esHorizontal()) {
                anillo.add(new Punto3D(posicion, k + desviacion, profundidad));
            } else {
                anillo.add(new Punto3D(k + desviacion, posicion, profundidad));
            }
        }
        return anillo;
    }

    /**
     * Averigua entre que alturas se extiende la region.
     *
     * @return un arreglo de dos posiciones: la altura minima y la maxima
     */
    private static double[] rangoDeAlturas(FuncionMatematica curvaSuperior,
                                           FuncionMatematica curvaInferior,
                                           double a, double b) {
        double minima = Double.POSITIVE_INFINITY;
        double maxima = Double.NEGATIVE_INFINITY;
        double paso = (b - a) / Constantes.CORTES_MALLA;

        for (int i = 0; i <= Constantes.CORTES_MALLA; i++) {
            double x = a + i * paso;
            double techo = ValidadorFuncion.evaluarSeguro(curvaSuperior, x);
            double piso = ValidadorFuncion.evaluarSeguro(curvaInferior, x);

            if (!Double.isNaN(techo)) {
                minima = Math.min(minima, techo);
                maxima = Math.max(maxima, techo);
            }
            if (!Double.isNaN(piso)) {
                minima = Math.min(minima, piso);
                maxima = Math.max(maxima, piso);
            }
        }
        return new double[]{minima, maxima};
    }
}
