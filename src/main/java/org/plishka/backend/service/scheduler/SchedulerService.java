package org.plishka.backend.service.scheduler;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.config.properties.StorageProperties;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.repository.user.EmailChangeTokenRepository;
import org.plishka.backend.repository.user.EmailVerificationTokenRepository;
import org.plishka.backend.repository.user.PasswordResetTokenRepository;
import org.plishka.backend.repository.user.RefreshTokenRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.file.MediaReferenceService;
import org.plishka.backend.service.storage.ObjectStorageService;
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
    private final MediaReferenceService mediaReferenceService;
    private final RetryableMediaStorageTagger retryableMediaStorageTagger;
    private final Clock clock;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *", zone = EUROPE_KYIV)
    public void cleanupExpiredRefreshTokens() {
        int deletedCount = refreshTokenRepository.deleteAllByExpiresAtBefore(now());

        if (deletedCount > 0) {
            log.info("RefreshToken cleanup: deleted {}", deletedCount);
        }
    }

    @Transactional
    @Scheduled(cron = "0 5 3 * * *", zone = EUROPE_KYIV)
    public void cleanupExpiredEmailVerificationTokens() {
        int deletedCount = emailVerificationTokenRepository.deleteAllByExpiresAtBefore(now());

        if (deletedCount > 0) {
            log.info("EmailVerificationToken cleanup: deleted {}", deletedCount);
        }
    }

    @Transactional
    @Scheduled(cron = "0 10 3 * * *", zone = EUROPE_KYIV)
    public void cleanupUnverifiedUsers() {
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
    }

    @Transactional
    @Scheduled(cron = "0 15 3 * * *", zone = EUROPE_KYIV)
    public void cleanupExpiredPasswordResetTokens() {
        int deletedCount = passwordResetTokenRepository.deleteAllByExpiresAtBefore(now());

        if (deletedCount > 0) {
            log.info("PasswordResetToken cleanup: deleted {}", deletedCount);
        }
    }

    @Transactional
    @Scheduled(cron = "0 20 3 * * *", zone = EUROPE_KYIV)
    public void cleanupExpiredEmailChangeTokens() {
        int deletedCount = emailChangeTokenRepository.deleteAllByExpiresAtBefore(now());

        if (deletedCount > 0) {
            log.info("EmailChangeToken cleanup: deleted {}", deletedCount);
        }
    }

    @Scheduled(cron = "0 0 4 * * SUN", zone = EUROPE_KYIV)
    public void cleanupOrphanS3Uploads() {
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

        if (deletablePendingUploadKeys.isEmpty()) {
            return;
        }

        objectStorageService.deleteObjects(deletablePendingUploadKeys);
        logSkippedAttachedPendingUploads(pendingUploadKeys, deletablePendingUploadKeys);
        log.info("Orphan upload cleanup: deleted {}", deletablePendingUploadKeys.size());
    }

    @Scheduled(cron = "0 5 4 * * SUN", zone = EUROPE_KYIV)
    public void reconcileAttachedS3Tags() {
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
                log.warn("Attached media tag reconciliation failed: s3Key={}", s3Key, exception);
            }
        }

        if (repairedCount > 0 || failedCount > 0) {
            log.info(
                    "Attached media tag reconciliation finished: checked={}, repaired={}, failed={}",
                    attachedS3Keys.size(),
                    repairedCount,
                    failedCount
            );
        }
    }

    private void logSkippedAttachedPendingUploads(
            List<String> pendingUploadKeys,
            List<String> deletedPendingUploadKeys
    ) {
        int skippedAttachedPendingUploadCount = pendingUploadKeys.size() - deletedPendingUploadKeys.size();
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
