/**
 * TODO: This GroupSerilaizer class is almost identical to the UserSerializer, 
 * the only difference is the type of data you are passing to the serilaizer, 
 * consider abstracting the serialization logic into a generic class that inherit from it.
 */
package com.rasel.server.db;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Serialize Group or List<Group> to JSON using Jackson.
 */
public class GroupSerializer {
    private final boolean indent;
    private String json = "";
    private boolean valid = false;
    private final Object data; // Group or List<Group>

    public GroupSerializer(Group group, boolean indent) {
        this.data = group;
        this.indent = indent;
    }

    public GroupSerializer(List<Group> groups, boolean indent) {
        this.data = groups;
        this.indent = indent;
    }

    public String serialize() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            if (indent)
                mapper.enable(SerializationFeature.INDENT_OUTPUT);

            Object safe = toSafeView(data);
            json = mapper.writeValueAsString(safe);
            valid = true;
        } catch (Exception e) {
            valid = false;
            json = "";
        }
        return json;
    }

    public boolean isValid() {
        return valid;
    }

    public String getJson() {
        return json;
    }

    // Convert domain objects to lightweight DTOs to avoid cycles (messages) and large payloads
    private static Object toSafeView(Object src) {
        if (src == null)
            return null;
        if (src instanceof Group g) {
            return new SafeGroup(g);
        }
        if (src instanceof List<?> list) {
            java.util.ArrayList<Object> out = new java.util.ArrayList<>(list.size());
            for (Object o : list) {
                if (o instanceof Group g) {
                    out.add(new SafeGroup(g));
                } else {
                    out.add(o);
                }
            }
            return out;
        }
        return src;
    }

    private static final class SafeUser {
        public String username;
        SafeUser(User u) { this.username = u != null ? u.getUsername() : null; }
    }

    private static final class SafeGroup {
        public String name;
        public SafeUser admin;
        public java.util.List<SafeUser> members;
        SafeGroup(Group g) {
            this.name = g != null ? g.getName() : null;
            this.admin = g != null ? new SafeUser(g.getAdmin()) : null;
            this.members = new java.util.ArrayList<>();
            if (g != null && g.getMembers() != null) {
                for (User m : g.getMembers()) {
                    this.members.add(new SafeUser(m));
                }
            }
        }
    }
}
