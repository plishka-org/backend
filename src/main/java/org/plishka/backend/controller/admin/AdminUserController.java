package org.plishka.backend.controller.admin;

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
public class AdminUserController {
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AdminUserService adminUserService;

    @GetMapping
    public PageResponse<AdminUserDto> getUsers(
            @Valid @ModelAttribute AdminUserSearchRequestDto userRequest,
            @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return adminUserService.getUsers(
                userRequest,
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @GetMapping("/banned")
    public PageResponse<AdminUserDto> getBannedUsers(
            @Valid @ModelAttribute AdminUserSearchRequestDto userRequest,
            @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return adminUserService.getBannedUsers(
                userRequest,
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @PutMapping("/{id}/ban")
    public AdminUserDto banUser(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Positive @PathVariable Long id
    ) {
        return adminUserService.banUser(principal.getUserId(), id);
    }

    @PutMapping("/{id}/unban")
    public AdminUserDto unbanUser(@Positive @PathVariable Long id) {
        return adminUserService.unbanUser(id);
    }

    @PostMapping("/ban/bulk")
    public BulkOperationResultDto banUsers(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody AdminUserBulkRequestDto request
    ) {
        return adminUserService.banUsers(principal.getUserId(), request);
    }

    @PostMapping("/unban/bulk")
    public BulkOperationResultDto unbanUsers(@Valid @RequestBody AdminUserBulkRequestDto request) {
        return adminUserService.unbanUsers(request);
    }
}
