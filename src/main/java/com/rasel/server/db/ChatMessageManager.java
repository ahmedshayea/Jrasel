package com.rasel.server.db;

import java.util.ArrayList;

/**
 * ChatMessageManager is responsible for managing chat messages in-memory.
 * It provides methods to add messages and retrieve them, either all or filtered
 * by group.
 */
public class ChatMessageManager {

    /**
     * Stores all chat messages in-memory.
     */
    private ArrayList<ChatMessage> messages = new ArrayList<>();

    /**
     * Adds a new chat message to the manager.
     *
     * @param message the ChatMessage to add
     */
    public void addMessage(ChatMessage message) {
        messages.add(message); // Add message to the list
    }

    /**
     * Retrieves all chat messages managed by this instance.
     *
     * @return an ArrayList of all ChatMessage objects
     */
    public ArrayList<ChatMessage> getMessages() {
        return messages;
    }

    /**
     * Retrieves all chat messages that belong to a specific group.
     *
     * @param group the Group to filter messages by
     * @return an ArrayList of ChatMessage objects for the specified group
     */
    public ArrayList<ChatMessage> getMessagesForGroup(Group group) {
        ArrayList<ChatMessage> groupMessages = new ArrayList<>(); // List to hold group messages
        for (ChatMessage message : messages) {
            // Check if the message belongs to the specified group
            if (message.getGroup().equals(group)) {
                groupMessages.add(message);
            }
        }
        return groupMessages;
    }

}
