package org.plishka.backend.controller.admin;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.dto.file.PresignUploadRequestDto;
import org.plishka.backend.dto.file.PresignUploadResponseDto;
import org.plishka.backend.service.file.FilePresignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminFileController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminFileControllerTest extends BaseControllerTest {
    private static final String UPLOAD_ENDPOINT = "/admin/files/presign/upload";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FilePresignService filePresignService;

    @Test
    void presignUpload_ShouldReturnPresignedUpload() throws Exception {
        PresignUploadRequestDto request = new PresignUploadRequestDto(
                MediaTargetType.PRODUCT,
                10L,
                org.plishka.backend.domain.media.MediaType.IMAGE,
                "image/jpeg",
                1024L,
                "bench.jpg",
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        );
        PresignUploadResponseDto response = PresignUploadResponseDto.builder()
                .s3Key("products/10/images/bench.jpg")
                .uploadUrl("https://storage.example/upload")
                .method("PUT")
                .expiresAt(Instant.parse("2026-06-29T10:00:00Z"))
                .requiredHeaders(Map.of("Content-Type", "image/jpeg"))
                .build();

        when(filePresignService.presignUpload(any(PresignUploadRequestDto.class))).thenReturn(response);

        mockMvc.perform(post(UPLOAD_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.s3Key").value("products/10/images/bench.jpg"))
                .andExpect(jsonPath("$.method").value("PUT"));

        verify(filePresignService).presignUpload(any(PresignUploadRequestDto.class));
    }
}
