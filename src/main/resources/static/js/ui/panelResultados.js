/**
 * panelResultados.js
 * Dibuja el bloque del resultado: el numero grande y los datos que lo acompanan.
 *
 * La jerarquia visual importa. Lo primero que el usuario debe ver es la
 * respuesta a lo que pregunto — el area, o el volumen —, y despues, en
 * tamano menor, todo lo demas: la integral con signo, el metodo usado, el
 * tiempo que tardo. Mezclarlos al mismo nivel obliga a leer todo para
 * encontrar el dato que se buscaba.
 */

import { formatearDuracion } from "../util/formatoNumeros.js";
import { embellecer } from "../util/formatoMatematico.js";

/**
 * Dibuja el resultado de un calculo de area.
 *
 * @param {HTMLElement} contenedor donde se dibuja
 * @param {object} resultado respuesta del backend
 * @param {string} [tema=""] "curvas" para el tema de area entre dos curvas
 */
export function dibujarArea(contenedor, resultado, tema = "") {
    if (!contenedor) {
        return;
    }

    contenedor.innerHTML = "";

    const caja = document.createElement("div");
    caja.className = tema ? `resultado resultado--${tema}` : "resultado";

    caja.appendChild(crearEncabezado("Area de la region", resultado.areaTexto, "u²"));

    const detalles = document.createElement("div");
    detalles.className = "resultado__detalles";

    // La integral solo se muestra aparte cuando difiere del area, que es
    // justo el caso interesante: la curva cruzo el eje.
    const difieren = Math.abs(resultado.integral - resultado.area) > 1e-9;
    if (difieren) {
        detalles.appendChild(crearDato(
            "Valor de la integral",
            resultado.integralTexto,
            "Con signo: lo que queda bajo el eje cuenta como negativo."
        ));
    }

    detalles.appendChild(crearDato("Intervalo",
        `[${resultado.limiteInferior}, ${resultado.limiteSuperior}]`));
    detalles.appendChild(crearDato("Metodo", resultado.metodo));
    detalles.appendChild(crearDato("Particiones", String(resultado.particiones)));

    if (Array.isArray(resultado.intersecciones) && resultado.intersecciones.length > 0) {
        detalles.appendChild(crearDato(
            "Puntos de corte",
            resultado.intersecciones.join(",  ")
        ));
    }

    detalles.appendChild(crearDato("Tiempo", formatearDuracion(resultado.milisegundos)));

    caja.appendChild(detalles);
    contenedor.appendChild(caja);
}

/**
 * Dibuja el resultado de un calculo de volumen.
 *
 * @param {HTMLElement} contenedor donde se dibuja
 * @param {object} resultado respuesta del backend
 */
export function dibujarVolumen(contenedor, resultado) {
    if (!contenedor) {
        return;
    }

    contenedor.innerHTML = "";

    const caja = document.createElement("div");
    caja.className = "resultado resultado--solidos";

    caja.appendChild(crearEncabezado("Volumen del solido", resultado.volumenTexto, "u³"));

    // En clase el volumen se deja en la forma "8*pi" y no en decimales, asi
    // que mostrar las dos versiones permite comparar con lo hecho a mano.
    if (resultado.volumenEnPi) {
        const exacto = document.createElement("div");
        exacto.className = "resultado__exacto";
        exacto.textContent = `= ${embellecer(resultado.volumenEnPi)}`;
        caja.appendChild(exacto);
    }

    const detalles = document.createElement("div");
    detalles.className = "resultado__detalles";

    detalles.appendChild(crearDato("Metodo", resultado.tipoSolido));
    detalles.appendChild(crearDato("Formula", resultado.formulaAplicada));
    detalles.appendChild(crearDato("Eje de giro", resultado.eje?.ecuacion ?? ""));
    detalles.appendChild(crearDato("Intervalo",
        `[${resultado.limiteInferior}, ${resultado.limiteSuperior}]`));

    detalles.appendChild(crearDato(
        "Area de la region",
        `${resultado.areaRegion} u²`,
        "Area plana que se hizo girar para generar el solido."
    ));

    detalles.appendChild(crearDato(
        "Superficie lateral",
        `${resultado.superficieLateral} u²`,
        "Area de la cara exterior del solido."
    ));

    detalles.appendChild(crearDato("Tiempo", formatearDuracion(resultado.milisegundos)));

    caja.appendChild(detalles);
    contenedor.appendChild(caja);
}

/**
 * Escribe en la cabecera de la grafica la integral que se resolvio.
 *
 * @param {HTMLElement} destino elemento donde escribir
 * @param {object} resultado respuesta del backend
 */
export function escribirPlanteamiento(destino, resultado) {
    if (!destino) {
        return;
    }

    const desde = resultado.limiteInferior;
    const hasta = resultado.limiteSuperior;

    if (resultado.funcionG) {
        destino.textContent =
            `∫ de ${desde} a ${hasta} de |(${embellecer(resultado.funcionF)}) − (${embellecer(resultado.funcionG)})| dx`;
        return;
    }

    if (resultado.funcionExterior !== undefined) {
        destino.textContent = resultado.formulaAplicada ?? "";
        return;
    }

    destino.textContent = `∫ de ${desde} a ${hasta} de (${embellecer(resultado.funcionF)}) dx`;
}

// ------------------------------------------------------------------
// PIEZAS
// ------------------------------------------------------------------

/**
 * Crea el encabezado con el numero grande.
 *
 * @param {string} etiqueta que representa el numero
 * @param {string} valor el numero ya formateado
 * @param {string} unidad unidad que lo acompana
 * @returns {DocumentFragment} el encabezado
 */
function crearEncabezado(etiqueta, valor, unidad) {
    const fragmento = document.createDocumentFragment();

    const titulo = document.createElement("div");
    titulo.className = "resultado__etiqueta";
    titulo.textContent = etiqueta;

    const numero = document.createElement("div");
    numero.className = "resultado__valor";
    numero.textContent = valor;

    const medida = document.createElement("span");
    medida.className = "resultado__unidad";
    medida.textContent = unidad;
    numero.appendChild(medida);

    fragmento.append(titulo, numero);
    return fragmento;
}

/**
 * Crea uno de los datos secundarios.
 *
 * @param {string} etiqueta nombre del dato
 * @param {string} valor contenido
 * @param {string} [ayuda] texto que aparece al dejar el puntero encima
 * @returns {HTMLElement} el dato
 */
function crearDato(etiqueta, valor, ayuda = "") {
    const caja = document.createElement("div");
    caja.className = "dato";

    if (ayuda) {
        caja.title = ayuda;
    }

    const nombre = document.createElement("span");
    nombre.className = "dato__etiqueta";
    nombre.textContent = etiqueta;

    const contenido = document.createElement("span");
    contenido.className = "dato__valor";
    contenido.textContent = valor;

    caja.append(nombre, contenido);
    return caja;
}
