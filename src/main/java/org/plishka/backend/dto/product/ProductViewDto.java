package org.plishka.backend.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Viewed product entry.")
public record ProductViewDto(
        @Schema(description = "Product view id.", example = "1")
        Long productViewId,
        @Schema(description = "View timestamp in UTC ISO-8601 format.", example = "2026-07-06T12:00:00Z")
        Instant viewedAt,
        @Schema(description = "Viewed product.")
        ProductSummaryDto product
) {
}
