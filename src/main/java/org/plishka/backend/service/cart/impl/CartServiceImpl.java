package org.plishka.backend.service.cart.impl;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.cart.Cart;
import org.plishka.backend.domain.cart.CartItem;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.cart.AddCartItemRequestDto;
import org.plishka.backend.dto.cart.CartItemSummaryDto;
import org.plishka.backend.dto.cart.CartSummaryDto;
import org.plishka.backend.dto.cart.MergeCartRequestDto;
import org.plishka.backend.dto.cart.UpdateCartItemRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.cart.CartItemMapper;
import org.plishka.backend.repository.cart.CartRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.cart.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cart service implementation.
 *
 * <p>Concurrency invariant for cart writes: all mutations of a user's cart must be
 * serialized through a pessimistic lock on the Cart aggregate root.
 *
 * <p>If the cart does not exist yet, the User row must be locked first, then the
 * cart must be looked up again before creating it. A missing Cart row cannot be
 * locked, so two concurrent first-writes would otherwise both insert and collide
 * on the unique constraint.
 *
 * <p>After acquiring the Cart lock, load the full cart aggregate separately
 * (items -&gt; product -&gt; category). Do not combine the write lock with a fetch
 * graph, because the database may lock the joined rows as part of the locking read.
 *
 * <p>Do not mutate cart_items through bulk DML; update them through the managed Cart
 * collection so Hibernate state stays consistent with the lock protocol.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {
    private static final int MAX_ITEM_QUANTITY = 50;

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemMapper cartItemMapper;

    @Override
    @Transactional(readOnly = true)
    public CartSummaryDto getCart(Long userId) {
        log.debug("Fetching cart for user id={}", userId);

        Cart cart = cartRepository.findAggregateByUserId(userId).orElse(null);
        if (cart == null) {
            log.debug("Cart not found for user id={}, returning empty cart", userId);
            return emptyCartSummary();
        }

        log.debug("Successfully fetched cart with {} items for user id={}", cart.getCartItems().size(), userId);
        return toCartSummaryDto(cart);
    }

    @Override
    @Transactional
    public CartSummaryDto addItem(Long userId, AddCartItemRequestDto requestDto) {
        log.debug("Adding product id={} to cart for user id={}, quantity={}",
                requestDto.productId(), userId, requestDto.quantity());

        Cart cart = getOrCreateLockedCartAggregate(userId);
        Product product = findProductOrThrow(requestDto.productId());
        addItemToCart(cart, product, requestDto.quantity());
        cartRepository.save(cart);

        log.debug("Successfully added product to cart for user id={}", userId);
        return toCartSummaryDto(cart);
    }

    @Override
    @Transactional
    public CartSummaryDto updateItem(Long userId, Long productId, UpdateCartItemRequestDto requestDto) {
        log.debug("Updating product id={} in cart for user id={}, new quantity={}",
                productId, userId, requestDto.quantity());

        Cart cart = getLockedCartAggregate(userId);
        CartItem cartItem = findCartItem(cart, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in cart"));
        cartItem.setQuantity(requestDto.quantity());

        log.debug("Successfully updated product quantity in cart for user id={}", userId);
        return toCartSummaryDto(cart);
    }

    @Override
    @Transactional
    public CartSummaryDto removeItem(Long userId, Long productId) {
        log.debug("Removing product id={} from cart for user id={}", productId, userId);

        Cart cart = getLockedCartAggregate(userId);
        boolean removed = cart.getCartItems()
                .removeIf(item -> item.getProduct().getId().equals(productId));
        if (!removed) {
            throw new ResourceNotFoundException("Product not found in cart");
        }

        log.debug("Successfully removed product from cart for user id={}", userId);
        return toCartSummaryDto(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        log.debug("Clearing cart for user id={}", userId);

        Cart cart = getLockedCartAggregate(userId);
        cart.getCartItems().clear();
        cartRepository.save(cart);

        log.debug("Successfully cleared cart for user id={}", userId);
    }

    @Override
    @Transactional
    public CartSummaryDto mergeCart(Long userId, MergeCartRequestDto requestDto) {
        log.debug("Merging client cart with {} item(s) into user id={} cart",
                requestDto.items().size(), userId);

        Cart targetCart = getOrCreateLockedCartAggregate(userId);
        Map<Long, Integer> aggregatedItems = aggregateItemsByProductId(requestDto.items());
        Map<Long, Product> productsById = findProductsByIdOrThrow(aggregatedItems.keySet());

        aggregatedItems.forEach((productId, quantity) ->
                mergeItemToCart(targetCart, productsById.get(productId), quantity)
        );

        cartRepository.save(targetCart);

        log.debug("Successfully merged client cart into user id={} cart", userId);
        return toCartSummaryDto(targetCart);
    }

    private void addItemToCart(Cart cart, Product product, int quantityToAdd) {
        findCartItem(cart, product.getId()).ifPresentOrElse(
                existingItem -> existingItem.setQuantity(
                        calculateUpdatedQuantity(existingItem.getQuantity(), quantityToAdd)),
                () -> cart.getCartItems().add(createCartItem(cart, product, quantityToAdd))
        );
    }

    private void mergeItemToCart(Cart cart, Product product, int quantityToAdd) {
        findCartItem(cart, product.getId()).ifPresentOrElse(
                existingItem -> existingItem.setQuantity(capMergedQuantity(
                        existingItem.getQuantity() + quantityToAdd)),
                () -> cart.getCartItems().add(createCartItem(
                        cart, product, capMergedQuantity(quantityToAdd)))
        );
    }

    private int calculateUpdatedQuantity(int currentQuantity, int quantityToAdd) {
        int updatedQuantity = currentQuantity + quantityToAdd;
        if (updatedQuantity > MAX_ITEM_QUANTITY) {
            throw new BadRequestException("Maximum quantity per item is " + MAX_ITEM_QUANTITY);
        }
        return updatedQuantity;
    }

    private int capMergedQuantity(int quantity) {
        return Math.min(quantity, MAX_ITEM_QUANTITY);
    }

    private Optional<CartItem> findCartItem(Cart cart, Long productId) {
        return cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
    }

    private Map<Long, Integer> aggregateItemsByProductId(List<AddCartItemRequestDto> items) {
        Map<Long, Integer> aggregated = new LinkedHashMap<>();
        for (AddCartItemRequestDto item : items) {
            aggregated.merge(item.productId(), item.quantity(), Integer::sum);
        }
        return aggregated;
    }

    private Map<Long, Product> findProductsByIdOrThrow(Set<Long> productIds) {
        List<Product> products = productRepository.findAllByIdIn(productIds);
        if (products.size() != productIds.size()) {
            Set<Long> foundIds = products.stream()
                    .map(Product::getId)
                    .collect(Collectors.toSet());
            Long missingProductId = productIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .orElseThrow();
            throw new ResourceNotFoundException("Product with ID " + missingProductId + " not found");
        }
        return products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private CartItem createCartItem(Cart cart, Product product, int quantity) {
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        return cartItem;
    }

    private CartSummaryDto toCartSummaryDto(Cart cart) {
        List<CartItemSummaryDto> items = cart.getCartItems().stream()
                .map(cartItemMapper::toSummaryDto)
                .toList();
        return new CartSummaryDto(items, calculateTotalPrice(items));
    }

    private CartSummaryDto emptyCartSummary() {
        return new CartSummaryDto(List.of(), BigDecimal.ZERO);
    }

    private Cart getLockedCartAggregate(Long userId) {
        cartRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id=" + userId));
        return loadCartAggregateOrThrow(userId);
    }

    private Cart getOrCreateLockedCartAggregate(Long userId) {
        ensureCartExistsWithLock(userId);
        return loadCartAggregateOrThrow(userId);
    }

    private void ensureCartExistsWithLock(Long userId) {
        if (cartRepository.findByUserIdForUpdate(userId).isPresent()) {
            return;
        }

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found"));
        if (cartRepository.findByUserIdForUpdate(userId).isEmpty()) {
            createNewCart(user);
        }
    }

    private void createNewCart(User user) {
        log.debug("Creating new cart for user id={}", user.getId());
        Cart newCart = new Cart();
        newCart.setUser(user);
        cartRepository.save(newCart);
    }

    private Cart loadCartAggregateOrThrow(Long userId) {
        return cartRepository.findAggregateByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id=" + userId));
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found"));
    }

    private BigDecimal calculateTotalPrice(List<CartItemSummaryDto> items) {
        return items.stream()
                .map(CartItemSummaryDto::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
