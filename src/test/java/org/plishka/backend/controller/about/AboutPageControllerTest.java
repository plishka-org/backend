package org.plishka.backend.controller.about;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.about.AboutPageContentDto;
import org.plishka.backend.dto.about.AboutPageResponse;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.service.about.AboutPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AboutPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AboutPageControllerTest extends BaseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AboutPageService aboutPageService;

    @Test
    void getAboutPage_ShouldReturnAboutDataAndStatus200() throws Exception {
        AboutPageResponse mockResponse = new AboutPageResponse(
                new AboutPageContentDto("Історія нашої майстерні..."),
                List.of()
        );
        when(aboutPageService.getAboutPageData()).thenReturn(mockResponse);

        mockMvc.perform(get("/about")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.historyText").value("Історія нашої майстерні..."))
                .andExpect(jsonPath("$.media").isArray());
    }

    @Test
    void attachMedia_ShouldReturn200_WhenRequestIsValid() throws Exception {
        AttachMediaRequestDto request = new AttachMediaRequestDto("about/1/images/test-image.jpg");

        mockMvc.perform(post("/about/media/attach")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(aboutPageService).attachMedia(request);
    }

    @Test
    void attachMedia_ShouldReturn400_WhenS3KeyIsBlank() throws Exception {
        AttachMediaRequestDto request = new AttachMediaRequestDto("");

        mockMvc.perform(post("/about/media/attach")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
