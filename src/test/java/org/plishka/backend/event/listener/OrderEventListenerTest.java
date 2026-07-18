package org.plishka.backend.event.listener;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.event.order.OrderCreatedEvent;
import org.plishka.backend.monitoring.metrics.BusinessMetricsRecorder;
import org.plishka.backend.service.notification.NotificationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {
    @Mock
    private NotificationService notificationService;

    private SimpleMeterRegistry meterRegistry;
    private OrderEventListener listener;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        listener = new OrderEventListener(
                notificationService,
                new BusinessMetricsRecorder(meterRegistry)
        );
    }

    @Test
    void handleOrderCreated_ShouldRecordCreatedMetricsAndNotify() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(100L)
                .orderNumber("ORD-123")
                .userId(1L)
                .userEmail("customer@example.com")
                .customerName("Customer")
                .deliveryCity("Kyiv")
                .phone("+380501234567")
                .notes(null)
                .totalPrice(900L)
                .createdAt(Instant.parse("2026-06-19T10:00:00Z"))
                .items(List.of(
                        OrderCreatedEvent.Item.builder()
                                .productName("Chair")
                                .categoryName("Furniture")
                                .quantity(2)
                                .unitPrice(450L)
                                .lineTotal(900L)
                                .build()
                ))
                .build();

        listener.handleOrderCreated(event);

        assertEquals(1.0, meterRegistry.get("order.created").counter().count());
        assertEquals(1, meterRegistry.get("order.total.price").summary().count());
        assertEquals(900.0, meterRegistry.get("order.total.price").summary().totalAmount());
        assertEquals(1, meterRegistry.get("order.items.count").summary().count());
        assertEquals(1.0, meterRegistry.get("order.items.count").summary().totalAmount());
        verify(notificationService).notifyOrderCreated(event);
    }
}
