package schat.server;

import java.io.IOException;

import java.util.Set;
import java.util.HashSet;

import java.net.ServerSocket;



/**
 *
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class Server {
    // Minimum number of users that need to join for rehashing
    // This is same as HashSet's initial capacity (per Oracle docs)
    private final int MIN_USERS = 16;

    private final int port;
    private ServerSocket sock;
    private Set<ClientHandler> handlers = new HashSet<ClientHandler>(MIN_USERS);
    private int onlineCount = 0;

    private Object lock = new Object();

    /**
     * Constructor for creating a Server instance
     * @param port Port at which server listens for incoming connections
     */
    public Server(int port) throws IOException {
        this.port = port;
        this.sock = new ServerSocket(port);
    }

    public int getPort() {
        return this.port;
    }

    public void listen() throws IOException {
        synchronized(this.lock) {
            try {
                ClientHandler c = new ClientHandler(sock.accept(), onlineCount);
                this.handlers.add(c);
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
