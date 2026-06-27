package org.plishka.backend.service.product.impl;

import java.util.List;
import java.util.Objects;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.service.product.ProductSortResolver;
import org.springframework.data.domain.Sort;

record ProductListCriteria(List<Long> categoryIds, Sort sort) {
    private static final int MAX_CATEGORY_FILTER_SIZE = 20;

    static ProductListCriteria from(List<Long> categoryIds, String sort) {
        List<Long> normalizedCategoryIds = categoryIds == null || categoryIds.isEmpty()
                ? List.of()
                : categoryIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        requireCategoryFilterWithinLimit(normalizedCategoryIds);

        return new ProductListCriteria(normalizedCategoryIds, ProductSortResolver.resolve(sort));
    }

    private static void requireCategoryFilterWithinLimit(List<Long> categoryIds) {
        if (categoryIds.size() > MAX_CATEGORY_FILTER_SIZE) {
            throw new BadRequestException("Category filter must contain at most 20 category ids");
        }
    }

    boolean hasCategoryFilter() {
        return !categoryIds.isEmpty();
    }
}
