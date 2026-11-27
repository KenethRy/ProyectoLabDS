package com.fruteriablanco.util;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Tema visual de Fruteria Blanco (UI-01). Se llama UNA vez en Main, despues de elegir el
 * Look and Feel, y cambia el aspecto de todas las pantallas: botones redondeados,
 * fondo suave, campos de texto con borde verde, tablas con seleccion verde y tipografia uniforme.
 */
public final class TemaVisual {

    public static final Color VERDE = new Color(46, 125, 82);
    public static final Color VERDE_CLARO = new Color(220, 238, 226);
    public static final Color FONDO = new Color(247, 250, 246);
    public static final Color LINEA = new Color(190, 210, 195);
    public static final Color TEXTO = new Color(40, 50, 42);

    private TemaVisual() {
    }

    private static BufferedImage logoOriginal;
    private static boolean logoIntentado;

    /** Logo de la fruteria (src/main/resources/imagenes/logo.png). Devuelve null si no se encuentra. */
    public static BufferedImage icono() {
        if (!logoIntentado) {
            logoIntentado = true;
            try (java.io.InputStream in = TemaVisual.class.getResourceAsStream("/imagenes/logo.png")) {
                if (in != null) {
                    logoOriginal = javax.imageio.ImageIO.read(in);
                }
            } catch (java.io.IOException ignorada) {
                // sin logo se muestra solo el texto
            }
        }
        return logoOriginal;
    }

    /** Componente que dibuja el logo (fondo transparente) con el alto indicado, bien suavizado. */
    public static JComponent logo(int alto) {
        BufferedImage original = icono();
        if (original == null) {
            JLabel texto = new JLabel("Frutería Blanco", SwingConstants.CENTER);
            texto.setFont(new Font("SansSerif", Font.BOLD, 24));
            texto.setForeground(VERDE);
            return texto;
        }
        int ancho = Math.round(original.getWidth() * (float) alto / original.getHeight());
        BufferedImage ajustada = reducir(original, ancho, alto);
        JComponent c = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.drawImage(ajustada, 0, 0, null);
            }
        };
        Dimension d = new Dimension(ancho, alto);
        c.setPreferredSize(d);
        c.setMinimumSize(d);
        c.setMaximumSize(d);
        c.setOpaque(false);
        return c;
    }

    /** Reduce a mitades sucesivas para que la imagen no se vea dentada. */
    private static BufferedImage reducir(BufferedImage img, int ancho, int alto) {
        BufferedImage actual = img;
        int w = img.getWidth();
        int h = img.getHeight();
        while (w / 2 >= ancho && h / 2 >= alto) {
            w /= 2;
            h /= 2;
            actual = escalar(actual, w, h);
        }
        return escalar(actual, ancho, alto);
    }

    private static BufferedImage escalar(BufferedImage img, int w, int h) {
        BufferedImage salida = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = salida.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(img, 0, 0, w, h, null);
        g2.dispose();
        return salida;
    }

    public static void aplicar() {
        // Botones redondeados en toda la aplicacion
        UIManager.put("ButtonUI", BotonRedondeadoUI.class.getName());
        UIManager.put("Button.background", new Color(226, 236, 228));
        UIManager.put("Button.foreground", TEXTO);

        // Fondos
        UIManager.put("Panel.background", FONDO);
        UIManager.put("OptionPane.background", FONDO);
        UIManager.put("RootPane.background", FONDO);
        UIManager.put("Label.foreground", TEXTO);

        // Campos de texto: borde verde suave con un poco de aire por dentro
        javax.swing.border.Border campo = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINEA, 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8));
        UIManager.put("TextField.border", campo);
        UIManager.put("PasswordField.border", campo);
        UIManager.put("TextField.selectionBackground", VERDE_CLARO);
        UIManager.put("TextField.selectionForeground", TEXTO);

        // Tablas
        UIManager.put("Table.selectionBackground", VERDE_CLARO);
        UIManager.put("Table.selectionForeground", TEXTO);
        UIManager.put("Table.gridColor", new Color(228, 236, 229));
        UIManager.put("Table.alternateRowColor", new Color(250, 252, 250));

        // Una sola tipografia (Segoe UI en Windows) con tamano comodo
        Font base = new Font("Segoe UI", Font.PLAIN, 13);
        if (!"Segoe UI".equals(base.getFamily())) {
            base = new Font("SansSerif", Font.PLAIN, 13);
        }
        List<Object> claves = new ArrayList<>();
        for (Enumeration<Object> e = UIManager.getDefaults().keys(); e.hasMoreElements(); ) {
            claves.add(e.nextElement());
        }
        for (Object clave : claves) {
            if (clave instanceof String s && s.endsWith(".font")) {
                Font actual = UIManager.getFont(s);
                int estilo = actual == null ? Font.PLAIN : actual.getStyle();
                UIManager.put(s, new FontUIResource(base.getFamily(), estilo, 13));
            }
        }
    }
}
