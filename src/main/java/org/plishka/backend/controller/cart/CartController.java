package org.plishka.backend.controller.cart;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.cart.AddCartItemRequestDto;
import org.plishka.backend.dto.cart.CartSummaryDto;
import org.plishka.backend.dto.cart.MergeCartRequestDto;
import org.plishka.backend.dto.cart.UpdateCartItemRequestDto;
import org.plishka.backend.security.AuthenticatedUserPrincipal;
import org.plishka.backend.service.cart.CartService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Cart")
@SecurityRequirement(name = "bearerAuth")
public class CartController {
    private final CartService cartService;

    @Operation(
            operationId = "getCart",
            summary = "Get cart",
            description = "Requires an active user and enabled shop mode. "
                    + "When shop mode is disabled, returns 403 with message \"Shop mode is disabled\"."
    )
    @GetMapping
    public CartSummaryDto getCart(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return cartService.getCart(principal.getUserId());
    }

    @Operation(
            operationId = "addCartItem",
            summary = "Add item to cart",
            description = "Requires an active user and enabled shop mode. "
                    + "When shop mode is disabled, returns 403 with message \"Shop mode is disabled\"."
    )
    @PostMapping("/items")
    public CartSummaryDto addItem(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody AddCartItemRequestDto requestDto
    ) {
        return cartService.addItem(principal.getUserId(), requestDto);
    }

    @Operation(
            operationId = "updateCartItem",
            summary = "Update cart item quantity",
            description = "Requires an active user and enabled shop mode. "
                    + "When shop mode is disabled, returns 403 with message \"Shop mode is disabled\"."
    )
    @PutMapping("/items/{productId}")
    public CartSummaryDto updateItem(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Positive @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequestDto requestDto
    ) {
        return cartService.updateItem(principal.getUserId(), productId, requestDto);
    }

    @Operation(
            operationId = "removeCartItem",
            summary = "Remove item from cart",
            description = "Requires an active user and enabled shop mode. "
                    + "When shop mode is disabled, returns 403 with message \"Shop mode is disabled\"."
    )
    @DeleteMapping("/items/{productId}")
    public CartSummaryDto removeItem(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Positive @PathVariable Long productId
    ) {
        return cartService.removeItem(principal.getUserId(), productId);
    }

    @Operation(
            operationId = "clearCart",
            summary = "Clear cart",
            description = "Requires an active user and enabled shop mode. "
                    + "When shop mode is disabled, returns 403 with message \"Shop mode is disabled\"."
    )
    @DeleteMapping
    public void clearCart(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        cartService.clearCart(principal.getUserId());
    }

    @Operation(
            operationId = "mergeCart",
            summary = "Merge cart items",
            description = "Requires an active user and enabled shop mode. "
                    + "When shop mode is disabled, returns 403 with message \"Shop mode is disabled\"."
    )
    @PostMapping("/merge")
    public CartSummaryDto mergeCart(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody MergeCartRequestDto requestDto
    ) {
        return cartService.mergeCart(principal.getUserId(), requestDto);
    }
}
