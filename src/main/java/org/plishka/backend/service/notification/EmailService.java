package org.plishka.backend.service.notification;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.service.auth.impl.AuthServiceImpl;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final AsyncEmailSender asyncEmailSender;

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
                .formatted(verificationLink, AuthServiceImpl.EMAIL_VERIFICATION_TOKEN_TTL_HOURS);
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
                .formatted(resetPasswordLink, AuthServiceImpl.PASSWORD_RESET_TOKEN_TTL_HOURS);
    }
}
