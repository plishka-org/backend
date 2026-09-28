package org.plishka.backend.controller.admin;

import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.admin.review.AdminReviewMediaService;
import org.plishka.backend.service.admin.review.AdminReviewService;
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

@WebMvcTest(AdminReviewMediaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminReviewMediaControllerTest extends BaseControllerTest {
    private static final String ATTACH_MEDIA_ENDPOINT = "/admin/reviews/{id}/media/attach";
    private static final String DELETE_MEDIA_ENDPOINT = "/admin/reviews/{reviewId}/media/{mediaId}";
    private static final String MARK_PRIMARY_ENDPOINT = "/admin/reviews/{reviewId}/media/{mediaId}/primary";
    private static final long REVIEW_ID = 1L;
    private static final long NOT_FOUND_REVIEW_ID = 999L;
    private static final long MEDIA_ID = 5L;
    private static final String BLANK_S3_KEY = "";
    private static final String REVIEW_MEDIA_KEY =
            "reviews/1/images/2026/05/7223994a-bf40-4cba-9f60-234162a211fa.jpg";
    private static final String MEDIA_ALREADY_ATTACHED_MESSAGE = "This media file is already attached.";
    private static final String REVIEW_NOT_FOUND_MESSAGE = "Review with ID 999 not found";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminReviewService adminReviewService;

    @MockitoBean
    private AdminReviewMediaService adminReviewMediaService;

    @Test
    void attachMedia_ShouldReturn200_WhenRequestIsValid() throws Exception {
        AttachMediaRequestDto request = attachMediaRequest(REVIEW_MEDIA_KEY);
        doNothing().when(adminReviewService).attachMedia(eq(REVIEW_ID), any(AttachMediaRequestDto.class));

        performAttachMedia(REVIEW_ID, request)
                .andExpect(status().isOk());

        verify(adminReviewService).attachMedia(eq(REVIEW_ID), any(AttachMediaRequestDto.class));
    }

    @Test
    void attachMedia_ShouldReturn400_WhenS3KeyIsBlank() throws Exception {
        AttachMediaRequestDto request = attachMediaRequest(BLANK_S3_KEY);

        performAttachMedia(REVIEW_ID, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void attachMedia_ShouldReturn404_WhenReviewNotFound() throws Exception {
        AttachMediaRequestDto request = attachMediaRequest(REVIEW_MEDIA_KEY);

        doThrow(new ResourceNotFoundException(REVIEW_NOT_FOUND_MESSAGE))
                .when(adminReviewService).attachMedia(eq(NOT_FOUND_REVIEW_ID), any(AttachMediaRequestDto.class));

        performAttachMedia(NOT_FOUND_REVIEW_ID, request)
                .andExpect(status().isNotFound());
    }

    @Test
    void attachMedia_ShouldReturn400_WhenMediaAlreadyAttached() throws Exception {
        doThrow(new BadRequestException(MEDIA_ALREADY_ATTACHED_MESSAGE))
                .when(adminReviewService).attachMedia(eq(REVIEW_ID), any(AttachMediaRequestDto.class));

        performAttachMedia(REVIEW_ID, attachMediaRequest(REVIEW_MEDIA_KEY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteMedia_ShouldReturn204() throws Exception {
        doNothing().when(adminReviewMediaService).deleteMedia(REVIEW_ID, MEDIA_ID);

        mockMvc.perform(delete(DELETE_MEDIA_ENDPOINT, REVIEW_ID, MEDIA_ID))
                .andExpect(status().isNoContent());

        verify(adminReviewMediaService).deleteMedia(REVIEW_ID, MEDIA_ID);
    }

    @Test
    void deleteMedia_ShouldReturn404_WhenMediaNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Media not found"))
                .when(adminReviewMediaService).deleteMedia(REVIEW_ID, MEDIA_ID);

        mockMvc.perform(delete(DELETE_MEDIA_ENDPOINT, REVIEW_ID, MEDIA_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAllMedia_ShouldReturn204() throws Exception {
        doNothing().when(adminReviewMediaService).deleteAllMedia(REVIEW_ID);

        mockMvc.perform(delete("/admin/reviews/{reviewId}/media", REVIEW_ID))
                .andExpect(status().isNoContent());

        verify(adminReviewMediaService).deleteAllMedia(REVIEW_ID);
    }

    @Test
    void markPrimary_ShouldReturn200() throws Exception {
        doNothing().when(adminReviewMediaService).markPrimary(REVIEW_ID, MEDIA_ID);

        mockMvc.perform(put(MARK_PRIMARY_ENDPOINT, REVIEW_ID, MEDIA_ID))
                .andExpect(status().isOk());

        verify(adminReviewMediaService).markPrimary(REVIEW_ID, MEDIA_ID);
    }

    @Test
    void markPrimary_ShouldReturn400_WhenMediaIsVideo() throws Exception {
        doThrow(new BadRequestException("Only IMAGE media can be primary"))
                .when(adminReviewMediaService).markPrimary(REVIEW_ID, MEDIA_ID);

        mockMvc.perform(put(MARK_PRIMARY_ENDPOINT, REVIEW_ID, MEDIA_ID))
                .andExpect(status().isBadRequest());
    }

    private ResultActions performAttachMedia(Long reviewId, AttachMediaRequestDto request) throws Exception {
        return mockMvc.perform(post(ATTACH_MEDIA_ENDPOINT, reviewId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private static AttachMediaRequestDto attachMediaRequest(String s3Key) {
        return new AttachMediaRequestDto(s3Key);
    }
}
