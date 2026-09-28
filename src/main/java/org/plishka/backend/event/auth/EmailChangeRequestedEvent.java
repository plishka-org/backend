package org.plishka.backend.event.auth;

public record EmailChangeRequestedEvent(
        String email,
        String verificationLink
) {
}
