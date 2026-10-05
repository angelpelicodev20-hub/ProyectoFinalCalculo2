package com.calculo2.integrales.util;

/**
 * Valores fijos que usa toda la aplicacion: puerto, tolerancias y limites de seguridad.
 * Los limites existen para que una funcion mal escrita no bloquee el servidor.
 */
public final class Constantes {

    private Constantes() {
    }

    /** Puerto por defecto del servidor HTTP. */
    public static final int PUERTO_POR_DEFECTO = 8080;

    /** Carpeta dentro del jar donde viven el HTML, CSS y JS. */
    public static final String RAIZ_ESTATICA = "/static";

    /** Numero de particiones por defecto para los metodos numericos. */
    public static final int PARTICIONES_POR_DEFECTO = 1000;

    /** Minimo de particiones aceptado en una peticion. */
    public static final int PARTICIONES_MINIMAS = 2;

    /** Maximo de particiones aceptado; evita peticiones que congelen el servidor. */
    public static final int PARTICIONES_MAXIMAS = 200_000;

    /** Tolerancia usada por el integrador adaptativo y la busqueda de raices. */
    public static final double TOLERANCIA = 1e-10;

    /** Profundidad maxima de recursion del integrador adaptativo. */
    public static final int PROFUNDIDAD_MAXIMA = 50;

    /** Decimales que se muestran en los resultados. */
    public static final int DECIMALES_RESULTADO = 6;

    /** Puntos que se envian al navegador para dibujar una curva en 2D. */
    public static final int PUNTOS_CURVA_2D = 400;

    /** Numero de cortes a lo largo del eje al construir la malla del solido. */
    public static final int CORTES_MALLA = 60;

    /** Numero de puntos por anillo al girar el perfil alrededor del eje. */
    public static final int SEGMENTOS_ANILLO = 36;

    /** Longitud maxima permitida para la expresion escrita por el usuario. */
    public static final int LONGITUD_MAXIMA_EXPRESION = 300;

    /** Separacion entre curvas por debajo de la cual se considera que se cortan. */
    public static final double EPSILON_INTERSECCION = 1e-9;

    /** Valor absoluto a partir del cual se considera que la funcion diverge. */
    public static final double LIMITE_DIVERGENCIA = 1e12;
}
