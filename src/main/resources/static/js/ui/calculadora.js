/**
 * calculadora.js
 * Impulsa la modalidad de calculadora.
 *
 * Aqui la respuesta es un numero y se acaba. Eso la separa del solucionador, y
 * la separacion es deliberada: mezclar las dos cosas en la misma pantalla haria
 * que ninguna quedara clara. La calculadora sirve para las cuentas sueltas que
 * aparecen en medio de un ejercicio; el solucionador, para el ejercicio entero.
 */

import { evaluarExpresion } from "../api/clienteApi.js";
import * as mensajes from "./mensajes.js";
import * as teclado from "./tecladoMatematico.js";
import { embellecer } from "../util/formatoMatematico.js";

/** Prepara la pagina de la calculadora. */
export function iniciarCalculadora() {
    const formulario = document.getElementById("formularioCalculadora");
    const expresion = document.getElementById("expresion");

    if (expresion) {
        teclado.construir(document.getElementById("teclado"), expresion);
    }

    formulario?.addEventListener("submit", (evento) => {
        evento.preventDefault();
        calcular();
    });

    for (const boton of document.querySelectorAll("[data-expresion]")) {
        boton.addEventListener("click", () => {
            expresion.value = boton.dataset.expresion;
            document.getElementById("valorVariable").value = boton.dataset.valor ?? "";
            calcular();
        });
    }
}

/** Evalua lo que haya escrito y muestra el resultado. */
async function calcular() {
    const zonaMensajes = document.getElementById("mensajes");
    const zonaResultado = document.getElementById("resultado");

    const expresion = document.getElementById("expresion")?.value ?? "";
    const valor = document.getElementById("valorVariable")?.value ?? "";

    if (!expresion.trim()) {
        mensajes.mostrar(zonaMensajes, "Escriba una expresion para calcular.", "atencion");
        return;
    }

    mensajes.limpiar(zonaMensajes);

    try {
        const respuesta = await evaluarExpresion({ expresion, variable: "x", valor });
        mostrarResultado(zonaResultado, respuesta);

    } catch (error) {
        mensajes.mostrarError(zonaMensajes, error);
        zonaResultado.innerHTML = "";
    }
}

/**
 * Dibuja el resultado de la evaluacion.
 *
 * @param {HTMLElement} contenedor donde se dibuja
 * @param {object} respuesta lo que devolvio el servidor
 */
function mostrarResultado(contenedor, respuesta) {
    contenedor.innerHTML = "";

    const caja = document.createElement("div");
    caja.className = "resultado";

    const etiqueta = document.createElement("div");
    etiqueta.className = "resultado__etiqueta";
    etiqueta.textContent = respuesta.evaluadaEn === null
        ? "Resultado"
        : `Resultado en x = ${respuesta.evaluadaEn}`;

    const valor = document.createElement("div");
    valor.className = "resultado__valor";
    valor.textContent = respuesta.valorTexto;

    caja.append(etiqueta, valor);

    // La forma exacta solo se muestra cuando aporta algo: repetir "0.5" como
    // "1/2" ayuda, pero repetir "3" como "3" es ruido.
    if (respuesta.valorExacto && respuesta.valorExacto !== respuesta.valorTexto) {
        const exacto = document.createElement("div");
        exacto.className = "resultado__exacto";
        exacto.textContent = `= ${respuesta.valorExacto}`;
        caja.appendChild(exacto);
    }

    const detalles = document.createElement("div");
    detalles.className = "resultado__detalles";

    agregarDato(detalles, "Se interpreto como", embellecer(respuesta.interpretacion));

    if (respuesta.desarrollado && respuesta.desarrollado !== respuesta.interpretacion) {
        agregarDato(detalles, "Desarrollado", embellecer(respuesta.desarrollado));
    }

    caja.appendChild(detalles);
    contenedor.appendChild(caja);
}

/** Agrega un par etiqueta/valor al bloque de detalles. */
function agregarDato(contenedor, etiqueta, valor) {
    const caja = document.createElement("div");
    caja.className = "dato";

    const nombre = document.createElement("span");
    nombre.className = "dato__etiqueta";
    nombre.textContent = etiqueta;

    const contenido = document.createElement("span");
    contenido.className = "dato__valor";
    contenido.textContent = valor;

    caja.append(nombre, contenido);
    contenedor.appendChild(caja);
}
