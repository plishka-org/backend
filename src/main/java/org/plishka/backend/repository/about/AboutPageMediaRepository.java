package org.plishka.backend.repository.about;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.plishka.backend.domain.about.AboutPageMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AboutPageMediaRepository extends JpaRepository<AboutPageMedia, Long> {
    List<AboutPageMedia> findAllByAboutPage_IdOrderByDisplayOrderAsc(Long aboutPageId);

    boolean existsByS3Key(String s3Key);

    @Query("SELECT m.s3Key FROM AboutPageMedia m")
    List<String> findAllS3Keys();

    @Query("SELECT COALESCE(MAX(m.displayOrder), 0) FROM AboutPageMedia m WHERE m.aboutPage.id = :aboutPageId")
    Integer findMaxDisplayOrderByAboutPageId(@Param("aboutPageId") Long aboutPageId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from AboutPageMedia m
            where m.aboutPage.id = :aboutPageId
            and m.id = :mediaId
            """)
    Optional<AboutPageMedia> findByAboutPageIdAndIdForUpdate(
            @Param("aboutPageId") Long aboutPageId,
            @Param("mediaId") Long mediaId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from AboutPageMedia m
            where m.aboutPage.id = :aboutPageId
            order by m.displayOrder, m.id
            """)
    List<AboutPageMedia> findAllByAboutPageIdForUpdateOrderByDisplayOrder(@Param("aboutPageId") Long aboutPageId);
}
