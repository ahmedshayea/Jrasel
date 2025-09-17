package com.rasel.server.db;

/**
 * Store user information.
 */
public class User {

    static private int idCounter = 0;
    final private String id;
    final private String username;
    private String password;

    User(String username, String password) {
        this.id = String.valueOf(++idCounter);
        this.username = username;
        this.password = hashPassword(password);
    }

    public String getUsername() {
        return username;
    }

    public String getId() {
        return id;
    }

    public Boolean checkPassword(String password) {
        return this.password.equals(hashPassword(password));
    }

    public String getPassword() {
        return password;
    }

    public String setPassword(String password) {
        this.password = hashPassword(password);
        return this.password;
    }

    /**
     * TODO: hash the password using SHA-256 algorithm.
     */
    private String hashPassword(String password) {
        return password;
    }

    @Override
    public String toString() {
        return username;
    }
}
