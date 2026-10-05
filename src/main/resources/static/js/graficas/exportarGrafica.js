/**
 * exportarGrafica.js
 * Guarda la grafica como imagen o la manda a imprimir.
 *
 * Sirve para pegar la grafica en el informe que se entrega. El canvas ya
 * tiene la imagen dibujada, asi que basta con pedirle su contenido en PNG.
 */

/**
 * Descarga la grafica como archivo PNG.
 *
 * El canvas puede tener el fondo transparente, y una imagen transparente
 * pegada en un documento blanco se ve mal. Por eso se copia a un canvas
 * intermedio con el fondo ya pintado.
 *
 * @param {HTMLCanvasElement} lienzo canvas a exportar
 * @param {string} [nombreArchivo="grafica"] nombre del archivo, sin extension
 */
export function descargarComoImagen(lienzo, nombreArchivo = "grafica") {
    if (!lienzo) {
        return;
    }

    const copia = document.createElement("canvas");
    copia.width = lienzo.width;
    copia.height = lienzo.height;

    const contexto = copia.getContext("2d");

    const fondo = getComputedStyle(document.documentElement)
        .getPropertyValue("--grafica-fondo")
        .trim() || "#ffffff";

    contexto.fillStyle = fondo;
    contexto.fillRect(0, 0, copia.width, copia.height);
    contexto.drawImage(lienzo, 0, 0);

    const enlace = document.createElement("a");
    enlace.download = `${nombreArchivo}.png`;
    enlace.href = copia.toDataURL("image/png");
    enlace.click();
}

/**
 * Manda la pagina a imprimir.
 *
 * Las reglas de impresion de base.css se encargan de ocultar los menus y
 * los botones, de modo que en el papel queden solo el enunciado, la grafica
 * y la explicacion paso a paso.
 */
export function imprimir() {
    window.print();
}

/**
 * Copia la grafica al portapapeles.
 *
 * Es mas comodo que descargar el archivo cuando lo unico que se quiere es
 * pegarla en un documento. No todos los navegadores lo permiten, asi que la
 * funcion informa si lo consiguio.
 *
 * @param {HTMLCanvasElement} lienzo canvas a copiar
 * @returns {Promise<boolean>} true si se copio
 */
export async function copiarAlPortapapeles(lienzo) {
    if (!lienzo || !navigator.clipboard || !window.ClipboardItem) {
        return false;
    }

    try {
        const imagen = await new Promise((resolver) => lienzo.toBlob(resolver, "image/png"));
        if (!imagen) {
            return false;
        }

        await navigator.clipboard.write([
            new ClipboardItem({ "image/png": imagen })
        ]);
        return true;

    } catch (error) {
        return false;
    }
}
