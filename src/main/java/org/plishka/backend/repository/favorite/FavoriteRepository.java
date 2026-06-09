package org.plishka.backend.repository.favorite;

import java.util.Optional;
import org.plishka.backend.domain.favorite.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    @EntityGraph(attributePaths = {"product", "product.category"})
    Page<Favorite> findAllByUser_Id(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "product.category"})
    Optional<Favorite> findByUser_IdAndProduct_Id(Long userId, Long productId);

    @Modifying
    @Query("""
            delete from Favorite f
            where f.user.id = :userId
              and f.product.id = :productId
            """)
    void deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}
