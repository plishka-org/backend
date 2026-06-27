package org.plishka.backend.dto.admin.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminUserBulkRequestDto(
        @NotEmpty(message = "User ids are required")
        @Size(max = 500, message = "User selection must contain at most 500 ids")
        List<@NotNull @Positive Long> userIds
) {
}
