package schat.server;

import schat.client.ClientInfo;
import java.net.Socket;

/**
 *
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class ClientHandler implements Runnable {
    private Socket sock;
    private final int clientId;
    private ClientInfo info;

    /**
     * Constructor for creating a new ClientHandler instance
     * @param   cSock       Client socket object
     * @param   clientId    Unique ID assigned to the client
     * @return  Creates a new worker instance to handle a single client
     */
    public ClientHandler(Socket cSock, int clientId) {
        this.sock = cSock;
        this.clientId = clientId;
        this.info = new ClientInfo();
    }

    /**
     *
     */
    public void run() {
        if(this.info.isUnset()) {
            // Ask client for info
        }
    }
}
