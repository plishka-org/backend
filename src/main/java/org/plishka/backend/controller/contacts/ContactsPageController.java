package org.plishka.backend.controller.contacts;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.contacts.ContactsPageResponse;
import org.plishka.backend.service.contacts.ContactsPageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contacts-page")
@RequiredArgsConstructor
public class ContactsPageController {
    private final ContactsPageService contactsPageService;

    @GetMapping
    public ContactsPageResponse getContactsPage() {
        return contactsPageService.getContactsPageData();
    }
}
