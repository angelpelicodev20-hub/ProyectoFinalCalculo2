package com.calculo2.integrales.controller;

import com.calculo2.integrales.config.ConfiguracionCors;
import com.calculo2.integrales.exception.ManejadorErrores;
import com.calculo2.integrales.service.CalculadoraService;
import com.calculo2.integrales.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

/**
 * Atiende la modalidad de calculadora.
 *
 * <p>Ruta: {@code POST /api/calcular}</p>
 *
 * <p>Recibe una expresion y, si lleva variable, el valor que debe tomar. Devuelve el
 * resultado, como se interpreto la expresion y su forma exacta cuando es una fraccion
 * sencilla.</p>
 */
public final class CalculadoraController implements HttpHandler {

    private final CalculadoraService servicio = new CalculadoraService();

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

            String expresion = JsonUtil.texto(cuerpo, "expresion", "");
            String variable = JsonUtil.texto(cuerpo, "variable", "x");

            // Un campo vacio significa que la expresion no lleva variable, no que valga cero.
            Object valorRecibido = cuerpo.get("valor");
            boolean tieneValor = valorRecibido != null
                    && !String.valueOf(valorRecibido).isBlank();
            double valor = JsonUtil.numero(cuerpo, "valor", 0);

            ManejadorErrores.responderJson(intercambio, 200,
                    servicio.evaluar(expresion, variable, valor, tieneValor));

        } catch (IOException e) {
            throw e;
        } catch (RuntimeException | StackOverflowError e) {
            ManejadorErrores.responderError(intercambio, e);
        } finally {
            intercambio.close();
        }
    }
}
