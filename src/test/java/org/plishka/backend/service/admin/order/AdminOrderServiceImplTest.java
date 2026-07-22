package org.plishka.backend.service.admin.order;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.dto.admin.order.AdminOrderDetailDto;
import org.plishka.backend.dto.admin.order.AdminOrderSearchRequestDto;
import org.plishka.backend.dto.admin.order.AdminOrderSummaryDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.order.OrderRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceImplTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AdminOrderDtoAssembler adminOrderDtoAssembler;

    @InjectMocks
    private AdminOrderServiceImpl service;

    @Test
    void getOrders_ShouldUseDefaultCreatedAtDescendingSort() {
        Order order = new Order();
        order.setId(1L);
        when(orderRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Order>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderRepository.findAllByIdInWithItems(anyList())).thenReturn(List.of(order));
        when(adminOrderDtoAssembler.toSummaryDtos(List.of(order))).thenReturn(List.of());

        service.getOrders(new AdminOrderSearchRequestDto(null, null), 0, 10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<Order>>any(),
                pageableCaptor.capture()
        );
        assertEquals("DESC", pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection().name());
    }

    @Test
    void getOrders_ShouldRejectUnsupportedSort() {
        assertThrows(
                BadRequestException.class,
                () -> service.getOrders(new AdminOrderSearchRequestDto(null, "name,asc"), 0, 10)
        );

        verifyNoInteractions(orderRepository);
    }

    @Test
    void getOrder_ShouldThrowWhenMissing() {
        when(orderRepository.findByIdWithItems(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getOrder(10L));
    }

    @Test
    void getOrder_ShouldAssembleDetails() {
        Order order = new Order();
        order.setId(10L);
        AdminOrderDetailDto detail = new AdminOrderDetailDto(
                10L,
                "ORD-10",
                null,
                "Customer",
                "+380501234567",
                List.of(),
                100L,
                "Kyiv",
                null
        );
        when(orderRepository.findByIdWithItems(10L)).thenReturn(Optional.of(order));
        when(adminOrderDtoAssembler.toDetailDto(order)).thenReturn(detail);

        assertEquals(detail, service.getOrder(10L));
    }
}
