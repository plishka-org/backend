package org.plishka.backend.repository.storage;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.plishka.backend.domain.storage.StorageDeletionOutbox;
import org.plishka.backend.domain.storage.StorageDeletionOutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StorageDeletionOutboxRepository extends JpaRepository<StorageDeletionOutbox, Long> {
    long countByStatus(StorageDeletionOutboxStatus status);

    long countByStatusAndNextAttemptAtLessThanEqual(StorageDeletionOutboxStatus status, Instant now);

    @Query("""
            select min(o.createdAt)
            from StorageDeletionOutbox o
            where o.status = :status
            """)
    Optional<Instant> findOldestCreatedAtByStatus(@Param("status") StorageDeletionOutboxStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o
            from StorageDeletionOutbox o
            where o.status = :status
              and o.nextAttemptAt <= :now
            order by o.nextAttemptAt, o.id
            """)
    List<StorageDeletionOutbox> findDueForUpdate(
            @Param("status") StorageDeletionOutboxStatus status,
            @Param("now") Instant now,
            Pageable pageable
    );
}
