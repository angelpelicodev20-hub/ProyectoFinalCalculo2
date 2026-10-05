/**
 * escenaTresD.js
 * Proyecta puntos del espacio sobre el plano del canvas.
 *
 * La camara imita la vista 3D de GeoGebra, que es la que los estudiantes ya
 * conocen de clase:
 *
 *   - El eje Z apunta hacia arriba. El plano XY, donde vive la region que se
 *     dibuja en la grafica plana, hace de suelo, y el solido se levanta sobre
 *     el y se hunde por debajo al girar.
 *   - La camara orbita alrededor de la escena con dos angulos: el azimut, que
 *     gira alrededor del eje Z, y la elevacion, que sube o baja la mirada.
 *   - La proyeccion es paralela, sin perspectiva. Asi las marcas de un eje
 *     quedan igual de separadas cerca y lejos, y se pueden leer distancias
 *     sobre el dibujo, que es para lo que sirven.
 *
 * Ademas de la posicion en pantalla se devuelve la cercania de cada punto a la
 * camara, que es lo que permite pintar primero lo de atras y despues lo de
 * adelante.
 */

/** Vista inicial: la misma orientacion que trae GeoGebra al abrir un solido. */
const AZIMUT_INICIAL = (20 * Math.PI) / 180;
const ELEVACION_INICIAL = (10 * Math.PI) / 180;

/** La elevacion no llega a los noventa grados: mirando justo desde arriba el
 *  eje Z se reduce a un punto y se pierde la orientacion. */
const ELEVACION_MAXIMA = (89 * Math.PI) / 180;

/** Espacio libre alrededor del encuadre inicial, para las etiquetas. */
const MARGEN_PIXELES = 28;

/** Cuanto se puede alejar o acercar respecto del encuadre inicial. */
const ZOOM_MINIMO = 1 / 20;
const ZOOM_MAXIMO = 60;

/**
 * Camara que mira el solido.
 */
export class Escena3D {

    /**
     * @param {number} ancho ancho del canvas en pixeles
     * @param {number} alto  alto del canvas en pixeles
     */
    constructor(ancho, alto) {
        this.ancho = ancho;
        this.alto = alto;

        this.azimutInicial = AZIMUT_INICIAL;
        this.elevacionInicial = ELEVACION_INICIAL;
        this.azimut = AZIMUT_INICIAL;
        this.elevacion = ELEVACION_INICIAL;

        this.centro = { x: 0, y: 0, z: 0 };
        this.escala = 40;
        this.escalaInicial = 40;

        // Desplazamiento de la vista en pixeles, para cuando se arrastra con
        // Shift o con el boton derecho.
        this.desplazamientoX = 0;
        this.desplazamientoY = 0;

        this.caja = null;

        // Cuenta los encuadres. Quien dibuja puede guardar calculos que solo
        // dependen del encuadre —como hasta donde llegan los ejes— y saber
        // cuando rehacerlos, sin repetirlos en cada cuadro del arrastre.
        this.encuadres = 0;
    }

    /**
     * Expresa un vector del mundo en los ejes de la pantalla.
     *
     * No traslada ni escala: solo gira. Sirve tanto para los puntos, despues
     * de restarles el centro, como para las normales con que se calcula la
     * luz de cada cara.
     *
     * @param {{x: number, y: number, z: number}} v vector en el mundo
     * @returns {{derecha: number, arriba: number, cerca: number}} el vector visto desde la camara
     */
    aVista(v) {
        // 1. Giro alrededor del eje Z, que es el arrastre horizontal.
        const cosA = Math.cos(this.azimut);
        const senA = Math.sin(this.azimut);
        const derecha = v.x * cosA - v.y * senA;
        const fondo = v.x * senA + v.y * cosA;

        // 2. Inclinacion de la mirada. Con la camara por encima del suelo, lo
        //    que esta mas al fondo sube en la pantalla y lo que esta mas alto
        //    queda un poco mas cerca.
        const cosE = Math.cos(this.elevacion);
        const senE = Math.sin(this.elevacion);
        const arriba = v.z * cosE + fondo * senE;
        const lejania = fondo * cosE - v.z * senE;

        return { derecha, arriba, cerca: -lejania };
    }

    /**
     * Convierte un punto del espacio en un punto del canvas.
     *
     * @param {{x: number, y: number, z: number}} punto punto en el espacio
     * @returns {{x: number, y: number, profundidad: number}} punto proyectado;
     *          la profundidad crece hacia la camara
     */
    proyectar(punto) {
        const vista = this.aVista({
            x: punto.x - this.centro.x,
            y: punto.y - this.centro.y,
            z: punto.z - this.centro.z
        });

        return {
            x: this.ancho / 2 + this.desplazamientoX + vista.derecha * this.escala,
            y: this.alto / 2 + this.desplazamientoY - vista.arriba * this.escala,
            profundidad: vista.cerca
        };
    }

    /**
     * Ajusta la camara para que la caja quepa entera en el canvas.
     *
     * Se proyectan las ocho esquinas con la orientacion actual y se toma la
     * escala mas restrictiva entre el ancho y el alto. El encuadre se guarda
     * para que el boton de inicio pueda volver exactamente a el.
     *
     * @param {object} caja extremos: xMinimo, xMaximo, yMinimo, yMaximo, zMinimo, zMaximo
     */
    encuadrar(caja) {
        this.caja = caja;
        this.centro = {
            x: (caja.xMinimo + caja.xMaximo) / 2,
            y: (caja.yMinimo + caja.yMaximo) / 2,
            z: (caja.zMinimo + caja.zMaximo) / 2
        };

        let mayorDerecha = 1e-9;
        let mayorArriba = 1e-9;

        for (const x of [caja.xMinimo, caja.xMaximo]) {
            for (const y of [caja.yMinimo, caja.yMaximo]) {
                for (const z of [caja.zMinimo, caja.zMaximo]) {
                    const vista = this.aVista({
                        x: x - this.centro.x,
                        y: y - this.centro.y,
                        z: z - this.centro.z
                    });
                    mayorDerecha = Math.max(mayorDerecha, Math.abs(vista.derecha));
                    mayorArriba = Math.max(mayorArriba, Math.abs(vista.arriba));
                }
            }
        }

        const libreAncho = Math.max(this.ancho / 2 - MARGEN_PIXELES, 10);
        const libreAlto = Math.max(this.alto / 2 - MARGEN_PIXELES, 10);

        this.escala = Math.min(libreAncho / mayorDerecha, libreAlto / mayorArriba);
        this.escalaInicial = this.escala;
        this.desplazamientoX = 0;
        this.desplazamientoY = 0;
        this.encuadres++;
    }

    /**
     * Gira la camara.
     *
     * @param {number} deltaAzimut    cuanto girar alrededor del eje Z, en radianes
     * @param {number} deltaElevacion cuanto subir la mirada, en radianes
     */
    girar(deltaAzimut, deltaElevacion) {
        this.azimut += deltaAzimut;
        this.elevacion = Math.max(-ELEVACION_MAXIMA,
            Math.min(ELEVACION_MAXIMA, this.elevacion + deltaElevacion));
    }

    /**
     * Acerca o aleja la vista.
     *
     * Si se da la posicion del puntero, el punto que esta debajo de el se
     * queda quieto, que es lo que se espera al usar la rueda: acercarse a lo
     * que se esta mirando y no al centro del dibujo.
     *
     * @param {number} factor multiplicador; mayor que 1 acerca
     * @param {number} [px] posicion horizontal del puntero en el canvas
     * @param {number} [py] posicion vertical del puntero en el canvas
     */
    acercar(factor, px, py) {
        const anterior = this.escala;
        this.escala = Math.max(this.escalaInicial * ZOOM_MINIMO,
            Math.min(this.escalaInicial * ZOOM_MAXIMO, this.escala * factor));

        const real = this.escala / anterior;
        if (px === undefined || py === undefined) {
            this.desplazamientoX *= real;
            this.desplazamientoY *= real;
            return;
        }

        const centroX = this.ancho / 2 + this.desplazamientoX;
        const centroY = this.alto / 2 + this.desplazamientoY;
        this.desplazamientoX += (px - centroX) * (1 - real);
        this.desplazamientoY += (py - centroY) * (1 - real);
    }

    /**
     * Mueve la vista sin girarla.
     *
     * @param {number} dx pixeles en horizontal
     * @param {number} dy pixeles en vertical
     */
    desplazar(dx, dy) {
        this.desplazamientoX += dx;
        this.desplazamientoY += dy;
    }

    /**
     * Cambia la vista con que se abre el visor, y a la que vuelve el boton de
     * inicio.
     *
     * @param {number} azimut    giro alrededor del eje Z, en grados
     * @param {number} elevacion inclinacion de la mirada, en grados
     */
    fijarVistaInicial(azimut, elevacion) {
        this.azimutInicial = (azimut * Math.PI) / 180;
        this.elevacionInicial = (elevacion * Math.PI) / 180;
        this.azimut = this.azimutInicial;
        this.elevacion = this.elevacionInicial;
    }

    /** Vuelve a la orientacion y al encuadre iniciales. */
    reiniciar() {
        this.azimut = this.azimutInicial;
        this.elevacion = this.elevacionInicial;
        if (this.caja) {
            this.encuadrar(this.caja);
        }
    }

    /** Indica si la camara esta por encima del plano XY. */
    miraDesdeArriba() {
        return this.elevacion >= 0;
    }
}

/**
 * Conecta el raton y los gestos tactiles al visor.
 *
 * Los oyentes se registran una sola vez por canvas. Cada vez que se resuelve
 * un ejercicio nuevo se crea otra camara, y si se volvieran a registrar, los
 * oyentes de los solidos anteriores seguirian vivos redibujando sobre el mismo
 * canvas en cada movimiento del raton. Por eso lo que se guarda en el canvas
 * es solo una referencia a la camara vigente, y los oyentes la leen de ahi.
 *
 *   Arrastrar           gira
 *   Shift + arrastrar   desplaza (tambien con el boton derecho)
 *   Rueda               acerca y aleja
 *   Dos dedos           acerca y aleja en pantallas tactiles
 *
 * @param {HTMLCanvasElement} lienzo canvas del visor
 * @param {Escena3D} escena camara a manipular
 * @param {Function} redibujar funcion que vuelve a pintar la escena
 */
export function conectarControles(lienzo, escena, redibujar) {
    lienzo.__visor3d = { escena, redibujar };

    if (lienzo.__visor3dConectado) {
        return;
    }
    lienzo.__visor3dConectado = true;

    const vigente = () => lienzo.__visor3d;

    let modo = null;
    let ultimoX = 0;
    let ultimoY = 0;
    let distanciaPinza = 0;

    const mover = (dx, dy) => {
        const { escena: camara, redibujar: pintar } = vigente();
        if (modo === "desplazar") {
            camara.desplazar(dx, dy);
        } else {
            camara.girar(dx * 0.01, dy * 0.01);
        }
        pintar();
    };

    lienzo.addEventListener("mousedown", (evento) => {
        modo = (evento.button === 2 || evento.shiftKey) ? "desplazar" : "girar";
        ultimoX = evento.clientX;
        ultimoY = evento.clientY;
        evento.preventDefault();
    });

    // El movimiento y el soltar se escuchan en toda la ventana: si el usuario
    // arrastra rapido y saca el puntero del lienzo, el giro debe seguir hasta
    // que suelte el boton.
    window.addEventListener("mousemove", (evento) => {
        if (!modo) {
            return;
        }
        const dx = evento.clientX - ultimoX;
        const dy = evento.clientY - ultimoY;
        ultimoX = evento.clientX;
        ultimoY = evento.clientY;
        mover(dx, dy);
    });

    window.addEventListener("mouseup", () => {
        modo = null;
    });

    // El boton derecho se usa para desplazar, asi que no debe abrir el menu.
    lienzo.addEventListener("contextmenu", (evento) => evento.preventDefault());

    lienzo.addEventListener("wheel", (evento) => {
        evento.preventDefault();
        const rect = lienzo.getBoundingClientRect();
        const { escena: camara, redibujar: pintar } = vigente();
        camara.acercar(evento.deltaY < 0 ? 1.12 : 1 / 1.12,
            evento.clientX - rect.left, evento.clientY - rect.top);
        pintar();
    }, { passive: false });

    // ---------- Pantallas tactiles ----------

    const distanciaEntre = (toques) => Math.hypot(
        toques[0].clientX - toques[1].clientX,
        toques[0].clientY - toques[1].clientY);

    lienzo.addEventListener("touchstart", (evento) => {
        if (evento.touches.length === 1) {
            modo = "girar";
            ultimoX = evento.touches[0].clientX;
            ultimoY = evento.touches[0].clientY;
        } else if (evento.touches.length === 2) {
            modo = "pinza";
            distanciaPinza = distanciaEntre(evento.touches);
        }
    }, { passive: true });

    lienzo.addEventListener("touchmove", (evento) => {
        if (!modo) {
            return;
        }
        evento.preventDefault();

        if (modo === "pinza" && evento.touches.length === 2) {
            const nueva = distanciaEntre(evento.touches);
            if (distanciaPinza > 0) {
                const { escena: camara, redibujar: pintar } = vigente();
                camara.acercar(nueva / distanciaPinza);
                pintar();
            }
            distanciaPinza = nueva;
            return;
        }

        if (evento.touches.length === 1) {
            const dx = evento.touches[0].clientX - ultimoX;
            const dy = evento.touches[0].clientY - ultimoY;
            ultimoX = evento.touches[0].clientX;
            ultimoY = evento.touches[0].clientY;
            mover(dx, dy);
        }
    }, { passive: false });

    lienzo.addEventListener("touchend", () => {
        modo = null;
    });
}
