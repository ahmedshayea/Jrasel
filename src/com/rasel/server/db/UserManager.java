package com.rasel.server.db;

import java.util.ArrayList;

/**
 * Manage users in-memory for now.
 */
public class UserManager {

    private final ArrayList<User> users = new ArrayList<>();

    /**
     * Add user to list; ensure username is unique.
     */
    public User createUser(String username, String password) throws Exception {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                throw new Exception("User already exists");
            }
        }
        User newUser = new User(username, password);
        users.add(newUser);
        return newUser;
    }

    /**
     * Return existing user or create a new one.
     */
    public User getOrCreateUser(String username, String password) {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                return u;
            }
        }
        try {
            return createUser(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a user by username, or null if not found.
     */
    public User getUser(String username) {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                return u;
            }
        }
        return null;
    }

    public ArrayList<User> getAllUsers() {
        return users;
    }

    public int getUserCount() {
        return users.size();
    }

    public User findByUsername(String username) {
        for (User u : users) {
            if (u.getUsername().equals(username)) return u;
        }
        return null;
    }
}
