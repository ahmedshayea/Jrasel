package com.rasel.server.db;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;

public class GroupParser {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static Group parseGroup(String json) throws Exception {
        return MAPPER.readValue(json, Group.class);
    }

    public static List<Group> parseGroups(String json) throws Exception {
        return MAPPER.readValue(json, new TypeReference<List<Group>>() {
        });
    }
}
