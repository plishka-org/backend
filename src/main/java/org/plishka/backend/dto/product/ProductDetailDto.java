package org.plishka.backend.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.plishka.backend.openapi.support.OpenApiExampleValues;

@Schema(description = "Product details.")
public record ProductDetailDto(
        @Schema(description = "Product id.", example = "123")
        Long productId,
        @Schema(description = "Product name.", example = OpenApiExampleValues.PRIMARY_PRODUCT_NAME)
        String name,
        @Schema(
                description = "Product description.",
                example = OpenApiExampleValues.PRODUCT_DESCRIPTION
        )
        String description,
        @Schema(
                description = "Price as integer amount in whole Ukrainian hryvnias (UAH). "
                        + "May be null when shop mode is disabled.",
                example = "1499",
                nullable = true
        )
        Long price,
        @Schema(description = "Product category.")
        CategoryDto category,
        @Schema(description = "Product media.")
        List<ProductMediaDto> media
) {
}
