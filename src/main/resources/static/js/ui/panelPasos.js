/**
 * panelPasos.js
 * Dibuja la explicacion paso a paso.
 *
 * Es la parte que convierte la aplicacion en algo util para estudiar: no
 * basta con dar el numero, hay que mostrar el camino. Los pasos vienen
 * redactados de Java, en PasosService; aqui solo se maquetan.
 */

import { embellecerPaso } from "../util/formatoMatematico.js";

/**
 * Dibuja la lista de pasos.
 *
 * @param {HTMLElement} contenedor donde se dibuja la lista
 * @param {Array<object>} pasos pasos que devolvio el backend
 * @param {string} [tema=""] "curvas" o "solidos", para el color de los numeros
 */
export function dibujar(contenedor, pasos, tema = "") {
    if (!contenedor) {
        return;
    }

    contenedor.innerHTML = "";

    if (!Array.isArray(pasos) || pasos.length === 0) {
        return;
    }

    const lista = document.createElement("ol");
    lista.className = tema ? `pasos pasos--${tema}` : "pasos";

    for (const paso of pasos) {
        lista.appendChild(construirPaso(paso));
    }

    contenedor.appendChild(lista);
}

/**
 * Construye el elemento de un paso.
 *
 * @param {object} paso paso a dibujar
 * @returns {HTMLLIElement} el elemento listo para insertarse
 */
function construirPaso(paso) {
    const elemento = document.createElement("li");
    elemento.className = "paso";

    const titulo = document.createElement("h4");
    titulo.className = "paso__titulo";
    titulo.textContent = paso.titulo ?? "";
    elemento.appendChild(titulo);

    if (paso.explicacion) {
        const explicacion = document.createElement("p");
        explicacion.className = "paso__explicacion";
        explicacion.textContent = paso.explicacion;
        elemento.appendChild(explicacion);
    }

    if (paso.expresion) {
        const expresion = document.createElement("pre");
        expresion.className = "paso__expresion";
        // textContent y no innerHTML: la expresion incluye lo que escribio el
        // usuario y no debe interpretarse como marcado.
        expresion.textContent = embellecerPaso(paso.expresion);
        elemento.appendChild(expresion);
    }

    if (paso.resultado) {
        const resultado = document.createElement("div");
        resultado.className = "paso__resultado";
        resultado.textContent = `= ${paso.resultado}`;
        elemento.appendChild(resultado);
    }

    return elemento;
}



/**
 * Muestra como se hallaron los limites cuando los calculo el sistema.
 *
 * Cuando el estudiante marca "Buscar los limites", los numeros que aparecen en
 * el formulario no los escribio el: los dedujo el programa. Usarlos sin mas
 * convertiria el resultado en algo que hay que creer. Aqui queda a la vista la
 * ecuacion que se resolvio y las soluciones que salieron, para poder
 * comprobarlo a mano.
 *
 * @param {HTMLElement} contenedor donde se dibuja el bloque
 * @param {string} explicacion texto que redacto el backend
 */
export function dibujarExplicacionDeLimites(contenedor, explicacion) {
    if (!contenedor) {
        return;
    }

    contenedor.innerHTML = "";

    if (!explicacion || !String(explicacion).trim()) {
        contenedor.classList.add("oculto");
        return;
    }

    contenedor.classList.remove("oculto");

    const titulo = document.createElement("h3");
    titulo.textContent = "De donde salieron los limites";
    titulo.style.marginBottom = "0.5rem";

    const cuerpo = document.createElement("pre");
    cuerpo.className = "paso__expresion";
    cuerpo.textContent = embellecerPaso(explicacion);

    contenedor.append(titulo, cuerpo);
}

/**
 * Dibuja la tabla de rebanadas de un solido de revolucion.
 *
 * Muestra el radio en varios puntos del eje, que es lo que permite
 * comprobar a mano si el planteamiento es correcto antes de fiarse del
 * volumen final.
 *
 * @param {HTMLElement} contenedor donde se dibuja la tabla
 * @param {Array<object>} rebanadas rebanadas que devolvio el backend
 * @param {boolean} esHueco true si el solido tiene agujero
 */
export function dibujarRebanadas(contenedor, rebanadas, esHueco) {
    if (!contenedor) {
        return;
    }

    contenedor.innerHTML = "";

    if (!Array.isArray(rebanadas) || rebanadas.length === 0) {
        contenedor.classList.add("oculto");
        return;
    }

    contenedor.classList.remove("oculto");

    const caja = document.createElement("div");
    caja.className = "tabla-desplazable";

    const tabla = document.createElement("table");
    tabla.className = "tabla-resaltada";

    const columnaInterior = esHueco ? '<th class="numero">Radio interior</th>' : "";
    tabla.innerHTML = `
        <thead>
            <tr>
                <th class="numero">Posicion</th>
                <th class="numero">Radio exterior</th>
                ${columnaInterior}
                <th class="numero">Volumen aproximado</th>
            </tr>
        </thead>
    `;

    const cuerpo = document.createElement("tbody");

    for (const rebanada of rebanadas) {
        const fila = document.createElement("tr");

        const posicion = document.createElement("td");
        posicion.className = "numero";
        posicion.textContent = rebanada.posicion;

        const exterior = document.createElement("td");
        exterior.className = "numero";
        exterior.textContent = rebanada.radioExterior;

        fila.append(posicion, exterior);

        if (esHueco) {
            const interior = document.createElement("td");
            interior.className = "numero";
            interior.textContent = rebanada.radioInterior;
            fila.appendChild(interior);
        }

        const volumen = document.createElement("td");
        volumen.className = "numero";
        volumen.textContent = rebanada.volumen;
        fila.appendChild(volumen);

        cuerpo.appendChild(fila);
    }

    tabla.appendChild(cuerpo);
    caja.appendChild(tabla);
    contenedor.appendChild(caja);
}
