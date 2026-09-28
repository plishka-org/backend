package org.plishka.backend.service.cart.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.plishka.backend.domain.product.Category;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.user.Role;
import org.plishka.backend.domain.user.User;
import org.plishka.backend.dto.cart.AddCartItemRequestDto;
import org.plishka.backend.dto.cart.CartSummaryDto;
import org.plishka.backend.dto.cart.MergeCartRequestDto;
import org.plishka.backend.dto.cart.UpdateCartItemRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.cart.CartRepository;
import org.plishka.backend.repository.product.CategoryRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.repository.user.RoleRepository;
import org.plishka.backend.repository.user.UserRepository;
import org.plishka.backend.service.cart.CartService;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.plishka.backend.testsupport.MySqlIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class CartServiceImplIntegrationTest extends MySqlIntegrationTest {
    private static final String PASSWORD = "Password1";

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @BeforeEach
    void enableShopMode() {
        jdbcTemplate.update("update system_settings set is_shop_mode_enabled = true where id = 1");
    }

    @Test
    void getCart_ShouldReturnEmptySummary_WhenCartDoesNotExist() {
        User user = createUser("empty-cart");

        CartSummaryDto cart = cartService.getCart(user.getId());

        assertEquals(0L, cart.totalPrice());
        assertEquals(List.of(), cart.items());
    }

    @Test
    void addItem_ShouldCreateCartAndPersistItem() {
        User user = createUser("add-item");
        Product product = createProduct("Add Item Product", 450L);

        CartSummaryDto cart = cartService.addItem(user.getId(), new AddCartItemRequestDto(product.getId(), 2));

        assertEquals(900L, cart.totalPrice());
        assertEquals(1, cart.items().size());
        assertEquals(product.getId(), cart.items().getFirst().productId());
        assertEquals(2, cart.items().getFirst().quantity());
        assertEquals(1, countCartItems(user.getId()));
    }

    @Test
    void addItem_ShouldIncreaseExistingQuantityAndRejectOverflow() {
        User user = createUser("add-existing");
        Product product = createProduct("Add Existing Product", 100L);

        cartService.addItem(user.getId(), new AddCartItemRequestDto(product.getId(), 30));
        CartSummaryDto cart = cartService.addItem(user.getId(), new AddCartItemRequestDto(product.getId(), 20));

        assertEquals(50, storedQuantity(user.getId(), product.getId()));
        assertEquals(5_000L, cart.totalPrice());
        assertThrows(
                BadRequestException.class,
                () -> cartService.addItem(user.getId(), new AddCartItemRequestDto(product.getId(), 1))
        );
        assertEquals(50, storedQuantity(user.getId(), product.getId()));
    }

    @Test
    void updateItem_ShouldPersistNewQuantityAndRecalculateTotal() {
        User user = createUser("update");
        Product product = createProduct("Update Product", 250L);
        cartService.addItem(user.getId(), new AddCartItemRequestDto(product.getId(), 2));

        CartSummaryDto cart = cartService.updateItem(
                user.getId(),
                product.getId(),
                new UpdateCartItemRequestDto(3)
        );

        assertEquals(3, storedQuantity(user.getId(), product.getId()));
        assertEquals(750L, cart.totalPrice());
    }

    @Test
    void removeItem_ShouldDeleteCartItemRow() {
        User user = createUser("remove");
        Product product = createProduct("Remove Product", 120L);
        cartService.addItem(user.getId(), new AddCartItemRequestDto(product.getId(), 2));

        CartSummaryDto cart = cartService.removeItem(user.getId(), product.getId());

        assertEquals(0, countCartItems(user.getId()));
        assertEquals(0L, cart.totalPrice());
    }

    @Test
    void clearCart_ShouldDeleteAllCartItemRows() {
        User user = createUser("clear");
        Product firstProduct = createProduct("Clear First Product", 100L);
        Product secondProduct = createProduct("Clear Second Product", 200L);
        cartService.addItem(user.getId(), new AddCartItemRequestDto(firstProduct.getId(), 1));
        cartService.addItem(user.getId(), new AddCartItemRequestDto(secondProduct.getId(), 1));

        cartService.clearCart(user.getId());

        assertEquals(0, countCartItems(user.getId()));
    }

    @Test
    void mergeCart_ShouldAggregateDuplicateItemsAndCapQuantity() {
        User user = createUser("merge");
        Product product = createProduct("Merge Product", 10L);
        cartService.addItem(user.getId(), new AddCartItemRequestDto(product.getId(), 45));

        CartSummaryDto cart = cartService.mergeCart(user.getId(), new MergeCartRequestDto(List.of(
                new AddCartItemRequestDto(product.getId(), 10),
                new AddCartItemRequestDto(product.getId(), 10)
        )));

        assertEquals(50, storedQuantity(user.getId(), product.getId()));
        assertEquals(500L, cart.totalPrice());
    }

    @Test
    void addItem_ShouldThrow404AndNotCreateCart_WhenProductIsMissing() {
        User user = createUser("missing-product");

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.addItem(user.getId(), new AddCartItemRequestDto(999_999L, 1))
        );

        assertEquals(0, countCarts(user.getId()));
    }

    private User createUser(String prefix) {
        User user = User.builder()
                .name("Cart User")
                .email(uniqueEmail(prefix))
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .roles(Set.of(userRole()))
                .isEmailVerified(true)
                .isBanned(false)
                .build();
        return userRepository.saveAndFlush(user);
    }

    private Role userRole() {
        return roleRepository.findByName(Role.RoleName.USER).orElseThrow();
    }

    private Product createProduct(String namePrefix, Long price) {
        Category category = new Category();
        category.setName(uniqueName(namePrefix + " Category"));
        category = categoryRepository.saveAndFlush(category);

        Product product = new Product();
        product.setName(uniqueName(namePrefix));
        product.setDescription("Integration test product");
        product.setPrice(price);
        product.setCategory(category);
        return productRepository.saveAndFlush(product);
    }

    private int countCarts(Long userId) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(
                "select count(*) from carts where user_id = ?",
                Integer.class,
                userId
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

    private int storedQuantity(Long userId, Long productId) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(
                """
                        select ci.quantity
                        from cart_items ci
                        join carts c on c.id = ci.cart_id
                        where c.user_id = ? and ci.product_id = ?
                        """,
                Integer.class,
                userId,
                productId
        ));
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + System.nanoTime() + "@example.com";
    }

    private String uniqueName(String prefix) {
        return prefix + " " + System.nanoTime();
    }
}
