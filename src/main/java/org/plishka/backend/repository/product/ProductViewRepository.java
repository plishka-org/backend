package org.plishka.backend.repository.product;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.plishka.backend.domain.product.ProductView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductViewRepository extends JpaRepository<ProductView, Long> {
    @EntityGraph(attributePaths = {"product", "product.category"})
    @Query("""
            select pv
            from ProductView pv
            where pv.user.id = :userId
              and pv.product.category is not null
            order by pv.viewedAt desc, pv.id desc
            """)
    List<ProductView> findAllVisibleByUserIdOrderByViewedAtDescIdDesc(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"product", "product.category"})
    Optional<ProductView> findByUser_IdAndProduct_Id(Long userId, Long productId);

    long countByUser_Id(Long userId);

    List<ProductView> findAllByUser_IdOrderByViewedAtAscIdAsc(Long userId, Pageable pageable);

    @Modifying
    @Query("""
            delete from ProductView pv
            where pv.id in :ids
            """)
    void deleteAllByIdIn(@Param("ids") Collection<Long> ids);
}
