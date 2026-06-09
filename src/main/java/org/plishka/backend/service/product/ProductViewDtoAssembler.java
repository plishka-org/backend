package org.plishka.backend.service.product;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.product.ProductView;
import org.plishka.backend.dto.product.ProductSummaryDto;
import org.plishka.backend.dto.product.ProductViewDto;
import org.plishka.backend.mapper.product.ProductViewMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductViewDtoAssembler {
    private final ProductSummaryAssembler productSummaryAssembler;
    private final ProductViewMapper productViewMapper;

    public ProductViewDto toDto(ProductView productView) {
        return productViewMapper.toDto(
                productView,
                productSummaryAssembler.toDto(productView.getProduct())
        );
    }

    public List<ProductViewDto> toDtos(List<ProductView> productViews) {
        Map<Long, ProductSummaryDto> productSummariesById = productSummaryAssembler.toDtoMapForProducts(
                productViews.stream()
                        .map(ProductView::getProduct)
                        .toList()
        );

        return productViews.stream()
                .map(productView -> productViewMapper.toDto(
                        productView,
                        productSummariesById.get(productView.getProduct().getId())
                ))
                .toList();
    }
}
