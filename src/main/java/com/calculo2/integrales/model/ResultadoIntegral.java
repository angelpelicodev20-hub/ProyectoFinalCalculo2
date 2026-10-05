package com.calculo2.integrales.model;

import com.calculo2.integrales.math.simbolico.Fraccion;
import com.calculo2.integrales.math.util.Redondeo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La respuesta completa de un calculo de area.
 *
 * <p>Junta las tres cosas que el usuario ve en pantalla: el numero, la explicacion paso a
 * paso y los datos de la grafica. Se arma en el servicio y se convierte a JSON en el
 * controlador.</p>
 *
 * <p>Vale la pena distinguir dos valores que suelen confundirse. La <em>integral</em> es
 * el resultado con signo: si la curva va por debajo del eje, es negativa. El <em>area</em>
 * es siempre positiva, porque un area geometrica no puede ser negativa. Cuando la funcion
 * cruza el eje dentro del intervalo los dos numeros son distintos, y mostrarlos por
 * separado es justo lo que evita el error mas comun del tema.</p>
 */
public final class ResultadoIntegral {

    private String funcionF = "";
    private String funcionG = "";
    private double limiteInferior;
    private double limiteSuperior;

    private double integral;
    private double area;
    private String metodo = "";
    private int particiones;

    private String antiderivada = "";
    private boolean resueltaAnaliticamente;
    private String explicacionLimites = "";
    private boolean limitesCalculados;

    /** Variable de integracion: x, o y si la region se describio con x = g(y). */
    private String variable = "x";

    private final List<Double> intersecciones = new ArrayList<>();
    private final List<PasoSolucion> pasos = new ArrayList<>();

    private DatosGrafica2D grafica = new DatosGrafica2D();
    private Map<String, Object> interpretacion;
    private String advertencia = "";
    private long milisegundos;

    // ------------------------------------------------------------------
    // DATOS DE ENTRADA
    // ------------------------------------------------------------------

    /** Guarda las funciones ya normalizadas por el parser. */
    public void fijarFunciones(String funcionF, String funcionG) {
        this.funcionF = funcionF == null ? "" : funcionF;
        this.funcionG = funcionG == null ? "" : funcionG;
    }

    /** Guarda el intervalo sobre el que se integro. */
    public void fijarLimites(double inferior, double superior) {
        this.limiteInferior = inferior;
        this.limiteSuperior = superior;
    }

    /** Guarda como se resolvio y con cuantos rectangulos, si hubo. */
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
     * Guarda como se dedujeron los limites.
     *
     * <p>Solo se llena cuando los calculo el sistema. Si los escribio el estudiante no
     * hay nada que explicar.</p>
     */
    public void fijarVariable(String variable) {
        this.variable = variable == null || variable.isBlank() ? "x" : variable;
    }

    public void fijarExplicacionLimites(String explicacion) {
        this.explicacionLimites = explicacion == null ? "" : explicacion;
        this.limitesCalculados = !this.explicacionLimites.isBlank();
    }

    // ------------------------------------------------------------------
    // RESULTADOS
    // ------------------------------------------------------------------

    /**
     * Guarda los dos valores del calculo.
     *
     * @param integral resultado con signo
     * @param area     area geometrica, siempre positiva
     */
    public void fijarValores(double integral, double area) {
        this.integral = integral;
        this.area = area;
    }

    /** Registra un punto donde las curvas se cortan o donde la curva cruza el eje. */
    public void agregarInterseccion(double x) {
        intersecciones.add(Redondeo.paraMostrar(x));
    }

    /** Agrega un renglon a la explicacion paso a paso. */
    public void agregarPaso(PasoSolucion paso) {
        pasos.add(paso);
    }

    /** Agrega todos los pasos de una vez. */
    public void agregarPasos(List<PasoSolucion> nuevos) {
        pasos.addAll(nuevos);
    }

    /** Guarda los datos de la grafica. */
    public void fijarGrafica(DatosGrafica2D grafica) {
        this.grafica = grafica;
    }

    /**
     * Guarda como se interpreto el ejercicio.
     *
     * <p>Viaja de vuelta al navegador para que los campos del formulario, el
     * procedimiento y la grafica queden con exactamente los mismos datos.</p>
     */
    public void fijarInterpretacion(Map<String, Object> interpretacion) {
        this.interpretacion = interpretacion;
    }

    /** Guarda un aviso que no impide calcular pero que el usuario deberia leer. */
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

    /** El resultado con signo. */
    public double integral() {
        return integral;
    }

    /** El area geometrica, siempre positiva. */
    public double area() {
        return area;
    }

    /** Los pasos generados hasta el momento. */
    public List<PasoSolucion> pasos() {
        return pasos;
    }

    /** Indica si la funcion cruza el eje o las curvas se cruzan dentro del intervalo. */
    public boolean tieneCruces() {
        return !intersecciones.isEmpty();
    }

    /** Indica si se resolvio encontrando la primitiva. */
    public boolean fueAnalitica() {
        return resueltaAnaliticamente;
    }

    /** El limite inferior usado. */
    public double limiteInferior() {
        return limiteInferior;
    }

    /** El limite superior usado. */
    public double limiteSuperior() {
        return limiteSuperior;
    }

    // ------------------------------------------------------------------
    // SALIDA
    // ------------------------------------------------------------------

    /** Arma el objeto que se envia al navegador. */
    public Map<String, Object> aMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("exito", true);
        mapa.put("funcionF", funcionF);
        mapa.put("funcionG", funcionG);
        mapa.put("variable", variable);
        mapa.put("limiteInferior", limiteInferior);
        mapa.put("limiteSuperior", limiteSuperior);
        mapa.put("integral", Redondeo.paraMostrar(integral));
        mapa.put("area", Redondeo.paraMostrar(area));
        mapa.put("integralTexto", Redondeo.texto(integral));
        mapa.put("areaTexto", Redondeo.texto(area));
        mapa.put("areaExacta", Fraccion.texto(area));
        mapa.put("metodo", metodo);
        mapa.put("particiones", particiones);
        mapa.put("antiderivada", antiderivada);
        mapa.put("analitica", resueltaAnaliticamente);
        mapa.put("explicacionLimites", explicacionLimites);
        mapa.put("limitesCalculados", limitesCalculados);
        mapa.put("intersecciones", intersecciones);
        mapa.put("pasos", pasosAMapa());
        mapa.put("grafica", grafica.aMapa());
        mapa.put("interpretacion", interpretacion);
        mapa.put("advertencia", advertencia);
        mapa.put("milisegundos", milisegundos);
        return mapa;
    }

    private List<Map<String, Object>> pasosAMapa() {
        List<Map<String, Object>> lista = new ArrayList<>(pasos.size());
        for (PasoSolucion paso : pasos) {
            lista.add(paso.aMapa());
        }
        return lista;
    }
}
