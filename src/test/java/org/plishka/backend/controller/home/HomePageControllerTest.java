package org.plishka.backend.controller.home;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.home.HomePageContentDto;
import org.plishka.backend.dto.home.HomePageProductDto;
import org.plishka.backend.dto.home.HomePageResponse;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.service.home.HomePageService;
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

@WebMvcTest(HomePageController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomePageControllerTest extends BaseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomePageService homePageService;

    @Test
    void getHomePage_ShouldReturnHomePageDataAndStatus200() throws Exception {
        HomePageResponse mockResponse = new HomePageResponse(
                new HomePageContentDto("Головна", "Опис головної"),
                List.of(),
                List.of(new HomePageProductDto(
                        1L,
                        "Garden bench",
                        new CategoryDto(2L, "Garden"),
                        1200L,
                        null
                )),
                List.of()
        );
        when(homePageService.getHomePageData()).thenReturn(mockResponse);

        mockMvc.perform(get("/home")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.title").value("Головна"))
                .andExpect(jsonPath("$.content.description").value("Опис головної"))
                .andExpect(jsonPath("$.advantages").isArray())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.products[0].productId").value(1))
                .andExpect(jsonPath("$.products[0].name").value("Garden bench"))
                .andExpect(jsonPath("$.products[0].price").value(1200))
                .andExpect(jsonPath("$.featuredReviews").isArray());
    }
}
