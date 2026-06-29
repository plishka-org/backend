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
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(org.springframework.data.jpa.domain.Specification<Product> specification, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p")
    Page<Product> findAllWithCategory(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findAllByCategory_IdIn(Collection<Long> categoryIds, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);

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

    @Query("""
            select p.id
            from Product p
            where p.category.id = :categoryId
            order by p.id
            """)
    List<Long> findIdsByCategoryIdOrderById(@Param("categoryId") Long categoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Product p
            where p.id in :ids
            order by p.id
            """)
    List<Product> findAllByIdInForUpdateOrderById(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = "category")
    @Query("""
            select p
            from Product p
            where p.id in :ids
            order by p.id
            """)
    List<Product> findAllWithCategoryByIdInOrderById(@Param("ids") Collection<Long> ids);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Product p
            set p.category = null
            where p.category.id = :categoryId
            """)
    int clearCategoryByCategoryId(@Param("categoryId") Long categoryId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            update products
            set category_id = :targetCategoryId
            where category_id = :sourceCategoryId
            """, nativeQuery = true)
    int moveCategoryByCategoryId(
            @Param("sourceCategoryId") Long sourceCategoryId,
            @Param("targetCategoryId") Long targetCategoryId
    );

    @Query("""
            select count(p) > 0
            from Product p
            where lower(p.name) = lower(:name)
            """)
    boolean existsByNameIgnoreCase(@Param("name") String name);

    @Query("""
            select count(p) > 0
            from Product p
            where lower(p.name) = lower(:name)
              and p.id <> :id
            """)
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);
}
