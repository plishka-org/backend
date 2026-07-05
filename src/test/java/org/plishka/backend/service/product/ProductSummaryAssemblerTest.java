package org.plishka.backend.service.product;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.dto.product.ProductSummaryDto;
import org.plishka.backend.mapper.product.ProductMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSummaryAssemblerTest {
    private static final long PRODUCT_ID = 10L;
    private static final long PRODUCT_PRICE = 450L;

    @Mock
    private ProductMediaQueryService productMediaQueryService;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private PriceVisibilityPolicy priceVisibilityPolicy;

    private ProductSummaryAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new ProductSummaryAssembler(
                productMediaQueryService,
                productMapper,
                priceVisibilityPolicy
        );
    }

    @Test
    void toDtos_ShouldHidePrices_WhenShopModeIsDisabled() {
        Product product = product();
        ProductSummaryDto summary = new ProductSummaryDto(PRODUCT_ID, product.getName(), null, null, null);

        when(priceVisibilityPolicy.isCurrentPriceVisible()).thenReturn(false);
        when(productMediaQueryService.findPrimaryMediaForProducts(List.of(product))).thenReturn(Map.of());
        when(priceVisibilityPolicy.visiblePrice(product, false)).thenReturn(null);
        when(productMapper.toSummaryDto(product, null, null)).thenReturn(summary);

        List<ProductSummaryDto> result = assembler.toDtos(List.of(product));

        assertEquals(List.of(summary), result);
        verify(priceVisibilityPolicy).visiblePrice(product, false);
        verify(productMapper).toSummaryDto(product, null, null);
    }

    private Product product() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Oak Garden Bench");
        product.setPrice(PRODUCT_PRICE);
        return product;
    }
}
