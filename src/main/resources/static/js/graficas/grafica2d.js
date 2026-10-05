/**
 * grafica2d.js
 * Grafica plana interactiva, al estilo de GeoGebra.
 *
 * Se arrastra para desplazar la vista, la rueda acerca y aleja hacia el
 * puntero, se pellizca en pantallas tactiles, y la columna de botones de la
 * derecha vuelve a la vista inicial, acerca, aleja o abre la pantalla
 * completa. Al pasar el puntero por una curva se lee el punto exacto sobre
 * ella, y al pasarlo por un punto de corte se lee su coordenada.
 *
 * Las curvas no son puntos fijos: llegan como arboles ya interpretados por
 * el servidor (ArbolJson.java) y se evaluan aqui en la zona que se este
 * viendo, de modo que al alejar la vista la curva sigue, como en GeoGebra.
 * La region sombreada, en cambio, si es fija: es la del ejercicio, entre los
 * limites con que se resolvio.
 *
 * Las curvas de la forma x = g(y) se dibujan en su sitio: x = y^2 es una
 * parabola que abre hacia la derecha.
 */

import { prepararLienzo, color } from "./ejes.js";
import { compilar } from "./evaluador.js";
import { formatearEje } from "../util/formatoNumeros.js";
import { embellecer } from "../util/formatoMatematico.js";

/** Separacion aproximada, en pixeles, entre dos lineas principales de la rejilla. */
const SEPARACION_REJILLA = 85;

/** A cuantos pixeles de una curva se considera que el puntero esta sobre ella. */
const DISTANCIA_DE_LECTURA = 10;

/** Factor de cada paso de la rueda o de los botones. */
const FACTOR_ZOOM = 1.18;

/** Los iconos de los botones, los mismos del visor 3D. */
const ICONOS = {
    inicio: '<path d="M3 11.5 12 4l9 7.5"/><path d="M5.5 10v9.5h13V10"/>',
    acercar: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m15.5 15.5 5 5M10.5 7.5v6M7.5 10.5h6"/>',
    alejar: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m15.5 15.5 5 5M7.5 10.5h6"/>',
    pantalla: '<path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/>'
};

// ==================================================================
// ENTRADA
// ==================================================================

/**
 * Dibuja una grafica en el canvas y la deja interactiva.
 *
 * Si se vuelve a llamar con los mismos datos (al cambiar el tamano de la
 * ventana o el tema), se conserva la vista que haya dejado el estudiante; con
 * datos nuevos se vuelve a encuadrar la region.
 *
 * @param {HTMLCanvasElement} lienzo canvas donde dibujar
 * @param {object} datos datos de la grafica que devolvio el backend
 * @param {object} [opciones] ajustes de dibujo
 * @param {boolean} [opciones.sombrearRegion=true] pintar la region de la integral
 * @param {boolean} [opciones.mostrarRectangulos=true] dibujar los rectangulos de Riemann
 * @returns {Visor2D|null} el visor, o null si no hay donde dibujar
 */
export function dibujar(lienzo, datos, opciones = {}) {
    if (!lienzo || !datos) {
        return null;
    }
    let visor = lienzo.__visor2d;
    if (!visor) {
        visor = new Visor2D(lienzo);
        lienzo.__visor2d = visor;
    }
    visor.mostrar(datos, opciones);
    return visor;
}

/**
 * Indica en que elemento escribir la lectura de coordenadas.
 *
 * Se conserva por compatibilidad con las paginas: el visor ya busca solo el
 * recuadro de lectura que tenga al lado.
 *
 * @param {HTMLCanvasElement} lienzo canvas de la grafica
 * @param {HTMLElement} lectura elemento donde se escribe la coordenada
 */
export function conectarLectura(lienzo, lectura) {
    if (lienzo?.__visor2d && lectura) {
        lienzo.__visor2d.lectura = lectura;
    }
}

// ==================================================================
// EL VISOR
// ==================================================================

/**
 * Una grafica plana con su camara: centro de la vista y pixeles por unidad.
 *
 * Guardar la vista como centro y escala, y no como rango visible, hace que al
 * cambiar el tamano del canvas (pantalla completa, girar el telefono) la
 * grafica no se deforme: se ve mas o menos plano, pero a la misma escala.
 */
class Visor2D {

    constructor(lienzo) {
        this.lienzo = lienzo;
        this.caja = lienzo.parentElement;
        this.lectura = this.caja?.querySelector(".grafica__lectura") ?? null;

        this.datos = null;
        this.opciones = {};
        this.curvas = [];

        /** Centro de la vista y escala: px = ancho/2 + (x - cx) * sx. */
        this.cx = 0;
        this.cy = 0;
        this.sx = 40;
        this.sy = 40;
        this.encuadrada = false;

        this.raton = null;
        this.punteros = new Map();
        this.arrastre = null;
        this.pellizco = null;
        this.cuadroPendiente = false;

        lienzo.style.touchAction = "none";
        this.conectarEventos();
        this.colocarBotones();
    }

    // ------------------------------------------------------------------
    // DATOS
    // ------------------------------------------------------------------

    /**
     * Muestra unos datos nuevos, o redibuja los mismos.
     *
     * @param {object} datos datos de la grafica
     * @param {object} opciones ajustes de dibujo
     */
    mostrar(datos, opciones) {
        const sonLosMismos = datos === this.datos;
        this.datos = datos;
        this.opciones = { sombrearRegion: true, mostrarRectangulos: true, ...opciones };

        if (!sonLosMismos) {
            this.curvas = prepararCurvas(datos);
            this.encuadrada = false;
        }
        this.redibujar();
    }

    /** Encuadra la region del ejercicio, con la misma escala en los dos ejes si se puede. */
    encuadrar() {
        const ancho = this.lienzo.clientWidth;
        const alto = this.lienzo.clientHeight;
        if (ancho <= 0 || alto <= 0 || !this.datos) {
            return;
        }
        const caja = cajaDeInteres(this.datos, this.curvas);
        const anchoCaja = Math.max(caja.xMax - caja.xMin, 1e-6);
        const altoCaja = Math.max(caja.yMax - caja.yMin, 1e-6);

        let sx = ancho / anchoCaja;
        let sy = alto / altoCaja;

        // GeoGebra mantiene la misma escala en los dos ejes, y asi una
        // circunferencia se ve redonda. Solo si eso dejara la region aplastada
        // contra un borde se usan escalas distintas.
        const proporcion = Math.max(sx, sy) / Math.min(sx, sy);
        if (proporcion <= 5) {
            sx = sy = Math.min(sx, sy);
        }

        this.cx = (caja.xMin + caja.xMax) / 2;
        this.cy = (caja.yMin + caja.yMax) / 2;
        this.sx = sx;
        this.sy = sy;
        this.encuadrada = true;
    }

    // ------------------------------------------------------------------
    // COORDENADAS
    // ------------------------------------------------------------------

    aPixelX(x) {
        return this.ancho / 2 + (x - this.cx) * this.sx;
    }

    aPixelY(y) {
        return this.alto / 2 - (y - this.cy) * this.sy;
    }

    aMatematicoX(px) {
        return this.cx + (px - this.ancho / 2) / this.sx;
    }

    aMatematicoY(py) {
        return this.cy - (py - this.alto / 2) / this.sy;
    }

    /** El rango visible de cada eje. */
    rango() {
        return {
            xMin: this.aMatematicoX(0),
            xMax: this.aMatematicoX(this.ancho),
            yMin: this.aMatematicoY(this.alto),
            yMax: this.aMatematicoY(0)
        };
    }

    // ------------------------------------------------------------------
    // CAMARA
    // ------------------------------------------------------------------

    /**
     * Acerca o aleja manteniendo fijo el punto bajo el puntero.
     *
     * @param {number} factor mayor que 1 para acercar
     * @param {number} [px] posicion del puntero; por defecto el centro
     * @param {number} [py] posicion del puntero
     */
    acercar(factor, px = this.ancho / 2, py = this.alto / 2) {
        const x = this.aMatematicoX(px);
        const y = this.aMatematicoY(py);
        const nuevoSx = this.sx * factor;
        const nuevoSy = this.sy * factor;
        if (nuevoSx > 1e7 || nuevoSx < 1e-6 || nuevoSy > 1e7 || nuevoSy < 1e-6) {
            return;
        }
        this.sx = nuevoSx;
        this.sy = nuevoSy;
        this.cx = x - (px - this.ancho / 2) / this.sx;
        this.cy = y + (py - this.alto / 2) / this.sy;
    }

    /** Desplaza la vista lo que se movio el puntero. */
    desplazar(dx, dy) {
        this.cx -= dx / this.sx;
        this.cy += dy / this.sy;
    }

    // ------------------------------------------------------------------
    // EVENTOS
    // ------------------------------------------------------------------

    conectarEventos() {
        const lienzo = this.lienzo;

        lienzo.addEventListener("pointerdown", (evento) => {
            lienzo.setPointerCapture?.(evento.pointerId);
            this.punteros.set(evento.pointerId, posicion(lienzo, evento));
            if (this.punteros.size === 1) {
                this.arrastre = { ...posicion(lienzo, evento), movido: false };
                lienzo.style.cursor = "grabbing";
            } else if (this.punteros.size === 2) {
                this.arrastre = null;
                this.pellizco = distanciaYCentro([...this.punteros.values()]);
            }
        });

        lienzo.addEventListener("pointermove", (evento) => {
            const actual = posicion(lienzo, evento);
            if (this.punteros.has(evento.pointerId)) {
                this.punteros.set(evento.pointerId, actual);
            }

            if (this.pellizco && this.punteros.size === 2) {
                const ahora = distanciaYCentro([...this.punteros.values()]);
                if (this.pellizco.distancia > 0) {
                    this.acercar(ahora.distancia / this.pellizco.distancia, ahora.x, ahora.y);
                }
                this.desplazar(ahora.x - this.pellizco.x, ahora.y - this.pellizco.y);
                this.pellizco = ahora;
                this.pedirCuadro();
                return;
            }

            if (this.arrastre) {
                const dx = actual.x - this.arrastre.x;
                const dy = actual.y - this.arrastre.y;
                if (Math.abs(dx) + Math.abs(dy) > 0) {
                    this.desplazar(dx, dy);
                    this.arrastre = { ...actual, movido: true };
                    this.raton = null;
                    this.pedirCuadro();
                }
                return;
            }

            this.raton = actual;
            this.pedirCuadro();
        });

        const soltar = (evento) => {
            this.punteros.delete(evento.pointerId);
            if (this.punteros.size < 2) {
                this.pellizco = null;
            }
            if (this.punteros.size === 0) {
                this.arrastre = null;
                lienzo.style.cursor = "";
            }
        };
        lienzo.addEventListener("pointerup", soltar);
        lienzo.addEventListener("pointercancel", soltar);

        lienzo.addEventListener("pointerleave", () => {
            if (!this.arrastre) {
                this.raton = null;
                this.pedirCuadro();
            }
        });

        lienzo.addEventListener("wheel", (evento) => {
            evento.preventDefault();
            const punto = posicion(lienzo, evento);
            this.acercar(evento.deltaY < 0 ? FACTOR_ZOOM : 1 / FACTOR_ZOOM, punto.x, punto.y);
            this.pedirCuadro();
        }, { passive: false });

        lienzo.addEventListener("dblclick", (evento) => {
            const punto = posicion(lienzo, evento);
            this.acercar(1.6, punto.x, punto.y);
            this.pedirCuadro();
        });

        // El canvas puede cambiar de tamano sin que la ventana lo haga: al
        // mostrarse un panel que estaba oculto, o al entrar en pantalla
        // completa. Se redibuja solo, y si todavia no se habia podido
        // encuadrar (el canvas medía cero), se encuadra ahora.
        if (typeof ResizeObserver !== "undefined") {
            new ResizeObserver(() => this.pedirCuadro()).observe(lienzo);
        }

        window.addEventListener("cambio-de-tema", () => this.pedirCuadro());
    }

    /** Agrupa los redibujados en uno por cuadro de animacion. */
    pedirCuadro() {
        if (this.cuadroPendiente) {
            return;
        }
        this.cuadroPendiente = true;
        requestAnimationFrame(() => {
            this.cuadroPendiente = false;
            this.redibujar();
        });
    }

    /** Pone la columna de botones redondos sobre la grafica. */
    colocarBotones() {
        const caja = this.caja;
        if (!caja || caja.querySelector(".visor2d__controles")) {
            return;
        }
        const barra = document.createElement("div");
        barra.className = "visor3d__controles visor2d__controles";

        const botones = [
            ["inicio", "Volver a la vista inicial"],
            ["acercar", "Acercar"],
            ["alejar", "Alejar"],
            ["pantalla", "Pantalla completa"]
        ];
        for (const [accion, titulo] of botones) {
            const boton = document.createElement("button");
            boton.type = "button";
            boton.className = "visor3d__boton";
            boton.dataset.accion = accion;
            boton.title = titulo;
            boton.setAttribute("aria-label", titulo);
            boton.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" `
                + `stroke-width="2" stroke-linecap="round" stroke-linejoin="round" `
                + `aria-hidden="true">${ICONOS[accion]}</svg>`;
            barra.appendChild(boton);
        }

        barra.addEventListener("click", (evento) => {
            const boton = evento.target.closest("button");
            if (!boton) {
                return;
            }
            switch (boton.dataset.accion) {
                case "inicio":
                    this.encuadrar();
                    break;
                case "acercar":
                    this.acercar(FACTOR_ZOOM * FACTOR_ZOOM);
                    break;
                case "alejar":
                    this.acercar(1 / (FACTOR_ZOOM * FACTOR_ZOOM));
                    break;
                case "pantalla":
                    if (document.fullscreenElement) {
                        document.exitFullscreen?.();
                    } else {
                        caja.requestFullscreen?.();
                    }
                    return;
                default:
                    return;
            }
            this.pedirCuadro();
        });

        caja.appendChild(barra);
    }

    // ------------------------------------------------------------------
    // DIBUJO
    // ------------------------------------------------------------------

    /** Pinta un cuadro completo. */
    redibujar() {
        const lienzo = this.lienzo;
        if (!this.datos || lienzo.clientWidth <= 0 || lienzo.clientHeight <= 0) {
            return;
        }
        if (!this.encuadrada) {
            this.encuadrar();
        }

        const contexto = prepararLienzo(lienzo);
        this.ancho = lienzo.clientWidth;
        this.alto = lienzo.clientHeight;
        const paleta = leerPaleta();

        contexto.fillStyle = paleta.fondo;
        contexto.fillRect(0, 0, this.ancho, this.alto);

        const pasos = this.pasosDeRejilla();
        this.dibujarRejilla(contexto, paleta, pasos);

        if (this.opciones.sombrearRegion !== false) {
            this.dibujarRegion(contexto, paleta);
        }
        if (this.opciones.mostrarRectangulos !== false) {
            this.dibujarRectangulos(contexto, paleta);
        }

        this.dibujarEjes(contexto, paleta, pasos);
        this.dibujarRectas(contexto, paleta);
        this.dibujarEjeDeGiro(contexto, paleta);

        for (const curva of this.curvas.filter((c) => c.papel === "contexto")) {
            this.dibujarCurva(contexto, curva, paleta.contexto, 1.6);
        }
        for (const curva of this.curvas.filter((c) => c.papel !== "contexto")) {
            this.dibujarCurva(contexto, curva, curva.color === "g" ? paleta.curvaG : paleta.curvaF, 2.6);
        }

        this.dibujarPuntos(contexto, paleta);
        this.dibujarLeyenda(contexto, paleta);
        this.dibujarLectura(contexto, paleta);
    }

    /** Separacion de las lineas principales de la rejilla, en unidades. */
    pasosDeRejilla() {
        return {
            x: pasoRedondo(SEPARACION_REJILLA / this.sx),
            y: pasoRedondo(SEPARACION_REJILLA / this.sy)
        };
    }

    dibujarRejilla(contexto, paleta, pasos) {
        const { xMin, xMax, yMin, yMax } = this.rango();
        contexto.save();
        contexto.lineWidth = 1;

        // Reticula fina: cinco divisiones entre cada linea principal.
        contexto.strokeStyle = paleta.rejillaFina;
        contexto.beginPath();
        for (let x = Math.ceil(xMin / (pasos.x / 5)) * (pasos.x / 5); x <= xMax; x += pasos.x / 5) {
            const px = Math.round(this.aPixelX(x)) + 0.5;
            contexto.moveTo(px, 0);
            contexto.lineTo(px, this.alto);
        }
        for (let y = Math.ceil(yMin / (pasos.y / 5)) * (pasos.y / 5); y <= yMax; y += pasos.y / 5) {
            const py = Math.round(this.aPixelY(y)) + 0.5;
            contexto.moveTo(0, py);
            contexto.lineTo(this.ancho, py);
        }
        contexto.stroke();

        contexto.strokeStyle = paleta.rejilla;
        contexto.beginPath();
        for (let x = Math.ceil(xMin / pasos.x) * pasos.x; x <= xMax; x += pasos.x) {
            const px = Math.round(this.aPixelX(x)) + 0.5;
            contexto.moveTo(px, 0);
            contexto.lineTo(px, this.alto);
        }
        for (let y = Math.ceil(yMin / pasos.y) * pasos.y; y <= yMax; y += pasos.y) {
            const py = Math.round(this.aPixelY(y)) + 0.5;
            contexto.moveTo(0, py);
            contexto.lineTo(this.ancho, py);
        }
        contexto.stroke();
        contexto.restore();
    }

    /**
     * Los ejes, con flechas, marcas y numeros.
     *
     * Si el origen queda fuera de la vista, el eje se pega al borde para que
     * los numeros sigan a la vista, como hace GeoGebra.
     */
    dibujarEjes(contexto, paleta, pasos) {
        const { xMin, xMax, yMin, yMax } = this.rango();
        const margen = 4;
        const yEje = Math.min(Math.max(this.aPixelY(0), margen), this.alto - margen);
        const xEje = Math.min(Math.max(this.aPixelX(0), margen), this.ancho - margen);

        contexto.save();
        contexto.strokeStyle = paleta.ejes;
        contexto.fillStyle = paleta.ejes;
        contexto.lineWidth = 1.4;

        contexto.beginPath();
        contexto.moveTo(0, yEje);
        contexto.lineTo(this.ancho, yEje);
        contexto.moveTo(xEje, this.alto);
        contexto.lineTo(xEje, 0);
        contexto.stroke();

        // Flechas en los extremos positivos.
        flecha(contexto, this.ancho - 1, yEje, 1, 0);
        flecha(contexto, xEje, 1, 0, -1);

        contexto.font = "12px system-ui, sans-serif";
        contexto.lineWidth = 1;

        // Numeros del eje horizontal: debajo, salvo que el eje este pegado abajo.
        const numerosAbajo = yEje < this.alto - 22;
        contexto.textAlign = "center";
        contexto.textBaseline = numerosAbajo ? "top" : "bottom";
        for (let x = Math.ceil(xMin / pasos.x) * pasos.x; x <= xMax; x += pasos.x) {
            if (Math.abs(x) < pasos.x * 1e-6) {
                continue;
            }
            const px = this.aPixelX(x);
            if (px < 14 || px > this.ancho - 50) {
                continue;
            }
            contexto.beginPath();
            contexto.moveTo(px, yEje - 3);
            contexto.lineTo(px, yEje + 3);
            contexto.stroke();
            textoConFondo(contexto, formatearEje(redondearAlPaso(x, pasos.x)), px,
                numerosAbajo ? yEje + 6 : yEje - 6, paleta);
        }

        // Numeros del eje vertical: a la izquierda, salvo que este pegado a la izquierda.
        const numerosALaIzquierda = xEje > 30;
        contexto.textAlign = numerosALaIzquierda ? "right" : "left";
        contexto.textBaseline = "middle";
        for (let y = Math.ceil(yMin / pasos.y) * pasos.y; y <= yMax; y += pasos.y) {
            if (Math.abs(y) < pasos.y * 1e-6) {
                continue;
            }
            const py = this.aPixelY(y);
            if (py < 24 || py > this.alto - 14) {
                continue;
            }
            contexto.beginPath();
            contexto.moveTo(xEje - 3, py);
            contexto.lineTo(xEje + 3, py);
            contexto.stroke();
            textoConFondo(contexto, formatearEje(redondearAlPaso(y, pasos.y)),
                numerosALaIzquierda ? xEje - 6 : xEje + 6, py, paleta);
        }

        // El origen y los nombres de los ejes.
        if (this.aPixelX(0) === xEje && this.aPixelY(0) === yEje) {
            contexto.textAlign = "right";
            contexto.textBaseline = "top";
            textoConFondo(contexto, "0", xEje - 5, yEje + 5, paleta);
        }
        contexto.font = "italic 600 14px system-ui, sans-serif";
        contexto.textAlign = "right";
        contexto.textBaseline = "bottom";
        contexto.fillText("x", this.ancho - 6, yEje - 6);
        contexto.textAlign = "left";
        contexto.textBaseline = "top";
        contexto.fillText("y", xEje + 8, 4);

        contexto.restore();
    }

    /**
     * Traza una curva evaluandola en toda la zona visible.
     *
     * Se levanta el lapiz donde la funcion no existe o salta de golpe, para no
     * dibujar una linea vertical falsa en una asintota.
     */
    dibujarCurva(contexto, curva, colorTrazo, grosor) {
        contexto.save();
        contexto.strokeStyle = colorTrazo;
        contexto.lineWidth = grosor;
        contexto.lineJoin = "round";
        contexto.lineCap = "round";
        contexto.beginPath();

        if (curva.fn) {
            const enX = curva.variable !== "y";
            const pixeles = enX ? this.ancho : this.alto;
            const muestras = Math.ceil(pixeles / 1.5);
            const { xMin, xMax, yMin, yMax } = this.rango();
            const desde = enX ? xMin : yMin;
            const hasta = enX ? xMax : yMax;
            const salto = (enX ? this.alto : this.ancho) * 1.5;

            let anterior = null;
            for (let i = 0; i <= muestras; i++) {
                const v = desde + (hasta - desde) * i / muestras;
                const valor = curva.fn(v);
                if (!Number.isFinite(valor)) {
                    anterior = null;
                    continue;
                }
                const px = enX ? this.aPixelX(v) : this.aPixelX(valor);
                const py = enX ? this.aPixelY(valor) : this.aPixelY(v);
                const fuera = Math.abs(enX ? py : px) > 1e6;
                if (fuera) {
                    anterior = null;
                    continue;
                }
                if (anterior && Math.abs((enX ? py : px) - (enX ? anterior.py : anterior.px)) > salto) {
                    anterior = null;
                }
                if (anterior) {
                    contexto.lineTo(px, py);
                } else {
                    contexto.moveTo(px, py);
                }
                anterior = { px, py };
            }
        } else if (Array.isArray(curva.puntos)) {
            let trazando = false;
            for (const punto of curva.puntos) {
                if (!Number.isFinite(punto.x) || !Number.isFinite(punto.y)) {
                    trazando = false;
                    continue;
                }
                const px = this.aPixelX(punto.x);
                const py = this.aPixelY(punto.y);
                if (trazando) {
                    contexto.lineTo(px, py);
                } else {
                    contexto.moveTo(px, py);
                    trazando = true;
                }
            }
        }
        contexto.stroke();
        contexto.restore();
    }

    /**
     * Sombrea la region del ejercicio, tramo por tramo.
     *
     * Cuando las curvas se cruzan, los tramos donde va arriba una y donde va
     * arriba la otra se pintan con dos tonos, que es justo lo que hay que ver
     * para entender por que la integral se parte en los cortes.
     */
    dibujarRegion(contexto, paleta) {
        const region = this.datos.region;
        if (region && this.curvas.length > 0) {
            const primera = this.curvas[region.primera];
            const segunda = region.segunda >= 0 ? this.curvas[region.segunda] : null;
            if (!primera?.fn) {
                return;
            }
            const cero = () => 0;
            const otra = segunda?.fn ?? cero;
            const tramos = Array.isArray(region.tramos) && region.tramos.length > 0
                ? region.tramos
                : [{ desde: region.desde, hasta: region.hasta, primeraArriba: true }];
            const variosTonos = tramos.some((t) => t.primeraArriba) && tramos.some((t) => !t.primeraArriba);

            for (const tramo of tramos) {
                const relleno = variosTonos && !tramo.primeraArriba ? paleta.relleno2 : paleta.relleno;
                this.rellenarEntre(contexto, primera.fn, otra, tramo.desde, tramo.hasta,
                    region.variable, relleno);
            }
            return;
        }

        // Datos antiguos, sin arboles: se sombrea con los puntos muestreados.
        this.sombrearConPuntos(contexto, paleta);
    }

    /** Rellena el area entre dos funciones de la misma variable. */
    rellenarEntre(contexto, una, otra, desde, hasta, variable, relleno) {
        const enX = variable !== "y";
        const muestras = 240;
        const ida = [];
        const vuelta = [];
        for (let i = 0; i <= muestras; i++) {
            const v = desde + (hasta - desde) * i / muestras;
            const a = una(v);
            const b = otra(v);
            if (!Number.isFinite(a) || !Number.isFinite(b)) {
                continue;
            }
            ida.push(enX ? [this.aPixelX(v), this.aPixelY(a)] : [this.aPixelX(a), this.aPixelY(v)]);
            vuelta.push(enX ? [this.aPixelX(v), this.aPixelY(b)] : [this.aPixelX(b), this.aPixelY(v)]);
        }
        if (ida.length < 2) {
            return;
        }
        contexto.save();
        contexto.fillStyle = relleno;
        contexto.beginPath();
        contexto.moveTo(ida[0][0], ida[0][1]);
        for (const [px, py] of ida) {
            contexto.lineTo(px, py);
        }
        for (let i = vuelta.length - 1; i >= 0; i--) {
            contexto.lineTo(vuelta[i][0], vuelta[i][1]);
        }
        contexto.closePath();
        contexto.fill();
        contexto.restore();
    }

    /** Sombreado de respaldo con los puntos que mando el servidor. */
    sombrearConPuntos(contexto, paleta) {
        const datos = this.datos;
        const desde = Math.min(datos.limiteInferior, datos.limiteSuperior);
        const hasta = Math.max(datos.limiteInferior, datos.limiteSuperior);
        const dentro = (p) => p.x >= desde && p.x <= hasta && Number.isFinite(p.y);
        const puntosF = (datos.curvaF ?? []).filter(dentro);
        const puntosG = (datos.curvaG ?? []).filter(dentro);
        if (puntosF.length < 2) {
            return;
        }
        contexto.save();
        contexto.fillStyle = paleta.relleno;
        contexto.beginPath();
        contexto.moveTo(this.aPixelX(puntosF[0].x), this.aPixelY(puntosF[0].y));
        for (const p of puntosF) {
            contexto.lineTo(this.aPixelX(p.x), this.aPixelY(p.y));
        }
        if (puntosG.length >= 2) {
            for (let i = puntosG.length - 1; i >= 0; i--) {
                contexto.lineTo(this.aPixelX(puntosG[i].x), this.aPixelY(puntosG[i].y));
            }
        } else {
            contexto.lineTo(this.aPixelX(puntosF[puntosF.length - 1].x), this.aPixelY(0));
            contexto.lineTo(this.aPixelX(puntosF[0].x), this.aPixelY(0));
        }
        contexto.closePath();
        contexto.fill();
        contexto.restore();
    }

    /** Los rectangulos de Riemann. */
    dibujarRectangulos(contexto, paleta) {
        const rectangulos = this.datos.rectangulos;
        if (!Array.isArray(rectangulos) || rectangulos.length === 0) {
            return;
        }
        contexto.save();
        contexto.fillStyle = paleta.rectangulo;
        contexto.strokeStyle = paleta.rectanguloBorde;
        contexto.lineWidth = 1;
        const cero = this.aPixelY(0);
        for (const r of rectangulos) {
            if (!Number.isFinite(r.altura)) {
                continue;
            }
            const izquierda = this.aPixelX(r.x);
            const derecha = this.aPixelX(r.x + r.base);
            const altura = this.aPixelY(r.altura);
            const arriba = Math.min(altura, cero);
            const alto = Math.abs(altura - cero);
            contexto.fillRect(izquierda, arriba, derecha - izquierda, alto);
            contexto.strokeRect(izquierda, arriba, derecha - izquierda, alto);
        }
        contexto.restore();
    }

    /** Los limites de integracion y las rectas que cierran la region. */
    dibujarRectas(contexto, paleta) {
        const rectas = Array.isArray(this.datos.rectas) && this.datos.rectas.length > 0
            ? this.datos.rectas
            : [
                { orientacion: "VERTICAL", valor: this.datos.limiteInferior,
                  etiqueta: `a = ${formatearEje(this.datos.limiteInferior)}`, tipo: "limite" },
                { orientacion: "VERTICAL", valor: this.datos.limiteSuperior,
                  etiqueta: `b = ${formatearEje(this.datos.limiteSuperior)}`, tipo: "limite" }
            ];

        contexto.save();
        contexto.font = "600 12px system-ui, sans-serif";
        for (const recta of rectas) {
            if (!Number.isFinite(recta.valor)) {
                continue;
            }
            const esLimite = recta.tipo === "limite";
            contexto.strokeStyle = esLimite ? paleta.ejes : paleta.frontera;
            contexto.fillStyle = esLimite ? paleta.ejes : paleta.frontera;
            contexto.lineWidth = esLimite ? 1.3 : 1.8;
            contexto.setLineDash(esLimite ? [6, 4] : []);
            contexto.beginPath();
            if (recta.orientacion === "VERTICAL") {
                const px = this.aPixelX(recta.valor);
                contexto.moveTo(px, 0);
                contexto.lineTo(px, this.alto);
                contexto.stroke();
                contexto.setLineDash([]);
                contexto.textAlign = "left";
                contexto.textBaseline = "top";
                // Junto al eje vertical el rotulo bajaria sobre la "y" del eje.
                const pegadaAlEje = Math.abs(px - this.aPixelX(0)) < 30;
                const altura = (esLimite ? 6 : 24) + (pegadaAlEje ? 20 : 0);
                textoConFondo(contexto, embellecer(recta.etiqueta), px + 4, altura, paleta);
            } else {
                const py = this.aPixelY(recta.valor);
                contexto.moveTo(0, py);
                contexto.lineTo(this.ancho, py);
                contexto.stroke();
                contexto.setLineDash([]);
                contexto.textAlign = "left";
                contexto.textBaseline = "bottom";
                textoConFondo(contexto, embellecer(recta.etiqueta), 8, py - 3, paleta);
            }
        }
        contexto.restore();
    }

    /** La recta alrededor de la cual gira la region, en un problema de volumen. */
    dibujarEjeDeGiro(contexto, paleta) {
        const eje = this.datos.ejeRevolucion;
        if (!eje) {
            return;
        }
        const k = eje.desplazamiento ?? 0;
        contexto.save();
        contexto.strokeStyle = paleta.corte;
        contexto.fillStyle = paleta.corte;
        contexto.lineWidth = 2;
        contexto.setLineDash([12, 5]);
        contexto.font = "600 12px system-ui, sans-serif";
        contexto.beginPath();
        if (eje.orientacion === "VERTICAL") {
            const px = this.aPixelX(k);
            contexto.moveTo(px, 0);
            contexto.lineTo(px, this.alto);
            contexto.stroke();
            contexto.setLineDash([]);
            contexto.textAlign = "left";
            contexto.textBaseline = "bottom";
            textoConFondo(contexto, `eje de giro: ${eje.ecuacion}`, px + 6, this.alto - 8, paleta);
        } else {
            const py = this.aPixelY(k);
            contexto.moveTo(0, py);
            contexto.lineTo(this.ancho, py);
            contexto.stroke();
            contexto.setLineDash([]);
            contexto.textAlign = "right";
            contexto.textBaseline = "bottom";
            textoConFondo(contexto, `eje de giro: ${eje.ecuacion}`, this.ancho - 60, py - 4, paleta);
        }
        contexto.restore();
    }

    /** Los puntos de corte, con su coordenada escrita al lado. */
    dibujarPuntos(contexto, paleta) {
        const puntos = this.puntos();
        contexto.save();
        contexto.font = "600 12px system-ui, sans-serif";
        contexto.textAlign = "left";
        contexto.textBaseline = "bottom";
        for (const punto of puntos) {
            const px = this.aPixelX(punto.x);
            const py = this.aPixelY(punto.y);
            if (px < -10 || px > this.ancho + 10 || py < -10 || py > this.alto + 10) {
                continue;
            }
            const esEsquina = punto.tipo === "esquina";
            contexto.beginPath();
            contexto.arc(px, py, esEsquina ? 3.5 : 5, 0, Math.PI * 2);
            contexto.fillStyle = esEsquina ? paleta.fondo : paleta.punto;
            contexto.strokeStyle = esEsquina ? paleta.punto : paleta.fondo;
            contexto.lineWidth = esEsquina ? 1.5 : 2;
            contexto.fill();
            contexto.stroke();
            if (!esEsquina && punto.etiqueta) {
                contexto.fillStyle = paleta.punto;
                textoConFondo(contexto, punto.etiqueta, px + 7, py - 5, paleta);
            }
        }
        contexto.restore();
    }

    /** Los puntos destacados, de los datos nuevos o de los antiguos. */
    puntos() {
        if (Array.isArray(this.datos.puntos) && this.datos.puntos.length > 0) {
            return this.datos.puntos;
        }
        return (this.datos.intersecciones ?? []).map((p) => ({
            x: p.x, y: p.y, tipo: "interseccion",
            etiqueta: `(${formatearEje(p.x)}, ${formatearEje(p.y)})`
        }));
    }

    /** La leyenda, arriba a la derecha: que curva es cada color. */
    dibujarLeyenda(contexto, paleta) {
        const entradas = [];
        for (const curva of this.curvas) {
            if (!curva.etiqueta) {
                continue;
            }
            const muestra = curva.papel === "contexto"
                ? paleta.contexto
                : (curva.color === "g" ? paleta.curvaG : paleta.curvaF);
            entradas.push({ texto: embellecer(curva.etiqueta), color: muestra });
        }
        if (entradas.length === 0) {
            return;
        }
        contexto.save();
        contexto.font = "13px system-ui, sans-serif";
        const anchoTexto = Math.min(260, Math.max(...entradas.map((e) => contexto.measureText(e.texto).width)));
        const ancho = anchoTexto + 40;
        const alto = entradas.length * 20 + 10;
        // Se deja libre la columna de botones de la derecha, que en pantallas
        // pequenas llega hasta arriba.
        const x = Math.max(10, this.ancho - ancho - 62);
        const y = 10;

        contexto.globalAlpha = 0.92;
        contexto.fillStyle = paleta.fondo;
        contexto.strokeStyle = paleta.rejilla;
        contexto.lineWidth = 1;
        rectanguloRedondeado(contexto, x, y, ancho, alto, 6);
        contexto.fill();
        contexto.stroke();
        contexto.globalAlpha = 1;

        contexto.textBaseline = "middle";
        contexto.textAlign = "left";
        entradas.forEach((entrada, i) => {
            const fila = y + 15 + i * 20;
            contexto.strokeStyle = entrada.color;
            contexto.lineWidth = 3;
            contexto.beginPath();
            contexto.moveTo(x + 10, fila);
            contexto.lineTo(x + 26, fila);
            contexto.stroke();
            contexto.fillStyle = paleta.texto;
            contexto.fillText(recortar(contexto, entrada.texto, anchoTexto), x + 32, fila);
        });
        contexto.restore();
    }

    /**
     * Lo que hay bajo el puntero: un punto de corte, el punto de una curva o
     * simplemente la coordenada.
     */
    dibujarLectura(contexto, paleta) {
        if (!this.raton) {
            this.lectura?.classList.remove("grafica__lectura--visible");
            return;
        }
        const { x: px, y: py } = this.raton;
        const x = this.aMatematicoX(px);
        const y = this.aMatematicoY(py);
        let texto = `x = ${formatearEje(x)}    y = ${formatearEje(y)}`;
        let marca = null;

        // 1. Un punto destacado.
        for (const punto of this.puntos()) {
            const dx = this.aPixelX(punto.x) - px;
            const dy = this.aPixelY(punto.y) - py;
            if (dx * dx + dy * dy <= 64) {
                marca = { px: this.aPixelX(punto.x), py: this.aPixelY(punto.y), color: paleta.punto };
                texto = `(${formatearEje(punto.x)}, ${formatearEje(punto.y)})`;
                break;
            }
        }

        // 2. La curva mas cercana.
        if (!marca) {
            let mejor = null;
            for (const curva of this.curvas) {
                if (!curva.fn) {
                    continue;
                }
                const enX = curva.variable !== "y";
                const valor = curva.fn(enX ? x : y);
                if (!Number.isFinite(valor)) {
                    continue;
                }
                const distancia = enX
                    ? Math.abs(this.aPixelY(valor) - py)
                    : Math.abs(this.aPixelX(valor) - px);
                if (distancia <= DISTANCIA_DE_LECTURA && (!mejor || distancia < mejor.distancia)) {
                    mejor = { curva, valor, distancia, enX };
                }
            }
            if (mejor) {
                const puntoX = mejor.enX ? x : mejor.valor;
                const puntoY = mejor.enX ? mejor.valor : y;
                const colorCurva = mejor.curva.papel === "contexto"
                    ? paleta.contexto
                    : (mejor.curva.color === "g" ? paleta.curvaG : paleta.curvaF);
                marca = { px: this.aPixelX(puntoX), py: this.aPixelY(puntoY), color: colorCurva };
                const nombre = mejor.curva.etiqueta ? `${embellecer(mejor.curva.etiqueta)}:  ` : "";
                texto = `${nombre}(${formatearEje(puntoX)}, ${formatearEje(puntoY)})`;
            }
        }

        if (marca) {
            contexto.save();
            contexto.beginPath();
            contexto.arc(marca.px, marca.py, 6, 0, Math.PI * 2);
            contexto.fillStyle = marca.color;
            contexto.strokeStyle = paleta.fondo;
            contexto.lineWidth = 2;
            contexto.fill();
            contexto.stroke();
            contexto.restore();
        }

        if (this.lectura) {
            this.lectura.textContent = texto;
            this.lectura.classList.add("grafica__lectura--visible");
        } else {
            contexto.save();
            contexto.font = "12px system-ui, sans-serif";
            contexto.textAlign = "left";
            contexto.textBaseline = "top";
            textoConFondo(contexto, texto, 10, 10, paleta);
            contexto.restore();
        }
    }
}

// ==================================================================
// APOYO
// ==================================================================

/** Prepara las curvas: compila los arboles, o se queda con los puntos antiguos. */
function prepararCurvas(datos) {
    if (Array.isArray(datos.funciones) && datos.funciones.length > 0) {
        return datos.funciones.map((funcion) => ({
            etiqueta: funcion.etiqueta ?? "",
            variable: funcion.variable ?? "x",
            papel: funcion.papel ?? "borde",
            color: funcion.color ?? "f",
            fn: compilar(funcion.arbol)
        }));
    }
    const curvas = [];
    if (Array.isArray(datos.curvaF) && datos.curvaF.length > 0) {
        curvas.push({ etiqueta: datos.etiquetaF ?? "", papel: "borde", color: "f", puntos: datos.curvaF });
    }
    if (Array.isArray(datos.curvaG) && datos.curvaG.length > 0) {
        curvas.push({ etiqueta: datos.etiquetaG ?? "", papel: "borde", color: "g", puntos: datos.curvaG });
    }
    return curvas;
}

/**
 * La zona que hay que ver al abrir la grafica: la region, sus puntos de corte,
 * el eje de giro y el origen, con un margen alrededor.
 */
function cajaDeInteres(datos, curvas) {
    const caja = { xMin: Infinity, xMax: -Infinity, yMin: Infinity, yMax: -Infinity };
    const incluir = (x, y) => {
        if (Number.isFinite(x)) {
            caja.xMin = Math.min(caja.xMin, x);
            caja.xMax = Math.max(caja.xMax, x);
        }
        if (Number.isFinite(y)) {
            caja.yMin = Math.min(caja.yMin, y);
            caja.yMax = Math.max(caja.yMax, y);
        }
    };

    const region = datos.region;
    if (region && curvas[region.primera]?.fn) {
        const una = curvas[region.primera].fn;
        const otra = region.segunda >= 0 && curvas[region.segunda]?.fn ? curvas[region.segunda].fn : () => 0;
        const enX = region.variable !== "y";
        for (let i = 0; i <= 120; i++) {
            const v = region.desde + (region.hasta - region.desde) * i / 120;
            for (const valor of [una(v), otra(v)]) {
                if (Number.isFinite(valor) && Math.abs(valor) < 1e6) {
                    enX ? incluir(v, valor) : incluir(valor, v);
                }
            }
        }
    } else if (datos.ventana) {
        incluir(datos.ventana.xMinimo, datos.ventana.yMinimo);
        incluir(datos.ventana.xMaximo, datos.ventana.yMaximo);
    }

    for (const punto of datos.puntos ?? []) {
        incluir(punto.x, punto.y);
    }
    const eje = datos.ejeRevolucion;
    if (eje) {
        if (eje.orientacion === "VERTICAL") {
            incluir(eje.desplazamiento, undefined);
        } else {
            incluir(undefined, eje.desplazamiento);
        }
    }
    incluir(0, 0);

    if (!Number.isFinite(caja.xMin) || !Number.isFinite(caja.yMin)) {
        return { xMin: -10, xMax: 10, yMin: -10, yMax: 10 };
    }

    // Margen: la region no debe quedar pegada al borde ni bajo los botones.
    const anchoX = Math.max(caja.xMax - caja.xMin, 1);
    const anchoY = Math.max(caja.yMax - caja.yMin, 1);
    return {
        xMin: caja.xMin - anchoX * 0.18,
        xMax: caja.xMax + anchoX * 0.28,
        yMin: caja.yMin - anchoY * 0.15,
        yMax: caja.yMax + anchoY * 0.22
    };
}

/** El paso redondo mas cercano por encima: 1, 2 o 5 por una potencia de diez. */
function pasoRedondo(ideal) {
    const magnitud = Math.pow(10, Math.floor(Math.log10(ideal)));
    const mantisa = ideal / magnitud;
    const redondo = mantisa <= 1 ? 1 : mantisa <= 2 ? 2 : mantisa <= 5 ? 5 : 10;
    return redondo * magnitud;
}

/** Quita el ruido de coma flotante al sumar el paso muchas veces. */
function redondearAlPaso(valor, paso) {
    return Math.round(valor / paso) * paso;
}

/** La posicion del puntero relativa al canvas. */
function posicion(lienzo, evento) {
    const marco = lienzo.getBoundingClientRect();
    return { x: evento.clientX - marco.left, y: evento.clientY - marco.top };
}

/** Distancia entre dos dedos y su punto medio, para el pellizco. */
function distanciaYCentro(puntos) {
    const [a, b] = puntos;
    return {
        distancia: Math.hypot(a.x - b.x, a.y - b.y),
        x: (a.x + b.x) / 2,
        y: (a.y + b.y) / 2
    };
}

/** Una punta de flecha en el extremo de un eje. */
function flecha(contexto, x, y, dx, dy) {
    const largo = 9;
    const ancho = 4.5;
    contexto.beginPath();
    contexto.moveTo(x, y);
    contexto.lineTo(x - dx * largo - dy * ancho, y - dy * largo + dx * ancho);
    contexto.lineTo(x - dx * largo + dy * ancho, y - dy * largo - dx * ancho);
    contexto.closePath();
    contexto.fill();
}

/**
 * Escribe un texto con un contorno del color del lienzo, para que se lea
 * encima de la rejilla, de una curva o de la region sombreada. Es lo que hace
 * GeoGebra con sus rotulos: sin recuadro, solo un halo fino.
 */
function textoConFondo(contexto, texto, x, y, paleta) {
    contexto.save();
    contexto.lineJoin = "round";
    contexto.lineWidth = 3;
    contexto.strokeStyle = paleta.fondo;
    contexto.globalAlpha = 0.9;
    contexto.strokeText(texto, x, y);
    contexto.restore();
    contexto.fillText(texto, x, y);
}

/** Recorta un texto que no cabe, terminandolo en puntos suspensivos. */
function recortar(contexto, texto, ancho) {
    if (contexto.measureText(texto).width <= ancho) {
        return texto;
    }
    let recortado = texto;
    while (recortado.length > 1 && contexto.measureText(`${recortado}…`).width > ancho) {
        recortado = recortado.slice(0, -1);
    }
    return `${recortado}…`;
}

/** Traza un rectangulo con las esquinas redondeadas. */
function rectanguloRedondeado(contexto, x, y, ancho, alto, radio) {
    contexto.beginPath();
    contexto.moveTo(x + radio, y);
    contexto.arcTo(x + ancho, y, x + ancho, y + alto, radio);
    contexto.arcTo(x + ancho, y + alto, x, y + alto, radio);
    contexto.arcTo(x, y + alto, x, y, radio);
    contexto.arcTo(x, y, x + ancho, y, radio);
    contexto.closePath();
}

/** Lee los colores del CSS en el momento de dibujar, para seguir el tema. */
function leerPaleta() {
    const leer = (nombre, respaldo) => color(nombre) || respaldo;
    return {
        fondo: leer("grafica-fondo", "#ffffff"),
        ejes: leer("grafica-ejes", "#64748b"),
        rejilla: leer("grafica-rejilla", "#eef2f7"),
        rejillaFina: leer("grafica-rejilla-fina", "#f6f8fb"),
        curvaF: leer("grafica-curva-f", "#2563eb"),
        curvaG: leer("grafica-curva-g", "#db2777"),
        relleno: leer("grafica-relleno", "rgba(37, 99, 235, 0.18)"),
        relleno2: leer("grafica-relleno-2", "rgba(219, 39, 119, 0.16)"),
        rectangulo: leer("grafica-rectangulo", "rgba(234, 88, 12, 0.28)"),
        rectanguloBorde: leer("grafica-rectangulo-borde", "#ea580c"),
        corte: leer("grafica-corte", "#7c3aed"),
        contexto: leer("grafica-contexto", "#9aa8bb"),
        frontera: leer("grafica-frontera", "#0d9488"),
        punto: leer("grafica-punto", "#1e293b"),
        texto: leer("color-texto", "#0f172a")
    };
}
