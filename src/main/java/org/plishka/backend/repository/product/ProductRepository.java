package org.plishka.backend.repository.product;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.plishka.backend.domain.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p")
    Page<Product> findAllWithCategory(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findAllByCategory_IdIn(Collection<Long> categoryIds, Pageable pageable);

    @Query("SELECT p.category.id FROM Product p WHERE p.id = :productId")
    Optional<Long> findCategoryIdByProductId(@Param("productId") Long productId);

    @EntityGraph(attributePaths = "category")
    Page<Product> findAllByCategory_IdAndIdNot(Long categoryId, Long id, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "media"})
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findDetailsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findAllByIdIn(@Param("ids") Collection<Long> ids);
}
