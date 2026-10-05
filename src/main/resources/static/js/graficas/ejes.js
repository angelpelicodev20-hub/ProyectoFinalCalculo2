/**
 * ejes.js
 * Traduce entre coordenadas matematicas y pixeles, y dibuja el plano.
 *
 * Es la base de toda grafica 2D del proyecto. El backend manda puntos en
 * coordenadas del ejercicio — x entre 0 y 4, y entre 0 y 2 — y el canvas
 * trabaja en pixeles, con el origen arriba a la izquierda y el eje vertical
 * apuntando hacia abajo. Esta clase hace la conversion en las dos
 * direcciones: una para dibujar, y la otra para saber sobre que punto de la
 * curva esta el puntero del raton.
 */

import { formatearEje } from "../util/formatoNumeros.js";

/** Espacio que se reserva alrededor del area de dibujo, para las etiquetas. */
const MARGEN = { arriba: 20, derecha: 20, abajo: 40, izquierda: 60 };

/** Separacion minima en pixeles entre dos lineas de la rejilla. */
const SEPARACION_MINIMA = 55;

/**
 * Sistema de coordenadas de una grafica.
 */
export class SistemaCoordenadas {

    /**
     * @param {HTMLCanvasElement} lienzo  canvas donde se dibuja
     * @param {object} ventana  rango visible: xMinimo, xMaximo, yMinimo, yMaximo
     */
    constructor(lienzo, ventana) {
        this.lienzo = lienzo;
        this.ventana = { ...ventana };

        // El ancho y el alto se toman del tamano en pixeles CSS, no del
        // atributo width del canvas: ese ultimo esta multiplicado por la
        // densidad de la pantalla para que el trazo salga nitido.
        this.ancho = lienzo.clientWidth;
        this.alto = lienzo.clientHeight;

        this.areaIzquierda = MARGEN.izquierda;
        this.areaArriba = MARGEN.arriba;
        this.areaAncho = Math.max(1, this.ancho - MARGEN.izquierda - MARGEN.derecha);
        this.areaAlto = Math.max(1, this.alto - MARGEN.arriba - MARGEN.abajo);
    }

    /**
     * Convierte una coordenada x matematica a pixeles.
     *
     * @param {number} x coordenada matematica
     * @returns {number} la posicion horizontal en pixeles
     */
    aPixelX(x) {
        const proporcion = (x - this.ventana.xMinimo)
            / (this.ventana.xMaximo - this.ventana.xMinimo);
        return this.areaIzquierda + proporcion * this.areaAncho;
    }

    /**
     * Convierte una coordenada y matematica a pixeles.
     *
     * Se resta a la altura porque en el canvas el eje vertical crece hacia
     * abajo, al contrario que en el plano cartesiano.
     *
     * @param {number} y coordenada matematica
     * @returns {number} la posicion vertical en pixeles
     */
    aPixelY(y) {
        const proporcion = (y - this.ventana.yMinimo)
            / (this.ventana.yMaximo - this.ventana.yMinimo);
        return this.areaArriba + this.areaAlto - proporcion * this.areaAlto;
    }

    /**
     * Convierte una posicion horizontal en pixeles a coordenada matematica.
     *
     * @param {number} pixel posicion en pixeles
     * @returns {number} la coordenada x
     */
    aMatematicoX(pixel) {
        const proporcion = (pixel - this.areaIzquierda) / this.areaAncho;
        return this.ventana.xMinimo
            + proporcion * (this.ventana.xMaximo - this.ventana.xMinimo);
    }

    /**
     * Convierte una posicion vertical en pixeles a coordenada matematica.
     *
     * @param {number} pixel posicion en pixeles
     * @returns {number} la coordenada y
     */
    aMatematicoY(pixel) {
        const proporcion = (this.areaArriba + this.areaAlto - pixel) / this.areaAlto;
        return this.ventana.yMinimo
            + proporcion * (this.ventana.yMaximo - this.ventana.yMinimo);
    }

    /**
     * Indica si un punto en pixeles cae dentro del area de dibujo.
     *
     * @param {number} px posicion horizontal
     * @param {number} py posicion vertical
     * @returns {boolean} true si esta dentro
     */
    estaDentro(px, py) {
        return px >= this.areaIzquierda
            && px <= this.areaIzquierda + this.areaAncho
            && py >= this.areaArriba
            && py <= this.areaArriba + this.areaAlto;
    }

    /** Recorta el dibujo al area util, para que nada se salga del marco. */
    recortarAlArea(contexto) {
        contexto.beginPath();
        contexto.rect(this.areaIzquierda, this.areaArriba, this.areaAncho, this.areaAlto);
        contexto.clip();
    }
}

/**
 * Prepara el canvas para dibujar y devuelve su contexto.
 *
 * Ajusta el tamano interno del canvas a la densidad de la pantalla. Sin
 * esto, en un monitor de alta resolucion las lineas se ven borrosas, porque
 * el navegador estira una imagen de menos pixeles de los que la pantalla
 * puede mostrar.
 *
 * @param {HTMLCanvasElement} lienzo canvas a preparar
 * @returns {CanvasRenderingContext2D} el contexto listo para dibujar
 */
export function prepararLienzo(lienzo) {
    const densidad = window.devicePixelRatio || 1;
    const ancho = lienzo.clientWidth;
    const alto = lienzo.clientHeight;

    lienzo.width = Math.round(ancho * densidad);
    lienzo.height = Math.round(alto * densidad);

    const contexto = lienzo.getContext("2d");
    contexto.setTransform(densidad, 0, 0, densidad, 0, 0);
    contexto.clearRect(0, 0, ancho, alto);

    return contexto;
}

/**
 * Lee un color definido en el CSS.
 *
 * Consultarlo aqui, y no escribirlo a mano en el JavaScript, es lo que hace
 * que la grafica cambie sola al pasar al tema oscuro: los colores viven en
 * variables.css y se leen en el momento de dibujar.
 *
 * @param {string} nombre nombre de la variable, sin los dos guiones
 * @returns {string} el color
 */
export function color(nombre) {
    return getComputedStyle(document.documentElement)
        .getPropertyValue(`--${nombre}`)
        .trim();
}

/**
 * Elige cada cuanto poner una marca en el eje.
 *
 * Las marcas deben caer en numeros redondos — 1, 2, 5, 10, 20, 50... — y no
 * en valores como 0.37, que serian correctos pero ilegibles. Se toma el paso
 * ideal segun el espacio disponible y se redondea al numero comodo mas
 * cercano por encima.
 *
 * @param {number} rango    ancho del intervalo visible
 * @param {number} pixeles  espacio disponible en pixeles
 * @returns {number} la separacion entre marcas, en unidades matematicas
 */
export function calcularPaso(rango, pixeles) {
    const marcasPosibles = Math.max(2, Math.floor(pixeles / SEPARACION_MINIMA));
    const pasoIdeal = rango / marcasPosibles;

    // Se separa el orden de magnitud para trabajar solo con la mantisa.
    const magnitud = Math.pow(10, Math.floor(Math.log10(pasoIdeal)));
    const mantisa = pasoIdeal / magnitud;

    let mantisaRedondeada;
    if (mantisa <= 1) {
        mantisaRedondeada = 1;
    } else if (mantisa <= 2) {
        mantisaRedondeada = 2;
    } else if (mantisa <= 5) {
        mantisaRedondeada = 5;
    } else {
        mantisaRedondeada = 10;
    }

    return mantisaRedondeada * magnitud;
}

/**
 * Dibuja la rejilla de fondo.
 *
 * @param {CanvasRenderingContext2D} contexto contexto del canvas
 * @param {SistemaCoordenadas} sistema sistema de coordenadas
 */
export function dibujarRejilla(contexto, sistema) {
    const { ventana } = sistema;
    const pasoX = calcularPaso(ventana.xMaximo - ventana.xMinimo, sistema.areaAncho);
    const pasoY = calcularPaso(ventana.yMaximo - ventana.yMinimo, sistema.areaAlto);

    contexto.save();
    contexto.strokeStyle = color("grafica-rejilla");
    contexto.lineWidth = 1;

    // Lineas verticales.
    const primeraX = Math.ceil(ventana.xMinimo / pasoX) * pasoX;
    for (let x = primeraX; x <= ventana.xMaximo; x += pasoX) {
        const px = sistema.aPixelX(x);
        contexto.beginPath();
        contexto.moveTo(px, sistema.areaArriba);
        contexto.lineTo(px, sistema.areaArriba + sistema.areaAlto);
        contexto.stroke();
    }

    // Lineas horizontales.
    const primeraY = Math.ceil(ventana.yMinimo / pasoY) * pasoY;
    for (let y = primeraY; y <= ventana.yMaximo; y += pasoY) {
        const py = sistema.aPixelY(y);
        contexto.beginPath();
        contexto.moveTo(sistema.areaIzquierda, py);
        contexto.lineTo(sistema.areaIzquierda + sistema.areaAncho, py);
        contexto.stroke();
    }

    contexto.restore();
}

/**
 * Dibuja los ejes X e Y con sus marcas y numeros.
 *
 * @param {CanvasRenderingContext2D} contexto contexto del canvas
 * @param {SistemaCoordenadas} sistema sistema de coordenadas
 */
export function dibujarEjes(contexto, sistema) {
    const { ventana } = sistema;
    const colorEjes = color("grafica-ejes");

    contexto.save();
    contexto.strokeStyle = colorEjes;
    contexto.fillStyle = colorEjes;
    contexto.lineWidth = 1.5;
    contexto.font = "11px system-ui, sans-serif";

    // Si el cero no entra en la ventana, el eje se dibuja pegado al borde
    // para que las etiquetas sigan teniendo donde apoyarse.
    const yDelEje = Math.min(
        Math.max(sistema.aPixelY(0), sistema.areaArriba),
        sistema.areaArriba + sistema.areaAlto
    );
    const xDelEje = Math.min(
        Math.max(sistema.aPixelX(0), sistema.areaIzquierda),
        sistema.areaIzquierda + sistema.areaAncho
    );

    // Eje horizontal.
    contexto.beginPath();
    contexto.moveTo(sistema.areaIzquierda, yDelEje);
    contexto.lineTo(sistema.areaIzquierda + sistema.areaAncho, yDelEje);
    contexto.stroke();

    // Eje vertical.
    contexto.beginPath();
    contexto.moveTo(xDelEje, sistema.areaArriba);
    contexto.lineTo(xDelEje, sistema.areaArriba + sistema.areaAlto);
    contexto.stroke();

    // Numeros del eje horizontal.
    const pasoX = calcularPaso(ventana.xMaximo - ventana.xMinimo, sistema.areaAncho);
    contexto.textAlign = "center";
    contexto.textBaseline = "top";

    const primeraX = Math.ceil(ventana.xMinimo / pasoX) * pasoX;
    for (let x = primeraX; x <= ventana.xMaximo; x += pasoX) {
        if (Math.abs(x) < pasoX * 1e-6) {
            continue;
        }
        const px = sistema.aPixelX(x);
        contexto.beginPath();
        contexto.moveTo(px, yDelEje);
        contexto.lineTo(px, yDelEje + 5);
        contexto.stroke();
        contexto.fillText(formatearEje(x), px, yDelEje + 8);
    }

    // Numeros del eje vertical.
    const pasoY = calcularPaso(ventana.yMaximo - ventana.yMinimo, sistema.areaAlto);
    contexto.textAlign = "right";
    contexto.textBaseline = "middle";

    const primeraY = Math.ceil(ventana.yMinimo / pasoY) * pasoY;
    for (let y = primeraY; y <= ventana.yMaximo; y += pasoY) {
        if (Math.abs(y) < pasoY * 1e-6) {
            continue;
        }
        const py = sistema.aPixelY(y);
        contexto.beginPath();
        contexto.moveTo(xDelEje - 5, py);
        contexto.lineTo(xDelEje, py);
        contexto.stroke();
        contexto.fillText(formatearEje(y), xDelEje - 8, py);
    }

    // El origen y los nombres de los ejes.
    contexto.textAlign = "right";
    contexto.textBaseline = "top";
    contexto.fillText("0", xDelEje - 6, yDelEje + 6);

    contexto.font = "italic 13px system-ui, sans-serif";
    contexto.textAlign = "right";
    contexto.textBaseline = "bottom";
    contexto.fillText("x", sistema.areaIzquierda + sistema.areaAncho, yDelEje - 6);

    contexto.textAlign = "left";
    contexto.textBaseline = "top";
    contexto.fillText("y", xDelEje + 6, sistema.areaArriba);

    contexto.restore();
}
