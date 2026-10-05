/**
 * evaluador.js
 * Evalua en el navegador las curvas que ya interpreto el servidor.
 *
 * La grafica interactiva necesita la funcion entera, no unos puntos sueltos:
 * al alejar o desplazar la vista aparecen zonas que no se habian calculado.
 * El servidor no manda el texto de la funcion para que aqui se vuelva a leer
 * (eso seria tener dos parsers que podrian entenderla distinto), sino el
 * arbol que ya construyo su parser. Este archivo solo lo recorre.
 *
 * Formato del arbol, igual que en ArbolJson.java:
 *
 *   {"t":"n","v":2}                      numero
 *   {"t":"v"}                            la variable
 *   {"t":"-","a":...}                    cambio de signo
 *   {"t":"b","o":"^","a":...,"b":...}    operacion
 *   {"t":"f","f":"sqrt","a":...}         funcion del catalogo
 */

/** Las mismas funciones del catalogo de Java. */
const FUNCIONES = {
    sin: Math.sin,
    sen: Math.sin,
    cos: Math.cos,
    tan: Math.tan,
    cot: (v) => 1 / Math.tan(v),
    sec: (v) => 1 / Math.cos(v),
    csc: (v) => 1 / Math.sin(v),
    asin: Math.asin,
    acos: Math.acos,
    atan: Math.atan,
    sinh: Math.sinh,
    cosh: Math.cosh,
    tanh: Math.tanh,
    exp: Math.exp,
    ln: Math.log,
    log: Math.log10,
    log10: Math.log10,
    sqrt: Math.sqrt,
    raiz: Math.sqrt,
    cbrt: Math.cbrt,
    abs: Math.abs,
    floor: Math.floor,
    ceil: Math.ceil,
    round: Math.round
};

/**
 * La potencia con la misma regla que el servidor: una raiz impar de un numero
 * negativo existe. Sin esto (-8)^(1/3) daria NaN aqui y -2 en Java, y la
 * curva se veria cortada donde el calculo si la usa.
 *
 * @param {number} base base
 * @param {number} exponente exponente
 * @returns {number} el resultado
 */
function potencia(base, exponente) {
    const resultado = Math.pow(base, exponente);
    if (!Number.isNaN(resultado) || base >= 0) {
        return resultado;
    }
    const inverso = 1 / exponente;
    const redondeado = Math.round(inverso);
    const esRaizImpar = Math.abs(inverso - redondeado) < 1e-9 && Math.abs(redondeado) % 2 === 1;
    return esRaizImpar ? -Math.pow(-base, exponente) : NaN;
}

/**
 * Convierte el arbol en una funcion de JavaScript.
 *
 * Se compila una sola vez en un cierre por nodo, en lugar de recorrer el
 * arbol en cada evaluacion: al dibujar se evalua miles de veces por cuadro.
 *
 * @param {object} arbol arbol de la expresion
 * @returns {(v: number) => number} la funcion de la variable
 */
export function compilar(arbol) {
    if (!arbol || typeof arbol !== "object") {
        return () => NaN;
    }
    switch (arbol.t) {
        case "n": {
            const valor = Number(arbol.v);
            return () => valor;
        }
        case "v":
            return (v) => v;
        case "-": {
            const operando = compilar(arbol.a);
            return (v) => -operando(v);
        }
        case "b": {
            const a = compilar(arbol.a);
            const b = compilar(arbol.b);
            switch (arbol.o) {
                case "+": return (v) => a(v) + b(v);
                case "-": return (v) => a(v) - b(v);
                case "*": return (v) => a(v) * b(v);
                case "/": return (v) => a(v) / b(v);
                case "%": return (v) => a(v) % b(v);
                case "^": return (v) => potencia(a(v), b(v));
                default: return () => NaN;
            }
        }
        case "f": {
            const funcion = FUNCIONES[arbol.f];
            const argumento = compilar(arbol.a);
            return funcion ? (v) => funcion(argumento(v)) : () => NaN;
        }
        default:
            return () => NaN;
    }
}
