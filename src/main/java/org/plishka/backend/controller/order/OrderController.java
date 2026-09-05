package org.plishka.backend.controller.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.common.PaginationRequestDto;
import org.plishka.backend.dto.order.CreateOrderRequestDto;
import org.plishka.backend.dto.order.OrderDetailDto;
import org.plishka.backend.dto.order.OrderSummaryDto;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.plishka.backend.service.order.OrderCheckoutService;
import org.plishka.backend.service.order.OrderService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final OrderService orderService;
    private final OrderCheckoutService orderCheckoutService;

    @Operation(
            operationId = "getOrder",
            summary = "Get order by id",
            description = "Requires an active user: authenticated, email verified, and not banned."
    )
    @GetMapping("/users/me/orders/{orderId}")
    public OrderDetailDto getOrder(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Positive @PathVariable Long orderId
    ) {
        return orderService.getOrderById(orderId, principal.getUserId());
    }

    @Operation(
            operationId = "getOrderByNumber",
            summary = "Get order by order number",
            description = "Requires an active user: authenticated, email verified, and not banned."
    )
    @GetMapping("/users/me/orders/number/{orderNumber}")
    public OrderDetailDto getOrderByNumber(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable String orderNumber
    ) {
        return orderService.getOrderByOrderNumber(orderNumber, principal.getUserId());
    }

    @Operation(
            operationId = "getUserOrders",
            summary = "List current user orders",
            description = "Requires an active user. Pagination defaults: page=0, size=20."
    )
    @GetMapping("/users/me/orders")
    public PageResponse<OrderSummaryDto> getUserOrders(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @ParameterObject @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return orderService.getUserOrders(
                principal.getUserId(),
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @Operation(
            operationId = "createOrder",
            summary = "Create order",
            description = "Requires an active user and enabled shop mode. "
                    + "Idempotency-Key is required. Same key and same payload returns the existing order; "
                    + "same key with a different payload returns 409. When shop mode is disabled, "
                    + "returns 403 with message \"Shop mode is disabled\"."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "customerName": "Olena Shevchenko",
                      "deliveryCity": "Shevchenka Street, 25",
                      "phone": "+380501234567",
                      "notes": "Call before delivery"
                    }
                    """))
    )
    @PostMapping("/orders")
    public OrderDetailDto createOrder(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Parameter(
                    description = "Required idempotency key. Max length: 64 characters.",
                    example = "order-create-20260706-0001",
                    required = true
            )
            @RequestHeader("Idempotency-Key")
            @NotBlank(message = "Idempotency-Key header must not be blank")
            @Size(max = 64, message = "Idempotency-Key header must not exceed 64 characters")
            String idempotencyKey,
            @Valid @RequestBody CreateOrderRequestDto requestDto
    ) {
        return orderCheckoutService.checkout(principal.getUserId(), idempotencyKey, requestDto);
    }

    @Operation(
            operationId = "repeatOrder",
            summary = "Repeat order",
            description = "Requires an active user and enabled shop mode. "
                    + "Idempotency-Key is required. Same key and same payload returns the existing order; "
                    + "same key with a different payload returns 409. When shop mode is disabled, "
                    + "returns 403 with message \"Shop mode is disabled\"."
    )
    @PostMapping("/users/me/orders/{orderId}/repeat")
    public OrderDetailDto repeatOrder(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Positive @PathVariable Long orderId,
            @Parameter(
                    description = "Required idempotency key. Max length: 64 characters.",
                    example = "order-repeat-20260706-0001",
                    required = true
            )
            @RequestHeader("Idempotency-Key")
            @NotBlank(message = "Idempotency-Key header must not be blank")
            @Size(max = 64, message = "Idempotency-Key header must not exceed 64 characters")
            String idempotencyKey
    ) {
        return orderService.repeatOrder(orderId, principal.getUserId(), idempotencyKey);
    }
}
