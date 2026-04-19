package org.plishka.backend.service.scheduler;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.config.BackendProperties;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.repository.user.EmailVerificationTokenRepository;
import org.plishka.backend.repository.user.PasswordResetTokenRepository;
import org.plishka.backend.repository.user.RefreshTokenRepository;
import org.plishka.backend.repository.user.UserRepository;
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
    private final Clock clock;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *", zone = EUROPE_KYIV)
    public void cleanupExpiredRefreshTokens() {
        int deletedCount = refreshTokenRepository.deleteAllByExpiresAtBefore(Instant.now(clock));

        if (deletedCount > 0) {
            log.info("RefreshToken cleanup: deleted {}", deletedCount);
        }
    }

    @Transactional
    @Scheduled(cron = "0 5 3 * * *", zone = EUROPE_KYIV)
    public void cleanupExpiredEmailVerificationTokens() {
        int deletedCount = emailVerificationTokenRepository.deleteAllByExpiresAtBefore(Instant.now(clock));

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

        int deletedVerificationTokens = emailVerificationTokenRepository.deleteAllByUserIdIn(userIds);
        int deletedUserRoles = userRepository.deleteAllUserRolesByUserIdIn(userIds);
        int deletedUsers = userRepository.deleteAllByIdIn(userIds);

        log.info(
                "Unverified users cleanup finished: deletedUsers={}, deletedVerificationTokens={}, deletedUserRoles={}",
                deletedUsers,
                deletedVerificationTokens,
                deletedUserRoles
        );
    }

    @Transactional
    @Scheduled(cron = "0 15 3 * * *", zone = EUROPE_KYIV)
    public void cleanupExpiredPasswordResetTokens() {
        int deletedCount = passwordResetTokenRepository.deleteAllByExpiresAtBefore(Instant.now(clock));

        if (deletedCount > 0) {
            log.info("PasswordResetToken cleanup: deleted {}", deletedCount);
        }
    }
}
