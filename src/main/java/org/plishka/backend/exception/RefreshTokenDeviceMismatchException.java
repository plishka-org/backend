package org.plishka.backend.exception;

public class RefreshTokenDeviceMismatchException extends RuntimeException {
    public RefreshTokenDeviceMismatchException(String message) {
        super(message);
    }
}
