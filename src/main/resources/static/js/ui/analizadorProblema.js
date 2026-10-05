/**
 * analizadorProblema.js
 * Impulsa la pagina donde el estudiante escribe el problema completo.
 *
 * El recorrido tiene dos tiempos, y estan separados a proposito:
 *
 *   1. Analizar. Se manda el enunciado, el servidor lo interpreta y devuelve el
 *      ejercicio estructurado. Con eso se llenan los campos y se muestra el
 *      razonamiento seguido, pero no se calcula nada todavia.
 *
 *   2. Resolver. El estudiante revisa esos datos, corrige lo que haga falta y
 *      lanza el calculo.
 *
 * Ese corte es lo que convierte la interpretacion automatica en una ayuda y no
 * en una imposicion: nada se resuelve con datos que el estudiante no haya visto
 * antes.
 */

import {
    analizarProblema,
    calcularAreaBajoCurva,
    calcularAreaEntreCurvas,
    calcularSolidoRevolucion
} from "../api/clienteApi.js";

import * as mensajes from "./mensajes.js";
import * as panelResultados from "./panelResultados.js";
import * as panelPasos from "./panelPasos.js";
import * as selectorMetodo from "./selectorMetodo.js";

import { dibujar as dibujar2D, conectarLectura } from "../graficas/grafica2d.js";
import { dibujarSolido, pintar as pintarSolido } from "../graficas/grafica3d.js";
import { embellecer, embellecerPaso } from "../util/formatoMatematico.js";

/** Lo que devolvio el ultimo analisis, para saber que tema resolver. */
let interpretacion = null;

/** El solido que se esta mostrando, para redibujarlo al cambiar el tamano. */
let solidoVigente = null;

/** Si ya se registro el oyente del tamano de la ventana. */
let escuchandoTamano = false;

/** Prepara la pagina de resolver un problema escrito. */
export function iniciarResolver() {
    const enunciado = document.getElementById("enunciado");
    const botonAnalizar = document.getElementById("botonAnalizar");
    const botonResolver = document.getElementById("botonResolver");
    const formulario = document.getElementById("formularioDatos");

    selectorMetodo.llenarTiposDeSolido(
        document.getElementById("tipoSolido"),
        document.getElementById("descripcionSolido"));

    botonAnalizar?.addEventListener("click", () => analizar(enunciado?.value ?? ""));

    formulario?.addEventListener("submit", (evento) => {
        evento.preventDefault();
        resolver();
    });

    botonResolver?.addEventListener("click", (evento) => {
        evento.preventDefault();
        resolver();
    });

    // El tema decide que campos tienen sentido; cambiarlo a mano tambien debe
    // reorganizar el formulario.
    document.getElementById("tema")?.addEventListener("change", () => {
        ajustarCamposAlTema(document.getElementById("tema").value);
    });

    // La variable de integracion decide como se nombran las funciones: con x
    // son y = f(x), con y son x = g(y). En un volumen la decide el metodo
    // junto con el eje, asi que tambien hay que escuchar esos dos campos.
    for (const id of ["variable", "tipoSolido", "eje"]) {
        document.getElementById(id)?.addEventListener("change", actualizarEtiquetas);
    }

    document.getElementById("limitesAutomaticos")?.addEventListener("change", actualizarLimites);

    // Los ejemplos del enunciado ahorran escribir un problema entero para probar.
    for (const boton of document.querySelectorAll("[data-ejemplo]")) {
        boton.addEventListener("click", () => {
            enunciado.value = boton.dataset.ejemplo;
            analizar(enunciado.value);
        });
    }

    ajustarCamposAlTema(document.getElementById("tema")?.value ?? "");
    actualizarLimites();
}

// ------------------------------------------------------------------
// PASO 1: ANALIZAR
// ------------------------------------------------------------------

/**
 * Manda el enunciado al servidor y vuelca el resultado en el formulario.
 *
 * @param {string} texto enunciado escrito por el estudiante
 */
async function analizar(texto) {
    const zonaMensajes = document.getElementById("mensajes");
    const zonaInterpretacion = document.getElementById("interpretacion");

    if (!texto.trim()) {
        mensajes.mostrar(zonaMensajes, "Escriba primero el enunciado del problema.", "atencion");
        return;
    }

    mensajes.limpiar(zonaMensajes);
    mensajes.mostrarCargando(zonaInterpretacion, "Interpretando el enunciado...");

    try {
        interpretacion = await analizarProblema(texto);

        llenarCampos(interpretacion);
        mostrarInterpretacion(interpretacion);

        document.getElementById("panelDatos")?.classList.remove("oculto");

    } catch (error) {
        mensajes.mostrarError(zonaMensajes, error);
        zonaInterpretacion.innerHTML = "";
    }
}

/**
 * Copia los datos interpretados a los campos del formulario.
 *
 * Lo que el servidor no pudo determinar llega como null y el campo se deja
 * vacio. Rellenarlo con un cero seria peor que dejarlo en blanco: el estudiante
 * podria darlo por bueno sin notar que nadie lo dedujo.
 *
 * @param {object} datos interpretacion devuelta por el servidor
 */
function llenarCampos(datos) {
    const fijar = (id, valor) => {
        const campo = document.getElementById(id);
        if (campo) {
            campo.value = (valor === null || valor === undefined) ? "" : valor;
        }
    };

    fijar("tema", datos.tema);
    fijar("funcionF", datos.funcionF);
    fijar("funcionG", datos.funcionG);
    fijar("limiteInferior", datos.limiteInferior);
    fijar("limiteSuperior", datos.limiteSuperior);
    fijar("variable", datos.variable === "y" ? "y" : "x");

    if (datos.eje) {
        fijar("eje", datos.eje.orientacion === "VERTICAL" ? "y" : "x");
        fijar("desplazamientoEje", datos.eje.desplazamiento);
    }

    if (datos.tipoSolido) {
        fijar("tipoSolido", datos.tipoSolido);
        // Cambiar el valor desde el codigo no avisa a nadie: sin este evento, la
        // formula y la descripcion debajo del selector seguirian siendo las del
        // metodo anterior.
        document.getElementById("tipoSolido")?.dispatchEvent(new Event("change"));
    }

    // Los limites que el analizador dedujo se escriben y la casilla de
    // buscarlos se desmarca: ya estan buscados, y la cuenta se muestra arriba.
    const casilla = document.getElementById("limitesAutomaticos");
    if (casilla && datos.limiteInferior !== null && datos.limiteSuperior !== null) {
        casilla.checked = false;
        actualizarLimites();
    }

    ajustarCamposAlTema(datos.tema);
}

/**
 * Muestra que entendio el sistema y en que se baso.
 *
 * @param {object} datos interpretacion devuelta por el servidor
 */
function mostrarInterpretacion(datos) {
    const zona = document.getElementById("interpretacion");
    if (!zona) {
        return;
    }

    zona.innerHTML = "";

    // ---------- Que se entendio ----------
    const resumen = document.createElement("dl");
    resumen.className = "resultado__detalles";
    resumen.style.marginTop = "0";

    agregarDato(resumen, "Tema", datos.temaEtiqueta);

    // Las curvas tal como las escribio el enunciado. Si hubo que despejarlas,
    // se ve al lado la forma que se usa para integrar: y^2 = 8x se integra
    // como y = sqrt(8x), y eso no puede aparecer sin decir de donde salio.
    for (const curva of datos.curvas ?? []) {
        agregarDato(resumen, "Curva del enunciado", embellecer(curva.ecuacion)
            + describirDespeje(curva, datos.variable));
    }

    const v = datos.variable === "y" ? "y" : "x";
    const otra = v === "x" ? "y" : "x";
    agregarDato(resumen, "Variable de integración", v === "x"
        ? "x (rebanadas verticales)"
        : "y (rebanadas horizontales: las curvas se usan como x = g(y))");

    if (datos.funcionF) {
        agregarDato(resumen, "Primer borde", `${otra} = ${embellecer(datos.funcionF)}`);
    }
    if (datos.funcionG) {
        agregarDato(resumen, "Segundo borde", `${otra} = ${embellecer(datos.funcionG)}`);
    }

    // Las fronteras de la región van antes que el eje y con su propio nombre, a
    // propósito: son los dos datos que más fácilmente se confunden. "La ordenada
    // 2" es una recta que cierra la región, no la recta alrededor de la cual
    // gira, y verlas en renglones distintos es lo que evita leerlas como una
    // sola cosa.
    for (const frontera of datos.fronteras ?? []) {
        agregarDato(resumen, "Frontera de la región",
            `${frontera.ecuacion}  (${frontera.origenEtiqueta})`);
    }

    if (datos.eje) {
        agregarDato(resumen, "Eje de revolución", datos.eje.ecuacion);
    }

    if (datos.tipoSolidoEtiqueta) {
        // Se distingue el método que pidió el enunciado del que dedujo el
        // sistema: son cosas distintas y el estudiante tiene que saber cuál
        // está mirando.
        const pedido = datos.metodoSolicitadoEtiqueta;
        const nota = pedido ? "lo pide el enunciado" : "deducido de la geometría";
        agregarDato(resumen, "Método", `${datos.tipoSolidoEtiqueta}  (${nota})`);

        if (pedido && datos.metodoSugeridoEtiqueta
                && datos.metodoSolicitado !== datos.metodoSugerido) {
            agregarDato(resumen, "Por geometría correspondería",
                datos.metodoSugeridoEtiqueta);
        }
    }

    const sinLimites = datos.limiteInferior === null || datos.limiteSuperior === null;
    const comoSeObtuvo = datos.origenes?.limites?.etiqueta;

    agregarDato(resumen, "Intervalo", sinLimites
        ? "sin determinar"
        : `[${redondear(datos.limiteInferior)}, ${redondear(datos.limiteSuperior)}]`
          + (comoSeObtuvo ? `  (${comoSeObtuvo})` : ""));

    if (Array.isArray(datos.puntos) && datos.puntos.length > 0) {
        agregarDato(resumen, "Puntos de corte",
            datos.puntos.map((punto) => embellecer(punto.texto)).join(",  "));
    }

    zona.appendChild(resumen);

    // ---------- Como se armo la region ----------
    // Despejes, rama elegida, variable de integracion: lo que hubo que hacer
    // antes de poder escribir la integral.
    if (datos.explicacionRegion) {
        zona.appendChild(crearPlegable("Cómo se armó la región", datos.explicacionRegion, true));
    }

    // ---------- Como se obtuvieron los limites ----------
    // Unos limites que aparecen en el formulario sin decir de donde salieron no
    // le sirven a quien esta aprendiendo: lo que hay que poder comprobar es la
    // cuenta, no el numero.
    if (!sinLimites && datos.explicacionLimites) {
        zona.appendChild(crearPlegable("Cómo se obtuvieron los límites",
            datos.explicacionLimites, true));
    }

    // ---------- Lecturas que admitian otra interpretacion ----------
    // No son datos que falten: con ellos se puede resolver. Lo que pasa es que
    // habia otra lectura defendible, y callarla dejaria al estudiante sin saber
    // que se tomo una decision por el.
    if (Array.isArray(datos.ambiguedades) && datos.ambiguedades.length > 0) {
        const aviso = document.createElement("div");
        aviso.className = "aviso";
        aviso.style.marginTop = "1rem";

        const cuerpo = document.createElement("div");
        cuerpo.className = "aviso__texto";

        const titulo = document.createElement("strong");
        titulo.textContent = "Lecturas que conviene confirmar";
        cuerpo.appendChild(titulo);

        const lista = document.createElement("ul");
        lista.style.margin = "0.25rem 0 0";
        for (const ambiguedad of datos.ambiguedades) {
            const entrada = document.createElement("li");
            entrada.textContent = embellecer(ambiguedad);
            lista.appendChild(entrada);
        }
        cuerpo.appendChild(lista);

        aviso.appendChild(cuerpo);
        zona.appendChild(aviso);
    }

    // ---------- Lo que falta ----------
    if (Array.isArray(datos.faltantes) && datos.faltantes.length > 0) {
        const aviso = document.createElement("div");
        aviso.className = "aviso aviso--atencion";
        aviso.style.marginTop = "1rem";

        const icono = document.createElement("span");
        icono.className = "aviso__icono";
        icono.textContent = "!";

        const cuerpo = document.createElement("div");
        cuerpo.className = "aviso__texto";

        // El mensaje nombra el dato concreto que falta, y solo ese. Todo lo
        // demás ya está arriba, identificado; decir "falta información" a secas
        // haría pensar que no se entendió nada del enunciado.
        const titulo = document.createElement("strong");
        titulo.textContent = "Esto no se pudo determinar con suficiente certeza";
        cuerpo.appendChild(titulo);

        const lista = document.createElement("ul");
        lista.style.margin = "0.25rem 0 0";
        for (const falta of datos.faltantes) {
            const entrada = document.createElement("li");
            entrada.textContent = embellecer(falta);
            lista.appendChild(entrada);
        }
        cuerpo.appendChild(lista);

        aviso.append(icono, cuerpo);
        zona.appendChild(aviso);
    }

    // ---------- Avisos ----------
    for (const texto of datos.avisos ?? []) {
        const aviso = document.createElement("p");
        aviso.className = "campo__ayuda";
        aviso.textContent = embellecer(texto);
        zona.appendChild(aviso);
    }

    // ---------- Razonamiento ----------
    if (Array.isArray(datos.evidencias) && datos.evidencias.length > 0) {
        const detalle = document.createElement("details");
        detalle.className = "plegable";
        detalle.style.marginTop = "1rem";

        const titulo = document.createElement("summary");
        titulo.textContent = "Por que se interpreto asi";
        detalle.appendChild(titulo);

        const cuerpo = document.createElement("div");
        cuerpo.className = "plegable__cuerpo";

        const lista = document.createElement("ul");
        lista.style.margin = "0";
        for (const evidencia of datos.evidencias) {
            const entrada = document.createElement("li");
            entrada.textContent = evidencia;
            lista.appendChild(entrada);
        }

        cuerpo.appendChild(lista);
        detalle.appendChild(cuerpo);
        zona.appendChild(detalle);
    }
}

/**
 * Una seccion plegable con un texto de procedimiento.
 *
 * @param {string} titulo lo que se ve cerrado
 * @param {string} texto el procedimiento, con sus saltos de linea
 * @param {boolean} abierto si empieza abierta
 * @returns {HTMLElement} la seccion
 */
function crearPlegable(titulo, texto, abierto) {
    const detalle = document.createElement("details");
    detalle.className = "plegable";
    detalle.style.marginTop = "1rem";
    detalle.open = abierto;

    const resumen = document.createElement("summary");
    resumen.textContent = titulo;
    detalle.appendChild(resumen);

    const cuerpo = document.createElement("div");
    cuerpo.className = "plegable__cuerpo";

    const pre = document.createElement("pre");
    pre.className = "mono";
    pre.style.whiteSpace = "pre-wrap";
    pre.style.margin = "0";
    pre.textContent = embellecerPaso(texto);

    cuerpo.appendChild(pre);
    detalle.appendChild(cuerpo);
    return detalle;
}

/**
 * Describe como se usa una curva en la variable de integracion.
 *
 * @param {object} curva curva del enunciado, con sus despejes
 * @param {string} variable variable de integracion
 * @returns {string} el texto que va al lado de la ecuacion
 */
function describirDespeje(curva, variable) {
    const despeje = variable === "y" ? curva.enY : curva.enX;
    if (!despeje || despeje.yaEstabaDespejada) {
        return "";
    }
    const otra = variable === "y" ? "x" : "y";
    const ramas = despeje.ramas.map((rama) => `${otra} = ${embellecer(rama.expresion)}`);
    return `   →   ${ramas.join("  ó  ")}`;
}

/** Un numero corto para el resumen: 1.4422495703 se ve como 1.44225. */
function redondear(valor) {
    const numero = Number(valor);
    if (!Number.isFinite(numero)) {
        return valor;
    }
    return Number.isInteger(numero) ? String(numero) : String(Number(numero.toFixed(6)));
}

/** Agrega un par etiqueta/valor al resumen. */
function agregarDato(contenedor, etiqueta, valor) {
    const caja = document.createElement("div");
    caja.className = "dato";

    const nombre = document.createElement("span");
    nombre.className = "dato__etiqueta";
    nombre.textContent = etiqueta;

    const contenido = document.createElement("span");
    contenido.className = "dato__valor";
    contenido.textContent = valor;

    caja.append(nombre, contenido);
    contenedor.appendChild(caja);
}

// ------------------------------------------------------------------
// PASO 2: RESOLVER
// ------------------------------------------------------------------

/** Resuelve el ejercicio con los datos que hay en el formulario. */
async function resolver() {
    const zonaMensajes = document.getElementById("mensajes");
    const zonaResultado = document.getElementById("resultado");

    const tema = document.getElementById("tema")?.value ?? "";
    if (!tema) {
        mensajes.mostrar(zonaMensajes,
            "Elija el tema antes de resolver, o analice primero el enunciado.", "atencion");
        return;
    }

    mensajes.limpiar(zonaMensajes);
    mensajes.limpiar(document.getElementById("advertencia"));
    mensajes.mostrarCargando(zonaResultado);

    try {
        // Los datos salen de los campos, no de la interpretacion guardada: si el
        // estudiante corrigio algo, es su correccion la que vale.
        const datos = leerFormulario(tema);
        const resultado = await ejecutarSegunTema(tema, datos);

        // El panel se muestra antes de dibujar. Un canvas oculto mide cero, y
        // dibujar en el antes de mostrarlo dejaba la grafica en blanco la
        // primera vez que se resolvia un ejercicio.
        document.getElementById("panelResultado")?.classList.remove("oculto");
        pintarResultado(tema, resultado);

        if (resultado.advertencia) {
            mensajes.mostrarAdvertencia(
                document.getElementById("advertencia"), resultado.advertencia);
        }

        // Los limites que calculo el servidor vuelven al formulario, para que lo
        // que se ve en pantalla y lo que se integro sean lo mismo.
        sincronizarLimites(resultado);

    } catch (error) {
        mensajes.mostrarError(zonaMensajes, error);
        zonaResultado.innerHTML = "";
    }
}

/** Lee los campos del formulario segun el tema. */
function leerFormulario(tema) {
    const valor = (id) => document.getElementById(id)?.value ?? "";
    const marcado = (id) => document.getElementById(id)?.checked ?? false;

    const contexto = contextoVigente();
    const comunes = {
        limiteInferior: valor("limiteInferior"),
        limiteSuperior: valor("limiteSuperior"),
        buscarLimites: marcado("limitesAutomaticos"),
        explicacionLimites: explicacionVigente(),
        explicacionRegion: contexto.explicacionRegion,
        curvasDelEnunciado: contexto.curvasDelEnunciado
    };

    if (tema === "VOLUMEN_SOLIDO") {
        return {
            ...comunes,
            funcionExterior: valor("funcionF"),
            funcionInterior: valor("funcionG"),
            tipoSolido: valor("tipoSolido"),
            eje: valor("eje"),
            desplazamientoEje: valor("desplazamientoEje") || 0,
            particiones: 2000
        };
    }

    return {
        ...comunes,
        funcionF: valor("funcionF"),
        funcionG: tema === "AREA_ENTRE_CURVAS" ? valor("funcionG") : "",
        variable: valor("variable") || "x",
        metodo: "ANALITICO",
        particiones: 20
    };
}

/**
 * Lo que el analizador averiguo del enunciado, si sigue valiendo para lo que
 * hay en los campos.
 *
 * La explicacion de la region (que curva se despejo, que rama se uso) y las
 * curvas completas del enunciado solo describen el ejercicio mientras las
 * funciones sean las que se interpretaron. Si el estudiante las cambio, se
 * dejan de mandar: dibujar la parabola del enunciado junto a otra funcion que
 * escribio despues confundiria mas que ayudar.
 *
 * @returns {{explicacionRegion: string, curvasDelEnunciado: string[]}} el contexto
 */
function contextoVigente() {
    const vacio = { explicacionRegion: "", curvasDelEnunciado: [] };
    if (!interpretacion) {
        return vacio;
    }
    const valor = (id) => (document.getElementById(id)?.value ?? "").trim();
    const mismasFunciones = valor("funcionF") === (interpretacion.funcionF ?? "").trim()
        && valor("funcionG") === (interpretacion.funcionG ?? "").trim();
    if (!mismasFunciones) {
        return vacio;
    }
    return {
        explicacionRegion: interpretacion.explicacionRegion ?? "",
        curvasDelEnunciado: (interpretacion.curvas ?? []).map((curva) => curva.ecuacion)
    };
}

/**
 * Devuelve la explicacion de los limites si sigue describiendo lo que hay en los
 * campos.
 *
 * Al interpretar el enunciado el servidor ya hizo la cuenta —igualar las curvas,
 * buscar los cortes— y la devolvio escrita. Si se manda de vuelta, el
 * procedimiento puede explicar de donde salio el intervalo en lugar de decir que
 * "viene dado": unos limites deducidos y presentados sin su cuenta no se
 * distinguen de unos inventados, que es justo lo que hay que evitar.
 *
 * Pero si el estudiante corrigio los limites, esa explicacion pasa a describir
 * otros numeros y se descarta. Mostrar el razonamiento de un intervalo junto a
 * otro distinto seria peor que no mostrar ninguno.
 *
 * @returns {string} la explicacion, o cadena vacia si ya no corresponde
 */
function explicacionVigente() {
    if (!interpretacion?.explicacionLimites) {
        return "";
    }

    const mismoValor = (id, deducido) => {
        const escrito = document.getElementById(id)?.value ?? "";
        if (escrito.trim() === "" || deducido === null || deducido === undefined) {
            return false;
        }
        return Number(escrito) === Number(deducido);
    };

    const intactos = mismoValor("limiteInferior", interpretacion.limiteInferior)
        && mismoValor("limiteSuperior", interpretacion.limiteSuperior);

    return intactos ? interpretacion.explicacionLimites : "";
}

/** Llama a la ruta que corresponde al tema. */
function ejecutarSegunTema(tema, datos) {
    switch (tema) {
        case "AREA_BAJO_CURVA":
            return calcularAreaBajoCurva(datos);
        case "AREA_ENTRE_CURVAS":
            return calcularAreaEntreCurvas(datos);
        case "VOLUMEN_SOLIDO":
            return calcularSolidoRevolucion(datos);
        default:
            return Promise.reject(new Error("Tema no reconocido."));
    }
}

/** Dibuja el resultado, los pasos y las graficas. */
function pintarResultado(tema, resultado) {
    const zonaResultado = document.getElementById("resultado");
    const zonaPasos = document.getElementById("pasos");
    const zonaLimites = document.getElementById("explicacionLimites");

    const lienzoRegion = document.getElementById("graficaRegion");
    const panelSolido = document.getElementById("panelSolido");

    panelPasos.dibujarExplicacionDeLimites(zonaLimites, resultado.explicacionLimites);

    if (tema === "VOLUMEN_SOLIDO") {
        panelResultados.dibujarVolumen(zonaResultado, resultado);
        panelPasos.dibujar(zonaPasos, resultado.pasos, "solidos");
        panelPasos.dibujarRebanadas(document.getElementById("rebanadas"),
            resultado.rebanadas, resultado.grafica?.hueco ?? false);

        // Un volumen necesita las dos vistas y no son intercambiables: en el
        // plano se entiende el planteamiento —que region gira y a que distancia
        // del eje esta cada borde—, y en el espacio se ve el cuerpo que sale.
        // Ensenar solo el solido deja al estudiante sin la parte que tiene que
        // saber plantear.
        rotularRegion("La región que gira",
            "La zona sombreada es lo que gira. Las líneas de puntos son los límites de "
            + "integración y la línea a trazos es el eje de revolución: la distancia de "
            + "cada borde a ese eje es el radio de la fórmula.");

        dibujar2D(lienzoRegion, resultado.region, {
            sombrearRegion: true,
            mostrarRectangulos: false
        });

        panelSolido?.classList.remove("oculto");

        const lienzoSolido = document.getElementById("grafica");
        const escena = dibujarSolido(lienzoSolido, resultado.grafica);

        // Un solo oyente para toda la pagina: antes se agregaba uno por cada
        // ejercicio resuelto, y todos los solidos anteriores seguian
        // redibujandose al cambiar el tamano de la ventana.
        solidoVigente = { lienzo: lienzoSolido, malla: resultado.grafica, escena };
        if (!escuchandoTamano) {
            escuchandoTamano = true;
            const repintar = () => {
                if (solidoVigente) {
                    pintarSolido(solidoVigente.lienzo, solidoVigente.malla, solidoVigente.escena);
                }
            };
            window.addEventListener("resize", repintar);
            window.addEventListener("cambio-de-tema", repintar);
        }
        return;
    }

    const variante = tema === "AREA_ENTRE_CURVAS" ? "curvas" : "";
    panelResultados.dibujarArea(zonaResultado, resultado, variante);
    panelPasos.dibujar(zonaPasos, resultado.pasos, variante);
    document.getElementById("rebanadas")?.classList.add("oculto");
    panelSolido?.classList.add("oculto");

    rotularRegion("Gráfica",
        "La gráfica usa exactamente los mismos datos con los que se resolvió.");

    const sistema = dibujar2D(lienzoRegion, resultado.grafica, { sombrearRegion: true });
    conectarLectura(lienzoRegion, document.getElementById("lectura"), sistema, resultado.grafica);
}

/** Ajusta el titulo y la ayuda de la grafica plana segun el tema. */
function rotularRegion(titulo, ayuda) {
    const rotulo = document.getElementById("tituloRegion");
    const explicacion = document.getElementById("ayudaRegion");

    if (rotulo) {
        rotulo.textContent = titulo;
    }
    if (explicacion) {
        explicacion.textContent = ayuda;
    }
}

/** Copia al formulario los limites con que se resolvio de verdad. */
function sincronizarLimites(resultado) {
    const inferior = document.getElementById("limiteInferior");
    const superior = document.getElementById("limiteSuperior");

    if (inferior && Number.isFinite(resultado.limiteInferior)) {
        inferior.value = resultado.limiteInferior;
    }
    if (superior && Number.isFinite(resultado.limiteSuperior)) {
        superior.value = resultado.limiteSuperior;
    }
}

// ------------------------------------------------------------------
// FORMULARIO
// ------------------------------------------------------------------

/**
 * Muestra u oculta los campos segun el tema.
 *
 * Pedir un eje de revolucion en un problema de area confundiria, y no pedir la
 * segunda funcion en un area entre curvas impediria resolverlo.
 *
 * @param {string} tema clave del tema
 */
function ajustarCamposAlTema(tema) {
    const mostrar = (id, visible) => {
        document.getElementById(id)?.classList.toggle("oculto", !visible);
    };

    const esVolumen = tema === "VOLUMEN_SOLIDO";
    const esEntreCurvas = tema === "AREA_ENTRE_CURVAS";

    mostrar("grupoFuncionG", esVolumen || esEntreCurvas);
    mostrar("grupoEje", esVolumen);
    mostrar("grupoTipoSolido", esVolumen);
    // En un volumen la variable la deciden el metodo y el eje; en un area la
    // elige el estudiante, o la propone el analizador.
    mostrar("grupoVariable", !esVolumen);

    actualizarEtiquetas();
}

/**
 * La variable de integracion vigente.
 *
 * En un volumen no es un dato suelto: discos y arandelas rebanan
 * perpendicularmente al eje, asi que con un eje vertical se integra en y; las
 * capas hacen lo contrario.
 *
 * @returns {string} "x" o "y"
 */
function variableVigente() {
    const tema = document.getElementById("tema")?.value ?? "";
    if (tema !== "VOLUMEN_SOLIDO") {
        return document.getElementById("variable")?.value === "y" ? "y" : "x";
    }
    const ejeVertical = document.getElementById("eje")?.value === "y";
    const capas = document.getElementById("tipoSolido")?.value === "CAPAS";
    return ejeVertical !== capas ? "y" : "x";
}

/**
 * Nombra los campos de las funciones segun la variable: con x son y = f(x),
 * con y son x = g(y). Escribir una funcion de y en un campo rotulado f(x) es
 * el error que llevaba a mensajes del parser que nadie entendia.
 */
function actualizarEtiquetas() {
    const tema = document.getElementById("tema")?.value ?? "";
    const v = variableVigente();
    const otra = v === "x" ? "y" : "x";
    const esVolumen = tema === "VOLUMEN_SOLIDO";

    const etiquetaF = document.getElementById("etiquetaFuncionF");
    if (etiquetaF) {
        etiquetaF.textContent = esVolumen
            ? `Primera curva: ${otra} = f(${v})`
            : `${otra} = f(${v})`;
    }
    const etiquetaG = document.getElementById("etiquetaFuncionG");
    if (etiquetaG) {
        etiquetaG.textContent = esVolumen
            ? `Segunda curva: ${otra} = g(${v}) (opcional en discos)`
            : `Segunda curva: ${otra} = g(${v})`;
    }
    const ayuda = document.getElementById("ayudaVariable");
    if (ayuda) {
        ayuda.textContent = v === "y"
            ? "Las funciones se escriben en términos de y, por ejemplo y^2 o y + 6."
            : "Las funciones se escriben en términos de x, por ejemplo x^2 o 4x - x^2.";
    }
}

/** Activa o desactiva los campos de limites segun la casilla. */
function actualizarLimites() {
    const automatico = document.getElementById("limitesAutomaticos")?.checked ?? false;

    for (const id of ["limiteInferior", "limiteSuperior"]) {
        const campo = document.getElementById(id);
        if (campo) {
            campo.disabled = automatico;
        }
    }

    document.getElementById("grupoLimites")
        ?.style.setProperty("opacity", automatico ? "0.5" : "1");
}
