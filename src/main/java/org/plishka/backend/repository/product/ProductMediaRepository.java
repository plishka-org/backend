package org.plishka.backend.repository.product;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.plishka.backend.domain.product.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Query("""
            select m
            from ProductMedia m
            where m.product.id in :productIds
            order by m.product.id, m.displayOrder, m.id
            """)
    List<ProductMedia> findAllByProductIdsOrderByProductIdAndDisplayOrder(
            @Param("productIds") Collection<Long> productIds
    );

    @Query("""
            select m.s3Key
            from ProductMedia m
            where m.product.id in :productIds
            order by m.product.id, m.id
            """)
    List<String> findS3KeysByProductIdInOrderByProductIdAndId(
            @Param("productIds") Collection<Long> productIds
    );

    @Query("SELECT COALESCE(MAX(m.displayOrder), 0) FROM ProductMedia m WHERE m.product.id = :productId")
    Integer findMaxDisplayOrderByProductId(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from ProductMedia m
            where m.product.id = :productId
              and m.id = :mediaId
            """)
    Optional<ProductMedia> findByProductIdAndIdForUpdate(
            @Param("productId") Long productId,
            @Param("mediaId") Long mediaId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from ProductMedia m
            where m.product.id = :productId
            order by m.displayOrder, m.id
            """)
    List<ProductMedia> findAllByProductIdForUpdateOrderByDisplayOrder(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from ProductMedia m
            where m.product.id = :productId
              and m.isPrimary = true
            """)
    Optional<ProductMedia> findPrimaryByProductIdForUpdate(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from ProductMedia m
            where m.product.id = :productId
              and m.mediaType = org.plishka.backend.domain.media.MediaType.IMAGE
            order by m.displayOrder, m.id
            """)
    List<ProductMedia> findImagesByProductIdForUpdateOrderByDisplayOrder(@Param("productId") Long productId);
}
