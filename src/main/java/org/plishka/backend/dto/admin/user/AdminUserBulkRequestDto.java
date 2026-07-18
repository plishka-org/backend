package org.plishka.backend.dto.admin.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Admin user bulk operation request.")
public record AdminUserBulkRequestDto(
        @Schema(description = "User ids. Maximum 500.", example = "[1, 2, 3]")
        @NotEmpty(message = "User ids are required")
        @Size(max = 500, message = "User selection must contain at most 500 ids")
        List<@NotNull @Positive Long> userIds
) {
}
