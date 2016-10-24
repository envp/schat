package schat.client;

import java.net.InetAddress;

/**
 * Class to encapsulate common client data that is used and stored by the
 * server. This class will be (for now) used exclusively by the server (as a
 * point of central contral). Later versions might see this be used by the
 * client side.
 *
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class ClientInfo {
    private String username;
    private InetAddress address;
    private int port;

    /**
     * Constructor for the ClientInfo class
     * @param   username    Connecting client's username
     * @param   address     Connecting client's remote address
     * @param   port        Connecting client's remote port
     * @return  A new instance encapsulating client information
     */
    public ClientInfo(String username, InetAddress address, int port) {
        this.username = username;
        this.address = address;
        this.port = port;
    }

    /**
     * Handy constructor to default everything to default values whose
     * presence is taken as the object being unset
     * @return A "default unset" instance of the ClientInfo class
     */
    public ClientInfo() {
        this(null, null, -1);
    }

    public int getPort() {
        return this.port;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public String getUsername() {
        return this.username;
    }

    public boolean isUnset() {
        return !(port > 0 && address != null && username != null);
    }
}
