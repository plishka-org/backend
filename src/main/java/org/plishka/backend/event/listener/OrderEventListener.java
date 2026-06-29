package org.plishka.backend.event.listener;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.event.order.OrderCreatedEvent;
import org.plishka.backend.service.notification.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        notificationService.notifyOrderCreated(event);
    }
}
