package schat.client;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import schat.message.Message;

/**
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class SocketInputThread implements Runnable
{
    private final ObjectInputStream input;
    private final String username;

    private final String DEFAULT_SAVE_LOCATION = "download";

    public SocketInputThread(String username, ObjectInputStream input)
    {
        this.username = username;
        this.input = input;
    }

    /**
     * Helper method for processing inbound text messages
     *
     * @param msg Message object containing inbound message
     */
    private void processInboundTextMessage(Message msg)
    {
        if (msg.isPrintable())
        {
            System.out.println(msg);
        }
    }

    /**
     * Helper method for processing inbound file transfers
     *
     * @param msg Message with file to be read from stream
     */
    private void processInboundFileMessage(Message msg, ObjectInputStream in)
    {
        long size = msg.getPayloadSize();
        int length = 0;
        String fname = msg.getBody();
        byte[] buffer = new byte[Message.MAX_PAYLOAD_SIZE];

        try
        {
            OutputStream outFile = new FileOutputStream(
                DEFAULT_SAVE_LOCATION + "/" + this.username + "/" + fname
            );

            // Process file per established protocol of
            // Inform -> send
            while (size > 0 && length != -1)
            {
                try
                {
                    length = in.read(buffer, 0, length);
                    outFile.write(buffer);
                    size -= length;
                }
                catch (IOException ioe)
                {
                    System.err.println("[ERROR] " + ioe.getMessage());
                }
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("[ERROR] " + fnfe.getMessage());
        }
    }

    @Override
    public void run()
    {
        Message message;
        while (true)
        {
            try
            {
                message = (Message) input.readObject();
                if (message.isTextMessage())
                {
                    processInboundTextMessage(message);
                }
                if (message.isFileMessage())
                {
                    processInboundFileMessage(message, input);
                }
            }
            catch (IOException | ClassNotFoundException ex)
            {
                System.err.println("[ERROR] " + ex.getMessage());
                break;
            }
        }
    }

}
