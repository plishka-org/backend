package org.plishka.backend.service.order.impl;

import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.cart.Cart;
import org.plishka.backend.domain.cart.CartItem;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.domain.order.OrderItem;
import org.plishka.backend.dto.order.CreateOrderRequestDto;
import org.plishka.backend.dto.order.OrderDetailDto;
import org.plishka.backend.event.order.OrderCreatedEvent;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ForbiddenException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.order.OrderMapper;
import org.plishka.backend.monitoring.metrics.BusinessMetricsRecorder;
import org.plishka.backend.monitoring.transaction.TransactionalMetricsPublisher;
import org.plishka.backend.repository.cart.CartRepository;
import org.plishka.backend.repository.order.OrderRepository;
import org.plishka.backend.service.notification.email.AdminNotificationOutboxService;
import org.plishka.backend.service.order.OrderCheckoutService;
import org.plishka.backend.service.order.OrderIdempotencyGuard;
import org.plishka.backend.service.order.OrderItemFactory;
import org.plishka.backend.service.order.OrderNumberGenerator;
import org.plishka.backend.service.pricing.PriceCalculator;
import org.plishka.backend.service.settings.ShopModeService;
import org.plishka.backend.util.RequestHashUtil;
import org.springframework.context.ApplicationEventPublisher;
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
    private static final String EMPTY_CART_MESSAGE = "Cart is empty, cannot proceed with checkout";
    private static final String OUTCOME_EMPTY_CART = "empty_cart";
    private static final String OUTCOME_ERROR = "error";
    private static final String OUTCOME_SHOP_DISABLED = "shop_disabled";
    private static final String OUTCOME_SUCCESS = "success";
    private static final Comparator<Long> NULLABLE_LONG_COMPARATOR = Comparator.nullsFirst(Long::compareTo);

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderIdempotencyGuard orderIdempotencyGuard;
    private final OrderItemFactory orderItemFactory;
    private final AdminNotificationOutboxService adminNotificationOutboxService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ShopModeService shopModeService;
    private final BusinessMetricsRecorder businessMetricsRecorder;
    private final TransactionalMetricsPublisher transactionalMetricsPublisher;

    @Override
    @Transactional
    public OrderDetailDto checkout(Long userId, String idempotencyKey, CreateOrderRequestDto requestDto) {
        var sample = businessMetricsRecorder.startTimer();
        String outcome = OUTCOME_ERROR;

        try {
            shopModeService.requireEnabled();
            log.debug("Starting checkout for user id={}", userId);

            Cart cart = findLockedCartAggregateOrThrow(userId);

            Optional<Order> existingOrder = orderIdempotencyGuard.findExistingOrder(userId, idempotencyKey);
            if (existingOrder.isPresent()) {
                String retryRequestHash = calculateRetryRequestHash(requestDto, cart, existingOrder.get());
                orderIdempotencyGuard.requireMatchingRequestHash(existingOrder.get(), retryRequestHash);
                log.debug("Returning existing order id={} for idempotent retry by user id={}",
                        existingOrder.get().getId(), userId);
                outcome = OUTCOME_SUCCESS;
                return orderMapper.toDetailDto(existingOrder.get());
            }

            validateCartNotEmpty(cart);

            String requestHash = calculateRequestHash(requestDto, cart);
            Order order = createOrderFromCart(cart, requestDto);
            order.setIdempotencyKey(idempotencyKey);
            order.setRequestHash(requestHash);
            Order savedOrder = orderRepository.saveAndFlush(order);

            clearCart(cart);

            OrderCreatedEvent orderCreatedEvent = OrderCreatedEvent.fromOrder(savedOrder);
            adminNotificationOutboxService.enqueueOrderCreated(orderCreatedEvent);
            applicationEventPublisher.publishEvent(orderCreatedEvent);

            log.debug("Order created with id={} for user id={}", savedOrder.getId(), userId);

            outcome = OUTCOME_SUCCESS;
            return orderMapper.toDetailDto(savedOrder);
        } catch (BadRequestException exception) {
            outcome = EMPTY_CART_MESSAGE.equals(exception.getMessage()) ? OUTCOME_EMPTY_CART : OUTCOME_ERROR;
            throw exception;
        } catch (ForbiddenException exception) {
            outcome = ShopModeService.SHOP_MODE_DISABLED_MESSAGE.equals(exception.getMessage())
                    ? OUTCOME_SHOP_DISABLED
                    : OUTCOME_ERROR;
            throw exception;
        } catch (RuntimeException exception) {
            outcome = OUTCOME_ERROR;
            throw exception;
        } finally {
            recordCheckoutAfterCompletionOrNow(sample, outcome);
        }
    }

    private void recordCheckoutAfterCompletionOrNow(Timer.Sample sample, String outcome) {
        if (!OUTCOME_SUCCESS.equals(outcome)) {
            recordCheckout(sample, outcome);
            return;
        }

        transactionalMetricsPublisher.afterCompletionOrNow(
                () -> recordCheckout(sample, OUTCOME_SUCCESS),
                () -> recordCheckout(sample, OUTCOME_ERROR)
        );
    }

    private void recordCheckout(Timer.Sample sample, String outcome) {
        businessMetricsRecorder.recordCheckoutAttempt(outcome);
        businessMetricsRecorder.recordCheckoutDuration(sample, outcome);
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

    private String calculateRetryRequestHash(CreateOrderRequestDto requestDto, Cart cart, Order existingOrder) {
        if (cart.getCartItems().isEmpty()) {
            return calculateRequestHash(requestDto, existingOrder);
        }
        return calculateRequestHash(requestDto, cart);
    }

    private String calculateRequestHash(CreateOrderRequestDto requestDto, Cart cart) {
        List<String> fields = requestHashFields(requestDto);
        cart.getCartItems().stream()
                .sorted(Comparator.comparing(cartItem -> cartItem.getProduct().getId()))
                .map(this::cartItemHashField)
                .forEach(fields::add);
        return RequestHashUtil.hash(fields.toArray(String[]::new));
    }

    private String calculateRequestHash(CreateOrderRequestDto requestDto, Order order) {
        List<String> fields = requestHashFields(requestDto);
        order.getOrderItems().stream()
                .sorted(Comparator.comparing(OrderItem::getProductId, NULLABLE_LONG_COMPARATOR))
                .map(this::orderItemHashField)
                .forEach(fields::add);
        return RequestHashUtil.hash(fields.toArray(String[]::new));
    }

    private List<String> requestHashFields(CreateOrderRequestDto requestDto) {
        List<String> fields = new ArrayList<>();
        fields.add(requestDto.customerName());
        fields.add(requestDto.deliveryCity());
        fields.add(requestDto.phone());
        fields.add(requestDto.notes());
        return fields;
    }

    private String cartItemHashField(CartItem cartItem) {
        return "%d:%d:%d".formatted(
                cartItem.getProduct().getId(),
                cartItem.getQuantity(),
                cartItem.getProduct().getPrice()
        );
    }

    private String orderItemHashField(OrderItem orderItem) {
        return "%s:%s:%s".formatted(orderItem.getProductId(), orderItem.getQuantity(), orderItem.getUnitPrice());
    }

    private void validateCartNotEmpty(Cart cart) {
        if (cart.getCartItems().isEmpty()) {
            throw new BadRequestException(EMPTY_CART_MESSAGE);
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

    private Long calculateTotalPrice(Order order) {
        return PriceCalculator.calculateTotal(order.getOrderItems().stream()
                .map(OrderItem::getLineTotal)
                .toList());
    }
}
