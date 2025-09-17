package com.rasel.common;

/**
 * Wire-format DTO for chat messages included in Response.DATA when
 * RESOURCE=MESSAGES and DATA_TYPE=JSON.
 */
public class ChatMessagePayload {
    public String group;
    public String senderId;
    public String senderName;
    public String content;
    public String timestamp; // ISO-8601

    public ChatMessagePayload() {}

    public ChatMessagePayload(String group, String senderId, String senderName, String content, String timestamp) {
        this.group = group;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = timestamp;
    }
}
