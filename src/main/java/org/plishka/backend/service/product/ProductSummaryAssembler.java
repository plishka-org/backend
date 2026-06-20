package org.plishka.backend.service.product;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.product.ProductMedia;
import org.plishka.backend.dto.product.ProductSummaryDto;
import org.plishka.backend.mapper.product.ProductMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductSummaryAssembler {
    private final ProductMediaQueryService productMediaQueryService;
    private final ProductMapper productMapper;

    public ProductSummaryDto toDto(Product product) {
        return toDtoMapForProducts(List.of(product)).get(product.getId());
    }

    public List<ProductSummaryDto> toDtos(Collection<Product> products) {
        return List.copyOf(toDtoMapForProducts(products).values());
    }

    public Map<Long, ProductSummaryDto> toDtoMapForProducts(Collection<Product> products) {
        Map<Long, ProductMedia> primaryMediaByProductId =
                productMediaQueryService.findPrimaryMediaForProducts(products);

        return products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        product -> productMapper.toSummaryDto(product, primaryMediaByProductId.get(product.getId())),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
    }
}
