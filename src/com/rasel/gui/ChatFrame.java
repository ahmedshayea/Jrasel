package com.rasel.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import com.rasel.client.ClientInterface;
import com.rasel.common.ResponseParser;

/**
 * Mock main chat UI: left column = groups, right column = messages + input.
 */
public class ChatFrame extends JFrame {
    private final DefaultListModel<String> groupsModel = new DefaultListModel<>();
    private final JList<String> groupsList = new JList<>(groupsModel);

    private final DefaultListModel<ChatMessage> messagesModel = new DefaultListModel<>();
    private final JList<ChatMessage> messagesList = new JList<>(messagesModel);

    private final JTextField messageField = new JTextField();
    private final JLabel headerLabel = new JLabel();
    private JButton refreshGroupsButton;

    private final String userDisplayName;

    public ChatFrame(String userDisplayName) {
        super("Rasel • Chat");
        this.userDisplayName = userDisplayName;
        initUI();
        loadGroups(true);
    }

    private void initUI() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout(12, 12));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(12, 12, 12, 12));

        // Left: groups
        JPanel left = new RoundedPanel(14, Theme.PANEL, Theme.BORDER);
        left.setLayout(new BorderLayout());
        JPanel groupsHeader = new JPanel(new BorderLayout());
        groupsHeader.setOpaque(false);
        JLabel groupsTitle = new JLabel("Groups");
        groupsTitle.setBorder(new EmptyBorder(8, 12, 8, 12));
        groupsTitle.setForeground(Theme.MUTED);

        refreshGroupsButton = new JButton(new AbstractAction("Refresh") {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadGroups(false);
            }
        });
        refreshGroupsButton.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        refreshGroupsButton.setFocusPainted(false);
        refreshGroupsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshGroupsButton.setBackground(Theme.BG_ELEVATED);
        refreshGroupsButton.setForeground(Theme.TEXT);
        JPanel rightHeaderButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        rightHeaderButtons.setOpaque(false);
        rightHeaderButtons.add(refreshGroupsButton);

        groupsHeader.add(groupsTitle, BorderLayout.WEST);
        groupsHeader.add(rightHeaderButtons, BorderLayout.EAST);
        left.add(groupsHeader, BorderLayout.NORTH);

        groupsList.setBackground(Theme.BG_ELEVATED);
        groupsList.setForeground(Theme.TEXT);
        groupsList.setSelectionBackground(Theme.SELECTION);
        groupsList.setSelectionForeground(Theme.TEXT);
        groupsList.setBorder(new EmptyBorder(8, 8, 8, 8));
        groupsList.setCellRenderer(new GroupCellRenderer());
        groupsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onGroupSelected(groupsList.getSelectedValue());
            }
        });
        left.add(new JScrollPane(groupsList), BorderLayout.CENTER);

        // Right: messages
        JPanel right = new RoundedPanel(14, Theme.PANEL, Theme.BORDER);
        right.setLayout(new BorderLayout());

        headerLabel.setBorder(new EmptyBorder(8, 12, 8, 12));
        headerLabel.setForeground(Theme.MUTED);
        right.add(headerLabel, BorderLayout.NORTH);

        messagesList.setBackground(Theme.BG_ELEVATED);
        messagesList.setForeground(Theme.TEXT);
        messagesList.setBorder(new EmptyBorder(8, 8, 8, 8));
        messagesList.setCellRenderer(new MessageCellRenderer());
        messagesList.setFixedCellHeight(-1); // allow variable height rows for wrapped bubbles
        messagesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        right.add(new JScrollPane(messagesList), BorderLayout.CENTER);

        JPanel inputBar = new JPanel(new BorderLayout(8, 0));
        inputBar.setBorder(new EmptyBorder(8, 8, 8, 8));
        inputBar.setBackground(Theme.BG_ELEVATED);

        styleTextField(messageField);
        JButton sendBtn = primaryButton("Send", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                appendOwnMessage();
            }
        });
        inputBar.add(messageField, BorderLayout.CENTER);
        inputBar.add(sendBtn, BorderLayout.EAST);
        right.add(inputBar, BorderLayout.SOUTH);

        // Split pane to allow resizing columns
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.28);
        split.setBorder(BorderFactory.createEmptyBorder());
        add(split, BorderLayout.CENTER);

        Theme.styleRoot(this.getContentPane());
    }

    private void onGroupSelected(String group) {
        if (group == null)
            return;
        headerLabel.setText("# " + group + "  —  messages");
        // Mock: show a few placeholder messages for the selected group
        messagesModel.clear();
        messagesModel.addElement(new ChatMessage("system", "Welcome to " + group + ". This is a mock view.", false));
        messagesModel.addElement(new ChatMessage("alice", "Hello!", false));
        messagesModel.addElement(new ChatMessage("bob", "Hi there.", false));
        scrollMessagesToEnd();
    }

    private void loadGroups(boolean selectFirstIfNoneSelected) {
        // Preserve current selection if possible on refresh
        final String prevSelection = groupsList.getSelectedValue();
        setGroupsLoading(true);

        SwingWorker<String[], Void> worker = new SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() throws Exception {
                ClientInterface client = GuiClient.getClient();
                if (client == null || !client.isConnected()) {
                    throw new IllegalStateException("Client is not connected");
                }
                // Send the request, then wait for the response and parse groups
                client.requestListGroups();
                ResponseParser resp = client.getResponse();
                if (resp == null || !resp.isOk()) return new String[0];
                String data = resp.getData();
                if (data == null || data.isBlank() || data.equals("No groups available")) {
                    return new String[0];
                }
                return data.split(",");
            }

            @Override
            protected void done() {
                setGroupsLoading(false);
                try {
                    String[] groups = get();
                    groupsModel.clear();
                    if (groups != null) {
                        for (String g : groups) {
                            if (g != null && !g.trim().isEmpty()) {
                                groupsModel.addElement(g.trim());
                            }
                        }
                    }

                    // Try to restore previous selection if it still exists
                    if (prevSelection != null && groupsModel.contains(prevSelection)) {
                        groupsList.setSelectedValue(prevSelection, true);
                    } else if (selectFirstIfNoneSelected && groupsModel.getSize() > 0) {
                        groupsList.setSelectedIndex(0);
                    } else {
                        groupsList.clearSelection();
                        headerLabel.setText("");
                    }
                } catch (Exception ex) {
                    // On failure, keep whatever is currently displayed, but notify the user
                    JOptionPane.showMessageDialog(ChatFrame.this,
                            "Failed to load groups: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void setGroupsLoading(boolean loading) {
        if (refreshGroupsButton != null) {
            refreshGroupsButton.setEnabled(!loading);
            refreshGroupsButton.setText(loading ? "Loading…" : "Refresh");
        }
    }

    private void appendOwnMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty())
            return;
        messageField.setText("");
        messagesModel.addElement(new ChatMessage(userDisplayName, text, true));
        scrollMessagesToEnd();
    }

    private void scrollMessagesToEnd() {
        int last = messagesModel.getSize() - 1;
        if (last >= 0) {
            messagesList.ensureIndexIsVisible(last);
        }
    }

    private static void styleTextField(JTextField f) {
        f.setBackground(Theme.BG);
        f.setForeground(Theme.TEXT);
        f.setCaretColor(Theme.TEXT);
        f.setSelectionColor(Theme.SELECTION);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
    }

    private static JButton primaryButton(String text, AbstractAction action) {
        JButton b = new JButton(action);
        b.setText(text);
        stylePrimaryButton(b);
        return b;
    }

    private static void stylePrimaryButton(JButton b) {
        b.setBackground(Theme.ACCENT);
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.ACCENT_DARK),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
