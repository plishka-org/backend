package org.plishka.backend.service.notification;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.event.callback.CallbackRequestCreatedEvent;
import org.plishka.backend.event.order.OrderCreatedEvent;
import org.plishka.backend.service.notification.email.EmailDisplayFormatter;
import org.plishka.backend.service.notification.email.EmailTemplateBuilder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EmailTemplateBuilderTest {
    private static final Instant CREATED_AT = Instant.parse("2026-06-19T10:00:00Z");
    private static final String USER_EMAIL = "user@example.com";
    private static final String VERIFICATION_LINK = "https://example.com/verify";
    private static final String RESET_LINK = "https://example.com/reset";

    @Mock
    private BackendProperties backendProperties;

    private EmailTemplateBuilder templateBuilder;

    @BeforeEach
    void setUp() {
        lenient().when(backendProperties.auth()).thenReturn(new BackendProperties.Auth(Duration.ofHours(24), Duration.ofHours(3)));

        templateBuilder = new EmailTemplateBuilder(new EmailDisplayFormatter(), backendProperties);
    }

    @Test
    void buildEmailVerificationText_ShouldIncludeVerificationLinkAndTtl() {
        String text = templateBuilder.buildEmailVerificationText(VERIFICATION_LINK);

        assertTrue(text.contains(VERIFICATION_LINK));
        assertTrue(text.contains("24 год."));
        assertTrue(text.contains("Дякуємо за реєстрацію в Plishka."));
    }

    @Test
    void buildPasswordResetEmailText_ShouldIncludeResetLinkAndTtl() {
        String text = templateBuilder.buildPasswordResetEmailText(RESET_LINK);

        assertTrue(text.contains(RESET_LINK));
        assertTrue(text.contains("3 год."));
    }

    @Test
    void buildEmailChangeVerificationText_ShouldIncludeVerificationLinkAndTtl() {
        String text = templateBuilder.buildEmailChangeVerificationText(VERIFICATION_LINK);

        assertTrue(text.contains(VERIFICATION_LINK));
        assertTrue(text.contains("24 год."));
    }

    @Test
    void buildEmailChangedNotificationText_ShouldIncludeNewEmail() {
        String text = templateBuilder.buildEmailChangedNotificationText("new@example.com");

        assertTrue(text.contains("new@example.com"));
    }

    @Test
    void buildOrderEmailText_ShouldIncludeOrderDetails() {
        String text = templateBuilder.buildOrderEmailText(sampleOrderEvent());

        assertTrue(text.contains("ORD-123"));
        assertTrue(text.contains("Іван"));
        assertTrue(text.contains("Київ"));
        assertTrue(text.contains("Подзвонити"));
        assertTrue(text.contains("Стілець"));
        assertTrue(text.contains("900.00 грн"));
    }

    @Test
    void buildOrderAdminEmailText_ShouldIncludeOrderAndUserDetails() {
        String text = templateBuilder.buildOrderAdminEmailText(sampleOrderEvent());

        assertTrue(text.contains("ID замовлення: 100"));
        assertTrue(text.contains("ID користувача: 1"));
        assertTrue(text.contains("customer@example.com"));
    }

    @Test
    void buildOrderEmailText_ShouldUseEmptyPlaceholderForBlankNotes() {
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

        String text = templateBuilder.buildOrderEmailText(event);

        assertTrue(text.contains("Коментар: " + EmailDisplayFormatter.EMPTY_VALUE));
        assertTrue(text.contains("Товари:\n" + EmailDisplayFormatter.EMPTY_VALUE));
    }

    @Test
    void buildOrderAdminEmailText_ShouldUseUnknownUserLabelWhenUserIdMissing() {
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

        String text = templateBuilder.buildOrderAdminEmailText(event);

        assertTrue(text.contains("ID користувача: " + EmailDisplayFormatter.UNKNOWN_USER_ID));
    }

    @Test
    void buildCallbackConfirmationUserText_ShouldIncludeCallbackDetails() {
        String text = templateBuilder.buildCallbackConfirmationUserText(sampleCallbackEvent());

        assertTrue(text.contains("Іван"));
        assertTrue(text.contains("Подзвоніть, будь ласка"));
    }

    @Test
    void buildCallbackNotificationAdminText_ShouldIncludeUserDetails() {
        String text = templateBuilder.buildCallbackNotificationAdminText(sampleCallbackEvent());

        assertTrue(text.contains("ID користувача: 1"));
        assertTrue(text.contains(USER_EMAIL));
    }

    @Test
    void buildCallbackNotificationAdminText_ShouldUseUnknownUserLabelWhenUserIdMissing() {
        CallbackRequestCreatedEvent event = CallbackRequestCreatedEvent.builder()
                .callbackRequestId(10L)
                .userId(null)
                .userEmail(USER_EMAIL)
                .name("Іван")
                .phone("+380501234567")
                .message("Подзвоніть, будь ласка")
                .createdAt(CREATED_AT)
                .build();

        String text = templateBuilder.buildCallbackNotificationAdminText(event);

        assertTrue(text.contains("ID користувача: " + EmailDisplayFormatter.UNKNOWN_USER_ID));
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
