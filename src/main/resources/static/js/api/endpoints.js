/**
 * endpoints.js
 * Las rutas del servidor, reunidas en un solo lugar.
 *
 * Tenerlas aqui evita que una direccion quede escrita a mano en varios
 * archivos: si manana cambia una ruta en ConfiguracionServidor.java, solo
 * hay que tocar este archivo.
 */

/**
 * Base de las direcciones.
 *
 * Se deja vacia a proposito para que las rutas salgan relativas. Asi la
 * pagina funciona igual en localhost que si se publica en otra direccion,
 * sin recompilar nada.
 */
export const BASE = "";

/** Rutas que atiende el backend. */
export const RUTAS = {
    /** Calcula el area bajo una curva. */
    areaBajoCurva: `${BASE}/api/area-bajo-curva`,

    /** Calcula el area entre dos curvas. */
    areaEntreCurvas: `${BASE}/api/area-entre-curvas`,

    /** Calcula el volumen de un solido de revolucion. */
    solidoRevolucion: `${BASE}/api/solido-revolucion`,

    /** Devuelve los puntos de una curva, sin integrar. */
    grafica: `${BASE}/api/grafica`,

    /** Interpreta un enunciado escrito en palabras; no resuelve, solo estructura. */
    analizar: `${BASE}/api/analizar`,

    /** Evalua una expresion suelta: es la modalidad de calculadora. */
    calcular: `${BASE}/api/calcular`,

    /** Informa el estado del servidor y las opciones disponibles. */
    salud: `${BASE}/api/salud`
};
