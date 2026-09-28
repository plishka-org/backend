package org.plishka.backend.dto.admin.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bulk operation result.")
public record BulkOperationResultDto(
        @Schema(description = "Number of affected records.", example = "12")
        int affectedCount
) {
}
