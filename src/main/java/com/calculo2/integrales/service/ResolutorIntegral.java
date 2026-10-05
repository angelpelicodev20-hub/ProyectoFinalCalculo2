package com.calculo2.integrales.service;

import com.calculo2.integrales.math.integracion.ReglaSimpson;
import com.calculo2.integrales.math.parser.NodoExpresion;
import com.calculo2.integrales.math.simbolico.ConversorPolinomio;
import com.calculo2.integrales.math.simbolico.FormateadorMatematico;
import com.calculo2.integrales.math.simbolico.Fraccion;
import com.calculo2.integrales.math.simbolico.IntegradorSimbolico;
import com.calculo2.integrales.math.simbolico.Polinomio;
import com.calculo2.integrales.math.util.Redondeo;
import com.calculo2.integrales.util.Constantes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resuelve una integral definida y deja por escrito como lo hizo.
 *
 * <p>Todos los temas terminan en el mismo sitio: una integral entre dos limites. Lo que
 * cambia de un tema a otro es como se construye el integrando, no como se resuelve. Por
 * eso los tres servicios pasan por aqui y obtienen la misma clase de respuesta, con lo
 * cual el procedimiento tiene el mismo aspecto en toda la aplicacion.</p>
 *
 * <p>Se intenta siempre el camino analitico: encontrar la primitiva y aplicar la regla de
 * Barrow. Es el que sirve para estudiar, porque deja escrito {@code F(x)} y su
 * evaluacion. Cuando la expresion se sale de lo que el integrador simbolico sabe
 * resolver, se recurre a una aproximacion numerica, y eso se dice con todas las letras en
 * lugar de presentar el numero como si fuera exacto.</p>
 */
public final class ResolutorIntegral {

    /** Particiones de la aproximacion numerica, cuando hay que recurrir a ella. */
    private static final int PARTICIONES_DE_RESPALDO = 20_000;

    /**
     * Una integral ya resuelta, con todo lo necesario para explicarla.
     *
     * @param valor         el resultado de la integral
     * @param integrando    el integrando escrito en limpio
     * @param desarrollado  el integrando desarrollado, o cadena vacia si no hacia falta
     * @param antiderivada  la primitiva, o cadena vacia si se resolvio por aproximacion
     * @param reglas        las reglas de integracion aplicadas
     * @param sustitucion   la evaluacion de la primitiva en los dos limites
     * @param fueAnalitica  true si se resolvio encontrando la primitiva
     */
    public record Resolucion(double valor, String integrando, String desarrollado,
                             String antiderivada, List<String> reglas,
                             String sustitucion, boolean fueAnalitica) {

        /** Indica si hubo que desarrollar el integrando antes de integrarlo. */
        public boolean seDesarrollo() {
            return !desarrollado.isBlank() && !desarrollado.equals(integrando);
        }
    }

    /**
     * Resuelve la integral definida del integrando entre los dos limites.
     *
     * @param integrando expresion a integrar
     * @param variable   nombre de la variable de integracion
     * @param a          limite inferior
     * @param b          limite superior
     * @return la resolucion completa
     */
    public Resolucion resolver(NodoExpresion integrando, String variable, double a, double b) {
        String textoIntegrando = FormateadorMatematico.escribir(integrando);
        String desarrollado = desarrollarSiEsPolinomio(integrando, variable);

        Optional<IntegradorSimbolico.Antiderivada> primitiva =
                IntegradorSimbolico.integrar(integrando, variable);

        if (primitiva.isPresent()) {
            IntegradorSimbolico.Antiderivada F = primitiva.get();
            double valor = F.integralDefinida(a, b);

            return new Resolucion(
                    valor,
                    textoIntegrando,
                    desarrollado,
                    F.texto(),
                    F.reglas(),
                    escribirSustitucion(F, variable, a, b),
                    true);
        }

        // Sin primitiva no hay regla de Barrow. Se aproxima, y se dice.
        double valor = new ReglaSimpson().integrar(
                integrando::evaluar, a, b, PARTICIONES_DE_RESPALDO);

        return new Resolucion(
                valor,
                textoIntegrando,
                desarrollado,
                "",
                List.of("No se encontro una primitiva elemental para este integrando, asi que "
                        + "el valor se obtuvo por aproximacion numerica en lugar de por la "
                        + "regla de Barrow."),
                "",
                false);
    }

    /**
     * Desarrolla el integrando cuando es un polinomio.
     *
     * <p>Es el paso de simplificar que se hace a mano antes de integrar: un integrando
     * como {@code (-x^2+4)^2 - (2x+1)^2} no se puede integrar termino a termino tal como
     * esta, y desarrollarlo a {@code x^4 - 12x^2 - 4x + 15} lo deja listo para aplicar la
     * regla de la potencia.</p>
     *
     * @return el polinomio desarrollado, o cadena vacia si el integrando no es polinomico
     */
    private String desarrollarSiEsPolinomio(NodoExpresion integrando, String variable) {
        Optional<String> polinomio = ConversorPolinomio.convertir(integrando)
                .map(p -> com.calculo2.integrales.math.algebra.Despejador.textoPolinomio(p, variable));
        if (polinomio.isPresent()) {
            return polinomio.get();
        }
        // Con raices, el desarrollo es la suma de potencias: x*sqrt(8x) = 2sqrt(2)x^(3/2).
        return com.calculo2.integrales.math.simbolico.SumaDePotencias.desde(integrando)
                .filter(suma -> !suma.esVacia())
                .map(suma -> suma.texto(variable))
                .orElse("");
    }

    /**
     * Escribe la sustitucion de los dos limites en la primitiva.
     *
     * <p>Es el paso que mas se equivoca a mano, porque hay que evaluar dos veces y restar
     * respetando los signos. Dejarlo escrito con los dos valores a la vista permite
     * comparar con el cuaderno de inmediato.</p>
     */
    private String escribirSustitucion(IntegradorSimbolico.Antiderivada primitiva,
                                       String variable, double a, double b) {
        double enSuperior = primitiva.evaluar(b);
        double enInferior = primitiva.evaluar(a);

        StringBuilder texto = new StringBuilder();
        texto.append("F(").append(variable).append(") = ").append(primitiva.texto()).append('\n');
        texto.append("F(").append(Redondeo.texto(b)).append(") = ")
             .append(Fraccion.texto(enSuperior)).append('\n');
        texto.append("F(").append(Redondeo.texto(a)).append(") = ")
             .append(Fraccion.texto(enInferior)).append('\n');
        texto.append("F(").append(Redondeo.texto(b)).append(") - F(")
             .append(Redondeo.texto(a)).append(") = ")
             .append(Fraccion.texto(enSuperior - enInferior));

        return texto.toString();
    }

    // ------------------------------------------------------------------
    // APOYO PARA LOS SERVICIOS
    // ------------------------------------------------------------------

    /**
     * Construye el arbol del integrando de una diferencia de cuadrados.
     *
     * <p>Es el integrando del metodo de arandelas: {@code [R(v)]^2 - [r(v)]^2}. Se arma
     * como arbol y no como texto para poder integrarlo simbolicamente despues.</p>
     *
     * @param radioExterior expresion del radio mayor
     * @param radioInterior expresion del radio menor
     * @return el arbol de la diferencia de los cuadrados
     */
    public static NodoExpresion diferenciaDeCuadrados(NodoExpresion radioExterior,
                                                      NodoExpresion radioInterior) {
        return new NodoExpresion.OperacionBinaria("-",
                alCuadrado(radioExterior), alCuadrado(radioInterior));
    }

    /** Eleva una expresion al cuadrado. */
    public static NodoExpresion alCuadrado(NodoExpresion expresion) {
        return new NodoExpresion.OperacionBinaria("^", expresion, new NodoExpresion.Numero(2));
    }

    /** Resta dos expresiones. */
    public static NodoExpresion restar(NodoExpresion izquierda, NodoExpresion derecha) {
        return new NodoExpresion.OperacionBinaria("-", izquierda, derecha);
    }

    /** Multiplica dos expresiones. */
    public static NodoExpresion multiplicar(NodoExpresion izquierda, NodoExpresion derecha) {
        return new NodoExpresion.OperacionBinaria("*", izquierda, derecha);
    }

    /**
     * Desplaza una expresion respecto del eje de giro.
     *
     * <p>El radio de un disco es la distancia de la curva al eje. Cuando el eje es un eje
     * coordenado esa distancia es el valor de la funcion, pero si el eje esta corrido hay
     * que restarle el desplazamiento. Devolver la expresion sin tocar cuando el
     * desplazamiento es cero evita arrastrar un " - 0" por todo el procedimiento.</p>
     *
     * @param expresion      la curva
     * @param desplazamiento el valor k del eje
     * @return la expresion de la distancia al eje
     */
    public static NodoExpresion respectoAlEje(NodoExpresion expresion, double desplazamiento) {
        if (Math.abs(desplazamiento) < 1e-12) {
            return expresion;
        }
        return new NodoExpresion.OperacionBinaria("-", expresion,
                new NodoExpresion.Numero(desplazamiento));
    }

    /**
     * Desarrolla una expresion a polinomio si se puede, para mostrarla simplificada.
     *
     * @param expresion expresion a desarrollar
     * @param variable  nombre de la variable
     * @return el polinomio, o vacio si la expresion no es polinomica
     */
    public static Optional<Polinomio> comoPolinomio(NodoExpresion expresion, String variable) {
        return ConversorPolinomio.convertir(expresion);
    }

    /**
     * Reparte las particiones entre varios tramos, respetando el minimo del proyecto.
     *
     * @param particiones particiones totales
     * @param tramos      en cuantos tramos se reparten
     * @return las particiones que le tocan a cada tramo
     */
    public static int particionesPorTramo(int particiones, int tramos) {
        int reparto = particiones / Math.max(1, tramos);
        return Math.max(Constantes.PARTICIONES_MINIMAS, reparto);
    }

    /** Las listas de reglas se devuelven copiadas para que nadie las modifique por fuera. */
    public static List<String> copiar(List<String> original) {
        return new ArrayList<>(original);
    }
}
