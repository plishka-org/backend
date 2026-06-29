package org.plishka.backend.service.order.impl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.domain.order.OrderItem;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.order.OrderDetailDto;
import org.plishka.backend.dto.order.OrderSummaryDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.order.OrderMapper;
import org.plishka.backend.repository.order.OrderRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.order.OrderIdempotencyGuard;
import org.plishka.backend.service.order.OrderItemFactory;
import org.plishka.backend.service.order.OrderNumberGenerator;
import org.plishka.backend.service.order.OrderService;
import org.plishka.backend.service.pricing.PriceCalculator;
import org.plishka.backend.util.RequestHashUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order service implementation.
 *
 * <p>Concurrency invariant for repeat orders: {@link #repeatOrder} must lock the
 * User row first so that concurrent repeats for the same user are serialized.
 * Repeat does not touch the cart, so without this lock two near-simultaneous
 * requests could both miss the existing order, both insert with the same
 * idempotency key, and the second would fail on the unique constraint instead of
 * returning the already-created order (i.e. the endpoint would stop being
 * idempotent under contention).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private static final String PRODUCT_UNAVAILABLE_MESSAGE =
            "Order cannot be repeated because one of its products is no longer available";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderIdempotencyGuard orderIdempotencyGuard;
    private final OrderItemFactory orderItemFactory;

    @Override
    @Transactional(readOnly = true)
    public OrderDetailDto getOrderById(Long orderId, Long userId) {
        log.debug("Fetching order id={} for user id={}", orderId, userId);

        Order order = findOwnedOrderWithItemsOrThrow(orderId, userId);

        log.debug("Successfully fetched order id={} for user id={}", orderId, userId);
        return orderMapper.toDetailDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailDto getOrderByOrderNumber(String orderNumber, Long userId) {
        log.debug("Fetching order by orderNumber={} for user id={}", orderNumber, userId);

        Order order = orderRepository.findByUserIdAndOrderNumber(userId, orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order with number " + orderNumber + " not found"));

        log.debug("Successfully fetched order by orderNumber={} for user id={}", orderNumber, userId);
        return orderMapper.toDetailDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryDto> getUserOrders(Long userId, int page, int size) {
        log.debug("Fetching orders for user id={}, page={}, size={}", userId, page, size);

        Page<Order> ordersPage = orderRepository.findByUserIdOrderByCreatedAtDescIdDesc(
                userId,
                PageRequest.of(page, size)
        );
        List<OrderSummaryDto> orderSummaries = toOrderSummaries(ordersPage.getContent());

        log.debug("Successfully fetched {} orders for user id={}", ordersPage.getNumberOfElements(), userId);

        return PageResponse.from(ordersPage, orderSummaries);
    }

    @Override
    @Transactional
    public OrderDetailDto repeatOrder(Long orderId, Long userId, String idempotencyKey) {
        log.debug("Repeating order id={} for user id={}", orderId, userId);

        lockUserOrThrow(userId);

        String requestHash = RequestHashUtil.hash(String.valueOf(orderId));
        Optional<Order> existingOrder = orderIdempotencyGuard.findExistingOrder(userId, idempotencyKey, requestHash);
        if (existingOrder.isPresent()) {
            log.debug("Returning existing order id={} for idempotent repeat by user id={}",
                    existingOrder.get().getId(), userId);
            return orderMapper.toDetailDto(existingOrder.get());
        }

        Order originalOrder = findOwnedOrderWithItemsOrThrow(orderId, userId);

        Order newOrder = createRepeatedOrder(originalOrder);
        newOrder.setIdempotencyKey(idempotencyKey);
        newOrder.setRequestHash(requestHash);
        Order savedOrder = orderRepository.saveAndFlush(newOrder);

        log.debug("Successfully repeated order id={} as new order id={} for user id={}",
                orderId, savedOrder.getId(), userId);

        return orderMapper.toDetailDto(savedOrder);
    }

    private Order createRepeatedOrder(Order originalOrder) {
        Order newOrder = new Order();
        newOrder.setUser(originalOrder.getUser());
        newOrder.setOrderNumber(orderNumberGenerator.generate());
        newOrder.setCustomerName(originalOrder.getCustomerName());
        newOrder.setDeliveryCity(originalOrder.getDeliveryCity());
        newOrder.setPhone(originalOrder.getPhone());
        newOrder.setNotes(originalOrder.getNotes());

        Map<Long, Product> currentProducts = loadCurrentProductsOrThrow(originalOrder.getOrderItems());
        originalOrder.getOrderItems().forEach(originalItem -> {
            Product product = currentProducts.get(originalItem.getProductId());
            OrderItem newItem = orderItemFactory.create(newOrder, product, originalItem.getQuantity());
            newOrder.getOrderItems().add(newItem);
        });
        newOrder.setTotalPrice(calculateTotalPrice(newOrder));

        return newOrder;
    }

    private Map<Long, Product> loadCurrentProductsOrThrow(List<OrderItem> originalItems) {
        Set<Long> productIds = originalItems.stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (productIds.stream().anyMatch(Objects::isNull)) {
            throw new BadRequestException(PRODUCT_UNAVAILABLE_MESSAGE);
        }

        Map<Long, Product> productsById = productRepository.findAllByIdIn(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        if (productsById.size() != productIds.size()) {
            throw new BadRequestException(PRODUCT_UNAVAILABLE_MESSAGE);
        }

        return productsById;
    }

    private List<OrderSummaryDto> toOrderSummaries(List<Order> orders) {
        return orders.stream()
                .map(orderMapper::toSummaryDto)
                .toList();
    }

    private void lockUserOrThrow(Long userId) {
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " not found"));
    }

    private Order findOwnedOrderWithItemsOrThrow(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserIdWithItems(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + orderId + " not found"));
    }

    private Long calculateTotalPrice(Order order) {
        return PriceCalculator.calculateTotal(order.getOrderItems().stream()
                .map(OrderItem::getLineTotal)
                .toList());
    }
}
