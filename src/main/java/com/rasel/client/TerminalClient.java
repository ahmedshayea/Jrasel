package com.rasel.client;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import com.rasel.common.Credentials;
import com.rasel.common.DataType;
import com.rasel.common.ResponseParser;
import com.rasel.server.db.Group;
import com.rasel.server.db.GroupParser;
import com.rasel.server.db.User;
import com.rasel.server.db.UserParser;

public class TerminalClient {

    private final ClientInterface client;
    private volatile String currentGroup = null;
    private final Scanner scanner = new Scanner(System.in); // shared scanner, not closed (System.in)
    private final Object consoleLock = new Object(); // synchronize mixed output from multiple threads

    // Subscriptions
    private AutoCloseable subMessages;
    private AutoCloseable subGroups;
    private AutoCloseable subUsers;

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
        if (name == null || name.isBlank())
            return FG_GRAY;
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
        System.out.println("  " + BOLD + "/help" + RESET + FG_GRAY + "  - Show this help" + RESET);
        System.out.println("  " + BOLD + "/login" + RESET + FG_GRAY + " - Re-authenticate" + RESET);
        System.out.println("  " + BOLD + "/create <group>" + RESET + FG_GRAY + " - Create and switch to a group" + RESET);
        System.out.println("  " + BOLD + "/switch <group>" + RESET + FG_GRAY + " - Switch current group" + RESET);
        System.out.println("  " + BOLD + "/groups" + RESET + FG_GRAY + " - List all groups" + RESET);
        System.out.println("  " + BOLD + "/users" + RESET + FG_GRAY + " - List all users" + RESET);
        System.out.println("  " + BOLD + "/users <group>" + RESET + FG_GRAY + " - List all users in group" + RESET);
        System.out.println("  " + BOLD + "/add <username>" + RESET + FG_GRAY + " - Add user to current group (admin only)" + RESET);
        System.out.println("  " + BOLD + "/quit" + RESET + FG_GRAY + "  - Exit client" + RESET);
        System.out.println(FG_GRAY + "Tip: start a command, e.g. '/cr', to see suggestions." + RESET);
    }

    private boolean suggestCommands(String input) {
        if (input == null)
            return false;
        String trimmed = input.trim();
        if (!trimmed.startsWith("/"))
            return false;

        String lower = trimmed.toLowerCase();
        java.util.List<String> matches = new java.util.ArrayList<>();
        for (String cmd : COMMANDS) {
            String head = cmd.split(" ")[0].toLowerCase();
            if (head.startsWith(lower)) {
                matches.add(cmd);
            }
        }
        if (matches.isEmpty())
            return false;
        System.out.println(FG_GRAY + "Suggestions:" + RESET);
        for (String m : matches) {
            System.out.println("  " + m);
        }
        return true;
    }

    public TerminalClient(ClientInterface client) {
        this.client = client;
    }

    public void start() {
        try {
            client.connect();
            System.out.println(BOLD + FG_GREEN + "Connected to server." + RESET);

            // Subscribe to server pushes
            subscribe();

            authenticate(); // interactive, uses subscriber API internally

            printHelp();
            handleUserInput(); // main input loop
        } catch (java.io.IOException e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    private void subscribe() {
        // Messages
        subMessages = client.onMessages(resp -> {
            synchronized (consoleLock) {
                // Clear current input line, print message, then re-draw prompt
                System.out.print("\r\u001B[2K");
                renderMessage(resp);
                System.out.print(getPrompt());
                System.out.flush();
            }
        });

        // Groups
        subGroups = client.onGroups(resp -> {
            synchronized (consoleLock) {
                System.out.print("\r\u001B[2K");
                renderGroups(resp);
                System.out.print(getPrompt());
                System.out.flush();
            }
        });

        // Users
        subUsers = client.onUsers(resp -> {
            synchronized (consoleLock) {
                System.out.print("\r\u001B[2K");
                renderUsers(resp);
                System.out.print(getPrompt());
                System.out.flush();
            }
        });
    }

    // Build current prompt string
    private String getPrompt() {
        String promptUser = client.isAuthenticated() && getUsername() != null ? getUsername() : "guest";
        String promptGroup = currentGroup != null ? currentGroup : "(no-group)";
        return (FG_GRAY + promptUser + "@" + promptGroup + FG_CYAN + " > " + RESET);
    }

    // Print prompt safely (used by input loop)
    private void printPrompt() {
        synchronized (consoleLock) {
            System.out.print(getPrompt());
            System.out.flush();
        }
    }

    private void authenticate() {
        while (!client.isAuthenticated()) {
            System.out.print(FG_CYAN + "Do you want to (1) Login or (2) Signup? Enter 1 or 2: " + RESET);
            String choice = scanner.nextLine().trim();
            System.out.print(FG_CYAN + "Username: " + RESET);
            String username = scanner.nextLine().trim();
            System.out.print(FG_CYAN + "Password: " + RESET);
            String password = scanner.nextLine();
            Credentials credentials = new Credentials(username, password);

            // One-shot wait using subscriber API
            CompletableFuture<Boolean> authResult = new CompletableFuture<>();
            AtomicReference<String> failureMsg = new AtomicReference<>(null);

            AutoCloseable okSub = client.onAuthSuccess(resp -> {
                try {
                    authResult.complete(true);
                } catch (Exception ignored) {
                }
            });
            AutoCloseable failSub = client.onAuthFailure(resp -> {
                try {
                    failureMsg.set(resp != null && resp.getData() != null ? resp.getData() : null);
                    authResult.complete(false);
                } catch (Exception ignored) {
                }
            });

            if ("2".equals(choice)) {
                client.signup(credentials);
            } else {
                client.authenticate(credentials);
            }

            boolean success = authResult.join(); // block this interactive step only

            // Cleanup subs
            try {
                if (okSub != null) okSub.close();
            } catch (Exception ignored) {}
            try {
                if (failSub != null) failSub.close();
            } catch (Exception ignored) {}

            if (success) {
                System.out.println(FG_GREEN + "✅ " + ("2".equals(choice) ? "Signup successful!" : "Authenticated!") + RESET);
            } else {
                String msg = failureMsg.get();
                System.out.println(FG_RED + "❌ " + (msg != null && !msg.isBlank() ? msg : "Authentication/Signup failed. Try again.") + RESET);
            }
        }
    }

    private void handleUserInput() {
        while (true) {
            printPrompt();
            String message = scanner.nextLine().trim();

            if (message.equals("/help") || message.equals("/h") || message.equals("?")) {
                printHelp();
                continue;
            }

            if (message.equals("/quit") || message.equals("/exit")) {
                System.out.println(FG_GREEN + "👋 Bye!" + RESET);
                System.exit(0);
            }

            // Show suggestions for partial commands
            if (message.startsWith("/") &&
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
                      message.equals("/exit"))) {
                if (suggestCommands(message)) continue;
            }

            if (message.equals("/login")) {
                client.clearSession();
                authenticate();
                continue;
            }

            if (message.startsWith("/create ")) {
                String groupName = message.substring(8).trim();
                if (groupName.isEmpty()) {
                    System.out.println(FG_YELLOW + "⚠️  Usage: /create <group>" + RESET);
                    continue;
                }
                client.requestCreateGroup(groupName); // async; server will respond
                currentGroup = groupName; // optimistic switch
                continue;
            }

            if (message.startsWith("/switch ")) {
                String groupName = message.substring(8).trim();
                if (groupName.isEmpty()) {
                    System.out.println(FG_YELLOW + "⚠️  Usage: /switch <group>" + RESET);
                    continue;
                }
                currentGroup = groupName;
                System.out.println(FG_GREEN + "➡️  Switched to group: " + currentGroup + RESET);
                continue;
            }

            if (message.equals("/groups")) {
                client.requestGroups(); // async; printed by groups subscriber
                continue;
            }

            if (message.startsWith("/users")) {
                String groupName = message.substring(6).trim();
                if (groupName.isEmpty()) {
                    client.requestUsers(); // all users
                } else {
                    client.requestUsers(groupName);
                }
                continue;
            }

            if (message.startsWith("/add ")) {
                if (currentGroup == null || currentGroup.isBlank()) {
                    System.out.println(FG_YELLOW + "⚠️  Select a group first with /switch <group> or /create <group>." + RESET);
                    continue;
                }
                String username = message.substring(5).trim();
                if (username.isEmpty()) {
                    System.out.println(FG_YELLOW + "⚠️  Usage: /add <username>" + RESET);
                    continue;
                }
                client.requestAddUserToGroup(currentGroup, username);
                continue;
            }

            if (!message.isEmpty() && client.isAuthenticated()) {
                if (currentGroup == null || currentGroup.isBlank()) {
                    System.out.println(FG_YELLOW + "⚠️  No group selected. Use /create <group> or /switch <group> first." + RESET);
                    continue;
                }
                client.sendMessage(currentGroup, message);
            }
        }
    }

    private void renderGroups(ResponseParser response) {
        if (response == null) return;

        System.out.println(BOLD + FG_CYAN + "Groups:" + RESET);

        String data = response.getData();
        if (data == null || data.isBlank()) {
            System.out.println(FG_GRAY + "(none)" + RESET);
            return;
        }

        // Only proceed if server said it's JSON
        if (response.getDataType() != DataType.JSON) {
            System.out.println(data);
            return;
        }

        try {
            // Parse server JSON into domain objects, then map to a flat DTO for printing
            java.util.List<Group> groups = GroupParser.parseGroups(data);
            if (groups == null || groups.isEmpty()) {
                System.out.println(FG_GRAY + "(none)" + RESET);
                return;
            }

            for (Group g : groups) {
                GroupDTO dto = GroupDTO.from(g);
                // Highlight currently selected group
                String namePart = dto.name != null ? dto.name : "(unnamed)";
                String nameColored = (currentGroup != null && currentGroup.equals(dto.name))
                        ? (BOLD + FG_BLUE + namePart + RESET)
                        : (BOLD + namePart + RESET);

                System.out.println(" - " + nameColored
                        + FG_GRAY + "  admin: " + RESET + (dto.adminUsername != null ? dto.adminUsername : "-")
                        + FG_GRAY + "  members: " + RESET + dto.membersCount);

                if (dto.memberUsernames != null && !dto.memberUsernames.isEmpty()) {
                    String list = String.join(", ", dto.memberUsernames);
                    System.out.println(FG_GRAY + "    " + list + RESET);
                }
            }
        } catch (Exception ex) {
            // Fallback: parse to raw shapes and render
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                        .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                java.util.List<RawGroup> groups = mapper.readValue(
                        data,
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<RawGroup>>() {});

                if (groups == null || groups.isEmpty()) {
                    System.out.println(FG_GRAY + "(none)" + RESET);
                    return;
                }

                for (RawGroup rg : groups) {
                    GroupDTO dto = GroupDTO.fromRaw(rg);
                    String namePart = dto.name != null ? dto.name : "(unnamed)";
                    String nameColored = (currentGroup != null && currentGroup.equals(dto.name))
                            ? (BOLD + FG_BLUE + namePart + RESET)
                            : (BOLD + namePart + RESET);

                    System.out.println(" - " + nameColored
                            + FG_GRAY + "  admin: " + RESET + (dto.adminUsername != null ? dto.adminUsername : "-")
                            + FG_GRAY + "  members: " + RESET + dto.membersCount);

                    if (dto.memberUsernames != null && !dto.memberUsernames.isEmpty()) {
                        String list = String.join(", ", dto.memberUsernames);
                        System.out.println(FG_GRAY + "    " + list + RESET);
                    }
                }
            } catch (Exception ex2) {
                System.out.println(FG_RED + "Failed to parse groups JSON." + RESET);
                System.out.println(data);
            }
        }
    }

    private void renderUsers(ResponseParser response) {
        if (response == null) return;
        System.out.println(BOLD + FG_CYAN + "Users:" + RESET);
        String data = response.getData();
        if (data == null || data.isBlank()) {
            System.out.println(FG_GRAY + "(none)" + RESET);
            return;
        }

        if (response.getDataType() != DataType.JSON) {
            System.out.println(data);
            return;
        }

        try {
            java.util.List<User> users = UserParser.parseUsers(data);
            if (users == null || users.isEmpty()) {
                System.out.println(FG_GRAY + "(none)" + RESET);
                return;
            }
            for (User u : users) {
                String uname = (u != null ? u.getUsername() : null);
                if (uname != null && !uname.isBlank()) {
                    System.out.println(" - " + uname);
                }
            }
        } catch (Exception ex) {
            System.out.println(FG_RED + "Failed to parse users JSON." + RESET);
            System.out.println(data);
        }
    }

    private void renderMessage(ResponseParser response) {
        String grp = response.getGroup();
        String sender = null;
        String ts = null;
        String text = response.getData();

        try {
            if (response.getResource() != null &&
                response.getResource().name().equals("MESSAGES") &&
                response.getDataType() == com.rasel.common.DataType.JSON) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.rasel.common.ChatMessagePayload payload = mapper.readValue(
                    text, com.rasel.common.ChatMessagePayload.class);
                if (payload != null) {
                    grp = payload.group != null ? payload.group : grp;
                    sender = payload.senderName != null ? payload.senderName
                            : (payload.senderId != null ? ("id:" + payload.senderId) : null);
                    ts = payload.timestamp;
                    text = payload.content != null ? payload.content : text;
                }
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException ignore) {}

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

        System.out.println(groupTag + from + text + " " + tsPart);
    }

    private String getUsername() {
        if (client instanceof Client c) {
            return c.getUsername();
        }
        return null;
    }

    public static void main(String[] args) {
        Client client = new Client("localhost", 12345);
        new TerminalClient(client).start();
    }

    // Flat DTO for group rendering in terminal
    private static final class GroupDTO {
        final String name;
        final String adminUsername;
        final int membersCount;
        final java.util.List<String> memberUsernames;

        GroupDTO(String name, String adminUsername, java.util.List<String> memberUsernames) {
            this.name = name;
            this.adminUsername = adminUsername;
            this.memberUsernames = memberUsernames != null ? memberUsernames : java.util.List.of();
            this.membersCount = this.memberUsernames.size();
        }

        static GroupDTO from(Group g) {
            if (g == null) return new GroupDTO(null, null, java.util.List.of());
            String name = g.getName();
            String admin = (g.getAdmin() != null ? g.getAdmin().getUsername() : null);
            java.util.List<String> members = new java.util.ArrayList<>();
            if (g.getMembers() != null) {
                for (User u : g.getMembers()) {
                    if (u != null && u.getUsername() != null && !u.getUsername().isBlank()) {
                        members.add(u.getUsername());
                    }
                }
            }
            return new GroupDTO(name, admin, members);
        }

        static GroupDTO fromRaw(RawGroup g) {
            if (g == null) return new GroupDTO(null, null, java.util.List.of());
            String name = g.name;
            String admin = (g.admin != null ? g.admin.username : null);
            java.util.List<String> members = new java.util.ArrayList<>();
            if (g.members != null) {
                for (RawUser u : g.members) {
                    if (u != null && u.username != null && !u.username.isBlank()) {
                        members.add(u.username);
                    }
                }
            }
            return new GroupDTO(name, admin, members);
        }
    }

    // Raw JSON shapes for fallback parsing of groups
    private static final class RawUser {
        public String username;
    }
    private static final class RawGroup {
        public String name;
        public RawUser admin;
        public java.util.List<RawUser> members;
    }
}
