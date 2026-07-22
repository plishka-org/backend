package org.plishka.backend.controller.admin;

import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.admin.settings.AdminSettingsDto;
import org.plishka.backend.dto.admin.settings.AdminSettingsRequestDto;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.service.admin.settings.AdminSettingsService;
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

@WebMvcTest(AdminSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminSettingsControllerTest extends BaseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminSettingsService adminSettingsService;

    @Test
    void getSettings_ShouldReturnSettings() throws Exception {
        when(adminSettingsService.getSettings()).thenReturn(new AdminSettingsDto(true, "admin@example.com"));

        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isShopModeEnabled").value(true))
                .andExpect(jsonPath("$.adminEmail").value("admin@example.com"));
    }

    @Test
    void updateSettings_ShouldReturnUpdatedSettings() throws Exception {
        AdminSettingsRequestDto request = new AdminSettingsRequestDto(false, "admin@example.com");
        when(adminSettingsService.updateSettings(any(AdminSettingsRequestDto.class)))
                .thenReturn(new AdminSettingsDto(false, "admin@example.com"));

        mockMvc.perform(put("/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isShopModeEnabled").value(false))
                .andExpect(jsonPath("$.adminEmail").value("admin@example.com"));
    }

    @Test
    void updateSettings_ShouldReturn400_WhenShopModeFlagIsMissing() throws Exception {
        mockMvc.perform(put("/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSettings_ShouldReturn500_WhenSettingsMissing() throws Exception {
        when(adminSettingsService.getSettings())
                .thenThrow(new RequiredSingletonUnavailableException("System settings not found"));

        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void updateSettings_ShouldReturn500_WhenSettingsMissing() throws Exception {
        AdminSettingsRequestDto request = new AdminSettingsRequestDto(false, "admin@example.com");
        when(adminSettingsService.updateSettings(any(AdminSettingsRequestDto.class)))
                .thenThrow(new RequiredSingletonUnavailableException("System settings not found"));

        mockMvc.perform(put("/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
