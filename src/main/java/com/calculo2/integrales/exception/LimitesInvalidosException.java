package com.calculo2.integrales.exception;

/**
 * Se lanza cuando los datos de la peticion no describen un ejercicio que se pueda
 * resolver: el limite inferior es mayor que el superior, el numero de particiones es
 * negativo, el eje de rotacion atraviesa la region, o el metodo elegido no se puede
 * aplicar a la funcion tal como esta escrita.
 */
public class LimitesInvalidosException extends RuntimeException {

    /** Sugerencia por defecto, para los errores que si son de limites. */
    private static final String SUGERENCIA_HABITUAL =
            "Revise los limites de integracion y el numero de particiones.";

    private final String sugerencia;

    /**
     * @param mensaje explicacion para el usuario
     */
    public LimitesInvalidosException(String mensaje) {
        this(mensaje, SUGERENCIA_HABITUAL);
    }

    /**
     * Crea el error con una sugerencia propia.
     *
     * <p>No todos los problemas que pasan por aqui se arreglan revisando los limites. Si
     * el metodo elegido no se puede aplicar a la funcion tal como esta escrita, decirle al
     * estudiante que mire los limites lo manda a buscar donde no hay nada: lo que tiene
     * que cambiar es el metodo o la forma de la funcion.</p>
     *
     * @param mensaje    explicacion para el usuario
     * @param sugerencia que puede hacer para salir del problema
     */
    public LimitesInvalidosException(String mensaje, String sugerencia) {
        super(mensaje);
        this.sugerencia = sugerencia;
    }

    /** Que se le propone al usuario para resolverlo. */
    public String getSugerencia() {
        return sugerencia;
    }
}
