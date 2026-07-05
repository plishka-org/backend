package org.plishka.backend.repository.home;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.plishka.backend.domain.home.HomePageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HomePageContentRepository extends JpaRepository<HomePageContent, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from HomePageContent c
            where c.id = :id
            """)
    Optional<HomePageContent> findByIdForUpdate(@Param("id") Long id);
}
