package com.calculo2.integrales.interprete;

import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.math.util.Redondeo;
import com.calculo2.integrales.math.util.ValidadorFuncion;
import com.calculo2.integrales.model.EjeRotacion;
import com.calculo2.integrales.model.TipoSolido;

import java.util.ArrayList;
import java.util.List;

/**
 * Decide con que metodo se resuelve un solido de revolucion.
 *
 * <p>La eleccion no se puede sacar del enunciado leyendo palabras: ningun ejercicio dice
 * "use arandelas". Se deduce de la geometria, y son dos preguntas encadenadas.</p>
 *
 * <p><b>Primera: en que direccion conviene rebanar.</b> Discos y arandelas cortan
 * perpendicularmente al eje de giro. Si el eje es horizontal y las funciones estan
 * escritas en terminos de {@code x}, esos cortes son verticales y todo encaja. Pero si el
 * eje es vertical, los cortes serian horizontales y habria que despejar {@code x} en
 * funcion de {@code y}, cosa que no siempre se puede. Las capas cilindricas rebanan en la
 * direccion contraria y evitan ese despeje, y por eso son el metodo natural cuando el eje
 * es perpendicular a la variable de las funciones.</p>
 *
 * <p><b>Segunda: el solido queda macizo o hueco.</b> Se responde midiendo. Se recorre el
 * intervalo calculando la distancia de la region al eje; si en todo el recorrido la
 * frontera mas cercana toca el eje, cada rebanada es un disco lleno. Si la region se
 * mantiene separada del eje, queda un agujero y las rebanadas son arandelas.</p>
 *
 * <p>Esa segunda pregunta se responde evaluando las funciones, no interpretando el texto:
 * es el propio ejercicio el que decide.</p>
 */
public final class SelectorMetodoSolido {

    private SelectorMetodoSolido() {
    }

    /** Cuantos puntos del intervalo se examinan para medir la region. */
    private static final int MUESTRAS = 200;

    /** Por debajo de esta distancia se considera que la region toca el eje. */
    private static final double TOLERANCIA_CONTACTO = 1e-6;

    /**
     * El metodo elegido y el razonamiento que llevo a el.
     *
     * @param tipo       metodo geometrico
     * @param evidencias explicacion de por que corresponde
     * @param avisos     detalles que conviene revisar
     */
    public record Seleccion(TipoSolido tipo, List<String> evidencias, List<String> avisos) {
    }

    /**
     * Elige el metodo analizando la geometria del ejercicio.
     *
     * @param curvaF   funcion principal, ya compilada
     * @param curvaG   segunda funcion, o null si el ejercicio solo tiene una
     * @param eje      recta alrededor de la cual gira la region
     * @param a        limite inferior
     * @param b        limite superior
     * @param variable nombre de la variable de las funciones
     * @return el metodo con su justificacion
     */
    public static Seleccion elegir(FuncionMatematica curvaF, FuncionMatematica curvaG,
                                   EjeRotacion eje, double a, double b, String variable) {
        List<String> evidencias = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        // ---------- Primera pregunta: direccion de las rebanadas ----------
        boolean funcionesEnX = variable.equalsIgnoreCase("x");
        boolean ejeParaleloALaVariable = eje.esHorizontal() == funcionesEnX;

        if (!ejeParaleloALaVariable) {
            evidencias.add("El eje de giro es " + (eje.esHorizontal() ? "horizontal" : "vertical")
                    + " y las funciones estan escritas en terminos de " + variable + ". "
                    + "Rebanar perpendicularmente al eje obligaria a despejar la funcion "
                    + "respecto de la otra variable; las capas cilindricas rebanan en la "
                    + "direccion contraria y evitan ese despeje.");

            return new Seleccion(TipoSolido.CAPAS, evidencias, avisos);
        }

        evidencias.add("El eje de giro es " + (eje.esHorizontal() ? "horizontal" : "vertical")
                + " y las funciones dependen de " + variable + ", asi que los cortes "
                + "perpendiculares al eje se pueden describir directamente: se puede usar "
                + "el metodo de discos o el de arandelas.");

        // ---------- Segunda pregunta: macizo o hueco ----------
        Medicion medicion = medirRegion(curvaF, curvaG, eje, a, b);

        if (medicion.sinDatos()) {
            avisos.add("No se pudo evaluar la region en el intervalo indicado, asi que el "
                    + "metodo se eligio por la forma del enunciado y conviene revisarlo.");
            boolean hayDosCurvas = curvaG != null;
            return new Seleccion(
                    hayDosCurvas ? TipoSolido.ARANDELAS : TipoSolido.DISCOS, evidencias, avisos);
        }

        if (medicion.radioInteriorMaximo() < TOLERANCIA_CONTACTO) {
            evidencias.add("Midiendo la region a lo largo del intervalo, su frontera mas "
                    + "cercana al eje lo toca: la distancia minima al eje vale cero. "
                    + "Entonces cada rebanada es un circulo completo, sin agujero, y "
                    + "corresponde el metodo de discos.");

            return new Seleccion(TipoSolido.DISCOS, evidencias, avisos);
        }

        // La region puede estar separada del eje en todo el intervalo, o tocarlo solo en
        // algunos puntos. En los dos casos hay hueco y corresponden arandelas, pero la
        // explicacion no es la misma y decir la que no es confundiria mas que ayudar.
        if (medicion.radioInteriorMinimo() > TOLERANCIA_CONTACTO) {
            evidencias.add("Midiendo la region a lo largo del intervalo, se mantiene "
                    + "separada del eje: su frontera mas cercana nunca se acerca a menos de "
                    + Redondeo.texto(medicion.radioInteriorMinimo())
                    + ". Al girar queda un hueco en el centro, asi que cada rebanada es una "
                    + "arandela y no un disco.");
        } else {
            evidencias.add("La region toca el eje en algunos puntos del intervalo, pero en "
                    + "el resto se separa de el hasta "
                    + Redondeo.texto(medicion.radioInteriorMaximo())
                    + ". Como en buena parte del recorrido queda hueco, el metodo que "
                    + "corresponde es el de arandelas, con radio interior variable.");
        }

        if (medicion.hayCruce()) {
            avisos.add("Las dos curvas se cruzan dentro del intervalo, de modo que cambia "
                    + "cual de ellas queda mas lejos del eje. El calculo toma en cada punto "
                    + "la mas lejana como radio exterior, pero conviene comprobar que el "
                    + "intervalo sea el que pide el ejercicio.");
        }

        return new Seleccion(TipoSolido.ARANDELAS, evidencias, avisos);
    }

    // ------------------------------------------------------------------
    // MEDICION DE LA REGION
    // ------------------------------------------------------------------

    /**
     * Lo que se observo al recorrer la region.
     *
     * @param radioInteriorMinimo distancia mas pequena de la region al eje
     * @param radioInteriorMaximo la mayor de las distancias minimas encontradas
     * @param hayCruce            true si las curvas intercambian posiciones
     * @param sinDatos            true si no se pudo evaluar en ningun punto
     */
    private record Medicion(double radioInteriorMinimo, double radioInteriorMaximo,
                            boolean hayCruce, boolean sinDatos) {
    }

    /**
     * Recorre el intervalo midiendo a que distancia queda la region del eje.
     *
     * <p>En cada punto se calculan las distancias de las dos fronteras al eje. La menor es
     * el radio del agujero en esa rebanada. Si esa menor llega a cero en todas partes, no
     * hay agujero.</p>
     *
     * <p>Cuando solo hay una funcion, la segunda frontera de la region es el eje
     * horizontal {@code y = 0}. Eso importa: girando alrededor del propio eje X la region
     * lo toca y el solido sale macizo, pero girando alrededor de una recta corrida como
     * {@code y = -1} esa misma frontera queda a distancia 1 y aparece un hueco.</p>
     */
    private static Medicion medirRegion(FuncionMatematica curvaF, FuncionMatematica curvaG,
                                        EjeRotacion eje, double a, double b) {
        double inicio = Math.min(a, b);
        double fin = Math.max(a, b);
        double paso = (fin - inicio) / MUESTRAS;

        double menorDistancia = Double.POSITIVE_INFINITY;
        double mayorDistanciaMinima = 0.0;
        int evaluados = 0;

        Boolean fEstabaArriba = null;
        boolean hayCruce = false;

        for (int i = 0; i <= MUESTRAS; i++) {
            double punto = inicio + i * paso;

            double valorF = ValidadorFuncion.evaluarSeguro(curvaF, punto);
            if (!ValidadorFuncion.esUtilizable(valorF)) {
                continue;
            }

            // La segunda frontera: la otra curva, o el eje horizontal si no la hay.
            double valorG = 0.0;
            if (curvaG != null) {
                valorG = ValidadorFuncion.evaluarSeguro(curvaG, punto);
                if (!ValidadorFuncion.esUtilizable(valorG)) {
                    continue;
                }

                // Los puntos donde las dos curvas valen lo mismo no dicen cual va arriba,
                // y hay que dejarlos fuera del recuento. Si no, un simple contacto, que
                // es lo que pasa en los extremos donde la region se cierra, se contaria
                // como que las curvas intercambian posiciones, y el ejercicio recibiria
                // un aviso de cruce que no existe.
                if (Math.abs(valorF - valorG) > TOLERANCIA_CONTACTO) {
                    boolean fArribaAhora = valorF > valorG;
                    if (fEstabaArriba != null && fEstabaArriba != fArribaAhora) {
                        hayCruce = true;
                    }
                    fEstabaArriba = fArribaAhora;
                }
            }

            double distanciaF = eje.radioDesde(valorF);
            double distanciaG = eje.radioDesde(valorG);
            double radioInterior = Math.min(distanciaF, distanciaG);

            menorDistancia = Math.min(menorDistancia, radioInterior);
            mayorDistanciaMinima = Math.max(mayorDistanciaMinima, radioInterior);
            evaluados++;
        }

        if (evaluados == 0) {
            return new Medicion(0, 0, false, true);
        }
        return new Medicion(menorDistancia, mayorDistanciaMinima, hayCruce, false);
    }
}
