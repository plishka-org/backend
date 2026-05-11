package org.plishka.backend.repository.user;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.plishka.backend.domain.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesByEmail(String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from User u
            where u.id in :userIds
            """)
    int deleteAllByIdIn(@Param("userIds") List<Long> userIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u
            from User u
            where u.email = :email
            """)
    Optional<User> findByEmailForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u
            from User u
            where u.id = :userId
            """)
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u
            from User u
            where u.isEmailVerified = false
              and u.createdAt < :threshold
            """)
    List<User> findUnverifiedUsersForCleanupForUpdate(@Param("threshold") Instant threshold);
}
