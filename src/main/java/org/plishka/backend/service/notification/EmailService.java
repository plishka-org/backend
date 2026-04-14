package org.plishka.backend.service.notification;

import lombok.RequiredArgsConstructor;
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

    private String buildVerificationEmailText(String verificationLink) {
        return String.format("""
                Welcome,
                
                Thank you for registering.
                
                Please verify your email address by clicking the link below:
                %s
                
                This link will expire in 24 hours.
                
                If you did not create an account, you can ignore this email.
                
                Best regards,
                Plishka
                """, verificationLink);
    }
}
