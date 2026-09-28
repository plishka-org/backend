package org.plishka.backend.repository.user;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.plishka.backend.domain.user.EmailChangeToken;
import org.plishka.backend.domain.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<EmailChangeToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ect
            from EmailChangeToken ect
            where ect.tokenHash = :tokenHash
            """)
    Optional<EmailChangeToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    void deleteAllByUser(User user);

    int deleteAllByExpiresAtBefore(Instant expiresAtBefore);
}
