package org.plishka.backend.controller.admin;

import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.service.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminProductMediaSecurityTest extends BaseControllerTest {
    private static final String PRODUCT_MEDIA_KEY =
            "products/10/images/2026/05/550e8400-e29b-41d4-a716-446655440000.jpg";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @Test
    void attachMedia_ShouldReturn401_WhenUserIsAnonymous() throws Exception {
        AttachMediaRequestDto request = new AttachMediaRequestDto(PRODUCT_MEDIA_KEY);

        mockMvc.perform(post("/admin/products/{id}/media/attach", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void attachMedia_ShouldReturn403_WhenUserIsNotAdmin() throws Exception {
        AttachMediaRequestDto request = new AttachMediaRequestDto(PRODUCT_MEDIA_KEY);

        mockMvc.perform(post("/admin/products/{id}/media/attach", 10L)
                        .with(user("user@example.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void attachMedia_ShouldReturn200_WhenUserIsAdmin() throws Exception {
        Long productId = 10L;
        AttachMediaRequestDto request = new AttachMediaRequestDto(PRODUCT_MEDIA_KEY);

        doNothing().when(productService).attachMedia(eq(productId), any(AttachMediaRequestDto.class));

        mockMvc.perform(post("/admin/products/{id}/media/attach", productId)
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(productService).attachMedia(eq(productId), any(AttachMediaRequestDto.class));
    }
}
