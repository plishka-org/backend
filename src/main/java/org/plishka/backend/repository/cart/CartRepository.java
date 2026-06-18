package org.plishka.backend.repository.cart;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.plishka.backend.domain.cart.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    /**
     * Acquires a pessimistic write lock on the cart row only. Intentionally does
     * not fetch the item graph: combining {@code FOR UPDATE} with a join fetch
     * would also lock the joined product/category rows, which are shared across
     * users and would cause contention or deadlocks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * Loads the full cart aggregate (items -> product -> category) in a single
     * query without locking. Call this after the lock has been acquired with
     * {@link #findByUserIdForUpdate(Long)}.
     */
    @EntityGraph(attributePaths = {"cartItems", "cartItems.product", "cartItems.product.category"})
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findAggregateByUserId(@Param("userId") Long userId);
}
