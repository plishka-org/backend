package org.plishka.backend.repository.notification;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.plishka.backend.domain.notification.AdminNotificationOutbox;
import org.plishka.backend.domain.notification.AdminNotificationOutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminNotificationOutboxRepository extends JpaRepository<AdminNotificationOutbox, Long> {
    long countByStatus(AdminNotificationOutboxStatus status);

    long countByStatusAndNextAttemptAtLessThanEqual(AdminNotificationOutboxStatus status, Instant now);

    @Query("""
            select min(o.createdAt)
            from AdminNotificationOutbox o
            where o.status = :status
            """)
    Optional<Instant> findOldestCreatedAtByStatus(@Param("status") AdminNotificationOutboxStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o
            from AdminNotificationOutbox o
            where o.status = :status
              and o.nextAttemptAt <= :now
            order by o.nextAttemptAt, o.id
            """)
    List<AdminNotificationOutbox> findDueForUpdate(
            @Param("status") AdminNotificationOutboxStatus status,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            update AdminNotificationOutbox o
            set o.recipient = :newRecipient
            where o.status = :status
            """)
    void updateRecipientsByStatus(
            @Param("newRecipient") String newRecipient,
            @Param("status") AdminNotificationOutboxStatus status
    );
}
