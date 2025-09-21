package com.rasel;

import java.util.List;

import com.rasel.server.Server;
import com.rasel.server.db.DatabaseManager;
import com.rasel.server.db.User;
import com.rasel.server.db.UserParser;
import com.rasel.server.db.UserSerializer;

public class Rasel {
    public static void main(String[] args) {
        Server server = new Server();
        server.acceptConnections();
    }
}
