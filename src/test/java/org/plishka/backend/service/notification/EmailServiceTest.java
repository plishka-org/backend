package org.plishka.backend.service.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.event.callback.CallbackRequestCreatedEvent;
import org.plishka.backend.event.order.OrderCreatedEvent;
import org.plishka.backend.service.notification.email.EmailService;
import org.plishka.backend.service.notification.email.EmailSubjects;
import org.plishka.backend.service.notification.email.EmailTemplateBuilder;
import org.plishka.backend.service.notification.email.transport.AsyncEmailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private EmailTemplateBuilder templateBuilder;

    @Mock
    private BackendProperties backendProperties;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        lenient().when(backendProperties.admin()).thenReturn(new BackendProperties.Admin(ADMIN_EMAIL));

        emailService = new EmailService(asyncEmailSender, templateBuilder, backendProperties);
    }

    @Test
    void sendEmailVerificationEmail_ShouldQueueEmailWithSubjectAndBody() {
        when(templateBuilder.buildEmailVerificationText(VERIFICATION_LINK)).thenReturn("verification body");

        emailService.sendEmailVerificationEmail(USER_EMAIL, VERIFICATION_LINK);

        verify(templateBuilder).buildEmailVerificationText(VERIFICATION_LINK);
        verify(asyncEmailSender).sendEmailAsync(USER_EMAIL, EmailSubjects.EMAIL_VERIFICATION, "verification body");
    }

    @Test
    void sendPasswordResetEmail_ShouldQueueEmailWithSubjectAndBody() {
        when(templateBuilder.buildPasswordResetEmailText(RESET_LINK)).thenReturn("reset body");

        emailService.sendPasswordResetEmail(USER_EMAIL, RESET_LINK);

        verify(templateBuilder).buildPasswordResetEmailText(RESET_LINK);
        verify(asyncEmailSender).sendEmailAsync(USER_EMAIL, EmailSubjects.PASSWORD_RESET, "reset body");
    }

    @Test
    void sendEmailChangeVerificationEmail_ShouldQueueEmailWithSubjectAndBody() {
        when(templateBuilder.buildEmailChangeVerificationText(VERIFICATION_LINK)).thenReturn("change body");

        emailService.sendEmailChangeVerificationEmail(USER_EMAIL, VERIFICATION_LINK);

        verify(templateBuilder).buildEmailChangeVerificationText(VERIFICATION_LINK);
        verify(asyncEmailSender).sendEmailAsync(USER_EMAIL, EmailSubjects.EMAIL_CHANGE_VERIFICATION, "change body");
    }

    @Test
    void sendEmailChangedNotificationEmail_ShouldQueueEmailWithSubjectAndBody() {
        when(templateBuilder.buildEmailChangedNotificationText("new@example.com")).thenReturn("changed body");

        emailService.sendEmailChangedNotificationEmail("old@example.com", "new@example.com");

        verify(templateBuilder).buildEmailChangedNotificationText("new@example.com");
        verify(asyncEmailSender).sendEmailAsync("old@example.com", EmailSubjects.EMAIL_CHANGED, "changed body");
    }

    @Test
    void sendOrderCreatedNotifications_ShouldQueueCustomerAndAdminEmails() {
        OrderCreatedEvent event = sampleOrderEvent();
        when(templateBuilder.buildOrderEmailText(event)).thenReturn("order user body");
        when(templateBuilder.buildOrderAdminEmailText(event)).thenReturn("order admin body");

        emailService.sendOrderCreatedNotifications(event);

        verify(asyncEmailSender).sendEmailAsync(
                "customer@example.com",
                EmailSubjects.orderConfirmationUser("ORD-123"),
                "order user body"
        );
        verify(asyncEmailSender).sendEmailAsync(
                ADMIN_EMAIL,
                EmailSubjects.orderNotificationAdmin("ORD-123"),
                "order admin body"
        );
    }

    @Test
    void sendCallbackCreatedNotifications_ShouldQueueUserAndAdminEmails() {
        CallbackRequestCreatedEvent event = sampleCallbackEvent();
        when(templateBuilder.buildCallbackConfirmationUserText(event)).thenReturn("callback user body");
        when(templateBuilder.buildCallbackNotificationAdminText(event)).thenReturn("callback admin body");

        emailService.sendCallbackCreatedNotifications(event);

        verify(asyncEmailSender).sendEmailAsync(
                USER_EMAIL,
                EmailSubjects.CALLBACK_CONFIRMATION_USER,
                "callback user body"
        );
        verify(asyncEmailSender).sendEmailAsync(
                ADMIN_EMAIL,
                EmailSubjects.CALLBACK_NOTIFICATION_ADMIN,
                "callback admin body"
        );
    }

    @Test
    void queueEmail_ShouldNotPropagateRejectedExecutionException() {
        when(templateBuilder.buildEmailVerificationText(VERIFICATION_LINK)).thenReturn("verification body");
        doThrow(new RejectedExecutionException("executor saturated"))
                .when(asyncEmailSender)
                .sendEmailAsync(any(), any(), any());

        assertDoesNotThrow(() -> emailService.sendEmailVerificationEmail(USER_EMAIL, VERIFICATION_LINK));

        verify(asyncEmailSender).sendEmailAsync(
                eq(USER_EMAIL),
                eq(EmailSubjects.EMAIL_VERIFICATION),
                eq("verification body")
        );
    }

    @Test
    void orderConfirmationSubject_ShouldIncludeOrderNumber() {
        assertEquals("Ваше замовлення ORD-123", EmailSubjects.orderConfirmationUser("ORD-123"));
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
