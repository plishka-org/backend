package org.plishka.backend.repository.home;

import org.plishka.backend.domain.home.HomePageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HomePageContentRepository extends JpaRepository<HomePageContent, Long> {
}
