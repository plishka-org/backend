package org.plishka.backend.service.scheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.config.properties.StorageProperties;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.exception.StorageOperationException;
import org.plishka.backend.monitoring.metrics.SchedulerMetricsRecorder;
import org.plishka.backend.monitoring.metrics.StorageMetricsRecorder;
import org.plishka.backend.monitoring.sentry.SentryMonitoringService;
import org.plishka.backend.repository.user.EmailChangeTokenRepository;
import org.plishka.backend.repository.user.EmailVerificationTokenRepository;
import org.plishka.backend.repository.user.PasswordResetTokenRepository;
import org.plishka.backend.repository.user.RefreshTokenRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.file.MediaReferenceService;
import org.plishka.backend.service.notification.email.AdminNotificationOutboxService;
import org.plishka.backend.service.storage.ObjectStorageService;
import org.plishka.backend.service.storage.StorageDeletionOutboxService;
import org.plishka.backend.service.storage.tagging.RetryableMediaStorageTagger;
import org.springframework.util.unit.DataSize;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-16T12:00:00Z");
    private static final Duration UNVERIFIED_USER_TTL = Duration.ofHours(24);
    private static final Duration ORPHAN_UPLOAD_TTL = Duration.ofHours(48);

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailChangeTokenRepository emailChangeTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private StorageDeletionOutboxService storageDeletionOutboxService;

    @Mock
    private AdminNotificationOutboxService adminNotificationOutboxService;

    @Mock
    private MediaReferenceService mediaReferenceService;

    @Mock
    private RetryableMediaStorageTagger retryableMediaStorageTagger;

    @Mock
    private SchedulerMetricsRecorder schedulerMetricsRecorder;

    @Mock
    private StorageMetricsRecorder storageMetricsRecorder;

    @Mock
    private SentryMonitoringService sentryMonitoringService;

    private SchedulerService schedulerService;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(1).run();
            return null;
        }).when(schedulerMetricsRecorder).recordJob(anyString(), any(Runnable.class));

        schedulerService = schedulerService(storageProperties(true));
    }

    @Test
    void cleanupExpiredRefreshTokens_ShouldDeleteTokensExpiredBeforeNow() {
        schedulerService.cleanupExpiredRefreshTokens();

        verify(refreshTokenRepository).deleteAllByExpiresAtBefore(NOW);
        verify(schedulerMetricsRecorder).recordJob(eq("cleanup_expired_refresh_tokens"), any(Runnable.class));
    }

    @Test
    void cleanupUnverifiedUsers_ShouldDeleteUsersOlderThanConfiguredTtl() {
        User firstUser = user(1L);
        User secondUser = user(2L);
        Instant threshold = NOW.minus(UNVERIFIED_USER_TTL);
        when(userRepository.findUnverifiedUsersForCleanupForUpdate(threshold))
                .thenReturn(List.of(firstUser, secondUser));

        schedulerService.cleanupUnverifiedUsers();

        verify(userRepository).findUnverifiedUsersForCleanupForUpdate(threshold);
        verify(userRepository).deleteAllByIdIn(List.of(1L, 2L));
    }

    @Test
    void cleanupExpiredEmailAndPasswordTokens_ShouldDeleteTokensExpiredBeforeNow() {
        schedulerService.cleanupExpiredEmailVerificationTokens();
        schedulerService.cleanupExpiredPasswordResetTokens();
        schedulerService.cleanupExpiredEmailChangeTokens();

        verify(emailVerificationTokenRepository).deleteAllByExpiresAtBefore(NOW);
        verify(passwordResetTokenRepository).deleteAllByExpiresAtBefore(NOW);
        verify(emailChangeTokenRepository).deleteAllByExpiresAtBefore(NOW);
    }

    @Test
    void cleanupOrphanS3Uploads_ShouldDoNothing_WhenStorageCleanupIsDisabled() {
        schedulerService = schedulerService(storageProperties(false));

        schedulerService.cleanupOrphanS3Uploads();

        verifyNoInteractions(objectStorageService);
        verifyNoInteractions(storageMetricsRecorder);
    }

    @Test
    void cleanupOrphanS3Uploads_ShouldDeleteOnlyPendingUploadsNotAttachedInDatabase() {
        Instant threshold = NOW.minus(ORPHAN_UPLOAD_TTL);
        when(objectStorageService.findPendingUploadKeysOlderThan(threshold))
                .thenReturn(List.of("attached.jpg", "orphan.jpg"));
        when(mediaReferenceService.isAttached("attached.jpg")).thenReturn(true);
        when(mediaReferenceService.isAttached("orphan.jpg")).thenReturn(false);

        schedulerService.cleanupOrphanS3Uploads();

        verify(objectStorageService).deleteObjects(List.of("orphan.jpg"));
        verify(storageMetricsRecorder).recordOrphanCleanup(2, 1, 1);
    }

    @Test
    void reconcileAttachedS3Tags_ShouldRepairMissingTagsAndCaptureFailures() {
        StorageOperationException failure = new StorageOperationException("S3 unavailable");
        when(mediaReferenceService.findAllAttachedS3Keys()).thenReturn(List.of("ok.jpg", "missing.jpg", "failed.jpg"));
        when(objectStorageService.isObjectMarkedAsAttached("ok.jpg")).thenReturn(true);
        when(objectStorageService.isObjectMarkedAsAttached("missing.jpg")).thenReturn(false);
        when(objectStorageService.isObjectMarkedAsAttached("failed.jpg")).thenReturn(false);
        doAnswer(invocation -> {
            if ("failed.jpg".equals(invocation.getArgument(0))) {
                throw failure;
            }
            return null;
        }).when(retryableMediaStorageTagger).markObjectAsAttached(anyString());

        schedulerService.reconcileAttachedS3Tags();

        verify(retryableMediaStorageTagger).markObjectAsAttached("missing.jpg");
        verify(retryableMediaStorageTagger).markObjectAsAttached("failed.jpg");
        verify(sentryMonitoringService).captureException(failure, "storage", "s3_tag_reconciliation");
        verify(storageMetricsRecorder).recordTagReconciliation(3, 1, 1);
    }

    @Test
    void processStorageDeletionOutbox_ShouldDelegateToOutboxService() {
        schedulerService.processStorageDeletionOutbox();

        verify(storageDeletionOutboxService).processDueDeletions();
    }

    @Test
    void processAdminNotificationOutbox_ShouldDelegateToOutboxService() {
        schedulerService.processAdminNotificationOutbox();

        verify(adminNotificationOutboxService).processDueNotifications();
    }

    private SchedulerService schedulerService(StorageProperties storageProperties) {
        return new SchedulerService(
                refreshTokenRepository,
                emailChangeTokenRepository,
                emailVerificationTokenRepository,
                passwordResetTokenRepository,
                userRepository,
                backendProperties(),
                storageProperties,
                objectStorageService,
                storageDeletionOutboxService,
                adminNotificationOutboxService,
                mediaReferenceService,
                retryableMediaStorageTagger,
                Clock.fixed(NOW, ZoneOffset.UTC),
                schedulerMetricsRecorder,
                storageMetricsRecorder,
                sentryMonitoringService
        );
    }

    private static BackendProperties backendProperties() {
        return new BackendProperties(
                "http://localhost:8080/api",
                Duration.ofDays(30),
                new BackendProperties.Auth(Duration.ofHours(24), Duration.ofHours(3)),
                new BackendProperties.Cleanup(UNVERIFIED_USER_TTL)
        );
    }

    private static StorageProperties storageProperties(boolean cleanupEnabled) {
        return new StorageProperties(
                new StorageProperties.S3(
                        "test-bucket",
                        "eu-central-1",
                        "access-key",
                        "secret-key",
                        null,
                        Duration.ofMinutes(15),
                        Duration.ofHours(1)
                ),
                new StorageProperties.MediaConstraints(DataSize.ofMegabytes(10)),
                new StorageProperties.MediaConstraints(DataSize.ofMegabytes(200)),
                new StorageProperties.Cleanup(cleanupEnabled, ORPHAN_UPLOAD_TTL)
        );
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
