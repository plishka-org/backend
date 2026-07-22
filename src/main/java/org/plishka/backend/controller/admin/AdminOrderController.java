package org.plishka.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.admin.order.AdminOrderDetailDto;
import org.plishka.backend.dto.admin.order.AdminOrderSearchRequestDto;
import org.plishka.backend.dto.admin.order.AdminOrderSummaryDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.common.PaginationRequestDto;
import org.plishka.backend.service.admin.order.AdminOrderService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin - Orders")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final AdminOrderService adminOrderService;

    @Operation(
            operationId = "adminGetOrders",
            summary = "Admin list orders",
            description = "Requires active user with ROLE_ADMIN. Pagination defaults: page=0, size=10. "
                    + "Supported sort values: createdAt,desc | createdAt,asc | totalPrice,desc | totalPrice,asc."
    )
    @GetMapping
    public PageResponse<AdminOrderSummaryDto> getOrders(
            @ParameterObject @Valid @ModelAttribute AdminOrderSearchRequestDto searchRequest,
            @ParameterObject @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return adminOrderService.getOrders(
                searchRequest,
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @Operation(
            operationId = "adminGetOrder",
            summary = "Admin get order",
            description = "Requires active user with ROLE_ADMIN."
    )
    @GetMapping("/{id}")
    public AdminOrderDetailDto getOrder(@Positive @PathVariable Long id) {
        return adminOrderService.getOrder(id);
    }
}
