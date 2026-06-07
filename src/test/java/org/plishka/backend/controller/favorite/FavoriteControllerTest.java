package org.plishka.backend.controller.favorite;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.favorite.FavoriteAddResult;
import org.plishka.backend.dto.favorite.FavoriteDto;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.dto.product.ProductSummaryDto;
import org.plishka.backend.service.favorite.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FavoriteControllerTest extends BaseControllerTest {
    private static final Long USER_ID = 100L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long FAVORITE_ID = 20L;
    private static final Long CATEGORY_ID = 50L;
    private static final int PAGE_NUMBER = 0;
    private static final int FAVORITES_PAGE_SIZE = 16;
    private static final String USER_EMAIL = "serhii@example.com";
    private static final String PRODUCT_NAME = "Oak Bench";
    private static final String CATEGORY_NAME = "Furniture";
    private static final Instant CREATED_AT = Instant.parse("2026-05-29T10:15:30Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteService favoriteService;

    @Test
    void getFavorites_ShouldReturnPaginatedFavoritesAndStatus200() throws Exception {
        PageResponse<FavoriteDto> response = new PageResponse<>(
                List.of(favorite()),
                PAGE_NUMBER,
                FAVORITES_PAGE_SIZE,
                1,
                1,
                true
        );

        when(favoriteService.getFavorites(USER_ID, PAGE_NUMBER, FAVORITES_PAGE_SIZE)).thenReturn(response);

        mockMvc.perform(get("/users/me/favorites")
                        .with(authenticatedUser(USER_ID, USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].favoriteId").value(FAVORITE_ID))
                .andExpect(jsonPath("$.content[0].product.productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.pageSize").value(FAVORITES_PAGE_SIZE));
    }

    @Test
    void addFavorite_ShouldReturn201_WhenFavoriteIsCreated() throws Exception {
        when(favoriteService.addFavorite(USER_ID, PRODUCT_ID))
                .thenReturn(new FavoriteAddResult(favorite(), true));

        mockMvc.perform(post("/users/me/favorites/{productId}", PRODUCT_ID)
                        .with(authenticatedUser(USER_ID, USER_EMAIL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.favoriteId").value(FAVORITE_ID));
    }

    @Test
    void addFavorite_ShouldReturn200_WhenFavoriteAlreadyExists() throws Exception {
        when(favoriteService.addFavorite(USER_ID, PRODUCT_ID))
                .thenReturn(new FavoriteAddResult(favorite(), false));

        mockMvc.perform(post("/users/me/favorites/{productId}", PRODUCT_ID)
                        .with(authenticatedUser(USER_ID, USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteId").value(FAVORITE_ID));
    }

    @Test
    void deleteFavorite_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/users/me/favorites/{productId}", PRODUCT_ID)
                        .with(authenticatedUser(USER_ID, USER_EMAIL)))
                .andExpect(status().isNoContent());

        verify(favoriteService).deleteFavorite(USER_ID, PRODUCT_ID);
    }

    private static FavoriteDto favorite() {
        return new FavoriteDto(FAVORITE_ID, CREATED_AT, productSummary());
    }

    private static ProductSummaryDto productSummary() {
        return new ProductSummaryDto(
                PRODUCT_ID,
                PRODUCT_NAME,
                new CategoryDto(CATEGORY_ID, CATEGORY_NAME),
                new BigDecimal("1200.00"),
                null
        );
    }
}
