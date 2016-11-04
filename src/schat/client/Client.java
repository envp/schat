package schat.client;

import schat.message.*;
import schat.exception.*;

import java.net.Socket;
import java.net.UnknownHostException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;

import java.util.logging.Logger;

/**
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class Client implements Runnable {
    private String username;
    private Socket sock;
    private BufferedReader stdIn;
    private ObjectInputStream sockIn;
    private ObjectOutputStream sockOut;
    private final PrintStream log = System.out;
    private Message introduction;

    /**
     * Constructor for a new client instance.
     * @param   username Unique string identifier chosen by each client,
     *                   subject to change on server-side availability
     * @param   serverAddress Server IP address passed as a string. Also
     *                        accepts 'localhost' as a form of loopback
     * @param   port Server port number to connect to.
     * @return  A new client instance created with the specified parameters
     */
    public Client(String username, int port, String serverAddress) throws IOException {
        this.username = username;
        this.sock = new Socket(serverAddress, port);
        this.stdIn = new BufferedReader(new InputStreamReader(System.in));
        this.introduction = new Message(MessageType.CLIENT_INTRODUCTION, "", username);
    }

    /**
     * Client constructor for handling connections to 127.0.0.1. Alias for
     * Client(username,port, "127.0.0.1")
     * @param   username Unique string identifier chosen by each client,
     *                   subject to change on server-side availability
     * @param   port Server port number to connect to.
     * @return  A new client instance which connects to a localhost server
     */
    public Client(String username, int port) throws IOException {
        this(username, port, "127.0.0.1");
    }

    /**
     * Accessor method for username associated with this client instance
     * @return username string associated with the current instance
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * There are two actions that happen in the run method.
     *
     * First: The client sends what is called an "introduction" message with
     * it's username, that is checked by the server for availability.
     *
     * Next: the read loop begins
     */
    public void run() {
        try {
            Message message;
            sockOut = new ObjectOutputStream(sock.getOutputStream());
            sockOut.flush();
            sockIn =  new ObjectInputStream(sock.getInputStream());

            // Client introduces itself to the server by telling server
            // it's choice of username
            sockOut.writeObject(this.introduction);

            while(true) {
                try {
                    message = Message.parseMessage(stdIn.readLine());
                    if(message.getBody() != null) {
                        System.out.println(message);
                        message.setFrom(this.username);
                        sockOut.writeObject(message);
                    }

                    // Print out what we read from the socket
                    System.out.println(sockIn.readObject());
                }
                catch(IllegalMessageException ime) {
                    System.out.println("[ERROR] " + ime.getMessage());
                }
            }
        }
        catch(IOException ioe) {
            System.err.println("[ERROR] " + ioe.getMessage());
        }
        catch(ClassNotFoundException cnfe) {
            System.err.println(cnfe.getMessage());
        }
    }
}
