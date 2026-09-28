package org.plishka.backend.event.auth;

public record EmailChangedEvent(
        String oldEmail,
        String newEmail
) {
}
