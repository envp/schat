package schat.server;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Executors;

/**
 * Class abstracting the chat server definition. The server handles: 1. Spawning
 * new threads for each client 2. Maintaining synchronized to various shared
 * data structures
 *
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class Server
{
    // Arbitary default port, nice number
    private static final int DEFAULT_PORT = 9012;
    private static final PrintStream log = System.out;

    private ServerSocket sock;

    private static ExecutorService workers = Executors.newCachedThreadPool();
    private ConcurrentHashMap<String, ClientHandler> userList;

    // Singleton instance
    private static Server self = null;

    /**
     * Creates a fresh Server instance. Private because of simplifying
     * assumption of 1 server port per JVM.
     *
     * @param port Port at which server listens for incoming connections
     * @return A new server instance
     */
    private Server(int port)
    {
        try
        {
            this.userList = new ConcurrentHashMap<>();
            this.sock = new ServerSocket(port);
        }
        catch (IOException ex)
        {
            System.err.println("[ERROR] " + ex.getMessage());
        }
    }

    /**
     * Fetches a new instance corresponding to the default number, unless there
     * is another instance already running.
     *
     * @return A singleton instance of Server
     * @throws java.io.IOException
     */
    public static Server getInstance() throws IOException
    {
        return getInstance(DEFAULT_PORT);
    }

    /**
     * Fetches a new instance corresponding to the given port number, unless
     * another one is already running.
     *
     * @param port The port at which the server singleton instance should run
     * @return A singleton instance of Server
     * @throws java.io.IOException
     */
    public static Server getInstance(int port) throws IOException
    {
        if (self == null)
        {
            self = new Server(port);
        }
        return self;
    }

    /**
     * The local port to which the current server instance is listening to for
     * inbound connections
     *
     * @return Local system port to which this server instance is bound
     */
    public static int getLocalPort()
    {
        return self.sock.getLocalPort();
    }

    /**
     * Local address / interface to which the server is listening
     *
     * @return InetAddress object of the local listening interface
     */
    public static InetAddress getInetAddress()
    {
        return self.sock.getInetAddress();
    }

    /**
     * Fetches the list of users registered with the currently running server
     * instance
     *
     * @return A concurrent hashmap object with user handler key-value pairs
     */
    public static synchronized ConcurrentHashMap<String, ClientHandler> getUserList()
    {
        return self.userList;
    }

    /**
     * Make the server start listening on the preset port. Every time a client
     * connects a new worker thread is created
     *
     * @throws java.io.IOException
     */
    public void listen() throws IOException
    {
        log.println("Listening for clients on tcp://"
            + getInetAddress().getHostAddress() + ":" + getLocalPort()
        );

        try
        {
            while (true)
            {
                workers.execute(
                    new ClientHandler(self.sock.accept())
                );
            }
        }
        catch (IOException ioe)
        {
            log.println("[ERROR] " + ioe.getMessage());
            workers.shutdown();
        }
        finally
        {
            if (!workers.isShutdown())
            {
                log.println("Shutting server down");
                workers.shutdown();
            }
            sock.close();
        }
    }
}
