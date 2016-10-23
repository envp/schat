package schat.server;

import java.io.IOException;

import java.util.Set;
import java.util.HashSet;

import java.net.ServerSocket;
import java.net.InetAddress;


/**
 * Class abstracting the chat server definition. The server handles:
 * 1.
 *
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class Server {
    // Minimum number of users that need to join for rehashing
    // This is same as HashSet's initial capacity (per Oracle docs)
    private final int MIN_USERS = 16;

    private ServerSocket sock;
    private int onlineCount = 0;

    private Object lock = new Object();

    /**
     * Creates a fresh Server instance
     * @param port Port at which server listens for incoming connections
     * @return A new server instance
     */
    public Server(int port) throws IOException {
        this.sock = new ServerSocket(port);
    }

    /**
     * The local port to which the current server instance is listening to for
     * inbound connections
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
    public InetAddress getLocalAddress() {
        return this.sock.getInetAddress();
    }

    /**
     * Make the server start listening on the preset port
     */
    public void listen() throws IOException {
        synchronized(this.lock) {
            try {
                new ClientHandler(sock.accept(), onlineCount);
                onlineCount++;
            }
            catch(IOException ioe) {
                // Log IOException and rethrow
            }
            finally {
                sock.close();
            }
        }
    }
}
