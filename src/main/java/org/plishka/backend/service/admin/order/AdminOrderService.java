package org.plishka.backend.service.admin.order;

import org.plishka.backend.dto.admin.order.AdminOrderDetailDto;
import org.plishka.backend.dto.admin.order.AdminOrderSearchRequestDto;
import org.plishka.backend.dto.admin.order.AdminOrderSummaryDto;
import org.plishka.backend.dto.common.PageResponse;

public interface AdminOrderService {
    PageResponse<AdminOrderSummaryDto> getOrders(AdminOrderSearchRequestDto searchRequest, int page, int size);

    AdminOrderDetailDto getOrder(Long orderId);
}
