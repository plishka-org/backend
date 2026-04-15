package org.plishka.backend.event.auth;

public record EmailVerificationRequestedEvent(
        String email,
        String verificationLink
) {
}
