package com.rasel.common;

public class Credentials {
    String username;
    String password;

    public Credentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String toString() {
        return "Credentials{"
                + "username='" + username + "'" +
                ", password='" + password + "'" +
                "}";
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
