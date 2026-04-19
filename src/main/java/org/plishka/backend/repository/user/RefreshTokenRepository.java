package org.plishka.backend.repository.user;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.plishka.backend.domain.user.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select rt
            from RefreshToken rt
            join fetch rt.user
            where rt.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    int deleteAllByExpiresAtBefore(Instant expiresAtBefore);

    @Modifying
    @Query("""
            delete from RefreshToken rt
            where rt.user.userId = :userId
            """)
    int deleteAllByUserId(Long userId);

    @Modifying
    @Query("""
            delete from RefreshToken rt
            where rt.user.userId = :userId
              and rt.deviceId = :deviceId
            """)
    int deleteAllByUserIdAndDeviceId(Long userId, String deviceId);
}
