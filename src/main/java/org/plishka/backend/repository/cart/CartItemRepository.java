package org.plishka.backend.repository.cart;

import java.util.Optional;
import org.plishka.backend.domain.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("""
            SELECT ci FROM CartItem ci
            JOIN ci.cart c
            WHERE c.user.id = :userId AND ci.product.id = :productId
            """)
    Optional<CartItem> findByUserIdAndProductId(
            @Param("userId") Long userId,
            @Param("productId") Long productId
    );

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM CartItem ci
            WHERE ci.cart.user.id = :userId AND ci.product.id = :productId
            """)
    int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}
