package com.calculo2.integrales.math.solidos;

import com.calculo2.integrales.math.integracion.MetodoIntegracion;
import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.model.EjeRotacion;

/**
 * Metodo de capas cilindricas, tambien llamado de cascarones.
 *
 * <p>En vez de rebanar el solido perpendicular al eje, se arma con tubos concentricos.
 * Cada rebanada vertical de la region, al girar, describe un cilindro hueco muy delgado.
 * Si se corta ese cilindro y se aplana, queda una lamina rectangular cuyo largo es la
 * circunferencia {@code 2*pi*radio}, cuyo alto es la altura de la region y cuyo grosor es
 * {@code dx}. De ahi sale la formula:</p>
 *
 * <pre>
 *   V = 2*pi * integral de a a b de (radio) * (altura) dx
 * </pre>
 *
 * <p>La diferencia clave con discos y arandelas esta en la orientacion. En esos dos
 * metodos se integra en la misma direccion del eje de giro; aqui se integra en la
 * direccion <em>perpendicular</em>. Por eso el radio no es el valor de la funcion sino la
 * distancia de la propia variable al eje: al girar alrededor de la recta {@code x = k}
 * integrando en {@code x}, el radio es {@code |x - k|}.</p>
 *
 * <p>Es el metodo que conviene cuando despejar la funcion para el otro eje resulta
 * complicado o imposible.</p>
 */
public final class MetodoCapas {

    private final MetodoIntegracion integrador;

    /**
     * @param integrador metodo numerico que resolvera la integral
     */
    public MetodoCapas(MetodoIntegracion integrador) {
        this.integrador = integrador;
    }

    /**
     * Construye la funcion que hay que integrar: radio por altura.
     *
     * @param curvaSuperior curva que limita la region por arriba
     * @param curvaInferior curva que la limita por abajo; usar la constante cero si la
     *                      region llega hasta el eje horizontal
     * @param eje           recta alrededor de la cual gira
     * @return la funcion {@code radio(v) * altura(v)}
     */
    public static FuncionMatematica integrando(FuncionMatematica curvaSuperior,
                                               FuncionMatematica curvaInferior,
                                               EjeRotacion eje) {
        return variable -> {
            double radio = Math.abs(variable - eje.desplazamiento());
            double altura = Math.abs(curvaSuperior.evaluar(variable) - curvaInferior.evaluar(variable));
            return radio * altura;
        };
    }

    /**
     * Calcula el volumen del solido.
     *
     * @param curvaSuperior curva que limita la region por arriba
     * @param curvaInferior curva que la limita por abajo
     * @param eje           recta alrededor de la cual gira
     * @param a             limite inferior
     * @param b             limite superior
     * @param particiones   particiones para el metodo numerico
     * @return el volumen
     */
    public double calcularVolumen(FuncionMatematica curvaSuperior, FuncionMatematica curvaInferior,
                                  EjeRotacion eje, double a, double b, int particiones) {
        double integral = integrador.integrar(
                integrando(curvaSuperior, curvaInferior, eje), a, b, particiones);
        return 2.0 * Math.PI * Math.abs(integral);
    }

    /**
     * Devuelve el radio de la capa que pasa por un punto.
     *
     * @param eje      recta alrededor de la cual gira
     * @param variable posicion de la capa
     * @return la distancia de esa capa al eje
     */
    public static double radioEn(EjeRotacion eje, double variable) {
        return Math.abs(variable - eje.desplazamiento());
    }

    /**
     * Devuelve la altura de la capa que pasa por un punto.
     *
     * @param curvaSuperior curva de arriba
     * @param curvaInferior curva de abajo
     * @param variable      posicion de la capa
     * @return la altura de la region en ese punto
     */
    public static double alturaEn(FuncionMatematica curvaSuperior, FuncionMatematica curvaInferior,
                                  double variable) {
        return Math.abs(curvaSuperior.evaluar(variable) - curvaInferior.evaluar(variable));
    }

    /**
     * Advierte si el eje de giro pasa por dentro del intervalo de integracion.
     *
     * <p>Cuando eso ocurre, las capas de un lado y del otro se superponen al girar y el
     * resultado de la formula deja de representar el volumen del solido. El servicio usa
     * esta comprobacion para avisarle al usuario en lugar de devolver un numero
     * silenciosamente equivocado.</p>
     *
     * @param eje recta alrededor de la cual gira
     * @param a   limite inferior
     * @param b   limite superior
     * @return true si el eje corta el interior del intervalo
     */
    public static boolean ejeAtraviesaLaRegion(EjeRotacion eje, double a, double b) {
        double inicio = Math.min(a, b);
        double fin = Math.max(a, b);
        double k = eje.desplazamiento();
        return k > inicio + 1e-9 && k < fin - 1e-9;
    }
}
