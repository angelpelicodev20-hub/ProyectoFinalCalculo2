package com.calculo2.integrales.config;

import com.calculo2.integrales.controller.AnalizarProblemaController;
import com.calculo2.integrales.controller.AreaBajoCurvaController;
import com.calculo2.integrales.controller.AreaEntreCurvasController;
import com.calculo2.integrales.controller.CalculadoraController;
import com.calculo2.integrales.controller.GraficaController;
import com.calculo2.integrales.controller.SaludController;
import com.calculo2.integrales.controller.SolidoRevolucionController;
import com.calculo2.integrales.util.Constantes;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.Executors;

/**
 * Levanta el servidor HTTP y conecta cada ruta con su controlador.
 *
 * <p>Se usa {@code com.sun.net.httpserver.HttpServer}, que viene incluido en el JDK. Con
 * eso el proyecto no necesita Tomcat, Spring ni ninguna libreria externa: basta con
 * ejecutar el jar y abrir el navegador.</p>
 *
 * <p>El mismo servidor cumple dos papeles. Entrega los archivos de {@code static} — el
 * HTML, el CSS y el JavaScript — y ademas responde las rutas bajo {@code /api} que hacen
 * los calculos. Como todo sale del mismo origen, el navegador no impone restricciones
 * entre una cosa y la otra.</p>
 */
public final class ConfiguracionServidor {

    private final int puerto;
    private HttpServer servidor;

    /** Crea la configuracion con el puerto que indique {@code application.properties}. */
    public ConfiguracionServidor() {
        this(leerPuertoConfigurado());
    }

    /**
     * @param puerto puerto en que escuchara el servidor
     */
    public ConfiguracionServidor(int puerto) {
        this.puerto = puerto;
    }

    /**
     * Crea el servidor, registra las rutas y lo pone a escuchar.
     *
     * @return el puerto en que quedo escuchando
     * @throws IOException si el puerto ya esta ocupado
     */
    public int iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(puerto), 0);

        // Rutas que hacen los calculos, una por tema.
        servidor.createContext("/api/area-bajo-curva", new AreaBajoCurvaController());
        servidor.createContext("/api/area-entre-curvas", new AreaEntreCurvasController());
        servidor.createContext("/api/solido-revolucion", new SolidoRevolucionController());

        // Interpreta un enunciado escrito en palabras; no resuelve, solo estructura.
        servidor.createContext("/api/analizar", new AnalizarProblemaController());

        // Evalua expresiones sueltas: es la modalidad de calculadora.
        servidor.createContext("/api/calcular", new CalculadoraController());

        servidor.createContext("/api/grafica", new GraficaController());
        servidor.createContext("/api/salud", new SaludController());

        // Todo lo demas se busca entre los archivos del frontend.
        servidor.createContext("/", ConfiguracionServidor::servirArchivoEstatico);

        // Un grupo de hilos permite atender varias peticiones a la vez; sin esto, una
        // integral pesada dejaria al resto de la pagina esperando.
        servidor.setExecutor(Executors.newFixedThreadPool(4));
        servidor.start();

        return puerto;
    }

    /** Detiene el servidor. */
    public void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    /** Puerto en que escucha el servidor. */
    public int puerto() {
        return puerto;
    }

    // ------------------------------------------------------------------
    // ARCHIVOS DEL FRONTEND
    // ------------------------------------------------------------------

    /**
     * Entrega un archivo de la carpeta {@code static}.
     *
     * <p>Los archivos se leen del classpath y no del disco, para que sigan funcionando
     * cuando el proyecto se empaqueta en un jar.</p>
     */
    private static void servirArchivoEstatico(HttpExchange intercambio) throws IOException {
        try {
            String ruta = intercambio.getRequestURI().getPath();

            // La raiz y las rutas que terminan en barra sirven la pagina principal.
            if (ruta.equals("/") || ruta.endsWith("/")) {
                ruta += "index.html";
            }

            String recurso = Constantes.RAIZ_ESTATICA + normalizar(ruta);

            try (InputStream entrada = ConfiguracionServidor.class.getResourceAsStream(recurso)) {
                if (entrada == null) {
                    responderNoEncontrado(intercambio, ruta);
                    return;
                }

                byte[] contenido = entrada.readAllBytes();
                intercambio.getResponseHeaders().set("Content-Type", tipoDeContenido(recurso));
                intercambio.sendResponseHeaders(200, contenido.length);

                try (OutputStream salida = intercambio.getResponseBody()) {
                    salida.write(contenido);
                }
            }
        } finally {
            intercambio.close();
        }
    }

    /**
     * Limpia la ruta pedida para que no se pueda salir de la carpeta {@code static}.
     *
     * <p>Sin esta limpieza, una peticion como {@code /../../application.properties} podria
     * leer archivos que no son del frontend.</p>
     */
    private static String normalizar(String ruta) {
        String limpia = ruta.replace("\\", "/");
        while (limpia.contains("..")) {
            limpia = limpia.replace("..", "");
        }
        return limpia.startsWith("/") ? limpia : "/" + limpia;
    }

    /** Deduce el tipo de contenido a partir de la extension del archivo. */
    private static String tipoDeContenido(String recurso) {
        String nombre = recurso.toLowerCase();
        if (nombre.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (nombre.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (nombre.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (nombre.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        if (nombre.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (nombre.endsWith(".png")) {
            return "image/png";
        }
        if (nombre.endsWith(".jpg") || nombre.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (nombre.endsWith(".ico")) {
            return "image/x-icon";
        }
        return "application/octet-stream";
    }

    private static void responderNoEncontrado(HttpExchange intercambio, String ruta)
            throws IOException {
        String mensaje = """
                <!doctype html>
                <html lang="es">
                <head><meta charset="utf-8"><title>Pagina no encontrada</title></head>
                <body style="font-family: system-ui, sans-serif; padding: 2rem;">
                  <h1>404 &mdash; No se encontro la pagina</h1>
                  <p>La ruta <code>%s</code> no existe en esta aplicacion.</p>
                  <p><a href="/">Volver al inicio</a></p>
                </body>
                </html>
                """.formatted(ruta);

        byte[] datos = mensaje.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        intercambio.sendResponseHeaders(404, datos.length);

        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(datos);
        }
    }

    // ------------------------------------------------------------------
    // CONFIGURACION
    // ------------------------------------------------------------------

    /**
     * Lee el puerto de {@code application.properties}.
     *
     * @return el puerto configurado, o el valor por defecto si el archivo no esta o el
     *         valor no es un numero
     */
    private static int leerPuertoConfigurado() {
        try (InputStream entrada = ConfiguracionServidor.class
                .getResourceAsStream("/application.properties")) {

            if (entrada == null) {
                return Constantes.PUERTO_POR_DEFECTO;
            }
            Properties propiedades = new Properties();
            propiedades.load(entrada);

            String valor = propiedades.getProperty("servidor.puerto");
            return valor == null ? Constantes.PUERTO_POR_DEFECTO : Integer.parseInt(valor.trim());

        } catch (IOException | NumberFormatException e) {
            return Constantes.PUERTO_POR_DEFECTO;
        }
    }
}
