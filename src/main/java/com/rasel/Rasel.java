package com.rasel;

import java.util.List;

import com.rasel.server.Server;
import com.rasel.server.db.DatabaseManager;
import com.rasel.server.db.User;
import com.rasel.server.db.UserParser;
import com.rasel.server.db.UserSerializer;

public class Rasel {
    public static void main(String[] args) {
        try {
            String data = "[{\"username\":\"ahmed\",\"password\":\"ahome\",\"id\":\"ahmed\"},{\"username\":\"asel\",\"password\":\"hom\",\"id\":\"asel\"}]";
            List<User> users = UserParser.parseUsers(data);

            for (var user : users) {
                System.out.println(user.getUsername());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Server server = new Server();
        // server.acceptConnections();
    }
}
