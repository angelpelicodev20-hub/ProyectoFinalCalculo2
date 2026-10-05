package com.calculo2.integrales.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lectura y escritura de JSON sin librerias externas.
 *
 * <p>Solo cubre lo que la aplicacion necesita: objetos, arreglos, cadenas, numeros,
 * booleanos y null. Es suficiente porque el frontend y el backend son del mismo
 * proyecto y el formato de los mensajes lo definimos nosotros.</p>
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    // ------------------------------------------------------------------
    // ESCRITURA
    // ------------------------------------------------------------------

    /**
     * Convierte un valor de Java a texto JSON.
     *
     * @param valor mapa, lista, arreglo, numero, cadena, booleano o null
     * @return la representacion JSON del valor
     */
    public static String escribir(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribirValor(valor, sb);
        return sb.toString();
    }

    private static void escribirValor(Object valor, StringBuilder sb) {
        switch (valor) {
            case null -> sb.append("null");
            case Map<?, ?> mapa -> escribirMapa(mapa, sb);
            case Iterable<?> lista -> escribirIterable(lista, sb);
            case double[] arreglo -> escribirArregloDouble(arreglo, sb);
            case int[] arreglo -> escribirArregloInt(arreglo, sb);
            case Object[] arreglo -> escribirIterable(java.util.Arrays.asList(arreglo), sb);
            case Number numero -> escribirNumero(numero, sb);
            case Boolean booleano -> sb.append(booleano.toString());
            case Enum<?> constante -> escribirTexto(constante.name(), sb);
            default -> escribirTexto(valor.toString(), sb);
        }
    }

    private static void escribirMapa(Map<?, ?> mapa, StringBuilder sb) {
        sb.append('{');
        boolean primero = true;
        for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
            if (!primero) {
                sb.append(',');
            }
            primero = false;
            escribirTexto(String.valueOf(entrada.getKey()), sb);
            sb.append(':');
            escribirValor(entrada.getValue(), sb);
        }
        sb.append('}');
    }

    private static void escribirIterable(Iterable<?> lista, StringBuilder sb) {
        sb.append('[');
        boolean primero = true;
        for (Object elemento : lista) {
            if (!primero) {
                sb.append(',');
            }
            primero = false;
            escribirValor(elemento, sb);
        }
        sb.append(']');
    }

    private static void escribirArregloDouble(double[] arreglo, StringBuilder sb) {
        sb.append('[');
        for (int i = 0; i < arreglo.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            escribirNumero(arreglo[i], sb);
        }
        sb.append(']');
    }

    private static void escribirArregloInt(int[] arreglo, StringBuilder sb) {
        sb.append('[');
        for (int i = 0; i < arreglo.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(arreglo[i]);
        }
        sb.append(']');
    }

    /**
     * JSON no admite NaN ni infinito, asi que esos casos se escriben como null.
     * El frontend los interpreta como "punto que no se puede dibujar".
     */
    private static void escribirNumero(Number numero, StringBuilder sb) {
        double d = numero.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            sb.append("null");
            return;
        }
        if (numero instanceof Integer || numero instanceof Long) {
            sb.append(numero);
            return;
        }
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            sb.append((long) d);
            return;
        }
        sb.append(d);
    }

    private static void escribirTexto(String texto, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------------
    // LECTURA
    // ------------------------------------------------------------------

    /**
     * Interpreta un texto JSON que debe ser un objeto.
     *
     * @param json texto recibido en el cuerpo de la peticion
     * @return el objeto como mapa; nunca null
     * @throws IllegalArgumentException si el texto no es un objeto JSON valido
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> leerObjeto(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        Object valor = new Lector(json).leerDocumento();
        if (!(valor instanceof Map)) {
            throw new IllegalArgumentException("Se esperaba un objeto JSON.");
        }
        return (Map<String, Object>) valor;
    }

    /**
     * Lee el cuerpo de una peticion HTTP y lo interpreta como objeto JSON.
     *
     * @param entrada flujo del cuerpo de la peticion
     * @return el objeto como mapa; vacio si la peticion no traia cuerpo
     * @throws IOException              si falla la lectura del flujo
     * @throws IllegalArgumentException si el contenido no es un objeto JSON valido
     */
    public static Map<String, Object> leerCuerpo(InputStream entrada) throws IOException {
        String texto = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        return leerObjeto(texto);
    }

    // ------------------------------------------------------------------
    // ACCESO COMODO A LOS CAMPOS DE UN MAPA
    // ------------------------------------------------------------------

    /** Devuelve el campo como texto recortado, o {@code porDefecto} si no viene. */
    public static String texto(Map<String, Object> mapa, String clave, String porDefecto) {
        Object valor = mapa.get(clave);
        if (valor == null) {
            return porDefecto;
        }
        String texto = String.valueOf(valor).trim();
        return texto.isEmpty() ? porDefecto : texto;
    }

    /** Devuelve el campo como {@code double}, o {@code porDefecto} si no viene o no es numero. */
    public static double numero(Map<String, Object> mapa, String clave, double porDefecto) {
        Object valor = mapa.get(clave);
        if (valor instanceof Number numero) {
            return numero.doubleValue();
        }
        if (valor == null) {
            return porDefecto;
        }
        try {
            return Double.parseDouble(String.valueOf(valor).trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    /** Devuelve el campo como {@code int}, o {@code porDefecto} si no viene o no es numero. */
    public static int entero(Map<String, Object> mapa, String clave, int porDefecto) {
        double valor = numero(mapa, clave, porDefecto);
        return (int) Math.round(valor);
    }

    /**
     * Devuelve el campo como lista de textos; vacia si no viene o no es una lista.
     *
     * @param mapa  objeto JSON ya interpretado
     * @param clave nombre del campo
     * @return los textos, sin los vacios
     */
    public static java.util.List<String> textos(Map<String, Object> mapa, String clave) {
        java.util.List<String> resultado = new java.util.ArrayList<>();
        if (mapa.get(clave) instanceof Iterable<?> lista) {
            for (Object elemento : lista) {
                if (elemento != null && !String.valueOf(elemento).isBlank()) {
                    resultado.add(String.valueOf(elemento).trim());
                }
            }
        }
        return resultado;
    }

    /** Devuelve el campo como {@code boolean}, o {@code porDefecto} si no viene. */
    public static boolean booleano(Map<String, Object> mapa, String clave, boolean porDefecto) {
        Object valor = mapa.get(clave);
        if (valor instanceof Boolean booleano) {
            return booleano;
        }
        if (valor == null) {
            return porDefecto;
        }
        return Boolean.parseBoolean(String.valueOf(valor).trim());
    }

    // ------------------------------------------------------------------
    // LECTOR INTERNO
    // ------------------------------------------------------------------

    /** Recorre el texto JSON caracter por caracter y arma la estructura equivalente. */
    private static final class Lector {

        private final String texto;
        private int posicion;

        private Lector(String texto) {
            this.texto = texto;
        }

        private Object leerDocumento() {
            saltarEspacios();
            Object valor = leerValor();
            saltarEspacios();
            if (posicion < texto.length()) {
                throw new IllegalArgumentException(
                        "Sobra texto despues del JSON en la posicion " + posicion + ".");
            }
            return valor;
        }

        private Object leerValor() {
            saltarEspacios();
            char c = mirar();
            return switch (c) {
                case '{' -> leerObjetoInterno();
                case '[' -> leerArreglo();
                case '"' -> leerCadena();
                case 't', 'f' -> leerBooleano();
                case 'n' -> leerNulo();
                default -> leerNumero();
            };
        }

        private Map<String, Object> leerObjetoInterno() {
            Map<String, Object> mapa = new LinkedHashMap<>();
            esperar('{');
            saltarEspacios();
            if (mirar() == '}') {
                posicion++;
                return mapa;
            }
            while (true) {
                saltarEspacios();
                String clave = leerCadena();
                saltarEspacios();
                esperar(':');
                mapa.put(clave, leerValor());
                saltarEspacios();
                char c = mirar();
                if (c == ',') {
                    posicion++;
                } else if (c == '}') {
                    posicion++;
                    return mapa;
                } else {
                    throw new IllegalArgumentException(
                            "Se esperaba ',' o '}' en la posicion " + posicion + ".");
                }
            }
        }

        private List<Object> leerArreglo() {
            List<Object> lista = new ArrayList<>();
            esperar('[');
            saltarEspacios();
            if (mirar() == ']') {
                posicion++;
                return lista;
            }
            while (true) {
                lista.add(leerValor());
                saltarEspacios();
                char c = mirar();
                if (c == ',') {
                    posicion++;
                } else if (c == ']') {
                    posicion++;
                    return lista;
                } else {
                    throw new IllegalArgumentException(
                            "Se esperaba ',' o ']' en la posicion " + posicion + ".");
                }
            }
        }

        private String leerCadena() {
            esperar('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (posicion >= texto.length()) {
                    throw new IllegalArgumentException("Cadena sin cerrar.");
                }
                char c = texto.charAt(posicion++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (posicion >= texto.length()) {
                    throw new IllegalArgumentException("Escape incompleto.");
                }
                char escape = texto.charAt(posicion++);
                switch (escape) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (posicion + 4 > texto.length()) {
                            throw new IllegalArgumentException("Escape unicode incompleto.");
                        }
                        sb.append((char) Integer.parseInt(texto.substring(posicion, posicion + 4), 16));
                        posicion += 4;
                    }
                    default -> throw new IllegalArgumentException("Escape desconocido.");
                }
            }
        }

        private Boolean leerBooleano() {
            if (texto.startsWith("true", posicion)) {
                posicion += 4;
                return Boolean.TRUE;
            }
            if (texto.startsWith("false", posicion)) {
                posicion += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Valor invalido en la posicion " + posicion + ".");
        }

        private Object leerNulo() {
            if (texto.startsWith("null", posicion)) {
                posicion += 4;
                return null;
            }
            throw new IllegalArgumentException("Valor invalido en la posicion " + posicion + ".");
        }

        private Double leerNumero() {
            int inicio = posicion;
            if (mirar() == '-' || mirar() == '+') {
                posicion++;
            }
            while (posicion < texto.length() && esParteDeNumero(texto.charAt(posicion))) {
                posicion++;
            }
            String fragmento = texto.substring(inicio, posicion);
            try {
                return Double.valueOf(fragmento);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Numero invalido: " + fragmento);
            }
        }

        private static boolean esParteDeNumero(char c) {
            return Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-';
        }

        private char mirar() {
            if (posicion >= texto.length()) {
                throw new IllegalArgumentException("JSON incompleto.");
            }
            return texto.charAt(posicion);
        }

        private void esperar(char esperado) {
            if (mirar() != esperado) {
                throw new IllegalArgumentException(
                        "Se esperaba " + esperado + " en la posicion " + posicion + ".");
            }
            posicion++;
        }

        private void saltarEspacios() {
            while (posicion < texto.length() && Character.isWhitespace(texto.charAt(posicion))) {
                posicion++;
            }
        }
    }
}
