package org.plishka.backend.repository.about;

import org.plishka.backend.domain.about.AboutPageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AboutPageContentRepository extends JpaRepository<AboutPageContent, Long> {
}
