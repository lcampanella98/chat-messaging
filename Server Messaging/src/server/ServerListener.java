package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

public class ServerListener implements Runnable {

    public static final int port = 5000;

    private ServerMain server;
    private ServerSocket serverSock;
    private boolean shouldRun;

    public ServerListener() {
        server = ServerMain.getInstance();
        server.clients = new HashMap<>();
        try {
            serverSock = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        shouldRun = true;
    }

    /**
     * Listens for connections to the server
     * and runs listeners for the clients
     */
    public void run() {
        Socket clientSock;
        ClientHandler listener;
        while (shouldRun) {
            try {
                clientSock = serverSock.accept(); // accept connection
                listener = (new ClientHandler(clientSock)); // create new instance of ClientHandler

                new Thread(listener).start(); // start a new thread to listen for messages

                server.log("Got a connection from " + clientSock.getInetAddress());
            } catch (SocketException ignored) { // SocketException caught when stop() is called
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

    }

    /**
     * Stops listening for connections
     * @throws IOException
     */
    public void stop() throws IOException {
        shouldRun = false;
        serverSock.close();
    }
}
