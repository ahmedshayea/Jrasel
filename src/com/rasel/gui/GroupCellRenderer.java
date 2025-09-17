package com.rasel.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Light-themed renderer for group items with a subtle rounded background when selected.
 */
public class GroupCellRenderer extends JPanel implements ListCellRenderer<String> {
    private final JLabel label = new JLabel();

    public GroupCellRenderer() {
        setLayout(new BorderLayout());
        setOpaque(true);
        label.setBorder(new EmptyBorder(8, 10, 8, 10));
        label.setFont(Theme.UI_FONT);
        add(label, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        label.setText(value);
        label.setForeground(Theme.TEXT);

        // Background styling
        if (isSelected) {
            setBackground(Theme.SELECTION);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER),
                    new EmptyBorder(4, 6, 4, 6)
            ));
        } else {
            setBackground(list.getBackground());
            setBorder(new EmptyBorder(4, 6, 4, 6));
        }
        return this;
    }
}
