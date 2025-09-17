package com.rasel.server;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO: add detailed documentation to this interface
 * ConnectionManagement
 */
interface ClientsManager {
    void addClient(ClientHandler client);

    void removeClient(ClientHandler client);

    List<ClientHandler> getClients();

    void addAuthenticatedClient(String userId, ClientHandler client);

    void removeAuthenticatedClient(String userId);

    ClientHandler getClientHandlerByUserId(String userId);
}

/**
 * Manage connected clients, thread creation, and authenticatred clients.
 * ConnectionManager
 */
public class ConnectionManager implements ClientsManager {

    ServerSocket serverSocket;
    private final List<ClientHandler> clients = new ArrayList<>();
    private final Map<String, ClientHandler> authenticatedClients =
        new ConcurrentHashMap<>();

    public ConnectionManager(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void addClient(ClientHandler client) {
        clients.add(client);
        new Thread(client).start();
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public void addAuthenticatedClient(String userId, ClientHandler client) {
        authenticatedClients.put(userId, client);
    }

    public void removeAuthenticatedClient(String userId) {
        authenticatedClients.remove(userId);
    }

    public ClientHandler getClientHandlerByUserId(String userId) {
        return authenticatedClients.get(userId);
    }
}
