package com.calculo2.integrales.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La malla del solido de revolucion, lista para dibujarse en el canvas.
 *
 * <p>La malla se guarda como una lista de anillos. Cada anillo es la circunferencia que
 * describe un punto del perfil al dar la vuelta completa alrededor del eje, y los anillos
 * van ordenados a lo largo del eje de giro. Con esa estructura el frontend puede unir el
 * anillo {@code i} con el {@code i+1} para formar las caras de la superficie.</p>
 *
 * <p>Cuando el solido es hueco se guardan dos mallas: la exterior y la del agujero.</p>
 */
public final class DatosGrafica3D {

    private final List<List<Punto3D>> mallaExterior = new ArrayList<>();
    private final List<List<Punto3D>> mallaInterior = new ArrayList<>();
    private final List<Punto2D> perfilExterior = new ArrayList<>();
    private final List<Punto2D> perfilInterior = new ArrayList<>();

    private EjeRotacion eje = EjeRotacion.ejeX();
    private double limiteInferior;
    private double limiteSuperior;
    private double radioMaximo = 1.0;
    private boolean hueco;

    /** Agrega un anillo a la superficie exterior. */
    public void agregarAnilloExterior(List<Punto3D> anillo) {
        mallaExterior.add(anillo);
    }

    /** Agrega un anillo a la superficie del agujero. */
    public void agregarAnilloInterior(List<Punto3D> anillo) {
        mallaInterior.add(anillo);
        hueco = true;
    }

    /** Agrega un punto al perfil exterior, la curva que se hace girar. */
    public void agregarPuntoPerfilExterior(double x, double y) {
        perfilExterior.add(new Punto2D(x, y));
    }

    /** Agrega un punto al perfil interior. */
    public void agregarPuntoPerfilInterior(double x, double y) {
        perfilInterior.add(new Punto2D(x, y));
    }

    /** Guarda el eje de revolucion, que el frontend dibuja como una linea de referencia. */
    public void fijarEje(EjeRotacion eje) {
        this.eje = eje;
    }

    /** Guarda el intervalo de integracion. */
    public void fijarLimites(double inferior, double superior) {
        this.limiteInferior = inferior;
        this.limiteSuperior = superior;
    }

    /**
     * Guarda el radio mas grande del solido.
     * El frontend lo usa para escalar la vista y que el solido quepa en el canvas.
     */
    public void fijarRadioMaximo(double radioMaximo) {
        this.radioMaximo = Math.max(radioMaximo, 1e-6);
    }

    /** Indica si el solido tiene agujero. */
    public boolean esHueco() {
        return hueco;
    }

    /** Arma el objeto que se envia al navegador. */
    public Map<String, Object> aMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("mallaExterior", mallaAMapa(mallaExterior));
        mapa.put("mallaInterior", mallaAMapa(mallaInterior));
        mapa.put("perfilExterior", perfilAMapa(perfilExterior));
        mapa.put("perfilInterior", perfilAMapa(perfilInterior));
        mapa.put("eje", eje.aMapa());
        mapa.put("limiteInferior", limiteInferior);
        mapa.put("limiteSuperior", limiteSuperior);
        mapa.put("radioMaximo", radioMaximo);
        mapa.put("hueco", hueco);
        return mapa;
    }

    private static List<List<Map<String, Object>>> mallaAMapa(List<List<Punto3D>> malla) {
        List<List<Map<String, Object>>> resultado = new ArrayList<>(malla.size());
        for (List<Punto3D> anillo : malla) {
            List<Map<String, Object>> puntos = new ArrayList<>(anillo.size());
            for (Punto3D punto : anillo) {
                puntos.add(punto.aMapa());
            }
            resultado.add(puntos);
        }
        return resultado;
    }

    private static List<Map<String, Object>> perfilAMapa(List<Punto2D> perfil) {
        List<Map<String, Object>> lista = new ArrayList<>(perfil.size());
        for (Punto2D punto : perfil) {
            lista.add(punto.aMapa());
        }
        return lista;
    }
}
