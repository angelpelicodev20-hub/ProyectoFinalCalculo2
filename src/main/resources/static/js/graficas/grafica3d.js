/**
 * grafica3d.js
 * Dibuja el solido de revolucion con el aspecto de la vista 3D de GeoGebra.
 *
 * La escena tiene cuatro elementos, los mismos que el estudiante ve en clase:
 *
 *   - Los tres ejes coordenados con sus colores de siempre: X rojo, Y verde y
 *     Z azul, cada uno con su flecha, sus marcas y sus numeros.
 *   - El plano XY como un suelo gris translucido. Es el plano de la grafica
 *     plana, donde vive la region antes de girar, y sirve de referencia para
 *     ver que parte del solido queda arriba y cual abajo.
 *   - El solido, semitransparente, para que se vea tambien su cara de atras
 *     y el eje que lo atraviesa.
 *   - La malla de meridianos y paralelos sobre la superficie, que es la que
 *     deja leer la forma: los paralelos son los circulos que describe cada
 *     punto del perfil al girar, y los meridianos son copias del perfil.
 *
 * Todo se pinta con el metodo del pintor: se ordenan las piezas de la mas
 * lejana a la mas cercana y se dibujan en ese orden. El plano XY parte la
 * escena en dos, y por eso se pinta entre lo que queda de un lado y lo que
 * queda del otro: asi la mitad del solido que esta debajo del suelo se ve
 * tenida de gris, como en GeoGebra.
 */

import { prepararLienzo, color } from "./ejes.js";
import { Escena3D, conectarControles } from "./escenaTresD.js";
import { formatearEje } from "../util/formatoNumeros.js";

/** Opacidad de la superficie. Dos capas superpuestas siguen dejando ver el fondo. */
const ALFA_SUPERFICIE = 0.45;

/** Las tapas de los extremos van mas tenues: son cortes, no la superficie. */
const ALFA_TAPA = 0.28;

/** Cuantos paralelos y meridianos se dibujan, aproximadamente. */
const PARALELOS = 12;
const MERIDIANOS = 18;

/** En cuantos tramos se parte cada eje para poder ordenarlo con el solido. */
const TRAMOS_EJE = 48;

/** Direccion de la luz respecto de la camara: desde arriba, a la izquierda. */
const LUZ = normalizar({ derecha: -0.35, arriba: 0.55, cerca: 0.76 });

/** Espacio que necesita la punta de un eje dentro del lienzo: la flecha y su nombre. */
const MARGEN_PUNTA = 36;

/**
 * La geometria se calcula una vez por solido y se reutiliza en cada cuadro.
 * Girar la camara no cambia el solido, solo como se ve.
 */
const geometrias = new WeakMap();

/** Hasta donde llega cada eje, calculado una vez por encuadre de cada camara. */
const alcances = new WeakMap();

/**
 * Dibuja un solido de revolucion y deja el visor listo para girarse.
 *
 * @param {HTMLCanvasElement} lienzo canvas del visor
 * @param {object} datos malla que devolvio el backend
 * @param {object} [opciones] ajustes de dibujo
 * @param {boolean} [opciones.mostrarMalla=true] dibujar meridianos y paralelos
 * @param {boolean} [opciones.mostrarEje=true] dibujar el eje de giro si no es un eje coordenado
 * @returns {Escena3D} la camara, por si hay que manipularla desde fuera
 */
export function dibujarSolido(lienzo, datos, opciones = {}) {
    const escena = new Escena3D(lienzo.clientWidth, lienzo.clientHeight);

    // Con un eje de giro vertical en la grafica plana, el solido queda tendido
    // a lo largo del eje Y, que con la vista habitual apunta casi hacia la
    // camara: se veria de punta, como un anillo, sin que se entienda su forma.
    // Se gira la vista para mirarlo de costado, como se mira el que gira
    // alrededor del eje X.
    if (datos.eje?.orientacion === "VERTICAL") {
        escena.fijarVistaInicial(-65, 12);
    }

    escena.encuadrar(obtenerGeometria(datos).encuadre);

    const redibujar = () => pintar(lienzo, datos, escena, opciones);

    conectarControles(lienzo, escena, redibujar);
    colocarBotones(lienzo);
    redibujar();

    return escena;
}

/**
 * Pinta un cuadro completo de la escena.
 *
 * @param {HTMLCanvasElement} lienzo canvas del visor
 * @param {object} datos malla del solido
 * @param {Escena3D} escena camara
 * @param {object} opciones ajustes de dibujo
 */
export function pintar(lienzo, datos, escena, opciones = {}) {
    const {
        mostrarMalla = true,
        mostrarEje = true
    } = opciones;

    const contexto = prepararLienzo(lienzo);

    // El tamano del canvas puede haber cambiado desde la ultima vez, por
    // ejemplo al pasar a pantalla completa o al girar el telefono.
    escena.ancho = lienzo.clientWidth;
    escena.alto = lienzo.clientHeight;

    const paleta = leerPaleta();
    const geometria = obtenerGeometria(datos);
    const alcance = alcanceDeLosEjes(geometria, escena);

    contexto.fillStyle = paleta.fondo;
    contexto.fillRect(0, 0, escena.ancho, escena.alto);

    const piezas = [
        ...piezasDelSolido(geometria, escena, paleta, mostrarMalla),
        ...piezasDeLosEjes(alcance, escena, paleta),
        ...(mostrarEje ? piezasDelEjeDeGiro(datos, geometria, escena, paleta) : [])
    ];

    // El plano XY separa la escena. Lo que queda del lado contrario a la
    // camara se pinta antes que el plano, y el plano lo cubre con su gris;
    // lo que queda del mismo lado se pinta despues, por encima.
    const tolerancia = geometria.ejes.largo * 1e-9;
    const desdeArriba = escena.miraDesdeArriba();
    const detras = [];
    const delante = [];

    for (const pieza of piezas) {
        const alOtroLado = desdeArriba ? pieza.z < -tolerancia : pieza.z > tolerancia;
        (alOtroLado ? detras : delante).push(pieza);
    }

    const deLejosACerca = (una, otra) => una.profundidad - otra.profundidad;
    detras.sort(deLejosACerca);
    delante.sort(deLejosACerca);

    for (const pieza of detras) {
        pieza.dibujar(contexto);
    }
    dibujarPlano(contexto, geometria, escena, paleta);
    for (const pieza of delante) {
        pieza.dibujar(contexto);
    }

    // Las flechas, los numeros y los nombres de los ejes van siempre encima,
    // como en GeoGebra: un rotulo tapado por el solido no se podria leer.
    dibujarRotulos(contexto, geometria, alcance, escena, paleta);
    dibujarLeyenda(contexto, datos, paleta);
}

// ==================================================================
// GEOMETRIA
// ==================================================================

/** Devuelve la geometria del solido, calculandola la primera vez. */
function obtenerGeometria(datos) {
    let geometria = geometrias.get(datos);
    if (!geometria) {
        geometria = construirGeometria(datos);
        geometrias.set(datos, geometria);
    }
    return geometria;
}

/**
 * Prepara todo lo que no depende de la camara.
 *
 * @param {object} datos malla que devolvio el backend
 * @returns {object} celdas de la superficie, medidas de los ejes y encuadre inicial
 */
function construirGeometria(datos) {
    const exterior = anillosValidos(datos.mallaExterior);
    const interior = anillosValidos(datos.mallaInterior);
    const ejeHorizontal = datos.eje?.orientacion !== "VERTICAL";

    const celdas = [
        ...celdasDeSuperficie(exterior, "exterior"),
        ...celdasDeSuperficie(interior, "interior"),
        ...celdasDeTapas(exterior, interior, ejeHorizontal)
    ];

    const extremos = medir([...exterior.flat(), ...interior.flat()]);

    return {
        celdas,
        ejes: medidasDeLosEjes(extremos),
        encuadre: encuadreInicial(extremos)
    };
}

/**
 * Agrupa la malla fina en celdas del tamano de la reticula que se dibuja.
 *
 * El backend manda muchos anillos para que el contorno salga suave, pero
 * dibujar una cara por cada cuadrito tiene un problema con la transparencia:
 * entre dos caras vecinas queda siempre una costura visible, y la superficie
 * se llenaria de lineas finas que no son la malla. La solucion es que cada
 * celda abarque un cuadro completo de la reticula, con su borde siguiendo
 * todos los puntos finos. Las costuras caen entonces justo sobre los
 * meridianos y paralelos, que se dibujan de todos modos, y el contorno del
 * solido conserva toda la resolucion.
 *
 * @param {Array<Array<object>>} anillos anillos del solido, en orden a lo largo del eje
 * @param {string} tipo "exterior" o "interior"
 * @returns {Array<object>} las celdas
 */
function celdasDeSuperficie(anillos, tipo) {
    const filas = anillos.length;
    if (filas < 2) {
        return [];
    }

    // Todos los anillos se cierran repitiendo el primer punto al final.
    const segmentos = Math.min(...anillos.map((anillo) => anillo.length)) - 1;
    if (segmentos < 2) {
        return [];
    }

    const cortesFila = cortes(filas - 1, Math.max(1, Math.round((filas - 1) / PARALELOS)));
    const cortesSegmento = cortes(segmentos, Math.max(1, Math.round(segmentos / MERIDIANOS)));

    const celdas = [];

    for (let a = 0; a < cortesFila.length - 1; a++) {
        const i0 = cortesFila[a];
        const i1 = cortesFila[a + 1];

        for (let b = 0; b < cortesSegmento.length - 1; b++) {
            const j0 = cortesSegmento[b];
            const j1 = cortesSegmento[b + 1];

            // El contorno da la vuelta a la celda pasando por cada punto fino.
            const contorno = [];
            for (let j = j0; j <= j1; j++) {
                contorno.push(anillos[i0][j]);
            }
            for (let i = i0 + 1; i <= i1; i++) {
                contorno.push(anillos[i][j1]);
            }
            for (let j = j1 - 1; j >= j0; j--) {
                contorno.push(anillos[i1][j]);
            }
            for (let i = i1 - 1; i > i0; i--) {
                contorno.push(anillos[i][j0]);
            }

            // Cada celda traza solo su paralelo de abajo y su meridiano de la
            // izquierda; el resto lo trazan las celdas vecinas. Asi cada linea
            // de la reticula se dibuja una sola vez.
            const lineas = [
                anillos[i0].slice(j0, j1 + 1),
                anillos.slice(i0, i1 + 1).map((anillo) => anillo[j0])
            ];
            if (i1 === filas - 1) {
                lineas.push(anillos[i1].slice(j0, j1 + 1));
            }

            celdas.push(crearCelda(contorno, lineas, tipo));
        }
    }

    return celdas;
}

/**
 * Cierra el solido con las tapas de los dos extremos.
 *
 * La tapa es el corte del solido por un plano perpendicular al eje: un circulo
 * si es macizo, un anillo si tiene hueco. Es justo la rebanada del metodo, y
 * verla en el extremo ayuda a distinguir de un vistazo los discos de las
 * arandelas.
 *
 * @param {Array<Array<object>>} exterior anillos de la superficie exterior
 * @param {Array<Array<object>>} interior anillos de la superficie del hueco
 * @param {boolean} ejeHorizontal true si el eje de giro es horizontal
 * @returns {Array<object>} las celdas de las tapas
 */
function celdasDeTapas(exterior, interior, ejeHorizontal) {
    if (exterior.length < 2) {
        return [];
    }

    const axial = (punto) => (ejeHorizontal ? punto.x : punto.y);
    const segmentos = Math.min(...exterior.map((anillo) => anillo.length)) - 1;
    const cortesSegmento = cortes(segmentos, Math.max(1, Math.round(segmentos / MERIDIANOS)));

    const celdas = [];

    for (const anillo of [exterior[0], exterior[exterior.length - 1]]) {
        const centro = promedioDePuntos(anillo.slice(0, segmentos));
        const radio = Math.max(...anillo.map((punto) => distancia(punto, centro)));

        // Donde el perfil toca el eje no hay tapa: el solido termina en punta.
        if (radio < 1e-9) {
            continue;
        }

        const posicion = axial(anillo[0]);
        const tolerancia = 1e-6 * (1 + Math.abs(posicion));
        const hueco = interior.find((otro) =>
            otro.length === anillo.length && Math.abs(axial(otro[0]) - posicion) <= tolerancia);

        for (let b = 0; b < cortesSegmento.length - 1; b++) {
            const j0 = cortesSegmento[b];
            const j1 = cortesSegmento[b + 1];

            const contorno = anillo.slice(j0, j1 + 1);
            if (hueco) {
                for (let j = j1; j >= j0; j--) {
                    contorno.push(hueco[j]);
                }
            } else {
                contorno.push(centro);
            }

            celdas.push(crearCelda(contorno, [], "tapa"));
        }
    }

    return celdas;
}

/**
 * Crea una celda con lo que hace falta para pintarla: su centro, para
 * ordenarla, y su normal, para iluminarla.
 *
 * La normal se calcula por el metodo de Newell, que funciona con cualquier
 * poligono aunque no sea perfectamente plano y no se rompe cuando dos de sus
 * vertices coinciden, como pasa en las celdas que tocan el eje.
 */
function crearCelda(contorno, lineas, tipo) {
    let nx = 0;
    let ny = 0;
    let nz = 0;

    for (let k = 0; k < contorno.length; k++) {
        const p = contorno[k];
        const q = contorno[(k + 1) % contorno.length];
        nx += (p.y - q.y) * (p.z + q.z);
        ny += (p.z - q.z) * (p.x + q.x);
        nz += (p.x - q.x) * (p.y + q.y);
    }

    const largo = Math.hypot(nx, ny, nz);
    const normal = largo > 1e-12
        ? { x: nx / largo, y: ny / largo, z: nz / largo }
        : { x: 0, y: 0, z: 0 };

    return { contorno, lineas, tipo, normal, centro: promedioDePuntos(contorno) };
}

/**
 * Elige la longitud de los ejes y cada cuanto poner marcas.
 *
 * Los ejes se extienden bastante mas alla del solido, como en GeoGebra, para
 * que se vean como rectas que atraviesan la escena y no como un marco pegado
 * a la figura. Las marcas caen en numeros redondos.
 */
function medidasDeLosEjes(extremos) {
    const mayor = Math.max(1e-6,
        Math.abs(extremos.xMinimo), Math.abs(extremos.xMaximo),
        Math.abs(extremos.yMinimo), Math.abs(extremos.yMaximo),
        Math.abs(extremos.zMinimo), Math.abs(extremos.zMaximo));

    const alcance = mayor * 1.6;
    const paso = pasoRedondo(alcance / 4);
    const ultimaMarca = Math.ceil(alcance / paso - 1e-9) * paso;

    return { paso, ultimaMarca, largo: ultimaMarca + paso * 0.6 };
}

/**
 * Decide hasta donde llega cada eje dentro del lienzo.
 *
 * Por el lado positivo el eje se corta antes del borde, para que la flecha y
 * el nombre se vean: un eje sin flecha no dice hacia donde crece. Por el lado
 * negativo se deja correr hasta el borde, como hace GeoGebra.
 *
 * Se calcula una vez por encuadre y no en cada cuadro. Si se recalculara
 * mientras se gira la camara, la punta de los ejes se deslizaria con cada
 * movimiento del raton, y lo que se espera es que los ejes sean rectas fijas
 * y lo que gire sea la vista.
 */
function alcanceDeLosEjes(geometria, escena) {
    const guardado = alcances.get(escena);
    if (guardado && guardado.encuadre === escena.encuadres && guardado.geometria === geometria) {
        return guardado.alcance;
    }

    const { largo, paso } = geometria.ejes;
    const origen = escena.proyectar({ x: 0, y: 0, z: 0 });

    const hastaElBorde = (unidad) => {
        const punto = escena.proyectar(unidad);
        const dx = punto.x - origen.x;
        const dy = punto.y - origen.y;

        // El punto del eje recorre la pantalla en linea recta; se busca cuanto
        // puede avanzar antes de que la punta se acerque demasiado al borde.
        let t = Infinity;
        if (dx > 1e-9) {
            t = Math.min(t, (escena.ancho - MARGEN_PUNTA - origen.x) / dx);
        } else if (dx < -1e-9) {
            t = Math.min(t, (MARGEN_PUNTA - origen.x) / dx);
        }
        if (dy > 1e-9) {
            t = Math.min(t, (escena.alto - MARGEN_PUNTA - origen.y) / dy);
        } else if (dy < -1e-9) {
            t = Math.min(t, (MARGEN_PUNTA - origen.y) / dy);
        }

        return Math.max(Math.min(largo, paso), Math.min(largo, t));
    };

    const alcance = {
        x: { positivo: hastaElBorde({ x: 1, y: 0, z: 0 }), negativo: largo },
        y: { positivo: hastaElBorde({ x: 0, y: 1, z: 0 }), negativo: largo },
        z: { positivo: hastaElBorde({ x: 0, y: 0, z: 1 }), negativo: largo }
    };

    alcances.set(escena, { encuadre: escena.encuadres, geometria, alcance });
    return alcance;
}

/**
 * Calcula que hay que ver al abrir el visor: el solido y el origen.
 *
 * Se incluye el origen para que los tres ejes se crucen dentro de la imagen
 * aunque el solido este apartado de el, y se deja un margen para que los ejes
 * asomen alrededor de la figura.
 */
function encuadreInicial(extremos) {
    const caja = {
        xMinimo: Math.min(0, extremos.xMinimo), xMaximo: Math.max(0, extremos.xMaximo),
        yMinimo: Math.min(0, extremos.yMinimo), yMaximo: Math.max(0, extremos.yMaximo),
        zMinimo: Math.min(0, extremos.zMinimo), zMaximo: Math.max(0, extremos.zMaximo)
    };

    const mitades = [
        (caja.xMaximo - caja.xMinimo) / 2,
        (caja.yMaximo - caja.yMinimo) / 2,
        (caja.zMaximo - caja.zMinimo) / 2
    ];
    const mayor = Math.max(...mitades, 1e-6);

    // Un solido muy aplanado en una direccion no debe quedar sin aire en ella.
    const [mx, my, mz] = mitades.map((mitad) => Math.max(mitad, mayor * 0.2) * 1.35);
    const cx = (caja.xMinimo + caja.xMaximo) / 2;
    const cy = (caja.yMinimo + caja.yMaximo) / 2;
    const cz = (caja.zMinimo + caja.zMaximo) / 2;

    return {
        xMinimo: cx - mx, xMaximo: cx + mx,
        yMinimo: cy - my, yMaximo: cy + my,
        zMinimo: cz - mz, zMaximo: cz + mz
    };
}

// ==================================================================
// PIEZAS QUE SE ORDENAN
// ==================================================================

/**
 * Convierte las celdas del solido en piezas listas para ordenar y pintar.
 *
 * La luz es suave a proposito. GeoGebra no sombrea con fuerza, y aqui el
 * sombreado solo tiene que insinuar la curvatura: lo que define la forma es
 * la malla.
 */
function piezasDelSolido(geometria, escena, paleta, mostrarMalla) {
    return geometria.celdas.map((celda) => {
        const puntos = celda.contorno.map((punto) => escena.proyectar(punto));
        const lineas = mostrarMalla
            ? celda.lineas.map((linea) => linea.map((punto) => escena.proyectar(punto)))
            : [];

        // Se ilumina por las dos caras: la de atras del solido tambien se ve
        // a traves de la transparencia y no debe quedar negra.
        const normal = escena.aVista(celda.normal);
        const incidencia = Math.abs(
            normal.derecha * LUZ.derecha + normal.arriba * LUZ.arriba + normal.cerca * LUZ.cerca);
        const brillo = 0.72 + 0.38 * incidencia;

        const base = celda.tipo === "interior" ? paleta.solidoInterior : paleta.solido;
        const alfa = celda.tipo === "tapa" ? ALFA_TAPA : ALFA_SUPERFICIE;
        const relleno = rgba(base, brillo, alfa);
        const trazo = celda.tipo === "interior" ? paleta.mallaTenue : paleta.malla;

        return {
            z: celda.centro.z,
            profundidad: escena.proyectar(celda.centro).profundidad,
            dibujar(contexto) {
                contexto.beginPath();
                contexto.moveTo(puntos[0].x, puntos[0].y);
                for (let k = 1; k < puntos.length; k++) {
                    contexto.lineTo(puntos[k].x, puntos[k].y);
                }
                contexto.closePath();
                contexto.fillStyle = relleno;
                contexto.fill();

                if (lineas.length > 0) {
                    contexto.strokeStyle = trazo;
                    contexto.lineWidth = 0.8;
                    contexto.lineJoin = "round";
                    for (const linea of lineas) {
                        contexto.beginPath();
                        contexto.moveTo(linea[0].x, linea[0].y);
                        for (let k = 1; k < linea.length; k++) {
                            contexto.lineTo(linea[k].x, linea[k].y);
                        }
                        contexto.stroke();
                    }
                }
            }
        };
    });
}

/**
 * Parte los tres ejes en tramos que se ordenan junto con el solido.
 *
 * Si los ejes se pintaran enteros al principio quedarian siempre detras, y al
 * final, siempre delante. Partidos, cada tramo cae en su sitio: el eje X que
 * atraviesa un solido girado sobre el se ve a traves de la superficie, y el
 * trozo del eje Y que viene hacia el observador pasa por delante.
 */
function piezasDeLosEjes(alcance, escena, paleta) {
    const piezas = [];

    const ejes = [
        { color: paleta.ejeX, alcance: alcance.x, punto: (t) => ({ x: t, y: 0, z: 0 }) },
        { color: paleta.ejeY, alcance: alcance.y, punto: (t) => ({ x: 0, y: t, z: 0 }) },
        { color: paleta.ejeZ, alcance: alcance.z, punto: (t) => ({ x: 0, y: 0, z: t }) }
    ];

    // Cada mitad se parte por separado para que el origen sea siempre un
    // corte: un tramo que cruzara el plano XY no tendria un lado claro.
    const mitad = TRAMOS_EJE / 2;

    for (const eje of ejes) {
        const cortesDelEje = [];
        for (let k = mitad; k >= 1; k--) {
            cortesDelEje.push((-eje.alcance.negativo * k) / mitad);
        }
        for (let k = 0; k <= mitad; k++) {
            cortesDelEje.push((eje.alcance.positivo * k) / mitad);
        }

        for (let k = 0; k < cortesDelEje.length - 1; k++) {
            const desde = cortesDelEje[k];
            const hasta = cortesDelEje[k + 1];

            const a = escena.proyectar(eje.punto(desde));
            const b = escena.proyectar(eje.punto(hasta));

            piezas.push({
                z: eje.punto((desde + hasta) / 2).z,
                profundidad: (a.profundidad + b.profundidad) / 2,
                dibujar(contexto) {
                    contexto.strokeStyle = eje.color;
                    contexto.lineWidth = 1.8;
                    contexto.lineCap = "round";
                    contexto.setLineDash([]);
                    contexto.beginPath();
                    contexto.moveTo(a.x, a.y);
                    contexto.lineTo(b.x, b.y);
                    contexto.stroke();
                }
            });
        }
    }

    return piezas;
}

/**
 * Traza el eje de giro cuando no coincide con un eje coordenado.
 *
 * Girando sobre el eje X o el eje Y no hace falta: el propio eje ya esta
 * dibujado. Pero si la region gira alrededor de una recta como y = -1, esa
 * recta es la que explica el solido —el radio se mide desde ella— y tiene
 * que verse. Va a trazos para no confundirla con los ejes.
 */
function piezasDelEjeDeGiro(datos, geometria, escena, paleta) {
    const k = datos.eje?.desplazamiento ?? 0;
    if (Math.abs(k) < 1e-12) {
        return [];
    }

    const horizontal = datos.eje?.orientacion !== "VERTICAL";
    const largo = geometria.ejes.largo;
    const punto = (t) => (horizontal ? { x: t, y: k, z: 0 } : { x: k, y: t, z: 0 });

    const tramos = TRAMOS_EJE;
    const proyectados = [];
    for (let i = 0; i <= tramos; i++) {
        proyectados.push(escena.proyectar(punto(-largo + (2 * largo * i) / tramos)));
    }

    // Cada tramo empieza el patron de trazos donde lo dejo el anterior, para
    // que la linea discontinua no se vea cortada en pedazos.
    const piezas = [];
    let recorrido = 0;

    for (let i = 0; i < tramos; i++) {
        const a = proyectados[i];
        const b = proyectados[i + 1];
        const desfase = recorrido;
        recorrido += Math.hypot(b.x - a.x, b.y - a.y);

        piezas.push({
            z: 0,
            profundidad: (a.profundidad + b.profundidad) / 2,
            dibujar(contexto) {
                contexto.strokeStyle = paleta.ejeGiro;
                contexto.lineWidth = 2;
                contexto.lineCap = "butt";
                contexto.setLineDash([10, 6]);
                contexto.lineDashOffset = desfase;
                contexto.beginPath();
                contexto.moveTo(a.x, a.y);
                contexto.lineTo(b.x, b.y);
                contexto.stroke();
                contexto.setLineDash([]);
                contexto.lineDashOffset = 0;
            }
        });
    }

    return piezas;
}

// ==================================================================
// PLANO Y ROTULOS
// ==================================================================

/**
 * Pinta el plano XY como un suelo gris que se desvanece hacia los bordes.
 *
 * Con proyeccion paralela, el cuadrado del plano se ve como un
 * paralelogramo, que es una transformacion afin del cuadrado. Por eso basta
 * con aplicar esa transformacion al contexto y dibujar el degradado en las
 * coordenadas del propio plano: el desvanecimiento llega igual a los cuatro
 * bordes, por mucho que el plano se vea inclinado.
 */
function dibujarPlano(contexto, geometria, escena, paleta) {
    const lado = geometria.ejes.largo;

    const origen = escena.proyectar({ x: 0, y: 0, z: 0 });
    const unoEnX = escena.proyectar({ x: 1, y: 0, z: 0 });
    const unoEnY = escena.proyectar({ x: 0, y: 1, z: 0 });

    contexto.save();
    contexto.transform(
        unoEnX.x - origen.x, unoEnX.y - origen.y,
        unoEnY.x - origen.x, unoEnY.y - origen.y,
        origen.x, origen.y);

    const degradado = contexto.createRadialGradient(0, 0, 0, 0, 0, lado * Math.SQRT2);
    degradado.addColorStop(0, rgba(paleta.plano, 1, 0.34));
    degradado.addColorStop(0.5, rgba(paleta.plano, 1, 0.28));
    degradado.addColorStop(0.72, rgba(paleta.plano, 1, 0.12));
    degradado.addColorStop(1, rgba(paleta.plano, 1, 0));

    contexto.fillStyle = degradado;
    contexto.fillRect(-lado, -lado, 2 * lado, 2 * lado);
    contexto.restore();
}

/**
 * Pinta las flechas, las marcas, los numeros y el nombre de cada eje.
 *
 * Los numeros se colocan siempre del mismo lado del eje en la pantalla
 * —debajo si el eje se ve tendido, a la izquierda si se ve de pie— para que
 * no salten de un lado a otro mientras se gira la camara. Cuando un eje se ve
 * muy en escorzo las marcas quedan apretadas y se rotula solo una de cada
 * tantas, para que los numeros no se monten.
 */
function dibujarRotulos(contexto, geometria, alcance, escena, paleta) {
    const { paso, ultimaMarca } = geometria.ejes;
    const cuantas = Math.round(ultimaMarca / paso);

    const ejes = [
        { nombre: "x", color: paleta.ejeX, alcance: alcance.x, unidad: { x: 1, y: 0, z: 0 } },
        { nombre: "y", color: paleta.ejeY, alcance: alcance.y, unidad: { x: 0, y: 1, z: 0 } },
        { nombre: "z", color: paleta.ejeZ, alcance: alcance.z, unidad: { x: 0, y: 0, z: 1 } }
    ];
    const sobreEje = (unidad, t) => ({ x: unidad.x * t, y: unidad.y * t, z: unidad.z * t });

    contexto.save();
    contexto.setLineDash([]);

    for (const eje of ejes) {
        const largo = eje.alcance.positivo;
        const punta = escena.proyectar(sobreEje(eje.unidad, largo));
        const previo = escena.proyectar(sobreEje(eje.unidad, largo - paso));

        let dx = punta.x - previo.x;
        let dy = punta.y - previo.y;
        const pixelesPorPaso = Math.hypot(dx, dy);

        // Un eje que apunta a la camara se reduce a un punto y no tiene
        // direccion en la pantalla: no hay flecha ni marcas que poner.
        if (pixelesPorPaso < 2) {
            continue;
        }
        dx /= pixelesPorPaso;
        dy /= pixelesPorPaso;

        // Perpendicular en pantalla, orientada hacia donde van los numeros.
        let nx = -dy;
        let ny = dx;
        if (Math.abs(ny) > 0.3) {
            if (ny < 0) {
                nx = -nx;
                ny = -ny;
            }
        } else if (nx > 0) {
            nx = -nx;
            ny = -ny;
        }

        contexto.fillStyle = eje.color;
        contexto.strokeStyle = eje.color;

        // Flecha.
        contexto.beginPath();
        contexto.moveTo(punta.x + dx * 5, punta.y + dy * 5);
        contexto.lineTo(punta.x - dx * 9 + nx * 5, punta.y - dy * 9 + ny * 5);
        contexto.lineTo(punta.x - dx * 9 - nx * 5, punta.y - dy * 9 - ny * 5);
        contexto.closePath();
        contexto.fill();

        // Nombre del eje, del lado opuesto a los numeros.
        contexto.font = "italic 600 15px system-ui, sans-serif";
        contexto.textAlign = "center";
        contexto.textBaseline = "middle";
        contexto.fillText(eje.nombre, punta.x + dx * 16 - nx * 11, punta.y + dy * 16 - ny * 11);

        // Marcas y numeros.
        const saltar = Math.max(1, Math.ceil(28 / pixelesPorPaso));
        contexto.font = "12px system-ui, sans-serif";
        contexto.lineWidth = 1.6;

        for (let m = -cuantas; m <= cuantas; m++) {
            const valor = m * paso;

            // Ni el cero, ni marcas fuera del eje, ni una tan pegada a la punta
            // que su numero quede debajo de la flecha.
            if (m === 0 || valor > largo - paso * 0.35 || valor < -eje.alcance.negativo) {
                continue;
            }
            const marca = escena.proyectar(sobreEje(eje.unidad, valor));

            contexto.beginPath();
            contexto.moveTo(marca.x - nx * 3.5, marca.y - ny * 3.5);
            contexto.lineTo(marca.x + nx * 3.5, marca.y + ny * 3.5);
            contexto.stroke();

            if (m % saltar === 0) {
                contexto.fillText(formatearEje(m * paso), marca.x + nx * 14, marca.y + ny * 14);
            }
        }
    }

    // El cero se rotula una sola vez, en el origen.
    const origen = escena.proyectar({ x: 0, y: 0, z: 0 });
    contexto.fillStyle = paleta.texto;
    contexto.font = "12px system-ui, sans-serif";
    contexto.textAlign = "right";
    contexto.textBaseline = "top";
    contexto.fillText("0", origen.x - 5, origen.y + 4);

    contexto.restore();
}

/** Recuerda en una esquina alrededor de que recta gira la region. */
function dibujarLeyenda(contexto, datos, paleta) {
    if (!datos.eje?.ecuacion) {
        return;
    }
    contexto.save();
    contexto.fillStyle = paleta.texto;
    contexto.font = "13px system-ui, sans-serif";
    contexto.textAlign = "left";
    contexto.textBaseline = "top";
    contexto.fillText(`Eje de giro: ${datos.eje.ecuacion}`, 12, 10);
    contexto.restore();
}

// ==================================================================
// BOTONES DEL VISOR
// ==================================================================

/** Iconos de los botones, dibujados con trazos para que sigan el color del tema. */
const ICONOS = {
    inicio: '<path d="M3 11.5 12 4l9 7.5"/><path d="M5.5 10v9.5h13V10"/>',
    acercar: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m15.5 15.5 5 5M10.5 7.5v6M7.5 10.5h6"/>',
    alejar: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m15.5 15.5 5 5M7.5 10.5h6"/>',
    pantalla: '<path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/>'
};

/**
 * Pone sobre el visor la columna de botones redondos de GeoGebra: volver a
 * la vista inicial, acercar, alejar y pantalla completa.
 *
 * Se crean una sola vez por visor. Igual que los controles del raton, actuan
 * sobre la camara vigente, que se lee del canvas en el momento del clic.
 */
function colocarBotones(lienzo) {
    const caja = lienzo.parentElement;
    if (!caja || caja.querySelector(".visor3d__controles")) {
        return;
    }

    const barra = document.createElement("div");
    barra.className = "visor3d__controles";

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
        const vigente = lienzo.__visor3d;
        if (!boton || !vigente) {
            return;
        }

        switch (boton.dataset.accion) {
            case "inicio":
                vigente.escena.reiniciar();
                break;
            case "acercar":
                vigente.escena.acercar(1.25);
                break;
            case "alejar":
                vigente.escena.acercar(0.8);
                break;
            case "pantalla":
                alternarPantallaCompleta(caja);
                return;
            default:
                return;
        }
        vigente.redibujar();
    });

    // Al entrar o salir de pantalla completa el canvas cambia de tamano, y
    // se vuelve a encuadrar para aprovechar el espacio nuevo.
    document.addEventListener("fullscreenchange", () => {
        requestAnimationFrame(() => {
            const vigente = lienzo.__visor3d;
            if (!vigente || !lienzo.isConnected) {
                return;
            }
            vigente.escena.ancho = lienzo.clientWidth;
            vigente.escena.alto = lienzo.clientHeight;
            if (vigente.escena.caja) {
                vigente.escena.encuadrar(vigente.escena.caja);
            }
            vigente.redibujar();
        });
    });

    caja.appendChild(barra);
}

/** Entra o sale de pantalla completa con el visor. */
function alternarPantallaCompleta(caja) {
    if (document.fullscreenElement) {
        document.exitFullscreen?.();
    } else {
        caja.requestFullscreen?.();
    }
}

// ==================================================================
// APOYO
// ==================================================================

/**
 * Lee los colores del tema actual.
 *
 * Viven en variables.css, como los de la grafica plana, para que el visor
 * cambie solo al pasar al tema oscuro.
 */
function leerPaleta() {
    const leer = (nombre, respaldo) => color(nombre) || respaldo;
    const solido = aComponentes(leer("grafica3d-solido", "#6b4f96"));

    return {
        fondo: leer("grafica-fondo", "#ffffff"),
        texto: leer("color-texto-suave", "#475569"),
        ejeX: leer("grafica3d-eje-x", "#cc0000"),
        ejeY: leer("grafica3d-eje-y", "#008000"),
        ejeZ: leer("grafica3d-eje-z", "#0000cc"),
        ejeGiro: leer("grafica-corte", "#7c3aed"),
        plano: aComponentes(leer("grafica3d-plano", "#808080")),
        solido,
        solidoInterior: solido.map((valor) => valor * 0.8),
        malla: rgba(aComponentes(leer("grafica3d-malla", "#2a2233")), 1, 0.72),
        mallaTenue: rgba(aComponentes(leer("grafica3d-malla", "#2a2233")), 1, 0.4)
    };
}

/**
 * Convierte un color CSS cualquiera en sus tres componentes.
 *
 * El color puede estar escrito de muchas formas —hexadecimal, rgb(), un
 * nombre—, asi que en lugar de interpretarlo a mano se le pide al navegador
 * que lo haga: se aplica a un elemento y se lee de vuelta, que siempre
 * devuelve la forma rgb().
 *
 * @param {string} colorCss color en cualquier notacion
 * @returns {number[]} las componentes roja, verde y azul
 */
function aComponentes(colorCss) {
    const sonda = document.createElement("span");
    sonda.style.display = "none";
    sonda.style.color = colorCss;
    document.body.appendChild(sonda);

    const resuelto = getComputedStyle(sonda).color;
    sonda.remove();

    const numeros = resuelto.match(/\d+(\.\d+)?/g);
    if (!numeros || numeros.length < 3) {
        return [107, 79, 150];
    }
    return numeros.slice(0, 3).map(Number);
}

/**
 * Arma un color rgba() a partir de componentes, un brillo y una opacidad.
 *
 * Se calcula sobre las componentes en lugar de recurrir a color-mix del CSS:
 * esa funcion no esta garantizada como valor de fillStyle en el canvas, y si
 * el navegador no la entiende no avisa, simplemente ignora la asignacion.
 */
function rgba(componentes, brillo, alfa) {
    const [rojo, verde, azul] = componentes.map(
        (valor) => Math.max(0, Math.min(255, Math.round(valor * brillo))));
    return `rgba(${rojo}, ${verde}, ${azul}, ${alfa})`;
}

/** Se queda con los anillos que se pueden dibujar. */
function anillosValidos(malla) {
    if (!Array.isArray(malla)) {
        return [];
    }
    return malla.filter((anillo) =>
        Array.isArray(anillo) && anillo.length >= 3 && anillo.every(esValido));
}

/**
 * Indica si un punto de la malla se puede dibujar.
 *
 * @param {object} punto punto a revisar
 * @returns {boolean} true si sus tres coordenadas son numeros finitos
 */
function esValido(punto) {
    return punto
        && Number.isFinite(punto.x)
        && Number.isFinite(punto.y)
        && Number.isFinite(punto.z);
}

/** La caja que envuelve a los puntos. */
function medir(puntos) {
    if (puntos.length === 0) {
        return { xMinimo: -1, xMaximo: 1, yMinimo: -1, yMaximo: 1, zMinimo: -1, zMaximo: 1 };
    }
    const caja = {
        xMinimo: Infinity, xMaximo: -Infinity,
        yMinimo: Infinity, yMaximo: -Infinity,
        zMinimo: Infinity, zMaximo: -Infinity
    };
    for (const punto of puntos) {
        caja.xMinimo = Math.min(caja.xMinimo, punto.x);
        caja.xMaximo = Math.max(caja.xMaximo, punto.x);
        caja.yMinimo = Math.min(caja.yMinimo, punto.y);
        caja.yMaximo = Math.max(caja.yMaximo, punto.y);
        caja.zMinimo = Math.min(caja.zMinimo, punto.z);
        caja.zMaximo = Math.max(caja.zMaximo, punto.z);
    }
    return caja;
}

/** Los indices de corte 0, paso, 2*paso... terminando siempre en el ultimo. */
function cortes(ultimo, paso) {
    const lista = [];
    for (let i = 0; i < ultimo; i += paso) {
        lista.push(i);
    }
    lista.push(ultimo);
    return lista;
}

/** El numero redondo —1, 2 o 5 por una potencia de diez— que no queda por debajo del dado. */
function pasoRedondo(valor) {
    const magnitud = Math.pow(10, Math.floor(Math.log10(valor)));
    const mantisa = valor / magnitud;
    if (mantisa <= 1) {
        return magnitud;
    }
    if (mantisa <= 2) {
        return 2 * magnitud;
    }
    if (mantisa <= 5) {
        return 5 * magnitud;
    }
    return 10 * magnitud;
}

/** El punto medio de una lista de puntos. */
function promedioDePuntos(puntos) {
    const suma = puntos.reduce(
        (total, punto) => ({ x: total.x + punto.x, y: total.y + punto.y, z: total.z + punto.z }),
        { x: 0, y: 0, z: 0 });
    return { x: suma.x / puntos.length, y: suma.y / puntos.length, z: suma.z / puntos.length };
}

/** Distancia entre dos puntos del espacio. */
function distancia(p, q) {
    return Math.hypot(p.x - q.x, p.y - q.y, p.z - q.z);
}

/** Lleva un vector a longitud uno. */
function normalizar(v) {
    const largo = Math.hypot(v.derecha, v.arriba, v.cerca);
    return { derecha: v.derecha / largo, arriba: v.arriba / largo, cerca: v.cerca / largo };
}
