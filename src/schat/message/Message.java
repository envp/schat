package schat.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class Message implements Serializable
{

    private static final long serialVersionUID = 1L;

    // Maximum byte size of message payload
    public static final int MAX_PAYLOAD_SIZE = 8192;

    private MessageType type;
    private String from;
    private List<String> to = new ArrayList<>();
    private String body = "";
    private long payloadSize = -1;

    private static final char TOK_CMD_START = '/';
    private static final String TOK_CMD_BLK = "!";
    private static final String TOK_CMD_RCV = "@";
    private static final String TOK_TEXT = "text";
    private static final String TOK_FILE = "file";
    private static final String TOK_QUIT = "quit";

    /**
     * Null / default constructor
     */
    public Message()
    {
    }

    /**
     * Constructor for the message class
     *
     * @param mType MessageType associated with the message
     * @param body String / text body of the message. Can be message text or
     * filename depending on the usecase
     * @param from String that is populated with the sender's username
     */
    public Message(MessageType mType, String body, String from)
    {
        this.type = mType;
        this.body = body;
        this.from = from;
    }

    /**
     * Accessor method for message type
     *
     * @return MessageType of the current instance
     */
    public MessageType getType()
    {
        return this.type;
    }

    /**
     * Accessor method for message body / text
     *
     * @return Message body of the current instance
     */
    public String getBody()
    {
        return this.body;
    }

    /**
     * Accessor method for message sender
     *
     * @return User who sent this message instance
     */
    public String getFrom()
    {
        return this.from;
    }
    
    /**
     * Returns the list of recipients as an array
     *
     * @return Recipients as an array
     */
    public String[] getRecipients()
    {
        return this.to.toArray(new String[0]);
    }

    /**
     * Accessor method for message payload size
     *
     * @return -1 if empty, positive value otherwise
     */
    public long getPayloadSize()
    {
        return this.payloadSize;
    }

    /**
     * Sets the payload size to given value, if it is under the upper limit
     *
     * @param size total size in bytes of data to be set
     */
    public void setPayloadSize(long size)
    {
        this.payloadSize = size;
    }

    /**
     * Mutator method for message sender
     *
     * @param from Sender username
     */
    public void setFrom(String from)
    {
        this.from = from;
    }

    /**
     * Mutator method for message body
     *
     * @param body Message body
     */
    public void setBody(String body)
    {
        this.body = body;
    }

    /**
     * Mutator method for message type
     *
     * @param mType Message type to associate with instance
     */
    public void setType(MessageType mType)
    {
        this.type = mType;
    }

    /**
     * Return the message represented as a string
     *
     * @return the message represented as a string
     */
    @Override
    public String toString()
    {
        return String.format("@%s: %s%n", this.from, this.body);
    }

    /**
     * Parse a string into a message The syntax is defined as follows:
     * /{command} {receiver} {data} Where the following regular expressions
     * represent each of the terms: {command} ::= text|file|exit {receiver} ::=
     * [@{username}\s]
     *
     * {data} ::= .
     *
     * {username} ::= [A-Za-z][A-Za-z0-9_]*
     *
     * If the command doesn't start with a '/' it is assumed to be a broadcast
     * message.
     *
     * @param str String to be parsed into a message object
     * @return Returns a message object encapsulating the action to be performed
     * as a Message with a relevant type
     * @throws schat.message.IllegalMessageException
     */
    public static Message parseMessage(String str) throws IllegalMessageException
    {
        Message msg = new Message();
        if (str.length() == 0)
        {
            throw new IllegalMessageException(
                "Empty messages cannot be processed."
            );
        }
        if (str.charAt(0) == TOK_CMD_START)
        {
            // Done processing first character
            str = str.substring(1);

            // Create a tokenizer and some variables to hold intermittent values
            StringTokenizer tk = new StringTokenizer(str);

            // Raise an exception, we need atleast two tokens including
            // the command (/text or /file) for a valid (non-empty) message
            if (tk.countTokens() < 2)
            {
                throw new IllegalMessageException(
                    "Empty messages cannot be processed."
                );
            }

            String[] tokens = new String[tk.countTokens()];

            // Copy all tokens into an array
            for (int i = 0; tk.hasMoreTokens(); ++i)
            {
                tokens[i] = tk.nextToken();
            }

            // Deal with unrecognized commands via exceptions
            if (!tokens[0].equals(TOK_TEXT)
                && !tokens[0].equals(TOK_FILE)
                && !tokens[0].equals(TOK_QUIT))
            {
                throw new IllegalMessageException(
                    "Unknown command: " + tokens[0]
                );
            }

            // Handle voluntary user exit
            if (tokens[0].equals(TOK_QUIT))
            {
                msg.type = MessageType.CLIENT_QUIT;
                return msg;
            }

            // Return early if we have a message of the the form:
            // /[text|file] <BODY>
            if (!tokens[1].startsWith(TOK_CMD_RCV) && !tokens[1].startsWith(TOK_CMD_BLK))
            {
                msg.type = tokens[0].equals(TOK_TEXT)
                    ? MessageType.CLIENT_TEXT_BROADCAST
                    : MessageType.CLIENT_FILE_BROADCAST;
                msg.body = str.substring(tokens[0].length() + 1);
                return msg;
            }

            // Now we're guaranteed to have only messages of the form:
            // /[text|file] [[@|!]<username>]+ <BODY>
            // To find out where the body begins, assume a homogenous cast list
            // and find the fist token whose fist character is different from
            // the first character of token[1]
            char firstType = tokens[1].charAt(0);
            int lastIndex = 1;
            for (int i = 1; i < tokens.length && tokens[i].charAt(0) == firstType; ++i)
            {
                msg.to.add(tokens[i].substring(1));
                lastIndex = i;
            }

            // At this point we need to check if a valid body was even
            // specified. The body is bound to be empty if tokens.length = msg.to.length + 1
            if (tokens.length - msg.to.size() == 1)
            {
                throw new IllegalMessageException(
                    "Empty messages cannot be processed."
                );
            }

            // We are now guaranteed a non-empty message body, i.e atleast 1 token
            String temp = "";
            for (int i = lastIndex + 1; i < tokens.length; ++i)
            {
                temp += tokens[i] + " ";
            }
            msg.body = temp.trim();

            // No broadcast messages make it to this point
            if (firstType == TOK_CMD_RCV.charAt(0))
            {
                msg.type = (tokens[0].equals(TOK_TEXT))
                    ? MessageType.CLIENT_TEXT_UNICAST
                    : MessageType.CLIENT_FILE_UNICAST;
            }
            else
            {
                msg.type = (tokens[0].equals(TOK_TEXT))
                    ? MessageType.CLIENT_TEXT_BLOCKCAST
                    : MessageType.CLIENT_FILE_BLOCKCAST;
            }

            // RETURN THE GLORIOUS MSG
            return msg;
        }

        // If the message doesn't follow the command format, default to a
        // text broadcast message
        msg.type = MessageType.CLIENT_TEXT_BROADCAST;
        msg.body = str;
        return msg;
    }

    /**
     * @TODO: A better name for this.
     * @param message
     * @return
     */
    public static String verboseString(Message message)
    {
        return String.format(
            "{\"Message\": "
            + "{\"from\":\"%s\", "
            + "\"to\":%s, "
            + "\"type\":\"%s\", "
            + "\"body':\"%s\"}}",
            message.getFrom(), Arrays.toString(message.getRecipients()),
            message.getType(), message.getBody()
        );
    }

    /**
     * Checks if a message is "printable" or a message that must appear on the
     * user interface
     *
     * @return boolean indicating printability
     */
    public boolean isPrintable()
    {
        return !(type == MessageType.CLIENT_INTRODUCTION
            && type == MessageType.CLIENT_QUIT
            && type == MessageType.CLIENT_TIMEOUT);
    }

    /**
     * Checks if the message instance is a text message i.e. treat message body
     * as free form text
     *
     * @return true if MessageType is one of the text types, false otherwise
     */
    public boolean isTextMessage()
    {
        return (type == MessageType.CLIENT_TEXT_BROADCAST
            || type == MessageType.CLIENT_TEXT_UNICAST
            || type == MessageType.CLIENT_TEXT_BLOCKCAST);
    }

    /**
     * Checks if the message instance is file message i.e. treat message body as
     * a file path (relative to where the application is being run from)
     *
     * @return true if MessageType is one of the file types, false otherwise
     */
    public boolean isFileMessage()
    {
        return (type == MessageType.CLIENT_FILE_BROADCAST
            || type == MessageType.CLIENT_FILE_UNICAST
            || type == MessageType.CLIENT_FILE_BLOCKCAST);
    }

    /**
     * Checks if the message body is blank or empty (trimmable whitespace or
     * null)
     *
     * @return true iff message body is non-null and non-empty on trimming
     */
    public boolean isBlank()
    {
        return body.trim().isEmpty();
    }
}
