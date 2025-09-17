package com.rasel.gui;

import java.awt.*;

/**
 * Simple theme utilities for a clean, elegant look.
 */
public final class Theme {
    private Theme() {}

    // Light theme with blue accent
    public static final Color BG = new Color(0xF6F8FA);           // App background
    public static final Color BG_ELEVATED = new Color(0xFFFFFF);   // Cards / panels
    public static final Color PANEL = new Color(0xFFFFFF);
    public static final Color TEXT = new Color(0x1F2328);          // Primary text
    public static final Color MUTED = new Color(0x57606A);         // Muted text
    public static final Color ACCENT = new Color(0x0969DA);        // Blue accent
    public static final Color ACCENT_DARK = new Color(0x0550AE);   // Darker accent
    public static final Color SELECTION = new Color(0xDFE5F1);     // Selection
    public static final Color BORDER = new Color(0xD0D7DE);        // Borders
    // Chat-specific soft bubble for other people messages
    public static final Color BUBBLE_OTHER = new Color(0xF2F4F8);

    public static final Font UI_FONT = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font UI_FONT_BOLD = UI_FONT.deriveFont(Font.BOLD);

    public static void styleRoot(Component c) {
        c.setFont(UI_FONT);
        if (c instanceof Container cont) {
            for (Component child : cont.getComponents()) {
                child.setFont(UI_FONT);
                if (child instanceof Container) styleRoot(child);
            }
        }
    }
}
