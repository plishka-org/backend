package org.plishka.backend.controller.contacts;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.contacts.ContactsPageResponse;
import org.plishka.backend.service.contacts.ContactsPageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contacts-page")
@RequiredArgsConstructor
@Tag(name = "Contacts")
public class ContactsPageController {
    private final ContactsPageService contactsPageService;

    @Operation(
            operationId = "getContactsPage",
            summary = "Get contacts page content",
            description = "Public contacts page content and social links."
    )
    @GetMapping
    public ContactsPageResponse getContactsPage() {
        return contactsPageService.getContactsPageData();
    }
}
