package org.plishka.backend.exception;

/**
 * Signals that a required singleton row is unavailable.
 *
 * <p>Do not add a dedicated global exception handler for this exception. It must use the generic
 * internal-error path so the global Sentry integration records the operational failure.</p>
 */
public class RequiredSingletonUnavailableException extends RuntimeException {
    public RequiredSingletonUnavailableException(String message) {
        super(message);
    }
}
