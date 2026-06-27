package org.plishka.backend.dto.home;

import java.math.BigDecimal;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.dto.product.ProductMediaPreviewDto;

public record HomePageProductDto(
        Long productId,
        String name,
        CategoryDto category,
        BigDecimal price,
        ProductMediaPreviewDto primaryMedia
) {
}
