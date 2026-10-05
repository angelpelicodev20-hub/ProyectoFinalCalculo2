package com.calculo2.integrales.model;

import com.calculo2.integrales.util.JsonUtil;

import java.util.List;
import java.util.Map;

/**
 * Los datos que llegan del formulario para calcular un solido de revolucion.
 *
 * <p>Ademas de la funcion y los limites hace falta saber alrededor de que recta gira la
 * region. El metodo geometrico puede venir indicado o puede dejarse en blanco: cuando no
 * viene, el sistema lo deduce midiendo la region, que es lo que hace el analizador de
 * enunciados.</p>
 *
 * @param funcionExterior curva que define el radio exterior, o el radio unico en discos
 * @param funcionInterior curva del radio interior; se usa en arandelas y capas
 * @param limiteInferior  limite inferior, o null si no se indico
 * @param limiteSuperior  limite superior, o null si no se indico
 * @param particiones     cortes para dibujar la malla y las rebanadas de la tabla
 * @param tipoSolido      metodo geometrico, o null para que se deduzca
 * @param eje             recta alrededor de la cual gira la region
 * @param buscarLimites   si los limites deben deducirse de los puntos de corte
 * @param explicacionLimites como se dedujeron los limites al interpretar el enunciado, o
 *                           cadena vacia si los escribio el estudiante. Viaja con la
 *                           peticion para que el procedimiento pueda explicar de donde
 *                           salieron: unos limites deducidos y presentados sin su cuenta
 *                           no se distinguen de unos inventados.
 * @param explicacionRegion  como se armo la region al interpretar el enunciado: que curvas
 *                           hubo que despejar y que rama se uso. Cadena vacia si no hubo
 *                           enunciado.
 * @param curvasDelEnunciado las curvas tal como las escribio el enunciado, para dibujarlas
 *                           completas en la grafica y no solo el trozo que cierra la region
 */
public record SolicitudSolido(String funcionExterior, String funcionInterior,
                              Double limiteInferior, Double limiteSuperior,
                              int particiones, TipoSolido tipoSolido, EjeRotacion eje,
                              boolean buscarLimites, String explicacionLimites,
                              String explicacionRegion, List<String> curvasDelEnunciado) {

    /** Normaliza los campos opcionales. */
    public SolicitudSolido {
        explicacionRegion = explicacionRegion == null ? "" : explicacionRegion;
        curvasDelEnunciado = curvasDelEnunciado == null ? List.of() : List.copyOf(curvasDelEnunciado);
    }

    /**
     * La solicitud sin el contexto del enunciado, como la arma el formulario de la pagina
     * del tema.
     */
    public SolicitudSolido(String funcionExterior, String funcionInterior,
                           Double limiteInferior, Double limiteSuperior,
                           int particiones, TipoSolido tipoSolido, EjeRotacion eje,
                           boolean buscarLimites, String explicacionLimites) {
        this(funcionExterior, funcionInterior, limiteInferior, limiteSuperior, particiones,
                tipoSolido, eje, buscarLimites, explicacionLimites, "", List.of());
    }

    /**
     * Arma la solicitud a partir del JSON recibido.
     *
     * @param cuerpo objeto JSON ya interpretado
     * @return la solicitud lista para validarse
     */
    public static SolicitudSolido desdeJson(Map<String, Object> cuerpo) {
        double desplazamiento = JsonUtil.numero(cuerpo, "desplazamientoEje", 0.0);
        String tipoTexto = JsonUtil.texto(cuerpo, "tipoSolido", "");

        return new SolicitudSolido(
                JsonUtil.texto(cuerpo, "funcionExterior", ""),
                JsonUtil.texto(cuerpo, "funcionInterior", ""),
                numeroOpcional(cuerpo, "limiteInferior"),
                numeroOpcional(cuerpo, "limiteSuperior"),
                JsonUtil.entero(cuerpo, "particiones", 2000),
                tipoTexto.isBlank() ? null : TipoSolido.desdeTexto(tipoTexto),
                EjeRotacion.desdeTexto(JsonUtil.texto(cuerpo, "eje", "x"), desplazamiento),
                JsonUtil.booleano(cuerpo, "buscarLimites", false),
                JsonUtil.texto(cuerpo, "explicacionLimites", ""),
                JsonUtil.texto(cuerpo, "explicacionRegion", ""),
                JsonUtil.textos(cuerpo, "curvasDelEnunciado"));
    }

    /** Lee un numero que puede no venir; un campo vacio significa "no indicado". */
    private static Double numeroOpcional(Map<String, Object> cuerpo, String clave) {
        Object valor = cuerpo.get(clave);
        if (valor == null || String.valueOf(valor).isBlank()) {
            return null;
        }
        double numero = JsonUtil.numero(cuerpo, clave, Double.NaN);
        return Double.isNaN(numero) ? null : numero;
    }

    /** Indica si la peticion incluye la curva del radio interior. */
    public boolean tieneCurvaInterior() {
        return funcionInterior != null && !funcionInterior.isBlank();
    }

    /** Indica si los dos limites vinieron indicados. */
    public boolean tieneLimites() {
        return limiteInferior != null && limiteSuperior != null;
    }

    /** El limite inferior y el superior en orden creciente. */
    public double inicio() {
        return Math.min(limiteInferior, limiteSuperior);
    }

    /** El limite superior y el inferior en orden creciente. */
    public double fin() {
        return Math.max(limiteInferior, limiteSuperior);
    }

    /**
     * Nombre de la variable de integracion.
     *
     * <p>La regla vive en {@link TipoSolido}, porque no depende de esta peticion sino de
     * la geometria del metodo, y el analizador de enunciados necesita la misma respuesta
     * sin tener una peticion delante.</p>
     *
     * @param tipo metodo geometrico que se va a aplicar
     * @return "x" o "y"
     */
    public String variableDeIntegracion(TipoSolido tipo) {
        return tipo.variableDeIntegracion(eje);
    }
}
