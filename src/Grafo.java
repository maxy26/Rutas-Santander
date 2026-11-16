import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Grafo ponderado no dirigido implementado con MATRIZ DE ADYACENCIA.
 *
 * - Cada estación es un nodo (vértice) identificado por su posición 0..n-1.
 * - ady[i][j] guarda los minutos del tramo entre i y j; 0 significa "no hay tramo".
 * - Como el bus va en ambos sentidos, la matriz es simétrica: ady[i][j] == ady[j][i].
 */
public class Grafo {

    public static final int MAX = 60;          // capacidad máxima de estaciones

    private final String[] nombres = new String[MAX];
    private final String[] troncales = new String[MAX];
    private final int[][] ady = new int[MAX][MAX];
    private int n = 0;                          // estaciones registradas

    // ------------------------------------------------------------ consultas básicas

    public int tamano() { return n; }
    public boolean estaVacio() { return n == 0; }
    public boolean estaLleno() { return n == MAX; }
    public String nombre(int i) { return nombres[i]; }
    public String troncal(int i) { return troncales[i]; }
    public int peso(int i, int j) { return ady[i][j]; }

    /** Búsqueda secuencial por nombre, sin importar mayúsculas. O(n). Devuelve -1 si no existe. */
    public int buscar(String nombre) {
        for (int i = 0; i < n; i++) {
            if (nombres[i].equalsIgnoreCase(nombre.trim())) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------ insertar

    /** Agrega una estación al final. O(n) por la búsqueda de repetidos. */
    public int agregarEstacion(String nombre, String troncal) {
        if (nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("El nombre no puede estar vacío.");
        if (estaLleno()) throw new IllegalStateException("El grafo está lleno (máximo " + MAX + " estaciones).");
        if (buscar(nombre) != -1) throw new IllegalArgumentException("La estación '" + nombre + "' ya existe.");
        nombres[n] = nombre.trim();
        troncales[n] = troncal.trim();
        // La fila y la columna nuevas ya están en 0 (sin tramos)
        return n++;
    }

    /** Agrega o actualiza el tramo entre dos estaciones. O(1). */
    public void agregarTramo(int a, int b, int minutos) {
        validar(a);
        validar(b);
        if (a == b) throw new IllegalArgumentException("Un tramo no puede unir una estación consigo misma.");
        if (minutos <= 0) throw new IllegalArgumentException("Los minutos deben ser mayores que 0.");
        ady[a][b] = minutos;
        ady[b][a] = minutos;   // no dirigido: matriz simétrica
    }

    // ------------------------------------------------------------ eliminar

    /** Quita el tramo entre dos estaciones. O(1). */
    public boolean eliminarTramo(int a, int b) {
        validar(a);
        validar(b);
        if (ady[a][b] == 0) return false;
        ady[a][b] = 0;
        ady[b][a] = 0;
        return true;
    }

    /**
     * Elimina una estación y todos sus tramos. O(n²):
     * se corren una posición hacia arriba las filas y hacia la izquierda las columnas siguientes.
     */
    public void eliminarEstacion(int k) {
        validar(k);
        for (int i = k; i < n - 1; i++) {
            nombres[i] = nombres[i + 1];
            troncales[i] = troncales[i + 1];
        }
        for (int i = k; i < n - 1; i++)            // filas
            for (int j = 0; j < n; j++) ady[i][j] = ady[i + 1][j];
        for (int j = k; j < n - 1; j++)            // columnas
            for (int i = 0; i < n; i++) ady[i][j] = ady[i][j + 1];
        for (int i = 0; i < n; i++) {               // limpiar la última fila y columna
            ady[n - 1][i] = 0;
            ady[i][n - 1] = 0;
        }
        nombres[n - 1] = null;
        troncales[n - 1] = null;
        n--;
    }

    // ------------------------------------------------------------ recorrer

    /** Grado del nodo = número de tramos que llegan a la estación. O(n). */
    public int grado(int i) {
        validar(i);
        int g = 0;
        for (int j = 0; j < n; j++) if (ady[i][j] > 0) g++;
        return g;
    }

    public int totalTramos() {
        int t = 0;
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++) if (ady[i][j] > 0) t++;
        return t;
    }

    /** Imprime la matriz de adyacencia (fila = origen, columna = destino). */
    public void imprimirMatriz() {
        StringBuilder sb = new StringBuilder("     ");
        for (int j = 0; j < n; j++) sb.append(String.format("%3d", j));
        sb.append('\n');
        for (int i = 0; i < n; i++) {
            sb.append(String.format("%3d |", i));
            for (int j = 0; j < n; j++) sb.append(ady[i][j] == 0 ? "  ." : String.format("%3d", ady[i][j]));
            sb.append('\n');
        }
        System.out.print(sb);
        System.out.println("(los números son minutos; '.' = no hay tramo)");
    }

    private void validar(int i) {
        if (i < 0 || i >= n) throw new IllegalArgumentException("Estación inexistente: " + i);
    }

    // ------------------------------------------------------------ carga desde archivo

    /**
     * Lee el archivo de datos. Formato (las líneas con # son comentarios):
     *   E;Nombre de la estación;Troncal
     *   T;Estación A;Estación B;minutos
     */
    public static Grafo cargar(String ruta) throws IOException {
        Grafo g = new Grafo();
        try (BufferedReader br = new BufferedReader(new FileReader(ruta, java.nio.charset.StandardCharsets.UTF_8))) {
            String linea;
            int num = 0;
            while ((linea = br.readLine()) != null) {
                num++;
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                String[] p = linea.split(";");
                try {
                    if (p[0].equals("E")) {
                        g.agregarEstacion(p[1], p[2]);
                    } else if (p[0].equals("T")) {
                        int a = g.buscar(p[1]), b = g.buscar(p[2]);
                        if (a == -1 || b == -1) throw new IllegalArgumentException("estación no declarada");
                        g.agregarTramo(a, b, Integer.parseInt(p[3].trim()));
                    }
                } catch (RuntimeException e) {
                    throw new IOException("Error en la línea " + num + " del archivo: " + e.getMessage());
                }
            }
        }
        return g;
    }
}
