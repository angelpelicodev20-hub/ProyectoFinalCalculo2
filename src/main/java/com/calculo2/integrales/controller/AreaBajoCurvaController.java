package com.calculo2.integrales.controller;

import com.calculo2.integrales.config.ConfiguracionCors;
import com.calculo2.integrales.exception.ManejadorErrores;
import com.calculo2.integrales.model.SolicitudIntegral;
import com.calculo2.integrales.service.AreaBajoCurvaService;
import com.calculo2.integrales.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

/**
 * Atiende la ruta del tema de area bajo la curva.
 *
 * <p>Ruta: {@code POST /api/area-bajo-curva}</p>
 *
 * <p>Recibe la funcion, los limites, el numero de particiones y el metodo numerico;
 * devuelve el area, la explicacion paso a paso y los puntos de la grafica.</p>
 *
 * <p>El controlador no hace matematica: solo lee el JSON, se lo pasa al servicio y
 * convierte la respuesta. Toda la logica del tema vive en
 * {@link AreaBajoCurvaService}.</p>
 */
public final class AreaBajoCurvaController implements HttpHandler {

    private final AreaBajoCurvaService servicio = new AreaBajoCurvaService();

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
            SolicitudIntegral solicitud = SolicitudIntegral.desdeJson(cuerpo);

            ManejadorErrores.responderJson(intercambio, 200, servicio.calcular(solicitud).aMapa());

        } catch (IOException e) {
            throw e;
        } catch (RuntimeException | StackOverflowError e) {
            ManejadorErrores.responderError(intercambio, e);
        } finally {
            intercambio.close();
        }
    }
}
