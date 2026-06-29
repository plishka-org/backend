package org.plishka.backend.repository.user;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.plishka.backend.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from User u
            where u.id = :userId
            """)
    void deleteByIdDirect(@Param("userId") Long userId);

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "roles")
    @Query("""
            select u
            from User u
            where u.id = :userId
            """)
    Optional<User> findByIdWithRolesForUpdate(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "roles")
    @Query("""
            select u
            from User u
            where u.id in :userIds
            order by u.id
            """)
    List<User> findAllByIdInWithRolesForUpdateOrderById(@Param("userIds") Collection<Long> userIds);

    @Query(value = """
            select new org.plishka.backend.repository.user.AdminUserRow(
                u.id,
                u.name,
                u.email,
                u.phone,
                u.isBanned,
                count(o.id)
            )
            from User u
            left join Order o on o.user = u
            where (:bannedOnly = false or u.isBanned = true)
              and (:search is null
                   or lower(u.name) like :search
                   or lower(u.email) like :search
                   or lower(u.phone) like :search)
            group by u.id, u.name, u.email, u.phone, u.isBanned, u.createdAt
            """,
            countQuery = """
            select count(u)
            from User u
            where (:bannedOnly = false or u.isBanned = true)
              and (:search is null
                   or lower(u.name) like :search
                   or lower(u.email) like :search
                   or lower(u.phone) like :search)
            """)
    Page<AdminUserRow> findAdminUsers(
            @Param("search") String search,
            @Param("bannedOnly") boolean bannedOnly,
            Pageable pageable
    );

    @Query("""
            select new org.plishka.backend.repository.user.AdminUserRow(
                u.id,
                u.name,
                u.email,
                u.phone,
                u.isBanned,
                count(o.id)
            )
            from User u
            left join Order o on o.user = u
            where u.id = :userId
            group by u.id, u.name, u.email, u.phone, u.isBanned
            """)
    Optional<AdminUserRow> findAdminUserById(@Param("userId") Long userId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u
            from User u
            where u.isEmailVerified = false
              and u.createdAt < :threshold
            """)
    List<User> findUnverifiedUsersForCleanupForUpdate(@Param("threshold") Instant threshold);
}
