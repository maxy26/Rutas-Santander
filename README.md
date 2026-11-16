# Rutas de Metrolínea

Proyecto final de Estructuras de Datos (E193, UTS, 2026B).

Calcula la ruta más rápida entre estaciones de Metrolínea, en el área metropolitana de Bucaramanga. Para eso usa un **grafo ponderado no dirigido** implementado con **matriz de adyacencia** y el **algoritmo de Dijkstra**.

## Qué hace

- Lista las estaciones y muestra la matriz de adyacencia.
- Calcula la ruta más rápida con Dijkstra y la compara con la ruta de menos paradas (recorrido en anchura con una cola).
- Agrega estaciones y tramos, cierra tramos (por ejemplo, por una obra) y elimina estaciones.
- Mide Dijkstra contra la fuerza bruta, que prueba todos los caminos, en grafos cada vez más grandes.

## Estructura

| Archivo | Contenido |
|---|---|
| `src/Grafo.java` | La estructura: matriz de adyacencia con sus operaciones de insertar, eliminar, buscar y recorrer |
| `src/Rutas.java` | Dijkstra, ruta con menos paradas, fuerza bruta recursiva, y una cola y una pila propias |
| `src/VentanaMetrolinea.java` | Ventana con el mapa (Swing) |
| `src/RutasMetrolinea.java` | Menú de consola |
| `src/PruebasRutas.java` | Casos normales, límite y de error |
| `datos/metrolinea.txt` | 22 estaciones y 26 tramos |

Los nombres de las estaciones son reales. Los minutos entre estaciones son **estimados** para el modelo académico, no son datos oficiales.

## Complejidad

| Operación | Complejidad |
|---|---|
| Agregar tramo | O(1) |
| Eliminar tramo | O(1) |
| Buscar estación por nombre | O(n) |
| Agregar estación | O(n) |
| Eliminar estación | O(n²) |
| Dijkstra con matriz | O(n²) |
| Menos paradas (BFS) con matriz | O(n²) |
| Fuerza bruta | Exponencial |

## Cómo ejecutarlo

Requiere Java 17 o superior. Desde la carpeta del proyecto:

```
javac -encoding UTF-8 -d out src/*.java
java -cp out VentanaMetrolinea     (ventana con el mapa)
java -cp out RutasMetrolinea       (versión de consola)
java -cp out PruebasRutas          (pruebas)
```

En Windows también se puede abrir la ventana con doble clic en `Abrir_Ventana.bat`.

En la ventana:
- Las estaciones se arrastran con el ratón.
- Con dos clics sobre estaciones se elige el origen y el destino.
- La ruta más rápida se marca en verde y la de menos paradas en naranja punteado.
- Los tramos cerrados se ven en rojo punteado.

La posición de cada estación en el mapa está en `datos/posiciones.txt`.

En la consola de Windows, ejecuta `chcp 65001` antes para que se vean bien las tildes.
