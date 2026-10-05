/**
 * almacenamiento.js
 * Recuerda las preferencias del usuario entre una visita y otra.
 *
 * Se guarda en el navegador, no en el servidor: son datos de comodidad —
 * el tema oscuro, el ultimo metodo elegido, las funciones que se probaron —
 * y no tiene sentido enviarlos a ningun lado.
 *
 * Cada acceso va dentro de un try porque localStorage puede fallar: en una
 * ventana privada, o si el navegador tiene bloqueado el guardado de datos,
 * lanza una excepcion. La aplicacion debe seguir funcionando en ese caso,
 * simplemente sin recordar nada.
 */

const PREFIJO = "calculo2:";
const MAXIMO_HISTORIAL = 10;

/**
 * Guarda un valor.
 *
 * @param {string} clave nombre del dato
 * @param {*} valor contenido, se guarda como JSON
 * @returns {boolean} true si se pudo guardar
 */
export function guardar(clave, valor) {
    try {
        localStorage.setItem(PREFIJO + clave, JSON.stringify(valor));
        return true;
    } catch (error) {
        return false;
    }
}

/**
 * Recupera un valor guardado.
 *
 * @param {string} clave nombre del dato
 * @param {*} porDefecto valor a devolver si no hay nada guardado
 * @returns {*} el valor recuperado, o el valor por defecto
 */
export function recuperar(clave, porDefecto = null) {
    try {
        const texto = localStorage.getItem(PREFIJO + clave);
        return texto === null ? porDefecto : JSON.parse(texto);
    } catch (error) {
        return porDefecto;
    }
}

/**
 * Borra un valor guardado.
 *
 * @param {string} clave nombre del dato
 */
export function borrar(clave) {
    try {
        localStorage.removeItem(PREFIJO + clave);
    } catch (error) {
        // Si no se puede borrar, no hay nada mas que hacer.
    }
}

/**
 * Agrega una funcion al historial de las que se han calculado.
 *
 * Las repetidas se mueven al principio en lugar de duplicarse, y la lista
 * se recorta para que no crezca sin control.
 *
 * @param {string} tema tema en que se uso, para separar los historiales
 * @param {string} funcion la funcion que escribio el usuario
 */
export function agregarAlHistorial(tema, funcion) {
    const texto = (funcion ?? "").trim();
    if (texto === "") {
        return;
    }

    const clave = `historial:${tema}`;
    const historial = recuperar(clave, []);

    const sinRepetida = historial.filter((entrada) => entrada !== texto);
    sinRepetida.unshift(texto);

    guardar(clave, sinRepetida.slice(0, MAXIMO_HISTORIAL));
}

/**
 * Devuelve el historial de funciones de un tema.
 *
 * @param {string} tema tema del que se quiere el historial
 * @returns {string[]} las funciones, de la mas reciente a la mas antigua
 */
export function obtenerHistorial(tema) {
    return recuperar(`historial:${tema}`, []);
}

/**
 * Guarda el tema visual elegido.
 *
 * @param {string} tema "claro" u "oscuro"
 */
export function guardarTema(tema) {
    guardar("tema", tema);
}

/**
 * Devuelve el tema visual que corresponde mostrar.
 *
 * Si el usuario nunca eligio, se respeta la preferencia que tenga
 * configurada en su sistema operativo.
 *
 * @returns {string} "claro" u "oscuro"
 */
export function obtenerTema() {
    const guardado = recuperar("tema", null);
    if (guardado === "claro" || guardado === "oscuro") {
        return guardado;
    }

    const prefiereOscuro = window.matchMedia
        && window.matchMedia("(prefers-color-scheme: dark)").matches;

    return prefiereOscuro ? "oscuro" : "claro";
}
