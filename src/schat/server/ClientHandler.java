package schat.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentMap;
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
            "{\"Message\": {\"from\":\"%s\", \"to\":%s, \"type\":\"%s\", \"body':\"%s\"}}%n",
            message.getFrom(), message.getRecipients(),
            message.getType(), message.getBody()
        );
    }
    
    @Override
    public void run() {
        try {
            Message message, temp;
            this.sockOut = new ObjectOutputStream(this.sock.getOutputStream());
            this.sockOut.flush();
            this.sockIn = new ObjectInputStream(this.sock.getInputStream());
            ConcurrentMap<String, ClientHandler> users;
            
            while(true) {
                message = (Message) this.sockIn.readObject();
                ClientHandler.logMessage(message);

                switch(message.getType()) {
                    case CLIENT_INTRODUCTION:
                        temp = new Message(MessageType.ACK_INTRO, "", "");
                        users = Server.getUserList();
                        if(users.containsKey(message.getFrom())) {
                            temp.setBody("N");
                        }
                        else {
                            users.putIfAbsent(message.getFrom(), this);
                            temp.setBody("Y");
                        }
                        this.sockOut.writeObject(temp);
                        temp = null;
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
        catch(ClassNotFoundException | IOException ex) {
            System.err.println("[ERROR]: " + ex.getMessage());
        }
    }
}
