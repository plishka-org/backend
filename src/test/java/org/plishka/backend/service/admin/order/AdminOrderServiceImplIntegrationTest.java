package org.plishka.backend.service.admin.order;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.dto.admin.order.AdminOrderSearchRequestDto;
import org.plishka.backend.dto.admin.order.AdminOrderSummaryDto;
import org.plishka.backend.repository.order.OrderRepository;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminOrderServiceImplIntegrationTest {
    private static final String REQUEST_HASH = "012345678901234567890123456789012345678901234567890123456789";

    @Autowired
    private AdminOrderService adminOrderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @Test
    void getOrders_ShouldTreatLikeWildcardsAsLiteralText() {
        Order percentMatch = createOrder("Customer % Literal");
        Order underscoreMatch = createOrder("Customer _ Literal");
        createOrder("Regular Customer");

        assertEquals(List.of(percentMatch.getId()), findOrderIds("%"));
        assertEquals(List.of(underscoreMatch.getId()), findOrderIds("_"));
    }

    private List<Long> findOrderIds(String searchQuery) {
        return adminOrderService.getOrders(
                        new AdminOrderSearchRequestDto(searchQuery, "createdAt,asc"),
                        0,
                        10
                )
                .content()
                .stream()
                .map(AdminOrderSummaryDto::orderId)
                .toList();
    }

    private Order createOrder(String customerName) {
        Order order = new Order();
        order.setOrderNumber("ORDER-" + System.nanoTime());
        order.setIdempotencyKey("idempotency-" + System.nanoTime());
        order.setRequestHash(REQUEST_HASH);
        order.setCustomerName(customerName);
        order.setTotalPrice(100L);
        order.setDeliveryCity("Kyiv");
        order.setPhone("+380501112233");
        return orderRepository.saveAndFlush(order);
    }
}
