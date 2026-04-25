package org.plishka.backend.service.scheduler;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.config.properties.BackendProperties;
import org.plishka.backend.config.properties.StorageProperties;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.repository.user.EmailVerificationTokenRepository;
import org.plishka.backend.repository.user.PasswordResetTokenRepository;
import org.plishka.backend.repository.user.RefreshTokenRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.storage.ObjectStorageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {
    private static final String EUROPE_KYIV = "Europe/Kyiv";

    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final BackendProperties backendProperties;
    private final StorageProperties storageProperties;
    private final ObjectStorageService objectStorageService;
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
                .map(User::getUserId)
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

    @Scheduled(cron = "0 0 4 * * SUN", zone = EUROPE_KYIV)
    public void cleanupOrphanS3Uploads() {
        if (!storageProperties.cleanup().enabled()) {
            return;
        }

        Instant threshold = now().minus(storageProperties.cleanup().orphanUploadTtl());
        List<String> orphanUploadKeys = objectStorageService.findPendingUploadKeysOlderThan(threshold);

        if (orphanUploadKeys.isEmpty()) {
            return;
        }

        objectStorageService.deleteObjects(orphanUploadKeys);
        log.info("Orphan upload cleanup: deleted {}", orphanUploadKeys.size());
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
