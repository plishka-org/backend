package org.plishka.backend.controller.admin;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.admin.order.AdminOrderDetailDto;
import org.plishka.backend.dto.admin.order.AdminOrderSummaryDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.admin.order.AdminOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest extends BaseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminOrderService adminOrderService;

    @Test
    void getOrders_ShouldReturnPagedOrders() throws Exception {
        when(adminOrderService.getOrders(any(), eq(0), eq(10))).thenReturn(new PageResponse<>(
                List.of(summary()),
                0,
                10,
                1,
                1,
                true
        ));

        mockMvc.perform(get("/admin/orders").param("search", "1499"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(10))
                .andExpect(jsonPath("$.content[0].totalPrice").value(1499));
    }

    @Test
    void getOrder_ShouldReturnDetails() throws Exception {
        when(adminOrderService.getOrder(10L)).thenReturn(detail());

        mockMvc.perform(get("/admin/orders/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.notes").value("Call before delivery"));
    }

    @Test
    void getOrder_ShouldReturn404_WhenMissing() throws Exception {
        when(adminOrderService.getOrder(10L))
                .thenThrow(new ResourceNotFoundException("Order with ID 10 not found"));

        mockMvc.perform(get("/admin/orders/{id}", 10L))
                .andExpect(status().isNotFound());
    }

    private static AdminOrderSummaryDto summary() {
        return new AdminOrderSummaryDto(
                10L,
                "ORD-10",
                Instant.parse("2026-07-01T12:00:00Z"),
                "Olena Shevchenko",
                "+380501234567",
                List.of(),
                1499L,
                "Kyiv"
        );
    }

    private static AdminOrderDetailDto detail() {
        return new AdminOrderDetailDto(
                10L,
                "ORD-10",
                Instant.parse("2026-07-01T12:00:00Z"),
                "Olena Shevchenko",
                "+380501234567",
                List.of(),
                1499L,
                "Kyiv",
                "Call before delivery"
        );
    }
}
