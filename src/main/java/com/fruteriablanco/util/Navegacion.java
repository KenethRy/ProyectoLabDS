package com.fruteriablanco.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Navegacion entre el menu principal y sus ventanas, y atajos de teclado. */
public final class Navegacion {

    private Navegacion() {
    }

    /** Abre una ventana, oculta el menu y lo vuelve a mostrar cuando la ventana se cierra. */
    public static void abrir(JFrame menu, JFrame destino) {
        menu.setVisible(false);
        destino.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                menu.setVisible(true);
                menu.toFront();
            }
        });
        agregarBarraSuperior(destino);
        destino.setVisible(true);
    }

    /**
     * Pone arriba de la ventana una barra verde con la flecha "Volver al menu" y el nombre de la
     * pantalla. El boton Volver original de cada pantalla se oculta (sigue funcionando con
     * Ctrl+B y la barra lo acciona, asi la confirmacion de VentaFrame se conserva).
     */
    private static void agregarBarraSuperior(JFrame ventana) {
        List<JButton> originales = new ArrayList<>();
        buscarBotonesVolver(ventana.getContentPane(), originales);
        if (originales.isEmpty()) {
            return;
        }
        JButton original = originales.get(0);
        originales.forEach(b -> b.setVisible(false));

        Color verde = new Color(46, 125, 82);
        JButton flecha = new JButton("\u2190  Volver al men\u00fa");
        flecha.setBackground(Color.WHITE);
        flecha.setForeground(verde);
        flecha.setFont(new Font("SansSerif", Font.BOLD, 13));
        flecha.setFocusPainted(false);
        flecha.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        flecha.setToolTipText("Volver al men\u00fa principal (Ctrl+B)");
        flecha.addActionListener(e -> original.doClick());

        String nombre = ventana.getTitle().replace(" - Fruter\u00eda Blanco", "");
        JLabel titulo = new JLabel(nombre, SwingConstants.RIGHT);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 15));
        titulo.setForeground(Color.WHITE);

        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setBackground(verde);
        barra.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 18));
        barra.add(flecha, BorderLayout.WEST);
        barra.add(titulo, BorderLayout.CENTER);

        Container contenido = ventana.getContentPane();
        Dimension tamano = ventana.getSize();
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(barra, BorderLayout.NORTH);
        raiz.add(contenido, BorderLayout.CENTER);
        ventana.setContentPane(raiz);
        ventana.setSize(tamano.width, tamano.height + barra.getPreferredSize().height);
        ventana.setLocationRelativeTo(null);
    }

    private static void buscarBotonesVolver(Container c, List<JButton> encontrados) {
        for (Component hijo : c.getComponents()) {
            if (hijo instanceof JButton b && Boolean.TRUE.equals(b.getClientProperty("esVolver"))) {
                encontrados.add(b);
            } else if (hijo instanceof Container interior) {
                buscarBotonesVolver(interior, encontrados);
            }
        }
    }

    /** Boton "Volver" que tambien responde a Ctrl+B. */
    public static JButton botonVolver(JFrame frame) {
        return botonVolver(frame, () -> true);
    }

    /** Igual que el anterior, pero puedeSalir permite confirmar antes de cerrar. */
    public static JButton botonVolver(JFrame frame, BooleanSupplier puedeSalir) {
        Runnable volver = () -> {
            if (puedeSalir.getAsBoolean()) {
                frame.dispose();
            }
        };

        JButton boton = new JButton("\u2190 Volver (Ctrl+B)");
        boton.putClientProperty("esVolver", true);
        boton.setContentAreaFilled(false);
        boton.setOpaque(true);
        boton.setBorderPainted(false);
        boton.setBackground(new Color(230, 230, 230));
        boton.setForeground(new Color(60, 60, 60));
        boton.setFont(new Font("SansSerif", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setMargin(new Insets(6, 14, 6, 14));
        boton.addActionListener(e -> volver.run());

        agregarAtajo(frame.getRootPane(), KeyEvent.VK_B, "volver", volver);
        return boton;
    }

    /** Registra Ctrl+tecla en toda la ventana, aunque el foco este en un campo de texto. */
    public static void agregarAtajo(JRootPane raiz, int tecla, String nombre, Runnable accion) {
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(tecla, InputEvent_CTRL()), nombre);
        raiz.getActionMap().put(nombre, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                accion.run();
            }
        });
    }

    private static int InputEvent_CTRL() {
        return java.awt.event.InputEvent.CTRL_DOWN_MASK;
    }
}
