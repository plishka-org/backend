package org.plishka.backend.service.admin.catalog.product;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.domain.home.HomePageProduct;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.domain.product.Category;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.product.ProductMedia;
import org.plishka.backend.dto.admin.common.SelectionMode;
import org.plishka.backend.dto.admin.category.CategoryDeleteStrategy;
import org.plishka.backend.dto.admin.product.BulkProductPriceOperation;
import org.plishka.backend.dto.admin.product.BulkProductPriceRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.repository.home.HomePageProductRepository;
import org.plishka.backend.repository.product.CategoryRepository;
import org.plishka.backend.repository.product.ProductMediaRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.service.admin.catalog.category.AdminCategoryService;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminProductServiceImplIntegrationTest {
    @Autowired
    private AdminProductService adminProductService;

    @Autowired
    private AdminCategoryService adminCategoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMediaRepository productMediaRepository;

    @Autowired
    private HomePageProductRepository homePageProductRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @Test
    void deleteCategory_ShouldKeepProductsWithoutCategory_WhenStrategyKeepsProducts() {
        Product product = createProductWithCategory();
        Long categoryId = product.getCategory().getId();
        createHomeProduct(product);

        adminCategoryService.deleteCategory(categoryId, CategoryDeleteStrategy.KEEP_PRODUCTS, null);

        assertNull(findStoredCategoryId(product.getId()));
        assertEquals(0, countHomeRows(product.getId()));
    }

    @Test
    void deleteProduct_ShouldEnqueueMediaDeletionBeforeCascadeDeletesMediaRows() {
        Product product = createProductWithCategory("Product With Media", "Category With Media");
        String s3Key = "products/" + product.getId()
                + "/images/2026/06/123e4567-e89b-12d3-a456-426614174000.jpg";
        createProductMedia(product, s3Key);

        adminProductService.deleteProduct(product.getId());

        assertEquals(1, countStorageDeletionOutboxRows(s3Key));
        assertEquals(0, countProductMediaRows(product.getId()));
    }

    @Test
    void updatePrices_ShouldRejectOverflowAndKeepStoredPrices() {
        Product firstProduct = createProductWithCategory("Bulk Price First", "Bulk Price Category First");
        Product secondProduct = createProductWithCategory("Bulk Price Second", "Bulk Price Category Second");
        setStoredPrice(secondProduct.getId(), 10_000_000L);
        entityManager.clear();

        BulkProductPriceRequestDto request = new BulkProductPriceRequestDto(
                SelectionMode.SELECTED,
                List.of(firstProduct.getId(), secondProduct.getId()),
                null,
                BulkProductPriceOperation.INCREASE_AMOUNT,
                1L
        );

        assertThrows(BadRequestException.class, () -> adminProductService.updatePrices(request));
        assertEquals(10L, findStoredPrice(firstProduct.getId()));
        assertEquals(10_000_000L, findStoredPrice(secondProduct.getId()));
    }

    private Product createProductWithCategory() {
        return createProductWithCategory("Bulk Clear Product", "Bulk Clear Category");
    }

    private Product createProductWithCategory(String productName, String categoryName) {
        Category category = new Category();
        category.setName(categoryName);
        category = categoryRepository.saveAndFlush(category);

        Product product = new Product();
        product.setName(productName);
        product.setDescription("Bulk clear category integration test product");
        product.setPrice(10L);
        product.setCategory(category);
        return productRepository.saveAndFlush(product);
    }

    private void createHomeProduct(Product product) {
        HomePageProduct homeProduct = new HomePageProduct();
        homeProduct.setProduct(product);
        homeProduct.setDisplayOrder(product.getId().intValue());
        homePageProductRepository.saveAndFlush(homeProduct);
    }

    private void createProductMedia(Product product, String s3Key) {
        ProductMedia media = new ProductMedia();
        media.setProduct(product);
        media.setS3Key(s3Key);
        media.setMediaType(MediaType.IMAGE);
        media.setIsPrimary(true);
        media.setDisplayOrder(1);
        productMediaRepository.saveAndFlush(media);
    }

    private Long findStoredCategoryId(Long productId) {
        return jdbcTemplate.queryForObject(
                "select category_id from products where id = ?",
                (resultSet, rowNumber) -> resultSet.getObject("category_id", Long.class),
                productId
        );
    }

    private Long findStoredPrice(Long productId) {
        return jdbcTemplate.queryForObject(
                "select price from products where id = ?",
                Long.class,
                productId
        );
    }

    private void setStoredPrice(Long productId, Long price) {
        jdbcTemplate.update(
                "update products set price = ? where id = ?",
                price,
                productId
        );
    }

    private Integer countHomeRows(Long productId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from home_page_products where product_id = ?",
                Integer.class,
                productId
        );
    }

    private Integer countProductMediaRows(Long productId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from product_media where product_id = ?",
                Integer.class,
                productId
        );
    }

    private Integer countStorageDeletionOutboxRows(String s3Key) {
        return jdbcTemplate.queryForObject(
                "select count(*) from storage_deletion_outbox where s3_key = ?",
                Integer.class,
                s3Key
        );
    }
}
