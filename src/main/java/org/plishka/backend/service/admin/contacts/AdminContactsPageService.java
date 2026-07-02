package org.plishka.backend.service.admin.contacts;

import org.plishka.backend.dto.admin.contacts.AdminContactsPageContentRequestDto;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageDto;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageSocialLinkDto;
import org.plishka.backend.dto.admin.contacts.AdminContactsPageSocialLinkRequestDto;
import org.plishka.backend.dto.contacts.ContactsPageContentDto;

public interface AdminContactsPageService {
    AdminContactsPageDto getContactsPage();

    ContactsPageContentDto updateContactsPageContent(AdminContactsPageContentRequestDto request);

    AdminContactsPageSocialLinkDto createSocialLink(AdminContactsPageSocialLinkRequestDto request);

    AdminContactsPageSocialLinkDto updateSocialLink(
            Long socialLinkId,
            AdminContactsPageSocialLinkRequestDto request
    );

    void deleteSocialLink(Long socialLinkId);
}
