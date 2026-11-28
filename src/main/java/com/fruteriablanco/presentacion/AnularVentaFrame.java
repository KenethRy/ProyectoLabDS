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

/** ANU-01: pantalla para anular ventas registradas por error (solo Administrador). */
public class AnularVentaFrame extends JFrame {

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
    private final JTextField txtMotivo = new JTextField(34);

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

    public AnularVentaFrame() {
        super("Anular venta - Frutería Blanco");
        construirInterfaz();
        buscar();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(860, 700);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        JPanel raiz = new JPanel(new BorderLayout(0, 10));
        raiz.setBackground(FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        // ---------- Filtros ----------
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filtros.setOpaque(false);
        filtros.add(etiqueta("Desde:"));
        filtros.add(txtDesde);
        filtros.add(etiqueta("Hasta:"));
        filtros.add(txtHasta);
        JLabel formato = new JLabel("(dd/mm/aaaa)");
        formato.setForeground(GRIS);
        filtros.add(formato);
        filtros.add(etiqueta("Folio o # de venta:"));
        filtros.add(txtIdentificador);

        JButton btnBuscar = new JButton("Buscar");
        estilizar(btnBuscar, VERDE, Color.WHITE);
        btnBuscar.addActionListener(e -> buscar());
        JButton btnLimpiar = new JButton("Limpiar");
        estilizar(btnLimpiar, new Color(230, 230, 230), new Color(60, 60, 60));
        btnLimpiar.addActionListener(e -> limpiar());
        filtros.add(btnBuscar);
        filtros.add(btnLimpiar);
        filtros.add(Navegacion.botonVolver(this));

        for (JTextField campo : new JTextField[]{txtDesde, txtHasta, txtIdentificador}) {
            campo.addActionListener(e -> buscar());
        }

        // ---------- Tablas ----------
        tablaVentas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaVentas.setRowHeight(24);
        tablaVentas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarDetalle();
            }
        });
        JScrollPane scrollVentas = new JScrollPane(tablaVentas);
        scrollVentas.setBorder(BorderFactory.createTitledBorder("Ventas"));
        scrollVentas.setOpaque(false);

        tablaDetalle.setRowHeight(24);
        JScrollPane scrollDetalle = new JScrollPane(tablaDetalle);
        scrollDetalle.setBorder(BorderFactory.createTitledBorder("Productos que regresarán al inventario"));
        scrollDetalle.setOpaque(false);

        JPanel centro = new JPanel(new GridLayout(2, 1, 0, 10));
        centro.setOpaque(false);
        centro.add(scrollVentas);
        centro.add(scrollDetalle);

        // ---------- Motivo y boton de anular ----------
        JPanel anulacion = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        anulacion.setOpaque(false);
        anulacion.add(etiqueta("Motivo de la anulación *"));
        anulacion.add(txtMotivo);
        JButton btnAnular = new JButton("Anular venta");
        estilizar(btnAnular, ROJO, Color.WHITE);
        btnAnular.addActionListener(e -> anular());
        anulacion.add(btnAnular);
        txtMotivo.addActionListener(e -> anular());

        lblMensaje.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel sur = new JPanel(new GridLayout(2, 1));
        sur.setOpaque(false);
        sur.add(anulacion);
        sur.add(lblMensaje);

        raiz.add(filtros, BorderLayout.NORTH);
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(sur, BorderLayout.SOUTH);
        setContentPane(raiz);
    }

    // ---------------------- Acciones ----------------------

    private void buscar() {
        try {
            ventasMostradas = servicio.consultar(
                    txtDesde.getText(), txtHasta.getText(), txtIdentificador.getText(), null);

            modeloVentas.setRowCount(0);
            modeloDetalle.setRowCount(0);
            for (VentaResumen v : ventasMostradas) {
                modeloVentas.addRow(new Object[]{
                        v.getFolio(), v.getFecha().format(FECHA_HORA), v.getVendedor(),
                        dinero(v.getTotal()), v.getEstado()});
            }

            if (ventasMostradas.isEmpty()) {
                mostrarMensaje("No se encontraron ventas con esos datos.", ROJO);
            } else {
                mostrarMensaje("Selecciona la venta, escribe el motivo y presiona \"Anular venta\".", GRIS);
                tablaVentas.setRowSelectionInterval(0, 0);
            }
        } catch (VentaException e) {
            mostrarMensaje(e.getMessage(), ROJO);
        }
    }

    private void mostrarDetalle() {
        modeloDetalle.setRowCount(0);
        VentaResumen v = ventaSeleccionada();
        if (v == null) {
            return;
        }
        try {
            for (DetalleVenta d : servicio.detalleDe(v.getIdVenta())) {
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

    private void anular() {
        VentaResumen venta = ventaSeleccionada();
        if (venta == null) {
            mostrarMensaje("Selecciona la venta que quieres anular.", ROJO);
            return;
        }
        // Criterio: una venta ya anulada no se puede anular otra vez
        if ("CANCELADA".equals(venta.getEstado())) {
            mostrarMensaje("La venta ya fue anulada", ROJO);
            return;
        }
        // Criterio: el motivo es obligatorio
        String motivo = txtMotivo.getText().trim();
        if (motivo.isEmpty()) {
            mostrarMensaje("Ingresa el motivo de la anulación.", ROJO);
            txtMotivo.requestFocusInWindow();
            return;
        }
        // Criterio: se pide confirmacion antes de anular
        int respuesta = JOptionPane.showConfirmDialog(this,
                "Vas a anular la venta " + venta.getFolio() + " por " + dinero(venta.getTotal()) + ".\n"
                        + "El stock de sus productos regresará al inventario.\n"
                        + "Motivo: " + motivo + "\n\n¿Deseas continuar?",
                "Confirmar anulación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            servicio.anular(venta.getIdVenta(), motivo);
            txtMotivo.setText("");
            buscar();
            mostrarMensaje("Venta " + venta.getFolio() + " anulada. El stock fue regresado al inventario.", VERDE);
        } catch (VentaException e) {
            buscar(); // por si otra persona ya la anulo: se ve el estado actualizado
            mostrarMensaje(e.getMessage(), ROJO);
        }
    }

    private void limpiar() {
        txtDesde.setText("");
        txtHasta.setText("");
        txtIdentificador.setText("");
        buscar();
    }

    private VentaResumen ventaSeleccionada() {
        int fila = tablaVentas.getSelectedRow();
        return fila < 0 || fila >= ventasMostradas.size() ? null : ventasMostradas.get(fila);
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
