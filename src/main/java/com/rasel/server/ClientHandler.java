package com.rasel.server;

import com.rasel.common.RequestIntent;
import com.rasel.common.RequestParser;
import com.rasel.common.Response;
import com.rasel.common.ResponseBuilder;
import com.rasel.server.db.DatabaseManager;
import com.rasel.server.db.Group;
import com.rasel.server.db.User;
import com.rasel.server.logging.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ResponseCache;
import java.net.Socket;
import java.util.ArrayList;

/**
 * TODO: write comprehensive docs for this client handler class.
 * ClientHandler
 */
public class ClientHandler implements Runnable {

    private Socket clientSocket;

    private ConnectionManager connectionManager;

    private User user;

    private Boolean isAuthenticated = false;

    private PrintWriter out;

    private BufferedReader in;

    private AuthenticationManager authManager;

    private static final DatabaseManager dbManager = new DatabaseManager();

    public ClientHandler(
        Socket clientSocket,
        User user,
        ConnectionManager connectionManager
    ) {
        this.clientSocket = clientSocket;
        this.user = user;
        this.connectionManager = connectionManager;
        this.authManager = new AuthenticationManager(dbManager);

        try {
            in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream())
            );
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            Log.debug(
                "Initialized IO streams for client %s:%d",
                clientSocket.getInetAddress().getHostAddress(),
                clientSocket.getPort()
            );
        } catch (IOException e) {
            Log.error(
                "Failed to initialize IO streams for client %s:%d",
                e,
                clientSocket.getInetAddress().getHostAddress(),
                clientSocket.getPort()
            );
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
                clientSocket.getPort()
            );
            while (true) {
                RequestParser request = getRequest();
                Log.debug(
                    "Handling request intent=%s auth=%s",
                    request.getIntent(),
                    request.isAuth()
                );
                handleRequest(request);
            }
        } catch (Exception e) {
            Log.error(
                "Client loop error for %s:%d",
                e,
                clientSocket.getInetAddress().getHostAddress(),
                clientSocket.getPort()
            );
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
                    clientSocket.getPort()
                );
            } catch (IOException e) {
                Log.warn(
                    "Error during client cleanup %s:%d",
                    e,
                    clientSocket.getInetAddress().getHostAddress(),
                    clientSocket.getPort()
                );
            }
        }
    }

    void handleRequest(RequestParser request) {
        if (!isAuthenticated && !(request.isAuth() || request.isSignup())) {
            sendResponse(
                ResponseBuilder.forbidden("you should be authenticated first")
            );
            Log.warn(
                "Forbidden request from unauthenticated client intent=%s",
                request.getIntent()
            );
            return;
        }

        Response response = null;
        switch (request.getIntent()) {
            case AUTH -> response = handleAuth(request);
            case SIGNUP -> response = handleSignup(request);
            case SEND -> response = handleSend(request);
            case CREATE -> response = handleCreate(request);
            case GET -> response = handleGet(request);
            case GET_GROUPS -> response = handleGetGroups(request);
            case GET_USERS -> response = handleGetUsers(request);
            case ADD -> response = handleAdd(request);
        }

        // Centralized sending strategy
        if (request.getIntent() == RequestIntent.SEND) {
            Response err = sendMessageToGroup(response, request.getGroup());
            if (err != null) {
                sendResponse(err);
            } else {
                Log.info(
                    "Message delivered user=%s group=%s size=%d",
                    this.user != null ? this.user.getUsername() : "?",
                    request.getGroup(),
                    request.getData() != null ? request.getData().length() : 0
                );
            }
        } else if (response != null) {
            // Reply only to the current client
            sendResponse(response);
        }

        if (response != null) {
            logResponse(response);
        }
    }

    /**
     * return all groups that user is member of
     *
     * @param request
     * @return
     */
    Response handleGetGroups(RequestParser request) {
        var groups = dbManager.groupManager.getUserGruops(user);
        // this will return a list with group names like this [group1, group2, ...],
        // with brackets included.
        return ResponseBuilder.ok(groups.toString());
    }

    Response handleGetUsers(RequestParser request) {
        var users = dbManager.userManager.getAllUsers();
        // this will return a list with group names like this [user1, user2, ...],
        // with brackets included.
        return ResponseBuilder.ok(users.toString());
    }

    /**
     * handle authentication request, authenticate credentials, if valid, add
     * client to auth client list stored in connectionManager, if not, return
     * ERROR response,
     */
    ResponseBuilder handleAuth(RequestParser request) {
        var credentials = request.getCredentials();

        if (credentials == null) {
            return ResponseBuilder.error("Credentials must be provided");
        }

        User authUser = authManager.authenticate(
            credentials.getUsername(),
            credentials.getPassword()
        );

        ResponseBuilder response;
        // success authentication
        if (authUser != null) {
            // add to auth list
            connectionManager.addAuthenticatedClient(authUser.getId(), this);
            isAuthenticated = true;
            this.user = authUser;
            // return success response
            response = ResponseBuilder.ok("successfull authentication");
            Log.info(
                "Authentication succeeded userId=%s username=%s",
                authUser.getId(),
                authUser.getUsername()
            );
        } else {
            response = ResponseBuilder.error("invalide credentials");
            Log.warn(
                "Authentication failed username=%s",
                credentials.getUsername()
            );
        }
        return response;
    }

    ResponseBuilder handleSignup(RequestParser request) {
        var credentials = request.getCredentials();
        ResponseBuilder response;
        try {
            User createdUser = DatabaseManager.userManager.createUser(
                credentials.getUsername(),
                credentials.getPassword()
            );
            if (createdUser != null) {
                connectionManager.addAuthenticatedClient(
                    createdUser.getId(),
                    this
                );
                isAuthenticated = true;
                this.user = createdUser;
                response = ResponseBuilder.ok(
                    "Signup successful and authenticated"
                );
                Log.info(
                    "Signup succeeded userId=%s username=%s",
                    createdUser.getId(),
                    createdUser.getUsername()
                );
            } else {
                response = ResponseBuilder.error("Signup failed");
                Log.warn(
                    "Signup failed for username=%s (unknown reason)",
                    credentials.getUsername()
                );
            }
        } catch (Exception e) {
            Log.error(
                "Signup error for username=%s",
                e,
                credentials.getUsername()
            );
            response = ResponseBuilder.error("User already exists");
        }
        return response;
    }

    // Removed unused sendMessage helper (broadcasts handled centrally)
    void sendResponse(Response response) {
        out.println(response.getResponseString());
    }

    ResponseBuilder handleSend(RequestParser request) {
        // Build response with sender info; sending handled by handleRequest
        String now = java.time.Instant.now().toString();
        String sid = this.user != null ? this.user.getId() : null;
        String sname = this.user != null ? this.user.getUsername() : null;
        return ResponseBuilder.okWithSender(
            request.getData(),
            request.getGroup(),
            sid,
            sname,
            now
        );
    }

    ResponseBuilder sendMessageToGroup(Response response, String groupName) {
        if (groupName == null || groupName.isBlank()) {
            Log.warn(
                "SEND missing group by userId=%s",
                this.user != null ? this.user.getId() : "?"
            );
            return ResponseBuilder.error("Group is required");
        }
        Group group = DatabaseManager.groupManager.getGroup(groupName);
        if (group == null) {
            Log.warn(
                "SEND to non-existent group=%s by userId=%s",
                groupName,
                this.user != null ? this.user.getId() : "?"
            );
            return ResponseBuilder.error("Group not found");
        }
        if (this.user == null || !group.isMember(this.user)) {
            Log.warn(
                "SEND forbidden: userId=%s not member of group=%s",
                this.user != null ? this.user.getId() : "?",
                groupName
            );
            return ResponseBuilder.forbidden(
                "You are not a member of this group"
            );
        }

        int delivered = 0;
        for (User member : group.getMembers()) {
            ClientHandler client = connectionManager.getClientHandlerByUserId(
                member.getId()
            );
            if (client != null) {
                client.sendResponse(response);
                delivered++;
            }
        }
        Log.debug(
            "Broadcasted to %d/%d members group=%s",
            delivered,
            group.getMembers().size(),
            groupName
        );
        return null;
    }

    /**
     * create group with the group identifier
     *
     * @param request
     */
    ResponseBuilder handleCreate(RequestParser request) {
        String groupIdentifier = request.getGroup();
        Group group = DatabaseManager.groupManager.getGroup(groupIdentifier);
        if (group != null) {
            Log.warn(
                "Group creation failed name=%s by userId=%s (already exists)",
                groupIdentifier,
                this.user != null ? this.user.getId() : "?"
            );
            return ResponseBuilder.error("Group already exists");
        }
        try {
            DatabaseManager.groupManager.createGroup(
                groupIdentifier,
                this.user
            );
            Log.info(
                "Group created name=%s by userId=%s",
                groupIdentifier,
                this.user != null ? this.user.getId() : "?"
            );
            return ResponseBuilder.ok("Group created successfully");
        } catch (Exception e) {
            Log.error(
                "Failed to create group name=%s by userId=%s",
                e,
                groupIdentifier,
                this.user != null ? this.user.getId() : "?"
            );
            return ResponseBuilder.error("Failed to create group");
        }
    }

    ResponseBuilder handleGet(RequestParser request) {
        // List available groups in a pretty bullet list
        var groups = DatabaseManager.groupManager.getAllGroups();
        if (groups.isEmpty()) {
            return ResponseBuilder.ok("No groups available");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Groups:\n");
        for (int i = 0; i < groups.size(); i++) {
            sb.append(" - ").append(groups.get(i).getName());
            if (i < groups.size() - 1) sb.append("\n");
        }
        return ResponseBuilder.ok(sb.toString());
    }

    ResponseBuilder handleAdd(RequestParser request) {
        String groupName = request.getGroup();
        String usernameToAdd = request.getData(); // overload DATA with username
        if (
            groupName == null ||
            groupName.isBlank() ||
            usernameToAdd == null ||
            usernameToAdd.isBlank()
        ) {
            return ResponseBuilder.error("Group and username are required");
        }
        Group group = DatabaseManager.groupManager.getGroup(groupName);
        if (group == null) {
            return ResponseBuilder.error("Group not found");
        }
        if (this.user == null || !group.isAdmin(this.user)) {
            return ResponseBuilder.forbidden(
                "Only group admin can add members"
            );
        }
        var target = DatabaseManager.userManager.findByUsername(usernameToAdd);
        if (target == null) {
            return ResponseBuilder.error("User not found");
        }
        if (group.isMember(target)) {
            return ResponseBuilder.ok("User is already a member");
        }
        try {
            DatabaseManager.groupManager.addMember(groupName, target);
            Log.info(
                "User added to group group=%s by admin=%s user=%s",
                groupName,
                this.user.getUsername(),
                target.getUsername()
            );
            return ResponseBuilder.ok("User added successfully");
        } catch (Exception e) {
            Log.error(
                "Failed to add user to group group=%s username=%s",
                e,
                groupName,
                usernameToAdd
            );
            return ResponseBuilder.error("Failed to add user");
        }
    }

    void logResponse(Response response) {
        Log.debug(
            "Response status=%s group=%s",
            response.getStatus().name(),
            response.getGroup()
        );
    }
}
