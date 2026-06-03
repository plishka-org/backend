package org.plishka.backend.service.cart;

import org.plishka.backend.dto.cart.AddCartItemRequestDto;
import org.plishka.backend.dto.cart.CartSummaryDto;
import org.plishka.backend.dto.cart.UpdateCartItemRequestDto;

public interface CartService {
    CartSummaryDto getCart(Long userId);

    CartSummaryDto addItem(Long userId, AddCartItemRequestDto requestDto);

    CartSummaryDto updateItem(Long userId, Long productId, UpdateCartItemRequestDto requestDto);

    CartSummaryDto removeItem(Long userId, Long productId);

    void clearCart(Long userId);

    CartSummaryDto mergeCart(Long userId, Long sourceCartId);
}
