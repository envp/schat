package schat.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import schat.message.Message;

/**
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class SocketInputThread implements Runnable
{
    private final ObjectInputStream input;
    private final Socket sock;
    private final String username;

    private String downloadPath = "./download/";

    public SocketInputThread(
        String username,
        Socket sock,
        ObjectInputStream input
    )
    {
        this.username = username;
        this.sock = sock;
        this.input = input;
        this.downloadPath = downloadPath.concat(this.username);
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
    private void processInboundFileMessage(Message msg)
    {
        File downloadDir = new File(downloadPath);

        if (!downloadDir.exists())
        {
            downloadDir.mkdirs();
        }

        String fileOutputPath = downloadPath + "/" + msg.getBody();
        File outFile = new File(fileOutputPath);
        BufferedOutputStream fileOutStream = null;
        BufferedInputStream in = null;
        int size, currentPos, bytesRead;
        byte[] buffer;

        System.out.format("Recieving file: %s (%d bytes) from user @%s%n",
            msg.getBody(), msg.getPayloadSize(), msg.getFrom()
        );

        try
        {
            outFile.createNewFile();
            in = new BufferedInputStream(this.sock.getInputStream());
            fileOutStream = new BufferedOutputStream(
                new FileOutputStream(outFile)
            );
            size = (int) msg.getPayloadSize();
            buffer = new byte[Message.MAX_PAYLOAD_SIZE];
            currentPos = 0;
            do
            {
                bytesRead = in.read(buffer, 0, buffer.length);
                fileOutStream.write(buffer, 0, bytesRead);
                currentPos += bytesRead;
            } while (bytesRead != -1 && currentPos < size);
            fileOutStream.flush();
            fileOutStream.close();
            System.out.println("File recieved.");
        }
        catch (IOException ex)
        {
            System.err.println("[ERROR] " + ex.getMessage());
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
                    processInboundFileMessage(message);
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
