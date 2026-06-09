package org.plishka.backend.dto.product;

import java.time.Instant;

public record ProductViewDto(
        Long productViewId,
        Instant viewedAt,
        ProductSummaryDto product
) {
}
