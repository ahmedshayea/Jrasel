package com.rasel.gui;

import javax.swing.*;

/**
 * Entry point for the mock Swing UI (not wired to backend).
 */
public class Launcher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                System.setProperty("sun.java2d.uiScale", "1.0");
                if (GuiClient.getClient() == null) {
                    JOptionPane.showMessageDialog(null, "Cannot connect to server.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                new LoginFrame().setVisible(true);
            } catch (SecurityException ignore) {
            }
        });
    }
}
