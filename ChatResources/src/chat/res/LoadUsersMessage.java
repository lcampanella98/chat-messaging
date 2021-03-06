package chat.res;

import chat.res.sender.Sender;
import chat.res.sender.User;

import java.util.ArrayList;

public class LoadUsersMessage extends Message {

    private ArrayList<User> users;

    public LoadUsersMessage(Sender sender, Sender receiver, ArrayList<User> userData) {
        super(sender, receiver, MessageType.LOAD_USERS);
        users = userData;
    }

    public ArrayList<User> getUserData() {
        return users;
    }
}
