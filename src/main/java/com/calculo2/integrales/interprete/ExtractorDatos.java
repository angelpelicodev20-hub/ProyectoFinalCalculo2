package com.calculo2.integrales.interprete;

import com.calculo2.integrales.math.parser.EvaluadorExpresion;

import java.text.Normalizer;
import java.util.Optional;

/**
 * Utilidades de lectura que comparten las piezas del interprete.
 *
 * <p>La lectura de las ecuaciones esta en {@link ExtractorEcuaciones}, la del intervalo en
 * {@link ExtractorIntervalo} y la de las rectas en {@link ExtractorFronteras}. Aqui queda
 * lo que usan todas: pasar el texto a minusculas sin tildes, buscar palabras y reconocer
 * un numero.</p>
 */
public final class ExtractorDatos {

    private ExtractorDatos() {
    }

    /**
     * Interpreta un trozo de texto como numero.
     *
     * <p>Se apoya en el propio parser, asi que acepta lo mismo que un campo del
     * formulario: {@code 2}, {@code -1.5}, {@code pi/2} o {@code sqrt(2)}. Lo que lleve
     * variables no es un numero y se rechaza.</p>
     *
     * @param texto fragmento a interpretar
     * @return el valor, o vacio si no es un numero
     */
    public static Optional<Double> comoNumero(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }

        String limpio = texto.trim();

        try {
            // Se compila con una variable que no puede aparecer en el texto, de modo que
            // cualquier letra suelta, x incluida, haga fallar la compilacion en vez de
            // colarse como incognita. Una variable en blanco no sirve: el parser la cambia
            // por x, y entonces "x^3 - x" pasaria por el numero cero.
            double valor = EvaluadorExpresion.compilar(limpio, "_").evaluar(0);
            return Double.isFinite(valor) ? Optional.of(valor) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * Deja el texto en minusculas y sin tildes, para poder buscar palabras.
     *
     * <p>Asi "revolucion" con tilde y sin ella son la misma palabra, y no hay que escribir
     * cada termino dos veces. Cada caracter sigue en la misma posicion que en el texto
     * original, de modo que lo que se encuentre aqui se puede recortar de alla.</p>
     *
     * @param texto texto original
     * @return el texto normalizado
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.toLowerCase();
    }

    /**
     * Indica si el texto contiene alguna de las palabras dadas.
     *
     * @param textoNormalizado texto ya pasado por {@link #normalizar(String)}
     * @param palabras         palabras a buscar
     * @return true si aparece al menos una
     */
    public static boolean contieneAlguna(String textoNormalizado, String... palabras) {
        for (String palabra : palabras) {
            if (textoNormalizado.contains(palabra)) {
                return true;
            }
        }
        return false;
    }
}
