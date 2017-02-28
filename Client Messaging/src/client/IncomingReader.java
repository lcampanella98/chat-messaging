package client;

import chat.res.LoadUsersMessage;
import chat.res.Message;
import chat.res.MessageType;
import chat.res.sender.Sender;
import chat.res.sender.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;

public class IncomingReader implements Runnable {
    private ChatMain chatMain;

    private ObjectInputStream reader;
    private boolean shouldRun = true;
    private Socket sock;
    private Message incomingMsg;

    public IncomingReader(Socket sock) {
        chatMain = ChatMain.getInstance();
        try {

            InputStream stream = sock.getInputStream();

            reader = new ObjectInputStream(stream);

            this.sock = sock;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void run() {
        MessageType mType;
        Sender sender;
        while (shouldRun) {
            try {
                incomingMsg = (Message) reader.readObject();
                sender = incomingMsg.getSender();
                mType = incomingMsg.getMessageType();
                if (mType.equals(MessageType.MESSAGE)) {

                    if (incomingMsg.getReceiver() == null) {
                        chatMain.showUserMessage(incomingMsg.getSender(),
                                incomingMsg.getMessageText());
                    } else if (incomingMsg.getReceiver().getName().equals(chatMain.user.getName())) {
                        String incomingMessage = incomingMsg.getMessageText();
                        chatMain.showUserMessage(incomingMsg.getSender(),
                                incomingMessage, true);
                    }

                } else if (mType.equals(MessageType.SERVER_STOP)) {
                    chatMain.disconnect(true);
                } else if (mType.equals(MessageType.CONNECT)) {
                    if (sender instanceof User) {
                        chatMain.userAdd((User) sender);
                        chatMain.reloadUsers();
                    }
                } else if (mType.equals(MessageType.DISCONNECT)) {
                    if (sender instanceof User) {
                        chatMain.userRemove((User)sender);
                        chatMain.reloadUsers();
                    }
                } else if (mType.equals(MessageType.FAILED)) {
                    String incomingMessage = incomingMsg.getMessageText();

                    chatMain.showUserMessage(incomingMsg.getSender(), incomingMessage);
                    chatMain.usernameField.requestFocus();
                    chatMain.failedConnect();
                } else if (mType.equals(MessageType.LOAD_USERS)) {
                    LoadUsersMessage loadMessage;
                    if (incomingMsg instanceof LoadUsersMessage) {
                        loadMessage = (LoadUsersMessage) incomingMsg;
                        for (User user : loadMessage.getUserData())
                            chatMain.userAdd(user);
                        chatMain.reloadUsers();
                    }
                }

            } catch (SocketException e) {
                break;
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                break;
            }
        }

    }

    public void stop() throws IOException {
        shouldRun = false;
        reader.close();
        sock.close();
    }
}
