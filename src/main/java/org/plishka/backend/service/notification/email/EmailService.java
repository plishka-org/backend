package org.plishka.backend.service.notification.email;

import java.util.concurrent.RejectedExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.event.callback.CallbackRequestCreatedEvent;
import org.plishka.backend.event.order.OrderCreatedEvent;
import org.plishka.backend.service.notification.email.transport.AsyncEmailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final AsyncEmailSender asyncEmailSender;
    private final EmailTemplateBuilder templateBuilder;
    private final BackendProperties backendProperties;

    public void sendEmailVerificationEmail(String email, String verificationLink) {
        queueEmail(
                email,
                EmailSubjects.EMAIL_VERIFICATION,
                templateBuilder.buildEmailVerificationText(verificationLink)
        );
    }

    public void sendPasswordResetEmail(String email, String resetPasswordLink) {
        queueEmail(
                email,
                EmailSubjects.PASSWORD_RESET,
                templateBuilder.buildPasswordResetEmailText(resetPasswordLink)
        );
    }

    public void sendEmailChangeVerificationEmail(String email, String verificationLink) {
        queueEmail(
                email,
                EmailSubjects.EMAIL_CHANGE_VERIFICATION,
                templateBuilder.buildEmailChangeVerificationText(verificationLink)
        );
    }

    public void sendEmailChangedNotificationEmail(String oldEmail, String newEmail) {
        queueEmail(
                oldEmail,
                EmailSubjects.EMAIL_CHANGED,
                templateBuilder.buildEmailChangedNotificationText(newEmail)
        );
    }

    public void sendOrderCreatedNotifications(OrderCreatedEvent event) {
        queueEmail(
                event.userEmail(),
                EmailSubjects.orderConfirmationUser(event.orderNumber()),
                templateBuilder.buildOrderEmailText(event)
        );
        queueEmail(
                backendProperties.admin().email(),
                EmailSubjects.orderNotificationAdmin(event.orderNumber()),
                templateBuilder.buildOrderAdminEmailText(event)
        );
    }

    public void sendCallbackCreatedNotifications(CallbackRequestCreatedEvent event) {
        queueEmail(
                event.userEmail(),
                EmailSubjects.CALLBACK_CONFIRMATION_USER,
                templateBuilder.buildCallbackConfirmationUserText(event)
        );
        queueEmail(
                backendProperties.admin().email(),
                EmailSubjects.CALLBACK_NOTIFICATION_ADMIN,
                templateBuilder.buildCallbackNotificationAdminText(event)
        );
    }

    private void queueEmail(String to, String subject, String text) {
        try {
            asyncEmailSender.sendEmailAsync(to, subject, text);
        } catch (RejectedExecutionException exception) {
            log.error(
                    "Email task rejected because executor is saturated: to={}, subject={}",
                    to,
                    subject,
                    exception
            );
        }
    }
}
