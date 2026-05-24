package org.plishka.backend.dto.product;

import java.math.BigDecimal;

public record ProductSummaryDto(
        Long productId,
        String name,
        CategoryDto category,
        BigDecimal price,
        ProductMediaPreviewDto primaryMedia
) {
}
