package org.plishka.backend.service.order.impl;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.cart.Cart;
import org.plishka.backend.domain.cart.CartItem;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.domain.order.OrderItem;
import org.plishka.backend.domain.order.OrderStatus;
import org.plishka.backend.dto.order.CreateOrderRequestDto;
import org.plishka.backend.dto.order.OrderDetailDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.order.OrderMapper;
import org.plishka.backend.repository.cart.CartRepository;
import org.plishka.backend.repository.order.OrderRepository;
import org.plishka.backend.service.order.OrderCheckoutService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCheckoutServiceImpl implements OrderCheckoutService {
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderDetailDto checkout(Long userId, CreateOrderRequestDto requestDto) {
        log.debug("Starting checkout for user id={}", userId);

        Cart cart = findCartByUserIdOrThrow(userId);
        validateCartNotEmpty(cart);

        Order order = createOrderFromCart(cart, requestDto);
        Order savedOrder = orderRepository.saveAndFlush(order);
        
        clearCart(cart);
        
        log.debug("Order created with id={} for user id={}", savedOrder.getId(), userId);

        return orderMapper.toDetailDto(savedOrder);
    }

    private Order createOrderFromCart(Cart cart, CreateOrderRequestDto requestDto) {
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setOrderNumber(generateOrderNumber());
        order.setCustomerName(requestDto.customerName());
        order.setDeliveryCity(requestDto.deliveryCity());
        order.setPhone(requestDto.phone());
        order.setNotes(requestDto.notes());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalPrice(calculateTotalPrice(cart));

        cart.getCartItems().forEach(cartItem -> {
            OrderItem orderItem = createOrderItem(order, cartItem);
            order.getOrderItems().add(orderItem);
        });

        return order;
    }

    private OrderItem createOrderItem(Order order, CartItem cartItem) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(cartItem.getProduct());
        orderItem.setProductNameSnapshot(cartItem.getProduct().getName());
        orderItem.setCategoryNameSnapshot(cartItem.getProduct().getCategory().getName());
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setUnitPrice(cartItem.getUnitPrice());
        orderItem.setLineTotal(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        return orderItem;
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

    private Cart findCartByUserIdOrThrow(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BigDecimal calculateTotalPrice(Cart cart) {
        return cart.getCartItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
