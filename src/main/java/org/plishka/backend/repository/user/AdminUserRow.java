package org.plishka.backend.repository.user;

public record AdminUserRow(
        Long id,
        String name,
        String email,
        String phone,
        boolean isBanned,
        long numberOfOrders
) {
}
