package com.calculo2.integrales.service;

import com.calculo2.integrales.exception.LimitesInvalidosException;
import com.calculo2.integrales.math.algebra.ResolutorEcuaciones;
import com.calculo2.integrales.math.parser.EvaluadorExpresion;
import com.calculo2.integrales.math.parser.FuncionMatematica;
import com.calculo2.integrales.math.parser.NodoExpresion;
import com.calculo2.integrales.math.util.BuscadorRaices;
import com.calculo2.integrales.math.util.Redondeo;
import com.calculo2.integrales.math.util.ValidadorFuncion;

import java.util.ArrayList;
import java.util.List;

/**
 * Encuentra donde se cortan dos curvas y en que tramos cada una va arriba.
 *
 * <p>Es la pieza central del tema de area entre dos curvas. La formula del material del
 * curso, {@code A = integral de (f - g) dx}, supone que {@code f} esta por encima de
 * {@code g} en todo el intervalo. Cuando las curvas se cruzan eso deja de valer: en los
 * tramos donde se invierten, la diferencia se vuelve negativa y las areas se cancelan
 * entre si en lugar de sumarse.</p>
 *
 * <p>La solucion es partir el intervalo en los puntos de corte y tratar cada tramo por
 * separado, tomando siempre el valor absoluto. Eso es lo que prepara esta clase.</p>
 */
public final class InterseccionService {

    /**
     * Un tramo en que se corta el intervalo, con la informacion de que curva va arriba.
     *
     * @param inicio        donde empieza el tramo
     * @param fin           donde termina
     * @param fEstaArriba   true si {@code f} va por encima de {@code g} en este tramo
     */
    public record Tramo(double inicio, double fin, boolean fEstaArriba) {

        /** Ancho del tramo. */
        public double ancho() {
            return fin - inicio;
        }

        /** Descripcion en palabras, para los pasos y la tabla de resultados. */
        public String descripcion(String nombreF, String nombreG) {
            String arriba = fEstaArriba ? nombreF : nombreG;
            String abajo = fEstaArriba ? nombreG : nombreF;
            return "De " + Redondeo.texto(inicio) + " a " + Redondeo.texto(fin)
                    + ": " + arriba + " va arriba y " + abajo + " abajo.";
        }
    }

    /**
     * Resuelve {@code f = g} (o {@code f = 0} si no hay segunda curva) de forma exacta
     * siempre que se pueda, con el procedimiento escrito.
     *
     * <p>Es el paso que el curso pide hacer a mano: igualar, pasar todo a un lado,
     * factorizar y leer las raices. {@link ResolutorEcuaciones} lo hace asi y deja cada
     * linea escrita; si la ecuacion no es polinomica ni se puede reducir a una, recurre al
     * calculo numerico y lo dice.</p>
     *
     * @param f     primera curva
     * @param g     segunda curva, o null para cortar con el eje
     * @param desde inicio de la busqueda numerica, si hiciera falta
     * @param hasta fin de la busqueda numerica
     * @return las soluciones y el procedimiento
     */
    public ResolutorEcuaciones.Solucion resolverCortes(EvaluadorExpresion f, EvaluadorExpresion g,
                                                       double desde, double hasta) {
        NodoExpresion derecha = g == null ? new NodoExpresion.Numero(0) : g.raiz();
        String textoDerecha = g == null ? "0" : g.expresionOriginal();
        ResolutorEcuaciones.Solucion solucion = ResolutorEcuaciones.resolver(
                f.raiz(), derecha, f.expresionOriginal(), textoDerecha, f.variable(), desde, hasta);
        return solucion.entre(desde, hasta);
    }

    /**
     * Redacta los cortes ya resueltos, con los puntos del plano que se obtienen.
     *
     * @param solucion la ecuacion resuelta
     * @param nombreF  como se llama la primera curva en el texto
     * @param nombreG  como se llama la segunda, o cadena vacia si se corta con el eje
     * @param variable variable de integracion
     * @param a        limite inferior ya decidido
     * @param b        limite superior
     * @return el texto, o cadena vacia si no habia nada que resolver
     */
    public String explicarCortes(ResolutorEcuaciones.Solucion solucion, String nombreF,
                                 String nombreG, String variable, double a, double b) {
        if (solucion.identicas()) {
            return "";
        }
        StringBuilder texto = new StringBuilder();
        if (nombreG.isBlank()) {
            texto.append("Se iguala la funcion a cero, f(").append(variable).append(") = 0:\n\n");
        } else {
            texto.append("Se igualan las dos curvas, f(").append(variable).append(") = g(")
                 .append(variable).append("):\n\n");
        }
        for (String paso : solucion.pasos()) {
            texto.append("   ").append(paso).append('\n');
        }

        if (solucion.raices().isEmpty()) {
            texto.append("\nNo hay puntos de corte");
            return texto.toString().trim();
        }

        List<String> dentro = new ArrayList<>();
        List<String> fuera = new ArrayList<>();
        for (var raiz : solucion.raices()) {
            String valor = raiz.exacta() ? raiz.texto() : Redondeo.texto(raiz.valor());
            if (raiz.valor() >= a - 1e-9 && raiz.valor() <= b + 1e-9) {
                dentro.add(variable + " = " + valor);
            } else {
                fuera.add(variable + " = " + valor);
            }
        }
        if (!dentro.isEmpty()) {
            texto.append("\nDentro del intervalo [").append(Redondeo.texto(a)).append(", ")
                 .append(Redondeo.texto(b)).append("] quedan: ").append(String.join(",  ", dentro))
                 .append('.');
        }
        if (!fuera.isEmpty()) {
            texto.append("\nFuera del intervalo, y por eso no se usan: ")
                 .append(String.join(",  ", fuera)).append('.');
        }
        return texto.toString().trim();
    }

    /**
     * Busca los puntos donde las dos curvas se cortan dentro del intervalo.
     *
     * @param funcionF primera curva
     * @param funcionG segunda curva
     * @param a        limite inferior
     * @param b        limite superior
     * @return las posiciones de corte, ordenadas
     */
    public List<Double> buscarIntersecciones(FuncionMatematica funcionF, FuncionMatematica funcionG,
                                             double a, double b) {
        return BuscadorRaices.buscarEnIntervalo(funcionF.menos(funcionG), a, b);
    }

    /**
     * Parte el intervalo en tramos donde no cambia cual curva va arriba.
     *
     * @param funcionF primera curva
     * @param funcionG segunda curva
     * @param a        limite inferior
     * @param b        limite superior
     * @return la lista de tramos, en orden
     */
    public List<Tramo> dividirEnTramos(FuncionMatematica funcionF, FuncionMatematica funcionG,
                                       double a, double b) {
        FuncionMatematica diferencia = funcionF.menos(funcionG);
        List<Double> cortes = BuscadorRaices.cortesDeIntegracion(diferencia, a, b);
        List<Tramo> tramos = new ArrayList<>();

        for (int i = 0; i < cortes.size() - 1; i++) {
            double inicio = cortes.get(i);
            double fin = cortes.get(i + 1);

            if (fin - inicio < 1e-12) {
                continue;
            }

            // Se decide con el punto medio del tramo: en los extremos la diferencia vale
            // cero justamente porque ahi se cruzan, y el signo no diria nada.
            double medio = (inicio + fin) / 2.0;
            double valorMedio = ValidadorFuncion.evaluarSeguro(diferencia, medio);

            tramos.add(new Tramo(inicio, fin, valorMedio >= 0));
        }
        return tramos;
    }

    /**
     * Deduce los limites de integracion a partir de las propias intersecciones.
     *
     * <p>Resuelve el caso tipico del ejercicio que dice "el area encerrada entre las dos
     * curvas" sin dar limites: la region encerrada va del primer punto de corte al
     * ultimo.</p>
     *
     * @param funcionF        primera curva
     * @param funcionG        segunda curva
     * @param rangoDeBusqueda hasta donde buscar a cada lado del origen
     * @return un arreglo con el limite inferior y el superior
     * @throws LimitesInvalidosException si las curvas no se cortan en al menos dos puntos
     */
    public double[] deducirLimites(FuncionMatematica funcionF, FuncionMatematica funcionG,
                                   double rangoDeBusqueda) {
        List<Double> cortes = buscarIntersecciones(
                funcionF, funcionG, -rangoDeBusqueda, rangoDeBusqueda);

        if (cortes.size() < 2) {
            throw new LimitesInvalidosException(
                    "Las dos curvas se cortan en " + cortes.size() + " punto(s) dentro del rango "
                            + "buscado, asi que no encierran una region. Escriba los limites de "
                            + "integracion a mano.");
        }

        return new double[]{cortes.get(0), cortes.get(cortes.size() - 1)};
    }

    /**
     * Redacta como se obtuvieron los limites a partir de las intersecciones.
     *
     * <p>Buscar los limites automaticamente y usarlos sin mas convertiria el resultado en
     * algo que el estudiante tiene que creer. Este texto deja a la vista el planteamiento
     * completo — la ecuacion que se resolvio y las soluciones que salieron — para que se
     * pueda comprobar a mano.</p>
     *
     * @param textoF        primera funcion, escrita
     * @param textoG        segunda funcion, escrita
     * @param intersecciones puntos de corte encontrados
     * @param variable      nombre de la variable
     * @return la explicacion, lista para mostrarse
     */
    public String explicarLimites(String textoF, String textoG, List<Double> intersecciones,
                                  String variable) {
        StringBuilder explicacion = new StringBuilder();

        explicacion.append("Se igualan las dos funciones para hallar donde se cortan:\n\n");
        explicacion.append("f(").append(variable).append(") = g(").append(variable).append(")\n");
        explicacion.append(textoF).append(" = ").append(textoG).append("\n\n");

        explicacion.append("Equivale a resolver f(").append(variable)
                   .append(") - g(").append(variable).append(") = 0:\n");
        explicacion.append("(").append(textoF).append(") - (").append(textoG).append(") = 0\n\n");

        if (intersecciones.isEmpty()) {
            explicacion.append("No se encontraron puntos de corte en el rango explorado.");
            return explicacion.toString();
        }

        explicacion.append("Soluciones:\n");
        for (double corte : intersecciones) {
            explicacion.append("  ").append(variable).append(" = ")
                       .append(Redondeo.texto(corte)).append('\n');
        }

        if (intersecciones.size() >= 2) {
            double primera = intersecciones.get(0);
            double ultima = intersecciones.get(intersecciones.size() - 1);

            explicacion.append("\nLa region encerrada va del primer corte al ultimo, asi que "
                    + "esos dos valores son los limites de integracion: de ")
                       .append(Redondeo.texto(primera)).append(" a ")
                       .append(Redondeo.texto(ultima)).append('.');
        }

        return explicacion.toString();
    }

    /**
     * Comprueba si la funcion cruza el eje horizontal dentro del intervalo.
     *
     * <p>Importa en el tema de area bajo la curva: si la curva baja del eje, la integral
     * definida devuelve un numero menor que el area geometrica, porque la parte de abajo
     * cuenta como negativa.</p>
     *
     * @param funcion funcion a revisar
     * @param a       limite inferior
     * @param b       limite superior
     * @return las posiciones donde la curva cruza el eje
     */
    public List<Double> buscarCrucesConElEje(FuncionMatematica funcion, double a, double b) {
        List<Double> cruces = new ArrayList<>();
        double inicio = Math.min(a, b);
        double fin = Math.max(a, b);

        for (double raiz : BuscadorRaices.buscarEnIntervalo(funcion, inicio, fin)) {
            boolean esInterior = raiz > inicio + 1e-9 && raiz < fin - 1e-9;
            if (esInterior && cambiaDeSignoAlrededor(funcion, raiz, fin - inicio)) {
                cruces.add(raiz);
            }
        }
        return cruces;
    }

    /**
     * Distingue un cruce real de un simple toque.
     *
     * <p>Una funcion como {@code x^2} vale cero en el origen pero no cambia de signo: no
     * hay area negativa que separar, asi que no hace falta partir la integral ahi.</p>
     */
    private boolean cambiaDeSignoAlrededor(FuncionMatematica funcion, double raiz, double ancho) {
        double h = Math.max(ancho * 1e-4, 1e-9);
        double antes = ValidadorFuncion.evaluarSeguro(funcion, raiz - h);
        double despues = ValidadorFuncion.evaluarSeguro(funcion, raiz + h);

        if (Double.isNaN(antes) || Double.isNaN(despues)) {
            return false;
        }
        return antes * despues < 0;
    }
}
