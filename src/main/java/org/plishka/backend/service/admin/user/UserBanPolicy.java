package org.plishka.backend.service.admin.user;

import org.plishka.backend.domain.user.Role;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.exception.ForbiddenException;

final class UserBanPolicy {
    private UserBanPolicy() {
    }

    static void requireCanBan(Long currentAdminId, User targetUser) {
        if (targetUser.getId().equals(currentAdminId)) {
            throw new ForbiddenException("Admin cannot ban himself");
        }

        if (hasAdminRole(targetUser)) {
            throw new ForbiddenException("Admin cannot ban another admin");
        }
    }

    private static boolean hasAdminRole(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(Role.RoleName.ADMIN::equals);
    }
}
