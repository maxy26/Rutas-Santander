# Rutas por carretera en Santander

Proyecto final de Estructuras de Datos (E193, UTS, 2026B).

Calcula la ruta más corta (km) o la más rápida (tiempo en carro) por carretera entre municipios de Santander (Colombia). Para eso usa un **grafo ponderado no dirigido** implementado con **matriz de adyacencia** y el **algoritmo de Dijkstra**.

## Qué hace

- Dibuja el mapa de 21 municipios y 27 vías, con los km o el tiempo de cada vía.
- Calcula con Dijkstra la ruta más corta o la más rápida, y la compara con la ruta de menos paradas (recorrido en anchura con una cola). No siempre coinciden: de Lebrija a San Vicente de Chucurí lo más corto es ir por Zapatoca (142 km, 5 h 08 min) y lo más rápido por Barrancabermeja (152,9 km, 4 h 01 min).
- Agrega municipios y vías, cierra vías (por ejemplo, por un derrumbe) y elimina municipios.
- Mide Dijkstra contra la fuerza bruta, que prueba todos los caminos, en grafos cada vez más grandes.

## Estructura

| Archivo | Contenido |
|---|---|
| `src/Grafo.java` | La estructura: matriz de adyacencia con sus operaciones de insertar, eliminar, buscar y recorrer |
| `src/Rutas.java` | Dijkstra, ruta con menos paradas, fuerza bruta recursiva, y una cola y una pila propias |
| `src/VentanaRutas.java` | Ventana con el mapa (Swing) |
| `src/RutasSantander.java` | Versión de consola |
| `src/PruebasRutas.java` | Casos normales, límite y de error |
| `datos/santander.txt` | Municipios, provincias, kilómetros y minutos de cada vía |
| `Rutas_Santander_Google_Maps.xlsx` | Datos originales consultados en Google Maps (carro, bus y a pie) |
| `datos/posiciones.txt` | Posición de cada municipio en el mapa |

Las distancias (km) y los tiempos en carro (minutos) son los de la ruta recomendada por **Google Maps**, consultada el 16 de noviembre de 2026. El tiempo incluye el tráfico del momento de la consulta. La red es una simplificación: solo incluye las vías principales entre estos municipios. La posición de cada municipio en el mapa se calculó con su latitud y longitud.

## Complejidad

| Operación | Complejidad |
|---|---|
| Agregar vía | O(1) |
| Eliminar vía | O(1) |
| Buscar municipio por nombre | O(n) |
| Agregar municipio | O(n) |
| Eliminar municipio | O(n²) |
| Dijkstra con matriz | O(n²) |
| Menos paradas (BFS) con matriz | O(n²) |
| Fuerza bruta | Exponencial |

## Cómo ejecutarlo

Requiere Java 17 o superior. En Windows basta con doble clic en `Abrir_Ventana.bat`. Desde la consola, en la carpeta del proyecto:

```
javac -encoding UTF-8 -d out src/*.java
java -cp out VentanaRutas       (ventana con el mapa)
java -cp out RutasSantander     (versión de consola)
java -cp out PruebasRutas       (pruebas)
```

En la ventana:
- Los municipios se arrastran con el ratón.
- Con dos clics sobre municipios se elige el origen y el destino.
- En «Buscar» se elige la ruta más corta (km) o la más rápida (tiempo).
- La ruta de Dijkstra se marca en verde y la de menos paradas en naranja punteado.
- Las vías cerradas se ven en rojo punteado.
