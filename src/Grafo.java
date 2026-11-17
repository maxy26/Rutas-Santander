import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Grafo ponderado no dirigido implementado con MATRIZ DE ADYACENCIA.
 *
 * - Cada municipio es un nodo (vértice) identificado por su posición 0..n-1.
 * - ady[i][j] guarda los km de la vía entre i y j; 0 significa "no hay vía".
 * - tiempo[i][j] guarda los minutos que se tarda en recorrer esa misma vía.
 * - Como las vías son de doble sentido, las matrices son simétricas: ady[i][j] == ady[j][i].
 */
public class Grafo {

    public static final int MAX = 60;          // capacidad máxima de municipios

    private final String[] nombres = new String[MAX];
    private final String[] provincias = new String[MAX];
    private final double[][] ady = new double[MAX][MAX];      // km (con decimales)
    private final double[][] tiempo = new double[MAX][MAX];   // minutos
    private int n = 0;                          // municipios registrados

    // ------------------------------------------------------------ consultas básicas

    public int tamano() { return n; }
    public boolean estaVacio() { return n == 0; }
    public boolean estaLleno() { return n == MAX; }
    public String nombre(int i) { return nombres[i]; }
    public String provincia(int i) { return provincias[i]; }
    public double peso(int i, int j) { return ady[i][j]; }
    public int minutos(int i, int j) { return (int) tiempo[i][j]; }

    /** Búsqueda secuencial por nombre, sin importar mayúsculas. O(n). Devuelve -1 si no existe. */
    public int buscar(String nombre) {
        for (int i = 0; i < n; i++) {
            if (nombres[i].equalsIgnoreCase(nombre.trim())) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------ insertar

    /** Agrega un municipio al final. O(n) por la búsqueda de repetidos. */
    public int agregarMunicipio(String nombre, String provincia) {
        if (nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("El nombre no puede estar vacío.");
        if (estaLleno()) throw new IllegalStateException("El grafo está lleno (máximo " + MAX + " municipios).");
        if (buscar(nombre) != -1) throw new IllegalArgumentException("El municipio '" + nombre + "' ya existe.");
        nombres[n] = nombre.trim();
        provincias[n] = provincia.trim();
        // La fila y la columna nuevas ya están en 0 (sin vías)
        return n++;
    }

    /** Agrega o actualiza la vía entre dos municipios, con su distancia y su tiempo. O(1). */
    public void agregarVia(int a, int b, double km, int min) {
        validar(a);
        validar(b);
        if (a == b) throw new IllegalArgumentException("Una vía no puede unir un municipio consigo mismo.");
        if (km <= 0) throw new IllegalArgumentException("Los km deben ser mayores que 0.");
        if (min <= 0) throw new IllegalArgumentException("Los minutos deben ser mayores que 0.");
        ady[a][b] = km;
        ady[b][a] = km;   // no dirigido: matriz simétrica
        tiempo[a][b] = min;
        tiempo[b][a] = min;
    }

    // ------------------------------------------------------------ eliminar

    /** Quita la vía entre dos municipios. O(1). */
    public boolean eliminarVia(int a, int b) {
        validar(a);
        validar(b);
        if (ady[a][b] == 0) return false;
        ady[a][b] = 0;
        ady[b][a] = 0;
        tiempo[a][b] = 0;
        tiempo[b][a] = 0;
        return true;
    }

    /**
     * Elimina un municipio y todas sus vías. O(n²):
     * se corren una posición hacia arriba las filas y hacia la izquierda las columnas siguientes.
     */
    public void eliminarMunicipio(int k) {
        validar(k);
        for (int i = k; i < n - 1; i++) {
            nombres[i] = nombres[i + 1];
            provincias[i] = provincias[i + 1];
        }
        for (double[][] m : new double[][][]{ady, tiempo}) {   // se hace igual en las dos matrices
            for (int i = k; i < n - 1; i++)            // filas
                for (int j = 0; j < n; j++) m[i][j] = m[i + 1][j];
            for (int j = k; j < n - 1; j++)            // columnas
                for (int i = 0; i < n; i++) m[i][j] = m[i][j + 1];
            for (int i = 0; i < n; i++) {               // limpiar la última fila y columna
                m[n - 1][i] = 0;
                m[i][n - 1] = 0;
            }
        }
        nombres[n - 1] = null;
        provincias[n - 1] = null;
        n--;
    }

    // ------------------------------------------------------------ recorrer

    /** Grado del nodo = número de vías que llegan al municipio. O(n). */
    public int grado(int i) {
        validar(i);
        int g = 0;
        for (int j = 0; j < n; j++) if (ady[i][j] > 0) g++;
        return g;
    }

    public int totalVias() {
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
            for (int j = 0; j < n; j++) sb.append(ady[i][j] == 0 ? "  ." : String.format("%3.0f", ady[i][j]));
            sb.append('\n');
        }
        System.out.print(sb);
        System.out.println("(los números son km redondeados; '.' = no hay vía)");
    }

    private void validar(int i) {
        if (i < 0 || i >= n) throw new IllegalArgumentException("Municipio inexistente: " + i);
    }

    // ------------------------------------------------------------ carga desde archivo

    /**
     * Lee el archivo de datos. Formato (las líneas con # son comentarios):
     *   M;Nombre del municipio;Provincia
     *   V;Municipio A;Municipio B;km;minutos
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
                    if (p[0].equals("M")) {
                        g.agregarMunicipio(p[1], p[2]);
                    } else if (p[0].equals("V")) {
                        int a = g.buscar(p[1]), b = g.buscar(p[2]);
                        if (a == -1 || b == -1) throw new IllegalArgumentException("municipio no declarado");
                        g.agregarVia(a, b, Double.parseDouble(p[3].trim()), Integer.parseInt(p[4].trim()));
                    }
                } catch (RuntimeException e) {
                    throw new IOException("Error en la línea " + num + " del archivo: " + e.getMessage());
                }
            }
        }
        return g;
    }
}
