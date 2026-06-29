package org.plishka.backend.dto.admin.user;

public record AdminUserDto(
        Long id,
        String name,
        String email,
        String phone,
        boolean isBanned,
        long numberOfOrders
) {
}
