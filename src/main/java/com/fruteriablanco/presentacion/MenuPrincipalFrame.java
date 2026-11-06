package com.fruteriablanco.presentacion;

import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.util.SesionActual;

import javax.swing.*;
import java.awt.*;

/**
 * Menú principal PROVISIONAL, para comprobar que el login funciona.
 * UI-01 (Brandon) lo reemplazará por el menú real con ventas, inventario y reportes,
 * y SEG-02 (Eliazin) agregará qué botones ve cada rol.
 */
public class MenuPrincipalFrame extends JFrame {

    public MenuPrincipalFrame() {
        super("Frutería Blanco");

        Usuario u = SesionActual.getUsuario();

        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 40, 24, 40));

        JLabel bienvenida = new JLabel("Bienvenido, " + u.getNombre(), SwingConstants.CENTER);
        bienvenida.setFont(bienvenida.getFont().deriveFont(Font.BOLD, 18f));
        panel.add(bienvenida);
        panel.add(new JLabel("Rol: " + u.getRol().getNombre(), SwingConstants.CENTER));

        JButton btnSalir = new JButton("Cerrar sesión");
        btnSalir.addActionListener(e -> {
            SesionActual.cerrar();
            new LoginFrame().setVisible(true);
            dispose();
        });
        panel.add(btnSalir);

        setContentPane(panel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(340, 0));
        setLocationRelativeTo(null);
    }
}
