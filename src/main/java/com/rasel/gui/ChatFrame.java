package com.rasel.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import com.rasel.client.ClientInterface;
import com.rasel.common.ResponseParser;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.rasel.server.logging.Log;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class ChatFrame extends JFrame {
    private final DefaultListModel<String> groupsModel = new DefaultListModel<>();
    private final JList<String> groupsList = new JList<>(groupsModel);

    private final DefaultListModel<ChatMessage> messagesModel = new DefaultListModel<>();
    private final JList<ChatMessage> messagesList = new JList<>(messagesModel);
    // Persist messages per group so switching groups keeps prior chat history (session-scoped)
    private final Map<String, List<ChatMessage>> messagesByGroup = new HashMap<>();

    private final JTextField messageField = new JTextField();
    private final JLabel headerLabel = new JLabel();
    private JButton refreshGroupsButton;
    private JButton newGroupButton;
    private JButton addUserButton;
    private JButton showUsersButton;
    private JButton allUsersButton;

    private final String userDisplayName;

    // NEW: subscriber handles
    private AutoCloseable groupsSub;
    private AutoCloseable messagesSub;

    // NEW: flag to control selection behavior after refresh
    private volatile boolean selectFirstAfterRefresh = false;
    // NEW: optional name to select when next groups list arrives (e.g., after create)
    private volatile String pendingSelectGroup = null;
    // NEW: loading watchdog to avoid UI stuck
    private javax.swing.Timer groupsLoadingTimer;

    // NEW: JSON mapper for payloads (ignore unknown fields for forward-compat)
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
                // Keep current selection and just reload
                loadGroups(false);
            }
        });
        refreshGroupsButton.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        refreshGroupsButton.setFocusPainted(false);
        refreshGroupsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshGroupsButton.setBackground(Theme.BG_ELEVATED);
        refreshGroupsButton.setForeground(Theme.TEXT);

        newGroupButton = new JButton(new AbstractAction("New Group") {
            @Override
            public void actionPerformed(ActionEvent e) {
                createNewGroup();
            }
        });
        newGroupButton.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        newGroupButton.setFocusPainted(false);
        newGroupButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        newGroupButton.setBackground(Theme.BG_ELEVATED);
        newGroupButton.setForeground(Theme.TEXT);

        addUserButton = new JButton(new AbstractAction("Add User") {
            @Override
            public void actionPerformed(ActionEvent e) {
                addUserToGroup();
            }
        });
        addUserButton.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        addUserButton.setFocusPainted(false);
        addUserButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addUserButton.setBackground(Theme.BG_ELEVATED);
        addUserButton.setForeground(Theme.TEXT);

        showUsersButton = new JButton(new AbstractAction("Users") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showUsersDialog();
            }
        });
        showUsersButton.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        showUsersButton.setFocusPainted(false);
        showUsersButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showUsersButton.setBackground(Theme.BG_ELEVATED);
        showUsersButton.setForeground(Theme.TEXT);
        allUsersButton = new JButton(new AbstractAction("All Users") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAllUsersDialog();
            }
        });
        allUsersButton.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        allUsersButton.setFocusPainted(false);
        allUsersButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        allUsersButton.setBackground(Theme.BG_ELEVATED);
        allUsersButton.setForeground(Theme.TEXT);
        JPanel rightHeaderButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        rightHeaderButtons.setOpaque(false);
    rightHeaderButtons.add(refreshGroupsButton);
    rightHeaderButtons.add(newGroupButton);
    rightHeaderButtons.add(addUserButton);
    rightHeaderButtons.add(showUsersButton);
    rightHeaderButtons.add(allUsersButton);

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

    // Allow pressing Enter to send the message
    getRootPane().setDefaultButton(sendBtn);

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
        if (client == null)
            return;

        groupsSub = client.onGroups(resp -> {
            // Log arrival on background callback thread
            try {
                int len = resp != null && resp.getData() != null ? resp.getData().length() : -1;
                Log.info("UI: onGroups received ok=%s len=%d", (resp != null && resp.isOk()), len);
            } catch (Exception ignored) {}
            SwingUtilities.invokeLater(() -> handleGroupsResponse(resp));
        });
        messagesSub = client.onMessages(resp -> SwingUtilities.invokeLater(() -> handleIncomingMessage(resp)));
    }

    private void onGroupSelected(String group) {
        if (group == null || group.isBlank()) {
            headerLabel.setText("");
            messagesModel.clear();
            return;
        }
        headerLabel.setText("# " + group + "  —  messages");
        // Load any cached messages for this group
        reloadMessagesViewFor(group);

        // Optionally, request history if supported by protocol (will merge when arrives)
        ClientInterface client = GuiClient.getClient();
        if (client != null) {
            client.requestMessages(group); // no-op if not implemented
        }
    }

    // Load messages from the store into the UI for the given group
    private void reloadMessagesViewFor(String group) {
        messagesModel.clear();
        List<ChatMessage> list = messagesByGroup.get(group);
        if (list != null && !list.isEmpty()) {
            for (ChatMessage m : list) {
                if (m != null) messagesModel.addElement(m);
            }
            scrollMessagesToEnd();
        }
    }

    // NEW: async groups loader using subscriber API
    private void loadGroups(boolean selectFirstIfNoneSelected) {
        this.selectFirstAfterRefresh = selectFirstIfNoneSelected;
    // previous selection is restored in handleGroupsResponse()
        putClientInLoadingState(true);

        Log.info("UI: loadGroups(selectFirst=%s)", selectFirstIfNoneSelected);

        // Start a watchdog to reset loading if no response arrives in time (3s)
        if (groupsLoadingTimer != null && groupsLoadingTimer.isRunning()) {
            groupsLoadingTimer.stop();
        }
        groupsLoadingTimer = new javax.swing.Timer(3000, e -> {
            // Timeout reached; re-enable controls to avoid stuck UI
            putClientInLoadingState(false);
            Log.info("UI: loadGroups watchdog timeout -> controls re-enabled");
        });
        groupsLoadingTimer.setRepeats(false);
        groupsLoadingTimer.start();

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
        Log.info("UI: requestGroups() sent");
    }

    // NEW: handle GROUPS response and update UI
    private void handleGroupsResponse(ResponseParser resp) {
        // Any groups response should cancel the loading watchdog
        if (groupsLoadingTimer != null) {
            groupsLoadingTimer.stop();
            groupsLoadingTimer = null;
        }
        putClientInLoadingState(false);
        try {
            int len = resp != null && resp.getData() != null ? resp.getData().length() : -1;
            Log.info("UI: handleGroupsResponse ok=%s len=%d", (resp != null && resp.isOk()), len);
        } catch (Exception ignored) {}
        if (resp == null || !resp.isOk()) {
            // Keep current list; optionally show a toast/dialog
            return;
        }

        final String prevSelection = groupsList.getSelectedValue();

        String data = resp.getData();
        try {
            String sample = data != null ? (data.length() > 120 ? data.substring(0, 120) + "…" : data) : "null";
            Log.info("UI: groups payload sample=%s", sample);
        } catch (Exception ignored) {}

        String[] groups = parseGroupList(data);
        Log.info("UI: parsed groups count=%d", groups != null ? groups.length : 0);

        boolean payloadLooksEmpty = data != null && data.trim().equals("[]");
        if (groups == null) groups = new String[0];
        // Safety: if parse yielded 0 but payload isn't an explicit empty list and we currently
        // show some groups, keep the current UI instead of wiping it.
        if (groups.length == 0 && !payloadLooksEmpty && groupsModel.getSize() > 0) {
            Log.info("UI: preserving existing groups UI (non-empty payload but 0 parsed)");
            // keep selection state, do not clear model
        } else {
            groupsModel.clear();
            for (String g : groups) {
                if (g != null && !g.trim().isEmpty()) {
                    groupsModel.addElement(g.trim());
                }
            }
        }

        // Preferred: select explicitly requested group (e.g., just created)
        if (pendingSelectGroup != null && groupsModel.contains(pendingSelectGroup)) {
            groupsList.setSelectedValue(pendingSelectGroup, true);
            pendingSelectGroup = null;
        }
        // Restore previous selection if possible, otherwise select first if requested
        else if (prevSelection != null && groupsModel.contains(prevSelection)) {
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

    // NEW: parsing supporting server JSON array of group objects, JSON array of strings, or plain text
    private String[] parseGroupList(String data) {
        if (data == null || data.isBlank())
            return new String[0];
        String trimmed = data.trim();

        // Try JSON array of objects (from GroupSerializer)
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                java.util.List<RawGroup> groups = MAPPER.readValue(
                        trimmed,
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<RawGroup>>() {});
                return groups.stream()
                        .map(g -> g != null ? g.name : null)
                        .filter(n -> n != null && !n.isBlank())
                        .toArray(String[]::new);
            } catch (com.fasterxml.jackson.core.JsonProcessingException ignoredObjects) {
                Log.info("UI: group parse -> not object array; trying string array");
                // Try JSON array of strings next
                try {
                    return MAPPER.readValue(trimmed, String[].class);
                } catch (com.fasterxml.jackson.core.JsonProcessingException ignoredStrings) {
                    Log.info("UI: group parse -> not string array; trying map fallback");
                    // Map-based fallback: extract `name` keys from arbitrary object arrays
                    try {
                        java.util.List<java.util.Map<String,Object>> list = MAPPER.readValue(
                                trimmed,
                                new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String,Object>>>() {});
                        java.util.List<String> names = new java.util.ArrayList<>();
                        if (list != null) {
                            for (java.util.Map<String,Object> m : list) {
                                if (m == null) continue;
                                Object n = m.get("name");
                                if (n instanceof String s && !s.isBlank()) names.add(s.trim());
                            }
                        }
                        if (!names.isEmpty()) return names.toArray(String[]::new);
                    } catch (Exception ignoredMaps) {
                        // fall through
                    }
                    Log.info("UI: group parse -> map fallback failed; fallback to split");
                    // fall through to simple split
                }
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
        if (newGroupButton != null) newGroupButton.setEnabled(!loading);
        if (addUserButton != null) addUserButton.setEnabled(!loading);
        if (showUsersButton != null) showUsersButton.setEnabled(!loading);
    if (allUsersButton != null) allUsersButton.setEnabled(!loading);
    }

    // NEW: set loading state (wrapper to keep method naming consistent)
    private void putClientInLoadingState(boolean loading) {
        setGroupsLoading(loading);
    }

    // NEW: create group via input dialog
    private void createNewGroup() {
        ClientInterface client = GuiClient.getClient();
        if (client == null || !client.isConnected()) {
            JOptionPane.showMessageDialog(this, "Client is not connected", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String name = JOptionPane.showInputDialog(this, "New group name:", "Create group", JOptionPane.PLAIN_MESSAGE);
        if (name == null) return; // cancelled
        name = name.trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Group name cannot be empty.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Remember to select it when list arrives
        pendingSelectGroup = name;
        Log.info("UI: createNewGroup(%s)", name);
        client.requestCreateGroup(name);
        // After creating, refresh groups and select first (likely the new one if server includes it)
        loadGroups(false);
    }

    // NEW: add user to selected or chosen group
    private void addUserToGroup() {
        ClientInterface client = GuiClient.getClient();
        if (client == null || !client.isConnected()) {
            JOptionPane.showMessageDialog(this, "Client is not connected", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String group = groupsList.getSelectedValue();
        if (group == null || group.isBlank()) {
            group = chooseGroupFromModel("Select a group:");
            if (group == null) return;
        }

        String username = JOptionPane.showInputDialog(this, "Username to add:", "Add user to group", JOptionPane.PLAIN_MESSAGE);
        if (username == null) return; // cancelled
        username = username.trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be empty.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        client.requestAddUserToGroup(group, username);
        JOptionPane.showMessageDialog(this, "Add request sent.", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    // NEW: show users of a group in a modal dialog
    private void showUsersDialog() {
        ClientInterface client = GuiClient.getClient();
        if (client == null || !client.isConnected()) {
            JOptionPane.showMessageDialog(this, "Client is not connected", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String group = groupsList.getSelectedValue();
        if (group == null || group.isBlank()) {
            group = chooseGroupFromModel("Choose a group to list users:");
            if (group == null) return;
        }
        final String selectedGroup = group;

        final JDialog dlg = new JDialog(this, "Users in " + selectedGroup, true);
        DefaultListModel<String> usersModel = new DefaultListModel<>();
        JList<String> usersList = new JList<>(usersModel);
        usersList.setBackground(Theme.BG_ELEVATED);
        usersList.setForeground(Theme.TEXT);
        usersModel.addElement("Loading…");
        dlg.getContentPane().setLayout(new BorderLayout());
        dlg.getContentPane().add(new JScrollPane(usersList), BorderLayout.CENTER);
        JButton close = new JButton("Close");
        close.addActionListener(e -> dlg.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        south.add(close);
        dlg.getContentPane().add(south, BorderLayout.SOUTH);
        dlg.setSize(360, 420);
        dlg.setLocationRelativeTo(this);

        final AutoCloseable[] sub = new AutoCloseable[1];
        sub[0] = client.onUsers(resp -> SwingUtilities.invokeLater(() -> {
            if (resp == null || !resp.isOk() || !resp.isJson()) return;
            String grp = resp.getGroup();
            if (grp == null || !grp.equals(selectedGroup)) return; // ignore other groups
            try {
                String data = resp.getData();
                java.util.List<RawUser> users = MAPPER.readValue(
                        data,
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<RawUser>>() {});
                usersModel.clear();
                if (users == null || users.isEmpty()) {
                    usersModel.addElement("(none)");
                } else {
                    for (RawUser u : users) {
                        if (u != null && u.username != null && !u.username.isBlank()) {
                            usersModel.addElement(u.username);
                        }
                    }
                }
            } catch (Exception ignored) {
                usersModel.clear();
                usersModel.addElement("Failed to parse users");
            } finally {
                try { if (sub[0] != null) sub[0].close(); } catch (Exception ignored2) {}
            }
        }));

        // Clean up subscription if dialog is closed prematurely
        dlg.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                try { if (sub[0] != null) sub[0].close(); } catch (Exception ignored) {}
            }
        });

        // Send the request and show dialog
        client.requestUsers(selectedGroup);
        dlg.setVisible(true);
    }

    // NEW: show all users in the system, with optional quick add-to-group
    private void showAllUsersDialog() {
        ClientInterface client = GuiClient.getClient();
        if (client == null || !client.isConnected()) {
            JOptionPane.showMessageDialog(this, "Client is not connected", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final JDialog dlg = new JDialog(this, "All Users", true);
        DefaultListModel<String> usersModel = new DefaultListModel<>();
        JList<String> usersList = new JList<>(usersModel);
        usersList.setBackground(Theme.BG_ELEVATED);
        usersList.setForeground(Theme.TEXT);
        usersModel.addElement("Loading…");

        // Controls for optional add-to-group
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        JButton addBtn = new JButton("Add to Group…");
        JButton close = new JButton("Close");
        close.addActionListener(e -> dlg.dispose());
        south.add(addBtn);
        south.add(close);

        dlg.getContentPane().setLayout(new BorderLayout());
        dlg.getContentPane().add(new JScrollPane(usersList), BorderLayout.CENTER);
        dlg.getContentPane().add(south, BorderLayout.SOUTH);
        dlg.setSize(400, 480);
        dlg.setLocationRelativeTo(this);

        // Subscription: listen for USERS with null/empty group (all users)
        final AutoCloseable[] sub = new AutoCloseable[1];
        sub[0] = client.onUsers(resp -> SwingUtilities.invokeLater(() -> {
            if (resp == null || !resp.isOk() || !resp.isJson()) return;
            // When group is null/blank -> all users
            if (resp.getGroup() != null && !resp.getGroup().isBlank()) return;
            try {
                String data = resp.getData();
                java.util.List<RawUser> users = MAPPER.readValue(
                        data,
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<RawUser>>() {});
                usersModel.clear();
                if (users == null || users.isEmpty()) {
                    usersModel.addElement("(none)");
                } else {
                    for (RawUser u : users) {
                        if (u != null && u.username != null && !u.username.isBlank()) {
                            usersModel.addElement(u.username);
                        }
                    }
                }
            } catch (Exception ignored) {
                usersModel.clear();
                usersModel.addElement("Failed to parse users");
            } finally {
                try { if (sub[0] != null) sub[0].close(); } catch (Exception ignored2) {}
            }
        }));

        // Add-to-group action
        addBtn.addActionListener(e -> {
            String selectedUser = usersList.getSelectedValue();
            if (selectedUser == null || selectedUser.isBlank()) {
                JOptionPane.showMessageDialog(dlg, "Select a user first.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String group = chooseGroupFromModel("Add '" + selectedUser + "' to which group?");
            if (group == null || group.isBlank()) return;
            client.requestAddUserToGroup(group, selectedUser);
            JOptionPane.showMessageDialog(dlg, "Add request sent.", "Info", JOptionPane.INFORMATION_MESSAGE);
        });

        // Clean up subscription if dialog is closed prematurely
        dlg.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                try { if (sub[0] != null) sub[0].close(); } catch (Exception ignored) {}
            }
        });

        // Send request and show dialog
        client.requestUsers(); // no groupName -> all users in system
        dlg.setVisible(true);
    }

    private String chooseGroupFromModel(String title) {
        int n = groupsModel.getSize();
        if (n == 0) {
            JOptionPane.showMessageDialog(this, "No groups available.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        String[] names = new String[n];
        for (int i = 0; i < n; i++) names[i] = groupsModel.getElementAt(i);
        Object selected = JOptionPane.showInputDialog(
                this,
                title,
                "Select group",
                JOptionPane.PLAIN_MESSAGE,
                null,
                names,
                names[0]);
        return selected != null ? selected.toString() : null;
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

        // Optimistic UI update (shows immediately) and persist in store
        ChatMessage msg = new ChatMessage(userDisplayName, text, true);
        messagesByGroup.computeIfAbsent(group, g -> new ArrayList<>()).add(msg);
        messagesModel.addElement(msg);
        scrollMessagesToEnd();
    }

    // NEW: handle incoming message payloads (JSON)
    private void handleIncomingMessage(ResponseParser resp) {
        if (resp == null || !resp.isOk())
            return;
        if (!resp.isJson())
            return;

        try {
            String json = resp.getData();
            String trimmed = json != null ? json.trim() : "";

            // Support both single message object and array of messages
            if (trimmed.startsWith("[")) {
                java.util.List<IncomingMessage> list = MAPPER.readValue(
                        trimmed,
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<IncomingMessage>>() {});
                if (list == null) return;
                for (IncomingMessage dto : list) {
                    handleOneIncoming(dto);
                }
            } else {
                IncomingMessage dto = MAPPER.readValue(trimmed, IncomingMessage.class);
                handleOneIncoming(dto);
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
            // Ignore malformed payloads
        }
    }

    // Merge a single incoming message into store and UI (if current group)
    private void handleOneIncoming(IncomingMessage dto) {
        if (dto == null || dto.group == null) return;
        String sender = dto.senderName != null ? dto.senderName : "unknown";
        String content = dto.content != null ? dto.content : "";

        // If server echoes our own message (ideally it doesn't), skip storing to avoid duplicates
        if (sender.equalsIgnoreCase(userDisplayName)) {
            return;
        }

        ChatMessage incoming = new ChatMessage(sender, content, false);
        messagesByGroup.computeIfAbsent(dto.group, g -> new ArrayList<>()).add(incoming);

        String selected = groupsList.getSelectedValue();
        if (Objects.equals(dto.group, selected)) {
            messagesModel.addElement(incoming);
            scrollMessagesToEnd();
        } else {
            // Optionally, mark unread for dto.group
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
            if (groupsSub != null)
                groupsSub.close();
        } catch (Exception ignored) {
        }
        try {
            if (messagesSub != null)
                messagesSub.close();
        } catch (Exception ignored) {
        }
    }

    // NEW: Minimal DTO for message JSON payloads
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private static final class IncomingMessage {
        public String group;
        public String senderId; // optional
        public String senderName; // preferred for display
        public String content;
        public String timestamp;
    }

    // NEW: Raw shape for groups JSON (fallback to avoid coupling to server classes)
    @SuppressWarnings("unused")
    private static final class RawUser {
        public String username;
    }
    @SuppressWarnings("unused")
    private static final class RawGroup {
        public String name;
        public RawUser admin;
        public java.util.List<RawUser> members;
        public java.util.List<Object> messages; // ignored
    }
}
