package com.rasel.server.db;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Parse ChatMessage JSON into domain model via DTO. */
public class ChatMessageParser {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static ChatMessage parse(String json) throws Exception {
        ChatMessageDTO dto = MAPPER.readValue(json, ChatMessageDTO.class);
        return ChatMessage.fromDTO(dto);
    }
}
