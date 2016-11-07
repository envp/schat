package schat.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import schat.message.*;


/**
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class Client implements Runnable {
    private String username;
    private Socket sock;
    private BufferedReader stdIn;
    private ObjectInputStream sockIn;
    private ObjectOutputStream sockOut;
    private Message introduction;
    private final String DEFAULT_FILE_SAVE_LOCATION = "download";

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
     * Helper method for processing inbound text messages
     * @param msg Message object containing inbound message
     */
    private void processInboundTextMessage(Message msg) {
        if(msg.isPrintable()) {
            System.out.println(msg);
        }
    }

    /**
     * Helper method for processing inbound file transfers
     * @param msg Message with file to be read from stream
     */
    private void processInboundFileMessage(Message msg, ObjectInputStream in) {
        long size = msg.getPayloadSize();
        int length = 0;
        String fname = msg.getBody();
        byte[] buffer = new byte[Message.MAX_PAYLOAD_SIZE];

        try {
            OutputStream outFile = new FileOutputStream(
                DEFAULT_FILE_SAVE_LOCATION + "/" + this.username + "/" + fname
            );

            // Process file per established protocol of
            // Inform -> send
            while(size > 0 && length != -1) {
                try {
                    length = in.read(buffer, 0, length);
                    outFile.write(buffer);
                    size -= length;
                }
                catch(IOException ioe) {
                    System.out.println("[ERROR] " + ioe.getMessage());
                }
            }
        }
        catch(FileNotFoundException fnfe) {
            System.out.println("[ERROR] Can't find file: " + fnfe.getMessage());
        }
    }

    /**
     * Helper method for processing outbound text messages
     * @param msg Message object containing outbound message
     */
    private void processOutboundTextMessage(Message msg, ObjectOutputStream out) throws IOException {
        msg.setFrom(this.username);
        out.writeObject(msg);
    }

    /**
     * Helper method for processing outbound file transfers
     * @param msg Message with path of file to be dumped into network
     */
    private void processOutboundFileMessage(Message msg, ObjectOutputStream out) throws IOException {
        // Try to create a file stream from the supplied object
        try {
            InputStream file = new FileInputStream(msg.getBody());
            OutputStream sockOut = this.sock.getOutputStream();

            byte[] buffer = new byte[Message.MAX_PAYLOAD_SIZE];
            int length = 0;

            // Tell them the file size to expect
            File f = new File(msg.getBody());
            msg.setPayloadSize(f.length());
            msg.setBody(f.getName());
            out.writeObject(msg);

            while((length = file.read(buffer)) != -1) {
                sockOut.write(buffer, 0, length);
            }
        }
        catch(FileNotFoundException fnfe) {
            System.out.println("[ERROR] Can't find file: " + fnfe.getMessage());
        }
    }
    
    /**
     * Wraps the username negotiation that initially takes place between the 
     * client and server.
     * @param in    ObjectInputStream of the socket being used for the current 
     *              communication session
     * @param out   ObjectOutputStream of the socket being used for the current 
     *              communication session
     */
    private void negotiateUsername(ObjectInputStream in, ObjectOutputStream out) 
            throws IOException, ClassNotFoundException {
        Message message;
        String usrn = this.username;
        // Client introduces itself to the server by telling server
        // it's choice of username
        out.writeObject(this.introduction);
            
        // Next message by protocol is the server's acknowledgement of
        // our introduction. 
        // @TODO    when does this logic of username negotiation break down?
        //          This is central as it also affects the server's user lookup 
        //          functionality
        message = (Message) in.readObject();
        
        while(message.getType() == MessageType.ACK_INTRO) {
            if(message.getBody().equals("OK")) {
                break;
            }
            
            System.out.print("Chosen username (" + this.username + 
                    ") is unavailable. " + "Please choose another one: ");
            usrn = this.stdIn.readLine();
            this.introduction.setFrom(usrn);
            out.writeObject(this.introduction);
        }
        this.username = usrn;
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
            
            negotiateUsername(sockIn, sockOut);
            
            while(true) {
                try {
                    // Read user input from command line
                    message = Message.parseMessage(stdIn.readLine());

                    // Process if not blank
                    if(!message.isBlank()) {
                        System.out.println(message);

                        if(message.isTextMessage()) {
                            processOutboundTextMessage(message, sockOut);
                        }
                        if(message.isFileMessage()) {
                            processOutboundFileMessage(message, sockOut);
                        }
                    }

                    // Read server responses from another socket
                    message = (Message) sockIn.readObject();
                    if(message.isTextMessage()) {
                        processInboundTextMessage(message);
                    }
                    if(message.isFileMessage()) {
                        processInboundFileMessage(message, sockIn);
                    }
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
