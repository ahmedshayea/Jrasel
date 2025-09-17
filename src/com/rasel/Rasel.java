package com.rasel;

import com.rasel.server.Server;

public class Rasel {

    public static void main(String[] args) {
        Server server = new Server();
        server.acceptConnections();
    }
}
