package com.rasel.server.db;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;

public class UserParser {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static User parseUser(String json) throws Exception {
        return MAPPER.readValue(json, User.class);
    }

    public static List<User> parseUsers(String json) throws Exception {
        return MAPPER.readValue(json, new TypeReference<List<User>>() {});
    }
}
