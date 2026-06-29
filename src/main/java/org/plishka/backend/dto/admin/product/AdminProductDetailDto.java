package org.plishka.backend.dto.admin.product;

import java.util.List;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.dto.product.ProductMediaDto;

public record AdminProductDetailDto(
        Long productId,
        String name,
        String description,
        Long price,
        CategoryDto category,
        List<ProductMediaDto> media
) {
}
