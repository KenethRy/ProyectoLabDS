package com.fruteriablanco.presentacion;

import com.fruteriablanco.modelo.Rol;
import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.servicio.UsuarioService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.fruteriablanco.presentacion.ProductoFrame.FONDO;
import static com.fruteriablanco.presentacion.ProductoFrame.TEXTO;
import static com.fruteriablanco.presentacion.ProductoFrame.estilizarBoton;

/** USR-01: formulario para registrar un usuario o modificar sus datos. */
class UsuarioDialog extends JDialog {

    private final Usuario usuarioEditar;
    private final UsuarioService servicio;
    private final int idAdmin;
    private final Runnable alGuardar;

    private final JTextField txtNombre = new JTextField(24);
    private final JTextField txtCorreo = new JTextField(24);
    private final JComboBox<Rol> cboRol = new JComboBox<>();
    private final JPasswordField txtContrasena = new JPasswordField(24);
    private final JPasswordField txtConfirmar = new JPasswordField(24);
    private final JButton btnGuardar = new JButton("Guardar usuario");

    UsuarioDialog(Window owner, Usuario usuario, UsuarioService servicio, int idAdmin, Runnable alGuardar) {
        super(owner, usuario == null ? "Registrar usuario" : "Modificar usuario", ModalityType.APPLICATION_MODAL);
        this.usuarioEditar = usuario;
        this.servicio = servicio;
        this.idAdmin = idAdmin;
        this.alGuardar = alGuardar;

        construir();
        cargarRoles();

        if (usuario != null) {
            txtNombre.setText(usuario.getNombre());
            txtCorreo.setText(usuario.getCorreo());
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void construir() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(FONDO);
        panel.setBorder(new EmptyBorder(18, 22, 18, 22));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 4, 5, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int fila = 0;
        agregar(panel, gc, fila++, "Nombre *", txtNombre);
        agregar(panel, gc, fila++, "Correo *", txtCorreo);
        agregar(panel, gc, fila++, "Rol *", cboRol);
        agregar(panel, gc, fila++, usuarioEditar == null ? "Contraseña *" : "Nueva contraseña", txtContrasena);
        agregar(panel, gc, fila++, "Confirmar contraseña" + (usuarioEditar == null ? " *" : ""), txtConfirmar);

        JLabel nota = new JLabel(usuarioEditar == null
                ? "* Campos obligatorios. La contraseña debe tener al menos 6 caracteres."
                : "Deja la contraseña vacía para conservar la actual.");
        nota.setFont(new Font("SansSerif", Font.PLAIN, 11));
        nota.setForeground(new Color(110, 115, 110));
        gc.gridx = 0;
        gc.gridy = fila++;
        gc.gridwidth = 2;
        panel.add(nota, gc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        JButton btnCancelar = new JButton("Cancelar");
        estilizarBoton(btnCancelar, false);
        estilizarBoton(btnGuardar, true);
        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> guardar());
        botones.add(btnCancelar);
        botones.add(btnGuardar);
        gc.gridy = fila;
        gc.insets = new Insets(14, 4, 0, 4);
        panel.add(botones, gc);

        getRootPane().setDefaultButton(btnGuardar);
        setContentPane(panel);
    }

    private void agregar(JPanel panel, GridBagConstraints gc, int fila, String etiqueta, JComponent campo) {
        JLabel lbl = new JLabel(etiqueta);
        lbl.setForeground(TEXTO);
        gc.gridx = 0;
        gc.gridy = fila;
        gc.gridwidth = 1;
        gc.weightx = 0;
        panel.add(lbl, gc);
        gc.gridx = 1;
        gc.weightx = 1;
        campo.setPreferredSize(new Dimension(260, 28));
        panel.add(campo, gc);
    }

    private void cargarRoles() {
        try {
            List<Rol> roles = servicio.listarRoles();
            for (Rol r : roles) {
                cboRol.addItem(r);
                if (usuarioEditar != null && usuarioEditar.getRol().getIdRol() == r.getIdRol()) {
                    cboRol.setSelectedItem(r);
                }
            }
            if (usuarioEditar == null) {
                cboRol.setSelectedIndex(-1); // obliga a elegir un rol
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los roles: " + ex.getMessage(),
                    "Fruteria Blanco", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardar() {
        Usuario u = new Usuario();
        if (usuarioEditar != null) {
            u.setIdUsuario(usuarioEditar.getIdUsuario());
        }
        u.setNombre(txtNombre.getText());
        u.setCorreo(txtCorreo.getText());
        u.setRol((Rol) cboRol.getSelectedItem());
        String clave = new String(txtContrasena.getPassword());
        String confirmar = new String(txtConfirmar.getPassword());

        btnGuardar.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (usuarioEditar == null) {
                    servicio.crear(u, clave, confirmar, idAdmin);
                } else {
                    servicio.actualizar(u, clave, confirmar, idAdmin);
                }
                return null;
            }

            @Override
            protected void done() {
                btnGuardar.setEnabled(true);
                try {
                    get();
                    JOptionPane.showMessageDialog(UsuarioDialog.this,
                            usuarioEditar == null
                                    ? "Usuario registrado. Ya puede iniciar sesion con su correo y contraseña."
                                    : "Usuario modificado correctamente.",
                            "Fruteria Blanco", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                    alGuardar.run();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable c = ex.getCause();
                    String msg = c instanceof IllegalArgumentException
                            ? c.getMessage()
                            : "No se pudo guardar el usuario: " + c.getMessage();
                    JOptionPane.showMessageDialog(UsuarioDialog.this, msg, "Fruteria Blanco",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }
}
