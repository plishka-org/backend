package org.plishka.backend.util;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.plishka.backend.exception.ResourceNotFoundException;

public final class EntityPresenceValidator {
    private EntityPresenceValidator() {
    }

    public static void requireAllIdsFound(
            Collection<Long> requestedIds,
            Collection<Long> foundIds,
            String entityName
    ) {
        Set<Long> foundIdSet = Set.copyOf(foundIds);
        List<Long> missingIds = requestedIds.stream()
                .filter(id -> !foundIdSet.contains(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException(entityName + " with ID " + missingIds.getFirst() + " not found");
        }
    }
}
