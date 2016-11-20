package schat.server;

import schat.message.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class ClientHandler implements Runnable
{
    private final Socket sock;
    private final String username;
    private ObjectInputStream sockIn;
    private ObjectOutputStream sockOut;
    private ReentrantLock socketWriteLock = new ReentrantLock();

    /**
     * Constructor for creating a new ClientHandler instance
     *
     * @param cSock Client socket object
     */
    public ClientHandler(Socket cSock)
    {
        this.sock = cSock;
        this.username = "";
    }

    /**
     * Prints out the message as a verbose form, JSON like string
     *
     * @param message Message to be logged
     */
    private static void logMessage(Message message)
    {
        System.out.println(Message.verboseString(message));
    }

    /**
     * Writes a message to another Handlers outbound socket
     *
     * @param other The target user who is supposed to receive the message
     * @param message Message to be sent to target user
     * @return true if write was successful, false otherwise
     */
    public boolean dispatchMessage(ClientHandler other, Message message)
    {
        this.socketWriteLock.lock();
        boolean sent = true;
        try
        {
            other.socketWriteLock.tryLock();
            try
            {
                other.sockOut.writeObject(message);
            }
            finally
            {
                other.socketWriteLock.unlock();
            }
        }
        catch (IOException ex)
        {
            sent = false;
        }
        finally
        {
            socketWriteLock.unlock();
        }
        return sent;
    }

    /**
     * Writes a message to <code>this</code> Handlers outbound socket
     *
     * @param message Message to be sent to target user
     * @return true if write was successful, false otherwise
     */
    private boolean dispatchMessage(Message message)
    {
        this.socketWriteLock.lock();
        boolean sent = true;
        try
        {
            this.sockOut.writeObject(message);
        }
        catch (IOException ex)
        {
            sent = false;
        }
        finally
        {
            socketWriteLock.unlock();
        }
        return sent;
    }

    private void processIntroduction(Message message)
    {
        ConcurrentHashMap<String, ClientHandler> users;
        Message temp;
        temp = new Message(MessageType.ACK_INTRO, "", "");
        users = Server.getUserList();

        if (users.containsKey(message.getFrom()))
        {
            temp.setBody("N");
        }
        else
        {
            users.putIfAbsent(message.getFrom(), this);
            temp.setBody("Y");
        }
        this.dispatchMessage(temp);
    }

    private boolean unicastText(Message message)
    {
        // Just picks up the first one, doesn't check length since it is assumed
        // that malformed messages are corrected client-side
        return dispatchMessage(
            Server.getUserList().get(message.getRecipients()[0]),
            message
        );
    }

    private boolean broadcastText(Message message)
    {
        boolean sent = true;
        Collection<ClientHandler> handlers = Server.getUserList().values();
        
        // Need to unset the message.to field, so create a fresh object local
        // to this so as not 
        Message msg = new Message(
            message.getType(), message.getBody(), message.getFrom()
        );
        
        for(ClientHandler handler : handlers)
        {
            dispatchMessage(handler, msg);
        }
        return sent;
    }

    private boolean blockcastText(Message message)
    {
        boolean sent = true;
        Collection<ClientHandler> handlers = Server.getUserList().values();
        
        // Need to unset the message.to field, so create a fresh object local
        // to this so as not 
        Message msg = new Message(
            message.getType(), message.getBody(), message.getFrom()
        );
        
        List<String> users = Arrays.asList(message.getRecipients());
        
        for(ClientHandler handler : handlers)
        {
            if(users.indexOf(handler.username) > 0)
            {
                sent = sent & dispatchMessage(handler, msg);
            }
        }
        return sent;
    }

    @Override
    public void run()
    {
        try
        {
            Message message;
            this.sockOut = new ObjectOutputStream(this.sock.getOutputStream());
            this.sockOut.flush();
            this.sockIn = new ObjectInputStream(this.sock.getInputStream());

            while (true)
            {
                message = (Message) this.sockIn.readObject();
                ClientHandler.logMessage(message);

                switch (message.getType())
                {
                    case CLIENT_INTRODUCTION:
                        processIntroduction(message);
                        break;
                    case CLIENT_TEXT_UNICAST:
                        unicastText(message);
                        break;
                    case CLIENT_TEXT_BROADCAST:
                        broadcastText(message);
                        break;
                    case CLIENT_TEXT_BLOCKCAST:
                        blockcastText(message);
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
        catch (ClassNotFoundException | IOException ex)
        {
            System.err.println("[ERROR]: " + ex.getMessage());
        }
    }
}
