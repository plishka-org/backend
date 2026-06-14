package org.plishka.backend.service.order;

import org.plishka.backend.dto.order.CreateOrderRequestDto;
import org.plishka.backend.dto.order.OrderDetailDto;

public interface OrderCheckoutService {
    OrderDetailDto checkout(Long userId, String idempotencyKey, CreateOrderRequestDto requestDto);
}
