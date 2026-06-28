package org.plishka.backend.controller.admin;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.admin.common.BulkOperationResultDto;
import org.plishka.backend.dto.admin.product.AdminProductDetailDto;
import org.plishka.backend.dto.admin.product.AdminProductListRequestDto;
import org.plishka.backend.dto.admin.product.AdminProductRequestDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.dto.product.ProductMediaDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.admin.catalog.home.AdminHomeProductService;
import org.plishka.backend.service.admin.catalog.product.AdminProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminProductControllerTest extends BaseControllerTest {
    private static final long PRODUCT_ID = 10L;
    private static final long CATEGORY_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminProductService adminProductService;

    @MockitoBean
    private AdminHomeProductService adminHomeProductService;

    @Test
    void getProducts_ShouldReturnPaginatedProducts() throws Exception {
        PageResponse<AdminProductDetailDto> response = new PageResponse<>(
                List.of(product()),
                0,
                20,
                1,
                1,
                true
        );
        when(adminProductService.getProducts(any(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/admin/products")
                        .param("categoryIds", "1")
                        .param("uncategorized", "true")
                        .param("search", "bench")
                        .param("sort", "price,desc")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.content[0].media[0].mediaType").value("IMAGE"));

        ArgumentCaptor<AdminProductListRequestDto> requestCaptor =
                ArgumentCaptor.forClass(AdminProductListRequestDto.class);
        verify(adminProductService).getProducts(requestCaptor.capture(), eq(2), eq(10));
        AdminProductListRequestDto request = requestCaptor.getValue();
        assertEquals(List.of(1L), request.categoryIds());
        assertEquals(true, request.uncategorized());
        assertEquals("bench", request.search());
        assertEquals("price,desc", request.sort());
    }

    @Test
    void getProduct_ShouldReturn404_WhenProductMissing() throws Exception {
        when(adminProductService.getProduct(PRODUCT_ID))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(get("/admin/products/{id}", PRODUCT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_ShouldReturn201_WhenRequestIsValid() throws Exception {
        AdminProductRequestDto request = request();
        when(adminProductService.createProduct(any(AdminProductRequestDto.class))).thenReturn(product());

        mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID));
    }

    @Test
    void createProduct_ShouldReturn400_WhenPriceIsInvalid() throws Exception {
        AdminProductRequestDto request = new AdminProductRequestDto(
                "Bench",
                "Description",
                BigDecimal.ZERO,
                CATEGORY_ID
        );

        mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_ShouldReturn400_WhenCategoryIsMissing() throws Exception {
        AdminProductRequestDto request = new AdminProductRequestDto(
                "Bench",
                "Description",
                new BigDecimal("10.00"),
                null
        );

        mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProduct_ShouldReturnProduct_WhenRequestIsValid() throws Exception {
        when(adminProductService.updateProduct(eq(PRODUCT_ID), any(AdminProductRequestDto.class))).thenReturn(product());

        mockMvc.perform(put("/admin/products/{id}", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID));
    }

    @Test
    void deleteProduct_ShouldReturn204_WhenProductExists() throws Exception {
        mockMvc.perform(delete("/admin/products/{id}", PRODUCT_ID))
                .andExpect(status().isNoContent());

        verify(adminProductService).deleteProduct(PRODUCT_ID);
    }

    @Test
    void deleteProducts_ShouldReturnAffectedCount() throws Exception {
        when(adminProductService.deleteProducts(any())).thenReturn(new BulkOperationResultDto(2));

        mockMvc.perform(post("/admin/products/bulk/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "selectionMode": "SELECTED",
                                  "productIds": [1, 2]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedCount").value(2));
    }

    @Test
    void updatePrices_ShouldReturnAffectedCount() throws Exception {
        when(adminProductService.updatePrices(any())).thenReturn(new BulkOperationResultDto(3));

        mockMvc.perform(post("/admin/products/bulk/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "selectionMode": "SELECTED",
                                  "productIds": [1, 2, 3],
                                  "operation": "INCREASE_PERCENT",
                                  "value": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedCount").value(3));
    }

    @Test
    void updatePrices_ShouldReturn400_WhenValueScaleIsInvalid() throws Exception {
        mockMvc.perform(post("/admin/products/bulk/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "selectionMode": "SELECTED",
                                  "productIds": [1],
                                  "operation": "INCREASE_AMOUNT",
                                  "value": 1.123
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteProducts_ShouldReturn400_WhenSelectionModeIsMissing() throws Exception {
        mockMvc.perform(post("/admin/products/bulk/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productIds": [1]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategories_ShouldReturnAffectedCount() throws Exception {
        when(adminProductService.updateCategories(any())).thenReturn(new BulkOperationResultDto(2));

        mockMvc.perform(post("/admin/products/bulk/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "selectionMode": "SELECTED",
                                  "productIds": [1, 2],
                                  "categoryId": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedCount").value(2));
    }

    @Test
    void updateCategories_ShouldReturn400_WhenTargetCategoryIsMissing() throws Exception {
        mockMvc.perform(post("/admin/products/bulk/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "selectionMode": "SELECTED",
                                  "productIds": [1],
                                  "categoryId": null
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replaceHomeProducts_ShouldReturn200_WhenRequestIsValid() throws Exception {
        mockMvc.perform(put("/admin/products/home")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productIds\":[3,1,5]}"))
                .andExpect(status().isOk());

        verify(adminHomeProductService).replaceHomeProducts(any());
    }

    @Test
    void reorderHomeProducts_ShouldReturn400_WhenServiceRejectsSet() throws Exception {
        doThrow(new BadRequestException("Invalid home set"))
                .when(adminHomeProductService).reorderHomeProducts(any());

        mockMvc.perform(put("/admin/products/home-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productIds\":[1,2]}"))
                .andExpect(status().isBadRequest());
    }

    private static AdminProductRequestDto request() {
        return new AdminProductRequestDto("Bench", "Description", new BigDecimal("10.00"), CATEGORY_ID);
    }

    private static AdminProductDetailDto product() {
        return new AdminProductDetailDto(
                PRODUCT_ID,
                "Bench",
                "Description",
                new BigDecimal("10.00"),
                new CategoryDto(CATEGORY_ID, "Furniture"),
                List.of(new ProductMediaDto(
                        5L,
                        "products/10/images/file.jpg",
                        org.plishka.backend.domain.media.MediaType.IMAGE,
                        true,
                        1
                ))
        );
    }
}
