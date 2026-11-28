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
        // ---------- Encabezado verde ----------
        JPanel encabezado = new JPanel(new GridLayout(2, 1, 0, 2));
        encabezado.setBackground(new Color(46, 125, 82));
        encabezado.setBorder(BorderFactory.createEmptyBorder(22, 20, 20, 20));
        JLabel titulo = new JLabel("Frutería Blanco", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 26));
        titulo.setForeground(Color.WHITE);
        JLabel subtitulo = new JLabel("Inicia sesión para continuar", SwingConstants.CENTER);
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitulo.setForeground(new Color(220, 238, 226));
        encabezado.add(titulo);
        encabezado.add(subtitulo);

        // ---------- Formulario ----------
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(247, 250, 246));
        panel.setBorder(BorderFactory.createEmptyBorder(22, 70, 26, 70));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        int fila = 0;
        gc.insets = new Insets(0, 0, 4, 0);
        gc.gridy = fila++;
        panel.add(etiqueta("Correo"), gc);
        gc.insets = new Insets(0, 0, 12, 0);
        gc.gridy = fila++;
        panel.add(txtCorreo, gc);
        gc.insets = new Insets(0, 0, 4, 0);
        gc.gridy = fila++;
        panel.add(etiqueta("Contraseña"), gc);
        gc.insets = new Insets(0, 0, 8, 0);
        gc.gridy = fila++;
        panel.add(txtContrasena, gc);

        lblMensaje.setForeground(new Color(180, 30, 30));
        gc.insets = new Insets(0, 0, 6, 0);
        gc.gridy = fila++;
        panel.add(lblMensaje, gc);

        btnIngresar.setBackground(new Color(46, 125, 82));
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnIngresar.setFocusPainted(false);
        btnIngresar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnIngresar.setMargin(new Insets(8, 14, 8, 14));
        gc.insets = new Insets(6, 0, 0, 0);
        gc.gridy = fila++;
        panel.add(btnIngresar, gc);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(encabezado, BorderLayout.NORTH);
        raiz.add(panel, BorderLayout.CENTER);
        setContentPane(raiz);
        getRootPane().setDefaultButton(btnIngresar); // Enter inicia sesión

        btnIngresar.addActionListener(e -> iniciarSesion());
    }

    private JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        return l;
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
