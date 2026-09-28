package org.plishka.backend.service.notification;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.notification.AdminNotificationOutbox;
import org.plishka.backend.domain.notification.AdminNotificationOutboxStatus;
import org.plishka.backend.domain.notification.AdminNotificationType;
import org.plishka.backend.domain.settings.SystemSettings;
import org.plishka.backend.event.callback.CallbackRequestCreatedEvent;
import org.plishka.backend.event.order.OrderCreatedEvent;
import org.plishka.backend.exception.NonRetryableEmailException;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.exception.RetryableEmailException;
import org.plishka.backend.monitoring.metrics.EmailMetricsRecorder;
import org.plishka.backend.monitoring.sentry.SentryMonitoringService;
import org.plishka.backend.repository.notification.AdminNotificationOutboxRepository;
import org.plishka.backend.repository.settings.SystemSettingsRepository;
import org.plishka.backend.service.notification.email.AdminNotificationOutboxService;
import org.plishka.backend.service.notification.email.EmailSubjects;
import org.plishka.backend.service.notification.email.EmailTemplateBuilder;
import org.plishka.backend.service.notification.email.EmailType;
import org.plishka.backend.service.notification.email.transport.EmailTransport;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminNotificationOutboxServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-24T12:00:00Z");
    private static final String ADMIN_EMAIL = "admin@example.com";

    @Mock
    private AdminNotificationOutboxRepository adminNotificationOutboxRepository;

    @Mock
    private EmailTransport emailTransport;

    @Mock
    private EmailTemplateBuilder templateBuilder;

    @Mock
    private SystemSettingsRepository systemSettingsRepository;

    @Mock
    private Validator validator;

    private final TransactionOperations transactionOperations = new TransactionOperations() {
        @Override
        public <T> T execute(TransactionCallback<T> action) {
            return action.doInTransaction(null);
        }
    };

    private AdminNotificationOutboxService service;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        SystemSettings settings = new SystemSettings();
        settings.setId(SystemSettings.SINGLETON_ID);
        settings.setAdminEmail(ADMIN_EMAIL);
        lenient().when(systemSettingsRepository.findById(SystemSettings.SINGLETON_ID)).thenReturn(Optional.of(settings));
        lenient().when(validator.validateProperty(settings, "adminEmail")).thenReturn(Set.of());
        service = new AdminNotificationOutboxService(
                adminNotificationOutboxRepository,
                emailTransport,
                templateBuilder,
                systemSettingsRepository,
                validator,
                transactionOperations,
                Clock.fixed(NOW, ZoneOffset.UTC),
                new EmailMetricsRecorder(meterRegistry),
                new SentryMonitoringService()
        );
    }

    @Test
    void enqueueOrderCreated_ShouldPersistAdminEmailSnapshot() {
        OrderCreatedEvent event = sampleOrderEvent();
        when(templateBuilder.buildOrderAdminEmailText(event)).thenReturn("order admin body");

        service.enqueueOrderCreated(event);

        ArgumentCaptor<AdminNotificationOutbox> entryCaptor =
                ArgumentCaptor.forClass(AdminNotificationOutbox.class);
        verify(adminNotificationOutboxRepository).save(entryCaptor.capture());

        AdminNotificationOutbox entry = entryCaptor.getValue();
        assertEquals(AdminNotificationType.ORDER_CREATED, entry.getNotificationType());
        assertEquals(event.orderId(), entry.getSourceId());
        assertEquals(ADMIN_EMAIL, entry.getRecipient());
        assertEquals(EmailSubjects.orderNotificationAdmin(event.orderNumber()), entry.getSubject());
        assertEquals("order admin body", entry.getBody());
        assertEquals(AdminNotificationOutboxStatus.PENDING, entry.getStatus());
        assertEquals(0, entry.getAttempts());
        assertEquals(NOW, entry.getNextAttemptAt());
    }

    @Test
    void enqueueCallbackCreated_ShouldPersistAdminEmailSnapshot() {
        CallbackRequestCreatedEvent event = sampleCallbackEvent();
        when(templateBuilder.buildCallbackNotificationAdminText(event)).thenReturn("callback admin body");

        service.enqueueCallbackCreated(event);

        ArgumentCaptor<AdminNotificationOutbox> entryCaptor =
                ArgumentCaptor.forClass(AdminNotificationOutbox.class);
        verify(adminNotificationOutboxRepository).save(entryCaptor.capture());

        AdminNotificationOutbox entry = entryCaptor.getValue();
        assertEquals(AdminNotificationType.CALLBACK_CREATED, entry.getNotificationType());
        assertEquals(event.callbackRequestId(), entry.getSourceId());
        assertEquals(EmailSubjects.CALLBACK_NOTIFICATION_ADMIN, entry.getSubject());
        assertEquals("callback admin body", entry.getBody());
    }

    @Test
    void enqueueOrderCreated_ShouldFailWithOperationalError_WhenSystemSettingsAreMissing() {
        when(systemSettingsRepository.findById(SystemSettings.SINGLETON_ID)).thenReturn(Optional.empty());

        assertThrows(
                RequiredSingletonUnavailableException.class,
                () -> service.enqueueOrderCreated(sampleOrderEvent())
        );

        verify(adminNotificationOutboxRepository, never()).save(any(AdminNotificationOutbox.class));
    }

    @Test
    void processDueNotifications_ShouldSendEmailAndDeleteOutboxRow_WhenSendSucceeds() {
        AdminNotificationOutbox entry = outboxEntry(1L, AdminNotificationType.ORDER_CREATED, 100L, 0);
        when(adminNotificationOutboxRepository.findDueForUpdate(
                eq(AdminNotificationOutboxStatus.PENDING),
                eq(NOW),
                any(Pageable.class)
        )).thenReturn(List.of(entry));

        int processedCount = service.processDueNotifications();

        assertEquals(1, processedCount);
        verify(emailTransport).sendEmail(EmailType.ORDER_ADMIN, ADMIN_EMAIL, "subject", "body");
        verify(adminNotificationOutboxRepository).delete(entry);
        assertEquals(1.0, outboxEntriesCounter(EmailMetricsRecorder.OUTCOME_PROCESSED));
        assertEquals(1.0, outboxEntriesCounter(EmailMetricsRecorder.OUTCOME_SENT));
        assertEquals(1.0, emailDeliveryCounter(EmailType.ORDER_ADMIN, EmailMetricsRecorder.OUTCOME_SENT));
    }

    @Test
    void processDueNotifications_ShouldScheduleRetry_WhenSendFailsWithRetryableError() {
        AdminNotificationOutbox entry = outboxEntry(1L, AdminNotificationType.CALLBACK_CREATED, 10L, 0);
        when(adminNotificationOutboxRepository.findDueForUpdate(
                eq(AdminNotificationOutboxStatus.PENDING),
                eq(NOW),
                any(Pageable.class)
        )).thenReturn(List.of(entry));
        doThrow(new RetryableEmailException("Resend temporary failure with status 500"))
                .when(emailTransport)
                .sendEmail(EmailType.CALLBACK_ADMIN, ADMIN_EMAIL, "subject", "body");

        int processedCount = service.processDueNotifications();

        assertEquals(1, processedCount);
        assertEquals(1, entry.getAttempts());
        assertEquals(AdminNotificationOutboxStatus.PENDING, entry.getStatus());
        assertEquals(NOW.plus(Duration.ofMinutes(5)), entry.getNextAttemptAt());
        assertEquals("Resend temporary failure with status 500", entry.getLastError());
        verify(adminNotificationOutboxRepository, never()).delete(entry);
        assertEquals(1.0, outboxEntriesCounter(EmailMetricsRecorder.OUTCOME_FAILED));
        assertEquals(1.0, emailDeliveryCounter(
                EmailType.CALLBACK_ADMIN,
                EmailMetricsRecorder.OUTCOME_FAILED_RETRYABLE
        ));
    }

    @Test
    void processDueNotifications_ShouldMarkFailed_WhenSendFailsWithNonRetryableError() {
        AdminNotificationOutbox entry = outboxEntry(1L, AdminNotificationType.ORDER_CREATED, 100L, 0);
        when(adminNotificationOutboxRepository.findDueForUpdate(
                eq(AdminNotificationOutboxStatus.PENDING),
                eq(NOW),
                any(Pageable.class)
        )).thenReturn(List.of(entry));
        doThrow(new NonRetryableEmailException("Resend permanent failure with status 400"))
                .when(emailTransport)
                .sendEmail(EmailType.ORDER_ADMIN, ADMIN_EMAIL, "subject", "body");

        service.processDueNotifications();

        assertEquals(1, entry.getAttempts());
        assertEquals(AdminNotificationOutboxStatus.FAILED, entry.getStatus());
        verify(adminNotificationOutboxRepository, never()).delete(entry);
        assertEquals(1.0, emailDeliveryCounter(
                EmailType.ORDER_ADMIN,
                EmailMetricsRecorder.OUTCOME_FAILED_NON_RETRYABLE
        ));
    }

    @Test
    void processDueNotifications_ShouldMarkFailed_WhenMaxAttemptsReached() {
        AdminNotificationOutbox entry = outboxEntry(1L, AdminNotificationType.ORDER_CREATED, 100L, 9);
        when(adminNotificationOutboxRepository.findDueForUpdate(
                eq(AdminNotificationOutboxStatus.PENDING),
                eq(NOW),
                any(Pageable.class)
        )).thenReturn(List.of(entry));
        doThrow(new RetryableEmailException("Resend temporary failure with status 503"))
                .when(emailTransport)
                .sendEmail(EmailType.ORDER_ADMIN, ADMIN_EMAIL, "subject", "body");

        service.processDueNotifications();

        assertEquals(10, entry.getAttempts());
        assertEquals(AdminNotificationOutboxStatus.FAILED, entry.getStatus());
        verify(adminNotificationOutboxRepository, never()).delete(entry);
    }

    private static AdminNotificationOutbox outboxEntry(
            Long id,
            AdminNotificationType notificationType,
            Long sourceId,
            int attempts
    ) {
        AdminNotificationOutbox entry = new AdminNotificationOutbox();
        entry.setId(id);
        entry.setNotificationType(notificationType);
        entry.setSourceId(sourceId);
        entry.setRecipient(ADMIN_EMAIL);
        entry.setSubject("subject");
        entry.setBody("body");
        entry.setStatus(AdminNotificationOutboxStatus.PENDING);
        entry.setAttempts(attempts);
        entry.setNextAttemptAt(NOW);
        return entry;
    }

    private static OrderCreatedEvent sampleOrderEvent() {
        return OrderCreatedEvent.builder()
                .orderId(100L)
                .orderNumber("ORD-123")
                .userId(1L)
                .userEmail("customer@example.com")
                .customerName("Customer")
                .deliveryCity("Kyiv")
                .phone("+380501234567")
                .notes("Call before delivery")
                .totalPrice(900L)
                .createdAt(NOW)
                .items(List.of())
                .build();
    }

    private static CallbackRequestCreatedEvent sampleCallbackEvent() {
        return CallbackRequestCreatedEvent.builder()
                .callbackRequestId(10L)
                .userId(1L)
                .userEmail("customer@example.com")
                .name("Customer")
                .phone("+380501234567")
                .message("Please call me")
                .createdAt(NOW)
                .build();
    }

    private double outboxEntriesCounter(String outcome) {
        return meterRegistry.get("email.admin.notification.outbox.entries")
                .tag("outcome", outcome)
                .counter()
                .count();
    }

    private double emailDeliveryCounter(EmailType emailType, String outcome) {
        return meterRegistry.get("email.delivery")
                .tag("email_type", emailType.getMetricValue())
                .tag("outcome", outcome)
                .counter()
                .count();
    }
}
