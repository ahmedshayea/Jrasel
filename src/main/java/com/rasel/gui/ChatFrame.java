package com.rasel.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import com.rasel.client.ClientInterface;
import com.rasel.common.ResponseParser;

// NEW: imports for subscribers and JSON parsing
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    // NEW: subscriber handles
    private AutoCloseable groupsSub;
    private AutoCloseable messagesSub;

    // NEW: flag to control selection behavior after refresh
    private volatile boolean selectFirstAfterRefresh = false;

    // NEW: JSON mapper for message payloads
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ChatFrame(String userDisplayName) {
        super("Rasel • Chat");
        this.userDisplayName = userDisplayName;
        initUI();
        subscribeToServerEvents();
        loadGroups(true);
    }

    private void initUI() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // changed to DISPOSE to cleanup subscriptions
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

        // NEW: cleanup subscriptions on window close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                cleanupSubscriptions();
            }
        });
    }

    // NEW: subscribe to GROUPS and MESSAGES resources
    private void subscribeToServerEvents() {
        ClientInterface client = GuiClient.getClient();
        if (client == null) return;

        groupsSub = client.onGroups(resp -> SwingUtilities.invokeLater(() -> handleGroupsResponse(resp)));
        messagesSub = client.onMessages(resp -> SwingUtilities.invokeLater(() -> handleIncomingMessage(resp)));
    }

    private void onGroupSelected(String group) {
        if (group == null)
            return;
        headerLabel.setText("# " + group + "  —  messages");
        // Clear current view; messages for this group will arrive asynchronously
        messagesModel.clear();

        // Optionally, request history if supported by protocol
        ClientInterface client = GuiClient.getClient();
        if (client != null) {
            client.requestMessages(group); // no-op if not implemented
        }
    }

    // NEW: async groups loader using subscriber API
    private void loadGroups(boolean selectFirstIfNoneSelected) {
        this.selectFirstAfterRefresh = selectFirstIfNoneSelected;
        final String prevSelection = groupsList.getSelectedValue();
        putClientInLoadingState(true);

        ClientInterface client = GuiClient.getClient();
        if (client == null || !client.isConnected()) {
            putClientInLoadingState(false);
            JOptionPane.showMessageDialog(
                    this,
                    "Client is not connected",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Send request; UI will be updated when GROUPS response arrives
        client.requestGroups();
    }

    // NEW: handle GROUPS response and update UI
    private void handleGroupsResponse(ResponseParser resp) {
        putClientInLoadingState(false);
        if (resp == null || !resp.isOk()) {
            // Keep current list; optionally show a toast/dialog
            return;
        }

        final String prevSelection = groupsList.getSelectedValue();

        String data = resp.getData();
        String[] groups = parseGroupList(data);

        groupsModel.clear();
        if (groups != null) {
            for (String g : groups) {
                if (g != null && !g.trim().isEmpty()) {
                    groupsModel.addElement(g.trim());
                }
            }
        }

        // Restore previous selection if possible, otherwise select first if requested
        if (prevSelection != null && groupsModel.contains(prevSelection)) {
            groupsList.setSelectedValue(prevSelection, true);
        } else if (selectFirstAfterRefresh && groupsModel.getSize() > 0) {
            groupsList.setSelectedIndex(0);
        } else {
            groupsList.clearSelection();
            headerLabel.setText("");
        }
        // Reset the flag after using it
        selectFirstAfterRefresh = false;
    }

    // NEW: naive parsing supporting CSV, newline-separated, or JSON array
    private String[] parseGroupList(String data) {
        if (data == null || data.isBlank()) return new String[0];
        String trimmed = data.trim();

        // Try JSON array: ["g1","g2"]
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                return MAPPER.readValue(trimmed, String[].class);
            } catch (Exception ignored) {
                // fallback to simple split below
            }
        }

        // Fall back: split on commas or newlines
        String[] parts = trimmed.split("[,\n]");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].replace("[", "").replace("]", "").trim();
        }
        return parts;
    }

    private void setGroupsLoading(boolean loading) {
        if (refreshGroupsButton != null) {
            refreshGroupsButton.setEnabled(!loading);
            refreshGroupsButton.setText(loading ? "Loading…" : "Refresh");
        }
    }

    // NEW: set loading state (wrapper to keep method naming consistent)
    private void putClientInLoadingState(boolean loading) {
        setGroupsLoading(loading);
    }

    private void appendOwnMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty())
            return;
        String group = groupsList.getSelectedValue();
        if (group == null || group.isBlank()) {
            JOptionPane.showMessageDialog(this, "Select a group first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        messageField.setText("");

        // Send to server
        ClientInterface client = GuiClient.getClient();
        if (client != null) {
            client.sendMessage(group, text);
        }

        // Optimistic UI update (shows immediately)
        messagesModel.addElement(new ChatMessage(userDisplayName, text, true));
        scrollMessagesToEnd();
    }

    // NEW: handle incoming message payloads (JSON)
    private void handleIncomingMessage(ResponseParser resp) {
        if (resp == null || !resp.isOk()) return;
        if (!resp.isJson()) return;

        try {
            String json = resp.getData();
            IncomingMessage dto = MAPPER.readValue(json, IncomingMessage.class);
            if (dto == null || dto.group == null) return;

            String selected = groupsList.getSelectedValue();
            if (Objects.equals(dto.group, selected)) {
                String sender = dto.senderName != null ? dto.senderName : "unknown";
                messagesModel.addElement(new ChatMessage(sender, dto.content != null ? dto.content : "", false));
                scrollMessagesToEnd();
            } else {
                // Optionally, indicate unread for other group(s)
                // e.g., badge the group in the list; left as an exercise
            }
        } catch (Exception ignored) {
            // Ignore malformed payloads
        }
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

    // NEW: unsubscribe on dispose
    private void cleanupSubscriptions() {
        try {
            if (groupsSub != null) groupsSub.close();
        } catch (Exception ignored) {}
        try {
            if (messagesSub != null) messagesSub.close();
        } catch (Exception ignored) {}
    }

    // NEW: Minimal DTO for message JSON payloads
    private static final class IncomingMessage {
        public String group;
        public String senderId;   // optional
        public String senderName; // preferred for display
        public String content;
        public String timestamp;
    }
}
