package org.plishka.backend.dto.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PaginationRequestDto(
        @Min(value = 0, message = "Page must be >= 0")
        @Max(value = 50, message = "Page must be <= 50")
        Integer page,

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
