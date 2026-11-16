/**
 * Algoritmos de rutas sobre el grafo de municipios.
 *
 *  - dijkstra:          ruta más corta (menos km) o más rápida (menos minutos).  O(n²) con matriz.
 *  - menosParadas:      ruta con menos municipios, recorrido en anchura.    O(n²) con matriz.
 *  - fuerzaBruta:       prueba TODOS los caminos simples (recursivo).       O(n!) en el peor caso.
 */
public class Rutas {

    /** Resultado de una búsqueda de ruta. */
    public static class Resultado {
        public int[] camino;        // municipios en orden, de origen a destino (vacío si no hay ruta)
        public int km;              // suma de los km del camino
        public int minutos;         // suma de los minutos del camino
        public long operaciones;    // pasos contados, para comparar algoritmos

        public boolean existe() { return camino.length > 0; }
    }

    // ================================================================= DIJKSTRA

    /** Ruta más corta en km. */
    public static Resultado dijkstra(Grafo g, int origen, int destino) {
        return dijkstra(g, origen, destino, false);
    }

    /** Si porTiempo es true, el peso de cada vía son sus minutos; si no, sus km. */
    public static Resultado dijkstra(Grafo g, int origen, int destino, boolean porTiempo) {
        int n = g.tamano();
        int[] dist = new int[n];          // mejor distancia conocida desde el origen
        int[] previo = new int[n];        // municipio anterior en el mejor camino
        boolean[] visitado = new boolean[n];
        long ops = 0;

        for (int i = 0; i < n; i++) {
            dist[i] = Integer.MAX_VALUE;
            previo[i] = -1;
        }
        dist[origen] = 0;

        for (int paso = 0; paso < n; paso++) {
            // 1. Elegir el municipio no visitado con menor distancia
            int u = -1;
            for (int i = 0; i < n; i++) {
                ops++;
                if (!visitado[i] && dist[i] != Integer.MAX_VALUE && (u == -1 || dist[i] < dist[u])) u = i;
            }
            if (u == -1 || u == destino) break;   // no quedan alcanzables, o ya llegamos
            visitado[u] = true;

            // 2. Relajar: ¿pasar por u mejora el camino a cada vecino v?
            for (int v = 0; v < n; v++) {
                ops++;
                int w = porTiempo ? g.minutos(u, v) : g.peso(u, v);
                if (w > 0 && !visitado[v] && dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    previo[v] = u;
                }
            }
        }
        Resultado r = reconstruir(previo, origen, destino, dist[destino] == Integer.MAX_VALUE);
        r.km = sumarKm(g, r.camino);
        r.minutos = sumarMinutos(g, r.camino);
        r.operaciones = ops;
        return r;
    }

    // ================================================================= MENOS PARADAS (BFS)

    public static Resultado menosParadas(Grafo g, int origen, int destino) {
        int n = g.tamano();
        int[] previo = new int[n];
        boolean[] visitado = new boolean[n];
        for (int i = 0; i < n; i++) previo[i] = -1;
        long ops = 0;

        Cola cola = new Cola(n);
        cola.encolar(origen);
        visitado[origen] = true;

        while (!cola.estaVacia()) {
            int u = cola.desencolar();
            if (u == destino) break;
            for (int v = 0; v < n; v++) {
                ops++;
                if (g.peso(u, v) > 0 && !visitado[v]) {
                    visitado[v] = true;
                    previo[v] = u;
                    cola.encolar(v);
                }
            }
        }
        Resultado r = reconstruir(previo, origen, destino, !visitado[destino]);
        r.km = sumarKm(g, r.camino);
        r.minutos = sumarMinutos(g, r.camino);
        r.operaciones = ops;
        return r;
    }

    // ================================================================= FUERZA BRUTA

    private static int[] mejorCamino;
    private static int mejorKm;
    private static long opsFuerza;
    private static long limite;

    /**
     * Explora recursivamente todos los caminos simples y se queda con el más corto.
     * Si se supera el límite de operaciones se detiene y devuelve operaciones = -1.
     */
    public static Resultado fuerzaBruta(Grafo g, int origen, int destino, long limiteOperaciones) {
        int n = g.tamano();
        mejorCamino = new int[0];
        mejorKm = Integer.MAX_VALUE;
        opsFuerza = 0;
        limite = limiteOperaciones;
        int[] camino = new int[n];
        boolean[] enCamino = new boolean[n];
        camino[0] = origen;
        enCamino[origen] = true;
        boolean completo = explorar(g, destino, camino, 1, enCamino, 0);

        Resultado r = new Resultado();
        r.camino = mejorCamino;
        r.km = mejorCamino.length > 0 ? mejorKm : 0;
        r.operaciones = completo ? opsFuerza : -1;
        return r;
    }

    private static boolean explorar(Grafo g, int destino, int[] camino, int largo, boolean[] enCamino, int km) {
        int u = camino[largo - 1];
        if (u == destino) {                                   // caso base: llegamos
            if (km < mejorKm) {
                mejorKm = km;
                mejorCamino = java.util.Arrays.copyOf(camino, largo);
            }
            return true;
        }
        for (int v = 0; v < g.tamano(); v++) {
            if (++opsFuerza > limite) return false;           // se cortó por tiempo
            int w = g.peso(u, v);
            if (w > 0 && !enCamino[v]) {
                camino[largo] = v;
                enCamino[v] = true;
                if (!explorar(g, destino, camino, largo + 1, enCamino, km + w)) return false;
                enCamino[v] = false;                          // vuelta atrás (backtracking)
            }
        }
        return true;
    }

    // ================================================================= auxiliares

    /** Reconstruye el camino desde el destino hacia atrás usando una PILA. */
    private static Resultado reconstruir(int[] previo, int origen, int destino, boolean sinRuta) {
        Resultado r = new Resultado();
        if (sinRuta) {
            r.camino = new int[0];
            return r;
        }
        Pila pila = new Pila(previo.length);
        for (int v = destino; v != -1; v = previo[v]) pila.apilar(v);
        r.camino = new int[pila.tamano()];
        for (int i = 0; i < r.camino.length; i++) r.camino[i] = pila.desapilar();
        return r;
    }

    public static int sumarMinutos(Grafo g, int[] camino) {
        int s = 0;
        for (int i = 0; i + 1 < camino.length; i++) s += g.minutos(camino[i], camino[i + 1]);
        return s;
    }

    /** Convierte minutos a texto: 98 -> "1 h 38 min". */
    public static String tiempo(int min) {
        return min < 60 ? min + " min" : (min / 60) + " h " + String.format("%02d", min % 60) + " min";
    }

    public static int sumarKm(Grafo g, int[] camino) {
        int s = 0;
        for (int i = 0; i + 1 < camino.length; i++) s += g.peso(camino[i], camino[i + 1]);
        return s;
    }

    /** Cola circular sobre arreglo (vista en clase), usada por el recorrido en anchura. */
    static class Cola {
        private final int[] datos;
        private int frente = 0, fin = 0, cantidad = 0;
        Cola(int capacidad) { datos = new int[Math.max(1, capacidad)]; }
        boolean estaVacia() { return cantidad == 0; }
        void encolar(int x) {
            if (cantidad == datos.length) throw new IllegalStateException("Cola llena");
            datos[fin] = x;
            fin = (fin + 1) % datos.length;
            cantidad++;
        }
        int desencolar() {
            if (estaVacia()) throw new IllegalStateException("Cola vacía");
            int x = datos[frente];
            frente = (frente + 1) % datos.length;
            cantidad--;
            return x;
        }
    }

    /** Pila sobre arreglo (vista en clase), usada para invertir el camino. */
    static class Pila {
        private final int[] datos;
        private int tope = 0;
        Pila(int capacidad) { datos = new int[Math.max(1, capacidad)]; }
        void apilar(int x) {
            if (tope == datos.length) throw new IllegalStateException("Pila llena");
            datos[tope++] = x;
        }
        int desapilar() {
            if (tope == 0) throw new IllegalStateException("Pila vacía");
            return datos[--tope];
        }
        int tamano() { return tope; }
    }
}
