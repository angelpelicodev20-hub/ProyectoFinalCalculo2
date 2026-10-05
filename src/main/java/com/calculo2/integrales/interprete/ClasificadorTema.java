package com.calculo2.integrales.interprete;

import com.calculo2.integrales.model.TemaProblema;

import java.util.ArrayList;
import java.util.List;

/**
 * Decide de que tema trata un enunciado.
 *
 * <p>La tentacion es buscar la palabra "volumen" y dar el tema por resuelto. Eso falla en
 * cuanto el enunciado esta redactado de otra forma, y sobre todo no distingue los dos
 * temas de area, que pueden usar exactamente el mismo vocabulario.</p>
 *
 * <p>Aqui se combinan tres clases de indicio:</p>
 *
 * <ol>
 *   <li><b>Dimension de lo que se pide.</b> Un volumen es una magnitud de tres
 *       dimensiones, y una region del plano solo llega a tener volumen si gira. Pedir un
 *       volumen implica, por si solo, que hay una revolucion de por medio.</li>
 *   <li><b>Presencia de un eje de giro.</b> Nombrar una recta como eje de revolucion no
 *       tiene sentido en un problema de area: ese dato solo sirve si algo rota.</li>
 *   <li><b>Cuantas fronteras tiene la region.</b> Este es el indicio que separa los dos
 *       temas de area, y es puramente estructural: con dos funciones la region queda
 *       encerrada entre ambas; con una sola, la segunda frontera es el eje horizontal.</li>
 * </ol>
 *
 * <p>El tercero funciona aunque el enunciado no diga "entre" ni "bajo", que es
 * precisamente lo que se buscaba.</p>
 */
public final class ClasificadorTema {

    private ClasificadorTema() {
    }

    /**
     * El resultado de clasificar, con el razonamiento que lo respalda.
     *
     * @param tema       el tema identificado
     * @param evidencias en que se baso la conclusion
     * @param seguro     true si los indicios coinciden entre si
     */
    public record Clasificacion(TemaProblema tema, List<String> evidencias, boolean seguro) {
    }

    /**
     * Clasifica el enunciado.
     *
     * @param textoNormalizado  enunciado en minusculas y sin tildes
     * @param cuantasFunciones  cuantas funciones se encontraron en el texto
     * @param hayEjeDeRevolucion true si se detecto un eje de giro
     * @return el tema con su justificacion
     */
    public static Clasificacion clasificar(String textoNormalizado, int cuantasFunciones,
                                           boolean hayEjeDeRevolucion) {
        List<String> evidencias = new ArrayList<>();

        boolean pideVolumen = ExtractorDatos.contieneAlguna(textoNormalizado,
                "volumen", "solido", "sólido", "cuerpo de revolucion");

        boolean hayGiro = ExtractorDatos.contieneAlguna(textoNormalizado,
                "girar", "gira", "giro", "rotar", "rota", "rotacion", "revolucion");

        boolean pideArea = ExtractorDatos.contieneAlguna(textoNormalizado,
                "area", "region", "superficie", "encerrada", "limitada");

        // ---------- Volumen ----------
        if (pideVolumen || hayGiro || (hayEjeDeRevolucion && !pideArea)) {

            if (pideVolumen) {
                evidencias.add("Se pide un volumen, que es una magnitud de tres dimensiones. "
                        + "Una region del plano solo genera volumen al girar alrededor de una "
                        + "recta, asi que el problema es de solidos de revolucion.");
            }
            if (hayGiro) {
                evidencias.add("El enunciado describe un giro o una rotacion de la region.");
            }
            if (hayEjeDeRevolucion) {
                evidencias.add("Se nombra una recta como eje de giro; ese dato solo tiene "
                        + "sentido si la region rota.");
            }

            // Que el tema sea claro no significa que esten todos los datos: si no hay eje,
            // el problema es de volumen igualmente, solo que incompleto.
            boolean seguro = pideVolumen || hayGiro;
            return new Clasificacion(TemaProblema.VOLUMEN_SOLIDO, evidencias, seguro);
        }

        // ---------- Areas ----------
        if (cuantasFunciones >= 2) {
            evidencias.add("El enunciado define dos funciones. La region queda encerrada "
                    + "entre ambas curvas, asi que el area se obtiene integrando su "
                    + "diferencia.");
            if (pideArea) {
                evidencias.add("Se pide un area, que es una magnitud de dos dimensiones: "
                        + "no hay revolucion de por medio.");
            }
            return new Clasificacion(TemaProblema.AREA_ENTRE_CURVAS, evidencias, true);
        }

        if (cuantasFunciones == 1) {
            evidencias.add("El enunciado define una sola funcion. La segunda frontera de la "
                    + "region es el eje horizontal, que es el caso del area bajo la curva.");
            if (pideArea) {
                evidencias.add("Se pide un area, que es una magnitud de dos dimensiones: "
                        + "no hay revolucion de por medio.");
            }
            return new Clasificacion(TemaProblema.AREA_BAJO_CURVA, evidencias, true);
        }

        // ---------- Sin datos suficientes ----------
        evidencias.add("No se encontro ninguna funcion en el enunciado, asi que no se puede "
                + "saber de que tema se trata.");
        return new Clasificacion(TemaProblema.DESCONOCIDO, evidencias, false);
    }
}
