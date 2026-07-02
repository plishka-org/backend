package org.plishka.backend.repository.home;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.plishka.backend.domain.home.HomePageAdvantage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HomePageAdvantageRepository extends JpaRepository<HomePageAdvantage, Long> {
    List<HomePageAdvantage> findAllByOrderByDisplayOrderAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a
            from HomePageAdvantage a
            where a.id = :id
            """)
    Optional<HomePageAdvantage> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a
            from HomePageAdvantage a
            order by a.displayOrder, a.id
            """)
    List<HomePageAdvantage> findAllForUpdateOrderByDisplayOrder();
}
