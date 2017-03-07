package client;

import chat.res.HelperMethods;
import chat.res.Message;
import chat.res.MessageType;
import chat.res.sender.Internal;
import chat.res.sender.Sender;
import chat.res.sender.User;
import sun.net.util.IPAddressUtil;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatMain extends JFrame {

    public static final int port = 5000;

    private static ChatMain instance;

    private Socket sock;
    private ObjectOutputStream writer;
    private String serverAddress = "127.0.0.1";

    private boolean isConnected, isConnecting;
    private boolean typingProtectedMessage;

    Sender internalSender;

    private User recipient;


    private HTMLDocument chatDoc;
    private HTMLEditorKit chatKit;
    private IncomingReader reader;

    DefaultListModel<User> userListModel = new DefaultListModel<>();
    Map<String, User> userList = new HashMap<>();
    User user;

    // Swing variables declaration
    protected JTextPane chatTextArea;
    protected JButton connectButton;
    protected JButton disconnectButton;
    protected JTextArea inputTextArea;
    protected JLabel jLabelUsername;
    protected JLabel jLabelOnlineUsers;
    protected JScrollPane inputScrollPane;
    protected JScrollPane chatScrollPane;
    protected JButton sendButton;
    protected JTextField usernameField;
    protected JList<User> usersList;

    private ChatMain() {

        internalSender = new Internal("[INTERNAL]", Color.RED);

        setFont(new Font("Calibri", Font.PLAIN, 14));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (isConnected) disconnect(false);
                e.getWindow().dispose();
            }
        });

        initComponents();

        setSize(618, 461);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        setLocationRelativeTo(null);

        String enterIPMessage = "Enter the ip address of the server to connect to. \nClick cancel to use localhost";
        String ipIn = JOptionPane.showInputDialog(enterIPMessage);

        while (ipIn != null && !isIPAddress(ipIn)) {
            ipIn = JOptionPane.showInputDialog("Error: Invalid IP Address.\n" + enterIPMessage);
        }

        if (ipIn != null) serverAddress = ipIn;

        isConnected = false;
        isConnecting = false;
        typingProtectedMessage = false;
    }

    void userAdd(User data) {
        userList.put(data.getName(), data);
    }

    void userRemove(User user) {
        userList.remove(user.getName());
    }

    private void writeUsers() {
        for (Entry<String, User> pair : userList.entrySet())
            userListModel.addElement(pair.getValue());
    }

    void clearTextArea() {
        chatTextArea.removeAll();
    }

    void reloadUsers() {
        userListModel.removeAllElements();
        writeUsers();
    }

    void disconnect(boolean serverStop) {
        Message message;
        try {
            if (!serverStop) {
                message = new Message(user, MessageType.DISCONNECT);
                sendMessage(message);
            }
            writer.close();
            reader.stop();
            sock.close();
            isConnected = false;
        } catch (IOException e) {
            e.printStackTrace();
            showUserMessage(internalSender, "Could not disconnect");
        }
        usernameField.setEditable(true);
        userListModel.removeAllElements();
        userList.clear();

        String disconnectMsg;
        if (serverStop)
            disconnectMsg = "You have been disconnected by the server";
        else
            disconnectMsg = "You are now disconnected";

        showUserMessage(internalSender, disconnectMsg);
    }

    private void initComponents() {

        inputScrollPane = new javax.swing.JScrollPane();
        inputTextArea = new javax.swing.JTextArea();
        chatScrollPane = new javax.swing.JScrollPane();
        chatScrollPane
                .setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        jLabelUsername = new javax.swing.JLabel();
        usernameField = new javax.swing.JTextField();
        connectButton = new javax.swing.JButton();
        disconnectButton = new javax.swing.JButton();
        sendButton = new javax.swing.JButton();
        jLabelOnlineUsers = new javax.swing.JLabel();

        setTitle("Chat Client");

        inputTextArea.setColumns(20);
        inputTextArea.setLineWrap(true);
        inputTextArea.setWrapStyleWord(true);
        inputTextArea.setRows(5);
        inputTextArea.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendButtonActionPerformed(null);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    inputTextArea.setText("");
                }
            }

        });

        inputScrollPane.setViewportView(inputTextArea);

        jLabelUsername.setText("Username:");

        usernameField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                usernameFieldActionPerformed(evt);
            }
        });

        connectButton.setText("Connect");
        connectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });

        disconnectButton.setText("Disconnect");
        disconnectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disconnectButtonActionPerformed(evt);
            }
        });

        sendButton.setText("Send");
        sendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButtonActionPerformed(evt);
            }
        });

        jLabelOnlineUsers
                .setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelOnlineUsers.setText("Online Users");

        UserListRenderer usersListRenderer = new UserListRenderer();
        usersList = new JList<>(userListModel);
        usersList.setCellRenderer(usersListRenderer);

        final JPopupMenu singleMessageMenu = new JPopupMenu("Popup");

        usersList.addMouseListener(new MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {

                    JMenuItem item = new JMenuItem("Send Message");
                    item.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            String selectedUser = usersList.getSelectedValue()
                                    .toString();
                            sendProtectedMessage(selectedUser);

                        }

                    });

                    singleMessageMenu.removeAll();
                    singleMessageMenu.add(item);
                    singleMessageMenu.show(usersList, e.getX(), e.getY());

                }
            }

        });
        usersList.setBackground(Color.WHITE);
        usersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(
                getContentPane());
        layout.setHorizontalGroup(layout
                .createParallelGroup(Alignment.TRAILING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(
                                        layout.createParallelGroup(
                                                Alignment.TRAILING)
                                                .addGroup(
                                                        Alignment.LEADING,
                                                        layout.createSequentialGroup()
                                                                .addGap(4)
                                                                .addComponent(
                                                                        jLabelUsername,
                                                                        GroupLayout.PREFERRED_SIZE,
                                                                        68,
                                                                        GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        ComponentPlacement.RELATED)
                                                                .addComponent(
                                                                        usernameField,
                                                                        GroupLayout.PREFERRED_SIZE,
                                                                        153,
                                                                        GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        ComponentPlacement.RELATED)
                                                                .addComponent(
                                                                        connectButton)
                                                                .addPreferredGap(
                                                                        ComponentPlacement.RELATED)
                                                                .addComponent(
                                                                        disconnectButton,
                                                                        GroupLayout.PREFERRED_SIZE,
                                                                        105,
                                                                        GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        ComponentPlacement.RELATED))
                                                .addGroup(
                                                        layout.createSequentialGroup()
                                                                .addComponent(
                                                                        inputScrollPane,
                                                                        GroupLayout.DEFAULT_SIZE,
                                                                        340,
                                                                        Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                        ComponentPlacement.UNRELATED)
                                                                .addComponent(
                                                                        sendButton,
                                                                        GroupLayout.PREFERRED_SIZE,
                                                                        69,
                                                                        GroupLayout.PREFERRED_SIZE))
                                                .addComponent(
                                                        chatScrollPane,
                                                        Alignment.LEADING,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        419, Short.MAX_VALUE))
                                .addGap(18)
                                .addGroup(
                                        layout.createParallelGroup(
                                                Alignment.LEADING, false)
                                                .addComponent(
                                                        jLabelOnlineUsers,
                                                        GroupLayout.PREFERRED_SIZE,
                                                        149,
                                                        GroupLayout.PREFERRED_SIZE)
                                                .addComponent(
                                                        usersList,
                                                        GroupLayout.PREFERRED_SIZE,
                                                        141,
                                                        GroupLayout.PREFERRED_SIZE))
                                .addContainerGap()));
        layout.setVerticalGroup(layout
                .createParallelGroup(Alignment.LEADING)
                .addGroup(
                        layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(
                                        layout.createParallelGroup(
                                                Alignment.BASELINE)
                                                .addComponent(
                                                        jLabelUsername,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        26, Short.MAX_VALUE)
                                                .addComponent(
                                                        jLabelOnlineUsers,
                                                        GroupLayout.PREFERRED_SIZE,
                                                        26,
                                                        GroupLayout.PREFERRED_SIZE)
                                                .addComponent(
                                                        usernameField,
                                                        GroupLayout.PREFERRED_SIZE,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        GroupLayout.PREFERRED_SIZE)
                                                .addComponent(connectButton)
                                                .addComponent(disconnectButton))
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addGroup(
                                        layout.createParallelGroup(
                                                Alignment.LEADING)
                                                .addGroup(
                                                        layout.createSequentialGroup()
                                                                .addComponent(
                                                                        chatScrollPane,
                                                                        GroupLayout.DEFAULT_SIZE,
                                                                        261,
                                                                        Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                        ComponentPlacement.RELATED)
                                                                .addGroup(
                                                                        layout.createParallelGroup(
                                                                                Alignment.LEADING,
                                                                                false)
                                                                                .addComponent(
                                                                                        sendButton,
                                                                                        GroupLayout.DEFAULT_SIZE,
                                                                                        GroupLayout.DEFAULT_SIZE,
                                                                                        Short.MAX_VALUE)
                                                                                .addComponent(
                                                                                        inputScrollPane,
                                                                                        GroupLayout.DEFAULT_SIZE,
                                                                                        96,
                                                                                        Short.MAX_VALUE)))
                                                .addComponent(
                                                        usersList,
                                                        GroupLayout.DEFAULT_SIZE,
                                                        363, Short.MAX_VALUE))
                                .addContainerGap()));

        chatTextArea = new JTextPane();
        chatScrollPane.setViewportView(chatTextArea);
        chatTextArea.setEditable(false);
        chatTextArea.setFont(new Font("Calibri", Font.PLAIN, 14));
        chatKit = new HTMLEditorKit();
        chatDoc = new HTMLDocument();

        chatTextArea.setEditorKit(chatKit);
        chatTextArea.setDocument(chatDoc);

        getContentPane().setLayout(layout);

        pack();
    }

    private void sendProtectedMessage(String tempRecipient) {

        if (tempRecipient == null || tempRecipient.isEmpty()) {
            showUserMessage(internalSender,
                    "You must select a recipient from the list");
        } else if (tempRecipient.equals(user.getName())) {
            showUserMessage(internalSender,
                    "You cannot send a message to yourself");
        } else {
            typingProtectedMessage = true;
            recipient = userList.get(tempRecipient);
            showUserMessage(
                    internalSender,
                    "Ready to send "
                            + recipient.getName()
                            + " a protected message. Type \"CANCEL\" to cancel the protected message.");
        }
    }

    private void usernameFieldActionPerformed(ActionEvent evt) {
        connectButtonActionPerformed(evt);
    }

    private boolean checkIllegalCharacters(String s) {
        Pattern pattern = Pattern.compile("[~#@*+%{}<>\\[\\]|\"\\^,./()=:;`]");
        Matcher matcher = pattern.matcher(s);
        return matcher.find();
    }

    private boolean isIPAddress(String s) {
        return IPAddressUtil.isIPv4LiteralAddress(s);
    }

    private void connectButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (isConnecting) return;

        boolean containsIllegalCharacters;
        user = new User(usernameField.getText().trim());
        containsIllegalCharacters = checkIllegalCharacters(user.getName());

        if (user.getName().length() <= 3) {

            showUserMessage(internalSender,
                    "Please input a username of at least 5 characters");
            usernameField.setText("");
            usernameField.requestFocus();
            return;
        } else if (user.getName().length() > 18) {
            showUserMessage(internalSender,
                    "Please input a username of under 18 characters");
            usernameField.setText("");
            usernameField.requestFocus();
            return;
        } else if (containsIllegalCharacters) {
            showUserMessage(internalSender,
                    "Your username contains illegal characters");
            usernameField.setText("");
            usernameField.requestFocus();
            return;
        }

        if (!isConnected) {
            usernameField.setEditable(false);
            connect();
        } else {
            showUserMessage(internalSender, "You are already connected!");
            inputTextArea.requestFocus();
        }
    }

    public void connect() {
        isConnecting = true;
        showUserMessage(internalSender, "Attempting connection to server...");
        Runnable runConnect = new Runnable() {
            @Override
            public void run() {
                try {

                    sock = new Socket();

                    sock.connect(new InetSocketAddress(serverAddress, port), 1000);

                    reader = new IncomingReader(sock);
                    new Thread(reader).start();

                    writer = new ObjectOutputStream(sock.getOutputStream());

                    Message connectMessage = new Message(user, MessageType.CONNECT);

                    if (!sendMessage(connectMessage)) {
                        showUserMessage(internalSender, "Error: Unable to connect to server");
                        failedConnect();
                        return;
                    }

                    isConnected = true;
                    isConnecting = false;

                    showUserMessage(internalSender, "You are now connected!");

                    inputTextArea.requestFocus();

                } catch (Exception e) {
                    showUserMessage(internalSender, "Error: Unable to connect to server");
                    failedConnect();
                }
            }
        };
        new Thread(runConnect).start();

    }

    public void failedConnect() {
        isConnected = false;
        isConnecting = false;
        usernameField.setEditable(true);
    }

    public void disconnectButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (isConnected) {
            disconnect(false);
        }
    }

    public void showUserMessage(Sender sender, String message) {
        showUserMessage(sender, message, false);
    }

    public void showUserMessage(Sender sender, String message,
                                boolean isProtected) {
        Color color = sender.getDisplayColor();

        String hex = HelperMethods.colorToHex(color);

        try {
            chatKit.insertHTML(
                    chatDoc,
                    chatDoc.getLength(),
                    "<strong><font size=\"4\" face=\"calibri\" color=\""
                            + hex
                            + "\">"
                            + sender
                            + "</font></strong><font size=\"4\" face=\"calibri\">"
                            + (isProtected ? "(protected)" : "")
                            + ":</font><font size=\"3\" face=\"calibri\"> "
                            + message + "</font>", 0, 0, null);

        } catch (BadLocationException | IOException e) {
            e.printStackTrace();
        }

        chatTextArea.setCaretPosition(chatDoc.getLength());
    }

    private void sendButtonActionPerformed(java.awt.event.ActionEvent evt) {

        if (!isConnected) {
            showUserMessage(internalSender, "You must be connected");
            inputTextArea.setText("");

            usernameField.requestFocus();
            return;
        }
        String text = inputTextArea.getText().trim();
        if (text.isEmpty()) {
            inputTextArea.requestFocus();
        } else if (text.length() > 200) {
            showUserMessage(internalSender,
                    "Message too long; cannot be over 200 characters long.");
        } else {
            Message message;
            if (typingProtectedMessage) {
                if (text.equalsIgnoreCase("cancel")) {
                    typingProtectedMessage = false;
                    recipient = null;
                    showUserMessage(internalSender, "Message Successfully Canceled");
                } else {
                    message = new Message(user, recipient, text,
                            MessageType.MESSAGE);
                    if (sendMessage(message)) {
                        showUserMessage(internalSender,
                                "Message to " + recipient.getName()
                                        + " was sent successfully");
                    } else {
                        showUserMessage(internalSender,
                                "Cancelling private message Status.");

                        typingProtectedMessage = false;
                        recipient = null;
                    }
                }
            } else {
                message = new Message(user, text,
                        MessageType.MESSAGE);
                sendMessage(message);
            }

            inputTextArea.setText("");
            inputTextArea.requestFocus();
        }
        inputTextArea.setText("");
        inputTextArea.requestFocus();
    }

    private boolean sendMessage(Message msg) {
        try {
            writer.writeObject(msg);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            showUserMessage(internalSender,
                    "ERROR: Message could not be sent. ");
            return false;
        }
        return true;
    }

    public static ChatMain getInstance() {
        if (instance == null) instance = new ChatMain();
        return instance;
    }

    public static void main(String args[]) {
        Runnable runChat = new Runnable() {
            @Override
            public void run() {
                ChatMain main = ChatMain.getInstance();
                main.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                main.setVisible(true);
            }
        };

        SwingUtilities.invokeLater(runChat);
    }

}