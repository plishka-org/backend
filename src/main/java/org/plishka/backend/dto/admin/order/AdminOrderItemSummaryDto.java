package org.plishka.backend.dto.admin.order;

import io.swagger.v3.oas.annotations.media.Schema;
import org.plishka.backend.dto.product.ProductMediaPreviewDto;
import org.plishka.backend.openapi.support.OpenApiExampleValues;

@Schema(description = "Admin order item summary.")
public record AdminOrderItemSummaryDto(
        @Schema(description = "Current product id. May be null for historical deleted products.", nullable = true)
        Long productId,

        @Schema(description = "Product name snapshot.", example = OpenApiExampleValues.PRIMARY_PRODUCT_NAME)
        String productName,

        @Schema(description = "Current primary product image. May be null when unavailable.", nullable = true)
        ProductMediaPreviewDto primaryImage
) {
}
