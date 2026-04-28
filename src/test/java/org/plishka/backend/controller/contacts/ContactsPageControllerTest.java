package org.plishka.backend.controller.contacts;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.controller.BaseControllerTest;
import org.plishka.backend.dto.contacts.ContactsPageResponse;
import org.plishka.backend.service.contacts.ContactsPageService;
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

@WebMvcTest(ContactsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContactsPageControllerTest extends BaseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContactsPageService contactsPageService;

    @Test
    void getContactsPage_ShouldReturnContactsDataAndStatus200() throws Exception {
        ContactsPageResponse mockResponse = new ContactsPageResponse(
                "+380991234567",
                "test@email.com",
                "Київ",
                "https://maps.google.com/...",
                List.of()
        );
        when(contactsPageService.getContactsPageData()).thenReturn(mockResponse);

        mockMvc.perform(get("/contacts-page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("+380991234567"))
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.socialLinks").isArray());
    }
}
