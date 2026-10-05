package com.calculo2.integrales.service;

import com.calculo2.integrales.exception.LimitesInvalidosException;
import com.calculo2.integrales.math.algebra.ResolutorEcuaciones;
import com.calculo2.integrales.math.parser.EvaluadorExpresion;
import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.math.parser.NodoExpresion;
import com.calculo2.integrales.math.simbolico.FormateadorMatematico;
import com.calculo2.integrales.math.util.Redondeo;
import com.calculo2.integrales.math.util.ValidadorFuncion;
import com.calculo2.integrales.model.ResultadoIntegral;
import com.calculo2.integrales.model.SolicitudIntegral;

import java.util.ArrayList;
import java.util.List;

/**
 * Resuelve el tema de area entre dos curvas.
 *
 * <pre>
 *   A = integral de a a b de [ f(x) - g(x) ] dx      con f arriba de g
 * </pre>
 *
 * <p>El procedimiento es el del curso:</p>
 *
 * <ol>
 *   <li>Hallar los puntos de interseccion igualando las curvas y factorizando.</li>
 *   <li>Determinar los limites: los escritos o los cortes.</li>
 *   <li>Identificar la funcion superior en cada tramo, con un punto de prueba.</li>
 *   <li>Integrar la de arriba menos la de abajo en cada tramo y sumar.</li>
 * </ol>
 *
 * <p>Cuando las curvas se cruzan dentro del intervalo, la de arriba cambia y la integral
 * de corrido daria un valor menor que el area, porque los tramos se restarian. Por eso
 * se parte siempre en los cortes.</p>
 *
 * <p>Si la region se describe con curvas {@code x = g(y)}, todo es igual cambiando
 * "arriba" por "a la derecha" e integrando respecto de y.</p>
 */
public final class AreaEntreCurvasService {

    /** Hasta donde se busca a cada lado del origen cuando hay que deducir los limites. */
    private static final double RANGO_DE_BUSQUEDA = 100.0;

    private final GraficaService graficaService = new GraficaService();
    private final InterseccionService interseccionService = new InterseccionService();
    private final PasosService pasosService = new PasosService();
    private final ResolutorIntegral resolutor = new ResolutorIntegral();

    /**
     * Ejecuta el calculo completo.
     *
     * @param solicitud datos que llegaron del formulario
     * @return el resultado con el numero, los pasos y la grafica
     * @throws LimitesInvalidosException si falta la segunda curva o si no encierran region
     */
    public ResultadoIntegral calcular(SolicitudIntegral solicitud) {
        long comienzo = System.currentTimeMillis();

        if (!solicitud.tieneSegundaCurva()) {
            throw new LimitesInvalidosException(
                    "Para calcular el area entre dos curvas hacen falta las dos funciones. "
                            + "Si solo tiene una, use el tema de area bajo la curva.");
        }

        String variable = solicitud.variable();
        EvaluadorExpresion expresionF = EvaluadorExpresion.compilar(solicitud.funcionF(), variable);
        EvaluadorExpresion expresionG = EvaluadorExpresion.compilar(solicitud.funcionG(), variable);
        FuncionMatematica funcionF = expresionF.comoFuncion();
        FuncionMatematica funcionG = expresionG.comoFuncion();

        // ---------- Puntos de interseccion ----------
        ResolutorEcuaciones.Solucion cortes = interseccionService.resolverCortes(
                expresionF, expresionG, -RANGO_DE_BUSQUEDA, RANGO_DE_BUSQUEDA);

        // ---------- Limites ----------
        double[] limites = determinarLimites(solicitud, cortes);
        double a = limites[0];
        double b = limites[1];

        ValidadorFuncion.validarLimites(a, b);
        ValidadorFuncion.validarIntegrable(funcionF, a, b, "f(" + variable + ")");
        ValidadorFuncion.validarIntegrable(funcionG, a, b, "g(" + variable + ")");

        List<Double> intersecciones = interseccionService.buscarIntersecciones(
                funcionF, funcionG, a, b);

        String explicacionLimites;
        if (!solicitud.explicacionLimites().isBlank()) {
            explicacionLimites = solicitud.explicacionLimites();
        } else if (solicitud.buscarLimites()) {
            explicacionLimites = "La region encerrada va del primer punto de corte al ultimo: de "
                    + variable + " = " + cortes.textoDe(a) + " a " + variable + " = "
                    + cortes.textoDe(b) + ".";
        } else {
            explicacionLimites = "";
        }

        ResultadoIntegral resultado = new ResultadoIntegral();
        resultado.fijarFunciones(expresionF.expresionOriginal(), expresionG.expresionOriginal());
        resultado.fijarLimites(a, b);
        resultado.fijarExplicacionLimites(explicacionLimites);
        resultado.fijarMetodo("Integracion directa (regla de Barrow)", 0);
        resultado.fijarVariable(variable);

        // ---------- Tramos: quien va arriba en cada uno ----------
        List<InterseccionService.Tramo> tramos =
                interseccionService.dividirEnTramos(funcionF, funcionG, a, b);

        String nombreF = nombre("f", variable, expresionF);
        String nombreG = nombre("g", variable, expresionG);
        List<PasosService.DetalleTramo> detalle = new ArrayList<>();
        double area = 0.0;
        for (InterseccionService.Tramo tramo : tramos) {
            EvaluadorExpresion arriba = tramo.fEstaArriba() ? expresionF : expresionG;
            EvaluadorExpresion abajo = tramo.fEstaArriba() ? expresionG : expresionF;
            NodoExpresion integrando = ResolutorIntegral.restar(arriba.raiz(), abajo.raiz());
            ResolutorIntegral.Resolucion resolucion =
                    resolutor.resolver(integrando, variable, tramo.inicio(), tramo.fin());
            double prueba = (tramo.inicio() + tramo.fin()) / 2.0;
            double areaTramo = Math.abs(resolucion.valor());
            area += areaTramo;
            detalle.add(new PasosService.DetalleTramo(tramo.inicio(), tramo.fin(), prueba,
                    tramo.fEstaArriba() ? nombreF : nombreG,
                    tramo.fEstaArriba() ? nombreG : nombreF,
                    ValidadorFuncion.evaluarSeguro(arriba.comoFuncion(), prueba),
                    ValidadorFuncion.evaluarSeguro(abajo.comoFuncion(), prueba),
                    FormateadorMatematico.escribir(integrando), resolucion, areaTramo));
        }

        // La integral sin partir: sirve para mostrar que da distinto cuando hay cruces.
        NodoExpresion diferencia = ResolutorIntegral.restar(expresionF.raiz(), expresionG.raiz());
        ResolutorIntegral.Resolucion resolucion = resolutor.resolver(diferencia, variable, a, b);

        resultado.fijarValores(resolucion.valor(), area);
        resultado.fijarAntiderivada(resolucion.antiderivada(), resolucion.fueAnalitica());
        intersecciones.forEach(resultado::agregarInterseccion);

        if (tramos.size() > 1) {
            resultado.fijarAdvertencia(
                    "Las curvas se cruzan " + (tramos.size() - 1) + " vez(ces) dentro del "
                            + "intervalo. Integrar de corrido daria "
                            + Redondeo.texto(resolucion.valor()) + ", porque los tramos se "
                            + "cancelan entre si; el area real es " + Redondeo.texto(area) + ".");
        }

        resultado.fijarGrafica(graficaService.graficarRegion(expresionF, expresionG, a, b,
                variable, solicitud.curvasDelEnunciado()));

        resultado.agregarPasos(pasosService.paraAreaEntreCurvas(new PasosService.ContextoArea(
                expresionF.expresionOriginal(), expresionG.expresionOriginal(),
                a, b, variable, resolucion, resolucion.valor(), area,
                intersecciones, explicacionLimites, tramos, "",
                interseccionService.explicarCortes(cortes, nombreF, nombreG, variable, a, b),
                detalle, solicitud.explicacionRegion())));

        resultado.fijarDuracion(System.currentTimeMillis() - comienzo);
        return resultado;
    }

    /** "y = x^2" o "x = y^2", segun la variable. */
    private String nombre(String letra, String variable, EvaluadorExpresion expresion) {
        String otra = variable.equals("x") ? "y" : "x";
        return otra + " = " + expresion.expresionOriginal();
    }

    // ------------------------------------------------------------------
    // LIMITES
    // ------------------------------------------------------------------

    /**
     * Decide que limites se van a usar.
     *
     * <p>Con la casilla marcada se deducen de las intersecciones, que es como se resuelve
     * el ejercicio clasico que pide "el area encerrada entre las dos curvas" sin dar
     * limites.</p>
     */
    private double[] determinarLimites(SolicitudIntegral solicitud, ResolutorEcuaciones.Solucion cortes) {
        if (solicitud.buscarLimites()) {
            List<Double> valores = cortes.valores();
            if (valores.size() < 2) {
                throw new LimitesInvalidosException(
                        "Las dos curvas se cortan en " + valores.size() + " punto(s), asi que no "
                                + "encierran una region por si solas. Escriba los limites de "
                                + "integracion, o las rectas que cierran la region.");
            }
            return new double[]{valores.get(0), valores.get(valores.size() - 1)};
        }

        if (!solicitud.tieneLimites()) {
            throw new LimitesInvalidosException(
                    "Faltan los limites de integracion. Escribalos, o marque \"Buscar los "
                            + "limites\" para obtenerlos de las intersecciones de las curvas.");
        }
        return new double[]{solicitud.inicio(), solicitud.fin()};
    }
}
