package com.calculo2.integrales.controller;

import com.calculo2.integrales.config.ConfiguracionCors;
import com.calculo2.integrales.exception.ManejadorErrores;
import com.calculo2.integrales.interprete.AnalizadorProblema;
import com.calculo2.integrales.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

/**
 * Interpreta un enunciado escrito en palabras.
 *
 * <p>Ruta: {@code POST /api/analizar}</p>
 *
 * <p>Recibe el texto del problema y devuelve el ejercicio ya estructurado: tema, metodo,
 * funciones, limites y eje, mas la lista de lo que falte y el razonamiento que llevo a
 * cada conclusion.</p>
 *
 * <p>Esta ruta no resuelve nada. Solo interpreta, y lo que devuelve sirve para llenar los
 * campos del formulario; resolver es un segundo paso que el estudiante lanza cuando ya
 * reviso esos datos. Separarlo asi es lo que hace que la interpretacion automatica sea una
 * ayuda y no una imposicion.</p>
 */
public final class AnalizarProblemaController implements HttpHandler {

    private final AnalizadorProblema analizador = new AnalizadorProblema();

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        if (ConfiguracionCors.atenderSondeo(intercambio)) {
            return;
        }
        ConfiguracionCors.aplicar(intercambio);

        try {
            if (!"POST".equalsIgnoreCase(intercambio.getRequestMethod())) {
                ManejadorErrores.responderMetodoNoPermitido(intercambio, "POST");
                return;
            }

            Map<String, Object> cuerpo = JsonUtil.leerCuerpo(intercambio.getRequestBody());
            String enunciado = JsonUtil.texto(cuerpo, "enunciado", "");

            Map<String, Object> respuesta = analizador.analizar(enunciado).aMapa();
            respuesta.put("exito", true);

            ManejadorErrores.responderJson(intercambio, 200, respuesta);

        } catch (IOException e) {
            throw e;
        } catch (RuntimeException | StackOverflowError e) {
            ManejadorErrores.responderError(intercambio, e);
        } finally {
            intercambio.close();
        }
    }
}
