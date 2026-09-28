package org.plishka.backend.exception;

public class RetryableEmailException extends RuntimeException {
    public RetryableEmailException(String message) {
        super(message);
    }

    public RetryableEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
