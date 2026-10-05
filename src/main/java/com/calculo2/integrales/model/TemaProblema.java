package com.calculo2.integrales.model;

import java.util.Locale;

/**
 * Los tres temas que resuelve la aplicacion, mas el caso en que no se pudo decidir.
 *
 * <p>Aparece cuando el estudiante escribe el problema en palabras y el sistema tiene que
 * averiguar de que trata. Que exista la constante {@link #DESCONOCIDO} es parte del
 * diseno: es preferible admitir que no se supo clasificar el enunciado a elegir un tema
 * al azar y resolver el ejercicio equivocado.</p>
 */
public enum TemaProblema {

    /** Area entre una curva y el eje horizontal. */
    AREA_BAJO_CURVA("Area bajo la curva", "/pages/area-bajo-curva.html"),

    /** Area de la region encerrada entre dos curvas. */
    AREA_ENTRE_CURVAS("Area entre dos curvas", "/pages/area-entre-curvas.html"),

    /** Volumen del solido generado al girar una region. */
    VOLUMEN_SOLIDO("Volumen de solidos de revolucion", "/pages/solidos-revolucion.html"),

    /** No se pudo determinar el tema con la informacion disponible. */
    DESCONOCIDO("Sin determinar", "");

    private final String etiqueta;
    private final String pagina;

    TemaProblema(String etiqueta, String pagina) {
        this.etiqueta = etiqueta;
        this.pagina = pagina;
    }

    /** Nombre del tema tal como se muestra en pantalla. */
    public String etiqueta() {
        return etiqueta;
    }

    /** Pagina de la aplicacion que resuelve este tema. */
    public String pagina() {
        return pagina;
    }

    /** Indica si el tema quedo determinado. */
    public boolean estaDeterminado() {
        return this != DESCONOCIDO;
    }

    /** Indica si el tema necesita una segunda funcion para plantearse. */
    public boolean necesitaDosFunciones() {
        return this == AREA_ENTRE_CURVAS;
    }

    /** Indica si el tema necesita un eje de revolucion. */
    public boolean necesitaEje() {
        return this == VOLUMEN_SOLIDO;
    }

    /**
     * Traduce el texto que llego del navegador al tema correspondiente.
     *
     * @param texto nombre del tema
     * @return el tema indicado, o {@link #DESCONOCIDO} si no coincide con ninguno
     */
    public static TemaProblema desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return DESCONOCIDO;
        }
        String normalizado = texto.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        for (TemaProblema tema : values()) {
            if (tema.name().equals(normalizado)) {
                return tema;
            }
        }
        return DESCONOCIDO;
    }
}
