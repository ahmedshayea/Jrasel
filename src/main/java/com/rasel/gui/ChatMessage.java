package com.rasel.gui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * A simple data object representing a single chat message.
 */
public class ChatMessage {
    private final String sender;
    private final String text;
    private final LocalTime timestamp;
    private final boolean isOwnMessage;

    public ChatMessage(String sender, String text, boolean isOwnMessage) {
        this.sender = sender;
        this.text = text;
        this.timestamp = LocalTime.now();
        this.isOwnMessage = isOwnMessage;
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public String getTimestampFormatted() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public boolean isOwnMessage() {
        return isOwnMessage;
    }
}
