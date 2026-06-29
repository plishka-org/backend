package org.plishka.backend.controller.order;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.order.CreateOrderRequestDto;
import org.plishka.backend.dto.order.OrderDetailDto;
import org.plishka.backend.dto.order.OrderItemDetailDto;
import org.plishka.backend.dto.order.OrderSummaryDto;
import org.plishka.backend.exception.ConflictException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.order.OrderCheckoutService;
import org.plishka.backend.service.order.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest extends BaseControllerTest {
    private static final long USER_ID = 1L;
    private static final long ORDER_ID = 25L;
    private static final long INVALID_ORDER_ID = -1L;
    private static final long NOT_FOUND_ORDER_ID = 999L;
    private static final long ORDER_ITEM_ID = 30L;
    private static final long PRODUCT_ID = 10L;
    private static final int FIRST_PAGE = 0;
    private static final int ORDER_PAGE_SIZE = 20;
    private static final int CUSTOM_ORDER_PAGE_SIZE = 5;
    private static final int QUANTITY = 2;
    private static final String USER_EMAIL = "customer@example.com";
    private static final String ORDER_NUMBER = "ORD-B01F4650";
    private static final String NOT_FOUND_ORDER_NUMBER = "ORD-404";
    private static final String CUSTOMER_NAME = "John Smith";
    private static final String DELIVERY_CITY = "Sumy";
    private static final String PHONE = "+380501234567";
    private static final String NOTES = "Please call before delivery";
    private static final String PRODUCT_NAME = "Oak Garden Bench";
    private static final String CATEGORY_NAME = "Outdoor Tables and Benches";
    private static final long UNIT_PRICE = 450L;
    private static final long SUBTOTAL = 900L;
    private static final long TOTAL_PRICE = 900L;
    private static final String ORDER_NOT_FOUND_MESSAGE = "Order not found";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_KEY = "11f2cbe7-3915-44f6-9bcd-3a1c70a47e92";
    private static final Instant CREATED_AT = Instant.parse("2026-05-31T10:15:30Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderCheckoutService orderCheckoutService;

    @Test
    void getOrder_ShouldReturnOrderDetailsAndStatus200() throws Exception {
        OrderDetailDto response = orderDetail();

        when(orderService.getOrderById(ORDER_ID, USER_ID)).thenReturn(response);

        mockMvc.perform(get("/users/me/orders/{orderId}", ORDER_ID)
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.orderNumber").value(ORDER_NUMBER))
                .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID));

        verify(orderService).getOrderById(ORDER_ID, USER_ID);
    }

    @Test
    void getOrder_ShouldReturn404_WhenOrderNotFound() throws Exception {
        when(orderService.getOrderById(NOT_FOUND_ORDER_ID, USER_ID))
                .thenThrow(new ResourceNotFoundException(ORDER_NOT_FOUND_MESSAGE));

        mockMvc.perform(get("/users/me/orders/{orderId}", NOT_FOUND_ORDER_ID)
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrder_ShouldReturn400_WhenOrderIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/users/me/orders/{orderId}", INVALID_ORDER_ID)
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrderByNumber_ShouldReturnOrderDetailsAndStatus200() throws Exception {
        OrderDetailDto response = orderDetail();

        when(orderService.getOrderByOrderNumber(ORDER_NUMBER, USER_ID)).thenReturn(response);

        mockMvc.perform(get("/users/me/orders/number/{orderNumber}", ORDER_NUMBER)
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.orderNumber").value(ORDER_NUMBER));

        verify(orderService).getOrderByOrderNumber(ORDER_NUMBER, USER_ID);
    }

    @Test
    void getOrderByNumber_ShouldReturn404_WhenOrderNotFound() throws Exception {
        when(orderService.getOrderByOrderNumber(NOT_FOUND_ORDER_NUMBER, USER_ID))
                .thenThrow(new ResourceNotFoundException(ORDER_NOT_FOUND_MESSAGE));

        mockMvc.perform(get("/users/me/orders/number/{orderNumber}", NOT_FOUND_ORDER_NUMBER)
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserOrders_ShouldReturnPaginatedOrdersAndStatus200() throws Exception {
        PageResponse<OrderSummaryDto> response = new PageResponse<>(
                List.of(orderSummary()),
                FIRST_PAGE,
                CUSTOM_ORDER_PAGE_SIZE,
                1,
                1,
                true
        );

        when(orderService.getUserOrders(USER_ID, FIRST_PAGE, CUSTOM_ORDER_PAGE_SIZE)).thenReturn(response);

        mockMvc.perform(get("/users/me/orders")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .param("page", String.valueOf(FIRST_PAGE))
                        .param("size", String.valueOf(CUSTOM_ORDER_PAGE_SIZE))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.pageSize").value(CUSTOM_ORDER_PAGE_SIZE))
                .andExpect(jsonPath("$.last").value(true));

        verify(orderService).getUserOrders(USER_ID, FIRST_PAGE, CUSTOM_ORDER_PAGE_SIZE);
    }

    @Test
    void getUserOrders_ShouldUseDefaultPagination_WhenPaginationIsMissing() throws Exception {
        PageResponse<OrderSummaryDto> response = new PageResponse<>(
                List.of(),
                FIRST_PAGE,
                ORDER_PAGE_SIZE,
                0,
                0,
                true
        );

        when(orderService.getUserOrders(USER_ID, FIRST_PAGE, ORDER_PAGE_SIZE)).thenReturn(response);

        mockMvc.perform(get("/users/me/orders")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(FIRST_PAGE))
                .andExpect(jsonPath("$.pageSize").value(ORDER_PAGE_SIZE));

        verify(orderService).getUserOrders(USER_ID, FIRST_PAGE, ORDER_PAGE_SIZE);
    }

    @Test
    void getUserOrders_ShouldReturn400_WhenPageIsNegative() throws Exception {
        mockMvc.perform(get("/users/me/orders")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .param("page", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_ShouldReturnOrderDetailsAndStatus200_WhenRequestIsValid() throws Exception {
        CreateOrderRequestDto request = createOrderRequest(CUSTOMER_NAME);
        OrderDetailDto response = orderDetail();

        when(orderCheckoutService.checkout(eq(USER_ID), eq(IDEMPOTENCY_KEY), any(CreateOrderRequestDto.class)))
                .thenReturn(response);

        performCreateOrder(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.customerName").value(CUSTOMER_NAME))
                .andExpect(jsonPath("$.totalPrice").value(900));

        verify(orderCheckoutService).checkout(eq(USER_ID), eq(IDEMPOTENCY_KEY), any(CreateOrderRequestDto.class));
    }

    @Test
    void createOrder_ShouldReturn400_WhenCustomerNameIsBlank() throws Exception {
        CreateOrderRequestDto request = createOrderRequest("");

        performCreateOrder(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_ShouldReturn400_WhenIdempotencyKeyHeaderIsMissing() throws Exception {
        CreateOrderRequestDto request = createOrderRequest(CUSTOMER_NAME);

        mockMvc.perform(post("/orders")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_ShouldReturn409_WhenIdempotencyKeyReusedWithDifferentPayload() throws Exception {
        CreateOrderRequestDto request = createOrderRequest(CUSTOMER_NAME);

        when(orderCheckoutService.checkout(eq(USER_ID), eq(IDEMPOTENCY_KEY), any(CreateOrderRequestDto.class)))
                .thenThrow(new ConflictException("Idempotency key was already used with a different request payload"));

        performCreateOrder(request)
                .andExpect(status().isConflict());
    }

    @Test
    void repeatOrder_ShouldReturnRepeatedOrderAndStatus200() throws Exception {
        OrderDetailDto response = orderDetail();

        when(orderService.repeatOrder(ORDER_ID, USER_ID, IDEMPOTENCY_KEY)).thenReturn(response);

        mockMvc.perform(post("/users/me/orders/{orderId}/repeat", ORDER_ID)
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .header(IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.items[0].quantity").value(QUANTITY));

        verify(orderService).repeatOrder(ORDER_ID, USER_ID, IDEMPOTENCY_KEY);
    }

    @Test
    void repeatOrder_ShouldReturn404_WhenOrderNotFound() throws Exception {
        when(orderService.repeatOrder(NOT_FOUND_ORDER_ID, USER_ID, IDEMPOTENCY_KEY))
                .thenThrow(new ResourceNotFoundException(ORDER_NOT_FOUND_MESSAGE));

        mockMvc.perform(post("/users/me/orders/{orderId}/repeat", NOT_FOUND_ORDER_ID)
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .header(IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void repeatOrder_ShouldReturn400_WhenOrderIdIsNotPositive() throws Exception {
        mockMvc.perform(post("/users/me/orders/{orderId}/repeat", INVALID_ORDER_ID)
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .header(IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void repeatOrder_ShouldReturn400_WhenIdempotencyKeyHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/users/me/orders/{orderId}/repeat", ORDER_ID)
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private ResultActions performCreateOrder(CreateOrderRequestDto request) throws Exception {
        return mockMvc.perform(post("/orders")
                        .with(authenticatedUser(USER_ID, USER_EMAIL))
                        .header(IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));
    }

    private static CreateOrderRequestDto createOrderRequest(String customerName) {
        return new CreateOrderRequestDto(
                customerName,
                DELIVERY_CITY,
                PHONE,
                NOTES
        );
    }

    private static OrderDetailDto orderDetail() {
        return new OrderDetailDto(
                ORDER_ID,
                ORDER_NUMBER,
                CUSTOMER_NAME,
                TOTAL_PRICE,
                DELIVERY_CITY,
                PHONE,
                NOTES,
                CREATED_AT,
                List.of(orderItem())
        );
    }

    private static OrderSummaryDto orderSummary() {
        return new OrderSummaryDto(
                ORDER_ID,
                ORDER_NUMBER,
                TOTAL_PRICE,
                CREATED_AT
        );
    }

    private static OrderItemDetailDto orderItem() {
        return new OrderItemDetailDto(
                ORDER_ITEM_ID,
                PRODUCT_ID,
                PRODUCT_NAME,
                CATEGORY_NAME,
                QUANTITY,
                UNIT_PRICE,
                SUBTOTAL
        );
    }
}
