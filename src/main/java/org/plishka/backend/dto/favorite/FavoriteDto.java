package org.plishka.backend.dto.favorite;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.plishka.backend.dto.product.ProductSummaryDto;

@Schema(description = "Favorite product entry.")
public record FavoriteDto(
        @Schema(description = "Favorite id.", example = "1")
        Long favoriteId,
        @Schema(description = "Creation timestamp in UTC ISO-8601 format.", example = "2026-07-06T12:00:00Z")
        Instant createdAt,
        @Schema(description = "Favorite product.")
        ProductSummaryDto product
) {
}
