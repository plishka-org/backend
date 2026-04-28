package org.plishka.backend.controller.review;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.service.review.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest extends BaseControllerTest {
    @Autowired
    private MockMvc mockMvc;

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
}
