package org.plishka.backend.service.order;

import org.plishka.backend.domain.order.Order;
import org.plishka.backend.domain.order.OrderItem;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.service.pricing.PriceCalculator;
import org.springframework.stereotype.Component;

@Component
public class OrderItemFactory {
    private static final String UNCATEGORIZED_CATEGORY_NAME = "Uncategorized";

    public OrderItem create(Order order, Product product, int quantity) {
        Long unitPrice = product.getPrice();

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProductId(product.getId());
        orderItem.setProductNameSnapshot(product.getName());
        orderItem.setCategoryNameSnapshot(resolveCategoryName(product));
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(unitPrice);
        orderItem.setLineTotal(PriceCalculator.calculateLineTotal(unitPrice, quantity));
        return orderItem;
    }

    private String resolveCategoryName(Product product) {
        return product.getCategory() == null ? UNCATEGORIZED_CATEGORY_NAME : product.getCategory().getName();
    }
}
