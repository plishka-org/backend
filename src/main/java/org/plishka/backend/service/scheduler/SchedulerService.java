package org.plishka.backend.service.scheduler;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.config.properties.StorageProperties;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.monitoring.metrics.SchedulerMetricsRecorder;
import org.plishka.backend.monitoring.metrics.StorageMetricsRecorder;
import org.plishka.backend.monitoring.sentry.SentryMonitoringService;
import org.plishka.backend.repository.user.EmailChangeTokenRepository;
import org.plishka.backend.repository.user.EmailVerificationTokenRepository;
import org.plishka.backend.repository.user.PasswordResetTokenRepository;
import org.plishka.backend.repository.user.RefreshTokenRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.file.MediaReferenceService;
import org.plishka.backend.service.storage.ObjectStorageService;
import org.plishka.backend.service.storage.StorageDeletionOutboxService;
import org.plishka.backend.service.storage.tagging.RetryableMediaStorageTagger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {
    private static final String EUROPE_KYIV = "Europe/Kyiv";

    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailChangeTokenRepository emailChangeTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final BackendProperties backendProperties;
    private final StorageProperties storageProperties;
    private final ObjectStorageService objectStorageService;
    private final StorageDeletionOutboxService storageDeletionOutboxService;
    private final MediaReferenceService mediaReferenceService;
    private final RetryableMediaStorageTagger retryableMediaStorageTagger;
    private final Clock clock;
    private final SchedulerMetricsRecorder schedulerMetricsRecorder;
    private final StorageMetricsRecorder storageMetricsRecorder;
    private final SentryMonitoringService sentryMonitoringService;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *", zone = EUROPE_KYIV)
    public void cleanupExpiredRefreshTokens() {
        schedulerMetricsRecorder.recordJob("cleanup_expired_refresh_tokens", () -> {
            int deletedCount = refreshTokenRepository.deleteAllByExpiresAtBefore(now());

            if (deletedCount > 0) {
                log.info("RefreshToken cleanup: deleted {}", deletedCount);
            }
        });
    }

    @Transactional
    @Scheduled(cron = "0 5 3 * * *", zone = EUROPE_KYIV)
    public void cleanupExpiredEmailVerificationTokens() {
        schedulerMetricsRecorder.recordJob("cleanup_expired_email_verification_tokens", () -> {
            int deletedCount = emailVerificationTokenRepository.deleteAllByExpiresAtBefore(now());

            if (deletedCount > 0) {
                log.info("EmailVerificationToken cleanup: deleted {}", deletedCount);
            }
        });
    }

    @Transactional
    @Scheduled(cron = "0 10 3 * * *", zone = EUROPE_KYIV)
    public void cleanupUnverifiedUsers() {
        schedulerMetricsRecorder.recordJob("cleanup_unverified_users", () -> {
            Instant threshold = Instant.now(clock).minus(backendProperties.cleanup().unverifiedUserTtl());
            List<User> usersToDelete = userRepository.findUnverifiedUsersForCleanupForUpdate(threshold);

            if (usersToDelete.isEmpty()) {
                return;
            }

            List<Long> userIds = usersToDelete.stream()
                    .map(User::getId)
                    .toList();

            int deletedUsers = userRepository.deleteAllByIdIn(userIds);

            log.info(
                    "Unverified users cleanup finished: deletedUsers={} (dependent rows removed by FK cascades)",
                    deletedUsers
            );
        });
    }

    @Transactional
    @Scheduled(cron = "0 15 3 * * *", zone = EUROPE_KYIV)
    public void cleanupExpiredPasswordResetTokens() {
        schedulerMetricsRecorder.recordJob("cleanup_expired_password_reset_tokens", () -> {
            int deletedCount = passwordResetTokenRepository.deleteAllByExpiresAtBefore(now());

            if (deletedCount > 0) {
                log.info("PasswordResetToken cleanup: deleted {}", deletedCount);
            }
        });
    }

    @Transactional
    @Scheduled(cron = "0 20 3 * * *", zone = EUROPE_KYIV)
    public void cleanupExpiredEmailChangeTokens() {
        schedulerMetricsRecorder.recordJob("cleanup_expired_email_change_tokens", () -> {
            int deletedCount = emailChangeTokenRepository.deleteAllByExpiresAtBefore(now());

            if (deletedCount > 0) {
                log.info("EmailChangeToken cleanup: deleted {}", deletedCount);
            }
        });
    }

    @Scheduled(cron = "0 0 4 * * SUN", zone = EUROPE_KYIV)
    public void cleanupOrphanS3Uploads() {
        schedulerMetricsRecorder.recordJob("cleanup_orphan_s3_uploads", () -> {
            if (!storageProperties.cleanup().enabled()) {
                return;
            }

            Instant threshold = now().minus(storageProperties.cleanup().orphanUploadTtl());
            List<String> pendingUploadKeys = objectStorageService.findPendingUploadKeysOlderThan(threshold);

            if (pendingUploadKeys.isEmpty()) {
                return;
            }

            List<String> deletablePendingUploadKeys = pendingUploadKeys.stream()
                    .filter(s3Key -> !mediaReferenceService.isAttached(s3Key))
                    .toList();

            int skippedAttachedCount = pendingUploadKeys.size() - deletablePendingUploadKeys.size();
            if (deletablePendingUploadKeys.isEmpty()) {
                storageMetricsRecorder.recordOrphanCleanup(pendingUploadKeys.size(), 0, skippedAttachedCount);
                return;
            }

            objectStorageService.deleteObjects(deletablePendingUploadKeys);
            storageMetricsRecorder.recordOrphanCleanup(
                    pendingUploadKeys.size(),
                    deletablePendingUploadKeys.size(),
                    skippedAttachedCount
            );
            logSkippedAttachedPendingUploads(skippedAttachedCount);
            log.info("Orphan upload cleanup: deleted {}", deletablePendingUploadKeys.size());
        });
    }

    @Scheduled(cron = "0 5 4 * * SUN", zone = EUROPE_KYIV)
    public void reconcileAttachedS3Tags() {
        schedulerMetricsRecorder.recordJob("reconcile_attached_s3_tags", () -> {
            List<String> attachedS3Keys = mediaReferenceService.findAllAttachedS3Keys();
            if (attachedS3Keys.isEmpty()) {
                return;
            }

            int repairedCount = 0;
            int failedCount = 0;

            for (String s3Key : attachedS3Keys) {
                try {
                    if (objectStorageService.isObjectMarkedAsAttached(s3Key)) {
                        continue;
                    }

                    retryableMediaStorageTagger.markObjectAsAttached(s3Key);
                    repairedCount++;
                } catch (RuntimeException exception) {
                    failedCount++;
                    sentryMonitoringService.captureException(exception, "storage", "s3_tag_reconciliation");
                    log.warn("Attached media tag reconciliation failed", exception);
                }
            }

            storageMetricsRecorder.recordTagReconciliation(attachedS3Keys.size(), repairedCount, failedCount);

            if (repairedCount > 0 || failedCount > 0) {
                log.info(
                        "Attached media tag reconciliation finished: checked={}, repaired={}, failed={}",
                        attachedS3Keys.size(),
                        repairedCount,
                        failedCount
                );
            }
        });
    }

    @Scheduled(cron = "0 0 * * * *", zone = EUROPE_KYIV)
    public void processStorageDeletionOutbox() {
        schedulerMetricsRecorder.recordJob(
                "process_storage_deletion_outbox",
                storageDeletionOutboxService::processDueDeletions
        );
    }

    private void logSkippedAttachedPendingUploads(int skippedAttachedPendingUploadCount) {
        if (skippedAttachedPendingUploadCount > 0) {
            log.warn(
                    "Orphan upload cleanup: skipped {} pending objects already attached in DB",
                    skippedAttachedPendingUploadCount
            );
        }
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
