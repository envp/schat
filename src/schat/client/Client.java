package schat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import schat.message.*;

/**
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class Client implements Runnable
{
    private String username;
    private Socket sock;
    private BufferedReader stdIn;
    private ObjectInputStream sockIn;
    private ObjectOutputStream sockOut;
    private Message introduction;

    /**
     * Constructor for a new client instance.
     *
     * @param username Unique string identifier chosen by each client, subject
     * to change on server-side availability
     * @param serverAddress Server IP address passed as a string. Also accepts
     * 'localhost' as a form of loopback
     * @param port Server port number to connect to.
     * @throws java.io.IOException
     */
    public Client(String username, int port, String serverAddress) throws IOException
    {
        this.username = username;
        this.sock = new Socket(serverAddress, port);
        this.stdIn = new BufferedReader(new InputStreamReader(System.in));
        this.introduction = new Message(MessageType.CLIENT_INTRODUCTION, "", username);
    }

    /**
     * Client constructor for handling connections to 127.0.0.1. Alias for
     * Client(username,port, "127.0.0.1")
     *
     * @param username Unique string identifier chosen by each client, subject
     * to change on server-side availability
     * @param port Server port number to connect to.
     * @throws java.io.IOException
     */
    public Client(String username, int port) throws IOException
    {
        this(username, port, "127.0.0.1");
    }

    /**
     * Accessor method for username associated with this client instance
     *
     * @return username string associated with the current instance
     */
    public String getUsername()
    {
        return this.username;
    }

    /**
     * Wraps the username negotiation that initially takes place between the
     * client and server.
     *
     * @param in ObjectInputStream of the socket being used for the current
     * communication session
     * @param out ObjectOutputStream of the socket being used for the current
     * communication session
     */
    private void negotiateUsername(ObjectInputStream in, ObjectOutputStream out)
        throws IOException, ClassNotFoundException
    {
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
        while (true)
        {
            message = (Message) in.readObject();
            if (MessageType.ACK_INTRO == message.getType())
            {
                if (message.getBody().equals("Y"))
                {
                    System.out.format("[INFO] Server accepted chosen username (%s). Connection completed.%n", this.username);
                    return;
                }
                else
                {
                    System.out.format("[INFO] Chosen username (%s) is unavailable. Please choose another one: ", usrn);
                    usrn = this.stdIn.readLine();
                    System.out.format("[INFO] Negotiating username choice (%s)%n", usrn);
                    this.username = usrn;
                    this.introduction = new Message(MessageType.CLIENT_INTRODUCTION, "", usrn);
                    out.writeObject(this.introduction);
                }
            }
        }
    }

    /**
     * There are two actions that happen in the run method.
     *
     * First: The client sends what is called an "introduction" message with
     * it's username, that is checked by the server for availability.
     *
     * Next: the read / write threads are started.
     */
    @Override
    public void run()
    {
        ExecutorService ioThreadPool = Executors.newFixedThreadPool(2);
        try
        {
            Message message;
            sockOut = new ObjectOutputStream(sock.getOutputStream());
            sockOut.flush();
            sockIn = new ObjectInputStream(sock.getInputStream());

            negotiateUsername(sockIn, sockOut);

            ioThreadPool.execute(
                new SocketOutputThread(
                    this.username, this.sock, this.sockOut, System.in
                )
            );
            ioThreadPool.execute(
                new SocketInputThread(this.username, this.sock, this.sockIn)
            );
        }
        catch (IOException | ClassNotFoundException ex)
        {
            System.err.println("[ERROR] " + ex.getMessage());
        }
    }
}
