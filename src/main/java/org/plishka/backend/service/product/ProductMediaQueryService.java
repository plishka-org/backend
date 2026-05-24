package org.plishka.backend.service.product;

import java.util.Collection;
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
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();

        if (productIds.isEmpty()) {
            return Map.of();
        }

        return productMediaRepository.findPrimaryMediaByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(media -> media.getProduct().getId(), Function.identity()));
    }
}
