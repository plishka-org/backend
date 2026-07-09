package org.plishka.backend.dto.admin.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Admin user row.")
public record AdminUserDto(
        @Schema(description = "User id.", example = "1")
        Long id,
        @Schema(description = "User display name.", example = "Olena Shevchenko")
        String name,
        @Schema(description = "User email address.", example = "olena@example.com")
        String email,
        @Schema(description = "Optional phone number.", example = "+380501234567", nullable = true)
        String phone,
        @Schema(description = "Whether the user is banned.", example = "false")
        boolean isBanned,
        @Schema(description = "Number of orders placed by the user.", example = "3")
        long numberOfOrders
) {
}
