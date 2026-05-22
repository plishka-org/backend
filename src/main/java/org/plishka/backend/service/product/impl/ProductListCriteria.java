package org.plishka.backend.service.product.impl;

import java.util.List;
import java.util.Locale;
import org.plishka.backend.exception.BadRequestException;
import org.springframework.data.domain.Sort;

record ProductListCriteria(List<Long> categoryIds, Sort sort) {
    private static final String NAME_ASC_SORT = "name,asc";
    private static final String NAME_DESC_SORT = "name,desc";
    private static final String DEFAULT_SORT = NAME_ASC_SORT;
    private static final int MAX_CATEGORY_FILTER_SIZE = 20;

    static ProductListCriteria from(List<Long> categoryIds, String sort) {
        List<Long> normalizedCategoryIds = categoryIds == null || categoryIds.isEmpty()
                ? List.of()
                : categoryIds.stream()
                .distinct()
                .toList();
        requireCategoryFilterWithinLimit(normalizedCategoryIds);

        String normalizedSort = normalizeSort(sort);
        Sort resolvedSort = switch (normalizedSort) {
            case NAME_ASC_SORT -> Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));
            case NAME_DESC_SORT -> Sort.by(Sort.Order.desc("name"), Sort.Order.desc("id"));
            default -> throw new BadRequestException("Unsupported product sort");
        };

        return new ProductListCriteria(normalizedCategoryIds, resolvedSort);
    }

    private static void requireCategoryFilterWithinLimit(List<Long> categoryIds) {
        if (categoryIds.size() > MAX_CATEGORY_FILTER_SIZE) {
            throw new BadRequestException("Category filter must contain at most 20 category ids");
        }
    }

    private static String normalizeSort(String sort) {
        return sort == null || sort.isBlank()
                ? DEFAULT_SORT
                : sort.trim().toLowerCase(Locale.ROOT);
    }

    boolean hasCategoryFilter() {
        return !categoryIds.isEmpty();
    }
}
