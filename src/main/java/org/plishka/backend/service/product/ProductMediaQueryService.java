package org.plishka.backend.service.product;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.product.ProductMedia;
import org.plishka.backend.repository.product.ProductMediaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductMediaQueryService {
    private final ProductMediaRepository productMediaRepository;

    @Transactional(readOnly = true)
    public Map<Long, ProductMedia> findPrimaryMediaForProducts(Collection<Product> products) {
        List<Long> productIds = extractProductIds(products);
        return findPrimaryMediaByProductIds(productIds);
    }

    @Transactional(readOnly = true)
    public Map<Long, ProductMedia> findPrimaryMediaByProductIds(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        return productMediaRepository.findPrimaryMediaByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(media -> media.getProduct().getId(), Function.identity()));
    }

    @Transactional(readOnly = true)
    public Map<Long, List<ProductMedia>> findMediaForProducts(Collection<Product> products) {
        List<Long> productIds = extractProductIds(products);
        if (productIds.isEmpty()) {
            return Map.of();
        }

        return productMediaRepository.findAllByProductIdsOrderByProductIdAndDisplayOrder(productIds)
                .stream()
                .collect(Collectors.groupingBy(
                        media -> media.getProduct().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private List<Long> extractProductIds(Collection<Product> products) {
        return products.stream()
                .map(Product::getId)
                .toList();
    }
}
