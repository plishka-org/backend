package org.plishka.backend.service.order.impl;

import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.cart.Cart;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.domain.order.OrderItem;
import org.plishka.backend.dto.order.CreateOrderRequestDto;
import org.plishka.backend.dto.order.OrderDetailDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.order.OrderMapper;
import org.plishka.backend.repository.cart.CartRepository;
import org.plishka.backend.repository.order.OrderRepository;
import org.plishka.backend.service.order.OrderCheckoutService;
import org.plishka.backend.service.order.OrderIdempotencyGuard;
import org.plishka.backend.service.order.OrderItemFactory;
import org.plishka.backend.service.order.OrderNumberGenerator;
import org.plishka.backend.util.RequestHashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order checkout service implementation.
 *
 * <p>Checkout participates in the cart write lock protocol described in
 * {@code CartServiceImpl}. Creating an order consumes the current cart state and
 * clears cart_items, so checkout must acquire the same Cart aggregate lock as
 * CartServiceImpl before reading items or clearing the cart.
 *
 * <p>Keep the lock acquisition and aggregate loading strategy aligned with
 * CartServiceImpl: lock the Cart root first, then load items -&gt; product -&gt;
 * category separately.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCheckoutServiceImpl implements OrderCheckoutService {
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderIdempotencyGuard orderIdempotencyGuard;
    private final OrderItemFactory orderItemFactory;

    @Override
    @Transactional
    public OrderDetailDto checkout(Long userId, String idempotencyKey, CreateOrderRequestDto requestDto) {
        log.debug("Starting checkout for user id={}", userId);

        Cart cart = findLockedCartAggregateOrThrow(userId);

        String requestHash = calculateRequestHash(requestDto);
        Optional<Order> existingOrder = orderIdempotencyGuard.findExistingOrder(userId, idempotencyKey, requestHash);
        if (existingOrder.isPresent()) {
            log.debug("Returning existing order id={} for idempotent retry by user id={}",
                    existingOrder.get().getId(), userId);
            return orderMapper.toDetailDto(existingOrder.get());
        }

        validateCartNotEmpty(cart);

        Order order = createOrderFromCart(cart, requestDto);
        order.setIdempotencyKey(idempotencyKey);
        order.setRequestHash(requestHash);
        Order savedOrder = orderRepository.saveAndFlush(order);

        clearCart(cart);

        log.debug("Order created with id={} for user id={}", savedOrder.getId(), userId);

        return orderMapper.toDetailDto(savedOrder);
    }

    private Order createOrderFromCart(Cart cart, CreateOrderRequestDto requestDto) {
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setOrderNumber(orderNumberGenerator.generate());
        order.setCustomerName(requestDto.customerName());
        order.setDeliveryCity(requestDto.deliveryCity());
        order.setPhone(requestDto.phone());
        order.setNotes(requestDto.notes());

        cart.getCartItems().forEach(cartItem -> {
            OrderItem orderItem = orderItemFactory.create(order, cartItem.getProduct(), cartItem.getQuantity());
            order.getOrderItems().add(orderItem);
        });
        order.setTotalPrice(calculateTotalPrice(order));

        return order;
    }

    private String calculateRequestHash(CreateOrderRequestDto requestDto) {
        return RequestHashUtil.hash(
                requestDto.customerName(),
                requestDto.deliveryCity(),
                requestDto.phone(),
                requestDto.notes()
        );
    }

    private void validateCartNotEmpty(Cart cart) {
        if (cart.getCartItems().isEmpty()) {
            throw new BadRequestException("Cart is empty, cannot proceed with checkout");
        }
    }

    private void clearCart(Cart cart) {
        log.debug("Clearing cart after successful checkout");
        cart.getCartItems().clear();
        cartRepository.save(cart);
    }

    private Cart findLockedCartAggregateOrThrow(Long userId) {
        cartRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));
        return cartRepository.findAggregateByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));
    }

    private BigDecimal calculateTotalPrice(Order order) {
        return order.getOrderItems().stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
