package org.plishka.backend.service.admin.order;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.util.LikePatternEscaper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

final class AdminOrderSpecifications {
    private AdminOrderSpecifications() {
    }

    static Specification<Order> forSearchQuery(String searchQuery) {
        if (!StringUtils.hasText(searchQuery)) {
            return Specification.unrestricted();
        }

        String normalizedSearchQuery = searchQuery.trim().toLowerCase(Locale.ROOT);
        Optional<Long> exactTotalPrice = parseExactTotalPrice(normalizedSearchQuery);
        return (root, ignoredQuery, criteriaBuilder) -> {
            List<Predicate> searchPredicates = new ArrayList<>();
            String searchPattern = LikePatternEscaper.containsPattern(normalizedSearchQuery);
            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("orderNumber")),
                    searchPattern,
                    LikePatternEscaper.ESCAPE_CHARACTER
            ));
            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("customerName")),
                    searchPattern,
                    LikePatternEscaper.ESCAPE_CHARACTER
            ));
            exactTotalPrice.ifPresent(
                    value -> searchPredicates.add(criteriaBuilder.equal(root.get("totalPrice"), value))
            );
            return criteriaBuilder.or(searchPredicates.toArray(Predicate[]::new));
        };
    }

    private static Optional<Long> parseExactTotalPrice(String searchQuery) {
        try {
            long value = Long.parseLong(searchQuery);
            return value >= 0 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
