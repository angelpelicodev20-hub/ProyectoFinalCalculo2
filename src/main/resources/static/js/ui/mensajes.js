/**
 * mensajes.js
 * Muestra errores, avisos y el indicador de que se esta calculando.
 *
 * Todos los mensajes de la aplicacion pasan por aqui para que se vean
 * iguales en las tres paginas, y sobre todo para que ninguno se quede
 * pegado en pantalla: antes de mostrar uno nuevo se limpia el anterior.
 */

/** Simbolo que acompana a cada clase de mensaje. */
const SIMBOLOS = {
    error: "✕",
    atencion: "!",
    exito: "✓",
    informacion: "i"
};

/**
 * Muestra un mensaje dentro de un contenedor.
 *
 * @param {HTMLElement} contenedor donde se escribe el mensaje
 * @param {string} texto contenido del mensaje
 * @param {string} [clase="informacion"] error, atencion, exito o informacion
 * @param {string} [titulo=""] encabezado opcional en negrita
 */
export function mostrar(contenedor, texto, clase = "informacion", titulo = "") {
    if (!contenedor) {
        return;
    }

    contenedor.innerHTML = "";
    contenedor.classList.remove("oculto");

    const aviso = document.createElement("div");
    aviso.className = `aviso aviso--${clase}`;
    aviso.setAttribute("role", clase === "error" ? "alert" : "status");

    const icono = document.createElement("span");
    icono.className = "aviso__icono";
    icono.setAttribute("aria-hidden", "true");
    icono.textContent = SIMBOLOS[clase] ?? SIMBOLOS.informacion;

    const cuerpo = document.createElement("div");
    cuerpo.className = "aviso__texto";

    if (titulo) {
        const encabezado = document.createElement("strong");
        encabezado.textContent = titulo;
        cuerpo.appendChild(encabezado);
    }

    // Se usa textContent y no innerHTML: el mensaje puede contener la
    // funcion que escribio el usuario, y esa nunca debe interpretarse
    // como HTML.
    cuerpo.appendChild(document.createTextNode(texto));

    aviso.append(icono, cuerpo);
    contenedor.appendChild(aviso);
}

/**
 * Muestra el mensaje de un error devuelto por el servidor.
 *
 * Junta el mensaje principal con la sugerencia que trae el backend, que es
 * la parte que le dice al usuario que hacer a continuacion.
 *
 * @param {HTMLElement} contenedor donde se escribe el mensaje
 * @param {Error} error error capturado
 */
export function mostrarError(contenedor, error) {
    const texto = error?.sugerencia
        ? `${error.message} ${error.sugerencia}`
        : (error?.message ?? "Ocurrio un error inesperado.");

    mostrar(contenedor, texto, "error", "No se pudo calcular");
}

/**
 * Muestra una advertencia que no impide el calculo.
 *
 * Se usa para los casos en que el resultado es correcto pero necesita una
 * aclaracion: la curva cruzo el eje, las curvas se cortaron, el eje de giro
 * atraviesa la region.
 *
 * @param {HTMLElement} contenedor donde se escribe el mensaje
 * @param {string} texto contenido de la advertencia
 */
export function mostrarAdvertencia(contenedor, texto) {
    if (!texto || !String(texto).trim()) {
        limpiar(contenedor);
        return;
    }
    mostrar(contenedor, texto, "atencion", "Tenga en cuenta");
}

/**
 * Borra el mensaje que hubiera.
 *
 * @param {HTMLElement} contenedor contenedor a limpiar
 */
export function limpiar(contenedor) {
    if (!contenedor) {
        return;
    }
    contenedor.innerHTML = "";
    contenedor.classList.add("oculto");
}

/**
 * Muestra el indicador de que el calculo esta en marcha.
 *
 * @param {HTMLElement} contenedor donde se muestra
 * @param {string} [texto="Calculando..."] leyenda que acompana
 */
export function mostrarCargando(contenedor, texto = "Calculando...") {
    if (!contenedor) {
        return;
    }

    contenedor.innerHTML = "";
    contenedor.classList.remove("oculto");

    const caja = document.createElement("div");
    caja.className = "cargando";
    caja.setAttribute("role", "status");

    const rueda = document.createElement("div");
    rueda.className = "cargando__rueda";
    rueda.setAttribute("aria-hidden", "true");

    const leyenda = document.createElement("span");
    leyenda.textContent = texto;

    caja.append(rueda, leyenda);
    contenedor.appendChild(caja);
}

/**
 * Muestra el estado inicial, cuando todavia no se ha calculado nada.
 *
 * @param {HTMLElement} contenedor donde se muestra
 * @param {string} simbolo simbolo grande que encabeza el mensaje
 * @param {string} texto leyenda
 */
export function mostrarEstadoVacio(contenedor, simbolo, texto) {
    if (!contenedor) {
        return;
    }

    contenedor.innerHTML = "";
    contenedor.classList.remove("oculto");

    const caja = document.createElement("div");
    caja.className = "estado-vacio";

    const icono = document.createElement("span");
    icono.className = "estado-vacio__simbolo";
    icono.setAttribute("aria-hidden", "true");
    icono.textContent = simbolo;

    const leyenda = document.createElement("p");
    leyenda.textContent = texto;
    leyenda.style.margin = "0";

    caja.append(icono, leyenda);
    contenedor.appendChild(caja);
}

/**
 * Marca un campo del formulario como invalido y escribe el motivo debajo.
 *
 * @param {HTMLElement} campo campo del formulario
 * @param {HTMLElement} zonaError elemento donde va el mensaje
 * @param {string} mensaje motivo del rechazo; vacio para quitar la marca
 */
export function marcarCampoInvalido(campo, zonaError, mensaje) {
    if (campo) {
        campo.classList.toggle("campo__control--invalido", Boolean(mensaje));
        campo.setAttribute("aria-invalid", mensaje ? "true" : "false");
    }
    if (zonaError) {
        zonaError.textContent = mensaje ?? "";
    }
}
