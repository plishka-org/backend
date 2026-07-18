package org.plishka.backend.repository;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.plishka.backend.domain.notification.AdminNotificationOutbox;
import org.plishka.backend.domain.notification.AdminNotificationOutboxStatus;
import org.plishka.backend.domain.notification.AdminNotificationType;
import org.plishka.backend.domain.storage.StorageDeletionOutbox;
import org.plishka.backend.domain.storage.StorageDeletionOutboxStatus;
import org.plishka.backend.repository.notification.AdminNotificationOutboxRepository;
import org.plishka.backend.repository.storage.StorageDeletionOutboxRepository;
import org.plishka.backend.testsupport.MySqlIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseIntegrityMySqlIntegrationTest extends MySqlIntegrationTest {
    private static final String PASSWORD_HASH =
            "012345678901234567890123456789012345678901234567890123456789";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StorageDeletionOutboxRepository storageDeletionOutboxRepository;

    @Autowired
    private AdminNotificationOutboxRepository adminNotificationOutboxRepository;

    @Test
    void productMedia_ShouldAllowOnlyOnePrimaryImagePerProduct() {
        Long productId = createProduct("single-primary-product");
        insertProductMedia(productId, "products/a/images/first.jpg", "IMAGE", 1, true);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertProductMedia(productId, "products/a/images/second.jpg", "IMAGE", 2, true)
        );
    }

    @Test
    void reviewMedia_ShouldAllowOnlyOnePrimaryImagePerReview() {
        Long reviewId = createReview("single-primary-review");
        insertReviewMedia(reviewId, "reviews/a/images/first.jpg", "IMAGE", 1, true);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertReviewMedia(reviewId, "reviews/a/images/second.jpg", "IMAGE", 2, true)
        );
    }

    @Test
    void orderItemProductReference_ShouldBeSetNull_WhenProductIsDeleted() {
        Long productId = createProduct("order-item-set-null");
        Long userId = createUser("order-item-set-null");
        Long orderId = createOrder(userId, "order-item-set-null");
        Long orderItemId = insertOrderItem(orderId, productId);

        jdbcTemplate.update("delete from products where id = ?", productId);

        Long storedProductId = jdbcTemplate.queryForObject(
                "select product_id from order_items where id = ?",
                Long.class,
                orderItemId
        );
        assertNull(storedProductId);
    }

    @Test
    void cartItemProductReference_ShouldRestrictProductDelete() {
        Long productId = createProduct("cart-item-restrict");
        Long userId = createUser("cart-item-restrict");
        Long cartId = createCart(userId);
        insertCartItem(cartId, productId);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("delete from products where id = ?", productId)
        );
    }

    @Test
    @Transactional
    void storageDeletionOutboxRepository_ShouldFindDueEntriesInAttemptOrderWithLock() {
        storageDeletionOutboxRepository.deleteAllInBatch();

        Instant now = Instant.now();
        StorageDeletionOutbox later = createOutboxEntry("storage/later.jpg", now.plusSeconds(60));
        StorageDeletionOutbox firstDue = createOutboxEntry("storage/first.jpg", now.minusSeconds(60));
        StorageDeletionOutbox secondDue = createOutboxEntry("storage/second.jpg", now.minusSeconds(30));

        List<StorageDeletionOutbox> dueEntries = storageDeletionOutboxRepository.findDueForUpdate(
                StorageDeletionOutboxStatus.PENDING,
                now,
                PageRequest.of(0, 10)
        );

        assertTrue(dueEntries.stream().noneMatch(entry -> entry.getId().equals(later.getId())));
        assertEquals(firstDue.getId(), dueEntries.get(0).getId());
        assertEquals(secondDue.getId(), dueEntries.get(1).getId());
    }

    @Test
    @Transactional
    void adminNotificationOutboxRepository_ShouldFindDueEntriesInAttemptOrderWithLock() {
        adminNotificationOutboxRepository.deleteAllInBatch();

        Instant now = Instant.now();
        AdminNotificationOutbox later = createAdminNotificationOutboxEntry(1001L, now.plusSeconds(60));
        AdminNotificationOutbox firstDue = createAdminNotificationOutboxEntry(1002L, now.minusSeconds(60));
        AdminNotificationOutbox secondDue = createAdminNotificationOutboxEntry(1003L, now.minusSeconds(30));

        List<AdminNotificationOutbox> dueEntries = adminNotificationOutboxRepository.findDueForUpdate(
                AdminNotificationOutboxStatus.PENDING,
                now,
                PageRequest.of(0, 10)
        );

        assertTrue(dueEntries.stream().noneMatch(entry -> entry.getId().equals(later.getId())));
        assertEquals(firstDue.getId(), dueEntries.get(0).getId());
        assertEquals(secondDue.getId(), dueEntries.get(1).getId());
    }

    private Long createUser(String prefix) {
        return insertAndReturnId(
                """
                        insert into users
                            (name, email, password_hash, is_banned, is_email_verified, created_at, updated_at)
                        values (?, ?, ?, false, true, current_timestamp(6), current_timestamp(6))
                        """,
                "MySQL User",
                prefix + "-" + System.nanoTime() + "@example.com",
                PASSWORD_HASH
        );
    }

    private Long createCategory(String prefix) {
        return insertAndReturnId(
                "insert into categories (name, created_at, updated_at) values (?, current_timestamp(6), "
                        + "current_timestamp(6))",
                prefix + "-category-" + System.nanoTime()
        );
    }

    private Long createProduct(String prefix) {
        Long categoryId = createCategory(prefix);
        return insertAndReturnId(
                """
                        insert into products
                            (name, description, price, category_id, created_at, updated_at)
                        values (?, 'MySQL integrity product', 100, ?, current_timestamp(6), current_timestamp(6))
                        """,
                prefix + "-product-" + System.nanoTime(),
                categoryId
        );
    }

    private Long createReview(String prefix) {
        return insertAndReturnId(
                """
                        insert into reviews
                            (author_name, content, is_featured, created_at, updated_at)
                        values (?, 'MySQL integrity review', false, current_timestamp(6), current_timestamp(6))
                        """,
                prefix + "-author-" + System.nanoTime()
        );
    }

    private Long createOrder(Long userId, String prefix) {
        return insertAndReturnId(
                """
                        insert into orders
                            (user_id, order_number, idempotency_key, request_hash, customer_name, total_price,
                             delivery_city, phone, notes, created_at, updated_at)
                        values (?, ?, ?, ?, 'MySQL User', 100, 'Kyiv', '+380501234567', null,
                                current_timestamp(6), current_timestamp(6))
                        """,
                userId,
                "MYSQL-" + System.nanoTime(),
                prefix + "-idempotency-" + System.nanoTime(),
                "0123456789012345678901234567890123456789012345678901234567890123"
        );
    }

    private Long createCart(Long userId) {
        return insertAndReturnId(
                "insert into carts (user_id, created_at, updated_at) values (?, current_timestamp(6), "
                        + "current_timestamp(6))",
                userId
        );
    }

    private Long insertOrderItem(Long orderId, Long productId) {
        return insertAndReturnId(
                """
                        insert into order_items
                            (order_id, product_id, product_name_snapshot, category_name_snapshot, quantity,
                             unit_price, line_total, created_at, updated_at)
                        values (?, ?, 'Snapshot product', 'Snapshot category', 1, 100, 100,
                                current_timestamp(6), current_timestamp(6))
                        """,
                orderId,
                productId
        );
    }

    private void insertCartItem(Long cartId, Long productId) {
        jdbcTemplate.update(
                """
                        insert into cart_items
                            (cart_id, product_id, quantity, created_at, updated_at)
                        values (?, ?, 1, current_timestamp(6), current_timestamp(6))
                        """,
                cartId,
                productId
        );
    }

    private void insertProductMedia(
            Long productId,
            String s3Key,
            String mediaType,
            int displayOrder,
            boolean primary
    ) {
        jdbcTemplate.update(
                """
                        insert into product_media
                            (product_id, s3_key, media_type, display_order, is_primary, created_at, updated_at)
                        values (?, ?, ?, ?, ?, current_timestamp(6), current_timestamp(6))
                        """,
                productId,
                s3Key + "-" + System.nanoTime(),
                mediaType,
                displayOrder,
                primary
        );
    }

    private void insertReviewMedia(
            Long reviewId,
            String s3Key,
            String mediaType,
            int displayOrder,
            boolean primary
    ) {
        jdbcTemplate.update(
                """
                        insert into review_media
                            (review_id, s3_key, media_type, display_order, is_primary, created_at, updated_at)
                        values (?, ?, ?, ?, ?, current_timestamp(6), current_timestamp(6))
                        """,
                reviewId,
                s3Key + "-" + System.nanoTime(),
                mediaType,
                displayOrder,
                primary
        );
    }

    private StorageDeletionOutbox createOutboxEntry(String s3Key, Instant nextAttemptAt) {
        StorageDeletionOutbox entry = new StorageDeletionOutbox();
        entry.setS3Key(s3Key + "-" + System.nanoTime());
        entry.setStatus(StorageDeletionOutboxStatus.PENDING);
        entry.setAttempts(0);
        entry.setNextAttemptAt(nextAttemptAt);
        return storageDeletionOutboxRepository.saveAndFlush(entry);
    }

    private AdminNotificationOutbox createAdminNotificationOutboxEntry(Long sourceId, Instant nextAttemptAt) {
        AdminNotificationOutbox entry = new AdminNotificationOutbox();
        entry.setNotificationType(AdminNotificationType.ORDER_CREATED);
        entry.setSourceId(sourceId);
        entry.setRecipient("admin@example.com");
        entry.setSubject("subject " + sourceId);
        entry.setBody("body " + sourceId);
        entry.setStatus(AdminNotificationOutboxStatus.PENDING);
        entry.setAttempts(0);
        entry.setNextAttemptAt(nextAttemptAt);
        return adminNotificationOutboxRepository.saveAndFlush(entry);
    }

    private Long insertAndReturnId(String sql, Object... args) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql);
        factory.setReturnGeneratedKeys(true);
        jdbcTemplate.update(factory.newPreparedStatementCreator(Arrays.asList(args)), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }
}
