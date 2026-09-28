package org.plishka.backend.service.admin.order;

import java.util.Locale;
import org.plishka.backend.exception.BadRequestException;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

final class AdminOrderSortResolver {
    private static final String CREATED_AT_ASC_SORT = "createdat,asc";
    private static final String CREATED_AT_DESC_SORT = "createdat,desc";
    private static final String TOTAL_PRICE_ASC_SORT = "totalprice,asc";
    private static final String TOTAL_PRICE_DESC_SORT = "totalprice,desc";
    private static final String DEFAULT_SORT = CREATED_AT_DESC_SORT;

    private AdminOrderSortResolver() {
    }

    static Sort resolveSort(String sort) {
        return switch (normalizeSortValue(sort)) {
            case CREATED_AT_ASC_SORT -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
            case CREATED_AT_DESC_SORT -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            case TOTAL_PRICE_ASC_SORT -> Sort.by(Sort.Order.asc("totalPrice"), Sort.Order.asc("id"));
            case TOTAL_PRICE_DESC_SORT -> Sort.by(Sort.Order.desc("totalPrice"), Sort.Order.desc("id"));
            default -> throw new BadRequestException("Unsupported order sort");
        };
    }

    private static String normalizeSortValue(String sort) {
        return StringUtils.hasText(sort) ? sort.trim().toLowerCase(Locale.ROOT) : DEFAULT_SORT;
    }
}
