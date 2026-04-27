package org.plishka.backend.repository.home;

import java.util.List;
import org.plishka.backend.domain.home.HomePageProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HomePageProductRepository extends JpaRepository<HomePageProduct, Long> {
    List<HomePageProduct> findAllByOrderByDisplayOrderAsc();
}
