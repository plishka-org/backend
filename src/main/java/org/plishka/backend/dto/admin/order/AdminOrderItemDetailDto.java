package org.plishka.backend.dto.admin.order;

import io.swagger.v3.oas.annotations.media.Schema;
import org.plishka.backend.dto.product.ProductMediaPreviewDto;
import org.plishka.backend.openapi.support.OpenApiExampleValues;

@Schema(description = "Admin order item details.")
public record AdminOrderItemDetailDto(
        @Schema(description = "Current product id. May be null for historical deleted products.", nullable = true)
        Long productId,

        @Schema(description = "Product name snapshot.", example = OpenApiExampleValues.PRIMARY_PRODUCT_NAME)
        String productName,

        @Schema(description = "Quantity.", example = "2")
        Integer quantity,

        @Schema(description = "Unit price in whole UAH.", example = "1499")
        Long unitPrice,

        @Schema(description = "Line subtotal in whole UAH.", example = "2998")
        Long subtotal,

        @Schema(description = "Current primary product image. May be null when unavailable.", nullable = true)
        ProductMediaPreviewDto primaryImage
) {
}
