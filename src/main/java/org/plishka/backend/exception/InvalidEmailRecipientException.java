package org.plishka.backend.exception;

public class InvalidEmailRecipientException extends RuntimeException {
    public InvalidEmailRecipientException(String message) {
        super(message);
    }
}
