package org.plishka.backend.mapper.cart;

import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.cart.CartItem;
import org.plishka.backend.dto.cart.CartItemSummaryDto;

@Mapper(config = MapStructConfig.class)
public interface CartItemMapper {
    @Mapping(target = "cartItemId", source = "id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "categoryName", source = "product.category.name")
    @Mapping(target = "subtotal", source = "cartItem")
    CartItemSummaryDto toSummaryDto(CartItem cartItem);

    default BigDecimal calculateSubtotal(CartItem cartItem) {
        return cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
    }
}
