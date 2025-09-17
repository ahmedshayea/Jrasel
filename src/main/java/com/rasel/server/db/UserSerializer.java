package com.rasel.server.db;

import java.util.List;
import com.rasel.server.db.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Serialize user data to Json using jackson , if isMany is ture, it will
 * serialize a list of users
 * UserSerializer
 */

/**
 * Serialize user data to Json using Jackson.
 * Configuration is set via constructors; serialization occurs when 'serialize'
 * is called.
 */
public class UserSerializer {
    private boolean allowIndent = false;
    private boolean isValid = false;
    private String jsonString = "";
    // user or list of users
    private Object data = null;

    public UserSerializer(User user, boolean allowIndent) {
        this.data = user;
        this.allowIndent = allowIndent;
    }

    public UserSerializer(List<User> users, boolean allowIndent) {
        this.data = users;
        this.allowIndent = allowIndent;
    }

    public String serialize() {
        ObjectMapper mapper = new ObjectMapper();
        if (allowIndent) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        try {
            jsonString = mapper.writeValueAsString(data);
            isValid = true;
            return jsonString;
        } catch (Exception e) {
            e.printStackTrace();
            isValid = false;
            jsonString = "";
            return jsonString;
        }
    }

    public boolean isValid() {
        return isValid;
    }

    public String getJsonString() {
        return jsonString;
    }
}
