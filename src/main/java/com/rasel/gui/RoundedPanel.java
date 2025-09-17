package com.rasel.gui;

import java.awt.*;
import javax.swing.*;

/**
 * A panel with rounded corners and optional border.
 */
public class RoundedPanel extends JPanel {
    private final int arc;
    private final Color fill;
    private final Color borderColor;

    public RoundedPanel(int arc, Color fill, Color borderColor) {
        super(true);
        this.arc = arc;
        this.fill = fill;
        this.borderColor = borderColor;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
