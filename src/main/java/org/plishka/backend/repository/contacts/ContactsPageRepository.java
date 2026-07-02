package org.plishka.backend.repository.contacts;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.plishka.backend.domain.contacts.ContactsPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactsPageRepository extends JpaRepository<ContactsPage, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from ContactsPage c
            where c.id = :id
            """)
    Optional<ContactsPage> findByIdForUpdate(@Param("id") Long id);
}
