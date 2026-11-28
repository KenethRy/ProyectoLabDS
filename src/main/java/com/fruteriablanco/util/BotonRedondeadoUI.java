package com.fruteriablanco.util;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

/**
 * Aspecto de boton redondeado para TODA la aplicacion (se instala en {@link TemaVisual}).
 * Respeta el color de fondo/letra que cada pantalla ya le pone al boton, y agrega
 * esquinas redondeadas, un efecto al pasar el mouse y otro al presionar.
 */
public class BotonRedondeadoUI extends BasicButtonUI {


    /** Lo llama UIManager por reflexion. */
    public static ComponentUI createUI(JComponent c) {
        return new BotonRedondeadoUI();
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setBorder(BorderFactory.createEmptyBorder(7, 20, 7, 20));
    }

    @Override
    public void update(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color fondo = b.getBackground();
        ButtonModel m = b.getModel();
        if (!b.isEnabled()) {
            fondo = mezclar(fondo, Color.WHITE, 0.55f);
        } else if (m.isPressed()) {
            fondo = mezclar(fondo, Color.BLACK, 0.22f);
        } else if (m.isRollover()) {
            fondo = mezclar(fondo, Color.BLACK, 0.10f);
        }

        // Si la pantalla marco el boton como opaco, Swing no repinta lo de atras y las esquinas
        // redondeadas dejan manchas: se rellena primero con el fondo del contenedor.
        if (c.isOpaque()) {
            g2.setColor(fondoDelContenedor(c));
            g2.fillRect(0, 0, c.getWidth(), c.getHeight());
        }
        int w = c.getWidth() - 1;
        int h = c.getHeight() - 1;
        int ARCO = c.getHeight();   // arco = alto: botones tipo pildora, totalmente redondeados
        g2.setColor(fondo);
        g2.fillRoundRect(0, 0, w, h, ARCO, ARCO);
        // Borde fino: da definicion a los botones claros sobre fondo claro
        g2.setColor(mezclar(fondo, Color.BLACK, 0.18f));
        g2.drawRoundRect(0, 0, w, h, ARCO, ARCO);

        // Anillo cuando el boton tiene el foco del teclado
        if (b.isFocusOwner() && b.isFocusPainted() && b.isEnabled()) {
            g2.setColor(new Color(255, 255, 255, 170));
            g2.drawRoundRect(3, 3, w - 6, h - 6, ARCO - 6, ARCO - 6);
        }
        g2.dispose();
        paint(g, c);
    }

    @Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
        // El anillo de foco se dibuja en update()
    }

    @Override
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics fm = g2.getFontMetrics(b.getFont());
        g2.setFont(b.getFont());
        g2.setColor(b.isEnabled() ? b.getForeground() : new Color(125, 125, 125));
        int mn = b.getDisplayedMnemonicIndex();
        javax.swing.plaf.basic.BasicGraphicsUtils.drawStringUnderlineCharAt(g2, text, mn, textRect.x, textRect.y + fm.getAscent());
    }

    private static Color fondoDelContenedor(JComponent c) {
        Container p = c.getParent();
        while (p != null && !p.isOpaque()) {
            p = p.getParent();
        }
        return p == null ? UIManager.getColor("Panel.background") : p.getBackground();
    }

    private static Color mezclar(Color a, Color b, float t) {
        int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }
}
