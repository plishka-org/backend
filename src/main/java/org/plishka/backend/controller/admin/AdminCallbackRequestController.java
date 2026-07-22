package org.plishka.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.admin.callback.AdminCallbackRequestSearchRequestDto;
import org.plishka.backend.dto.callback.CallbackRequestDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.common.PaginationRequestDto;
import org.plishka.backend.service.admin.callback.AdminCallbackRequestService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/callback-requests")
@RequiredArgsConstructor
@Tag(name = "Admin - Callback")
@SecurityRequirement(name = "bearerAuth")
public class AdminCallbackRequestController {
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final AdminCallbackRequestService adminCallbackRequestService;

    @Operation(
            operationId = "adminGetCallbackRequests",
            summary = "Admin list callback requests",
            description = "Requires active user with ROLE_ADMIN. Pagination defaults: page=0, size=10. "
                    + "Supported sort values: createdAt,desc | createdAt,asc."
    )
    @GetMapping
    public PageResponse<CallbackRequestDto> getCallbackRequests(
            @ParameterObject @Valid @ModelAttribute AdminCallbackRequestSearchRequestDto searchRequest,
            @ParameterObject @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return adminCallbackRequestService.getCallbackRequests(
                searchRequest,
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }
}
