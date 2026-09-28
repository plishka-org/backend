package org.plishka.backend.controller.admin;

import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.admin.home.AdminHomePageContentRequestDto;
import org.plishka.backend.dto.admin.home.AdminHomePageDto;
import org.plishka.backend.dto.home.HomePageContentDto;
import org.plishka.backend.service.admin.home.AdminHomePageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminHomePageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminHomePageControllerTest extends BaseControllerTest {
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
                .andExpect(jsonPath("$.content.description").value("Description"));
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

    private static AdminHomePageDto homePage() {
        return new AdminHomePageDto(new HomePageContentDto("Home", "Description"));
    }
}
