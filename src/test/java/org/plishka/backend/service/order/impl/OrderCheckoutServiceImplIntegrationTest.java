package org.plishka.backend.service.order.impl;

import org.junit.jupiter.api.Test;
import org.plishka.backend.domain.cart.Cart;
import org.plishka.backend.domain.cart.CartItem;
import org.plishka.backend.domain.product.Category;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.order.CreateOrderRequestDto;
import org.plishka.backend.repository.cart.CartRepository;
import org.plishka.backend.repository.product.CategoryRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.plishka.backend.service.order.OrderCheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderCheckoutServiceImplIntegrationTest {
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

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @Test
    void checkout_ShouldPersistProductNameSnapshotLongerThanOneHundredCharacters() {
        User user = createUser();
        Product product = createProduct(LONG_PRODUCT_NAME);
        createCartWithProduct(user, product);

        orderCheckoutService.checkout(user.getId(), "checkout-long-product-name", createOrderRequest());

        assertEquals(LONG_PRODUCT_NAME, findStoredProductNameSnapshot());
    }

    private User createUser() {
        User user = new User();
        user.setName("Checkout User");
        user.setEmail("checkout-long-product-name@example.com");
        user.setPhone("+380501234567");
        user.setPasswordHash(PASSWORD_HASH);
        user.setEmailVerified(true);
        return userRepository.saveAndFlush(user);
    }

    private Product createProduct(String productName) {
        Category category = new Category();
        category.setName("Checkout Long Name Category");
        category = categoryRepository.saveAndFlush(category);

        Product product = new Product();
        product.setName(productName);
        product.setDescription("Checkout product with name longer than one hundred characters");
        product.setPrice(10L);
        product.setCategory(category);
        return productRepository.saveAndFlush(product);
    }

    private void createCartWithProduct(User user, Product product) {
        Cart cart = new Cart();
        cart.setUser(user);

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cart.getCartItems().add(cartItem);

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

    private String findStoredProductNameSnapshot() {
        return jdbcTemplate.queryForObject(
                "select product_name_snapshot from order_items",
                String.class
        );
    }
}
