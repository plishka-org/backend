package org.plishka.backend.service.admin.user;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.admin.common.BulkOperationResultDto;
import org.plishka.backend.dto.admin.user.AdminUserBulkRequestDto;
import org.plishka.backend.dto.admin.user.AdminUserDto;
import org.plishka.backend.dto.admin.user.AdminUserListRequestDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.user.AdminUserRow;
import org.plishka.backend.repository.user.RefreshTokenRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.util.BulkIdNormalizer;
import org.plishka.backend.util.EntityPresenceValidator;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {
    private static final int MAX_USER_IDS = 500;
    private static final String USER_ENTITY_NAME = "User";
    private static final String USER_NOT_FOUND_MESSAGE = "User with ID %d not found";
    private static final String USER_IDS_REQUIRED_MESSAGE = "User ids are required";
    private static final String USER_IDS_LIMIT_MESSAGE = "User ids must contain at most %d ids";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserDto> getUsers(AdminUserListRequestDto request, int page, int size) {
        return getUsersPage(request, page, size, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserDto> getBannedUsers(AdminUserListRequestDto request, int page, int size) {
        return getUsersPage(request, page, size, true);
    }

    @Override
    @Transactional
    public AdminUserDto banUser(Long currentAdminId, Long targetUserId) {
        User targetUser = findUserWithRolesForUpdateOrThrow(targetUserId);
        UserBanPolicy.requireCanBan(currentAdminId, targetUser);
        targetUser.setBanned(true);
        refreshTokenRepository.deleteAllByUserId(targetUser.getId());

        return findAdminUserDtoOrThrow(targetUser.getId());
    }

    @Override
    @Transactional
    public AdminUserDto unbanUser(Long targetUserId) {
        User targetUser = findUserWithRolesForUpdateOrThrow(targetUserId);
        targetUser.setBanned(false);

        return findAdminUserDtoOrThrow(targetUser.getId());
    }

    @Override
    @Transactional
    public BulkOperationResultDto banUsers(Long currentAdminId, AdminUserBulkRequestDto request) {
        List<Long> targetUserIds = normalizeBulkUserIds(request.userIds());
        List<User> targetUsers = userRepository.findAllByIdInWithRolesForUpdateOrderById(targetUserIds);
        requireRequestedUsersFound(targetUserIds, targetUsers);
        targetUsers.forEach(user -> UserBanPolicy.requireCanBan(currentAdminId, user));
        targetUsers.forEach(user -> user.setBanned(true));
        refreshTokenRepository.deleteAllByUserIdIn(targetUserIds);

        return new BulkOperationResultDto(targetUsers.size());
    }

    @Override
    @Transactional
    public BulkOperationResultDto unbanUsers(AdminUserBulkRequestDto request) {
        List<Long> targetUserIds = normalizeBulkUserIds(request.userIds());
        List<User> targetUsers = userRepository.findAllByIdInWithRolesForUpdateOrderById(targetUserIds);
        requireRequestedUsersFound(targetUserIds, targetUsers);
        targetUsers.forEach(user -> user.setBanned(false));

        return new BulkOperationResultDto(targetUsers.size());
    }

    private PageResponse<AdminUserDto> getUsersPage(
            AdminUserListRequestDto request,
            int page,
            int size,
            boolean bannedOnly
    ) {
        AdminUserListCriteria criteria = AdminUserListCriteria.from(request, page, size, bannedOnly);
        Page<AdminUserRow> userRowsPage = userRepository.findAdminUsers(
                criteria.search(),
                criteria.bannedOnly(),
                criteria.pageRequest()
        );
        List<AdminUserDto> userDtos = userRowsPage.getContent()
                .stream()
                .map(this::toDto)
                .toList();

        return PageResponse.from(userRowsPage, userDtos);
    }

    private List<Long> normalizeBulkUserIds(List<Long> userIds) {
        return BulkIdNormalizer.normalizeRequired(
                userIds,
                USER_IDS_REQUIRED_MESSAGE,
                USER_IDS_LIMIT_MESSAGE,
                MAX_USER_IDS
        );
    }

    private void requireRequestedUsersFound(Collection<Long> requestedIds, Collection<User> users) {
        EntityPresenceValidator.requireAllIdsFound(
                requestedIds,
                users.stream().map(User::getId).toList(),
                USER_ENTITY_NAME
        );
    }

    private User findUserWithRolesForUpdateOrThrow(Long userId) {
        return userRepository.findByIdWithRolesForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE.formatted(userId)));
    }

    private AdminUserDto findAdminUserDtoOrThrow(Long userId) {
        return userRepository.findAdminUserById(userId)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE.formatted(userId)));
    }

    private AdminUserDto toDto(AdminUserRow row) {
        return new AdminUserDto(
                row.id(),
                row.name(),
                row.email(),
                row.phone(),
                row.isBanned(),
                row.numberOfOrders()
        );
    }
}
