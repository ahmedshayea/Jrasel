package com.rasel.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.rasel.common.DataType;
import com.rasel.common.RequestParser;
import com.rasel.common.Response;
import com.rasel.common.ResponseBuilder;
import com.rasel.common.ResponseResource;
import com.rasel.common.ResponseStatus;
import com.rasel.server.db.ChatMessage;
import com.rasel.server.db.DatabaseManager;
import com.rasel.server.db.Group;
import com.rasel.server.db.User;
import com.rasel.server.logging.Log;

/**
 * TODO: write comprehensive docs for this client handler class. ClientHandler
 */
public class ClientHandler implements Runnable {

    private Socket clientSocket;

    private ConnectionManager connectionManager;

    private User user;

    private Boolean isAuthenticated = false;

    private PrintWriter out;

    private BufferedReader in;

    private AuthenticationManager authManager;

    // Use DatabaseManager static singletons directly
    public ClientHandler(
            Socket clientSocket,
            User user,
            ConnectionManager connectionManager) {
        this.clientSocket = clientSocket;
        this.user = user;
        this.connectionManager = connectionManager;
        this.authManager = new AuthenticationManager();

        try {
            in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            Log.debug(
                    "Initialized IO streams for client %s:%d",
                    clientSocket.getInetAddress().getHostAddress(),
                    clientSocket.getPort());
        } catch (IOException e) {
            Log.error(
                    "Failed to initialize IO streams for client %s:%d",
                    e,
                    clientSocket.getInetAddress().getHostAddress(),
                    clientSocket.getPort());
        }
    }

    RequestParser getRequest() throws Exception {
        StringBuilder rb = new StringBuilder();
        boolean ended = false;
        String line;
        int lineCount = 0;
        final int MAX_LINES = 10000;
        while ((line = in.readLine()) != null) {
            if (line.equals("END_OF_REQUEST")) {
                ended = true;
                break;
            }
            rb.append(line).append("\n");
            if (++lineCount > MAX_LINES) {
                throw new Exception("Request too large");
            }
        }

        if (!ended) {
            throw new IOException("Stream ended before END_OF_RESPONSE");
        }

        String payload = rb.toString();
        if (payload.isBlank()) {
            throw new Exception("Empty response");
        }
        Log.trace("Received raw request (%d chars)", payload.length());
        return new RequestParser(payload);
    }

    @Override
    public void run() {
        try {
            Log.info(
                    "Client connected %s:%d",
                    clientSocket.getInetAddress().getHostAddress(),
                    clientSocket.getPort());
            while (true) {
                RequestParser request = getRequest();
                Log.debug(
                        "Handling request intent=%s auth=%s",
                        request.getIntent(),
                        request.isAuth());
                handleRequest(request);
            }
        } catch (Exception e) {
            Log.error(
                    "Client loop error for %s:%d",
                    e,
                    clientSocket.getInetAddress().getHostAddress(),
                    clientSocket.getPort());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
                Log.info(
                        "Client disconnected %s:%d",
                        clientSocket.getInetAddress().getHostAddress(),
                        clientSocket.getPort());
            } catch (IOException e) {
                Log.warn(
                        "Error during client cleanup %s:%d",
                        e,
                        clientSocket.getInetAddress().getHostAddress(),
                        clientSocket.getPort());
            }
        }
    }

    void handleRequest(RequestParser request) {
        if (!isAuthenticated && !(request.isAuth() || request.isSignup())) {
            sendResponse(ResponseBuilder.forbidden("you should be authenticated first"));
            Log.warn(
                    "Forbidden request from unauthenticated client intent=%s",
                    request.getIntent());
            return;
        }

        switch (request.getIntent()) {
            case AUTH ->
                handleAuth(request);
            case SIGNUP ->
                handleSignup(request);
            case SEND ->
                handleSend(request);
            case CREATE ->
                handleCreate(request);
            case GET ->
                handleGet(request);
            case GET_GROUPS ->
                handleGetGroups(request);
            case GET_USERS ->
                handleGetUsers(request);
            case ADD ->
                handleAdd(request);
        }
    }

    /**
     * return all groups that user is member of
     *
     * @param request
     */
    void handleGetGroups(RequestParser request) {
        var groups = DatabaseManager.groupManager.getUserGruops(user);
        // Tag resource so client.onGroups() subscribers receive it
        var resp = ResponseBuilder.ok(groups.toString(), ResponseResource.GROUPS);
        sendResponse(resp);
        logResponse(resp);
    }

    /**
     * return all users in the system, send response with resource of 'users' to
     * the client
     *
     * @param request
     */
    void handleGetUsers(RequestParser request) {
        var users = DatabaseManager.userManager.getAllUsers();
        // Tag resource so client.onUsers() subscribers receive it
        var resp = ResponseBuilder.ok(users.toString(), ResponseResource.USERS);
        sendResponse(resp);
        logResponse(resp);
    }

    /**
     * handle authentication request, authenticate credentials, if valid, add
     * client to auth client list stored in connectionManager, if not, return
     * ERROR response,
     */
    void handleAuth(RequestParser request) {
        var credentials = request.getCredentials();

        if (credentials == null) {
            var resp = new ResponseBuilder(
                "Credentials must be provided",
                DataType.TEXT,
                null,
                ResponseStatus.ERROR,
                ResponseResource.AUTH_FAILURE
            );
            sendResponse(resp);
            logResponse(resp);
            return;
        }

        User authUser = authManager.authenticate(
                credentials.getUsername(),
                credentials.getPassword());

        ResponseBuilder response;
        if (authUser != null) {
            connectionManager.addAuthenticatedClient(authUser.getId(), this);
            isAuthenticated = true;
            this.user = authUser;

            response = new ResponseBuilder(
                "Authentication successful",
                DataType.TEXT,
                null,
                ResponseStatus.OK,
                ResponseResource.AUTH_SUCCESS
            );
            Log.info("Authentication succeeded userId=%s username=%s",
                    authUser.getId(), authUser.getUsername());
        } else {
            response = new ResponseBuilder(
                "Invalid credentials",
                DataType.TEXT,
                null,
                ResponseStatus.FORBIDDEN,
                ResponseResource.AUTH_FAILURE
            );
            Log.warn("Authentication failed username=%s", credentials.getUsername());
        }
        sendResponse(response);
        logResponse(response);
    }

    void handleSignup(RequestParser request) {
        var credentials = request.getCredentials();
        ResponseBuilder response;
        try {
            User createdUser = DatabaseManager.userManager.createUser(
                    credentials.getUsername(),
                    credentials.getPassword());
            if (createdUser != null) {
                connectionManager.addAuthenticatedClient(
                        createdUser.getId(),
                        this);
                isAuthenticated = true;
                this.user = createdUser;

                // Important: emit AUTH_SUCCESS so login subscribers fire
                response = new ResponseBuilder(
                        "Signup successful and authenticated",
                        DataType.TEXT,
                        null,
                        ResponseStatus.OK,
                        ResponseResource.AUTH_SUCCESS
                );
                Log.info(
                        "Signup succeeded userId=%s username=%s",
                        createdUser.getId(),
                        createdUser.getUsername());
            } else {
                // Emit AUTH_FAILURE to trigger failure subscribers
                response = new ResponseBuilder(
                        "Signup failed",
                        DataType.TEXT,
                        null,
                        ResponseStatus.ERROR,
                        ResponseResource.AUTH_FAILURE
                );
                Log.warn(
                        "Signup failed for username=%s (unknown reason)",
                        credentials.getUsername());
            }
        } catch (Exception e) {
            Log.error(
                    "Signup error for username=%s",
                    e,
                    credentials.getUsername());
            // Emit AUTH_FAILURE to trigger failure subscribers
            response = new ResponseBuilder(
                    "User already exists",
                    DataType.TEXT,
                    null,
                    ResponseStatus.ERROR,
                    ResponseResource.AUTH_FAILURE
            );
        }
        sendResponse(response);
        logResponse(response);
    }

    void handleSend(RequestParser request) {
        String groupName = request.getGroup();
        if (groupName == null || groupName.isBlank()) {
            var err = ResponseBuilder.error("Group is required");
            sendResponse(err);
            logResponse(err);
            return;
        }

        Group group = DatabaseManager.groupManager.getGroup(groupName);
        if (group == null) {
            var err = ResponseBuilder.error("Group not found");
            sendResponse(err);
            logResponse(err);
            return;
        }

        if (this.user == null || !group.isMember(this.user)) {
            var err = ResponseBuilder.forbidden("You are not a member of this group");
            sendResponse(err);
            logResponse(err);
            return;
        }

        // Build domain message
        String now = java.time.Instant.now().toString();
        User sender = this.user;
        String content = request.getData();

        ChatMessage chatMessage = new ChatMessage(sender, content, group, now);
        DatabaseManager.chatMessageManager.addMessage(chatMessage);

        // Serialize as JSON payload
        com.rasel.server.db.ChatMessageSerializer serializer = new com.rasel.server.db.ChatMessageSerializer();
        String json = serializer.serialize(chatMessage);

        var response = ResponseBuilder
                .ok(json, com.rasel.common.DataType.JSON, groupName, ResponseResource.MESSAGES);

        // Broadcast to all group members
        int delivered = 0;
        for (User member : group.getMembers()) {
            ClientHandler client = connectionManager.getClientHandlerByUserId(member.getId());
            if (client != null) {
                client.sendResponse(response);
                delivered++;
            }
        }
        Log.info(
                "Message delivered user=%s group=%s size=%d delivered=%d/%d",
                this.user != null ? this.user.getUsername() : "?",
                groupName,
                content != null ? content.length() : 0,
                delivered,
                group.getMembers().size());
    }

    /**
     * create group with the group identifier
     *
     * @param request
     */
    void handleCreate(RequestParser request) {
        String groupIdentifier = request.getGroup();
        Group group = DatabaseManager.groupManager.getGroup(groupIdentifier);
        if (group != null) {
            Log.warn(
                    "Group creation failed name=%s by userId=%s (already exists)",
                    groupIdentifier,
                    this.user != null ? this.user.getId() : "?");
            var resp = ResponseBuilder.error("Group already exists");
            sendResponse(resp);
            logResponse(resp);
            return;
        }
        try {
            DatabaseManager.groupManager.createGroup(
                    groupIdentifier,
                    this.user);
            Log.info(
                    "Group created name=%s by userId=%s",
                    groupIdentifier,
                    this.user != null ? this.user.getId() : "?");
            var resp = ResponseBuilder.ok("Group created successfully");
            sendResponse(resp);
            logResponse(resp);
        } catch (Exception e) {
            Log.error(
                    "Failed to create group name=%s by userId=%s",
                    e,
                    groupIdentifier,
                    this.user != null ? this.user.getId() : "?");
            var resp = ResponseBuilder.error("Failed to create group");
            sendResponse(resp);
            logResponse(resp);
        }
    }

    void handleGet(RequestParser request) {
        var groups = DatabaseManager.groupManager.getAllGroups();
        if (groups.isEmpty()) {
            var resp = ResponseBuilder.ok("No groups available");
            sendResponse(resp);
            logResponse(resp);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Groups:\n");
        for (int i = 0; i < groups.size(); i++) {
            sb.append(" - ").append(groups.get(i).getName());
            if (i < groups.size() - 1) {
                sb.append("\n");
            }
        }
        var resp = ResponseBuilder.ok(sb.toString());
        sendResponse(resp);
        logResponse(resp);
    }

    void handleAdd(RequestParser request) {
        String groupName = request.getGroup();
        String usernameToAdd = request.getData(); // overload DATA with username
        if (groupName == null
                || groupName.isBlank()
                || usernameToAdd == null
                || usernameToAdd.isBlank()) {
            var resp = ResponseBuilder.error("Group and username are required");
            sendResponse(resp);
            logResponse(resp);
            return;
        }
        Group group = DatabaseManager.groupManager.getGroup(groupName);
        if (group == null) {
            var resp = ResponseBuilder.error("Group not found");
            sendResponse(resp);
            logResponse(resp);
            return;
        }
        if (this.user == null || !group.isAdmin(this.user)) {
            var resp = ResponseBuilder.forbidden("Only group admin can add members");
            sendResponse(resp);
            logResponse(resp);
            return;
        }
        var target = DatabaseManager.userManager.findByUsername(usernameToAdd);
        if (target == null) {
            var resp = ResponseBuilder.error("User not found");
            sendResponse(resp);
            logResponse(resp);
            return;
        }
        if (group.isMember(target)) {
            var resp = ResponseBuilder.ok("User is already a member");
            sendResponse(resp);
            logResponse(resp);
            return;
        }
        try {
            DatabaseManager.groupManager.addMember(groupName, target);
            Log.info(
                    "User added to group group=%s by admin=%s user=%s",
                    groupName,
                    this.user.getUsername(),
                    target.getUsername());
            var resp = ResponseBuilder.ok("User added successfully");
            sendResponse(resp);
            logResponse(resp);
        } catch (Exception e) {
            Log.error(
                    "Failed to add user to group group=%s username=%s",
                    e,
                    groupName,
                    usernameToAdd);
            var resp = ResponseBuilder.error("Failed to add user");
            sendResponse(resp);
            logResponse(resp);
        }
    }

    void sendResponse(ResponseBuilder response) {
        if (response == null) {
            Log.error("Attempted to send null response");
            return;
        }
        String payload = response.getResponseString();
        if (payload == null || payload.isBlank()) {
            Log.error("Attempted to send empty response");
            return;
        }
        out.println(payload);
        out.flush();
        Log.trace("Sent response (%d chars)", payload.length());

    }

    void logResponse(Response response) {
        Log.debug(
                "Response status=%s group=%s",
                response.getStatus().name(),
                response.getGroup());
    }
}
