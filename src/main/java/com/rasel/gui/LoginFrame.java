package com.rasel.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField; // NEW
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.rasel.client.ClientInterface;
import com.rasel.common.Credentials;

/**
 * Login UI wired to subscriber API with friendly loading and status feedback.
 */
public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);

    // NEW: persistent buttons and status widgets
    private JButton signInBtn;
    private JButton signUpBtn;
    private final JLabel statusLabel = new JLabel(" "); // space keeps layout height
    private final JProgressBar progress = new JProgressBar(); // indeterminate spinner

    private final static ClientInterface client = GuiClient.getClient();

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

        // NEW: buttons as fields to toggle during loading
        signInBtn = primaryButton("Sign in", e -> signIn());
        signUpBtn = subtleButton("Create account", e -> createAccount());

        // NEW: progress + status
        progress.setIndeterminate(true);
        progress.setVisible(false);
        progress.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        statusLabel.setForeground(Theme.MUTED);

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
        card.add(signInBtn, c);

        c.gridy++;
        card.add(signUpBtn, c);

        // NEW: status row (progress + text)
        JPanel statusRow = new JPanel(new GridBagLayout());
        statusRow.setOpaque(false);
        GridBagConstraints sc = new GridBagConstraints();
        sc.insets = new Insets(4, 0, 0, 0);
        sc.gridx = 0; sc.gridy = 0; sc.anchor = GridBagConstraints.WEST;
        statusRow.add(progress, sc);
        sc.gridx = 1; sc.weightx = 1.0; sc.fill = GridBagConstraints.HORIZONTAL; sc.insets = new Insets(4, 8, 0, 0);
        statusRow.add(statusLabel, sc);

        c.gridy++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        card.add(statusRow, c);

        GridBagConstraints root = new GridBagConstraints();
        root.gridx = 0;
        root.gridy = 0;
        root.anchor = GridBagConstraints.CENTER;
        add(card, root);

        Theme.styleRoot(this.getContentPane());
        setMinimumSize(new Dimension(480, 340));
        setLocationRelativeTo(null);

        // NEW: Enter submits sign-in by default
        getRootPane().setDefaultButton(signInBtn);
    }

    private void signIn() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password.");
            return;
        }
        Credentials credentials = new Credentials(username, password);

        // Subscribe BEFORE sending the request to avoid races with fast responses
        final AtomicBoolean done = new AtomicBoolean(false);
        final AutoCloseable[] subs = new AutoCloseable[2];

        onAuthStart(false);

        subs[0] = client.onAuthSuccess(resp -> {
            if (!done.compareAndSet(false, true)) return; // first event only
            closeSubsQuietly(subs);
            SwingUtilities.invokeLater(() -> {
                showSuccess("Signed in successfully.");
                openChatFrame();
            });
        });

        subs[1] = client.onAuthFailure(resp -> {
            if (!done.compareAndSet(false, true)) return;
            closeSubsQuietly(subs);
            String msg = (resp != null && resp.getData() != null && !resp.getData().isBlank())
                    ? resp.getData()
                    : "Authentication failed. Please check your username and password.";
            SwingUtilities.invokeLater(() -> {
                showError(msg);
                onAuthEnd();
            });
        });

        client.authenticate(credentials);
    }

    // NEW: create account using the same fields (fire-and-forget signup + auth subscribers)
    private void createAccount() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password.");
            return;
        }
        Credentials credentials = new Credentials(username, password);

        final AtomicBoolean done = new AtomicBoolean(false);
        final AutoCloseable[] subs = new AutoCloseable[2];

        onAuthStart(true);

        subs[0] = client.onAuthSuccess(resp -> {
            if (!done.compareAndSet(false, true)) return;
            closeSubsQuietly(subs);
            SwingUtilities.invokeLater(() -> {
                showSuccess("Account created. You are now signed in.");
                openChatFrame();
            });
        });

        subs[1] = client.onAuthFailure(resp -> {
            if (!done.compareAndSet(false, true)) return;
            closeSubsQuietly(subs);
            String msg = (resp != null && resp.getData() != null && !resp.getData().isBlank())
                    ? resp.getData()
                    : "Signup failed. Try a different username.";
            SwingUtilities.invokeLater(() -> {
                showError(msg);
                onAuthEnd();
            });
        });

        client.signup(credentials);
    }

    private void openChatFrame() {
        // Reset UI (in case ChatFrame closes back to login later)
        onAuthEnd();
        dispose();
        new ChatFrame(
                usernameField.getText().trim().isEmpty()
                        ? "guest"
                        : usernameField.getText().trim())
                .setVisible(true);
    }

    // --- UI helpers: loading + status ---

    private void onAuthStart(boolean signup) {
        setBusy(true);
        statusLabel.setForeground(Theme.MUTED);
        statusLabel.setText(signup ? "Creating account..." : "Signing in...");
        signInBtn.setText(signup ? "Sign in" : "Signing in...");
        signUpBtn.setText(signup ? "Creating account..." : "Create account");
    }

    private void onAuthEnd() {
        setBusy(false);
        statusLabel.setForeground(Theme.MUTED);
        statusLabel.setText(" ");
        signInBtn.setText("Sign in");
        signUpBtn.setText("Create account");
    }

    private void setBusy(boolean busy) {
        usernameField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        signInBtn.setEnabled(!busy);
        signUpBtn.setEnabled(!busy);
        progress.setVisible(busy);
        progress.setIndeterminate(busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void showError(String msg) {
        statusLabel.setForeground(new Color(204, 68, 68)); // subtle red
        statusLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        statusLabel.setForeground(Theme.ACCENT);
        statusLabel.setText(msg);
    }

    private static void closeSubsQuietly(AutoCloseable[] subs) {
        if (subs == null) return;
        for (AutoCloseable s : subs) {
            try { if (s != null) s.close(); } catch (Exception ignored) {}
        }
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

    private static JButton primaryButton(String text, java.awt.event.ActionListener l) {
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

    private static JButton subtleButton(String text, java.awt.event.ActionListener l) {
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
