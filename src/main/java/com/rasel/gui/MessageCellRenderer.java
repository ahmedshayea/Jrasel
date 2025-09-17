package com.rasel.gui;

import java.awt.*;
import javax.swing.*;

/**
 * A custom ListCellRenderer that uses a MessageBubble component to display ChatMessages.
 * It wraps the bubble in a panel with a FlowLayout to control alignment (left/right).
 */
public class MessageCellRenderer implements ListCellRenderer<ChatMessage> {

    @Override
    public Component getListCellRendererComponent(JList<? extends ChatMessage> list, ChatMessage message, int index,
                                                  boolean isSelected, boolean cellHasFocus) {

        MessageBubble bubble = new MessageBubble(message);

        // Compute an appropriate max width for the bubble based on the list width
        int listWidth = list.getWidth();
        if (listWidth <= 0 && list.getParent() != null) {
            listWidth = list.getParent().getWidth();
        }
        if (listWidth <= 0) listWidth = 600; // fallback during initial layout
        int maxBubbleWidth = Math.max(220, (int) (listWidth * 0.7));
        bubble.configureForWidth(maxBubbleWidth);

        // Use a wrapper panel with FlowLayout to achieve left/right alignment.
    JPanel wrapper = new JPanel(new FlowLayout(message.isOwnMessage() ? FlowLayout.RIGHT : FlowLayout.LEFT, 6, 6));
        wrapper.setOpaque(true); // Must be true for background color to show
        wrapper.add(bubble);

        // Set background based on selection
        if (isSelected) {
            wrapper.setBackground(list.getSelectionBackground());
        } else {
            wrapper.setBackground(list.getBackground());
        }

        return wrapper;
    }
}
