package com.rasel.server.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/** Serialize ChatMessage (domain) to JSON via ChatMessageDTO. */
public class ChatMessageSerializer {
    private final boolean indent;
    private String json = "";
    private boolean valid = false;

    public ChatMessageSerializer() { this(false); }
    public ChatMessageSerializer(boolean indent) { this.indent = indent; }

    public String serialize(ChatMessage msg) {
        try {
            ChatMessageDTO dto = msg != null ? msg.toDTO() : null;
            ObjectMapper mapper = new ObjectMapper();
            if (indent) mapper.enable(SerializationFeature.INDENT_OUTPUT);
            json = mapper.writeValueAsString(dto);
            valid = true;
    } catch (Exception e) {
            valid = false;
            json = "";
        }
        return json;
    }

    public boolean isValid() { return valid; }
    public String getJson() { return json; }
}
