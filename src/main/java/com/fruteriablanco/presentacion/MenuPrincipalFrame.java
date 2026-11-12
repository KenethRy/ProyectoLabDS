package com.fruteriablanco.presentacion;

import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.util.SesionActual;

import javax.swing.*;
import java.awt.*;

/** Menu principal posterior al inicio de sesion. */
public class MenuPrincipalFrame extends JFrame {

    private static final Color VERDE = new Color(46, 125, 82);
    private static final Color FONDO = new Color(247, 250, 246);

    public MenuPrincipalFrame() {
        super("Frutería Blanco");

        Usuario usuario = SesionActual.getUsuario();

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 38, 28, 38));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new Insets(5, 0, 5, 0);

        JLabel titulo = new JLabel("Frutería Blanco", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        titulo.setForeground(VERDE);
        gc.gridy = 0;
        panel.add(titulo, gc);

        JLabel bienvenida = new JLabel("Bienvenido, " + usuario.getNombre(), SwingConstants.CENTER);
        bienvenida.setFont(new Font("SansSerif", Font.BOLD, 16));
        gc.gridy = 1;
        gc.insets = new Insets(12, 0, 3, 0);
        panel.add(bienvenida, gc);

        JLabel rol = new JLabel("Rol: " + usuario.getRol().getNombre(), SwingConstants.CENTER);
        rol.setFont(new Font("SansSerif", Font.PLAIN, 14));
        rol.setForeground(new Color(85, 95, 85));
        gc.gridy = 2;
        gc.insets = new Insets(0, 0, 18, 0);
        panel.add(rol, gc);

        if (usuario.esAdministrador()) {
            JButton btnProductos = new JButton("Registrar y administrar productos");
            btnProductos.setFont(new Font("SansSerif", Font.BOLD, 14));
            btnProductos.setContentAreaFilled(false);
            btnProductos.setOpaque(true);
            btnProductos.setBorderPainted(false);
            btnProductos.setBackground(VERDE);
            btnProductos.setForeground(Color.WHITE);
            btnProductos.setFocusPainted(false);
            btnProductos.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnProductos.addActionListener(e -> new ProductoFrame().setVisible(true));
            gc.gridy = 3;
            gc.insets = new Insets(0, 0, 8, 0);
            panel.add(btnProductos, gc);

            // USR-01: registrar y administrar usuarios (solo Administrador)
            JButton btnUsuarios = new JButton("Registrar y administrar usuarios");
            btnUsuarios.setFont(new Font("SansSerif", Font.BOLD, 14));
            btnUsuarios.setContentAreaFilled(false);
            btnUsuarios.setOpaque(true);
            btnUsuarios.setBorderPainted(false);
            btnUsuarios.setBackground(VERDE);
            btnUsuarios.setForeground(Color.WHITE);
            btnUsuarios.setFocusPainted(false);
            btnUsuarios.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnUsuarios.addActionListener(e -> new UsuarioFrame().setVisible(true));
            gc.gridy = 4;
            panel.add(btnUsuarios, gc);
        } else {
            JLabel aviso = new JLabel("La administración de productos esta disponible para el rol Administrador.", SwingConstants.CENTER);
            aviso.setFont(new Font("SansSerif", Font.PLAIN, 13));
            aviso.setForeground(new Color(120, 90, 40));
            gc.gridy = 3;
            gc.insets = new Insets(0, 0, 8, 0);
            panel.add(aviso, gc);
        }
// VEN-01: registrar ventas (Administrador y Empleado)
        boolean puedeVender = usuario.esAdministrador()
                || "Empleado".equalsIgnoreCase(usuario.getRol().getNombre());
        if (puedeVender) {
            JButton btnVentas = new JButton("Registrar venta");
            btnVentas.setContentAreaFilled(false);
            btnVentas.setOpaque(true);
            btnVentas.setBorderPainted(false);
            btnVentas.setBackground(VERDE);
            btnVentas.setForeground(Color.WHITE);
            btnVentas.setFont(new Font("SansSerif", Font.BOLD, 14));
            btnVentas.setFocusPainted(false);
            btnVentas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnVentas.addActionListener(e -> new VentaFrame().setVisible(true));
            gc.gridy = 5;
            gc.insets = new Insets(0, 0, 8, 0);
            panel.add(btnVentas, gc);
        }
        JButton btnSalir = new JButton("Cerrar sesion");
        btnSalir.setContentAreaFilled(false);
        btnSalir.setOpaque(true);
        btnSalir.setBorderPainted(false);
        btnSalir.setBackground(new Color(230, 230, 230));
        btnSalir.setForeground(new Color(60, 60, 60));
        btnSalir.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnSalir.setFocusPainted(false);
        btnSalir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSalir.addActionListener(e -> {
            SesionActual.cerrar();
            new LoginFrame().setVisible(true);
            dispose();

        });
        gc.gridy = 6;
        gc.insets = new Insets(8, 0, 0, 0);
        panel.add(btnSalir, gc);

        setContentPane(panel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(410, 250));
        pack();
        setLocationRelativeTo(null);
    }
}
