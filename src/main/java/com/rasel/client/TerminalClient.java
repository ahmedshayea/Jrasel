package com.rasel.client;

import com.rasel.common.Credentials;
import com.rasel.common.ResponseParser;
import java.awt.datatransfer.ClipboardOwner;
import java.util.Scanner;

public class TerminalClient {

    private final Client client;
    private volatile String currentGroup = null;
    private final Scanner scanner = new Scanner(System.in); // shared scanner, not closed (System.in)
    private final Object consoleLock = new Object(); // synchronize mixed output from multiple threads

    // ANSI styling
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String FG_RED = "\u001B[31m";
    private static final String FG_GREEN = "\u001B[32m";
    private static final String FG_YELLOW = "\u001B[33m";
    private static final String FG_BLUE = "\u001B[34m";
    private static final String FG_MAGENTA = "\u001B[35m";
    private static final String FG_CYAN = "\u001B[36m";
    private static final String FG_GRAY = "\u001B[90m";

    private String colorForUser(String name) {
        if (name == null || name.isBlank()) return FG_GRAY;
        int h = Math.abs(name.hashCode());
        return switch (h % 6) {
            case 0 -> FG_GREEN;
            case 1 -> FG_YELLOW;
            case 2 -> FG_BLUE;
            case 3 -> FG_MAGENTA;
            case 4 -> FG_CYAN;
            default -> FG_RED;
        };
    }

    // Commands registry
    private static final String[] COMMANDS = new String[] {
        "/help",
        "/login",
        "/create <group>",
        "/switch <group>",
        "/groups",
        "/users",
        "/users <group>",
        "/add <username>",
        "/quit",
    };

    private void printHelp() {
        System.out.println(BOLD + FG_CYAN + "\nAvailable commands:" + RESET);
        System.out.println(
            "  " +
                BOLD +
                "/help" +
                RESET +
                FG_GRAY +
                "  - Show this help" +
                RESET
        );
        System.out.println(
            "  " +
                BOLD +
                "/login" +
                RESET +
                FG_GRAY +
                " - Re-authenticate" +
                RESET
        );
        System.out.println(
            "  " +
                BOLD +
                "/create <group>" +
                RESET +
                FG_GRAY +
                " - Create and switch to a group" +
                RESET
        );
        System.out.println(
            "  " +
                BOLD +
                "/switch <group>" +
                RESET +
                FG_GRAY +
                " - Switch current group" +
                RESET
        );
        System.out.println(
            "  " +
                BOLD +
                "/groups" +
                RESET +
                FG_GRAY +
                " - List all groups" +
                RESET
        );
        System.out.println(
            "  " +
                BOLD +
                "/users" +
                RESET +
                FG_GRAY +
                " - List all users" +
                RESET
        );
        System.out.println(
            "  " +
                BOLD +
                "/users <group>" +
                RESET +
                FG_GRAY +
                " - List all users in group" +
                RESET
        );
        System.out.println(
            "  " +
                BOLD +
                "/add <username>" +
                RESET +
                FG_GRAY +
                " - Add user to current group (admin only)" +
                RESET
        );
        System.out.println(
            "  " + BOLD + "/quit" + RESET + FG_GRAY + "  - Exit client" + RESET
        );
        System.out.println(
            FG_GRAY +
                "Tip: start a command, e.g. '/cr', to see suggestions." +
                RESET
        );
    }

    private boolean suggestCommands(String input) {
        if (input == null) return false;
        String trimmed = input.trim();
        if (!trimmed.startsWith("/")) return false;

        // exact matches handled elsewhere
        String lower = trimmed.toLowerCase();
        java.util.List<String> matches = new java.util.ArrayList<>();
        for (String cmd : COMMANDS) {
            // Compare only the command keyword part (before space)
            String head = cmd.split(" ")[0].toLowerCase();
            if (head.startsWith(lower)) {
                matches.add(cmd);
            }
        }
        if (matches.isEmpty()) return false;
        System.out.println(FG_GRAY + "Suggestions:" + RESET);
        for (String m : matches) {
            System.out.println("  " + m);
        }
        return true;
    }

    public TerminalClient(Client client) {
        this.client = client;
    }

    public void start() {
        try {
            client.connect();
            System.out.println(
                BOLD + FG_GREEN + "Connected to server." + RESET
            );

            authenticate();
            printHelp();

            new Thread(this::handleReceive).start();
            handleUserInput();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    // Build current prompt string
    private String getPrompt() {
        String promptUser = client.getUsername() != null
            ? client.getUsername()
            : "guest";
        String promptGroup = currentGroup != null ? currentGroup : "(no-group)";
        return (
            FG_GRAY + promptUser + "@" + promptGroup + FG_CYAN + " > " + RESET
        );
    }

    // Print prompt safely (used by both input and receive threads)
    private void printPrompt() {
        synchronized (consoleLock) {
            System.out.print(getPrompt());
            System.out.flush();
        }
    }

    private void authenticate() {
        while (!client.isAuthenticated()) {
            System.out.print(
                FG_CYAN +
                    "Do you want to (1) Login or (2) Signup? Enter 1 or 2: " +
                    RESET
            );
            String choice = scanner.nextLine().trim();
            System.out.print(FG_CYAN + "Username: " + RESET);
            String username = scanner.nextLine();
            System.out.print(FG_CYAN + "Password: " + RESET);
            String password = scanner.nextLine();
            Credentials credentials = new Credentials(username, password);

            try {
                boolean success;
                if (choice.equals("2")) {
                    success = client.signup(credentials);
                    if (success) {
                        System.out.println(
                            FG_GREEN +
                                "✅ Signup successful! You are now logged in." +
                                RESET
                        );
                    } else {
                        System.out.println(
                            FG_RED + "❌ Signup failed. Try again." + RESET
                        );
                    }
                } else {
                    success = client.authenticate(credentials);
                    if (success) {
                        System.out.println(
                            FG_GREEN + "✅ Authenticated!" + RESET
                        );
                    } else {
                        System.out.println(
                            FG_RED +
                                "❌ Authentication failed. Try again." +
                                RESET
                        );
                    }
                }
            } catch (Exception e) {
                System.out.println(
                    FG_RED + "❌ Error during authentication/signup." + RESET
                );
            }
        }
    }

    private void handleReceive() {
        try {
            while (true) {
                ResponseParser response = client.getResponse();
                renderIncoming(response);
            }
        } catch (Exception e) {
            System.out.println(FG_RED + "❌ Error receiving message." + RESET);
        }
    }

    private void handleUserInput() {
        while (true) {
            printPrompt();
            String message = scanner.nextLine().trim();

            if (
                message.equals("/help") ||
                message.equals("/h") ||
                message.equals("?")
            ) {
                printHelp();
                continue;
            }

            if (message.equals("/quit") || message.equals("/exit")) {
                System.out.println(FG_GREEN + "👋 Bye!" + RESET);
                System.exit(0);
            }

            // Show suggestions for partial commands
            if (
                message.startsWith("/") &&
                !(message.startsWith("/create ") ||
                    message.startsWith("/switch ") ||
                    message.startsWith("/add ") ||
                    message.equals("/groups") ||
                    message.startsWith("/users") ||
                    message.equals("/login") ||
                    message.equals("/help") ||
                    message.equals("/h") ||
                    message.equals("?") ||
                    message.equals("/quit") ||
                    message.equals("/exit"))
            ) {
                if (suggestCommands(message)) continue;
            }

            if (message.equals("/login")) {
                client.setAuthenticated(false);
                authenticate();
                continue;
            }
            if (message.startsWith("/create ")) {
                String groupName = message.substring(8).trim();
                if (groupName.isEmpty()) {
                    System.out.println(
                        FG_YELLOW + "⚠️  Usage: /create <group>" + RESET
                    );
                    continue;
                }
                // Non-blocking: send request; response will be printed by receiver thread
                client.requestCreateGroup(groupName);
                // Optimistically switch to this group; server will error if creation fails
                currentGroup = groupName;
                continue;
            }

            if (message.startsWith("/switch ")) {
                String groupName = message.substring(8).trim();
                if (groupName.isEmpty()) {
                    System.out.println(
                        FG_YELLOW + "⚠️  Usage: /switch <group>" + RESET
                    );
                    continue;
                }
                currentGroup = groupName;
                System.out.println(
                    FG_GREEN + "➡️  Switched to group: " + currentGroup + RESET
                );
                continue;
            }

            if (message.equals("/groups")) {
                // Non-blocking: send request; receiver thread will print the list from server
                // response
                client.requestListGroups();
                continue;
            }
            if (message.startsWith("/users")) {
                // check if user provided group name or not
                String groupName = message.substring(6).trim();
                if (groupName.isEmpty()) {
                    client.requestGetUsers();
                    continue;
                }
                client.requestGetUsers(groupName);
            }

            if (message.startsWith("/add ")) {
                if (currentGroup == null || currentGroup.isBlank()) {
                    System.out.println(
                        FG_YELLOW +
                            "⚠️  Select a group first with /switch <group> or /create <group>." +
                            RESET
                    );
                    continue;
                }
                String username = message.substring(5).trim();
                if (username.isEmpty()) {
                    System.out.println(
                        FG_YELLOW + "⚠️  Usage: /add <username>" + RESET
                    );
                    continue;
                }
                client.requestAddUserToGroup(currentGroup, username);
                continue;
            }

            if (!message.isEmpty() && client.isAuthenticated()) {
                if (currentGroup == null || currentGroup.isBlank()) {
                    System.out.println(
                        FG_YELLOW +
                            "⚠️  No group selected. Use /create <group> or /switch <group> first." +
                            RESET
                    );
                    continue;
                }
                client.sendMessage(currentGroup, message);
            }
        }
    }

    private void renderIncoming(ResponseParser response) {
        String grp = response.getGroup();
        String sender = response.getSenderName();
        if (
            (sender == null || sender.isBlank()) &&
            response.getSenderId() != null &&
            !response.getSenderId().isBlank()
        ) {
            sender = "id:" + response.getSenderId();
        }
        String ts = response.getTimestamp();
        String tsPart = (ts != null && !ts.isBlank())
            ? (DIM + FG_GRAY + "(" + ts + ")" + RESET + " ")
            : "";
        String groupTag = (grp != null && !grp.isBlank())
            ? (BOLD + FG_BLUE + "[" + grp + "]" + RESET + " ")
            : "";
        String nameColor = colorForUser(sender);
        String from = (sender != null && !sender.isBlank())
            ? (nameColor + sender + RESET + ": ")
            : "";
        String text = response.getData();
        synchronized (consoleLock) {
            // Clear current input line, print message, then re-draw prompt
            System.out.print("\r\u001B[2K"); // CR + clear entire line
            System.out.println(groupTag + from + text + " " + tsPart);
            System.out.print(getPrompt());
            System.out.flush();
        }
    }

    public static void main(String[] args) {
        Client client = new Client("localhost", 12345);
        new TerminalClient(client).start();
    }
}
