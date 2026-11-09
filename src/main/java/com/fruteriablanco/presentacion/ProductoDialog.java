package com.fruteriablanco.presentacion;

import com.fruteriablanco.modelo.Producto;
import com.fruteriablanco.servicio.ProductoService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ExecutionException;

/** Formulario para dar de alta un producto o modificar sus datos generales. */
class ProductoDialog extends JDialog {

    private final Producto productoEditar;
    private final ProductoService servicio;
    private final int idUsuario;
    private final Runnable alGuardar;

    private final JTextField txtCodigo = new JTextField(18);
    private final JTextField txtNombre = new JTextField(22);
    private final JComboBox<String> cboCategoria = new JComboBox<>(new String[]{
            "Frutas", "Verduras", "Legumbres", "Tuberculos", "Hierbas", "Otros"});
    private final JTextField txtPrecio = new JTextField(12);
    private final JComboBox<Producto.UnidadMedida> cboUnidad = new JComboBox<>(Producto.UnidadMedida.values());
    private final JTextField txtStockMinimo = new JTextField("0", 12);
    private final JSpinner spDiasAviso = new JSpinner(new SpinnerNumberModel(3, 0, 365, 1));
    private final JTextArea txtDescripcion = new JTextArea(3, 22);

    private final JTextField txtStockInicial = new JTextField("0", 12);
    private final JTextField txtProveedor = new JTextField(18);
    private final JTextField txtTeléfonoProveedor = new JTextField(18);
    private final JTextField txtVencimiento = new JTextField(12);
    private final JButton btnGuardar = new JButton("Guardar producto");

    ProductoDialog(Window owner, Producto producto, ProductoService servicio,
                   int idUsuario, Runnable alGuardar) {
        super(owner, producto == null ? "Registrar producto" : "Editar producto", ModalityType.APPLICATION_MODAL);
        this.productoEditar = producto;
        this.servicio = servicio;
        this.idUsuario = idUsuario;
        this.alGuardar = alGuardar;

        construirInterfaz();
        if (productoEditar != null) cargarDatos();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void construirInterfaz() {
        JPanel principal = new JPanel(new BorderLayout(8, 10));
        principal.setBackground(ProductoFrame.FONDO);
        principal.setBorder(new EmptyBorder(18, 20, 16, 20));

        JLabel titulo = new JLabel(productoEditar == null ? "Datos del nuevo producto" : "Modificar datos del producto");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 19));
        titulo.setForeground(ProductoFrame.VERDE_OSCURO);
        principal.add(titulo, BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        int fila = 0;
        agregarCampo(campos, gc, fila++, "Código *", txtCodigo, "Ej. FR-005");
        agregarCampo(campos, gc, fila++, "Nombre del producto *", txtNombre, null);
        cboCategoria.setEditable(true);
        agregarCampo(campos, gc, fila++, "Categoría *", cboCategoria, null);
        agregarCampo(campos, gc, fila++, "Precio de venta *", txtPrecio, "En pesos mexicanos");
        agregarCampo(campos, gc, fila++, "Unidad de medida *", cboUnidad, null);
        agregarCampo(campos, gc, fila++, "Stock minimo", txtStockMinimo, "Cantidad para alerta de bajo stock");
        agregarCampo(campos, gc, fila++, "Avisar vencimiento (dias)", spDiasAviso, null);

        gc.gridx = 0; gc.gridy = fila; gc.weightx = 0;
        campos.add(new JLabel("Descripción"), gc);
        gc.gridx = 1; gc.weightx = 1;
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        campos.add(new JScrollPane(txtDescripcion), gc);
        fila++;

        if (productoEditar == null) {
            JSeparator separador = new JSeparator();
            gc.gridx = 0; gc.gridy = fila++; gc.gridwidth = 2;
            campos.add(separador, gc);
            gc.gridwidth = 1;
            JLabel lblInicial = new JLabel("Inventario inicial (opcional)");
            lblInicial.setFont(lblInicial.getFont().deriveFont(Font.BOLD));
            lblInicial.setForeground(ProductoFrame.VERDE_OSCURO);
            gc.gridx = 0; gc.gridy = fila++; gc.gridwidth = 2;
            campos.add(lblInicial, gc);
            gc.gridwidth = 1;
            agregarCampo(campos, gc, fila++, "Existencia inicial", txtStockInicial, "Puede ser 0");
            agregarCampo(campos, gc, fila++, "Proveedor", txtProveedor, "Opcional");
            agregarCampo(campos, gc, fila++, "Teléfono del proveedor", txtTeléfonoProveedor, "Opcional");
            agregarCampo(campos, gc, fila++, "Vencimiento (AAAA-MM-DD)", txtVencimiento, "Opcional");
        } else {
            JLabel nota = new JLabel("Para modificar existencias utiliza Registrar entrada o Registrar salida.");
            nota.setForeground(new Color(95, 105, 95));
            gc.gridx = 0; gc.gridy = fila; gc.gridwidth = 2;
            campos.add(nota, gc);
        }

        JScrollPane scroll = new JScrollPane(campos);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);
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

    private void agregarCampo(JPanel panel, GridBagConstraints gc, int fila,
                              String etiqueta, JComponent campo, String ayuda) {
        gc.gridy = fila;
        gc.gridx = 0;
        gc.weightx = 0;
        panel.add(new JLabel(etiqueta), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        panel.add(campo, gc);
        if (ayuda != null) {
            gc.gridx = 2;
            gc.weightx = 0;
            JLabel nota = new JLabel(ayuda);
            nota.setForeground(new Color(115, 120, 110));
            nota.setFont(nota.getFont().deriveFont(10f));
            panel.add(nota, gc);
        }
    }

    private void cargarDatos() {
        txtCodigo.setText(productoEditar.getCodigo());
        txtNombre.setText(productoEditar.getNombre());
        cboCategoria.setSelectedItem(productoEditar.getCategoria());
        txtPrecio.setText(productoEditar.getPrecio().toPlainString());
        cboUnidad.setSelectedItem(productoEditar.getUnidadMedida());
        txtStockMinimo.setText(productoEditar.getStockMinimo().stripTrailingZeros().toPlainString());
        spDiasAviso.setValue(productoEditar.getDiasAvisoVencimiento());
        txtDescripcion.setText(productoEditar.getDescripcion() == null ? "" : productoEditar.getDescripcion());
        txtCodigo.setEditable(false);
    }

    private void guardar() {
        try {
            Producto p = productoEditar == null ? new Producto() : productoEditar;
            p.setCodigo(txtCodigo.getText().trim());
            p.setNombre(txtNombre.getText().trim());
            p.setCategoria(String.valueOf(cboCategoria.getEditor().getItem()).trim());
            p.setPrecio(new BigDecimal(txtPrecio.getText().trim()));
            p.setUnidadMedida((Producto.UnidadMedida) cboUnidad.getSelectedItem());
            p.setStockMinimo(new BigDecimal(txtStockMinimo.getText().trim()));
            p.setDiasAvisoVencimiento((Integer) spDiasAviso.getValue());
            p.setDescripcion(txtDescripcion.getText().trim());

            BigDecimal stockInicial = BigDecimal.ZERO;
            LocalDate vencimiento = null;
            if (productoEditar == null) {
                stockInicial = new BigDecimal(txtStockInicial.getText().trim());
                vencimiento = parsearFecha(txtVencimiento.getText());
            }

            btnGuardar.setEnabled(false);
            btnGuardar.setText("Guardando...");
            Producto finalP = p;
            BigDecimal finalStockInicial = stockInicial;
            LocalDate finalVencimiento = vencimiento;
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    if (productoEditar == null) {
                        servicio.crear(finalP, finalStockInicial, txtProveedor.getText(),
                                txtTeléfonoProveedor.getText(), finalVencimiento, idUsuario);
                    } else {
                        servicio.actualizar(finalP, idUsuario);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("Guardar producto");
                    try {
                        get();
                        JOptionPane.showMessageDialog(ProductoDialog.this,
                                productoEditar == null ? "Producto registrado correctamente." : "Cambios guardados correctamente.",
                                "Frutería Blanco", JOptionPane.INFORMATION_MESSAGE);
                        alGuardar.run();
                        dispose();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        mostrarError("La operación fue interrumpida.");
                    } catch (ExecutionException ex) {
                        Throwable causa = ex.getCause();
                        mostrarError(causa == null ? "No fue posible guardar el producto." : causa.getMessage());
                    }
                }
            }.execute();
        } catch (NumberFormatException ex) {
            mostrarError("Revisa los campos numericos. Utiliza punto para los decimales.");
        } catch (DateTimeParseException ex) {
            mostrarError("La fecha debe tener el formato AAAA-MM-DD, por ejemplo 2026-12-31.");
        } catch (Exception ex) {
            mostrarError(ex.getMessage());
        }
    }

    private LocalDate parsearFecha(String texto) {
        if (texto == null || texto.isBlank()) return null;
        return LocalDate.parse(texto.trim());
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this,
                mensaje == null || mensaje.isBlank() ? "Verifica los datos ingresados." : mensaje,
                "No fue posible guardar", JOptionPane.WARNING_MESSAGE);
    }
}
