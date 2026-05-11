package org.plishka.backend.repository.about;

import java.util.List;
import org.plishka.backend.domain.about.AboutPageMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AboutPageMediaRepository extends JpaRepository<AboutPageMedia, Long> {
    List<AboutPageMedia> findAllByAboutPageIdOrderByDisplayOrderAsc(Long aboutPageId);
}
