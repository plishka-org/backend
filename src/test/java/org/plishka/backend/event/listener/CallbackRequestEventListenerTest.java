package org.plishka.backend.event.listener;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.event.callback.CallbackRequestCreatedEvent;
import org.plishka.backend.monitoring.metrics.BusinessMetricsRecorder;
import org.plishka.backend.service.notification.NotificationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CallbackRequestEventListenerTest {
    @Mock
    private NotificationService notificationService;

    private SimpleMeterRegistry meterRegistry;
    private CallbackRequestEventListener listener;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        listener = new CallbackRequestEventListener(
                notificationService,
                new BusinessMetricsRecorder(meterRegistry)
        );
    }

    @Test
    void handleCallbackRequestCreated_ShouldRecordCreatedMetricAndNotify() {
        CallbackRequestCreatedEvent event = CallbackRequestCreatedEvent.builder()
                .callbackRequestId(10L)
                .userId(1L)
                .userEmail("user@example.com")
                .name("Customer")
                .phone("+380501234567")
                .message("Call me")
                .createdAt(Instant.parse("2026-06-19T10:00:00Z"))
                .build();

        listener.handleCallbackRequestCreated(event);

        assertEquals(1.0, meterRegistry.get("callback.request.created").counter().count());
        verify(notificationService).notifyCallbackRequestCreated(event);
    }
}
