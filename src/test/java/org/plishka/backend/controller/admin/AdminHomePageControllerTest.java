package org.plishka.backend.controller.admin;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.admin.home.AdminHomePageAdvantageDto;
import org.plishka.backend.dto.admin.home.AdminHomePageAdvantageRequestDto;
import org.plishka.backend.dto.admin.home.AdminHomePageContentRequestDto;
import org.plishka.backend.dto.admin.home.AdminHomePageDto;
import org.plishka.backend.dto.home.HomePageContentDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.admin.home.AdminHomePageService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminHomePageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminHomePageControllerTest extends BaseControllerTest {
    private static final String CREATE_ADVANTAGE_ENDPOINT = "/admin/home-page/advantages";
    private static final String DELETE_ADVANTAGE_ENDPOINT = "/admin/home-page/advantages/{advantageId}";
    private static final long ADVANTAGE_ID = 5L;
    private static final String ICON_KEY =
            "about/1/images/2026/05/550e8400-e29b-41d4-a716-446655440000.png";
    private static final String ADVANTAGE_NOT_FOUND_MESSAGE = "Home page advantage with ID 5 not found";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminHomePageService adminHomePageService;

    @Test
    void getHomePage_ShouldReturnHomePageData() throws Exception {
        when(adminHomePageService.getHomePage()).thenReturn(homePage());

        mockMvc.perform(get("/admin/home-page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.title").value("Home"))
                .andExpect(jsonPath("$.advantages[0].advantageId").value(ADVANTAGE_ID))
                .andExpect(jsonPath("$.advantages[0].displayOrder").value(1));
    }

    @Test
    void updateHomePage_ShouldReturnUpdatedContent() throws Exception {
        AdminHomePageContentRequestDto request = new AdminHomePageContentRequestDto("Updated title", "Updated description");
        when(adminHomePageService.updateHomePageContent(any(AdminHomePageContentRequestDto.class)))
                .thenReturn(new HomePageContentDto("Updated title", "Updated description"));

        mockMvc.perform(put("/admin/home-page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void updateHomePage_ShouldReturn400_WhenTitleIsBlank() throws Exception {
        mockMvc.perform(put("/admin/home-page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "description": "Description"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAdvantage_ShouldReturn201_WhenRequestIsValid() throws Exception {
        AdminHomePageAdvantageRequestDto request =
                new AdminHomePageAdvantageRequestDto("Quality", "Handmade", ICON_KEY);
        when(adminHomePageService.createAdvantage(any(AdminHomePageAdvantageRequestDto.class))).thenReturn(advantage());

        performCreateAdvantage(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.advantageId").value(ADVANTAGE_ID))
                .andExpect(jsonPath("$.title").value("Quality"));
    }

    @Test
    void createAdvantage_ShouldReturn400_WhenTitleIsBlank() throws Exception {
        performCreateAdvantage(new AdminHomePageAdvantageRequestDto(" ", "Handmade", ICON_KEY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAdvantage_ShouldReturnUpdatedAdvantage() throws Exception {
        AdminHomePageAdvantageRequestDto request =
                new AdminHomePageAdvantageRequestDto("Quality", "Updated", ICON_KEY);
        when(adminHomePageService.updateAdvantage(eq(ADVANTAGE_ID), any(AdminHomePageAdvantageRequestDto.class)))
                .thenReturn(advantage());

        mockMvc.perform(put("/admin/home-page/advantages/{advantageId}", ADVANTAGE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advantageId").value(ADVANTAGE_ID));
    }

    @Test
    void deleteAdvantage_ShouldReturn204() throws Exception {
        doNothing().when(adminHomePageService).deleteAdvantage(ADVANTAGE_ID);

        mockMvc.perform(delete(DELETE_ADVANTAGE_ENDPOINT, ADVANTAGE_ID))
                .andExpect(status().isNoContent());

        verify(adminHomePageService).deleteAdvantage(ADVANTAGE_ID);
    }

    @Test
    void deleteAdvantage_ShouldReturn404_WhenAdvantageMissing() throws Exception {
        doThrow(new ResourceNotFoundException(ADVANTAGE_NOT_FOUND_MESSAGE))
                .when(adminHomePageService).deleteAdvantage(ADVANTAGE_ID);

        mockMvc.perform(delete(DELETE_ADVANTAGE_ENDPOINT, ADVANTAGE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void reorderAdvantages_ShouldReturn200_WhenOrderIsValid() throws Exception {
        doNothing().when(adminHomePageService).reorderAdvantages(any());

        mockMvc.perform(put("/admin/home-page/advantages/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "advantageIds": [2, 1]
                                }
                                """))
                .andExpect(status().isOk());

        verify(adminHomePageService).reorderAdvantages(any());
    }

    @Test
    void reorderAdvantages_ShouldReturn400_WhenServiceRejectsOrder() throws Exception {
        doThrow(new BadRequestException("Advantage order must contain the current home page advantage ids"))
                .when(adminHomePageService).reorderAdvantages(any());

        mockMvc.perform(put("/admin/home-page/advantages/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "advantageIds": [2]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAdvantage_ShouldReturn404_WhenAdvantageMissing() throws Exception {
        when(adminHomePageService.updateAdvantage(eq(ADVANTAGE_ID), any(AdminHomePageAdvantageRequestDto.class)))
                .thenThrow(new ResourceNotFoundException(ADVANTAGE_NOT_FOUND_MESSAGE));

        mockMvc.perform(put("/admin/home-page/advantages/{advantageId}", ADVANTAGE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AdminHomePageAdvantageRequestDto("Quality", null, null)
                        )))
                .andExpect(status().isNotFound());
    }

    private ResultActions performCreateAdvantage(AdminHomePageAdvantageRequestDto request) throws Exception {
        return mockMvc.perform(post(CREATE_ADVANTAGE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private static AdminHomePageDto homePage() {
        return new AdminHomePageDto(
                new HomePageContentDto("Home", "Description"),
                List.of(advantage())
        );
    }

    private static AdminHomePageAdvantageDto advantage() {
        return new AdminHomePageAdvantageDto(ADVANTAGE_ID, "Quality", "Handmade", ICON_KEY, 1);
    }
}
