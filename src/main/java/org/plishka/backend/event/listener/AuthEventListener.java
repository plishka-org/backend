package org.plishka.backend.event.listener;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.event.auth.EmailChangeRequestedEvent;
import org.plishka.backend.event.auth.EmailChangedEvent;
import org.plishka.backend.event.auth.EmailVerificationRequestedEvent;
import org.plishka.backend.event.auth.PasswordResetRequestedEvent;
import org.plishka.backend.service.notification.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AuthEventListener {
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        notificationService.notifyEmailVerificationRequested(
                event.email(),
                event.verificationLink()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        notificationService.notifyPasswordResetRequested(
                event.email(),
                event.resetPasswordLink()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailChangeRequested(EmailChangeRequestedEvent event) {
        notificationService.notifyEmailChangeRequested(
                event.email(),
                event.verificationLink()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailChanged(EmailChangedEvent event) {
        notificationService.notifyEmailChanged(
                event.oldEmail(),
                event.newEmail()
        );
    }
}
