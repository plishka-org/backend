package org.plishka.backend.dto.common;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Pagination query parameters.")
public record PaginationRequestDto(
        @Parameter(description = "Zero-based page number. Defaults to 0 when omitted.", example = "0")
        @Schema(nullable = true)
        @Min(value = 0, message = "Page must be >= 0")
        @Max(value = 50, message = "Page must be <= 50")
        Integer page,

        @Parameter(description = "Page size. Default depends on endpoint; see operation description.", example = "16")
        @Schema(nullable = true)
        @Min(value = 1, message = "Size must be >= 1")
        @Max(value = 100, message = "Size must be <= 100")
        Integer size
) {
    public int resolvePage() {
        return page == null ? 0 : page;
    }

    public int resolveSize(int defaultSize) {
        return size == null ? defaultSize : size;
    }
}
