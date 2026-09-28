package org.plishka.backend.controller.review;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.review.ReviewDetailDto;
import org.plishka.backend.dto.review.ReviewMediaDto;
import org.plishka.backend.dto.review.ReviewMediaPreviewDto;
import org.plishka.backend.dto.review.ReviewSummaryDto;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.review.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.when;
import static org.plishka.backend.domain.media.MediaType.IMAGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest extends BaseControllerTest {
    private static final String REVIEWS_ENDPOINT = "/reviews";
    private static final String REVIEW_DETAILS_ENDPOINT = "/reviews/{id}";

    private static final Long REVIEW_ID = 1L;
    private static final Long REVIEW_MEDIA_ID = 5L;
    private static final Long NOT_FOUND_REVIEW_ID = 999L;
    private static final Long INVALID_REVIEW_ID = -1L;
    private static final int PAGE_NUMBER = 0;
    private static final int PAGE_SIZE = 16;
    private static final int TOTAL_PAGES = 1;
    private static final long TOTAL_ELEMENTS = 1L;
    private static final int DISPLAY_ORDER = 1;
    private static final String AUTHOR_NAME = "Olha Petrova";
    private static final String REVIEW_CONTENT = "Great work";
    private static final Instant CREATED_AT = Instant.parse("2026-04-15T10:00:00Z");
    private static final String REVIEW_MEDIA_KEY =
            "reviews/1/images/2026/05/550e8400-e29b-41d4-a716-446655440000.jpg";
    private static final String REVIEW_NOT_FOUND_MESSAGE = "Review not found";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    void getReviews_ShouldReturnPaginatedReviewsAndStatus200() throws Exception {
        PageResponse<ReviewSummaryDto> response = reviewPageResponse();
        when(reviewService.getReviews(PAGE_NUMBER, PAGE_SIZE)).thenReturn(response);

        performGetReviews(PAGE_NUMBER, PAGE_SIZE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].reviewId").value(REVIEW_ID))
                .andExpect(jsonPath("$.content[0].authorName").value(AUTHOR_NAME))
                .andExpect(jsonPath("$.content[0].primaryMedia.s3Key").value(REVIEW_MEDIA_KEY))
                .andExpect(jsonPath("$.content[0].primaryMedia.mediaType").value("IMAGE"))
                .andExpect(jsonPath("$.content[0].media").doesNotExist())
                .andExpect(jsonPath("$.pageSize").value(PAGE_SIZE));
    }

    @Test
    void getReview_ShouldReturnReviewDetailsAndStatus200() throws Exception {
        ReviewDetailDto response = reviewDetail();
        when(reviewService.getReview(REVIEW_ID)).thenReturn(response);

        performGetReview(REVIEW_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(REVIEW_ID))
                .andExpect(jsonPath("$.authorName").value(AUTHOR_NAME))
                .andExpect(jsonPath("$.media[0].s3Key").value(REVIEW_MEDIA_KEY))
                .andExpect(jsonPath("$.media[0].isPrimary").value(true))
                .andExpect(jsonPath("$.media[0].displayOrder").value(DISPLAY_ORDER));
    }

    @Test
    void getReview_ShouldReturn404_WhenReviewNotFound() throws Exception {
        when(reviewService.getReview(NOT_FOUND_REVIEW_ID))
                .thenThrow(new ResourceNotFoundException(REVIEW_NOT_FOUND_MESSAGE));

        performGetReview(NOT_FOUND_REVIEW_ID)
                .andExpect(status().isNotFound());
    }

    @Test
    void getReview_ShouldReturn400_WhenIdIsNotPositive() throws Exception {
        performGetReview(INVALID_REVIEW_ID)
                .andExpect(status().isBadRequest());
    }

    private ResultActions performGetReviews(int page, int size) throws Exception {
        return mockMvc.perform(get(REVIEWS_ENDPOINT)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions performGetReview(Long reviewId) throws Exception {
        return mockMvc.perform(get(REVIEW_DETAILS_ENDPOINT, reviewId)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private static PageResponse<ReviewSummaryDto> reviewPageResponse() {
        return new PageResponse<>(
                List.of(reviewSummary()),
                PAGE_NUMBER,
                PAGE_SIZE,
                TOTAL_ELEMENTS,
                TOTAL_PAGES,
                true
        );
    }

    private static ReviewSummaryDto reviewSummary() {
        return new ReviewSummaryDto(
                REVIEW_ID,
                AUTHOR_NAME,
                REVIEW_CONTENT,
                CREATED_AT,
                reviewMediaPreview()
        );
    }

    private static ReviewDetailDto reviewDetail() {
        return new ReviewDetailDto(
                REVIEW_ID,
                AUTHOR_NAME,
                REVIEW_CONTENT,
                CREATED_AT,
                List.of(reviewMedia())
        );
    }

    private static ReviewMediaPreviewDto reviewMediaPreview() {
        return new ReviewMediaPreviewDto(
                REVIEW_MEDIA_ID,
                REVIEW_MEDIA_KEY,
                IMAGE
        );
    }

    private static ReviewMediaDto reviewMedia() {
        return new ReviewMediaDto(
                REVIEW_MEDIA_ID,
                REVIEW_MEDIA_KEY,
                IMAGE,
                true,
                DISPLAY_ORDER
        );
    }
}
