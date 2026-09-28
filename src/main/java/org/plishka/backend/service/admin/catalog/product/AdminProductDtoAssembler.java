package org.plishka.backend.service.admin.catalog.product;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.product.ProductMedia;
import org.plishka.backend.dto.admin.product.AdminProductDetailDto;
import org.plishka.backend.dto.product.ProductMediaDto;
import org.plishka.backend.mapper.product.CategoryMapper;
import org.plishka.backend.mapper.product.ProductMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AdminProductDtoAssembler {
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    AdminProductDetailDto toDto(Product product) {
        return toDto(product, product.getMedia());
    }

    AdminProductDetailDto toDto(Product product, List<ProductMedia> media) {
        List<ProductMediaDto> mediaDtos = media.stream()
                .map(productMapper::toMediaDto)
                .toList();

        return new AdminProductDetailDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                categoryMapper.toDto(product.getCategory()),
                mediaDtos
        );
    }

    List<AdminProductDetailDto> toDtos(
            Collection<Product> products,
            Map<Long, List<ProductMedia>> mediaByProductId
    ) {
        return products.stream()
                .map(product -> toDto(product, mediaByProductId.getOrDefault(product.getId(), List.of())))
                .toList();
    }
}
