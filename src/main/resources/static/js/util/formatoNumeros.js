/**
 * formatoNumeros.js
 * Da formato a los numeros antes de mostrarlos en pantalla.
 *
 * El backend ya redondea los resultados; aqui solo se decide como se
 * escriben, que no es lo mismo. Un area de 12 debe verse como "12" y no
 * como "12.000000", y un numero muy pequeno o muy grande se lee mejor en
 * notacion cientifica.
 */

/**
 * Escribe un numero para mostrarlo al usuario.
 *
 * @param {number} valor numero a escribir
 * @param {number} [decimales=6] maximo de decimales
 * @returns {string} el numero como texto
 */
export function formatear(valor, decimales = 6) {
    if (valor === null || valor === undefined || Number.isNaN(valor)) {
        return "indefinido";
    }
    if (!Number.isFinite(valor)) {
        return valor > 0 ? "+infinito" : "-infinito";
    }

    // Los enteros se escriben sin decimales.
    if (Number.isInteger(valor)) {
        return String(valor);
    }

    // Fuera de este rango, la notacion decimal se vuelve ilegible.
    const magnitud = Math.abs(valor);
    if (magnitud !== 0 && (magnitud < 1e-4 || magnitud >= 1e9)) {
        return valor.toExponential(4);
    }

    // Se recorta a los decimales pedidos y se quitan los ceros sobrantes.
    return parseFloat(valor.toFixed(decimales)).toString();
}

/**
 * Escribe un numero con una cantidad fija de decimales, sin quitar ceros.
 * Se usa en las tablas, donde las columnas deben quedar alineadas.
 *
 * @param {number} valor numero a escribir
 * @param {number} [decimales=4] decimales fijos
 * @returns {string} el numero como texto
 */
export function formatearFijo(valor, decimales = 4) {
    if (valor === null || valor === undefined || !Number.isFinite(valor)) {
        return "—";
    }
    return valor.toFixed(decimales);
}

/**
 * Escribe un numero para las etiquetas de los ejes de la grafica.
 *
 * Ahi el espacio es escaso, asi que se usan pocos decimales y se
 * aprovecha que las marcas del eje suelen caer en valores redondos.
 *
 * @param {number} valor numero a escribir
 * @returns {string} el numero como texto
 */
export function formatearEje(valor) {
    if (Math.abs(valor) < 1e-10) {
        return "0";
    }
    const magnitud = Math.abs(valor);
    if (magnitud < 0.001 || magnitud >= 100000) {
        return valor.toExponential(1);
    }
    if (Number.isInteger(valor)) {
        return String(valor);
    }
    return parseFloat(valor.toFixed(3)).toString();
}

/**
 * Expresa un numero como multiplo de pi cuando el multiplo sale limpio.
 *
 * En clase los volumenes se dejan en la forma "8π" y no en "25.1327", asi
 * que mostrar las dos versiones permite comparar con el resultado hecho a
 * mano.
 *
 * @param {number} valor numero a examinar
 * @returns {string} el multiplo de pi, o cadena vacia si no sale limpio
 */
export function comoMultiploDePi(valor) {
    if (!Number.isFinite(valor) || Math.abs(valor) < 1e-12) {
        return "";
    }
    const cociente = valor / Math.PI;

    if (Math.abs(cociente - Math.round(cociente)) < 1e-7) {
        const entero = Math.round(cociente);
        return entero === 1 ? "π" : `${entero}π`;
    }

    for (let denominador = 2; denominador <= 32; denominador++) {
        const numerador = cociente * denominador;
        if (Math.abs(numerador - Math.round(numerador)) < 1e-7) {
            return `${Math.round(numerador)}π/${denominador}`;
        }
    }
    return "";
}

/**
 * Escribe una duracion en milisegundos de forma legible.
 *
 * @param {number} milisegundos duracion
 * @returns {string} la duracion como texto
 */
export function formatearDuracion(milisegundos) {
    if (milisegundos < 1000) {
        return `${milisegundos} ms`;
    }
    return `${(milisegundos / 1000).toFixed(2)} s`;
}
