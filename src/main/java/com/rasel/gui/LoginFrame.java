package com.rasel.gui;

import com.rasel.common.Credentials;
import java.awt.*;
import javax.swing.*;

/**
 * Mock login UI (not wired to backend). On sign in, opens ChatFrame.
 */
public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);

    public LoginFrame() {
        super("Rasel • Login");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setBackground(Theme.BG);
        setLayout(new GridBagLayout());

        RoundedPanel card = new RoundedPanel(
                16,
                Theme.BG_ELEVATED,
                Theme.BORDER);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("Welcome to Rasel");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.UI_FONT_BOLD.deriveFont(18f));

        JLabel userLabel = new JLabel("Username");
        userLabel.setForeground(Theme.MUTED);
        JLabel passLabel = new JLabel("Password");
        passLabel.setForeground(Theme.MUTED);

        styleTextField(usernameField);
        styleTextField(passwordField);

        JButton signIn = primaryButton("Sign in", e -> signIn());
        JButton signUp = subtleButton("Create account", e -> JOptionPane.showMessageDialog(
                this,
                "Mock UI only — no backend wiring yet.",
                "Info",
                JOptionPane.INFORMATION_MESSAGE));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        card.add(title, c);
        c.gridy++;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        card.add(userLabel, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        card.add(usernameField, c);
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        card.add(passLabel, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        card.add(passwordField, c);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        card.add(signIn, c);
        c.gridy++;
        card.add(signUp, c);

        GridBagConstraints root = new GridBagConstraints();
        root.gridx = 0;
        root.gridy = 0;
        root.anchor = GridBagConstraints.CENTER;
        add(card, root);

        Theme.styleRoot(this.getContentPane());
        setMinimumSize(new Dimension(460, 320));
        setLocationRelativeTo(null);
    }

    private void signIn() {
        Credentials credentials = new Credentials(
                usernameField.getText().trim(),
                new String(passwordField.getPassword()));
        try {

            boolean isAuthenticated = GuiClient.getClient().authenticate(credentials);

            if (isAuthenticated) {
                openChatFrame();
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Authentication failed. Please check your username and password.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "An error occurred during authentication: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void openChatFrame() {
        SwingUtilities.invokeLater(() -> {
            dispose();
            new ChatFrame(
                    usernameField.getText().trim().isEmpty()
                            ? "guest"
                            : usernameField.getText().trim())
                    .setVisible(true);
        });
    }

    private static void styleTextField(JTextField f) {
        f.setBackground(Theme.PANEL);
        f.setForeground(Theme.TEXT);
        f.setCaretColor(Theme.TEXT);
        f.setSelectionColor(Theme.SELECTION);
        f.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.BORDER),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }

    private static JButton primaryButton(String text, AbstractAction action) {
        JButton b = new JButton(action);
        b.setText(text);
        stylePrimaryButton(b);
        return b;
    }

    private static JButton primaryButton(
            String text,
            java.awt.event.ActionListener l) {
        JButton b = new JButton(text);
        stylePrimaryButton(b);
        b.addActionListener(l);
        return b;
    }

    private static void stylePrimaryButton(JButton b) {
        b.setBackground(Theme.ACCENT);
        b.setForeground(Color.WHITE);
        b.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.ACCENT_DARK),
                        BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private static JButton subtleButton(
            String text,
            java.awt.event.ActionListener l) {
        JButton b = new JButton(text);
        b.setBackground(Theme.BG_ELEVATED);
        b.setForeground(Theme.MUTED);
        b.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.BORDER),
                        BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        b.setFocusPainted(false);
        b.addActionListener(l);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
