package org.plishka.backend.service.order;

import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.exception.ConflictException;
import org.plishka.backend.repository.order.OrderRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderIdempotencyGuard {
    private final OrderRepository orderRepository;

    public Optional<Order> findExistingOrder(Long userId, String idempotencyKey, String requestHash) {
        return orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(existingOrder -> {
                    if (!Objects.equals(existingOrder.getRequestHash(), requestHash)) {
                        throw new ConflictException(
                                "Idempotency key was already used with a different request payload");
                    }
                    return existingOrder;
                });
    }
}
