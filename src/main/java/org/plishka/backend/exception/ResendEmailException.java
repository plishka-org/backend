package org.plishka.backend.exception;

public class ResendEmailException extends RuntimeException {
    public ResendEmailException(String message) {
        super(message);
    }

    public ResendEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
