package com.rasel.server;

import com.rasel.common.RequestParser;
import com.rasel.common.ResponseBuilder;
import com.rasel.common.ResponseStatus;
import com.rasel.server.db.DatabaseManager;
import com.rasel.server.db.DatabaseManager;
import com.rasel.server.db.Group;
import com.rasel.server.db.User;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * TODO: explain Server class
 *
 * what should the server class do ? it must initiate the server socket and
 * listen for incoming connections, once a connection is established, it should
 * read the request from the client and parse it using the RequestParser class,
 * it must perform the requied action by the request, like sending messages to
 * groups, creating new group creating new user, etc, it must return a Rasel
 * Response using ResponseBuilder.
 */
public class Server {

    // TODO: allow port to be assigned per object.
    final int PORT = 12345;
    ServerSocket socket;
    ConnectionManager connectionManager;

    public Server() {
        try {
            socket = new ServerSocket(PORT);
            connectionManager = new ConnectionManager(socket);
            System.out.println("✅ Server is up and running on port " + PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptConnections() {
        try {
            while (true) {
                Socket connection = socket.accept();
                System.out.println(
                    "🔌 Client connected: " +
                        socket.getInetAddress().getHostAddress()
                );
                ClientHandler client = new ClientHandler(
                    connection,
                    null,
                    connectionManager
                );
                connectionManager.addClient(client);
            }
        } catch (IOException e) {
            System.err.println("🔥 Server error: " + e.getMessage());
            System.out.println("🛑 Server is shutting down.");
        }
    }
}
