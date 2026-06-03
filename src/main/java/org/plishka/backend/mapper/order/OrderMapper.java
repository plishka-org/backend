package org.plishka.backend.mapper.order;

import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.domain.order.OrderItem;
import org.plishka.backend.dto.order.OrderDetailDto;
import org.plishka.backend.dto.order.OrderItemDetailDto;
import org.plishka.backend.dto.order.OrderSummaryDto;

@Mapper(config = MapStructConfig.class)
public interface OrderMapper {
    @Mapping(source = "id", target = "orderId")
    @Mapping(source = "orderItems", target = "items")
    OrderDetailDto toDetailDto(Order order);

    @Mapping(source = "id", target = "orderId")
    OrderSummaryDto toSummaryDto(Order order);

    @Mapping(source = "id", target = "orderItemId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "productNameSnapshot", target = "productName")
    @Mapping(source = "categoryNameSnapshot", target = "categoryName")
    @Mapping(target = "subtotal", source = "orderItem")
    OrderItemDetailDto toItemDetailDto(OrderItem orderItem);

    default BigDecimal calculateSubtotal(OrderItem orderItem) {
        return orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
    }
}
