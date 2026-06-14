package org.plishka.backend.controller.order;

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
public class OrderController {
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final OrderService orderService;
    private final OrderCheckoutService orderCheckoutService;

    @GetMapping("/users/me/orders/{orderId}")
    public OrderDetailDto getOrder(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Positive @PathVariable Long orderId
    ) {
        return orderService.getOrderById(orderId, principal.getUserId());
    }

    @GetMapping("/users/me/orders/number/{orderNumber}")
    public OrderDetailDto getOrderByNumber(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable String orderNumber
    ) {
        return orderService.getOrderByOrderNumber(orderNumber, principal.getUserId());
    }

    @GetMapping("/users/me/orders")
    public PageResponse<OrderSummaryDto> getUserOrders(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return orderService.getUserOrders(
                principal.getUserId(),
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @PostMapping("/orders")
    public OrderDetailDto createOrder(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestHeader("Idempotency-Key")
            @NotBlank(message = "Idempotency-Key header must not be blank")
            @Size(max = 64, message = "Idempotency-Key header must not exceed 64 characters")
            String idempotencyKey,
            @Valid @RequestBody CreateOrderRequestDto requestDto
    ) {
        return orderCheckoutService.checkout(principal.getUserId(), idempotencyKey, requestDto);
    }

    @PostMapping("/users/me/orders/{orderId}/repeat")
    public OrderDetailDto repeatOrder(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Positive @PathVariable Long orderId
    ) {
        return orderService.repeatOrder(orderId, principal.getUserId());
    }
}
