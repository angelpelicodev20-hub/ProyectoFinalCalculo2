package com.calculo2.integrales.controller;

import com.calculo2.integrales.config.ConfiguracionCors;
import com.calculo2.integrales.exception.ManejadorErrores;
import com.calculo2.integrales.math.parser.EvaluadorExpresion;
import com.calculo2.integrales.model.DatosGrafica2D;
import com.calculo2.integrales.service.GraficaService;
import com.calculo2.integrales.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dibuja una funcion sin calcular ninguna integral.
 *
 * <p>Ruta: {@code POST /api/grafica}</p>
 *
 * <p>Sirve para la vista previa: mientras el usuario escribe la funcion, el formulario
 * pide los puntos y muestra la curva. Asi se ve de inmediato si la expresion esta bien
 * escrita y si los limites elegidos abarcan la region que se busca, antes de lanzar el
 * calculo completo.</p>
 *
 * <p>Acepta una segunda funcion opcional, para la vista previa del tema de area entre dos
 * curvas.</p>
 */
public final class GraficaController implements HttpHandler {

    private final GraficaService servicio = new GraficaService();

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

            String textoF = JsonUtil.texto(cuerpo, "funcionF", "");
            String textoG = JsonUtil.texto(cuerpo, "funcionG", "");
            double a = JsonUtil.numero(cuerpo, "limiteInferior", -5.0);
            double b = JsonUtil.numero(cuerpo, "limiteSuperior", 5.0);

            EvaluadorExpresion expresionF = EvaluadorExpresion.compilar(textoF);

            DatosGrafica2D grafica;
            if (textoG.isBlank()) {
                grafica = servicio.graficarUnaCurva(
                        expresionF.comoFuncion(), a, b, "f(x) = " + expresionF.expresionOriginal());
            } else {
                EvaluadorExpresion expresionG = EvaluadorExpresion.compilar(textoG);
                grafica = servicio.graficarDosCurvas(
                        expresionF.comoFuncion(), expresionG.comoFuncion(), a, b,
                        "f(x) = " + expresionF.expresionOriginal(),
                        "g(x) = " + expresionG.expresionOriginal());
            }

            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("exito", true);
            respuesta.put("funcionNormalizada", expresionF.textoNormalizado());
            respuesta.put("grafica", grafica.aMapa());

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
