package com.fruteriablanco.presentacion;

import com.fruteriablanco.modelo.Producto;
import com.fruteriablanco.servicio.ProductoService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Dialogo para registrar una entrada o salida de inventario. */
class MovimientoDialog extends JDialog {

    private final Producto producto;
    private final String tipo;
    private final ProductoService servicio;
    private final int idUsuario;
    private final Runnable alGuardar;

    private final JLabel lblStock = new JLabel();
    private final JComboBox<String> cboMotivo = new JComboBox<>();
    private final JTextField txtCantidad = new JTextField("1", 12);
    private final JTextField txtProveedor = new JTextField(18);
    private final JTextField txtTeléfono = new JTextField(18);
    private final JTextField txtVencimiento = new JTextField(12);
    private final JTextArea txtObservaciones = new JTextArea(3, 22);
    private final JButton btnGuardar = new JButton("Guardar movimiento");

    MovimientoDialog(Window owner, Producto producto, String tipo,
                     ProductoService servicio, int idUsuario, Runnable alGuardar) {
        super(owner, "ENTRADA".equals(tipo) ? "Registrar entrada" : "Registrar salida", ModalityType.APPLICATION_MODAL);
        this.producto = producto;
        this.tipo = tipo;
        this.servicio = servicio;
        this.idUsuario = idUsuario;
        this.alGuardar = alGuardar;
        construirInterfaz();
        cargarMotivos();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void construirInterfaz() {
        JPanel principal = new JPanel(new BorderLayout(8, 10));
        principal.setBackground(ProductoFrame.FONDO);
        principal.setBorder(new EmptyBorder(18, 20, 16, 20));

        JLabel titulo = new JLabel("ENTRADA".equals(tipo) ? "Entrada de inventario" : "Salida de inventario");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 19));
        titulo.setForeground(ProductoFrame.VERDE_OSCURO);
        principal.add(titulo, BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 6, 5, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        lblStock.setText("Producto: " + producto.getNombre() + " | Existencia actual: "
                + producto.getStock().stripTrailingZeros().toPlainString() + " " + producto.getUnidadMedida().name());
        lblStock.setForeground(new Color(80, 95, 80));
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
        campos.add(lblStock, gc);
        gc.gridwidth = 1;

        campo(campos, gc, 1, "Motivo *", cboMotivo);
        campo(campos, gc, 2, "Cantidad *", txtCantidad);

        if ("ENTRADA".equals(tipo)) {
            campo(campos, gc, 3, "Proveedor", txtProveedor);
            campo(campos, gc, 4, "Teléfono del proveedor", txtTeléfono);
            campo(campos, gc, 5, "Vencimiento (AAAA-MM-DD)", txtVencimiento);
        }

        gc.gridx = 0; gc.gridy = "ENTRADA".equals(tipo) ? 6 : 3; gc.weightx = 0;
        campos.add(new JLabel("Observaciones"), gc);
        gc.gridx = 1; gc.weightx = 1;
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);
        campos.add(new JScrollPane(txtObservaciones), gc);

        JScrollPane scroll = new JScrollPane(campos);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        principal.add(scroll, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        JButton btnCancelar = new JButton("Cancelar");
        ProductoFrame.estilizarBoton(btnGuardar, true);
        ProductoFrame.estilizarBoton(btnCancelar, false);
        botones.add(btnCancelar);
        botones.add(btnGuardar);
        principal.add(botones, BorderLayout.SOUTH);
        setContentPane(principal);

        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> guardar());
        getRootPane().setDefaultButton(btnGuardar);
    }

    private void campo(JPanel panel, GridBagConstraints gc, int fila, String etiqueta, JComponent componente) {
        gc.gridx = 0; gc.gridy = fila; gc.weightx = 0;
        panel.add(new JLabel(etiqueta), gc);
        gc.gridx = 1; gc.weightx = 1;
        panel.add(componente, gc);
    }

    private void cargarMotivos() {
        cboMotivo.setEnabled(false);
        btnGuardar.setEnabled(false);
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return servicio.listarMotivos(tipo);
            }
            @Override
            protected void done() {
                try {
                    List<String> motivos = get();
                    for (String motivo : motivos) cboMotivo.addItem(motivo);
                    if (motivos.isEmpty()) {
                        JOptionPane.showMessageDialog(MovimientoDialog.this,
                                "No hay motivos de inventario configurados. Revisa database/schema.sql.",
                                "Catalogo incompleto", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    Throwable causa = ex instanceof ExecutionException && ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(MovimientoDialog.this, causa.getMessage(),
                            "Error al consultar motivos", JOptionPane.ERROR_MESSAGE);
                } finally {
                    cboMotivo.setEnabled(true);
                    btnGuardar.setEnabled(cboMotivo.getItemCount() > 0);
                }
            }
        }.execute();
    }

    private void guardar() {
        try {
            BigDecimal cantidad = new BigDecimal(txtCantidad.getText().trim());
            String motivo = (String) cboMotivo.getSelectedItem();
            LocalDate vencimiento = null;
            if ("ENTRADA".equals(tipo) && !txtVencimiento.getText().trim().isEmpty()) {
                vencimiento = LocalDate.parse(txtVencimiento.getText().trim());
            }

            btnGuardar.setEnabled(false);
            btnGuardar.setText("Guardando...");
            LocalDate fechaFinal = vencimiento;
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    servicio.registrarMovimiento(producto.getIdProducto(), tipo, cantidad, motivo,
                            txtProveedor.getText(), txtTeléfono.getText(), fechaFinal,
                            txtObservaciones.getText(), idUsuario);
                    return null;
                }
                @Override
                protected void done() {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("Guardar movimiento");
                    try {
                        get();
                        JOptionPane.showMessageDialog(MovimientoDialog.this,
                                "Movimiento guardado y existencia actualizada correctamente.",
                                "Frutería Blanco", JOptionPane.INFORMATION_MESSAGE);
                        alGuardar.run();
                        dispose();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        error("La operación fue interrumpida.");
                    } catch (ExecutionException ex) {
                        Throwable causa = ex.getCause();
                        error(causa == null ? "No fue posible guardar el movimiento." : causa.getMessage());
                    }
                }
            }.execute();
        } catch (NumberFormatException ex) {
            error("Ingresa una cantidad numerica valida. Utiliza punto para los decimales.");
        } catch (DateTimeParseException ex) {
            error("La fecha debe tener el formato AAAA-MM-DD, por ejemplo 2026-12-31.");
        }
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this,
                mensaje == null || mensaje.isBlank() ? "Verifica los datos ingresados." : mensaje,
                "No fue posible guardar", JOptionPane.WARNING_MESSAGE);
    }
}
