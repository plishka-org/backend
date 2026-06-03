package org.plishka.backend.service.order.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.domain.order.OrderItem;
import org.plishka.backend.domain.order.OrderStatus;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.order.OrderDetailDto;
import org.plishka.backend.dto.order.OrderSummaryDto;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.order.OrderMapper;
import org.plishka.backend.repository.order.OrderRepository;
import org.plishka.backend.service.order.OrderNumberGenerator;
import org.plishka.backend.service.order.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderNumberGenerator orderNumberGenerator;

    @Override
    @Transactional(readOnly = true)
    public OrderDetailDto getOrderById(Long orderId, Long userId) {
        log.debug("Fetching order id={} for user id={}", orderId, userId);

        Order order = findOrderByIdWithItemsOrThrow(orderId);
        validateOrderOwnership(order, userId);

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

        Page<Order> ordersPage = orderRepository.findByUserIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(page, size)
        );
        List<OrderSummaryDto> orderSummaries = toOrderSummaries(ordersPage.getContent());

        log.debug("Successfully fetched {} orders for user id={}", ordersPage.getNumberOfElements(), userId);

        return PageResponse.from(ordersPage, orderSummaries);
    }

    @Override
    @Transactional
    public OrderDetailDto repeatOrder(Long orderId, Long userId) {
        log.debug("Repeating order id={} for user id={}", orderId, userId);

        Order originalOrder = findOrderByIdWithItemsOrThrow(orderId);
        validateOrderOwnership(originalOrder, userId);

        Order newOrder = createOrderFromExisting(originalOrder);
        Order savedOrder = orderRepository.save(newOrder);
        
        log.debug("Successfully repeated order id={} as new order id={} for user id={}",
                orderId, savedOrder.getId(), userId);

        return orderMapper.toDetailDto(savedOrder);
    }

    private Order createOrderFromExisting(Order originalOrder) {
        Order newOrder = new Order();
        newOrder.setUser(originalOrder.getUser());
        newOrder.setOrderNumber(orderNumberGenerator.generate());
        newOrder.setCustomerName(originalOrder.getCustomerName());
        newOrder.setDeliveryCity(originalOrder.getDeliveryCity());
        newOrder.setPhone(originalOrder.getPhone());
        newOrder.setNotes(originalOrder.getNotes());
        newOrder.setStatus(OrderStatus.PENDING);
        newOrder.setTotalPrice(originalOrder.getTotalPrice());

        originalOrder.getOrderItems().forEach(originalItem -> {
            OrderItem newItem = copyOrderItem(newOrder, originalItem);
            newOrder.getOrderItems().add(newItem);
        });

        return newOrder;
    }

    private OrderItem copyOrderItem(Order newOrder, OrderItem originalItem) {
        OrderItem newItem = new OrderItem();
        newItem.setOrder(newOrder);
        newItem.setProduct(originalItem.getProduct());
        newItem.setProductNameSnapshot(originalItem.getProductNameSnapshot());
        newItem.setCategoryNameSnapshot(originalItem.getCategoryNameSnapshot());
        newItem.setQuantity(originalItem.getQuantity());
        newItem.setUnitPrice(originalItem.getUnitPrice());
        newItem.setLineTotal(originalItem.getLineTotal());
        return newItem;
    }

    private void validateOrderOwnership(Order order, Long userId) {
        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found or does not belong to user");
        }
    }

    private List<OrderSummaryDto> toOrderSummaries(List<Order> orders) {
        return orders.stream()
                .map(orderMapper::toSummaryDto)
                .toList();
    }

    private Order findOrderByIdWithItemsOrThrow(Long orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + orderId + " not found"));
    }

}
