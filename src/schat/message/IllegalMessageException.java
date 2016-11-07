package schat.message;

import java.io.Serializable;

public class IllegalMessageException extends Exception {
    private static final long serialVersionUID = 1L;

    public IllegalMessageException(String message) {
        super(message);
    }
}
