package org.plishka.backend.service.admin.user;

import java.util.Locale;
import org.plishka.backend.dto.admin.user.AdminUserSearchRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

record AdminUserListCriteria(String search, boolean bannedOnly, PageRequest pageRequest) {
    private static final String CREATED_AT_DESC_SORT = "createdat,desc";
    private static final String ID_DESC_SORT = "id,desc";

    static AdminUserListCriteria from(
            AdminUserSearchRequestDto request,
            int page,
            int size,
            boolean bannedOnly
    ) {
        return new AdminUserListCriteria(
                normalizeSearch(request.search()),
                bannedOnly,
                PageRequest.of(page, size, resolveSort(request.sort()))
        );
    }

    private static Sort resolveSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        }

        String normalizedSort = sort.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedSort) {
            case CREATED_AT_DESC_SORT -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            case ID_DESC_SORT -> Sort.by(Sort.Order.desc("id"));
            default -> throw new BadRequestException("Unsupported user sort");
        };
    }

    private static String normalizeSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }

        return "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
