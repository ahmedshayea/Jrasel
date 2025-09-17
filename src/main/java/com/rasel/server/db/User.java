package com.rasel.server.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Store user information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    final private String username;
    private String password;

    @JsonCreator
    public User(
            @JsonProperty("username") String username,
            @JsonProperty("password") String password) {
        this.username = username;
        this.password = hashPassword(password);
    }

    public String getUsername() {
        return username;
    }

    public String getId() {
        return username;
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
