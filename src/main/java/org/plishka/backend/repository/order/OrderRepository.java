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
            LEFT JOIN FETCH o.orderItems
            WHERE o.id = :id AND o.user.id = :userId
            """)
    Optional<Order> findByIdAndUserIdWithItems(@Param("id") Long id, @Param("userId") Long userId);

    Page<Order> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.orderItems
            WHERE o.user.id = :userId AND o.orderNumber = :orderNumber
            """)
    Optional<Order> findByUserIdAndOrderNumber(@Param("userId") Long userId, @Param("orderNumber") String orderNumber);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.orderItems
            WHERE o.user.id = :userId AND o.idempotencyKey = :idempotencyKey
            """)
    Optional<Order> findByUserIdAndIdempotencyKey(
            @Param("userId") Long userId,
            @Param("idempotencyKey") String idempotencyKey
    );
}
