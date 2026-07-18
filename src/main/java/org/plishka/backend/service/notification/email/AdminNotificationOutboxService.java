package org.plishka.backend.service.notification.email;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.domain.notification.AdminNotificationOutbox;
import org.plishka.backend.domain.notification.AdminNotificationOutboxStatus;
import org.plishka.backend.domain.notification.AdminNotificationType;
import org.plishka.backend.event.callback.CallbackRequestCreatedEvent;
import org.plishka.backend.event.order.OrderCreatedEvent;
import org.plishka.backend.exception.InvalidEmailRecipientException;
import org.plishka.backend.exception.NonRetryableEmailException;
import org.plishka.backend.exception.RetryableEmailException;
import org.plishka.backend.monitoring.metrics.EmailMetricsRecorder;
import org.plishka.backend.monitoring.sentry.SentryMonitoringService;
import org.plishka.backend.repository.notification.AdminNotificationOutboxRepository;
import org.plishka.backend.service.notification.email.transport.EmailTransport;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationOutboxService {
    private static final int BATCH_SIZE = 100;
    private static final int MAX_ATTEMPTS = 10;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;
    private static final Duration BASE_RETRY_DELAY = Duration.ofMinutes(5);
    private static final Duration MAX_RETRY_DELAY = Duration.ofHours(2);

    private final AdminNotificationOutboxRepository adminNotificationOutboxRepository;
    private final EmailTransport emailTransport;
    private final EmailTemplateBuilder templateBuilder;
    private final BackendProperties backendProperties;
    private final TransactionOperations transactionOperations;
    private final Clock clock;
    private final EmailMetricsRecorder emailMetricsRecorder;
    private final SentryMonitoringService sentryMonitoringService;

    @Transactional
    public void enqueueOrderCreated(OrderCreatedEvent event) {
        adminNotificationOutboxRepository.save(createOrderCreatedEntry(event, now()));
    }

    @Transactional
    public void enqueueCallbackCreated(CallbackRequestCreatedEvent event) {
        adminNotificationOutboxRepository.save(createCallbackCreatedEntry(event, now()));
    }

    public int processDueNotifications() {
        Instant now = now();
        BatchResult totalResult = BatchResult.empty();

        while (true) {
            BatchResult batchResult = transactionOperations.execute(status -> processDueNotificationBatch(now));
            totalResult = totalResult.plus(batchResult);
            if (batchResult.checkedCount() < BATCH_SIZE) {
                break;
            }
        }

        if (totalResult.checkedCount() > 0) {
            log.info(
                    "Admin notification outbox processed: checked={}, sent={}, failed={}",
                    totalResult.checkedCount(),
                    totalResult.sentCount(),
                    totalResult.failedCount()
            );
        }
        emailMetricsRecorder.recordAdminNotificationOutboxEntries(
                totalResult.checkedCount(),
                totalResult.sentCount(),
                totalResult.failedCount()
        );

        return totalResult.checkedCount();
    }

    private BatchResult processDueNotificationBatch(Instant now) {
        var entries = adminNotificationOutboxRepository.findDueForUpdate(
                AdminNotificationOutboxStatus.PENDING,
                now,
                PageRequest.of(0, BATCH_SIZE)
        );
        if (entries.isEmpty()) {
            return BatchResult.empty();
        }

        int sentCount = 0;
        int failedCount = 0;
        for (AdminNotificationOutbox entry : entries) {
            if (trySend(entry, now)) {
                sentCount++;
            } else {
                failedCount++;
            }
        }

        return new BatchResult(entries.size(), sentCount, failedCount);
    }

    private boolean trySend(AdminNotificationOutbox entry, Instant now) {
        EmailType emailType = resolveEmailType(entry.getNotificationType());
        try {
            emailTransport.sendEmail(emailType, entry.getRecipient(), entry.getSubject(), entry.getBody());
            emailMetricsRecorder.recordSent(emailType);
            adminNotificationOutboxRepository.delete(entry);
            return true;
        } catch (RetryableEmailException exception) {
            emailMetricsRecorder.recordRetryableFailure(emailType);
            recordFailure(entry, now, exception, false);
            return false;
        } catch (NonRetryableEmailException | InvalidEmailRecipientException exception) {
            emailMetricsRecorder.recordNonRetryableFailure(emailType);
            recordFailure(entry, now, exception, true);
            return false;
        } catch (RuntimeException exception) {
            emailMetricsRecorder.recordRetryableFailure(emailType);
            recordFailure(entry, now, exception, false);
            return false;
        }
    }

    private void recordFailure(
            AdminNotificationOutbox entry,
            Instant now,
            RuntimeException exception,
            boolean nonRetryable
    ) {
        int attempts = entry.getAttempts() + 1;
        entry.setAttempts(attempts);
        entry.setLastError(resolveErrorMessage(exception));
        entry.setNextAttemptAt(now.plus(resolveRetryDelay(attempts)));

        boolean terminalFailure = nonRetryable || attempts >= MAX_ATTEMPTS;
        if (terminalFailure) {
            entry.setStatus(AdminNotificationOutboxStatus.FAILED);
            sentryMonitoringService.captureException(exception, "email", "admin_notification_outbox_terminal_failure");
            log.warn(
                    "Admin notification outbox terminal failure: id={}, type={}, sourceId={}, attempts={}, error={}",
                    entry.getId(),
                    entry.getNotificationType(),
                    entry.getSourceId(),
                    attempts,
                    entry.getLastError(),
                    exception
            );
            return;
        }

        log.warn(
                "Admin notification outbox failed: id={}, type={}, sourceId={}, attempts={}, error={}",
                entry.getId(),
                entry.getNotificationType(),
                entry.getSourceId(),
                attempts,
                entry.getLastError()
        );
    }

    private String resolveErrorMessage(RuntimeException exception) {
        String message = StringUtils.hasText(exception.getMessage())
                ? exception.getMessage()
                : exception.getClass().getSimpleName();

        return message.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private Duration resolveRetryDelay(int attempts) {
        Duration retryDelay = BASE_RETRY_DELAY;
        for (int retry = 1; retry < attempts; retry++) {
            retryDelay = retryDelay.multipliedBy(2);
            if (retryDelay.compareTo(MAX_RETRY_DELAY) >= 0) {
                return MAX_RETRY_DELAY;
            }
        }
        return retryDelay;
    }

    private AdminNotificationOutbox createOrderCreatedEntry(OrderCreatedEvent event, Instant now) {
        return createEntry(
                AdminNotificationType.ORDER_CREATED,
                event.orderId(),
                EmailSubjects.orderNotificationAdmin(event.orderNumber()),
                templateBuilder.buildOrderAdminEmailText(event),
                now
        );
    }

    private AdminNotificationOutbox createCallbackCreatedEntry(CallbackRequestCreatedEvent event, Instant now) {
        return createEntry(
                AdminNotificationType.CALLBACK_CREATED,
                event.callbackRequestId(),
                EmailSubjects.CALLBACK_NOTIFICATION_ADMIN,
                templateBuilder.buildCallbackNotificationAdminText(event),
                now
        );
    }

    private AdminNotificationOutbox createEntry(
            AdminNotificationType notificationType,
            Long sourceId,
            String subject,
            String body,
            Instant now
    ) {
        AdminNotificationOutbox entry = new AdminNotificationOutbox();
        entry.setNotificationType(notificationType);
        entry.setSourceId(sourceId);
        entry.setRecipient(backendProperties.admin().email());
        entry.setSubject(subject);
        entry.setBody(body);
        entry.setStatus(AdminNotificationOutboxStatus.PENDING);
        entry.setAttempts(0);
        entry.setNextAttemptAt(now);
        return entry;
    }

    private EmailType resolveEmailType(AdminNotificationType notificationType) {
        return switch (notificationType) {
            case ORDER_CREATED -> EmailType.ORDER_ADMIN;
            case CALLBACK_CREATED -> EmailType.CALLBACK_ADMIN;
        };
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private record BatchResult(int checkedCount, int sentCount, int failedCount) {
        private static BatchResult empty() {
            return new BatchResult(0, 0, 0);
        }

        private BatchResult plus(BatchResult other) {
            return new BatchResult(
                    checkedCount + other.checkedCount,
                    sentCount + other.sentCount,
                    failedCount + other.failedCount
            );
        }
    }
}
