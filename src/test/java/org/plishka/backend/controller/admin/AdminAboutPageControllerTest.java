package org.plishka.backend.controller.admin;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.about.AboutPageContentDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageContentRequestDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageMediaDto;
import org.plishka.backend.service.admin.about.AdminAboutPageService;
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

@WebMvcTest(AdminAboutPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAboutPageControllerTest extends BaseControllerTest {
    private static final long MEDIA_ID = 7L;
    private static final String ABOUT_PAGE_MEDIA_KEY =
            "about/1/images/2026/05/550e8400-e29b-41d4-a716-446655440010.jpg";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminAboutPageService adminAboutPageService;

    @Test
    void getAboutPage_ShouldReturnAboutPageData() throws Exception {
        when(adminAboutPageService.getAboutPage()).thenReturn(aboutPage());

        mockMvc.perform(get("/admin/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.mainTitle").value("Main title"))
                .andExpect(jsonPath("$.media[0].mediaId").value(MEDIA_ID))
                .andExpect(jsonPath("$.media[0].displayOrder").value(1));
    }

    @Test
    void updateAboutPage_ShouldReturnUpdatedContent() throws Exception {
        AdminAboutPageContentRequestDto request = new AdminAboutPageContentRequestDto(
                "Main title",
                "Main subtitle",
                "Secondary title",
                "Secondary subtitle"
        );
        when(adminAboutPageService.updateAboutPageContent(any(AdminAboutPageContentRequestDto.class)))
                .thenReturn(new AboutPageContentDto("Main title", "Main subtitle", "Secondary title", "Secondary subtitle"));

        mockMvc.perform(put("/admin/about")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mainTitle").value("Main title"))
                .andExpect(jsonPath("$.secondarySubtitle").value("Secondary subtitle"));
    }

    @Test
    void updateAboutPage_ShouldReturn400_WhenMainTitleIsBlank() throws Exception {
        mockMvc.perform(put("/admin/about")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mainTitle": " ",
                                  "mainSubtitle": "Main subtitle",
                                  "secondaryTitle": "Secondary title",
                                  "secondarySubtitle": "Secondary subtitle"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private static AdminAboutPageDto aboutPage() {
        return new AdminAboutPageDto(
                new AboutPageContentDto("Main title", "Main subtitle", "Secondary title", "Secondary subtitle"),
                List.of(new AdminAboutPageMediaDto(MEDIA_ID, ABOUT_PAGE_MEDIA_KEY,
                        org.plishka.backend.domain.media.MediaType.IMAGE, 1))
        );
    }
}
