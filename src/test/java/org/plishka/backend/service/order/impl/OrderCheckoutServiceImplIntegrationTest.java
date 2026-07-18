package org.plishka.backend.service.order.impl;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.plishka.backend.domain.cart.Cart;
import org.plishka.backend.domain.cart.CartItem;
import org.plishka.backend.domain.product.Category;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.order.CreateOrderRequestDto;
import org.plishka.backend.dto.order.OrderDetailDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ConflictException;
import org.plishka.backend.exception.ForbiddenException;
import org.plishka.backend.repository.cart.CartRepository;
import org.plishka.backend.repository.product.CategoryRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.plishka.backend.service.order.OrderCheckoutService;
import org.plishka.backend.testsupport.MySqlIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class OrderCheckoutServiceImplIntegrationTest extends MySqlIntegrationTest {
    private static final String LONG_PRODUCT_NAME = "Premium garden modular lounge set with weatherproof cushions "
            + "and extended walnut-finished corner table";
    private static final String PASSWORD_HASH =
            "012345678901234567890123456789012345678901234567890123456789";

    @Autowired
    private OrderCheckoutService orderCheckoutService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @BeforeEach
    void enableShopMode() {
        jdbcTemplate.update("update system_settings set is_shop_mode_enabled = true where id = 1");
    }

    @Test
    @Transactional
    void checkout_ShouldPersistProductNameSnapshotLongerThanOneHundredCharacters() {
        User user = createUser("long-name");
        Product product = createProduct(LONG_PRODUCT_NAME);
        createCartWithProduct(user, product);
        double successAttemptsBefore = checkoutAttemptCount("success");
        double errorAttemptsBefore = checkoutAttemptCount("error");

        OrderDetailDto order = orderCheckoutService.checkout(
                user.getId(),
                "checkout-long-product-name-" + System.nanoTime(),
                createOrderRequest()
        );

        assertEquals(LONG_PRODUCT_NAME, findStoredProductNameSnapshot(order.orderId()));
        assertEquals(successAttemptsBefore, checkoutAttemptCount("success"));

        // Checkout success metrics are transaction-aware; this test transaction rolls back.
        TestTransaction.end();

        assertEquals(1.0, checkoutAttemptCount("error") - errorAttemptsBefore);
    }

    @Test
    void checkout_ShouldCreateOrderItemsClearCartAndRecordSuccessMetric() {
        User user = createUser("happy-path");
        Product product = createProduct("Checkout Happy Path Product " + System.nanoTime(), 450L);
        createCartWithProduct(user, product, 2);
        double successAttemptsBefore = checkoutAttemptCount("success");

        OrderDetailDto order = orderCheckoutService.checkout(
                user.getId(),
                "checkout-happy-path-" + System.nanoTime(),
                createOrderRequest()
        );

        assertEquals(900L, order.totalPrice());
        assertEquals(1, countOrders(user.getId()));
        assertEquals(1, countOrderItems(order.orderId()));
        assertEquals(0, countCartItems(user.getId()));
        assertEquals(product.getName(), findStoredProductNameSnapshot(order.orderId()));
        assertEquals(1, countAdminNotificationOutboxRows("ORDER_CREATED", order.orderId()));
        assertEquals(1.0, checkoutAttemptCount("success") - successAttemptsBefore);
    }

    @Test
    void checkout_ShouldReturnExistingOrder_WhenIdempotencyKeyAndPayloadMatch() {
        User user = createUser("idempotent");
        Product product = createProduct("Checkout Idempotent Product " + System.nanoTime(), 100L);
        createCartWithProduct(user, product, 1);
        String idempotencyKey = "checkout-idempotent-" + System.nanoTime();
        CreateOrderRequestDto request = createOrderRequest();

        OrderDetailDto firstResponse = orderCheckoutService.checkout(user.getId(), idempotencyKey, request);
        OrderDetailDto secondResponse = orderCheckoutService.checkout(user.getId(), idempotencyKey, request);

        assertEquals(firstResponse.orderId(), secondResponse.orderId());
        assertEquals(1, countOrders(user.getId()));
    }

    @Test
    void checkout_ShouldRejectSameIdempotencyKeyWithDifferentPayload() {
        User user = createUser("idempotency-conflict");
        Product product = createProduct("Checkout Conflict Product " + System.nanoTime(), 100L);
        createCartWithProduct(user, product, 1);
        String idempotencyKey = "checkout-conflict-" + System.nanoTime();

        orderCheckoutService.checkout(user.getId(), idempotencyKey, createOrderRequest());

        assertThrows(
                ConflictException.class,
                () -> orderCheckoutService.checkout(user.getId(), idempotencyKey,
                        new CreateOrderRequestDto("Checkout User", "Kyiv", "+380501234567", "Different notes"))
        );
        assertEquals(1, countOrders(user.getId()));
    }

    @Test
    void checkout_ShouldRejectSameIdempotencyKey_WhenCartChangedAfterSuccessfulCheckout() {
        User user = createUser("idempotency-cart-conflict");
        Product firstProduct = createProduct("Checkout Cart Conflict Product A " + System.nanoTime(), 100L);
        Product secondProduct = createProduct("Checkout Cart Conflict Product B " + System.nanoTime(), 200L);
        createCartWithProduct(user, firstProduct, 1);
        String idempotencyKey = "checkout-cart-conflict-" + System.nanoTime();
        CreateOrderRequestDto request = createOrderRequest();

        OrderDetailDto firstResponse = orderCheckoutService.checkout(user.getId(), idempotencyKey, request);
        addCartItem(user, secondProduct, 1);

        assertThrows(
                ConflictException.class,
                () -> orderCheckoutService.checkout(user.getId(), idempotencyKey, request)
        );
        assertEquals(1, countOrders(user.getId()));
        assertEquals(1, countOrderItems(firstResponse.orderId()));
        assertEquals(1, countCartItems(user.getId()));
    }

    @Test
    void checkout_ShouldRejectEmptyCart() {
        User user = createUser("empty-cart");
        createEmptyCart(user);

        assertThrows(
                BadRequestException.class,
                () -> orderCheckoutService.checkout(user.getId(), "checkout-empty-" + System.nanoTime(),
                        createOrderRequest())
        );
        assertEquals(0, countOrders(user.getId()));
    }

    @Test
    void checkout_ShouldRejectWhenShopModeIsDisabled() {
        jdbcTemplate.update("update system_settings set is_shop_mode_enabled = false where id = 1");
        User user = createUser("shop-disabled");

        assertThrows(
                ForbiddenException.class,
                () -> orderCheckoutService.checkout(user.getId(), "checkout-disabled-" + System.nanoTime(),
                        createOrderRequest())
        );
        assertEquals(0, countOrders(user.getId()));
    }

    private User createUser(String emailPrefix) {
        User user = new User();
        user.setName("Checkout User");
        user.setEmail(emailPrefix + "-" + System.nanoTime() + "@example.com");
        user.setPhone("+380501234567");
        user.setPasswordHash(PASSWORD_HASH);
        user.setEmailVerified(true);
        return userRepository.saveAndFlush(user);
    }

    private Product createProduct(String productName) {
        return createProduct(productName, 10L);
    }

    private Product createProduct(String productName, Long price) {
        Category category = new Category();
        category.setName("Checkout Category " + System.nanoTime());
        category = categoryRepository.saveAndFlush(category);

        Product product = new Product();
        product.setName(productName);
        product.setDescription("Checkout product with name longer than one hundred characters");
        product.setPrice(price);
        product.setCategory(category);
        return productRepository.saveAndFlush(product);
    }

    private void createCartWithProduct(User user, Product product) {
        createCartWithProduct(user, product, 1);
    }

    private void createCartWithProduct(User user, Product product, int quantity) {
        Cart cart = new Cart();
        cart.setUser(user);

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        cart.getCartItems().add(cartItem);

        cartRepository.saveAndFlush(cart);
    }

    private void addCartItem(User user, Product product, int quantity) {
        Cart cart = cartRepository.findAggregateByUserId(user.getId()).orElseThrow();

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        cart.getCartItems().add(cartItem);

        cartRepository.saveAndFlush(cart);
    }

    private void createEmptyCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.saveAndFlush(cart);
    }

    private CreateOrderRequestDto createOrderRequest() {
        return new CreateOrderRequestDto(
                "Checkout User",
                "Kyiv",
                "+380501234567",
                null
        );
    }

    private String findStoredProductNameSnapshot(Long orderId) {
        return jdbcTemplate.queryForObject(
                "select product_name_snapshot from order_items where order_id = ?",
                String.class,
                orderId
        );
    }

    private double checkoutAttemptCount(String outcome) {
        var counter = meterRegistry.find("checkout.attempt").tag("outcome", outcome).counter();
        return counter == null ? 0.0 : counter.count();
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

    private int countAdminNotificationOutboxRows(String notificationType, Long sourceId) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from admin_notification_outbox
                        where notification_type = ? and source_id = ?
                        """,
                Integer.class,
                notificationType,
                sourceId
        ));
    }

    private int countCartItems(Long userId) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from cart_items ci
                        join carts c on c.id = ci.cart_id
                        where c.user_id = ?
                """,
                Integer.class,
                userId
        ));
    }
}
