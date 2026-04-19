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

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesByEmail(String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from User u
            where u.userId in :userIds
            """)
    int deleteAllByIdIn(List<Long> userIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            delete from users_roles
            where user_id in (:userIds)
            """,
            nativeQuery = true)
    int deleteAllUserRolesByUserIdIn(List<Long> userIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u
            from User u
            where u.email = :email
            """)
    Optional<User> findByEmailForUpdate(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u
            from User u
            where u.userId = :userId
            """)
    Optional<User> findByIdForUpdate(Long userId);

    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u
            from User u
            where u.isEmailVerified = false
              and u.createdAt < :threshold
            """)
    List<User> findUnverifiedUsersForCleanupForUpdate(Instant threshold);
}
