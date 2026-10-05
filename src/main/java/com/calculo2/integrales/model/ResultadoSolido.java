package com.calculo2.integrales.model;

import com.calculo2.integrales.math.util.Redondeo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La respuesta completa de un calculo de volumen de solido de revolucion.
 *
 * <p>Ademas del volumen y la explicacion paso a paso, lleva la malla que el navegador
 * dibuja y el area de la region plana que se hizo girar, porque compararlas ayuda a
 * entender que el volumen sale de rotar esa area.</p>
 */
public final class ResultadoSolido {

    private String funcionExterior = "";
    private String funcionInterior = "";
    private double limiteInferior;
    private double limiteSuperior;

    private double volumen;
    private double areaRegion;
    private double superficieLateral;

    private String tipoSolido = "";
    private String formulaAplicada = "";
    private String metodo = "";
    private int particiones;
    private EjeRotacion eje = EjeRotacion.ejeX();

    private final List<PasoSolucion> pasos = new ArrayList<>();
    private final List<Map<String, Object>> rebanadas = new ArrayList<>();

    private String antiderivada = "";
    private boolean resueltaAnaliticamente;
    private String explicacionLimites = "";
    private boolean limitesCalculados;
    private Map<String, Object> interpretacion;

    private DatosGrafica3D grafica = new DatosGrafica3D();

    /** La region plana que se hace girar, dibujada con los mismos datos del calculo. */
    private DatosGrafica2D region;
    private String advertencia = "";
    private long milisegundos;

    // ------------------------------------------------------------------
    // DATOS DE ENTRADA
    // ------------------------------------------------------------------

    /** Guarda las curvas ya normalizadas por el parser. */
    public void fijarFunciones(String exterior, String interior) {
        this.funcionExterior = exterior == null ? "" : exterior;
        this.funcionInterior = interior == null ? "" : interior;
    }

    /** Guarda el intervalo sobre el que se integro. */
    public void fijarLimites(double inferior, double superior) {
        this.limiteInferior = inferior;
        this.limiteSuperior = superior;
    }

    /** Guarda que metodo geometrico se aplico y con que formula. */
    public void fijarTipoSolido(TipoSolido tipo) {
        this.tipoSolido = tipo.etiqueta();
        this.formulaAplicada = tipo.formula();
    }

    /** Guarda como se resolvio la integral y con cuantos cortes se dibujo la malla. */
    public void fijarMetodo(String metodo, int particiones) {
        this.metodo = metodo;
        this.particiones = particiones;
    }

    /**
     * Guarda la primitiva encontrada.
     *
     * @param antiderivada la primitiva escrita, o cadena vacia si no se encontro
     * @param fueAnalitica true si se resolvio por la regla de Barrow
     */
    public void fijarAntiderivada(String antiderivada, boolean fueAnalitica) {
        this.antiderivada = antiderivada == null ? "" : antiderivada;
        this.resueltaAnaliticamente = fueAnalitica;
    }

    /**
     * Guarda como se dedujeron los limites, cuando los calculo el sistema.
     *
     * <p>Si los escribio el estudiante no hay nada que explicar y queda vacio.</p>
     */
    public void fijarExplicacionLimites(String explicacion) {
        this.explicacionLimites = explicacion == null ? "" : explicacion;
        this.limitesCalculados = !this.explicacionLimites.isBlank();
    }

    /**
     * Guarda como se interpreto el ejercicio.
     *
     * <p>Viaja de vuelta al navegador para que los campos, el procedimiento y la grafica
     * queden con exactamente los mismos datos.</p>
     */
    public void fijarInterpretacion(Map<String, Object> interpretacion) {
        this.interpretacion = interpretacion;
    }

    /** Guarda el eje alrededor del cual giro la region. */
    public void fijarEje(EjeRotacion eje) {
        this.eje = eje;
    }

    // ------------------------------------------------------------------
    // RESULTADOS
    // ------------------------------------------------------------------

    /**
     * Guarda los valores calculados.
     *
     * @param volumen           volumen del solido
     * @param areaRegion        area de la region plana que se hizo girar
     * @param superficieLateral area de la superficie exterior del solido
     */
    public void fijarValores(double volumen, double areaRegion, double superficieLateral) {
        this.volumen = volumen;
        this.areaRegion = areaRegion;
        this.superficieLateral = superficieLateral;
    }

    /**
     * Registra una rebanada del solido para la tabla de detalle.
     *
     * @param posicion       donde se hizo el corte
     * @param radioExterior  radio mayor de la rebanada
     * @param radioInterior  radio del agujero; cero si el solido es macizo
     * @param volumenParcial volumen aproximado de esa rebanada
     */
    public void agregarRebanada(double posicion, double radioExterior, double radioInterior,
                                double volumenParcial) {
        Map<String, Object> rebanada = new LinkedHashMap<>();
        rebanada.put("posicion", Redondeo.paraMostrar(posicion));
        rebanada.put("radioExterior", Redondeo.paraMostrar(radioExterior));
        rebanada.put("radioInterior", Redondeo.paraMostrar(radioInterior));
        rebanada.put("volumen", Redondeo.paraMostrar(volumenParcial));
        rebanadas.add(rebanada);
    }

    /** Agrega un renglon a la explicacion paso a paso. */
    public void agregarPaso(PasoSolucion paso) {
        pasos.add(paso);
    }

    /** Agrega todos los pasos de una vez. */
    public void agregarPasos(List<PasoSolucion> nuevos) {
        pasos.addAll(nuevos);
    }

    /** El volumen escrito como multiplo de pi, o cadena vacia si no sale limpio. */
    public String volumenEnPi() {
        return textoEnTerminosDePi(volumen);
    }

    /** Guarda la malla que se dibuja en el canvas. */
    /**
     * Guarda la grafica plana de la region que gira.
     *
     * <p>Acompana a la malla del solido en lugar de sustituirla. Son dos vistas del mismo
     * ejercicio y responden preguntas distintas: el solido ensena que cuerpo salio, y la
     * region ensena de donde salio y desde donde se mide cada radio.</p>
     *
     * @param region los puntos de la region, o null si no se pudo dibujar
     */
    public void fijarRegion(DatosGrafica2D region) {
        this.region = region;
    }

    public void fijarGrafica(DatosGrafica3D grafica) {
        this.grafica = grafica;
    }

    /** Guarda un aviso que el usuario deberia leer. */
    public void fijarAdvertencia(String advertencia) {
        this.advertencia = advertencia;
    }

    /** Guarda cuanto tardo el calculo. */
    public void fijarDuracion(long milisegundos) {
        this.milisegundos = milisegundos;
    }

    // ------------------------------------------------------------------
    // LECTURA
    // ------------------------------------------------------------------

    /** El volumen calculado. */
    public double volumen() {
        return volumen;
    }

    /** El area de la region que se hizo girar. */
    public double areaRegion() {
        return areaRegion;
    }

    /** Los pasos generados hasta el momento. */
    public List<PasoSolucion> pasos() {
        return pasos;
    }

    // ------------------------------------------------------------------
    // SALIDA
    // ------------------------------------------------------------------

    /** Arma el objeto que se envia al navegador. */
    public Map<String, Object> aMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("exito", true);
        mapa.put("funcionExterior", funcionExterior);
        mapa.put("funcionInterior", funcionInterior);
        mapa.put("limiteInferior", limiteInferior);
        mapa.put("limiteSuperior", limiteSuperior);
        mapa.put("volumen", Redondeo.paraMostrar(volumen));
        mapa.put("volumenTexto", Redondeo.texto(volumen));
        mapa.put("volumenEnPi", textoEnTerminosDePi(volumen));
        mapa.put("areaRegion", Redondeo.paraMostrar(areaRegion));
        mapa.put("superficieLateral", Redondeo.paraMostrar(superficieLateral));
        mapa.put("tipoSolido", tipoSolido);
        mapa.put("formulaAplicada", formulaAplicada);
        mapa.put("metodo", metodo);
        mapa.put("particiones", particiones);
        mapa.put("eje", eje.aMapa());
        mapa.put("rebanadas", rebanadas);
        mapa.put("antiderivada", antiderivada);
        mapa.put("analitica", resueltaAnaliticamente);
        mapa.put("explicacionLimites", explicacionLimites);
        mapa.put("limitesCalculados", limitesCalculados);
        mapa.put("interpretacion", interpretacion);
        mapa.put("pasos", pasosAMapa());
        mapa.put("grafica", grafica.aMapa());
        mapa.put("region", region == null ? null : region.aMapa());
        mapa.put("advertencia", advertencia);
        mapa.put("milisegundos", milisegundos);
        return mapa;
    }

    /**
     * Expresa el volumen como un multiplo de pi cuando el multiplo sale limpio.
     *
     * <p>En clase los volumenes casi siempre se dejan en la forma {@code 8*pi} en lugar de
     * {@code 25.1327}, asi que mostrar las dos versiones ayuda a comparar con el resultado
     * hecho a mano. Si el cociente no es un numero sencillo, se devuelve cadena vacia y el
     * frontend solo muestra el decimal.</p>
     */
    private static String textoEnTerminosDePi(double valor) {
        if (Math.abs(valor) < 1e-12) {
            return "0";
        }
        double cociente = valor / Math.PI;

        // Multiplo entero: 8*pi
        if (Math.abs(cociente - Math.rint(cociente)) < 1e-7) {
            long entero = Math.round(cociente);
            return entero == 1 ? "pi" : entero + "*pi";
        }

        // Fraccion sencilla: (32/5)*pi
        for (int denominador = 2; denominador <= 2000; denominador++) {
            double numerador = cociente * denominador;
            if (Math.abs(numerador - Math.rint(numerador)) < 1e-7) {
                long arriba = Math.round(numerador);
                return "(" + arriba + "/" + denominador + ")*pi";
            }
        }
        return "";
    }

    private List<Map<String, Object>> pasosAMapa() {
        List<Map<String, Object>> lista = new ArrayList<>(pasos.size());
        for (PasoSolucion paso : pasos) {
            lista.add(paso.aMapa());
        }
        return lista;
    }
}
