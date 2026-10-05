/**
 * tecladoMatematico.js
 * Botones que insertan simbolos en el campo de la funcion.
 *
 * Escribir "sqrt(" o recordar que la potencia va con el acento circunflejo
 * no es evidente para quien usa la aplicacion por primera vez. Estos botones
 * lo resuelven, y ademas dejan el cursor colocado dentro del parentesis para
 * que se pueda seguir escribiendo sin tocar el raton.
 */

/**
 * Teclas disponibles.
 *
 * `insercion` es lo que se escribe en el campo, y la barra vertical marca
 * donde debe quedar el cursor despues de insertarlo.
 */
const TECLAS = [
    { etiqueta: "x",   insercion: "x" },
    { etiqueta: "x²",  insercion: "x^2" },
    { etiqueta: "xⁿ",  insercion: "x^|" },
    { etiqueta: "√",   insercion: "sqrt(|)" },
    { etiqueta: "π",   insercion: "pi" },
    { etiqueta: "e",   insercion: "e" },

    { etiqueta: "sin", insercion: "sin(|)" },
    { etiqueta: "cos", insercion: "cos(|)" },
    { etiqueta: "tan", insercion: "tan(|)" },
    { etiqueta: "ln",  insercion: "ln(|)" },
    { etiqueta: "log", insercion: "log(|)" },
    { etiqueta: "eˣ",  insercion: "exp(|)" },

    { etiqueta: "( )", insercion: "(|)" },
    { etiqueta: "+",   insercion: " + " },
    { etiqueta: "−",   insercion: " - " },
    { etiqueta: "×",   insercion: " * " },
    { etiqueta: "÷",   insercion: " / " },
    { etiqueta: "|x|", insercion: "abs(|)" }
];

/**
 * Construye el teclado y lo conecta con un campo de texto.
 *
 * @param {HTMLElement} contenedor donde se dibujan los botones
 * @param {HTMLInputElement} campo campo de la funcion
 * @param {Function} [alCambiar] se llama despues de cada insercion
 */
export function construir(contenedor, campo, alCambiar) {
    if (!contenedor || !campo) {
        return;
    }

    contenedor.innerHTML = "";
    contenedor.className = "teclado-matematico";

    for (const tecla of TECLAS) {
        const boton = document.createElement("button");
        boton.type = "button";
        boton.className = "tecla";
        boton.textContent = tecla.etiqueta;
        boton.title = `Insertar ${tecla.insercion.replace("|", "")}`;

        boton.addEventListener("click", () => {
            insertar(campo, tecla.insercion);
            if (typeof alCambiar === "function") {
                alCambiar();
            }
        });

        contenedor.appendChild(boton);
    }
}

/**
 * Inserta texto en la posicion del cursor.
 *
 * Si hay texto seleccionado, lo reemplaza. Si la insercion trae una barra
 * vertical, el cursor queda ahi; si no, queda al final de lo insertado.
 *
 * @param {HTMLInputElement} campo campo donde se escribe
 * @param {string} texto texto a insertar, con "|" marcando el cursor
 */
export function insertar(campo, texto) {
    const inicio = campo.selectionStart ?? campo.value.length;
    const fin = campo.selectionEnd ?? campo.value.length;

    const posicionDelCursor = texto.indexOf("|");
    const insercion = texto.replace("|", "");

    const antes = campo.value.slice(0, inicio);
    const despues = campo.value.slice(fin);

    campo.value = antes + insercion + despues;

    const nuevaPosicion = posicionDelCursor >= 0
        ? inicio + posicionDelCursor
        : inicio + insercion.length;

    campo.focus();
    campo.setSelectionRange(nuevaPosicion, nuevaPosicion);

    // El evento se dispara a mano porque asignar .value desde JavaScript no
    // lo genera solo, y la vista previa esta escuchandolo.
    campo.dispatchEvent(new Event("input", { bubbles: true }));
}

/**
 * Llena una lista de ejemplos que se cargan con un clic.
 *
 * Sirven para probar la aplicacion sin tener que pensar en un ejercicio, y
 * para mostrar de entrada como se escriben las funciones.
 *
 * @param {HTMLElement} contenedor donde se dibujan las pastillas
 * @param {Array<object>} ejemplos lista de ejemplos a ofrecer
 * @param {Function} alElegir se llama con el ejemplo elegido
 */
export function construirEjemplos(contenedor, ejemplos, alElegir) {
    if (!contenedor) {
        return;
    }

    contenedor.innerHTML = "";
    contenedor.className = "pastillas";

    for (const ejemplo of ejemplos) {
        const boton = document.createElement("button");
        boton.type = "button";
        boton.className = "pastilla";
        boton.textContent = ejemplo.etiqueta;

        if (ejemplo.descripcion) {
            boton.title = ejemplo.descripcion;
        }

        boton.addEventListener("click", () => alElegir(ejemplo));
        contenedor.appendChild(boton);
    }
}
