package schat.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import schat.message.*;

/**
 *
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class ClientHandler implements Runnable
{
    private final Socket sock;
    private String username;
    private ObjectInputStream sockIn;
    private ObjectOutputStream sockOut;
    private final ReentrantLock socketIOLock = new ReentrantLock();

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
    public boolean dispatchText(ClientHandler other, Message message)
    {
        this.socketIOLock.lock();
        boolean sent = true;
        try
        {
            other.socketIOLock.tryLock();
            try
            {
                other.sockOut.writeObject(message);
            }
            finally
            {
                other.socketIOLock.unlock();
            }
        }
        catch (IOException ex)
        {
            System.err.println("[ERROR] " + ex.getMessage());
            sent = false;
        }
        finally
        {
            socketIOLock.unlock();
        }
        return sent;
    }

    /**
     * Writes a message to <code>this</code> Handlers outbound socket
     *
     * @param message Message to be sent to target user
     * @return true if write was successful, false otherwise
     */
    private boolean dispatchText(Message message)
    {
        this.socketIOLock.tryLock();
        boolean sent = true;
        try
        {
            this.sockOut.writeObject(message);
        }
        catch (IOException ex)
        {
            System.err.println("[ERROR]" + ex.getMessage());
            sent = false;
        }
        finally
        {
            this.socketIOLock.unlock();
        }
        return sent;
    }

    private boolean dispatchMultiText(
        Collection<ClientHandler> handlers,
        Message message
    )
    {
        boolean sent = true;
        for (ClientHandler handler : handlers)
        {
            sent = sent & ClientHandler.this.dispatchText(handler, message);
        }
        return sent;
    }

    private boolean dispatchMultiFile(
        List<ClientHandler> handlers,
        Message message
    )
    {
        this.socketIOLock.lock();
        boolean sent = true;
        int currentPos, size, bytesRead;
        byte[] buffer;
        BufferedInputStream in = null;
        BufferedOutputStream[] out = new BufferedOutputStream[handlers.size()];
        try
        {
            // First we acquire locks to get all the output streams from
            // each of the the handlers
            ClientHandler handler;
            for (int i = 0; i < handlers.size(); ++i)
            {
                handler = handlers.get(i);
                handler.socketIOLock.tryLock();
                try
                {
                    handler.sockOut.writeObject(message);
                    out[i] = new BufferedOutputStream(
                        handler.sock.getOutputStream()
                    );
                    out[i].flush();
                }
                catch (IOException ex)
                {
                    System.err.println("[ERROR] " + ex.getMessage());
                    sent = false;
                }
                finally
                {
                    handler.socketIOLock.unlock();
                }
            }

            // Now that we have the respective output streams, we can write
            // the data we read to each, one by one
            currentPos = 0;
            size = (int) message.getPayloadSize();
            buffer = new byte[Message.MAX_PAYLOAD_SIZE];
            in = new BufferedInputStream(this.sock.getInputStream());

            do
            {
                bytesRead = in.read(buffer, 0, buffer.length);
                for (int i = 0; i < handlers.size(); ++i)
                {
                    handler = handlers.get(i);
                    handler.socketIOLock.tryLock();
                    try
                    {
                        out[i].write(buffer, 0, bytesRead);
                        out[i].flush();
                    }
                    finally
                    {
                        handler.socketIOLock.unlock();
                    }
                }
                currentPos += bytesRead;
            } while (bytesRead != -1 && currentPos < size);

        }
        catch (IOException ex)
        {
            System.err.println("[ERROR] " + ex.getMessage());
            sent = false;
        }
        finally
        {
            this.socketIOLock.unlock();
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
            this.username = message.getFrom();
        }
        this.dispatchText(temp);
    }

    /**
     * Not strictly unicast but a multicast method. Sends the message to a list
     * of recipients, also forwards attachments
     *
     * @param message Message to be sent (may be text or file)
     * @return boolean status indicating success of relay operation
     */
    private boolean unicastMessage(Message message)
    {
        Collection<ClientHandler> users = Server.getUserList().values();
        HashSet<String> recipients = new HashSet<>(
            Arrays.asList(message.getRecipients())
        );
        List<ClientHandler> handlers = users.stream().
            filter(h -> (recipients.contains(h.username)
                && !h.username.equals(this.username))
            ).collect(Collectors.toList());

        if (message.isTextMessage())
        {
            return this.dispatchMultiText(handlers, message);
        }
        // It is a file message (guaranteed by switch-case)
        return this.dispatchMultiFile(handlers, message);
    }

    /**
     * Sends a file to all users but the sender on the server
     *
     * @param message Message to be relayed
     * @return boolean status indicating success of relay operation
     */
    private boolean broadcastMessage(Message message)
    {
        Collection<ClientHandler> users = Server.getUserList().values();
        List<ClientHandler> handlers = users.
            stream().
            filter(h -> !h.username.equals(this.username)).
            collect(Collectors.toList());

        // Need to unset the message.to field, so create a fresh object local
        // to this so as not cause problems
        Message msg = new Message(
            message.getType(), message.getBody(), message.getFrom()
        );

        if (message.isTextMessage())
        {
            return dispatchMultiText(handlers, msg);
        }
        // It is a file message (guaranteed by switch-case)
        msg.setPayloadSize(message.getPayloadSize());

        return dispatchMultiFile(handlers, msg);
    }

    /**
     * Sends file to all but some users on the server (including sender)
     *
     * @param message Message to be relayed
     * @return boolean status indicating success of relay operation
     */
    private boolean blockcastMessage(Message message)
    {

        HashSet<String> blockList = new HashSet<>(
            Arrays.asList(message.getRecipients())
        );
        blockList.add(this.username);

        // Filter out the ones we don't want to send stuff to
        List<ClientHandler> handlers = Server.getUserList().values().
            stream().
            filter(
                h -> !blockList.contains(h.username)
            ).
            collect(Collectors.toList());

        // Need to unset the message.to field, so create a fresh local object
        Message msg = new Message(
            message.getType(), message.getBody(), message.getFrom()
        );
        if (message.isTextMessage())
        {
            return dispatchMultiText(handlers, msg);
        }

        msg.setPayloadSize(message.getPayloadSize());

        return dispatchMultiFile(handlers, msg);
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
                    case CLIENT_FILE_UNICAST:
                        unicastMessage(message);
                        break;
                    case CLIENT_TEXT_BROADCAST:
                    case CLIENT_FILE_BROADCAST:
                        broadcastMessage(message);
                        break;
                    case CLIENT_TEXT_BLOCKCAST:
                    case CLIENT_FILE_BLOCKCAST:
                        blockcastMessage(message);
                        break;
                    default:
                        break;
                }
            }
        }
        catch (ClassNotFoundException ex)
        {
            System.err.println("[ERROR]: " + ex.getMessage());
        }
        catch (IOException ex)
        {
            // Connection was reset, remove self from the list of users online
            System.err.println("[ERROR]: " + ex.getMessage());
            // Connection was reset, remove self from the list of users online
            ClientHandler self = Server.getUserList().remove(this.username);
            if(self != null)
            {
                System.out.println("Goodbye, " + self.username);
            }}
        }
    }
