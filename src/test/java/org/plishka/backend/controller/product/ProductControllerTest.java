package org.plishka.backend.controller.product;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.dto.product.ProductDetailDto;
import org.plishka.backend.dto.product.ProductMediaDto;
import org.plishka.backend.dto.product.ProductMediaPreviewDto;
import org.plishka.backend.dto.product.ProductSummaryDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.plishka.backend.domain.media.MediaType.IMAGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest extends BaseControllerTest {
    private static final long PRODUCT_ID = 10L;
    private static final long RELATED_PRODUCT_ID = 11L;
    private static final long NOT_FOUND_PRODUCT_ID = 999L;
    private static final long PRODUCT_CATEGORY_ID = 1L;
    private static final long RELATED_PRODUCT_CATEGORY_ID = 2L;
    private static final long PRODUCT_MEDIA_ID = 5L;
    private static final int FIRST_PAGE = 0;
    private static final int PRODUCT_PAGE_SIZE = 16;
    private static final int DEFAULT_PRODUCT_PAGE_SIZE = 16;
    private static final int RELATED_PRODUCT_PAGE_SIZE = 4;
    private static final String PRODUCT_NAME = "Oak Garden Bench";
    private static final String PRODUCT_DESCRIPTION = "Handmade oak garden bench";
    private static final String PRODUCT_CATEGORY_NAME = "Outdoor Tables and Benches";
    private static final long PRODUCT_PRICE = 450L;
    private static final String RELATED_PRODUCT_NAME = "Carved Wooden Wall Decor";
    private static final String RELATED_PRODUCT_CATEGORY_NAME = "Decor Items";
    private static final long RELATED_PRODUCT_PRICE = 320L;
    private static final String PRODUCT_SORT = "name,desc";
    private static final String UNSUPPORTED_PRODUCT_SORT = "rating,desc";
    private static final String CATEGORY_IDS_PARAM = "1,2,3";
    private static final String PRODUCT_MEDIA_KEY =
            "products/10/images/2026/05/550e8400-e29b-41d4-a716-446655440000.jpg";
    private static final List<Long> CATEGORY_FILTER = List.of(1L, 2L, 3L);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void getProducts_ShouldReturnPaginatedProductsAndStatus200() throws Exception {
        ProductSummaryDto product = productSummary();
        PageResponse<ProductSummaryDto> response = new PageResponse<>(
                List.of(product),
                FIRST_PAGE,
                PRODUCT_PAGE_SIZE,
                1,
                1,
                true
        );

        when(productService.getProducts(CATEGORY_FILTER, PRODUCT_SORT, FIRST_PAGE, PRODUCT_PAGE_SIZE))
                .thenReturn(response);

        mockMvc.perform(get("/products")
                        .param("categoryIds", CATEGORY_IDS_PARAM)
                        .param("sort", PRODUCT_SORT)
                        .param("page", String.valueOf(FIRST_PAGE))
                        .param("size", String.valueOf(PRODUCT_PAGE_SIZE))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.content[0].category.categoryId").value(PRODUCT_CATEGORY_ID))
                .andExpect(jsonPath("$.content[0].category.name").value(PRODUCT_CATEGORY_NAME))
                .andExpect(jsonPath("$.content[0].primaryMedia.mediaType").value("IMAGE"))
                .andExpect(jsonPath("$.pageSize").value(PRODUCT_PAGE_SIZE));

        verify(productService).getProducts(CATEGORY_FILTER, PRODUCT_SORT, FIRST_PAGE, PRODUCT_PAGE_SIZE);
    }

    @Test
    void getProducts_ShouldUseDefaultPagination_WhenPaginationIsMissing() throws Exception {
        PageResponse<ProductSummaryDto> response = new PageResponse<>(
                List.of(),
                FIRST_PAGE,
                DEFAULT_PRODUCT_PAGE_SIZE,
                0,
                0,
                true
        );

        when(productService.getProducts(List.of(), null, FIRST_PAGE, DEFAULT_PRODUCT_PAGE_SIZE))
                .thenReturn(response);

        mockMvc.perform(get("/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(FIRST_PAGE))
                .andExpect(jsonPath("$.pageSize").value(DEFAULT_PRODUCT_PAGE_SIZE));

        verify(productService).getProducts(List.of(), null, FIRST_PAGE, DEFAULT_PRODUCT_PAGE_SIZE);
    }

    @Test
    void getProducts_ShouldReturn400_WhenSortIsUnsupported() throws Exception {
        when(productService.getProducts(
                any(),
                eq(UNSUPPORTED_PRODUCT_SORT),
                eq(FIRST_PAGE),
                eq(DEFAULT_PRODUCT_PAGE_SIZE)
        ))
                .thenThrow(new BadRequestException("Unsupported product sort"));

        mockMvc.perform(get("/products")
                        .param("sort", UNSUPPORTED_PRODUCT_SORT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProducts_ShouldReturn400_WhenCategoryFilterContainsMoreThanTwentyIds() throws Exception {
        String categoryIds = LongStream.rangeClosed(1, 21)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));

        mockMvc.perform(get("/products")
                        .param("categoryIds", categoryIds)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProducts_ShouldReturn400_WhenPageIsNegative() throws Exception {
        mockMvc.perform(get("/products")
                        .param("page", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProducts_ShouldReturn400_WhenSizeIsOutOfRange() throws Exception {
        mockMvc.perform(get("/products")
                        .param("size", "101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProduct_ShouldReturnProductDetailsAndStatus200() throws Exception {
        ProductDetailDto response = productDetail();

        when(productService.getProduct(PRODUCT_ID)).thenReturn(response);

        mockMvc.perform(get("/products/{id}", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.category.categoryId").value(PRODUCT_CATEGORY_ID))
                .andExpect(jsonPath("$.media[0].isPrimary").value(true));
    }

    @Test
    void getProduct_ShouldReturn404_WhenProductNotFound() throws Exception {
        when(productService.getProduct(NOT_FOUND_PRODUCT_ID))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(get("/products/{id}", NOT_FOUND_PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProduct_ShouldReturn400_WhenIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/products/{id}", -1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRelatedProducts_ShouldReturnRelatedProductsAndStatus200() throws Exception {
        ProductSummaryDto relatedProduct = relatedProductSummary();

        PageResponse<ProductSummaryDto> response = new PageResponse<>(
                List.of(relatedProduct),
                FIRST_PAGE,
                RELATED_PRODUCT_PAGE_SIZE,
                1,
                1,
                true
        );

        when(productService.getRelatedProducts(PRODUCT_ID, FIRST_PAGE, RELATED_PRODUCT_PAGE_SIZE))
                .thenReturn(response);

        mockMvc.perform(get("/products/{id}/related", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].productId").value(RELATED_PRODUCT_ID))
                .andExpect(jsonPath("$.content[0].category.name").value(RELATED_PRODUCT_CATEGORY_NAME))
                .andExpect(jsonPath("$.pageSize").value(RELATED_PRODUCT_PAGE_SIZE))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getRelatedProducts_ShouldUseDefaultPagination_WhenPaginationIsMissing() throws Exception {
        PageResponse<ProductSummaryDto> response = new PageResponse<>(
                List.of(),
                FIRST_PAGE,
                RELATED_PRODUCT_PAGE_SIZE,
                0,
                0,
                true
        );

        when(productService.getRelatedProducts(PRODUCT_ID, FIRST_PAGE, RELATED_PRODUCT_PAGE_SIZE))
                .thenReturn(response);

        mockMvc.perform(get("/products/{id}/related", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(FIRST_PAGE))
                .andExpect(jsonPath("$.pageSize").value(RELATED_PRODUCT_PAGE_SIZE));

        verify(productService).getRelatedProducts(PRODUCT_ID, FIRST_PAGE, RELATED_PRODUCT_PAGE_SIZE);
    }

    @Test
    void getRelatedProducts_ShouldReturn400_WhenIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/products/{id}/related", -1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private static ProductSummaryDto productSummary() {
        return new ProductSummaryDto(
                PRODUCT_ID,
                PRODUCT_NAME,
                productCategory(),
                PRODUCT_PRICE,
                new ProductMediaPreviewDto(
                        PRODUCT_MEDIA_ID,
                        PRODUCT_MEDIA_KEY,
                        IMAGE
                )
        );
    }

    private static ProductDetailDto productDetail() {
        return new ProductDetailDto(
                PRODUCT_ID,
                PRODUCT_NAME,
                PRODUCT_DESCRIPTION,
                PRODUCT_PRICE,
                productCategory(),
                List.of(new ProductMediaDto(
                        PRODUCT_MEDIA_ID,
                        PRODUCT_MEDIA_KEY,
                        IMAGE,
                        true,
                        1
                ))
        );
    }

    private static ProductSummaryDto relatedProductSummary() {
        return new ProductSummaryDto(
                RELATED_PRODUCT_ID,
                RELATED_PRODUCT_NAME,
                new CategoryDto(RELATED_PRODUCT_CATEGORY_ID, RELATED_PRODUCT_CATEGORY_NAME),
                RELATED_PRODUCT_PRICE,
                null
        );
    }

    private static CategoryDto productCategory() {
        return new CategoryDto(PRODUCT_CATEGORY_ID, PRODUCT_CATEGORY_NAME);
    }
}
