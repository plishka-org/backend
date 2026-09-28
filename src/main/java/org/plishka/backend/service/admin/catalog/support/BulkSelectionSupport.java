package org.plishka.backend.service.admin.catalog.support;

import java.util.List;
import org.plishka.backend.dto.admin.common.SelectionMode;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.util.BulkIdNormalizer;

public final class BulkSelectionSupport {
    private BulkSelectionSupport() {
    }

    public static List<Long> normalizeIdsForSelection(
            SelectionMode selectionMode,
            List<Long> ids,
            String fieldName,
            int maxSelectedIds
    ) {
        requireSelectionMode(selectionMode);
        List<Long> normalizedIds = BulkIdNormalizer.normalize(ids);

        if (selectionMode == SelectionMode.SELECTED && normalizedIds.isEmpty()) {
            throw new BadRequestException(fieldName + " are required for SELECTED selection mode");
        }

        if (selectionMode == SelectionMode.SELECTED && normalizedIds.size() > maxSelectedIds) {
            throw new BadRequestException(fieldName + " must contain at most " + maxSelectedIds + " ids");
        }

        return normalizedIds;
    }

    private static void requireSelectionMode(SelectionMode selectionMode) {
        if (selectionMode == null) {
            throw new BadRequestException("Selection mode is required");
        }
    }
}
