package com.rasel.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * A custom panel that renders a single chat message with a "bubble" look.
 * It includes sender, text (with wrapping), and timestamp.
 */
public class MessageBubble extends JPanel {

    private final JLabel senderLabel = new JLabel();
    private final JTextArea messageArea = new JTextArea();
    private final JLabel timeLabel = new JLabel();
    private final JPanel header = new JPanel(new BorderLayout());
    private final Color bubbleColor;
    private static final int MAX_HEIGHT = 260; // clamp very long messages

    public MessageBubble(ChatMessage message) {
        this.bubbleColor = message.isOwnMessage() ? Theme.ACCENT : Theme.BUBBLE_OTHER;

        // --- Style ---
        setLayout(new BorderLayout(4, 4));
    setBorder(new EmptyBorder(10, 14, 10, 14));
        setOpaque(false); // We will draw the rounded background ourselves

        senderLabel.setText(message.getSender());
        senderLabel.setFont(Theme.UI_FONT_BOLD);
    senderLabel.setForeground(message.isOwnMessage() ? Color.WHITE : Theme.MUTED);

        messageArea.setText(message.getText());
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setEditable(false);
        messageArea.setFocusable(false);
        messageArea.setBackground(new Color(0, 0, 0, 0)); // Transparent
    messageArea.setForeground(message.isOwnMessage() ? Color.WHITE : Theme.TEXT);
        messageArea.setFont(Theme.UI_FONT);

        timeLabel.setText(message.getTimestampFormatted());
        timeLabel.setFont(Theme.UI_FONT.deriveFont(11f));
    timeLabel.setForeground(message.isOwnMessage() ? new Color(255,255,255,210) : Theme.MUTED);

        // --- Layout ---
        header.setOpaque(false);
        header.add(senderLabel, BorderLayout.WEST);
        header.add(timeLabel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(messageArea, BorderLayout.CENTER);
    }

    /**
     * Configure the bubble to wrap text to the given max width and compute a suitable preferred size.
     */
    public void configureForWidth(int maxWidth) {
        Insets ins = getInsets();
        int innerW = Math.max(120, maxWidth - (ins.left + ins.right));
        // Size text area to desired width so it computes wrapped preferred height
        messageArea.setSize(new Dimension(innerW, Integer.MAX_VALUE));
        Dimension taPref = messageArea.getPreferredSize();
        Dimension hdrPref = header.getPreferredSize();
        int prefW = Math.min(maxWidth, Math.max(taPref.width, 120) + ins.left + ins.right);
        int prefH = Math.min(MAX_HEIGHT, taPref.height + hdrPref.height + ins.top + ins.bottom + 6);
        setPreferredSize(new Dimension(prefW, prefH));
        setMaximumSize(new Dimension(maxWidth, MAX_HEIGHT));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int w = getWidth();
    int h = getHeight();
    // subtle shadow
    g2.setColor(new Color(0,0,0,30));
    g2.fillRoundRect(2, 3, w - 2, h - 2, 18, 18);
    // bubble fill
    g2.setColor(bubbleColor);
    g2.fillRoundRect(0, 0, w - 4, h - 4, 18, 18);
        g2.dispose();
        super.paintComponent(g);
    }

    // Preferred size is computed in configureForWidth(maxWidth)
}
