package com.calculo2.integrales.controller;

import com.calculo2.integrales.config.ConfiguracionCors;
import com.calculo2.integrales.exception.ManejadorErrores;
import com.calculo2.integrales.model.SolicitudSolido;
import com.calculo2.integrales.service.SolidoRevolucionService;
import com.calculo2.integrales.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

/**
 * Atiende la ruta del tema de solidos de revolucion.
 *
 * <p>Ruta: {@code POST /api/solido-revolucion}</p>
 *
 * <p>Recibe la curva (o las dos curvas, si son arandelas), los limites, el eje de giro y
 * el metodo geometrico. Devuelve el volumen, la explicacion paso a paso y la malla de
 * puntos que el navegador dibuja como solido.</p>
 */
public final class SolidoRevolucionController implements HttpHandler {

    private final SolidoRevolucionService servicio = new SolidoRevolucionService();

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
            SolicitudSolido solicitud = SolicitudSolido.desdeJson(cuerpo);

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
