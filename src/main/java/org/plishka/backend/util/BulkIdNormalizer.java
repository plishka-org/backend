package org.plishka.backend.util;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.plishka.backend.exception.BadRequestException;

public final class BulkIdNormalizer {
    private BulkIdNormalizer() {
    }

    public static List<Long> normalize(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    public static List<Long> normalizeRequired(
            Collection<Long> ids,
            String requiredMessage,
            String limitMessage,
            int maxIds
    ) {
        List<Long> normalizedIds = normalize(ids);
        if (normalizedIds.isEmpty()) {
            throw new BadRequestException(requiredMessage);
        }

        if (normalizedIds.size() > maxIds) {
            throw new BadRequestException(limitMessage.formatted(maxIds));
        }

        return normalizedIds;
    }
}
