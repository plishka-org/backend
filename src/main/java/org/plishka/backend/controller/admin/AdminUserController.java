package org.plishka.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.admin.common.BulkOperationResultDto;
import org.plishka.backend.dto.admin.user.AdminUserBulkRequestDto;
import org.plishka.backend.dto.admin.user.AdminUserDto;
import org.plishka.backend.dto.admin.user.AdminUserSearchRequestDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.common.PaginationRequestDto;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.plishka.backend.service.admin.user.AdminUserService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AdminUserService adminUserService;

    @Operation(
            operationId = "adminGetUsers",
            summary = "Admin list users",
            description = "Requires active user with ROLE_ADMIN. Pagination defaults: page=0, size=20. "
                    + "Supported sort values: createdAt,desc | id,desc."
    )
    @GetMapping
    public PageResponse<AdminUserDto> getUsers(
            @ParameterObject @Valid @ModelAttribute AdminUserSearchRequestDto userRequest,
            @ParameterObject @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return adminUserService.getUsers(
                userRequest,
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @Operation(
            operationId = "adminGetBannedUsers",
            summary = "Admin list banned users",
            description = "Requires active user with ROLE_ADMIN. Pagination defaults: page=0, size=20. "
                    + "Supported sort values: createdAt,desc | id,desc."
    )
    @GetMapping("/banned")
    public PageResponse<AdminUserDto> getBannedUsers(
            @ParameterObject @Valid @ModelAttribute AdminUserSearchRequestDto userRequest,
            @ParameterObject @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return adminUserService.getBannedUsers(
                userRequest,
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @Operation(
            operationId = "adminBanUser",
            summary = "Admin ban user",
            description = "Requires active user with ROLE_ADMIN."
    )
    @PutMapping("/{id}/ban")
    public AdminUserDto banUser(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Positive @PathVariable Long id
    ) {
        return adminUserService.banUser(principal.getUserId(), id);
    }

    @Operation(
            operationId = "adminUnbanUser",
            summary = "Admin unban user",
            description = "Requires active user with ROLE_ADMIN."
    )
    @PutMapping("/{id}/unban")
    public AdminUserDto unbanUser(@Positive @PathVariable Long id) {
        return adminUserService.unbanUser(id);
    }

    @Operation(
            operationId = "adminBulkBanUsers",
            summary = "Admin bulk ban users",
            description = "Requires active user with ROLE_ADMIN."
    )
    @PostMapping("/ban/bulk")
    public BulkOperationResultDto banUsers(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody AdminUserBulkRequestDto request
    ) {
        return adminUserService.banUsers(principal.getUserId(), request);
    }

    @Operation(
            operationId = "adminBulkUnbanUsers",
            summary = "Admin bulk unban users",
            description = "Requires active user with ROLE_ADMIN."
    )
    @PostMapping("/unban/bulk")
    public BulkOperationResultDto unbanUsers(@Valid @RequestBody AdminUserBulkRequestDto request) {
        return adminUserService.unbanUsers(request);
    }
}
