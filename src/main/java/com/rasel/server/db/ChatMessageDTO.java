package com.rasel.server.db;

/**
 * Lightweight DTO used for wire format and persistence serialization.
 * Uses primary keys (group name, username) instead of object references.
 */
public class ChatMessageDTO {
    public String group;      // group name
    public String senderId;   // optional id if available
    public String senderName; // username
    public String content;
    public String timestamp;  // ISO-8601 string

    public ChatMessageDTO() {}

    public ChatMessageDTO(String group, String senderId, String senderName, String content, String timestamp) {
        this.group = group;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = timestamp;
    }
}
