/**
 * Pruebas del proyecto: casos normales, límite y de error.
 * Ejecución:  java -cp out PruebasRutas
 */
public class PruebasRutas {

    private static int ok = 0, fallas = 0;

    public static void main(String[] args) throws Exception {
        Grafo g = Grafo.cargar("datos/metrolinea.txt");
        int portal = g.buscar("Portal Floridablanca"), quebrada = g.buscar("Quebradaseca");
        int provenza = g.buscar("Provenza"), isla = g.buscar("La Isla");

        System.out.println("--- Casos normales ---");
        verificar("Se cargan 22 estaciones", g.tamano() == 22);
        verificar("La matriz es simétrica", simetrica(g));
        Rutas.Resultado r = Rutas.dijkstra(g, portal, quebrada);
        verificar("Hay ruta Portal Floridablanca -> Quebradaseca", r.existe());
        verificar("Dijkstra coincide con fuerza bruta", r.minutos == Rutas.fuerzaBruta(g, portal, quebrada, Long.MAX_VALUE).minutos);
        verificar("Los minutos del camino suman el total", Rutas.sumarMinutos(g, r.camino) == r.minutos);
        Rutas.Resultado rp = Rutas.dijkstra(g, provenza, isla);
        Rutas.Resultado bp = Rutas.menosParadas(g, provenza, isla);
        verificar("Provenza -> La Isla: menos paradas usa el tramo directo (1 tramo)", bp.camino.length == 2);
        verificar("Provenza -> La Isla: Dijkstra evita el trancón (" + rp.minutos + " < " + bp.minutos + " min)", rp.minutos < bp.minutos);

        System.out.println("--- Casos límite ---");
        Rutas.Resultado mismo = Rutas.dijkstra(g, provenza, provenza);
        verificar("Origen igual a destino: 0 min y una sola estación", mismo.minutos == 0 && mismo.camino.length == 1);
        Grafo vacio = new Grafo();
        verificar("Grafo vacío", vacio.estaVacio() && vacio.totalTramos() == 0);
        Grafo lleno = new Grafo();
        for (int i = 0; i < Grafo.MAX; i++) lleno.agregarEstacion("E" + i, "x");
        verificar("Grafo lleno rechaza otra estación", lanza(() -> lleno.agregarEstacion("Otra", "x")));
        Grafo copia = Grafo.cargar("datos/metrolinea.txt");
        copia.eliminarTramo(copia.buscar("Provenza"), copia.buscar("Diamante"));
        Rutas.Resultado desvio = Rutas.dijkstra(copia, copia.buscar("Provenza"), copia.buscar("Quebradaseca"));
        verificar("Con Provenza-Diamante cerrado, Dijkstra usa el desvío por La Isla", desvio.existe()
                && desvio.camino[1] == copia.buscar("La Isla"));
        copia.eliminarTramo(copia.buscar("Provenza"), copia.buscar("La Isla"));
        verificar("Cerrando también Provenza-La Isla la red queda partida: no hay ruta al norte",
                !Rutas.dijkstra(copia, copia.buscar("Provenza"), copia.buscar("Quebradaseca")).existe());
        int telebu = copia.buscar("Telebucaramanga");
        copia.eliminarEstacion(copia.buscar("Villabel"));
        verificar("Eliminar estación conserva los demás tramos", copia.peso(copia.buscar("Telebucaramanga"), copia.buscar("Provenza")) == 9
                && copia.tamano() == 21 && telebu >= 0);
        int sola = copia.agregarEstacion("Estación aislada", "x");
        verificar("Estación sin tramos: no hay ruta", !Rutas.dijkstra(copia, sola, 0).existe());

        System.out.println("--- Casos de error ---");
        verificar("Estación repetida se rechaza", lanza(() -> g.agregarEstacion("provenza", "x")));
        verificar("Nombre vacío se rechaza", lanza(() -> g.agregarEstacion("  ", "x")));
        verificar("Minutos negativos se rechazan", lanza(() -> g.agregarTramo(0, 1, -5)));
        verificar("Tramo de una estación consigo misma se rechaza", lanza(() -> g.agregarTramo(2, 2, 3)));
        verificar("Estación inexistente se rechaza", lanza(() -> g.grado(99)));
        verificar("Buscar un nombre que no existe devuelve -1", g.buscar("Estación Marte") == -1);

        System.out.println("\nResultado: " + ok + " correctas, " + fallas + " fallidas.");
        if (fallas > 0) System.exit(1);
    }

    private static boolean simetrica(Grafo g) {
        for (int i = 0; i < g.tamano(); i++)
            for (int j = 0; j < g.tamano(); j++) if (g.peso(i, j) != g.peso(j, i)) return false;
        return true;
    }

    private static boolean lanza(Runnable r) {
        try { r.run(); return false; } catch (RuntimeException e) { return true; }
    }

    private static void verificar(String caso, boolean cumple) {
        System.out.println((cumple ? "[OK]    " : "[FALLA] ") + caso);
        if (cumple) ok++; else fallas++;
    }
}
