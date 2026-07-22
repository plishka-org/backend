package org.plishka.backend.dto.admin.callback;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Admin callback request list query parameters.")
public record AdminCallbackRequestSearchRequestDto(
        @Parameter(description = "Requester name or phone search.", example = "050")
        @Schema(nullable = true)
        @Size(max = 100, message = "Search must contain at most 100 characters")
        String search,

        @Parameter(description = "Sort value: createdAt,desc | createdAt,asc.", example = "createdAt,desc")
        @Schema(nullable = true)
        String sort
) {
}
