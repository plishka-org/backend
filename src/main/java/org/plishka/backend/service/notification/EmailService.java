package org.plishka.backend.service.notification;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.event.callback.CallbackRequestCreatedEvent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final AsyncEmailSender asyncEmailSender;
    private final BackendProperties backendProperties;

    public void sendEmailVerificationEmail(String email, String verificationLink) {
        String subject = "Verify your email";
        String text = buildVerificationEmailText(verificationLink);

        asyncEmailSender.sendEmailAsync(email, subject, text);
    }

    public void sendPasswordResetEmail(String email, String resetPasswordLink) {
        String subject = "Reset your password";
        String text = buildPasswordResetEmailText(resetPasswordLink);

        asyncEmailSender.sendEmailAsync(email, subject, text);
    }

    public void sendEmailChangeVerificationEmail(String email, String verificationLink) {
        String subject = "Confirm your new email";
        String text = buildEmailChangeVerificationText(verificationLink);

        asyncEmailSender.sendEmailAsync(email, subject, text);
    }

    public void sendEmailChangedNotificationEmail(String oldEmail, String newEmail) {
        String subject = "Your account email was changed";
        String text = buildEmailChangedNotificationText(newEmail);

        asyncEmailSender.sendEmailAsync(oldEmail, subject, text);
    }

    public void sendCallbackRequestEmail(CallbackRequestCreatedEvent event) {
        String subject = "New callback request";
        String text = buildCallbackRequestEmailText(event);

        asyncEmailSender.sendEmailAsync(backendProperties.callback().adminEmail(), subject, text);
    }

    private String buildVerificationEmailText(String verificationLink) {
        return """
                Welcome,
                
                Thank you for registering.
                
                Please verify your email address by clicking the link below:
                %s
                
                This link will expire in %d hours.
                
                If you did not create an account, you can ignore this email.
                
                Best regards,
                Plishka
                """
                .formatted(verificationLink, backendProperties.auth().emailVerificationTokenTtl().toHours());
    }

    private String buildPasswordResetEmailText(String resetPasswordLink) {
        return """
                Hello,

                We received a request to reset your password.

                Click the link below to continue resetting your password:
                %s

                This link will expire in %d hours.

                If you did not request a password reset, you can ignore this email.

                Best regards,
                Plishka
                """
                .formatted(resetPasswordLink, backendProperties.auth().passwordResetTokenTtl().toHours());
    }

    private String buildEmailChangeVerificationText(String verificationLink) {
        return """
                Hello,

                We received a request to change your account email.

                Confirm your new email address by clicking the link below:
                %s

                This link will expire in %d hours.

                If you did not request this change, you can ignore this email.

                Best regards,
                Plishka
                """
                .formatted(verificationLink, backendProperties.auth().emailVerificationTokenTtl().toHours());
    }

    private String buildEmailChangedNotificationText(String newEmail) {
        return """
                Hello,

                Your account email has been changed to:
                %s

                If you did not make this change, reset your password immediately.

                Best regards,
                Plishka
                """
                .formatted(newEmail);
    }

    private String buildCallbackRequestEmailText(CallbackRequestCreatedEvent event) {
        return """
                New callback request

                Request ID: %d
                User ID: %s
                Created at: %s
                Name: %s
                Phone: %s

                Message:
                %s
                """
                .formatted(
                        event.callbackRequestId(),
                        event.userId() == null ? "deleted or unknown" : event.userId(),
                        event.createdAt(),
                        event.name(),
                        event.phone(),
                        event.message()
                );
    }
}
