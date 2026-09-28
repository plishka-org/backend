package org.plishka.backend.repository.settings;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.plishka.backend.domain.settings.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from SystemSettings s
            where s.id = :id
            """)
    Optional<SystemSettings> findByIdForUpdate(@Param("id") Long id);
}
