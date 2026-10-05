/**
 * selectorMetodo.js
 * Llena los selectores de metodo numerico y de tipo de solido.
 *
 * Las opciones no estan escritas en el HTML: se piden al servidor. Asi la
 * lista sale directamente de los enum de Java, y agregar un metodo alla lo
 * hace aparecer aqui sin tocar el frontend. Tambien evita el problema
 * clasico de que la etiqueta del HTML y la constante del backend dejen de
 * coincidir con el tiempo.
 */

import { consultarSalud } from "../api/clienteApi.js";
import { recuperar, guardar } from "../util/almacenamiento.js";

/** Lo que respondio el servidor, guardado para no volver a pedirlo. */
let catalogo = null;

/**
 * Pide al servidor la lista de metodos y opciones.
 *
 * @returns {Promise<object|null>} el catalogo, o null si el servidor no responde
 */
export async function cargarCatalogo() {
    if (catalogo) {
        return catalogo;
    }
    try {
        catalogo = await consultarSalud();
        return catalogo;
    } catch (error) {
        return null;
    }
}

/**
 * Llena el selector de metodos numericos.
 *
 * @param {HTMLSelectElement} selector selector a llenar
 * @param {HTMLElement} [descripcion] elemento donde explicar el metodo elegido
 * @returns {Promise<void>}
 */
export async function llenarMetodos(selector, descripcion) {
    if (!selector) {
        return;
    }

    const datos = await cargarCatalogo();
    if (!datos) {
        // Sin servidor no hay opciones que ofrecer; el mensaje de error lo
        // muestra la pagina por su cuenta.
        return;
    }

    selector.innerHTML = "";

    for (const metodo of datos.metodosDeArea) {
        const opcion = document.createElement("option");
        opcion.value = metodo.clave;
        opcion.textContent = metodo.etiqueta;
        opcion.dataset.descripcion = metodo.descripcion;
        opcion.dataset.formula = metodo.formula;
        opcion.dataset.dibujaRectangulos = String(metodo.dibujaRectangulos);
        selector.appendChild(opcion);
    }

    // Se recupera el metodo que el usuario uso la ultima vez.
    const guardado = recuperar("metodo", "ANALITICO");
    if ([...selector.options].some((opcion) => opcion.value === guardado)) {
        selector.value = guardado;
    }

    const actualizar = () => {
        guardar("metodo", selector.value);
        mostrarDescripcion(selector, descripcion);

        // Los controles de Riemann solo tienen sentido con Riemann elegido.
        const elegida = selector.options[selector.selectedIndex];
        const usaRectangulos = elegida?.dataset.dibujaRectangulos === "true";

        for (const id of ["grupoRiemann", "grupoParticiones"]) {
            document.getElementById(id)?.classList.toggle("oculto", !usaRectangulos);
        }
    };

    selector.addEventListener("change", actualizar);
    actualizar();
}

/**
 * Llena el selector de donde se mide la altura de los rectangulos.
 *
 * Las tres posiciones no son tres metodos distintos: son el mismo metodo de
 * Riemann midiendo en un punto u otro de cada subintervalo. La diferencia se ve
 * en la grafica, y explica por que la aproximacion se queda corta o se pasa.
 *
 * @param {HTMLSelectElement} selector selector a llenar
 * @returns {Promise<void>}
 */
export async function llenarPosicionesRiemann(selector) {
    if (!selector) {
        return;
    }

    const datos = await cargarCatalogo();
    if (!datos) {
        return;
    }

    selector.innerHTML = "";

    for (const posicion of datos.posicionesRiemann) {
        const opcion = document.createElement("option");
        opcion.value = posicion.clave;
        opcion.textContent = `Altura en el ${posicion.descripcion}`;
        selector.appendChild(opcion);
    }

    selector.value = "MEDIO";
}

/**
 * Llena el selector de tipo de solido.
 *
 * @param {HTMLSelectElement} selector selector a llenar
 * @param {HTMLElement} [descripcion] elemento donde explicar el metodo elegido
 * @param {Function} [alCambiar] se llama con el tipo elegido al cambiar
 * @returns {Promise<void>}
 */
export async function llenarTiposDeSolido(selector, descripcion, alCambiar) {
    if (!selector) {
        return;
    }

    const datos = await cargarCatalogo();
    if (!datos) {
        return;
    }

    selector.innerHTML = "";

    for (const tipo of datos.tiposDeSolido) {
        const opcion = document.createElement("option");
        opcion.value = tipo.clave;
        opcion.textContent = tipo.etiqueta;
        opcion.dataset.descripcion = tipo.explicacion;
        opcion.dataset.formula = tipo.formula;
        opcion.dataset.necesitaSegundaCurva = String(tipo.necesitaSegundaCurva);
        selector.appendChild(opcion);
    }

    const actualizar = () => {
        mostrarDescripcion(selector, descripcion);
        if (typeof alCambiar === "function") {
            const elegida = selector.options[selector.selectedIndex];
            alCambiar({
                clave: selector.value,
                necesitaSegundaCurva: elegida?.dataset.necesitaSegundaCurva === "true"
            });
        }
    };

    selector.addEventListener("change", actualizar);
    actualizar();
}

/**
 * Escribe bajo el selector la formula y la explicacion de la opcion elegida.
 *
 * @param {HTMLSelectElement} selector selector consultado
 * @param {HTMLElement} destino elemento donde escribir
 */
function mostrarDescripcion(selector, destino) {
    if (!destino) {
        return;
    }

    const opcion = selector.options[selector.selectedIndex];
    if (!opcion) {
        destino.textContent = "";
        return;
    }

    destino.innerHTML = "";

    if (opcion.dataset.formula) {
        const formula = document.createElement("div");
        formula.className = "mono";
        formula.style.marginBottom = "0.25rem";
        formula.textContent = opcion.dataset.formula;
        destino.appendChild(formula);
    }

    if (opcion.dataset.descripcion) {
        destino.appendChild(document.createTextNode(opcion.dataset.descripcion));
    }
}

/**
 * Indica si el metodo elegido es una suma de Riemann.
 *
 * Solo en ese caso tiene sentido dibujar los rectangulos sobre la curva, asi
 * que la pagina lo consulta antes de ofrecer esa opcion.
 *
 * @param {string} clave clave del metodo
 * @returns {boolean} true si es alguna variante de Riemann
 */
export function esMetodoDeRiemann(clave) {
    if (!catalogo) {
        return String(clave).startsWith("RIEMANN");
    }
    const metodo = catalogo.metodosDeArea.find((entrada) => entrada.clave === clave);
    return metodo?.dibujaRectangulos ?? false;
}

/**
 * Devuelve el catalogo ya cargado, o null si aun no se pidio.
 *
 * @returns {object|null} el catalogo
 */
export function catalogoActual() {
    return catalogo;
}
