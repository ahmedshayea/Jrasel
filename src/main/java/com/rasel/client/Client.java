package com.rasel.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

import com.rasel.common.Credentials;
import com.rasel.common.RequestBuilder;
import com.rasel.common.RequestIntent;
import com.rasel.common.ResponseParser;
import com.rasel.common.ResponseResource;
import com.rasel.server.logging.Log;

public class Client implements ClientInterface {

    private final String serverAddress;
    private final int serverPort;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private volatile boolean authenticated = false;
    private Credentials credentials;

    private final ResponseBus responseBus = new ResponseBus();
    private Thread receiverThread;

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    // Connection lifecycle

    @Override
    public void connect() throws IOException {
        socket = new Socket(serverAddress, serverPort);
        socket.setKeepAlive(true);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        startReceiver();
    }

    @Override
    public void disconnect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close(); // this will also unblock the receiver thread
        }
    }

    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    // Authentication

    @Override
    public void authenticate(Credentials credentials) {
        this.credentials = credentials;
        String authRequest = RequestBuilder.authRequest(credentials);
        sendRequest(authRequest);
    }

    @Override
    public void signup(Credentials credentials) {
        this.credentials = credentials;
        RequestBuilder request = new RequestBuilder(
                RequestIntent.SIGNUP,
                credentials,
                null,
                null
        );
        sendRequest(request.getRequest());
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    // Requests

    @Override
    public void sendMessage(String group, String message) {
        if (!authenticated) return;
        String request = RequestBuilder.sendMessageRequest(credentials, group, message);
        sendRequest(request);
    }

    @Override
    public void requestCreateGroup(String groupName) throws Exception {
        if (!authenticated) return;
        String req = RequestBuilder.createGroupRequest(credentials, groupName);
        sendRequest(req);
    }

    @Override
    public void requestGroups() {
        if (!authenticated) return;
        String req = RequestBuilder.getGroupsRequest(credentials);
        sendRequest(req);
    }

    @Override
    public void requestUsers() {
        if (!authenticated) return;
        String req = RequestBuilder.getUsersRequest(credentials);
        sendRequest(req);
    }

    @Override
    public void requestMessages(String groupName) {
        if (!authenticated) return;
        // Implement once RequestBuilder supports it; keep non-throwing.
        // Example:
        // String req = RequestBuilder.getMessagesRequest(credentials, groupName);
        // sendRequest(req);
        Log.info("requestMessages: not implemented in RequestBuilder; group=%s", groupName);
    }

    @Override
    public void requestAddUserToGroup(String groupName, String username) {
        if (!authenticated) return;
        String req = RequestBuilder.addUserToGroupRequest(credentials, groupName, username);
        sendRequest(req);
    }

    @Override
    public void sendRequest(RequestBuilder request) {
        sendRequest(request.getRequest());
    }

    // Legacy (discouraged with async receiver running)

    @Deprecated
    @Override
    public ResponseParser getResponse() throws Exception {
        throw new UnsupportedOperationException("Synchronous getResponse() is disabled; use subscriber APIs.");
    }

    // Subscriptions

    @Override
    public AutoCloseable onUsers(Consumer<ResponseParser> handler) {
        return responseBus.on(ResponseResource.USERS, handler);
    }

    @Override
    public AutoCloseable onGroups(Consumer<ResponseParser> handler) {
        return responseBus.on(ResponseResource.GROUPS, handler);
    }

    @Override
    public AutoCloseable onMessages(Consumer<ResponseParser> handler) {
        return responseBus.on(ResponseResource.MESSAGES, handler);
    }

    @Override
    public AutoCloseable onAuthSuccess(Consumer<ResponseParser> handler) {
        // Update state when event arrives then forward to handler
        return responseBus.on(ResponseResource.AUTH_SUCCESS, resp -> {
            authenticated = true;
            if (handler != null) handler.accept(resp);
        });
    }

    @Override
    public AutoCloseable onAuthFailure(Consumer<ResponseParser> handler) {
        return responseBus.on(ResponseResource.AUTH_FAILURE, resp -> {
            authenticated = false;
            if (handler != null) handler.accept(resp);
        });
    }

    @Override
    public AutoCloseable on(ResponseResource resource, Consumer<ResponseParser> handler) {
        return responseBus.on(resource, handler);
    }

    // Internals

    public void sendRequest(String request) {
        if (!isConnected()) return;
        out.println(request);
    }

    private void startReceiver() {
        receiverThread = new Thread(this::receiveLoop, "response-receiver");
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private void receiveLoop() {
        try {
            while (!Thread.currentThread().isInterrupted() && isConnected()) {
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
                    if (++lineCount > MAX_LINES) throw new IOException("Response too large");
                }
                if (!ended) break; // socket probably closed

                String payload = sb.toString();
                if (payload.isBlank()) continue;

                // Prime keys to avoid stale values
                String primed = "STATUS:\nRESOURCE:\nDATA_TYPE:\nGROUP:\nDATA:\n" + payload;
                ResponseParser resp = new ResponseParser(primed);

                responseBus.publish(resp);
            }
        } catch (Exception e) {
            Log.warn("Receiver loop terminated: %s", e.getMessage());
        }
    }

    public String getUsername() {
        return credentials != null ? credentials.getUsername() : null;
    }
}
