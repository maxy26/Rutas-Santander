/**
 * Pruebas del proyecto: casos normales, límite y de error.
 * Ejecución:  java -cp out PruebasRutas
 */
public class PruebasRutas {

    private static int ok = 0, fallas = 0;

    public static void main(String[] args) throws Exception {
        Grafo g = Grafo.cargar("datos/santander.txt");
        int bga = g.buscar("Bucaramanga"), sanGil = g.buscar("San Gil");
        int piede = g.buscar("Piedecuesta"), giron = g.buscar("Girón");

        System.out.println("--- Casos normales ---");
        verificar("Se cargan 21 municipios y 27 vías", g.tamano() == 21 && g.totalVias() == 27);
        verificar("La matriz es simétrica", simetrica(g));
        Rutas.Resultado r = Rutas.dijkstra(g, bga, sanGil);
        verificar("Hay ruta Bucaramanga -> San Gil (" + r.km + " km)", r.existe());
        verificar("Dijkstra coincide con fuerza bruta", r.km == Rutas.fuerzaBruta(g, bga, sanGil, Long.MAX_VALUE).km);
        verificar("Los km del camino suman el total", Rutas.sumarKm(g, r.camino) == r.km);
        int soc = g.buscar("Socorro"), cha = g.buscar("Charalá");
        Rutas.Resultado corta = Rutas.dijkstra(g, soc, cha, false), rapida = Rutas.dijkstra(g, soc, cha, true);
        verificar("Socorro -> Charalá: lo más corto va por Oiba (" + corta.km + " km)", corta.camino[1] == g.buscar("Oiba"));
        verificar("Socorro -> Charalá: lo más rápido va por San Gil (" + Rutas.tiempo(rapida.minutos) + " < " + Rutas.tiempo(corta.minutos) + ")",
                rapida.camino[1] == g.buscar("San Gil") && rapida.minutos < corta.minutos);
        verificar("Minutos negativos se rechazan", lanza(() -> g.agregarVia(0, 1, 10, -3)));
        Rutas.Resultado rp = Rutas.dijkstra(g, piede, giron);
        Rutas.Resultado bp = Rutas.menosParadas(g, piede, giron);
        verificar("Piedecuesta -> Girón: menos paradas pasa por Bucaramanga", bp.camino.length == 3 && bp.camino[1] == bga);
        verificar("Piedecuesta -> Girón: Dijkstra va por Floridablanca (" + rp.km + " < " + bp.km + " km)",
                rp.km < bp.km && rp.camino[1] == g.buscar("Floridablanca"));

        System.out.println("--- Casos límite ---");
        Rutas.Resultado mismo = Rutas.dijkstra(g, bga, bga);
        verificar("Origen igual a destino: 0 km y un solo municipio", mismo.km == 0 && mismo.camino.length == 1);
        Grafo vacio = new Grafo();
        verificar("Grafo vacío", vacio.estaVacio() && vacio.totalVias() == 0);
        Grafo lleno = new Grafo();
        for (int i = 0; i < Grafo.MAX; i++) lleno.agregarMunicipio("M" + i, "x");
        verificar("Grafo lleno rechaza otro municipio", lanza(() -> lleno.agregarMunicipio("Otro", "x")));
        Grafo copia = Grafo.cargar("datos/santander.txt");
        int cB = copia.buscar("Bucaramanga"), cG = copia.buscar("Girón"), cBarr = copia.buscar("Barrancabermeja");
        copia.eliminarVia(cG, copia.buscar("Lebrija"));
        Rutas.Resultado desvio = Rutas.dijkstra(copia, cB, cBarr);
        verificar("Con Girón-Lebrija cerrada, Dijkstra desvía por Zapatoca (" + desvio.km + " km)", desvio.existe()
                && contiene(desvio.camino, copia.buscar("Zapatoca")));
        copia.eliminarVia(cG, copia.buscar("Zapatoca"));
        copia.eliminarVia(cB, copia.buscar("Rionegro"));
        verificar("Cerrando también Girón-Zapatoca y Bucaramanga-Rionegro la red queda partida",
                !Rutas.dijkstra(copia, cB, cBarr).existe());
        copia.eliminarMunicipio(copia.buscar("Curití"));
        verificar("Eliminar un municipio conserva las demás vías", copia.tamano() == 20
                && copia.peso(copia.buscar("Aratoca"), copia.buscar("San Gil")) == g.peso(g.buscar("Aratoca"), sanGil));
        int solo = copia.agregarMunicipio("Municipio aislado", "x");
        verificar("Municipio sin vías: no hay ruta", !Rutas.dijkstra(copia, solo, 0).existe());

        System.out.println("--- Casos de error ---");
        verificar("Municipio repetido se rechaza (sin importar mayúsculas)", lanza(() -> g.agregarMunicipio("bucaramanga", "x")));
        verificar("Nombre vacío se rechaza", lanza(() -> g.agregarMunicipio("  ", "x")));
        verificar("Kilómetros negativos se rechazan", lanza(() -> g.agregarVia(0, 1, -5, 10)));
        verificar("Vía de un municipio consigo mismo se rechaza", lanza(() -> g.agregarVia(2, 2, 3, 3)));
        verificar("Municipio inexistente se rechaza", lanza(() -> g.grado(99)));
        verificar("Buscar un nombre que no existe devuelve -1", g.buscar("Municipio Marte") == -1);

        System.out.println("\nResultado: " + ok + " correctas, " + fallas + " fallidas.");
        if (fallas > 0) System.exit(1);
    }

    private static boolean contiene(int[] c, int x) {
        for (int v : c) if (v == x) return true;
        return false;
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
