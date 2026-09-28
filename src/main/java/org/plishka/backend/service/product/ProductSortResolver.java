package org.plishka.backend.service.product;

import java.util.Locale;
import org.plishka.backend.exception.BadRequestException;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

public final class ProductSortResolver {
    private static final String NAME_ASC_SORT = "name,asc";
    private static final String NAME_DESC_SORT = "name,desc";
    private static final String PRICE_ASC_SORT = "price,asc";
    private static final String PRICE_DESC_SORT = "price,desc";
    private static final String DEFAULT_SORT = NAME_ASC_SORT;

    private ProductSortResolver() {
    }

    public static boolean isPriceSort(String sort) {
        String normalizedSort = normalize(sort);
        return PRICE_ASC_SORT.equals(normalizedSort) || PRICE_DESC_SORT.equals(normalizedSort);
    }

    public static Sort resolve(String sort) {
        String normalizedSort = normalize(sort);

        return switch (normalizedSort) {
            case NAME_ASC_SORT -> Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));
            case NAME_DESC_SORT -> Sort.by(Sort.Order.desc("name"), Sort.Order.desc("id"));
            case PRICE_ASC_SORT -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case PRICE_DESC_SORT -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("id"));
            default -> throw new BadRequestException("Unsupported product sort");
        };
    }

    private static String normalize(String sort) {
        return StringUtils.hasText(sort)
                ? sort.trim().toLowerCase(Locale.ROOT)
                : DEFAULT_SORT;
    }
}
