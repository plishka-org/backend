package org.plishka.backend.repository.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.plishka.backend.domain.user.EmailVerificationToken;
import org.plishka.backend.domain.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    void deleteAllByUser(User user);

    int deleteAllByExpiresAtBefore(Instant expiresAtBefore);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from EmailVerificationToken evt
            where evt.user.userId in :userIds
            """)
    int deleteAllByUserIdIn(List<Long> userIds);
}
