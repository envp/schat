package schat.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
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
    private void processOutboundTextMessage(Message msg)
        throws IOException
    {
        this.output.writeObject(msg);
//        System.out.format("%s%n%n", msg.toString());
    }

    /**
     * Helper method for processing outbound file transfers
     *
     * @param msg Message with path of file to be dumped into network
     */
    private void processOutboundFileMessage(Message msg)
        throws IOException
    {
        byte[] buffer;
        int bytesRead;
        File sendFile = new File(msg.getBody());
        
        msg.setPayloadSize(sendFile.length());
        msg.setBody(sendFile.getName());
        
        BufferedInputStream fileReadStream = null;
        BufferedOutputStream fileToSocket = new BufferedOutputStream(
            this.socket.getOutputStream()
        );

        if (sendFile.exists() && sendFile.isFile())
        {
            System.out.format("Sending file: %s (%d bytes)%n", 
                sendFile.getAbsolutePath(), 
                sendFile.length()
            );
            this.output.writeObject(msg);
            buffer = new byte[Message.MAX_PAYLOAD_SIZE];
            try
            {
                fileReadStream = new BufferedInputStream(
                    new FileInputStream(sendFile)
                );
            }
            catch (FileNotFoundException ex)
            {
                System.err.println("[ERROR] " + ex.getMessage());
            }

            try
            {
                while ((bytesRead = fileReadStream.read(buffer, 0, buffer.length)) != -1)
                {
                    fileToSocket.write(buffer, 0, bytesRead);
                }
                fileToSocket.flush();
            }
            catch (IOException ex)
            {
                System.err.println("[ERROR] " + ex.getMessage());
            }
            finally
            {
                if (fileReadStream != null)
                {
                    fileReadStream.close();
                }
            }
            System.out.println("File sent.");
        }
        else
        {
            System.out.format("[ERROR] Path not a file, or doesnt exist: %s%n", 
                sendFile.getAbsolutePath()
            );
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
                        if (message.isTextMessage())
                        {
                            processOutboundTextMessage(message);
                        }
                        if (message.isFileMessage())
                        {
                            processOutboundFileMessage(message);
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
