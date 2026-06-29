package org.plishka.backend.dto.admin.user;

import jakarta.validation.constraints.Size;

public record AdminUserListRequestDto(
        @Size(max = 100, message = "Search must contain at most 100 characters")
        String search,

        String sort
) {
}
