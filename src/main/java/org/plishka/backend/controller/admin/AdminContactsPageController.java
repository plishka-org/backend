package org.plishka.backend.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageContentRequestDto;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageDto;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageSocialLinkDto;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageSocialLinkRequestDto;
import org.plishka.backend.dto.contacts.ContactsPageContentDto;
import org.plishka.backend.service.admin.contacts.AdminContactsPageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/contacts-page")
@RequiredArgsConstructor
public class AdminContactsPageController {
    private final AdminContactsPageService adminContactsPageService;

    @GetMapping
    public AdminContactsPageDto getContactsPage() {
        return adminContactsPageService.getContactsPage();
    }

    @PutMapping
    public ContactsPageContentDto updateContactsPage(@Valid @RequestBody AdminContactsPageContentRequestDto request) {
        return adminContactsPageService.updateContactsPageContent(request);
    }

    @PostMapping("/social-links")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminContactsPageSocialLinkDto createSocialLink(
            @Valid @RequestBody AdminContactsPageSocialLinkRequestDto request
    ) {
        return adminContactsPageService.createSocialLink(request);
    }

    @PutMapping("/social-links/{socialLinkId}")
    public AdminContactsPageSocialLinkDto updateSocialLink(
            @Positive @PathVariable Long socialLinkId,
            @Valid @RequestBody AdminContactsPageSocialLinkRequestDto request
    ) {
        return adminContactsPageService.updateSocialLink(socialLinkId, request);
    }

    @DeleteMapping("/social-links/{socialLinkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSocialLink(@Positive @PathVariable Long socialLinkId) {
        adminContactsPageService.deleteSocialLink(socialLinkId);
    }
}
