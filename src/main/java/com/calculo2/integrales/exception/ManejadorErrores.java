package com.calculo2.integrales.exception;

import com.calculo2.integrales.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones del backend a respuestas que el navegador entiende.
 *
 * <p>Un error en un proyecto de calculo casi siempre es culpa de un dato mal escrito, no
 * de una falla del programa: un parentesis sin cerrar, un logaritmo con argumento
 * negativo, limites al reves. Por eso el mensaje que llega al navegador debe explicar el
 * problema en terminos del ejercicio, y no mostrar el nombre de una clase de Java.</p>
 *
 * <p>Los codigos de estado se reparten asi: 400 cuando el usuario escribio algo que no se
 * puede procesar, y 500 cuando el fallo es del programa. Distinguirlos ayuda a saber
 * donde buscar cuando algo sale mal.</p>
 */
public final class ManejadorErrores {

    private ManejadorErrores() {
    }

    /**
     * Envia una respuesta JSON con el codigo indicado.
     *
     * @param intercambio conexion con el navegador
     * @param codigo      codigo de estado HTTP
     * @param cuerpo      objeto que se convertira a JSON
     * @throws IOException si la conexion se corta mientras se escribe
     */
    public static void responderJson(HttpExchange intercambio, int codigo, Object cuerpo)
            throws IOException {
        byte[] datos = JsonUtil.escribir(cuerpo).getBytes(StandardCharsets.UTF_8);

        intercambio.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        intercambio.sendResponseHeaders(codigo, datos.length);

        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(datos);
        }
    }

    /**
     * Convierte una excepcion en la respuesta de error correspondiente.
     *
     * @param intercambio conexion con el navegador
     * @param error       la excepcion que se produjo
     * @throws IOException si la conexion se corta mientras se escribe
     */
    public static void responderError(HttpExchange intercambio, Throwable error) throws IOException {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("exito", false);

        int codigo;

        switch (error) {
            case ExpresionInvalidaException expresion -> {
                codigo = 400;
                cuerpo.put("tipo", "EXPRESION_INVALIDA");
                cuerpo.put("mensaje", expresion.getMessage());
                cuerpo.put("posicion", expresion.getPosicion());
                cuerpo.put("sugerencia",
                        "Revise la funcion que escribio. Recuerde cerrar todos los parentesis y usar "
                                + "los nombres del catalogo, por ejemplo sin(x), ln(x) o sqrt(x).");
            }
            case LimitesInvalidosException limites -> {
                codigo = 400;
                cuerpo.put("tipo", "DATOS_INVALIDOS");
                cuerpo.put("mensaje", limites.getMessage());
                cuerpo.put("sugerencia", limites.getSugerencia());
            }
            case IllegalArgumentException argumento -> {
                codigo = 400;
                cuerpo.put("tipo", "PETICION_INVALIDA");
                cuerpo.put("mensaje", "Los datos enviados no se pudieron leer: "
                        + argumento.getMessage());
                cuerpo.put("sugerencia", "Vuelva a enviar el formulario.");
            }
            case StackOverflowError desbordamiento -> {
                codigo = 400;
                cuerpo.put("tipo", "FUNCION_DEMASIADO_COMPLEJA");
                cuerpo.put("mensaje",
                        "La funcion es demasiado complicada o el calculo requiere demasiadas "
                                + "divisiones del intervalo.");
                cuerpo.put("sugerencia",
                        "Simplifique la funcion o reduzca el numero de particiones.");
            }
            default -> {
                codigo = 500;
                cuerpo.put("tipo", "ERROR_INTERNO");
                cuerpo.put("mensaje", "Ocurrio un error inesperado al procesar el calculo.");
                cuerpo.put("detalle", String.valueOf(error.getMessage()));
                cuerpo.put("sugerencia",
                        "Intente con otra funcion o con un intervalo distinto.");

                // Solo los errores no previstos se registran en la consola del servidor.
                System.err.println("[ERROR] " + error);
                error.printStackTrace(System.err);
            }
        }

        responderJson(intercambio, codigo, cuerpo);
    }

    /**
     * Responde que el metodo HTTP usado no corresponde a esta ruta.
     *
     * @param intercambio    conexion con el navegador
     * @param metodoEsperado el metodo que si se acepta, por ejemplo "POST"
     * @throws IOException si la conexion se corta mientras se escribe
     */
    public static void responderMetodoNoPermitido(HttpExchange intercambio, String metodoEsperado)
            throws IOException {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("exito", false);
        cuerpo.put("tipo", "METODO_NO_PERMITIDO");
        cuerpo.put("mensaje", "Esta ruta solo acepta peticiones " + metodoEsperado + ".");

        responderJson(intercambio, 405, cuerpo);
    }
}
