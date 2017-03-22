package org.apache.james.modules;

public class MissingArgumentException extends RuntimeException {

    public MissingArgumentException(String message) {
        super(message);
    }

}
