
package com.fruteriablanco.presentacion;

import com.fruteriablanco.modelo.DetalleVenta;
import com.fruteriablanco.modelo.Producto;
import com.fruteriablanco.modelo.Venta;
import com.fruteriablanco.servicio.VentaException;
import com.fruteriablanco.servicio.VentaService;

import javax.swing.*;
        import javax.swing.table.DefaultTableModel;
import java.awt.*;
        import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Pantalla para registrar ventas (VEN-01). */
public class VentaFrame extends JFrame {

    private static final Color VERDE = new Color(46, 125, 82);
    private static final Color FONDO = new Color(247, 250, 246);

    private final VentaService servicio = new VentaService();
    private Venta venta = new Venta();
    private List<Producto> productosMostrados = new ArrayList<>();

    private final JTextField txtBuscar = new JTextField(18);
    private final JTextField txtCantidad = new JTextField(6);

    private final DefaultTableModel modeloProductos = new DefaultTableModel(
            new String[]{"Código", "Producto", "Unidad", "Precio", "Stock"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaProductos = new JTable(modeloProductos);

    private final DefaultTableModel modeloCarrito = new DefaultTableModel(
            new String[]{"Producto", "Cantidad", "Precio", "Subtotal"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaCarrito = new JTable(modeloCarrito);

    private final JLabel lblSubtotal = new JLabel("Subtotal: $0.00");
    private final JLabel lblTotal = new JLabel("Total: $0.00");
    private final JLabel lblMensaje = new JLabel(" ");

    public VentaFrame() {
        super("Registrar venta - Frutería Blanco");
        construirInterfaz();
        cargarProductos();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(820, 650);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        JPanel raiz = new JPanel(new BorderLayout(0, 10));
        raiz.setBackground(FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        // ---------- Parte de arriba: busqueda de productos ----------
        JPanel panelBuscar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panelBuscar.setOpaque(false);
        JLabel lblBuscar = new JLabel("Buscar producto:");
        lblBuscar.setFont(new Font("SansSerif", Font.BOLD, 13));
        JButton btnBuscar = new JButton("Buscar");
        estilizar(btnBuscar, VERDE, Color.WHITE);
        btnBuscar.addActionListener(e -> cargarProductos());
        txtBuscar.addActionListener(e -> cargarProductos()); // Enter busca
        panelBuscar.add(lblBuscar);
        panelBuscar.add(txtBuscar);
        panelBuscar.add(btnBuscar);

        // ---------- Tabla de productos + cantidad ----------
        tablaProductos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaProductos.setRowHeight(24);
        JScrollPane scrollProductos = new JScrollPane(tablaProductos);
        scrollProductos.setBorder(BorderFactory.createTitledBorder("Productos disponibles"));
        scrollProductos.setOpaque(false);

        JPanel panelAgregar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panelAgregar.setOpaque(false);
        JLabel lblCantidad = new JLabel("Cantidad:");
        lblCantidad.setFont(new Font("SansSerif", Font.BOLD, 13));
        JButton btnAgregar = new JButton("Agregar a la venta");
        estilizar(btnAgregar, VERDE, Color.WHITE);
        btnAgregar.addActionListener(e -> agregar());
        txtCantidad.addActionListener(e -> agregar()); // Enter agrega
        panelAgregar.add(lblCantidad);
        panelAgregar.add(txtCantidad);
        panelAgregar.add(btnAgregar);

        JPanel panelProductos = new JPanel(new BorderLayout());
        panelProductos.setOpaque(false);
        panelProductos.add(scrollProductos, BorderLayout.CENTER);
        panelProductos.add(panelAgregar, BorderLayout.SOUTH);

        // ---------- Tabla del carrito ----------
        tablaCarrito.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaCarrito.setRowHeight(24);
        JScrollPane scrollCarrito = new JScrollPane(tablaCarrito);
        scrollCarrito.setBorder(BorderFactory.createTitledBorder("Venta actual"));
        scrollCarrito.setOpaque(false);

        JPanel centro = new JPanel(new GridLayout(2, 1, 0, 10));
        centro.setOpaque(false);
        centro.add(panelProductos);
        centro.add(scrollCarrito);

        // ---------- Parte de abajo: totales y botones ----------
        lblSubtotal.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblTotal.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblTotal.setForeground(VERDE);
        lblMensaje.setForeground(new Color(180, 30, 30));

        JPanel totales = new JPanel(new GridLayout(3, 1));
        totales.setOpaque(false);
        totales.add(lblSubtotal);
        totales.add(lblTotal);
        totales.add(lblMensaje);

        JButton btnQuitar = new JButton("Quitar producto");
        estilizar(btnQuitar, new Color(230, 230, 230), new Color(60, 60, 60));
        btnQuitar.addActionListener(e -> quitar());

        JButton btnConfirmar = new JButton("Confirmar venta");
        estilizar(btnConfirmar, VERDE, Color.WHITE);
        btnConfirmar.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnConfirmar.addActionListener(e -> confirmar());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnQuitar);
        botones.add(btnConfirmar);

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setOpaque(false);
        abajo.add(totales, BorderLayout.WEST);
        abajo.add(botones, BorderLayout.EAST);

        raiz.add(panelBuscar, BorderLayout.NORTH);
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(abajo, BorderLayout.SOUTH);
        setContentPane(raiz);
    }

    // ---------------------- Acciones ----------------------

    private void cargarProductos() {
        try {
            productosMostrados = servicio.buscarProductos(txtBuscar.getText());
            modeloProductos.setRowCount(0);
            for (Producto p : productosMostrados) {
                modeloProductos.addRow(new Object[]{
                        p.getCodigo(), p.getNombre(), p.getUnidadMedida(),
                        dinero(p.getPrecio()), p.getStock().stripTrailingZeros().toPlainString()
                });
            }
            lblMensaje.setText(" ");
        } catch (VentaException e) {
            mostrarError(e.getMessage());
        }
    }

    private void agregar() {
        int fila = tablaProductos.getSelectedRow();
        Producto producto = fila >= 0 ? productosMostrados.get(fila) : null;
        try {
            servicio.agregarProducto(venta, producto, txtCantidad.getText());
            txtCantidad.setText("");
            lblMensaje.setText(" ");
            refrescarCarrito();
        } catch (VentaException e) {
            mostrarError(e.getMessage());
        }
    }

    private void quitar() {
        int fila = tablaCarrito.getSelectedRow();
        if (fila < 0) {
            mostrarError("Selecciona un producto de la venta para quitarlo.");
            return;
        }
        servicio.quitarProducto(venta, venta.getDetalles().get(fila).getIdProducto());
        lblMensaje.setText(" ");
        refrescarCarrito();
    }

    private void confirmar() {
        try {
            servicio.confirmar(venta);
            JOptionPane.showMessageDialog(this,
                    "Venta registrada.\nFolio: " + venta.getFolio() + "\nTotal: " + dinero(venta.getTotal()),
                    "Venta exitosa", JOptionPane.INFORMATION_MESSAGE);
            venta = new Venta();
            refrescarCarrito();
            cargarProductos(); // para ver el stock actualizado
        } catch (VentaException e) {
            mostrarError(e.getMessage());
        }
    }

    private void refrescarCarrito() {
        modeloCarrito.setRowCount(0);
        for (DetalleVenta d : venta.getDetalles()) {
            modeloCarrito.addRow(new Object[]{
                    d.getNombreProducto(),
                    d.getCantidad().stripTrailingZeros().toPlainString(),
                    dinero(d.getPrecioUnitario()),
                    dinero(d.getSubtotal())
            });
        }
        lblSubtotal.setText("Subtotal: " + dinero(venta.getSubtotal()));
        lblTotal.setText("Total: " + dinero(venta.getTotal()));
    }

    // ---------------------- Utilidades ----------------------

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
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
