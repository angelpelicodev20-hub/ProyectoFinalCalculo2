package com.calculo2.integrales.config;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/**
 * Cabeceras que permiten abrir el frontend desde un origen distinto al del servidor.
 *
 * <p>En el uso normal no hacen falta: el mismo servidor Java entrega el HTML y responde
 * las rutas, asi que todo viaja bajo el mismo origen. Se incluyen para el caso en que
 * alguien abra los archivos de {@code static} directamente con doble clic, o los sirva
 * desde el Live Server del editor mientras desarrolla. Sin estas cabeceras el navegador
 * bloquearia esas peticiones.</p>
 */
public final class ConfiguracionCors {

    private ConfiguracionCors() {
    }

    /**
     * Agrega las cabeceras a una respuesta.
     *
     * @param intercambio conexion con el navegador
     */
    public static void aplicar(HttpExchange intercambio) {
        Headers cabeceras = intercambio.getResponseHeaders();
        cabeceras.set("Access-Control-Allow-Origin", "*");
        cabeceras.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        cabeceras.set("Access-Control-Allow-Headers", "Content-Type");
        cabeceras.set("Access-Control-Max-Age", "3600");
    }

    /**
     * Atiende la peticion de sondeo que el navegador manda antes de un POST.
     *
     * @param intercambio conexion con el navegador
     * @return true si la peticion era de sondeo y ya quedo respondida
     * @throws IOException si la conexion se corta mientras se escribe
     */
    public static boolean atenderSondeo(HttpExchange intercambio) throws IOException {
        if (!"OPTIONS".equalsIgnoreCase(intercambio.getRequestMethod())) {
            return false;
        }
        aplicar(intercambio);
        intercambio.sendResponseHeaders(204, -1);
        intercambio.close();
        return true;
    }
}
