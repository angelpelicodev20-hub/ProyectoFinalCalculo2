package com.calculo2.integrales.service;

import com.calculo2.integrales.exception.LimitesInvalidosException;
import com.calculo2.integrales.math.algebra.ResolutorEcuaciones;
import com.calculo2.integrales.math.integracion.SumaRiemann;
import com.calculo2.integrales.math.parser.EvaluadorExpresion;
import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.math.simbolico.FormateadorMatematico;
import com.calculo2.integrales.math.util.Redondeo;
import com.calculo2.integrales.math.util.ValidadorFuncion;
import com.calculo2.integrales.model.DatosGrafica2D;
import com.calculo2.integrales.model.MetodoArea;
import com.calculo2.integrales.model.ResultadoIntegral;
import com.calculo2.integrales.model.SolicitudIntegral;

import java.util.ArrayList;
import java.util.List;

/**
 * Resuelve el tema de area bajo la curva.
 *
 * <pre>
 *   A = integral de a a b de |f(x)| dx
 * </pre>
 *
 * <p>Se resuelve analiticamente: se busca la primitiva y se aplica la regla de Barrow. Ese
 * es el procedimiento del curso, y es el que deja algo que el estudiante pueda comparar
 * con su cuaderno.</p>
 *
 * <p>El punto delicado del tema es la diferencia entre la integral y el area. La integral
 * definida cuenta como negativo lo que queda debajo del eje, de modo que si la curva lo
 * cruza dentro del intervalo, las dos porciones se cancelan entre si. El area geometrica,
 * en cambio, siempre es positiva. Por eso el servicio busca los cortes con el eje
 * (factorizando cuando se puede), parte el intervalo en ellos, comprueba el signo en cada
 * tramo, integra cada uno por separado y suma los valores absolutos.</p>
 */
public final class AreaBajoCurvaService {

    /** Hasta donde se buscan las raices cuando hay que deducir los limites. */
    private static final double RANGO_DE_BUSQUEDA = 50.0;

    private final GraficaService graficaService = new GraficaService();
    private final InterseccionService interseccionService = new InterseccionService();
    private final PasosService pasosService = new PasosService();
    private final ResolutorIntegral resolutor = new ResolutorIntegral();

    /**
     * Ejecuta el calculo completo.
     *
     * @param solicitud datos que llegaron del formulario
     * @return el resultado con el numero, los pasos y la grafica
     */
    public ResultadoIntegral calcular(SolicitudIntegral solicitud) {
        long comienzo = System.currentTimeMillis();

        String variable = solicitud.variable();
        EvaluadorExpresion expresion = EvaluadorExpresion.compilar(solicitud.funcionF(), variable);
        FuncionMatematica funcion = expresion.comoFuncion();

        // ---------- Cortes con el eje ----------
        ResolutorEcuaciones.Solucion cortes = interseccionService.resolverCortes(
                expresion, null, -RANGO_DE_BUSQUEDA, RANGO_DE_BUSQUEDA);

        // ---------- Limites ----------
        double[] limites = determinarLimites(solicitud, cortes);
        double a = limites[0];
        double b = limites[1];
        String explicacionLimites = explicacionDeLimites(solicitud, cortes, variable, a, b);

        ValidadorFuncion.validarLimites(a, b);
        ValidadorFuncion.validarIntegrable(funcion, a, b, "f(" + variable + ")");

        ResultadoIntegral resultado = new ResultadoIntegral();
        resultado.fijarFunciones(expresion.expresionOriginal(), "");
        resultado.fijarLimites(a, b);
        resultado.fijarExplicacionLimites(explicacionLimites);
        resultado.fijarVariable(variable);

        // ---------- Integral con signo ----------
        ResolutorIntegral.Resolucion resolucion =
                resolutor.resolver(expresion.raiz(), variable, a, b);
        resultado.fijarAntiderivada(resolucion.antiderivada(), resolucion.fueAnalitica());

        // ---------- Area geometrica, tramo por tramo ----------
        List<Double> cruces = interseccionService.buscarCrucesConElEje(funcion, a, b);
        List<PasosService.DetalleTramo> detalle = integrarPorTramos(expresion, a, b, cruces);
        double area = 0.0;
        for (PasosService.DetalleTramo tramo : detalle) {
            area += tramo.area();
        }

        resultado.fijarValores(resolucion.valor(), area);
        cruces.forEach(resultado::agregarInterseccion);

        if (!cruces.isEmpty()) {
            resultado.fijarAdvertencia(
                    "La curva cruza el eje dentro del intervalo. La integral de corrido vale "
                            + Redondeo.texto(resolucion.valor()) + " porque la parte que queda "
                            + "debajo del eje cuenta como negativa; el area geometrica es "
                            + Redondeo.texto(area) + ".");
        }

        // ---------- Riemann, si se pidio ----------
        MetodoArea metodo = solicitud.metodo();
        int rectangulos = solicitud.particionesValidas();
        boolean conRectangulos = metodo.dibujaRectangulos() && variable.equals("x");
        String textoRiemann = conRectangulos
                ? describirRiemann(solicitud, funcion, a, b, rectangulos, resolucion.valor())
                : "";

        resultado.fijarMetodo(metodo.etiqueta(), conRectangulos ? rectangulos : 0);
        resultado.fijarGrafica(construirGrafica(solicitud, expresion, a, b, rectangulos, conRectangulos));

        // ---------- Pasos ----------
        resultado.agregarPasos(pasosService.paraAreaBajoCurva(new PasosService.ContextoArea(
                expresion.expresionOriginal(), "", a, b, variable,
                resolucion, resolucion.valor(), area, cruces, explicacionLimites,
                List.of(), textoRiemann,
                interseccionService.explicarCortes(cortes, "f", "", variable, a, b),
                detalle, solicitud.explicacionRegion())));

        resultado.fijarDuracion(System.currentTimeMillis() - comienzo);
        return resultado;
    }

    // ------------------------------------------------------------------
    // LIMITES
    // ------------------------------------------------------------------

    /**
     * Decide que limites se van a usar.
     *
     * <p>Cuando el estudiante marca la casilla, los limites salen de los puntos donde la
     * curva corta el eje: la region encerrada entre una curva y el eje empieza y termina
     * justo ahi. Si la curva no corta el eje en dos puntos no hay region cerrada que
     * medir, y se dice en lugar de escoger un intervalo cualquiera.</p>
     */
    private double[] determinarLimites(SolicitudIntegral solicitud, ResolutorEcuaciones.Solucion cortes) {
        if (!solicitud.buscarLimites()) {
            if (!solicitud.tieneLimites()) {
                throw new LimitesInvalidosException(
                        "Faltan los limites de integracion. Escribalos, o marque \"Buscar los "
                                + "limites\" para obtenerlos de los cortes de la curva con el eje.");
            }
            return new double[]{solicitud.inicio(), solicitud.fin()};
        }

        List<Double> raices = cortes.valores();
        if (raices.size() < 2) {
            throw new LimitesInvalidosException(
                    "La curva corta el eje en " + raices.size() + " punto(s), asi que no encierra "
                            + "una region con el. Escriba los limites a mano.");
        }
        return new double[]{raices.get(0), raices.get(raices.size() - 1)};
    }

    /** Redacta como se dedujeron los limites, o cadena vacia si los dio el estudiante. */
    private String explicacionDeLimites(SolicitudIntegral solicitud, ResolutorEcuaciones.Solucion cortes,
                                        String variable, double a, double b) {
        if (!solicitud.explicacionLimites().isBlank()) {
            return solicitud.explicacionLimites();
        }
        if (!solicitud.buscarLimites()) {
            return "";
        }
        return "La region encerrada entre la curva y el eje va del primer corte al ultimo: de "
                + variable + " = " + cortes.textoDe(a) + " a " + variable + " = "
                + cortes.textoDe(b) + ".";
    }

    // ------------------------------------------------------------------
    // AREA
    // ------------------------------------------------------------------

    /**
     * Integra cada tramo por separado, con su punto de prueba.
     *
     * <p>Cada tramo se resuelve de forma analitica, para que el area y la integral salgan
     * del mismo procedimiento y no de dos metodos distintos que podrian discrepar en los
     * ultimos decimales.</p>
     */
    private List<PasosService.DetalleTramo> integrarPorTramos(EvaluadorExpresion expresion,
                                                              double a, double b,
                                                              List<Double> cruces) {
        List<Double> cortes = new ArrayList<>();
        cortes.add(a);
        cortes.addAll(cruces);
        cortes.add(b);

        String variable = expresion.variable();
        String nombreCurva = (variable.equals("x") ? "f(x)" : "g(y)");
        String nombreEje = variable.equals("x") ? "el eje X" : "el eje Y";
        String integrando = FormateadorMatematico.escribir(expresion.raiz());

        List<PasosService.DetalleTramo> tramos = new ArrayList<>();
        for (int i = 0; i < cortes.size() - 1; i++) {
            double inicio = cortes.get(i);
            double fin = cortes.get(i + 1);
            if (fin - inicio < 1e-12) {
                continue;
            }
            double prueba = (inicio + fin) / 2.0;
            double valor = ValidadorFuncion.evaluarSeguro(expresion.comoFuncion(), prueba);
            boolean porEncima = valor >= 0;

            ResolutorIntegral.Resolucion resolucion =
                    resolutor.resolver(expresion.raiz(), variable, inicio, fin);
            tramos.add(new PasosService.DetalleTramo(inicio, fin, prueba,
                    porEncima ? nombreCurva : nombreEje,
                    porEncima ? nombreEje : nombreCurva,
                    porEncima ? valor : 0.0,
                    porEncima ? 0.0 : valor,
                    integrando, resolucion, Math.abs(resolucion.valor())));
        }
        return tramos;
    }

    // ------------------------------------------------------------------
    // RIEMANN
    // ------------------------------------------------------------------

    /**
     * Redacta la comparacion entre la suma de rectangulos y el valor exacto.
     *
     * <p>Es lo que convierte a Riemann en algo util despues de tener el resultado
     * analitico: ver de cuanto se queda corta la aproximacion, y hacia que lado, explica
     * por que la integral es un limite y no una suma finita.</p>
     */
    private String describirRiemann(SolicitudIntegral solicitud, FuncionMatematica funcion,
                                    double a, double b, int rectangulos, double valorExacto) {
        SumaRiemann riemann = new SumaRiemann(solicitud.posicionRiemann());
        double aproximacion = riemann.integrar(funcion, a, b, rectangulos);
        double h = (b - a) / rectangulos;

        StringBuilder texto = new StringBuilder();
        texto.append("n = ").append(rectangulos).append(" rectangulos\n");
        texto.append("h = (b - a) / n = (").append(Redondeo.texto(b)).append(" - ")
             .append(Redondeo.texto(a)).append(") / ").append(rectangulos)
             .append(" = ").append(Redondeo.texto(h)).append('\n');
        texto.append("Altura medida en el ").append(solicitud.posicionRiemann().descripcion())
             .append(" de cada subintervalo.\n\n");
        texto.append("Suma de Riemann = ").append(Redondeo.texto(aproximacion)).append('\n');
        texto.append("Valor exacto    = ").append(Redondeo.texto(valorExacto)).append('\n');
        texto.append("Diferencia      = ")
             .append(Redondeo.texto(Math.abs(aproximacion - valorExacto)));

        return texto.toString();
    }

    // ------------------------------------------------------------------
    // GRAFICA
    // ------------------------------------------------------------------

    /**
     * Arma la grafica con los mismos limites con que se resolvio el ejercicio.
     *
     * <p>Recibe {@code a} y {@code b} ya determinados, no los de la solicitud: si los
     * limites se buscaron automaticamente, dibujar los del formulario mostraria una region
     * distinta de la que se calculo.</p>
     */
    private DatosGrafica2D construirGrafica(SolicitudIntegral solicitud, EvaluadorExpresion expresion,
                                            double a, double b, int rectangulos,
                                            boolean conRectangulos) {
        DatosGrafica2D grafica = graficaService.graficarRegion(expresion, null, a, b,
                expresion.variable(), solicitud.curvasDelEnunciado());

        if (conRectangulos) {
            graficaService.agregarRectangulos(grafica,
                    new SumaRiemann(solicitud.posicionRiemann()), expresion.comoFuncion(), a, b,
                    rectangulos);
        }
        return grafica;
    }
}
