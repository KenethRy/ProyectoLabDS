package com.fruteriablanco;

import com.fruteriablanco.presentacion.LoginFrame;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // Si falla, Swing usa su aspecto por defecto
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
