package org.plishka.backend.exception;

public class NonRetryableEmailException extends RuntimeException {
    public NonRetryableEmailException(String message) {
        super(message);
    }
}
