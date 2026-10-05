# Proyecto Final Cálculo 2

Aplicación para resolver integrales y mostrar su representación gráfica en 2D y 3D.

El usuario escribe la función a integrar, la aplicación calcula el resultado, dibuja la región
o el sólido correspondiente y muestra el procedimiento paso a paso.

## Temas que resuelve

1. **Área bajo la curva** — integral definida de `f(x)` en `[a, b]`.
2. **Área entre dos curvas** — integral de `|f(x) - g(x)|` entre sus intersecciones.
3. **Sólidos de revolución** — volumen por discos, arandelas y capas cilíndricas.

## Cómo ejecutarlo

```bash
mvn clean package
java -jar target/calculo2.jar
```

El navegador se abre solo en `http://localhost:8080`. Si ese puerto está ocupado, se le puede
pasar otro:

```bash
java -jar target/calculo2.jar 9090
```

Para detener el servidor, `Ctrl+C` en la consola.

## Tecnologías

- **Backend:** Java 21+, únicamente con la biblioteca estándar. El servidor HTTP es el que
  viene incluido en el JDK (`com.sun.net.httpserver`), y el intérprete de funciones, los
  métodos de integración y el generador de JSON están escritos desde cero.
- **Frontend:** HTML, CSS y JavaScript sin frameworks. Las gráficas 2D y el visor 3D están
  dibujados a mano sobre `<canvas>`, incluida la proyección del espacio al plano.
- **Pruebas:** JUnit 5, la única dependencia externa del proyecto y solo en el ámbito de test.

No se usa ningún servicio ni API externa: todo el cálculo ocurre en la máquina donde corre.

## Cómo está repartido el trabajo

Java hace toda la matemática y JavaScript solo dibuja. El backend evalúa las funciones, resuelve
las integrales y devuelve tanto el resultado como los puntos ya calculados de la curva; el
frontend traduce esos puntos a píxeles. Así la gráfica nunca puede contradecir al número, porque
los dos salen de la misma evaluación.

## Tres modos de uso

1. **Calculadora** — se escribe una expresión y se obtiene su valor. Para las cuentas sueltas
   que aparecen en medio de un ejercicio.
2. **Resolver un problema escrito** — se copia el enunciado tal como está en la hoja. La
   aplicación identifica el tema y el método, extrae funciones, límites y eje, y llena con
   ellos los campos del ejercicio para que el estudiante los revise antes de resolver.
3. **Validar un ejercicio hecho a mano** — se elige el tema, se introduce lo que uno planteó y
   la aplicación resuelve de nuevo mostrando el procedimiento completo, para compararlo con el
   del cuaderno.

## Cómo se resuelve

Los ejercicios se resuelven **analíticamente**: se busca la primitiva y se aplica la regla de
Barrow. Eso es lo que permite mostrar `F(x)` y su evaluación en los límites, que es el
procedimiento del curso; un método numérico daría el mismo número sin nada que copiar.

El integrador simbólico cubre polinomios (desarrollando productos y potencias), la regla de la
potencia con cualquier exponente constante, el caso `1/x` y las funciones del catálogo con
argumento lineal. Cuando una expresión queda fuera de ese alcance lo dice con todas las letras y
calcula el valor por aproximación, en lugar de inventar una primitiva.

| Opción | Dónde aparece | Para qué sirve |
|---|---|---|
| Integración directa (Barrow) | los tres temas | resultado exacto y procedimiento completo |
| Rectángulos de Riemann | área bajo la curva | ver de dónde nace la integral |
| Discos, arandelas, capas | sólidos de revolución | el método lo decide la geometría de la región |

## Interpretación de enunciados

El analizador no busca palabras sueltas. Combina tres clases de indicio: la dimensión de lo que
se pide (un volumen exige una revolución), la presencia de una recta nombrada como eje de giro
(ese dato solo tiene sentido si algo rota) y el número de fronteras de la región, que es lo que
separa el área bajo una curva del área entre dos.

El método de un sólido se deduce midiendo: se recorre el intervalo calculando la distancia de la
región al eje. Si la toca, las rebanadas son discos; si se mantiene separada, hay hueco y son
arandelas; y si el eje es perpendicular a la variable de las funciones, corresponden capas. Pero
si el enunciado pide un método concreto —"aplicando el método de capas cilíndricas"— se aplica
ese, porque el ejercicio está practicando justamente ese método; la diferencia se avisa, nunca se
cambia en silencio.

### Deducir no es inventar

Que el enunciado no escriba un dato no significa que no lo diga. El planificador de la región
(`PlanificadorRegion`) prueba todas las combinaciones de bordes —las curvas, sus ramas, las rectas,
los ejes— y todos los intervalos posibles: el escrito, el que cierran dos rectas, el que cierran
los cortes de las curvas, el que cierran una recta y un corte, o una recta y el eje de giro. Se
queda con la región que usa todas las fronteras que nombra el enunciado, y lo hace dos veces: con
rebanadas verticales (respecto de x) y con rebanadas horizontales (respecto de y). Si en x la
región quedaría partida en tramos —como con `x = y²` y `x = y + 6`—, la describe en y. Solo si
ninguna combinación cierra la región se pide el dato.

Las curvas no tienen que venir despejadas: `y² = 8x`, `4x² + 9y² = 36`, `(x – 1)² = 20 – 4y` o
`4y = 4 – x²` se despejan en la variable que haga falta, con el procedimiento escrito, y se elige la
rama que cierra la región (o la del primer cuadrante, si el enunciado lo dice). También se lee la
escritura de las listas de ejercicios tal como viene: guiones largos, `x2` por `x²`, intervalos
como "x menor e igual q 1 y x mayor e igual q 0", y ejes como "al rotar y = 0", "haciendo rotar el
eje y" o "con respecto a esa recta".

Cuando se dedujo, se enseña la cuenta: un intervalo presentado sin el razonamiento que lo produjo
no se distingue de uno inventado. Y cada dato viaja con su procedencia —escrito, deducido,
interpretado del lenguaje o faltante—, porque no todos merecen la misma confianza.

### Una frontera no es el eje de giro

Es la distinción que más cambia el resultado. La misma ecuación `x = 2` cierra la región en
*"limitada por la curva y la ordenada 2"* y es el eje de revolución en *"gira alrededor de
x = 2"*. Lo que las separa es la preposición que las introduce, y por eso ninguna recta se lee sin
mirar antes su contexto.

Lo que el enunciado no diga y no se pueda deducir, la aplicación **no lo completa**: lo nombra —el
dato concreto, no "falta información"— y espera. El eje de revolución solo se toma sin estar
escrito en un caso: cuando el enunciado no dice alrededor de qué gira pero nombra un único eje
coordenado como frontera ("la región limitada por y = x – x³ y el eje x"). Entonces se toma ese
eje, marcado como *interpretado* y con un aviso para que se confirme. Si no nombra ninguno, o nombra
los dos, se pide.

## Detalles que resuelve

- **Área contra integral.** Cuando la curva cruza el eje X, la integral definida y el área
  geométrica dejan de coincidir, porque lo que queda debajo del eje cuenta como negativo. La
  aplicación parte la integral en los cruces y muestra los dos valores por separado.
- **Curvas que se cruzan.** En el área entre dos curvas, si cambia cuál va arriba, integrar de
  corrido haría que los tramos se restaran. El programa detecta los cortes y suma cada tramo en
  valor absoluto.
- **Ejes de giro corridos.** Los sólidos se pueden girar alrededor de rectas como `y = 2`, no
  solo de los ejes coordenados; en ese caso el radio pasa a ser `|f(x) - k|`.
- **Volumen en términos de π.** Cuando el resultado es un múltiplo limpio de π, se muestra
  también en esa forma (`8π`, `16π/105`), que es como se deja en clase.
- **Puntos de intersección paso a paso.** Las ecuaciones de los cortes se resuelven como a mano:
  se pasa todo a un lado, se saca factor común, se factoriza (dos números, diferencia de
  cuadrados, fórmula general, teorema de la raíz racional) y se iguala cada factor a cero. Con
  raíces, se eleva al cuadrado y se descartan las soluciones falsas.
- **Función superior en cada tramo.** En el área entre curvas se comprueba con un punto de prueba
  qué curva va arriba en cada tramo, y cada tramo se integra y se evalúa por separado.
- **Raíces en el integrando.** `(√(8x))²` se simplifica a `8x` y `x·√(8x)` se integra como
  `4√2·x^(3/2)`, de modo que esos volúmenes también salen por la regla de Barrow y no por
  aproximación.
- **Gráficas interactivas.** La gráfica plana de las cuatro páginas se maneja como en GeoGebra:
  arrastrar para mover, rueda o pellizco para acercar, botones de vista inicial y pantalla
  completa, y lectura del punto bajo el puntero. Las curvas viajan como árbol ya interpretado por
  el parser de Java, y el navegador las vuelve a evaluar en la zona que se esté mirando.

## Estructura

```
ProyectoFinalCalculo2/
├── pom.xml                     Configuración de Maven
├── docs/                       PDFs de referencia de los tres temas
└── src/
    ├── main/
    │   ├── java/com/calculo2/integrales/
    │   │   ├── AplicacionIntegrales.java   Punto de entrada
    │   │   ├── config/                     Servidor, rutas y CORS
    │   │   ├── controller/                 Rutas HTTP (temas, analizar, calculadora)
    │   │   ├── interprete/                 Lectura de enunciados escritos en palabras
    │   │   ├── model/                      Datos de entrada/salida y enums
    │   │   ├── service/                    Lógica de cada tema y del resolutor
    │   │   ├── math/
    │   │   │   ├── parser/                 Lectura y evaluación de f(x)
    │   │   │   ├── algebra/                Factorizar, resolver ecuaciones y despejar
    │   │   │   ├── simbolico/              Polinomios, antiderivadas y fracciones
    │   │   │   ├── integracion/            Riemann y Simpson (respaldo interno)
    │   │   │   ├── solidos/                Discos, arandelas, capas y malla 3D
    │   │   │   └── util/                   Raíces, validaciones, redondeo
    │   │   ├── exception/                  Errores y su traducción a JSON
    │   │   └── util/                       JSON y constantes
    │   └── resources/
    │       ├── application.properties
    │       └── static/                     Frontend servido por el backend
    │           ├── index.html
    │           ├── pages/                  Un tema por página, más resolver,
    │           │                           calculadora y ayuda
    │           ├── css/                    Variables, layout, componentes, gráficas
    │           ├── js/
    │           │   ├── api/                Llamadas al backend
    │           │   ├── ui/                 Formularios, resultados, pasos
    │           │   ├── graficas/           Gráfica 2D y visor 3D
    │           │   └── util/               Validaciones y formato
    │           └── assets/img/
    └── test/java/com/calculo2/integrales/   Pruebas del parser, métodos y servicios
```

## Pruebas

```bash
mvn test
```

Son 226 pruebas. Además de los casos habituales, verifican los resultados contra valores que se
conocen de antemano: los dos ejercicios de los PDFs del curso (`√x` girando sobre el eje X da
`8π`; la región entre `y = x` y `y = x²` da `2π/15`) y varios sólidos de geometría elemental —
cono, esfera, cilindro, tubo — cuyo volumen se sabe por fórmula. Si alguna fórmula quedara mal
aplicada, esos casos lo delatan de inmediato.

`EnunciadosRealesTest` toma cuatro enunciados copiados tal como vienen en las tareas y comprueba
que no solo salga el número correcto, sino que salga por el camino correcto: que "la ordenada 2"
se lea como una frontera y no como el eje, que los límites se calculen en lugar de pedirse, y que
lo único que se pida sea lo que de verdad no se puede saber.

`EjerciciosDelCursoTest` resuelve de principio a fin los ejercicios de la lista del curso
—parábolas sin despejar, la elipse, curvas `x = g(y)`, ejes corridos, "con respecto a esa
recta"— y compara cada volumen con el del libro, además de trece enunciados de área. `AlgebraTest`
cubre la factorización, las ecuaciones con raíces y los despejes.

## Estado

Terminado y funcionando. Backend, frontend y pruebas completos.
