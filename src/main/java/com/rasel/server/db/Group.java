package com.rasel.server.db;

import java.util.ArrayList;

/**
 * Represents a group in the chat server. Each group has a name, an admin, and a
 * list of members.
 * Provides methods to manage group membership and retrieve group information.
 */
public class Group {

    /**
     * The name of the group.
     */
    final private String name;
    /**
     * The list of users who are members of this group.
     */
    ArrayList<User> members = new ArrayList<>();
    /**
     * The admin user of this group.
     */
    User admin;

    /**
     * Constructs a new Group with the specified name and admin.
     * The admin is automatically added to the members list.
     *
     * @param name  the name of the group
     * @param admin the admin user of the group
     */
    Group(String name, User admin) {
        this.name = name;
        this.admin = admin;
        this.members.add(admin);
    }

    /**
     * Returns the name of the group.
     *
     * @return the group name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the list of members in the group.
     *
     * @return the list of group members
     */
    public ArrayList<User> getMembers() {
        return members;
    }

    /**
     * Adds a user to the group members.
     *
     * @param user the user to add
     */
    public void addMember(User user) {
        members.add(user);
    }

    /**
     * Removes a user from the group members.
     *
     * @param user the user to remove
     */
    public void removeMember(User user) {
        members.remove(user);
    }

    /**
     * Checks if a user is a member of the group.
     *
     * @param user the user to check
     * @return true if the user is a member, false otherwise
     */
    public Boolean isMember(User user) {
        return members.contains(user);
    }

    /**
     * Returns the admin of the group.
     *
     * @return the admin user
     */
    public User getAdmin() {
        return admin;
    }

    /**
     * Checks if the specified user is the admin of the group.
     *
     * @param user the user to check
     * @return true if the user is the admin, false otherwise
     */
    public Boolean isAdmin(User user) {
        return admin.equals(user);
    }

    /**
     * Returns the name of the group as its string representation.
     *
     * @return the group name
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Get all messages for this group.
     *
     * @return the list of chat messages for this group
     */
    public ArrayList<ChatMessage> getMessages() {
        // Implementation needed
        return DatabaseManager.chatMessageManager.getMessagesForGroup(this);
    }
}
