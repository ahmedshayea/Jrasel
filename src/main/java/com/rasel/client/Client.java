package com.rasel.client;

import com.rasel.common.Credentials;
import com.rasel.common.RequestBuilder;
import com.rasel.common.RequestIntent;
import com.rasel.common.RequestParser;
import com.rasel.common.ResponseParser;
import com.rasel.server.logging.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements ClientInterface {

    private final String serverAddress;
    private final int serverPort;
    Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean authenticated = false;
    private Credentials credentials;

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void connect() throws IOException {
        socket = new Socket(serverAddress, serverPort);
        socket.setKeepAlive(true);

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void disconnect() throws IOException {
        socket.close();
    }

    public boolean isConnected() {
        if (socket == null) return false;

        return socket.isConnected();
    }

    public String[] getAvailableUsers() {
        // TODO: implement this method
        String[] availableUsers = new String[0];
        return availableUsers;
    }

    public String[] getUsersInGroup(String groupName) {
        // TODO: implement this method
        return new String[0];
    }

    public boolean authenticate(Credentials credentials) throws Exception {
        this.credentials = credentials;
        String authRequest = com.rasel.common.RequestBuilder.authRequest(
            credentials
        );
        sendRequest(authRequest);
        ResponseParser response = getResponse();
        authenticated = response.isOk();
        return authenticated;
    }

    public boolean signup(Credentials credentials) throws Exception {
        // Build SIGNUP request using RequestBuilder
        this.credentials = credentials; // ensure subsequent requests have creds
        RequestBuilder request = new RequestBuilder(
            RequestIntent.SIGNUP,
            credentials,
            null, // No group for signup
            null // No data for signup
        );

        // Send request to server
        sendRequest(request.getRequest());

        // Wait for response
        ResponseParser response = getResponse();
        if (response.isOk()) {
            setAuthenticated(true);
            return true;
        } else {
            setAuthenticated(false);
            return false;
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * TODO: this method should be removed, you should not allow
     * direct modification to authenticated state
     *
     * @param value
     */
    public void setAuthenticated(boolean value) {
        authenticated = value;
    }

    /**
     */
    public void sendMessage(String group, String message) {
        if (authenticated) {
            String request = RequestBuilder.sendMessageRequest(
                credentials,
                group,
                message
            );
            sendRequest(request);
        }
    }

    public void sendRequest(String request) {
        out.println(request);
    }

    public void sendRequest(RequestBuilder request) {
        out.println(request.getRequest());
    }

    public ResponseParser getResponse() throws Exception {
        StringBuilder sb = new StringBuilder(256);
        String line;
        boolean ended = false;
        int lineCount = 0;
        final int MAX_LINES = 10000;

        while ((line = in.readLine()) != null) {
            if ("END_OF_RESPONSE".equals(line)) {
                ended = true;
                break;
            }
            sb.append(line).append('\n');
            if (++lineCount > MAX_LINES) {
                throw new Exception("Response too large");
            }
        }

        if (!ended) {
            throw new IOException("Stream ended before END_OF_RESPONSE");
        }

        String payload = sb.toString();
        if (payload.isBlank()) {
            throw new Exception("Empty response");
        }

        // Prime keys to avoid stale values from static Parser.macros and ensure
        // defaults
        String primed = "STATUS:\nDATA_TYPE:\nGROUP:\nDATA:\n" + payload;
        return new ResponseParser(primed);
    }

    private BufferedReader getInputStream() {
        return in;
    }

    // --- Group related helpers ---

    public boolean createGroup(String groupName) throws Exception {
        if (!authenticated) return false;
        String req = RequestBuilder.createGroupRequest(credentials, groupName);
        sendRequest(req);
        ResponseParser resp = getResponse();
        return resp.isOk();
    }

    public String[] listGroups() throws Exception {
        if (!authenticated) return new String[0];
        // Request without group to get all groups
        String req = RequestBuilder.getGroupRequest(credentials, null);
        sendRequest(req);
        ResponseParser resp = getResponse();
        if (!resp.isOk()) return new String[0];
        String data = resp.getData();
        if (
            data == null || data.isBlank() || data.equals("No groups available")
        ) return new String[0];
        System.out.println(data);
        return data.split(",");
    }

    // Non-blocking helpers (do not await response; let a single receiver consume)
    public void requestCreateGroup(String groupName) {
        if (!authenticated) return;
        String req = RequestBuilder.createGroupRequest(credentials, groupName);
        sendRequest(req);
    }

    public void requestListGroups() {
        if (!authenticated) return;
        String req = RequestBuilder.getGroupsRequest(credentials);
        sendRequest(req);
    }

    public void requestAddUserToGroup(String groupName, String username) {
        if (!authenticated) return;
        String req = RequestBuilder.addUserToGroupRequest(
            credentials,
            groupName,
            username
        );
        sendRequest(req);
    }

    public void requestGetUsers() {
        if (!authenticated) return;
        String req = RequestBuilder.getUsersRequest(credentials);
        sendRequest(req);
    }

    public void requestGetUsers(String groupName) {
        if (!authenticated) return;
        String req = RequestBuilder.getUsersRequest(credentials, groupName);
        sendRequest(req);
    }

    public String getUsername() {
        return credentials != null ? credentials.getUsername() : null;
    }
}
