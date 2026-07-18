package org.plishka.backend.service.notification.email;

import java.util.concurrent.RejectedExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.event.callback.CallbackRequestCreatedEvent;
import org.plishka.backend.event.order.OrderCreatedEvent;
import org.plishka.backend.monitoring.metrics.EmailMetricsRecorder;
import org.plishka.backend.monitoring.sentry.SentryMonitoringService;
import org.plishka.backend.service.notification.email.transport.AsyncEmailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final AsyncEmailSender asyncEmailSender;
    private final EmailTemplateBuilder templateBuilder;
    private final EmailMetricsRecorder emailMetricsRecorder;
    private final SentryMonitoringService sentryMonitoringService;

    public void sendEmailVerificationEmail(String email, String verificationLink) {
        queueEmail(
                EmailType.VERIFICATION,
                email,
                EmailSubjects.EMAIL_VERIFICATION,
                templateBuilder.buildEmailVerificationText(verificationLink)
        );
    }

    public void sendPasswordResetEmail(String email, String resetPasswordLink) {
        queueEmail(
                EmailType.PASSWORD_RESET,
                email,
                EmailSubjects.PASSWORD_RESET,
                templateBuilder.buildPasswordResetEmailText(resetPasswordLink)
        );
    }

    public void sendEmailChangeVerificationEmail(String email, String verificationLink) {
        queueEmail(
                EmailType.EMAIL_CHANGE,
                email,
                EmailSubjects.EMAIL_CHANGE_VERIFICATION,
                templateBuilder.buildEmailChangeVerificationText(verificationLink)
        );
    }

    public void sendEmailChangedNotificationEmail(String oldEmail, String newEmail) {
        queueEmail(
                EmailType.EMAIL_CHANGE,
                oldEmail,
                EmailSubjects.EMAIL_CHANGED,
                templateBuilder.buildEmailChangedNotificationText(newEmail)
        );
    }

    public void sendOrderCreatedUserNotification(OrderCreatedEvent event) {
        queueEmail(
                EmailType.ORDER_USER,
                event.userEmail(),
                EmailSubjects.orderConfirmationUser(event.orderNumber()),
                templateBuilder.buildOrderEmailText(event)
        );
    }

    public void sendCallbackCreatedUserNotification(CallbackRequestCreatedEvent event) {
        queueEmail(
                EmailType.CALLBACK_USER,
                event.userEmail(),
                EmailSubjects.CALLBACK_CONFIRMATION_USER,
                templateBuilder.buildCallbackConfirmationUserText(event)
        );
    }

    private void queueEmail(EmailType emailType, String to, String subject, String text) {
        try {
            asyncEmailSender.sendEmailAsync(emailType, to, subject, text);
            emailMetricsRecorder.recordQueued(emailType);
        } catch (RejectedExecutionException exception) {
            emailMetricsRecorder.recordRejected(emailType);
            sentryMonitoringService.captureException(exception, "email", emailType.getMetricValue());
            log.error(
                    "Email task rejected because executor is saturated: emailType={}",
                    emailType.getMetricValue(),
                    exception
            );
        }
    }
}
