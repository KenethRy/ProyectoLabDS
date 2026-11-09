package com.fruteriablanco.presentacion;

import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.servicio.AuthException;
import com.fruteriablanco.servicio.AuthService;
import com.fruteriablanco.util.SesionActual;

import javax.swing.*;
import java.awt.*;

/**
 * Capa de presentación de SEG-01.
 * Solo lee los campos, llama al servicio y muestra el resultado. No tiene SQL ni reglas.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService = new AuthService();

    private final JTextField txtCorreo = new JTextField(22);
    private final JPasswordField txtContrasena = new JPasswordField(22);
    private final JButton btnIngresar = new JButton("Iniciar sesión");
    private final JLabel lblMensaje = new JLabel(" ");

    public LoginFrame() {
        super("Frutería Blanco - Iniciar sesión");
        construirInterfaz();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(4, 0, 4, 0);

        JLabel titulo = new JLabel("Frutería Blanco", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));
        gc.gridy = 0;
        gc.insets = new Insets(0, 0, 16, 0);
        panel.add(titulo, gc);

        gc.insets = new Insets(4, 0, 4, 0);
        gc.gridy = 1;
        panel.add(new JLabel("Correo"), gc);
        gc.gridy = 2;
        panel.add(txtCorreo, gc);
        gc.gridy = 3;
        panel.add(new JLabel("Contraseña"), gc);
        gc.gridy = 4;
        panel.add(txtContrasena, gc);

        lblMensaje.setForeground(new Color(180, 30, 30));
        gc.gridy = 5;
        panel.add(lblMensaje, gc);

        gc.gridy = 6;
        gc.insets = new Insets(8, 0, 0, 0);
        panel.add(btnIngresar, gc);

        setContentPane(panel);
        getRootPane().setDefaultButton(btnIngresar); // Enter inicia sesión

        btnIngresar.addActionListener(e -> iniciarSesion());
    }

    private void iniciarSesion() {
        String correo = txtCorreo.getText();
        String contrasena = new String(txtContrasena.getPassword());

        btnIngresar.setEnabled(false);
        lblMensaje.setText(" ");

        // SwingWorker: la consulta a la base corre fuera del hilo de la ventana para no congelarla
        new SwingWorker<Usuario, Void>() {
            @Override
            protected Usuario doInBackground() throws Exception {
                return authService.iniciarSesion(correo, contrasena);
            }

            @Override
            protected void done() {
                btnIngresar.setEnabled(true);
                try {
                    Usuario usuario = get();
                    SesionActual.iniciar(usuario);
                    new MenuPrincipalFrame().setVisible(true);
                    dispose();
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable causa = ex.getCause();
                    if (causa instanceof AuthException) {
                        lblMensaje.setText(causa.getMessage());
                    } else {
                        causa.printStackTrace();
                        lblMensaje.setText("Ocurrió un error inesperado.");
                    }
                    txtContrasena.setText("");
                    txtContrasena.requestFocusInWindow();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
