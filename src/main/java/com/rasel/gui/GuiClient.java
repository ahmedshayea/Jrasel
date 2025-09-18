package com.rasel.gui;

import com.rasel.client.Client;
import com.rasel.client.ClientInterface;

public class GuiClient {
    private static ClientInterface client;

    static {
        client = new Client("192.168.8.2", 12345);
        try {
            client.connect();
        } catch (Exception e) {
            // Handle connection error (e.g., show dialog, exit, or retry)
            e.printStackTrace();
            client = null;
        }
    }

    public static ClientInterface getClient() {
        return client;
    }
}
