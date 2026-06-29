package org.plishka.backend.dto.product;

public record ProductSummaryDto(
        Long productId,
        String name,
        CategoryDto category,
        Long price,
        ProductMediaPreviewDto primaryMedia
) {
}
