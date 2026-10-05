/**
 * main.js
 * Punto de entrada del frontend.
 *
 * Se encarga de lo que comparten todas las paginas — el tema visual, el
 * enlace activo del menu — y despues arranca la pagina concreta segun lo que
 * diga el atributo data-pagina del body. Un solo archivo de entrada evita
 * tener un script distinto por pagina repitiendo la misma preparacion.
 */

import {
    calcularAreaBajoCurva,
    calcularAreaEntreCurvas,
    calcularSolidoRevolucion
} from "./api/clienteApi.js";

import { PaginaDeCalculo, leerCamposComunes } from "./ui/formularioIntegral.js";
import { iniciarResolver } from "./ui/analizadorProblema.js";
import { iniciarCalculadora } from "./ui/calculadora.js";
import * as selectorMetodo from "./ui/selectorMetodo.js";
import * as panelResultados from "./ui/panelResultados.js";
import * as panelPasos from "./ui/panelPasos.js";
import * as mensajes from "./ui/mensajes.js";

import { dibujar as dibujar2D } from "./graficas/grafica2d.js";
import { dibujarSolido, pintar as pintarSolido } from "./graficas/grafica3d.js";
import { obtenerTema, guardarTema } from "./util/almacenamiento.js";
import { leerNumero } from "./util/validaciones.js";

// ------------------------------------------------------------------
// ARRANQUE
// ------------------------------------------------------------------

document.addEventListener("DOMContentLoaded", () => {
    aplicarTema(obtenerTema());
    prepararBotonDeTema();
    marcarEnlaceActivo();

    const pagina = document.body.dataset.pagina;

    switch (pagina) {
        case "area-bajo-curva":
            iniciarAreaBajoCurva();
            break;
        case "area-entre-curvas":
            iniciarAreaEntreCurvas();
            break;
        case "solidos-revolucion":
            iniciarSolidosDeRevolucion();
            break;
        case "resolver":
            iniciarResolver();
            break;
        case "calculadora":
            iniciarCalculadora();
            break;
        case "ayuda":
            iniciarAyuda();
            break;
        default:
            // La pagina de inicio no necesita nada mas.
            break;
    }
});

// ------------------------------------------------------------------
// TEMA VISUAL
// ------------------------------------------------------------------

/**
 * Aplica el tema claro u oscuro.
 *
 * @param {string} tema "claro" u "oscuro"
 */
function aplicarTema(tema) {
    document.documentElement.dataset.tema = tema;

    const boton = document.getElementById("botonTema");
    if (boton) {
        boton.textContent = tema === "oscuro" ? "☀" : "☾";
        boton.title = tema === "oscuro" ? "Cambiar a tema claro" : "Cambiar a tema oscuro";
    }
}

/** Conecta el boton que cambia entre tema claro y oscuro. */
function prepararBotonDeTema() {
    const boton = document.getElementById("botonTema");
    if (!boton) {
        return;
    }

    boton.addEventListener("click", () => {
        const actual = document.documentElement.dataset.tema;
        const nuevo = actual === "oscuro" ? "claro" : "oscuro";

        aplicarTema(nuevo);
        guardarTema(nuevo);

        // Las graficas leen sus colores del CSS, asi que hay que avisarles
        // para que se vuelvan a dibujar con la paleta nueva.
        window.dispatchEvent(new CustomEvent("cambio-de-tema"));
    });
}

/** Marca en el menu el enlace de la pagina que se esta viendo. */
function marcarEnlaceActivo() {
    const rutaActual = window.location.pathname;

    for (const enlace of document.querySelectorAll(".barra-superior__enlace")) {
        const destino = new URL(enlace.href, window.location.origin).pathname;
        if (destino === rutaActual) {
            enlace.setAttribute("aria-current", "page");
        }
    }
}

// ------------------------------------------------------------------
// TEMA 1: AREA BAJO LA CURVA
// ------------------------------------------------------------------

/** Prepara la pagina de area bajo la curva. */
function iniciarAreaBajoCurva() {
    const pagina = new PaginaDeCalculo({
        tema: "area-bajo-curva",

        calcular: calcularAreaBajoCurva,

        armarSolicitud: (elementos) => ({
            ...leerCamposComunes(elementos),
            posicionRiemann: document.getElementById("posicionRiemann")?.value ?? "MEDIO",
            buscarLimites: elementos.limitesAutomaticos?.checked ?? false
        }),

        pintarResultado: (pagina, resultado) => {
            pagina.pintarArea(resultado);

            // Los rectangulos solo se dibujan si el estudiante eligio Riemann;
            // con la integracion directa no hay rectangulos que mostrar.
            const metodoElegido = pagina.elementos.metodo?.value ?? "";
            pagina.dibujarGrafica(resultado.grafica, {
                sombrearRegion: true,
                mostrarRectangulos: selectorMetodo.esMetodoDeRiemann(metodoElegido)
            });
        },

        ejemplos: [
            { etiqueta: "x²", funcionF: "x^2", a: 0, b: 1,
              descripcion: "El area vale 1/3" },
            { etiqueta: "√x", funcionF: "sqrt(x)", a: 0, b: 4,
              descripcion: "El area vale 16/3" },
            { etiqueta: "sin(x)", funcionF: "sin(x)", a: 0, b: 3.14159265,
              descripcion: "El area vale 2" },
            { etiqueta: "sin(x) con cruce", funcionF: "sin(x)", a: 0, b: 6.28318531,
              descripcion: "La integral da 0 pero el area es 4" },
            { etiqueta: "1/x", funcionF: "1/x", a: 1, b: 2.71828183,
              descripcion: "El area vale 1" },
            { etiqueta: "4 - x²", funcionF: "4 - x^2", a: -2, b: 2,
              descripcion: "El area vale 32/3" }
        ]
    });

    // Donde se mide la altura de los rectangulos: es una opcion de Riemann, no un
    // metodo aparte, y por eso vive en su propio selector.
    selectorMetodo.llenarPosicionesRiemann(document.getElementById("posicionRiemann"));

    pagina.iniciar();
}

// ------------------------------------------------------------------
// TEMA 2: AREA ENTRE DOS CURVAS
// ------------------------------------------------------------------

/** Prepara la pagina de area entre dos curvas. */
function iniciarAreaEntreCurvas() {
    const pagina = new PaginaDeCalculo({
        tema: "area-entre-curvas",

        calcular: calcularAreaEntreCurvas,

        armarSolicitud: (elementos) => ({
            ...leerCamposComunes(elementos),
            funcionG: elementos.funcionG?.value ?? "",
            buscarLimites: elementos.limitesAutomaticos?.checked ?? false
        }),

        pintarResultado: (pagina, resultado) => {
            pagina.pintarArea(resultado, "curvas");
            pagina.dibujarGrafica(resultado.grafica, {
                sombrearRegion: true,
                mostrarRectangulos: false
            });
        },

        ejemplos: [
            { etiqueta: "x y x²", funcionF: "x", funcionG: "x^2", a: 0, b: 1,
              descripcion: "El area vale 1/6" },
            { etiqueta: "x³ y x", funcionF: "x^3", funcionG: "x", a: -1, b: 1,
              descripcion: "Se cruzan: la integral da 0 pero el area es 1/2" },
            { etiqueta: "4 - x² y el eje", funcionF: "4 - x^2", funcionG: "0", a: -2, b: 2,
              descripcion: "El area vale 32/3" },
            { etiqueta: "sin y cos", funcionF: "sin(x)", funcionG: "cos(x)", a: 0, b: 3.14159265,
              descripcion: "El area vale 2·raiz(2)" },
            { etiqueta: "Parabolas", funcionF: "x^2", funcionG: "2x - x^2", a: 0, b: 1,
              descripcion: "Region entre dos parabolas" }
        ]
    });

    pagina.iniciar();
}

// ------------------------------------------------------------------
// TEMA 3: SOLIDOS DE REVOLUCION
// ------------------------------------------------------------------

/** Prepara la pagina de solidos de revolucion. */
function iniciarSolidosDeRevolucion() {
    const pagina = new PaginaDeCalculo({
        tema: "solidos-revolucion",

        calcular: calcularSolidoRevolucion,

        armarSolicitud: (elementos) => ({
            funcionExterior: elementos.funcionF?.value ?? "",
            funcionInterior: elementos.funcionG?.value ?? "",
            limiteInferior: elementos.limiteInferior?.value ?? "",
            limiteSuperior: elementos.limiteSuperior?.value ?? "",
            particiones: Math.round(leerNumero(elementos.particiones, 2000)),
            tipoSolido: elementos.tipoSolido?.value ?? "",
            buscarLimites: elementos.limitesAutomaticos?.checked ?? false,
            eje: elementos.eje?.value ?? "x",
            desplazamientoEje: leerNumero(elementos.desplazamientoEje, 0)
        }),

        pintarResultado: (pagina, resultado) => {
            const { elementos } = pagina;

            panelResultados.dibujarVolumen(elementos.resultado, resultado);
            panelResultados.escribirPlanteamiento(elementos.planteamiento, resultado);
            panelPasos.dibujar(elementos.pasos, resultado.pasos, "solidos");
            panelPasos.dibujarRebanadas(
                elementos.rebanadas, resultado.rebanadas, resultado.grafica?.hueco ?? false);

            dibujarRegionDelSolido(resultado.region);
            dibujarVisorTresD(pagina, resultado.grafica, resultado.region);
        },

        ejemplos: [
            { etiqueta: "√x sobre eje X", funcionF: "sqrt(x)", a: 0, b: 4,
              tipoSolido: "DISCOS", eje: "x",
              descripcion: "Ejemplo del material: el volumen vale 8π" },
            { etiqueta: "x y x² (arandelas)", funcionF: "x", funcionG: "x^2", a: 0, b: 1,
              tipoSolido: "ARANDELAS", eje: "x",
              descripcion: "Ejemplo del material: el volumen vale 2π/15" },
            { etiqueta: "Cono", funcionF: "x", a: 0, b: 3,
              tipoSolido: "DISCOS", eje: "x",
              descripcion: "Cono de radio y altura 3: 9π" },
            { etiqueta: "Esfera", funcionF: "sqrt(4 - x^2)", a: -2, b: 2,
              tipoSolido: "DISCOS", eje: "x",
              descripcion: "Esfera de radio 2: 32π/3" },
            { etiqueta: "x² por capas", funcionF: "x^2", a: 0, b: 1,
              tipoSolido: "CAPAS", eje: "y",
              descripcion: "Girando sobre el eje Y: π/2" }
        ]
    });

    // El tipo de solido decide si hace falta la segunda curva: solo las
    // arandelas la usan, porque son las unicas que tienen agujero.
    selectorMetodo.llenarTiposDeSolido(
        pagina.elementos.tipoSolido,
        pagina.elementos.descripcionSolido,
        (tipo) => {
            const grupo = pagina.elementos.grupoFuncionG;
            if (grupo) {
                grupo.classList.toggle("oculto", !tipo.necesitaSegundaCurva
                    && tipo.clave !== "CAPAS");
            }
        }
    );

    pagina.iniciar();
}

/**
 * Dibuja en el plano la region que se hace girar.
 *
 * Es la vista que explica el ejercicio. El visor 3D ensena el cuerpo que
 * resulto, pero el planteamiento esta en el plano: que region gira, entre que
 * curvas queda, desde donde hasta donde, y a que distancia del eje esta cada
 * borde. Esa distancia es el radio de la formula.
 *
 * Los puntos vienen del mismo objeto con que se calculo el volumen, asi que las
 * dos vistas y el procedimiento no pueden describir ejercicios distintos.
 *
 * @param {object} region datos de la region que devolvio el backend
 */
function dibujarRegionDelSolido(region) {
    const lienzo = document.getElementById("graficaRegion");
    if (!lienzo || !region) {
        return;
    }
    dibujar2D(lienzo, region, { sombrearRegion: true, mostrarRectangulos: false });
}

/**
 * Dibuja el solido en el visor y conecta sus controles.
 *
 * @param {PaginaDeCalculo} pagina pagina en curso
 * @param {object} malla malla que devolvio el backend
 * @param {object} [region] region plana, para redibujarla junto con el solido
 */
function dibujarVisorTresD(pagina, malla, region) {
    const lienzo = pagina.elementos.grafica;
    if (!lienzo || !malla) {
        return;
    }

    const escena = dibujarSolido(lienzo, malla);
    pagina.escenaTresD = escena;

    // El visor 3D no usa el sistema de coordenadas de las graficas planas, asi
    // que se le sustituye el metodo de redibujado: la pagina lo llama al
    // cambiar el tamano de la ventana y al cambiar de tema, y sin esto el
    // solido se quedaria deformado o con los colores del tema anterior.
    // Las dos vistas se redibujan juntas. La region es una grafica plana normal y
    // se repintaria sola, pero el metodo que la pagina llama es este, asi que si
    // no se encadena aqui se quedaria con los colores del tema anterior.
    pagina.datosGrafica = null;
    pagina.redibujarGrafica = () => {
        pintarSolido(lienzo, malla, escena);
        dibujarRegionDelSolido(region);
    };

    const botonReiniciar = document.getElementById("botonReiniciarVista");
    if (botonReiniciar) {
        botonReiniciar.onclick = () => {
            escena.reiniciar();
            pintarSolido(lienzo, malla, escena);
        };
    }
}

// ------------------------------------------------------------------
// PAGINA DE AYUDA
// ------------------------------------------------------------------

/**
 * Llena la pagina de ayuda con el catalogo del servidor.
 *
 * La lista de funciones permitidas sale de CatalogoFunciones.java, no de una
 * tabla escrita a mano: asi nunca queda desactualizada respecto a lo que el
 * parser acepta de verdad.
 */
async function iniciarAyuda() {
    const catalogo = await selectorMetodo.cargarCatalogo();
    const zonaError = document.getElementById("mensajes");

    if (!catalogo) {
        mensajes.mostrar(
            zonaError,
            "No se pudo consultar la lista de funciones. Revise que el servidor siga corriendo.",
            "error"
        );
        return;
    }

    llenarTablaDeFunciones(catalogo.funciones);
    llenarTablaDeConstantes(catalogo.constantes);
    llenarTablaDeMetodos(catalogo.metodos);
    llenarTablaDeSolidos(catalogo.tiposDeSolido);
}

/**
 * Llena la tabla de funciones permitidas.
 *
 * @param {object} funciones nombre a descripcion
 */
function llenarTablaDeFunciones(funciones) {
    const cuerpo = document.getElementById("tablaFunciones");
    if (!cuerpo) {
        return;
    }

    cuerpo.innerHTML = "";

    for (const [nombre, descripcion] of Object.entries(funciones)) {
        const fila = document.createElement("tr");

        const celdaNombre = document.createElement("td");
        celdaNombre.className = "mono";
        celdaNombre.textContent = `${nombre}(x)`;

        const celdaDescripcion = document.createElement("td");
        celdaDescripcion.textContent = descripcion;

        fila.append(celdaNombre, celdaDescripcion);
        cuerpo.appendChild(fila);
    }
}

/**
 * Llena la tabla de constantes.
 *
 * @param {object} constantes nombre a valor
 */
function llenarTablaDeConstantes(constantes) {
    const cuerpo = document.getElementById("tablaConstantes");
    if (!cuerpo) {
        return;
    }

    cuerpo.innerHTML = "";

    for (const [nombre, valor] of Object.entries(constantes)) {
        const fila = document.createElement("tr");

        const celdaNombre = document.createElement("td");
        celdaNombre.className = "mono";
        celdaNombre.textContent = nombre;

        const celdaValor = document.createElement("td");
        celdaValor.className = "numero";
        celdaValor.textContent = Number(valor).toFixed(6);

        fila.append(celdaNombre, celdaValor);
        cuerpo.appendChild(fila);
    }
}

/**
 * Llena la tabla de metodos numericos.
 *
 * @param {Array<object>} metodos metodos disponibles
 */
function llenarTablaDeMetodos(metodos) {
    const cuerpo = document.getElementById("tablaMetodos");
    if (!cuerpo) {
        return;
    }

    cuerpo.innerHTML = "";

    for (const metodo of metodos) {
        const fila = document.createElement("tr");

        const nombre = document.createElement("td");
        nombre.textContent = metodo.etiqueta;

        const formula = document.createElement("td");
        formula.className = "mono";
        formula.textContent = metodo.formula;

        const descripcion = document.createElement("td");
        descripcion.textContent = metodo.descripcion;

        fila.append(nombre, formula, descripcion);
        cuerpo.appendChild(fila);
    }
}

/**
 * Llena la tabla de metodos para solidos.
 *
 * @param {Array<object>} tipos tipos de solido disponibles
 */
function llenarTablaDeSolidos(tipos) {
    const cuerpo = document.getElementById("tablaSolidos");
    if (!cuerpo) {
        return;
    }

    cuerpo.innerHTML = "";

    for (const tipo of tipos) {
        const fila = document.createElement("tr");

        const nombre = document.createElement("td");
        nombre.textContent = tipo.etiqueta;

        const formula = document.createElement("td");
        formula.className = "mono";
        formula.textContent = tipo.formula;

        const cuando = document.createElement("td");
        cuando.textContent = tipo.explicacion;

        fila.append(nombre, formula, cuando);
        cuerpo.appendChild(fila);
    }
}
