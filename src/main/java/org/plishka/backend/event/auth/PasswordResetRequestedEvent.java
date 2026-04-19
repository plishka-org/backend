package org.plishka.backend.event.auth;

public record PasswordResetRequestedEvent(
        String email,
        String resetPasswordLink
) {
}
