package org.plishka.backend.controller.admin;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.about.AboutPageContentDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageContentRequestDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageMediaDto;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.admin.about.AdminAboutPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(AdminAboutPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAboutPageControllerTest extends BaseControllerTest {
    private static final String ATTACH_MEDIA_ENDPOINT = "/admin/about/media/attach";
    private static final String DELETE_MEDIA_ENDPOINT = "/admin/about/media/{mediaId}";
    private static final long MEDIA_ID = 7L;
    private static final String BLANK_S3_KEY = "";
    private static final String MEDIA_ALREADY_ATTACHED_MESSAGE = "This media file is already attached.";
    private static final String ABOUT_MEDIA_KEY =
            "about/1/images/2026/05/7223994a-bf40-4cba-9f60-234162a211fa.jpg";
    private static final String ABOUT_PAGE_MEDIA_KEY =
            "about/1/images/2026/05/550e8400-e29b-41d4-a716-446655440010.jpg";
    private static final String MEDIA_NOT_FOUND_MESSAGE = "About page media with ID 7 not found";

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
                .andExpect(jsonPath("$.content.historyTitle").value("History"))
                .andExpect(jsonPath("$.media[0].mediaId").value(MEDIA_ID))
                .andExpect(jsonPath("$.media[0].displayOrder").value(1));
    }

    @Test
    void updateAboutPage_ShouldReturnUpdatedContent() throws Exception {
        AdminAboutPageContentRequestDto request = new AdminAboutPageContentRequestDto(
                "History",
                "History text",
                "Current",
                "Current text"
        );
        when(adminAboutPageService.updateAboutPageContent(any(AdminAboutPageContentRequestDto.class)))
                .thenReturn(new AboutPageContentDto("History", "History text", "Current", "Current text"));

        mockMvc.perform(put("/admin/about")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historyTitle").value("History"))
                .andExpect(jsonPath("$.currentText").value("Current text"));
    }

    @Test
    void updateAboutPage_ShouldReturn400_WhenHistoryTitleIsBlank() throws Exception {
        mockMvc.perform(put("/admin/about")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "historyTitle": " ",
                                  "historyText": "History text",
                                  "currentTitle": "Current",
                                  "currentText": "Current text"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attachMedia_ShouldReturn200_WhenRequestIsValid() throws Exception {
        AttachMediaRequestDto request = attachMediaRequest(ABOUT_MEDIA_KEY);
        doNothing().when(adminAboutPageService).attachMedia(any(AttachMediaRequestDto.class));

        performAttachMedia(request)
                .andExpect(status().isOk());

        verify(adminAboutPageService).attachMedia(request);
    }

    @Test
    void attachMedia_ShouldReturn400_WhenS3KeyIsBlank() throws Exception {
        AttachMediaRequestDto request = attachMediaRequest(BLANK_S3_KEY);

        performAttachMedia(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void attachMedia_ShouldReturn400_WhenMediaAlreadyAttached() throws Exception {
        AttachMediaRequestDto request = attachMediaRequest(ABOUT_MEDIA_KEY);

        doThrow(new BadRequestException(MEDIA_ALREADY_ATTACHED_MESSAGE))
                .when(adminAboutPageService).attachMedia(any(AttachMediaRequestDto.class));

        performAttachMedia(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteMedia_ShouldReturn204() throws Exception {
        doNothing().when(adminAboutPageService).deleteMedia(MEDIA_ID);

        mockMvc.perform(delete(DELETE_MEDIA_ENDPOINT, MEDIA_ID))
                .andExpect(status().isNoContent());

        verify(adminAboutPageService).deleteMedia(MEDIA_ID);
    }

    @Test
    void deleteAllMedia_ShouldReturn204() throws Exception {
        doNothing().when(adminAboutPageService).deleteAllMedia();

        mockMvc.perform(delete("/admin/about/media"))
                .andExpect(status().isNoContent());

        verify(adminAboutPageService).deleteAllMedia();
    }

    @Test
    void reorderMedia_ShouldReturn200_WhenOrderIsValid() throws Exception {
        doNothing().when(adminAboutPageService).reorderMedia(any());

        mockMvc.perform(put("/admin/about/media/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mediaIds": [2, 1]
                                }
                                """))
                .andExpect(status().isOk());

        verify(adminAboutPageService).reorderMedia(any());
    }

    @Test
    void reorderMedia_ShouldReturn400_WhenServiceRejectsOrder() throws Exception {
        doThrow(new BadRequestException("Media order must contain the current about page media ids"))
                .when(adminAboutPageService).reorderMedia(any());

        mockMvc.perform(put("/admin/about/media/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mediaIds": [2]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteMedia_ShouldReturn404_WhenMediaMissing() throws Exception {
        doThrow(new ResourceNotFoundException(MEDIA_NOT_FOUND_MESSAGE))
                .when(adminAboutPageService).deleteMedia(MEDIA_ID);

        mockMvc.perform(delete(DELETE_MEDIA_ENDPOINT, MEDIA_ID))
                .andExpect(status().isNotFound());
    }

    private ResultActions performAttachMedia(AttachMediaRequestDto request) throws Exception {
        return mockMvc.perform(post(ATTACH_MEDIA_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private static AttachMediaRequestDto attachMediaRequest(String s3Key) {
        return new AttachMediaRequestDto(s3Key);
    }

    private static AdminAboutPageDto aboutPage() {
        return new AdminAboutPageDto(
                new AboutPageContentDto("History", "History text", "Current", "Current text"),
                List.of(new AdminAboutPageMediaDto(MEDIA_ID, ABOUT_PAGE_MEDIA_KEY,
                        org.plishka.backend.domain.media.MediaType.IMAGE, 1))
        );
    }
}
