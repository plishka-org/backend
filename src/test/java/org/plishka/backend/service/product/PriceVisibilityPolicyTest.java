package org.plishka.backend.service.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.service.settings.ShopModeService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PriceVisibilityPolicyTest {
    private static final long PRODUCT_PRICE = 450L;

    @Mock
    private ShopModeService shopModeService;

    private PriceVisibilityPolicy priceVisibilityPolicy;

    @BeforeEach
    void setUp() {
        priceVisibilityPolicy = new PriceVisibilityPolicy(shopModeService);
    }

    @Test
    void visiblePrice_ShouldReturnProductPrice_WhenCurrentPriceIsVisible() {
        assertEquals(PRODUCT_PRICE, priceVisibilityPolicy.visiblePrice(product(), true));
    }

    @Test
    void visiblePrice_ShouldReturnNull_WhenCurrentPriceIsNotVisible() {
        assertNull(priceVisibilityPolicy.visiblePrice(product(), false));
    }

    @Test
    void assertPriceSortAllowed_ShouldRejectPriceSort_WhenCurrentPriceIsNotVisible() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> priceVisibilityPolicy.assertPriceSortAllowed("price,asc", false)
        );

        assertEquals("Price sorting is not available when shop mode is disabled", exception.getMessage());
    }

    @Test
    void assertPriceSortAllowed_ShouldAllowNameSort_WhenCurrentPriceIsNotVisible() {
        assertDoesNotThrow(() -> priceVisibilityPolicy.assertPriceSortAllowed("name,desc", false));
    }

    private Product product() {
        Product product = new Product();
        product.setPrice(PRODUCT_PRICE);
        return product;
    }
}
