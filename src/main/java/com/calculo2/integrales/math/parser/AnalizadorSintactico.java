package com.calculo2.integrales.math.parser;

import com.calculo2.integrales.exception.ExpresionInvalidaException;

import java.util.List;

/**
 * Convierte la lista de tokens en el arbol de {@link NodoExpresion}.
 *
 * <p>Usa descenso recursivo: un metodo por nivel de precedencia. El orden, de menor a
 * mayor prioridad, es</p>
 *
 * <pre>
 *   expresion := termino  (('+' | '-') termino)*
 *   termino   := unario   (('*' | '/' | '%') unario)*
 *   unario    := ('-' | '+') unario | potencia
 *   potencia  := primario ('^' unario)?
 *   primario  := numero | variable | constante | funcion '(' expresion ')' | '(' expresion ')'
 * </pre>
 *
 * <p>La potencia llama a {@code unario} en su lado derecho, y por eso queda asociada a
 * la derecha ({@code 2^3^2} es {@code 2^(3^2)}) y admite exponentes negativos
 * ({@code 2^-1}). Como {@code unario} esta por encima de la potencia, {@code -x^2}
 * se interpreta como {@code -(x^2)}, igual que en clase.</p>
 */
public final class AnalizadorSintactico {

    private final List<Token> tokens;
    private int indice;

    /**
     * @param tokens lista producida por {@link Tokenizador}, terminada en FIN
     */
    public AnalizadorSintactico(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Analiza la expresion completa.
     *
     * @return la raiz del arbol
     * @throws ExpresionInvalidaException si la expresion esta mal formada
     */
    public NodoExpresion analizar() {
        NodoExpresion raiz = analizarExpresion();
        if (actual().tipo() != Token.Tipo.FIN) {
            throw new ExpresionInvalidaException(
                    "Sobra '" + actual().texto() + "' al final de la funcion.", actual().posicion());
        }
        return raiz;
    }

    // ------------------------------------------------------------------
    // NIVELES DE PRECEDENCIA
    // ------------------------------------------------------------------

    private NodoExpresion analizarExpresion() {
        NodoExpresion izquierda = analizarTermino();
        while (actual().esOperador("+") || actual().esOperador("-")) {
            String simbolo = avanzar().texto();
            NodoExpresion derecha = analizarTermino();
            izquierda = new NodoExpresion.OperacionBinaria(simbolo, izquierda, derecha);
        }
        return izquierda;
    }

    private NodoExpresion analizarTermino() {
        NodoExpresion izquierda = analizarUnario();
        while (actual().esOperador("*") || actual().esOperador("/") || actual().esOperador("%")) {
            String simbolo = avanzar().texto();
            NodoExpresion derecha = analizarUnario();
            izquierda = new NodoExpresion.OperacionBinaria(simbolo, izquierda, derecha);
        }
        return izquierda;
    }

    private NodoExpresion analizarUnario() {
        if (actual().esOperador("-")) {
            avanzar();
            return new NodoExpresion.Negacion(analizarUnario());
        }
        if (actual().esOperador("+")) {
            avanzar();
            return analizarUnario();
        }
        return analizarPotencia();
    }

    private NodoExpresion analizarPotencia() {
        NodoExpresion base = analizarPrimario();
        if (actual().esOperador("^")) {
            avanzar();
            NodoExpresion exponente = analizarUnario();
            return new NodoExpresion.OperacionBinaria("^", base, exponente);
        }
        return base;
    }

    private NodoExpresion analizarPrimario() {
        Token token = actual();

        switch (token.tipo()) {
            case NUMERO -> {
                avanzar();
                return new NodoExpresion.Numero(token.valor());
            }
            case VARIABLE -> {
                avanzar();
                return new NodoExpresion.Variable(token.texto());
            }
            case CONSTANTE -> {
                avanzar();
                return new NodoExpresion.Constante(
                        token.texto(), CatalogoFunciones.obtenerConstante(token.texto()));
            }
            case FUNCION -> {
                return analizarLlamadaFuncion();
            }
            case PARENTESIS_IZQUIERDO -> {
                avanzar();
                NodoExpresion interior = analizarExpresion();
                exigirParentesisDerecho();
                return interior;
            }
            case PARENTESIS_DERECHO -> throw new ExpresionInvalidaException(
                    "Hay un parentesis ')' que no abrio antes.", token.posicion());
            case FIN -> throw new ExpresionInvalidaException(
                    "La funcion termina antes de tiempo; falta algo despues del ultimo operador.",
                    token.posicion());
            default -> throw new ExpresionInvalidaException(
                    "No se esperaba '" + token.texto() + "' en esta parte de la funcion.",
                    token.posicion());
        }
    }

    private NodoExpresion analizarLlamadaFuncion() {
        Token nombre = avanzar();
        if (actual().tipo() != Token.Tipo.PARENTESIS_IZQUIERDO) {
            throw new ExpresionInvalidaException(
                    "Despues de '" + nombre.texto() + "' debe abrir un parentesis, por ejemplo "
                            + nombre.texto() + "(x).",
                    actual().posicion());
        }
        avanzar();
        NodoExpresion argumento = analizarExpresion();

        if (actual().tipo() == Token.Tipo.COMA) {
            throw new ExpresionInvalidaException(
                    "La funcion '" + nombre.texto() + "' recibe un solo argumento.",
                    actual().posicion());
        }
        exigirParentesisDerecho();
        return new NodoExpresion.LlamadaFuncion(nombre.texto(), argumento);
    }

    private void exigirParentesisDerecho() {
        if (actual().tipo() != Token.Tipo.PARENTESIS_DERECHO) {
            throw new ExpresionInvalidaException(
                    "Falta cerrar un parentesis.", actual().posicion());
        }
        avanzar();
    }

    // ------------------------------------------------------------------
    // RECORRIDO DE LA LISTA
    // ------------------------------------------------------------------

    private Token actual() {
        return tokens.get(Math.min(indice, tokens.size() - 1));
    }

    private Token avanzar() {
        Token token = actual();
        if (indice < tokens.size() - 1) {
            indice++;
        }
        return token;
    }
}
