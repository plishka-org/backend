package org.plishka.backend.controller.admin;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageContentRequestDto;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageDto;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageSocialLinkDto;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageSocialLinkRequestDto;
import org.plishka.backend.dto.contacts.ContactsPageContentDto;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.service.admin.contacts.AdminContactsPageService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminContactsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminContactsPageControllerTest extends BaseControllerTest {
    private static final long SOCIAL_LINK_ID = 5L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminContactsPageService adminContactsPageService;

    @Test
    void getContactsPage_ShouldReturnContactsPageData() throws Exception {
        when(adminContactsPageService.getContactsPage()).thenReturn(contactsPage());

        mockMvc.perform(get("/admin/contacts-page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.phoneNumber").value("+380501234567"))
                .andExpect(jsonPath("$.socialLinks[0].socialLinkId").value(SOCIAL_LINK_ID))
                .andExpect(jsonPath("$.socialLinks[0].displayOrder").value(1));
    }

    @Test
    void updateContactsPage_ShouldReturnUpdatedContent() throws Exception {
        AdminContactsPageContentRequestDto request = new AdminContactsPageContentRequestDto(
                "+380501234567",
                "info@plishka.com",
                "Kyiv",
                "https://maps.example"
        );
        when(adminContactsPageService.updateContactsPageContent(any(AdminContactsPageContentRequestDto.class)))
                .thenReturn(new ContactsPageContentDto(
                        "+380501234567",
                        "info@plishka.com",
                        "Kyiv",
                        "https://maps.example"
                ));

        mockMvc.perform(put("/admin/contacts-page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("+380501234567"))
                .andExpect(jsonPath("$.email").value("info@plishka.com"));
    }

    @Test
    void updateContactsPage_ShouldReturn400_WhenPhoneIsInvalid() throws Exception {
        mockMvc.perform(put("/admin/contacts-page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "0501234567",
                                  "email": "info@plishka.com",
                                  "address": "Kyiv",
                                  "googleMapsUrl": "https://maps.example"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateContactsPage_ShouldReturn400_WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(put("/admin/contacts-page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "+380501234567",
                                  "email": "not-an-email",
                                  "address": "Kyiv",
                                  "googleMapsUrl": "https://maps.example"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSocialLink_ShouldReturn201_WhenRequestIsValid() throws Exception {
        AdminContactsPageSocialLinkRequestDto request =
                new AdminContactsPageSocialLinkRequestDto("Instagram", "https://instagram.com/plishka");
        when(adminContactsPageService.createSocialLink(any(AdminContactsPageSocialLinkRequestDto.class)))
                .thenReturn(socialLink());

        mockMvc.perform(post("/admin/contacts-page/social-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.socialLinkId").value(SOCIAL_LINK_ID))
                .andExpect(jsonPath("$.name").value("Instagram"));
    }

    @Test
    void createSocialLink_ShouldReturn400_WhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/admin/contacts-page/social-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "url": "https://instagram.com/plishka"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSocialLink_ShouldReturnUpdatedSocialLink() throws Exception {
        AdminContactsPageSocialLinkRequestDto request =
                new AdminContactsPageSocialLinkRequestDto("Telegram", "https://t.me/plishka");
        when(adminContactsPageService.updateSocialLink(eq(SOCIAL_LINK_ID), any(AdminContactsPageSocialLinkRequestDto.class)))
                .thenReturn(socialLink());

        mockMvc.perform(put("/admin/contacts-page/social-links/{socialLinkId}", SOCIAL_LINK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.socialLinkId").value(SOCIAL_LINK_ID));
    }

    @Test
    void deleteSocialLink_ShouldReturn204() throws Exception {
        doNothing().when(adminContactsPageService).deleteSocialLink(SOCIAL_LINK_ID);

        mockMvc.perform(delete("/admin/contacts-page/social-links/{socialLinkId}", SOCIAL_LINK_ID))
                .andExpect(status().isNoContent());

        verify(adminContactsPageService).deleteSocialLink(SOCIAL_LINK_ID);
    }

    @Test
    void updateSocialLink_ShouldReturn404_WhenSocialLinkMissing() throws Exception {
        when(adminContactsPageService.updateSocialLink(eq(SOCIAL_LINK_ID), any(AdminContactsPageSocialLinkRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Contacts page social link with ID 5 not found"));

        mockMvc.perform(put("/admin/contacts-page/social-links/{socialLinkId}", SOCIAL_LINK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AdminContactsPageSocialLinkRequestDto("Instagram", "https://instagram.com")
                        )))
                .andExpect(status().isNotFound());
    }

    private static AdminContactsPageDto contactsPage() {
        return new AdminContactsPageDto(
                new ContactsPageContentDto("+380501234567", "info@plishka.com", "Kyiv", "https://maps.example"),
                List.of(socialLink())
        );
    }

    private static AdminContactsPageSocialLinkDto socialLink() {
        return new AdminContactsPageSocialLinkDto(SOCIAL_LINK_ID, "Instagram", "https://instagram.com/plishka", 1);
    }
}
