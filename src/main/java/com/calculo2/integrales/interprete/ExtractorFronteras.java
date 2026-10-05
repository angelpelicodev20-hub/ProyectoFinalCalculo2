package com.calculo2.integrales.interprete;

import com.calculo2.integrales.math.util.Redondeo;
import com.calculo2.integrales.model.Frontera;
import com.calculo2.integrales.model.OrigenDato;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encuentra las rectas que cierran la region, sin confundirlas con el eje de giro.
 *
 * <p>Una region del plano no queda definida solo por las curvas: hace falta saber por
 * donde se corta. Esos cortes son rectas, y el enunciado las nombra de varias maneras que
 * significan lo mismo:</p>
 *
 * <pre>
 *   "y la ordenada 2"                         ->  la recta x = 2
 *   "la ordenada correspondiente a x = 2"     ->  la recta x = 2
 *   "y la recta x = 3", "las rectas y = 1"    ->  esas rectas
 *   "y el eje X"                              ->  la recta y = 0
 *   "y los ejes coordenados"                  ->  las rectas x = 0 e y = 0
 * </pre>
 *
 * <p>La regla que gobierna todo este archivo es una sola: <b>una recta introducida por
 * una expresion de giro nombra el eje de revolucion, no una frontera</b>. En "alrededor
 * del eje X" el eje X dice alrededor de que rota la region; en "acotada por la grafica de
 * f y el eje X" el mismo eje X dice por donde se cierra la region. Por eso las rectas se
 * buscan primero todas, con su posicion, y despues se quitan las que caen dentro de la
 * expresion del eje que encontro {@link ExtractorEje}.</p>
 */
public final class ExtractorFronteras {

    private ExtractorFronteras() {
    }

    /**
     * "la ordenada 2", "la ordenada correspondiente a x = 2", "las ordenadas en 1 y 3".
     *
     * <p>La ordenada de un punto es su altura, y "la ordenada 2" nombra el segmento
     * vertical que va del eje X hasta la curva en el punto de abscisa 2. Ese segmento
     * cierra la region por un costado, asi que la frontera es la recta {@code x = 2}.</p>
     */
    private static final Pattern ORDENADA = Pattern.compile(
            "\\bordenadas?\\s+(?:en\\s+|de\\s+|correspondientes?\\s+(?:a|al)\\s+)?(?:(x)\\s*=\\s*)?"
                    + "(-?\\d+(?:[.,]\\d+)?)");

    /** "la abscisa 3": el segmento horizontal a la altura 3, o sea la recta y = 3. */
    private static final Pattern ABSCISA = Pattern.compile(
            "\\babscisas?\\s+(?:en\\s+|de\\s+|correspondientes?\\s+(?:a|al)\\s+)?(?:(y)\\s*=\\s*)?"
                    + "(-?\\d+(?:[.,]\\d+)?)");

    /** El eje X nombrado como recta. */
    private static final Pattern EJE_X = Pattern.compile(
            "\\beje\\s+(?:de\\s+las\\s+)?(?:x|abscisas|horizontal|ox)\\b");

    /** El eje Y nombrado como recta. */
    private static final Pattern EJE_Y = Pattern.compile(
            "\\beje\\s+(?:de\\s+las\\s+)?(?:y|ordenadas|vertical|oy)\\b");

    /** "los ejes coordenados", "los ejes de coordenadas", "los ejes". */
    private static final Pattern LOS_EJES = Pattern.compile(
            "\\b(?:los\\s+)?ejes\\s+(?:coordenados|de\\s+coordenadas|cartesianos)\\b|\\blos\\s+ejes\\b");

    /**
     * Busca todas las rectas nombradas, con su posicion, sin decidir aun cual es el eje.
     *
     * @param normalizado enunciado en minusculas y sin tildes, ya corregido
     * @param ecuaciones  las ecuaciones encontradas en el texto
     * @param intervalo   el intervalo escrito, cuyas rectas no se repiten como fronteras
     * @return las rectas nombradas, en orden de aparicion
     */
    public static List<ExtractorEje.RectaNombrada> buscarTodas(
            String normalizado, List<ExtractorEcuaciones.EcuacionDetectada> ecuaciones,
            Optional<ExtractorIntervalo.Intervalo> intervalo) {

        List<ExtractorEje.RectaNombrada> rectas = new ArrayList<>();
        if (normalizado == null || normalizado.isBlank()) {
            return rectas;
        }

        // ---------- Rectas escritas como ecuacion ----------
        for (ExtractorEcuaciones.EcuacionDetectada ecuacion : ecuaciones) {
            if (!ecuacion.nombre().isEmpty()) {
                continue;
            }
            if (intervalo.isPresent() && ecuacion.inicio() >= intervalo.get().inicio()
                    && ecuacion.inicio() < intervalo.get().fin()) {
                continue;
            }
            Optional<Frontera> recta = comoRecta(ecuacion);
            recta.ifPresent(frontera -> rectas.add(
                    new ExtractorEje.RectaNombrada(frontera, ecuacion.inicio())));
        }

        // ---------- "La ordenada 2" ----------
        Matcher ordenada = ORDENADA.matcher(normalizado);
        while (ordenada.find()) {
            double k = comoNumero(ordenada.group(2));
            boolean escrita = ordenada.group(1) != null;
            rectas.add(new ExtractorEje.RectaNombrada(Frontera.vertical(k,
                    escrita ? OrigenDato.EXPLICITO : OrigenDato.INTERPRETADO,
                    escrita
                        ? "El enunciado nombra \"" + ordenada.group().trim() + "\": la ordenada en "
                          + "x = " + Redondeo.texto(k) + " es el segmento vertical sobre esa recta, "
                          + "y cierra la region por un costado."
                        : "El enunciado nombra \"" + ordenada.group().trim() + "\". La ordenada es la "
                          + "altura de un punto, y nombrar la ordenada en un valor senala el "
                          + "segmento vertical que va desde el eje X hasta la curva en ese "
                          + "punto. Ese segmento cierra la region, asi que se lee como la "
                          + "recta x = " + Redondeo.texto(k) + ". Es una interpretacion del "
                          + "lenguaje, no un dato calculado: si no es lo que dice el "
                          + "ejercicio, corrija el campo antes de resolver."),
                    ordenada.start()));
        }

        Matcher abscisa = ABSCISA.matcher(normalizado);
        while (abscisa.find()) {
            double k = comoNumero(abscisa.group(2));
            rectas.add(new ExtractorEje.RectaNombrada(Frontera.horizontal(k,
                    abscisa.group(1) != null ? OrigenDato.EXPLICITO : OrigenDato.INTERPRETADO,
                    "El enunciado nombra \"" + abscisa.group().trim() + "\". La abscisa senala el "
                            + "segmento horizontal a esa altura, asi que se lee como la recta y = "
                            + Redondeo.texto(k) + "."),
                    abscisa.start()));
        }

        // ---------- Los ejes coordenados como frontera ----------
        Matcher losEjes = LOS_EJES.matcher(normalizado);
        while (losEjes.find()) {
            rectas.add(new ExtractorEje.RectaNombrada(Frontera.horizontal(0, OrigenDato.EXPLICITO,
                    "El enunciado nombra los ejes coordenados como borde de la region: uno de "
                            + "ellos es el eje X, la recta y = 0."), losEjes.start()));
            rectas.add(new ExtractorEje.RectaNombrada(Frontera.vertical(0, OrigenDato.EXPLICITO,
                    "El enunciado nombra los ejes coordenados como borde de la region: el otro "
                            + "es el eje Y, la recta x = 0."), losEjes.start()));
        }

        Matcher ejeX = EJE_X.matcher(normalizado);
        while (ejeX.find()) {
            rectas.add(new ExtractorEje.RectaNombrada(Frontera.horizontal(0, OrigenDato.EXPLICITO,
                    "El enunciado nombra el eje X como limite de la region: es la recta y = 0."),
                    ejeX.start()));
        }

        Matcher ejeY = EJE_Y.matcher(normalizado);
        while (ejeY.find()) {
            rectas.add(new ExtractorEje.RectaNombrada(Frontera.vertical(0, OrigenDato.EXPLICITO,
                    "El enunciado nombra el eje Y como limite de la region: es la recta x = 0."),
                    ejeY.start()));
        }

        rectas.sort((uno, otro) -> Integer.compare(uno.posicion(), otro.posicion()));
        return rectas;
    }

    /**
     * Se queda con las fronteras: quita las que son el eje de giro y las repetidas.
     *
     * @param rectas todas las rectas nombradas
     * @param eje    el eje encontrado, si lo hay
     * @return las fronteras, en orden de aparicion
     */
    public static List<ExtractorEje.RectaNombrada> depurar(List<ExtractorEje.RectaNombrada> rectas,
                                                           Optional<ExtractorEje.EjeDetectado> eje) {
        List<ExtractorEje.RectaNombrada> fronteras = new ArrayList<>();
        for (ExtractorEje.RectaNombrada recta : rectas) {
            if (eje.isPresent() && eje.get().contiene(recta.posicion())) {
                continue;
            }
            boolean repetida = false;
            for (int i = 0; i < fronteras.size(); i++) {
                Frontera existente = fronteras.get(i).frontera();
                if (existente.orientacion() == recta.frontera().orientacion()
                        && Math.abs(existente.valor() - recta.frontera().valor()) < 1e-12) {
                    repetida = true;
                    // Si una lectura la escribe con todas las letras, esa es la que cuenta:
                    // "la ordenada correspondiente a x = 2" no es una interpretacion.
                    if (existente.origen() == OrigenDato.INTERPRETADO
                            && recta.frontera().origen() == OrigenDato.EXPLICITO) {
                        fronteras.set(i, new ExtractorEje.RectaNombrada(
                                recta.frontera(), fronteras.get(i).posicion()));
                    }
                    break;
                }
            }
            if (!repetida) {
                fronteras.add(recta);
            }
        }
        return fronteras;
    }

    /** Lee una ecuacion como recta vertical u horizontal, si lo es. */
    private static Optional<Frontera> comoRecta(ExtractorEcuaciones.EcuacionDetectada ecuacion) {
        String izquierda = ecuacion.izquierda().trim().toLowerCase();
        String derecha = ecuacion.derecha().trim();
        boolean esX = izquierda.equals("x");
        boolean esY = izquierda.equals("y");
        if (!esX && !esY) {
            return Optional.empty();
        }
        Optional<Double> valor = ExtractorDatos.comoNumero(derecha);
        if (valor.isEmpty()) {
            return Optional.empty();
        }
        double k = valor.get();
        return Optional.of(esX
                ? Frontera.vertical(k, OrigenDato.EXPLICITO,
                        "El enunciado escribe la recta x = " + Redondeo.texto(k)
                                + ", que es vertical y cierra la region por un costado.")
                : Frontera.horizontal(k, OrigenDato.EXPLICITO,
                        "El enunciado escribe la recta y = " + Redondeo.texto(k)
                                + ", que es horizontal y cierra la region por arriba o por abajo."));
    }

    /** Lee el numero admitiendo la coma decimal. */
    private static double comoNumero(String texto) {
        return Double.parseDouble(texto.replace(',', '.'));
    }
}
