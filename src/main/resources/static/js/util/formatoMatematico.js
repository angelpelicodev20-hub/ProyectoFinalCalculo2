/**
 * formatoMatematico.js
 * Convierte las expresiones de texto plano en algo agradable de leer.
 *
 * El backend trabaja con texto plano — "x^2", "pi", "sqrt(x)" — porque es
 * lo que el usuario escribe y lo que el parser entiende. En pantalla, en
 * cambio, se lee mejor "x²", "π" y "√(x)". Estas funciones hacen esa
 * traduccion, que es solo de presentacion: nunca se manda de vuelta al
 * servidor lo que sale de aqui.
 */

/** Como se dibuja cada digito cuando va en un exponente. */
const SUPERINDICES = {
    "0": "⁰", "1": "¹", "2": "²", "3": "³", "4": "⁴",
    "5": "⁵", "6": "⁶", "7": "⁷", "8": "⁸", "9": "⁹",
    "+": "⁺", "-": "⁻", "n": "ⁿ"
};

/**
 * Da formato a una expresion para mostrarla.
 *
 * @param {string} expresion expresion en texto plano
 * @returns {string} la expresion con simbolos matematicos
 */
export function embellecer(expresion) {
    if (!expresion) {
        return "";
    }

    let texto = String(expresion);

    // Constantes y funciones con simbolo propio.
    // Las funciones se reconocen aunque vayan pegadas a un coeficiente, como en
    // 4sqrt(2) o 2pi: lo que importa es que no las preceda otra letra.
    texto = texto.replace(/(?<![a-zA-Z])pi(?![a-zA-Z])/g, "π");
    texto = texto.replace(/(?<![a-zA-Z])sqrt\(/g, "√(");
    texto = texto.replace(/(?<![a-zA-Z])raiz\(/g, "√(");
    texto = texto.replace(/(?<![a-zA-Z])cbrt\(/g, "∛(");
    texto = texto.replace(/\+-/g, "±");

    // Exponentes de un solo termino: x^2 pasa a x².
    texto = texto.replace(/\^(-?\d+)/g, (coincidencia, exponente) => {
        const convertido = [...exponente]
            .map((caracter) => SUPERINDICES[caracter] ?? null)
            .join("");
        return convertido.includes("null") ? coincidencia : convertido;
    });

    // Operadores con su simbolo tipografico.
    texto = texto.replace(/\*/g, "·");
    texto = texto.replace(/<=/g, "≤").replace(/>=/g, "≥");

    return texto;
}

/**
 * Da formato a la expresion de un paso de la solucion.
 *
 * Se diferencia de {@link embellecer} en que respeta los saltos de linea
 * y traduce las palabras que el backend escribe en texto corrido, como
 * "integral de a a b".
 *
 * @param {string} expresion expresion del paso
 * @returns {string} la expresion lista para mostrarse
 */
export function embellecerPaso(expresion) {
    if (!expresion) {
        return "";
    }

    return String(expresion)
        .split("\n")
        // Solo se cambia "integral" por el simbolo cuando es la formula —
        // "integral de 0 a 2 de ..."—, no cuando es la palabra dentro de una
        // explicacion: "habria que partir la integral" debe seguir diciendo eso.
        .map((linea) => embellecer(linea)
            .replace(/\bintegral(?= de [-\d.(\[])/g, "∫"))
        .join("\n");
}

/**
 * Escribe la integral definida completa, con sus limites.
 *
 * @param {string} funcion    funcion que se integra
 * @param {number|string} a   limite inferior
 * @param {number|string} b   limite superior
 * @param {string} [variable="x"] variable de integracion
 * @returns {string} la integral como texto
 */
export function escribirIntegral(funcion, a, b, variable = "x") {
    return `∫ desde ${a} hasta ${b} de (${embellecer(funcion)}) d${variable}`;
}

/**
 * Quita el "f(x) = " del principio de una expresion, si lo trae.
 * Sirve para mostrar solo la funcion en las etiquetas de la grafica.
 *
 * @param {string} etiqueta texto que puede traer el encabezado
 * @returns {string} la funcion sola
 */
export function soloLaFuncion(etiqueta) {
    if (!etiqueta) {
        return "";
    }
    const posicionIgual = etiqueta.indexOf("=");
    return posicionIgual >= 0
        ? etiqueta.slice(posicionIgual + 1).trim()
        : etiqueta.trim();
}
