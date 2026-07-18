package org.plishka.backend.dto.admin.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.dto.product.ProductMediaDto;
import org.plishka.backend.openapi.support.OpenApiExampleValues;

@Schema(description = "Admin product details.")
public record AdminProductDetailDto(
        @Schema(description = "Product id.", example = "123")
        Long productId,
        @Schema(description = "Product name.", example = OpenApiExampleValues.PRIMARY_PRODUCT_NAME)
        String name,
        @Schema(
                description = "Product description.",
                example = OpenApiExampleValues.PRODUCT_DESCRIPTION
        )
        String description,
        @Schema(description = "Price as integer amount in whole Ukrainian hryvnias (UAH).", example = "1499")
        Long price,
        @Schema(description = "Product category. May be null for uncategorized products.", nullable = true)
        CategoryDto category,
        @Schema(description = "Product media.")
        List<ProductMediaDto> media
) {
}
