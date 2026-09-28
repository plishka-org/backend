package org.plishka.backend.event.order;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.domain.order.OrderItem;

@Builder
public record OrderCreatedEvent(
        Long orderId,
        String orderNumber,
        Long userId,
        String userEmail,
        String customerName,
        String deliveryCity,
        String phone,
        String notes,
        Long totalPrice,
        Instant createdAt,
        List<Item> items
) {
    @Builder
    public record Item(
            String productName,
            String categoryName,
            Integer quantity,
            Long unitPrice,
            Long lineTotal
    ) {
    }

    public static OrderCreatedEvent fromOrder(Order order) {
        return OrderCreatedEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .userEmail(order.getUser().getEmail())
                .customerName(order.getCustomerName())
                .deliveryCity(order.getDeliveryCity())
                .phone(order.getPhone())
                .notes(order.getNotes())
                .totalPrice(order.getTotalPrice())
                .createdAt(order.getCreatedAt())
                .items(order.getOrderItems().stream()
                        .map(OrderCreatedEvent::toItem)
                        .toList())
                .build();
    }

    private static Item toItem(OrderItem orderItem) {
        return Item.builder()
                .productName(orderItem.getProductNameSnapshot())
                .categoryName(orderItem.getCategoryNameSnapshot())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .lineTotal(orderItem.getLineTotal())
                .build();
    }
}
