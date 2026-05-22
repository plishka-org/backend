package org.plishka.backend.dto.product;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailDto(
        Long productId,
        String name,
        String description,
        BigDecimal price,
        CategoryDto category,
        List<ProductMediaDto> media
) {
}
