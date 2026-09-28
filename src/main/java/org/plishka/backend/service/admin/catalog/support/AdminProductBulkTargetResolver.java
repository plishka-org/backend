package org.plishka.backend.service.admin.catalog.support;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.dto.admin.common.SelectionMode;
import org.plishka.backend.dto.admin.product.AdminProductFiltersDto;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.util.EntityPresenceValidator;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminProductBulkTargetResolver {
    private static final int MAX_SELECTED_IDS = 500;
    private static final Sort ID_ASC_SORT = Sort.by(Sort.Order.asc("id"));

    private final ProductRepository productRepository;

    public List<Long> resolveTargetIds(
            SelectionMode selectionMode,
            List<Long> requestedIds,
            AdminProductFiltersDto filters
    ) {
        List<Long> normalizedIds = BulkSelectionSupport.normalizeIdsForSelection(
                selectionMode,
                requestedIds,
                "Product ids",
                MAX_SELECTED_IDS
        );

        if (selectionMode == SelectionMode.SELECTED) {
            return normalizedIds;
        }

        Set<Long> excludedIds = Set.copyOf(normalizedIds);
        Specification<Product> specification = AdminProductSpecifications.fromFilters(filters);

        return productRepository.findAll(specification, ID_ASC_SORT)
                .stream()
                .map(Product::getId)
                .filter(id -> !excludedIds.contains(id))
                .toList();
    }

    public List<Product> findTargetProductsForUpdate(
            SelectionMode selectionMode,
            List<Long> requestedIds,
            AdminProductFiltersDto filters
    ) {
        List<Long> targetIds = resolveTargetIds(selectionMode, requestedIds, filters);
        if (targetIds.isEmpty()) {
            return List.of();
        }

        List<Product> products = productRepository.findAllByIdInForUpdateOrderById(targetIds);
        if (selectionMode == SelectionMode.SELECTED) {
            EntityPresenceValidator.requireAllIdsFound(
                    targetIds,
                    products.stream().map(Product::getId).toList(),
                    "Product"
            );
        }

        return products;
    }
}
