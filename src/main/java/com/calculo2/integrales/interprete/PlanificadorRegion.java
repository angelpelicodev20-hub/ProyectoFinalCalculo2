package com.calculo2.integrales.interprete;

import com.calculo2.integrales.math.algebra.Despejador;
import com.calculo2.integrales.math.algebra.ResolutorEcuaciones;
import com.calculo2.integrales.math.parser.EvaluadorExpresion;
import com.calculo2.integrales.math.simbolico.ConversorPolinomio;
import com.calculo2.integrales.math.util.Redondeo;
import com.calculo2.integrales.math.util.ValidadorFuncion;
import com.calculo2.integrales.model.Curva;
import com.calculo2.integrales.model.EjeRotacion;
import com.calculo2.integrales.model.Frontera;
import com.calculo2.integrales.model.OrigenDato;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Decide que region describe el enunciado y como se rebana.
 *
 * <p>Con las curvas, las rectas y las demas pistas del enunciado ya leidas, falta
 * responder la pregunta que de verdad importa: cual es la region. Eso significa elegir</p>
 *
 * <ul>
 *   <li>respecto de que variable se integra: rebanadas verticales (x) u horizontales (y);</li>
 *   <li>que dos bordes la limitan en cada rebanada, y que rama de cada curva es la que
 *       cuenta ({@code y^2 = 8x} tiene una rama arriba y otra abajo);</li>
 *   <li>entre que valores se extiende: el intervalo de integracion.</li>
 * </ul>
 *
 * <p>El procedimiento es el que se haria a mano, pero probando todas las combinaciones.
 * Para cada par de bordes se calculan sus puntos de corte y se forman los intervalos
 * posibles: el escrito en el enunciado, el que cierran dos rectas, el que cierran los
 * cortes de las curvas, el que cierra una recta con un corte. Cada candidato se comprueba
 * (los dos bordes existen en todo el intervalo, ninguna otra frontera del enunciado
 * atraviesa la region, se respeta el cuadrante) y se puntua por cuantas de las fronteras
 * que nombra el enunciado usa. La region buena es la que usa todas.</p>
 *
 * <p>Cuando en una variable la region no se puede describir con dos bordes fijos (quedaria
 * partida en tramos), se prueba la otra. Es lo que pasa con {@code x = y^2} y
 * {@code x = y + 6}: con rebanadas verticales la region cambia de borde a mitad de camino,
 * y con rebanadas horizontales no.</p>
 *
 * <p>Nada de esto inventa datos. Los elementos que no estan escritos pero que el
 * enunciado implica (el eje X como suelo en un area bajo la curva, los ejes en el primer
 * cuadrante) entran marcados como implicitos, y una region que dependa de ellos lo dice.</p>
 */
public final class PlanificadorRegion {

    private PlanificadorRegion() {
    }

    /** Hasta donde se buscan cortes cuando el enunciado no acota la region. */
    private static final double ALCANCE = 100.0;

    /** Muestras con que se comprueba cada candidato. */
    private static final int MUESTRAS = 64;

    /** Dos valores mas cercanos que esto se consideran el mismo. */
    private static final double TOLERANCIA = 1e-7;

    // ==================================================================
    // DATOS
    // ==================================================================

    /** Cuadrante en que el enunciado situa la region. */
    public enum Cuadrante {
        NINGUNO(""), PRIMERO("primer"), SEGUNDO("segundo"), TERCERO("tercer"), CUARTO("cuarto");

        private final String nombre;

        Cuadrante(String nombre) {
            this.nombre = nombre;
        }

        /** El nombre, para los textos: "primer cuadrante". */
        public String texto() {
            return nombre + " cuadrante";
        }

        /** Rango permitido para x: {minimo, maximo}. */
        double[] rangoX() {
            return switch (this) {
                case PRIMERO, CUARTO -> new double[]{0, Double.POSITIVE_INFINITY};
                case SEGUNDO, TERCERO -> new double[]{Double.NEGATIVE_INFINITY, 0};
                default -> new double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
            };
        }

        /** Rango permitido para y. */
        double[] rangoY() {
            return switch (this) {
                case PRIMERO, SEGUNDO -> new double[]{0, Double.POSITIVE_INFINITY};
                case TERCERO, CUARTO -> new double[]{Double.NEGATIVE_INFINITY, 0};
                default -> new double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
            };
        }

        /** La desigualdad en palabras: "x >= 0, y >= 0". */
        String desigualdades() {
            return switch (this) {
                case PRIMERO -> "x >= 0, y >= 0";
                case SEGUNDO -> "x <= 0, y >= 0";
                case TERCERO -> "x <= 0, y <= 0";
                case CUARTO -> "x >= 0, y <= 0";
                default -> "";
            };
        }
    }

    /**
     * Lo que se sabe del enunciado.
     *
     * @param curvas          las curvas, con sus despejes
     * @param fronteras       las rectas que cierran la region
     * @param intervalo       el intervalo escrito, si lo hay
     * @param cuadrante       el cuadrante en que esta la region
     * @param sueloImplicito  true si el eje X cierra la region sin que el enunciado lo nombre
     * @param razonSuelo      por que se toma ese suelo, para explicarlo
     * @param origenSuelo     que tan firme es esa lectura
     * @param eje             el eje de giro, o null
     * @param variableForzada la variable que exige el metodo pedido, o null
     * @param motivoForzada   por que la exige
     */
    public record Entrada(List<Curva> curvas, List<Frontera> fronteras,
                          Optional<ExtractorIntervalo.Intervalo> intervalo, Cuadrante cuadrante,
                          boolean sueloImplicito, String razonSuelo, OrigenDato origenSuelo,
                          EjeRotacion eje, String variableForzada, String motivoForzada) {
    }

    /**
     * Un borde de la region visto como funcion de la variable de integracion.
     *
     * @param id        identifica el elemento del enunciado del que sale
     * @param expresion la funcion, escrita para el parser
     * @param ecuacion  como se nombra en los textos: "y = sqrt(8x)"
     * @param funcion   la funcion compilada
     * @param implicito true si el enunciado no lo nombra
     * @param orden     posicion en el enunciado, para respetar el orden de escritura
     * @param signo     +1 o -1 si es una rama de una raiz par, 0 si no
     * @param esRecta   true si es una recta constante
     * @param curva     la curva de la que sale, o null si es una recta
     * @param rama      la rama del despeje, o null
     * @param origen    que tan firme es el dato
     */
    public record Borde(String id, String expresion, String ecuacion, EvaluadorExpresion funcion,
                        boolean implicito, int orden, int signo, boolean esRecta, Curva curva,
                        Despejador.Rama rama, OrigenDato origen) {

        double en(double valor) {
            return ValidadorFuncion.evaluarSeguro(funcion.comoFuncion(), valor);
        }

        boolean requirioDespeje(String variable) {
            return curva != null && curva.requiereDespejeEn(variable);
        }

        /** Indica si es la recta constante de ese valor. */
        public boolean esLaConstante(double valor) {
            return esRecta && Math.abs(en(0.0) - valor) < 1e-12;
        }
    }

    /**
     * Una recta perpendicular a la variable de integracion: cierra el intervalo.
     *
     * @param id        identifica el elemento del enunciado
     * @param valor     donde corta
     * @param ecuacion  como se nombra
     * @param implicito true si el enunciado no la nombra como frontera
     * @param origen    que tan firme es el dato
     */
    record Corte(String id, double valor, String ecuacion, boolean implicito, OrigenDato origen) {
    }

    /**
     * Un punto destacado de la region.
     *
     * @param x     abscisa
     * @param y     ordenada
     * @param texto el punto escrito con valores exactos
     */
    public record Punto(double x, double y, String texto) {
    }

    /**
     * La region decidida.
     *
     * @param variable                variable de integracion
     * @param a                       limite inferior
     * @param b                       limite superior
     * @param textoA                  el limite inferior escrito de forma exacta
     * @param textoB                  el limite superior escrito de forma exacta
     * @param primero                 el borde que aparece antes en el enunciado
     * @param segundo                 el otro borde
     * @param superior                el que queda arriba (o a la derecha) en la mayor parte
     * @param inferior                el que queda abajo (o a la izquierda)
     * @param origen                  que tan firme es el intervalo
     * @param explicacionRegion       como se armo la region: despejes, ramas, variable
     * @param explicacionLimites      como se obtuvo el intervalo
     * @param ambiguedades            otras lecturas posibles
     * @param puntos                  cortes y esquinas de la region
     * @param respetaVariableForzada  false si el metodo pedido no se pudo aplicar tal cual
     * @param completo                true si la region usa todas las fronteras del enunciado
     * @param seCruzan                true si los bordes se cruzan dentro del intervalo
     */
    public record Plan(String variable, double a, double b, String textoA, String textoB,
                       Borde primero, Borde segundo, Borde superior, Borde inferior,
                       OrigenDato origen, String explicacionRegion, String explicacionLimites,
                       List<String> ambiguedades, List<Punto> puntos,
                       boolean respetaVariableForzada, boolean completo, boolean seCruzan) {
    }

    /** Un intervalo candidato para un par de bordes. */
    private record Opcion(Borde f, Borde g, double a, double b, double rango, String tipo,
                          List<Corte> cortes, ResolutorEcuaciones.Solucion cruces,
                          boolean recortado) {
    }

    /** Un candidato ya comprobado y puntuado. */
    private record Candidato(Opcion opcion, String variable, int cobertura, int despejes,
                             boolean polinomico, int signos, Borde superior, Borde inferior,
                             boolean seCruzan, Set<String> usados) {
    }

    /** Los bordes y cortes de la region vistos desde una variable. */
    private record Espacio(String variable, List<Borde> bordes, List<Corte> cortes) {
    }

    // ==================================================================
    // ENTRADA PRINCIPAL
    // ==================================================================

    /**
     * Decide la region.
     *
     * @param entrada lo que se sabe del enunciado
     * @return el plan, o vacio si ninguna region cierra con los datos disponibles
     */
    public static Optional<Plan> planificar(Entrada entrada) {
        Set<String> explicitos = elementosExplicitos(entrada);

        Espacio enX = construirEspacio(entrada, "x");
        Espacio enY = construirEspacio(entrada, "y");

        List<Candidato> candidatosX = evaluarEspacio(enX, entrada, explicitos);
        List<Candidato> candidatosY = evaluarEspacio(enY, entrada, explicitos);

        Optional<Candidato> mejorX = candidatosX.isEmpty() ? Optional.empty() : Optional.of(candidatosX.get(0));
        Optional<Candidato> mejorY = candidatosY.isEmpty() ? Optional.empty() : Optional.of(candidatosY.get(0));

        if (mejorX.isEmpty() && mejorY.isEmpty()) {
            return Optional.empty();
        }

        int total = explicitos.size();
        Candidato elegido;
        boolean respetaForzada = true;
        String forzada = entrada.variableForzada();

        if (forzada != null) {
            Optional<Candidato> enForzada = forzada.equals("x") ? mejorX : mejorY;
            Optional<Candidato> enOtra = forzada.equals("x") ? mejorY : mejorX;
            if (enForzada.isPresent() && enForzada.get().cobertura() >= total) {
                elegido = enForzada.get();
            } else if (enOtra.isPresent()
                    && (enForzada.isEmpty() || enOtra.get().cobertura() > enForzada.get().cobertura())) {
                elegido = enOtra.get();
                respetaForzada = false;
            } else {
                elegido = enForzada.orElseGet(enOtra::get);
            }
        } else if (mejorX.isEmpty()) {
            elegido = mejorY.get();
        } else if (mejorY.isEmpty()) {
            elegido = mejorX.get();
        } else {
            elegido = COMPARADOR_ENTRE_VARIABLES.compare(mejorX.get(), mejorY.get()) <= 0
                    ? mejorX.get() : mejorY.get();
        }

        List<Candidato> mismosCandidatos = elegido.variable().equals("x") ? candidatosX : candidatosY;
        Candidato otraVariable = elegido.variable().equals("x")
                ? mejorY.orElse(null) : mejorX.orElse(null);

        return Optional.of(construirPlan(elegido, mismosCandidatos, otraVariable, entrada, total,
                respetaForzada));
    }

    // ==================================================================
    // CONSTRUCCION DE CADA ESPACIO
    // ==================================================================

    /** Los ids de lo que el enunciado nombra: lo que una buena region tiene que usar. */
    private static Set<String> elementosExplicitos(Entrada entrada) {
        Set<String> ids = new LinkedHashSet<>();
        for (Curva curva : entrada.curvas()) {
            ids.add(idCurva(curva));
        }
        for (int i = 0; i < entrada.fronteras().size(); i++) {
            ids.add("r" + i);
        }
        if (entrada.intervalo().isPresent()) {
            ids.add("intervalo");
        }
        return ids;
    }

    private static String idCurva(Curva curva) {
        return "c" + curva.orden();
    }

    /** Arma los bordes y los cortes vistos desde una variable. */
    private static Espacio construirEspacio(Entrada entrada, String v) {
        List<Borde> bordes = new ArrayList<>();
        List<Corte> cortes = new ArrayList<>();
        String w = v.equals("x") ? "y" : "x";

        // ---------- Curvas ----------
        for (Curva curva : entrada.curvas()) {
            Despejador.Despeje despeje = curva.despejeEn(v);
            if (despeje == null) {
                continue;
            }
            for (Despejador.Rama rama : despeje.ramas()) {
                compilar(rama.expresion(), v).ifPresent(funcion -> bordes.add(new Borde(
                        idCurva(curva), rama.expresion(), w + " = " + rama.expresion(), funcion,
                        false, curva.posicion(), rama.signo(), false, curva, rama,
                        OrigenDato.EXPLICITO)));
            }
        }

        // ---------- Rectas ----------
        for (int i = 0; i < entrada.fronteras().size(); i++) {
            Frontera frontera = entrada.fronteras().get(i);
            boolean esFuncionDeV = frontera.esVertical() == v.equals("y");
            if (esFuncionDeV) {
                agregarRecta(bordes, "r" + i, frontera.valor(), w, false, posicionDe(entrada, i),
                        frontera.origen(), v);
            } else {
                cortes.add(new Corte("r" + i, frontera.valor(), frontera.ecuacion(), false,
                        frontera.origen()));
            }
        }

        // ---------- Intervalo escrito ----------
        entrada.intervalo().ifPresent(intervalo -> {
            double desde = Math.min(intervalo.inferior(), intervalo.superior());
            double hasta = Math.max(intervalo.inferior(), intervalo.superior());
            if (intervalo.variable().equals(v)) {
                cortes.add(new Corte("intervalo", desde, v + " = " + Redondeo.texto(desde), false,
                        OrigenDato.EXPLICITO));
                cortes.add(new Corte("intervalo", hasta, v + " = " + Redondeo.texto(hasta), false,
                        OrigenDato.EXPLICITO));
            } else {
                // Acotar x en [0, 2] es lo mismo que cerrar la region con las rectas x = 0
                // y x = 2, que vistas desde y son dos bordes constantes.
                agregarRecta(bordes, "intervalo", desde, intervalo.variable(), false, 1_000_000,
                        OrigenDato.EXPLICITO, v);
                agregarRecta(bordes, "intervalo", hasta, intervalo.variable(), false, 1_000_001,
                        OrigenDato.EXPLICITO, v);
            }
        });

        // ---------- El eje X como suelo, cuando el enunciado lo implica ----------
        if (entrada.sueloImplicito() && !hayRecta(entrada.fronteras(), false, 0.0)) {
            if (v.equals("x")) {
                agregarRecta(bordes, "suelo", 0.0, "y", true, 2_000_000, entrada.origenSuelo(), v);
            } else {
                cortes.add(new Corte("suelo", 0.0, "y = 0", true, entrada.origenSuelo()));
            }
        }

        // ---------- Los ejes del cuadrante ----------
        if (entrada.cuadrante() != Cuadrante.NINGUNO) {
            boolean yaHaySuelo = entrada.sueloImplicito() || hayRecta(entrada.fronteras(), false, 0.0);
            if (!yaHaySuelo) {
                if (v.equals("x")) {
                    agregarRecta(bordes, "cuadrante", 0.0, "y", true, 2_000_001, OrigenDato.DERIVADO, v);
                } else {
                    cortes.add(new Corte("cuadrante", 0.0, "y = 0", true, OrigenDato.DERIVADO));
                }
            }
            if (!hayRecta(entrada.fronteras(), true, 0.0)) {
                if (v.equals("x")) {
                    cortes.add(new Corte("cuadrante", 0.0, "x = 0", true, OrigenDato.DERIVADO));
                } else {
                    agregarRecta(bordes, "cuadrante", 0.0, "x", true, 2_000_002, OrigenDato.DERIVADO, v);
                }
            }
        }

        // ---------- El eje de giro como posible borde ----------
        EjeRotacion eje = entrada.eje();
        if (eje != null) {
            boolean perpendicularALaVariable = v.equals("x") != eje.esHorizontal();
            if (perpendicularALaVariable) {
                cortes.add(new Corte("eje", eje.desplazamiento(), eje.ecuacion(), true,
                        OrigenDato.DERIVADO));
            }
        }

        return new Espacio(v, bordes, cortes);
    }

    private static int posicionDe(Entrada entrada, int indiceFrontera) {
        // Las rectas van despues de las curvas que aparecen en el mismo lugar; el orden
        // solo se usa para presentar f y g como los escribio el estudiante.
        return 500_000 + indiceFrontera;
    }

    private static void agregarRecta(List<Borde> bordes, String id, double valor, String variableDeLaRecta,
                                     boolean implicito, int orden, OrigenDato origen, String v) {
        String texto = Redondeo.texto(valor);
        compilar(texto, v).ifPresent(funcion -> bordes.add(new Borde(id, texto,
                variableDeLaRecta + " = " + texto, funcion, implicito, orden, 0, true, null, null,
                origen)));
    }

    private static boolean hayRecta(List<Frontera> fronteras, boolean vertical, double valor) {
        for (Frontera frontera : fronteras) {
            if (frontera.esVertical() == vertical && Math.abs(frontera.valor() - valor) < 1e-12) {
                return true;
            }
        }
        return false;
    }

    private static Optional<EvaluadorExpresion> compilar(String expresion, String variable) {
        try {
            return Optional.of(EvaluadorExpresion.compilar(expresion, variable));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    // ==================================================================
    // CANDIDATOS
    // ==================================================================

    /** Prueba todos los pares de bordes y devuelve los candidatos validos, el mejor primero. */
    private static List<Candidato> evaluarEspacio(Espacio espacio, Entrada entrada, Set<String> explicitos) {
        List<Candidato> candidatos = new ArrayList<>();
        List<Borde> bordes = espacio.bordes();
        Map<String, ResolutorEcuaciones.Solucion> cache = new HashMap<>();

        for (int i = 0; i < bordes.size(); i++) {
            for (int j = i + 1; j < bordes.size(); j++) {
                Borde f = bordes.get(i);
                Borde g = bordes.get(j);
                if (f.implicito() && g.implicito()) {
                    continue;
                }
                // Dos rectas paralelas nunca encierran region por si solas en esta
                // direccion: hacen falta cortes, y se prueban igual.
                ResolutorEcuaciones.Solucion cruces = cache.computeIfAbsent(i + "-" + j,
                        clave -> cruces(f, g, espacio.variable()));
                if (cruces.identicas()) {
                    continue;
                }
                for (Opcion opcion : opciones(f, g, cruces, espacio, entrada)) {
                    evaluar(opcion, espacio, entrada, explicitos).ifPresent(candidatos::add);
                }
            }
        }
        candidatos.sort(COMPARADOR_EN_UNA_VARIABLE);
        return candidatos;
    }

    /** Resuelve f = g y se queda con las soluciones donde las dos existen. */
    private static ResolutorEcuaciones.Solucion cruces(Borde f, Borde g, String v) {
        ResolutorEcuaciones.Solucion solucion = ResolutorEcuaciones.resolver(
                f.funcion().raiz(), g.funcion().raiz(), f.expresion(), g.expresion(),
                v, -ALCANCE, ALCANCE).entre(-ALCANCE, ALCANCE);
        List<com.calculo2.integrales.math.algebra.Factorizador.Raiz> validas = new ArrayList<>();
        for (var raiz : solucion.raices()) {
            double enF = f.en(raiz.valor());
            double enG = g.en(raiz.valor());
            if (Double.isFinite(enF) && Double.isFinite(enG)
                    && Math.abs(enF - enG) < 1e-6 * Math.max(1.0, Math.abs(enF))) {
                validas.add(raiz);
            }
        }
        return new ResolutorEcuaciones.Solucion(validas, solucion.pasos(), solucion.exacta(),
                solucion.identicas());
    }

    /** Los intervalos posibles para un par de bordes. */
    private static List<Opcion> opciones(Borde f, Borde g, ResolutorEcuaciones.Solucion cruces,
                                         Espacio espacio, Entrada entrada) {
        List<Opcion> opciones = new ArrayList<>();
        List<Double> valores = cruces.valores();
        boolean conImplicito = f.implicito() || g.implicito();

        List<Corte> explicitos = new ArrayList<>();
        List<Corte> implicitos = new ArrayList<>();
        List<Corte> delIntervalo = new ArrayList<>();
        for (Corte corte : espacio.cortes()) {
            if (corte.id().equals("intervalo")) {
                delIntervalo.add(corte);
            } else if (corte.implicito()) {
                implicitos.add(corte);
            } else {
                explicitos.add(corte);
            }
        }

        // 1. El intervalo escrito.
        if (delIntervalo.size() == 2) {
            opciones.add(new Opcion(f, g, delIntervalo.get(0).valor(), delIntervalo.get(1).valor(),
                    1, "intervalo", delIntervalo, cruces, false));
        }

        // 2. Dos rectas del enunciado.
        for (int i = 0; i < explicitos.size(); i++) {
            for (int j = i + 1; j < explicitos.size(); j++) {
                Corte uno = explicitos.get(i);
                Corte otro = explicitos.get(j);
                if (Math.abs(uno.valor() - otro.valor()) < TOLERANCIA) {
                    continue;
                }
                Corte menor = uno.valor() < otro.valor() ? uno : otro;
                Corte mayor = uno.valor() < otro.valor() ? otro : uno;
                opciones.add(new Opcion(f, g, menor.valor(), mayor.valor(), 2, "rectas",
                        List.of(menor, mayor), cruces, false));
            }
        }

        // 3. Una recta y el corte mas cercano de los bordes, a cada lado.
        double rangoRectaYCorte = conImplicito ? 5 : 3;
        for (Corte recta : explicitos) {
            agregarRectaYCorte(opciones, f, g, cruces, recta, valores, rangoRectaYCorte);
        }

        // 4. Los cortes de los dos bordes entre si.
        double rangoCortes = conImplicito ? 6 : 3;
        if (valores.size() >= 2) {
            opciones.add(new Opcion(f, g, valores.get(0), valores.get(valores.size() - 1),
                    rangoCortes, "cruces", List.of(), cruces, false));
            if (valores.size() > 2) {
                for (int i = 0; i + 1 < valores.size(); i++) {
                    opciones.add(new Opcion(f, g, valores.get(i), valores.get(i + 1),
                            rangoCortes + 0.5, "cruces", List.of(), cruces, false));
                }
            }
        }

        // 5. Una recta del enunciado y una implicita (el eje de giro, un eje del cuadrante).
        for (Corte recta : explicitos) {
            for (Corte implicita : implicitos) {
                if (Math.abs(recta.valor() - implicita.valor()) < TOLERANCIA) {
                    continue;
                }
                Corte menor = recta.valor() < implicita.valor() ? recta : implicita;
                Corte mayor = recta.valor() < implicita.valor() ? implicita : recta;
                opciones.add(new Opcion(f, g, menor.valor(), mayor.valor(), 4, "recta+eje",
                        List.of(menor, mayor), cruces, false));
            }
        }

        // 6. Una recta implicita y un corte de los bordes.
        for (Corte implicita : implicitos) {
            agregarRectaYCorte(opciones, f, g, cruces, implicita, valores, 5.5);
        }

        // Todas las opciones se recortan al cuadrante, si el enunciado lo fija.
        List<Opcion> recortadas = new ArrayList<>();
        double[] rango = espacio.variable().equals("x")
                ? entrada.cuadrante().rangoX() : entrada.cuadrante().rangoY();
        for (Opcion opcion : opciones) {
            double a = Math.max(opcion.a(), rango[0]);
            double b = Math.min(opcion.b(), rango[1]);
            if (b - a <= TOLERANCIA) {
                continue;
            }
            boolean recortada = a != opcion.a() || b != opcion.b();
            recortadas.add(recortada
                    ? new Opcion(opcion.f(), opcion.g(), a, b, Math.max(opcion.rango(), 5),
                            opcion.tipo(), opcion.cortes(), opcion.cruces(), true)
                    : opcion);
        }
        return recortadas;
    }

    private static void agregarRectaYCorte(List<Opcion> opciones, Borde f, Borde g,
                                           ResolutorEcuaciones.Solucion cruces, Corte recta,
                                           List<Double> valores, double rango) {
        Double izquierda = null;
        Double derecha = null;
        for (double valor : valores) {
            if (valor < recta.valor() - TOLERANCIA) {
                izquierda = valor;
            } else if (valor > recta.valor() + TOLERANCIA && derecha == null) {
                derecha = valor;
            }
        }
        if (izquierda != null) {
            opciones.add(new Opcion(f, g, izquierda, recta.valor(), rango, "recta+corte",
                    List.of(recta), cruces, false));
        }
        if (derecha != null) {
            opciones.add(new Opcion(f, g, recta.valor(), derecha, rango, "recta+corte",
                    List.of(recta), cruces, false));
        }
    }

    /**
     * Comprueba un candidato y lo puntua.
     *
     * @return el candidato, o vacio si no describe una region valida
     */
    private static Optional<Candidato> evaluar(Opcion opcion, Espacio espacio, Entrada entrada,
                                               Set<String> explicitos) {
        double a = opcion.a();
        double b = opcion.b();
        if (b - a <= TOLERANCIA) {
            return Optional.empty();
        }
        Borde f = opcion.f();
        Borde g = opcion.g();
        String v = espacio.variable();

        double[] rangoOtra = v.equals("x") ? entrada.cuadrante().rangoY() : entrada.cuadrante().rangoX();

        // Un intervalo escrito para la otra variable tambien acota la region: si el
        // enunciado dice 0 <= x <= 2, una descripcion respecto de y no puede salirse de ahi.
        if (entrada.intervalo().isPresent() && !entrada.intervalo().get().variable().equals(v)) {
            ExtractorIntervalo.Intervalo escrito = entrada.intervalo().get();
            rangoOtra = new double[]{
                    Math.max(rangoOtra[0], Math.min(escrito.inferior(), escrito.superior())),
                    Math.min(rangoOtra[1], Math.max(escrito.inferior(), escrito.superior()))};
        }

        int definidos = 0;
        int fArriba = 0;
        int gArriba = 0;
        double mayorSeparacion = 0.0;
        double paso = (b - a) / MUESTRAS;

        for (int k = 0; k < MUESTRAS; k++) {
            double punto = a + (k + 0.5) * paso;
            double enF = f.en(punto);
            double enG = g.en(punto);
            if (!ValidadorFuncion.esUtilizable(enF) || !ValidadorFuncion.esUtilizable(enG)) {
                continue;
            }
            definidos++;

            // El cuadrante acota tambien la otra variable: en el primero, nada por debajo
            // del eje X.
            double escala = Math.max(1.0, Math.max(Math.abs(enF), Math.abs(enG)));
            if (enF < rangoOtra[0] - 1e-9 * escala || enF > rangoOtra[1] + 1e-9 * escala
                    || enG < rangoOtra[0] - 1e-9 * escala || enG > rangoOtra[1] + 1e-9 * escala) {
                return Optional.empty();
            }

            double separacion = Math.abs(enF - enG);
            mayorSeparacion = Math.max(mayorSeparacion, separacion);
            if (separacion > 1e-12) {
                if (enF > enG) {
                    fArriba++;
                } else {
                    gArriba++;
                }
            }

            // Ninguna otra frontera del enunciado puede atravesar la region.
            double bajo = Math.min(enF, enG);
            double alto = Math.max(enF, enG);
            for (Borde otro : espacio.bordes()) {
                if (otro == f || otro == g || otro.implicito()) {
                    continue;
                }
                double enOtro = otro.en(punto);
                if (Double.isFinite(enOtro) && enOtro > bajo + 1e-7 * escala
                        && enOtro < alto - 1e-7 * escala) {
                    return Optional.empty();
                }
            }
        }
        if (definidos < MUESTRAS * 0.9 || mayorSeparacion < 1e-9) {
            return Optional.empty();
        }

        // Tampoco una recta del enunciado puede cortarla por dentro.
        for (Corte corte : espacio.cortes()) {
            if (!corte.implicito() && corte.valor() > a + TOLERANCIA && corte.valor() < b - TOLERANCIA) {
                return Optional.empty();
            }
        }

        // ---------- Que fronteras del enunciado usa ----------
        Set<String> usados = new LinkedHashSet<>();
        usados.add(f.id());
        usados.add(g.id());
        for (Corte corte : espacio.cortes()) {
            if (Math.abs(corte.valor() - a) < TOLERANCIA || Math.abs(corte.valor() - b) < TOLERANCIA) {
                if (!corte.id().equals("intervalo") || opcion.tipo().equals("intervalo")) {
                    usados.add(corte.id());
                }
            }
        }
        int cobertura = 0;
        for (String id : usados) {
            if (explicitos.contains(id)) {
                cobertura++;
            }
        }

        Set<String> curvasDespejadas = new LinkedHashSet<>();
        if (f.requirioDespeje(v)) {
            curvasDespejadas.add(f.id());
        }
        if (g.requirioDespeje(v)) {
            curvasDespejadas.add(g.id());
        }

        boolean polinomico = ConversorPolinomio.esPolinomio(f.funcion().raiz())
                && ConversorPolinomio.esPolinomio(g.funcion().raiz());

        boolean fEsSuperior = fArriba >= gArriba;
        return Optional.of(new Candidato(opcion, v, cobertura, curvasDespejadas.size(), polinomico,
                f.signo() + g.signo(), fEsSuperior ? f : g, fEsSuperior ? g : f,
                fArriba > 0 && gArriba > 0, usados));
    }

    /** Dentro de una variable: mas fronteras usadas, intervalo mas firme, menos despejes. */
    private static final Comparator<Candidato> COMPARADOR_EN_UNA_VARIABLE = Comparator
            .comparingInt((Candidato c) -> -c.cobertura())
            .thenComparingDouble(c -> c.opcion().rango())
            .thenComparingInt(Candidato::despejes)
            .thenComparingInt(c -> c.polinomico() ? 0 : 1)
            .thenComparingInt(c -> -c.signos())
            .thenComparingDouble(c -> c.opcion().a());

    /**
     * Entre las dos variables: la que usa mas fronteras; a igualdad, la que exige menos
     * despejes, que es la que respeta como estan escritas las curvas; despues el intervalo
     * mas firme; y por ultimo x, que es como se plantea por defecto en el curso.
     */
    private static final Comparator<Candidato> COMPARADOR_ENTRE_VARIABLES = Comparator
            .comparingInt((Candidato c) -> -c.cobertura())
            .thenComparingInt(Candidato::despejes)
            .thenComparingDouble(c -> c.opcion().rango())
            .thenComparingInt(c -> c.variable().equals("x") ? 0 : 1)
            .thenComparingInt(c -> c.polinomico() ? 0 : 1);

    // ==================================================================
    // EL PLAN Y SU EXPLICACION
    // ==================================================================

    private static Plan construirPlan(Candidato elegido, List<Candidato> mismaVariable,
                                      Candidato enLaOtraVariable, Entrada entrada, int total,
                                      boolean respetaForzada) {
        Opcion opcion = elegido.opcion();
        String v = elegido.variable();
        String w = v.equals("x") ? "y" : "x";
        Borde f = opcion.f();
        Borde g = opcion.g();
        Borde primero = f.orden() <= g.orden() ? f : g;
        Borde segundo = primero == f ? g : f;

        double a = opcion.a();
        double b = opcion.b();
        ResolutorEcuaciones.Solucion cruces = opcion.cruces();
        String textoA = textoDeLimite(a, cruces, opcion.cortes());
        String textoB = textoDeLimite(b, cruces, opcion.cortes());

        List<String> ambiguedades = new ArrayList<>();
        buscarAmbiguedades(elegido, mismaVariable, ambiguedades);

        // ---------- Origen ----------
        OrigenDato origen = OrigenDato.EXPLICITO;
        if (!opcion.tipo().equals("intervalo") && !opcion.tipo().equals("rectas")) {
            origen = OrigenDato.DERIVADO;
        }
        origen = masDebil(origen, f.origen());
        origen = masDebil(origen, g.origen());
        for (Corte corte : opcion.cortes()) {
            origen = masDebil(origen, corte.origen());
        }
        if (opcion.recortado()) {
            origen = masDebil(origen, OrigenDato.DERIVADO);
        }

        // ---------- Puntos ----------
        List<Punto> puntos = new ArrayList<>();
        for (var raiz : cruces.raices()) {
            double valor = raiz.valor();
            if (valor < a - TOLERANCIA || valor > b + TOLERANCIA) {
                continue;
            }
            double otro = f.en(valor);
            String textoV = raiz.exacta() ? raiz.texto() : Redondeo.texto(valor);
            String textoW = Redondeo.texto(otro);
            puntos.add(v.equals("x")
                    ? new Punto(valor, otro, "(" + textoV + ", " + textoW + ")")
                    : new Punto(otro, valor, "(" + textoW + ", " + textoV + ")"));
        }

        String explicacionRegion = explicarRegion(elegido, entrada, enLaOtraVariable, respetaForzada);
        String explicacionLimites = explicarLimites(elegido, entrada, textoA, textoB, puntos);

        return new Plan(v, a, b, textoA, textoB, primero, segundo, elegido.superior(),
                elegido.inferior(), origen, explicacionRegion, explicacionLimites, ambiguedades,
                puntos, respetaForzada, elegido.cobertura() >= total, elegido.seCruzan());
    }

    /** Anota las otras lecturas igual de buenas que la elegida. */
    private static void buscarAmbiguedades(Candidato elegido, List<Candidato> candidatos,
                                           List<String> ambiguedades) {
        Opcion opcion = elegido.opcion();
        String v = elegido.variable();
        for (Candidato otro : candidatos) {
            if (otro == elegido || otro.cobertura() != elegido.cobertura()
                    || otro.opcion().rango() != opcion.rango()) {
                continue;
            }
            boolean otroIntervalo = Math.abs(otro.opcion().a() - opcion.a()) > TOLERANCIA
                    || Math.abs(otro.opcion().b() - opcion.b()) > TOLERANCIA;
            boolean mismosBordes = otro.usados().equals(elegido.usados());

            if (otroIntervalo && mismosBordes) {
                String texto = "Las mismas fronteras encierran tambien la region que va de "
                        + v + " = " + Redondeo.texto(otro.opcion().a()) + " a " + v + " = "
                        + Redondeo.texto(otro.opcion().b()) + ". Se tomo la que va de "
                        + Redondeo.texto(opcion.a()) + " a " + Redondeo.texto(opcion.b())
                        + "; si el ejercicio se refiere a la otra, corrija los limites.";
                if (!ambiguedades.contains(texto)) {
                    ambiguedades.add(texto);
                }
            } else if (!otroIntervalo && mismosBordes
                    && otro.opcion().f().curva() != null
                    && (otro.opcion().f() != opcion.f() || otro.opcion().g() != opcion.g())) {
                String texto = "La curva tiene dos ramas y las dos cierran una region con las "
                        + "mismas fronteras. Se tomo " + nombreDeRamas(elegido) + "; con la otra "
                        + "rama la region es su reflejo, y si el eje de giro es el eje de simetria "
                        + "el volumen sale igual.";
                if (!ambiguedades.contains(texto)) {
                    ambiguedades.add(texto);
                }
            }
        }
    }

    private static String nombreDeRamas(Candidato candidato) {
        List<String> nombres = new ArrayList<>();
        for (Borde borde : List.of(candidato.opcion().f(), candidato.opcion().g())) {
            if (borde.rama() != null && !borde.rama().descripcion().isBlank()) {
                nombres.add("la " + borde.rama().descripcion() + ", " + borde.ecuacion());
            }
        }
        return nombres.isEmpty() ? "una de ellas" : String.join(" y ", nombres);
    }

    /** El limite escrito de forma exacta: una raiz, una recta o el numero. */
    private static String textoDeLimite(double valor, ResolutorEcuaciones.Solucion cruces,
                                        List<Corte> cortes) {
        for (Corte corte : cortes) {
            if (Math.abs(corte.valor() - valor) < TOLERANCIA) {
                return Redondeo.texto(valor);
            }
        }
        return cruces.textoDe(valor);
    }

    private static OrigenDato masDebil(OrigenDato uno, OrigenDato otro) {
        if (uno == OrigenDato.INTERPRETADO || otro == OrigenDato.INTERPRETADO) {
            return OrigenDato.INTERPRETADO;
        }
        if (uno == OrigenDato.DERIVADO || otro == OrigenDato.DERIVADO) {
            return OrigenDato.DERIVADO;
        }
        return OrigenDato.EXPLICITO;
    }

    // ------------------------------------------------------------------
    // EXPLICACION DE LA REGION
    // ------------------------------------------------------------------

    /**
     * Explica como se armo la region: despejes, ramas, bordes implicitos y variable.
     */
    private static String explicarRegion(Candidato elegido, Entrada entrada, Candidato otraVariable,
                                         boolean respetaForzada) {
        String v = elegido.variable();
        String w = v.equals("x") ? "y" : "x";
        StringBuilder texto = new StringBuilder();
        Opcion opcion = elegido.opcion();
        // Solo vale la pena un paso de planteamiento si hubo algo que hacer: despejar, elegir
        // una rama, integrar respecto de y, o cerrar la region con un borde no escrito.
        boolean hayQueExplicar = v.equals("y") || entrada.variableForzada() != null;

        // ---------- Variable de integracion ----------
        if (entrada.variableForzada() != null && respetaForzada) {
            texto.append(entrada.motivoForzada()).append("\n\n");
        } else if (entrada.variableForzada() != null) {
            texto.append("El metodo pedido obligaria a integrar respecto de ")
                 .append(entrada.variableForzada()).append(", pero descrita asi la region no ")
                 .append("queda entre los mismos dos bordes en todo su recorrido. Se describe ")
                 .append("respecto de ").append(v).append(".\n\n");
        } else if (otraVariable == null || otraVariable.cobertura() < elegido.cobertura()) {
            if (v.equals("y")) {
                texto.append("Con rebanadas verticales (respecto de x) la region no queda entre ")
                     .append("los mismos dos bordes en todo su ancho: habria que partir la ")
                     .append("integral. Con rebanadas horizontales, respecto de y, cada rebanada ")
                     .append("va siempre de un mismo borde a otro, asi que se integra respecto de y.")
                     .append("\n\n");
            } else {
                texto.append("La region se describe con rebanadas verticales: se integra ")
                     .append("respecto de x.\n\n");
            }
        } else {
            texto.append(v.equals("x")
                    ? "La region se describe con rebanadas verticales: se integra respecto de x."
                    : "Las curvas se describen mejor como funciones de y (x = g(y)), asi que la "
                      + "region se describe con rebanadas horizontales: se integra respecto de y.")
                 .append("\n\n");
        }

        // ---------- Despejes ----------
        Set<Curva> explicadas = new LinkedHashSet<>();
        for (Borde borde : List.of(opcion.f(), opcion.g())) {
            Curva curva = borde.curva();
            if (curva == null || !explicadas.add(curva)) {
                continue;
            }
            Despejador.Despeje despeje = curva.despejeEn(v);
            if (despeje.yaEstabaDespejada()) {
                texto.append("La curva ").append(curva.ecuacion()).append(" ya esta escrita como ")
                     .append(w).append(" en funcion de ").append(v).append(".\n");
                continue;
            }
            hayQueExplicar = true;
            texto.append("La curva ").append(curva.ecuacion()).append(" no esta escrita como ")
                 .append(w).append(" en funcion de ").append(v).append(". Despejando ")
                 .append(w).append(":\n");
            for (String paso : despeje.pasos()) {
                texto.append("   ").append(paso.trim()).append('\n');
            }
            if (despeje.ramas().size() > 1) {
                List<String> ramas = new ArrayList<>();
                for (Despejador.Rama rama : despeje.ramas()) {
                    ramas.add(w + " = " + rama.expresion() + " (" + rama.descripcion() + ")");
                }
                texto.append("Tiene dos ramas: ").append(String.join(" y ", ramas)).append(".\n");
                boolean usaLasDos = opcion.f().curva() == curva && opcion.g().curva() == curva;
                if (usaLasDos) {
                    texto.append("La region queda entre las dos ramas.\n");
                } else {
                    Borde usado = opcion.f().curva() == curva ? opcion.f() : opcion.g();
                    Borde otro = usado == opcion.f() ? opcion.g() : opcion.f();
                    texto.append("Se usa la ").append(usado.rama().descripcion()).append(", ")
                         .append(usado.ecuacion());
                    if (entrada.cuadrante() != Cuadrante.NINGUNO) {
                        texto.append(", porque la region esta en el ").append(entrada.cuadrante().texto())
                             .append(" (").append(entrada.cuadrante().desigualdades()).append(").\n");
                    } else {
                        texto.append(", que es la que encierra region con ").append(otro.ecuacion())
                             .append(".\n");
                    }
                }
            }
        }

        // ---------- Bordes implicitos ----------
        for (Borde borde : List.of(opcion.f(), opcion.g())) {
            if (borde.id().equals("suelo")) {
                hayQueExplicar = true;
                texto.append(entrada.razonSuelo()).append('\n');
            } else if (borde.id().equals("cuadrante")) {
                hayQueExplicar = true;
                texto.append("La region esta en el ").append(entrada.cuadrante().texto())
                     .append(", asi que la cierra el eje ").append(borde.ecuacion().startsWith("y") ? "X" : "Y")
                     .append(" (").append(borde.ecuacion()).append(").\n");
            }
        }

        // ---------- Cual va arriba ----------
        double medio = (opcion.a() + opcion.b()) / 2.0;
        Borde superior = elegido.superior();
        Borde inferior = elegido.inferior();
        String arriba = v.equals("x") ? "por encima" : "a la derecha";
        String abajo = v.equals("x") ? "por debajo" : "a la izquierda";
        if (elegido.seCruzan()) {
            texto.append("Los dos bordes se cruzan dentro del intervalo, asi que cambia cual va ")
                 .append(arriba).append("; el procedimiento lo tiene en cuenta tramo por tramo.\n");
        } else {
            texto.append("Probando un punto interior, ").append(v).append(" = ")
                 .append(Redondeo.texto(medio)).append(": ")
                 .append(superior.ecuacion()).append(" vale ").append(Redondeo.texto(superior.en(medio)))
                 .append(" y ").append(inferior.ecuacion()).append(" vale ")
                 .append(Redondeo.texto(inferior.en(medio))).append(". Entonces ")
                 .append(superior.ecuacion()).append(" queda ").append(arriba).append(" y ")
                 .append(inferior.ecuacion()).append(" ").append(abajo).append(".\n");
        }
        return hayQueExplicar ? texto.toString().trim() : "";
    }

    // ------------------------------------------------------------------
    // EXPLICACION DE LOS LIMITES
    // ------------------------------------------------------------------

    /**
     * Explica de donde salieron los limites, con la ecuacion resuelta paso a paso.
     */
    private static String explicarLimites(Candidato elegido, Entrada entrada, String textoA,
                                          String textoB, List<Punto> puntos) {
        Opcion opcion = elegido.opcion();
        String v = elegido.variable();
        StringBuilder texto = new StringBuilder();

        if (opcion.tipo().equals("intervalo")) {
            texto.append("Para los limites de integracion, ")
                 .append(entrada.intervalo().map(ExtractorIntervalo.Intervalo::origen).orElse(""))
                 .append(".");
            if (entrada.intervalo().isPresent() && !entrada.intervalo().get().variable().equals(v)) {
                texto.append(" ");
            }
            return texto.toString();
        }

        boolean usaCruces = !opcion.tipo().equals("rectas") && !opcion.tipo().equals("recta+eje");
        if (usaCruces) {
            Borde f = opcion.f();
            Borde g = opcion.g();
            boolean conElSuelo = g.esRecta() && g.implicito() || f.esRecta() && f.implicito();
            if (conElSuelo) {
                texto.append("Se buscan los cortes de la curva con el eje, igualando a ")
                     .append(Redondeo.texto(g.esRecta() ? g.en(0) : f.en(0))).append(":\n\n");
            } else {
                texto.append("Se igualan las dos fronteras, f(").append(v).append(") = g(")
                     .append(v).append("), para hallar donde se cortan:\n\n");
            }
            for (String paso : opcion.cruces().pasos()) {
                texto.append("   ").append(paso).append('\n');
            }
            if (!puntos.isEmpty()) {
                List<String> textos = new ArrayList<>();
                for (Punto punto : puntos) {
                    textos.add(punto.texto());
                }
                texto.append("\nPuntos de corte: ").append(String.join(",  ", textos)).append('\n');
            }
        }

        for (Corte corte : opcion.cortes()) {
            texto.append('\n').append(describirCorte(corte, opcion, entrada, v));
        }
        if (opcion.recortado()) {
            texto.append("\nLa region esta en el ").append(entrada.cuadrante().texto())
                 .append(" (").append(entrada.cuadrante().desigualdades())
                 .append("), asi que se queda la parte con ").append(v)
                 .append(entrada.cuadrante().rangoX()[0] == 0 && v.equals("x")
                         || entrada.cuadrante().rangoY()[0] == 0 && v.equals("y") ? " >= 0" : " <= 0")
                 .append(".");
        }

        texto.append("\n\nPor lo tanto el intervalo es [").append(textoA).append(", ")
             .append(textoB).append("]");
        if (!textoA.equals(Redondeo.texto(opcion.a())) || !textoB.equals(Redondeo.texto(opcion.b()))) {
            texto.append(", es decir [").append(Redondeo.texto(opcion.a())).append(", ")
                 .append(Redondeo.texto(opcion.b())).append("]");
        }
        texto.append(".");
        return texto.toString().trim();
    }

    private static String describirCorte(Corte corte, Opcion opcion, Entrada entrada, String v) {
        boolean esInferior = Math.abs(corte.valor() - opcion.a()) < TOLERANCIA;
        String lado = v.equals("x")
                ? (esInferior ? "por la izquierda" : "por la derecha")
                : (esInferior ? "por abajo" : "por arriba");
        return switch (corte.id()) {
            case "eje" -> "El eje de giro, " + corte.ecuacion() + ", cierra la region " + lado
                    + ": el solido nace en el eje y llega hasta la otra recta.";
            case "suelo" -> "El eje X (y = 0) cierra la region " + lado + ".";
            case "cuadrante" -> "El eje " + (corte.ecuacion().startsWith("x") ? "Y" : "X") + " ("
                    + corte.ecuacion() + ") cierra la region " + lado + ", porque la region esta en el "
                    + entrada.cuadrante().texto() + ".";
            default -> {
                String lectura = "";
                for (int i = 0; i < entrada.fronteras().size(); i++) {
                    if (("r" + i).equals(corte.id())) {
                        lectura = entrada.fronteras().get(i).origen() == OrigenDato.INTERPRETADO
                                ? " " + entrada.fronteras().get(i).lectura() : "";
                    }
                }
                yield "La recta " + corte.ecuacion() + " cierra la region " + lado + "." + lectura;
            }
        };
    }
}
