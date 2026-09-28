package org.plishka.backend.service.order;

import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.order.OrderDetailDto;
import org.plishka.backend.dto.order.OrderSummaryDto;

public interface OrderService {
    OrderDetailDto getOrderById(Long orderId, Long userId);

    OrderDetailDto getOrderByOrderNumber(String orderNumber, Long userId);

    PageResponse<OrderSummaryDto> getUserOrders(Long userId, int page, int size);

    OrderDetailDto repeatOrder(Long orderId, Long userId, String idempotencyKey);
}
