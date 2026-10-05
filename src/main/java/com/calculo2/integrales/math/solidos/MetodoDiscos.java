package com.calculo2.integrales.math.solidos;

import com.calculo2.integrales.math.integracion.MetodoIntegracion;
import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.math.util.ValidadorFuncion;
import com.calculo2.integrales.model.EjeRotacion;

/**
 * Metodo de discos.
 *
 * <p>Al girar la region alrededor del eje, cada rebanada perpendicular al eje se
 * convierte en un disco: un cilindro muy delgado de radio {@code R} y espesor
 * {@code dx}. El volumen de ese cilindro es {@code pi * R^2 * dx}, y sumar todos los
 * discos es justamente integrar:</p>
 *
 * <pre>
 *   Eje de revolucion horizontal:   V = pi * integral de a a b de [R(x)]^2 dx
 *   Eje de revolucion vertical:     V = pi * integral de a a b de [R(y)]^2 dy
 * </pre>
 *
 * <p>El radio {@code R} es la distancia de la curva al eje de giro. Cuando el giro es
 * sobre el eje X, esa distancia es simplemente {@code f(x)}; cuando es sobre una recta
 * corrida {@code y = k}, pasa a ser {@code |f(x) - k|}.</p>
 *
 * <p>Este metodo aplica cuando la region toca el eje de revolucion, de modo que el solido
 * queda macizo. Si queda un hueco en medio, el metodo correcto es
 * {@link MetodoArandelas}.</p>
 */
public final class MetodoDiscos {

    private final MetodoIntegracion integrador;

    /**
     * @param integrador metodo numerico que resolvera la integral
     */
    public MetodoDiscos(MetodoIntegracion integrador) {
        this.integrador = integrador;
    }

    /**
     * Construye la funcion que hay que integrar: el radio al cuadrado.
     *
     * <p>Se devuelve por separado del volumen porque los pasos de la solucion necesitan
     * mostrarla, y porque asi el factor {@code pi} queda fuera de la integral, igual que
     * se escribe en el cuaderno.</p>
     *
     * @param curva curva que se hace girar
     * @param eje   recta alrededor de la cual gira
     * @return la funcion {@code [R(v)]^2}
     */
    public static FuncionMatematica integrando(FuncionMatematica curva, EjeRotacion eje) {
        return variable -> {
            double radio = eje.radioDesde(curva.evaluar(variable));
            return radio * radio;
        };
    }

    /**
     * Calcula el volumen del solido.
     *
     * @param curva       curva que se hace girar
     * @param eje         recta alrededor de la cual gira
     * @param a           limite inferior
     * @param b           limite superior
     * @param particiones particiones para el metodo numerico
     * @return el volumen
     */
    public double calcularVolumen(FuncionMatematica curva, EjeRotacion eje,
                                  double a, double b, int particiones) {
        double integral = integrador.integrar(integrando(curva, eje), a, b, particiones);
        return Math.PI * Math.abs(integral);
    }

    /**
     * Calcula el area de la superficie exterior del solido.
     *
     * <pre>
     *   S = 2*pi * integral de R(x) * raiz(1 + [f'(x)]^2) dx
     * </pre>
     *
     * <p>El factor de la raiz aparece porque la superficie sigue la curva inclinada, no la
     * proyeccion horizontal. La derivada se aproxima por diferencias centradas, ya que el
     * proyecto trabaja con funciones evaluables y no con derivadas simbolicas.</p>
     *
     * @param curva       curva que se hace girar
     * @param eje         recta alrededor de la cual gira
     * @param a           limite inferior
     * @param b           limite superior
     * @param particiones particiones para el metodo numerico
     * @return el area de la superficie lateral
     */
    public double calcularSuperficieLateral(FuncionMatematica curva, EjeRotacion eje,
                                            double a, double b, int particiones) {
        FuncionMatematica integrandoSuperficie = variable -> {
            double radio = eje.radioDesde(curva.evaluar(variable));
            double pendiente = derivadaAproximada(curva, variable);
            return radio * Math.sqrt(1.0 + pendiente * pendiente);
        };
        double integral = integrador.integrar(integrandoSuperficie, a, b, particiones);
        return 2.0 * Math.PI * Math.abs(integral);
    }

    /**
     * Aproxima la derivada por diferencias centradas.
     *
     * <p>El paso {@code h} se escala con la magnitud del punto: usar un paso fijo daria
     * mala precision cuando {@code x} es muy grande, porque los dos valores evaluados
     * quedarian indistinguibles en punto flotante.</p>
     */
    private static double derivadaAproximada(FuncionMatematica curva, double x) {
        double h = 1e-6 * Math.max(1.0, Math.abs(x));
        double adelante = ValidadorFuncion.evaluarSeguro(curva, x + h);
        double atras = ValidadorFuncion.evaluarSeguro(curva, x - h);

        if (Double.isNaN(adelante) || Double.isNaN(atras)) {
            return 0.0;
        }
        return (adelante - atras) / (2.0 * h);
    }
}
