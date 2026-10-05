/**
 * clienteApi.js
 * Habla con el backend de Java.
 *
 * Todas las peticiones pasan por aqui, y por una razon concreta: el manejo
 * de errores. El servidor responde los problemas del usuario con codigo 400
 * y un cuerpo JSON que explica que salio mal — "falta cerrar un parentesis",
 * "ln(x) no esta definida en ese intervalo" —, y ese mensaje es justo el que
 * hay que mostrar en pantalla. Si cada pagina llamara a fetch por su cuenta,
 * ese mensaje se perderia y el usuario solo veria un error generico.
 */

import { RUTAS } from "./endpoints.js";

/** Tiempo maximo que se espera una respuesta, en milisegundos. */
const TIEMPO_LIMITE = 30000;

/**
 * Error que representa una respuesta fallida del servidor.
 *
 * Conserva el mensaje redactado por el backend y los datos extra que
 * lo acompanan, como la posicion del caracter que causo el problema.
 */
export class ErrorDeCalculo extends Error {

    /**
     * @param {string} mensaje    explicacion para el usuario
     * @param {object} [detalles] cuerpo completo de la respuesta de error
     */
    constructor(mensaje, detalles = {}) {
        super(mensaje);
        this.name = "ErrorDeCalculo";
        this.tipo = detalles.tipo ?? "ERROR";
        this.sugerencia = detalles.sugerencia ?? "";
        this.posicion = detalles.posicion ?? -1;
    }
}

/**
 * Manda una peticion POST con un cuerpo JSON.
 *
 * @param {string} ruta  direccion del servidor
 * @param {object} datos objeto que se enviara como JSON
 * @returns {Promise<object>} la respuesta ya interpretada
 * @throws {ErrorDeCalculo} si el servidor rechaza la peticion o no responde
 */
async function enviar(ruta, datos) {
    // El controlador aborta la peticion si el servidor tarda demasiado; sin
    // esto, una funcion muy pesada dejaria la pagina esperando para siempre.
    const controlador = new AbortController();
    const temporizador = setTimeout(() => controlador.abort(), TIEMPO_LIMITE);

    let respuesta;
    try {
        respuesta = await fetch(ruta, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(datos),
            signal: controlador.signal
        });
    } catch (error) {
        clearTimeout(temporizador);

        if (error.name === "AbortError") {
            throw new ErrorDeCalculo(
                "El calculo tardo demasiado y se cancelo.",
                { sugerencia: "Pruebe con menos particiones o con una funcion mas simple." }
            );
        }
        throw new ErrorDeCalculo(
            "No se pudo contactar al servidor.",
            { sugerencia: "Revise que la aplicacion de Java siga corriendo en la consola." }
        );
    } finally {
        clearTimeout(temporizador);
    }

    return interpretar(respuesta);
}

/**
 * Convierte la respuesta HTTP en un objeto, o en un error con su mensaje.
 *
 * @param {Response} respuesta respuesta cruda de fetch
 * @returns {Promise<object>} el cuerpo interpretado
 * @throws {ErrorDeCalculo} si la respuesta indica un fallo
 */
async function interpretar(respuesta) {
    let cuerpo;
    try {
        cuerpo = await respuesta.json();
    } catch (error) {
        throw new ErrorDeCalculo(
            `El servidor respondio algo que no se pudo leer (codigo ${respuesta.status}).`
        );
    }

    if (!respuesta.ok || cuerpo.exito === false) {
        throw new ErrorDeCalculo(
            cuerpo.mensaje ?? "El servidor rechazo la peticion.",
            cuerpo
        );
    }

    return cuerpo;
}

/**
 * Pide el area bajo una curva.
 *
 * @param {object} datos funcion, limites, particiones y metodo
 * @returns {Promise<object>} el resultado completo
 */
export function calcularAreaBajoCurva(datos) {
    return enviar(RUTAS.areaBajoCurva, datos);
}

/**
 * Pide el area entre dos curvas.
 *
 * @param {object} datos las dos funciones, limites, particiones y metodo
 * @returns {Promise<object>} el resultado completo
 */
export function calcularAreaEntreCurvas(datos) {
    return enviar(RUTAS.areaEntreCurvas, datos);
}

/**
 * Pide el volumen de un solido de revolucion.
 *
 * @param {object} datos curvas, limites, eje de giro y metodo
 * @returns {Promise<object>} el resultado completo
 */
export function calcularSolidoRevolucion(datos) {
    return enviar(RUTAS.solidoRevolucion, datos);
}

/**
 * Pide los puntos de una curva para la vista previa, sin integrar.
 *
 * @param {object} datos funcion y rango a dibujar
 * @returns {Promise<object>} los datos de la grafica
 */
export function obtenerGrafica(datos) {
    return enviar(RUTAS.grafica, datos);
}

/**
 * Interpreta un enunciado escrito en palabras.
 *
 * No resuelve nada: devuelve el ejercicio estructurado — tema, metodo, funciones,
 * limites y eje — junto con lo que falte y el razonamiento seguido. Resolver es un
 * segundo paso, despues de que el estudiante revise esos datos.
 *
 * @param {string} enunciado el problema tal como lo escribio el estudiante
 * @returns {Promise<object>} la interpretacion
 */
export function analizarProblema(enunciado) {
    return enviar(RUTAS.analizar, { enunciado });
}

/**
 * Evalua una expresion suelta.
 *
 * @param {object} datos expresion y, si lleva variable, el valor que toma
 * @returns {Promise<object>} el resultado de la evaluacion
 */
export function evaluarExpresion(datos) {
    return enviar(RUTAS.calcular, datos);
}

/**
 * Consulta el estado del servidor y las opciones que ofrece.
 *
 * @returns {Promise<object>} metodos, tipos de solido y catalogo de funciones
 * @throws {ErrorDeCalculo} si el servidor no responde
 */
export async function consultarSalud() {
    let respuesta;
    try {
        respuesta = await fetch(RUTAS.salud);
    } catch (error) {
        throw new ErrorDeCalculo("El servidor no responde.");
    }
    return interpretar(respuesta);
}
