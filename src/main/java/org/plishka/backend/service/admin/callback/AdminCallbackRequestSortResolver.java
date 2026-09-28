package org.plishka.backend.service.admin.callback;

import java.util.Locale;
import org.plishka.backend.exception.BadRequestException;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

final class AdminCallbackRequestSortResolver {
    private static final String CREATED_AT_ASC_SORT = "createdat,asc";
    private static final String CREATED_AT_DESC_SORT = "createdat,desc";

    private AdminCallbackRequestSortResolver() {
    }

    static Sort resolveSort(String sort) {
        return switch (normalizeSortValue(sort)) {
            case CREATED_AT_ASC_SORT -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
            case CREATED_AT_DESC_SORT -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            default -> throw new BadRequestException("Unsupported callback request sort");
        };
    }

    private static String normalizeSortValue(String sort) {
        return StringUtils.hasText(sort) ? sort.trim().toLowerCase(Locale.ROOT) : CREATED_AT_DESC_SORT;
    }
}
