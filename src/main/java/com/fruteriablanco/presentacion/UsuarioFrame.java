package com.fruteriablanco.presentacion;

import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.servicio.UsuarioService;
import com.fruteriablanco.util.SesionActual;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static com.fruteriablanco.presentacion.ProductoFrame.FONDO;
import static com.fruteriablanco.presentacion.ProductoFrame.TEXTO;
import static com.fruteriablanco.presentacion.ProductoFrame.VERDE_OSCURO;
import static com.fruteriablanco.presentacion.ProductoFrame.estilizarBoton;

/** USR-01: módulo de usuarios (crear, consultar, modificar y eliminar). Solo Administrador. */
public class UsuarioFrame extends JFrame {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UsuarioService servicio = new UsuarioService();

    private final JTextField txtBuscar = new JTextField(22);
    private final JLabel lblEstado = new JLabel(" ");

    private final JButton btnNuevo = new JButton("Registrar usuario");
    private final JButton btnEditar = new JButton("Modificar");
    private final JButton btnEliminar = new JButton("Eliminar");
    private final JButton btnReactivar = new JButton("Reactivar");
    private final JButton btnActualizar = new JButton("Actualizar");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Correo", "Rol", "Estado", "Fecha de alta"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);

    private List<Usuario> usuarios = new ArrayList<>();

    public UsuarioFrame() {
        super("Fruteria Blanco - Administracion de usuarios");

        Usuario actual = SesionActual.getUsuario();
        if (actual == null || !actual.esAdministrador()) {
            JOptionPane.showMessageDialog(null,
                    "Solo el rol Administrador puede administrar usuarios.",
                    "Acceso restringido", JOptionPane.WARNING_MESSAGE);
            dispose();
            return;
        }

        construirInterfaz();
        configurarEventos();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(860, 500));
        setSize(980, 600);
        setLocationRelativeTo(null);

        cargarUsuarios();
    }

    private void construirInterfaz() {
        JPanel principal = new JPanel(new BorderLayout(0, 14));
        principal.setBackground(FONDO);
        principal.setBorder(new EmptyBorder(18, 22, 16, 22));

        JLabel titulo = new JLabel("Usuarios del sistema");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        titulo.setForeground(VERDE_OSCURO);
        JLabel subtitulo = new JLabel("Registra usuarios, asigna su rol y controla quién puede acceder al sistema.");
        subtitulo.setForeground(new Color(95, 105, 95));
        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 3));
        textos.setOpaque(false);
        textos.add(titulo);
        textos.add(subtitulo);
        principal.add(textos, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);

        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        busqueda.setOpaque(false);
        JLabel lblBuscar = new JLabel("Buscar por nombre, correo o rol:");
        lblBuscar.setForeground(TEXTO);
        busqueda.add(lblBuscar);
        txtBuscar.setPreferredSize(new Dimension(230, 30));
        busqueda.add(txtBuscar);
        JButton btnBuscar = new JButton("Buscar");
        estilizarBoton(btnBuscar, false);
        btnBuscar.addActionListener(e -> cargarUsuarios());
        busqueda.add(btnBuscar);
        centro.add(busqueda, BorderLayout.NORTH);

        tabla.setRowHeight(30);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.setFillsViewportHeight(true);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tabla.setSelectionBackground(new Color(210, 235, 215));
        tabla.setSelectionForeground(Color.BLACK);
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(new Color(226, 239, 226));
        tabla.getTableHeader().setForeground(TEXTO);
        int[] anchos = {45, 180, 240, 110, 80, 130};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(218, 228, 218)));
        centro.add(scroll, BorderLayout.CENTER);
        principal.add(centro, BorderLayout.CENTER);

        JPanel inferior = new JPanel(new BorderLayout(8, 8));
        inferior.setOpaque(false);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        acciones.setOpaque(false);
        estilizarBoton(btnNuevo, true);
        for (JButton b : new JButton[]{btnEditar, btnEliminar, btnReactivar, btnActualizar}) {
            estilizarBoton(b, false);
        }
        acciones.add(btnNuevo);
        acciones.add(btnEditar);
        acciones.add(btnEliminar);
        acciones.add(btnReactivar);
        acciones.add(btnActualizar);
        inferior.add(acciones, BorderLayout.NORTH);
        lblEstado.setForeground(new Color(90, 105, 90));
        inferior.add(lblEstado, BorderLayout.SOUTH);
        principal.add(inferior, BorderLayout.SOUTH);

        setContentPane(principal);
    }

    private void configurarEventos() {
        btnNuevo.addActionListener(e ->
                new UsuarioDialog(this, null, servicio, idAdmin(), this::cargarUsuarios).setVisible(true));

        btnEditar.addActionListener(e -> {
            Usuario u = seleccionado();
            if (u != null) {
                new UsuarioDialog(this, u, servicio, idAdmin(), this::cargarUsuarios).setVisible(true);
            }
        });

        btnEliminar.addActionListener(e -> eliminarSeleccionado());
        btnReactivar.addActionListener(e -> reactivarSeleccionado());
        btnActualizar.addActionListener(e -> cargarUsuarios());
        txtBuscar.addActionListener(e -> cargarUsuarios());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarEstadoBotones();
            }
        });
    }

    private void cargarUsuarios() {
        lblEstado.setText("Consultando usuarios...");
        String filtro = txtBuscar.getText().trim();

        new SwingWorker<List<Usuario>, Void>() {
            @Override
            protected List<Usuario> doInBackground() throws Exception {
                return servicio.listar(filtro);
            }

            @Override
            protected void done() {
                try {
                    usuarios = get();
                    modeloTabla.setRowCount(0);
                    for (Usuario u : usuarios) {
                        modeloTabla.addRow(new Object[]{
                                u.getIdUsuario(),
                                u.getNombre(),
                                u.getCorreo(),
                                u.getRol().getNombre(),
                                u.isActivo() ? "Activo" : "Inactivo",
                                u.getFechaCreacion() == null ? "" : u.getFechaCreacion().format(FECHA)
                        });
                    }
                    lblEstado.setText(usuarios.size() + " usuario(s) encontrado(s).");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    mostrarError(ex.getCause());
                } finally {
                    actualizarEstadoBotones();
                }
            }
        }.execute();
    }

    private void eliminarSeleccionado() {
        Usuario u = seleccionado();
        if (u == null) return;

        int r = JOptionPane.showConfirmDialog(this,
                "El usuario '" + u.getNombre() + "' ya no podra iniciar sesion.\n"
                        + "Sus ventas e historial se conservaran.\n\nDeseas continuar?",
                "Confirmar eliminacion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;

        ejecutar(() -> { servicio.eliminar(u.getIdUsuario(), idAdmin()); return null; },
                "Usuario eliminado correctamente.");
    }

    private void reactivarSeleccionado() {
        Usuario u = seleccionado();
        if (u == null) return;

        int r = JOptionPane.showConfirmDialog(this,
                "Deseas reactivar al usuario '" + u.getNombre() + "'?",
                "Confirmar reactivacion", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;

        ejecutar(() -> { servicio.reactivar(u.getIdUsuario(), idAdmin()); return null; },
                "Usuario reactivado correctamente.");
    }

    private void ejecutar(Callable<Void> accion, String exito) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                return accion.call();
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(UsuarioFrame.this, exito, "Fruteria Blanco",
                            JOptionPane.INFORMATION_MESSAGE);
                    cargarUsuarios();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    mostrarError(ex.getCause());
                }
            }
        }.execute();
    }

    private Usuario seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario de la tabla.",
                    "Fruteria Blanco", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return usuarios.get(tabla.convertRowIndexToModel(fila));
    }

    private void actualizarEstadoBotones() {
        int fila = tabla.getSelectedRow();
        Usuario u = fila >= 0 && fila < tabla.getRowCount()
                ? usuarios.get(tabla.convertRowIndexToModel(fila)) : null;
        btnEditar.setEnabled(u != null);
        btnEliminar.setEnabled(u != null && u.isActivo() && u.getIdUsuario() != idAdmin());
        btnReactivar.setEnabled(u != null && !u.isActivo());
    }

    private int idAdmin() {
        return SesionActual.getUsuario().getIdUsuario();
    }

    private void mostrarError(Throwable error) {
        String detalle = error == null || error.getMessage() == null || error.getMessage().isBlank()
                ? "No fue posible completar la operacion." : error.getMessage();
        JOptionPane.showMessageDialog(this, detalle, "Fruteria Blanco", JOptionPane.ERROR_MESSAGE);
        lblEstado.setText("No se pudo completar la consulta u operacion.");
    }
}
