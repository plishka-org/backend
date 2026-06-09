package org.plishka.backend.event.listener;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.event.callback.CallbackRequestCreatedEvent;
import org.plishka.backend.service.notification.EmailService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CallbackRequestEventListener {
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCallbackRequestCreated(CallbackRequestCreatedEvent event) {
        emailService.sendCallbackRequestEmail(event);
    }
}
