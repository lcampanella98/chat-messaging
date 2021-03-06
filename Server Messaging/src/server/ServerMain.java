package server;

import chat.res.LoadUsersMessage;
import chat.res.Message;
import chat.res.MessageType;
import chat.res.sender.ServerSender;
import chat.res.sender.User;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Map;

public final class ServerMain extends JFrame {

    private static ServerMain instance; // singleton instance

    private ServerListener serverListener; // listener for connections

    Map<String, ClientHandler> clients; // map of user-names to clients

    ServerSender serverSender; // sender object used for server messages

    private static Color[] userColors = {
            new Color(61, 90, 255),
            new Color(54, 199, 189), new Color(46, 219, 107),
            new Color(86, 201, 32), new Color(202, 217, 43),
            new Color(255, 162, 14), new Color(230, 48, 90),
            new Color(237, 47, 168), new Color(210, 26, 217),
            new Color(144, 39, 230)}; // assign to each new user a predefined color

    private int colorIndex;

    // Swing variables declarations
    protected JScrollPane jScrollPane;
    protected JTextArea outputPane;
    protected JButton startButton;
    protected JButton stopButton;

    private ServerMain() {
        setResizable(false);

        initComponents();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                serverStop();
                e.getWindow().dispose();
            }
        });

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); // sets "look and feel" of GUI
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        setLocationRelativeTo(null); // centers the window

        serverSender = new ServerSender("[SERVER]", Color.RED);
        colorIndex = 0;
    }

    /**
     * Initialize all java swing components (generated by window-builder)
     */
    private void initComponents() {

        jScrollPane = new javax.swing.JScrollPane();
        outputPane = new javax.swing.JTextArea();
        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();

        setTitle("Chat Server");

        outputPane.setColumns(20);
        outputPane.setEditable(false);
        outputPane.setLineWrap(true);
        outputPane.setRows(5);
        outputPane.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jScrollPane.setViewportView(outputPane);

        startButton.setText("Start");
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        stopButton.setText("Stop");
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(
                getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(
                                                        layout.createSequentialGroup()
                                                                .addComponent(
                                                                        startButton,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                        179,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18,
                                                                        18)
                                                                .addComponent(
                                                                        stopButton,
                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                        183,
                                                                        Short.MAX_VALUE))
                                                .addComponent(
                                                        jScrollPane,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        380, Short.MAX_VALUE))
                                .addContainerGap()));
        layout.setVerticalGroup(layout
                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane,
                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                        229,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(
                                        layout.createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(startButton)
                                                .addComponent(stopButton))
                                .addContainerGap(19, Short.MAX_VALUE)));
        pack();
    }

    /**
     * Starts the server on a new thread
     * if not already started
     *
     * @param evt
     */
    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (serverListener != null) return; // server already running

        serverListener = new ServerListener();
        new Thread(serverListener).start(); // listen for connections to the server

        log("Server started. Listening for connections...");
    }

    /**
     * Stops the server if running
     * and notifies all clients
     *
     * @param evt
     */
    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (serverListener == null) return; // server already stopped

        broadcastMessage(new Message(serverSender,
                "Server is stopping and all users will be disconnected",
                MessageType.MESSAGE));

        log("Server stopping... ");

        serverStop();

        colorIndex = 0;
        log("Server stopped");
    }

    /**
     * Broadcasts the SERVER_STOP message
     * to all clients and stops the server
     */
    private void serverStop() {
        broadcastMessage(new Message(serverSender, MessageType.SERVER_STOP)); // broadcast the SERVER_STOP

        try {
            if (clients != null) {
                for (ClientHandler handler : clients.values()) {
                    handler.stop(); // stop() each client
                }
                clients.clear();
            }

            if (serverListener != null) {
                serverListener.stop(); // stop() the serverListener
                serverListener = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tells all clients that a new user has connected.
     *
     * @param user          The user that just connected
     * @param clientHandler The handler for the user
     * @return Whether or not the user could be successfully added.
     */
    boolean userAdd(User user, ClientHandler clientHandler) {
        Message message;

        if (clients.containsKey(user.getName())) { // user-name (case sensitive) already taken
            message = new Message(serverSender, user,
                    "Someone has already taken that username", MessageType.FAILED);
            sendPrivateMessage(message);
            return false;
        }

        if (colorIndex >= userColors.length) colorIndex = 0; // reset colorIndex if need be
        user.setDisplayColor(userColors[colorIndex++]); // set the display color of the user

        message = new Message(user, MessageType.CONNECT);
        broadcastMessage(message); // tell all clients the user connected

        clients.put(user.getName(), clientHandler); // put client handler into clients

        ArrayList<User> allUsers = new ArrayList<>(clients.size());

        for (ClientHandler handler : clients.values())
            allUsers.add(handler.getUser());

        message = new LoadUsersMessage(serverSender, user, allUsers); // give the new user the list of users
        sendPrivateMessage(message);

        return true;
    }

    /**
     * Disconnects from the user and
     * broadcasts the disconnection to all clients.
     *
     * @param user The user that disconnected
     */
    void userRemove(User user) {

        try {
            clients.get(user.getName()).stop(); // stop the client listener
        } catch (IOException ignored) {
        }

        clients.remove(user.getName()); // remove from clients map

        Message message = new Message(user, MessageType.DISCONNECT);
        broadcastMessage(message); // tell all clients the user disconnected
    }

    /**
     * Broadcasts a message to all clients currently connected.
     *
     * @param message The message to broadcast
     */
    void broadcastMessage(Message message) {
        if (clients == null) return;

        log("Broadcasting: [" + message + "]");

        for (ClientHandler handler : clients.values()) {
            try {
                ObjectOutputStream writer = handler.getObjectOutputStream();
                writer.writeObject(message);
                writer.flush();
            } catch (Exception e) {
                e.printStackTrace();
                log("Error broadcasting message to " + handler.getUser());
            }
        }
    }

    /**
     * Sends a private message intended for only one recipient.
     *
     * @param message The message to send
     */
    void sendPrivateMessage(Message message) {
        if (clients == null) return;
        try {
            log("Sending message: [" + message.getMessageText()
                    + "] from " + message.getSender() + " to " + message.getReceiver()); // logs the message

            ObjectOutputStream writer = clients.get(message.getReceiver().getName()).getObjectOutputStream();
            writer.writeObject(message); // write message
            writer.flush(); // flush the stream
        } catch (Exception e) {
            e.printStackTrace();
            log("Error sending message from "
                    + message.getSender().getName() + " to "
                    + message.getReceiver().getName());
        }
    }

    /**
     * Logs some text to the server output pane
     *
     * @param message The text to log
     */
    void log(String message) {
        outputPane.append(message + "\n");
        outputPane.setCaretPosition(outputPane.getDocument().getLength()); // scroll the output pane to the bottom
    }

    /**
     * Using singleton design pattern, gets
     * or creates the ServerMain instance.
     *
     * @return The ServerMain instance
     */
    static ServerMain getInstance() {
        if (instance == null) instance = new ServerMain();
        return instance;
    }

    /**
     * Main method to instantiate ServerMain and show the GUI
     *
     * @param args Command line arguments, not looked at
     */
    public static void main(String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ServerMain main = ServerMain.getInstance();
                main.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
                main.setVisible(true);
            }
        });
    }
}