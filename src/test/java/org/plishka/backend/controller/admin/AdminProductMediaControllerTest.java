package org.plishka.backend.controller.admin;

import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.admin.catalog.product.AdminProductMediaService;
import org.plishka.backend.service.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductMediaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminProductMediaControllerTest extends BaseControllerTest {
    private static final String ATTACH_MEDIA_ENDPOINT = "/admin/products/{id}/media/attach";
    private static final Long PRODUCT_ID = 10L;
    private static final Long MEDIA_ID = 5L;
    private static final Long NOT_FOUND_PRODUCT_ID = 999L;
    private static final Long INVALID_PRODUCT_ID = -1L;
    private static final String BLANK_S3_KEY = "";
    private static final String MEDIA_ALREADY_ATTACHED_MESSAGE = "This media file is already attached.";
    private static final String PRODUCT_NOT_FOUND_MESSAGE = "Product not found";
    private static final String PRODUCT_MEDIA_KEY =
            "products/10/images/2026/05/550e8400-e29b-41d4-a716-446655440000.jpg";
    private static final String NOT_FOUND_PRODUCT_MEDIA_KEY =
            "products/999/images/2026/05/550e8400-e29b-41d4-a716-446655440001.jpg";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private AdminProductMediaService adminProductMediaService;

    @Test
    void attachMedia_ShouldReturn200_WhenRequestIsValid() throws Exception {
        AttachMediaRequestDto request = attachMediaRequest(PRODUCT_MEDIA_KEY);

        performAttachMedia(PRODUCT_ID, request)
                .andExpect(status().isOk());

        verify(productService).attachMedia(eq(PRODUCT_ID), any(AttachMediaRequestDto.class));
    }

    @Test
    void attachMedia_ShouldReturn400_WhenS3KeyIsBlank() throws Exception {
        AttachMediaRequestDto request = attachMediaRequest(BLANK_S3_KEY);

        performAttachMedia(PRODUCT_ID, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void attachMedia_ShouldReturn400_WhenProductIdIsNotPositive() throws Exception {
        AttachMediaRequestDto request = attachMediaRequest(PRODUCT_MEDIA_KEY);

        performAttachMedia(INVALID_PRODUCT_ID, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void attachMedia_ShouldReturn400_WhenMediaAlreadyAttached() throws Exception {
        AttachMediaRequestDto request = attachMediaRequest(PRODUCT_MEDIA_KEY);

        doThrow(new BadRequestException(MEDIA_ALREADY_ATTACHED_MESSAGE))
                .when(productService).attachMedia(eq(PRODUCT_ID), any(AttachMediaRequestDto.class));

        performAttachMedia(PRODUCT_ID, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void attachMedia_ShouldReturn404_WhenProductNotFound() throws Exception {
        AttachMediaRequestDto request = attachMediaRequest(NOT_FOUND_PRODUCT_MEDIA_KEY);

        doThrow(new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE))
                .when(productService).attachMedia(eq(NOT_FOUND_PRODUCT_ID), any(AttachMediaRequestDto.class));

        performAttachMedia(NOT_FOUND_PRODUCT_ID, request)
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMedia_ShouldReturn204_WhenMediaExists() throws Exception {
        doNothing().when(adminProductMediaService).deleteMedia(PRODUCT_ID, MEDIA_ID);

        mockMvc.perform(delete("/admin/products/{productId}/media/{mediaId}", PRODUCT_ID, MEDIA_ID))
                .andExpect(status().isNoContent());

        verify(adminProductMediaService).deleteMedia(PRODUCT_ID, MEDIA_ID);
    }

    @Test
    void deleteMedia_ShouldReturn404_WhenMediaNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Media not found"))
                .when(adminProductMediaService).deleteMedia(PRODUCT_ID, MEDIA_ID);

        mockMvc.perform(delete("/admin/products/{productId}/media/{mediaId}", PRODUCT_ID, MEDIA_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAllMedia_ShouldReturn204_WhenProductExists() throws Exception {
        mockMvc.perform(delete("/admin/products/{productId}/media", PRODUCT_ID))
                .andExpect(status().isNoContent());

        verify(adminProductMediaService).deleteAllMedia(PRODUCT_ID);
    }

    @Test
    void markPrimary_ShouldReturn200_WhenMediaIsImage() throws Exception {
        mockMvc.perform(put("/admin/products/{productId}/media/{mediaId}/primary", PRODUCT_ID, MEDIA_ID))
                .andExpect(status().isOk());

        verify(adminProductMediaService).markPrimary(PRODUCT_ID, MEDIA_ID);
    }

    @Test
    void markPrimary_ShouldReturn400_WhenMediaIsVideo() throws Exception {
        doThrow(new BadRequestException("Only IMAGE media can be primary"))
                .when(adminProductMediaService).markPrimary(PRODUCT_ID, MEDIA_ID);

        mockMvc.perform(put("/admin/products/{productId}/media/{mediaId}/primary", PRODUCT_ID, MEDIA_ID))
                .andExpect(status().isBadRequest());
    }

    private ResultActions performAttachMedia(Long productId, AttachMediaRequestDto request) throws Exception {
        return mockMvc.perform(post(ATTACH_MEDIA_ENDPOINT, productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));
    }

    private static AttachMediaRequestDto attachMediaRequest(String s3Key) {
        return new AttachMediaRequestDto(s3Key);
    }
}
