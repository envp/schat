package schat.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import schat.message.*;

/**
 *
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class ClientHandler implements Runnable {
    private Socket sock;
    private final String username;
    private ObjectInputStream sockIn;
    private ObjectOutputStream sockOut;

    /**
     * Constructor for creating a new ClientHandler instance
     * @param   cSock       Client socket object
     * @param   clientId    Unique ID assigned to the client
     * @return  Creates a new worker instance to handle a single client
     */
    public ClientHandler(Socket cSock) {
        this.sock = cSock;
        this.username = "";
    }

    private static void logMessage(Message message) {
        System.out.format(
            "{\"Message\": {\"from\":\"%s\", \"to\":%s, \"type\":\"%s\", \"body':\"%s\"}}",
            message.getFrom(), message.getRecipients(),
            message.getType(), message.getBody()
        );
    }

    public void run() {
        try {
            Message message;
            this.sockOut = new ObjectOutputStream(this.sock.getOutputStream());
            this.sockOut.flush();
            this.sockIn = new ObjectInputStream(this.sock.getInputStream());

            while(true) {
                message = (Message) this.sockIn.readObject();
                ClientHandler.logMessage(message);

                switch(message.getType()) {
                    case CLIENT_INTRODUCTION:
                        break;
                    case CLIENT_TEXT_UNICAST:
                    case CLIENT_TEXT_BROADCAST:
                    case CLIENT_TEXT_BLOCKCAST:
                        break;
                    case CLIENT_FILE_UNICAST:
                    case CLIENT_FILE_BROADCAST:
                    case CLIENT_FILE_BLOCKCAST:
                        break;
                    default:
                        break;
                }
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
