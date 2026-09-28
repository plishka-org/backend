package org.plishka.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin - Contacts")
@SecurityRequirement(name = "bearerAuth")
public class AdminContactsPageController {
    private final AdminContactsPageService adminContactsPageService;

    @Operation(
            operationId = "adminGetContactsPage",
            summary = "Admin get contacts page",
            description = "Requires active user with ROLE_ADMIN."
    )
    @GetMapping
    public AdminContactsPageDto getContactsPage() {
        return adminContactsPageService.getContactsPage();
    }

    @Operation(
            operationId = "adminUpdateContactsPage",
            summary = "Admin update contacts page content",
            description = "Requires active user with ROLE_ADMIN. Contact fields are optional and may be null."
    )
    @PutMapping
    public ContactsPageContentDto updateContactsPage(@Valid @RequestBody AdminContactsPageContentRequestDto request) {
        return adminContactsPageService.updateContactsPageContent(request);
    }

    @Operation(
            operationId = "adminCreateContactSocialLink",
            summary = "Admin create contact social link",
            description = "Requires active user with ROLE_ADMIN."
    )
    @ApiResponse(responseCode = "201", description = "Social link created.")
    @PostMapping("/social-links")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminContactsPageSocialLinkDto createSocialLink(
            @Valid @RequestBody AdminContactsPageSocialLinkRequestDto request
    ) {
        return adminContactsPageService.createSocialLink(request);
    }

    @Operation(
            operationId = "adminUpdateContactSocialLink",
            summary = "Admin update contact social link",
            description = "Requires active user with ROLE_ADMIN."
    )
    @PutMapping("/social-links/{socialLinkId}")
    public AdminContactsPageSocialLinkDto updateSocialLink(
            @Positive @PathVariable Long socialLinkId,
            @Valid @RequestBody AdminContactsPageSocialLinkRequestDto request
    ) {
        return adminContactsPageService.updateSocialLink(socialLinkId, request);
    }

    @Operation(
            operationId = "adminDeleteContactSocialLink",
            summary = "Admin delete contact social link",
            description = "Requires active user with ROLE_ADMIN."
    )
    @ApiResponse(responseCode = "204", description = "Social link deleted.")
    @DeleteMapping("/social-links/{socialLinkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSocialLink(@Positive @PathVariable Long socialLinkId) {
        adminContactsPageService.deleteSocialLink(socialLinkId);
    }
}
