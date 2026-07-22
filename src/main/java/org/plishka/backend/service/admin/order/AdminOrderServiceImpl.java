package org.plishka.backend.service.admin.order;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.dto.admin.order.AdminOrderDetailDto;
import org.plishka.backend.dto.admin.order.AdminOrderSearchRequestDto;
import org.plishka.backend.dto.admin.order.AdminOrderSummaryDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.order.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {
    private static final String ORDER_NOT_FOUND_MESSAGE = "Order with ID %d not found";

    private final OrderRepository orderRepository;
    private final AdminOrderDtoAssembler adminOrderDtoAssembler;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSummaryDto> getOrders(AdminOrderSearchRequestDto searchRequest, int page, int size) {
        Page<Order> orderPage = findOrderPage(searchRequest, page, size);
        List<Order> ordersWithItems = loadOrdersWithItemsPreservingPageOrder(orderPage.getContent());
        List<AdminOrderSummaryDto> orderSummaries = adminOrderDtoAssembler.toSummaryDtos(ordersWithItems);
        return PageResponse.from(orderPage, orderSummaries);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDetailDto getOrder(Long orderId) {
        Order order = findOrderWithItemsOrThrow(orderId);
        return adminOrderDtoAssembler.toDetailDto(order);
    }

    private Page<Order> findOrderPage(AdminOrderSearchRequestDto searchRequest, int page, int size) {
        return orderRepository.findAll(
                AdminOrderSpecifications.forSearchQuery(searchRequest.search()),
                PageRequest.of(page, size, AdminOrderSortResolver.resolveSort(searchRequest.sort()))
        );
    }

    private Order findOrderWithItemsOrThrow(Long orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_MESSAGE.formatted(orderId)));
    }

    private List<Order> loadOrdersWithItemsPreservingPageOrder(List<Order> ordersOnPage) {
        if (ordersOnPage.isEmpty()) {
            return List.of();
        }

        List<Long> orderIdsOnPage = ordersOnPage.stream().map(Order::getId).toList();
        List<Order> fetchedOrdersWithItems = orderRepository.findAllByIdInWithItems(orderIdsOnPage);
        Map<Long, Order> ordersByOrderId = fetchedOrdersWithItems.stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        return ordersOnPage.stream().map(order -> ordersByOrderId.get(order.getId())).toList();
    }
}
