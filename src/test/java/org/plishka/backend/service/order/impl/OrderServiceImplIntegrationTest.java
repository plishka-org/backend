package org.plishka.backend.service.order.impl;

import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.plishka.backend.domain.order.Order;
import org.plishka.backend.domain.order.OrderItem;
import org.plishka.backend.domain.product.Category;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.order.OrderDetailDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ConflictException;
import org.plishka.backend.exception.ForbiddenException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.order.OrderRepository;
import org.plishka.backend.repository.product.CategoryRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.plishka.backend.service.order.OrderService;
import org.plishka.backend.testsupport.MySqlIntegrationTest;
import org.plishka.backend.util.RequestHashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceImplIntegrationTest extends MySqlIntegrationTest {
    private static final String PASSWORD_HASH =
            "012345678901234567890123456789012345678901234567890123456789";

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @BeforeEach
    void enableShopMode() {
        jdbcTemplate.update("update system_settings set is_shop_mode_enabled = true where id = 1");
    }

    @Test
    void repeatOrder_ShouldCreateNewOrderFromOriginalUsingCurrentProductData() {
        User user = createUser("repeat-happy");
        Product product = createProduct("Repeat Product " + System.nanoTime(), 150L);
        Order originalOrder = createOrder(user, product, 2, "original-repeat-" + System.nanoTime());
        product.setPrice(175L);
        productRepository.saveAndFlush(product);

        OrderDetailDto repeatedOrder = orderService.repeatOrder(
                originalOrder.getId(),
                user.getId(),
                "repeat-happy-" + System.nanoTime()
        );

        assertNotEquals(originalOrder.getId(), repeatedOrder.orderId());
        assertEquals(350L, repeatedOrder.totalPrice());
        assertEquals(1, repeatedOrder.items().size());
        assertEquals(product.getId(), repeatedOrder.items().getFirst().productId());
        assertEquals(175L, repeatedOrder.items().getFirst().unitPrice());
        assertEquals(2, repeatedOrder.items().getFirst().quantity());
        assertEquals(2, countOrders(user.getId()));
        assertEquals(1, countOrderItems(repeatedOrder.orderId()));
    }

    @Test
    void repeatOrder_ShouldReturnExistingOrder_WhenIdempotencyKeyAndOriginalOrderMatch() {
        User user = createUser("repeat-idempotent");
        Product product = createProduct("Repeat Idempotent Product " + System.nanoTime(), 100L);
        Order originalOrder = createOrder(user, product, 1, "original-idempotent-" + System.nanoTime());
        String idempotencyKey = "repeat-idempotent-" + System.nanoTime();

        OrderDetailDto firstResponse = orderService.repeatOrder(originalOrder.getId(), user.getId(), idempotencyKey);
        OrderDetailDto secondResponse = orderService.repeatOrder(originalOrder.getId(), user.getId(), idempotencyKey);

        assertEquals(firstResponse.orderId(), secondResponse.orderId());
        assertEquals(2, countOrders(user.getId()));
    }

    @Test
    void repeatOrder_ShouldRejectSameIdempotencyKeyWithDifferentOriginalOrder() {
        User user = createUser("repeat-conflict");
        Product firstProduct = createProduct("Repeat Conflict Product A " + System.nanoTime(), 100L);
        Product secondProduct = createProduct("Repeat Conflict Product B " + System.nanoTime(), 200L);
        Order firstOriginal = createOrder(user, firstProduct, 1, "original-conflict-a-" + System.nanoTime());
        Order secondOriginal = createOrder(user, secondProduct, 1, "original-conflict-b-" + System.nanoTime());
        String idempotencyKey = "repeat-conflict-" + System.nanoTime();

        orderService.repeatOrder(firstOriginal.getId(), user.getId(), idempotencyKey);

        assertThrows(
                ConflictException.class,
                () -> orderService.repeatOrder(secondOriginal.getId(), user.getId(), idempotencyKey)
        );
        assertEquals(3, countOrders(user.getId()));
    }

    @Test
    void repeatOrder_ShouldRejectOrderOwnedByAnotherUser() {
        User owner = createUser("repeat-owner");
        User otherUser = createUser("repeat-other");
        Product product = createProduct("Repeat Ownership Product " + System.nanoTime(), 100L);
        Order originalOrder = createOrder(owner, product, 1, "original-owner-" + System.nanoTime());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.repeatOrder(originalOrder.getId(), otherUser.getId(),
                        "repeat-foreign-" + System.nanoTime())
        );
        assertEquals(1, countOrders(owner.getId()));
        assertEquals(0, countOrders(otherUser.getId()));
    }

    @Test
    void repeatOrder_ShouldRejectWhenShopModeIsDisabled() {
        jdbcTemplate.update("update system_settings set is_shop_mode_enabled = false where id = 1");
        User user = createUser("repeat-disabled");
        Product product = createProduct("Repeat Disabled Product " + System.nanoTime(), 100L);
        Order originalOrder = createOrder(user, product, 1, "original-disabled-" + System.nanoTime());

        assertThrows(
                ForbiddenException.class,
                () -> orderService.repeatOrder(originalOrder.getId(), user.getId(),
                        "repeat-disabled-" + System.nanoTime())
        );
        assertEquals(1, countOrders(user.getId()));
    }

    @Test
    void repeatOrder_ShouldRejectWhenOriginalProductIsUnavailable() {
        User user = createUser("repeat-unavailable");
        Product product = createProduct("Repeat Unavailable Product " + System.nanoTime(), 100L);
        Order originalOrder = createOrder(user, product, 1, "original-unavailable-" + System.nanoTime());
        productRepository.delete(product);
        productRepository.flush();

        assertThrows(
                BadRequestException.class,
                () -> orderService.repeatOrder(originalOrder.getId(), user.getId(),
                        "repeat-unavailable-" + System.nanoTime())
        );
        assertEquals(1, countOrders(user.getId()));
    }

    private User createUser(String emailPrefix) {
        User user = new User();
        user.setName("Repeat User");
        user.setEmail(emailPrefix + "-" + System.nanoTime() + "@example.com");
        user.setPhone("+380501234567");
        user.setPasswordHash(PASSWORD_HASH);
        user.setEmailVerified(true);
        return userRepository.saveAndFlush(user);
    }

    private Product createProduct(String productName, Long price) {
        Category category = new Category();
        category.setName("Repeat Category " + System.nanoTime());
        category = categoryRepository.saveAndFlush(category);

        Product product = new Product();
        product.setName(productName);
        product.setDescription("Repeat order product");
        product.setPrice(price);
        product.setCategory(category);
        return productRepository.saveAndFlush(product);
    }

    private Order createOrder(User user, Product product, int quantity, String idempotencyKey) {
        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber("TEST-" + System.nanoTime());
        order.setIdempotencyKey(idempotencyKey);
        order.setRequestHash(RequestHashUtil.hash(idempotencyKey));
        order.setCustomerName("Repeat User");
        order.setDeliveryCity("Kyiv");
        order.setPhone("+380501234567");
        order.setNotes(null);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProductId(product.getId());
        orderItem.setProductNameSnapshot(product.getName());
        orderItem.setCategoryNameSnapshot(product.getCategory().getName());
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(product.getPrice());
        orderItem.setLineTotal(product.getPrice() * quantity);
        order.getOrderItems().add(orderItem);
        order.setTotalPrice(orderItem.getLineTotal());

        return orderRepository.saveAndFlush(order);
    }

    private int countOrders(Long userId) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(
                "select count(*) from orders where user_id = ?",
                Integer.class,
                userId
        ));
    }

    private int countOrderItems(Long orderId) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(
                "select count(*) from order_items where order_id = ?",
                Integer.class,
                orderId
        ));
    }
}
