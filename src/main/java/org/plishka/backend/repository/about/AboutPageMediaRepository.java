package org.plishka.backend.repository.about;

import java.util.List;
import org.plishka.backend.domain.about.AboutPageMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AboutPageMediaRepository extends JpaRepository<AboutPageMedia, Long> {
    List<AboutPageMedia> findAllByAboutPage_IdOrderByDisplayOrderAsc(Long aboutPageId);

    boolean existsByS3Key(String s3Key);

    @Query("SELECT COALESCE(MAX(m.displayOrder), 0) FROM AboutPageMedia m WHERE m.aboutPage.id = :aboutPageId")
    Integer findMaxDisplayOrderByAboutPageId(@Param("aboutPageId") Long aboutPageId);
}
