package schat.message;

import java.io.Serializable;

import java.util.logging.Logger;

/**
 * @author Vaibhav Yenamandra (vyenman@ufl.edu)
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private MessageType type;
    private String from;

    public Message(MessageType mType) {
        this.type = mType;
    }

    public MessageType getType() {
        return this.type;
    }

    /**
     * Parse a string into a message
     *  The syntax is defined as follows:
     *
     * @param  str String to be parsed into a message object
     * @return     Returns a message object encapsulating the action to be
     *             performed as a Message with a relevant type
     */
    public static Message parseMessage(String str) {
        return new Message(MessageType.CLIENT_INTRODUCTION);
    }
}
