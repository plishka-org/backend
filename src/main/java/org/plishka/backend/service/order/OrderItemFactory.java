package org.plishka.backend.service.order;

import java.math.BigDecimal;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.domain.order.OrderItem;
import org.plishka.backend.domain.product.Product;
import org.springframework.stereotype.Component;

@Component
public class OrderItemFactory {
    public OrderItem create(Order order, Product product, int quantity) {
        BigDecimal unitPrice = product.getPrice();

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProductId(product.getId());
        orderItem.setProductNameSnapshot(product.getName());
        orderItem.setCategoryNameSnapshot(product.getCategory().getName());
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(unitPrice);
        orderItem.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        return orderItem;
    }
}
