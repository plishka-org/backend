package org.plishka.backend.mapper.cart;

import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.cart.CartItem;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.dto.cart.CartItemSummaryDto;

@Mapper(config = MapStructConfig.class)
public interface CartItemMapper {
    String UNCATEGORIZED_CATEGORY_NAME = "Uncategorized";

    @Mapping(target = "cartItemId", source = "id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "categoryName", source = "product")
    @Mapping(target = "unitPrice", source = "product.price")
    @Mapping(target = "subtotal", source = "cartItem")
    CartItemSummaryDto toSummaryDto(CartItem cartItem);

    default String resolveCategoryName(Product product) {
        return product.getCategory() == null ? UNCATEGORIZED_CATEGORY_NAME : product.getCategory().getName();
    }

    default BigDecimal calculateSubtotal(CartItem cartItem) {
        return cartItem.getProduct().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
    }
}
