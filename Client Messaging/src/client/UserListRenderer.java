package client;

import chat.res.sender.User;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import java.awt.Component;


class UserListRenderer extends JLabel implements ListCellRenderer<User> {

    public Component getListCellRendererComponent(
            JList<? extends User> list, // the list
            User user, // user to display
            int index, // cell index
            boolean isSelected, // is the cell selected
            boolean cellHasFocus) // does the cell have focus
    {
        String s = user.toString();
        setText(s);

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(user.getDisplayColor());
        }

        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setOpaque(true);
        return this;
    }
}
