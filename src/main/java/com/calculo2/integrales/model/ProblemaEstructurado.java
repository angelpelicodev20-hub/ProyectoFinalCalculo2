package com.calculo2.integrales.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El ejercicio ya interpretado, con cada dato en su sitio.
 *
 * <p>Es la pieza central de la arquitectura. Da igual si el estudiante escribio el
 * problema en palabras o si lleno el formulario a mano: en los dos casos se termina aqui,
 * y de aqui salen los tres consumidores — los campos de la pantalla, el solucionador y la
 * grafica. Como los tres leen el mismo objeto, no puede ocurrir que el formulario diga
 * {@code [0, 1]} y el procedimiento integre entre otros limites.</p>
 *
 * <p>Los datos numericos se guardan como {@link Double} y no como {@code double} a
 * proposito. Hace falta distinguir "el limite inferior vale cero" de "no se pudo
 * determinar el limite inferior", y con un {@code double} las dos cosas serian el mismo
 * cero. Un dato ausente queda en {@code null} y se anota en {@link #faltantes()}, nunca
 * se rellena con un valor inventado.</p>
 */
public final class ProblemaEstructurado {

    private String textoOriginal = "";
    private TemaProblema tema = TemaProblema.DESCONOCIDO;

    private String funcionF = "";
    private String funcionG = "";
    private String variable = "x";

    private Double limiteInferior;
    private Double limiteSuperior;

    private EjeRotacion eje;
    private TipoSolido tipoSolido;

    /** El metodo que pide el enunciado, que no tiene por que ser el que sugiere la geometria. */
    private TipoSolido metodoSolicitado;

    /** El metodo que corresponderia por la forma de la region. */
    private TipoSolido metodoSugerido;

    private boolean buscarLimites;

    /** Como se dedujeron los limites, para poder mostrarlo en vez de solo darlos. */
    private String explicacionLimites = "";

    /**
     * Como se armo la region: que curvas hubo que despejar, que rama se uso, por que se
     * integra respecto de x o de y y cual borde va arriba.
     */
    private String explicacionRegion = "";

    /** Las curvas del enunciado, con sus despejes, tal como se leyeron. */
    private final List<Curva> curvas = new ArrayList<>();

    /** Los puntos de corte que cierran la region, con sus coordenadas. */
    private final List<Map<String, Object>> puntos = new ArrayList<>();

    private final List<Frontera> fronteras = new ArrayList<>();
    private final List<String> faltantes = new ArrayList<>();
    private final List<String> evidencias = new ArrayList<>();
    private final List<String> avisos = new ArrayList<>();
    private final List<String> ambiguedades = new ArrayList<>();

    /**
     * De donde salio cada dato.
     *
     * <p>Se guarda por separado del valor porque son dos cosas distintas: el intervalo
     * {@code [0, 1]} es el mismo numero lo escriba el enunciado o lo devuelva una ecuacion,
     * pero lo que hay que mostrarle al estudiante no es lo mismo en los dos casos.</p>
     */
    private final Map<String, OrigenDato> origenes = new LinkedHashMap<>();

    // ------------------------------------------------------------------
    // ESCRITURA
    // ------------------------------------------------------------------

    /** Guarda el enunciado tal como lo escribio el estudiante. */
    public void fijarTextoOriginal(String texto) {
        this.textoOriginal = texto == null ? "" : texto.trim();
    }

    /** Guarda el tema identificado. */
    public void fijarTema(TemaProblema tema) {
        this.tema = tema == null ? TemaProblema.DESCONOCIDO : tema;
    }

    /** Guarda la funcion principal, tal como aparece en el enunciado. */
    public void fijarFuncionF(String funcionF) {
        this.funcionF = funcionF == null ? "" : funcionF.trim();
    }

    /** Guarda la segunda funcion. */
    public void fijarFuncionG(String funcionG) {
        this.funcionG = funcionG == null ? "" : funcionG.trim();
    }

    /** Guarda el nombre de la variable independiente del enunciado. */
    public void fijarVariable(String variable) {
        this.variable = (variable == null || variable.isBlank()) ? "x" : variable.trim();
    }

    /** Guarda los limites de integracion; cualquiera puede quedar en null. */
    public void fijarLimites(Double inferior, Double superior) {
        this.limiteInferior = inferior;
        this.limiteSuperior = superior;
    }

    /** Guarda el eje de revolucion. */
    public void fijarEje(EjeRotacion eje) {
        this.eje = eje;
    }

    /** Guarda el metodo geometrico elegido para el solido. */
    public void fijarTipoSolido(TipoSolido tipoSolido) {
        this.tipoSolido = tipoSolido;
    }

    /** Marca que los limites deben buscarse a partir de las intersecciones. */
    public void fijarBuscarLimites(boolean buscarLimites) {
        this.buscarLimites = buscarLimites;
    }

    /** Guarda el metodo que el propio enunciado pide aplicar. */
    public void fijarMetodoSolicitado(TipoSolido metodo) {
        this.metodoSolicitado = metodo;
    }

    /** Guarda el metodo que corresponderia por la geometria de la region. */
    public void fijarMetodoSugerido(TipoSolido metodo) {
        this.metodoSugerido = metodo;
    }

    /** Guarda el razonamiento con el que se obtuvieron los limites. */
    public void fijarExplicacionLimites(String explicacion) {
        this.explicacionLimites = explicacion == null ? "" : explicacion;
    }

    /** Guarda como se armo la region. */
    public void fijarExplicacionRegion(String explicacion) {
        this.explicacionRegion = explicacion == null ? "" : explicacion;
    }

    /** Agrega una curva del enunciado. */
    public void agregarCurva(Curva curva) {
        if (curva != null) {
            curvas.add(curva);
        }
    }

    /** Agrega un punto de corte de la region. */
    public void agregarPunto(double x, double y, String texto) {
        Map<String, Object> punto = new LinkedHashMap<>();
        punto.put("x", x);
        punto.put("y", y);
        punto.put("texto", texto);
        puntos.add(punto);
    }

    /** Agrega una recta que cierra la region. */
    public void agregarFrontera(Frontera frontera) {
        if (frontera != null && !fronteras.contains(frontera)) {
            fronteras.add(frontera);
        }
    }

    /**
     * Registra de donde salio un dato.
     *
     * @param dato   nombre del dato: "limites", "eje", "funcionF", "metodo"
     * @param origen si estaba escrito, se dedujo, se interpreto o falta
     */
    public void fijarOrigen(String dato, OrigenDato origen) {
        origenes.put(dato, origen);
    }

    /**
     * Anota una lectura que admitia mas de una interpretacion razonable.
     *
     * <p>No es lo mismo que un dato faltante. Un dato ambiguo si se determino, y con el
     * se puede resolver; lo que ocurre es que habia otra lectura defendible y callarla
     * dejaria al estudiante sin saber que habia una decision que tomar.</p>
     *
     * @param descripcion la ambiguedad y las lecturas posibles
     */
    public void anotarAmbiguedad(String descripcion) {
        if (!ambiguedades.contains(descripcion)) {
            ambiguedades.add(descripcion);
        }
    }

    /**
     * Anota un dato que hace falta y no se pudo deducir del enunciado.
     *
     * @param descripcion que informacion falta, en palabras
     */
    public void anotarFaltante(String descripcion) {
        if (!faltantes.contains(descripcion)) {
            faltantes.add(descripcion);
        }
    }

    /**
     * Anota en que se baso el sistema para concluir algo.
     *
     * <p>Estas notas se le muestran al estudiante. La interpretacion automatica puede
     * equivocarse, y quien lee el razonamiento puede corregirlo; quien solo ve el
     * resultado, no.</p>
     *
     * @param razon el razonamiento, en palabras
     */
    public void anotarEvidencia(String razon) {
        if (!evidencias.contains(razon)) {
            evidencias.add(razon);
        }
    }

    /**
     * Anota algo que conviene revisar aunque no impida resolver.
     *
     * @param aviso el aviso, en palabras
     */
    public void anotarAviso(String aviso) {
        if (!avisos.contains(aviso)) {
            avisos.add(aviso);
        }
    }

    // ------------------------------------------------------------------
    // LECTURA
    // ------------------------------------------------------------------

    /** El enunciado original. */
    public String textoOriginal() {
        return textoOriginal;
    }

    /** El tema identificado. */
    public TemaProblema tema() {
        return tema;
    }

    /** La funcion principal. */
    public String funcionF() {
        return funcionF;
    }

    /** La segunda funcion, o cadena vacia. */
    public String funcionG() {
        return funcionG;
    }

    /** El nombre de la variable independiente. */
    public String variable() {
        return variable;
    }

    /** El limite inferior, o null si no se determino. */
    public Double limiteInferior() {
        return limiteInferior;
    }

    /** El limite superior, o null si no se determino. */
    public Double limiteSuperior() {
        return limiteSuperior;
    }

    /** El eje de revolucion, o null si no aplica o no se determino. */
    public EjeRotacion eje() {
        return eje;
    }

    /** El metodo geometrico, o null si no se determino. */
    public TipoSolido tipoSolido() {
        return tipoSolido;
    }

    /** Indica si los limites deben buscarse automaticamente. */
    public boolean buscarLimites() {
        return buscarLimites;
    }

    /** Los datos que hacen falta para poder resolver. */
    public List<String> faltantes() {
        return List.copyOf(faltantes);
    }

    /** El razonamiento que llevo a cada conclusion. */
    public List<String> evidencias() {
        return List.copyOf(evidencias);
    }

    /** Los avisos que conviene revisar. */
    public List<String> avisos() {
        return List.copyOf(avisos);
    }

    /** Las lecturas que admitian mas de una interpretacion. */
    public List<String> ambiguedades() {
        return List.copyOf(ambiguedades);
    }

    /** Las rectas que cierran la region. */
    public List<Frontera> fronteras() {
        return List.copyOf(fronteras);
    }

    /** El metodo que pide el enunciado, o null si no nombra ninguno. */
    public TipoSolido metodoSolicitado() {
        return metodoSolicitado;
    }

    /** El metodo que corresponde por la geometria, o null si no se pudo analizar. */
    public TipoSolido metodoSugerido() {
        return metodoSugerido;
    }

    /** El razonamiento con el que se obtuvieron los limites. */
    public String explicacionLimites() {
        return explicacionLimites;
    }

    /** Como se armo la region. */
    public String explicacionRegion() {
        return explicacionRegion;
    }

    /** Las curvas del enunciado. */
    public List<Curva> curvas() {
        return List.copyOf(curvas);
    }

    /** Los puntos de corte de la region. */
    public List<Map<String, Object>> puntos() {
        return List.copyOf(puntos);
    }

    /**
     * De donde salio un dato.
     *
     * @param dato nombre del dato
     * @return su origen, o {@link OrigenDato#FALTANTE} si nunca se registro
     */
    public OrigenDato origenDe(String dato) {
        return origenes.getOrDefault(dato, OrigenDato.FALTANTE);
    }

    /** Indica si hay una segunda funcion. */
    public boolean tieneSegundaFuncion() {
        return !funcionG.isBlank();
    }

    /** Indica si los dos limites quedaron determinados. */
    public boolean tieneLimites() {
        return limiteInferior != null && limiteSuperior != null;
    }

    /**
     * Indica si el problema esta listo para resolverse.
     *
     * <p>Faltar informacion no es un error: es el estado normal de un enunciado
     * incompleto, y lo que corresponde es pedirsela al estudiante.</p>
     *
     * @return true si no falta nada
     */
    public boolean estaCompleto() {
        return faltantes.isEmpty() && tema.estaDeterminado() && !funcionF.isBlank();
    }

    // ------------------------------------------------------------------
    // SALIDA
    // ------------------------------------------------------------------

    /**
     * Arma el objeto que se envia al navegador.
     *
     * <p>Los datos no determinados viajan como {@code null}, y el formulario los deja
     * vacios en lugar de escribir un cero que el estudiante podria tomar por bueno.</p>
     */
    public Map<String, Object> aMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("textoOriginal", textoOriginal);
        mapa.put("tema", tema.name());
        mapa.put("temaEtiqueta", tema.etiqueta());
        mapa.put("pagina", tema.pagina());
        mapa.put("funcionF", funcionF);
        mapa.put("funcionG", funcionG);
        mapa.put("variable", variable);
        mapa.put("limiteInferior", limiteInferior);
        mapa.put("limiteSuperior", limiteSuperior);
        mapa.put("eje", eje == null ? null : eje.aMapa());
        mapa.put("tipoSolido", tipoSolido == null ? null : tipoSolido.name());
        mapa.put("tipoSolidoEtiqueta", tipoSolido == null ? null : tipoSolido.etiqueta());
        mapa.put("buscarLimites", buscarLimites);
        mapa.put("metodoSolicitado", metodoSolicitado == null ? null : metodoSolicitado.name());
        mapa.put("metodoSolicitadoEtiqueta",
                metodoSolicitado == null ? null : metodoSolicitado.etiqueta());
        mapa.put("metodoSugerido", metodoSugerido == null ? null : metodoSugerido.name());
        mapa.put("metodoSugeridoEtiqueta",
                metodoSugerido == null ? null : metodoSugerido.etiqueta());
        mapa.put("explicacionLimites", explicacionLimites);
        mapa.put("explicacionRegion", explicacionRegion);
        mapa.put("curvas", curvasAMapa());
        mapa.put("puntos", puntos);
        mapa.put("fronteras", fronterasAMapa());
        mapa.put("origenes", origenesAMapa());
        mapa.put("faltantes", faltantes);
        mapa.put("evidencias", evidencias);
        mapa.put("avisos", avisos);
        mapa.put("ambiguedades", ambiguedades);
        mapa.put("completo", estaCompleto());
        return mapa;
    }

    /** Las curvas, con sus despejes. */
    private List<Map<String, Object>> curvasAMapa() {
        List<Map<String, Object>> lista = new ArrayList<>(curvas.size());
        for (Curva curva : curvas) {
            lista.add(curva.aMapa());
        }
        return lista;
    }

    /** Las fronteras con su ecuacion, su origen y la lectura que las produjo. */
    private List<Map<String, Object>> fronterasAMapa() {
        List<Map<String, Object>> lista = new ArrayList<>(fronteras.size());
        for (Frontera frontera : fronteras) {
            Map<String, Object> entrada = new LinkedHashMap<>();
            entrada.put("ecuacion", frontera.ecuacion());
            entrada.put("orientacion", frontera.orientacion().name());
            entrada.put("valor", frontera.valor());
            entrada.put("origen", frontera.origen().name());
            entrada.put("origenEtiqueta", frontera.origen().etiqueta());
            entrada.put("lectura", frontera.lectura());
            lista.add(entrada);
        }
        return lista;
    }

    /** El origen de cada dato, con su nombre para mostrar. */
    private Map<String, Object> origenesAMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (Map.Entry<String, OrigenDato> entrada : origenes.entrySet()) {
            Map<String, Object> detalle = new LinkedHashMap<>();
            detalle.put("clave", entrada.getValue().name());
            detalle.put("etiqueta", entrada.getValue().etiqueta());
            mapa.put(entrada.getKey(), detalle);
        }
        return mapa;
    }
}
