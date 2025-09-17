package com.rasel.server.db;

import java.util.ArrayList;

/**
 * Manage groups in-memory.
 */
public class GroupManager {

    private final ArrayList<Group> groups = new ArrayList<>();

    public Group createGroup(String name, User admin) throws Exception {
        if (getGroup(name) != null) {
            throw new Exception("Group already exists");
        }
        Group group = new Group(name, admin);
        groups.add(group);
        return group;
    }

    public Group getGroup(String name) {
        for (Group group : groups) {
            if (group.getName().equals(name)) {
                return group;
            }
        }
        return null;
    }

    public void addMember(String groupName, User user) throws Exception {
        Group group = getGroup(groupName);
        if (group == null) {
            throw new Exception("Group not found");
        }
        if (group.isMember(user)) {
            throw new Exception("User is already a member");
        }
        group.addMember(user);
    }

    public void removeMember(String groupName, User user) throws Exception {
        Group group = getGroup(groupName);
        if (group == null) {
            throw new Exception("Group not found");
        }
        if (!group.isMember(user)) {
            throw new Exception("User is not a member");
        }
        if (group.isAdmin(user)) {
            throw new Exception("Admin cannot be removed");
        }
        group.removeMember(user);
    }

    public ArrayList<Group> getAllGroups() {
        return groups;
    }

    /**
     * return all groups that user is member of.
     * 
     * @param user
     * @return
     */
    public ArrayList<Group> getUserGruops(User user) {
        ArrayList<Group> userGroups = new ArrayList<>();
        for (Group group : groups) {
            if (group.isMember(user)) {
                userGroups.add(group);
            }
        }
        return userGroups;
    }

    public void transferAdmin(String groupName, User newAdmin)
            throws Exception {
        Group group = getGroup(groupName);
        if (group == null) {
            throw new Exception("Group not found");
        }
        if (!group.isMember(newAdmin)) {
            throw new Exception("New admin must be a current member");
        }
        group.admin = newAdmin;
    }
}
