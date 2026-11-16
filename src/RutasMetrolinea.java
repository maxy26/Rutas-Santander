import java.util.Random;
import java.util.Scanner;

/**
 * Rutas de Metrolínea: programa de consola que calcula la ruta más rápida entre
 * estaciones del área metropolitana de Bucaramanga usando un grafo ponderado y Dijkstra.
 *
 * Ejecución:  java -cp out RutasMetrolinea [archivo de datos]
 */
public class RutasMetrolinea {

    private static final Scanner sc = new Scanner(System.in, "UTF-8");
    private static Grafo g;

    public static void main(String[] args) {
        String archivo = args.length > 0 ? args[0] : "datos/metrolinea.txt";
        try {
            g = Grafo.cargar(archivo);
        } catch (Exception e) {
            System.out.println("No se pudo cargar '" + archivo + "': " + e.getMessage());
            System.out.println("Se inicia con un grafo vacío.");
            g = new Grafo();
        }
        System.out.println("=== RUTAS DE METROLÍNEA ===");
        System.out.println(g.tamano() + " estaciones y " + g.totalTramos() + " tramos cargados.");

        int op;
        do {
            System.out.println();
            System.out.println("1. Ver estaciones");
            System.out.println("2. Ver matriz de adyacencia");
            System.out.println("3. Ruta más rápida entre dos estaciones (Dijkstra)");
            System.out.println("4. Comparar Dijkstra con fuerza bruta (tiempos)");
            System.out.println("5. Agregar estación");
            System.out.println("6. Agregar o cambiar un tramo");
            System.out.println("7. Cerrar un tramo (eliminar)");
            System.out.println("8. Eliminar estación");
            System.out.println("0. Salir");
            op = leerEntero("Opción: ");
            try {
                switch (op) {
                    case 1: listar(); break;
                    case 2: g.imprimirMatriz(); break;
                    case 3: rutaMasRapida(); break;
                    case 4: comparar(); break;
                    case 5: agregarEstacion(); break;
                    case 6: agregarTramo(); break;
                    case 7: cerrarTramo(); break;
                    case 8: eliminarEstacion(); break;
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
        if (g.estaVacio()) { System.out.println("No hay estaciones."); return; }
        System.out.printf("%-4s %-22s %-17s %s%n", "N.º", "Estación", "Troncal", "Conexiones");
        for (int i = 0; i < g.tamano(); i++)
            System.out.printf("%-4d %-22s %-17s %d%n", i, g.nombre(i), g.troncal(i), g.grado(i));
    }

    private static void rutaMasRapida() {
        if (g.tamano() < 2) { System.out.println("Se necesitan al menos dos estaciones."); return; }
        int o = leerEstacion("Estación de origen (número o nombre): ");
        int d = leerEstacion("Estación de destino (número o nombre): ");

        Rutas.Resultado rapida = Rutas.dijkstra(g, o, d);
        if (!rapida.existe()) {
            System.out.println("No hay ninguna ruta entre " + g.nombre(o) + " y " + g.nombre(d) + ".");
            return;
        }
        System.out.println("\nRuta más rápida (Dijkstra):");
        imprimirCamino(rapida.camino);
        System.out.println("Total: " + rapida.minutos + " min, " + (rapida.camino.length - 1) + " tramos.");

        Rutas.Resultado corta = Rutas.menosParadas(g, o, d);
        System.out.println("\nRuta con menos paradas (recorrido en anchura):");
        imprimirCamino(corta.camino);
        System.out.println("Total: " + corta.minutos + " min, " + (corta.camino.length - 1) + " tramos.");
        if (corta.minutos > rapida.minutos)
            System.out.println("-> Menos paradas NO es más rápido: Dijkstra ahorra " + (corta.minutos - rapida.minutos) + " min.");
        else
            System.out.println("-> En este caso ambas rutas tardan lo mismo.");
    }

    private static void imprimirCamino(int[] c) {
        for (int i = 0; i < c.length; i++) {
            if (i == 0) System.out.println("  " + g.nombre(c[i]));
            else System.out.println("  -> " + g.nombre(c[i]) + "  (+" + g.peso(c[i - 1], c[i]) + " min)");
        }
    }

    /** Mide Dijkstra y fuerza bruta en el grafo actual y en cuadrículas cada vez más grandes. */
    private static void comparar() {
        final long LIMITE = 300_000_000L;
        System.out.println("\nCuadrículas de k x k estaciones, ruta de una esquina a la opuesta.");
        System.out.println("Pesos aleatorios de 1 a 9 minutos (semilla fija, siempre los mismos).");
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
            String igual = rf.operaciones < 0 ? "(cortada)" : (rf.minutos == rd.minutos ? "sí" : "NO");
            System.out.printf("%-8s %-10d %-14d %-12.3f %-16s %-12.1f %s%n",
                    k + "x" + k, k * k, rd.operaciones, msD, opsF, msF, igual);
        }
        System.out.println("Dijkstra crece como n² y la fuerza bruta de forma exponencial.");
    }

    /** Grafo de prueba en forma de cuadrícula k x k (cada nodo unido a derecha y abajo). */
    static Grafo cuadricula(int k) {
        Grafo c = new Grafo();
        Random rnd = new Random(42);
        for (int i = 0; i < k * k; i++) c.agregarEstacion("N" + i, "prueba");
        for (int f = 0; f < k; f++)
            for (int col = 0; col < k; col++) {
                int i = f * k + col;
                if (col + 1 < k) c.agregarTramo(i, i + 1, 1 + rnd.nextInt(9));
                if (f + 1 < k) c.agregarTramo(i, i + k, 1 + rnd.nextInt(9));
            }
        return c;
    }

    private static void agregarEstacion() {
        System.out.print("Nombre de la estación: ");
        String nombre = sc.nextLine();
        System.out.print("Troncal: ");
        String troncal = sc.nextLine();
        int i = g.agregarEstacion(nombre, troncal);
        System.out.println("Estación agregada con el número " + i + ". Ahora agréguele tramos (opción 6).");
    }

    private static void agregarTramo() {
        int a = leerEstacion("Estación A: ");
        int b = leerEstacion("Estación B: ");
        int m = leerEntero("Minutos del tramo: ");
        g.agregarTramo(a, b, m);
        System.out.println("Tramo " + g.nombre(a) + " - " + g.nombre(b) + " = " + m + " min.");
    }

    private static void cerrarTramo() {
        int a = leerEstacion("Estación A: ");
        int b = leerEstacion("Estación B: ");
        System.out.println(g.eliminarTramo(a, b) ? "Tramo cerrado." : "Esas estaciones no tenían un tramo directo.");
    }

    private static void eliminarEstacion() {
        int k = leerEstacion("Estación a eliminar: ");
        String nombre = g.nombre(k);
        g.eliminarEstacion(k);
        System.out.println("Se eliminó " + nombre + ". Las estaciones siguientes cambiaron de número.");
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

    /** Acepta el número de la estación o su nombre. */
    private static int leerEstacion(String msg) {
        while (true) {
            System.out.print(msg);
            String s = sc.hasNextLine() ? sc.nextLine().trim() : "";
            int i;
            try { i = Integer.parseInt(s); } catch (NumberFormatException e) { i = g.buscar(s); }
            if (i >= 0 && i < g.tamano()) return i;
            System.out.println("No existe esa estación. Use la opción 1 para ver la lista.");
        }
    }
}
