package com.fruteriablanco.presentacion;


import com.fruteriablanco.modelo.Producto;
import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.servicio.ProductoService;
import com.fruteriablanco.util.SesionActual;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Pantalla de consulta y administracion del catalogo de productos.
 */
public class ProductoFrame extends JFrame {

    static final Color VERDE = new Color(46, 125, 82);
    static final Color VERDE_OSCURO = new Color(31, 93, 60);
    static final Color FONDO = new Color(247, 250, 246);
    static final Color TEXTO = new Color(45, 55, 45);

    private final ProductoService servicio = new ProductoService();

    private final JTextField txtBuscar = new JTextField(22);
    private final JLabel lblEstado = new JLabel(" ");

    private final JButton btnNuevo = new JButton("Registrar producto");
    private final JButton btnEditar = new JButton("Editar datos");
    private final JButton btnEntrada = new JButton("Registrar entrada");
    private final JButton btnSalida = new JButton("Registrar salida");
    private final JButton btnDesactivar = new JButton("Desactivar");
    private final JButton btnActivar = new JButton("Activar");
    private final JButton btnActualizar = new JButton("Actualizar");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{
                    "ID",
                    "Codigo",
                    "Producto",
                    "Categoria",
                    "Descripcion",
                    "Precio",
                    "Existencia",
                    "Stock minimo",
                    "Unidad",
                    "Estado"
            }, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable tabla = new JTable(modeloTabla);

    private List<Producto> productos = new ArrayList<>();

    public ProductoFrame() {

        super("Fruteria Blanco - Administracion de productos");

        Usuario usuario = SesionActual.getUsuario();

        if (usuario == null || !usuario.esAdministrador()) {

            JOptionPane.showMessageDialog(
                    null,
                    "Solo el rol Administrador puede administrar productos.",
                    "Acceso restringido",
                    JOptionPane.WARNING_MESSAGE
            );

            dispose();
            return;
        }

        construirInterfaz();
        configurarEventos();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 560));
        setSize(1250, 680);
        setLocationRelativeTo(null);

        cargarProductos();
    }

    private void construirInterfaz() {

        JPanel principal = new JPanel(new BorderLayout(0, 14));
        principal.setBackground(FONDO);
        principal.setBorder(new EmptyBorder(18, 22, 16, 22));

        // ENCABEZADO
        JPanel encabezado = new JPanel(new BorderLayout(8, 4));
        encabezado.setOpaque(false);

        JLabel titulo = new JLabel("Productos e inventario");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        titulo.setForeground(VERDE_OSCURO);

        JLabel subtitulo = new JLabel(
                "Registra frutas y verduras, actualiza sus datos y controla las existencias."
        );

        subtitulo.setForeground(new Color(95, 105, 95));

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 3));
        textos.setOpaque(false);

        textos.add(titulo);
        textos.add(subtitulo);

        encabezado.add(textos, BorderLayout.WEST);
        principal.add(encabezado, BorderLayout.NORTH);

        // CENTRO
        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);

        JPanel busqueda = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 0)
        );

        busqueda.setOpaque(false);

        JLabel lblBuscar = new JLabel(
                "Buscar por codigo, nombre o categoria:"
        );
        lblBuscar.setForeground(TEXTO);

        busqueda.add(lblBuscar);

        txtBuscar.setPreferredSize(new Dimension(230, 30));
        txtBuscar.setForeground(Color.BLACK);
        busqueda.add(txtBuscar);

        JButton btnBuscar = new JButton("Buscar");
        estilizarBoton(btnBuscar, false);

        btnBuscar.addActionListener(e -> cargarProductos());

        busqueda.add(btnBuscar);

        centro.add(busqueda, BorderLayout.NORTH);

        // TABLA
        tabla.setRowHeight(30);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.setFillsViewportHeight(true);

        tabla.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tabla.setForeground(Color.BLACK);

        tabla.setSelectionBackground(new Color(210, 235, 215));
        tabla.setSelectionForeground(Color.BLACK);

        tabla.getTableHeader().setFont(
                new Font("SansSerif", Font.BOLD, 12)
        );

        tabla.getTableHeader().setBackground(
                new Color(226, 239, 226)
        );

        tabla.getTableHeader().setForeground(TEXTO);

        tabla.getColumnModel().getColumn(0).setPreferredWidth(45);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(140);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(100);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(6).setPreferredWidth(85);
        tabla.getColumnModel().getColumn(7).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(8).setPreferredWidth(75);
        tabla.getColumnModel().getColumn(9).setPreferredWidth(75);

        JScrollPane scroll = new JScrollPane(tabla);

        scroll.setBorder(
                BorderFactory.createLineBorder(
                        new Color(218, 228, 218)
                )
        );

        centro.add(scroll, BorderLayout.CENTER);

        principal.add(centro, BorderLayout.CENTER);

        // BOTONES INFERIORES
        JPanel inferior = new JPanel(new BorderLayout(8, 8));
        inferior.setOpaque(false);

        JPanel acciones = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 7, 0)
        );

        acciones.setOpaque(false);

        estilizarBoton(btnNuevo, true);
        estilizarBoton(btnEditar, false);
        estilizarBoton(btnEntrada, false);
        estilizarBoton(btnSalida, false);
        estilizarBoton(btnDesactivar, false);
        estilizarBoton(btnActivar, false);
        estilizarBoton(btnActualizar, false);

        acciones.add(btnNuevo);
        acciones.add(btnEditar);
        acciones.add(btnEntrada);
        acciones.add(btnSalida);
        acciones.add(btnDesactivar);
        acciones.add(btnActivar);
        acciones.add(btnActualizar);

        inferior.add(acciones, BorderLayout.NORTH);

        lblEstado.setForeground(new Color(90, 105, 90));
        inferior.add(lblEstado, BorderLayout.SOUTH);

        principal.add(inferior, BorderLayout.SOUTH);

        setContentPane(principal);
    }

    private void configurarEventos() {

        btnNuevo.addActionListener(e ->
                new ProductoDialog(
                        this,
                        null,
                        servicio,
                        usuarioActual(),
                        this::cargarProductos
                ).setVisible(true)
        );

        btnEditar.addActionListener(e -> {

            Producto p = productoSeleccionado();

            if (p == null) return;

            if (!p.isActivo()) {
                mensaje(
                        "No se puede editar un producto desactivado.",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            new ProductoDialog(
                    this,
                    p,
                    servicio,
                    usuarioActual(),
                    this::cargarProductos
            ).setVisible(true);
        });

        btnEntrada.addActionListener(e -> abrirMovimiento("ENTRADA"));

        btnSalida.addActionListener(e -> abrirMovimiento("SALIDA"));

        btnDesactivar.addActionListener(e -> desactivarSeleccionado());

        btnActivar.addActionListener(e -> activarSeleccionado());

        btnActualizar.addActionListener(e -> cargarProductos());

        txtBuscar.addActionListener(e -> cargarProductos());

        tabla.getSelectionModel().addListSelectionListener(e -> {

            if (!e.getValueIsAdjusting()) {
                actualizarEstadoBotones();
            }
        });
    }

    // CONSULTAR PRODUCTOS
    private void cargarProductos() {

        setAccionesActivas(false);

        lblEstado.setText("Consultando productos...");

        String filtro = txtBuscar.getText().trim();

        new SwingWorker<List<Producto>, Void>() {

            @Override
            protected List<Producto> doInBackground() throws Exception {
                return servicio.listar(filtro);
            }

            @Override
            protected void done() {

                try {

                    productos = get();

                    modeloTabla.setRowCount(0);

                    for (Producto p : productos) {

                        modeloTabla.addRow(new Object[]{
                                p.getIdProducto(),
                                p.getCodigo(),
                                p.getNombre(),
                                p.getCategoria(),
                                p.getDescripcion(),
                                "$" + p.getPrecio().toPlainString(),
                                p.getStock().stripTrailingZeros().toPlainString(),
                                p.getStockMinimo().stripTrailingZeros().toPlainString(),
                                p.getUnidadMedida().name(),
                                p.isActivo() ? "Activo" : "Inactivo"
                        });
                    }

                    lblEstado.setText(
                            productos.size() + " producto(s) encontrado(s)."
                    );

                } catch (InterruptedException ex) {

                    Thread.currentThread().interrupt();
                    lblEstado.setText("Consulta interrumpida.");

                } catch (ExecutionException ex) {

                    mostrarError(ex.getCause());

                } finally {

                    setAccionesActivas(true);
                    actualizarEstadoBotones();
                }
            }

        }.execute();
    }

    // REGISTRAR ENTRADA O SALIDA
    private void abrirMovimiento(String tipo) {

        Producto p = productoSeleccionado();

        if (p == null) return;

        if (!p.isActivo()) {

            mensaje(
                    "No se pueden registrar movimientos en un producto desactivado.",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        new MovimientoDialog(
                this,
                p,
                tipo,
                servicio,
                usuarioActual(),
                this::cargarProductos
        ).setVisible(true);
    }

    // DESACTIVAR PRODUCTO
    private void desactivarSeleccionado() {

        Producto p = productoSeleccionado();

        if (p == null) return;

        if (!p.isActivo()) {

            mensaje(
                    "El producto ya esta desactivado.",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "El producto '" + p.getNombre()
                        + "' dejara de aparecer como activo.\n"
                        + "Sus movimientos e historial se conservaran.\n\n"
                        + "Deseas continuar?",
                "Confirmar baja",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) return;

        setAccionesActivas(false);

        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {

                servicio.desactivar(
                        p.getIdProducto(),
                        usuarioActual()
                );

                return null;
            }

            @Override
            protected void done() {

                setAccionesActivas(true);

                try {

                    get();

                    mensaje(
                            "Producto desactivado correctamente.",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    cargarProductos();

                } catch (Exception ex) {

                    mostrarError(causa(ex));
                }
            }

        }.execute();
    }

    // ACTIVAR PRODUCTO
    private void activarSeleccionado() {

        Producto p = productoSeleccionado();

        if (p == null) return;

        if (p.isActivo()) {

            mensaje(
                    "El producto ya esta activo.",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                "Deseas volver a activar el producto '"
                        + p.getNombre() + "'?",
                "Confirmar reactivacion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) return;

        setAccionesActivas(false);

        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {

                servicio.activar(
                        p.getIdProducto(),
                        usuarioActual()
                );

                return null;
            }

            @Override
            protected void done() {

                setAccionesActivas(true);

                try {

                    get();

                    mensaje(
                            "Producto activado correctamente.",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    cargarProductos();

                } catch (Exception ex) {

                    mostrarError(causa(ex));
                }
            }

        }.execute();
    }

    // PRODUCTO SELECCIONADO
    private Producto productoSeleccionado() {

        int filaVista = tabla.getSelectedRow();

        if (filaVista < 0) {

            mensaje(
                    "Selecciona un producto de la tabla.",
                    JOptionPane.WARNING_MESSAGE
            );

            return null;
        }

        int filaModelo = tabla.convertRowIndexToModel(filaVista);

        return productos.get(filaModelo);
    }

    private int usuarioActual() {
        return SesionActual.getUsuario().getIdUsuario();
    }

    // ESTADO DE LOS BOTONES
    private void actualizarEstadoBotones() {

        int fila = tabla.getSelectedRow();

        boolean seleccionado = fila >= 0;

        btnEditar.setEnabled(seleccionado);
        btnEntrada.setEnabled(seleccionado);
        btnSalida.setEnabled(seleccionado);
        btnDesactivar.setEnabled(seleccionado);
        btnActivar.setEnabled(seleccionado);

        if (seleccionado) {

            Producto p = productos.get(
                    tabla.convertRowIndexToModel(fila)
            );

            btnEditar.setEnabled(p.isActivo());
            btnEntrada.setEnabled(p.isActivo());
            btnSalida.setEnabled(p.isActivo());
            btnDesactivar.setEnabled(p.isActivo());
            btnActivar.setEnabled(!p.isActivo());

        }
    }

    private void setAccionesActivas(boolean activa) {

        btnNuevo.setEnabled(activa);
        btnActualizar.setEnabled(activa);
        btnActivar.setEnabled(activa);
        txtBuscar.setEnabled(activa);

        if (!activa) {

            btnEditar.setEnabled(false);
            btnEntrada.setEnabled(false);
            btnSalida.setEnabled(false);
            btnDesactivar.setEnabled(false);
            btnActivar.setEnabled(false);
        }
    }

    // MOSTRAR ERRORES
    private void mostrarError(Throwable error) {

        String detalle = error == null
                ? "Error desconocido."
                : error.getMessage();

        if (detalle == null || detalle.isBlank()) {
            detalle = "No fue posible completar la operacion.";
        }

        mensaje(detalle, JOptionPane.ERROR_MESSAGE);

        lblEstado.setText(
                "No se pudo completar la consulta u operacion."
        );
    }

    private Throwable causa(Exception ex) {

        return ex instanceof ExecutionException && ex.getCause() != null
                ? ex.getCause()
                : ex;
    }

    private void mensaje(String texto, int tipo) {

        JOptionPane.showMessageDialog(
                this,
                texto,
                "Fruteria Blanco",
                tipo
        );
    }

    // ESTILO DE BOTONES
    static void estilizarBoton(JButton boton, boolean principal) {

        boton.setFont(new Font("SansSerif", Font.BOLD, 12));
        boton.setFocusPainted(false);

        boton.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        boton.setBorder(
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        );

        if (principal) {

            boton.setBackground(VERDE);

        } else {

            boton.setBackground(new Color(232, 241, 231));
        }

        // Todas las letras de los botones en negro
        boton.setForeground(Color.BLACK);

        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
    }
}
