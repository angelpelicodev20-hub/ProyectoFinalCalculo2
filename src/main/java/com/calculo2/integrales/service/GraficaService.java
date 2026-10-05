package com.calculo2.integrales.service;

import com.calculo2.integrales.math.algebra.Despejador;
import com.calculo2.integrales.math.algebra.ResolutorEcuaciones;
import com.calculo2.integrales.math.integracion.SumaRiemann;
import com.calculo2.integrales.math.parser.EvaluadorExpresion;
import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.math.parser.NodoExpresion;
import com.calculo2.integrales.math.util.BuscadorRaices;
import com.calculo2.integrales.math.util.Redondeo;
import com.calculo2.integrales.math.util.ValidadorFuncion;
import com.calculo2.integrales.model.DatosGrafica2D;
import com.calculo2.integrales.model.EjeRotacion;
import com.calculo2.integrales.util.ArbolJson;
import com.calculo2.integrales.util.Constantes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Prepara lo que el navegador necesita para dibujar la region.
 *
 * <p>La grafica es interactiva: se puede alejar, acercar y desplazar como en GeoGebra.
 * Para eso no bastan unos puntos calculados de antemano, porque al alejar la vista hay que
 * dibujar la curva en zonas nuevas. Por eso cada curva viaja con su arbol ya interpretado
 * por el parser de Java ({@link ArbolJson}), y el navegador lo evalua donde haga falta.
 * La lectura de la expresion sigue estando en un solo lugar.</p>
 *
 * <p>Ademas se mandan puntos ya muestreados, en coordenadas reales del plano, por si el
 * navegador no pudiera evaluar el arbol, y para calcular la ventana inicial.</p>
 *
 * <p>Las regiones descritas respecto de y ({@code x = g(y)}) se dibujan en su sitio: la
 * curva {@code x = y^2} es una parabola que abre hacia la derecha, no hacia arriba. La
 * variable de integracion decide como se recorre la region, no como se orienta el
 * plano.</p>
 */
public final class GraficaService {

    /**
     * Cuanto se extiende el dibujo mas alla del intervalo de integracion, como fraccion
     * de su ancho. Ver un poco de curva a los lados ayuda a entender la region.
     */
    private static final double EXTENSION = 0.25;

    /** Margen que se deja alrededor de la curva dentro del canvas. */
    private static final double MARGEN_VENTANA = 0.1;

    /** Cuantos rectangulos de Riemann se dibujan como maximo. */
    private static final int MAXIMO_RECTANGULOS_DIBUJADOS = 60;

    // ------------------------------------------------------------------
    // REGION DE UN EJERCICIO
    // ------------------------------------------------------------------

    /**
     * Arma la grafica de la region entre dos bordes.
     *
     * @param primera            un borde de la region
     * @param segunda            el otro, o null si el otro borde es el eje (cero)
     * @param a                  limite inferior
     * @param b                  limite superior
     * @param variable           variable de integracion
     * @param curvasDelEnunciado ecuaciones del enunciado para dibujarlas completas
     * @return los datos de la grafica
     */
    public DatosGrafica2D graficarRegion(EvaluadorExpresion primera, EvaluadorExpresion segunda,
                                         double a, double b, String variable,
                                         List<String> curvasDelEnunciado) {
        DatosGrafica2D datos = new DatosGrafica2D();
        datos.fijarLimites(a, b);
        datos.fijarVariable(variable);

        String otra = variable.equals("x") ? "y" : "x";
        String etiquetaPrimera = otra + " = " + primera.expresionOriginal();
        String etiquetaSegunda = segunda == null ? "" : otra + " = " + segunda.expresionOriginal();
        datos.fijarEtiquetas(etiquetaPrimera, etiquetaSegunda);

        // ---------- Puntos muestreados ----------
        muestrear(primera.comoFuncion(), a, b, variable, datos::agregarPuntoF);
        if (segunda != null) {
            muestrear(segunda.comoFuncion(), a, b, variable, datos::agregarPuntoG);
        }

        // ---------- Curvas para la grafica interactiva ----------
        int indicePrimera = datos.agregarFuncion(etiquetaPrimera, primera.expresionOriginal(),
                variable, ArbolJson.aMapa(primera.raiz()), "borde", "f");
        int indiceSegunda = segunda == null ? -1
                : datos.agregarFuncion(etiquetaSegunda, segunda.expresionOriginal(), variable,
                        ArbolJson.aMapa(segunda.raiz()), "borde", "g");

        // ---------- Region y sus tramos ----------
        FuncionMatematica uno = primera.comoFuncion();
        FuncionMatematica otro = segunda == null ? FuncionMatematica.cero() : segunda.comoFuncion();
        FuncionMatematica diferencia = uno.menos(otro);
        List<Double> cortes = BuscadorRaices.cortesDeIntegracion(diferencia, a, b);
        List<Map<String, Object>> tramos = new ArrayList<>();
        for (int i = 0; i + 1 < cortes.size(); i++) {
            double desde = cortes.get(i);
            double hasta = cortes.get(i + 1);
            if (hasta - desde < 1e-12) {
                continue;
            }
            double medio = (desde + hasta) / 2.0;
            Map<String, Object> tramo = new LinkedHashMap<>();
            tramo.put("desde", desde);
            tramo.put("hasta", hasta);
            tramo.put("primeraArriba", ValidadorFuncion.evaluarSeguro(diferencia, medio) >= 0);
            tramos.add(tramo);
        }
        datos.fijarRegion(variable, a, b, indicePrimera, indiceSegunda, tramos);

        // ---------- Puntos de corte ----------
        // Con los bordes del dominio incluidos: la elipse toca el eje justo donde la raiz
        // deja de existir, y ahi no hay cambio de signo que detectar.
        double margen = Math.max(1e-9, (b - a) * 1e-6);
        for (double corte : ResolutorEcuaciones.raicesNumericas(diferencia, a - margen, b + margen)) {
            double valor = ValidadorFuncion.evaluarSeguro(uno, corte);
            agregarPunto(datos, variable, corte, valor, segunda == null ? "corte" : "interseccion");
        }
        // Las esquinas de la region en los limites, donde la curva toca la recta.
        for (double limite : new double[]{a, b}) {
            double enUna = ValidadorFuncion.evaluarSeguro(uno, limite);
            double enOtra = ValidadorFuncion.evaluarSeguro(otro, limite);
            if (ValidadorFuncion.esUtilizable(enUna) && Math.abs(enUna - enOtra) > 1e-9) {
                agregarPunto(datos, variable, limite, enUna, "esquina");
            }
        }

        // ---------- Rectas de los limites ----------
        boolean vertical = variable.equals("x");
        datos.agregarRecta(vertical, a, "a = " + Redondeo.texto(a), "limite");
        datos.agregarRecta(vertical, b, "b = " + Redondeo.texto(b), "limite");

        // ---------- Curvas completas del enunciado ----------
        agregarCurvasDelEnunciado(datos, curvasDelEnunciado,
                List.of(primera.expresionOriginal(), segunda == null ? "" : segunda.expresionOriginal()),
                variable);

        datos.calcularVentana(MARGEN_VENTANA);
        return datos;
    }

    /**
     * Arma la grafica plana de la region que se hace girar.
     *
     * <p>El visor 3D ensena como es el solido, pero no de donde salio. Lo que hay que
     * entender para resolver el ejercicio esta en el plano: que region es, entre que
     * curvas queda, donde empieza y donde termina, y a que distancia del eje esta cada
     * borde. Esa distancia es el radio de la formula.</p>
     *
     * <p>Cuando el solido no tiene curva interior, la region llega hasta el eje de giro si
     * se rebana perpendicular a el (discos), o hasta el eje X si se rebana en la otra
     * direccion (capas), y ese borde se dibuja tambien.</p>
     *
     * @param exterior           curva del radio exterior, o la de arriba en capas
     * @param interior           la otra curva, o null
     * @param eje                eje de giro
     * @param a                  limite inferior
     * @param b                  limite superior
     * @param variable           variable de integracion
     * @param curvasDelEnunciado ecuaciones del enunciado, para dibujarlas completas
     * @return los datos de la grafica
     */
    public DatosGrafica2D graficarRegionDelSolido(EvaluadorExpresion exterior, EvaluadorExpresion interior,
                                                  EjeRotacion eje, double a, double b, String variable,
                                                  List<String> curvasDelEnunciado) {
        EvaluadorExpresion segunda = interior;
        if (segunda == null) {
            // Rebanando perpendicular al eje (discos), la region llega hasta el eje.
            boolean discos = eje.esHorizontal() == variable.equals("x");
            if (discos && !eje.pasaPorElOrigen()) {
                segunda = EvaluadorExpresion.compilar(Redondeo.texto(eje.desplazamiento()), variable);
            }
        }
        DatosGrafica2D datos = graficarRegion(exterior, segunda, a, b, variable, curvasDelEnunciado);
        datos.fijarEjeRevolucion(eje);
        datos.calcularVentana(MARGEN_VENTANA);
        return datos;
    }

    // ------------------------------------------------------------------
    // VISTA PREVIA
    // ------------------------------------------------------------------

    /**
     * Arma la grafica de una sola curva con su region sombreada.
     *
     * @param funcion  funcion a dibujar
     * @param a        limite inferior de integracion
     * @param b        limite superior
     * @param etiqueta nombre de la curva para la leyenda
     * @return los datos de la grafica
     */
    public DatosGrafica2D graficarUnaCurva(FuncionMatematica funcion, double a, double b,
                                           String etiqueta) {
        DatosGrafica2D datos = new DatosGrafica2D();
        datos.fijarLimites(a, b);
        datos.fijarEtiquetas(etiqueta, "");
        muestrear(funcion, a, b, "x", datos::agregarPuntoF);
        datos.calcularVentana(MARGEN_VENTANA);
        return datos;
    }

    /**
     * Arma la grafica de dos curvas y la region que queda entre ellas.
     *
     * @param funcionF  primera curva
     * @param funcionG  segunda curva
     * @param a         limite inferior de integracion
     * @param b         limite superior
     * @param etiquetaF nombre de la primera curva
     * @param etiquetaG nombre de la segunda
     * @return los datos de la grafica
     */
    public DatosGrafica2D graficarDosCurvas(FuncionMatematica funcionF, FuncionMatematica funcionG,
                                            double a, double b,
                                            String etiquetaF, String etiquetaG) {
        DatosGrafica2D datos = new DatosGrafica2D();
        datos.fijarLimites(a, b);
        datos.fijarEtiquetas(etiquetaF, etiquetaG);
        muestrear(funcionF, a, b, "x", datos::agregarPuntoF);
        muestrear(funcionG, a, b, "x", datos::agregarPuntoG);
        datos.calcularVentana(MARGEN_VENTANA);
        return datos;
    }

    /**
     * Agrega los rectangulos de la suma de Riemann para que se vean sobre la curva.
     *
     * <p>Cuando el usuario pide muchas particiones se dibuja solo una muestra: mil
     * rectangulos en pantalla se ven como una mancha y hacen lenta la grafica. El calculo
     * del area si usa todas las particiones que se pidieron.</p>
     *
     * @param datos       grafica a la que se agregan
     * @param riemann     variante de Riemann elegida
     * @param funcion     funcion que se esta integrando
     * @param a           limite inferior
     * @param b           limite superior
     * @param particiones particiones que pidio el usuario
     */
    public void agregarRectangulos(DatosGrafica2D datos, SumaRiemann riemann,
                                   FuncionMatematica funcion, double a, double b,
                                   int particiones) {
        int cuantosDibujar = Math.min(particiones, MAXIMO_RECTANGULOS_DIBUJADOS);

        for (SumaRiemann.Rectangulo rectangulo :
                riemann.generarRectangulos(funcion, a, b, cuantosDibujar)) {

            if (Double.isNaN(rectangulo.altura())) {
                continue;
            }
            datos.agregarRectangulo(
                    rectangulo.xIzquierda(),
                    rectangulo.base(),
                    rectangulo.altura(),
                    rectangulo.xMuestra());
        }
    }

    // ------------------------------------------------------------------
    // APOYO
    // ------------------------------------------------------------------

    /** Recibe cada punto muestreado de una curva. */
    @FunctionalInterface
    private interface ReceptorDePuntos {
        void recibir(double x, double y);
    }

    /**
     * Evalua la funcion en puntos repartidos por el rango visible.
     *
     * <p>Los puntos donde la funcion no esta definida se envian igual, con {@code y} en
     * NaN, para que el dibujo levante el lapiz en lugar de unir dos tramos que no se
     * tocan. Si la region se describe respecto de y, cada punto se manda ya colocado en
     * el plano: la abscisa es el valor de la funcion y la ordenada la variable.</p>
     */
    private void muestrear(FuncionMatematica funcion, double a, double b, String variable,
                           ReceptorDePuntos receptor) {
        double inicio = Math.min(a, b);
        double fin = Math.max(a, b);
        double ancho = fin - inicio;

        double desde = inicio - ancho * EXTENSION;
        double hasta = fin + ancho * EXTENSION;
        double paso = (hasta - desde) / Constantes.PUNTOS_CURVA_2D;

        for (int i = 0; i <= Constantes.PUNTOS_CURVA_2D; i++) {
            double v = desde + i * paso;
            double valor = ValidadorFuncion.evaluarSeguro(funcion, v);
            double util = ValidadorFuncion.esUtilizable(valor) ? valor : Double.NaN;
            if (variable.equals("y")) {
                receptor.recibir(util, v);
            } else {
                receptor.recibir(v, util);
            }
        }
    }

    /** Agrega un punto destacado, colocandolo en el plano segun la variable. */
    private void agregarPunto(DatosGrafica2D datos, String variable, double v, double valor,
                              String tipo) {
        if (!ValidadorFuncion.esUtilizable(valor)) {
            return;
        }
        double x = variable.equals("x") ? v : valor;
        double y = variable.equals("x") ? valor : v;
        datos.agregarPunto(x, y, "(" + Redondeo.texto(x) + ", " + Redondeo.texto(y) + ")", tipo);
    }

    /**
     * Agrega las curvas del enunciado tal como se escribieron, completas.
     *
     * <p>La region solo usa un trozo de cada curva: de {@code y^2 = 8x} quiza solo la rama
     * de arriba, de la elipse solo un cuarto. Pero entender por que la region es esa exige
     * ver la curva entera, como la dibujaria GeoGebra al escribir la ecuacion.</p>
     */
    private void agregarCurvasDelEnunciado(DatosGrafica2D datos, List<String> ecuaciones,
                                           List<String> yaDibujadas, String variable) {
        for (String ecuacion : ecuaciones) {
            int igual = ecuacion.indexOf('=');
            if (igual <= 0) {
                continue;
            }
            String izquierda = ecuacion.substring(0, igual).trim();
            String derecha = ecuacion.substring(igual + 1).trim();
            String izquierdaMinuscula = izquierda.toLowerCase();

            // Una recta x = k o y = k.
            Optional<Double> valor = numero(derecha);
            if ((izquierdaMinuscula.equals("x") || izquierdaMinuscula.equals("y")) && valor.isPresent()) {
                datos.agregarRecta(izquierdaMinuscula.equals("x"), valor.get(),
                        izquierdaMinuscula + " = " + Redondeo.texto(valor.get()), "frontera");
                continue;
            }

            // Una funcion declarada como f(x) = ... se trata como y = ...
            if (izquierda.matches("[a-zA-Z]\\s*\\(\\s*x\\s*\\)")) {
                izquierda = "y";
            }

            // Se despeja primero en la variable de la region, para reconocer las ramas que
            // ya estan dibujadas como borde y no repetirlas: x = y^2 en una region
            // descrita respecto de y es exactamente el borde x = y^2.
            String despejada = variable.equals("x") ? "y" : "x";
            String alternativa = variable.equals("x") ? "x" : "y";
            Optional<Despejador.Despeje> despeje = Despejador.despejar(izquierda, derecha, despejada);
            if (despeje.isEmpty()) {
                despeje = Despejador.despejar(izquierda, derecha, alternativa);
            }
            if (despeje.isEmpty()) {
                continue;
            }
            String variableDeLaCurva = despeje.get().otraVariable();
            boolean rotulada = false;
            for (Despejador.Rama rama : despeje.get().ramas()) {
                if (variableDeLaCurva.equals(variable) && yaDibujadas.contains(rama.expresion())) {
                    continue;
                }
                try {
                    NodoExpresion arbol = EvaluadorExpresion.compilar(rama.expresion(), variableDeLaCurva).raiz();
                    datos.agregarFuncion(rotulada ? "" : ecuacion, rama.expresion(), variableDeLaCurva,
                            ArbolJson.aMapa(arbol), "contexto", "contexto");
                    rotulada = true;
                } catch (RuntimeException e) {
                    // Una rama que no se puede leer simplemente no se dibuja.
                }
            }
        }
    }

    private Optional<Double> numero(String texto) {
        try {
            double valor = EvaluadorExpresion.compilar(texto, "_").evaluar(0);
            return Double.isFinite(valor) ? Optional.of(valor) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
