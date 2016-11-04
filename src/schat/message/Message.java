package schat.message;

import schat.exception.IllegalMessageException;

import java.io.Serializable;

import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;

import java.util.logging.Logger;

/**
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String from;
    private List<String> to = new ArrayList<String>();
    private Object body = "";

    private static final char TOK_CMD_START = '/';
    private static final String TOK_CMD_BLK = "!";
    private static final String TOK_CMD_RCV = "@";
    private static final String TOK_TEXT = "text";
    private static final String TOK_FILE = "file";


    /**
     * Null / default constructor
     * @return Returns an empty instance of the Message class
     */
    public Message() {}

    public Message(MessageType mType, Object body, String from) {
        this.from = from;
        this.type = mType;
        this.body = body;
    }

    public MessageType getType() {
        return this.type;
    }

    public Object getBody() {
        return this.body;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public void setType(MessageType mType) {
        this.type = mType;
    }

    public String toString() {
        // return "message; " + "to:" + this.to + "; " + "type:" + this.type + "; " + "body:" + this.body;
        return "@" + this.from + ": " + (String) this.body + "\n";

    }

    /**
     * Parse a string into a message
     *  The syntax is defined as follows: /<command> <reciever> <data>
     *  Where the following regular expressions represent each of the terms:
     *  <command>   ::= text|file
     *  <reciever>  ::= [@<username>\s]*
     *  <data>      ::= .*
     *  <username>  ::= [A-Za-z][A-Za-z0-9_]*
     *
     * If the command doesn't start with a '/' it is assumed to be a broadcast
     * message.
     *
     * @param  str String to be parsed into a message object
     * @return     Returns a message object encapsulating the action to be
     *             performed as a Message with a relevant type
     */
    public static Message parseMessage(String str) throws IllegalMessageException {
        Message msg = new Message();
        if(str.length() == 0) {
            throw new IllegalMessageException(
                "Empty messages cannot be processed."
            );
        }
        if(str.charAt(0) == TOK_CMD_START) {
            // Done processing first character
            str = str.substring(1);

            // Create a tokenizer and some variables to hold intermittent values
            StringTokenizer tk = new StringTokenizer(str);
            String command;

            // Raise an exception, we need atleast two tokens including
            // the command (/text or /file) for a valid (non-empty) message
            if(tk.countTokens() < 2) {
                throw new IllegalMessageException(
                    "Empty messages cannot be processed."
                );
            }

            String[] tokens = new String[tk.countTokens()];

            // Copy all tokens into an array
            for(int i = 0; tk.hasMoreTokens(); ++i) {
                tokens[i] = tk.nextToken();
            }

            // Deal with unrecognized commands via exceptions
            if(!tokens[0].equals(TOK_TEXT) && !tokens[0].equals(TOK_FILE)) {
                throw new IllegalMessageException(
                    "Unknown command: " + tokens[0]
                );
            }

            // Exit early if we have a message of the the form:
            // /[text|file] <BODY>
            if(!tokens[1].startsWith(TOK_CMD_RCV) && !tokens[1].startsWith(TOK_CMD_BLK)) {
                msg.type = tokens[0].equals(TOK_TEXT) ? MessageType.CLIENT_TEXT_BROADCAST : MessageType.CLIENT_FILE_BROADCAST;
                msg.body = (Object) str.substring(tokens[0].length() - 1);
                return msg;
            }

            // Now we're guaranteed to have only messages of the form:
            // /[text|file] [[@|!]<username>]+ <BODY>
            // To find out where the body begins, assume a homogenous cast list
            // and find the fist token whose fist character is different from
            // the first character of token[1]
            char firstType = tokens[1].charAt(0);
            int lastIndex = 1;
            for(int i = 1; i < tokens.length && tokens[i].charAt(0) == firstType; ++i) {
                msg.to.add(tokens[i].substring(1));
                lastIndex = i;
            }

            // At this point we need to check if a valid body was even
            // specified. The body is bound to be empty if tokens.length = msg.to.length + 1
            if(tokens.length - msg.to.size() == 1) {
                throw new IllegalMessageException(
                    "Empty messages cannot be processed."
                );
            }

            // We are now guaranteed a non-empty message body, i.e atleast 1 token
            String temp = "";
            for(int i = lastIndex + 1; i < tokens.length; ++i) {
                temp += tokens[i] + " ";
            }
            msg.body = temp.trim();

            // No broadcast messages make it to this point
            if(firstType == TOK_CMD_RCV.charAt(0)) {
                msg.type = (tokens[0].equals(TOK_TEXT)) ? MessageType.CLIENT_TEXT_UNICAST : MessageType.CLIENT_FILE_UNICAST;
            }
            else {
                msg.type = (tokens[0].equals(TOK_TEXT)) ? MessageType.CLIENT_TEXT_BLOCKCAST : MessageType.CLIENT_FILE_BLOCKCAST;
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
}
