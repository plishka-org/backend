package org.plishka.backend.service.order;

import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ConflictException;
import org.plishka.backend.repository.order.OrderRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderIdempotencyGuard {
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 64;

    private final OrderRepository orderRepository;

    public Optional<Order> findExistingOrder(Long userId, String idempotencyKey, String requestHash) {
        validateIdempotencyKey(idempotencyKey);

        return orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(existingOrder -> {
                    if (!Objects.equals(existingOrder.getRequestHash(), requestHash)) {
                        throw new ConflictException(
                                "Idempotency key was already used with a different request payload");
                    }
                    return existingOrder;
                });
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key must not be blank");
        }
        if (idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BadRequestException(
                    "Idempotency-Key must not exceed " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
        }
    }
}
