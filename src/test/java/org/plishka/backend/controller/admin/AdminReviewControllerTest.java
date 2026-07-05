package org.plishka.backend.controller.admin;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.admin.review.AdminReviewDetailDto;
import org.plishka.backend.dto.admin.review.AdminReviewFeaturedRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewSummaryDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.review.ReviewMediaDto;
import org.plishka.backend.dto.review.ReviewMediaPreviewDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.admin.review.AdminReviewService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.plishka.backend.domain.media.MediaType.IMAGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminReviewControllerTest extends BaseControllerTest {
    private static final String DELETE_REVIEW_ENDPOINT = "/admin/reviews/{id}";
    private static final long REVIEW_ID = 1L;
    private static final long NOT_FOUND_REVIEW_ID = 999L;
    private static final long MEDIA_ID = 5L;
    private static final String REVIEW_MEDIA_KEY =
            "reviews/1/images/2026/05/7223994a-bf40-4cba-9f60-234162a211fa.jpg";
    private static final String REVIEW_NOT_FOUND_MESSAGE = "Review with ID 999 not found";
    private static final String FEATURED_LIMIT_MESSAGE = "Home page cannot contain more than 5 featured reviews";
    private static final Instant CREATED_AT = Instant.parse("2026-04-15T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminReviewService adminReviewService;

    @Test
    void getReviews_ShouldReturnPaginatedReviews() throws Exception {
        when(adminReviewService.getReviews(0, 16)).thenReturn(reviewPageResponse());

        mockMvc.perform(get("/admin/reviews")
                        .param("page", "0")
                        .param("size", "16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reviewId").value(REVIEW_ID))
                .andExpect(jsonPath("$.content[0].isFeatured").value(true))
                .andExpect(jsonPath("$.content[0].primaryMedia.s3Key").value(REVIEW_MEDIA_KEY));
    }

    @Test
    void getReview_ShouldReturnReviewDetails() throws Exception {
        when(adminReviewService.getReview(REVIEW_ID)).thenReturn(reviewDetail());

        mockMvc.perform(get("/admin/reviews/{id}", REVIEW_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(REVIEW_ID))
                .andExpect(jsonPath("$.isFeatured").value(true))
                .andExpect(jsonPath("$.media[0].isPrimary").value(true));
    }

    @Test
    void createReview_ShouldReturn201() throws Exception {
        AdminReviewRequestDto request = new AdminReviewRequestDto("Olha Petrova", "Great work");
        when(adminReviewService.createReview(any(AdminReviewRequestDto.class))).thenReturn(reviewDetail());

        mockMvc.perform(post("/admin/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewId").value(REVIEW_ID));
    }

    @Test
    void updateReview_ShouldReturnUpdatedReview() throws Exception {
        AdminReviewRequestDto request = new AdminReviewRequestDto("Olha Petrova", "Updated content");
        when(adminReviewService.updateReview(eq(REVIEW_ID), any(AdminReviewRequestDto.class)))
                .thenReturn(reviewDetail());

        mockMvc.perform(put("/admin/reviews/{id}", REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(REVIEW_ID));
    }

    @Test
    void deleteReview_ShouldReturn204() throws Exception {
        doNothing().when(adminReviewService).deleteReview(REVIEW_ID);

        mockMvc.perform(delete(DELETE_REVIEW_ENDPOINT, REVIEW_ID))
                .andExpect(status().isNoContent());

        verify(adminReviewService).deleteReview(REVIEW_ID);
    }

    @Test
    void deleteReview_ShouldReturn404_WhenReviewMissing() throws Exception {
        doThrow(new ResourceNotFoundException(REVIEW_NOT_FOUND_MESSAGE))
                .when(adminReviewService).deleteReview(NOT_FOUND_REVIEW_ID);

        mockMvc.perform(delete(DELETE_REVIEW_ENDPOINT, NOT_FOUND_REVIEW_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateFeatured_ShouldReturnUpdatedReview() throws Exception {
        when(adminReviewService.updateFeatured(eq(REVIEW_ID), any(AdminReviewFeaturedRequestDto.class)))
                .thenReturn(reviewDetail());

        mockMvc.perform(patch("/admin/reviews/{id}/featured", REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"featured\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFeatured").value(true));
    }

    @Test
    void updateFeatured_ShouldReturn400_WhenFeaturedLimitReached() throws Exception {
        doThrow(new BadRequestException(FEATURED_LIMIT_MESSAGE))
                .when(adminReviewService).updateFeatured(eq(REVIEW_ID), any(AdminReviewFeaturedRequestDto.class));

        mockMvc.perform(patch("/admin/reviews/{id}/featured", REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"featured\": true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReview_ShouldReturn400_WhenAuthorNameIsBlank() throws Exception {
        mockMvc.perform(post("/admin/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorName": " ",
                                  "content": "Great work"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReview_ShouldReturn404_WhenReviewMissing() throws Exception {
        when(adminReviewService.getReview(REVIEW_ID))
                .thenThrow(new ResourceNotFoundException("Review with ID 1 not found"));

        mockMvc.perform(get("/admin/reviews/{id}", REVIEW_ID))
                .andExpect(status().isNotFound());
    }

    private static PageResponse<AdminReviewSummaryDto> reviewPageResponse() {
        return new PageResponse<>(
                List.of(new AdminReviewSummaryDto(
                        REVIEW_ID,
                        "Olha Petrova",
                        "Great work",
                        CREATED_AT,
                        true,
                        new ReviewMediaPreviewDto(MEDIA_ID, REVIEW_MEDIA_KEY, IMAGE)
                )),
                0,
                16,
                1L,
                1,
                true
        );
    }

    private static AdminReviewDetailDto reviewDetail() {
        return new AdminReviewDetailDto(
                REVIEW_ID,
                "Olha Petrova",
                "Great work",
                CREATED_AT,
                true,
                List.of(new ReviewMediaDto(MEDIA_ID, REVIEW_MEDIA_KEY, IMAGE, true, 1))
        );
    }
}
