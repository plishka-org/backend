package org.plishka.backend.service.notification;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.event.callback.CallbackRequestCreatedEvent;
import org.plishka.backend.event.order.OrderCreatedEvent;
import org.plishka.backend.service.notification.email.EmailDisplayFormatter;
import org.plishka.backend.service.notification.email.EmailService;
import org.plishka.backend.service.notification.email.transport.AsyncEmailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    private static final Instant CREATED_AT = Instant.parse("2026-06-19T10:00:00Z");
    private static final String USER_EMAIL = "user@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String VERIFICATION_LINK = "https://example.com/verify";
    private static final String RESET_LINK = "https://example.com/reset";

    @Mock
    private AsyncEmailSender asyncEmailSender;

    @Mock
    private BackendProperties backendProperties;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        lenient().when(backendProperties.auth()).thenReturn(new BackendProperties.Auth(Duration.ofHours(24), Duration.ofHours(3)));
        lenient().when(backendProperties.admin()).thenReturn(new BackendProperties.Admin(ADMIN_EMAIL));

        emailService = new EmailService(
                asyncEmailSender,
                new EmailDisplayFormatter(),
                backendProperties
        );
    }

    @Test
    void sendEmailVerificationEmail_ShouldQueueEmailWithVerificationData() {
        emailService.sendEmailVerificationEmail(USER_EMAIL, VERIFICATION_LINK);

        String text = captureQueuedText(USER_EMAIL, "Підтвердження email");

        assertTrue(text.contains(VERIFICATION_LINK));
        assertTrue(text.contains("24 год."));
        assertTrue(text.contains("Дякуємо за реєстрацію в Plishka."));
    }

    @Test
    void sendPasswordResetEmail_ShouldQueueEmailWithResetData() {
        emailService.sendPasswordResetEmail(USER_EMAIL, RESET_LINK);

        String text = captureQueuedText(USER_EMAIL, "Відновлення пароля");

        assertTrue(text.contains(RESET_LINK));
        assertTrue(text.contains("3 год."));
    }

    @Test
    void sendEmailChangeVerificationEmail_ShouldQueueEmailWithVerificationData() {
        emailService.sendEmailChangeVerificationEmail(USER_EMAIL, VERIFICATION_LINK);

        String text = captureQueuedText(USER_EMAIL, "Підтвердження нової email-адреси");

        assertTrue(text.contains(VERIFICATION_LINK));
        assertTrue(text.contains("24 год."));
    }

    @Test
    void sendEmailChangedNotificationEmail_ShouldQueueEmailWithNewEmail() {
        emailService.sendEmailChangedNotificationEmail("old@example.com", "new@example.com");

        String text = captureQueuedText("old@example.com", "Email вашого акаунта змінено");

        assertTrue(text.contains("new@example.com"));
    }

    @Test
    void sendOrderCreatedNotifications_ShouldQueueCustomerAndAdminEmails() {
        emailService.sendOrderCreatedNotifications(sampleOrderEvent());

        String customerText = captureQueuedText("customer@example.com", "Ваше замовлення ORD-123");
        String adminText = captureQueuedText(ADMIN_EMAIL, "Нове замовлення ORD-123");

        assertTrue(customerText.contains("ORD-123"));
        assertTrue(customerText.contains("Іван"));
        assertTrue(customerText.contains("Київ"));
        assertTrue(customerText.contains("Подзвонити"));
        assertTrue(customerText.contains("Стілець"));
        assertTrue(customerText.contains("900.00 грн"));

        assertTrue(adminText.contains("ID замовлення: 100"));
        assertTrue(adminText.contains("ID користувача: 1"));
        assertTrue(adminText.contains("customer@example.com"));
    }

    @Test
    void sendOrderCreatedNotifications_ShouldUseUnknownUserLabelWhenUserIdMissing() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(100L)
                .orderNumber("ORD-123")
                .userId(null)
                .userEmail("customer@example.com")
                .customerName("Іван")
                .deliveryCity("Київ")
                .phone("+380501234567")
                .notes("Подзвонити")
                .totalPrice(new BigDecimal("900.00"))
                .createdAt(CREATED_AT)
                .items(List.of())
                .build();

        emailService.sendOrderCreatedNotifications(event);

        String adminText = captureQueuedText(ADMIN_EMAIL, "Нове замовлення ORD-123");

        assertTrue(adminText.contains("ID користувача: " + EmailDisplayFormatter.UNKNOWN_USER_ID));
    }

    @Test
    void sendOrderCreatedNotifications_ShouldUseEmptyPlaceholderForBlankNotes() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(100L)
                .orderNumber("ORD-123")
                .userId(1L)
                .userEmail("customer@example.com")
                .customerName("Іван")
                .deliveryCity("Київ")
                .phone("+380501234567")
                .notes("  ")
                .totalPrice(new BigDecimal("900.00"))
                .createdAt(CREATED_AT)
                .items(List.of())
                .build();

        emailService.sendOrderCreatedNotifications(event);

        String text = captureQueuedText("customer@example.com", "Ваше замовлення ORD-123");

        assertTrue(text.contains("Коментар: " + EmailDisplayFormatter.EMPTY_VALUE));
        assertTrue(text.contains("Товари:\n" + EmailDisplayFormatter.EMPTY_VALUE));
    }

    @Test
    void sendCallbackCreatedNotifications_ShouldQueueUserAndAdminEmails() {
        emailService.sendCallbackCreatedNotifications(sampleCallbackEvent());

        String userText = captureQueuedText(USER_EMAIL, "Ми отримали вашу заявку на дзвінок");
        String adminText = captureQueuedText(ADMIN_EMAIL, "Нова заявка на дзвінок");

        assertTrue(userText.contains("Іван"));
        assertTrue(userText.contains("Подзвоніть, будь ласка"));
        assertTrue(adminText.contains("ID користувача: 1"));
        assertTrue(adminText.contains(USER_EMAIL));
    }

    @Test
    void sendCallbackCreatedNotifications_ShouldUseUnknownUserLabelWhenUserIdMissing() {
        CallbackRequestCreatedEvent event = CallbackRequestCreatedEvent.builder()
                .callbackRequestId(10L)
                .userId(null)
                .userEmail(USER_EMAIL)
                .name("Іван")
                .phone("+380501234567")
                .message("Подзвоніть, будь ласка")
                .createdAt(CREATED_AT)
                .build();

        emailService.sendCallbackCreatedNotifications(event);

        String adminText = captureQueuedText(ADMIN_EMAIL, "Нова заявка на дзвінок");

        assertTrue(adminText.contains("ID користувача: " + EmailDisplayFormatter.UNKNOWN_USER_ID));
    }

    @Test
    void queueEmail_ShouldNotPropagateRejectedExecutionException() {
        doThrow(new RejectedExecutionException("executor saturated"))
                .when(asyncEmailSender)
                .sendEmailAsync(any(), any(), any());

        assertDoesNotThrow(() -> emailService.sendEmailVerificationEmail(USER_EMAIL, VERIFICATION_LINK));

        verify(asyncEmailSender).sendEmailAsync(
                eq(USER_EMAIL),
                eq("Підтвердження email"),
                any()
        );
    }

    private String captureQueuedText(String to, String subject) {
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);

        verify(asyncEmailSender).sendEmailAsync(
                eq(to),
                eq(subject),
                textCaptor.capture()
        );

        return textCaptor.getValue();
    }

    private static OrderCreatedEvent sampleOrderEvent() {
        return OrderCreatedEvent.builder()
                .orderId(100L)
                .orderNumber("ORD-123")
                .userId(1L)
                .userEmail("customer@example.com")
                .customerName("Іван")
                .deliveryCity("Київ")
                .phone("+380501234567")
                .notes("Подзвонити")
                .totalPrice(new BigDecimal("900.00"))
                .createdAt(CREATED_AT)
                .items(List.of(OrderCreatedEvent.Item.builder()
                        .productName("Стілець")
                        .categoryName("Дерево")
                        .quantity(2)
                        .unitPrice(new BigDecimal("450.00"))
                        .lineTotal(new BigDecimal("900.00"))
                        .build()))
                .build();
    }

    private static CallbackRequestCreatedEvent sampleCallbackEvent() {
        return CallbackRequestCreatedEvent.builder()
                .callbackRequestId(10L)
                .userId(1L)
                .userEmail(USER_EMAIL)
                .name("Іван")
                .phone("+380501234567")
                .message("Подзвоніть, будь ласка")
                .createdAt(CREATED_AT)
                .build();
    }
}
