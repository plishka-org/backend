package org.plishka.backend.repository.home;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.plishka.backend.domain.home.HomePageProduct;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HomePageProductRepository extends JpaRepository<HomePageProduct, Long> {
    @EntityGraph(attributePaths = {"product", "product.category"})
    List<HomePageProduct> findAllByOrderByDisplayOrderAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select h
            from HomePageProduct h
            order by h.displayOrder
            """)
    List<HomePageProduct> findAllForUpdateOrderByDisplayOrder();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from HomePageProduct h
            where h.product.id = :productId
            """)
    int deleteByProductId(@Param("productId") Long productId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from HomePageProduct h
            where h.product.id in :productIds
            """)
    int deleteAllByProductIdIn(@Param("productIds") List<Long> productIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from HomePageProduct h
            where h.product.category.id = :categoryId
            """)
    int deleteByProductCategoryId(@Param("categoryId") Long categoryId);
}
