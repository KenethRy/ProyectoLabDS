
package com.fruteriablanco.presentacion;

import com.fruteriablanco.modelo.DetalleVenta;
import com.fruteriablanco.modelo.VentaResumen;
import com.fruteriablanco.servicio.VentaException;
import com.fruteriablanco.servicio.VentaService;

import com.fruteriablanco.util.Navegacion;

import javax.swing.*;
        import javax.swing.table.DefaultTableModel;
import java.awt.*;
        import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Pantalla para consultar ventas registradas (BUS-02). */
public class ConsultaVentasFrame extends JFrame {

    private static final Color VERDE = new Color(46, 125, 82);
    private static final Color FONDO = new Color(247, 250, 246);
    private static final Color ROJO = new Color(180, 30, 30);
    private static final Color GRIS = new Color(85, 95, 85);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VentaService servicio = new VentaService();
    private List<VentaResumen> ventasMostradas = new ArrayList<>();

    private final JTextField txtDesde = new JTextField(9);
    private final JTextField txtHasta = new JTextField(9);
    private final JTextField txtIdentificador = new JTextField(14);
    private final JTextField txtProducto = new JTextField(14);

    private final DefaultTableModel modeloVentas = new DefaultTableModel(
            new String[]{"Folio", "Fecha", "Vendedor", "Total", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaVentas = new JTable(modeloVentas);

    private final DefaultTableModel modeloDetalle = new DefaultTableModel(
            new String[]{"Producto", "Cantidad", "Precio", "Subtotal"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaDetalle = new JTable(modeloDetalle);

    private final JLabel lblMensaje = new JLabel(" ");

    public ConsultaVentasFrame() {
        super("Consultar ventas - Frutería Blanco");
        construirInterfaz();
        buscar(); // al abrir muestra las ventas mas recientes
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(860, 660);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        JPanel raiz = new JPanel(new BorderLayout(0, 10));
        raiz.setBackground(FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        // ---------- Filtros ----------
        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        fila1.setOpaque(false);
        fila1.add(etiqueta("Desde:"));
        fila1.add(txtDesde);
        fila1.add(etiqueta("Hasta:"));
        fila1.add(txtHasta);
        JLabel formato = new JLabel("(dd/mm/aaaa)");
        formato.setForeground(GRIS);
        fila1.add(formato);
        fila1.add(etiqueta("Folio o # de venta:"));
        fila1.add(txtIdentificador);

        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        fila2.setOpaque(false);
        fila2.add(etiqueta("Producto (nombre o código):"));
        fila2.add(txtProducto);

        JButton btnBuscar = new JButton("Buscar");
        estilizar(btnBuscar, VERDE, Color.WHITE);
        btnBuscar.addActionListener(e -> buscar());
        JButton btnLimpiar = new JButton("Limpiar");
        estilizar(btnLimpiar, new Color(230, 230, 230), new Color(60, 60, 60));
        btnLimpiar.addActionListener(e -> limpiar());
        fila2.add(btnBuscar);
        fila2.add(btnLimpiar);
        fila2.add(Navegacion.botonVolver(this));

        // Enter en cualquier campo tambien busca
        for (JTextField campo : new JTextField[]{txtDesde, txtHasta, txtIdentificador, txtProducto}) {
            campo.addActionListener(e -> buscar());
        }

        JPanel filtros = new JPanel(new GridLayout(2, 1));
        filtros.setOpaque(false);
        filtros.add(fila1);
        filtros.add(fila2);

        // ---------- Tablas ----------
        tablaVentas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaVentas.setRowHeight(24);
        tablaVentas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarDetalle();
            }
        });
        JScrollPane scrollVentas = new JScrollPane(tablaVentas);
        scrollVentas.setBorder(BorderFactory.createTitledBorder("Ventas encontradas"));
        scrollVentas.setOpaque(false);

        tablaDetalle.setRowHeight(24);
        JScrollPane scrollDetalle = new JScrollPane(tablaDetalle);
        scrollDetalle.setBorder(BorderFactory.createTitledBorder("Productos de la venta seleccionada"));
        scrollDetalle.setOpaque(false);

        JPanel centro = new JPanel(new GridLayout(2, 1, 0, 10));
        centro.setOpaque(false);
        centro.add(scrollVentas);
        centro.add(scrollDetalle);

        lblMensaje.setFont(new Font("SansSerif", Font.PLAIN, 13));

        raiz.add(filtros, BorderLayout.NORTH);
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(lblMensaje, BorderLayout.SOUTH);
        setContentPane(raiz);
    }

    // ---------------------- Acciones ----------------------

    private void buscar() {
        try {
            ventasMostradas = servicio.consultar(
                    txtDesde.getText(), txtHasta.getText(),
                    txtIdentificador.getText(), txtProducto.getText());

            modeloVentas.setRowCount(0);
            modeloDetalle.setRowCount(0);
            for (VentaResumen v : ventasMostradas) {
                modeloVentas.addRow(new Object[]{
                        v.getFolio(), v.getFecha().format(FECHA_HORA), v.getVendedor(),
                        dinero(v.getTotal()), v.getEstado()});
            }

            // Criterio: se informa cuando no existen resultados
            if (ventasMostradas.isEmpty()) {
                mostrarMensaje("No se encontraron ventas con esos datos.", ROJO);
            } else {
                String plural = ventasMostradas.size() == 1 ? "venta" : "ventas";
                mostrarMensaje("Se encontraron " + ventasMostradas.size() + " " + plural
                        + ". Selecciona una para ver sus productos.", GRIS);
                tablaVentas.setRowSelectionInterval(0, 0);
            }
        } catch (VentaException e) {
            mostrarMensaje(e.getMessage(), ROJO);
        }
    }

    private void mostrarDetalle() {
        modeloDetalle.setRowCount(0);
        int fila = tablaVentas.getSelectedRow();
        if (fila < 0 || fila >= ventasMostradas.size()) {
            return;
        }
        try {
            for (DetalleVenta d : servicio.detalleDe(ventasMostradas.get(fila).getIdVenta())) {
                modeloDetalle.addRow(new Object[]{
                        d.getNombreProducto(),
                        d.getCantidad().stripTrailingZeros().toPlainString(),
                        dinero(d.getPrecioUnitario()),
                        dinero(d.getSubtotal())});
            }
        } catch (VentaException e) {
            mostrarMensaje(e.getMessage(), ROJO);
        }
    }

    private void limpiar() {
        txtDesde.setText("");
        txtHasta.setText("");
        txtIdentificador.setText("");
        txtProducto.setText("");
        buscar();
    }

    // ---------------------- Utilidades ----------------------

    private void mostrarMensaje(String texto, Color color) {
        lblMensaje.setForeground(color);
        lblMensaje.setText(texto);
    }

    private JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        return l;
    }

    private String dinero(BigDecimal valor) {
        return String.format("$%.2f", valor);
    }

    /** Mismo estilo de botones que el resto de la aplicacion. */
    private void estilizar(JButton boton, Color fondo, Color letra) {
        boton.setContentAreaFilled(false);
        boton.setOpaque(true);
        boton.setBorderPainted(false);
        boton.setBackground(fondo);
        boton.setForeground(letra);
        boton.setFont(new Font("SansSerif", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setMargin(new Insets(6, 14, 6, 14));
    }
}