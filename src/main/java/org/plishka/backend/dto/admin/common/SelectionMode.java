package org.plishka.backend.dto.admin.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bulk selection mode. SELECTED means productIds only; EXCEPT_SELECTED means all matching "
        + "filters except productIds.")
public enum SelectionMode {
    SELECTED,
    EXCEPT_SELECTED
}
