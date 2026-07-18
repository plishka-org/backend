package org.plishka.backend.dto.admin.user;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Admin user search query parameters.")
public record AdminUserSearchRequestDto(
        @Parameter(description = "Search text.", example = "olena")
        @Schema(nullable = true)
        @Size(max = 100, message = "Search must contain at most 100 characters")
        String search,

        @Parameter(description = "Sort value: createdAt,desc | id,desc.", example = "createdAt,desc")
        @Schema(description = "Sort value: createdAt,desc | id,desc.", example = "createdAt,desc", nullable = true)
        String sort
) {
}
