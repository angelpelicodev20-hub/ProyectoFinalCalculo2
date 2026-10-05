package com.calculo2.integrales.math.util;

import com.calculo2.integrales.util.Constantes;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ajusta los resultados antes de mostrarlos.
 *
 * <p>Los metodos numericos acumulan error de redondeo: una integral que vale
 * exactamente 12 puede salir como 11.999999999997. Aqui se limpia ese ruido para que
 * el resultado en pantalla coincida con el que el alumno obtiene a mano.</p>
 */
public final class Redondeo {

    private Redondeo() {
    }

    /**
     * Redondea al numero de decimales que se muestran en la aplicacion.
     *
     * @param valor numero a redondear
     * @return el valor redondeado, o el mismo valor si es NaN o infinito
     */
    public static double aDecimalesDeSalida(double valor) {
        return a(valor, Constantes.DECIMALES_RESULTADO);
    }

    /**
     * Redondea a la cantidad de decimales indicada.
     *
     * @param valor    numero a redondear
     * @param decimales cuantos decimales conservar
     * @return el valor redondeado, o el mismo valor si es NaN o infinito
     */
    public static double a(double valor, int decimales) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            return valor;
        }
        return BigDecimal.valueOf(valor)
                .setScale(decimales, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Convierte a cero los valores que solo son ruido de redondeo.
     *
     * <p>Sin esto, una integral simetrica como la de {@code sin(x)} entre {@code -pi} y
     * {@code pi} podria mostrarse como {@code -0.000000000000001} en lugar de {@code 0}.</p>
     *
     * @param valor numero a limpiar
     * @return cero si el valor es despreciable; el mismo valor en caso contrario
     */
    public static double limpiarCeros(double valor) {
        if (Math.abs(valor) < 1e-12) {
            return 0.0;
        }
        return valor;
    }

    /**
     * Aplica la limpieza de ceros y luego el redondeo de salida.
     * Es la combinacion que usan los servicios antes de responder.
     */
    public static double paraMostrar(double valor) {
        return aDecimalesDeSalida(limpiarCeros(valor));
    }

    /**
     * Da formato de texto a un numero para las explicaciones paso a paso.
     *
     * <p>Los enteros se escriben sin decimales, para que un paso diga "= 12" y no
     * "= 12.000000".</p>
     *
     * @param valor numero a escribir
     * @return el numero como texto
     */
    public static String texto(double valor) {
        if (Double.isNaN(valor)) {
            return "indefinido";
        }
        if (Double.isInfinite(valor)) {
            return valor > 0 ? "+infinito" : "-infinito";
        }
        double limpio = paraMostrar(valor);
        if (limpio == Math.rint(limpio) && Math.abs(limpio) < 1e15) {
            return String.valueOf((long) limpio);
        }
        return BigDecimal.valueOf(limpio)
                .stripTrailingZeros()
                .toPlainString();
    }

    /**
     * Da formato con una cantidad concreta de decimales, sin quitar los ceros finales.
     * Se usa en las tablas donde conviene que todas las columnas se alineen.
     *
     * @param valor     numero a escribir
     * @param decimales decimales fijos
     * @return el numero como texto
     */
    public static String texto(double valor, int decimales) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            return texto(valor);
        }
        return BigDecimal.valueOf(valor)
                .setScale(decimales, RoundingMode.HALF_UP)
                .toPlainString();
    }
}
