/**
 * formularioIntegral.js
 * Coordina el trabajo de una pagina de calculo.
 *
 * Las tres paginas — area bajo la curva, area entre dos curvas y solidos de
 * revolucion — siguen exactamente el mismo guion: leer el formulario,
 * validarlo, pedir el calculo al servidor, dibujar la grafica y mostrar los
 * pasos. Lo que cambia entre ellas son los campos del formulario y la ruta a
 * la que se llama, y eso se recibe como configuracion.
 *
 * Escribir esto tres veces habria significado corregir cada error tres veces.
 */

import * as mensajes from "./mensajes.js";
import * as teclado from "./tecladoMatematico.js";
import * as selectorMetodo from "./selectorMetodo.js";
import * as panelResultados from "./panelResultados.js";
import * as panelPasos from "./panelPasos.js";

import { dibujar as dibujar2D, conectarLectura } from "../graficas/grafica2d.js";
import { descargarComoImagen, imprimir } from "../graficas/exportarGrafica.js";
import { validarFuncion, validarLimites, validarParticiones, leerNumero } from "../util/validaciones.js";
import { agregarAlHistorial } from "../util/almacenamiento.js";

/**
 * Pagina de calculo.
 */
export class PaginaDeCalculo {

    /**
     * @param {object} configuracion ajustes propios de la pagina
     * @param {string} configuracion.tema identificador del tema, para colores e historial
     * @param {Function} configuracion.calcular funcion del cliente que llama al servidor
     * @param {Function} configuracion.armarSolicitud arma el objeto que se envia
     * @param {Function} configuracion.pintarResultado dibuja la respuesta
     * @param {Array<object>} [configuracion.ejemplos] ejemplos que se ofrecen
     */
    constructor(configuracion) {
        this.configuracion = configuracion;
        this.elementos = this.buscarElementos();
        this.sistemaGrafica = null;
        this.ultimoResultado = null;
    }

    /**
     * Localiza los elementos de la pagina.
     *
     * Se buscan todos aunque no existan en las tres paginas: los que falten
     * quedan en null y los modulos que los reciben ya saben ignorarlos.
     *
     * @returns {object} los elementos encontrados
     */
    buscarElementos() {
        const buscar = (id) => document.getElementById(id);

        return {
            formulario: buscar("formulario"),
            funcionF: buscar("funcionF"),
            funcionG: buscar("funcionG"),
            grupoFuncionG: buscar("grupoFuncionG"),
            limiteInferior: buscar("limiteInferior"),
            limiteSuperior: buscar("limiteSuperior"),
            particiones: buscar("particiones"),
            metodo: buscar("metodo"),
            descripcionMetodo: buscar("descripcionMetodo"),
            tipoSolido: buscar("tipoSolido"),
            descripcionSolido: buscar("descripcionSolido"),
            eje: buscar("eje"),
            desplazamientoEje: buscar("desplazamientoEje"),
            limitesAutomaticos: buscar("limitesAutomaticos"),
            grupoLimites: buscar("grupoLimites"),

            errorFuncionF: buscar("errorFuncionF"),
            errorFuncionG: buscar("errorFuncionG"),
            errorLimites: buscar("errorLimites"),

            botonCalcular: buscar("botonCalcular"),
            botonLimpiar: buscar("botonLimpiar"),
            botonDescargar: buscar("botonDescargar"),
            botonImprimir: buscar("botonImprimir"),

            teclado: buscar("teclado"),
            ejemplos: buscar("ejemplos"),

            mensajes: buscar("mensajes"),
            advertencia: buscar("advertencia"),
            resultado: buscar("resultado"),
            planteamiento: buscar("planteamiento"),
            pasos: buscar("pasos"),
            explicacionLimites: buscar("explicacionLimites"),
            rebanadas: buscar("rebanadas"),

            grafica: buscar("grafica"),
            lectura: buscar("lectura"),
            zonaResultados: buscar("zonaResultados")
        };
    }

    /**
     * Deja la pagina lista para usarse.
     *
     * @returns {Promise<void>}
     */
    async iniciar() {
        const { elementos, configuracion } = this;

        await selectorMetodo.llenarMetodos(elementos.metodo, elementos.descripcionMetodo);

        if (elementos.teclado && elementos.funcionF) {
            teclado.construir(elementos.teclado, elementos.funcionF);
        }

        if (elementos.ejemplos && Array.isArray(configuracion.ejemplos)) {
            teclado.construirEjemplos(
                elementos.ejemplos,
                configuracion.ejemplos,
                (ejemplo) => this.cargarEjemplo(ejemplo)
            );
        }

        if (elementos.formulario) {
            elementos.formulario.addEventListener("submit", (evento) => {
                evento.preventDefault();
                this.ejecutar();
            });
        }

        if (elementos.botonLimpiar) {
            elementos.botonLimpiar.addEventListener("click", () => this.limpiar());
        }

        if (elementos.botonDescargar) {
            elementos.botonDescargar.addEventListener("click", () => {
                descargarComoImagen(elementos.grafica, `grafica-${configuracion.tema}`);
            });
        }

        if (elementos.botonImprimir) {
            elementos.botonImprimir.addEventListener("click", () => imprimir());
        }

        // La casilla de limites automaticos desactiva los campos de a y b, en
        // lugar de ocultarlos: asi se sigue viendo que existen y por que estan
        // deshabilitados.
        if (elementos.limitesAutomaticos) {
            elementos.limitesAutomaticos.addEventListener("change", () => {
                this.actualizarLimites();
            });
            this.actualizarLimites();
        }

        // La grafica se redibuja al cambiar el tamano de la ventana, porque el
        // canvas necesita saber su nuevo ancho para no salir deformado.
        window.addEventListener("resize", () => this.redibujarGrafica());

        // Y tambien al cambiar de tema, porque los colores se leen del CSS.
        window.addEventListener("cambio-de-tema", () => this.redibujarGrafica());

        this.mostrarEstadoInicial();
    }

    /**
     * Ejecuta el calculo completo.
     *
     * @returns {Promise<void>}
     */
    async ejecutar() {
        const { elementos, configuracion } = this;

        if (!this.validar()) {
            return;
        }

        mensajes.limpiar(elementos.mensajes);
        mensajes.limpiar(elementos.advertencia);
        mensajes.mostrarCargando(elementos.resultado);
        this.bloquearBoton(true);

        try {
            const solicitud = configuracion.armarSolicitud(elementos);
            const resultado = await configuracion.calcular(solicitud);

            this.ultimoResultado = resultado;
            agregarAlHistorial(configuracion.tema, elementos.funcionF.value);

            configuracion.pintarResultado(this, resultado);
            this.sincronizarCampos(resultado);

            if (resultado.advertencia) {
                mensajes.mostrarAdvertencia(elementos.advertencia, resultado.advertencia);
            }

            // Se lleva la vista al resultado, util en pantallas pequenas donde
            // el formulario ocupa toda la primera pantalla.
            elementos.zonaResultados?.scrollIntoView({ behavior: "smooth", block: "nearest" });

        } catch (error) {
            mensajes.mostrarError(elementos.mensajes, error);
            elementos.resultado.innerHTML = "";
            this.marcarPosicionDelError(error);

        } finally {
            this.bloquearBoton(false);
        }
    }

    /**
     * Revisa el formulario antes de enviarlo.
     *
     * @returns {boolean} true si todo esta en orden
     */
    validar() {
        const { elementos } = this;
        let valido = true;

        const revisionF = validarFuncion(elementos.funcionF?.value, "La funcion f(x)");
        mensajes.marcarCampoInvalido(
            elementos.funcionF, elementos.errorFuncionF,
            revisionF.valido ? "" : revisionF.mensaje
        );
        valido = valido && revisionF.valido;

        // La segunda funcion solo se revisa si la pagina la pide y esta visible.
        const pideSegundaFuncion = elementos.funcionG
            && !elementos.grupoFuncionG?.classList.contains("oculto");

        if (pideSegundaFuncion) {
            const revisionG = validarFuncion(elementos.funcionG.value, "La funcion g(x)");
            mensajes.marcarCampoInvalido(
                elementos.funcionG, elementos.errorFuncionG,
                revisionG.valido ? "" : revisionG.mensaje
            );
            valido = valido && revisionG.valido;
        }

        // Con limites automaticos no hay nada que validar: los pone el servidor.
        const usaLimitesAutomaticos = elementos.limitesAutomaticos?.checked ?? false;
        if (!usaLimitesAutomaticos) {
            const a = leerNumero(elementos.limiteInferior, NaN);
            const b = leerNumero(elementos.limiteSuperior, NaN);

            const revisionLimites = validarLimites(a, b);
            if (elementos.errorLimites) {
                elementos.errorLimites.textContent = revisionLimites.valido
                    ? "" : revisionLimites.mensaje;
            }
            valido = valido && revisionLimites.valido;
        }

        const revisionParticiones = validarParticiones(
            Math.round(leerNumero(elementos.particiones, 1000))
        );
        if (!revisionParticiones.valido) {
            mensajes.mostrar(elementos.mensajes, revisionParticiones.mensaje, "error");
            valido = false;
        }

        return valido;
    }

    /**
     * Dibuja la grafica 2D y conecta la lectura de coordenadas.
     *
     * @param {object} datosGrafica datos que devolvio el backend
     * @param {object} [opciones] ajustes de dibujo
     */
    dibujarGrafica(datosGrafica, opciones = {}) {
        const { elementos } = this;
        if (!elementos.grafica || !datosGrafica) {
            return;
        }

        this.datosGrafica = datosGrafica;
        this.opcionesGrafica = opciones;

        this.sistemaGrafica = dibujar2D(elementos.grafica, datosGrafica, opciones);
        conectarLectura(elementos.grafica, elementos.lectura, this.sistemaGrafica, datosGrafica);
    }

    /** Vuelve a dibujar la ultima grafica, con los datos que ya se tienen. */
    redibujarGrafica() {
        if (this.datosGrafica) {
            this.dibujarGrafica(this.datosGrafica, this.opcionesGrafica);
        }
    }

    /**
     * Dibuja el resultado, los pasos y las tablas de un calculo de area.
     *
     * @param {object} resultado respuesta del backend
     * @param {string} [tema=""] variante de color
     */
    pintarArea(resultado, tema = "") {
        const { elementos } = this;

        panelResultados.dibujarArea(elementos.resultado, resultado, tema);
        panelResultados.escribirPlanteamiento(elementos.planteamiento, resultado);
        panelPasos.dibujar(elementos.pasos, resultado.pasos, tema);
        panelPasos.dibujarExplicacionDeLimites(
                elementos.explicacionLimites, resultado.explicacionLimites);
    }

    /**
     * Escribe en el formulario los datos con los que se resolvio de verdad.
     *
     * Importa cuando los limites se buscaron automaticamente: el estudiante dejo
     * esos campos vacios y el servidor los calculo. Si no se copiaran de vuelta,
     * el formulario diria una cosa y el procedimiento otra, que es justo lo que
     * la aplicacion no debe permitir.
     *
     * @param {object} resultado respuesta del servidor
     */
    sincronizarCampos(resultado) {
        const { elementos } = this;

        if (elementos.limiteInferior && Number.isFinite(resultado.limiteInferior)) {
            elementos.limiteInferior.value = resultado.limiteInferior;
        }
        if (elementos.limiteSuperior && Number.isFinite(resultado.limiteSuperior)) {
            elementos.limiteSuperior.value = resultado.limiteSuperior;
        }

        // El metodo del solido puede haberlo deducido el servidor midiendo la region.
        const tipoDeducido = resultado.interpretacion?.tipoSolido;
        if (elementos.tipoSolido && tipoDeducido) {
            elementos.tipoSolido.value = tipoDeducido;
        }
    }

    /**
     * Carga un ejemplo en el formulario y lo calcula.
     *
     * @param {object} ejemplo ejemplo elegido
     */
    cargarEjemplo(ejemplo) {
        const { elementos } = this;

        if (ejemplo.funcionF !== undefined && elementos.funcionF) {
            elementos.funcionF.value = ejemplo.funcionF;
        }
        if (ejemplo.funcionG !== undefined && elementos.funcionG) {
            elementos.funcionG.value = ejemplo.funcionG;
        }
        if (ejemplo.a !== undefined && elementos.limiteInferior) {
            elementos.limiteInferior.value = ejemplo.a;
        }
        if (ejemplo.b !== undefined && elementos.limiteSuperior) {
            elementos.limiteSuperior.value = ejemplo.b;
        }
        if (ejemplo.tipoSolido !== undefined && elementos.tipoSolido) {
            elementos.tipoSolido.value = ejemplo.tipoSolido;
            elementos.tipoSolido.dispatchEvent(new Event("change"));
        }
        if (ejemplo.eje !== undefined && elementos.eje) {
            elementos.eje.value = ejemplo.eje;
        }
        if (ejemplo.desplazamientoEje !== undefined && elementos.desplazamientoEje) {
            elementos.desplazamientoEje.value = ejemplo.desplazamientoEje;
        }

        this.ejecutar();
    }

    /** Vacia el formulario y los resultados. */
    limpiar() {
        const { elementos } = this;

        elementos.formulario?.reset();

        mensajes.marcarCampoInvalido(elementos.funcionF, elementos.errorFuncionF, "");
        mensajes.marcarCampoInvalido(elementos.funcionG, elementos.errorFuncionG, "");
        mensajes.limpiar(elementos.mensajes);
        mensajes.limpiar(elementos.advertencia);

        for (const zona of [elementos.pasos, elementos.explicacionLimites,
                            elementos.rebanadas]) {
            if (zona) {
                zona.innerHTML = "";
                zona.classList.add("oculto");
            }
        }

        if (elementos.planteamiento) {
            elementos.planteamiento.textContent = "";
        }

        this.datosGrafica = null;
        this.ultimoResultado = null;

        if (elementos.grafica) {
            const contexto = elementos.grafica.getContext("2d");
            contexto.clearRect(0, 0, elementos.grafica.width, elementos.grafica.height);
        }

        this.mostrarEstadoInicial();
    }

    /** Muestra el mensaje de bienvenida en la zona de resultados. */
    mostrarEstadoInicial() {
        mensajes.mostrarEstadoVacio(
            this.elementos.resultado,
            "∫",
            "Escriba una funcion y presione Calcular para ver el resultado."
        );
    }

    /**
     * Activa o desactiva los campos de limites segun la casilla automatica.
     */
    actualizarLimites() {
        const { elementos } = this;
        const automatico = elementos.limitesAutomaticos?.checked ?? false;

        for (const campo of [elementos.limiteInferior, elementos.limiteSuperior]) {
            if (campo) {
                campo.disabled = automatico;
            }
        }

        if (elementos.grupoLimites) {
            elementos.grupoLimites.style.opacity = automatico ? "0.5" : "1";
        }
    }

    /**
     * Coloca el cursor donde el parser encontro el problema.
     *
     * El backend informa la posicion exacta del caracter que no supo leer, y
     * llevar ahi el cursor ahorra tener que buscarlo a ojo.
     *
     * @param {Error} error error recibido
     */
    marcarPosicionDelError(error) {
        const posicion = error?.posicion ?? -1;
        const campo = this.elementos.funcionF;

        if (posicion < 0 || !campo) {
            return;
        }

        campo.focus();
        campo.setSelectionRange(posicion, Math.min(posicion + 1, campo.value.length));
    }

    /**
     * Bloquea el boton mientras el calculo esta en marcha.
     *
     * @param {boolean} bloqueado true para bloquear
     */
    bloquearBoton(bloqueado) {
        const boton = this.elementos.botonCalcular;
        if (!boton) {
            return;
        }
        boton.disabled = bloqueado;
        boton.textContent = bloqueado ? "Calculando..." : "Calcular";
    }
}

/**
 * Lee los campos comunes a todas las paginas.
 *
 * @param {object} elementos elementos del formulario
 * @returns {object} los datos comunes de la solicitud
 */
export function leerCamposComunes(elementos) {
    return {
        funcionF: elementos.funcionF?.value ?? "",
        // Los limites se envian tal cual estan escritos. Un campo vacio viaja como
        // cadena vacia y el servidor lo entiende como "no indicado"; convertirlo
        // aqui a un cero haria que el calculo usara un limite que nadie escribio.
        limiteInferior: elementos.limiteInferior?.value ?? "",
        limiteSuperior: elementos.limiteSuperior?.value ?? "",
        particiones: Math.round(leerNumero(elementos.particiones, 20)),
        metodo: elementos.metodo?.value ?? "ANALITICO"
    };
}
