package com.rasel.server;

import com.rasel.server.db.DatabaseManager;
import com.rasel.server.db.User;

/**
 * Authentication manager, handles authentication, signup
 * 
 * @author shayea
 */
public class AuthenticationManager {
    public AuthenticationManager() {}

    public User authenticate(String username, String password) {
        User user = DatabaseManager.userManager.getUser(username);

        if (user != null && user.checkPassword(password)) {
            return user;
        }
        return null;
    }

    public User signup(String username, String password) throws Exception {
        return DatabaseManager.userManager.createUser(username, password);
    }
}
