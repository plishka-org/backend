package org.plishka.backend.service.cart.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.cart.Cart;
import org.plishka.backend.domain.cart.CartItem;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.dto.cart.AddCartItemRequestDto;
import org.plishka.backend.dto.cart.CartItemSummaryDto;
import org.plishka.backend.dto.cart.CartSummaryDto;
import org.plishka.backend.dto.cart.UpdateCartItemRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.cart.CartItemMapper;
import org.plishka.backend.repository.cart.CartItemRepository;
import org.plishka.backend.repository.cart.CartRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.cart.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {
    private static final int MAX_ITEM_QUANTITY = 1000;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemMapper cartItemMapper;

    @Override
    @Transactional(readOnly = true)
    public CartSummaryDto getCart(Long userId) {
        log.debug("Fetching cart for user id={}", userId);

        Cart cart = findCartByUserIdOrNull(userId);
        if (cart == null) {
            log.debug("Cart not found for user id={}, returning empty cart", userId);
            return new CartSummaryDto(List.of(), BigDecimal.ZERO);
        }

        List<CartItemSummaryDto> items = toCartItemSummaries(cart.getCartItems());
        BigDecimal totalPrice = calculateTotalPrice(items);

        log.debug("Successfully fetched cart with {} items for user id={}", items.size(), userId);

        return new CartSummaryDto(items, totalPrice);
    }

    @Override
    @Transactional
    public CartSummaryDto addItem(Long userId, AddCartItemRequestDto requestDto) {
        log.debug("Adding product id={} to cart for user id={}, quantity={}", 
                requestDto.productId(), userId, requestDto.quantity());

        Cart cart = findOrCreateCart(userId);
        Product product = findProductOrThrow(requestDto.productId());
        
        findCartItemByProductAndUpdate(cart, product, requestDto.quantity());
        log.debug("Successfully added product to cart for user id={}", userId);

        return getCart(userId);
    }

    @Override
    @Transactional
    public CartSummaryDto updateItem(Long userId, Long productId, UpdateCartItemRequestDto requestDto) {
        log.debug("Updating product id={} in cart for user id={}, new quantity={}", 
                productId, userId, requestDto.quantity());

        Cart cart = findCartByUserIdOrThrow(userId);
        CartItem cartItem = findCartItemByCartAndProductOrThrow(cart.getId(), productId);

        cartItem.setQuantity(requestDto.quantity());
        cartItemRepository.save(cartItem);

        log.debug("Successfully updated product quantity in cart for user id={}", userId);

        return getCart(userId);
    }

    @Override
    @Transactional
    public CartSummaryDto removeItem(Long userId, Long productId) {
        log.debug("Removing product id={} from cart for user id={}", productId, userId);

        Cart cart = findCartByUserIdOrThrow(userId);
        CartItem cartItem = findCartItemByCartAndProductOrThrow(cart.getId(), productId);

        cart.getCartItems().remove(cartItem);
        cartRepository.save(cart);

        log.debug("Successfully removed product from cart for user id={}", userId);

        return getCart(userId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        log.debug("Clearing cart for user id={}", userId);

        Cart cart = findCartByUserIdOrThrow(userId);
        cart.getCartItems().clear();
        cartRepository.save(cart);

        log.debug("Successfully cleared cart for user id={}", userId);
    }

    @Override
    @Transactional
    public CartSummaryDto mergeCart(Long userId, String sourceCartToken) {
        log.debug("Merging source cart into user id={} cart", userId);

        Cart targetCart = findOrCreateCart(userId);
        Cart sourceCart = findCartByMergeTokenOrThrow(sourceCartToken);
        validateCartCanBeMerged(sourceCart, targetCart, userId);

        sourceCart.getCartItems().forEach(sourceItem ->
                mergeCartItem(targetCart, sourceItem)
        );

        cartRepository.save(targetCart);
        cartRepository.delete(sourceCart);

        log.debug("Successfully merged source cart into user id={} cart", userId);

        return getCart(userId);
    }

    private void findCartItemByProductAndUpdate(Cart cart, Product product, int quantityToAdd) {
        cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .ifPresentOrElse(
                    cartItem -> {
                        log.debug("Cart item already exists, updating quantity");
                        cartItem.setQuantity(calculateUpdatedQuantity(cartItem.getQuantity(), quantityToAdd));
                    },
                    () -> {
                        log.debug("Creating new cart item");
                        CartItem newCartItem = createCartItem(cart, product, quantityToAdd);
                        cart.getCartItems().add(newCartItem);
                    }
        );
        cartRepository.save(cart);
    }

    private void mergeCartItem(Cart targetCart, CartItem sourceItem) {
        targetCart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(sourceItem.getProduct().getId()))
                .findFirst()
                .ifPresentOrElse(
                    existingItem -> {
                        log.debug("Product already in target cart, updating quantity");
                        existingItem.setQuantity(calculateUpdatedQuantity(
                                existingItem.getQuantity(),
                                sourceItem.getQuantity()
                        ));
                    },
                    () -> {
                        log.debug("Adding new item to target cart");
                        CartItem newItem = createCartItem(targetCart, sourceItem.getProduct(),
                                sourceItem.getQuantity());
                        targetCart.getCartItems().add(newItem);
                    }
        );
    }

    private CartItem createCartItem(Cart cart, Product product, int quantity) {
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        cartItem.setUnitPrice(product.getPrice());
        return cartItem;
    }

    private int calculateUpdatedQuantity(int currentQuantity, int quantityToAdd) {
        int updatedQuantity = currentQuantity + quantityToAdd;
        if (updatedQuantity > MAX_ITEM_QUANTITY) {
            throw new BadRequestException("Maximum quantity per item is " + MAX_ITEM_QUANTITY);
        }

        return updatedQuantity;
    }

    private List<CartItemSummaryDto> toCartItemSummaries(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(cartItemMapper::toSummaryDto)
                .toList();
    }

    private Cart findOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCart(userId));
    }

    private Cart createNewCart(Long userId) {
        log.debug("Creating new cart for user id={}", userId);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found"));
        
        Cart newCart = new Cart();
        newCart.setUser(user);
        newCart.setMergeToken(generateMergeToken());
        return cartRepository.save(newCart);
    }

    private Cart findCartByUserIdOrNull(Long userId) {
        return cartRepository.findByUserId(userId).orElse(null);
    }

    private Cart findCartByUserIdOrThrow(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id=" + userId));
    }

    private Cart findCartByMergeTokenOrThrow(String mergeToken) {
        return cartRepository.findByMergeToken(mergeToken)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
    }

    private CartItem findCartItemByCartAndProductOrThrow(Long cartId, Long productId) {
        return cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in cart"));
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found"));
    }

    private void validateCartCanBeMerged(Cart sourceCart, Cart targetCart, Long userId) {
        if (sourceCart.getId().equals(targetCart.getId())) {
            throw new BadRequestException("Cannot merge cart into itself");
        }

        if (sourceCart.getUser() != null && !sourceCart.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Cart not found");
        }

        if (sourceCart.getUser() != null) {
            throw new BadRequestException("Source cart already belongs to current user");
        }
    }

    private String generateMergeToken() {
        return UUID.randomUUID().toString();
    }

    private BigDecimal calculateTotalPrice(List<CartItemSummaryDto> items) {
        return items.stream()
                .map(CartItemSummaryDto::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
