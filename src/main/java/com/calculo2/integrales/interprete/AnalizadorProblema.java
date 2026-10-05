package com.calculo2.integrales.interprete;

import com.calculo2.integrales.math.algebra.Despejador;
import com.calculo2.integrales.math.parser.EvaluadorExpresion;
import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.model.Curva;
import com.calculo2.integrales.model.EjeRotacion;
import com.calculo2.integrales.model.Frontera;
import com.calculo2.integrales.model.OrigenDato;
import com.calculo2.integrales.model.ProblemaEstructurado;
import com.calculo2.integrales.model.TemaProblema;
import com.calculo2.integrales.model.TipoSolido;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Convierte un enunciado escrito en palabras en un ejercicio estructurado.
 *
 * <p>Es la entrada del camino que describe la arquitectura del proyecto:</p>
 *
 * <pre>
 *   enunciado escrito  ->  analizador  ->  ProblemaEstructurado
 *                                              |
 *                        +---------------------+---------------------+
 *                        |                     |                     |
 *                   campos del            solucionador            grafica
 *                   formulario
 * </pre>
 *
 * <p>Los tres consumidores leen el mismo objeto, y por eso no puede pasar que el
 * formulario muestre un intervalo y el procedimiento integre en otro.</p>
 *
 * <p>El trabajo se reparte en piezas, cada una con una sola responsabilidad:</p>
 *
 * <ol>
 *   <li>{@link PreprocesadorEnunciado} corrige la escritura: guiones largos, "x2", "x ^2".</li>
 *   <li>{@link ExtractorIntervalo} lee el intervalo, se escriba como se escriba.</li>
 *   <li>{@link ExtractorEcuaciones} encuentra todas las ecuaciones, despejadas o no.</li>
 *   <li>{@link ExtractorFronteras} y {@link ExtractorEje} separan las rectas que cierran la
 *       region de la recta alrededor de la cual gira.</li>
 *   <li>{@link Despejador} escribe cada curva como funcion de x y de y.</li>
 *   <li>{@link PlanificadorRegion} decide que region es, como se rebana y entre que
 *       limites.</li>
 *   <li>{@link SelectorMetodoSolido} decide el metodo del solido midiendo la region.</li>
 * </ol>
 *
 * <p>El analizador deduce todo lo que se pueda deducir, pero no inventa nada. Resolver
 * {@code x^3 = x^2} para encontrar los limites es deducir; elegir unos limites plausibles
 * porque el enunciado no los dice seria inventar. Lo que no sale por ningun camino se pide,
 * nombrando el dato concreto que falta.</p>
 */
public final class AnalizadorProblema {

    /**
     * Analiza el enunciado.
     *
     * @param enunciado texto escrito por el estudiante
     * @return el ejercicio estructurado, con lo que falte marcado como tal
     */
    public ProblemaEstructurado analizar(String enunciado) {
        ProblemaEstructurado problema = new ProblemaEstructurado();
        problema.fijarTextoOriginal(enunciado);

        if (enunciado == null || enunciado.isBlank()) {
            problema.anotarFaltante("El enunciado del problema.");
            return problema;
        }

        // ---------- 1. Escritura ----------
        String texto = PreprocesadorEnunciado.corregir(enunciado).trim();
        String normalizado = ExtractorDatos.normalizar(texto);

        // ---------- 2. Intervalo ----------
        Optional<ExtractorIntervalo.Intervalo> intervalo = ExtractorIntervalo.buscar(normalizado);
        String textoSinIntervalo = intervalo
                .map(i -> blanquear(texto, i.inicio(), i.fin()))
                .orElse(texto);

        // ---------- 3. Ecuaciones, rectas y eje ----------
        List<ExtractorEcuaciones.EcuacionDetectada> ecuaciones =
                ExtractorEcuaciones.buscar(textoSinIntervalo);

        List<ExtractorEje.RectaNombrada> todasLasRectas =
                ExtractorFronteras.buscarTodas(normalizado, ecuaciones, intervalo);
        Optional<ExtractorEje.EjeDetectado> eje = ExtractorEje.buscar(normalizado, todasLasRectas);
        List<ExtractorEje.RectaNombrada> rectas = ExtractorFronteras.depurar(todasLasRectas, eje);

        List<Frontera> fronteras = new ArrayList<>();
        for (ExtractorEje.RectaNombrada recta : rectas) {
            fronteras.add(recta.frontera());
            problema.agregarFrontera(recta.frontera());
            problema.anotarEvidencia(recta.frontera().lectura());
            if (recta.frontera().origen() == OrigenDato.INTERPRETADO) {
                problema.anotarAmbiguedad("La frontera " + recta.frontera().ecuacion() + " no viene "
                        + "escrita como ecuacion: se dedujo del lenguaje del enunciado. "
                        + "Compruebe que es la recta que usted entiende, y tenga presente que "
                        + "una frontera de la region no es el eje de giro.");
            }
        }

        // ---------- 4. Curvas ----------
        List<Curva> curvas = construirCurvas(ecuaciones, eje, problema);
        curvas.forEach(problema::agregarCurva);

        // ---------- 5. Tema ----------
        int cuantasFunciones = curvas.size() + contarRectasHorizontalesNoNulas(fronteras);
        ClasificadorTema.Clasificacion clasificacion = ClasificadorTema.clasificar(
                normalizado, cuantasFunciones, eje.isPresent());
        problema.fijarTema(clasificacion.tema());
        clasificacion.evidencias().forEach(problema::anotarEvidencia);

        // ---------- 6. Eje ----------
        EjeRotacion ejeDeGiro = null;
        if (problema.tema() == TemaProblema.VOLUMEN_SOLIDO) {
            if (eje.isPresent()) {
                ejeDeGiro = eje.get().eje();
                problema.fijarEje(ejeDeGiro);
                problema.fijarOrigen("eje", OrigenDato.EXPLICITO);
                problema.anotarEvidencia("Para el eje de revolucion, " + eje.get().razon() + ".");
            } else {
                ejeDeGiro = deducirEjeNoEscrito(rectas, problema);
            }
        }

        // ---------- 7. Metodo pedido ----------
        if (problema.tema() == TemaProblema.VOLUMEN_SOLIDO) {
            ExtractorMetodo.buscar(normalizado).ifPresent(pedido -> {
                problema.fijarMetodoSolicitado(pedido.tipo());
                problema.anotarEvidencia(pedido.lectura());
            });
        }

        // ---------- 8. La region ----------
        Optional<PlanificadorRegion.Plan> plan = Optional.empty();
        if (!curvas.isEmpty()) {
            plan = PlanificadorRegion.planificar(armarEntrada(problema, curvas, fronteras,
                    intervalo, normalizado, ejeDeGiro));
            plan.ifPresent(encontrado -> aplicarPlan(problema, encontrado));
            if (plan.isEmpty()) {
                llenarSinRegion(problema, curvas);
            }
        }

        // ---------- 9. Metodo que corresponde por la geometria ----------
        plan.ifPresent(encontrado -> elegirMetodo(problema, encontrado));

        // ---------- 10. Que falta de verdad ----------
        anotarLoQueFalta(problema, clasificacion, curvas, plan.isPresent());

        return problema;
    }

    // ------------------------------------------------------------------
    // CURVAS
    // ------------------------------------------------------------------

    /**
     * Convierte cada ecuacion que no es una recta en una curva, despejada en las dos
     * variables cuando se puede.
     */
    private List<Curva> construirCurvas(List<ExtractorEcuaciones.EcuacionDetectada> ecuaciones,
                                        Optional<ExtractorEje.EjeDetectado> eje,
                                        ProblemaEstructurado problema) {
        List<Curva> curvas = new ArrayList<>();
        for (ExtractorEcuaciones.EcuacionDetectada ecuacion : ecuaciones) {
            if (eje.isPresent() && eje.get().contiene(ecuacion.inicio())) {
                continue;
            }
            if (esRecta(ecuacion)) {
                continue;
            }

            String izquierda = ecuacion.izquierda().trim().toLowerCase();
            String derecha = ecuacion.derecha().trim();
            String ecuacionTexto = ecuacion.nombre().isEmpty()
                    ? ecuacion.izquierda().trim() + " = " + derecha
                    : ecuacion.nombre() + "(" + ecuacion.variableDeclarada() + ") = " + derecha;

            Optional<Despejador.Despeje> enX = Despejador.despejar(izquierda, derecha, "y");
            Optional<Despejador.Despeje> enY = Despejador.despejar(izquierda, derecha, "x");

            // Una funcion declarada como f(x) se respeta tal cual: aunque se pudiera
            // despejar al reves, es una funcion de x.
            String forma;
            if (izquierda.equals("y") && !contieneVariable(derecha, "y")) {
                forma = "Y_DE_X";
            } else if (izquierda.equals("x") && !contieneVariable(derecha, "x")) {
                forma = "X_DE_Y";
            } else {
                forma = "IMPLICITA";
            }

            if (enX.isEmpty() && enY.isEmpty()) {
                problema.anotarAviso("No se pudo escribir la curva " + ecuacionTexto + " como "
                        + "funcion de x ni de y, asi que no se uso para armar la region.");
                continue;
            }

            Curva curva = new Curva(curvas.size(), ecuacion.nombre(), ecuacionTexto, forma,
                    enX.orElse(null), enY.orElse(null), ecuacion.inicio());
            curvas.add(curva);

            problema.anotarEvidencia(describirCurva(curva));
        }
        return curvas;
    }

    /** Una ecuacion del tipo x = 2 o y = -1, sin nombre de funcion. */
    private boolean esRecta(ExtractorEcuaciones.EcuacionDetectada ecuacion) {
        if (!ecuacion.nombre().isEmpty()) {
            return false;
        }
        String izquierda = ecuacion.izquierda().trim().toLowerCase();
        return (izquierda.equals("x") || izquierda.equals("y"))
                && ExtractorDatos.comoNumero(ecuacion.derecha().trim()).isPresent();
    }

    private boolean contieneVariable(String expresion, String variable) {
        try {
            return Despejador.contiene(EvaluadorExpresion.analizarConVariables(expresion,
                    java.util.Set.of("x", "y")), variable);
        } catch (RuntimeException e) {
            return true;
        }
    }

    /** La evidencia de como se leyo cada curva. */
    private String describirCurva(Curva curva) {
        if (curva.forma().equals("Y_DE_X")) {
            return "Se identifico la curva " + curva.ecuacion() + ".";
        }
        if (curva.forma().equals("X_DE_Y")) {
            return "Se identifico la curva " + curva.ecuacion() + ", que da x en funcion de y.";
        }
        return "Se identifico la curva " + curva.ecuacion() + ", que no esta despejada; se "
                + "despeja la variable que haga falta al armar la region.";
    }

    private int contarRectasHorizontalesNoNulas(List<Frontera> fronteras) {
        int cuenta = 0;
        for (Frontera frontera : fronteras) {
            if (!frontera.esVertical() && Math.abs(frontera.valor()) > 1e-12) {
                cuenta++;
            }
        }
        return cuenta;
    }

    // ------------------------------------------------------------------
    // EJE NO ESCRITO
    // ------------------------------------------------------------------

    /**
     * Toma como eje el unico eje coordenado que el enunciado nombra, cuando no dice
     * alrededor de que gira.
     *
     * <p>"Calcular el volumen ... engendrado por la region limitada por y = x - x^3 y el eje
     * x (0 &lt;= x &lt;= 1)" no dice el eje de giro, pero el unico eje que aparece es el X, que
     * ademas cierra la region: es el giro habitual de este tipo de ejercicio. Se toma, se
     * marca como interpretado y se dice. Si el enunciado no nombra ningun eje, o nombra los
     * dos, no hay de donde sacarlo y se pide.</p>
     */
    private EjeRotacion deducirEjeNoEscrito(List<ExtractorEje.RectaNombrada> rectas,
                                            ProblemaEstructurado problema) {
        boolean nombraEjeX = false;
        boolean nombraEjeY = false;
        for (ExtractorEje.RectaNombrada recta : rectas) {
            String lectura = recta.frontera().lectura();
            if (!lectura.startsWith("El enunciado nombra el eje")
                    && !lectura.startsWith("El enunciado nombra los ejes")) {
                continue;
            }
            if (recta.frontera().esVertical()) {
                nombraEjeY = true;
            } else {
                nombraEjeX = true;
            }
        }
        if (nombraEjeX == nombraEjeY) {
            return null;
        }
        EjeRotacion eje = nombraEjeX ? EjeRotacion.ejeX() : EjeRotacion.ejeY();
        problema.fijarEje(eje);
        problema.fijarOrigen("eje", OrigenDato.INTERPRETADO);
        String nombre = nombraEjeX ? "eje X" : "eje Y";
        problema.anotarEvidencia("El enunciado no dice alrededor de que eje gira la region. El "
                + "unico eje que nombra es el " + nombre + ", que ademas cierra la region, asi que "
                + "se tomo como eje de giro.");
        problema.anotarAmbiguedad("El eje de giro no esta escrito: se tomo el " + nombre
                + " porque es el unico eje que nombra el enunciado. Si el ejercicio gira la region "
                + "alrededor de otra recta, cambie el eje antes de resolver.");
        return eje;
    }

    // ------------------------------------------------------------------
    // LA REGION
    // ------------------------------------------------------------------

    /** Reune todo lo que necesita el planificador. */
    private PlanificadorRegion.Entrada armarEntrada(ProblemaEstructurado problema, List<Curva> curvas,
                                                    List<Frontera> fronteras,
                                                    Optional<ExtractorIntervalo.Intervalo> intervalo,
                                                    String normalizado, EjeRotacion eje) {
        PlanificadorRegion.Cuadrante cuadrante = leerCuadrante(normalizado);

        // El eje X como suelo: lo dice "bajo la curva", y lo implica una sola curva sin
        // ninguna otra frontera por arriba o por abajo.
        boolean diceBajo = ExtractorDatos.contieneAlguna(normalizado,
                "bajo la curva", "bajo la grafica", "debajo de la curva", "bajo la funcion",
                "area bajo", "bajo f", "bajo y");
        boolean unaSolaCurva = curvas.size() == 1 && contarRectasHorizontales(fronteras) == 0;
        boolean sueloImplicito = diceBajo || unaSolaCurva;

        String razonSuelo = diceBajo
                ? "El area bajo una curva es la que queda entre la curva y el eje X, asi que el eje "
                  + "X (y = 0) cierra la region aunque el enunciado no lo escriba."
                : "El enunciado nombra una sola curva y ninguna otra frontera por arriba o por "
                  + "abajo; como en el area bajo una curva, la region se cierra con el eje X "
                  + "(y = 0). Si el ejercicio dice otra cosa, corrija los datos.";
        OrigenDato origenSuelo = diceBajo ? OrigenDato.DERIVADO : OrigenDato.INTERPRETADO;

        String forzada = null;
        String motivo = null;
        TipoSolido pedido = problema.metodoSolicitado();
        if (problema.tema() == TemaProblema.VOLUMEN_SOLIDO && pedido != null && eje != null) {
            forzada = pedido.variableDeIntegracion(eje);
            motivo = "El enunciado pide " + pedido.etiqueta().toLowerCase() + " con el eje "
                    + eje.ecuacion() + ". " + (pedido == TipoSolido.CAPAS
                        ? "Las capas cilindricas se apilan en direccion perpendicular al eje"
                        : "Ese metodo rebana perpendicularmente al eje")
                    + ", asi que se integra respecto de " + forzada + ".";
        }

        return new PlanificadorRegion.Entrada(curvas, fronteras, intervalo, cuadrante,
                sueloImplicito, razonSuelo, origenSuelo, eje, forzada, motivo);
    }

    private int contarRectasHorizontales(List<Frontera> fronteras) {
        int cuenta = 0;
        for (Frontera frontera : fronteras) {
            if (!frontera.esVertical()) {
                cuenta++;
            }
        }
        return cuenta;
    }

    private PlanificadorRegion.Cuadrante leerCuadrante(String normalizado) {
        if (normalizado.contains("primer cuadrante") || normalizado.contains("primero cuadrante")
                || normalizado.contains("1er cuadrante") || normalizado.contains("i cuadrante")) {
            return PlanificadorRegion.Cuadrante.PRIMERO;
        }
        if (normalizado.contains("segundo cuadrante")) {
            return PlanificadorRegion.Cuadrante.SEGUNDO;
        }
        if (normalizado.contains("tercer cuadrante")) {
            return PlanificadorRegion.Cuadrante.TERCERO;
        }
        if (normalizado.contains("cuarto cuadrante")) {
            return PlanificadorRegion.Cuadrante.CUARTO;
        }
        return PlanificadorRegion.Cuadrante.NINGUNO;
    }

    /**
     * Copia la region decidida en el problema: funciones, variable, limites y
     * explicaciones.
     */
    private void aplicarPlan(ProblemaEstructurado problema, PlanificadorRegion.Plan plan) {
        problema.fijarVariable(plan.variable());
        problema.fijarLimites(plan.a(), plan.b());
        problema.fijarOrigen("limites", plan.origen());
        problema.fijarExplicacionLimites(plan.explicacionLimites());
        problema.fijarExplicacionRegion(plan.explicacionRegion());
        if (!plan.explicacionRegion().isBlank()) {
            problema.anotarEvidencia(plan.explicacionRegion());
        }
        problema.anotarEvidencia(plan.explicacionLimites());
        plan.ambiguedades().forEach(problema::anotarAmbiguedad);
        for (PlanificadorRegion.Punto punto : plan.puntos()) {
            problema.agregarPunto(punto.x(), punto.y(), punto.texto());
        }

        if (!plan.completo()) {
            problema.anotarAviso("La region que se pudo cerrar no usa todas las curvas y rectas "
                    + "que nombra el enunciado. Revise que sea la que pide el ejercicio.");
        }

        PlanificadorRegion.Borde primero = plan.primero();
        PlanificadorRegion.Borde segundo = plan.segundo();
        problema.fijarOrigen("funcionF", OrigenDato.EXPLICITO);

        // ---------- Tema de area: bajo la curva o entre curvas ----------
        if (problema.tema() == TemaProblema.AREA_BAJO_CURVA
                || problema.tema() == TemaProblema.AREA_ENTRE_CURVAS) {
            Optional<PlanificadorRegion.Borde> suelo = esElSuelo(primero, plan.variable())
                    ? Optional.of(primero)
                    : (esElSuelo(segundo, plan.variable()) ? Optional.of(segundo) : Optional.empty());

            if (suelo.isPresent() && plan.variable().equals("x")) {
                PlanificadorRegion.Borde curva = suelo.get() == primero ? segundo : primero;
                problema.fijarTema(TemaProblema.AREA_BAJO_CURVA);
                problema.fijarFuncionF(curva.expresion());
                problema.fijarFuncionG("");
            } else {
                problema.fijarTema(TemaProblema.AREA_ENTRE_CURVAS);
                problema.fijarFuncionF(primero.expresion());
                problema.fijarFuncionG(segundo.expresion());
                problema.fijarOrigen("funcionG", OrigenDato.EXPLICITO);
            }
            return;
        }

        // ---------- Volumen: los dos bordes; el metodo decide despues si sobra uno ----------
        problema.fijarFuncionF(primero.expresion());
        problema.fijarFuncionG(segundo.expresion());
        problema.fijarOrigen("funcionG", OrigenDato.EXPLICITO);
    }

    /**
     * Pone en los campos las curvas que se leyeron aunque la region no haya cerrado.
     *
     * <p>Que falte el intervalo no es motivo para perder lo que si se entendio: la funcion
     * va a su campo, tal como se escribio, y el estudiante solo tiene que completar lo que
     * falta.</p>
     */
    private void llenarSinRegion(ProblemaEstructurado problema, List<Curva> curvas) {
        String variable = curvas.get(0).forma().equals("X_DE_Y") ? "y" : "x";
        problema.fijarVariable(variable);
        List<String> funciones = new ArrayList<>();
        for (Curva curva : curvas) {
            Despejador.Despeje despeje = curva.despejeEn(variable);
            if (despeje != null && !despeje.ramas().isEmpty()) {
                funciones.add(despeje.ramas().get(0).expresion());
            }
        }
        if (!funciones.isEmpty()) {
            problema.fijarFuncionF(funciones.get(0));
            problema.fijarOrigen("funcionF", OrigenDato.EXPLICITO);
        }
        if (funciones.size() > 1 && problema.tema() != TemaProblema.AREA_BAJO_CURVA) {
            problema.fijarFuncionG(funciones.get(1));
            problema.fijarOrigen("funcionG", OrigenDato.EXPLICITO);
        }
        if (problema.tema() == TemaProblema.VOLUMEN_SOLIDO && problema.metodoSolicitado() != null) {
            problema.fijarTipoSolido(problema.metodoSolicitado());
            problema.fijarOrigen("metodo", OrigenDato.EXPLICITO);
        } else if (problema.tema() == TemaProblema.VOLUMEN_SOLIDO && problema.eje() != null) {
            // Sin region no se puede medir si el solido sale macizo o hueco, pero la
            // primera pregunta si tiene respuesta: si el eje es perpendicular a la variable
            // de la curva, el metodo que no exige despejar es el de capas.
            boolean ejePerpendicular = problema.eje().esHorizontal() == variable.equals("y");
            if (ejePerpendicular) {
                problema.fijarTipoSolido(TipoSolido.CAPAS);
                problema.fijarMetodoSugerido(TipoSolido.CAPAS);
                problema.fijarOrigen("metodo", OrigenDato.DERIVADO);
                problema.anotarEvidencia("El eje " + problema.eje().ecuacion() + " es perpendicular "
                        + "a la variable de la curva, " + variable + ": con capas cilindricas no hace "
                        + "falta despejarla.");
            }
        }
    }

    /** El eje X (y = 0) visto desde x: el suelo de un area bajo la curva. */
    private boolean esElSuelo(PlanificadorRegion.Borde borde, String variable) {
        return variable.equals("x") && borde.esLaConstante(0.0);
    }

    // ------------------------------------------------------------------
    // METODO
    // ------------------------------------------------------------------

    /**
     * Determina el metodo del solido y ajusta los bordes que le llegan al solucionador.
     *
     * <p>Se calculan dos metodos por separado: el que pide el enunciado y el que corresponde
     * por la forma de la region. Se aplica el pedido, porque el ejercicio esta practicando
     * ese; pero si la geometria sugeria otro se dice.</p>
     */
    private void elegirMetodo(ProblemaEstructurado problema, PlanificadorRegion.Plan plan) {
        if (problema.tema() != TemaProblema.VOLUMEN_SOLIDO) {
            return;
        }
        EjeRotacion eje = problema.eje();
        TipoSolido pedido = problema.metodoSolicitado();

        if (eje == null) {
            if (pedido != null) {
                problema.fijarTipoSolido(pedido);
                problema.fijarOrigen("metodo", OrigenDato.EXPLICITO);
            }
            return;
        }

        String v = plan.variable();
        FuncionMatematica curvaF = plan.primero().funcion().comoFuncion();
        FuncionMatematica curvaG = plan.segundo().funcion().comoFuncion();

        SelectorMetodoSolido.Seleccion seleccion = SelectorMetodoSolido.elegir(
                curvaF, curvaG, eje, plan.a(), plan.b(), v);
        problema.fijarMetodoSugerido(seleccion.tipo());
        seleccion.evidencias().forEach(problema::anotarEvidencia);
        seleccion.avisos().forEach(problema::anotarAviso);

        TipoSolido tipo;
        if (pedido == null) {
            tipo = seleccion.tipo();
            problema.fijarOrigen("metodo", OrigenDato.DERIVADO);
        } else {
            tipo = pedido;
            problema.fijarOrigen("metodo", OrigenDato.EXPLICITO);
            if (pedido != seleccion.tipo()) {
                problema.anotarAviso("El enunciado pide " + pedido.etiqueta().toLowerCase()
                        + " y se aplicara ese, porque el ejercicio esta pidiendo practicar "
                        + "precisamente ese metodo. Por la geometria de la region habria "
                        + "correspondido " + seleccion.tipo().etiqueta().toLowerCase()
                        + ": los dos dan el mismo volumen si el planteamiento es correcto, "
                        + "asi que si no coinciden hay un error en alguna parte.");
            }
            if (!plan.respetaVariableForzada()) {
                avisarQueElMetodoNoSeAplicaDirecto(problema, pedido, seleccion.tipo());
            }
        }
        problema.fijarTipoSolido(tipo);

        // Un borde y = 0 que coincide con el eje X de giro no aporta radio: en discos se
        // quita, porque el solucionador ya toma el eje como el otro borde. Si el eje esta
        // corrido (x = 1) se deja, para que la region siga cerrandose en el eje y no en
        // x = 0; el disco sale igual, con radio interior cero.
        PlanificadorRegion.Borde segundo = plan.segundo();
        PlanificadorRegion.Borde primero = plan.primero();
        if (tipo == TipoSolido.DISCOS && eje.pasaPorElOrigen()) {
            if (segundo.esLaConstante(0.0)) {
                problema.fijarFuncionG("");
            } else if (primero.esLaConstante(0.0)) {
                problema.fijarFuncionF(segundo.expresion());
                problema.fijarFuncionG("");
            }
        } else if (tipo == TipoSolido.CAPAS) {
            // En capas la altura es la diferencia de los bordes; un borde y = 0 no hace
            // falta escribirlo.
            if (segundo.esLaConstante(0.0)) {
                problema.fijarFuncionG("");
            } else if (primero.esLaConstante(0.0)) {
                problema.fijarFuncionF(segundo.expresion());
                problema.fijarFuncionG("");
            }
        }
    }

    /**
     * Avisa cuando el metodo pedido no se puede aplicar directamente con esta region.
     *
     * <p>Cada metodo rebana en una direccion. Si en la direccion del metodo pedido la region
     * queda partida en tramos, o la curva no se puede despejar, el metodo no se puede aplicar
     * tal cual. Se avisa al interpretar, que es cuando el estudiante todavia puede cambiar el
     * metodo en el formulario.</p>
     */
    private void avisarQueElMetodoNoSeAplicaDirecto(ProblemaEstructurado problema,
                                                    TipoSolido pedido, TipoSolido sugerido) {
        String necesaria = pedido.variableDeIntegracion(problema.eje());
        problema.anotarAviso("Ojo: " + pedido.etiqueta().toLowerCase() + " con el eje "
                + problema.eje().ecuacion() + " obliga a integrar respecto de " + necesaria
                + ", y descrita asi la region no queda entre los mismos dos bordes en todo su "
                + "recorrido (habria que despejar " + (necesaria.equals("y") ? "x" : "y")
                + " y partir la integral en tramos). Con la region tal como esta, el metodo que "
                + "se puede aplicar directamente es " + sugerido.etiqueta().toLowerCase() + ".");
    }

    // ------------------------------------------------------------------
    // DATOS FALTANTES
    // ------------------------------------------------------------------

    /**
     * Anota que informacion hace falta y no se pudo obtener de ninguna manera.
     *
     * <p>El mensaje nombra el dato concreto que falta, en vez de decir "falta
     * informacion": el estudiante tiene que saber que escribir.</p>
     */
    private void anotarLoQueFalta(ProblemaEstructurado problema,
                                  ClasificadorTema.Clasificacion clasificacion,
                                  List<Curva> curvas, boolean hayRegion) {

        if (!clasificacion.tema().estaDeterminado()) {
            problema.anotarFaltante("No se pudo identificar el tema del problema. "
                    + "Seleccionelo manualmente o escriba el enunciado con mas detalle.");
            return;
        }

        if (!clasificacion.seguro()) {
            problema.anotarAviso("El tema se dedujo de la estructura del enunciado, pero "
                    + "conviene confirmarlo antes de resolver.");
        }

        if (problema.funcionF().isBlank() && curvas.isEmpty()) {
            problema.anotarFaltante("La funcion a integrar. No se encontro en el enunciado "
                    + "ninguna curva escrita como ecuacion, por ejemplo y = x^2.");
        }

        if (problema.tema().necesitaDosFunciones() && !problema.tieneSegundaFuncion()
                && hayRegion) {
            problema.anotarFaltante("La segunda funcion que limita la region.");
        }

        if (problema.tema().necesitaEje() && problema.eje() == null) {
            problema.anotarFaltante("El eje de revolucion. El enunciado nombra la region y sus "
                    + "fronteras, pero no dice alrededor de que recta gira, y eso no se puede "
                    + "deducir: la misma region girada sobre el eje X o sobre el eje Y produce "
                    + "solidos distintos. Indique el eje para continuar.");
        }

        if (!problema.tieneLimites() && !curvas.isEmpty()) {
            problema.anotarFaltante(explicarIntervaloFaltante(curvas, problema));
        }
    }

    /** Explica por que no se pudo cerrar la region, con las curvas que si se leyeron. */
    private String explicarIntervaloFaltante(List<Curva> curvas, ProblemaEstructurado problema) {
        List<String> nombres = new ArrayList<>();
        for (Curva curva : curvas) {
            nombres.add(curva.ecuacion());
        }
        String eje = problema.eje() == null ? "" : " (la recta " + problema.eje().ecuacion()
                + " es el eje de giro, no una frontera)";
        return "El intervalo de integracion. El enunciado nombra " + String.join(" y ", nombres)
                + eje + ", pero con eso la region no queda cerrada: no hay dos cortes entre "
                + "curvas, ni rectas verticales, ni un intervalo escrito que la limiten. "
                + "Escriba los limites, o la recta o curva que falta para cerrar la region.";
    }

    // ------------------------------------------------------------------
    // CONSTRUCCION DESDE EL FORMULARIO
    // ------------------------------------------------------------------

    /**
     * Arma el mismo objeto estructurado a partir de los campos del formulario.
     *
     * <p>Existe para que los dos caminos de la aplicacion terminen en la misma
     * estructura. El estudiante que escribe el enunciado y el que llena los campos a mano
     * producen un {@link ProblemaEstructurado} indistinguible, y a partir de ahi el
     * solucionador y la grafica hacen exactamente lo mismo.</p>
     *
     * @param tema          tema elegido
     * @param funcionF      funcion principal
     * @param funcionG      segunda funcion, puede ir vacia
     * @param limiteInferior limite inferior, o null
     * @param limiteSuperior limite superior, o null
     * @param eje           eje de revolucion, o null
     * @param buscarLimites si los limites deben deducirse de las intersecciones
     * @return el ejercicio estructurado
     */
    public ProblemaEstructurado desdeFormulario(TemaProblema tema, String funcionF, String funcionG,
                                                Double limiteInferior, Double limiteSuperior,
                                                EjeRotacion eje, boolean buscarLimites) {
        ProblemaEstructurado problema = new ProblemaEstructurado();
        problema.fijarTema(tema);
        problema.fijarFuncionF(funcionF);
        problema.fijarFuncionG(funcionG);
        problema.fijarLimites(limiteInferior, limiteSuperior);
        problema.fijarEje(eje);
        problema.fijarBuscarLimites(buscarLimites);

        problema.fijarOrigen("funcionF", OrigenDato.EXPLICITO);
        if (limiteInferior != null && limiteSuperior != null) {
            problema.fijarOrigen("limites", OrigenDato.EXPLICITO);
        }
        if (eje != null) {
            problema.fijarOrigen("eje", OrigenDato.EXPLICITO);
        }

        problema.anotarEvidencia("Los datos los introdujo el estudiante en el formulario.");

        if (tema == TemaProblema.VOLUMEN_SOLIDO && eje != null && problema.tieneLimites()
                && !problema.funcionF().isBlank()) {
            try {
                FuncionMatematica curvaF = EvaluadorExpresion
                        .compilar(problema.funcionF(), problema.variable()).comoFuncion();
                FuncionMatematica curvaG = problema.tieneSegundaFuncion()
                        ? EvaluadorExpresion.compilar(problema.funcionG(), problema.variable()).comoFuncion()
                        : null;
                SelectorMetodoSolido.Seleccion seleccion = SelectorMetodoSolido.elegir(
                        curvaF, curvaG, eje, problema.limiteInferior(), problema.limiteSuperior(),
                        problema.variable());
                problema.fijarMetodoSugerido(seleccion.tipo());
                problema.fijarTipoSolido(seleccion.tipo());
                problema.fijarOrigen("metodo", OrigenDato.DERIVADO);
                seleccion.evidencias().forEach(problema::anotarEvidencia);
            } catch (RuntimeException e) {
                problema.anotarAviso("No se pudo analizar la geometria de la region para elegir "
                        + "el metodo; seleccionelo manualmente.");
            }
        }

        // Con la casilla marcada, los limites dejan de ser un dato faltante: se van a
        // calcular, y el procedimiento explicara de donde salieron.
        if (buscarLimites && !problema.tieneLimites()) {
            problema.anotarEvidencia("Los limites se buscaran a partir de los puntos donde "
                    + "se cortan las curvas.");
            return problema;
        }

        if (problema.funcionF().isBlank()) {
            problema.anotarFaltante("La funcion a integrar.");
        }
        if (tema.necesitaEje() && eje == null) {
            problema.anotarFaltante("El eje de revolucion.");
        }
        if (!problema.tieneLimites()) {
            problema.anotarFaltante("El intervalo de integracion.");
        }
        return problema;
    }

    // ------------------------------------------------------------------
    // APOYO
    // ------------------------------------------------------------------

    /** Cambia por espacios un tramo del texto, conservando las posiciones del resto. */
    private static String blanquear(String texto, int inicio, int fin) {
        StringBuilder resultado = new StringBuilder(texto);
        for (int i = Math.max(0, inicio); i < Math.min(texto.length(), fin); i++) {
            resultado.setCharAt(i, ' ');
        }
        return resultado.toString();
    }
}
