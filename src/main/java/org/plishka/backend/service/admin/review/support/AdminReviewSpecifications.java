package org.plishka.backend.service.admin.review.support;

import java.util.Locale;
import org.plishka.backend.domain.review.Review;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class AdminReviewSpecifications {
    private AdminReviewSpecifications() {
    }

    public static Specification<Review> bySearch(String search) {
        if (!StringUtils.hasText(search)) {
            return Specification.unrestricted();
        }

        String searchPattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";

        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("authorName")), searchPattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), searchPattern)
        );
    }
}
