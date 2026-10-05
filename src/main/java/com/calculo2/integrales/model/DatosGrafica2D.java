package com.calculo2.integrales.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Todo lo que el navegador necesita para dibujar la grafica en el canvas.
 *
 * <p>El backend no dibuja: calcula los puntos y los manda. El JavaScript solo traduce
 * coordenadas matematicas a pixeles y pinta. Repartir asi el trabajo evita duplicar en
 * el navegador la logica de evaluar funciones.</p>
 */
public final class DatosGrafica2D {

    private final List<Punto2D> curvaF = new ArrayList<>();
    private final List<Punto2D> curvaG = new ArrayList<>();
    private final List<Punto2D> intersecciones = new ArrayList<>();
    private final List<Map<String, Object>> rectangulos = new ArrayList<>();

    private String etiquetaF = "f(x)";
    private String etiquetaG = "g(x)";
    private double limiteInferior;
    private double limiteSuperior;

    /** Nombre de la variable horizontal, para rotular la grafica. */
    private String variable = "x";

    /**
     * La recta alrededor de la cual gira la region, cuando el ejercicio es de volumen.
     *
     * <p>Va en la grafica plana y no solo en el visor 3D porque es la que explica el
     * solido: el radio de cada rebanada es la distancia de la curva a esta recta, y sin
     * verla dibujada esa distancia no se sabe desde donde se mide.</p>
     */
    private EjeRotacion ejeRevolucion;

    /**
     * Las curvas que dibuja la grafica interactiva, cada una con su arbol para que el
     * navegador la evalue en cualquier zona de la vista.
     */
    private final List<Map<String, Object>> funciones = new ArrayList<>();

    /** La region sombreada: entre que funciones, en que variable y en que tramos. */
    private Map<String, Object> region;

    /** Puntos destacados: cortes entre curvas, cortes con el eje. */
    private final List<Map<String, Object>> puntos = new ArrayList<>();

    /** Rectas: los limites de integracion y las fronteras rectas del enunciado. */
    private final List<Map<String, Object>> rectas = new ArrayList<>();

    private double xMinimo = -10;
    private double xMaximo = 10;
    private double yMinimo = -10;
    private double yMaximo = 10;

    /** Agrega un punto a la curva principal. */
    public void agregarPuntoF(double x, double y) {
        curvaF.add(new Punto2D(x, y));
    }

    /** Agrega un punto a la segunda curva. */
    public void agregarPuntoG(double x, double y) {
        curvaG.add(new Punto2D(x, y));
    }

    /**
     * Agrega una curva para la grafica interactiva.
     *
     * @param etiqueta  como se nombra en la grafica: "y = x^2", "x = y^2/8"
     * @param expresion la expresion, en la variable indicada
     * @param variable  "x" si es y = f(x), "y" si es x = g(y)
     * @param arbol     el arbol ya convertido a mapa
     * @param papel     "borde" si limita la region, "contexto" si solo se muestra
     * @param color     "f", "g" o "contexto"
     * @return la posicion de la curva, para referirse a ella desde la region
     */
    public int agregarFuncion(String etiqueta, String expresion, String variable,
                              Map<String, Object> arbol, String papel, String color) {
        Map<String, Object> funcion = new LinkedHashMap<>();
        funcion.put("etiqueta", etiqueta);
        funcion.put("expresion", expresion);
        funcion.put("variable", variable);
        funcion.put("arbol", arbol);
        funcion.put("papel", papel);
        funcion.put("color", color);
        funciones.add(funcion);
        return funciones.size() - 1;
    }

    /**
     * Describe la region sombreada.
     *
     * @param variable  variable de integracion
     * @param desde     limite inferior
     * @param hasta     limite superior
     * @param primera   posicion de una de las funciones que la limitan
     * @param segunda   posicion de la otra, o -1 si la otra frontera es el eje (cero)
     * @param tramos    tramos en que cambia cual va arriba
     */
    public void fijarRegion(String variable, double desde, double hasta, int primera, int segunda,
                            List<Map<String, Object>> tramos) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("variable", variable);
        mapa.put("desde", desde);
        mapa.put("hasta", hasta);
        mapa.put("primera", primera);
        mapa.put("segunda", segunda);
        mapa.put("tramos", tramos);
        this.region = mapa;
    }

    /** Agrega un punto destacado, en coordenadas x, y. */
    public void agregarPunto(double x, double y, String etiqueta, String tipo) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            return;
        }
        Map<String, Object> punto = new LinkedHashMap<>();
        punto.put("x", x);
        punto.put("y", y);
        punto.put("etiqueta", etiqueta);
        punto.put("tipo", tipo);
        puntos.add(punto);
    }

    /**
     * Agrega una recta a la grafica.
     *
     * @param vertical true si es x = valor, false si es y = valor
     * @param valor    donde corta
     * @param etiqueta como se rotula
     * @param tipo     "limite" o "frontera"
     */
    public void agregarRecta(boolean vertical, double valor, String etiqueta, String tipo) {
        Map<String, Object> recta = new LinkedHashMap<>();
        recta.put("orientacion", vertical ? "VERTICAL" : "HORIZONTAL");
        recta.put("valor", valor);
        recta.put("etiqueta", etiqueta);
        recta.put("tipo", tipo);
        rectas.add(recta);
    }

    /** Marca un punto donde las dos curvas se cortan. */
    public void agregarInterseccion(double x, double y) {
        intersecciones.add(new Punto2D(x, y));
    }

    /**
     * Agrega un rectangulo de la suma de Riemann para que se dibuje sobre la curva.
     *
     * @param xIzquierda borde izquierdo de la base
     * @param base       ancho del rectangulo
     * @param altura     valor de la funcion en el punto de muestra
     * @param xMuestra   punto donde se midio la altura
     */
    public void agregarRectangulo(double xIzquierda, double base, double altura, double xMuestra) {
        Map<String, Object> rectangulo = new LinkedHashMap<>();
        rectangulo.put("x", xIzquierda);
        rectangulo.put("base", base);
        rectangulo.put("altura", altura);
        rectangulo.put("xMuestra", xMuestra);
        rectangulos.add(rectangulo);
    }

    /**
     * Fija los limites de integracion, que se dibujan como dos rectas verticales.
     */
    public void fijarLimites(double inferior, double superior) {
        this.limiteInferior = inferior;
        this.limiteSuperior = superior;
    }

    /** Cambia los nombres que aparecen en la leyenda. */
    public void fijarEtiquetas(String etiquetaF, String etiquetaG) {
        this.etiquetaF = etiquetaF;
        this.etiquetaG = etiquetaG;
    }

    /** Guarda el nombre de la variable horizontal. */
    public void fijarVariable(String variable) {
        this.variable = (variable == null || variable.isBlank()) ? "x" : variable;
    }

    /**
     * Guarda el eje de revolucion para que se dibuje sobre la region.
     *
     * @param eje la recta de giro, o null si el ejercicio no es de volumen
     */
    public void fijarEjeRevolucion(EjeRotacion eje) {
        this.ejeRevolucion = eje;
    }

    /**
     * Calcula la ventana visible a partir de los puntos que ya se agregaron.
     *
     * <p>Se toma el rango real de la curva y se le deja un margen alrededor, para que la
     * region sombreada no quede pegada al borde del canvas. Si la funcion es constante,
     * se abre una ventana artificial: sin esto, el rango vertical seria cero y no se
     * podria dibujar nada.</p>
     *
     * @param margen fraccion del rango que se deja libre a cada lado, por ejemplo 0.1
     */
    public void calcularVentana(double margen) {
        double xMin = Double.POSITIVE_INFINITY;
        double xMax = Double.NEGATIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;

        for (List<Punto2D> curva : List.of(curvaF, curvaG)) {
            for (Punto2D punto : curva) {
                if (!punto.esDibujable()) {
                    continue;
                }
                xMin = Math.min(xMin, punto.x());
                xMax = Math.max(xMax, punto.x());
                yMin = Math.min(yMin, punto.y());
                yMax = Math.max(yMax, punto.y());
            }
        }

        if (xMin > xMax) {
            // No hubo ningun punto dibujable; se deja la ventana por defecto.
            return;
        }

        // El intervalo de integracion siempre tiene que verse completo, en el eje de la
        // variable con que se integra.
        if (variable.equals("y")) {
            yMin = Math.min(yMin, Math.min(limiteInferior, limiteSuperior));
            yMax = Math.max(yMax, Math.max(limiteInferior, limiteSuperior));
        } else {
            xMin = Math.min(xMin, Math.min(limiteInferior, limiteSuperior));
            xMax = Math.max(xMax, Math.max(limiteInferior, limiteSuperior));
        }

        // Los dos ejes deben verse: son la referencia de toda la grafica.
        yMin = Math.min(yMin, 0.0);
        yMax = Math.max(yMax, 0.0);
        xMin = Math.min(xMin, 0.0);
        xMax = Math.max(xMax, 0.0);

        // Y el eje de giro tambien, aunque caiga fuera de la region. Si queda fuera de
        // la ventana no se ve desde donde se mide el radio, que es lo unico que la
        // grafica de un solido tiene que dejar claro.
        if (ejeRevolucion != null) {
            if (ejeRevolucion.esHorizontal()) {
                yMin = Math.min(yMin, ejeRevolucion.desplazamiento());
                yMax = Math.max(yMax, ejeRevolucion.desplazamiento());
            } else {
                xMin = Math.min(xMin, ejeRevolucion.desplazamiento());
                xMax = Math.max(xMax, ejeRevolucion.desplazamiento());
            }
        }

        double anchoX = Math.max(xMax - xMin, 1e-6);
        double anchoY = Math.max(yMax - yMin, 1e-6);

        this.xMinimo = xMin - anchoX * margen;
        this.xMaximo = xMax + anchoX * margen;
        this.yMinimo = yMin - anchoY * margen;
        this.yMaximo = yMax + anchoY * margen;
    }

    /** Arma el objeto que se envia al navegador. */
    public Map<String, Object> aMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("curvaF", puntosAMapa(curvaF));
        mapa.put("curvaG", puntosAMapa(curvaG));
        mapa.put("intersecciones", puntosAMapa(intersecciones));
        mapa.put("rectangulos", rectangulos);
        mapa.put("etiquetaF", etiquetaF);
        mapa.put("etiquetaG", etiquetaG);
        mapa.put("limiteInferior", limiteInferior);
        mapa.put("limiteSuperior", limiteSuperior);
        mapa.put("variable", variable);
        mapa.put("ejeRevolucion", ejeRevolucion == null ? null : ejeRevolucion.aMapa());
        mapa.put("ventana", ventanaAMapa());
        mapa.put("funciones", funciones);
        mapa.put("region", region);
        mapa.put("puntos", puntos);
        mapa.put("rectas", rectas);
        return mapa;
    }

    private Map<String, Object> ventanaAMapa() {
        Map<String, Object> ventana = new LinkedHashMap<>();
        ventana.put("xMinimo", xMinimo);
        ventana.put("xMaximo", xMaximo);
        ventana.put("yMinimo", yMinimo);
        ventana.put("yMaximo", yMaximo);
        return ventana;
    }

    private static List<Map<String, Object>> puntosAMapa(List<Punto2D> puntos) {
        List<Map<String, Object>> lista = new ArrayList<>(puntos.size());
        for (Punto2D punto : puntos) {
            lista.add(punto.aMapa());
        }
        return lista;
    }

    /** Los puntos de la curva principal. */
    public List<Punto2D> curvaF() {
        return curvaF;
    }

    /** Los puntos de la segunda curva. */
    public List<Punto2D> curvaG() {
        return curvaG;
    }
}
