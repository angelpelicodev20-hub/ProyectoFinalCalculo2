package com.calculo2.integrales.service;

import com.calculo2.integrales.math.simbolico.Fraccion;
import com.calculo2.integrales.math.util.Redondeo;
import com.calculo2.integrales.model.EjeRotacion;
import com.calculo2.integrales.model.PasoSolucion;
import com.calculo2.integrales.model.TipoSolido;

import java.util.ArrayList;
import java.util.List;

/**
 * Redacta el procedimiento completo de cada ejercicio.
 *
 * <p>Esta clase es la razon de ser del proyecto. Un numero solo se puede copiar; un
 * procedimiento se puede estudiar, y sobre todo se puede comparar con el que el estudiante
 * hizo a mano para encontrar donde se desvio.</p>
 *
 * <p>Por eso cada paso responde cuatro preguntas, y no solo la primera:</p>
 *
 * <ul>
 *   <li><b>Que</b> se esta haciendo.</li>
 *   <li><b>Por que</b> se hace en este momento.</li>
 *   <li><b>Que formula</b> lo respalda.</li>
 *   <li><b>De donde sale</b> cada valor que aparece.</li>
 * </ul>
 *
 * <p>Decir "el radio es f(x)" no ensena nada. Decir que, como el giro es alrededor del eje
 * X, la distancia vertical de la curva al eje es justamente el valor de la funcion, y que
 * por eso el radio vale {@code f(x)}, si.</p>
 */
public final class PasosService {

    /**
     * Todo lo que hace falta para redactar un procedimiento de area.
     *
     * @param funcionF            primera funcion, en limpio
     * @param funcionG            segunda funcion, o cadena vacia
     * @param a                   limite inferior
     * @param b                   limite superior
     * @param variable            variable de integracion
     * @param resolucion          la integral ya resuelta
     * @param integral            valor con signo
     * @param area                area geometrica
     * @param cruces              donde la curva cruza el eje o donde se cortan las curvas
     * @param explicacionLimites  como se obtuvieron los limites, o cadena vacia
     * @param tramos              tramos en que se partio la integral
     * @param aproximacionRiemann descripcion de la suma de Riemann, o cadena vacia
     * @param explicacionCortes   la ecuacion de los cortes resuelta paso a paso
     * @param detalle             cada tramo con su punto de prueba y su integral
     * @param explicacionRegion   como se armo la region desde el enunciado, o cadena vacia
     */
    public record ContextoArea(String funcionF, String funcionG, double a, double b,
                               String variable, ResolutorIntegral.Resolucion resolucion,
                               double integral, double area, List<Double> cruces,
                               String explicacionLimites,
                               List<InterseccionService.Tramo> tramos,
                               String aproximacionRiemann, String explicacionCortes,
                               List<DetalleTramo> detalle, String explicacionRegion) {
    }

    /**
     * Un tramo del intervalo, ya integrado.
     *
     * @param desde       inicio del tramo
     * @param hasta       fin del tramo
     * @param prueba      punto interior donde se comprobo cual va arriba
     * @param arriba      nombre de lo que va arriba (una funcion o el eje)
     * @param abajo       nombre de lo que va abajo
     * @param valorArriba lo que vale arriba en el punto de prueba
     * @param valorAbajo  lo que vale abajo
     * @param integrando  lo que se integra en el tramo
     * @param resolucion  la integral del tramo, con su primitiva y su sustitucion
     * @param area        el area del tramo, positiva
     */
    public record DetalleTramo(double desde, double hasta, double prueba, String arriba,
                               String abajo, double valorArriba, double valorAbajo,
                               String integrando, ResolutorIntegral.Resolucion resolucion,
                               double area) {
    }

    /**
     * Todo lo que hace falta para redactar un procedimiento de solido.
     *
     * @param tipo               metodo geometrico aplicado
     * @param funcionExterior    curva del radio exterior
     * @param funcionInterior    curva del radio interior, o cadena vacia
     * @param eje                recta de revolucion
     * @param a                  limite inferior
     * @param b                  limite superior
     * @param variable           variable de integracion
     * @param resolucion         la integral ya resuelta
     * @param volumen            volumen final
     * @param volumenEnPi        el volumen como multiplo de pi, o cadena vacia
     * @param razonesDelMetodo   por que corresponde ese metodo
     * @param explicacionLimites como se obtuvieron los limites, o cadena vacia
     * @param comprobacionPosicion prueba de cual curva queda mas lejos del eje, o cadena
     *                             vacia si el ejercicio solo tiene una curva
     * @param radios             los radios ya escritos como distancias al eje
     * @param explicacionRegion  como se armo la region desde el enunciado, o cadena vacia
     */
    public record ContextoSolido(TipoSolido tipo, String funcionExterior, String funcionInterior,
                                 EjeRotacion eje, double a, double b, String variable,
                                 ResolutorIntegral.Resolucion resolucion, double volumen,
                                 String volumenEnPi, List<String> razonesDelMetodo,
                                 String explicacionLimites, String comprobacionPosicion,
                                 Radios radios, String explicacionRegion) {

        /** Indica si hay dos curvas y por tanto hubo que decidir cual va mas lejos del eje. */
        public boolean hayQueCompararLasCurvas() {
            return !comprobacionPosicion.isBlank();
        }
    }

    /**
     * Los radios de un solido, ya escritos como distancias al eje.
     *
     * @param exterior   radio exterior, o el unico en discos
     * @param interior   radio interior en arandelas
     * @param radioCapa  distancia del cascaron al eje, en capas
     * @param alturaCapa altura del cascaron, en capas
     */
    public record Radios(String exterior, String interior, String radioCapa, String alturaCapa) {
    }

    /**
     * El paso que explica como se armo la region a partir del enunciado.
     *
     * <p>Aparece cuando hubo trabajo previo que mostrar: curvas que hubo que despejar,
     * una rama que elegir, una variable de integracion que decidir. Sin ese paso, el
     * estudiante veria aparecer {@code y = sqrt(8x)} sin saber de donde salio, si el
     * enunciado decia {@code y^2 = 8x}.</p>
     */
    private PasoSolucion pasoDeRegion(int numero, String explicacionRegion) {
        return new PasoSolucion(numero, "Plantear la region",
                "Antes de integrar hay que escribir la region con funciones de la variable de "
                        + "integracion: despejar las curvas que no lo esten, decidir que rama de "
                        + "cada una cierra la region y cual queda por encima.",
                explicacionRegion, "");
    }

    // ==================================================================
    // AREA BAJO LA CURVA
    // ==================================================================

    /**
     * Redacta el procedimiento del area bajo una curva.
     *
     * <p>El orden es el de clase: identificar, ver donde la curva corta el eje (porque el
     * area por debajo del eje cuenta en positivo y obliga a partir la integral),
     * determinar los limites, ver el signo de la funcion en cada tramo, plantear, integrar
     * y evaluar cada tramo, y sumar.</p>
     *
     * @param contexto datos del ejercicio ya resuelto
     * @return los pasos, en orden
     */
    public List<PasoSolucion> paraAreaBajoCurva(ContextoArea contexto) {
        List<PasoSolucion> pasos = new ArrayList<>();
        int n = 1;
        String v = contexto.variable();
        String eje = v.equals("x") ? "el eje X" : "el eje Y";

        // ---------- Identificar ----------
        pasos.add(new PasoSolucion(n++, "Identificar el problema",
                "Se busca el area encerrada entre la curva y " + eje + ", dentro del intervalo. "
                        + "La region tiene dos fronteras: la curva y el eje, que es la recta "
                        + (v.equals("x") ? "y = 0" : "x = 0") + ".",
                nombreFuncion(v) + " = " + contexto.funcionF()
                        + "\nRegion entre la curva y " + eje + ", desde " + v + " = "
                        + Redondeo.texto(contexto.a()) + " hasta " + v + " = "
                        + Redondeo.texto(contexto.b()),
                ""));

        if (!contexto.explicacionRegion().isBlank()) {
            pasos.add(pasoDeRegion(n++, contexto.explicacionRegion()));
        }

        // ---------- Cortes con el eje ----------
        if (!contexto.explicacionCortes().isBlank()) {
            pasos.add(new PasoSolucion(n++, "Hallar los cortes de la curva con el eje",
                    "Hay que saber donde la curva toca o cruza " + eje + ", igualandola a cero. "
                            + "Esos puntos pueden ser los limites de la region y, si caen dentro "
                            + "del intervalo, marcan donde la curva pasa al otro lado del eje.",
                    contexto.explicacionCortes(),
                    ""));
        }

        // ---------- Limites ----------
        pasos.add(pasoDeLimites(n++, sinCuentaRepetida(contexto), contexto.a(), contexto.b(), v));

        // ---------- Signo en cada tramo ----------
        pasos.add(pasoDeSigno(n++, contexto));

        // ---------- Plantear ----------
        pasos.add(new PasoSolucion(n++, "Plantear la integral",
                contexto.detalle().size() > 1
                        ? "Como la curva queda a los dos lados del eje, la integral se parte en "
                          + "los cortes. En los tramos donde la curva va por debajo del eje la "
                          + "integral sale negativa, y para sumar area hay que cambiarle el signo."
                        : "Cada rectangulo que aproxima la region tiene por altura el valor de la "
                          + "funcion y por base un trozo minusculo del eje, d" + v + ". Sumar "
                          + "todos esos rectangulos cuando su base tiende a cero es integrar.",
                "A = " + planteamientoBajoCurva(contexto),
                ""));

        // ---------- Integrar y evaluar ----------
        if (!contexto.resolucion().fueAnalitica()) {
            pasos.addAll(pasosDeIntegracion(n, contexto.resolucion(), v));
            n++;
        } else {
            pasos.add(pasoDePrimitiva(n++, contexto.resolucion(), v));
            pasos.add(pasoDeEvaluacionPorTramos(n++, contexto, true));
        }

        // ---------- Riemann ----------
        if (!contexto.aproximacionRiemann().isBlank()) {
            pasos.add(new PasoSolucion(n++, "Comprobar con rectangulos de Riemann",
                    "La integral nacio como el limite de una suma de rectangulos. Calcular esa "
                            + "suma con un numero concreto de rectangulos permite ver cuanto se "
                            + "acerca al valor exacto, y en la grafica se ve por que sobra o falta "
                            + "area en cada uno.",
                    contexto.aproximacionRiemann(),
                    ""));
        }

        // ---------- Resultado ----------
        pasos.add(pasoFinalDeArea(n, contexto));
        return pasos;
    }

    /** A = integral de a a b de f, o la suma de tramos con su signo. */
    private String planteamientoBajoCurva(ContextoArea contexto) {
        String v = contexto.variable();
        if (contexto.detalle().size() <= 1) {
            return integralDe(contexto.a(), contexto.b(), contexto.funcionF(), v);
        }
        StringBuilder texto = new StringBuilder();
        for (DetalleTramo tramo : contexto.detalle()) {
            boolean debajo = tramo.arriba().startsWith("el eje");
            if (texto.isEmpty()) {
                texto.append(debajo ? "-" : "");
            } else {
                texto.append(debajo ? " - " : " + ");
            }
            texto.append(integralDe(tramo.desde(), tramo.hasta(), contexto.funcionF(), v));
        }
        return texto.toString();
    }

    /** El ultimo paso del area, que distingue la integral del area geometrica. */
    private PasoSolucion pasoFinalDeArea(int numero, ContextoArea contexto) {
        boolean coinciden = Math.abs(contexto.integral() - contexto.area()) < 1e-9;
        String suma = sumaDeTramos(contexto);

        if (coinciden) {
            return new PasoSolucion(numero, "Resultado final",
                    contexto.detalle().size() > 1
                            ? "Sumando el area de cada tramo se obtiene el area total."
                            : "Como la region no cambia de lado, el area es el valor de la "
                              + "integral.",
                    (suma.isEmpty() ? "" : "A = " + suma + "\n")
                            + "A = " + Fraccion.texto(contexto.area()) + " unidades cuadradas",
                    Redondeo.texto(contexto.area()));
        }

        return new PasoSolucion(numero, "Resultado final",
                "Los dos numeros son distintos y cada uno responde a una pregunta distinta. "
                        + "La integral definida de corrido vale menos porque la parte que queda "
                        + "bajo el eje entra restando; el area geometrica suma esa parte en "
                        + "positivo, porque un area no puede ser negativa.",
                (suma.isEmpty() ? "" : "A = " + suma + "\n")
                        + "Area geometrica  = " + Fraccion.texto(contexto.area())
                        + " unidades cuadradas"
                        + "\nIntegral de corrido = " + Fraccion.texto(contexto.integral()),
                Redondeo.texto(contexto.area()));
    }

    // ==================================================================
    // AREA ENTRE DOS CURVAS
    // ==================================================================

    /**
     * Redacta el procedimiento del area entre dos curvas.
     *
     * <p>Sigue los pasos que se piden en el curso: hallar los puntos de interseccion
     * igualando y factorizando, identificar la funcion superior en cada tramo con un punto
     * de prueba, plantear la integral de la superior menos la inferior (partida en los
     * cortes si se cruzan), integrar, evaluar cada tramo y sumar.</p>
     *
     * @param contexto datos del ejercicio ya resuelto
     * @return los pasos, en orden
     */
    public List<PasoSolucion> paraAreaEntreCurvas(ContextoArea contexto) {
        List<PasoSolucion> pasos = new ArrayList<>();
        int n = 1;
        String v = contexto.variable();
        String w = v.equals("x") ? "y" : "x";

        // ---------- Identificar ----------
        pasos.add(new PasoSolucion(n++, "Identificar el problema",
                "Se busca el area de la region encerrada entre dos curvas. A diferencia del "
                        + "area bajo una curva, aqui ninguna de las dos fronteras es el eje: la "
                        + "altura de la region en cada punto es la diferencia entre las dos "
                        + (v.equals("x") ? "(la de arriba menos la de abajo)."
                                          : "(la de la derecha menos la de la izquierda, porque "
                                            + "las curvas estan escritas como x = g(y))."),
                w + " = f(" + v + ") = " + contexto.funcionF()
                        + "\n" + w + " = g(" + v + ") = " + contexto.funcionG(),
                ""));

        if (!contexto.explicacionRegion().isBlank()) {
            pasos.add(pasoDeRegion(n++, contexto.explicacionRegion()));
        }

        // ---------- Puntos de interseccion ----------
        if (!contexto.explicacionCortes().isBlank()) {
            pasos.add(new PasoSolucion(n++, "Hallar los puntos de interseccion",
                    "Las curvas se cortan donde valen lo mismo, asi que se igualan y se resuelve "
                            + "la ecuacion: se pasa todo a un lado, se simplifica y se factoriza. "
                            + "Los puntos de corte cierran la region y, si caen dentro del "
                            + "intervalo, marcan donde las curvas cambian de posicion.",
                    contexto.explicacionCortes(),
                    ""));
        }

        // ---------- Limites ----------
        pasos.add(pasoDeLimites(n++, sinCuentaRepetida(contexto), contexto.a(), contexto.b(), v));

        // ---------- Quien va arriba ----------
        pasos.add(pasoDeSigno(n++, contexto));

        // ---------- Plantear ----------
        pasos.add(new PasoSolucion(n++, "Plantear la integral",
                contexto.detalle().size() > 1
                        ? "Las curvas se cruzan dentro del intervalo, asi que no hay una sola que "
                          + "vaya siempre " + (v.equals("x") ? "arriba" : "a la derecha")
                          + ". Se integra tramo a tramo, restando en cada uno la de "
                          + (v.equals("x") ? "arriba menos la de abajo" : "la derecha menos la de la izquierda")
                          + "; integrar de corrido haria que las areas se cancelaran."
                        : "La altura de cada rectangulo es la distancia entre las curvas, la de "
                          + (v.equals("x") ? "arriba menos la de abajo" : "la derecha menos la de la izquierda")
                          + ". El orden de la resta importa: al reves daria el mismo numero con el "
                          + "signo cambiado.",
                "A = " + planteamientoEntreCurvas(contexto),
                ""));

        // ---------- Integrar y evaluar ----------
        if (!contexto.resolucion().fueAnalitica()) {
            pasos.addAll(pasosDeIntegracion(n, contexto.resolucion(), v));
            n++;
        } else {
            if (contexto.resolucion().seDesarrollo()) {
                pasos.add(new PasoSolucion(n++, "Simplificar el integrando",
                        "Antes de integrar se agrupan los terminos semejantes, para que quede "
                                + "una suma de potencias a la que se le aplica la regla de la "
                                + "potencia.",
                        contexto.detalle().isEmpty() ? contexto.resolucion().integrando()
                                : contexto.detalle().get(0).integrando()
                                  + "\n= " + contexto.detalle().get(0).resolucion().desarrollado(),
                        ""));
            }
            pasos.add(pasoDePrimitiva(n++, contexto.detalle().isEmpty()
                    ? contexto.resolucion() : contexto.detalle().get(0).resolucion(), v));
            pasos.add(pasoDeEvaluacionPorTramos(n++, contexto, false));
        }

        // ---------- Resultado ----------
        String suma = sumaDeTramos(contexto);
        pasos.add(new PasoSolucion(n, "Resultado final",
                contexto.detalle().size() > 1
                        ? "Sumando el area de cada tramo se obtiene el area total de la region."
                        : "El valor obtenido es el area de la region encerrada entre las dos "
                          + "curvas.",
                (suma.isEmpty() ? "" : "A = " + suma + "\n")
                        + "A = " + Fraccion.texto(contexto.area()) + " unidades cuadradas",
                Redondeo.texto(contexto.area())));

        return pasos;
    }

    /** A = integral de (arriba - abajo), partida por tramos si hace falta. */
    private String planteamientoEntreCurvas(ContextoArea contexto) {
        String v = contexto.variable();
        if (contexto.detalle().isEmpty()) {
            return integralDe(contexto.a(), contexto.b(), "f(" + v + ") - g(" + v + ")", v);
        }
        StringBuilder texto = new StringBuilder();
        for (DetalleTramo tramo : contexto.detalle()) {
            if (!texto.isEmpty()) {
                texto.append("\n  + ");
            }
            texto.append(integralDe(tramo.desde(), tramo.hasta(), tramo.integrando(), v));
        }
        return texto.toString();
    }

    // ==================================================================
    // PASOS COMUNES A LAS AREAS
    // ==================================================================

    /**
     * El paso que identifica que va arriba en cada tramo, con su punto de prueba.
     *
     * <p>Es el paso que mas se salta a mano y el que mas errores produce: suponer que la
     * primera funcion escrita es la de arriba. Se comprueba evaluando en un punto interior
     * de cada tramo, como se hace en clase.</p>
     */
    private PasoSolucion pasoDeSigno(int numero, ContextoArea contexto) {
        String v = contexto.variable();
        boolean enX = v.equals("x");
        String titulo = contexto.funcionG().isBlank()
                ? "Ver de que lado del eje queda la curva"
                : (enX ? "Identificar la funcion superior" : "Identificar la curva de la derecha");

        StringBuilder detalle = new StringBuilder();
        for (DetalleTramo tramo : contexto.detalle()) {
            detalle.append("En [").append(Redondeo.texto(tramo.desde())).append(", ")
                   .append(Redondeo.texto(tramo.hasta())).append("], con ").append(v).append(" = ")
                   .append(Redondeo.texto(tramo.prueba())).append(": ")
                   .append(tramo.arriba()).append(" = ").append(Redondeo.texto(tramo.valorArriba()))
                   .append(" y ").append(tramo.abajo()).append(" = ")
                   .append(Redondeo.texto(tramo.valorAbajo())).append("\n   -> ")
                   .append(tramo.arriba()).append(enX ? " va arriba" : " queda a la derecha")
                   .append(".\n");
        }

        return new PasoSolucion(numero, titulo,
                "No se puede suponer cual va " + (enX ? "arriba" : "a la derecha") + " por el orden "
                        + "en que se escribieron: se evalua en un punto interior de cada tramo y "
                        + "se compara.",
                detalle.toString().trim(),
                "");
    }

    /** El paso que da la primitiva. */
    private PasoSolucion pasoDePrimitiva(int numero, ResolutorIntegral.Resolucion resolucion,
                                         String variable) {
        StringBuilder reglas = new StringBuilder();
        for (String regla : resolucion.reglas()) {
            reglas.append("- ").append(regla).append('\n');
        }
        return new PasoSolucion(numero, "Integrar",
                "Se busca una primitiva F, es decir una funcion cuya derivada sea el "
                        + "integrando. Estas son las reglas que se aplicaron:",
                reglas.toString().trim()
                        + "\n\nF(" + variable + ") = " + resolucion.antiderivada() + " + C",
                "");
    }

    /**
     * Evalua la primitiva en los extremos de cada tramo, regla de Barrow.
     *
     * @param bajoCurva true si es area bajo la curva, donde un tramo bajo el eje cambia
     *                  de signo
     */
    private PasoSolucion pasoDeEvaluacionPorTramos(int numero, ContextoArea contexto,
                                                   boolean bajoCurva) {
        StringBuilder texto = new StringBuilder();
        int indice = 1;
        for (DetalleTramo tramo : contexto.detalle()) {
            if (contexto.detalle().size() > 1) {
                texto.append("Tramo ").append(indice).append(", de ")
                     .append(Redondeo.texto(tramo.desde())).append(" a ")
                     .append(Redondeo.texto(tramo.hasta())).append(":\n");
            }
            ResolutorIntegral.Resolucion r = tramo.resolucion();
            if (contexto.detalle().size() > 1 && !r.antiderivada().equals(contexto.resolucion().antiderivada())
                    && !bajoCurva) {
                texto.append("   F(").append(contexto.variable()).append(") = ")
                     .append(r.antiderivada()).append('\n');
            }
            for (String linea : r.sustitucion().split("\n")) {
                if (linea.startsWith("F(" + contexto.variable() + ")")) {
                    continue;
                }
                texto.append("   ").append(linea).append('\n');
            }
            if (bajoCurva && r.valor() < 0) {
                texto.append("   El tramo queda bajo el eje: su area es |")
                     .append(Fraccion.texto(r.valor())).append("| = ")
                     .append(Fraccion.texto(tramo.area())).append('\n');
            }
            indice++;
        }
        return new PasoSolucion(numero, "Evaluar los limites",
                "El teorema fundamental del calculo, o regla de Barrow, dice que la integral "
                        + "definida es la primitiva evaluada en el limite superior menos la "
                        + "primitiva evaluada en el inferior. La constante C se cancela en la "
                        + "resta.",
                texto.toString().trim(),
                "");
    }

    /**
     * La explicacion de los limites sin la ecuacion resuelta, cuando esa ecuacion ya tiene
     * su propio paso.
     *
     * <p>Los cortes se resuelven en el paso anterior, factorizacion incluida. Repetir
     * todas esas lineas al hablar de los limites solo alarga el procedimiento; aqui basta
     * con los puntos que se obtuvieron y con las rectas que cierran la region.</p>
     */
    private String sinCuentaRepetida(ContextoArea contexto) {
        String explicacion = contexto.explicacionLimites();
        if (contexto.explicacionCortes().isBlank() || explicacion.isBlank()
                || !explicacion.contains("   ")) {
            return explicacion;
        }
        StringBuilder resumen = new StringBuilder();
        for (String linea : explicacion.split("\n")) {
            boolean esDeLaCuenta = linea.startsWith("   ")
                    || linea.startsWith("Se igualan") || linea.startsWith("Se buscan los cortes");
            if (esDeLaCuenta) {
                continue;
            }
            if (linea.isBlank() && (resumen.isEmpty() || resumen.toString().endsWith("\n\n"))) {
                continue;
            }
            resumen.append(linea).append('\n');
        }
        String texto = resumen.toString().trim();
        return texto.isEmpty() ? explicacion
                : "Con los cortes que se hallaron en el paso anterior:\n\n" + texto;
    }

    /** La suma de las areas de los tramos: 1/4 + 1/4. */
    private String sumaDeTramos(ContextoArea contexto) {
        if (contexto.detalle().size() <= 1) {
            return "";
        }
        List<String> sumandos = new ArrayList<>();
        for (DetalleTramo tramo : contexto.detalle()) {
            sumandos.add(Fraccion.texto(tramo.area()));
        }
        return String.join(" + ", sumandos);
    }

    /** "f(x)" o "x = g(y)" segun la variable. */
    private String nombreFuncion(String variable) {
        return variable.equals("x") ? "f(x)" : "x = g(y)";
    }

    // ==================================================================
    // SOLIDOS DE REVOLUCION
    // ==================================================================

    /**
     * Redacta el procedimiento de un solido de revolucion.
     *
     * @param contexto datos del ejercicio ya resuelto
     * @return los pasos, en orden
     */
    public List<PasoSolucion> paraSolido(ContextoSolido contexto) {
        List<PasoSolucion> pasos = new ArrayList<>();
        int n = 1;
        String v = contexto.variable();

        // ---------- 1. Identificar ----------
        StringBuilder funciones = new StringBuilder("f(" + v + ") = " + contexto.funcionExterior());
        if (!contexto.funcionInterior().isBlank()) {
            funciones.append("\ng(").append(v).append(") = ").append(contexto.funcionInterior());
        }

        pasos.add(new PasoSolucion(n++, "Identificar el problema",
                "Se tiene una region plana que va a girar alrededor de una recta. Al girar, "
                        + "cada punto de la region describe una circunferencia, y el conjunto de "
                        + "todas ellas forma un cuerpo solido cuyo volumen se busca.",
                funciones.toString(),
                ""));

        // ---------- Region, cuando hubo que plantearla desde el enunciado ----------
        if (contexto.explicacionRegion() != null && !contexto.explicacionRegion().isBlank()) {
            pasos.add(pasoDeRegion(n++, contexto.explicacionRegion()));
        }

        // ---------- 2. Eje ----------
        pasos.add(new PasoSolucion(n++, "Identificar el eje de revolucion",
                "El eje determina hacia donde se mide el radio de giro, y es el dato que decide "
                        + "toda la geometria del solido: la misma region girada alrededor de dos "
                        + "rectas distintas produce solidos distintos.",
                "Eje de revolucion: " + contexto.eje().ecuacion()
                        + "  (" + contexto.eje().nombreLargo() + ")",
                ""));

        // ---------- 3. Limites ----------
        pasos.add(pasoDeLimites(n++, contexto.explicacionLimites(),
                contexto.a(), contexto.b(), v));

        // ---------- 4. Metodo ----------
        StringBuilder razones = new StringBuilder();
        for (String razon : contexto.razonesDelMetodo()) {
            razones.append("- ").append(razon).append('\n');
        }

        pasos.add(new PasoSolucion(n++, "Identificar el metodo",
                "El metodo no se elige por gusto: lo impone la geometria. Hay que ver en que "
                        + "direccion conviene rebanar el solido y si las rebanadas salen llenas o "
                        + "con agujero. " + contexto.tipo().explicacion(),
                razones.toString().trim() + "\n\nMetodo elegido: " + contexto.tipo().etiqueta()
                        + "\n" + formulaEn(contexto.tipo(), v),
                ""));

        // ---------- 5. Cual curva va mas lejos del eje ----------
        if (contexto.hayQueCompararLasCurvas() && contexto.tipo() == TipoSolido.CAPAS) {
            String donde = v.equals("y") ? "a la derecha" : "arriba";
            pasos.add(new PasoSolucion(n++, "Determinar cual curva va " + donde,
                    "La altura de cada cascaron es la distancia entre las dos curvas: la de "
                            + donde + " menos la otra. No se puede suponer cual es por el orden "
                            + "en que se escribieron; se comprueba evaluando las dos en un punto "
                            + "del intervalo.",
                    contexto.comprobacionPosicion(),
                    ""));
        } else if (contexto.hayQueCompararLasCurvas()) {
            pasos.add(new PasoSolucion(n++, "Determinar cual curva queda mas lejos del eje",
                    "Antes de escribir los radios hay que saber cual de las dos curvas queda "
                            + "por fuera, porque es la que da el radio exterior. No se puede "
                            + "suponer que sea la que aparece escrita primero: eso depende de "
                            + "como se redacto el enunciado, no de la geometria. Se resuelve "
                            + "como en clase, evaluando las dos en un punto del intervalo.",
                    contexto.comprobacionPosicion(),
                    ""));
        }

        // ---------- 6. Radios ----------
        pasos.add(pasoDeRadios(n++, contexto));

        // ---------- 7. Construir la integral ----------
        pasos.add(new PasoSolucion(n++, "Construir la integral",
                "Se sustituyen los radios y los limites en la formula del metodo. La constante "
                        + (contexto.tipo() == TipoSolido.CAPAS ? "2*pi" : "pi")
                        + " sale fuera de la integral porque no depende de " + v + ".",
                escribirIntegralDelSolido(contexto),
                ""));

        // ---------- 8. Simplificar ----------
        if (contexto.resolucion().seDesarrollo()) {
            pasos.add(new PasoSolucion(n++, "Simplificar el integrando",
                    "Antes de integrar conviene desarrollar los cuadrados y agrupar los "
                            + "terminos semejantes. Asi el integrando queda como una suma de "
                            + "potencias de " + v + ", y a cada una se le puede aplicar "
                            + "directamente la regla de la potencia.",
                    contexto.resolucion().integrando() + "\n= " + contexto.resolucion().desarrollado(),
                    ""));
        }

        // ---------- 9. Integrar y evaluar ----------
        List<PasoSolucion> integracion = pasosDeIntegracion(n, contexto.resolucion(), v);
        pasos.addAll(integracion);
        n += integracion.size();

        // ---------- 10. Resultado ----------
        String constante = (contexto.tipo() == TipoSolido.CAPAS) ? "2*pi" : "pi";
        StringBuilder resultado = new StringBuilder();
        resultado.append("V = ").append(constante).append(" * ")
                 .append(Fraccion.texto(Math.abs(contexto.resolucion().valor())))
                 .append(" = ").append(Redondeo.texto(contexto.volumen()))
                 .append(" unidades cubicas");

        if (!contexto.volumenEnPi().isBlank()) {
            resultado.append("\nEn terminos de pi:  V = ").append(contexto.volumenEnPi());
        }

        pasos.add(new PasoSolucion(n, "Resultado final",
                "Por ultimo se multiplica el valor de la integral por la constante que quedo "
                        + "fuera. En clase el volumen se suele dejar expresado como multiplo de "
                        + "pi, porque asi el resultado es exacto y no un decimal redondeado.",
                resultado.toString(),
                Redondeo.texto(contexto.volumen())));

        return pasos;
    }

    /**
     * Explica de donde sale cada radio.
     *
     * <p>Es el paso que el requisito pedagogico marca como imprescindible: no basta con
     * escribir que el radio vale {@code f(x)}, hay que decir por que la distancia de la
     * curva al eje es justamente esa.</p>
     */
    private PasoSolucion pasoDeRadios(int numero, ContextoSolido contexto) {
        String v = contexto.variable();
        EjeRotacion eje = contexto.eje();
        boolean ejeEnElOrigen = eje.pasaPorElOrigen();

        String explicacion;
        String formula;

        switch (contexto.tipo()) {
            case DISCOS -> {
                explicacion = "Como el solido gira alrededor de " + eje.nombreLargo()
                        + ", la distancia " + (eje.esHorizontal() ? "vertical" : "horizontal")
                        + " desde la curva hasta el eje es el radio del disco en ese punto. "
                        + (ejeEnElOrigen
                            ? "Al estar el eje en el origen, esa distancia es el valor mismo de "
                              + "la funcion."
                            : "Como el eje esta corrido hasta " + eje.ecuacion() + ", hay que "
                              + "restarle ese desplazamiento al valor de la funcion.");

                formula = "R(" + v + ") = " + contexto.radios().exterior();
            }
            case ARANDELAS -> {
                explicacion = "Hay dos radios porque la rebanada es un anillo. El exterior lo da "
                        + "la curva que queda mas lejos del eje y el interior la que queda mas "
                        + "cerca, que es la que delimita el agujero. El area del anillo es la del "
                        + "circulo grande menos la del pequeno, y de ahi sale la resta de "
                        + "cuadrados: se restan las areas, no los radios.";

                formula = "R(" + v + ") = " + contexto.radios().exterior()
                        + "   (radio exterior)"
                        + "\nr(" + v + ") = " + contexto.radios().interior()
                        + "   (radio interior)"
                        + "\nArea del anillo = pi*R^2 - pi*r^2 = pi*(R^2 - r^2)";
            }
            case CAPAS -> {
                explicacion = "Aqui el radio no es el valor de la funcion. Cada rebanada "
                        + (v.equals("x") ? "vertical" : "horizontal")
                        + " de la region, al girar, describe un cilindro hueco; su radio es la "
                        + "distancia de esa rebanada al eje "
                        + (ejeEnElOrigen ? "(la propia variable " + v + ")"
                                         : eje.ecuacion() + ", medida de modo que salga positiva")
                        + ", y su altura es la distancia entre los dos bordes de la region. Si "
                        + "se corta el cilindro y se aplana queda un rectangulo de largo "
                        + "2*pi*radio y de alto la altura, y eso explica el 2*pi de la formula.";

                formula = "radio  = " + contexto.radios().radioCapa()
                        + "\naltura = " + contexto.radios().alturaCapa();
            }
            default -> {
                explicacion = "";
                formula = "";
            }
        }

        return new PasoSolucion(numero, "Determinar los radios", explicacion, formula, "");
    }

    /** La formula del metodo escrita con la variable de integracion: R(y), dy. */
    private String formulaEn(TipoSolido tipo, String variable) {
        if (variable.equals("x")) {
            return tipo.formula();
        }
        return tipo.formula().replace("(x)", "(y)").replace("dx", "dy").replace(" x * ", " y * ");
    }

    /** Escribe la integral del solido ya con los datos sustituidos. */
    private String escribirIntegralDelSolido(ContextoSolido contexto) {
        String v = contexto.variable();
        String rango = "de " + Redondeo.texto(contexto.a()) + " a " + Redondeo.texto(contexto.b());

        return switch (contexto.tipo()) {
            case DISCOS -> "V = pi * integral " + rango + " de ["
                    + contexto.resolucion().integrando() + "] d" + v;
            case ARANDELAS -> "V = pi * integral " + rango + " de ("
                    + contexto.resolucion().integrando() + ") d" + v;
            case CAPAS -> "V = 2*pi * integral " + rango + " de ("
                    + contexto.resolucion().integrando() + ") d" + v;
        };
    }

    // ==================================================================
    // PASOS COMUNES
    // ==================================================================

    /**
     * El paso que fija los limites de integracion.
     *
     * <p>Cuando los limites se buscaron a partir de las intersecciones, aqui se muestra
     * el planteamiento completo. El estudiante tiene que poder ver de donde salieron esos
     * numeros: usarlos sin explicarlos convertiria el resultado en algo que hay que creer
     * en lugar de algo que se puede seguir.</p>
     */
    private PasoSolucion pasoDeLimites(int numero, String explicacion, double a, double b,
                                       String variable) {
        if (explicacion == null || explicacion.isBlank()) {
            return new PasoSolucion(numero, "Determinar los limites de integracion",
                    "Los limites vienen dados en el enunciado y delimitan la parte del eje "
                            + "sobre la que se extiende la region.",
                    "a = " + Redondeo.texto(a) + "\nb = " + Redondeo.texto(b),
                    "");
        }

        boolean escritos = explicacion.startsWith("Para los limites de integracion");
        boolean yaConcluye = escritos || explicacion.contains("el intervalo es");
        return new PasoSolucion(numero, "Determinar los limites de integracion",
                escritos
                        ? "Los limites vienen en el enunciado y delimitan la parte del eje sobre "
                          + "la que se extiende la region."
                        : "Los limites no vienen escritos como numeros, asi que se deducen de la "
                          + "propia region: de los puntos donde se cortan sus bordes y de las "
                          + "rectas que la cierran.",
                explicacion + (yaConcluye ? "\n\n" : "\n\nPor lo tanto:\n")
                        + "a = " + Redondeo.texto(a) + "\nb = " + Redondeo.texto(b),
                "");
    }

    /**
     * Los pasos de integrar y de evaluar los limites.
     *
     * <p>Van juntos porque son las dos mitades de la regla de Barrow, y separarlos en dos
     * metodos distintos haria que se pudieran generar por separado y quedar
     * descolocados.</p>
     */
    private List<PasoSolucion> pasosDeIntegracion(int numeroInicial,
                                                  ResolutorIntegral.Resolucion resolucion,
                                                  String variable) {
        List<PasoSolucion> pasos = new ArrayList<>();
        int n = numeroInicial;

        if (!resolucion.fueAnalitica()) {
            pasos.add(new PasoSolucion(n, "Resolver la integral",
                    "Este integrando no tiene una primitiva que se pueda escribir con las "
                            + "funciones elementales del curso, asi que no se puede aplicar la "
                            + "regla de Barrow. El valor se obtuvo por aproximacion numerica: es "
                            + "fiable como numero, pero no hay un procedimiento simbolico que "
                            + "copiar al cuaderno.",
                    String.join("\n", resolucion.reglas()),
                    Redondeo.texto(resolucion.valor())));
            return pasos;
        }

        StringBuilder reglas = new StringBuilder();
        for (String regla : resolucion.reglas()) {
            reglas.append("- ").append(regla).append('\n');
        }

        pasos.add(new PasoSolucion(n++, "Integrar",
                "Se busca una primitiva F, es decir una funcion cuya derivada sea el "
                        + "integrando. Estas son las reglas que se aplicaron:",
                reglas.toString().trim()
                        + "\n\nF(" + variable + ") = " + resolucion.antiderivada() + " + C",
                ""));

        pasos.add(new PasoSolucion(n, "Evaluar los limites",
                "El teorema fundamental del calculo, o regla de Barrow, dice que la integral "
                        + "definida es la primitiva evaluada en el limite superior menos la "
                        + "primitiva evaluada en el inferior. La constante C se cancela en la "
                        + "resta, y por eso no hace falta arrastrarla.",
                resolucion.sustitucion(),
                Redondeo.texto(resolucion.valor())));

        return pasos;
    }

    // ==================================================================
    // ESCRITURA
    // ==================================================================

    /** Escribe una integral definida con sus limites. */
    private String integralDe(double a, double b, String integrando, String variable) {
        return "integral de " + Redondeo.texto(a) + " a " + Redondeo.texto(b)
                + " de (" + integrando + ") d" + variable;
    }

    /** Escribe la suma de los tramos en que se partio la integral. */
    private String escribirSumaDeTramos(ContextoArea contexto) {
        StringBuilder suma = new StringBuilder();
        List<Double> cortes = new ArrayList<>();
        cortes.add(contexto.a());
        cortes.addAll(contexto.cruces());
        cortes.add(contexto.b());

        for (int i = 0; i < cortes.size() - 1; i++) {
            if (i > 0) {
                suma.append(" + ");
            }
            suma.append("|integral de ").append(Redondeo.texto(cortes.get(i)))
                .append(" a ").append(Redondeo.texto(cortes.get(i + 1))).append("|");
        }
        return suma.toString();
    }

    /** Escribe una lista de valores separados por comas. */
    private String listaDeValores(List<Double> valores) {
        List<String> textos = new ArrayList<>();
        for (double valor : valores) {
            textos.add(Redondeo.texto(valor));
        }
        return String.join(", ", textos);
    }
}
