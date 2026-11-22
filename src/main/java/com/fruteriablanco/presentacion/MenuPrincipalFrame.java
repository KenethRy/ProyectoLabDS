package com.fruteriablanco.presentacion;

import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.util.Navegacion;
import com.fruteriablanco.util.SesionActual;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

/**
 * Menu principal posterior al inicio de sesion.
 * Atajos: Ctrl+1 Productos, Ctrl+2 Usuarios, Ctrl+3 Ventas, Ctrl+4 Alertas, Ctrl+5 Consultar ventas, Ctrl+6 Respaldo (Administrador).
 * Al abrir una ventana el menu se oculta y reaparece al volver (boton Volver o Ctrl+B).
 */
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

        gc.insets = new Insets(0, 0, 8, 0);

        if (usuario.esAdministrador()) {
            gc.gridy = 3;
            panel.add(opcion("Registrar y administrar productos", 1, KeyEvent.VK_1, ProductoFrame::new), gc);

            // USR-01: registrar y administrar usuarios (solo Administrador)
            gc.gridy = 4;
            panel.add(opcion("Registrar y administrar usuarios", 2, KeyEvent.VK_2, UsuarioFrame::new), gc);

            // BD-01: respaldo y restauracion (solo Administrador)
            gc.gridy = 6;
            panel.add(opcion("Respaldo y restauración", 6, KeyEvent.VK_6, RespaldoFrame::new), gc);
        } else {
            JLabel aviso = new JLabel("La administración de productos esta disponible para el rol Administrador.", SwingConstants.CENTER);
            aviso.setFont(new Font("SansSerif", Font.PLAIN, 13));
            aviso.setForeground(new Color(120, 90, 40));
            gc.gridy = 3;
            panel.add(aviso, gc);
        }

        // VEN-01: registrar ventas (Administrador y Empleado)
        boolean puedeVender = usuario.esAdministrador()
                || "Empleado".equalsIgnoreCase(usuario.getRol().getNombre());
        if (puedeVender) {
            gc.gridy = 5;
            panel.add(opcion("Registrar venta", 3, KeyEvent.VK_3, VentaFrame::new), gc);
        }

        // BUS-02: consultar ventas (todos los roles)
        gc.gridy = 7;
        panel.add(opcion("Consultar ventas", 5, KeyEvent.VK_5, ConsultaVentasFrame::new), gc);

        // ALE: alertas de inventario
        gc.gridy = 8;
        panel.add(opcion("Alertas de inventario", 4, KeyEvent.VK_4,
                () -> new AlertasFrame(usuario.getIdUsuario())), gc);

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
        gc.gridy = 9;
        gc.insets = new Insets(8, 0, 0, 0);
        panel.add(btnSalir, gc);

        setContentPane(panel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(410, 250));
        pack();
        setLocationRelativeTo(null);
    }

    /** Crea el boton de una opcion y registra su atajo Ctrl+numero. */
    private JButton opcion(String texto, int numero, int tecla, Supplier<JFrame> ventana) {
        JButton boton = new JButton(texto + "  (Ctrl+" + numero + ")");
        boton.setFont(new Font("SansSerif", Font.BOLD, 14));
        boton.setContentAreaFilled(false);
        boton.setOpaque(true);
        boton.setBorderPainted(false);
        boton.setBackground(VERDE);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Runnable abrir = () -> Navegacion.abrir(this, ventana.get());
        boton.addActionListener(e -> abrir.run());
        Navegacion.agregarAtajo(getRootPane(), tecla, "opcion" + numero, abrir);
        return boton;
    }
}
