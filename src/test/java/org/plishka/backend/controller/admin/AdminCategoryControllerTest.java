package org.plishka.backend.controller.admin;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.admin.category.AdminCategoryRequestDto;
import org.plishka.backend.dto.admin.category.CategoryDeleteStrategy;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.exception.ConflictException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.admin.catalog.category.AdminCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCategoryControllerTest extends BaseControllerTest {
    private static final long CATEGORY_ID = 1L;
    private static final String CATEGORY_NAME = "Gazebos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminCategoryService adminCategoryService;

    @Test
    void getCategories_ShouldReturnCategoriesAndStatus200() throws Exception {
        when(adminCategoryService.getCategories("gaz")).thenReturn(List.of(category()));

        mockMvc.perform(get("/admin/categories")
                        .param("search", "gaz")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryId").value(CATEGORY_ID))
                .andExpect(jsonPath("$[0].name").value(CATEGORY_NAME));
    }

    @Test
    void getCategories_ShouldReturn400_WhenSearchIsTooLong() throws Exception {
        mockMvc.perform(get("/admin/categories")
                        .param("search", "a".repeat(101)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_ShouldReturn201_WhenRequestIsValid() throws Exception {
        AdminCategoryRequestDto request = new AdminCategoryRequestDto(CATEGORY_NAME);
        when(adminCategoryService.createCategory(any(AdminCategoryRequestDto.class))).thenReturn(category());

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID));
    }

    @Test
    void createCategory_ShouldReturn400_WhenNameIsBlank() throws Exception {
        AdminCategoryRequestDto request = new AdminCategoryRequestDto(" ");

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_ShouldReturn409_WhenNameExists() throws Exception {
        AdminCategoryRequestDto request = new AdminCategoryRequestDto(CATEGORY_NAME);
        when(adminCategoryService.createCategory(any(AdminCategoryRequestDto.class)))
                .thenThrow(new ConflictException("Category already exists"));

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateCategory_ShouldReturnCategory_WhenRequestIsValid() throws Exception {
        AdminCategoryRequestDto request = new AdminCategoryRequestDto(CATEGORY_NAME);
        when(adminCategoryService.updateCategory(eq(CATEGORY_ID), any(AdminCategoryRequestDto.class)))
                .thenReturn(category());

        mockMvc.perform(put("/admin/categories/{id}", CATEGORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID));
    }

    @Test
    void updateCategory_ShouldReturn404_WhenCategoryNotFound() throws Exception {
        AdminCategoryRequestDto request = new AdminCategoryRequestDto(CATEGORY_NAME);
        when(adminCategoryService.updateCategory(eq(CATEGORY_ID), any(AdminCategoryRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(put("/admin/categories/{id}", CATEGORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCategory_ShouldReturn204_WhenRequestIsValid() throws Exception {
        doNothing().when(adminCategoryService)
                .deleteCategory(CATEGORY_ID, CategoryDeleteStrategy.KEEP_PRODUCTS, null);

        mockMvc.perform(delete("/admin/categories/{id}", CATEGORY_ID)
                        .param("strategy", "KEEP_PRODUCTS"))
                .andExpect(status().isNoContent());

        verify(adminCategoryService).deleteCategory(CATEGORY_ID, CategoryDeleteStrategy.KEEP_PRODUCTS, null);
    }

    @Test
    void deleteCategory_ShouldPassMoveTargetToService() throws Exception {
        long targetCategoryId = 2L;

        mockMvc.perform(delete("/admin/categories/{id}", CATEGORY_ID)
                        .param("strategy", "MOVE_PRODUCTS")
                        .param("targetCategoryId", String.valueOf(targetCategoryId)))
                .andExpect(status().isNoContent());

        verify(adminCategoryService).deleteCategory(CATEGORY_ID, CategoryDeleteStrategy.MOVE_PRODUCTS, targetCategoryId);
    }

    private static CategoryDto category() {
        return new CategoryDto(CATEGORY_ID, CATEGORY_NAME);
    }
}
