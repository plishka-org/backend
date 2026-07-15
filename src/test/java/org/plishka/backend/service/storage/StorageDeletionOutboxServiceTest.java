package org.plishka.backend.service.storage;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.storage.StorageDeletionOutbox;
import org.plishka.backend.domain.storage.StorageDeletionOutboxStatus;
import org.plishka.backend.exception.StorageOperationException;
import org.plishka.backend.monitoring.metrics.StorageMetricsRecorder;
import org.plishka.backend.monitoring.sentry.SentryMonitoringService;
import org.plishka.backend.repository.storage.StorageDeletionOutboxRepository;
import org.plishka.backend.service.storage.validation.S3ObjectKeyValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageDeletionOutboxServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-23T12:00:00Z");
    private static final String VALID_IMAGE_KEY =
            "products/1/images/2026/06/123e4567-e89b-12d3-a456-426614174000.jpg";

    @Mock
    private StorageDeletionOutboxRepository storageDeletionOutboxRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private S3ObjectKeyValidator s3ObjectKeyValidator;

    private final TransactionOperations transactionOperations = new TransactionOperations() {
        @Override
        public <T> T execute(TransactionCallback<T> action) {
            return action.doInTransaction(null);
        }
    };

    private StorageDeletionOutboxService service;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new StorageDeletionOutboxService(
                storageDeletionOutboxRepository,
                objectStorageService,
                s3ObjectKeyValidator,
                transactionOperations,
                Clock.fixed(NOW, ZoneOffset.UTC),
                new StorageMetricsRecorder(meterRegistry),
                new SentryMonitoringService()
        );
    }

    @Test
    void enqueueDeletes_ShouldValidateNormalizeAndDeduplicateKeys() {
        String paddedKey = " " + VALID_IMAGE_KEY + " ";
        when(s3ObjectKeyValidator.validateAndNormalizeS3Key(paddedKey)).thenReturn(VALID_IMAGE_KEY);
        when(s3ObjectKeyValidator.validateAndNormalizeS3Key(VALID_IMAGE_KEY)).thenReturn(VALID_IMAGE_KEY);

        service.enqueueDeletes(Arrays.asList(paddedKey, "", null, VALID_IMAGE_KEY));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<StorageDeletionOutbox>> entriesCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(storageDeletionOutboxRepository).saveAll(entriesCaptor.capture());

        List<StorageDeletionOutbox> entries = StreamSupport.stream(entriesCaptor.getValue().spliterator(), false)
                .toList();
        assertEquals(1, entries.size());
        Assertions.assertAll(
                () -> assertEquals(VALID_IMAGE_KEY, entries.get(0).getS3Key()),
                () -> assertEquals(StorageDeletionOutboxStatus.PENDING, entries.get(0).getStatus()),
                () -> assertEquals(0, entries.get(0).getAttempts()),
                () -> assertEquals(NOW, entries.get(0).getNextAttemptAt())
        );
    }

    @Test
    void processDueDeletions_ShouldDeleteOutboxRows_WhenStorageDeleteSucceeds() {
        StorageDeletionOutbox first = outboxEntry(1L, "first.jpg", 0);
        StorageDeletionOutbox second = outboxEntry(2L, "second.jpg", 0);
        when(storageDeletionOutboxRepository.findDueForUpdate(
                eq(StorageDeletionOutboxStatus.PENDING),
                eq(NOW),
                any(Pageable.class)
        )).thenReturn(List.of(first, second));

        int processedCount = service.processDueDeletions();

        assertEquals(2, processedCount);
        verify(objectStorageService).deleteObject("first.jpg");
        verify(objectStorageService).deleteObject("second.jpg");
        verify(storageDeletionOutboxRepository).delete(first);
        verify(storageDeletionOutboxRepository).delete(second);
        assertEquals(2.0, outboxEntriesCounter("processed"));
        assertEquals(2.0, outboxEntriesCounter("deleted"));
    }

    @Test
    void processDueDeletions_ShouldProcessBatchesUntilNoDueRowsLeft() {
        List<StorageDeletionOutbox> firstBatch = outboxEntries(1, 100);
        List<StorageDeletionOutbox> secondBatch = outboxEntries(101, 100);
        when(storageDeletionOutboxRepository.findDueForUpdate(
                eq(StorageDeletionOutboxStatus.PENDING),
                eq(NOW),
                any(Pageable.class)
        )).thenReturn(firstBatch, secondBatch, List.of());

        int processedCount = service.processDueDeletions();

        assertEquals(200, processedCount);
        verify(storageDeletionOutboxRepository, times(3)).findDueForUpdate(
                eq(StorageDeletionOutboxStatus.PENDING),
                eq(NOW),
                any(Pageable.class)
        );
        verify(objectStorageService, times(200)).deleteObject(anyString());
    }

    @Test
    void processDueDeletions_ShouldScheduleRetry_WhenStorageDeleteFails() {
        StorageDeletionOutbox entry = outboxEntry(1L, "failed.jpg", 0);
        when(storageDeletionOutboxRepository.findDueForUpdate(
                eq(StorageDeletionOutboxStatus.PENDING),
                eq(NOW),
                any(Pageable.class)
        )).thenReturn(List.of(entry));
        doThrow(new StorageOperationException("S3 is down"))
                .when(objectStorageService)
                .deleteObject("failed.jpg");

        int processedCount = service.processDueDeletions();

        assertEquals(1, processedCount);
        assertEquals(1, entry.getAttempts());
        assertEquals(StorageDeletionOutboxStatus.PENDING, entry.getStatus());
        assertEquals(NOW.plus(Duration.ofHours(1)), entry.getNextAttemptAt());
        assertEquals("S3 is down", entry.getLastError());
        verify(storageDeletionOutboxRepository, never()).delete(entry);
        assertEquals(1.0, outboxEntriesCounter("processed"));
        assertEquals(1.0, outboxEntriesCounter("failed"));
    }

    @Test
    void processDueDeletions_ShouldCapRetryDelay_WhenBackoffExceedsMaxDelay() {
        StorageDeletionOutbox entry = outboxEntry(1L, "terminal.jpg", 7);
        when(storageDeletionOutboxRepository.findDueForUpdate(
                eq(StorageDeletionOutboxStatus.PENDING),
                eq(NOW),
                any(Pageable.class)
        )).thenReturn(List.of(entry));
        doThrow(new StorageOperationException("S3 is down"))
                .when(objectStorageService)
                .deleteObject("terminal.jpg");

        service.processDueDeletions();

        assertEquals(NOW.plus(Duration.ofHours(32)), entry.getNextAttemptAt());
    }

    @Test
    void processDueDeletions_ShouldMarkFailed_WhenMaxAttemptsReached() {
        StorageDeletionOutbox entry = outboxEntry(1L, "terminal.jpg", 9);
        when(storageDeletionOutboxRepository.findDueForUpdate(
                eq(StorageDeletionOutboxStatus.PENDING),
                eq(NOW),
                any(Pageable.class)
        )).thenReturn(List.of(entry));
        doThrow(new StorageOperationException("S3 is still down"))
                .when(objectStorageService)
                .deleteObject("terminal.jpg");

        service.processDueDeletions();

        assertEquals(10, entry.getAttempts());
        assertEquals(StorageDeletionOutboxStatus.FAILED, entry.getStatus());
        verify(storageDeletionOutboxRepository, never()).delete(entry);
    }

    private static StorageDeletionOutbox outboxEntry(Long id, String s3Key, int attempts) {
        StorageDeletionOutbox entry = new StorageDeletionOutbox();
        entry.setId(id);
        entry.setS3Key(s3Key);
        entry.setStatus(StorageDeletionOutboxStatus.PENDING);
        entry.setAttempts(attempts);
        entry.setNextAttemptAt(NOW);
        return entry;
    }

    private static List<StorageDeletionOutbox> outboxEntries(int firstId, int count) {
        return IntStream.range(firstId, firstId + count)
                .mapToObj(id -> outboxEntry((long) id, "object-" + id + ".jpg", 0))
                .toList();
    }

    private double outboxEntriesCounter(String outcome) {
        return meterRegistry.get("storage.deletion.outbox.entries")
                .tag("outcome", outcome)
                .counter()
                .count();
    }
}
