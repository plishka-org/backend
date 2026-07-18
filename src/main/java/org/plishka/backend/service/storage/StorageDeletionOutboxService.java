package org.plishka.backend.service.storage;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.storage.StorageDeletionOutbox;
import org.plishka.backend.domain.storage.StorageDeletionOutboxStatus;
import org.plishka.backend.monitoring.metrics.StorageMetricsRecorder;
import org.plishka.backend.monitoring.sentry.SentryMonitoringService;
import org.plishka.backend.repository.storage.StorageDeletionOutboxRepository;
import org.plishka.backend.service.storage.validation.S3ObjectKeyValidator;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageDeletionOutboxService {
    private static final int BATCH_SIZE = 100;
    private static final int MAX_ATTEMPTS = 10;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;
    private static final Duration BASE_RETRY_DELAY = Duration.ofMinutes(60);
    private static final Duration MAX_RETRY_DELAY = Duration.ofHours(32);

    private final StorageDeletionOutboxRepository storageDeletionOutboxRepository;
    private final ObjectStorageService objectStorageService;
    private final S3ObjectKeyValidator s3ObjectKeyValidator;
    private final TransactionOperations transactionOperations;
    private final Clock clock;
    private final StorageMetricsRecorder storageMetricsRecorder;
    private final SentryMonitoringService sentryMonitoringService;

    @Transactional
    public void enqueueDelete(String s3Key) {
        enqueueDeletesInternal(List.of(s3Key));
    }

    @Transactional
    public void enqueueDeletes(Collection<String> s3Keys) {
        enqueueDeletesInternal(s3Keys);
    }

    public int processDueDeletions() {
        Instant now = now();
        BatchResult totalResult = BatchResult.empty();

        while (true) {
            BatchResult batchResult = transactionOperations.execute(status -> processDueDeletionBatch(now));
            totalResult = totalResult.plus(batchResult);
            if (batchResult.checkedCount() < BATCH_SIZE) {
                break;
            }
        }

        if (totalResult.checkedCount() > 0) {
            log.info(
                    "Storage deletion outbox processed: checked={}, deleted={}, failed={}",
                    totalResult.checkedCount(),
                    totalResult.deletedCount(),
                    totalResult.failedCount()
            );
        }
        storageMetricsRecorder.recordDeletionOutboxEntries(
                totalResult.checkedCount(),
                totalResult.deletedCount(),
                totalResult.failedCount()
        );

        return totalResult.checkedCount();
    }

    private BatchResult processDueDeletionBatch(Instant now) {
        List<StorageDeletionOutbox> entries = findDueEntries(now);
        if (entries.isEmpty()) {
            return BatchResult.empty();
        }

        return processEntries(entries, now);
    }

    private List<StorageDeletionOutbox> findDueEntries(Instant now) {
        return storageDeletionOutboxRepository.findDueForUpdate(
                StorageDeletionOutboxStatus.PENDING,
                now,
                PageRequest.of(0, BATCH_SIZE)
        );
    }

    private BatchResult processEntries(List<StorageDeletionOutbox> entries, Instant now) {
        int deletedCount = 0;
        int failedCount = 0;
        for (StorageDeletionOutbox entry : entries) {
            if (tryDeleteObject(entry, now)) {
                deletedCount++;
            } else {
                failedCount++;
            }
        }

        return new BatchResult(entries.size(), deletedCount, failedCount);
    }

    private boolean tryDeleteObject(StorageDeletionOutbox entry, Instant now) {
        try {
            objectStorageService.deleteObject(entry.getS3Key());
            storageDeletionOutboxRepository.delete(entry);
            return true;
        } catch (RuntimeException exception) {
            recordFailure(entry, now, exception);
            return false;
        }
    }

    private void recordFailure(StorageDeletionOutbox entry, Instant now, RuntimeException exception) {
        int attempts = entry.getAttempts() + 1;
        entry.setAttempts(attempts);
        entry.setLastError(resolveErrorMessage(exception));
        entry.setNextAttemptAt(now.plus(resolveRetryDelay(attempts)));

        boolean terminalFailure = attempts >= MAX_ATTEMPTS;
        if (terminalFailure) {
            entry.setStatus(StorageDeletionOutboxStatus.FAILED);
            sentryMonitoringService.captureException(exception, "storage", "deletion_outbox_terminal_failure");
            log.warn(
                    "Storage deletion outbox terminal failure: id={}, attempts={}, error={}",
                    entry.getId(),
                    attempts,
                    entry.getLastError(),
                    exception
            );
            return;
        }

        log.warn(
                "Storage deletion outbox failed: id={}, attempts={}, error={}",
                entry.getId(),
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

    private void enqueueDeletesInternal(Collection<String> s3Keys) {
        List<String> normalizedKeys = normalizeKeys(s3Keys);
        if (normalizedKeys.isEmpty()) {
            return;
        }

        Instant now = now();
        List<StorageDeletionOutbox> entries = normalizedKeys.stream()
                .map(s3Key -> createEntry(s3Key, now))
                .toList();

        storageDeletionOutboxRepository.saveAll(entries);
    }

    private List<String> normalizeKeys(Collection<String> s3Keys) {
        if (s3Keys == null || s3Keys.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalizedKeys = new LinkedHashSet<>();
        s3Keys.stream()
                .filter(StringUtils::hasText)
                .map(s3ObjectKeyValidator::validateAndNormalizeS3Key)
                .forEach(normalizedKeys::add);
        return List.copyOf(normalizedKeys);
    }

    private StorageDeletionOutbox createEntry(String s3Key, Instant now) {
        StorageDeletionOutbox entry = new StorageDeletionOutbox();
        entry.setS3Key(s3Key);
        entry.setStatus(StorageDeletionOutboxStatus.PENDING);
        entry.setAttempts(0);
        entry.setNextAttemptAt(now);
        return entry;
    }

    private record BatchResult(int checkedCount, int deletedCount, int failedCount) {
        private static BatchResult empty() {
            return new BatchResult(0, 0, 0);
        }

        private BatchResult plus(BatchResult other) {
            return new BatchResult(
                    checkedCount + other.checkedCount,
                    deletedCount + other.deletedCount,
                    failedCount + other.failedCount
            );
        }
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
