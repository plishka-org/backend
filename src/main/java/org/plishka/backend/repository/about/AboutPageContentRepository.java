package org.plishka.backend.repository.about;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.plishka.backend.domain.about.AboutPageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AboutPageContentRepository extends JpaRepository<AboutPageContent, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM AboutPageContent c WHERE c.id = :id")
    Optional<AboutPageContent> findByIdForUpdate(@Param("id") Long id);
}
