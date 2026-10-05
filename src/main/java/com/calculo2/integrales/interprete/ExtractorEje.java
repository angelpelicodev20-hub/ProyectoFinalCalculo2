package com.calculo2.integrales.interprete;

import com.calculo2.integrales.model.EjeRotacion;
import com.calculo2.integrales.model.Frontera;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encuentra en el enunciado la recta alrededor de la cual gira la region.
 *
 * <p>El dato es delicado porque la misma escritura significa cosas distintas segun el
 * lugar. En "gira alrededor de x = 2" la expresion {@code x = 2} nombra el eje de
 * revolucion; en "la region limitada por x = 0 y x = 4" nombra los bordes de la region, y
 * confundirlos cambiaria por completo el solido.</p>
 *
 * <p>Lo que distingue un caso del otro es lo que va delante: un verbo de giro o una
 * preposicion que lo anuncia ("alrededor de", "en torno a", "con respecto a", "sobre",
 * "al rotar", "haciendo girar"). Solo se acepta como eje lo que sigue inmediatamente a una
 * de esas expresiones, saltando articulos ("el", "la", "la recta"). Asi
 * "al rotar y = 0", "haciendo rotar el eje y" y "en torno de la recta x = 2" se leen como
 * eje, y "la rotacion del area comprendida entre..." no, porque detras de "rotacion" no
 * viene una recta sino la descripcion de la region.</p>
 *
 * <p>Tambien se resuelve la referencia "con respecto a esa recta": el eje es entonces la
 * ultima recta que nombro el enunciado. Esa recta sigue siendo ademas una frontera de la
 * region; tiene los dos papeles a la vez.</p>
 */
public final class ExtractorEje {

    private ExtractorEje() {
    }

    /** Palabras que anuncian un eje de revolucion. */
    private static final Pattern ANUNCIO = Pattern.compile(
            "\\b(alrededor|en\\s+torno|con\\s+respecto|respecto|sobre|rotar|rota|rotando|"
                    + "rotacion|girar|gira|girando|giro|revolucion)\\b");

    /** Articulos y preposiciones que pueden ir entre el anuncio y el eje. */
    private static final Pattern RELLENO = Pattern.compile(
            "^[\\s,:]*(?:(?:de|del|a|al|la|el|los|las|lo|en|hacer|haciendo|que|se|sobre)\\s+)*");

    /** "la recta x = 3", "y = -1". */
    private static final Pattern RECTA = Pattern.compile(
            "^(?:(?:la\\s+)?(?:recta|linea|vertical|horizontal)\\s+(?:de\\s+ecuacion\\s+)?)?"
                    + "([xy])\\s*=\\s*(-?\\d+(?:[.,]\\d+)?)(?![\\w.^(])");

    /** "eje x", "eje de las y", "eje de las abscisas", "eje ox". */
    private static final Pattern EJE_COORDENADO = Pattern.compile(
            "^(?:el\\s+)?eje\\s+(?:de\\s+las\\s+)?(x|y|abscisas|ordenadas|horizontal|vertical|ox|oy)\\b");

    /** "esa recta", "dicha recta", "la misma recta", "esta ordenada". */
    private static final Pattern REFERENCIA = Pattern.compile(
            "^(?:esa|esta|dicha|aquella|la\\s+misma)\\s+(recta|linea|ordenada|abscisa|vertical|horizontal)\\b");

    /**
     * Un eje encontrado, junto con el motivo por el que se concluyo.
     *
     * @param eje           la recta de revolucion
     * @param razon         explicacion para mostrarsela al estudiante
     * @param inicio        donde empieza en el texto la expresion del eje
     * @param fin           donde termina
     * @param porReferencia true si el eje se nombro como "esa recta"
     */
    public record EjeDetectado(EjeRotacion eje, String razon, int inicio, int fin,
                               boolean porReferencia) {

        /** Indica si una posicion del texto cae dentro de la expresion del eje. */
        public boolean contiene(int posicion) {
            return !porReferencia && posicion >= inicio && posicion < fin;
        }
    }

    /**
     * Busca el eje de revolucion.
     *
     * @param textoNormalizado enunciado ya en minusculas y sin tildes
     * @return el eje, o vacio si el enunciado no lo dice
     */
    public static Optional<EjeDetectado> buscar(String textoNormalizado) {
        return buscar(textoNormalizado, List.of());
    }

    /**
     * Busca el eje de revolucion, resolviendo las referencias a rectas anteriores.
     *
     * @param textoNormalizado enunciado ya en minusculas y sin tildes
     * @param rectas           rectas nombradas en el enunciado, con su posicion, para
     *                         resolver "esa recta"
     * @return el eje, o vacio si el enunciado no lo dice
     */
    public static Optional<EjeDetectado> buscar(String textoNormalizado, List<RectaNombrada> rectas) {
        if (textoNormalizado == null || textoNormalizado.isBlank()) {
            return Optional.empty();
        }

        Matcher anuncio = ANUNCIO.matcher(textoNormalizado);
        while (anuncio.find()) {
            String resto = textoNormalizado.substring(anuncio.end());
            Matcher relleno = RELLENO.matcher(resto);
            int salto = relleno.find() ? relleno.end() : 0;
            String despues = resto.substring(salto);
            int base = anuncio.end() + salto;
            String verbo = anuncio.group(1).replaceAll("\\s+", " ");

            Matcher recta = RECTA.matcher(despues);
            if (recta.find()) {
                double k = Double.parseDouble(recta.group(2).replace(',', '.'));
                boolean horizontal = recta.group(1).equals("y");
                EjeRotacion eje = horizontal ? EjeRotacion.horizontal(k) : EjeRotacion.vertical(k);
                return Optional.of(new EjeDetectado(eje,
                        "el enunciado dice \"" + verbo + " ... " + recta.group().trim()
                                + "\": el giro es alrededor de la recta " + eje.ecuacion()
                                + ", que es " + (horizontal ? "horizontal" : "vertical"),
                        base + recta.start(1), base + recta.end(), false));
            }

            Matcher coordenado = EJE_COORDENADO.matcher(despues);
            if (coordenado.find()) {
                String cual = coordenado.group(1);
                boolean esX = cual.equals("x") || cual.equals("abscisas")
                        || cual.equals("horizontal") || cual.equals("ox");
                EjeRotacion eje = esX ? EjeRotacion.ejeX() : EjeRotacion.ejeY();
                return Optional.of(new EjeDetectado(eje,
                        "el enunciado dice \"" + verbo + " ... " + coordenado.group().trim()
                                + "\": el giro es alrededor del eje " + (esX ? "X" : "Y")
                                + ", que es la recta " + eje.ecuacion(),
                        base + coordenado.start(), base + coordenado.end(), false));
            }

            Matcher referencia = REFERENCIA.matcher(despues);
            if (referencia.find()) {
                Optional<RectaNombrada> anterior = ultimaAntes(rectas, anuncio.start(),
                        referencia.group(1));
                if (anterior.isPresent()) {
                    Frontera recta2 = anterior.get().frontera();
                    EjeRotacion eje = recta2.esVertical()
                            ? EjeRotacion.vertical(recta2.valor())
                            : EjeRotacion.horizontal(recta2.valor());
                    return Optional.of(new EjeDetectado(eje,
                            "el enunciado dice \"" + verbo + " ... " + referencia.group().trim()
                                    + "\", que se refiere a la recta nombrada antes, "
                                    + recta2.ecuacion() + ". Esa recta cierra la region y "
                                    + "ademas es el eje de giro",
                            base + referencia.start(), base + referencia.end(), true));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Una recta nombrada en el enunciado, con su posicion, para resolver referencias.
     *
     * @param frontera la recta
     * @param posicion donde aparece en el texto
     */
    public record RectaNombrada(Frontera frontera, int posicion) {
    }

    /** La ultima recta nombrada antes de una posicion, del tipo que pide la referencia. */
    private static Optional<RectaNombrada> ultimaAntes(List<RectaNombrada> rectas, int posicion,
                                                       String tipo) {
        RectaNombrada elegida = null;
        for (RectaNombrada recta : rectas) {
            if (recta.posicion() >= posicion) {
                continue;
            }
            boolean pideVertical = tipo.equals("ordenada") || tipo.equals("vertical");
            boolean pideHorizontal = tipo.equals("abscisa") || tipo.equals("horizontal");
            if (pideVertical && !recta.frontera().esVertical()) {
                continue;
            }
            if (pideHorizontal && recta.frontera().esVertical()) {
                continue;
            }
            if (elegida == null || recta.posicion() > elegida.posicion()) {
                elegida = recta;
            }
        }
        return Optional.ofNullable(elegida);
    }
}
