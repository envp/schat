package schat.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import schat.message.IllegalMessageException;
import schat.message.Message;

/**
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class SocketOutputThread implements Runnable
{
    private final String username;
    private final ObjectOutputStream output;
    private final BufferedReader input;
    private final Socket socket;

    public SocketOutputThread(
        String username,
        Socket socket,
        ObjectOutputStream output,
        InputStream input
    ) throws IOException
    {
        this.username = username;
        this.socket = socket;
        this.output = output;
        this.input = new BufferedReader(new InputStreamReader(input));
    }

    /**
     * Helper method for processing outbound text messages
     *
     * @param msg Message object containing outbound message
     */
    private void processOutboundTextMessage(Message msg, ObjectOutputStream out)
        throws IOException
    {
        out.writeObject(msg);
    }

    /**
     * Helper method for processing outbound file transfers
     *
     * @param msg Message with path of file to be dumped into network
     */
    private void processOutboundFileMessage(Message msg, ObjectOutputStream out)
        throws IOException
    {
        // Try to create a file stream from the supplied object
        try
        {
            InputStream file = new FileInputStream(msg.getBody());
            OutputStream sockOut = this.socket.getOutputStream();

            byte[] buffer = new byte[Message.MAX_PAYLOAD_SIZE];
            int length = 0;

            // Tell them the file size to expect
            File f = new File(msg.getBody());
            msg.setPayloadSize(f.length());
            msg.setBody(f.getName());
            out.writeObject(msg);

            while ((length = file.read(buffer)) != -1)
            {
                sockOut.write(buffer, 0, length);
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.err.println("[ERROR] Can't find file: " + fnfe.getMessage());
        }
    }

    @Override
    public void run()
    {
        try
        {
            Message message;

            while (true)
            {
                try
                {
                    // Read user input from command line
                    message = Message.parseMessage(input.readLine());
                    message.setFrom(this.username);

                    // Process if not blank
                    if (!message.isBlank())
                    {
                        System.out.println(message);

                        if (message.isTextMessage())
                        {
                            processOutboundTextMessage(message, output);
                        }
                        if (message.isFileMessage())
                        {
                            processOutboundFileMessage(message, output);
                        }
                    }
                }
                catch (IllegalMessageException ex)
                {
                    System.err.println("[ERROR]" + ex.getMessage());
                }
            }
        }
        catch (IOException ex)
        {
            System.err.println("[ERROR]" + ex.getMessage());
        }
    }

}
