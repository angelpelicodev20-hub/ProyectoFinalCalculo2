package com.calculo2.integrales.controller;

import com.calculo2.integrales.config.ConfiguracionCors;
import com.calculo2.integrales.exception.ManejadorErrores;
import com.calculo2.integrales.math.integracion.SumaRiemann;
import com.calculo2.integrales.math.parser.CatalogoFunciones;
import com.calculo2.integrales.model.MetodoArea;
import com.calculo2.integrales.model.TemaProblema;
import com.calculo2.integrales.model.TipoSolido;
import com.calculo2.integrales.util.Constantes;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Informa que el servidor esta arriba y que opciones ofrece.
 *
 * <p>Ruta: {@code GET /api/salud}</p>
 *
 * <p>Cumple dos funciones. La primera es de diagnostico: si la pagina no responde, abrir
 * esta ruta dice de inmediato si el problema esta en el servidor o en el navegador. La
 * segunda es llenar la interfaz sin duplicar listas: los selectores de metodo, los de tipo
 * de solido y las tablas de la pagina de ayuda se construyen con lo que devuelve esta
 * ruta.</p>
 *
 * <p>Ese detalle importa mas de lo que parece. Como la ayuda se llena desde aqui, no puede
 * quedar describiendo metodos que la aplicacion ya no tiene: al quitar un metodo del enum
 * de Java desaparece a la vez del selector y de la documentacion.</p>
 */
public final class SaludController implements HttpHandler {

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        if (ConfiguracionCors.atenderSondeo(intercambio)) {
            return;
        }
        ConfiguracionCors.aplicar(intercambio);

        try {
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("exito", true);
            respuesta.put("estado", "activo");
            respuesta.put("aplicacion", "Proyecto Final Calculo 2");
            respuesta.put("versionJava", System.getProperty("java.version"));
            respuesta.put("temas", listaDeTemas());
            respuesta.put("metodosDeArea", listaDeMetodosDeArea());
            respuesta.put("posicionesRiemann", listaDePosicionesRiemann());
            respuesta.put("tiposDeSolido", listaDeTiposDeSolido());
            respuesta.put("funciones", CatalogoFunciones.descripciones());
            respuesta.put("constantes", CatalogoFunciones.constantes());
            respuesta.put("limites", listaDeLimites());

            ManejadorErrores.responderJson(intercambio, 200, respuesta);

        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            ManejadorErrores.responderError(intercambio, e);
        } finally {
            intercambio.close();
        }
    }

    /** Los tres temas que resuelve la aplicacion. */
    private static List<Map<String, Object>> listaDeTemas() {
        List<Map<String, Object>> lista = new ArrayList<>();

        for (TemaProblema tema : TemaProblema.values()) {
            if (!tema.estaDeterminado()) {
                continue;
            }
            Map<String, Object> entrada = new LinkedHashMap<>();
            entrada.put("clave", tema.name());
            entrada.put("etiqueta", tema.etiqueta());
            entrada.put("pagina", tema.pagina());
            entrada.put("necesitaDosFunciones", tema.necesitaDosFunciones());
            entrada.put("necesitaEje", tema.necesitaEje());
            lista.add(entrada);
        }
        return lista;
    }

    /**
     * Las formas de resolver un area.
     *
     * <p>Son dos: la integracion analitica, que resuelve por la regla de Barrow, y los
     * rectangulos de Riemann, que se conservan porque muestran de donde nace la integral.
     * Los demas metodos numericos se retiraron.</p>
     */
    private static List<Map<String, Object>> listaDeMetodosDeArea() {
        List<Map<String, Object>> lista = new ArrayList<>();

        for (MetodoArea metodo : MetodoArea.values()) {
            Map<String, Object> entrada = new LinkedHashMap<>();
            entrada.put("clave", metodo.name());
            entrada.put("etiqueta", metodo.etiqueta());
            entrada.put("dibujaRectangulos", metodo.dibujaRectangulos());
            entrada.put("descripcion", descripcionDelMetodo(metodo));
            entrada.put("formula", formulaDelMetodo(metodo));
            lista.add(entrada);
        }
        return lista;
    }

    private static String descripcionDelMetodo(MetodoArea metodo) {
        return switch (metodo) {
            case ANALITICO -> "Busca la primitiva de la funcion y aplica la regla de Barrow: "
                    + "el resultado es exacto y queda un procedimiento completo que se puede "
                    + "comparar con el del cuaderno.";
            case RIEMANN -> "Divide el intervalo en rectangulos y suma sus areas. Es la "
                    + "definicion de la que nace la integral definida; se conserva porque ver "
                    + "los rectangulos sobre la curva explica el concepto mejor que cualquier "
                    + "formula.";
        };
    }

    private static String formulaDelMetodo(MetodoArea metodo) {
        return switch (metodo) {
            case ANALITICO -> "A = F(b) - F(a),  donde F'(x) = f(x)";
            case RIEMANN -> "A = suma desde k=1 hasta n de f(x_k) * h,  con h = (b - a) / n";
        };
    }

    /** Donde se puede medir la altura de los rectangulos de Riemann. */
    private static List<Map<String, Object>> listaDePosicionesRiemann() {
        List<Map<String, Object>> lista = new ArrayList<>();

        for (SumaRiemann.Posicion posicion : SumaRiemann.Posicion.values()) {
            Map<String, Object> entrada = new LinkedHashMap<>();
            entrada.put("clave", posicion.name());
            entrada.put("etiqueta", posicion.etiqueta());
            entrada.put("descripcion", posicion.descripcion());
            lista.add(entrada);
        }
        return lista;
    }

    /** Los tres metodos para solidos, con su formula y cuando conviene cada uno. */
    private static List<Map<String, Object>> listaDeTiposDeSolido() {
        List<Map<String, Object>> lista = new ArrayList<>();

        for (TipoSolido tipo : TipoSolido.values()) {
            Map<String, Object> entrada = new LinkedHashMap<>();
            entrada.put("clave", tipo.name());
            entrada.put("etiqueta", tipo.etiqueta());
            entrada.put("formula", tipo.formula());
            entrada.put("explicacion", tipo.explicacion());
            entrada.put("necesitaSegundaCurva", tipo.necesitaSegundaCurva());
            lista.add(entrada);
        }
        return lista;
    }

    /** Los topes que aplica el servidor, para que el formulario los respete. */
    private static Map<String, Object> listaDeLimites() {
        Map<String, Object> limites = new LinkedHashMap<>();
        limites.put("particionesMinimas", Constantes.PARTICIONES_MINIMAS);
        limites.put("particionesMaximas", Constantes.PARTICIONES_MAXIMAS);
        limites.put("longitudMaximaExpresion", Constantes.LONGITUD_MAXIMA_EXPRESION);
        limites.put("decimales", Constantes.DECIMALES_RESULTADO);
        return limites;
    }
}
