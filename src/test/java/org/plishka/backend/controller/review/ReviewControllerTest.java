package org.plishka.backend.controller.review;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.review.ReviewService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest extends BaseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    void getReviews_ShouldReturnPaginatedReviewsAndStatus200() throws Exception {
        PageResponse mockResponse = new PageResponse<>(List.of(), 0, 16, 0, 0, true);
        when(reviewService.getApprovedReviews(0, 16)).thenReturn(mockResponse);

        mockMvc.perform(get("/reviews")
                        .param("page", "0")
                        .param("size", "16")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageSize").value(16));
    }

    @Test
    void getFeaturedReviews_ShouldReturnListAndStatus200() throws Exception {
        when(reviewService.getFeaturedReviews()).thenReturn(List.of());

        mockMvc.perform(get("/reviews/featured")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void attachMedia_ShouldReturn200_WhenRequestIsValid() throws Exception {
        Long reviewId = 1L;
        AttachMediaRequestDto request = new AttachMediaRequestDto("reviews/1/images/test.jpg");

        doNothing().when(reviewService).attachMedia(eq(reviewId), any(AttachMediaRequestDto.class));

        mockMvc.perform(post("/reviews/{id}/media/attach", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(reviewService).attachMedia(eq(reviewId), any(AttachMediaRequestDto.class));
    }

    @Test
    void attachMedia_ShouldReturn400_WhenS3KeyIsBlank() throws Exception {
        Long reviewId = 1L;
        AttachMediaRequestDto request = new AttachMediaRequestDto("");

        mockMvc.perform(post("/reviews/{id}/media/attach", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attachMedia_ShouldReturn404_WhenReviewNotFound() throws Exception {
        Long notFoundReviewId = 999L;
        AttachMediaRequestDto request = new AttachMediaRequestDto("reviews/999/images/test.jpg");

        doThrow(new ResourceNotFoundException("Review not found"))
                .when(reviewService).attachMedia(eq(notFoundReviewId), any(AttachMediaRequestDto.class));

        mockMvc.perform(post("/reviews/{id}/media/attach", notFoundReviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void attachMedia_ShouldReturn400_WhenMediaAlreadyAttached() throws Exception {
        Long reviewId = 1L;
        AttachMediaRequestDto request = new AttachMediaRequestDto("reviews/1/images/test.jpg");

        doThrow(new BadRequestException("This media file is already attached."))
                .when(reviewService).attachMedia(eq(reviewId), any(AttachMediaRequestDto.class));

        mockMvc.perform(post("/reviews/{id}/media/attach", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
