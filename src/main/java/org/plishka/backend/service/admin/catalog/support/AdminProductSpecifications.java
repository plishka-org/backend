package org.plishka.backend.service.admin.catalog.support;

import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.dto.admin.product.AdminProductFiltersDto;
import org.plishka.backend.util.BulkIdNormalizer;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class AdminProductSpecifications {
    private AdminProductSpecifications() {
    }

    public static Specification<Product> fromFilters(AdminProductFiltersDto filters) {
        if (filters == null) {
            return Specification.unrestricted();
        }

        return byCategoryFilter(filters.categoryIds(), filters.uncategorized())
                .and(bySearch(filters.search()));
    }

    private static Specification<Product> byCategoryFilter(List<Long> categoryIds, Boolean uncategorized) {
        List<Long> normalizedIds = BulkIdNormalizer.normalize(categoryIds);
        boolean includeUncategorized = Boolean.TRUE.equals(uncategorized);

        if (hasNoCategoryFilter(normalizedIds, includeUncategorized)) {
            return Specification.unrestricted();
        }

        if (isUncategorizedOnly(normalizedIds, includeUncategorized)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("category"));
        }

        if (isCategoryOnly(includeUncategorized)) {
            return (root, query, criteriaBuilder) -> root.get("category").get("id").in(normalizedIds);
        }

        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                root.get("category").get("id").in(normalizedIds),
                criteriaBuilder.isNull(root.get("category"))
        );
    }

    private static Specification<Product> bySearch(String search) {
        if (!StringUtils.hasText(search)) {
            return Specification.unrestricted();
        }

        String searchTerm = search.trim();
        String searchPattern = "%" + searchTerm.toLowerCase(Locale.ROOT) + "%";
        BigDecimal searchedPrice = parseSearchPrice(searchTerm);

        return (root, query, criteriaBuilder) -> {
            var categoryJoin = root.join("category", JoinType.LEFT);
            var nameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern);
            var categoryLike = criteriaBuilder.like(criteriaBuilder.lower(categoryJoin.get("name")), searchPattern);

            if (searchedPrice == null) {
                return criteriaBuilder.or(nameLike, categoryLike);
            }

            return criteriaBuilder.or(nameLike, categoryLike, criteriaBuilder.equal(root.get("price"), searchedPrice));
        };
    }

    private static boolean hasNoCategoryFilter(List<Long> categoryIds, boolean includeUncategorized) {
        return categoryIds.isEmpty() && !includeUncategorized;
    }

    private static boolean isUncategorizedOnly(List<Long> categoryIds, boolean includeUncategorized) {
        return categoryIds.isEmpty() && includeUncategorized;
    }

    private static boolean isCategoryOnly(boolean includeUncategorized) {
        return !includeUncategorized;
    }

    private static BigDecimal parseSearchPrice(String searchTerm) {
        try {
            return new BigDecimal(searchTerm);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
