package org.plishka.backend.service.notification;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.event.callback.CallbackRequestCreatedEvent;
import org.plishka.backend.event.order.OrderCreatedEvent;
import org.plishka.backend.service.notification.email.EmailService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    private static final String USER_EMAIL = "user@example.com";
    private static final String VERIFICATION_LINK = "https://example.com/verify";

    @Mock
    private EmailService emailService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(emailService);
    }

    @Test
    void notifyEmailVerificationRequested_ShouldDelegateToEmailService() {
        notificationService.notifyEmailVerificationRequested(USER_EMAIL, VERIFICATION_LINK);

        verify(emailService).sendEmailVerificationEmail(USER_EMAIL, VERIFICATION_LINK);
    }

    @Test
    void notifyEmailVerificationRequested_ShouldNotPropagateEmailServiceFailures() {
        doThrow(new RuntimeException("email failed"))
                .when(emailService)
                .sendEmailVerificationEmail(USER_EMAIL, VERIFICATION_LINK);

        assertDoesNotThrow(() ->
                notificationService.notifyEmailVerificationRequested(USER_EMAIL, VERIFICATION_LINK));
    }

    @Test
    void notifyOrderCreated_ShouldNotPropagateEmailServiceFailures() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(100L)
                .orderNumber("ORD-123")
                .userId(1L)
                .userEmail(USER_EMAIL)
                .customerName("Іван")
                .deliveryCity("Київ")
                .phone("+380501234567")
                .notes("Подзвонити")
                .totalPrice(900L)
                .createdAt(Instant.parse("2026-06-19T10:00:00Z"))
                .items(List.of())
                .build();

        doThrow(new RuntimeException("email failed"))
                .when(emailService)
                .sendOrderCreatedNotifications(event);

        assertDoesNotThrow(() -> notificationService.notifyOrderCreated(event));
    }

    @Test
    void notifyCallbackRequestCreated_ShouldNotPropagateEmailServiceFailures() {
        CallbackRequestCreatedEvent event = CallbackRequestCreatedEvent.builder()
                .callbackRequestId(10L)
                .userId(1L)
                .userEmail(USER_EMAIL)
                .name("Іван")
                .phone("+380501234567")
                .message("Подзвоніть")
                .createdAt(Instant.parse("2026-06-19T10:00:00Z"))
                .build();

        doThrow(new RuntimeException("email failed"))
                .when(emailService)
                .sendCallbackCreatedNotifications(event);

        assertDoesNotThrow(() -> notificationService.notifyCallbackRequestCreated(event));
    }
}
