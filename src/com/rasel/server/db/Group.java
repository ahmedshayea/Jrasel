package com.rasel.server.db;

import java.util.ArrayList;

public class Group {

    final private String name;
    ArrayList<User> members = new ArrayList<>();
    User admin;

    Group(String name, User admin) {
        this.name = name;
        this.admin = admin;
        this.members.add(admin);
    }

    public String getName() {
        return name;
    }

    public ArrayList<User> getMembers() {
        return members;
    }

    public void addMember(User user) {
        members.add(user);
    }

    public void removeMember(User user) {
        members.remove(user);
    }

    public Boolean isMember(User user) {
        return members.contains(user);
    }

    public User getAdmin() {
        return admin;
    }

    public Boolean isAdmin(User user) {
        return admin.equals(user);
    }

    @Override
    public String toString() {
        return name;
    }
}

// GroupManager moved to its own public file.
