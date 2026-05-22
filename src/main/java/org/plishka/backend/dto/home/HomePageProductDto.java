package org.plishka.backend.dto.home;

import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.dto.product.ProductMediaPreviewDto;

public record HomePageProductDto(
        Long productId,
        String name,
        CategoryDto category,
        ProductMediaPreviewDto primaryMedia
) {
}
