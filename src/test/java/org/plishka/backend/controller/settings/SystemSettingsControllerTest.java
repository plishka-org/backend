package org.plishka.backend.controller.settings;

import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.settings.SystemSettingsDto;
import org.plishka.backend.service.settings.SystemSettingsService;
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

@WebMvcTest(SystemSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemSettingsControllerTest extends BaseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemSettingsService systemSettingsService;

    @Test
    void getSettings_ShouldReturnSettingsAndStatus200() throws Exception {
        SystemSettingsDto mockResponse = new SystemSettingsDto(false);
        when(systemSettingsService.getPublicSettings()).thenReturn(mockResponse);

        mockMvc.perform(get("/settings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isShopModeEnabled").value(false));
    }
}
