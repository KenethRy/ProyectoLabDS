package com.fruteriablanco.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
        destino.setVisible(true);
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

        JButton boton = new JButton("Volver (Ctrl+B)");
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
