package org.plishka.backend.repository.product;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.plishka.backend.domain.product.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByDisplayOrderAscIdAsc();

    List<Category> findAllByNameContainingIgnoreCaseOrderByDisplayOrderAscIdAsc(String name);

    @Query("SELECT COALESCE(MAX(c.displayOrder), 0) FROM Category c")
    Integer findMaxDisplayOrder();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Category c ORDER BY c.displayOrder, c.id")
    List<Category> findAllForUpdateOrderByDisplayOrder();

    @Query("""
            select count(c) > 0
            from Category c
            where lower(c.name) = lower(:name)
            """)
    boolean existsByNameIgnoreCase(@Param("name") String name);

    @Query("""
            select count(c) > 0
            from Category c
            where lower(c.name) = lower(:name)
              and c.id <> :id
            """)
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from Category c
            where c.id = :id
            """)
    Optional<Category> findByIdForUpdate(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete
            from Category c
            where c.id = :id
            """)
    int deleteByIdDirect(@Param("id") Long id);
}
