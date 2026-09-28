package org.plishka.backend.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "Page response.")
public record PageResponse<T>(
        @Schema(description = "Page content.")
        List<T> content,
        @Schema(description = "Zero-based page number.", example = "0")
        int pageNumber,
        @Schema(description = "Resolved page size.", example = "16")
        int pageSize,
        @Schema(description = "Total matching elements.", example = "42")
        long totalElements,
        @Schema(description = "Total pages.", example = "3")
        int totalPages,
        @Schema(description = "Whether this is the last page.", example = "false")
        boolean last
) {
    public static <T> PageResponse<T> from(Page<?> page, List<T> content) {
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
