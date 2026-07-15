package org.plishka.backend.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.event.callback.CallbackRequestCreatedEvent;
import org.plishka.backend.event.order.OrderCreatedEvent;
import org.plishka.backend.service.notification.email.EmailService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final EmailService emailService;

    public void notifyEmailVerificationRequested(String email, String verificationLink) {
        try {
            emailService.sendEmailVerificationEmail(email, verificationLink);
        } catch (Exception exception) {
            log.error("Failed to send email verification notification", exception);
        }
    }

    public void notifyPasswordResetRequested(String email, String resetPasswordLink) {
        try {
            emailService.sendPasswordResetEmail(email, resetPasswordLink);
        } catch (Exception exception) {
            log.error("Failed to send password reset notification", exception);
        }
    }

    public void notifyEmailChangeRequested(String email, String verificationLink) {
        try {
            emailService.sendEmailChangeVerificationEmail(email, verificationLink);
        } catch (Exception exception) {
            log.error("Failed to send email change verification notification", exception);
        }
    }

    public void notifyEmailChanged(String oldEmail, String newEmail) {
        try {
            emailService.sendEmailChangedNotificationEmail(oldEmail, newEmail);
        } catch (Exception exception) {
            log.error("Failed to send email changed notification", exception);
        }
    }

    public void notifyOrderCreated(OrderCreatedEvent event) {
        try {
            emailService.sendOrderCreatedNotifications(event);
        } catch (Exception exception) {
            log.error(
                    "Failed to send order notification: orderId={}",
                    event.orderId(),
                    exception
            );
        }
    }

    public void notifyCallbackRequestCreated(CallbackRequestCreatedEvent event) {
        try {
            emailService.sendCallbackCreatedNotifications(event);
        } catch (Exception exception) {
            log.error(
                    "Failed to send callback notification: callbackRequestId={}",
                    event.callbackRequestId(),
                    exception
            );
        }
    }
}
