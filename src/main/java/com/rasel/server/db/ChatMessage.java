package com.rasel.server.db;

/**
 * ChatMessage domain object used in server runtime and persisted in-memory.
 * For serialization, use ChatMessageDTO via toDTO()/fromDTO().
 */
public class ChatMessage {
    private final Group group; // domain reference
    private final User sender; // domain reference
    private final String content;
    private final String timestamp; // ISO-8601 string

    public ChatMessage(User sender, String content, Group group, String timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.group = group;
    }

    // Factory to build from DTO using DatabaseManager
    public static ChatMessage fromDTO(ChatMessageDTO dto) {
        if (dto == null) return null;
        Group group = DatabaseManager.groupManager.getGroup(dto.group);
        // senderId is optional; resolve by name primarily
        User sender = DatabaseManager.userManager.getUser(dto.senderName);
        return new ChatMessage(sender, dto.content, group, dto.timestamp);
    }

    // For wire format serialization
    public ChatMessageDTO toDTO() {
        String senderId = this.sender != null ? this.sender.getId() : null;
        String senderName = this.sender != null ? this.sender.getUsername() : null;
        String groupName = this.group != null ? this.group.getName() : null;
        return new ChatMessageDTO(groupName, senderId, senderName, content, timestamp);
    }

    public User getSender() { return sender; }
    public String getContent() { return content; }
    public String getTimestamp() { return timestamp; }
    public Group getGroup() { return group; }
}
