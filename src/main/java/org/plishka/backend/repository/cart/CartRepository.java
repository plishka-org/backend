package org.plishka.backend.repository.cart;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.plishka.backend.domain.cart.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("""
            SELECT DISTINCT c FROM Cart c
            LEFT JOIN FETCH c.cartItems ci
            LEFT JOIN FETCH ci.product p
            LEFT JOIN FETCH p.category
            WHERE c.user.id = :userId
            """)
    Optional<Cart> findByUserId(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdForUpdate(@Param("userId") Long userId);
}
