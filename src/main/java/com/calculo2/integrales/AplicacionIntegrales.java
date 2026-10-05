package com.calculo2.integrales;

import com.calculo2.integrales.config.ConfiguracionServidor;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/**
 * Punto de entrada de la aplicacion.
 *
 * <p>Arranca el servidor y, si el sistema lo permite, abre el navegador en la pagina
 * principal. A partir de ahi todo ocurre en el navegador: el usuario escribe la funcion,
 * el JavaScript la manda al servidor, Java calcula y devuelve el resultado con los puntos
 * de la grafica.</p>
 *
 * <p>Se ejecuta con:</p>
 *
 * <pre>
 *   mvn clean package
 *   java -jar target/calculo2.jar
 * </pre>
 *
 * <p>Opcionalmente se le puede pasar otro puerto como argumento, util si el 8080 ya esta
 * ocupado por otro programa:</p>
 *
 * <pre>
 *   java -jar target/calculo2.jar 9090
 * </pre>
 */
public final class AplicacionIntegrales {

    private AplicacionIntegrales() {
    }

    /**
     * @param argumentos opcionalmente, el puerto en que debe escuchar el servidor
     */
    public static void main(String[] argumentos) {
        ConfiguracionServidor configuracion = crearConfiguracion(argumentos);

        try {
            int puerto = configuracion.iniciar();
            String direccion = "http://localhost:" + puerto;

            imprimirBienvenida(direccion);
            abrirNavegador(direccion);

            // Deja el servidor corriendo hasta que se cierre la consola con Ctrl+C.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nCerrando el servidor. Hasta luego.");
                configuracion.detener();
            }));

        } catch (IOException e) {
            System.err.println("No se pudo iniciar el servidor en el puerto "
                    + configuracion.puerto() + ".");
            System.err.println("Puede que otro programa lo este usando. Pruebe con otro puerto:");
            System.err.println("    java -jar target/calculo2.jar 9090");
            System.exit(1);
        }
    }

    /** Arma la configuracion, tomando el puerto del argumento si viene uno valido. */
    private static ConfiguracionServidor crearConfiguracion(String[] argumentos) {
        if (argumentos.length > 0) {
            try {
                return new ConfiguracionServidor(Integer.parseInt(argumentos[0].trim()));
            } catch (NumberFormatException e) {
                System.err.println("El puerto '" + argumentos[0] + "' no es un numero. "
                        + "Se usara el puerto configurado.");
            }
        }
        return new ConfiguracionServidor();
    }

    private static void imprimirBienvenida(String direccion) {
        System.out.println();
        System.out.println("  PROYECTO FINAL DE CALCULO II");
        System.out.println("  Universidad Mariano Galvez de Guatemala");
        System.out.println();
        System.out.println("  Temas que resuelve:");
        System.out.println("    1. Area bajo la curva");
        System.out.println("    2. Area entre dos curvas");
        System.out.println("    3. Solidos de revolucion");
        System.out.println();
        System.out.println("  Servidor listo en:  " + direccion);
        System.out.println("  Para detenerlo, presione Ctrl+C en esta ventana.");
        System.out.println();
    }

    /**
     * Abre el navegador en la pagina principal.
     *
     * <p>No siempre se puede: hay sistemas sin escritorio grafico y consolas donde la
     * llamada falla. Si no funciona, el usuario todavia tiene la direccion impresa arriba,
     * asi que el fallo no debe detener el programa.</p>
     */
    private static void abrirNavegador(String direccion) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(direccion));
            }
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            System.out.println("  (Abra el navegador manualmente en la direccion de arriba.)");
        }
    }
}
