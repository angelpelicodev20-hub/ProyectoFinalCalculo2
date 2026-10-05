package com.calculo2.integrales.model;

import com.calculo2.integrales.math.integracion.SumaRiemann;
import com.calculo2.integrales.util.Constantes;
import com.calculo2.integrales.util.JsonUtil;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Los datos que llegan del formulario para calcular un area.
 *
 * <p>Sirve tanto para area bajo la curva como para area entre dos curvas: en el primer
 * caso {@code funcionG} viene vacia y se entiende que la segunda frontera es el eje X.</p>
 *
 * <p>Los limites se guardan como {@link Double} para poder distinguir "vale cero" de "no
 * se indico". Esa diferencia importa cuando el estudiante marca la casilla de buscar los
 * limites: entonces los campos llegan vacios a proposito y el sistema los calcula, en
 * lugar de tomarlos por ceros.</p>
 *
 * @param funcionF         la funcion principal, tal como la escribio el usuario
 * @param funcionG         la segunda curva, o cadena vacia si no aplica
 * @param limiteInferior   limite inferior, o null si no se indico
 * @param limiteSuperior   limite superior, o null si no se indico
 * @param particiones      rectangulos de la suma de Riemann
 * @param metodo           forma de resolver elegida
 * @param posicionRiemann  donde se mide la altura de cada rectangulo
 * @param buscarLimites    si los limites deben deducirse de los puntos de corte
 * @param explicacionLimites como se dedujeron los limites al interpretar el enunciado, o
 *                           cadena vacia si los escribio el estudiante. Viaja con la
 *                           peticion para que el procedimiento pueda explicar de donde
 *                           salieron: unos limites deducidos y presentados sin su cuenta
 *                           no se distinguen de unos inventados.
 * @param variable         variable de integracion: x, o y cuando la region se describe
 *                         con curvas x = g(y)
 * @param explicacionRegion como se armo la region al interpretar el enunciado
 * @param curvasDelEnunciado las curvas tal como las escribio el enunciado, para la grafica
 */
public record SolicitudIntegral(String funcionF, String funcionG,
                                Double limiteInferior, Double limiteSuperior,
                                int particiones, MetodoArea metodo,
                                SumaRiemann.Posicion posicionRiemann,
                                boolean buscarLimites, String explicacionLimites,
                                String variable, String explicacionRegion,
                                List<String> curvasDelEnunciado) {

    /**
     * La solicitud de siempre: funciones de x y sin contexto del enunciado.
     */
    public SolicitudIntegral(String funcionF, String funcionG,
                             Double limiteInferior, Double limiteSuperior,
                             int particiones, MetodoArea metodo,
                             SumaRiemann.Posicion posicionRiemann,
                             boolean buscarLimites, String explicacionLimites) {
        this(funcionF, funcionG, limiteInferior, limiteSuperior, particiones, metodo,
                posicionRiemann, buscarLimites, explicacionLimites, "x", "", List.of());
    }

    /**
     * Normaliza la variable: solo se admiten x e y.
     */
    public SolicitudIntegral {
        variable = (variable != null && variable.trim().equalsIgnoreCase("y")) ? "y" : "x";
        explicacionRegion = explicacionRegion == null ? "" : explicacionRegion;
        curvasDelEnunciado = curvasDelEnunciado == null ? List.of() : List.copyOf(curvasDelEnunciado);
    }

    /**
     * Arma la solicitud a partir del JSON recibido.
     *
     * @param cuerpo objeto JSON ya interpretado
     * @return la solicitud lista para validarse
     */
    public static SolicitudIntegral desdeJson(Map<String, Object> cuerpo) {
        return new SolicitudIntegral(
                JsonUtil.texto(cuerpo, "funcionF", ""),
                JsonUtil.texto(cuerpo, "funcionG", ""),
                numeroOpcional(cuerpo, "limiteInferior"),
                numeroOpcional(cuerpo, "limiteSuperior"),
                JsonUtil.entero(cuerpo, "particiones", 20),
                MetodoArea.desdeTexto(JsonUtil.texto(cuerpo, "metodo", "ANALITICO")),
                posicionDesdeTexto(JsonUtil.texto(cuerpo, "posicionRiemann", "MEDIO")),
                JsonUtil.booleano(cuerpo, "buscarLimites", false),
                JsonUtil.texto(cuerpo, "explicacionLimites", ""),
                JsonUtil.texto(cuerpo, "variable", "x"),
                JsonUtil.texto(cuerpo, "explicacionRegion", ""),
                JsonUtil.textos(cuerpo, "curvasDelEnunciado"));
    }

    /**
     * Lee un numero que puede no venir.
     *
     * <p>Un campo vacio del formulario llega como cadena vacia o como null, y en los dos
     * casos significa lo mismo: el estudiante no lo indico.</p>
     */
    private static Double numeroOpcional(Map<String, Object> cuerpo, String clave) {
        Object valor = cuerpo.get(clave);
        if (valor == null || String.valueOf(valor).isBlank()) {
            return null;
        }
        double numero = JsonUtil.numero(cuerpo, clave, Double.NaN);
        return Double.isNaN(numero) ? null : numero;
    }

    /** Interpreta donde se mide la altura de los rectangulos. */
    private static SumaRiemann.Posicion posicionDesdeTexto(String texto) {
        String normalizado = texto.trim().toUpperCase(Locale.ROOT);
        for (SumaRiemann.Posicion posicion : SumaRiemann.Posicion.values()) {
            if (posicion.name().equals(normalizado)) {
                return posicion;
            }
        }
        return SumaRiemann.Posicion.MEDIO;
    }

    /** Indica si la peticion incluye una segunda curva. */
    public boolean tieneSegundaCurva() {
        return funcionG != null && !funcionG.isBlank();
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
     * Indica si el usuario escribio los limites al reves.
     *
     * <p>Importa porque intercambiar los limites de una integral definida le cambia el
     * signo al resultado, y eso se explica en los pasos.</p>
     */
    public boolean limitesInvertidos() {
        return tieneLimites() && limiteInferior > limiteSuperior;
    }

    /** El numero de rectangulos, acotado a lo que tiene sentido dibujar y calcular. */
    public int particionesValidas() {
        return Math.max(Constantes.PARTICIONES_MINIMAS,
                Math.min(particiones, Constantes.PARTICIONES_MAXIMAS));
    }
}
