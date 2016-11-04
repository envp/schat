package schat.server;

import schat.message.*;

import java.net.Socket;

import java.io.PrintStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.io.IOException;

import java.util.logging.Logger;

/**
 *
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class ClientHandler implements Runnable {
    private Socket sock;
    private final int clientId;
    private ObjectInputStream sockIn;
    private ObjectOutputStream sockOut;

    /**
     * Constructor for creating a new ClientHandler instance
     * @param   cSock       Client socket object
     * @param   clientId    Unique ID assigned to the client
     * @return  Creates a new worker instance to handle a single client
     */
    public ClientHandler(Socket cSock, int clientId) {
        this.sock = cSock;
        this.clientId = clientId;
    }

    public void run() {
        try {
            Message message;
            this.sockOut = new ObjectOutputStream(this.sock.getOutputStream());
            this.sockOut.flush();
            this.sockIn = new ObjectInputStream(this.sock.getInputStream());

            while(true) {
                message = (Message) this.sockIn.readObject();
                System.out.println(message.getType());
            }
        }
        catch(ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        catch(IOException ioe) {
            // handle IOE from object input stream
            ioe.printStackTrace();
        }
    }
}
