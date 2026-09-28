package org.plishka.backend.service.admin.order;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.domain.order.OrderItem;
import org.plishka.backend.domain.product.ProductMedia;
import org.plishka.backend.dto.admin.order.AdminOrderDetailDto;
import org.plishka.backend.dto.admin.order.AdminOrderItemDetailDto;
import org.plishka.backend.dto.admin.order.AdminOrderItemSummaryDto;
import org.plishka.backend.dto.admin.order.AdminOrderSummaryDto;
import org.plishka.backend.dto.product.ProductMediaPreviewDto;
import org.plishka.backend.mapper.product.ProductMapper;
import org.plishka.backend.service.product.ProductMediaQueryService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AdminOrderDtoAssembler {
    private final ProductMediaQueryService productMediaQueryService;
    private final ProductMapper productMapper;

    List<AdminOrderSummaryDto> toSummaryDtos(Collection<Order> orders) {
        Map<Long, ProductMediaPreviewDto> primaryImagesByProductId = loadPrimaryImagesByProductId(orders);
        return orders.stream().map(order -> mapToSummaryDto(order, primaryImagesByProductId)).toList();
    }

    AdminOrderDetailDto toDetailDto(Order order) {
        return mapToDetailDto(order, loadPrimaryImagesByProductId(List.of(order)));
    }

    private AdminOrderSummaryDto mapToSummaryDto(
            Order order,
            Map<Long, ProductMediaPreviewDto> primaryImagesByProductId
    ) {
        List<AdminOrderItemSummaryDto> orderItems = order.getOrderItems().stream()
                .map(orderItem -> mapToSummaryItemDto(orderItem, primaryImagesByProductId))
                .toList();
        return new AdminOrderSummaryDto(
                order.getId(),
                order.getOrderNumber(),
                order.getCreatedAt(),
                order.getCustomerName(),
                order.getPhone(),
                orderItems,
                order.getTotalPrice(),
                order.getDeliveryCity()
        );
    }

    private AdminOrderDetailDto mapToDetailDto(
            Order order,
            Map<Long, ProductMediaPreviewDto> primaryImagesByProductId
    ) {
        List<AdminOrderItemDetailDto> orderItems = order.getOrderItems().stream()
                .map(orderItem -> mapToDetailItemDto(orderItem, primaryImagesByProductId))
                .toList();
        return new AdminOrderDetailDto(
                order.getId(),
                order.getOrderNumber(),
                order.getCreatedAt(),
                order.getCustomerName(),
                order.getPhone(),
                orderItems,
                order.getTotalPrice(),
                order.getDeliveryCity(),
                order.getNotes()
        );
    }

    private AdminOrderItemSummaryDto mapToSummaryItemDto(
            OrderItem orderItem,
            Map<Long, ProductMediaPreviewDto> primaryImagesByProductId
    ) {
        return new AdminOrderItemSummaryDto(
                orderItem.getProductId(),
                orderItem.getProductNameSnapshot(),
                primaryImagesByProductId.get(orderItem.getProductId())
        );
    }

    private AdminOrderItemDetailDto mapToDetailItemDto(
            OrderItem orderItem,
            Map<Long, ProductMediaPreviewDto> primaryImagesByProductId
    ) {
        return new AdminOrderItemDetailDto(
                orderItem.getProductId(),
                orderItem.getProductNameSnapshot(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getLineTotal(),
                primaryImagesByProductId.get(orderItem.getProductId())
        );
    }

    private Map<Long, ProductMediaPreviewDto> loadPrimaryImagesByProductId(Collection<Order> orders) {
        List<Long> productIds = extractProductIds(orders);
        Map<Long, ProductMedia> primaryMediaByProductId = productMediaQueryService
                .findPrimaryMediaByProductIds(productIds);
        return mapToMediaPreviews(primaryMediaByProductId);
    }

    private List<Long> extractProductIds(Collection<Order> orders) {
        return orders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Map<Long, ProductMediaPreviewDto> mapToMediaPreviews(
            Map<Long, ProductMedia> primaryMediaByProductId
    ) {
        return primaryMediaByProductId.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> productMapper.toMediaPreviewDto(entry.getValue()),
                        (first, second) -> first
                ));
    }
}
