package org.plishka.backend.mapper.cart;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.cart.CartItem;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.dto.cart.CartItemSummaryDto;
import org.plishka.backend.service.pricing.PriceCalculator;

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

    default Long calculateSubtotal(CartItem cartItem) {
        return PriceCalculator.calculateLineTotal(cartItem.getProduct().getPrice(), cartItem.getQuantity());
    }
}
