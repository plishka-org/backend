package org.plishka.backend.dto.favorite;

import java.time.Instant;
import org.plishka.backend.dto.product.ProductSummaryDto;

public record FavoriteDto(
        Long favoriteId,
        Instant createdAt,
        ProductSummaryDto product
) {
}
