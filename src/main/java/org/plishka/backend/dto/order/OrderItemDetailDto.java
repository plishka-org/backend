package org.plishka.backend.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import org.plishka.backend.openapi.support.OpenApiExampleValues;

@Schema(description = "Order item details.")
public record OrderItemDetailDto(
        @Schema(description = "Order item id.", example = "1")
        Long orderItemId,
        @Schema(description = "Product id.", example = "123")
        Long productId,
        @Schema(description = "Product name.", example = OpenApiExampleValues.PRIMARY_PRODUCT_NAME)
        String productName,
        @Schema(description = "Category name.", example = OpenApiExampleValues.GENERIC_CATEGORY_NAME)
        String categoryName,
        @Schema(description = "Quantity.", example = "2")
        Integer quantity,
        @Schema(description = "Unit price as integer amount in whole Ukrainian hryvnias (UAH).", example = "1499")
        Long unitPrice,
        @Schema(description = "Subtotal as integer amount in whole Ukrainian hryvnias (UAH).", example = "2998")
        Long subtotal
) {
}
