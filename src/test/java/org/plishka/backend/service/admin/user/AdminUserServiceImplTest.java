package org.plishka.backend.service.admin.user;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.user.Role;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.admin.user.AdminUserBulkRequestDto;
import org.plishka.backend.dto.admin.user.AdminUserDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ForbiddenException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.user.AdminUserRow;
import org.plishka.backend.repository.user.RefreshTokenRepository;
import org.plishka.backend.repository.user.UserRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {
    private static final long ADMIN_ID = 1L;
    private static final long USER_ID = 2L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AdminUserServiceImpl service;

    @Test
    void banUser_ShouldRejectSelfBan() {
        User admin = user(ADMIN_ID, Role.RoleName.ADMIN);
        when(userRepository.findByIdWithRolesForUpdate(ADMIN_ID)).thenReturn(Optional.of(admin));

        assertThrows(ForbiddenException.class, () -> service.banUser(ADMIN_ID, ADMIN_ID));

        verify(refreshTokenRepository, never()).deleteAllByUserId(ADMIN_ID);
    }

    @Test
    void banUser_ShouldRejectAnotherAdmin() {
        User targetAdmin = user(USER_ID, Role.RoleName.ADMIN);
        when(userRepository.findByIdWithRolesForUpdate(USER_ID)).thenReturn(Optional.of(targetAdmin));

        assertThrows(ForbiddenException.class, () -> service.banUser(ADMIN_ID, USER_ID));

        verify(refreshTokenRepository, never()).deleteAllByUserId(USER_ID);
    }

    @Test
    void banUser_ShouldSetBannedAndDeleteRefreshTokens() {
        User user = user(USER_ID, Role.RoleName.USER);
        when(userRepository.findByIdWithRolesForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findAdminUserById(USER_ID)).thenReturn(Optional.of(adminUserRow(true)));

        AdminUserDto response = service.banUser(ADMIN_ID, USER_ID);

        assertTrue(user.isBanned());
        assertTrue(response.isBanned());
        verify(refreshTokenRepository).deleteAllByUserId(USER_ID);
    }

    @Test
    void bulkBan_ShouldThrowBeforeMutating_WhenUserIsMissing() {
        User user = user(USER_ID, Role.RoleName.USER);
        when(userRepository.findAllByIdInWithRolesForUpdateOrderById(List.of(USER_ID, 3L)))
                .thenReturn(List.of(user));

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.banUsers(ADMIN_ID, new AdminUserBulkRequestDto(List.of(USER_ID, 3L)))
        );

        assertFalse(user.isBanned());
        verify(refreshTokenRepository, never()).deleteAllByUserIdIn(List.of(USER_ID, 3L));
    }

    @Test
    void bulkBan_ShouldRejectNullUserIds() {
        assertThrows(
                BadRequestException.class,
                () -> service.banUsers(ADMIN_ID, new AdminUserBulkRequestDto(Arrays.asList((Long) null)))
        );

        verify(userRepository, never()).findAllByIdInWithRolesForUpdateOrderById(List.of());
    }

    private static AdminUserRow adminUserRow(boolean banned) {
        return new AdminUserRow(USER_ID, "Serhii", "serhii@example.com", "+380501234567", banned, 0);
    }

    private static User user(Long id, Role.RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setId(id);
        user.setRoles(new HashSet<>(Set.of(role)));
        return user;
    }
}
