package org.plishka.backend.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.event.auth.EmailVerificationRequestedEvent;
import org.plishka.backend.service.notification.EmailService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventListener {
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        emailService.sendEmailVerificationEmail(
                event.email(),
                event.verificationLink()
        );
    }
}
