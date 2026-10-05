package com.calculo2.integrales.model;

import java.util.Locale;

/**
 * Los tres metodos para calcular el volumen de un solido de revolucion.
 *
 * <p>Corresponden a los que aparecen en el material del curso. La eleccion no es de
 * gusto: depende de si el solido es macizo o hueco y de si conviene integrar respecto a
 * la misma variable del eje o respecto a la otra.</p>
 */
public enum TipoSolido {

    /**
     * Discos: el solido es macizo, cada corte perpendicular al eje es un circulo lleno.
     *
     * <pre>V = pi * integral de [R(x)]^2 dx</pre>
     */
    DISCOS("Metodo de discos",
            "V = pi * integral de [R(x)]^2 dx",
            "Cada rebanada perpendicular al eje es un circulo completo. Se usa cuando la region "
                    + "toca el eje de revolucion, asi que el solido queda macizo."),

    /**
     * Arandelas: el solido es hueco, cada corte es un anillo entre dos radios.
     *
     * <pre>V = pi * integral de ([R(x)]^2 - [r(x)]^2) dx</pre>
     */
    ARANDELAS("Metodo de arandelas",
            "V = pi * integral de ([R(x)]^2 - [r(x)]^2) dx",
            "Cada rebanada es un anillo: se calcula el disco exterior y se le resta el agujero "
                    + "interior. Se usa cuando la region esta limitada por dos curvas y queda "
                    + "separada del eje."),

    /**
     * Capas cilindricas: el solido se arma con tubos concentricos.
     *
     * <pre>V = 2*pi * integral de (radio) * (altura) dx</pre>
     */
    CAPAS("Metodo de capas cilindricas",
            "V = 2*pi * integral de x * f(x) dx",
            "En vez de rebanar perpendicular al eje, se arma el solido con cilindros huecos "
                    + "concentricos. Conviene cuando despejar la funcion para el otro eje seria "
                    + "complicado.");

    private final String etiqueta;
    private final String formula;
    private final String explicacion;

    TipoSolido(String etiqueta, String formula, String explicacion) {
        this.etiqueta = etiqueta;
        this.formula = formula;
        this.explicacion = explicacion;
    }

    /** Nombre del metodo tal como se muestra en pantalla. */
    public String etiqueta() {
        return etiqueta;
    }

    /** La formula del metodo. */
    public String formula() {
        return formula;
    }

    /** Cuando conviene usar este metodo y por que. */
    public String explicacion() {
        return explicacion;
    }

    /** Indica si el metodo necesita una segunda curva para el radio interior. */
    public boolean necesitaSegundaCurva() {
        return this == ARANDELAS;
    }

    /**
     * Dice respecto de que variable hay que integrar con este metodo y este eje.
     *
     * <p>Depende de los dos, no solo del eje. Discos y arandelas rebanan de forma
     * perpendicular al eje de giro, asi que se integra en la misma direccion del eje:
     * respecto de {@code x} si el eje es horizontal, respecto de {@code y} si es vertical.
     * Las capas cilindricas hacen lo contrario, porque los cascarones se apilan en
     * direccion perpendicular al eje.</p>
     *
     * <p>De aqui sale tambien la condicion para poder aplicar un metodo: si esta variable
     * no es aquella en que esta escrita la funcion, hay que despejarla antes, y eso rara
     * vez se puede hacer.</p>
     *
     * @param eje recta alrededor de la cual gira la region
     * @return "x" o "y"
     */
    public String variableDeIntegracion(EjeRotacion eje) {
        boolean integrarEnX = (this == CAPAS) != eje.esHorizontal();
        return integrarEnX ? "x" : "y";
    }

    /**
     * Traduce el texto que llego del navegador al metodo correspondiente.
     *
     * @param texto nombre del metodo
     * @return el metodo indicado, o {@link #DISCOS} si el texto no coincide con ninguno
     */
    public static TipoSolido desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return DISCOS;
        }
        String normalizado = texto.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (TipoSolido tipo : values()) {
            if (tipo.name().equals(normalizado)) {
                return tipo;
            }
        }
        // Sinonimos que el usuario podria escribir.
        return switch (normalizado) {
            case "ANILLO", "ANILLOS", "ARANDELA" -> ARANDELAS;
            case "CAPA", "CASCARONES", "CILINDROS" -> CAPAS;
            case "DISCO" -> DISCOS;
            default -> DISCOS;
        };
    }
}
