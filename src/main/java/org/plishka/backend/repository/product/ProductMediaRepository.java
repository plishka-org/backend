package org.plishka.backend.repository.product;

import java.util.Collection;
import java.util.List;
import org.plishka.backend.domain.product.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {
    boolean existsByS3Key(String s3Key);

    @Query("SELECT m.s3Key FROM ProductMedia m")
    List<String> findAllS3Keys();

    boolean existsByProduct_IdAndIsPrimaryTrue(Long productId);

    @Query("SELECT m FROM ProductMedia m WHERE m.product.id IN :productIds AND m.isPrimary = true")
    List<ProductMedia> findPrimaryMediaByProductIds(@Param("productIds") Collection<Long> productIds);

    @Query("SELECT COALESCE(MAX(m.displayOrder), 0) FROM ProductMedia m WHERE m.product.id = :productId")
    Integer findMaxDisplayOrderByProductId(@Param("productId") Long productId);
}
