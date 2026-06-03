package org.plishka.backend.repository.order;

import java.util.Optional;
import org.plishka.backend.domain.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.orderItems oi
            LEFT JOIN FETCH oi.product p
            LEFT JOIN FETCH p.category
            WHERE o.id = :id
            """)
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.orderNumber = :orderNumber")
    Optional<Order> findByUserIdAndOrderNumber(@Param("userId") Long userId, @Param("orderNumber") String orderNumber);
}
