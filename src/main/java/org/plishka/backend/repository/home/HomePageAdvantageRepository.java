package org.plishka.backend.repository.home;

import java.util.List;
import org.plishka.backend.domain.home.HomePageAdvantage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HomePageAdvantageRepository extends JpaRepository<HomePageAdvantage, Long> {
    List<HomePageAdvantage> findAllByOrderByDisplayOrderAsc();
}
