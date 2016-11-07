package schat.server;

import java.io.IOException;
import java.io.PrintStream;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import java.net.ServerSocket;
import java.net.InetAddress;

/**
 * Class abstracting the chat server definition. The server handles:
 * 1.
 *
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class Server {
    // Minimum number of users that need to join for rehashing the client map
    // This is same as HashSet's initial capacity (per Oracle docs)
    private final int MIN_USERS = 16;
    private final PrintStream log = System.out;

    private ServerSocket sock;
    private int onlineCount = 0;

    private Object lock = new Object();
    private static ExecutorService workers = Executors.newCachedThreadPool();

    /**
     * Creates a fresh Server instance
     * @param port Port at which server listens for incoming connections
     * @return A new server instance
     */
    public Server(int port) throws IOException {
        this.sock = new ServerSocket(port);
    }

    /**
     * The local port to which the current server instance is listening to
     *  for inbound connections
     *
     * @return Local system port to which this server instance is bound
     */
    public int getLocalPort() {
        return this.sock.getLocalPort();
    }

    /**
     * Local address / interface to which the server is listening
     * @return InetAddress object of the local listening interface
     */
    public InetAddress getInetAddress() {
        return this.sock.getInetAddress();
    }

    /**
     * Make the server start listening on the preset port. Every time a client
     * connects a new worker thread is created
     */
    public void listen() throws IOException {
        this.log.println("Listening for clients on tcp://" +
            getInetAddress().getHostAddress() + ":" + getLocalPort()
        );

        try {
            while(true) {
                workers.execute(
                    new ClientHandler(this.sock.accept(), onlineCount)
                );
                onlineCount++;
            }
        }
        catch(IOException ioe) {
            this.log.println("[ERROR] " + ioe.getMessage());
            workers.shutdown();
        }
        finally {
            if(!workers.isShutdown()) {
                this.log.println("Shutting server down");
                workers.shutdown();
            }
            this.sock.close();
        }
    }
}
