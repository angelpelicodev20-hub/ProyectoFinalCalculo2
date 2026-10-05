/**
 * validaciones.js
 * Revisa los datos del formulario antes de mandarlos al servidor.
 *
 * El backend vuelve a validarlo todo, y esa es la validacion que cuenta:
 * nunca se debe confiar en lo que llega del navegador. Lo de aqui existe
 * por comodidad, para que el usuario vea el error de inmediato en lugar
 * de esperar una respuesta.
 */

/** Numero de particiones aceptado, en concordancia con Constantes.java. */
const PARTICIONES_MINIMAS = 2;
const PARTICIONES_MAXIMAS = 200000;
const LONGITUD_MAXIMA = 300;

/**
 * Revisa el texto de una funcion.
 *
 * No comprueba que la expresion sea valida — de eso se encarga el parser
 * de Java, que es el unico que conoce la gramatica completa — sino que
 * detecta los descuidos mas comunes antes de gastar una peticion.
 *
 * @param {string} texto lo que escribio el usuario
 * @param {string} [nombre="La funcion"] como llamarla en el mensaje
 * @returns {{valido: boolean, mensaje: string}} el resultado de la revision
 */
export function validarFuncion(texto, nombre = "La funcion") {
    const funcion = (texto ?? "").trim();

    if (funcion === "") {
        return { valido: false, mensaje: `${nombre} no puede quedar vacia.` };
    }

    if (funcion.length > LONGITUD_MAXIMA) {
        return {
            valido: false,
            mensaje: `${nombre} es demasiado larga; el maximo son ${LONGITUD_MAXIMA} caracteres.`
        };
    }

    const balance = revisarParentesis(funcion);
    if (!balance.valido) {
        return { valido: false, mensaje: balance.mensaje };
    }

    // Dos operadores seguidos casi siempre son un error de tecleo. Se
    // exceptua el signo que sigue a una potencia, porque "2^-1" es valido.
    if (/[+\-*/]{2,}/.test(funcion.replace(/\^-/g, "^"))) {
        return {
            valido: false,
            mensaje: `${nombre} tiene dos operadores seguidos.`
        };
    }

    return { valido: true, mensaje: "" };
}

/**
 * Comprueba que los parentesis abran y cierren en orden.
 *
 * @param {string} texto expresion a revisar
 * @returns {{valido: boolean, mensaje: string}} el resultado de la revision
 */
export function revisarParentesis(texto) {
    let abiertos = 0;

    for (const caracter of texto) {
        if (caracter === "(") {
            abiertos++;
        } else if (caracter === ")") {
            abiertos--;
            if (abiertos < 0) {
                return {
                    valido: false,
                    mensaje: "Hay un parentesis ')' que no se abrio antes."
                };
            }
        }
    }

    if (abiertos > 0) {
        return {
            valido: false,
            mensaje: `Falta cerrar ${abiertos} parentesis.`
        };
    }

    return { valido: true, mensaje: "" };
}

/**
 * Revisa los limites de integracion.
 *
 * @param {number} a limite inferior
 * @param {number} b limite superior
 * @returns {{valido: boolean, mensaje: string}} el resultado de la revision
 */
export function validarLimites(a, b) {
    if (!Number.isFinite(a) || !Number.isFinite(b)) {
        return {
            valido: false,
            mensaje: "Los limites de integracion deben ser numeros."
        };
    }

    if (Math.abs(b - a) < 1e-12) {
        return {
            valido: false,
            mensaje: "Los dos limites son iguales, asi que la region no tiene area."
        };
    }

    return { valido: true, mensaje: "" };
}

/**
 * Revisa el numero de particiones.
 *
 * @param {number} particiones valor del formulario
 * @returns {{valido: boolean, mensaje: string}} el resultado de la revision
 */
export function validarParticiones(particiones) {
    if (!Number.isInteger(particiones)) {
        return {
            valido: false,
            mensaje: "El numero de particiones debe ser un numero entero."
        };
    }

    if (particiones < PARTICIONES_MINIMAS) {
        return {
            valido: false,
            mensaje: `El numero de particiones debe ser al menos ${PARTICIONES_MINIMAS}.`
        };
    }

    if (particiones > PARTICIONES_MAXIMAS) {
        return {
            valido: false,
            mensaje: `El numero de particiones no puede pasar de ${PARTICIONES_MAXIMAS}.`
        };
    }

    return { valido: true, mensaje: "" };
}

/**
 * Lee un campo de texto como numero.
 *
 * @param {HTMLInputElement} campo campo del formulario
 * @param {number} porDefecto valor si el campo esta vacio o no es numero
 * @returns {number} el numero leido
 */
export function leerNumero(campo, porDefecto = 0) {
    const texto = (campo?.value ?? "").trim();
    if (texto === "") {
        return porDefecto;
    }
    const valor = Number(texto);
    return Number.isFinite(valor) ? valor : porDefecto;
}
