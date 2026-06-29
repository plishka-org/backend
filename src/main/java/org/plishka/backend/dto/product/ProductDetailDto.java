package org.plishka.backend.dto.product;

import java.util.List;

public record ProductDetailDto(
        Long productId,
        String name,
        String description,
        Long price,
        CategoryDto category,
        List<ProductMediaDto> media
) {
}
