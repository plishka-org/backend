package org.plishka.backend.repository.cart;

import java.util.Collection;
import java.util.List;
import org.plishka.backend.domain.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("""
            select distinct ci.cart.id
            from CartItem ci
            where ci.product.id in :productIds
            order by ci.cart.id
            """)
    List<Long> findCartIdsByProductIdInOrderById(@Param("productIds") Collection<Long> productIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from CartItem ci
            where ci.product.id in :productIds
            """)
    void deleteAllByProductIdIn(@Param("productIds") Collection<Long> productIds);
}
