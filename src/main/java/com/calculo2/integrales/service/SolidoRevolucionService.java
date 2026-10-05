package com.calculo2.integrales.service;

import com.calculo2.integrales.exception.LimitesInvalidosException;
import com.calculo2.integrales.interprete.SelectorMetodoSolido;
import com.calculo2.integrales.math.parser.EvaluadorExpresion;
import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.math.parser.NodoExpresion;
import com.calculo2.integrales.math.algebra.Despejador;
import com.calculo2.integrales.math.simbolico.ConversorPolinomio;
import com.calculo2.integrales.math.simbolico.FormaRadical;
import com.calculo2.integrales.math.simbolico.FormateadorMatematico;
import com.calculo2.integrales.math.simbolico.Polinomio;
import com.calculo2.integrales.math.solidos.GeneradorMallaRevolucion;
import com.calculo2.integrales.math.solidos.MetodoArandelas;
import com.calculo2.integrales.math.solidos.MetodoCapas;
import com.calculo2.integrales.math.util.PosicionRelativa;
import com.calculo2.integrales.math.util.Redondeo;
import com.calculo2.integrales.math.util.ValidadorFuncion;
import com.calculo2.integrales.model.DatosGrafica3D;
import com.calculo2.integrales.model.EjeRotacion;
import com.calculo2.integrales.model.ResultadoSolido;
import com.calculo2.integrales.model.SolicitudSolido;
import com.calculo2.integrales.model.TipoSolido;

import java.util.List;
import java.util.Optional;

/**
 * Resuelve el tema de solidos de revolucion por los tres metodos del curso.
 *
 * <pre>
 *   Discos:     V = pi * integral de [R(v)]^2 dv
 *   Arandelas:  V = pi * integral de ( [R(v)]^2 - [r(v)]^2 ) dv
 *   Capas:      V = 2*pi * integral de (radio) * (altura) dv
 * </pre>
 *
 * <p>El integrando se construye como expresion simbolica y no como funcion evaluable. Eso
 * permite desarrollarlo, integrarlo por la regla de la potencia y evaluar los limites, que
 * es el procedimiento que se sigue en clase; una funcion que solo se sabe evaluar daria el
 * mismo numero sin nada que explicar.</p>
 *
 * <p>Si no se indica el metodo, se deduce midiendo la region con el mismo analisis que usa
 * el interprete de enunciados.</p>
 */
public final class SolidoRevolucionService {

    private final PasosService pasosService = new PasosService();
    private final InterseccionService interseccionService = new InterseccionService();
    private final ResolutorIntegral resolutor = new ResolutorIntegral();
    private final GraficaService graficaService = new GraficaService();

    /**
     * Ejecuta el calculo completo.
     *
     * @param solicitud datos que llegaron del formulario
     * @return el resultado con el volumen, los pasos y la malla
     */
    public ResultadoSolido calcular(SolicitudSolido solicitud) {
        long comienzo = System.currentTimeMillis();

        EjeRotacion eje = solicitud.eje();

        // Sin limites no hay nada que calcular, y es lo primero que hay que decir: si
        // el metodo tampoco encaja, ese aviso puede esperar a que el intervalo exista.
        if (!solicitud.buscarLimites() && !solicitud.tieneLimites()) {
            throw new LimitesInvalidosException(
                    "Faltan los limites de integracion. Escribalos, o marque \"Buscar los "
                            + "limites\" si la region queda encerrada entre las dos curvas.");
        }

        // El metodo y la variable de integracion se condicionan mutuamente, asi que
        // primero se elige un metodo provisional para saber en que variable compilar.
        TipoSolido tipoProvisional = solicitud.tipoSolido() != null
                ? solicitud.tipoSolido()
                : (solicitud.tieneCurvaInterior() ? TipoSolido.ARANDELAS : TipoSolido.DISCOS);

        String variable = solicitud.variableDeIntegracion(tipoProvisional);

        comprobarQueElMetodoSePuedeAplicar(solicitud, tipoProvisional, variable);

        EvaluadorExpresion expresionExterior =
                EvaluadorExpresion.compilar(solicitud.funcionExterior(), variable);
        FuncionMatematica curvaExterior = expresionExterior.comoFuncion();

        EvaluadorExpresion expresionInterior = null;
        FuncionMatematica curvaInterior = null;
        if (solicitud.tieneCurvaInterior()) {
            expresionInterior = EvaluadorExpresion.compilar(solicitud.funcionInterior(), variable);
            curvaInterior = expresionInterior.comoFuncion();
        }

        // ---------- Limites ----------
        double[] limites = determinarLimites(solicitud, curvaExterior, curvaInterior);
        double a = limites[0];
        double b = limites[1];

        ValidadorFuncion.validarLimites(a, b);
        ValidadorFuncion.validarIntegrable(curvaExterior, a, b, "la funcion");
        if (curvaInterior != null) {
            ValidadorFuncion.validarIntegrable(curvaInterior, a, b, "la segunda funcion");
        }

        // ---------- Metodo ----------
        SelectorMetodoSolido.Seleccion seleccion = SelectorMetodoSolido.elegir(
                curvaExterior, curvaInterior, eje, a, b, variable);

        TipoSolido tipo = solicitud.tipoSolido() != null ? solicitud.tipoSolido() : seleccion.tipo();

        if (tipo.necesitaSegundaCurva() && !solicitud.tieneCurvaInterior()) {
            throw new LimitesInvalidosException(
                    "El metodo de arandelas necesita las dos curvas que limitan la region. "
                            + "Si la region llega hasta el eje, use el metodo de discos.");
        }

        List<String> razones = razonesDelMetodo(solicitud, seleccion, tipo);

        // Si la region queda a los dos lados del eje, al girar la mitad de un lado cae
        // dentro del solido que genera la otra: no hay agujero, y cada rebanada es un disco
        // completo con el radio de la curva mas lejana.
        if (tipo != TipoSolido.CAPAS && expresionInterior != null
                && regionAtraviesaElEje(curvaExterior, curvaInterior, eje, a, b)) {
            PosicionRelativa.Comparacion distancia = PosicionRelativa.compararDistanciaAlEje(
                    curvaExterior, curvaInterior, eje, a, b);
            if (!distancia.seCruzan()) {
                EvaluadorExpresion lejana = distancia.primeraVaArriba() ? expresionExterior : expresionInterior;
                razones.add("La region queda a los dos lados del eje " + eje.ecuacion() + ". Al "
                        + "girar, la parte de un lado cae dentro del solido que genera la otra, asi "
                        + "que no queda hueco: cada rebanada es un disco completo cuyo radio es la "
                        + "distancia de la curva mas lejana, " + lejana.expresionOriginal() + ".");
                tipo = TipoSolido.DISCOS;
                expresionExterior = lejana;
                expresionInterior = null;
                curvaExterior = lejana.comoFuncion();
                curvaInterior = null;
            }
        }

        // Cual de las dos curvas hace de radio exterior no lo decide el orden en que se
        // escribieron, sino cual queda mas lejos del eje. En capas la pregunta es otra: cual
        // va arriba, porque la altura del cascaron es la de arriba menos la de abajo. Se
        // comprueba antes de construir nada, porque de aqui salen a la vez la formula, el
        // dibujo y la integral.
        Radios radios = tipo == TipoSolido.CAPAS
                ? ordenarPorAltura(expresionExterior, expresionInterior, curvaExterior,
                        curvaInterior, a, b, variable)
                : ordenarRadios(expresionExterior, expresionInterior, curvaExterior,
                        curvaInterior, eje, a, b);

        expresionExterior = radios.exterior();
        expresionInterior = radios.interior();
        curvaExterior = expresionExterior.comoFuncion();
        curvaInterior = expresionInterior == null ? null : expresionInterior.comoFuncion();

        if (radios.seInvirtio()) {
            razones.add(radios.explicacion());
        }

        ResultadoSolido resultado = new ResultadoSolido();
        resultado.fijarFunciones(expresionExterior.expresionOriginal(),
                expresionInterior == null ? "" : expresionInterior.expresionOriginal());
        resultado.fijarLimites(a, b);
        resultado.fijarTipoSolido(tipo);
        resultado.fijarEje(eje);
        resultado.fijarMetodo("Integracion directa (regla de Barrow)", solicitud.particiones());
        resultado.fijarExplicacionLimites(explicacionDeLimites(solicitud, expresionExterior,
                expresionInterior, curvaExterior, curvaInterior, a, b));

        // ---------- Radios orientados ----------
        // Los radios se escriben como distancias positivas: si la curva queda por debajo
        // de un eje y = 6, el radio es 6 - f(x) y no f(x) - 6. Al elevar al cuadrado da lo
        // mismo, pero el planteamiento que se ensena tiene que ser el que se escribe en el
        // cuaderno.
        NodoExpresion radioExterior = radioHastaElEje(expresionExterior, eje, a, b);
        NodoExpresion radioInterior = expresionInterior == null
                ? null : radioHastaElEje(expresionInterior, eje, a, b);
        NodoExpresion radioDeLaCapa = radioDeLaCapa(variable, eje, a, b);
        NodoExpresion alturaDeLaCapa = expresionInterior == null
                ? expresionExterior.raiz()
                : ResolutorIntegral.restar(expresionExterior.raiz(), expresionInterior.raiz());

        // ---------- Integral ----------
        NodoExpresion integrando = switch (tipo) {
            case DISCOS -> ResolutorIntegral.alCuadrado(radioExterior);
            case ARANDELAS -> ResolutorIntegral.diferenciaDeCuadrados(radioExterior,
                    radioInterior == null ? new NodoExpresion.Numero(0) : radioInterior);
            case CAPAS -> ResolutorIntegral.multiplicar(radioDeLaCapa, alturaDeLaCapa);
        };

        ResolutorIntegral.Resolucion resolucion = resolutor.resolver(integrando, variable, a, b);

        double constante = (tipo == TipoSolido.CAPAS) ? 2.0 * Math.PI : Math.PI;
        double volumen = constante * Math.abs(resolucion.valor());

        double areaRegion = calcularAreaDeLaRegion(
                expresionExterior, expresionInterior, variable, a, b);

        double superficie = calcularSuperficieLateral(curvaExterior, eje, a, b);

        resultado.fijarValores(volumen, areaRegion, superficie);
        resultado.fijarAntiderivada(resolucion.antiderivada(), resolucion.fueAnalitica());

        avisarSiElEjeAtraviesaLaRegion(resultado, tipo, eje, a, b);
        agregarRebanadas(resultado, tipo, curvaExterior, curvaInterior, eje, a, b);

        resultado.fijarGrafica(construirMalla(
                tipo, curvaExterior, curvaInterior, eje, a, b));

        // Las dos vistas salen de las mismas funciones, los mismos limites y el mismo eje
        // que la integral. Es lo que impide que el dibujo describa un ejercicio y el
        // procedimiento otro.
        resultado.fijarRegion(graficaService.graficarRegionDelSolido(
                expresionExterior, expresionInterior, eje, a, b, variable,
                solicitud.curvasDelEnunciado()));

        // ---------- Pasos ----------
        String textoRadioExterior = FormateadorMatematico.escribir(radioExterior);
        String textoRadioInterior = radioInterior == null ? "0" : FormateadorMatematico.escribir(radioInterior);
        resultado.agregarPasos(pasosService.paraSolido(new PasosService.ContextoSolido(
                tipo,
                FormateadorMatematico.escribir(expresionExterior.raiz()),
                expresionInterior == null ? "" : FormateadorMatematico.escribir(expresionInterior.raiz()),
                eje, a, b, variable, resolucion, volumen, resultado.volumenEnPi(),
                razones, resultado.aMapa().get("explicacionLimites").toString(),
                tipo == TipoSolido.DISCOS ? "" : radios.comprobacion(),
                new PasosService.Radios(textoRadioExterior, textoRadioInterior,
                        FormateadorMatematico.escribir(radioDeLaCapa),
                        FormateadorMatematico.escribir(alturaDeLaCapa)),
                solicitud.explicacionRegion())));

        resultado.fijarDuracion(System.currentTimeMillis() - comienzo);
        return resultado;
    }

    // ------------------------------------------------------------------
    // VIABILIDAD DEL METODO
    // ------------------------------------------------------------------

    /**
     * Comprueba que el metodo elegido se pueda aplicar a la funcion tal como esta escrita.
     *
     * <p>Cada metodo rebana en una direccion, y eso obliga a describir la region respecto
     * de una variable concreta. Discos y arandelas cortan perpendicularmente al eje, asi
     * que con un eje horizontal integran respecto de {@code x}; las capas rebanan en la
     * direccion contraria, y con ese mismo eje horizontal necesitan la region descrita
     * como {@code x = g(y)}.</p>
     *
     * <p>Cuando el estudiante pide capas para una region escrita como {@code y = f(x)} y
     * el eje es horizontal, no es que el metodo este prohibido: es que antes habria que
     * despejar {@code x} en funcion de {@code y}, y eso rara vez se puede hacer. Sin ese
     * despeje la expresion no se puede ni leer, porque habla de una variable que no es la
     * de integracion.</p>
     *
     * <p>Antes esto terminaba en un error del parser diciendo que no reconocia la
     * {@code x}, que es verdad pero no explica nada. Lo que el estudiante necesita saber
     * es que el metodo que pidio exige un paso previo, y cual es el metodo que si se puede
     * aplicar tal como escribio el ejercicio.</p>
     */
    private void comprobarQueElMetodoSePuedeAplicar(SolicitudSolido solicitud, TipoSolido tipo,
                                                    String variableDeIntegracion) {
        String variableDeLaFuncion = variableQueUsa(solicitud.funcionExterior());

        if (variableDeLaFuncion == null || variableDeLaFuncion.equals(variableDeIntegracion)) {
            return;
        }

        String alternativa = (tipo == TipoSolido.CAPAS)
                ? (solicitud.tieneCurvaInterior() ? "arandelas" : "discos")
                : "capas cilindricas";

        throw new LimitesInvalidosException(
                "Para aplicar " + tipo.etiqueta().toLowerCase() + " con el eje "
                        + solicitud.eje().ecuacion() + " hay que integrar respecto de "
                        + variableDeIntegracion + ", pero la funcion esta escrita en terminos de "
                        + variableDeLaFuncion + ". Habria que despejar " + variableDeLaFuncion
                        + " y reescribirla como " + variableDeLaFuncion + " = g("
                        + variableDeIntegracion + "), cosa que no siempre se puede hacer.\n\n"
                        + "Dos salidas: escriba la funcion ya despejada, o use el metodo de "
                        + alternativa + ", que rebana en la otra direccion y funciona con la "
                        + "funcion tal como esta.",
                "Cambie el metodo a " + alternativa + ", o reescriba la funcion despejada "
                        + "respecto de " + variableDeIntegracion + ".");
    }

    /**
     * Averigua en que variable esta escrita una expresion.
     *
     * <p>Se prueba a compilarla con cada una de las dos y se devuelve la que funcione. Es
     * mas fiable que buscar letras en el texto, porque el parser ya sabe distinguir la
     * {@code x} de una variable de la {@code x} de {@code max} o {@code exp}.</p>
     *
     * @param expresion la funcion escrita por el estudiante
     * @return "x", "y", o null si es una constante o no se puede leer de ninguna forma
     */
    private String variableQueUsa(String expresion) {
        boolean enX = compila(expresion, "x");
        boolean enY = compila(expresion, "y");

        // Una constante como "4" compila con las dos, y entonces da igual: no hay ninguna
        // variable que despejar y el metodo se puede aplicar sin mas.
        if (enX && enY) {
            return null;
        }
        if (enX) {
            return "x";
        }
        return enY ? "y" : null;
    }

    /** Indica si la expresion se puede leer tomando esa variable como la independiente. */
    private boolean compila(String expresion, String variable) {
        try {
            EvaluadorExpresion.compilar(expresion, variable);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // ORDEN DE LOS RADIOS
    // ------------------------------------------------------------------

    /**
     * Las dos curvas ya puestas en su papel, con la comprobacion que lo justifica.
     *
     * @param exterior     la que queda mas lejos del eje
     * @param interior     la que queda mas cerca, o null si solo hay una curva
     * @param seInvirtio   true si hubo que intercambiarlas respecto de como se escribieron
     * @param explicacion  por que se intercambiaron
     * @param comprobacion la prueba con un punto del intervalo, para copiarla al cuaderno
     * @param seCruzan     true si las curvas intercambian posiciones dentro del intervalo
     */
    private record Radios(EvaluadorExpresion exterior, EvaluadorExpresion interior,
                          boolean seInvirtio, String explicacion, String comprobacion,
                          boolean seCruzan) {
    }

    /**
     * Decide cual de las dos curvas es el radio exterior y cual el interior.
     *
     * <p>El orden en que el estudiante las escribio no dice nada: en {@code y = x^3} y
     * {@code y = x^2} sobre {@code [0, 1]} la segunda queda por encima, y por tanto mas
     * lejos del eje X. Tomarlas como vinieron pondria {@code R = x^3} y {@code r = x^2},
     * es decir el radio menor donde va el mayor.</p>
     *
     * <p>Ese error se esconde con facilidad. La integral saldria negativa y el valor
     * absoluto del final devolveria el volumen correcto, asi que el numero no delataria
     * nada; lo que quedaria mal es todo lo que el estudiante viene a ver, que es la
     * formula escrita, el dibujo y el procedimiento.</p>
     *
     * <p>La comparacion se hace contra el eje y no en altura, porque no son la misma
     * pregunta: girando alrededor de {@code y = 5}, la curva mas alta es la que queda mas
     * cerca del eje y la que define el agujero.</p>
     */
    private Radios ordenarRadios(EvaluadorExpresion expresionExterior,
                                 EvaluadorExpresion expresionInterior,
                                 FuncionMatematica curvaExterior, FuncionMatematica curvaInterior,
                                 EjeRotacion eje, double a, double b) {

        if (expresionInterior == null) {
            return new Radios(expresionExterior, null, false, "", "", false);
        }

        PosicionRelativa.Comparacion comparacion = PosicionRelativa.compararDistanciaAlEje(
                curvaExterior, curvaInterior, eje, a, b);

        String comprobacion = comparacion.comprobacion(
                expresionExterior.expresionOriginal(),
                expresionInterior.expresionOriginal(),
                expresionExterior.variable());

        if (comparacion.primeraVaArriba()) {
            return new Radios(expresionExterior, expresionInterior, false, "", comprobacion,
                    comparacion.seCruzan());
        }

        return new Radios(expresionInterior, expresionExterior, true,
                "Se intercambiaron las dos curvas respecto del orden en que se escribieron: "
                        + "midiendo en el intervalo, la que queda mas lejos del eje es "
                        + expresionInterior.expresionOriginal() + ", asi que es esa la que hace "
                        + "de radio exterior. El orden de escritura no decide el papel de cada "
                        + "curva; lo decide la distancia al eje.",
                comprobacion, comparacion.seCruzan());
    }

    // ------------------------------------------------------------------
    // INTEGRANDO
    // ------------------------------------------------------------------

    /**
     * El radio de una curva: su distancia al eje, escrita como distancia positiva.
     *
     * <p>Con el eje en el origen el radio es la propia funcion, como se escribe en clase.
     * Con un eje corrido se resta el desplazamiento en el orden que deja el radio positivo:
     * si la curva queda por debajo de {@code y = 6}, el radio es {@code 6 - f(x)}. Si lo que
     * queda es una raiz sola, como {@code (1 + sqrt(20 - 4y)) - 1}, se simplifica a
     * {@code sqrt(20 - 4y)}.</p>
     */
    private NodoExpresion radioHastaElEje(EvaluadorExpresion curva, EjeRotacion eje,
                                          double a, double b) {
        double k = eje.desplazamiento();
        NodoExpresion raiz = curva.raiz();

        Optional<Polinomio> constante = ConversorPolinomio.convertir(raiz).filter(Polinomio::esConstante);
        if (constante.isPresent()) {
            return new NodoExpresion.Numero(Math.abs(constante.get().coeficiente(0) - k));
        }
        if (Math.abs(k) < 1e-12) {
            return raiz;
        }

        boolean quedaDebajo = true;
        double paso = (b - a) / 20.0;
        for (int i = 0; i <= 20; i++) {
            double valor = ValidadorFuncion.evaluarSeguro(curva.comoFuncion(), a + i * paso);
            if (ValidadorFuncion.esUtilizable(valor) && valor > k + 1e-9) {
                quedaDebajo = false;
                break;
            }
        }
        NodoExpresion distancia = quedaDebajo
                ? ResolutorIntegral.restar(new NodoExpresion.Numero(k), raiz)
                : ResolutorIntegral.restar(raiz, new NodoExpresion.Numero(k));
        return simplificarRaiz(distancia, curva.variable()).orElse(distancia);
    }

    /**
     * Reescribe {@code (1 + 2sqrt(5 - y)) - 1} como {@code 2sqrt(5 - y)}.
     *
     * @return la raiz sola, o vacio si la expresion no es una raiz por un numero
     */
    private Optional<NodoExpresion> simplificarRaiz(NodoExpresion expresion, String variable) {
        Optional<FormaRadical> forma = FormaRadical.de(expresion);
        if (forma.isEmpty() || !forma.get().esRaizPura()) {
            return Optional.empty();
        }
        try {
            String funcion = forma.get().indice() == 2 ? "sqrt" : "cbrt";
            NodoExpresion dentro = EvaluadorExpresion.compilar(
                    Despejador.textoPolinomio(forma.get().radicando(), variable), variable).raiz();
            NodoExpresion raiz = new NodoExpresion.LlamadaFuncion(funcion, dentro);
            double c = forma.get().coeficiente();
            if (Math.abs(c - 1.0) < 1e-12) {
                return Optional.of(raiz);
            }
            if (Math.abs(c + 1.0) < 1e-12) {
                return Optional.of(new NodoExpresion.Negacion(raiz));
            }
            return Optional.of(ResolutorIntegral.multiplicar(new NodoExpresion.Numero(c), raiz));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * El radio de un cascaron: la distancia de la rebanada al eje.
     *
     * <p>Si la region queda a la izquierda del eje {@code x = 2}, el radio es {@code 2 - x};
     * si queda a la derecha, {@code x - 2}. Con el eje en el origen es la propia variable.</p>
     */
    private NodoExpresion radioDeLaCapa(String variable, EjeRotacion eje, double a, double b) {
        double k = eje.desplazamiento();
        double medio = (a + b) / 2.0;
        NodoExpresion v = new NodoExpresion.Variable(variable);
        if (medio >= k) {
            return Math.abs(k) < 1e-12 ? v : ResolutorIntegral.restar(v, new NodoExpresion.Numero(k));
        }
        return Math.abs(k) < 1e-12
                ? new NodoExpresion.Negacion(v)
                : ResolutorIntegral.restar(new NodoExpresion.Numero(k), v);
    }

    /**
     * Indica si la region queda a los dos lados del eje.
     *
     * <p>Pasa con {@code y = sqrt(8x)} e {@code y = -sqrt(8x)} girando sobre el eje X: una
     * curva arriba y otra abajo. Se comprueba que en la mayor parte del intervalo las dos
     * curvas esten en lados opuestos.</p>
     */
    private boolean regionAtraviesaElEje(FuncionMatematica curvaUna, FuncionMatematica curvaOtra,
                                         EjeRotacion eje, double a, double b) {
        double k = eje.desplazamiento();
        int opuestas = 0;
        int validas = 0;
        for (int i = 1; i < 100; i++) {
            double punto = a + (b - a) * i / 100.0;
            double una = ValidadorFuncion.evaluarSeguro(curvaUna, punto);
            double otra = ValidadorFuncion.evaluarSeguro(curvaOtra, punto);
            if (!ValidadorFuncion.esUtilizable(una) || !ValidadorFuncion.esUtilizable(otra)) {
                continue;
            }
            validas++;
            if ((una - k) * (otra - k) < -1e-12) {
                opuestas++;
            }
        }
        return validas > 0 && opuestas * 2 > validas;
    }

    /**
     * En capas, ordena las curvas por altura: la de arriba primero.
     *
     * <p>La altura del cascaron es la curva de arriba menos la de abajo, y eso no tiene que
     * ver con cual queda mas lejos del eje. Con funciones de y, "arriba" es "a la
     * derecha".</p>
     */
    private Radios ordenarPorAltura(EvaluadorExpresion expresionUna, EvaluadorExpresion expresionOtra,
                                    FuncionMatematica curvaUna, FuncionMatematica curvaOtra,
                                    double a, double b, String variable) {
        if (expresionOtra == null) {
            return new Radios(expresionUna, null, false, "", "", false);
        }
        PosicionRelativa.Comparacion comparacion = PosicionRelativa.comparar(curvaUna, curvaOtra, a, b);
        String comprobacion = comparacion.comprobacion(expresionUna.expresionOriginal(),
                expresionOtra.expresionOriginal(), variable);
        if (variable.equals("y")) {
            comprobacion = comprobacion.replace("va por encima de", "queda a la derecha de");
        }
        if (comparacion.primeraVaArriba()) {
            return new Radios(expresionUna, expresionOtra, false, "", comprobacion,
                    comparacion.seCruzan());
        }
        String donde = variable.equals("y") ? "a la derecha" : "arriba";
        return new Radios(expresionOtra, expresionUna, true,
                "En capas la altura de cada cascaron es la curva de " + donde + " menos la otra. "
                        + "Midiendo en el intervalo, la que queda " + donde + " es "
                        + expresionOtra.expresionOriginal() + ".",
                comprobacion, comparacion.seCruzan());
    }

    // ------------------------------------------------------------------
    // LIMITES
    // ------------------------------------------------------------------

    /** Decide que limites se van a usar, buscandolos si el estudiante lo pidio. */
    private double[] determinarLimites(SolicitudSolido solicitud, FuncionMatematica curvaExterior,
                                       FuncionMatematica curvaInterior) {
        if (solicitud.buscarLimites()) {
            if (curvaInterior == null) {
                throw new LimitesInvalidosException(
                        "Para buscar los limites hacen falta dos curvas que se corten. Con una "
                                + "sola curva, escriba el intervalo a mano.");
            }
            double[] deducidos = interseccionService.deducirLimites(
                    curvaExterior, curvaInterior, 100.0);
            return new double[]{deducidos[0], deducidos[1]};
        }

        if (!solicitud.tieneLimites()) {
            throw new LimitesInvalidosException(
                    "Faltan los limites de integracion. Escribalos, o marque \"Buscar los "
                            + "limites\" si la region queda encerrada entre las dos curvas.");
        }
        return new double[]{solicitud.inicio(), solicitud.fin()};
    }

    /** Redacta como se dedujeron los limites, o cadena vacia si los dio el estudiante. */
    private String explicacionDeLimites(SolicitudSolido solicitud, EvaluadorExpresion exterior,
                                        EvaluadorExpresion interior, FuncionMatematica curvaExterior,
                                        FuncionMatematica curvaInterior, double a, double b) {
        // Cuando los limites los dedujo el interprete del enunciado, la cuenta ya esta
        // hecha y viaja con la peticion. Repetirla aqui daria el mismo numero por otro
        // camino, y el estudiante veria una explicacion que no es la que se le mostro al
        // interpretar el problema.
        if (!solicitud.explicacionLimites().isBlank()) {
            return solicitud.explicacionLimites();
        }

        if (!solicitud.buscarLimites() || interior == null) {
            return "";
        }

        var cortes = interseccionService.resolverCortes(exterior, interior, -100.0, 100.0);
        String v = exterior.variable();
        return interseccionService.explicarCortes(cortes, exterior.expresionOriginal(),
                interior.expresionOriginal(), v, a, b)
                + "\n\nLa region encerrada va del primer corte al ultimo, asi que el intervalo es ["
                + cortes.textoDe(a) + ", " + cortes.textoDe(b) + "].";
    }

    // ------------------------------------------------------------------
    // METODO
    // ------------------------------------------------------------------

    /**
     * Reune las razones por las que corresponde el metodo aplicado.
     *
     * <p>Cuando el estudiante elige un metodo distinto del que sugiere la geometria, se
     * dice: puede tener una razon para hacerlo, pero tambien puede ser el error que esta
     * buscando.</p>
     */
    private List<String> razonesDelMetodo(SolicitudSolido solicitud,
                                          SelectorMetodoSolido.Seleccion seleccion,
                                          TipoSolido tipoUsado) {
        List<String> razones = ResolutorIntegral.copiar(seleccion.evidencias());
        razones.addAll(seleccion.avisos());

        if (solicitud.tipoSolido() != null && solicitud.tipoSolido() != seleccion.tipo()) {
            razones.add("Nota: por la geometria de la region corresponderia "
                    + seleccion.tipo().etiqueta() + ", pero se aplico "
                    + tipoUsado.etiqueta() + " porque asi se indico en el formulario.");
        }
        return razones;
    }

    // ------------------------------------------------------------------
    // MAGNITUDES ADICIONALES
    // ------------------------------------------------------------------

    /**
     * Calcula el area de la region plana que se hace girar.
     *
     * <p>No entra en la formula del volumen, pero se muestra junto al resultado porque
     * conecta este tema con los dos anteriores: el solido nace de rotar esa area.</p>
     */
    private double calcularAreaDeLaRegion(EvaluadorExpresion exterior, EvaluadorExpresion interior,
                                          String variable, double a, double b) {
        NodoExpresion altura = (interior == null)
                ? exterior.raiz()
                : ResolutorIntegral.restar(exterior.raiz(), interior.raiz());

        return Math.abs(resolutor.resolver(altura, variable, a, b).valor());
    }

    /**
     * Calcula el area de la superficie exterior del solido.
     *
     * <pre>S = 2*pi * integral de R(x) * raiz(1 + [f'(x)]^2) dx</pre>
     *
     * <p>Este si se resuelve por aproximacion numerica, y no por descuido: la raiz que
     * aparece casi nunca tiene primitiva elemental, ni siquiera para funciones sencillas.
     * Es un dato complementario que no forma parte del procedimiento del ejercicio.</p>
     */
    private double calcularSuperficieLateral(FuncionMatematica curva, EjeRotacion eje,
                                             double a, double b) {
        return new com.calculo2.integrales.math.solidos.MetodoDiscos(
                new com.calculo2.integrales.math.integracion.ReglaSimpson())
                .calcularSuperficieLateral(curva, eje, a, b, 2000);
    }

    /**
     * Avisa cuando el eje de giro pasa por dentro de la region.
     *
     * <p>En ese caso las dos mitades de la region se superponen al girar y el volumen que
     * devuelve la formula no corresponde al solido que uno se imagina.</p>
     */
    private void avisarSiElEjeAtraviesaLaRegion(ResultadoSolido resultado, TipoSolido tipo,
                                                EjeRotacion eje, double a, double b) {
        if (tipo == TipoSolido.CAPAS && MetodoCapas.ejeAtraviesaLaRegion(eje, a, b)) {
            resultado.fijarAdvertencia(
                    "El eje de giro " + eje.ecuacion() + " queda dentro del intervalo ["
                            + Redondeo.texto(a) + ", " + Redondeo.texto(b) + "]. Al girar, la parte "
                            + "de un lado se monta sobre la del otro y el volumen calculado cuenta "
                            + "esa zona dos veces. Conviene partir el ejercicio en dos, uno a cada "
                            + "lado del eje.");
        }
    }

    /** Registra algunas rebanadas del solido para la tabla de detalle. */
    private void agregarRebanadas(ResultadoSolido resultado, TipoSolido tipo,
                                  FuncionMatematica curvaExterior, FuncionMatematica curvaInterior,
                                  EjeRotacion eje, double a, double b) {
        int cuantas = 10;
        double paso = (b - a) / cuantas;
        FuncionMatematica interiorOCero = curvaInterior == null
                ? FuncionMatematica.cero() : curvaInterior;

        for (int i = 0; i <= cuantas; i++) {
            double posicion = a + i * paso;

            double radioExterior;
            double radioInterior;
            double volumenParcial;

            switch (tipo) {
                case DISCOS -> {
                    radioExterior = eje.radioDesde(
                            ValidadorFuncion.evaluarSeguro(curvaExterior, posicion));
                    radioInterior = 0.0;
                    volumenParcial = Math.PI * radioExterior * radioExterior * paso;
                }
                case ARANDELAS -> {
                    radioExterior = MetodoArandelas.radioExteriorEn(
                            curvaExterior, interiorOCero, eje, posicion);
                    radioInterior = MetodoArandelas.radioInteriorEn(
                            curvaExterior, interiorOCero, eje, posicion);
                    volumenParcial = Math.PI * paso
                            * (radioExterior * radioExterior - radioInterior * radioInterior);
                }
                case CAPAS -> {
                    radioExterior = MetodoCapas.radioEn(eje, posicion);
                    radioInterior = 0.0;
                    double altura = MetodoCapas.alturaEn(curvaExterior, interiorOCero, posicion);
                    volumenParcial = 2.0 * Math.PI * radioExterior * altura * paso;
                }
                default -> {
                    continue;
                }
            }

            if (Double.isNaN(radioExterior) || Double.isNaN(volumenParcial)) {
                continue;
            }
            resultado.agregarRebanada(posicion, radioExterior, radioInterior, volumenParcial);
        }
    }

    /** Construye la malla del solido segun el metodo aplicado. */
    private DatosGrafica3D construirMalla(TipoSolido tipo, FuncionMatematica curvaExterior,
                                          FuncionMatematica curvaInterior, EjeRotacion eje,
                                          double a, double b) {
        FuncionMatematica interiorOCero = curvaInterior == null
                ? FuncionMatematica.cero() : curvaInterior;

        return switch (tipo) {
            case DISCOS -> GeneradorMallaRevolucion.paraDiscos(curvaExterior, eje, a, b);
            case ARANDELAS -> GeneradorMallaRevolucion.paraArandelas(
                    curvaExterior, interiorOCero, eje, a, b);
            case CAPAS -> GeneradorMallaRevolucion.paraCapas(
                    curvaExterior, interiorOCero, eje, a, b);
        };
    }
}
