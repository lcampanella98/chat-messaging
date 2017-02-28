package chat.res.sender;

import java.awt.*;
import java.io.Serializable;

public class User extends Sender {
    public User(String userName) {
        super(userName);
    }
    public User(String userName, Color color) {
        super(userName, color);
    }
}
