package server;

import chat.res.Message;
import chat.res.MessageType;
import chat.res.sender.Sender;
import chat.res.sender.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {

    private ServerMain server;

    private Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private User user;

    private boolean shouldRun;

    public ClientHandler(Socket socket) {
        shouldRun = true;
        server = ServerMain.getInstance();
        this.socket = socket;
        try {
            outputStream = new ObjectOutputStream(this.socket.getOutputStream()); // sends messages to client
            inputStream = new ObjectInputStream(this.socket.getInputStream()); // reads messages from client
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        Message message;
        MessageType msgType;
        Sender sender;

        while (shouldRun) {
            try {
                message = (Message) inputStream.readObject(); // read the message from the client
                msgType = message.getMessageType();
                sender = message.getSender();

                server.log("Received: " + message + " from " + message.getSender()); // log the message

                if (msgType.equals(MessageType.CONNECT)) { // client sent their connect message
                    User user;
                    if (!(sender instanceof User)) continue; // ensure sender is a user
                    user = (User) sender;
                    setUser(user); // set the user associated with this client

                    boolean successfulAdd = server.userAdd(user, this); // add the user
                    if (successfulAdd) {
                        // tell everyone the new user connected
                        server.broadcastMessage(new Message(server.serverSender,
                                message.getSender().getName() + " has joined the chat.", MessageType.MESSAGE));

                    }

                } else if (msgType.equals(MessageType.DISCONNECT)) { // client sent their disconnect message
                    User user;
                    if (!(sender instanceof User)) continue; // ensure sender is a user
                    user = (User) sender;

                    server.userRemove(user); // remove the user

                    // tell everyone the user disconnected
                    server.broadcastMessage(new Message(server.serverSender,
                            user + " has disconnected.", MessageType.MESSAGE));


                } else if (msgType.equals(MessageType.MESSAGE)) { // the client sent a message
                    if (message.getReceiver() == null) // message intended for the whole chat
                        server.broadcastMessage(message);
                    else // message intended for a single recipient
                        server.sendPrivateMessage(message);

                } else
                    server.log("Invalid message received from " + sender);
            } catch (SocketException ignored) { // SocketException caught when stop() is called
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * Stops listening for messages and
     * closes the socket and its input
     * and output streams.
     *
     * @throws IOException
     */
    void stop() throws IOException {
        shouldRun = false;
        inputStream.close();
        outputStream.close();
        socket.close();
    }

    ObjectOutputStream getObjectOutputStream() {
        return outputStream;
    }

    User getUser() {
        return user;
    }

    void setUser(User user) {
        this.user = user;
    }
}