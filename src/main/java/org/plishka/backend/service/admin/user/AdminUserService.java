package org.plishka.backend.service.admin.user;

import org.plishka.backend.dto.admin.common.BulkOperationResultDto;
import org.plishka.backend.dto.admin.user.AdminUserBulkRequestDto;
import org.plishka.backend.dto.admin.user.AdminUserDto;
import org.plishka.backend.dto.admin.user.AdminUserListRequestDto;
import org.plishka.backend.dto.common.PageResponse;

public interface AdminUserService {
    PageResponse<AdminUserDto> getUsers(AdminUserListRequestDto request, int page, int size);

    PageResponse<AdminUserDto> getBannedUsers(AdminUserListRequestDto request, int page, int size);

    AdminUserDto banUser(Long currentAdminId, Long targetUserId);

    AdminUserDto unbanUser(Long targetUserId);

    BulkOperationResultDto banUsers(Long currentAdminId, AdminUserBulkRequestDto request);

    BulkOperationResultDto unbanUsers(AdminUserBulkRequestDto request);
}
