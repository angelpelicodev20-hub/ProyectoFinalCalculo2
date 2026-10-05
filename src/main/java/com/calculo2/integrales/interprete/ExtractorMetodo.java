package com.calculo2.integrales.interprete;

import com.calculo2.integrales.model.TipoSolido;

import java.util.Optional;

/**
 * Reconoce el metodo que el enunciado pide aplicar.
 *
 * <p>Muchos ejercicios del curso no preguntan solo por el volumen: piden resolverlo por
 * un metodo concreto, porque lo que se esta practicando es ese metodo. "Aplicando el
 * metodo de capas cilindricas" es parte del enunciado, no una sugerencia, y cambiarlo en
 * silencio por el que la geometria haria mas comodo dejaria al estudiante sin lo que fue
 * a buscar.</p>
 *
 * <p>Lo que se lee aqui es el metodo <b>pedido</b>. El metodo que la geometria
 * <b>sugiere</b> lo calcula {@link SelectorMetodoSolido} midiendo la region, y se guarda
 * aparte: cuando los dos no coinciden hay algo que decirle al estudiante, y para poder
 * decirselo hay que conservar los dos.</p>
 */
public final class ExtractorMetodo {

    private ExtractorMetodo() {
    }

    /**
     * Un metodo pedido por el enunciado.
     *
     * @param tipo    el metodo
     * @param lectura la frase del enunciado de la que se dedujo
     */
    public record MetodoPedido(TipoSolido tipo, String lectura) {
    }

    /**
     * Busca el metodo que pide el enunciado.
     *
     * <p>Las capas se comprueban primero porque sus nombres son los mas largos y los mas
     * inconfundibles. Anillos y arandelas van despues, y los discos al final: "disco" es
     * la palabra que mas facilmente aparece de pasada al describir una rebanada, asi que
     * conviene que sea la ultima en reclamar el enunciado.</p>
     *
     * @param textoNormalizado enunciado en minusculas y sin tildes
     * @return el metodo pedido, o vacio si el enunciado no nombra ninguno
     */
    public static Optional<MetodoPedido> buscar(String textoNormalizado) {
        if (textoNormalizado == null || textoNormalizado.isBlank()) {
            return Optional.empty();
        }

        if (ExtractorDatos.contieneAlguna(textoNormalizado,
                "capas cilindricas", "capa cilindrica", "cascarones cilindricos",
                "cascaron cilindrico", "cortezas cilindricas", "capas", "cascarones")) {

            return Optional.of(new MetodoPedido(TipoSolido.CAPAS,
                    "El enunciado pide resolverlo por capas cilindricas."));
        }

        if (ExtractorDatos.contieneAlguna(textoNormalizado,
                "anillos", "anillo", "arandelas", "arandela")) {

            return Optional.of(new MetodoPedido(TipoSolido.ARANDELAS,
                    "El enunciado pide resolverlo por anillos o arandelas, que son dos nombres "
                            + "del mismo metodo: la rebanada perpendicular al eje sale con "
                            + "agujero."));
        }

        if (ExtractorDatos.contieneAlguna(textoNormalizado, "discos", "disco")) {
            return Optional.of(new MetodoPedido(TipoSolido.DISCOS,
                    "El enunciado pide resolverlo por discos."));
        }

        return Optional.empty();
    }
}
