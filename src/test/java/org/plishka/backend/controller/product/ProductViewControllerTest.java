package org.plishka.backend.controller.product;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.dto.product.ProductSummaryDto;
import org.plishka.backend.dto.product.ProductViewDto;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.product.ProductViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductViewControllerTest extends BaseControllerTest {
    private static final Long USER_ID = 100L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long NOT_FOUND_PRODUCT_ID = 999L;
    private static final Long PRODUCT_VIEW_ID = 30L;
    private static final Long CATEGORY_ID = 1L;
    private static final String USER_EMAIL = "serhii@example.com";
    private static final String PRODUCT_NAME = "Oak Bench";
    private static final String CATEGORY_NAME = "Furniture";
    private static final long PRODUCT_PRICE = 1200L;
    private static final Instant VIEWED_AT = Instant.parse("2026-05-29T10:15:30Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductViewService productViewService;

    @Test
    void recordProductView_ShouldReturnProductViewAndStatus200() throws Exception {
        when(productViewService.recordProductView(USER_ID, PRODUCT_ID)).thenReturn(productView());

        mockMvc.perform(post("/products/{id}/view", PRODUCT_ID)
                        .with(authenticatedUser(USER_ID, USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productViewId").value(PRODUCT_VIEW_ID))
                .andExpect(jsonPath("$.product.productId").value(PRODUCT_ID));
    }

    @Test
    void getViewedProducts_ShouldReturnViewedProductsAndStatus200() throws Exception {
        when(productViewService.getViewedProducts(USER_ID)).thenReturn(List.of(productView()));

        mockMvc.perform(get("/users/me/viewed")
                        .with(authenticatedUser(USER_ID, USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productViewId").value(PRODUCT_VIEW_ID))
                .andExpect(jsonPath("$[0].product.productId").value(PRODUCT_ID));
    }

    @Test
    void recordProductView_ShouldReturn404_WhenProductIsMissing() throws Exception {
        when(productViewService.recordProductView(USER_ID, NOT_FOUND_PRODUCT_ID))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(post("/products/{id}/view", NOT_FOUND_PRODUCT_ID)
                        .with(authenticatedUser(USER_ID, USER_EMAIL)))
                .andExpect(status().isNotFound());
    }

    @Test
    void recordProductView_ShouldReturn400_WhenProductIdIsNotPositive() throws Exception {
        mockMvc.perform(post("/products/{id}/view", -1L)
                        .with(authenticatedUser(USER_ID, USER_EMAIL)))
                .andExpect(status().isBadRequest());
    }

    private static ProductViewDto productView() {
        return new ProductViewDto(PRODUCT_VIEW_ID, VIEWED_AT, new ProductSummaryDto(
                PRODUCT_ID,
                PRODUCT_NAME,
                new CategoryDto(CATEGORY_ID, CATEGORY_NAME),
                PRODUCT_PRICE,
                null
        ));
    }
}
