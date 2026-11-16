import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Versión gráfica (Java Swing) del programa de rutas.
 * Solo dibuja y recibe órdenes: la estructura y los algoritmos están en Grafo y Rutas.
 *
 * Ejecución:  java -cp out VentanaRutas
 */
public class VentanaRutas extends JFrame {

    private static final String DATOS = "datos/santander.txt";
    private static final String POSICIONES = "datos/posiciones.txt";
    private static final int R = 11;                     // radio de cada municipio

    private Grafo g;
    private final Map<String, Point> pos = new HashMap<>();   // posición de cada municipio por nombre
    private final List<int[]> cerrados = new ArrayList<>();   // vías cerradas: {a, b, km}
    private int[] rutaRapida = new int[0];
    private int[] rutaParadas = new int[0];

    private final JComboBox<String> cbOrigen = new JComboBox<>();
    private final JComboBox<String> cbDestino = new JComboBox<>();
    private final JTextArea txt = new JTextArea();
    private final Mapa mapa = new Mapa();
    private String colocando = null;                     // municipio nuevo esperando un clic en el mapa

    public VentanaRutas() {
        super("Rutas por carretera en Santander - Grafo ponderado y Dijkstra");
        cargarDatos();

        // ---- barra superior
        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        arriba.add(new JLabel("Origen:"));
        arriba.add(cbOrigen);
        arriba.add(new JLabel("Destino:"));
        arriba.add(cbDestino);
        arriba.add(boton("Calcular ruta", e -> calcular()));
        arriba.add(boton("Comparar con fuerza bruta", e -> comparar()));
        arriba.add(boton("Ver matriz", e -> verMatriz()));

        // ---- panel derecho
        txt.setEditable(false);
        txt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txt.setLineWrap(true);
        txt.setWrapStyleWord(true);
        JPanel botones = new JPanel(new GridLayout(0, 1, 4, 4));
        botones.add(boton("Cerrar vía", e -> cerrarVia()));
        botones.add(boton("Reabrir vía cerrada", e -> reabrirVia()));
        botones.add(boton("Agregar o cambiar vía", e -> agregarVia()));
        botones.add(boton("Agregar municipio", e -> agregarMunicipio()));
        botones.add(boton("Eliminar municipio", e -> eliminarMunicipio()));
        botones.add(boton("Restaurar datos originales", e -> { cargarDatos(); limpiarRuta(); info("Datos originales cargados."); }));
        JPanel derecha = new JPanel(new BorderLayout(4, 6));
        derecha.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        derecha.setPreferredSize(new Dimension(320, 0));
        derecha.add(new JScrollPane(txt), BorderLayout.CENTER);
        derecha.add(botones, BorderLayout.SOUTH);

        add(arriba, BorderLayout.NORTH);
        add(mapa, BorderLayout.CENTER);
        add(derecha, BorderLayout.EAST);
        setSize(1180, 860);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        info("Elija origen y destino arriba, o haga clic en dos municipios del mapa.\n\n"
                + "Puede arrastrar los municipios para acomodar el dibujo.\n\n"
                + "Verde: ruta más corta (Dijkstra).\nNaranja punteada: ruta con menos paradas.\nRojo punteado: vía cerrada.");
    }

    // ================================================================ datos

    private void cargarDatos() {
        try {
            g = Grafo.cargar(DATOS);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar " + DATOS + ":\n" + e.getMessage());
            g = new Grafo();
        }
        pos.clear();
        cerrados.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(POSICIONES, StandardCharsets.UTF_8))) {
            String l;
            while ((l = br.readLine()) != null) {
                if (l.isBlank() || l.startsWith("#")) continue;
                String[] p = l.split(";");
                pos.put(p[0].trim(), new Point(Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim())));
            }
        } catch (Exception e) {
            // sin archivo de posiciones: se reparten en círculo más abajo
        }
        for (int i = 0; i < g.tamano(); i++)
            pos.computeIfAbsent(g.nombre(i), k -> new Point(450 + (int) (300 * Math.cos(pos.size())), 380 + (int) (300 * Math.sin(pos.size()))));
        actualizarCombos();
        if (mapa != null) mapa.repaint();
    }

    private void actualizarCombos() {
        Object o = cbOrigen.getSelectedItem(), d = cbDestino.getSelectedItem();
        cbOrigen.removeAllItems();
        cbDestino.removeAllItems();
        for (int i = 0; i < g.tamano(); i++) {
            cbOrigen.addItem(g.nombre(i));
            cbDestino.addItem(g.nombre(i));
        }
        if (o != null) cbOrigen.setSelectedItem(o);
        if (d != null) cbDestino.setSelectedItem(d);
    }

    // ================================================================ acciones

    private void calcular() {
        if (g.tamano() < 2) { info("Se necesitan al menos dos municipios."); return; }
        int o = cbOrigen.getSelectedIndex(), d = cbDestino.getSelectedIndex();
        if (o == d) { limpiarRuta(); info("El origen y el destino son el mismo municipio: 0 km."); return; }

        Rutas.Resultado rapida = Rutas.dijkstra(g, o, d);
        Rutas.Resultado paradas = Rutas.menosParadas(g, o, d);
        rutaRapida = rapida.camino;
        rutaParadas = paradas.camino;
        mapa.repaint();

        if (!rapida.existe()) {
            info("No hay ninguna ruta entre " + g.nombre(o) + " y " + g.nombre(d)
                    + ".\n\nLa red quedó partida: revise las vías cerradas.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("RUTA MÁS CORTA (Dijkstra)\n");
        describir(sb, rapida.camino);
        sb.append("Total: ").append(rapida.km).append(" km, ").append(rapida.camino.length - 1).append(" vías\n\n");
        sb.append("MENOS PARADAS (anchura)\n");
        describir(sb, paradas.camino);
        sb.append("Total: ").append(paradas.km).append(" km, ").append(paradas.camino.length - 1).append(" vías\n\n");
        if (paradas.km > rapida.km)
            sb.append("Menos paradas NO es lo más corto:\nDijkstra ahorra ").append(paradas.km - rapida.km).append(" km.");
        else
            sb.append("En este caso ambas rutas miden lo mismo.");
        info(sb.toString());
    }

    private void describir(StringBuilder sb, int[] c) {
        for (int i = 0; i < c.length; i++) {
            if (i == 0) sb.append("  ").append(g.nombre(c[i])).append('\n');
            else sb.append("  -> ").append(g.nombre(c[i])).append(" (+").append(g.peso(c[i - 1], c[i])).append(")\n");
        }
    }

    private void comparar() {
        info("Midiendo... (tarda unos segundos)");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                final long LIMITE = 300_000_000L;
                StringBuilder sb = new StringBuilder("DIJKSTRA vs FUERZA BRUTA\nCuadrículas k x k, esquina a esquina\n\n");
                sb.append(String.format("%-5s %-8s %-11s%n", "k", "Dijkstra", "F. bruta"));
                for (int k = 3; k <= 7; k++) {
                    Grafo c = RutasSantander.cuadricula(k);
                    long t0 = System.nanoTime();
                    for (int r = 0; r < 100; r++) Rutas.dijkstra(c, 0, k * k - 1);
                    double msD = (System.nanoTime() - t0) / 1e6 / 100;
                    long t1 = System.nanoTime();
                    Rutas.Resultado f = Rutas.fuerzaBruta(c, 0, k * k - 1, LIMITE);
                    double msF = (System.nanoTime() - t1) / 1e6;
                    sb.append(String.format("%-5s %-8s %s%n", k + "x" + k, String.format("%.3f", msD),
                            f.operaciones < 0 ? "cortada" : String.format("%.1f", msF)));
                }
                return sb.append("\n(tiempos en milisegundos)\n\nDijkstra crece como n²;\nla fuerza bruta, de forma exponencial.\n'cortada': superó 300 millones de operaciones.").toString();
            }
            @Override protected void done() {
                try { info(get()); } catch (Exception e) { info("Error: " + e.getMessage()); }
            }
        }.execute();
    }

    private void verMatriz() {
        int n = g.tamano();
        DefaultTableModel m = new DefaultTableModel(n, n + 1);
        String[] cols = new String[n + 1];
        cols[0] = "";
        for (int j = 0; j < n; j++) cols[j + 1] = String.valueOf(j);
        m.setColumnIdentifiers(cols);
        for (int i = 0; i < n; i++) {
            m.setValueAt(i + " " + g.nombre(i), i, 0);
            for (int j = 0; j < n; j++) m.setValueAt(g.peso(i, j) == 0 ? "" : g.peso(i, j), i, j + 1);
        }
        JTable t = new JTable(m);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.getColumnModel().getColumn(0).setPreferredWidth(170);
        for (int j = 1; j <= n; j++) t.getColumnModel().getColumn(j).setPreferredWidth(28);
        JScrollPane sp = new JScrollPane(t);
        sp.setPreferredSize(new Dimension(900, 450));
        JOptionPane.showMessageDialog(this, sp, "Matriz de adyacencia (km; vacío = no hay vía)", JOptionPane.PLAIN_MESSAGE);
    }

    private void cerrarVia() {
        int[] ab = pedirDosMunicipios("Cerrar vía");
        if (ab == null) return;
        int w = g.peso(ab[0], ab[1]);
        if (!g.eliminarVia(ab[0], ab[1])) { info("Esos municipios no tienen una vía directa."); return; }
        cerrados.add(new int[]{ab[0], ab[1], w});
        limpiarRuta();
        info("Vía " + g.nombre(ab[0]) + " - " + g.nombre(ab[1]) + " cerrado.\nVuelva a calcular la ruta para ver el desvío.");
    }

    private void reabrirVia() {
        if (cerrados.isEmpty()) { info("No hay vías cerradas."); return; }
        String[] op = new String[cerrados.size()];
        for (int i = 0; i < op.length; i++) op[i] = g.nombre(cerrados.get(i)[0]) + " - " + g.nombre(cerrados.get(i)[1]);
        Object sel = JOptionPane.showInputDialog(this, "Vía a reabrir:", "Reabrir", JOptionPane.PLAIN_MESSAGE, null, op, op[0]);
        if (sel == null) return;
        int[] t = cerrados.remove(java.util.Arrays.asList(op).indexOf(sel));
        g.agregarVia(t[0], t[1], t[2]);
        limpiarRuta();
        info("Vía " + sel + " reabierto (" + t[2] + " km).");
    }

    private void agregarVia() {
        int[] ab = pedirDosMunicipios("Agregar o cambiar vía");
        if (ab == null) return;
        String s = JOptionPane.showInputDialog(this, "Kilómetros de la vía:");
        if (s == null) return;
        try {
            g.agregarVia(ab[0], ab[1], Integer.parseInt(s.trim()));
            limpiarRuta();
            info("Vía " + g.nombre(ab[0]) + " - " + g.nombre(ab[1]) + " = " + s.trim() + " km.");
        } catch (NumberFormatException e) {
            error("Escriba un número entero de km.");
        } catch (RuntimeException e) {
            error(e.getMessage());
        }
    }

    private void agregarMunicipio() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre del municipio:");
        if (nombre == null) return;
        String provincia = JOptionPane.showInputDialog(this, "Provincia:", "Nueva provincia");
        if (provincia == null) return;
        try {
            g.agregarMunicipio(nombre, provincia);
            colocando = nombre.trim();
            pos.put(colocando, new Point(-100, -100));
            actualizarCombos();
            info("Haga clic en el mapa donde quiere ubicar '" + colocando + "'.\nDespués agréguele vías.");
        } catch (RuntimeException e) {
            error(e.getMessage());
        }
    }

    private void eliminarMunicipio() {
        if (g.estaVacio()) return;
        String[] op = nombres();
        Object sel = JOptionPane.showInputDialog(this, "Municipio a eliminar:", "Eliminar", JOptionPane.PLAIN_MESSAGE, null, op, op[0]);
        if (sel == null) return;
        g.eliminarMunicipio(g.buscar((String) sel));
        cerrados.clear();          // los números cambiaron: se descartan las vías cerradas guardados
        actualizarCombos();
        limpiarRuta();
        info("Se eliminó " + sel + " con todas sus vías.");
    }

    // ================================================================ auxiliares

    private int[] pedirDosMunicipios(String titulo) {
        JComboBox<String> a = new JComboBox<>(nombres()), b = new JComboBox<>(nombres());
        JPanel p = new JPanel(new GridLayout(2, 2, 6, 6));
        p.add(new JLabel("Municipio A:"));
        p.add(a);
        p.add(new JLabel("Municipio B:"));
        p.add(b);
        if (JOptionPane.showConfirmDialog(this, p, titulo, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return null;
        if (a.getSelectedIndex() == b.getSelectedIndex()) { error("Elija dos municipios distintos."); return null; }
        return new int[]{a.getSelectedIndex(), b.getSelectedIndex()};
    }

    private String[] nombres() {
        String[] s = new String[g.tamano()];
        for (int i = 0; i < s.length; i++) s[i] = g.nombre(i);
        return s;
    }

    private void limpiarRuta() {
        rutaRapida = new int[0];
        rutaParadas = new int[0];
        mapa.repaint();
    }

    private void info(String s) { txt.setText(s); txt.setCaretPosition(0); }
    private void error(String s) { JOptionPane.showMessageDialog(this, s, "Error", JOptionPane.ERROR_MESSAGE); }

    private static JButton boton(String texto, java.awt.event.ActionListener a) {
        JButton b = new JButton(texto);
        b.addActionListener(a);
        return b;
    }

    private static Color colorProvincia(String t) {
        switch (t) {
            case "Soto": return new Color(0x1565C0);
            case "Soto Norte": return new Color(0x00838F);
            case "Mares": return new Color(0x6A1B9A);
            case "Guanentá": return new Color(0x2E7D32);
            case "Comunera": return new Color(0xEF6C00);
            default: return Color.DARK_GRAY;
        }
    }

    private static boolean contiene(int[] c, int a, int b) {
        for (int i = 0; i + 1 < c.length; i++)
            if ((c[i] == a && c[i + 1] == b) || (c[i] == b && c[i + 1] == a)) return true;
        return false;
    }

    // ================================================================ dibujo del grafo

    private class Mapa extends JPanel {
        private int arrastrando = -1;
        private int clics = 0;      // para elegir origen y destino con el ratón
        private boolean recienUbicada = false;

        Mapa() {
            setBackground(new Color(0xFAFAF7));
            MouseAdapter m = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (colocando != null) {                       // ubicar municipio nuevo
                        pos.put(colocando, e.getPoint());
                        colocando = null;
                        recienUbicada = true;              // que este clic no cuente como origen
                        repaint();
                        return;
                    }
                    arrastrando = municipioEn(e.getPoint());
                }
                @Override public void mouseDragged(MouseEvent e) {
                    if (arrastrando >= 0) { pos.put(g.nombre(arrastrando), e.getPoint()); repaint(); }
                }
                @Override public void mouseClicked(MouseEvent e) {
                    if (recienUbicada) { recienUbicada = false; return; }
                    int i = municipioEn(e.getPoint());
                    if (i < 0) return;
                    if (clics++ % 2 == 0) { cbOrigen.setSelectedIndex(i); info("Origen: " + g.nombre(i) + "\nAhora haga clic en el destino."); }
                    else { cbDestino.setSelectedIndex(i); calcular(); }
                }
                @Override public void mouseReleased(MouseEvent e) { arrastrando = -1; }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        private int municipioEn(Point p) {
            for (int i = 0; i < g.tamano(); i++)
                if (pos.get(g.nombre(i)).distance(p) <= R + 3) return i;
            return -1;
        }

        @Override protected void paintComponent(Graphics gr) {
            super.paintComponent(gr);
            Graphics2D g2 = (Graphics2D) gr;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int n = g.tamano();

            // vías cerradas (rojo punteado)
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6, 6}, 0));
            g2.setColor(new Color(0xE53935));
            for (int[] t : cerrados) linea(g2, t[0], t[1]);

            // vías normales y sus km
            for (int i = 0; i < n; i++)
                for (int j = i + 1; j < n; j++) {
                    if (g.peso(i, j) == 0) continue;
                    boolean rapida = contiene(rutaRapida, i, j), paradas = contiene(rutaParadas, i, j);
                    if (rapida) { g2.setColor(new Color(0x43A047)); g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); }
                    else { g2.setColor(new Color(0xB0BEC5)); g2.setStroke(new BasicStroke(2.5f)); }
                    linea(g2, i, j);
                    if (paradas) {
                        g2.setColor(new Color(0xFB8C00));
                        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{8, 7}, 0));
                        linea(g2, i, j);
                    }
                    Point a = pos.get(g.nombre(i)), b = pos.get(g.nombre(j));
                    String w = String.valueOf(g.peso(i, j));
                    int mx = (a.x + b.x) / 2, my = (a.y + b.y) / 2;
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                    int tw = g2.getFontMetrics().stringWidth(w);
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(mx - tw / 2 - 3, my - 8, tw + 6, 15, 6, 6);
                    g2.setColor(new Color(0x37474F));
                    g2.drawString(w, mx - tw / 2, my + 4);
                }

            // municipios
            g2.setStroke(new BasicStroke(2f));
            int o = rutaRapida.length > 0 ? rutaRapida[0] : -1, d = rutaRapida.length > 0 ? rutaRapida[rutaRapida.length - 1] : -1;
            for (int i = 0; i < n; i++) {
                Point p = pos.get(g.nombre(i));
                g2.setColor(i == o || i == d ? new Color(0xFFD600) : Color.WHITE);
                g2.fillOval(p.x - R, p.y - R, 2 * R, 2 * R);
                g2.setColor(colorProvincia(g.provincia(i)));
                g2.drawOval(p.x - R, p.y - R, 2 * R, 2 * R);
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
                String num = String.valueOf(i);
                g2.drawString(num, p.x - g2.getFontMetrics().stringWidth(num) / 2, p.y + 4);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                g2.setColor(new Color(0x263238));
                g2.drawString(g.nombre(i), p.x + R + 4, p.y - R + 2);
            }

            // leyenda de provincias
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            String[] tr = {"Soto", "Soto Norte", "Mares", "Guanentá", "Comunera"};
            int y = getHeight() - 18 * tr.length - 8;
            for (String t : tr) {
                g2.setColor(colorProvincia(t));
                g2.drawOval(12, y - 10, 12, 12);
                g2.setColor(new Color(0x37474F));
                g2.drawString("Provincia " + t, 30, y);
                y += 18;
            }
        }

        private void linea(Graphics2D g2, int i, int j) {
            Point a = pos.get(g.nombre(i)), b = pos.get(g.nombre(j));
            g2.draw(new Line2D.Float(a.x, a.y, b.x, b.y));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaRutas().setVisible(true));
    }
}
