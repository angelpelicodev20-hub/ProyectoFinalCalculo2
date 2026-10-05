package com.calculo2.integrales.controller;

import com.calculo2.integrales.config.ConfiguracionCors;
import com.calculo2.integrales.exception.ManejadorErrores;
import com.calculo2.integrales.model.SolicitudIntegral;
import com.calculo2.integrales.service.AreaEntreCurvasService;
import com.calculo2.integrales.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

/**
 * Atiende la ruta del tema de area entre dos curvas.
 *
 * <p>Ruta: {@code POST /api/area-entre-curvas}</p>
 *
 * <p>Recibe las dos funciones y, o bien los limites, o bien la indicacion de deducirlos
 * de los puntos de corte. Devuelve el area, los tramos en que se dividio el intervalo, la
 * explicacion paso a paso y los puntos de las dos curvas.</p>
 */
public final class AreaEntreCurvasController implements HttpHandler {

    private final AreaEntreCurvasService servicio = new AreaEntreCurvasService();

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
