import java.util.Random;
import java.util.Scanner;

/**
 * Rutas por carretera en Santander: programa de consola que calcula la ruta más corta entre
 * municipios del área metropolitana de Bucaramanga usando un grafo ponderado y Dijkstra.
 *
 * Ejecución:  java -cp out RutasSantander [archivo de datos]
 */
public class RutasSantander {

    private static final Scanner sc = new Scanner(System.in, "UTF-8");
    private static Grafo g;

    public static void main(String[] args) {
        String archivo = args.length > 0 ? args[0] : "datos/santander.txt";
        try {
            g = Grafo.cargar(archivo);
        } catch (Exception e) {
            System.out.println("No se pudo cargar '" + archivo + "': " + e.getMessage());
            System.out.println("Se inicia con un grafo vacío.");
            g = new Grafo();
        }
        System.out.println("=== RUTAS POR CARRETERA EN SANTANDER ===");
        System.out.println(g.tamano() + " municipios y " + g.totalVias() + " vías cargadas.");

        int op;
        do {
            System.out.println();
            System.out.println("1. Ver municipios");
            System.out.println("2. Ver matriz de adyacencia");
            System.out.println("3. Ruta más corta entre dos municipios (Dijkstra)");
            System.out.println("4. Comparar Dijkstra con fuerza bruta (tiempos)");
            System.out.println("5. Agregar municipio");
            System.out.println("6. Agregar o cambiar una vía");
            System.out.println("7. Cerrar una vía (eliminar)");
            System.out.println("8. Eliminar municipio");
            System.out.println("0. Salir");
            op = leerEntero("Opción: ");
            try {
                switch (op) {
                    case 1: listar(); break;
                    case 2: g.imprimirMatriz(); break;
                    case 3: rutaMasRapida(); break;
                    case 4: comparar(); break;
                    case 5: agregarMunicipio(); break;
                    case 6: agregarVia(); break;
                    case 7: cerrarVia(); break;
                    case 8: eliminarMunicipio(); break;
                    case 0: System.out.println("Hasta luego."); break;
                    default: System.out.println("Opción no válida.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } while (op != 0);
    }

    // ------------------------------------------------------------ opciones

    private static void listar() {
        if (g.estaVacio()) { System.out.println("No hay municipios."); return; }
        System.out.printf("%-4s %-22s %-17s %s%n", "N.º", "Municipio", "Provincia", "Conexiones");
        for (int i = 0; i < g.tamano(); i++)
            System.out.printf("%-4d %-22s %-17s %d%n", i, g.nombre(i), g.provincia(i), g.grado(i));
    }

    private static void rutaMasRapida() {
        if (g.tamano() < 2) { System.out.println("Se necesitan al menos dos municipios."); return; }
        int o = leerMunicipio("Municipio de origen (número o nombre): ");
        int d = leerMunicipio("Municipio de destino (número o nombre): ");
        if (o == d) { System.out.println("El origen y el destino son el mismo municipio: 0 km."); return; }

        Rutas.Resultado rapida = Rutas.dijkstra(g, o, d);
        if (!rapida.existe()) {
            System.out.println("No hay ninguna ruta entre " + g.nombre(o) + " y " + g.nombre(d) + ".");
            return;
        }
        System.out.println("\nRuta más corta (Dijkstra):");
        imprimirCamino(rapida.camino);
        System.out.println("Total: " + rapida.km + " km, " + (rapida.camino.length - 1) + " vías.");

        Rutas.Resultado corta = Rutas.menosParadas(g, o, d);
        System.out.println("\nRuta con menos paradas (recorrido en anchura):");
        imprimirCamino(corta.camino);
        System.out.println("Total: " + corta.km + " km, " + (corta.camino.length - 1) + " vías.");
        if (corta.km > rapida.km)
            System.out.println("-> Menos paradas NO es lo más corto: Dijkstra ahorra " + (corta.km - rapida.km) + " km.");
        else
            System.out.println("-> En este caso ambas rutas miden lo mismo.");
    }

    private static void imprimirCamino(int[] c) {
        for (int i = 0; i < c.length; i++) {
            if (i == 0) System.out.println("  " + g.nombre(c[i]));
            else System.out.println("  -> " + g.nombre(c[i]) + "  (+" + g.peso(c[i - 1], c[i]) + " km)");
        }
    }

    /** Mide Dijkstra y fuerza bruta en el grafo actual y en cuadrículas cada vez más grandes. */
    private static void comparar() {
        final long LIMITE = 300_000_000L;
        System.out.println("\nCuadrículas de k x k municipios, ruta de una esquina a la opuesta.");
        System.out.println("Pesos aleatorios de 1 a 9 km (semilla fija, siempre los mismos).");
        System.out.printf("%-8s %-10s %-14s %-12s %-16s %-12s %s%n",
                "k x k", "Nodos", "Dijkstra ops", "Dijkstra ms", "Fuerza bruta ops", "F. bruta ms", "Mismo resultado");
        for (int k = 3; k <= 7; k++) {
            Grafo c = cuadricula(k);
            int o = 0, d = k * k - 1;

            long t0 = System.nanoTime();
            Rutas.Resultado rd = null;
            for (int r = 0; r < 100; r++) rd = Rutas.dijkstra(c, o, d);   // se repite para medir mejor
            double msD = (System.nanoTime() - t0) / 1e6 / 100;

            long t1 = System.nanoTime();
            Rutas.Resultado rf = Rutas.fuerzaBruta(c, o, d, LIMITE);
            double msF = (System.nanoTime() - t1) / 1e6;

            String opsF = rf.operaciones < 0 ? "> " + LIMITE : String.valueOf(rf.operaciones);
            String igual = rf.operaciones < 0 ? "(cortada)" : (rf.km == rd.km ? "sí" : "NO");
            System.out.printf("%-8s %-10d %-14d %-12.3f %-16s %-12.1f %s%n",
                    k + "x" + k, k * k, rd.operaciones, msD, opsF, msF, igual);
        }
        System.out.println("Dijkstra crece como n² y la fuerza bruta de forma exponencial.");
    }

    /** Grafo de prueba en forma de cuadrícula k x k (cada nodo unido a derecha y abajo). */
    static Grafo cuadricula(int k) {
        Grafo c = new Grafo();
        Random rnd = new Random(42);
        for (int i = 0; i < k * k; i++) c.agregarMunicipio("N" + i, "prueba");
        for (int f = 0; f < k; f++)
            for (int col = 0; col < k; col++) {
                int i = f * k + col;
                if (col + 1 < k) c.agregarVia(i, i + 1, 1 + rnd.nextInt(9));
                if (f + 1 < k) c.agregarVia(i, i + k, 1 + rnd.nextInt(9));
            }
        return c;
    }

    private static void agregarMunicipio() {
        System.out.print("Nombre del municipio: ");
        String nombre = sc.nextLine();
        System.out.print("Provincia: ");
        String provincia = sc.nextLine();
        int i = g.agregarMunicipio(nombre, provincia);
        System.out.println("Municipio agregado con el número " + i + ". Ahora agréguele vías (opción 6).");
    }

    private static void agregarVia() {
        int a = leerMunicipio("Municipio A: ");
        int b = leerMunicipio("Municipio B: ");
        if (a == b) throw new IllegalArgumentException("Una vía no puede unir un municipio consigo mismo.");
        int m = leerEntero("Kilómetros de la vía: ");
        g.agregarVia(a, b, m);
        System.out.println("Vía " + g.nombre(a) + " - " + g.nombre(b) + " = " + m + " km.");
    }

    private static void cerrarVia() {
        int a = leerMunicipio("Municipio A: ");
        int b = leerMunicipio("Municipio B: ");
        System.out.println(g.eliminarVia(a, b) ? "Vía cerrada." : "Esos municipios no tenían una vía directa.");
    }

    private static void eliminarMunicipio() {
        int k = leerMunicipio("Municipio a eliminar: ");
        String nombre = g.nombre(k);
        g.eliminarMunicipio(k);
        System.out.println("Se eliminó " + nombre + ". Los municipios siguientes cambiaron de número.");
    }

    // ------------------------------------------------------------ lectura

    private static int leerEntero(String msg) {
        while (true) {
            System.out.print(msg);
            String s = sc.hasNextLine() ? sc.nextLine().trim() : "0";
            try { return Integer.parseInt(s); }
            catch (NumberFormatException e) { System.out.println("Escriba un número."); }
        }
    }

    /** Aceptal número del municipio o su nombre. */
    private static int leerMunicipio(String msg) {
        while (true) {
            System.out.print(msg);
            String s = sc.hasNextLine() ? sc.nextLine().trim() : "";
            int i;
            try { i = Integer.parseInt(s); } catch (NumberFormatException e) { i = g.buscar(s); }
            if (i >= 0 && i < g.tamano()) return i;
            System.out.println("No existe ese municipio. Use la opción 1 para ver la lista.");
        }
    }
}
